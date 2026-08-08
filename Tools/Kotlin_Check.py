#!/usr/bin/env python3
"""
Catches Kotlin compile errors introduced by the working tree, without an
Android SDK.

Why this exists
---------------
A full type-check needs `android.jar`, which is only distributed through
dl.google.com. Running kotlinc without it produces thousands of errors — every
Android and Compose symbol is unresolved — so the output is useless as a
pass/fail signal, and filtering it by hand hides real errors among the noise.
That is exactly how an `Unresolved reference 'floorDiv'` reached CI.

The trick is that the noise is IDENTICAL on both sides of a change. Compile the
baseline (a ref that is known to build) and the working tree with the same
compiler and the same missing classpath, reduce each diagnostic to its message
text, and diff the two multisets. Whatever is new is the change's own fault.

Usage
-----
    python3 Tools/check_kotlin.py [--baseline origin/main] [--kotlinc PATH]

Set OMNI_KOTLINC to point at a kotlinc binary, or pass --kotlinc. Exits non-zero
when the working tree introduces diagnostics the baseline did not have.
"""

import argparse
import collections
import os
import re
import glob
import shutil
import subprocess
import sys
import tempfile

SOURCE_GLOB = "Backrooms/Source/Main/Kotlin/com/omni/backrooms"

# Two shapes, because the compiler reports differently depending on how it is
# driven. Gradle's build-tools API emits
#     e: file:///abs/File.kt:120:31 Unresolved reference 'floorDiv'.
# while the standalone CLI emits
#     path/File.kt:120:31: error: unresolved reference 'floorDiv'.
DIAGNOSTIC_RES = (
    re.compile(r"^e: file://(?P<path>.+?\.kts?):(?P<line>\d+):(?P<col>\d+) (?P<msg>.*)$"),
    re.compile(r"^(?P<path>.+?\.kts?):(?P<line>\d+):(?P<col>\d+): error: (?P<msg>.*)$"),
)


def parse_diagnostic(line: str):
    for pattern in DIAGNOSTIC_RES:
        match = pattern.match(line)
        if match:
            # Case differs between the two front ends ("Unresolved" vs
            # "unresolved"), so normalise before the two sides are compared.
            return match.group("msg").strip().lower()
    return None

# Diagnostics that are purely a consequence of the missing Android classpath.
# They appear in equal numbers on both sides, so the diff already cancels them —
# these are dropped only to keep a failure report readable.
CASCADE_PATTERNS = (
    "overrides nothing",
    "should be called only from a coroutine",
    "illegal annotation class",
    "is ambiguous for this expression",
    "'operator' modifier is required",
    "'this' is not defined in this context",
)


# Names written with an explicit `kotlin.` or `java.` qualifier. Those packages
# ARE on the classpath even without the Android SDK, so if the compiler cannot
# resolve one of them the symbol genuinely does not exist — no amount of missing
# Android jars explains it. This is the precise discriminator that separates a
# real mistake from classpath noise, and the one that would have caught
# `kotlin.math.floorDiv` (an extension, never a two-argument function) before it
# reached CI.
STDLIB_REF_RE = re.compile(r"\b(?:kotlin|java)(?:\.[a-z][A-Za-z0-9_]*)+\.([A-Za-z_][A-Za-z0-9_]*)")
UNRESOLVED_RE = re.compile(r"unresolved reference '([^']+)'")


def stdlib_leaf_names(root: str) -> set:
    """Leaf identifiers used through a fully-qualified kotlin./java. path."""
    names = set()
    src = os.path.join(root, SOURCE_GLOB)
    if not os.path.isdir(src):
        return names
    for entry in os.listdir(src):
        if not entry.endswith(".kt"):
            continue
        with open(os.path.join(src, entry), encoding="utf-8") as fh:
            for line in fh:
                stripped = line.lstrip()
                # Imports name the package, not a call — an unresolved import is
                # the Android classpath talking, not a missing stdlib symbol.
                if stripped.startswith("import ") or stripped.startswith("package "):
                    continue
                for match in STDLIB_REF_RE.finditer(line):
                    names.add(match.group(1).lower())
    return names


