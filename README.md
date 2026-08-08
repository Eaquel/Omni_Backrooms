<!--
  This is the English README and the one GitHub shows by default.

  A note on the language bar below, because it looks like a workaround and is
  one: GitHub renders README.md and nothing else, with no content negotiation
  and no way to branch on the reader's Accept-Language. A link bar is the only
  honest option — every "auto-detecting README" is either JavaScript that
  GitHub strips or an image proxy that logs whoever loads it.

  The game itself does detect the device language, on first launch, with no
  setting to find. That distinction is worth keeping straight: the app is
  automatic, this page cannot be.

  When you fix a bug, add it to "Recent fixes". When you add a language, add it
  to the bar in all ten files.
-->

**English** ·
[Türkçe](README.tr.md) ·
[Deutsch](README.de.md) ·
[Español](README.es.md) ·
[Français](README.fr.md) ·
[Italiano](README.it.md) ·
[Português](README.pt.md) ·
[Русский](README.ru.md) ·
[日本語](README.ja.md) ·
[中文](README.zh.md)

# Omni Backrooms

An Android survival horror game set in Level 0 — endless mono-yellow office
corridors, damp carpet, humming fluorescents, and one thing in there with you
that cannot be killed.

Written from scratch: the renderer is OpenGL ES 3.0 driven from Kotlin, the
simulation is C++ through the NDK, and the level is not a map file but a pure
function of cell coordinates, so it never ends and never repeats a seam.

## What is in here

| | |
|---|---|
| **Level 0** | Infinite. Every cell — floor, wall, light, damp — is derived from its own coordinates and the run's seed, so the world is identical for two players who never exchange a byte of it. |
| **One creature** | Not a crowd. It sees by raycast, so walls actually hide you; it hears by how loud you are, so crouching genuinely buys something; and it remembers where it last saw you. |
| **The flashlight** | Slows it, then drives it off. It does not kill it. Nothing in the Backrooms dies — it retreats, fades, waits at distance, and comes back when it sees you again or hears you get careless. |
| **No audio files** | Every sound is synthesised on the device. The APK contains no WAVs, no OGGs, nothing. |
| **Ten languages** | Turkish, English, German, Spanish, French, Italian, Portuguese, Russian, Japanese, Chinese — complete, not partial. The game picks your device's on first launch. |
| **Cosmetics only** | Frames, trails and characters. Nothing sold anywhere in the game affects how it plays. |

## Building it

```bash
git clone https://github.com/Eaquel/Omni_Backrooms.git
cd Omni_Backrooms
./gradlew :Backrooms:assembleRelease
```

You need JDK 25, Android SDK 36, the NDK and CMake 4.3.2. Release builds are
signed from a keystore that is not in this repository; `assembleDebug` needs
nothing extra.

## The checks

Seven of the eight tools in `Tools/` run on every push. They exist because each
guards something the Gradle build genuinely cannot see:

| Tool | What it catches |
|---|---|
| `Shaders_Check.py` | GLSL lives inside Kotlin raw strings, so a shader that will not compile is invisible until the screen using it opens and goes black. Every one is compiled with `glslangValidator`. |
| `Assets_Check.py` | Hand-written vector icons that `aapt2` accepts and renders garbled; mesh UVs that no longer match world position; the inspection camera leaving its backdrop; duplicate and unreferenced assets; a locale that fell behind; the Unity build contradicting itself; a character rig whose bones are not on the geometry they claim to drive, proved by animating it and measuring the seams. Also `--optimise`, a lossless PNG re-encoder. |
| `Native_Check.py` | The JNI contract. Kotlin declares `external fun`, C++ defines `Java_..._name`, and **nothing** connects them at build time — not the Kotlin compiler, not the C++ compiler, not the linker. A rename on one side is an `UnsatisfiedLinkError` on first call; a changed argument count is worse, because JNI binds by name and reads the extra arguments off the stack without complaining. |
| `Kotlin_Check.py` | Every import against the dependency behind it, both ways. The Kotlin here compiles without the Android classpath, so a library that is genuinely gone looks exactly like one that is merely off the path — which is how removing Firebase quietly took `androidx.media3` with it and only surfaced ninety seconds into a Gradle build. |
| `Level_0_Check.py` | Floods the world from the spawn over many seeds and proves the exit is reachable. An unreachable exit is an unwinnable run and it is completely silent. |
| `Entity_Check.py` | Compiles the real AI, puts a creature in the real Level 0, and watches: sight blocked by walls, hearing that scales with noise, the retreat-and-return cycle that must never latch. |
| `Code_To_Sound.py` | Renders the shipped C++ generators and compares them against a Python reference sample for sample. Also writes WAVs, so sounds that exist only as code can actually be listened to. |

Run them all:

```bash
for t in Shaders Assets Native Entity Kotlin; do python3 Tools/${t}_Check.py; done
python3 Tools/Level_0_Check.py 40
python3 Tools/Code_To_Sound.py
```

Every check in here was verified by putting its bug back and watching it fail.
A check that has never failed is a check nobody has any reason to trust.

## Layout

```
Backrooms/Source/Main/
  Kotlin/com/omni/backrooms/     UI, renderer, game loop  (~14k lines)
  Native/                        C++ through the NDK      (~3.9k lines)
    Map/        Level 0 as a pure function of coordinates
    Entity/     creature AI — perception, retreat, return
    Sound/      every generator; there are no audio files
    Frame/      profile frame cosmetics
    Trail/      footstep trail cosmetics
    Shield/     the detectors, and what the binary claims to be
  Assets/                        textures, meshes, story
  res/values*/                   ten languages
Tools/                           the eight checks
```

## Recent fixes

Newest first. This list is updated with every fix.

