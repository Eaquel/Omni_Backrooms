#!/usr/bin/env python3
"""
Market inspection camera check.

The inspection sheet lets the player orbit and dolly around the character, and
the studio she stands in is two quads and a procedural gradient. That leaves a
handful of ways to get a visibly broken shot that no unit test and no compiler
can see:

  * the camera drops below the cove floor at a low angle and a long dolly, and
    the backdrop swallows her;
  * a frame edge runs off the end of the backdrop plate, so the gradient stops
    on a hard line instead of fading into nothing — which is exactly the thing
    an infinity cove exists to avoid, and which only shows up on the widest
    aspect ratios;
  * the reframing that keeps her head in shot when zoomed in cuts her feet off
    when zoomed out, or the other way round.

All three are pure geometry, so they can be settled here rather than on a
phone. This reproduces CharacterPreviewRenderer.updateView and the projection
exactly and sweeps the whole pitch/distance/aspect envelope.

KEEP THE CONSTANTS BELOW IN STEP WITH THE RENDERER.

    python3 Tools/preview_cam.py
"""
import math

# Mirrors CharacterPreviewRenderer.
MIN_DIST, MAX_DIST = 1.7, 5.2
MIN_PITCH, MAX_PITCH = -10.0, 38.0
FOVY = 34.0
NEAR, FAR = 0.1, 40.0
COVE = 14.0          # floor half-extent, wall half-width, wall height
CHAR_H, CHAR_R = 1.7, 0.40

def view(d, pitch_deg):
    d = max(MIN_DIST, min(MAX_DIST, d))
    far = min(1.0, max(0.0, (d - MIN_DIST) / (MAX_DIST - MIN_DIST)))
    ty = 1.38 - 0.46 * far
    p = math.radians(pitch_deg)
    eye = (0.0, max(0.22, ty + math.sin(p) * d), math.cos(p) * d)
    return eye, (0.0, ty, 0.0)

def basis(eye, tgt):
    f = [tgt[i] - eye[i] for i in range(3)]
    n = math.dist(eye, tgt); f = [c / n for c in f]
    up = (0.0, 1.0, 0.0)
    s = [f[1]*up[2]-f[2]*up[1], f[2]*up[0]-f[0]*up[2], f[0]*up[1]-f[1]*up[0]]
    ln = math.sqrt(sum(c*c for c in s)); s = [c/ln for c in s]
    u = [s[1]*f[2]-s[2]*f[1], s[2]*f[0]-s[0]*f[2], s[0]*f[1]-s[1]*f[0]]
    return f, s, u

def inside_frustum(eye, tgt, aspect, pt):
    """Is pt inside the view frustum?"""
    f, s, u = basis(eye, tgt)
    v = [pt[i] - eye[i] for i in range(3)]
    z = sum(v[i]*f[i] for i in range(3))
    if z < NEAR or z > FAR: return False
    x = sum(v[i]*s[i] for i in range(3))
    y = sum(v[i]*u[i] for i in range(3))
    hv = z * math.tan(math.radians(FOVY / 2)); hh = hv * aspect
    return abs(y) <= hv and abs(x) <= hh

def corner_ray_hits_cove(eye, tgt, aspect, sx, sy):
    """Cast a ray through a frame corner; return True if it lands on the cove."""
    f, s, u = basis(eye, tgt)
    hv = math.tan(math.radians(FOVY / 2)); hh = hv * aspect
    d = [f[i] + s[i]*sx*hh + u[i]*sy*hv for i in range(3)]
    n = math.sqrt(sum(c*c for c in d)); d = [c/n for c in d]
    # back wall at z = -COVE
    if d[2] < -1e-6:
        t = (-COVE - eye[2]) / d[2]
        if t > 0:
            hx, hy = eye[0]+d[0]*t, eye[1]+d[1]*t
            if abs(hx) <= COVE and 0 <= hy <= COVE: return True, ("wall", hx, hy, t)
    # floor at y = 0
    if d[1] < -1e-6:
        t = (0.0 - eye[1]) / d[1]
        if t > 0:
            hx, hz = eye[0]+d[0]*t, eye[2]+d[2]*t
            if abs(hx) <= COVE and abs(hz) <= COVE: return True, ("floor", hx, hz, t)
    return False, ("miss", d[0], d[1], d[2])

fails = []
aspects = [0.45, 0.5625, 0.75, 1.0, 1.6, 2.2]   # portrait phone .. ultrawide landscape
for aspect in aspects:
    for pi in range(0, 25):
        pitch = MIN_PITCH + (MAX_PITCH - MIN_PITCH) * pi / 24
        for di in range(0, 25):
            d = MIN_DIST + (MAX_DIST - MIN_DIST) * di / 24
            eye, tgt = view(d, pitch)
            if eye[1] < 0.21:
                fails.append(f"eye below floor  aspect={aspect} pitch={pitch:.1f} d={d:.2f} eyeY={eye[1]:.3f}")
            for sx, sy in ((-1,-1), (1,-1), (-1,1), (1,1), (0,1), (0,-1)):
                ok, info = corner_ray_hits_cove(eye, tgt, aspect, sx, sy)
                if not ok:
                    fails.append(f"frame edge misses cove  aspect={aspect} pitch={pitch:.1f} d={d:.2f} corner=({sx},{sy}) {info}")
                elif info[3] > FAR:
                    fails.append(f"cove beyond far plane  aspect={aspect} pitch={pitch:.1f} d={d:.2f} t={info[3]:.1f}")

# Whole figure must be in frame at the fully pulled-back, house-angle shot.
for aspect in aspects:
    eye, tgt = view(MAX_DIST, 7.0)
    for y in (0.0, CHAR_H):
        for ang in range(0, 360, 45):
            pt = (CHAR_R*math.cos(math.radians(ang)), y, CHAR_R*math.sin(math.radians(ang)))
            if not inside_frustum(eye, tgt, aspect, pt):
                fails.append(f"figure clipped at max dist  aspect={aspect} pt={pt}")

# Head must be in frame at the fully pushed-in shot.
for aspect in aspects:
    eye, tgt = view(MIN_DIST, 7.0)
    for pt in ((0.0, CHAR_H, 0.0), (0.0, CHAR_H - 0.30, 0.0)):
        if not inside_frustum(eye, tgt, aspect, pt):
            fails.append(f"head clipped at min dist  aspect={aspect} pt={pt}")

print(f"eye height range: "
      f"{min(view(d,p)[0][1] for d in (MIN_DIST,MAX_DIST) for p in (MIN_PITCH,MAX_PITCH)):.3f} .. "
      f"{max(view(d,p)[0][1] for d in (MIN_DIST,MAX_DIST) for p in (MIN_PITCH,MAX_PITCH)):.3f} m")
for label, d in (("closest", MIN_DIST), ("default", 3.3), ("widest", MAX_DIST)):
    eye, tgt = view(d, 7.0)
    hv = d * math.tan(math.radians(FOVY/2))
    print(f"{label:8s} d={d:.2f}  aim y={tgt[1]:.2f}  vertical span {tgt[1]-hv:+.2f} .. {tgt[1]+hv:+.2f} m")
print()
if fails:
    for f_ in fails[:12]: print("FAIL", f_)
    if len(fails) > 12: print(f"... and {len(fails) - 12} more")
    print(f"\nFAILED ({len(fails)} failure(s))")
    raise SystemExit(1)
print(f"PASSED — {len(aspects)*25*25} camera states, all framing invariants hold")
