[English](README.md) ·
[Türkçe](README.tr.md) ·
[Deutsch](README.de.md) ·
[Español](README.es.md) ·
**Français** ·
[Italiano](README.it.md) ·
[Português](README.pt.md) ·
[Русский](README.ru.md) ·
[日本語](README.ja.md) ·
[中文](README.zh.md)

# Omni Backrooms

Un jeu d'horreur et de survie Android situé au Niveau 0 : des couloirs de bureau
jaune monotone sans fin, une moquette humide, des néons qui bourdonnent, et une
chose là-dedans avec vous qu'on ne peut pas tuer.

Écrit de zéro : le rendu est en OpenGL ES 3.0 piloté depuis Kotlin, la
simulation est en C++ via le NDK, et le niveau n'est pas un fichier de carte
mais une fonction pure des coordonnées de cellule — il ne finit jamais et ne
répète aucune jointure.

## Ce qu'il y a dedans

| | |
|---|---|
| **Niveau 0** | Infini. Chaque cellule — sol, mur, lumière, humidité — se déduit de ses propres coordonnées et de la graine de la partie : le monde est donc identique pour deux joueurs qui n'en échangent pas un seul octet. |
| **Une créature** | Pas une foule. Elle voit par lancer de rayon, donc les murs vous cachent réellement ; elle entend selon le bruit que vous faites, donc s'accroupir sert vraiment à quelque chose ; et elle se souvient de l'endroit où elle vous a vu en dernier. |
| **La lampe** | La ralentit, puis la chasse. Elle ne la tue pas. Rien ne meurt dans les Backrooms : elle se retire, se dissipe, attend à distance et revient quand elle vous revoit ou vous entend devenir imprudent. |
| **Aucun fichier audio** | Chaque son est synthétisé sur l'appareil. L'APK ne contient pas un seul WAV, pas un OGG, rien. |
| **Dix langues** | Turc, anglais, allemand, espagnol, français, italien, portugais, russe, japonais, chinois — complètes, pas partielles. Le jeu choisit celle de votre appareil au premier lancement. |
| **Cosmétique uniquement** | Cadres, traces et personnages. Rien de ce qui est vendu dans le jeu ne change la façon dont on y joue. |

## Le compiler

```bash
git clone https://github.com/Eaquel/Omni_Backrooms.git
cd Omni_Backrooms
./gradlew :Backrooms:assembleRelease
```

Il vous faut le JDK 25, le SDK Android 36, le NDK et CMake 4.3.2. Les builds de
release sont signés avec un keystore absent de ce dépôt ; `assembleDebug` n'a
besoin de rien de plus.

## Les vérifications

Sept des huit outils de `Tools/` s'exécutent à chaque push. Ils existent parce
que chacun protège quelque chose que la compilation Gradle ne peut tout
simplement pas voir :

| Outil | Ce qu'il attrape |
|---|---|
| `Shaders_Check.py` | Le GLSL vit à l'intérieur de chaînes brutes Kotlin : un shader qui ne compile pas reste invisible jusqu'à ce que l'écran qui l'utilise s'ouvre et reste noir. Chacun est compilé avec `glslangValidator`. |
| `Assets_Check.py` | Des icônes vectorielles écrites à la main qu'`aapt2` accepte et dessine de travers ; des UV de maillage qui ne correspondent plus à la position monde ; la caméra d'inspection qui sort de son décor ; des ressources dupliquées ou jamais référencées ; une langue restée en arrière ; le déguisement Unity qui se contredit. Et aussi `--optimise`, un ré-encodeur PNG sans perte. |
| `Native_Check.py` | Le contrat JNI. Kotlin déclare `external fun`, le C++ définit `Java_..._name`, et **rien** ne relie les deux à la compilation — ni le compilateur Kotlin, ni celui du C++, ni l'éditeur de liens. Un renommage d'un seul côté donne un `UnsatisfiedLinkError` au premier appel ; un nombre d'arguments modifié est pire, car JNI lie par le nom et lit les arguments en trop sur la pile sans broncher. |
| `Kotlin_Check.py` | Chaque import face à la dépendance qui le porte, dans les deux sens. Le Kotlin ici compile sans le classpath Android : une bibliothèque réellement supprimée ressemble à s'y méprendre à une simplement absente du chemin — c'est ainsi que retirer Firebase a emporté `androidx.media3`. |
| `Level_0_Check.py` | Inonde le monde depuis le point d'arrivée sur de nombreuses graines et prouve que la sortie est atteignable. Une sortie inatteignable est une partie ingagnable, et c'est parfaitement silencieux. |
| `Entity_Check.py` | Compile la vraie IA, place une créature dans le vrai Niveau 0 et regarde : la vue bloquée par les murs, l'ouïe qui suit le bruit, et le cycle retraite-retour qui ne doit jamais se bloquer. |
| `Code_To_Sound.py` | Restitue les générateurs C++ effectivement livrés et les compare échantillon par échantillon à une référence Python. Écrit aussi des WAV, pour que des sons qui n'existent que sous forme de code puissent vraiment être écoutés. |

Tout lancer :

```bash
for t in Shaders Assets Native Entity Kotlin; do python3 Tools/${t}_Check.py; done
python3 Tools/Level_0_Check.py 40
python3 Tools/Code_To_Sound.py
```

Chaque vérification ici a été validée en y remettant son bug. Une vérification
qui n'a jamais échoué ne donne à personne une raison de lui faire confiance.

## Organisation

