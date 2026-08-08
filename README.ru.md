[English](README.md) ·
[Türkçe](README.tr.md) ·
[Deutsch](README.de.md) ·
[Español](README.es.md) ·
[Français](README.fr.md) ·
[Italiano](README.it.md) ·
[Português](README.pt.md) ·
**Русский** ·
[日本語](README.ja.md) ·
[中文](README.zh.md)

# Omni Backrooms

Хоррор про выживание для Android, действие которого происходит на Уровне 0:
бесконечные однотонно-жёлтые офисные коридоры, сырой ковролин, гудящие лампы —
и то, что находится там вместе с вами и чего нельзя убить.

Написано с нуля: рендер — OpenGL ES 3.0, управляемый из Kotlin, симуляция — C++
через NDK, а уровень — не файл карты, а чистая функция координат ячейки. Поэтому
он не кончается и не повторяет ни одного стыка.

## Что здесь есть

| | |
|---|---|
| **Уровень 0** | Бесконечный. Каждая ячейка — пол, стена, свет, сырость — выводится из собственных координат и зерна забега, так что мир одинаков для двух игроков, не обменявшихся ни одним его байтом. |
| **Одно существо** | Не толпа. Оно видит лучом, поэтому стены действительно скрывают вас; слышит по тому, насколько вы шумите, поэтому приседать действительно имеет смысл; и помнит, где видело вас в последний раз. |
| **Фонарик** | Сначала замедляет его, потом прогоняет. Не убивает. В Backrooms ничто не умирает: оно отступает, растворяется, ждёт на расстоянии и возвращается, когда снова вас увидит или услышит вашу неосторожность. |
| **Ни одного аудиофайла** | Каждый звук синтезируется на устройстве. В APK нет ни одного WAV, ни одного OGG, ничего. |
| **Десять языков** | Турецкий, английский, немецкий, испанский, французский, итальянский, португальский, русский, японский, китайский — полностью, а не частично. При первом запуске игра сама выбирает язык устройства. |
| **Только косметика** | Рамки, следы и персонажи. Ничто из продаваемого в игре не влияет на то, как в неё играют. |

## Сборка

```bash
git clone https://github.com/Eaquel/Omni_Backrooms.git
cd Omni_Backrooms
./gradlew :Backrooms:assembleRelease
```

Нужны JDK 25, Android SDK 36, NDK и CMake 4.3.2. Релизные сборки подписываются
хранилищем ключей, которого нет в этом репозитории; для `assembleDebug` ничего
дополнительного не требуется.

## Проверки

Семь из восьми инструментов в `Tools/` запускаются при каждом push. Они
существуют потому, что каждый охраняет то, чего сборка Gradle попросту не видит:

| Инструмент | Что он ловит |
|---|---|
| `Shaders_Check.py` | GLSL живёт внутри «сырых» строк Kotlin, так что шейдер, который не компилируется, остаётся невидимым, пока не откроется использующий его экран и не останется чёрным. Каждый компилируется через `glslangValidator`. |
| `Assets_Check.py` | Написанные вручную векторные иконки, которые `aapt2` принимает и рисует криво; UV меша, переставшие совпадать с мировой позицией; камера осмотра, уходящая за задник; дублирующиеся и никем не используемые ресурсы; отставший язык; маскировка под Unity, противоречащая сама себе. Плюс `--optimise` — перекодировщик PNG без потерь. |
| `Native_Check.py` | Контракт JNI. Kotlin объявляет `external fun`, C++ определяет `Java_..._name`, и на этапе сборки эти две стороны **ничто** не связывает: ни компилятор Kotlin, ни компилятор C++, ни компоновщик. Переименование с одной стороны — это `UnsatisfiedLinkError` при первом вызове; изменившееся число аргументов хуже, потому что JNI связывает по имени и молча читает лишние аргументы со стека. |
| `Kotlin_Check.py` | Каждый импорт против зависимости за ним, в обе стороны. Kotlin здесь компилируется без Android-classpath, поэтому по-настоящему удалённая библиотека выглядит ровно как та, которой просто нет в пути, — так удаление Firebase тихо унесло `androidx.media3`. |
| `Level_0_Check.py` | Заливает мир от точки появления по множеству зёрен и доказывает, что выход достижим. Недостижимый выход — это непроходимый забег, и он абсолютно беззвучен. |
| `Entity_Check.py` | Компилирует настоящий ИИ, помещает существо в настоящий Уровень 0 и смотрит: зрение, перекрываемое стенами, слух, масштабируемый шумом, и цикл «отступить и вернуться», который никогда не должен заклинить. |
| `Code_To_Sound.py` | Прогоняет те самые C++-генераторы, которые уходят в сборку, и сравнивает их с эталоном на Python сэмпл за сэмплом. А ещё пишет WAV, чтобы звуки, существующие только как код, можно было действительно послушать. |

