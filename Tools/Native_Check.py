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


SHIELD_PROBE = r"""
// Runs the guard's detectors against a /proc we lay out ourselves.
//
// The point of going through RootDetector::scan() rather than calling the
// parsing functions directly: a detector is free to stop calling them. The
// first version of this probe did call them directly, and it passed with every
// original bug put back.
#include "Shield/Shield.h"

#include <cstdio>
#include <filesystem>
#include <fstream>
#include <string>

using namespace omni::shield;
namespace fs = std::filesystem;

// A stock, locked-bootloader user build. The only "overlay" is the RRO mount
// that ships on the hardware, and /system_ext is on every Android 11+ device.
static constexpr const char* kStock =
    "/dev/block/dm-4 / ext4 ro,seclabel,relatime 0 0\n"
    "/dev/block/dm-5 /system_ext ext4 ro,seclabel,relatime 0 0\n"
    "/dev/block/dm-6 /vendor ext4 ro,seclabel,relatime 0 0\n"
    "/dev/block/dm-7 /product ext4 ro,seclabel,relatime 0 0\n"
    "tmpfs /apex tmpfs ro,seclabel,relatime 0 0\n"
    "overlay /vendor/overlay overlay ro,seclabel,lowerdir=/vendor 0 0\n"
    "/dev/block/by-name/userdata /data f2fs rw,seclabel 0 0\n";

// An overlay whose target really is the system image.
static constexpr const char* kOverlaidSystem =
    "/dev/block/dm-4 / ext4 ro,seclabel,relatime 0 0\n"
    "overlay /system overlay rw,seclabel,upperdir=/mnt/scratch/overlay 0 0\n"
    "/dev/block/by-name/userdata /data f2fs rw,seclabel 0 0\n";

// A root manager naming itself.
static constexpr const char* kMagiskMounts =
    "/dev/block/dm-4 / ext4 ro,seclabel,relatime 0 0\n"
    "magisk /sbin tmpfs rw,seclabel,mode=755 0 0\n";

// A socket table with no Frida in it. 69A2 — port 27042 — appears twice as a
// decoy: inside an inode number and inside a remote address. A substring
// search takes both.
static constexpr const char* kCleanTcp =
    "  sl  local_address rem_address   st tx_queue rx_queue tr tm->when retrnsmt   uid  timeout inode\n"
    "   0: 0100007F:1F90 00000000:0000 0A 00000000:00000000 00:00000000 00000000  1000  0 4269A2 1 0000000000000000 100 0 0 10 0\n"
    "   1: 020200C0:BF26 A56069A2:01BB 01 00000000:00000000 02:00000040 00000000  1000  0 2357   2 0000000039f06d0f 20 4 30 281 -1\n"
    "   2: 00000000:1BB8 00000000:0000 0A 00000000:00000000 00:00000000 00000000     0  0 1969   1 0000000038ff7476 100 0 0 10 0\n";

// frida-server actually listening on 27042.
static constexpr const char* kFridaTcp =
    "  sl  local_address rem_address   st tx_queue rx_queue tr tm->when retrnsmt   uid  timeout inode\n"
    "   1: 00000000:69A2 00000000:0000 0A 00000000:00000000 00:00000000 00000000     0  0 1969   1 0000000038ff7476 100 0 0 10 0\n";

// An outbound connection to someone else's Frida. Not this process being
// instrumented, and not a server running here.
static constexpr const char* kConnectedTcp =
    "  sl  local_address rem_address   st tx_queue rx_queue tr tm->when retrnsmt   uid  timeout inode\n"
    "   0: 0100007F:69A2 0A684FA0:01BB 01 00000000:00000000 02:00000040 00000000  1000  0 2357 2 00000000 20 4 30 281 -1\n";

static constexpr const char* kCleanStatus =
    "Name:\tomni.backrooms\nState:\tS (sleeping)\nTgid:\t9001\nTracerPid:\t0\n";
static constexpr const char* kTracedStatus =
    "Name:\tomni.backrooms\nState:\tS (sleeping)\nTgid:\t9001\nTracerPid:\t3241\n";
static constexpr const char* kHeldStatus =
    "Name:\tomni.backrooms\nState:\tt (tracing stop)\nTgid:\t9001\nTracerPid:\t3241\n";

static void put(const fs::path& p, const char* body) {
    fs::create_directories(p.parent_path());
    std::ofstream(p) << body;
}

/** Lays out a /proc the detectors will read, and points them at it. */
static void stage(const fs::path& root, const char* mounts, const char* tcp, const char* status) {
    fs::remove_all(root);
    put(root / "self" / "mounts", mounts);
    put(root / "self" / "status", status);
    put(root / "self" / "maps",   "7f8a00000000-7f8a00021000 r-xp 00000000 fd:00 128  /apex/com.android.runtime/lib64/bionic/libc.so\n");
    put(root / "net"  / "tcp",    tcp);
    put(root / "net"  / "tcp6",   "  sl  local_address rem_address   st\n");
    put(root / "cpuinfo",         "Processor\t: AArch64 Processor rev 1 (aarch64)\n");
    fs::create_directories(root / "self" / "task");
    procRoot() = root.string();
}

static void say(const char* name, bool v) { std::printf("%s %d\n", name, v ? 1 : 0); }

// Scanning must not change what the next scan sees.
//
// This runs in its own process, on the real /proc, and nothing else may run
// first. The bug it exists for was a detector mutating the process it was
// inspecting — PTRACE_TRACEME with an undo that could not work — and once the
// mutation has happened the readings are steady again. Sharing a process with
// the staged tests hid it: they tripped the mutation, and by the time this ran
// there was nothing left to observe.
static int stability() {
    DebugDetector debug;
    RootDetector  root;
    const uint32_t d0 = debug.scan(), r0 = root.scan();
    bool stable = true;
    for (int i = 0; i < 4; ++i)
        if (debug.scan() != d0 || root.scan() != r0) stable = false;
    std::printf("STABLE %d 0x%x\n", stable ? 1 : 0, d0);
    return 0;
}

int main(int argc, char** argv) {
    const std::string mode = argc > 1 ? argv[1] : "staged";
    if (mode == "stable") return stability();

    const fs::path base = argc > 2 ? fs::path(argv[2]) : fs::temp_directory_path() / "omni_shield";
    const fs::path root = base / "proc";

    // Root, through RootDetector::scan().
    stage(root, kStock, kCleanTcp, kCleanStatus);
    RootDetector rd;
    say("MOUNT_STOCK", (rd.scan() & FLAG_SHADOW_MOUNT) != 0);

    stage(root, kOverlaidSystem, kCleanTcp, kCleanStatus);
    const bool sysHit = (rd.scan() & FLAG_SHADOW_MOUNT) != 0;
    say("MOUNT_SYSTEM", sysHit);
    std::printf("WHY_MOUNT %s\n", rd.why().empty() ? "-" : "yes");

    stage(root, kMagiskMounts, kCleanTcp, kCleanStatus);
    say("MOUNT_MAGISK", (rd.scan() & FLAG_SHADOW_MOUNT) != 0);

    stage(root, "", kCleanTcp, kCleanStatus);
    say("MOUNT_EMPTY", (rd.scan() & FLAG_SHADOW_MOUNT) != 0);
    // Evidence must not outlive the verdict it belongs to.
    std::printf("WHY_CLEARED %s\n", rd.why().empty() ? "yes" : "-");

    // Frida, through FridaDetector::scan().
    FridaDetector fd;
    stage(root, kStock, kCleanTcp, kCleanStatus);
    say("TCP_CLEAN", (fd.scan() & FLAG_FRIDA_PORT) != 0);
    stage(root, kStock, kFridaTcp, kCleanStatus);
    say("TCP_FRIDA", (fd.scan() & FLAG_FRIDA_PORT) != 0);
    stage(root, kStock, kConnectedTcp, kCleanStatus);
    say("TCP_CONNECTED", (fd.scan() & FLAG_FRIDA_PORT) != 0);

    // Debug, through DebugDetector::scan().
    DebugDetector dd;
    stage(root, kStock, kCleanTcp, kCleanStatus);
    uint32_t clean = dd.scan();
    say("DBG_CLEAN_TRACED", (clean & FLAG_PTRACE_TRACED) != 0);
    say("DBG_CLEAN_WAIT",   (clean & FLAG_DEBUG_WAIT)    != 0);
    stage(root, kStock, kCleanTcp, kTracedStatus);
    uint32_t traced = dd.scan();
    say("DBG_TRACED",      (traced & FLAG_PTRACE_TRACED) != 0);
    say("DBG_TRACED_WAIT", (traced & FLAG_DEBUG_WAIT)    != 0);
    stage(root, kStock, kCleanTcp, kHeldStatus);
    say("DBG_HELD", (dd.scan() & FLAG_DEBUG_WAIT) != 0);

    fs::remove_all(base);
    return 0;
}
"""


