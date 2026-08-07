#!/usr/bin/env python3
"""
Code_To_Sound.py — the sound design, as code.

This game ships no audio files. Every sound is synthesised on the device by the
native audio engine, which keeps the APK small, lets a sound respond
continuously to game state instead of being a fixed clip, and means a footstep
on carpet in a dead hall can differ from the same footstep under a working tube
without anyone authoring two recordings.

The catch is that code you cannot hear is code nobody checks. This tool is the
other half of that arrangement: the same generators the engine uses, written
once here, rendered to WAV so they can actually be listened to, and asserted on
so a bad edit is caught rather than shipped.

The generators below used to be ports of what Engine.cpp does, which left the
obvious hole: nothing asserted the port and the original agreed. They could
drift apart indefinitely and this tool would keep reporting that a sound nobody
had ever heard was fine.

They now live in Native/Sound/Synth.cpp, a module with no Android dependency.
The Python here is a reference implementation kept for readability and for the
signal assertions, and every run compiles the real C++ and compares the two
sample for sample. What is checked is what ships.

    python3 Tools/Code_To_Sound.py                 # check every generator
    python3 Tools/Code_To_Sound.py --render out/   # write WAVs to listen to
    python3 Tools/Code_To_Sound.py --list
"""
from __future__ import annotations

import argparse
import math
import os
import struct
import subprocess
import sys
import tempfile
import wave

RATE = 44100

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
NATIVE = os.path.join(REPO, "Backrooms/Source/Main/Native")

failures: list[str] = []


def check(ok: bool, what: str) -> None:
    if not ok:
        failures.append(what)


# ===========================================================================
# Deterministic noise. Nothing here uses random(): the same call must give the
# same sample on every device and in every run, or a sound cannot be checked.
# ===========================================================================

def _hash01(n: int) -> float:
    n &= 0xFFFFFFFF
    n = (n ^ 61) ^ (n >> 16)
    n = (n + (n << 3)) & 0xFFFFFFFF
    n ^= n >> 4
    n = (n * 0x27D4EB2D) & 0xFFFFFFFF
    n ^= n >> 15
    return (n & 0xFFFFFF) / 0xFFFFFF


def white(i: int) -> float:
    return _hash01(i) * 2.0 - 1.0


def _sample_index(t: float) -> int:
    """Round, never truncate — see the note on sampleIndex() in Synth.cpp."""
    return int(round(t * RATE))


def _lerp(a: float, b: float, t: float) -> float:
    return a + (b - a) * t


def value_noise(x: float) -> float:
    """Smooth 1-D noise. Used wherever a parameter should drift rather than
    jump — tape speed, ballast flicker, the wobble on a monster's voice."""
    i = math.floor(x)
    f = x - i
    f = f * f * (3.0 - 2.0 * f)
    return _lerp(white(int(i)), white(int(i) + 1), f)


# ===========================================================================
# Generators
# ===========================================================================

def vhs_intro(t: float, duration: float) -> float:
    """
    The title sting: a dead tape being played.

    Four things happen at once, which is what stops it sounding like plain
    noise. The transport spins up, so a low rumble slides in from below pitch.
    The head makes contact and hiss arrives with it. The tape has dropouts —
    brief, hard gaps, not fades — because that is what damaged tape does. And
    underneath it all sits mains hum at 50 Hz plus its third harmonic, which is
    the sound of equipment that is on rather than merely present.
    """
    n = _sample_index(t)
    env_in = min(1.0, t / 0.35)
    env_out = min(1.0, max(0.0, (duration - t) / 0.6))
    env = env_in * env_out

    # Transport spinning up: pitch rises and settles.
    spin = 1.0 - math.exp(-t * 3.2)
    rumble = math.sin(2 * math.pi * (22.0 + 26.0 * spin) * t) * 0.45 * spin

    # Head contact hiss, shaped by a slow drift so it breathes.
    hiss = white(n) * (0.16 + 0.10 * value_noise(t * 7.0)) * spin

    # Dropouts: hard gates, irregular, short.
    drop = 1.0 if value_noise(t * 11.0 + 3.1) > -0.55 else 0.12

    # Mains hum — 50 Hz and its third, the way a real earth loop sounds.
    hum = (math.sin(2 * math.pi * 50.0 * t) * 0.09 +
           math.sin(2 * math.pi * 150.0 * t) * 0.035) * spin

    # Tape wow: the whole signal's pitch wavers slightly.
    wow = 1.0 + value_noise(t * 1.7) * 0.012
    body = math.sin(2 * math.pi * 190.0 * t * wow) * 0.08 * spin

    return (rumble + hiss + hum + body) * drop * env


