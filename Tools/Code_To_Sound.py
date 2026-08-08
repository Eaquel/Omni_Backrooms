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
import re
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


def footstep(t: float, pace: float, surface: float, step: float) -> float:
    """One footfall. See Sound/Synth.cpp for why each term is shaped this way."""
    if t < 0.0 or t > 0.42:
        return 0.0
    n = _sample_index(t)
    p = max(0.0, min(1.0, pace))
    s = max(0.0, min(1.0, surface))

    si = int(step) & 0xFFFFFFFF
    j0 = _hash01(si * 2654435761)
    j1 = _hash01(si * 40503 + 17)
    j2 = _hash01(si * 2246822519 + 5)

    f0 = (54.0 + 14.0 * s) * (0.90 + 0.20 * j0)
    body_decay = (13.0 + 9.0 * s) * (0.88 + 0.24 * j1)
    body_env = math.exp(-t * body_decay) * (1.0 - math.exp(-t * 900.0))
    body = math.sin(2 * math.pi * f0 * t * (1.0 - 0.35 * t)) * body_env

    tt = t - (0.026 + 0.010 * j2)
    toe_env = (math.exp(-tt * (body_decay * 1.9)) * (1.0 - math.exp(-tt * 1400.0))
               if tt > 0.0 else 0.0)
    toe = math.sin(2 * math.pi * f0 * 1.6 * tt) * toe_env * 0.42

    lp = sum(white((n - k + si * 977) & 0xFFFFFFFF) for k in range(8)) / 8.0
    scuff = lp * math.exp(-t * (26.0 + 14.0 * s)) * (0.22 + 0.30 * s)

    click = math.exp(-t * 300.0) * white((n + 7 + si * 31) & 0xFFFFFFFF) * s * s * 0.55

    gain = (0.62 + 0.45 * p) * (0.86 + 0.28 * j2)
    return (body * 0.72 + toe + scuff + click) * gain


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


def room_tone(t: float, damp: float) -> float:
    """The empty building. See Sound/Synth.cpp for why each layer is there."""
    n = _sample_index(t)
    d = max(0.0, min(1.0, damp))
    drone = (math.sin(2 * math.pi * 47.0 * t) * 0.055 +
             math.sin(2 * math.pi * 47.2 * t) * 0.045)
    lp = sum(white((n - k) & 0xFFFFFFFF) for k in range(12)) / 12.0
    air = lp * 0.085
    top = white(n + 991) * 0.012
    cyc = t - 3.4 * math.floor(t / 3.4)
    ring = math.exp(-cyc * 26.0)
    drip = math.sin(2 * math.pi * (1180.0 - 260.0 * cyc) * cyc) * ring * 0.16 * d
    return drone + air + top + drip


def breath(t: float, exertion: float) -> float:
    """Her breathing. In and out are different shapes on purpose."""
    e = max(0.0, min(1.0, exertion))
    rate = 0.30 + 0.85 * e
    ph = t * rate - math.floor(t * rate)
    inh = math.sin(math.pi * min(ph / 0.55, 1.0))
    out = math.sin(math.pi * (ph - 0.55) / 0.45) if ph > 0.55 else 0.0
    env = inh * 0.55 + out * 1.0
    n = _sample_index(t)
    lp = sum(white((n - k) & 0xFFFFFFFF) for k in range(5)) / 5.0
    formant = (math.sin(2 * math.pi * 620.0 * t) * 0.25 +
               math.sin(2 * math.pi * 1180.0 * t) * 0.12)
    return (lp * 0.7 + lp * formant) * env * (0.12 + 0.5 * e)


def heartbeat(t: float, fear: float) -> float:
    """Lub and dub, the second softer and a fifth of a beat behind."""
    f = max(0.0, min(1.0, fear))
    bpm = 58.0 + 62.0 * f
    period = 60.0 / bpm
    ph = (t - period * math.floor(t / period)) / period

    def thump(u: float, gain: float) -> float:
        if u < 0.0:
            return 0.0
        env = math.exp(-u * 26.0) * (1.0 - math.exp(-u * 420.0))
        return math.sin(2 * math.pi * (52.0 - 20.0 * u) * u) * env * gain

    return (thump(ph * period, 1.0) + thump((ph - 0.22) * period, 0.62)) * (0.15 + 0.85 * f)


def torch_click(t: float) -> float:
    """The switch: a contact transient and a spring ring, over in 40 ms."""
    if t < 0.0 or t > 0.06:
        return 0.0
    n = _sample_index(t)
    snap = white(n) * math.exp(-t * 620.0)
    ring = math.sin(2 * math.pi * 2400.0 * t) * math.exp(-t * 150.0) * 0.35
    return (snap * 0.8 + ring) * 0.5


