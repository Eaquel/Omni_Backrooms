[English](README.md) ·
[Türkçe](README.tr.md) ·
**Deutsch** ·
[Español](README.es.md) ·
[Français](README.fr.md) ·
[Italiano](README.it.md) ·
[Português](README.pt.md) ·
[Русский](README.ru.md) ·
[日本語](README.ja.md) ·
[中文](README.zh.md)

# Omni Backrooms

Ein Android-Survival-Horror-Spiel auf Ebene 0 — endlose eintönig gelbe
Bürokorridore, feuchter Teppich, brummende Leuchtstoffröhren und etwas darin
bei Ihnen, das sich nicht töten lässt.

Von Grund auf geschrieben: der Renderer ist OpenGL ES 3.0, von Kotlin
angesteuert, die Simulation ist C++ über das NDK, und die Ebene ist keine
Kartendatei, sondern eine reine Funktion von Zellkoordinaten — sie endet nie und
wiederholt keine einzige Naht.

## Was drin ist

| | |
|---|---|
| **Ebene 0** | Unendlich. Jede Zelle — Boden, Wand, Licht, Feuchtigkeit — leitet sich aus ihren eigenen Koordinaten und dem Seed des Durchgangs ab. Die Welt ist damit für zwei Spieler identisch, die kein einziges Byte davon austauschen. |
| **Eine Kreatur** | Keine Menge. Sie sieht per Strahl, Wände verstecken Sie also wirklich; sie hört nach dem, wie laut Sie sind, Ducken bringt also tatsächlich etwas; und sie merkt sich, wo sie Sie zuletzt gesehen hat. |
| **Die Taschenlampe** | Verlangsamt sie, dann vertreibt sie sie. Sie tötet sie nicht. In den Backrooms stirbt nichts — sie zieht sich zurück, verblasst, wartet auf Abstand und kommt wieder, wenn sie Sie erneut sieht oder unvorsichtig werden hört. |
| **Keine Audiodateien** | Jeder Klang wird auf dem Gerät synthetisiert. Im APK ist keine einzige WAV, keine OGG, nichts. |
| **Zehn Sprachen** | Türkisch, Englisch, Deutsch, Spanisch, Französisch, Italienisch, Portugiesisch, Russisch, Japanisch, Chinesisch — vollständig, nicht teilweise. Das Spiel wählt beim ersten Start die Ihres Geräts. |
| **Nur Kosmetik** | Rahmen, Spuren und Charaktere. Nichts, was irgendwo im Spiel verkauft wird, beeinflusst, wie es sich spielt. |

## Bauen

```bash
git clone https://github.com/Eaquel/Omni_Backrooms.git
cd Omni_Backrooms
./gradlew :Backrooms:assembleRelease
```

Sie brauchen JDK 25, Android SDK 36, das NDK und CMake 4.3.2. Release-Builds
werden mit einem Keystore signiert, der nicht in diesem Repository liegt;
`assembleDebug` braucht nichts zusätzlich.

## Die Prüfungen

Sieben der acht Werkzeuge in `Tools/` laufen bei jedem Push. Es gibt sie, weil
jedes etwas absichert, das der Gradle-Build schlicht nicht sehen kann:

