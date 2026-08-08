[English](README.md) ·
[Türkçe](README.tr.md) ·
[Deutsch](README.de.md) ·
**Español** ·
[Français](README.fr.md) ·
[Italiano](README.it.md) ·
[Português](README.pt.md) ·
[Русский](README.ru.md) ·
[日本語](README.ja.md) ·
[中文](README.zh.md)

# Omni Backrooms

Un juego de terror y supervivencia para Android ambientado en el Nivel 0:
pasillos de oficina de un amarillo monótono e interminable, moqueta húmeda,
fluorescentes que zumban y una cosa ahí dentro contigo que no se puede matar.

Escrito desde cero: el renderizador es OpenGL ES 3.0 gobernado desde Kotlin, la
simulación es C++ a través del NDK, y el nivel no es un archivo de mapa sino una
función pura de coordenadas de celda, así que no acaba nunca y no repite ni una
costura.

## Qué hay aquí

| | |
|---|---|
| **Nivel 0** | Infinito. Cada celda —suelo, pared, luz, humedad— se deriva de sus propias coordenadas y de la semilla de la partida, de modo que el mundo es idéntico para dos jugadores que no intercambian ni un byte de él. |
| **Una criatura** | No una multitud. Ve por trazado de rayos, así que las paredes te esconden de verdad; oye según el ruido que haces, así que agacharse sirve realmente para algo; y recuerda dónde te vio por última vez. |
| **La linterna** | La frena y luego la ahuyenta. No la mata. En los Backrooms no muere nada: se retira, se desvanece, espera a distancia y vuelve cuando te ve otra vez o te oye descuidarte. |
| **Sin archivos de audio** | Todo sonido se sintetiza en el dispositivo. El APK no contiene ni un WAV, ni un OGG, nada. |
| **Diez idiomas** | Turco, inglés, alemán, español, francés, italiano, portugués, ruso, japonés y chino: completos, no a medias. El juego elige el de tu dispositivo al primer arranque. |
| **Solo estética** | Marcos, rastros y personajes. Nada de lo que se vende en el juego afecta a cómo se juega. |

## Compilarlo

```bash
git clone https://github.com/Eaquel/Omni_Backrooms.git
cd Omni_Backrooms
./gradlew :Backrooms:assembleRelease
```

Necesitas JDK 25, Android SDK 36, el NDK y CMake 4.3.2. Las compilaciones de
release se firman con un almacén de claves que no está en este repositorio;
`assembleDebug` no necesita nada más.

## Las comprobaciones

Siete de las ocho herramientas de `Tools/` se ejecutan en cada push. Existen
porque cada una protege algo que la compilación de Gradle sencillamente no puede
ver:

| Herramienta | Qué detecta |
|---|---|
| `Shaders_Check.py` | El GLSL vive dentro de cadenas literales de Kotlin, así que un shader que no compila es invisible hasta que se abre la pantalla que lo usa y se queda en negro. Todos se compilan con `glslangValidator`. |
| `Assets_Check.py` | Iconos vectoriales escritos a mano que `aapt2` acepta y dibuja mal; UVs de malla que ya no coinciden con la posición en el mundo; la cámara de inspección saliéndose del fondo; recursos duplicados y nunca referenciados; un idioma que se ha quedado atrás; el disfraz de Unity contradiciéndose. También `--optimise`, un recodificador PNG sin pérdida. |
| `Native_Check.py` | El contrato JNI. Kotlin declara `external fun`, C++ define `Java_..._name`, y en tiempo de compilación **nada** conecta ambos lados: ni el compilador de Kotlin, ni el de C++, ni el enlazador. Un renombrado en un solo lado es un `UnsatisfiedLinkError` en la primera llamada; cambiar el número de argumentos es peor, porque JNI enlaza por nombre y lee los argumentos sobrantes de la pila sin quejarse. |
| `Kotlin_Check.py` | Cada import contra la dependencia que lo respalda, en ambos sentidos. El Kotlin aquí compila sin el classpath de Android, así que una biblioteca realmente eliminada es idéntica a una que solo no está en la ruta: así quitar Firebase se llevó `androidx.media3` sin decir nada. |
| `Level_0_Check.py` | Inunda el mundo desde el punto de aparición con muchas semillas y demuestra que la salida es alcanzable. Una salida inalcanzable es una partida imposible de ganar, y es completamente silenciosa. |
| `Entity_Check.py` | Compila la IA real, pone una criatura en el Nivel 0 real y observa: visión bloqueada por paredes, oído que escala con el ruido, y el ciclo de retirada y regreso que jamás debe bloquearse. |
| `Code_To_Sound.py` | Renderiza los generadores de C++ que se distribuyen y los compara muestra a muestra con una referencia en Python. También escribe WAVs, para que sonidos que solo existen como código puedan escucharse de verdad. |

Ejecutarlas todas:

```bash
for t in Shaders Assets Native Entity Kotlin; do python3 Tools/${t}_Check.py; done
python3 Tools/Level_0_Check.py 40
python3 Tools/Code_To_Sound.py
```

Cada comprobación de aquí se verificó volviendo a introducir su fallo. Una
comprobación que nunca ha fallado no le da a nadie ningún motivo para fiarse
de ella.

## Estructura