def check_shield() -> None:
    """
    The guard's detectors, run against inputs instead of against opinion.

    A player photographed the in-game security dialog: `reason: root,
    flags=0x40200`. Decoded, that is FLAG_PTRACE_TRACED and FLAG_SHADOW_MOUNT
    with every genuine root bit — ROOT_BINARY, ROOT_PROPS, ROOT_PATHS, MAGISK,
    ZYGISK, KSU, SELINUX_OFF — clear. It was not a rooted device. It was two of
    our own checks, and a third one sitting next to them that was worse:

      * SHADOW_MOUNT asked `containsCI(m,"overlay") && containsCI(m,"/system")`
        over the whole mount table. Two independent searches, so an overlay
        over /vendor/overlay and the /system_ext line — both present on stock
        retail hardware — combined into "root", which is HIGH, which is the
        dialog.

      * PTRACE_TRACED called PTRACE_TRACEME and tried to undo it with
        PTRACE_DETACH on pid 0, which cannot work. The process stayed traced,
        so every scan after the first reported a debugger.

      * FRIDA_PORT searched /proc/net/tcp for the literals "6D58", "71D4",
        "2717" and "5039" — not Frida's ports, and matched as substrings
        against a file that is nothing but hex. Frida is CRITICAL, and CRITICAL
        calls killProcess. That one was a coin toss on ending a player's run.

    Everything here is a property those checks must have. The Shield module
    compiles on a host, which is what makes asking possible at all.
    """
    section("Shield")
    with tempfile.TemporaryDirectory() as tmp:
        src = os.path.join(tmp, "shield_probe.cpp")
        exe = os.path.join(tmp, "shield_probe")
        with open(src, "w", encoding="utf-8") as f:
            f.write(SHIELD_PROBE)
        build = subprocess.run(
            ["g++", "-std=c++20", "-O2", "-I", NATIVE, src,
             os.path.join(NATIVE, "Shield/Shield.cpp"), "-o", exe],
            capture_output=True, text=True)
        if build.returncode != 0:
            failures.append("the shield probe did not compile:\n" + build.stderr[:2000])
            return
        stdout = ""
        # Two runs, because the stability check needs a process nothing has
        # scanned in yet — see the comment above stability() in the probe.
        for mode in ("staged", "stable"):
            run = subprocess.run([exe, mode, os.path.join(tmp, "stage")],
                                 capture_output=True, text=True)
            if run.returncode != 0:
                failures.append(f"the shield probe exited {run.returncode} in {mode} mode")
                return
            stdout += run.stdout
        out = dict(line.split(None, 1) for line in stdout.splitlines() if line.strip())

    def flag(key: str) -> bool:
        return out.get(key, "").split()[0] == "1"

    # Mount tables. The first of these is the bug a player photographed.
    check(not flag("MOUNT_STOCK"),
          "a stock mount table with a /vendor/overlay RRO mount is read as root "
          "— this is the SHADOW_MOUNT false positive that put 'root' in front of "
          "a player with an unmodified phone")
    check(not flag("MOUNT_EMPTY"),
          "an unreadable mount table is read as root, so a device that denies us "
          "/proc/self/mounts is accused rather than left alone")
    check(flag("MOUNT_SYSTEM"),
          "an overlay mounted directly over /system is not detected — the check "
          "has been narrowed past the thing it exists for")
    check(flag("MOUNT_MAGISK"),
          "a mount whose source is literally 'magisk' is not detected")
    check(out.get("WHY_MOUNT") == "yes",
          "a mount verdict carries no evidence — the flag word alone is what "
          "made the last false positive take a morning to decode")
    check(out.get("WHY_CLEARED") == "yes",
          "evidence from an earlier scan survives into a clean one, so the "
          "report names a mount the device no longer has")

    # Socket tables.
    check(not flag("TCP_CLEAN"),
          "a socket table containing 69A2 inside an inode number and inside a "
          "remote address is read as Frida — and Frida is CRITICAL, which kills "
          "the process, so this false positive ends a player's run")
    check(flag("TCP_FRIDA"),
          "frida-server listening on 27042 is not detected — the port scan is "
          "looking for the wrong numbers")
    check(not flag("TCP_CONNECTED"),
          "an established connection on port 27042 is read as Frida listening; "
          "only a socket in state 0A (LISTEN) means a server is running here")

    # The debugger flags, and the fact that they mean two different things.
    check(not flag("DBG_CLEAN_TRACED") and not flag("DBG_CLEAN_WAIT"),
          "an untraced process with TracerPid 0 is reported as debugged")
    check(flag("DBG_TRACED"),
          "a real TracerPid is not read as a debugger being attached")
    check(not flag("DBG_TRACED_WAIT"),
          "merely being attached to is reported as being held at a breakpoint, "
          "so PTRACE_TRACED and DEBUG_WAIT carry the same meaning and one of "
          "them is noise")
    check(flag("DBG_HELD"),
          "a process parked in tracing-stop is not detected")

    # The one that has to hold on this machine rather than on staged text.
    stable = out.get("STABLE", "0 0x0").split()
    print(f"   detectors stable across 5 scans: {stable[0] == '1'} (debug flags {stable[1]})")
    check(stable[0] == "1",
          "scanning changes what the next scan sees. A detector that probes by "
          "mutating the process reports something different the second time, and "
          "since the flag word is OR-ed for the life of the run, that second "
          "answer is permanent")


def main() -> int:
    check_jni_contract()
    check_jni_signatures()
    check_cmake_sources()
    check_host_build()
    check_ending()
    check_shield()

    print()
    for f in failures:
        print("FAIL", f)
    print("PASSED" if not failures else
          f"FAILED ({len(failures)} failure{'' if len(failures) == 1 else 's'})")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