# Top-level (and member) declarations this project makes. Used to tell a
# cascading extension-on-an-Android-receiver apart from a name that simply is
# not there any more.
DECL_RE = re.compile(
    r"^\s*(?:@\w+\s+)*(?:private |internal |public |protected )?"
    r"(?:inline |suspend |external |override |operator )*"
    r"(?:fun(?:\s+<[^>]+>)?\s+(?:[A-Za-z0-9_.<>?]+\.)?|val\s+|var\s+|class\s+|object\s+|enum class\s+|data class\s+)"
    r"([A-Za-z_][A-Za-z0-9_]*)"
)


def declared_names(root: str) -> set:
    names = set()
    src = os.path.join(root, SOURCE_GLOB)
    if not os.path.isdir(src):
        return names
    for entry in os.listdir(src):
        if not entry.endswith(".kt"):
            continue
        with open(os.path.join(src, entry), encoding="utf-8") as fh:
            for line in fh:
                match = DECL_RE.match(line)
                if match:
                    names.add(match.group(1).lower())
    return names


def find_kotlinc(explicit: str | None) -> str:
    for candidate in (explicit, os.environ.get("OMNI_KOTLINC"), shutil.which("kotlinc")):
        if candidate and os.path.exists(candidate):
            return candidate
    print("kotlinc not found. Pass --kotlinc or set OMNI_KOTLINC.", file=sys.stderr)
    sys.exit(2)


def compile_tree(kotlinc: str, root: str, outdir: str) -> collections.Counter:
    """Runs kotlinc over one tree and returns a multiset of diagnostic messages."""
    src = os.path.join(root, SOURCE_GLOB)
    if not os.path.isdir(src):
        print(f"no Kotlin sources under {src}", file=sys.stderr)
        sys.exit(2)
    files = sorted(
        os.path.join(src, f) for f in os.listdir(src) if f.endswith(".kt")
    )
    result = subprocess.run(
        [kotlinc, "-nowarn", "-d", outdir, *files],
        capture_output=True, text=True,
    )
    messages = collections.Counter()
    for line in (result.stdout + result.stderr).splitlines():
        msg = parse_diagnostic(line.strip())
        if msg:
            messages[msg] += 1
    return messages


# ===========================================================================
# Gradle dependencies against what the source actually imports
# ===========================================================================

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
KT_GLOB = os.path.join(REPO_ROOT, "Backrooms/Source/Main/Kotlin/**/*.kt")
APP_GRADLE = os.path.join(REPO_ROOT, "Backrooms/build.gradle.kts")
CATALOG = os.path.join(REPO_ROOT, "Gradle/libs.versions.toml")

# Library alias -> the import prefix its classes arrive under. Only libraries
# whose absence is invisible until link time need to be here; anything the
# Kotlin compiler resolves inside this module does not.
# Library alias -> the import prefixes that library, and only that library,
# provides. Per artifact rather than per top-level package: androidx.media3.ui
# and androidx.media3.exoplayer are two separate dependencies, and a check keyed
# on "androidx.media3" lets one of them vanish behind the other.
LIB_PREFIXES = {
    "androidx-media3-exoplayer": ("androidx.media3.exoplayer", "androidx.media3.common"),
    "androidx-media3-ui":        ("androidx.media3.ui",),
    "retrofit-core":             ("retrofit2",),
    "okhttp":                    ("okhttp3",),
    "firebase-auth":             ("com.google.firebase.auth",),
    "firebase-firestore":        ("com.google.firebase.firestore",),
    "firebase-crashlytics":      ("com.google.firebase.crashlytics",),
    "firebase-messaging":        ("com.google.firebase.messaging",),
    "firebase-analytics":        ("com.google.firebase.analytics",),
    "firebase-config":           ("com.google.firebase.remoteconfig",),
    "androidx-credentials":      ("androidx.credentials",),
    "google-id-credential":      ("com.google.android.libraries.identity.googleid",),
    "androidx-room-runtime":     ("androidx.room",),
    "androidx-billing":          ("com.android.billingclient",),
}