GENERATORS = {
    "vhs_intro":        (lambda t: vhs_intro(t, 2.6), 2.6),
    "fluorescent_ok":   (lambda t: fluorescent_hum(t, 1.0), 2.0),
    "fluorescent_dying": (lambda t: fluorescent_hum(t, 0.15), 2.0),
    "footstep_carpet":  (lambda t: footstep(t, 0.0, 0.0, 3), 0.42),
    "footstep_carpet2": (lambda t: footstep(t, 0.0, 0.0, 4), 0.42),
    "footstep_run":     (lambda t: footstep(t, 1.0, 0.6, 7), 0.42),
    "monster_far":      (lambda t: monster_voice(t, 0.15), 2.0),
    "monster_near":     (lambda t: monster_voice(t, 1.0), 2.0),
    "room_tone_dry":    (lambda t: room_tone(t, 0.0), 4.0),
    "room_tone_damp":   (lambda t: room_tone(t, 1.0), 4.0),
    "breath_rest":      (lambda t: breath(t, 0.15), 4.0),
    "breath_sprint":    (lambda t: breath(t, 1.0), 4.0),
    "heartbeat_calm":   (lambda t: heartbeat(t, 0.2), 4.0),
    "heartbeat_close":  (lambda t: heartbeat(t, 1.0), 4.0),
    "torch_click":      (torch_click, 0.06),
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
    for (int i = 0; i < n; ++i) emit(footstep(float(i) / kSynthRate, 0.0f, 0.0f, 3.0f));
    for (int i = 0; i < n; ++i) emit(footstep(float(i) / kSynthRate, 0.0f, 0.0f, 4.0f));
    for (int i = 0; i < n; ++i) emit(footstep(float(i) / kSynthRate, 1.0f, 0.6f, 7.0f));
    for (int i = 0; i < n; ++i) emit(monsterVoice(float(i) / kSynthRate, 0.15f));
    for (int i = 0; i < n; ++i) emit(monsterVoice(float(i) / kSynthRate, 1.0f));
    for (int i = 0; i < n; ++i) emit(roomTone(float(i) / kSynthRate, 0.0f));
    for (int i = 0; i < n; ++i) emit(roomTone(float(i) / kSynthRate, 1.0f));
    for (int i = 0; i < n; ++i) emit(breath(float(i) / kSynthRate, 0.15f));
    for (int i = 0; i < n; ++i) emit(breath(float(i) / kSynthRate, 1.0f));
    for (int i = 0; i < n; ++i) emit(heartbeat(float(i) / kSynthRate, 0.2f));
    for (int i = 0; i < n; ++i) emit(heartbeat(float(i) / kSynthRate, 1.0f));
    for (int i = 0; i < n; ++i) emit(torchClick(float(i) / kSynthRate));
    return 0;
}
"""

PARITY_ORDER = [
    ("vhs_intro",          lambda t: vhs_intro(t, 2.6)),
    ("fluorescent_ok",     lambda t: fluorescent_hum(t, 1.0)),
    ("fluorescent_dying",  lambda t: fluorescent_hum(t, 0.15)),
    ("footstep_carpet",    lambda t: footstep(t, 0.0, 0.0, 3)),
    ("footstep_carpet2",   lambda t: footstep(t, 0.0, 0.0, 4)),
    ("footstep_run",       lambda t: footstep(t, 1.0, 0.6, 7)),
    ("monster_far",        lambda t: monster_voice(t, 0.15)),
    ("monster_near",       lambda t: monster_voice(t, 1.0)),
    ("room_tone_dry",      lambda t: room_tone(t, 0.0)),
    ("room_tone_damp",     lambda t: room_tone(t, 1.0)),
    ("breath_rest",        lambda t: breath(t, 0.15)),
    ("breath_sprint",      lambda t: breath(t, 1.0)),
    ("heartbeat_calm",     lambda t: heartbeat(t, 0.2)),
    ("heartbeat_close",    lambda t: heartbeat(t, 1.0)),
    ("torch_click",        torch_click),
]


def check_footstep_character() -> None:
    """
    A footstep has to sound like a foot, not a tick.

    The report was that walking went "dit dit" and was hard to listen to. It
    did, and the reason was measurable: the old generator put its dominant
    energy at about 1.1 kHz and was over in 53 ms, which is a click. A real
    footfall on carpet lives under 200 Hz and lasts 120-180 ms. The unfiltered
    white noise in its scuff term was what pushed the spectrum an octave and a
    half above where a step belongs.

    The other half was worse and has nothing to do with spectrum: the synth
    restarted the generator at t = 0 with the same arguments every time, so
    every footfall of a walk was the same waveform, on a metronome. Anything
    both perfectly periodic and perfectly identical reads as a UI beep.

    None of that is visible in the code and none of it is audible in CI, so it
    is measured here: where the energy sits, how long it lasts, and whether two
    consecutive steps differ at all.
    """
    print("\n── Footstep character")
    dur = 0.42

    def dominant_hz(sig):
        zc = sum(1 for i in range(1, len(sig)) if (sig[i - 1] < 0) != (sig[i] < 0))
        return zc / 2 / dur

    def tail_ms(sig):
        peak = max(abs(v) for v in sig) or 1.0
        for i in range(len(sig)):
            if all(abs(v) < peak * 0.1 for v in sig[i:i + 400]):
                return 1000.0 * i / RATE
        return 1000.0 * len(sig) / RATE

    for label, pace, surf in (("carpet", 0.0, 0.0), ("in game", 0.0, 0.3),
                              ("running", 1.0, 0.6)):
        sig = render(lambda t: footstep(t, pace, surf, 3), dur)
        hz, ms = dominant_hz(sig), tail_ms(sig)
        print(f"   {label:8s} dominant ~{hz:4.0f} Hz, decays by {ms:4.0f} ms")
        check(hz < 260.0,
              f"the {label} footstep's energy sits at {hz:.0f} Hz — a step is a "
              f"low thump under 200 Hz, and anything up here is a tick")
        check(ms > 85.0,
              f"the {label} footstep is over in {ms:.0f} ms; under about 85 there "
              f"is no body to it, only the attack")

    # Consecutive footfalls. This measured exactly zero before.
    a = render(lambda t: footstep(t, 0.0, 0.3, 3), dur)
    b = render(lambda t: footstep(t, 0.0, 0.3, 4), dur)
    d = max(abs(x - y) for x, y in zip(a, b))
    print(f"   two consecutive steps differ by {d:.3f}")
    check(d > 0.05,
          f"two consecutive footfalls differ by {d:.3f} — a walk made of one "
          f"repeated waveform is a metronome, whatever the waveform is")


def check_reachable() -> None:
    """
    Every generator has to reach a speaker.

    Sound/Synth.h opens with "code you cannot hear is code nobody checks" and
    "what gets checked is what ships". Neither was true. fluorescentHum,
    footstep and monsterVoice — three of the four generators, the ones this
    file renders and compares against a Python reference sample for sample —
    had no caller anywhere in the engine. Only the title sting reached the
    speaker. What actually played was a second, cruder set of generators
    written inline in Engine.cpp: an 800-radian-per-second "click" that is
    really 127 Hz, a monster whose frequency modulation was applied to an
    integer sample counter so its phase jumped whenever the pitch moved, and an
    ambience layer of unfiltered white noise from a std::mt19937 — which is not
    deterministic, so two players standing in the same place heard different
    things, the one property the header says the design exists to guarantee.

    So the tool was verifying three sounds nobody had ever heard, while four
    unchecked ones played. Every generator declared in the header is now
    required to appear in the engine that feeds the audio callback.
    """
    print("\n── Reachability")
    header = open(os.path.join(NATIVE, "Sound/Synth.h"), encoding="utf-8").read()
    engine = open(os.path.join(NATIVE, "Engine.cpp"), encoding="utf-8").read()

    declared = re.findall(r"^\[\[nodiscard\]\] float (\w+)\(", header, re.M)
    # hash01/white/valueNoise are the shared primitives every generator is built
    # from, not sounds in their own right.
    PRIMITIVES = {"hash01", "white", "valueNoise"}
    gens = [g for g in declared if g not in PRIMITIVES]
    check(len(gens) >= 4, "the header declares almost no generators — has it moved?")

    for g in gens:
        # A generator counts as reachable if the engine names it, or if the
        # header's own OneShot plays it (the engine names OneShot).
        in_engine = re.search(r"\b" + g + r"\s*\(", engine) is not None
        in_oneshot = re.search(r"\b" + g + r"\s*\(", header[header.index("class OneShot"):]) \
            if "class OneShot" in header else None
        ok = in_engine or in_oneshot is not None
        where = "engine" if in_engine else ("OneShot" if ok else "NOWHERE")
        print(f"   {g:20s} {where}")
        check(ok, f"{g} is synthesised, rendered and checked here, and nothing "
                  f"in the engine ever plays it — it is a sound no player can hear")

    # And the reverse: the engine must not carry a second set of generators of
    # its own. An oscillator in the audio callback is one this file cannot see.
    callback = engine[engine.index("aaudioDataCallback"):] if "aaudioDataCallback" in engine else ""
    stray = re.findall(r"std::mt19937|uniform_real_distribution", callback[:4000])
    check(not stray,
          "the audio callback draws from a random number generator, so what it "
          "plays cannot be rendered, cannot be compared, and differs between two "
          "players standing in the same place")


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
    check_footstep_character()
    check_reachable()
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