Запустить всё:

```bash
for t in Shaders Assets Native Entity Kotlin; do python3 Tools/${t}_Check.py; done
python3 Tools/Level_0_Check.py 40
python3 Tools/Code_To_Sound.py
```

Каждая проверка здесь была проверена тем, что в код возвращали её ошибку. У
проверки, которая ни разу не срабатывала, нет никаких оснований для доверия.

## Структура

```
Backrooms/Source/Main/
  Kotlin/com/omni/backrooms/     интерфейс, рендер, игровой цикл  (~14k строк)
  Native/                        C++ через NDK                    (~3,9k строк)
    Map/        Уровень 0 как чистая функция координат
    Entity/     ИИ существа — восприятие, отступление, возвращение
    Sound/      все генераторы; аудиофайлов нет
    Ending/     как заканчивается забег — чистая функция времени
    Frame/      косметика рамок профиля
    Trail/      косметика следов
    Shield/     детекторы, и то, за что бинарник себя выдаёт
  Assets/                        текстуры, меши, история
  res/values*/                   десять языков
Tools/                           восемь проверок
```

## Недавние исправления

Самое новое сверху. Список пополняется при каждом исправлении.

- **The ceiling was not too low; the lens was too wide.** 70 degrees went in as
  the VERTICAL field of view, which is 109 horizontal on a 2:1 phone where a
  normal game sits at 75-90. 52 now, and the ceiling 2.6 m to 3.0 m.
- **The light was coming from nowhere.** Ambient at 0.20 against a tube's 1.05
  lit every room whether it had a fitting or not. Ambient 0.085, a steeper
  shader response and brighter fittings: pool-to-gloom contrast 11.6x to 38.2x.
  The check's bound was 3, slack enough to pass the broken tuning; it is 18.
- **The check was measuring against a formula that no longer existed** — its
  thresholds were hand-computed from the shader's old lighting equation and
  written in as constants. It reads the real coefficients now.
- **Fog and the VHS filter default off, and the filter is at half strength.**
  `observeVhs()` also defaulted to true while `observe()` defaulted to false.
- **The draw distance was the horizon**: a 55 m far plane. 110 m now.
- **Door frames were built in 28% of corridor cells** — a header and two jambs
  each, so a corridor was a run of portals every few metres. Level 0 is an
  office floor with openings in its partitions, not a colonnade.
- **Some floor squares were unlit**, and were meant to be: a feature dimmed its
  cell to 34%, a hard-edged rectangle two thirds darker than everything touching
  it. It read as a tile that had failed to light.
- **The wall had lines ruled across it** every 800 mm at 74% brightness, and the
  carpet a grid at 80%. A paper seam is a shadow you notice when you look for
  it: 94% and 93% now.
- **Opening the menu mid-walk left her walking on the spot.** Footsteps run on
  their own interval in the audio callback and only the movement branch stopped
  them — a branch that does not run while paused.
- **The footprints pointed where the camera was, not where her feet are**: they
  were stamped with the raw camera yaw while the avatar is drawn at a smoothed
  yaw that chases it.
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
- **Firebase никогда не работал и утянул за собой многое.** Здесь нет
  google-services.json, а CI подставляет заглушку: каждая запись Crashlytics,
  каждая запись в Firestore и каждый запрос Remote Config падали в рантайме
  внутри `runCatching`, который это проглатывал. С REST API та же история по
  адресу api.omnibackrooms.com, который не резолвится, а netcode под ним
  опустошал сокет, в который никто не писал, — включая голосовой чат. Всё
  удалено, вместе с Room, Billing и Credential Manager, на которые не было ни
  одной ссылки.