def check_dependency_imports() -> list[str]:
    """
    Every import must have a dependency behind it, and the other way round.

    This exists because removing Firebase took androidx.media3 out with it. The
    grep that was supposed to prove media3 was unused had `-E` with `\\|`
    alternation, which under extended regex is a literal pipe, so it searched
    for the string "media3|ExoPlayer" and found nothing. Five imports were
    deleted out from under a composable that was still on screen, and the whole
    thing only surfaced in a Gradle build a minute and a half in.

    The Kotlin compiler cannot catch this here, because this tool runs without
    the Android classpath: an import of a library that is genuinely gone is
    indistinguishable from one whose jar simply is not on the path. Comparing
    the two lists is text, needs nothing installed, and is exact.
    """
    problems: list[str] = []
    gradle = open(APP_GRADLE, encoding="utf-8").read()

    imports: dict[str, set[str]] = {}
    for path in glob.glob(KT_GLOB, recursive=True):
        for line in open(path, encoding="utf-8"):
            m = re.match(r"import ([\w.]+)", line)
            if m:
                imports.setdefault(m.group(1), set()).add(os.path.basename(path))

    for alias, prefixes in sorted(LIB_PREFIXES.items()):
        accessor = alias.replace("-", ".")
        declared = bool(re.search(
            r"(?:implementation|api|ksp)\(\s*(?:platform\()?libs\." +
            re.escape(accessor) + r"\b", gradle))
        used = {imp: f for imp, f in imports.items()
                if any(imp.startswith(p + ".") for p in prefixes)}
        if used and not declared:
            where = sorted({f for fs in used.values() for f in fs})
            problems.append(
                f"{len(used)} import(s) of {'/'.join(prefixes)} in {', '.join(where)} "
                f"but libs.{accessor} is not a dependency — the Kotlin here cannot "
                f"resolve them and only a full Gradle build will say so")
        if declared and not used:
            problems.append(
                f"libs.{accessor} is a dependency and nothing imports {'/'.join(prefixes)}")
        state = "ok" if bool(used) == declared else "MISMATCH"
        print(f"   {alias:28s} declared={str(declared):5s} imports={len(used):<3d} {state}")
    return problems