```
Backrooms/Source/Main/
  Kotlin/com/omni/backrooms/     interface, rendu, boucle de jeu  (~14k lignes)
  Native/                        C++ via le NDK                   (~3,9k lignes)
    Map/        le Niveau 0 comme fonction pure des coordonnées
    Entity/     IA de la créature — perception, retraite, retour
    Sound/      tous les générateurs ; il n'y a aucun fichier audio
    Ending/     la fin d'une partie, fonction pure du temps
    Frame/      cosmétiques de cadre de profil
    Trail/      cosmétiques de traces de pas
    Shield/     les détecteurs, et ce que le binaire prétend être
  Assets/                        textures, maillages, histoire
  res/values*/                   dix langues
Tools/                           les huit vérifications
```

## Corrections récentes

Les plus récentes en premier. Cette liste est mise à jour à chaque correction.

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
- **Firebase n'a jamais fonctionné, et a emporté beaucoup avec lui.** Il n'y a
  pas de google-services.json ici et la CI injecte un substitut : chaque journal
  Crashlytics, chaque écriture Firestore et chaque lecture Remote Config
  échouait à l'exécution dans un `runCatching` qui l'avalait. L'API REST était
  la même histoire sur api.omnibackrooms.com, qui ne résout pas, et le netcode
  en dessous vidait une socket où personne n'écrivait — chat vocal compris.
  Tout est parti, avec Room, Billing et le Credential Manager, que rien
  n'utilisait.
- **La lampe était un cercle au milieu de l'écran.** Dessiné en uv (0.5, 0.47)
  dans la passe post, sans position dans le monde : d'où la lumière qui semblait
  sortir de sa poitrine. C'est un vrai spot dans le shader de scène désormais,
  depuis la lentille du modèle.
- **Les traces possédées ne pouvaient pas être équipées.** Trois fautes de
  suite.
- **La permission de notification était demandée par-dessus l'intro.** La porte
  était à côté du NavHost au lieu d'être dedans.
- **Deux textures n'étaient pas des puissances de deux.** 1536x1024 et
  1448x1086, donc pas de chaîne de mipmaps. Les quatre font 1024x1024 ; les
  ressources passent de 6,0 Mo à 4,7 Mo.
- **Le personnage avait quatre bras.** Le maillage en contenait deux paires : un
  corps aux bras le long du buste, et une robe dont les manches partaient droit
  en T-pose. Les os avaient été posés sur les manches, si bien que le rig
  agitait du tissu vide pendant que les bras visibles restaient soudés aux
  hanches. Les manches sont sur les bras désormais, et la liaison mesure le long
  de la surface plutôt qu'à travers l'air — l'ourlet de la jupe passe à 4 cm de
  la main, et aucune mesure en ligne droite ne les sépare. Huit coques
  dupliquées à un millimètre sont parties avec : 1139 sommets et leur z-fighting.
- **Le Niveau 0 contenait une foule.** De trois à huit créatures, réapprovisionnées
  toutes les douze secondes. Une foule est occupée, pas effrayante. Il n'y en a
  plus qu'une, et la difficulté change laquelle c'est, pas combien il y en a.
- **Les créatures voyaient à travers les murs.** La vue était un test de
  distance qui ignorait totalement le niveau : le seul moyen de rompre le
  contact était de la distancer.
- **En chasser une la supprimait définitivement.** La retraite mesurait sa
  distance depuis la position courante du joueur — la suivre la maintenait donc
  en fuite indéfiniment — et l'état en attente réinitialisait sa dissipation à
  chaque tick, si bien que le retour ne pouvait jamais aboutir. Les deux
  trouvés par simulation, aucun trouvable sur un appareil.
- **Huit pour cent de tout bruit synthétisé était un échantillon répété.** Le
  C++ comme le Python prenaient l'indice de bruit comme `int(t * 44100)`, et en
  flottant `i/44100*44100` tombe d'un cheveu sous `i`. Audible, invisible sur
  une forme d'onde.
- **Les joueurs turcs voyaient un `%d` littéral** sur l'étiquette de taille de
  salon : la chaîne portait un spécificateur de format et était dessinée sans
  argument.
- **Les ressources par défaut étaient en turc.** `values/` est ce vers quoi
  Android se rabat pour une langue sans entrée propre : toute chaîne non
  traduite apparaissait donc en turc au milieu d'un menu allemand. C'est
  l'anglais qui s'y trouve désormais.
- **La CI signalait un échec sur du code vert.** Les deux jobs se disputaient
  l'unique exécuteur ; les vérifications statiques ne démarraient jamais,
  expiraient dans la file et faisaient échouer l'exécution alors que l'APK se
  construisait parfaitement à chaque fois.
- **Le personnage semblait avoir quatre bras.** Le rig multipliait l'angle de
  rotation par un gradient de position, ce qui étale un membre au lieu de le
  faire tourner. Remplacé par du vrai linear blend skinning sur un squelette de
  douze os.
- **La protection anti-altération accusait des appareils sains** à chaque
  lancement, à cause d'une simple recherche de sous-chaîne dans
  `/proc/self/maps`. Elle indique maintenant ce qu'elle a trouvé et écrit la
  raison dans `Documents/Backrooms_Log/`.
- **Les textures de plafond étaient inversées** selon la diagonale de chaque
  dalle : l'émetteur distribuait les UV dans un ordre de coins fixe, qui n'est
  correct que pour un quadrilatère enroulé dans l'autre sens.

## Licence

Tous droits réservés. Le code est ici pour être lu.
