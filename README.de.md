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
    Frame/      Profilrahmen-Kosmetik
    Trail/      Fußspuren-Kosmetik
    Shield/     die Detektoren, und als was sich die Binärdatei ausgibt
  Assets/                        Texturen, Meshes, Story
  res/values*/                   zehn Sprachen
Tools/                           die acht Prüfungen
```

## Zuletzt behoben

Neuestes zuerst. Diese Liste wird bei jeder Korrektur ergänzt.

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