| Werkzeug | Was es findet |
|---|---|
| `Shaders_Check.py` | GLSL steht in Kotlin-Rohstrings. Ein Shader, der nicht kompiliert, bleibt unsichtbar, bis der Bildschirm, der ihn nutzt, sich öffnet und schwarz bleibt. Jeder wird mit `glslangValidator` übersetzt. |
| `Assets_Check.py` | Handgeschriebene Vektor-Icons, die `aapt2` annimmt und verzerrt zeichnet; Mesh-UVs, die nicht mehr zur Weltposition passen; die Inspektionskamera, die ihren Hintergrund verlässt; doppelte und nie referenzierte Assets; eine zurückgefallene Sprache; eine Unity-Tarnung, die sich selbst widerspricht. Außerdem `--optimise`, ein verlustfreier PNG-Neucodierer. |
| `Native_Check.py` | Der JNI-Vertrag. Kotlin deklariert `external fun`, C++ definiert `Java_..._name`, und zur Bauzeit verbindet die beiden **nichts** — weder der Kotlin-Compiler noch der C++-Compiler noch der Linker. Eine einseitige Umbenennung ist ein `UnsatisfiedLinkError` beim ersten Aufruf; eine geänderte Argumentzahl ist schlimmer, denn JNI bindet über den Namen und liest die überzähligen Argumente kommentarlos vom Stack. |
| `Kotlin_Check.py` | Jeden Import gegen die Abhängigkeit dahinter, in beide Richtungen. Das Kotlin hier kompiliert ohne Android-Classpath, also sieht eine wirklich entfernte Bibliothek genauso aus wie eine, die nur nicht im Pfad liegt — so nahm das Entfernen von Firebase still `androidx.media3` mit. |
| `Level_0_Check.py` | Flutet die Welt vom Startpunkt aus über viele Seeds und beweist, dass der Ausgang erreichbar ist. Ein unerreichbarer Ausgang ist ein ungewinnbarer Durchgang, und er ist völlig lautlos. |
| `Entity_Check.py` | Kompiliert die echte KI, setzt eine Kreatur in die echte Ebene 0 und schaut zu: von Wänden blockierte Sicht, mit Lautstärke skalierendes Gehör, der Rückzug-und-Rückkehr-Zyklus, der niemals hängen bleiben darf. |
| `Code_To_Sound.py` | Rendert die ausgelieferten C++-Generatoren und vergleicht sie Sample für Sample mit einer Python-Referenz. Schreibt außerdem WAVs, damit Klänge, die nur als Code existieren, tatsächlich hörbar werden. |

Alle ausführen:

```bash
for t in Shaders Assets Native Entity Kotlin; do python3 Tools/${t}_Check.py; done
python3 Tools/Level_0_Check.py 40
python3 Tools/Code_To_Sound.py
```

Jede Prüfung hier wurde verifiziert, indem ihr Fehler wieder eingebaut wurde.
Eine Prüfung, die nie fehlgeschlagen ist, gibt niemandem einen Grund, ihr zu
trauen.

## Aufbau

```
Backrooms/Source/Main/
  Kotlin/com/omni/backrooms/     UI, Renderer, Spielschleife  (~14k Zeilen)
  Native/                        C++ über das NDK             (~3,9k Zeilen)
    Map/        Ebene 0 als reine Funktion von Koordinaten
    Entity/     Kreatur-KI — Wahrnehmung, Rückzug, Rückkehr
    Sound/      alle Generatoren; es gibt keine Audiodateien
    Ending/     wie ein Lauf endet, als reine Funktion der Zeit
    Frame/      Profilrahmen-Kosmetik
    Trail/      Fußspuren-Kosmetik
    Shield/     die Detektoren, und als was sich die Binärdatei ausgibt
  Assets/                        Texturen, Meshes, Story
  res/values*/                   zehn Sprachen
Tools/                           die acht Prüfungen
```

## Zuletzt behoben

Neuestes zuerst. Diese Liste wird bei jeder Korrektur ergänzt.

