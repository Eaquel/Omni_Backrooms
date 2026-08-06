#!/usr/bin/env python3
"""
Compiles every GLSL shader embedded in the Kotlin sources.

A shader that fails to compile throws at runtime, on the GL thread, the first
time the screen it belongs to is opened — and the only symptom the player gets
is a black screen. The Kotlin compiler cannot see inside a raw string, so
nothing else in the build catches it. This does.

Requires glslangValidator (Debian/Ubuntu: apt install glslang-tools).

    python3 Tools/check_shaders.py            # whole project
    python3 Tools/check_shaders.py path.kt    # one file

Exits non-zero if any shader fails, so it can gate a build.
"""

import os
import re
import subprocess
import sys
import tempfile

# Any `private const val NAME = """..."""` whose body declares a #version.
SHADER_RE = re.compile(r'(?:private )?const val (\w+)\s*=\s*"""(.*?)"""', re.S)

KOTLIN_ROOT = os.path.join("Backrooms", "Source", "Main", "Kotlin")


def stage_of(name: str, body: str) -> str:
    """Vertex shaders write gl_Position and declare no fragment output."""
    if "gl_Position" in body and "out vec4 fragColor" not in body:
        return "vert"
    return "frag"


def check_file(path: str, workdir: str) -> tuple[int, int]:
    with open(path, encoding="utf-8") as fh:
        src = fh.read()

    checked = failed = 0
    for name, body in SHADER_RE.findall(src):
        if "#version" not in body:
            continue
        checked += 1
        stage = stage_of(name, body)
        shader_path = os.path.join(workdir, f"{name}.{stage}")
        with open(shader_path, "w", encoding="utf-8") as fh:
            fh.write(body)

        result = subprocess.run(
            ["glslangValidator", "-S", stage, shader_path],
            capture_output=True, text=True,
        )
        if result.returncode == 0:
            print(f"  ok    {name} ({stage})")
        else:
            failed += 1
            print(f"  FAIL  {name} ({stage})")
            for line in (result.stdout + result.stderr).strip().splitlines():
                print(f"        {line}")
    return checked, failed


def main() -> int:
    if len(sys.argv) > 1:
        targets = sys.argv[1:]
    else:
        targets = []
        for root, _dirs, files in os.walk(KOTLIN_ROOT):
            targets += [os.path.join(root, f) for f in files if f.endswith(".kt")]
        targets.sort()

    if subprocess.run(["which", "glslangValidator"], capture_output=True).returncode != 0:
        print("glslangValidator not found; install glslang-tools", file=sys.stderr)
        return 2

    total = failures = 0
    with tempfile.TemporaryDirectory() as workdir:
        for path in targets:
            checked, failed = check_file(path, workdir)
            if checked:
                print(f"{path}: {checked} shader(s)")
            total += checked
            failures += failed

    print(f"\n{total} shader(s) checked, {failures} failed")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