- **Фонарик был кругом посреди экрана.** Рисовался в uv (0.5, 0.47) в
  пост-проходе и не имел положения в мире — именно поэтому свет выглядел
  исходящим у неё из груди. Теперь это настоящий прожектор в шейдере сцены, из
  линзы модели.
- **Купленные следы нельзя было надеть.** Три ошибки подряд.
- **Разрешение на уведомления спрашивали поверх интро.** Ворота стояли рядом с
  NavHost, а не внутри него.
- **Две текстуры не были степенями двойки.** 1536x1024 и 1448x1086, то есть без
  цепочки мипмапов. Все четыре стали 1024x1024; ресурсы с 6,0 МБ до 4,7 МБ.
- **У персонажа было четыре руки.** В меше лежало две пары: тело с руками вдоль
  корпуса и платье, рукава которого торчали прямо в T-позе. Кости положили на
  рукава, поэтому риг размахивал пустой тканью, а руки, которые видит игрок,
  оставались приваренными к бёдрам. Теперь рукава на руках, а привязка меряет
  расстояние вдоль поверхности, а не по воздуху: подол юбки проходит в 4 см от
  кисти, и никакая мера по прямой их не различит. Вместе с этим ушли восемь
  оболочек, продублированных в миллиметре друг от друга: 1139 вершин и их
  z-fighting.
- **На Уровне 0 была толпа.** От трёх до восьми существ, пополняемых каждые
  двенадцать секунд. Толпа — это суета, а не страх. Теперь ровно одно, и
  сложность меняет, какое именно, а не сколько их.
- **Существа видели сквозь стены.** Зрение было проверкой расстояния, полностью
  игнорировавшей уровень, так что разорвать контакт можно было только убежав.
- **Прогнать существо означало убрать его навсегда.** Отступление измеряло
  расстояние от текущей позиции игрока, поэтому преследование держало его в
  бегстве бесконечно; а состояние ожидания сбрасывало его растворение каждый
  тик, поэтому возвращение никогда не могло завершиться. Обе ошибки найдены
  симуляцией, ни одну нельзя было найти на устройстве.
- **Восемь процентов любого синтезированного шума были повтором сэмпла.** И C++,
  и Python брали индекс шума как `int(t * 44100)`, а в float `i/44100*44100`
  оказывается на волосок ниже `i`. Слышно на слух, не видно на форме волны.
- **Турецкие игроки видели буквальный `%d`** на подписи размера комнаты: строка
  содержала спецификатор формата, а рисовалась без аргумента.
- **Ресурсы по умолчанию были турецкими.** `values/` — это то, к чему Android
  обращается для языка без собственной записи, так что любая непереведённая
  строка появлялась по-турецки посреди немецкого меню. Теперь там английский.
- **CI сообщал об ошибке на исправном коде.** Две задачи боролись за один
  раннер; статические проверки так и не стартовали, истекали по таймауту в
  очереди и роняли прогон, хотя APK каждый раз собирался безупречно.
- **Персонаж выглядел четвероруким.** Риг умножал угол поворота на градиент
  позиции, а это раскрывает конечность веером, а не поворачивает её. Заменено
  настоящим linear blend skinning по скелету из двенадцати костей.
- **Защита от вмешательства обвиняла чистые устройства** при каждом запуске
  из-за голого поиска подстроки в `/proc/self/maps`. Теперь она сообщает, что
  именно нашла, и пишет причину в `Documents/Backrooms_Log/`.
- **Текстуры потолка были отражены** по диагонали каждой плитки: эмиттер выдавал
  UV в фиксированном порядке углов, что верно только для четырёхугольника с
  обратной намоткой.

## Лицензия

Все права защищены. Код лежит здесь, чтобы его читали.