```
Backrooms/Source/Main/
  Kotlin/com/omni/backrooms/     interfaz, renderizador, bucle  (~14k líneas)
  Native/                        C++ a través del NDK           (~3,9k líneas)
    Map/        el Nivel 0 como función pura de coordenadas
    Entity/     IA de la criatura — percepción, retirada, regreso
    Sound/      todos los generadores; no hay archivos de audio
    Ending/     cómo termina una partida, función pura del tiempo
    Frame/      cosméticos de marco de perfil
    Trail/      cosméticos de rastro de pisadas
    Shield/     los detectores, y lo que el binario aparenta ser
  Assets/                        texturas, mallas, historia
  res/values*/                   diez idiomas
Tools/                           las ocho comprobaciones
```

## Correcciones recientes

Lo más nuevo primero. Esta lista se actualiza con cada corrección.

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
- **Firebase nunca funcionó, y se llevó mucho consigo.** Aquí no hay
  google-services.json y CI inyecta un marcador de posición, así que cada log de
  Crashlytics, escritura en Firestore y consulta de Remote Config fallaba en
  ejecución dentro de un `runCatching` que se lo tragaba. La API REST era lo
  mismo en api.omnibackrooms.com, que no resuelve, y el netcode debajo vaciaba
  un socket al que nadie enviaba —chat de voz incluido—. Todo fuera, junto con
  Room, Billing y Credential Manager, que nada referenciaba.
- **La linterna era un círculo en el centro de la pantalla.** Dibujado en uv
  (0.5, 0.47) en el pase de post, sin posición en el mundo: por eso la luz
  parecía salir de su pecho. Ahora es un foco real en el shader de escena, desde
  la lente del modelo.
- **Los rastros en posesión no se podían equipar.** Tres fallos seguidos.
- **El permiso de notificaciones se pedía sobre la intro.** La puerta estaba
  junto al NavHost en vez de dentro.
- **Dos texturas no eran potencia de dos.** 1536x1024 y 1448x1086, sin cadena de
  mipmaps. Las cuatro son 1024x1024; los recursos pasan de 6,0 MB a 4,7 MB.
- **El personaje tenía cuatro brazos.** La malla contenía dos pares: un cuerpo
  con los brazos a los costados y un vestido cuyas mangas salían rectas en
  T-pose. Los huesos se habían colocado sobre las mangas, así que el rig
  agitaba tela vacía mientras los brazos que se ven seguían soldados a la
  cadera. Ahora las mangas están sobre los brazos, y el vínculo mide a lo largo
  de la superficie en vez de por el aire: el borde de la falda pasa a 4 cm de la
  mano, y ninguna medida en línea recta distingue una de otra. Con ello se
  fueron ocho cascarones duplicados a un milímetro: 1139 vértices y su z-fighting.
- **El Nivel 0 tenía una multitud.** De tres a ocho criaturas, repuestas cada
  doce segundos. Una multitud está ocupada, no da miedo. Ahora hay exactamente
  una, y la dificultad cambia cuál es esa una, no cuántas hay.
- **Las criaturas veían a través de las paredes.** La visión era una prueba de
  distancia que ignoraba el nivel por completo, así que la única forma de
  romper el contacto era correr más que ella.
- **Ahuyentar a una la eliminaba para siempre.** La retirada medía su distancia
  desde la posición actual del jugador, así que seguirla la mantenía huyendo
  eternamente; y el estado en reposo reiniciaba su desvanecimiento en cada tick,
  así que el regreso nunca podía completarse. Ambos hallados por simulación,
  ninguno localizable en un dispositivo.
- **El ocho por ciento de todo ruido sintetizado era una muestra repetida.**
  Tanto el C++ como el Python tomaban el índice de ruido como `int(t * 44100)`,
  y en coma flotante `i/44100*44100` cae un pelo por debajo de `i`. Audible,
  invisible en una forma de onda.
- **Los jugadores turcos veían un `%d` literal** en la etiqueta del tamaño de
  sala: la cadena llevaba un especificador de formato y se dibujaba sin
  argumento.
- **Los recursos por defecto estaban en turco.** `values/` es a lo que Android
  recurre para un idioma sin entrada propia, así que cualquier cadena sin
  traducir aparecía en turco en medio de un menú alemán. Ahora ahí está el
  inglés.
- **CI informaba de fallo con el código en verde.** Los dos trabajos competían
  por el único ejecutor; las comprobaciones estáticas nunca arrancaban, agotaban
  su tiempo en la cola y hacían fracasar la ejecución mientras el APK se
  compilaba perfectamente cada vez.
- **El personaje parecía tener cuatro brazos.** El esqueleto multiplicaba el
  ángulo de rotación por un gradiente de posición, lo que abanica un miembro en
  lugar de girarlo. Sustituido por linear blend skinning real sobre un esqueleto
  de doce huesos.
- **La protección antimanipulación acusaba a dispositivos limpios** en cada
  arranque, por una búsqueda de subcadena sin más en `/proc/self/maps`. Ahora
  informa de qué ha encontrado y escribe el motivo en `Documents/Backrooms_Log/`.
- **Las texturas del techo estaban reflejadas** en la diagonal de cada baldosa:
  el emisor entregaba las UVs en un orden fijo de esquinas, que solo es correcto
  para un cuadrilátero enrollado al revés.

## Licencia

Todos los derechos reservados. El código está aquí para leerse.
