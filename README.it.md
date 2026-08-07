[English](README.md) ·
[Türkçe](README.tr.md) ·
[Deutsch](README.de.md) ·
[Español](README.es.md) ·
[Français](README.fr.md) ·
**Italiano** ·
[Português](README.pt.md) ·
[Русский](README.ru.md) ·
[日本語](README.ja.md) ·
[中文](README.zh.md)

# Omni Backrooms

Un gioco horror di sopravvivenza per Android ambientato al Livello 0: corridoi
d'ufficio giallo monotono senza fine, moquette umida, neon che ronzano e una
cosa là dentro con te che non si può uccidere.

Scritto da zero: il renderer è OpenGL ES 3.0 guidato da Kotlin, la simulazione è
C++ attraverso l'NDK, e il livello non è un file di mappa ma una funzione pura
delle coordinate di cella — non finisce mai e non ripete una sola giuntura.

## Cosa c'è qui dentro

| | |
|---|---|
| **Livello 0** | Infinito. Ogni cella — pavimento, muro, luce, umidità — deriva dalle proprie coordinate e dal seme della partita, quindi il mondo è identico per due giocatori che non se ne scambiano un solo byte. |
| **Una creatura** | Non una folla. Vede tramite raggio, quindi i muri ti nascondono davvero; sente in base a quanto rumore fai, quindi accovacciarsi serve davvero a qualcosa; e ricorda dove ti ha visto l'ultima volta. |
| **La torcia** | La rallenta, poi la scaccia. Non la uccide. Nelle Backrooms non muore niente: si ritira, si dissolve, aspetta a distanza e torna quando ti rivede o ti sente diventare imprudente. |
| **Nessun file audio** | Ogni suono è sintetizzato sul dispositivo. Nell'APK non c'è un solo WAV, né un OGG, niente. |
| **Dieci lingue** | Turco, inglese, tedesco, spagnolo, francese, italiano, portoghese, russo, giapponese, cinese — complete, non parziali. Al primo avvio il gioco sceglie quella del tuo dispositivo. |
| **Solo estetica** | Cornici, scie e personaggi. Niente di ciò che si vende nel gioco cambia il modo in cui si gioca. |

## Compilarlo

```bash
git clone https://github.com/Eaquel/Omni_Backrooms.git
cd Omni_Backrooms
./gradlew :Backrooms:assembleRelease
```

Servono JDK 25, Android SDK 36, l'NDK e CMake 4.3.2. Le build di release sono
firmate con un keystore che non si trova in questo repository; `assembleDebug`
non richiede nulla in più.

## I controlli

Sei degli otto strumenti in `Tools/` girano a ogni push. Esistono perché
ciascuno protegge qualcosa che la build Gradle semplicemente non può vedere:

| Strumento | Cosa intercetta |
|---|---|
| `Shaders_Check.py` | Il GLSL vive dentro stringhe grezze Kotlin: uno shader che non compila resta invisibile finché non si apre la schermata che lo usa e resta nera. Ognuno viene compilato con `glslangValidator`. |
| `Assets_Check.py` | Icone vettoriali scritte a mano che `aapt2` accetta e disegna storte; UV di mesh che non corrispondono più alla posizione nel mondo; la telecamera d'ispezione che esce dal fondale; risorse duplicate e mai referenziate; una lingua rimasta indietro; il travestimento Unity che si contraddice. Inoltre `--optimise`, un ricodificatore PNG senza perdita. |
| `Native_Check.py` | Il contratto JNI. Kotlin dichiara `external fun`, il C++ definisce `Java_..._name`, e in fase di build **niente** collega i due lati: né il compilatore Kotlin, né quello C++, né il linker. Rinominare da una parte sola è un `UnsatisfiedLinkError` alla prima chiamata; cambiare il numero di argomenti è peggio, perché JNI collega per nome e legge gli argomenti in eccesso dallo stack senza protestare. |
| `Level_0_Check.py` | Inonda il mondo dal punto di comparsa su molti semi e dimostra che l'uscita è raggiungibile. Un'uscita irraggiungibile è una partita invincibile, ed è del tutto silenziosa. |
| `Entity_Check.py` | Compila l'IA vera, mette una creatura nel Livello 0 vero e osserva: vista bloccata dai muri, udito che scala con il rumore, e il ciclo ritirata-ritorno che non deve mai bloccarsi. |
| `Code_To_Sound.py` | Riproduce i generatori C++ effettivamente distribuiti e li confronta campione per campione con un riferimento Python. Scrive anche dei WAV, così suoni che esistono solo come codice si possono davvero ascoltare. |

