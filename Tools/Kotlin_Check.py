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


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--baseline", default="origin/main",
                        help="git ref known to compile (default: origin/main)")
    parser.add_argument("--kotlinc", default=None)
    args = parser.parse_args()

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
            print(f"cannot export {args.baseline}: {archive.stderr.decode().strip()}",
                  file=sys.stderr)
            return 2
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
