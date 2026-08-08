#!/usr/bin/env python3
"""
Native_Check.py — the C++ side, and its contract with Kotlin.

Three things this catches that nothing else does:

1. THE JNI CONTRACT. A Kotlin `external fun foo()` is bound to a C++
   `Java_com_omni_backrooms_NativeBridge_foo` by NAME, at the moment it is
   first called. Nothing checks that the two agree — not the Kotlin compiler,
   not the C++ compiler, not the linker. A typo, a rename on one side only, or
   an `external fun` nobody ever implemented all build perfectly and then throw
   UnsatisfiedLinkError on a player's device, usually deep into a run.

2. THE CMAKE SOURCE LIST. A .cpp under Native/ that is not in CMakeLists.txt
   compiles nowhere and its symbols are simply absent — which lands as the same
   runtime failure as (1), from a different direction.

3. WARNINGS. The host-compilable modules are built with -Wall -Wextra
   -Wpedantic as errors. Engine.cpp itself needs the NDK (jni.h, android/*,
   aaudio) so it cannot be built here; its JNI surface is still parsed.

    python3 Tools/Native_Check.py
"""
from __future__ import annotations

import glob
import os
import re
import subprocess
import sys
import tempfile

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
NATIVE = os.path.join(REPO, "Backrooms/Source/Main/Native")
KOTLIN = os.path.join(REPO, "Backrooms/Source/Main/Kotlin/com/omni/backrooms")

# Modules with no Android dependency, so they can be compiled on the host.
HOST_MODULES = ["Map/Level_0.cpp", "Frame/Frame.cpp", "Trail/Trail.cpp",
                "Entity/Entity.cpp", "Sound/Synth.cpp", "Ending/Ending.cpp",
                "Shield/Unity.cpp", "Shield/Shield.cpp"]

JNI_PREFIX = "Java_com_omni_backrooms_NativeBridge_"

failures: list[str] = []


def check(ok: bool, what: str) -> None:
    if not ok:
        failures.append(what)


def section(title: str) -> None:
    print(f"\n── {title}")


def jni_exports() -> set[str]:
    """Method names exported from the native side."""
    names = set()
    for path in glob.glob(os.path.join(NATIVE, "**/*.cpp"), recursive=True):
        text = open(path, encoding="utf-8").read()
        for m in re.finditer(re.escape(JNI_PREFIX) + r"([A-Za-z0-9_]+)", text):
            names.add(m.group(1))
    return names


def kotlin_externals() -> set[str]:
    """`external fun` declarations on NativeBridge."""
    src = os.path.join(KOTLIN, "Service.kt")
    if not os.path.exists(src):
        failures.append("Service.kt not found; cannot read the JNI declarations")
        return set()
    text = open(src, encoding="utf-8").read()
    m = re.search(r"class NativeBridge[^{]*\{", text)
    check(m is not None, "no NativeBridge class found")
    if not m:
        return set()
    # Walk to the matching close brace so declarations in other classes are not
    # swept up.
    i, depth = m.end() - 1, 0
    while i < len(text):
        if text[i] == "{":
            depth += 1
        elif text[i] == "}":
            depth -= 1
            if depth == 0:
                break
        i += 1
    body = text[m.end():i]
    return set(re.findall(r"external fun\s+([A-Za-z0-9_]+)\s*\(", body))


# Kotlin type -> the JNI type it arrives as. Only the types this bridge
# actually uses; anything else is reported rather than silently accepted.
KOTLIN_TO_JNI = {
    "Int": "jint", "Long": "jlong", "Float": "jfloat", "Double": "jdouble",
    "Boolean": "jboolean", "Byte": "jbyte", "Short": "jshort", "Char": "jchar",
    "String": "jstring", "String?": "jstring",
    "FloatArray": "jfloatArray", "FloatArray?": "jfloatArray",
    "IntArray": "jintArray", "IntArray?": "jintArray",
    "ByteArray": "jbyteArray", "ByteArray?": "jbyteArray",
    # Anything that crosses as a plain object reference.
    "Bitmap": "jobject", "Any": "jobject", "Any?": "jobject", "Object": "jobject",
}