- **Eight creatures became one, and the Smiler got a body.** A bestiary is a
  different game — you learn to read which one you are looking at, and the
  reading is the fun. Level 0 holds one thing you never get a good look at. The
  Smiler itself was a cut-out: an ovoid with a noise-displaced edge is one
  contour, the same thickness the whole way round. It is a density field now —
  four octaves of drifting noise pushed along a curl, wide at the hem, narrowing
  and swaying as it rises, throwing tendrils. The face is multiplied by the
  smoke around it, so it surfaces where the column is thick and swims where it
  is not; painted on at full strength it was a decal. Seven behaviour trees in
  the native AI went with the seven creatures.
- **A texture hung in mid-air, and it was the doorway.** The mesher drew a
  single horizontal quad at 0.82 of the wall height with both long edges ending
  in open space. The comment above it described "a lintel and two jambs";
  neither had ever been written. It is a real frame now, and Level_0_Check
  states the assumption it stands on — which then caught the doorway rule
  existing twice, once in `featureAt` and once inside `sampleChunk`, with the
  mesher reading one and every check reading the other.
- **Frames were drawn across the photo.** The clearance was a fraction of each
  sample's own radius, and `frameProfile` normalises only the widest sample to
  1.0, so at the narrow points the cap shrank with them: 0.283 of the box
  against a portrait of 0.33. The bound is absolute now. The ring also stopped
  being tilted 0.62 rad away from the camera, which was projecting a circular
  frame as an ellipse around a circular picture.
- **All eight creatures were the Smiler.** The only difference was a tint the
  shader multiplies by 0.055. Each has its own silhouette now — the Howler's
  low head and heavy shoulders, the Party Goer's limbs, the Deathmoth's wings,
  the Wretched's six eyes, the Faceling's blank stillness.
- **The third-person arrival had no camera.** The body collapsed and stood up
  in a corner of the frame at a flat 2.6 m. The boom now pulls back for the
  fall, cuts to knee height on impact, and rises with her.
- **Firebase never worked, and took a lot with it.** There is no
  google-services.json here and CI injects a placeholder, so every Crashlytics
  log, Firestore write and Remote Config fetch failed at runtime inside a
  `runCatching` that swallowed it. The REST API was the same story at
  api.omnibackrooms.com, which does not resolve, and the netcode under it drained
  a socket nothing sent to — voice chat included. All of it is gone, with Room,
  Billing and the Credential Manager, which nothing referenced at all. Kotlin
  drops 2824 lines.
- **The flashlight was a circle in the middle of the screen.** Drawn at uv
  (0.5, 0.47) in the post pass, with no position in the world, which is exactly
  why the beam looked like it came out of her chest. It is a real spotlight in
  the scene shader now, cast from the lens of the torch model.
- **Owned trails could not be worn.** Three faults in a row: the owned-id set
  was assigned from frames alone and overwrote every trail, nothing but buying
  one could equip it, and the corridor read the equipped id once per screen.
- **Notifications were asked for over the intro.** The permission gate sat
  beside the NavHost rather than inside it. It is behind the intro now, and the
  settings toggle can request the permission instead of only linking out.
- **Two textures were not powers of two.** 1536x1024 and 1448x1086, so neither
  could carry a mipmap chain and both shimmered at a distance. All four are
  1024x1024; assets go 6.0MB to 4.7MB.
- **The character had four arms.** The mesh held two pairs: a body with its arms
  at its sides, and a dress whose sleeves stood straight out in a T-pose. The
  bones had been laid along the sleeves, so the rig swung empty cloth while the
  arms the player sees stayed welded to the hips. The sleeves are on the arms
  now, and binding measures distance along the surface instead of through the
  air — the skirt hem passes within 4cm of the hand, and no straight-line
  measure can tell those two apart. Eight shells duplicated a millimetre away
  went with it: 1139 vertices, and the z-fighting they were causing.
- **Level 0 held a crowd.** Three to eight creatures, topped up every twelve
  seconds. A crowd is busy, not frightening. It now holds exactly one, and
  difficulty changes what that one is rather than how many there are.
- **Creatures saw through walls.** Sight was a distance test that ignored the
  level entirely, so there was no way to break contact except by outrunning it.
- **Driving one off removed it permanently.** The retreat measured its distance
  from the live player position, so following it kept it fleeing forever; and
  the parked state reset its fade every tick, so the return could never finish.
  Both found by simulation, neither findable on a device.
- **Eight per cent of every synthesised noise was a repeated sample.** Both the
  C++ and the Python took the noise index as `int(t * 44100)`, and in float
  `i/44100*44100` lands a hair under `i`. Audible, invisible in a waveform.
- **Turkish players saw a literal `%d`** on the room-size label, which was
  written with a format specifier and drawn without an argument.
- **The default resources were Turkish.** `values/` is what Android falls back
  to for a language it has no entry for, so any untranslated string appeared in
  Turkish inside an otherwise German menu. English holds it now.
- **CI reported failure on green code.** The two jobs raced for one runner;
  the static checks never started, timed out queued, and failed the run while
  the APK built perfectly every time.
- **The character looked four-armed.** The rig rotated limbs by multiplying the
  angle by a position gradient, which fans a limb rather than turning it.
  Replaced with real linear blend skinning over a twelve-bone skeleton.
- **The anti-tamper guard accused clean devices** on every launch, from a bare
  substring search of `/proc/self/maps`. It now reports what it found, and
  writes the reason to `Documents/Backrooms_Log/`.
- **Ceiling textures were mirrored** across each tile's diagonal: the emitter
  handed out UVs in a fixed corner order, which is only correct for a quad
  wound the other way.

## Licence

All rights reserved. The code is here to be read.