- **The walls, floor and ceiling are generated now, and the three images are
  gone** — 4.6 MB for a flat colour with grain on it. The shader reproduces the
  measured mean and grain of each file, at the hue of the lobby background clip
  (a ratio of 1.00, 0.80, 0.42, warmer than the walls' old 1.00, 0.90, 0.34).
  It never repeats and cannot be seen to tile.
- **Walking went "dit dit."** The footstep put its energy at 1.1 kHz and was
  over in 53 ms — a click, not a step — and every footfall of a walk was the
  same waveform on a metronome. Now heel and toe, a low body under a low-passed
  scuff, and a step index that varies pitch, decay and level: 1131 Hz to 67 Hz,
  53 ms to 134 ms, consecutive steps from 0.000 apart to 0.549.
- **The exit was never where the level said it was.** The door is placed
  352-544 m out and the leash that re-anchors it was 320 m, so on 40 of 40 seeds
  it was pulled in to 147 m before the player moved. The authored run length had
  never been played.
- **The ending sampler broke the release build.** `OmniGLRenderer` holds a
  Context and nothing else; everything it needs arrives as a provider lambda.
  The new code called `bridge.endingParams` from inside it, and there is no
  `bridge` there. Every static check passed and Gradle failed. Kotlin_Check
  cannot see this — without the Android classpath `bridge` is indistinguishable
  from the thousands of symbols unresolved because a jar is missing — so the
  architectural rule got stated instead: the GL renderer does not reach into the
  view model.
- **The end of a run was a dialog on a black rectangle.** Both screens painted
  the level over at 88% black and put a card on top, which threw away the only
  frame that mattered. A new `Native/Ending/` turns (which ending, seconds in)
  into the eight post-process parameters the transition is made of: a death
  drains the colour, splits the channels, tears rows out in bursts, pulls the
  frame toward the middle and only then shuts down; an escape is the opposite
  curve in every term. The stats panel rises on the last of those same numbers,
  so it cannot appear before the picture has finished failing.
- **The transition got a check rather than an opinion**, since you have to die
  to see it. Native_Check samples both endings on the host and asserts that the
  first frame of an ending is the frame before it, that the panel is not half up
  before 55% through, that neither runs under 1.2s or over 3.5s, and that a
  death never brightens while an escape does. All four verified by injection.
- **Three of the four sounds in the game had never been played.**
  `fluorescentHum`, `footstep` and `monsterVoice` — the three the sound tool
  renders and compares against a Python reference sample for sample — had no
  caller anywhere in the engine. What played instead was a cruder set written
  inline in `Engine.cpp`, including an ambience layer of unfiltered white noise
  from a `std::mt19937`, which is not deterministic. The tool was verifying
  sounds nobody had heard while unchecked ones played. Fourth time one rule has
  lived in two copies here with only one checked, and the first time the checked
  copy was the dead one.
- **Four new sounds.** Room tone with a beating drone, low-passed air and a drip
  whose wetness follows the mains; breathing with different shapes in and out;
  a two-part heartbeat; and a torch switch. Eleven generators now agree with
  their C++ to within 1e-6, and the tool fails if any of them has no caller.
- **Footsteps never stopped** once triggered, so letting go of the stick left
  her walking on the spot.
- **The body came through the dress, and the legs had no calves.** Neither is
  a structural fault — the file parses, the shells are closed, the rig survives
  its poses — so nothing could see them. Skin was outside the fabric in 10 of 34
  sampled directions round the trunk, worst by 16 mm, and the body carried its
  own moulded skirt hanging below the real hem. The torso is tucked to a 6 mm
  clearance inside the garment now. One leg measured 68 mm at calf height
  against 63 at the ankle, a ratio of 1.08 where a real leg is nearer 1.6; the
  calf is 87 mm now, with the swell swept against the rig's worst skinning seam
  so the improvement costs nothing.
- **The room was the right size and everything in it was twice as big.** The
  ceiling grid was on a 1.6 m module where a metric suspended ceiling is
  600 mm, the carpet tiles 800 mm against a real 500, the wall joints 1.6 m
  against an 800 mm paper drop. The light fittings called themselves 2x4
  troffers and were built at double that, from fractions of the cell rather
  than in metres. Level 0's own dimensions were never wrong — 3.2 m cells and a
  2.6 m ceiling are ordinary office numbers — but the grid overhead is the
  strongest cue the eye has for the size of a space, and it was counting five
  tiles across a corridor that should show eight. The walls also have a
  skirting board now, which they never had.
- **The check watching the level's darkness was the wrong shape.** A per-seed
  bound tuned on eight seeds failed four of twenty. It asserts on the
  distribution now — median, p90, and the gap between the darkest and
  brightest seed. That last one matters: the old mains-failure noise gives a
  better median than the shipped level and is still broken, because its seeds
  range from 1.7% to 67.5%.
- **Most of the level was pitch black.** Ceiling fittings sat on a global
  lattice — a tube where both coordinates were multiples of four — so whether a
  corridor was lit came down to its coordinate parity, and a corridor that
  missed the lattice got nothing along its whole length. 54% of open floor was
  under 0.08 illuminance and the longest walk you could not see a step of was
  192 metres. Fittings now snap onto floor, one per four-by-four block, and the
  falloff is wide enough that adjacent pools meet. Nothing renders black now.
- **One seed was a lit lobby and the next a third pitch dark.** Mains failure
  used noise one wavelength every 178 metres, so the unpowered share of the
  floor ran from 0% to 35% depending on the seed. At 71 metres and a tenth of
  the world it measures 7% to 19%, and the ambient floor rose from 0.055 to
  0.20 so a dead section is gloom rather than an unlit screen.
- **`fixtureAt` and `sampleChunk` disagreed about where the lights hang** — the
  placement rule written twice, and only one copy updated. Third time this
  shape of bug has appeared, so Level_0_Check now compares the two answers
  cell by cell. The pitch-black assertion it already had tested illuminance
  against 0.02 while the ambient floor was 0.055, so it had never fired.
- **The flashlight swung the wrong way in first person.** Its world position
  came from a forward vector with two components negated relative to the
  camera's own, so looking up sent the beam down. Third person read the
  avatar's transform and was never wrong. Both use the same basis now, with a
  wider cone and gentler falloff.
- **Turning was about three times too fast, and phone-dependent.** The look
  delta went in as raw pixels and came out as degrees — over 500 degrees of yaw
  per swipe at the default. It is dp now, at 0.42 degrees each, and
  Assets_Check simulates a full swipe at both ends of the slider.
- **The VHS effect stayed on when switched off.** The setting gated the
  shader's grain and chroma, but the scanline overlay was a separate Compose
  layer drawn unconditionally — the most visible part of the effect ignored the
  switch.
- **Eight creatures became one, and the Smiler got a body.** Level 0 holds one
  thing you never get a good look at. The Smiler was a cut-out — one contour,
  the same thickness the whole way round — and is a drifting density field now,
  with the face multiplied by the smoke so it surfaces and swims with it. Seven
  behaviour trees in the native AI went with the seven creatures.
- **A texture hung in mid-air, and it was the doorway.** A single horizontal
  quad at 0.82 of the wall height, both long edges ending in open space.
- **Frames were drawn across the photo**, and tilted 0.62 rad so a circular
  frame projected as an ellipse around a circular picture.
- **All eight creatures were the Smiler**, separated only by a tint the shader
  multiplies by 0.055.
- **The third-person arrival had no camera** — the body collapsed and stood up
  in a corner of the frame at a flat 2.6 m.
- **Firebase hat nie funktioniert und nahm vieles mit.** Es gibt hier keine
  google-services.json, CI spielt einen Platzhalter ein: jedes Crashlytics-Log,
  jeder Firestore-Schreibvorgang und jeder Remote-Config-Abruf scheiterte zur
  Laufzeit in einem `runCatching`, das es verschluckte. Die REST-API war dasselbe
  unter api.omnibackrooms.com, das nicht auflöst, und der Netzcode darunter
  leerte einen Socket, an den niemand sendete — Sprachchat inklusive. Alles weg,
  samt Room, Billing und Credential Manager, die nichts referenzierte.
- **Die Taschenlampe war ein Kreis in der Bildmitte.** Bei uv (0.5, 0.47) im
  Post-Pass gezeichnet, ohne Position in der Welt — genau deshalb schien das
  Licht aus ihrer Brust zu kommen. Jetzt ein echter Spot im Szenen-Shader, aus
  der Linse des Lampenmodells.
- **Besessene Spuren ließen sich nicht anlegen.** Drei Fehler hintereinander.
- **Benachrichtigungen wurden über dem Intro erfragt.** Das Gate stand neben dem
  NavHost statt darin.
- **Zwei Texturen waren keine Zweierpotenzen.** 1536x1024 und 1448x1086, also
  keine Mipmap-Kette. Alle vier sind 1024x1024; Assets 6,0 MB auf 4,7 MB.
- **Die Figur hatte vier Arme.** Das Mesh enthielt zwei Paare: einen Körper mit
  den Armen an den Seiten und ein Kleid, dessen Ärmel in T-Pose gerade
  abstanden. Die Knochen lagen auf den Ärmeln, das Rig schwang also leeren Stoff,
  während die Arme, die man sieht, an der Hüfte festgeschweißt blieben. Die
  Ärmel liegen jetzt auf den Armen, und die Bindung misst entlang der Oberfläche
  statt durch die Luft — der Rocksaum verläuft 4 cm neben der Hand, und kein
  geradliniges Maß kann beide auseinanderhalten. Acht um einen Millimeter
  versetzt duplizierte Schalen gingen mit: 1139 Vertices und ihr Z-Fighting.
- **Ebene 0 enthielt eine Menge.** Drei bis acht Kreaturen, alle zwölf Sekunden
  aufgefüllt. Eine Menge ist geschäftig, nicht beängstigend. Jetzt genau eine,
  und die Schwierigkeit ändert, welche das ist, nicht wie viele es sind.
- **Kreaturen sahen durch Wände.** Sicht war ein Abstandstest, der die Ebene
  völlig ignorierte; man konnte den Kontakt nur abbrechen, indem man davonlief.
- **Eine zu vertreiben entfernte sie dauerhaft.** Der Rückzug maß seinen Abstand
  zur aktuellen Spielerposition, Verfolgen hielt sie also ewig auf der Flucht;
  und der geparkte Zustand setzte ihr Verblassen jeden Tick zurück, die Rückkehr
  konnte also nie fertig werden. Beides in der Simulation gefunden, beides auf
  einem Gerät nicht auffindbar.
- **Acht Prozent jedes synthetisierten Rauschens war ein wiederholtes Sample.**
  C++ wie Python nahmen den Rausch-Index als `int(t * 44100)`, und in float
  landet `i/44100*44100` ein Haar unter `i`. Hörbar, in einer Wellenform
  unsichtbar.
- **Türkische Spieler sahen ein wörtliches `%d`** auf dem Raumgrößen-Label: die
  Zeichenkette hatte einen Formatplatzhalter und wurde ohne Argument gezeichnet.
- **Die Standardressourcen waren Türkisch.** `values/` ist das, worauf Android
  für eine Sprache ohne eigenen Eintrag zurückfällt — jede nicht übersetzte
  Zeichenkette erschien also auf Türkisch mitten in einem deutschen Menü. Dort
  steht jetzt Englisch.
- **CI meldete Fehler bei fehlerfreiem Code.** Die beiden Jobs kämpften um den
  einen Runner; die statischen Prüfungen starteten nie, liefen in der
  Warteschlange in ihr Timeout und ließen den Lauf scheitern, während das APK
  jedes Mal einwandfrei gebaut wurde.
- **Die Figur wirkte vierarmig.** Das Rig multiplizierte den Drehwinkel mit
  einem Positionsgradienten, was ein Glied auffächert statt es zu drehen.
  Ersetzt durch echtes Linear Blend Skinning über ein Zwölf-Knochen-Skelett.
- **Der Manipulationsschutz beschuldigte saubere Geräte** bei jedem Start,
  wegen einer nackten Teilstring-Suche in `/proc/self/maps`. Er nennt jetzt,
  was er gefunden hat, und schreibt den Grund nach `Documents/Backrooms_Log/`.
- **Decken-Texturen waren an der Diagonale jeder Kachel gespiegelt**: der
  Emitter gab UVs in fester Eckreihenfolge aus, was nur für ein andersherum
  gewickeltes Viereck stimmt.

## Lizenz

Alle Rechte vorbehalten. Der Code steht hier, um gelesen zu werden.