def _split_params(text: str) -> list[str]:
    """Top-level comma split, so a generic or a default value cannot break it."""
    out, depth, cur = [], 0, ""
    for ch in text:
        if ch in "(<[":
            depth += 1
        elif ch in ")>]":
            depth -= 1
        if ch == "," and depth == 0:
            out.append(cur); cur = ""
        else:
            cur += ch
    if cur.strip():
        out.append(cur)
    return [p.strip() for p in out if p.strip()]


def kotlin_signatures(body: str) -> dict[str, list[str]]:
    """Parameter types of each `external fun`, in order."""
    sigs = {}
    for m in re.finditer(r"external fun\s+([A-Za-z0-9_]+)\s*\(", body):
        i, depth = m.end() - 1, 0
        while i < len(body):
            if body[i] == "(":
                depth += 1
            elif body[i] == ")":
                depth -= 1
                if depth == 0:
                    break
            i += 1
        params = _split_params(body[m.end():i])
        sigs[m.group(1)] = [p.split(":", 1)[1].strip() for p in params if ":" in p]
    return sigs


def native_signatures() -> dict[str, list[str]]:
    """Parameter types of each JNI definition, after JNIEnv* and jobject."""
    sigs = {}
    for path in glob.glob(os.path.join(NATIVE, "**/*.cpp"), recursive=True):
        text = open(path, encoding="utf-8").read()
        for m in re.finditer(re.escape(JNI_PREFIX) + r"([A-Za-z0-9_]+)\s*\(", text):
            i, depth = m.end() - 1, 0
            while i < len(text):
                if text[i] == "(":
                    depth += 1
                elif text[i] == ")":
                    depth -= 1
                    if depth == 0:
                        break
                i += 1
            params = _split_params(text[m.end():i])
            # Drop the two the JVM always supplies.
            params = params[2:]
            types = []
            for p in params:
                p = p.strip()
                # "jfloat x" -> jfloat; a bare "jfloat" (unnamed) -> jfloat
                types.append(p.split()[0] if p.split() else p)
            sigs[m.group(1)] = types
    return sigs


def check_jni_signatures() -> None:
    """
    Arity and types, not just names.

    JNI resolves by name alone when a method is not overloaded, so a native
    function that takes four floats will happily bind to a Kotlin declaration
    that passes eight. Nothing errors. The extra arguments are read off the
    stack as whatever happened to be there, and the symptom is a creature that
    behaves strangely on some devices and correctly on others.

    This is the one contract in the project with no compiler behind it at all,
    which is exactly why it is worth spelling out here.
    """
    section("JNI signatures")
    src = os.path.join(KOTLIN, "Service.kt")
    if not os.path.exists(src):
        return
    text = open(src, encoding="utf-8").read()
    m = re.search(r"class NativeBridge[^{]*\{", text)
    if not m:
        return
    i, depth = m.end() - 1, 0
    while i < len(text):
        if text[i] == "{":
            depth += 1
        elif text[i] == "}":
            depth -= 1
            if depth == 0:
                break
        i += 1
    kt = kotlin_signatures(text[m.end():i])
    cpp = native_signatures()

    checked = 0
    for name, kparams in sorted(kt.items()):
        if name not in cpp:
            continue                        # already reported by the name check
        nparams = cpp[name]
        if len(kparams) != len(nparams):
            failures.append(
                f"{name}: Kotlin passes {len(kparams)} argument(s), native takes "
                f"{len(nparams)} — JNI binds by name and will not catch this")
            continue
        for pos, (k, n) in enumerate(zip(kparams, nparams)):
            want = KOTLIN_TO_JNI.get(k)
            if want is None:
                failures.append(f"{name}: argument {pos + 1} has Kotlin type "
                                f"'{k}', which this check does not know")
            elif want != n:
                failures.append(f"{name}: argument {pos + 1} is {k} in Kotlin "
                                f"but {n} in native (expected {want})")
        checked += 1
    print(f"   {checked} signature(s) compared, "
          f"{sum(1 for f in failures if f.split(':')[0] in kt)} mismatch(es)")


def check_jni_contract() -> None:
    section("JNI contract")
    exported = jni_exports()
    declared = kotlin_externals()

    missing = sorted(declared - exported)
    for name in missing:
        failures.append(
            f"external fun {name}() has no {JNI_PREFIX}{name} on the native side "
            f"— this throws UnsatisfiedLinkError the first time it is called")

    # The reverse is not an error: native may legitimately export more than the
    # current Kotlin uses. Report it so a dead export gets noticed.
    unused = sorted(exported - declared)

    print(f"   {len(declared)} external fun, {len(exported)} JNI exports, "
          f"{len(missing)} unbound, {len(unused)} native-only")
    for name in unused:
        print(f"     native-only: {name}")