def fluorescent_hum(t: float, health: float) -> float:
    """A tube's own sound. `health` 1 is a good fitting, 0 a failing ballast —
    which buzzes harder and stutters."""
    fail = 1.0 - max(0.0, min(1.0, health))
    hum = (math.sin(2 * math.pi * 100.0 * t) * 0.10 +
           math.sin(2 * math.pi * 300.0 * t) * 0.05 * (0.3 + fail))
    buzz = white(_sample_index(t)) * 0.02 * fail
    stutter = 1.0 if value_noise(t * 9.0) > -0.7 + fail * 0.5 else 0.25
    return (hum + buzz) * stutter


def footstep(t: float, pace: float, surface: float) -> float:
    """
    One footfall. `pace` 0 walking, 1 running; `surface` 0 carpet, 1 hard.

    A step is a transient with almost no sustain — the mistake in a synthesised
    footstep is always too much tail. Carpet is a soft thud plus cloth; a hard
    floor adds a click on top.
    """
    if t < 0.0:
        return 0.0
    n = _sample_index(t)
    decay = math.exp(-t * (34.0 + 22.0 * surface))
    thud = math.sin(2 * math.pi * (78.0 - 18.0 * surface) * t) * decay
    scuff = white(n) * decay * (0.30 + 0.45 * surface)
    click = math.exp(-t * 260.0) * white(n + 7) * surface * 0.7
    return (thud * 0.6 + scuff * 0.35 + click) * (0.7 + 0.5 * pace)


def monster_voice(t: float, proximity: float) -> float:
    """
    Not a growl. A resonance that should not be there — an inharmonic pair well
    below speech, amplitude-modulated so it seems to breathe, getting closer to
    a pitch the ear can hold as `proximity` rises.
    """
    p = max(0.0, min(1.0, proximity))
    f0 = 41.0 + 14.0 * p
    f1 = f0 * 1.4983                       # deliberately not a simple ratio
    breath = 0.55 + 0.45 * math.sin(2 * math.pi * (0.7 + 0.5 * p) * t)
    body = (math.sin(2 * math.pi * f0 * t) * 0.55 +
            math.sin(2 * math.pi * f1 * t) * 0.30)
    grit = white(_sample_index(t)) * 0.06 * p
    return (body * breath + grit) * (0.25 + 0.75 * p)


GENERATORS = {
    "vhs_intro":        (lambda t: vhs_intro(t, 2.6), 2.6),
    "fluorescent_ok":   (lambda t: fluorescent_hum(t, 1.0), 2.0),
    "fluorescent_dying": (lambda t: fluorescent_hum(t, 0.15), 2.0),
    "footstep_carpet":  (lambda t: footstep(t, 0.0, 0.0), 0.35),
    "footstep_run":     (lambda t: footstep(t, 1.0, 0.6), 0.35),
    "monster_far":      (lambda t: monster_voice(t, 0.15), 2.0),
    "monster_near":     (lambda t: monster_voice(t, 1.0), 2.0),
}


# ===========================================================================

def render(fn, duration: float) -> list[float]:
    return [fn(i / RATE) for i in range(int(duration * RATE))]


def write_wav(path: str, samples: list[float]) -> None:
    peak = max((abs(s) for s in samples), default=1.0) or 1.0
    scale = 0.89 / peak
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(b"".join(
            struct.pack("<h", int(max(-1.0, min(1.0, s * scale)) * 32767))
            for s in samples))


def check_generators() -> None:
    print("── Generators")
    for name, (fn, dur) in GENERATORS.items():
        s = render(fn, dur)
        peak = max(abs(v) for v in s)
        rms = math.sqrt(sum(v * v for v in s) / len(s))

        check(all(math.isfinite(v) for v in s), f"{name}: produced a non-finite sample")
        # Clipping is the one fault that is audible on every device and
        # inaudible in a waveform screenshot.
        check(peak <= 1.0 + 1e-6, f"{name}: clips at {peak:.3f}")
        check(peak > 0.02, f"{name}: effectively silent (peak {peak:.4f})")
        check(rms > 0.002, f"{name}: no energy (rms {rms:.5f})")

        # Determinism. A sound built from random() cannot be checked, and worse,
        # differs between two players standing in the same place.
        again = render(fn, min(dur, 0.25))
        check(again == s[:len(again)], f"{name}: not deterministic between runs")

        # A DC offset wastes headroom and thumps when the sound starts.
        dc = sum(s) / len(s)
        check(abs(dc) < 0.08, f"{name}: DC offset {dc:+.3f}")

        print(f"   {name:20s} {dur:4.2f}s  peak {peak:5.3f}  rms {rms:5.3f}  dc {dc:+.4f}")


# ===========================================================================
# Parity with the shipped C++
# ===========================================================================

