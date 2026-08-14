#!/usr/bin/env python3
"""
Compiles and LINKS every GLSL shader embedded in the Kotlin sources.

A shader that fails to compile throws at runtime, on the GL thread, the first
time the screen it belongs to is opened — and the only symptom the player gets
is a black screen. The Kotlin compiler cannot see inside a raw string, so
nothing else in the build catches it. This does.

Compiling is only half of it. A vertex and a fragment shader can each be
perfectly valid and still refuse to form a program: uniforms of the same name
must agree across stages in type AND precision. A vertex shader defaults float
to highp; a fragment shader has no default at all and every one here declares
`precision mediump float;`. So a float uniform read by both stages and left
bare is a precision mismatch by construction. Lenient drivers link it anyway,
which is why it can ship — a Galaxy S23 running the release build got

    Omni program link failed: Error: Uniform uGrowth precision mismatch with
    other stage.

and a black rectangle where the lobby's vines belong. Both halves passed this
tool. So the pairs named in `linkGlProgram(V, F)` are now linked here too.

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
# The programs the game actually builds: linkGlProgram(VERTEX, FRAGMENT).
PROGRAM_RE = re.compile(r"linkGlProgram\(\s*(\w+)\s*,\s*(\w+)\s*[,)]")

KOTLIN_ROOT = os.path.join("Backrooms", "Source", "Main", "Kotlin")


def stage_of(name: str, body: str, programs: set[tuple[str, str]]) -> str:
    """
    Which stage a shader is.

    How the game uses it beats any guess made from its text: the first argument
    of linkGlProgram is a vertex shader and the second is a fragment shader, by
    definition. The heuristic below is the fallback for a shader that is in no
    program at all — and that is itself worth knowing, so it is reported.
    """
    for vert, frag in programs:
        if name == vert:
            return "vert"
        if name == frag:
            return "frag"
    if "gl_Position" in body and "out vec4 fragColor" not in body:
        return "vert"
    return "frag"


def check_file(path: str, workdir: str) -> tuple[int, int]:
    with open(path, encoding="utf-8") as fh:
        src = fh.read()

    programs = set(PROGRAM_RE.findall(src))
    paired = {n for pair in programs for n in pair}

    checked = failed = 0
    written: dict[str, str] = {}
    for name, body in SHADER_RE.findall(src):
        if "#version" not in body:
            continue
        checked += 1
        stage = stage_of(name, body, programs)
        shader_path = os.path.join(workdir, f"{name}.{stage}")
        with open(shader_path, "w", encoding="utf-8") as fh:
            fh.write(body)
        written[name] = shader_path

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

        # A shader in no program is either dead weight in the APK or a program
        # somebody forgot to build. Neither is visible from the Kotlin side.
        if name not in paired:
            failed += 1
            print(f"  FAIL  {name} is in no linkGlProgram() call — it compiles "
                  f"but nothing ever runs it")

    bodies = {n: b for n, b in SHADER_RE.findall(src) if "#version" in b}

    # And now the half that separate compilation cannot see.
    for vert, frag in sorted(programs):
        missing = [n for n in (vert, frag) if n not in written]
        if missing:
            failed += 1
            print(f"  FAIL  program {vert} + {frag}: no shader source for "
                  f"{', '.join(missing)}")
            continue
        result = subprocess.run(
            ["glslangValidator", "-l", written[vert], written[frag]],
            capture_output=True, text=True,
        )
        if result.returncode == 0:
            print(f"  ok    {vert} + {frag} (links)")
        else:
            failed += 1
            print(f"  FAIL  {vert} + {frag} does not link")
            for line in (result.stdout + result.stderr).strip().splitlines():
                # glslang echoes each input filename; that is not a diagnostic.
                if line.strip() and not line.strip().endswith((".vert", ".frag")):
                    print(f"        {line.strip()}")

        for kind, name, vp, fp in precision_gaps(bodies[vert], bodies[frag]):
            failed += 1
            print(f"  FAIL  {vert} + {frag}: {kind} {name} is {vp} in the vertex "
                  f"stage and {fp} in the fragment stage")
    return checked, failed


FLOAT_TYPES = {"float", "vec2", "vec3", "vec4", "mat2", "mat3", "mat4"}
DECL_RE = re.compile(
    r"^\s*(?:layout\s*\([^)]*\)\s*)?(uniform|in|out)\s+"
    r"(?:(highp|mediump|lowp)\s+)?([A-Za-z0-9_]+)\s+([A-Za-z_]\w*)", re.M)
DEFAULT_RE = re.compile(r"precision\s+(highp|mediump|lowp)\s+float\s*;")


def _precisions(body: str, stage: str) -> dict[str, dict[str, str]]:
    """Effective precision of every float-typed declaration in one stage."""
    m = DEFAULT_RE.search(body)
    # GLSL ES 3.00: the vertex stage defaults float to highp; the fragment
    # stage has no default at all, which is why every fragment shader must
    # declare one — and why the two stages disagree unless someone says so.
    default = m.group(1) if m else ("highp" if stage == "vert" else "none")
    out: dict[str, dict[str, str]] = {}
    for storage, explicit, typ, name in DECL_RE.findall(body):
        if typ in FLOAT_TYPES:
            out.setdefault(storage, {})[name] = explicit or default
    return out


def precision_gaps(vert_body: str, frag_body: str) -> list[tuple[str, str, str, str]]:
    """
    Cross-stage precision disagreements, for uniforms and for varyings.

    glslang rejects the uniform case and accepts the varying one, which is what
    the ES 3.00 spec says: uniforms must match, varyings need not. Drivers do
    not all agree. The two field failures this exists for were

        Error: Uniform uGrowth precision mismatch with other stage.   (Adreno)
        L0001 The fragment floating-point variable uGrowth does not
        match the vertex variable uGrowth. The precision does not match. (Mali)

    and the second message does not distinguish a uniform from a varying at
    all. Matching both costs a keyword per declaration and removes the whole
    class, so this is stricter than the spec on purpose.
    """
    vp, fp = _precisions(vert_body, "vert"), _precisions(frag_body, "frag")
    gaps = []
    for name, p in sorted(vp.get("uniform", {}).items()):
        q = fp.get("uniform", {}).get(name)
        if q is not None and q != p:
            gaps.append(("uniform", name, p, q))
    for name, p in sorted(vp.get("out", {}).items()):
        q = fp.get("in", {}).get(name)
        if q is not None and q != p:
            gaps.append(("varying", name, p, q))
    return gaps


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