def check_cmake_sources() -> None:
    section("CMake source list")
    cml = os.path.join(NATIVE, "CMakeLists.txt")
    check(os.path.exists(cml), "CMakeLists.txt not found")
    if not os.path.exists(cml):
        return
    text = open(cml, encoding="utf-8").read()
    on_disk = {
        os.path.relpath(p, NATIVE).replace(os.sep, "/")
        for p in glob.glob(os.path.join(NATIVE, "**/*.cpp"), recursive=True)
    }
    listed = {s for s in on_disk if re.search(r"^\s*" + re.escape(s) + r"\s*$", text, re.M)}
    for missing in sorted(on_disk - listed):
        failures.append(
            f"{missing} is not in CMakeLists.txt — it compiles nowhere and its "
            f"symbols will be absent at runtime")
    print(f"   {len(on_disk)} source(s) on disk, {len(listed)} listed")


def check_host_build() -> None:
    section("Host build (-Wall -Wextra -Wpedantic as errors)")
    with tempfile.TemporaryDirectory() as tmp:
        for rel in HOST_MODULES:
            src = os.path.join(NATIVE, rel)
            if not os.path.exists(src):
                failures.append(f"{rel} listed as a host module but not on disk")
                continue
            obj = os.path.join(tmp, rel.replace("/", "_") + ".o")
            r = subprocess.run(
                ["g++", "-std=c++20", "-O2", "-c", "-Wall", "-Wextra", "-Wpedantic",
                 "-Werror", "-I", NATIVE, src, "-o", obj],
                capture_output=True, text=True)
            if r.returncode != 0:
                failures.append(f"{rel} does not build clean:\n{r.stderr[:1500]}")
            else:
                print(f"   {rel:24s} ok")


ENDING_PROBE = r"""
// Samples the real transition across its whole length and prints it, so the
// curves can be asserted instead of judged by dying on a phone.
#include "Ending/Ending.h"
#include <cstdio>

int main() {
    using namespace omni::ending;
    for (int k = 1; k <= 2; ++k) {
        const Kind kind = static_cast<Kind>(k);
        const float dur = duration(kind);
        std::printf("KIND %d %f\n", k, dur);
        for (int i = 0; i <= 100; ++i) {
            const float t = dur * float(i) / 100.0f;
            const Params p = evaluate(kind, t);
            std::printf("%d %f %f %f %f %f %f %f %f %f\n", k, t,
                        p.desaturate, p.vignette, p.aberration, p.tear,
                        p.pull, p.bloom, p.exposure, p.panel);
        }
        // Past the end: a caller that keeps sampling must get the settled
        // state, not something that has run off.
        const Params over = evaluate(kind, dur * 4.0f);
        std::printf("OVER %d %f %f %f\n", k, over.vignette, over.exposure, over.panel);
    }
    return 0;
}
"""