def check_top_level_nesting() -> list[str]:
    """
    Every top-level class must actually be top-level.

    A deletion that takes a closing brace with it does not produce a syntax
    error: Kotlin simply reads everything after it as nested inside whatever
    was left open. The file parses, the compile pass here reports nothing new,
    and the damage only surfaces in KSP on a real build as
    "com.omni.backrooms.GuardManager.GuardVM.LobbyVM could not be resolved" --
    three top-level classes reported as one path.

    That is exactly what happened removing dead code: a helper walked backwards
    over blank lines before a declaration and ate the previous function's
    closing braces, twice, in two files. Both passed every check here.

    So this counts braces. A declaration written at column zero has to sit at
    depth zero; anything else means a brace went missing above it.
    """
    problems: list[str] = []
    decl = re.compile(r'^(?:@\w+\s*)?(?:internal |private |public )?'
                      r'(?:data |enum |sealed )?(?:class|object|interface)\s+(\w+)')
    for path in sorted(glob.glob(KT_GLOB, recursive=True)):
        src = open(path, encoding="utf-8").read()
        # Braces inside strings and comments are not structure.
        for pat, rep in ((r'"""(?:.|\n)*?"""', '""'),
                         (r'"(?:\\.|[^"\\\n])*"', '""'),
                         (r"'(?:\\.|[^'\\\n])'", "''"),
                         (r'//[^\n]*', ''),
                         (r'/\*(?:.|\n)*?\*/', '')):
            src = re.sub(pat, rep, src)
        depth = 0
        name = os.path.basename(path)
        for n, line in enumerate(src.split('\n'), 1):
            m = decl.match(line)
            if m and depth != 0:
                problems.append(
                    f"{name}:{n} declares {m.group(1)} at column zero but "
                    f"{depth} brace(s) are still open above it — a closing brace "
                    f"is missing, and Kotlin will read this as a nested class")
                break
            depth += line.count('{') - line.count('}')
        if depth != 0 and not problems:
            problems.append(f"{name} ends with {depth} unbalanced brace(s)")
        print(f"   {name:16s} {'ok' if not problems or name not in problems[-1] else 'UNBALANCED'}")
    return problems


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--baseline", default="origin/main",
                        help="git ref known to compile (default: origin/main)")
    parser.add_argument("--kotlinc", default=None)
    args = parser.parse_args()

    print("\n── Top-level nesting")
    nest_problems = check_top_level_nesting()
    for p in nest_problems:
        print("FAIL", p)

    print("\n── Gradle dependencies vs imports")
    dep_problems = nest_problems + check_dependency_imports()
    for p in dep_problems:
        print("FAIL", p)

    if args.kotlinc is None and not os.environ.get("OMNI_KOTLINC") and shutil.which("kotlinc") is None:
        # The text check above needs nothing installed, so a missing compiler
        # must not skip it. CI has no kotlinc and this is the half that runs
        # there.
        print("\nkotlinc not found — skipping the compile pass.")
        return 1 if dep_problems else 0

    kotlinc = find_kotlinc(args.kotlinc)

    with tempfile.TemporaryDirectory() as tmp:
        baseline_tree = os.path.join(tmp, "baseline")
        print(f"exporting baseline {args.baseline} ...")
        os.makedirs(baseline_tree)
        archive = subprocess.run(
            ["git", "archive", args.baseline],
            capture_output=True, check=False,
        )
        if archive.returncode != 0:
            # CI checks out shallow, so origin/main is usually not there. The
            # compile pass is a comparison against a known-good tree and cannot
            # run without one; the dependency half above needs no baseline and
            # has already run. Skipping beats failing on a missing ref.
            print(f"cannot export {args.baseline} "
                  f"({archive.stderr.decode().strip()}) — skipping the compile pass.")
            return 1 if dep_problems else 0
        subprocess.run(["tar", "-x", "-C", baseline_tree], input=archive.stdout, check=True)

        print("compiling baseline ...")
        before = compile_tree(kotlinc, baseline_tree, os.path.join(tmp, "out-base"))
        print("compiling working tree ...")
        after = compile_tree(kotlinc, ".", os.path.join(tmp, "out-head"))

    introduced = after - before
    resolved = before - after

    def is_cascade(msg: str) -> bool:
        return any(p in msg for p in CASCADE_PATTERNS)

    # A stdlib symbol that will not resolve is a real error, full stop.
    stdlib_names = stdlib_leaf_names(".")
    proven = {}
    for msg, count in after.items():
        match = UNRESOLVED_RE.search(msg)
        if match and match.group(1).lower() in stdlib_names:
            proven[msg] = count

    # Unresolved names that ARE declared somewhere in this project get their own
    # bucket. Most are extensions on an Android receiver the compiler could not
    # resolve, so they cascade — but a renamed or mis-called project function
    # lands here too, and that one is real. Worth eyeballing every time.
    declared = declared_names(".")
    project = {}
    for msg, count in introduced.items():
        match = UNRESOLVED_RE.search(msg)
        if match and match.group(1).lower() in declared and msg not in proven:
            project[msg] = count

    suspicious = {m: n for m, n in introduced.items()
                  if not is_cascade(m) and m not in proven and m not in project}
    cascade = {m: n for m, n in introduced.items() if is_cascade(m)}

    print(f"\nbaseline diagnostics : {sum(before.values())}")
    print(f"working tree         : {sum(after.values())}")
    print(f"resolved by change   : {sum(resolved.values())}")
    print(f"introduced by change : {sum(introduced.values())}")
    print(f"stdlib names checked : {len(stdlib_names)}")

    if proven:
        print("\nDEFINITELY BROKEN — a fully-qualified kotlin./java. symbol")
        print("does not exist. The stdlib IS on the classpath, so this is real:")
        for msg, count in sorted(proven.items(), key=lambda kv: -kv[1]):
            print(f"  x{count:<4} {msg}")

    if project:
        print("\nNEW, and names something this project declares. Usually an")
        print("extension whose receiver the compiler could not resolve — but a")
        print("renamed or wrongly-called project function looks identical:")
        for msg, count in sorted(project.items(), key=lambda kv: -kv[1]):
            print(f"  x{count:<4} {msg}")

    if suspicious:
        print("\nNEW since the baseline. Every one of these must be a genuine")
        print("Android/Compose API the baseline simply never referenced —")
        print("check each name before shipping:")
        for msg, count in sorted(suspicious.items(), key=lambda kv: -kv[1]):
            print(f"  x{count:<4} {msg}")

    if cascade:
        print("\nnew, and shaped like missing-classpath fallout:")
        for msg, count in sorted(cascade.items(), key=lambda kv: -kv[1]):
            print(f"  x{count:<4} {msg}")

    if proven:
        print("\nFAILED")
        return 1
    print("\nPASSED — no provably-broken references")
    return 0


if __name__ == "__main__":
    sys.exit(main())
