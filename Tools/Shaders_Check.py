#!/usr/bin/env python3

import os
import re
import subprocess
import sys
import tempfile

SHADER_RE = re.compile(r'(?:private )?const val (\w+)\s*=\s*"""(.*?)"""', re.S)
PROGRAM_RE = re.compile(r"linkGlProgram\(\s*(\w+)\s*,\s*(\w+)\s*[,)]")

KOTLIN_ROOT = os.path.join("Backrooms", "Source", "Main", "Kotlin")


AGSL_ENTRY_RE = re.compile(r"half4\s+main\s*\(")

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

AGSL_KNOWN = {
    "float", "float2", "float3", "float4", "half", "half2", "half3", "half4",
    "int", "int2", "int3", "int4", "bool", "float2x2", "float3x3", "float4x4",
    "shader", "colorFilter", "blender", "uniform", "const", "return", "if",
    "else", "for", "while", "break", "continue", "in", "out", "inout",
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
    problems: list[str] = []
    code = re.sub(r"//[^\n]*", "", body)
    code = re.sub(r"/\*.*?\*/", lambda m: "\n" * m.group(0).count("\n"), code, flags=re.S)

    for bad, why in AGSL_ABSENT.items():
        for m in re.finditer(r"\b" + re.escape(bad) + r"\b", code):
            line = code.count("\n", 0, m.start()) + 1
            problems.append(f"{name}:{line} uses `{bad}` — {why}")

    declared = set(AGSL_KNOWN)
    for m in re.finditer(r"\(([^)]*)\)\s*\{", code):
        for part in m.group(1).split(","):
            bits = part.strip().split()
            if len(bits) >= 2:
                declared.add(bits[-1])
    for m in AGSL_DECL_RE.finditer(code):
        declared.add(m.group(2))
    for m in re.finditer(r"\b(?:half4|float4|float|half|void)\s+([A-Za-z_]\w*)\s*\(", code):
        declared.add(m.group(1))

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

        if stage == "frag":
            for leak in precision_leaks(body):
                failed += 1
                print(f"  FAIL  {name}: {leak}")

        if name not in paired:
            failed += 1
            print(f"  FAIL  {name} is in no linkGlProgram() call — it compiles "
                  f"but nothing ever runs it")

    bodies = {n: b for n, b in SHADER_RE.findall(src) if "#version" in b}

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


VEC_TYPES = "float|vec2|vec3|vec4"
FUNC_RE = re.compile(
    r"\b(?:float|vec2|vec3|vec4)\s+([A-Za-z_]\w*)\s*\(([^)]*)\)\s*\{")
CALL_RE = re.compile(r"\b([A-Za-z_]\w*)\s*\(")
CONSTRUCTORS = {"vec2", "vec3", "vec4", "float", "int",
                "ivec2", "ivec3", "ivec4", "mat2", "mat3", "mat4"}


def precision_leaks(body: str) -> list[str]:
    """
    Where a world coordinate falls out of highp in a mediump fragment shader.

    A fragment shader has no default float precision, so every one here says
    `precision mediump float;`, and the varyings carrying world position are
    declared highp. That protects the varying and nothing else.

    mediump is a half on most mobile parts — ten mantissa bits. At 500 m from
    the origin its grid is 0.25 m, and the floor's carpet is sampled at 41
    cycles per metre. That is the blockiness testers reported getting worse the
    further they walked, and it doubles with every doubling of distance.

    Only two places actually lose it, because GLSL evaluates an operation at
    the highest precision of its operands — so a local assigned from highp
    arithmetic is computed correctly whatever the local is declared as:

      * A FUNCTION PARAMETER. `vec3 surfaceFloor(vec3 wp)` called with the
        highp varying truncates on the call, before any arithmetic happens.
        That was the bug.
      * A UNIFORM. `uniform vec3 uTorchPos` holds only mediump to begin with,
        so `vWorldPos - uTorchPos` is a large subtraction with a quantised
        operand: at 500 m the torch origin snaps to a 0.25 m grid and the beam
        lands on a wall in steps. That was the other one.

    Nothing in GLSL warns about either. The shader compiles, links, and looks
    perfect for the first hundred metres.
    """
    if not re.search(r"precision\s+mediump\s+float", body):
        return []
    # Every fragment input here is highp — they have to match the vertex stage.
    # Only the ones carrying a WORLD position matter, and the project names
    # those `...World...` or `...Pos`; a UV or a growth fraction is bounded and
    # mediump is right for it. The naming convention is the contract: a world
    # varying called something else is invisible to this, so keep the name.
    world = {n for n in re.findall(
        r"\bin\s+highp\s+(?:" + VEC_TYPES + r")\s+([A-Za-z_]\w*)", body)
        if re.search(r"World|Pos", n)}
    if not world:
        return []
    problems: list[str] = []

    def mentions(expr: str) -> bool:
        return any(re.search(r"\b" + re.escape(w) + r"\b", expr) for w in world)

    def carries_magnitude(expr: str) -> bool:
        """True while the expression is still the coordinate itself: only
        arithmetic, swizzles and vector constructors, no reducing call."""
        return all(m.group(1) in CONSTRUCTORS for m in CALL_RE.finditer(expr))

    params: dict[str, list[tuple[str, str]]] = {}
    for m in FUNC_RE.finditer(body):
        ps = []
        for part in m.group(2).split(","):
            bits = part.strip().split()
            if len(bits) >= 2:
                ps.append((bits[0] if bits[0] in ("highp", "mediump", "lowp") else "",
                           bits[-1]))
        params[m.group(1)] = ps

    for fname, ps in params.items():
        for m in re.finditer(r"\b" + re.escape(fname) + r"\s*\(([^);]*)\)", body):
            for idx, arg in enumerate(a.strip() for a in m.group(1).split(",")):
                if idx < len(ps) and mentions(arg) and carries_magnitude(arg) \
                        and ps[idx][0] != "highp":
                    line = body.count("\n", 0, m.start()) + 1
                    problems.append(
                        f"line {line}: {fname}() takes the world position in "
                        f"argument {idx + 1}, but its parameter `{ps[idx][1]}` is "
                        f"not highp — the value is truncated on the call, before "
                        f"any arithmetic")

    # A uniform used in magnitude arithmetic against a world varying is itself a
    # world position, and must be able to hold one.
    uniforms = dict(re.findall(
        r"\buniform\s+(?:(highp|mediump|lowp)\s+)?(?:" + VEC_TYPES +
        r")\s+([A-Za-z_]\w*)", body))
    for qual, uname in [(q, n) for q, n in
                        re.findall(r"\buniform\s+(?:(highp|mediump|lowp)\s+)?(?:"
                                   + VEC_TYPES + r")\s+([A-Za-z_]\w*)", body)]:
        if qual == "highp":
            continue
        for m in re.finditer(
                r"[^;\n]*\b" + re.escape(uname) + r"\b[^;\n]*", body):
            expr = m.group(0)
            if expr.lstrip().startswith("uniform"):
                continue
            if not mentions(expr):
                continue
            # A direct binary +/- against the world position, not merely both
            # names appearing in the same statement: uTime is added inside a
            # noise argument and is not a position.
            pair = "|".join(re.escape(w) for w in world)
            if not re.search(r"(?:" + pair + r")(?:\.\w+)?\s*[-+]\s*"
                             + re.escape(uname) + r"\b|"
                             + re.escape(uname) + r"(?:\.\w+)?\s*[-+]\s*(?:"
                             + pair + r")\b", expr):
                continue
            line = body.count("\n", 0, m.start()) + 1
            msg = (f"line {line}: `{uname}` is added to or subtracted from the "
                   f"world position but is not a highp uniform — it can only "
                   f"hold a 0.25 m grid at 500 m, so the difference is quantised")
            if msg not in problems:
                problems.append(msg)
    _ = uniforms

    # The sin-hash. `fract(sin(dot(p, k)) * 43758.5453)` is the standard GLSL
    # one-liner and it falls apart on large arguments regardless of precision
    # qualifiers: at 500 m the floor samples it at 41 cycles per metre, so the
    # argument reaches 9e6, where one representable float step is a whole
    # radian — sixteen per cent of a period. The hash stops being a hash and
    # becomes banding. An integer hash on the lattice point is exact at any
    # coordinate and costs the same.
    for m in re.finditer(r"fract\s*\(\s*sin\s*\(", body):
        line = body.count("\n", 0, m.start()) + 1
        problems.append(
            f"line {line}: fract(sin(...)) as a hash — its argument grows with "
            f"the world position and at 500 m one float step is a sixth of a "
            f"radian, so it bands instead of hashing. Hash the integer lattice "
            f"point instead")
    return problems


def _precisions(body: str, stage: str) -> dict[str, dict[str, str]]:
    m = DEFAULT_RE.search(body)
    default = m.group(1) if m else ("highp" if stage == "vert" else "none")
    out: dict[str, dict[str, str]] = {}
    for storage, explicit, typ, name in DECL_RE.findall(body):
        if typ in FLOAT_TYPES:
            out.setdefault(storage, {})[name] = explicit or default
    return out


def precision_gaps(vert_body: str, frag_body: str) -> list[tuple[str, str, str, str]]:
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
