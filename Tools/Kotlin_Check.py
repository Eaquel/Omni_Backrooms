#!/usr/bin/env python3

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

DIAGNOSTIC_RES = (
    re.compile(r"^e: file://(?P<path>.+?\.kts?):(?P<line>\d+):(?P<col>\d+) (?P<msg>.*)$"),
    re.compile(r"^(?P<path>.+?\.kts?):(?P<line>\d+):(?P<col>\d+): error: (?P<msg>.*)$"),
)


def parse_diagnostic(line: str):
    for pattern in DIAGNOSTIC_RES:
        match = pattern.match(line)
        if match:
            return match.group("msg").strip().lower()
    return None

CASCADE_PATTERNS = (
    "overrides nothing",
    "should be called only from a coroutine",
    "illegal annotation class",
    "is ambiguous for this expression",
    "'operator' modifier is required",
    "'this' is not defined in this context",
)


STDLIB_REF_RE = re.compile(r"\b(?:kotlin|java)(?:\.[a-z][A-Za-z0-9_]*)+\.([A-Za-z_][A-Za-z0-9_]*)")
UNRESOLVED_RE = re.compile(r"unresolved reference '([^']+)'")


def stdlib_leaf_names(root: str) -> set:
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
                if stripped.startswith("import ") or stripped.startswith("package "):
                    continue
                for match in STDLIB_REF_RE.finditer(line):
                    names.add(match.group(1).lower())
    return names


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


REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
KT_GLOB = os.path.join(REPO_ROOT, "Backrooms/Source/Main/Kotlin/**/*.kt")
APP_GRADLE = os.path.join(REPO_ROOT, "Backrooms/build.gradle.kts")
CATALOG = os.path.join(REPO_ROOT, "Gradle/libs.versions.toml")
GAME = os.path.join(REPO_ROOT, "Backrooms/Source/Main/Kotlin/com/omni/backrooms/Backrooms.kt")

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


def check_renderer_isolation() -> list[str]:
    problems: list[str] = []
    src = open(GAME, encoding="utf-8").read()
    start = src.find("class OmniGLRenderer")
    if start < 0:
        return ["OmniGLRenderer is gone — this rule needs rewriting for whatever "
                "replaced it"]

    depth, i, opened = 0, src.index("{", start), False
    while i < len(src):
        if src[i] == "{":
            depth += 1
            opened = True
        elif src[i] == "}":
            depth -= 1
            if opened and depth == 0:
                break
        i += 1
    body = src[start:i]

    for m in re.finditer(r"\bbridge\s*\.", body):
        line = src.count("\n", 0, start + m.start()) + 1
        problems.append(
            f"Backrooms.kt:{line} OmniGLRenderer calls bridge.* — it has no "
            f"bridge, and it is not meant to: everything it needs from the game "
            f"comes in as a provider assigned from the composable, the way "
            f"chunkProvider and trailSource do")
    return problems


def check_chunk_miss_is_retried() -> list[str]:
    problems: list[str] = []
    src = open(GAME, encoding="utf-8").read()
    start = src.find("private fun streamChunks")
    if start < 0:
        return ["streamChunks is gone — this rule needs rewriting for whatever "
                "replaced it"]
    depth, i, opened = 0, src.index("{", start), False
    while i < len(src):
        if src[i] == "{":
            depth += 1
            opened = True
        elif src[i] == "}":
            depth -= 1
            if opened and depth == 0:
                break
        i += 1
    body = src[start:i]

    for m in re.finditer(r"chunkMeshes\s*\[[^\]]+\]\s*=\s*ChunkMesh\(\)", body):
        line = src.count("\n", 0, start + m.start()) + 1
        problems.append(
            f"Backrooms.kt:{line} streamChunks caches an empty ChunkMesh for a "
            f"chunk the provider missed — that is a permanent decision from a "
            f"transient answer, and it blanks the level on any device where the "
            f"GL thread starts before the world is valid. Record the miss in "
            f"chunkMisses so it is retried")
    if "?: ChunkMesh()" in body:
        problems.append(
            "streamChunks falls back to an empty ChunkMesh when the mesh build "
            "returns null — same permanent hole, same symptom")
    if "chunkMisses" not in body:
        problems.append(
            "streamChunks no longer consults chunkMisses — a missed chunk is "
            "either never retried or retried every single frame")
    return problems


def check_top_level_nesting() -> list[str]:
    problems: list[str] = []
    decl = re.compile(r'^(?:@\w+\s*)?(?:internal |private |public )?'
                      r'(?:data |enum |sealed )?(?:class|object|interface)\s+(\w+)')
    for path in sorted(glob.glob(KT_GLOB, recursive=True)):
        src = open(path, encoding="utf-8").read()
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

    print("\n── Renderer isolation")
    iso_problems = check_renderer_isolation()
    for p in iso_problems:
        print("FAIL", p)
    if not iso_problems:
        print("   OmniGLRenderer takes everything through providers")

    print("\n── Chunk streaming")
    miss_problems = check_chunk_miss_is_retried()
    for p in miss_problems:
        print("FAIL", p)
    if not miss_problems:
        print("   a missed chunk is retried, not written off")

    print("\n── Top-level nesting")
    nest_problems = iso_problems + miss_problems + check_top_level_nesting()
    for p in nest_problems:
        print("FAIL", p)

    print("\n── Gradle dependencies vs imports")
    dep_problems = nest_problems + check_dependency_imports()
    for p in dep_problems:
        print("FAIL", p)

    if args.kotlinc is None and not os.environ.get("OMNI_KOTLINC") and shutil.which("kotlinc") is None:
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

    stdlib_names = stdlib_leaf_names(".")
    proven = {}
    for msg, count in after.items():
        match = UNRESOLVED_RE.search(msg)
        if match and match.group(1).lower() in stdlib_names:
            proven[msg] = count

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