PARITY_PROBE = r"""
// Renders the real generators to stdout so the reference above can be compared
// against them. Printed as hex bit patterns rather than decimals: a float
// printed to six places hides exactly the kind of small divergence — a wrapped
// hash index, a float/double mix-up — that this is here to find.
#include "Sound/Synth.h"

#include <cstdio>
#include <cstring>
#include <cstdint>

using namespace omni::sound;

static void emit(float v) {
    uint32_t bits; std::memcpy(&bits, &v, sizeof bits);
    std::printf("%08x\n", bits);
}

int main() {
    const int n = 4410;                       // a tenth of a second each
    for (int i = 0; i < n; ++i) emit(vhsIntro(float(i) / kSynthRate, 2.6f));
    for (int i = 0; i < n; ++i) emit(fluorescentHum(float(i) / kSynthRate, 1.0f));
    for (int i = 0; i < n; ++i) emit(fluorescentHum(float(i) / kSynthRate, 0.15f));
    for (int i = 0; i < n; ++i) emit(footstep(float(i) / kSynthRate, 0.0f, 0.0f));
    for (int i = 0; i < n; ++i) emit(footstep(float(i) / kSynthRate, 1.0f, 0.6f));
    for (int i = 0; i < n; ++i) emit(monsterVoice(float(i) / kSynthRate, 0.15f));
    for (int i = 0; i < n; ++i) emit(monsterVoice(float(i) / kSynthRate, 1.0f));
    return 0;
}
"""

PARITY_ORDER = [
    ("vhs_intro",          lambda t: vhs_intro(t, 2.6)),
    ("fluorescent_ok",     lambda t: fluorescent_hum(t, 1.0)),
    ("fluorescent_dying",  lambda t: fluorescent_hum(t, 0.15)),
    ("footstep_carpet",    lambda t: footstep(t, 0.0, 0.0)),
    ("footstep_run",       lambda t: footstep(t, 1.0, 0.6)),
    ("monster_far",        lambda t: monster_voice(t, 0.15)),
    ("monster_near",       lambda t: monster_voice(t, 1.0)),
]


def check_parity() -> None:
    """
    The Python above and the C++ that ships must agree.

    Tolerance is 1e-4 absolute, not exact equality: the C++ computes in float
    and Python in double, so the last couple of bits legitimately differ. What
    that tolerance will not absorb is a generator that was edited on one side
    only — a changed constant, a dropped term, a hash index that wraps
    differently — which is the whole failure mode this exists to catch.
    """
    print("\n── Parity with Native/Sound/Synth.cpp")
    src_dir = os.path.join(NATIVE, "Sound")
    if not os.path.exists(os.path.join(src_dir, "Synth.cpp")):
        failures.append("Native/Sound/Synth.cpp is missing; nothing to compare against")
        return

    with tempfile.TemporaryDirectory() as tmp:
        probe = os.path.join(tmp, "parity.cpp")
        with open(probe, "w", encoding="utf-8") as f:
            f.write(PARITY_PROBE)
        exe = os.path.join(tmp, "parity")
        r = subprocess.run(
            ["g++", "-std=c++20", "-O2", "-Wall", "-Wextra", "-Wpedantic", "-Werror",
             "-I", NATIVE, probe, os.path.join(src_dir, "Synth.cpp"), "-o", exe],
            capture_output=True, text=True)
        if r.returncode != 0:
            failures.append(f"Synth.cpp does not build clean:\n{r.stderr[:1500]}")
            return

        run = subprocess.run([exe], capture_output=True, text=True, timeout=120)
        if run.returncode != 0:
            failures.append(f"the parity probe crashed: {run.stderr[:600]}")
            return

        native = [struct.unpack("<f", bytes.fromhex(line)[::-1])[0]
                  for line in run.stdout.split()]

    n = 4410
    check(len(native) == n * len(PARITY_ORDER),
          f"probe emitted {len(native)} samples, expected {n * len(PARITY_ORDER)}")
    if len(native) != n * len(PARITY_ORDER):
        return

    for idx, (name, fn) in enumerate(PARITY_ORDER):
        worst, at = 0.0, 0
        for i in range(n):
            d = abs(fn(i / RATE) - native[idx * n + i])
            if d > worst:
                worst, at = d, i
        check(worst <= 1e-4,
              f"{name}: Python and C++ disagree by {worst:.6f} at sample {at} "
              f"({at / RATE:.4f}s) — one side was edited without the other")
        print(f"   {name:20s} max |python - c++|  {worst:.2e}")


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--render", metavar="DIR", help="write every generator to a WAV")
    ap.add_argument("--list", action="store_true", help="list the generators")
    args = ap.parse_args()

    if args.list:
        for name, (_, dur) in GENERATORS.items():
            print(f"{name}  ({dur}s)")
        return 0

    check_generators()
    check_parity()

    if args.render:
        os.makedirs(args.render, exist_ok=True)
        print("\n── Rendering")
        for name, (fn, dur) in GENERATORS.items():
            path = os.path.join(args.render, f"{name}.wav")
            write_wav(path, render(fn, dur))
            print(f"   {path}")

    print()
    for f in failures:
        print("FAIL", f)
    print("PASSED" if not failures else
          f"FAILED ({len(failures)} failure{'' if len(failures) == 1 else 's'})")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
