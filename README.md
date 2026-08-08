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

- **The body came through the dress, and the legs had no calves.** Two faults
  that no structural check can see: the file parses, the shells are closed, the
  rig survives its poses, and the character still does not sit in her clothes.
  Measured on the shipped mesh, skin was outside the fabric in 10 of 34 sampled
  directions round the trunk — the worst by 16 mm on a figure one unit tall,
  some 3 cm at human scale — and the body carried its own moulded skirt flaring
  to 109 mm below a hem that is 107, a second skirt hanging out from under the
  first. Both surfaces are star-shaped about the vertical axis over the dress's
  height, so the test is a direct one: at the same height and the same angle, is
  the skin further from the axis than the cloth? The torso is now tucked to a
  6 mm clearance inside the garment, with the arms, legs, head and dress
  untouched and the normals rebuilt on every triangle that moved.
  Separately, one leg's widest cross-section measured 68 mm at calf height
  against 63 at the ankle — a ratio of 1.08, where a real lower leg is nearer
  1.6, because the calf belly is the widest part of it. The legs were a straight
  taper from knee to ankle, and that, far more than triangle count, is what made
  them read as sticks. The calf is 87 mm now. The swell was swept against the
  rig's own worst skinning seam, since a wider surface moves further under the
  same bone rotation: everything up to 1.35x shares one pre-existing seam in the
  thigh, and 1.42x introduces a new one in the shin, so 1.35 is the whole of the
  improvement that costs nothing. A thigh swell was tried and dropped — every
  version of it pushed the worst seam from 2.97 cm to 3.2 cm, and the
  measurement it was correcting rested on four vertices.
- **The room was the right size and everything in it was twice as big.** The
  ceiling did not feel like the Backrooms because its T-bar grid was on a 1.6 m
  module, and a metric suspended ceiling is 600 mm. The carpet tiles were
  800 mm against a real 500. The wall joints were on a 1.6 m module against an
  800 mm paper drop. The light fittings called themselves 2x4 troffers in a
  comment — 610 by 1220 mm — and were built 1340 by 2430, from fractions of the
  cell rather than in metres, so they would have silently rescaled if the cell
  ever moved. The fluorescent tubes were 134 mm across; a T8 is 26, the 8 being
  its diameter in eighths of an inch. Level 0's own dimensions were never
  wrong: 3.2 m cells and a 2.6 m ceiling are ordinary office numbers. But the
  grid overhead is the strongest cue the eye has for how big a space is, and it
  was counting five tiles across a corridor that should show eight, so a
  correctly sized room read as one built for something larger than a person.
  Everything is in metres now and matches the part it imitates. The walls also
  have a skirting board for the first time, a 100 mm one with a shadow gasket
  under it — every wall in the level used to run straight into the carpet with
  nothing at the join, which no built room does.
- **The check that watched the level's darkness was the wrong shape.** A
  per-seed bound on how much floor needs the torch was tuned on eight seeds and
  looked fine; over twenty, four seeds crossed it. How dark a seed comes out
  genuinely varies — that is what a seed is — so a bound tight enough to catch a
  regression fails honest seeds. It asserts on the distribution over all sixty
  now: the median, the p90 and the gap between the darkest and brightest seed.
  All three earn their place, and the last one especially: restoring the old
  178-metre mains-failure noise gives a *better* median than the shipped level,
  16.0% against 20.9%, because most of its seeds are bright. It is the 1.7% to
  67.5% range that makes it broken. The failure regions are 34 m across now
  rather than 178, chosen off a sweep — that is where the spread stops
  shrinking and the darkest seed stops being a different game from the
  brightest.
- **Most of the level was pitch black, and the check that watched for it was
  asking about a constant.** Ceiling fittings were placed on a global lattice —
  a cell carried a tube if it was open floor and both its coordinates were
  multiples of four. The lattice is global and the floor plan is not, so
  whether a corridor was lit came down to its coordinate parity: a one-cell
  corridor running along z = 7 never touches a lattice row and received no
  fitting anywhere along its length. Measured over six seeds, 54% of all open
  floor sat under 0.08 illuminance, which the scene shader renders at 9% of
  albedo, and the longest unbroken walk through cells you could not see a step
  of was 60 cells: 192 metres. The fitting now looks for the floor instead of
  waiting for the floor to arrive under it — one tube per four-by-four block,
  same density, rung outward from the lattice point to the nearest open cell in
  a fixed order so the world stays a pure function of its coordinates. The
  falloff width went from 0.95 to 1.70, because at 0.95 the midpoint between
  two tubes — the single most common place to be standing — got a tenth of one
  tube's output. Nothing renders black now and 20% of the floor wants the
  torch, against 70% before.
- **One seed was a lit lobby and the next was a third pitch dark.** Mains
  failure came from noise one wavelength every 178 metres, so a player only
  ever saw about two wavelengths of it and two samples are not a distribution:
  across six seeds the unpowered share of the floor ran from 0% to 35%. Two
  people playing the same game were not in the same kind of place. The scale is
  71 metres now and the failure threshold moved from a fifth of the world to a
  tenth, which measures 7% to 19% across ten seeds — dead sections you come
  across, rather than a world that is dark as often as not. The ambient floor
  went from 0.055 to 0.20 as well, so an unpowered corridor is gloom you reach
  for the torch in rather than an unlit screen.
- **`fixtureAt` and `sampleChunk` disagreed about where the lights hang.** The
  placement rule was written out twice, and when the bulk sampler learned to
  snap onto floor the single-cell query kept the old lattice. Third time this
  exact shape of bug has surfaced — the doorway rule, the two media3 artifacts,
  now the fittings — so it has an assertion of its own: Level_0_Check compares
  the two answers over every cell of twenty-five chunks per seed. The
  pitch-black assertion that had been in that file since the lighting rewrite
  never once fired, because it tested whether illuminance was under 0.02 while
  the ambient floor was 0.055. It is joined by one that measures what a player
  can actually see, with the bound set by re-injecting each fault and checking
  it crosses.
- **The flashlight swung the wrong way in first person.** The torch's world
  position was built from a forward vector with two of its three components
  negated relative to the one the camera itself was built from, so looking up
  sent the beam down and looking left sent it right. Third person read the
  avatar's own transform and was never wrong, which is why only half the game
  showed it. Both now come from the same basis. The cone is wider and the
  falloff gentler as well — it was a torch you had to aim at a wall to know was
  on.
- **Turning was about three times too fast, on every phone by a different
  amount.** The look delta went into `cameraLook` as raw pixels and came out as
  degrees, so a swipe across a 1080p screen was over 500 degrees of yaw at the
  default sensitivity, and the same gesture on a denser screen turned further
  still. It is measured in dp now, at 0.42 degrees each. Assets_Check simulates
  a full-width swipe at both ends of the slider and fails if the default leaves
  a quarter turn to three quarters — and it caught, on its first run, that the
  pause menu's slider ran to 4.0 while the settings screen's ran to 3.0.
- **The VHS effect stayed on when it was switched off.** The setting gated the
  shader's grain and chroma terms, but the scanline overlay was a separate
  Compose layer drawn unconditionally over the game — so the most visible part
  of the effect ignored the switch. There was also a `GameState.vhsEnabled`
  that returned a hardcoded `true`, shadowing the real value for anything that
  read it off the game state rather than the settings state.
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