Eseguirli tutti:

```bash
for t in Shaders Assets Native Entity; do python3 Tools/${t}_Check.py; done
python3 Tools/Level_0_Check.py 40
python3 Tools/Code_To_Sound.py
```

Ogni controllo qui è stato verificato rimettendoci dentro il suo bug. Un
controllo che non ha mai fallito non dà a nessuno un motivo per fidarsene.

## Struttura

```
Backrooms/Source/Main/
  Kotlin/com/omni/backrooms/     interfaccia, renderer, game loop  (~14k righe)
  Native/                        C++ attraverso l'NDK              (~3,9k righe)
    Map/        il Livello 0 come funzione pura delle coordinate
    Entity/     IA della creatura — percezione, ritirata, ritorno
    Sound/      tutti i generatori; non ci sono file audio
    Frame/      cosmetici delle cornici del profilo
    Trail/      cosmetici delle scie di passi
    Shield/     i rilevatori, e ciò che il binario dichiara di essere
  Assets/                        texture, mesh, storia
  res/values*/                   dieci lingue
Tools/                           gli otto controlli
```

## Correzioni recenti

Le più recenti per prime. Questo elenco si aggiorna a ogni correzione.

- **Il Livello 0 conteneva una folla.** Da tre a otto creature, rimpiazzate ogni
  dodici secondi. Una folla è indaffarata, non spaventosa. Ora ne contiene
  esattamente una, e la difficoltà cambia quale sia, non quante siano.
- **Le creature vedevano attraverso i muri.** La vista era un test di distanza
  che ignorava del tutto il livello: l'unico modo di interrompere il contatto
  era correre più veloce.
- **Scacciarne una la rimuoveva per sempre.** La ritirata misurava la distanza
  dalla posizione attuale del giocatore, quindi seguirla la teneva in fuga
  all'infinito; e lo stato in attesa azzerava la sua dissolvenza a ogni tick,
  quindi il ritorno non poteva mai concludersi. Entrambi trovati per
  simulazione, nessuno dei due trovabile su un dispositivo.
- **L'otto per cento di ogni rumore sintetizzato era un campione ripetuto.** Sia
  il C++ sia il Python prendevano l'indice del rumore come `int(t * 44100)`, e
  in virgola mobile `i/44100*44100` cade di un pelo sotto `i`. Udibile,
  invisibile in una forma d'onda.
- **I giocatori turchi vedevano un `%d` letterale** sull'etichetta della
  dimensione stanza: la stringa aveva un segnaposto di formato e veniva
  disegnata senza argomento.
- **Le risorse predefinite erano in turco.** `values/` è ciò a cui Android
  ripiega per una lingua senza voce propria, quindi ogni stringa non tradotta
  compariva in turco in mezzo a un menu tedesco. Adesso lì c'è l'inglese.
- **La CI segnalava errori su codice verde.** I due job si contendevano l'unico
  runner; i controlli statici non partivano mai, scadevano in coda e facevano
  fallire l'esecuzione mentre l'APK veniva compilato alla perfezione ogni volta.
- **Il personaggio sembrava avere quattro braccia.** Il rig moltiplicava l'angolo
  di rotazione per un gradiente di posizione, il che apre un arto a ventaglio
  invece di ruotarlo. Sostituito con vero linear blend skinning su uno scheletro
  a dodici ossa.
- **La protezione anti-manomissione accusava dispositivi puliti** a ogni avvio,
  per una nuda ricerca di sottostringa in `/proc/self/maps`. Ora dichiara cosa
  ha trovato e scrive il motivo in `Documents/Backrooms_Log/`.
- **Le texture del soffitto erano specchiate** lungo la diagonale di ogni
  piastrella: l'emettitore forniva le UV in un ordine fisso di angoli, corretto
  solo per un quadrilatero avvolto nel verso opposto.

## Licenza

Tutti i diritti riservati. Il codice è qui per essere letto.
