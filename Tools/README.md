# Omni Backrooms — asset tools

Small Python toolchain for getting the character model out of an FBX and into
the game. No third-party packages; standard library only.

## Files

| File | Purpose |
|---|---|
| `fbx_reader.py` | Binary-FBX parser. Node tree, geometry, model transforms, connections, animation census. |
| `fbx_to_omesh.py` | Converts an FBX into the engine's `.omesh` format. |

## Usage

```bash
python3 fbx_to_omesh.py Anime_Character.fbx character.omesh
```

Then copy `character.omesh` and the texture into the app's assets root:

```
Backrooms/Source/Main/Assets/character.omesh
Backrooms/Source/Main/Assets/character_texture.png
```

The engine loads them by those exact names (`CharacterMesh.load` in
`Backrooms.kt`). A missing or malformed file is logged and skipped — the game
falls back to billboard entities rather than failing to start.

## What the tool found in this particular FBX

Worth recording, because two of these are traps that a naive exporter walks
straight into.

**1. There is no animation in the file.**

```
Animation content: 0 stacks, 0 curves
```

The mesh is rigged — 34 deformers, 32 clusters, 2 skins — but no animation
curves were ever exported. There is nothing to play back. The engine therefore
generates motion in the vertex shader from a height-weighted sway model: feet
stay planted, torso and head carry the movement, legs swing in opposite phase
across the model's centre line. If you later export real animation from
Blender, that becomes the thing to replace.

**2. Two objects carry corrupt transforms.**

```
Traje.001    Lcl Scaling = 37463.4
Cube.001     Lcl Scaling = 119.9,  translation 1688 units away
```

Applying those transforms throws the meshes tens of thousands of units off.
But `Traje.001` — the suit — has *local* geometry that is already correctly
positioned around the body. So the converter tries the transform, and if the
result is implausible relative to the rest of the model, retries with identity.
That recovers the suit. `Cube.001` fails both ways, which is correct: it is a
leftover scene cube, not part of the character.

This is why the conversion keeps five meshes and drops one:

```
kept:    Cuerpo.001, Cabeza.002, Traje.001, export.002, Cuerpo.003
dropped: Cube.001
```

**3. Axis convention differs.**

The file is Z-up (Blender). The engine is Y-up. Positions and normals are
remapped `(x, y, z) -> (x, z, -y)`, and V coordinates are flipped for GL's
texture origin.

## Output format

`.omesh`, little-endian:

```
magic      4 bytes   "OMSH"
version    u16 major, u16 minor
counts     u32 vertex_count, u32 index_count
vertices   vertex_count * 8 floats   (px py pz  nx ny nz  u v)
indices    index_count * u16         (u32 if vertex_count > 65535)
```

Deliberately plain: a flat interleaved vertex buffer plus indices maps directly
onto a single `glBufferData` call with no parsing on the device.

The model is normalised to unit height standing on `y = 0`, centred on X and Z,
so the engine can scale it to any world height with a single multiply
(`CHAR_SCALE = 1.75f` for a 1.75 m character).

## Current output

```
7886 vertices, 8426 triangles, 302924 bytes, 16-bit indices
```

## Verification

| File | Purpose |
|---|---|
| `check_shaders.py` | Compiles every GLSL shader embedded in the Kotlin sources. |
| `level0_probe.cpp` | Exercises the level generator over many seeds and asserts the properties a run depends on. |

### Shaders

```bash
python3 Tools/check_shaders.py
```

A shader that fails to compile throws at runtime, on the GL thread, the first
time the screen it belongs to is opened — and the only symptom is a black
screen. The Kotlin compiler cannot see inside a raw string, so nothing else in
the build catches it.

Needs `glslangValidator` (`apt install glslang-tools`).

### Level generator

```bash
g++ -std=c++20 -O2 -I Backrooms/Source/Main/Native \
    Tools/level0_probe.cpp Backrooms/Source/Main/Native/Map/Level_0.cpp \
    -o /tmp/level0_probe && /tmp/level0_probe 40
```

Level 0 is an infinite pure function, so nothing about it can be eyeballed in an
editor and a bad seed cannot be spotted until a player is already lost in it.
The probe floods the map from the spawn and proves the exit is actually
reachable, checks that a relocated exit is reachable from wherever the player
wandered to, and asserts the floor plan's density, its lighting coverage and
that no column ever stands somewhere it could seal a corridor.
