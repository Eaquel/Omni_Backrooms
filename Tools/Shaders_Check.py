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

# ===========================================================================
# AGSL
#
# A RuntimeShader is a shader in a Kotlin raw string exactly like the GL ones,
# with one difference that matters more than all the similarities: it is
# compiled by the RuntimeShader *constructor*, on the main thread, inside
# composition. A source error is not a black screen, it is
# IllegalArgumentException on the first frame of the lobby — the app does not
# start.
#
# This tool was written for GLSL and skipped AGSL entirely, because the shader
# scan required a `#version` line and AGSL has none. So the one shader in the
# project whose failure mode is a crash was the one shader nothing checked, and
# `fwidth(d)` shipped. AGSL has no derivative functions at all.
#
# There is no SkSL compiler on a build machine, so this cannot compile them. It
# does the two things that catch the mistakes actually made: it rejects GLSL
# builtins AGSL does not have, and it requires every identifier to be declared
# before it is used, which is what turns one bad line into six errors.
# ===========================================================================

AGSL_ENTRY_RE = re.compile(r"half4\s+main\s*\(")

# GLSL/ES builtins with no AGSL equivalent. Each one is a hard compile error in
# the RuntimeShader constructor, which is a crash rather than a missing effect.
AGSL_ABSENT = {
    "fwidth":    "AGSL has no derivative functions; scale from the `size` uniform instead",
    "dFdx":      "AGSL has no derivative functions",
    "dFdy":      "AGSL has no derivative functions",
    "texture":   "an AGSL shader samples through an `uniform shader` with .eval(coord)",
    "texture2D": "an AGSL shader samples through an `uniform shader` with .eval(coord)",
    "textureLod": "AGSL has no explicit-LOD sampling",
    "discard":   "AGSL has no discard; return a transparent half4 instead",
    "gl_FragCoord":  "AGSL passes the coordinate into main() as its parameter",
    "gl_FragColor":  "AGSL returns its colour from main()",
    "gl_Position":   "AGSL has no vertex stage",
    "atan2":     "AGSL spells it atan(y, x)",
}

# Everything AGSL does provide, plus the types, so an unknown name is really
# unknown rather than merely unlisted.
AGSL_KNOWN = {
    # types and constructors
    "float", "float2", "float3", "float4", "half", "half2", "half3", "half4",
    "int", "int2", "int3", "int4", "bool", "float2x2", "float3x3", "float4x4",
    "shader", "colorFilter", "blender", "uniform", "const", "return", "if",
    "else", "for", "while", "break", "continue", "in", "out", "inout",
    # builtins
    "abs", "acos", "all", "any", "asin", "atan", "ceil", "clamp", "cos",
    "cross", "degrees", "distance", "dot", "eval", "exp", "exp2", "faceforward",
    "floor", "fract", "inversesqrt", "length", "log", "log2", "max", "min",
    "mix", "mod", "normalize", "pow", "radians", "reflect", "refract", "sample",
    "saturate", "sign", "sin", "smoothstep", "sqrt", "step", "tan", "unpremul",
    "toLinearSrgb", "fromLinearSrgb", "not", "equal", "notEqual", "lessThan",
    "lessThanEqual", "greaterThan", "greaterThanEqual",
}

AGSL_DECL_RE = re.compile(
    r"\b(?:uniform\s+)?(?:const\s+)?"
    r"(float|float2|float3|float4|half|half2|half3|half4|int|int2|int3|int4|"
    r"bool|float2x2|float3x3|float4x4|shader|colorFilter|blender)\s+"
    r"([A-Za-z_]\w*)")
AGSL_IDENT_RE = re.compile(r"\b([A-Za-z_]\w*)\b")


def check_agsl(name: str, body: str) -> list[str]:
    """Everything about an AGSL shader that can be decided without SkSL."""
    problems: list[str] = []
    # Comments out first, and only the text — the newlines stay, so every line
    # number below is still the line number in the shader. A comment explaining
    # why fwidth cannot be used is not a use of fwidth, and the first version of
    # this check reported the explanation.
    code = re.sub(r"//[^\n]*", "", body)
    code = re.sub(r"/\*.*?\*/", lambda m: "\n" * m.group(0).count("\n"), code, flags=re.S)

    for bad, why in AGSL_ABSENT.items():
        for m in re.finditer(r"\b" + re.escape(bad) + r"\b", code):
            line = code.count("\n", 0, m.start()) + 1
            problems.append(f"{name}:{line} uses `{bad}` — {why}")

    # Declared before used. Function parameters and declarations both count;
    # anything else that is not a builtin, a number or a field access is a name
    # the compiler will not know either.
    declared = set(AGSL_KNOWN)
    for m in re.finditer(r"\(([^)]*)\)\s*\{", code):     # parameter lists
        for part in m.group(1).split(","):
            bits = part.strip().split()
            if len(bits) >= 2:
                declared.add(bits[-1])
    for m in AGSL_DECL_RE.finditer(code):
        declared.add(m.group(2))
    for m in re.finditer(r"\b(?:half4|float4|float|half|void)\s+([A-Za-z_]\w*)\s*\(", code):
        declared.add(m.group(1))

    # A field or swizzle follows a dot and is not a free identifier.
    stripped = re.sub(r"\.\w+", "", code)
    for m in AGSL_IDENT_RE.finditer(stripped):
        ident = m.group(1)
        if ident in declared or ident.isdigit():
            continue
        line = stripped.count("\n", 0, m.start()) + 1
        problems.append(
            f"{name}:{line} uses `{ident}`, which is not declared and is not an "
            f"AGSL builtin — RuntimeShader throws on this, in composition, which "
            f"is a crash and not a missing effect")
    return problems


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
        # AGSL: no #version line, compiled at runtime by RuntimeShader, and a
        # failure there is a crash rather than a black screen.
        if "#version" not in body and AGSL_ENTRY_RE.search(body):
            checked += 1
            agsl = check_agsl(name, body)
            if not agsl:
                print(f"  ok    {name} (agsl)")
            else:
                failed += len(agsl)
                print(f"  FAIL  {name} (agsl)")
                for p in agsl:
                    print(f"        {p}")
            continue
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