def check_ending() -> None:
    """
    The run-over transition, measured.

    The end of a run used to be a Compose card on a black scrim: the level you
    had just been standing in painted over at 88% black, with a rounded
    rectangle on top of it. That reads as a dialog, and a dialog is what you
    dismiss. It is also the one piece of the game that is hardest to look at —
    you have to die to see it — so it is exactly the piece that needs a check
    rather than an opinion.

    Ending::evaluate is a pure function of (kind, seconds), which is what makes
    that possible at all. Everything below is a property the transition has to
    have for it to read as one event rather than a set of effects switching on.
    """
    section("Ending")
    with tempfile.TemporaryDirectory() as tmp:
        src = os.path.join(tmp, "ending_probe.cpp")
        exe = os.path.join(tmp, "ending_probe")
        with open(src, "w", encoding="utf-8") as f:
            f.write(ENDING_PROBE)
        build = subprocess.run(
            ["g++", "-std=c++20", "-O2", "-I", NATIVE, src,
             os.path.join(NATIVE, "Ending/Ending.cpp"), "-o", exe],
            capture_output=True, text=True)
        if build.returncode != 0:
            failures.append("the ending probe did not compile:\n" + build.stderr[:2000])
            return
        out = subprocess.run([exe], capture_output=True, text=True).stdout

    NAMES = ["desaturate", "vignette", "aberration", "tear",
             "pull", "bloom", "exposure", "panel"]
    curves: dict[int, list[list[float]]] = {1: [], 2: []}
    durations: dict[int, float] = {}
    settled: dict[int, tuple[float, float, float]] = {}
    for line in out.splitlines():
        parts = line.split()
        if parts[0] == "KIND":
            durations[int(parts[1])] = float(parts[2])
        elif parts[0] == "OVER":
            settled[int(parts[1])] = (float(parts[2]), float(parts[3]), float(parts[4]))
        else:
            curves[int(parts[0])].append([float(v) for v in parts[1:]])

    for kind, label in ((1, "death"), (2, "escape")):
        rows = curves[kind]
        check(len(rows) == 101, f"the {label} transition did not sample")
        if len(rows) != 101:
            continue
        dur = durations[kind]
        # Long enough to register, short enough not to be a wait.
        check(1.2 <= dur <= 3.5,
              f"the {label} transition runs {dur:.2f}s — under 1.2 it is a cut "
              f"and over 3.5 it is a delay between the player and their score")

        first = rows[0][1:]
        # The first frame of an ending must be the frame before it. Anything
        # non-zero here is a state the eye reads as a cut.
        for i, name in enumerate(NAMES):
            want = 1.0 if name == "exposure" else 0.0
            check(abs(first[i] - want) < 1e-6,
                  f"the {label} transition starts with {name} at {first[i]:.3f} "
                  f"rather than {want:.0f} — its first frame is a jump")

        panel = [r[8] for r in rows]
        check(all(b >= a - 1e-6 for a, b in zip(panel, panel[1:])),
              f"the {label} panel does not rise monotonically — it appears, "
              f"retreats and comes back")
        check(panel[-1] > 0.999, f"the {label} panel never fully arrives")
        # The picture has to fail before the stats cover it. If the panel is up
        # while the frame is still intact, the transition is happening behind a
        # card and nobody sees it.
        half = next(i for i, v in enumerate(panel) if v > 0.5)
        check(half >= 55,
              f"the {label} panel is half up {half}% into the transition, before "
              f"the frame behind it has finished changing")

        for i, name in enumerate(NAMES):
            col = [r[i + 1] for r in rows]
            check(all(v == v and abs(v) < 100.0 for v in col),
                  f"{label}.{name} runs away or goes non-finite")
        print(f"   {label:7s} {dur:.2f}s  panel half-up at {half}%  "
              f"peak vignette {max(r[2] for r in rows):.2f}  "
              f"peak bloom {max(r[6] for r in rows):.2f}  "
              f"exposure {min(r[7] for r in rows):.2f}..{max(r[7] for r in rows):.2f}")

        v, e, pn = settled[kind]
        check(abs(pn - panel[-1]) < 1e-6 and abs(v - rows[-1][2]) < 1e-6,
              f"sampling past the end of the {label} transition does not hold "
              f"the settled state")

    # The two endings must not be the same effect with a different tint. This is
    # the failure mode a card on a scrim has by construction, and the one thing
    # a reader of the code would not notice.
    d, s = curves[1], curves[2]
    if len(d) == 101 and len(s) == 101:
        diff = max(abs(a[i + 1] - b[i + 1]) for a, b in zip(d, s) for i in range(8))
        peak_d = max(r[7] for r in d)      # death exposure never lifts
        peak_s = max(r[7] for r in s)      # escape exposure does
        print(f"   death vs escape: largest divergence {diff:.2f}, "
              f"peak exposure {peak_d:.2f} vs {peak_s:.2f}")
        check(diff > 0.4, "the two endings are the same curve — one of them is "
                          "the other with a different colour")
        check(peak_d <= 1.0 < peak_s,
              "a death should never brighten and an escape should — these two "
              "move the exposure the same way")


def main() -> int:
    check_jni_contract()
    check_jni_signatures()
    check_cmake_sources()
    check_host_build()
    check_ending()

    print()
    for f in failures:
        print("FAIL", f)
    print("PASSED" if not failures else
          f"FAILED ({len(failures)} failure{'' if len(failures) == 1 else 's'})")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
