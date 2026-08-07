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

Six des huit outils de `Tools/` s'exécutent à chaque push. Ils existent parce
que chacun protège quelque chose que la compilation Gradle ne peut tout
simplement pas voir :

| Outil | Ce qu'il attrape |
|---|---|
| `Shaders_Check.py` | Le GLSL vit à l'intérieur de chaînes brutes Kotlin : un shader qui ne compile pas reste invisible jusqu'à ce que l'écran qui l'utilise s'ouvre et reste noir. Chacun est compilé avec `glslangValidator`. |
| `Assets_Check.py` | Des icônes vectorielles écrites à la main qu'`aapt2` accepte et dessine de travers ; des UV de maillage qui ne correspondent plus à la position monde ; la caméra d'inspection qui sort de son décor ; des ressources dupliquées ou jamais référencées ; une langue restée en arrière ; le déguisement Unity qui se contredit. Et aussi `--optimise`, un ré-encodeur PNG sans perte. |
| `Native_Check.py` | Le contrat JNI. Kotlin déclare `external fun`, le C++ définit `Java_..._name`, et **rien** ne relie les deux à la compilation — ni le compilateur Kotlin, ni celui du C++, ni l'éditeur de liens. Un renommage d'un seul côté donne un `UnsatisfiedLinkError` au premier appel ; un nombre d'arguments modifié est pire, car JNI lie par le nom et lit les arguments en trop sur la pile sans broncher. |
| `Level_0_Check.py` | Inonde le monde depuis le point d'arrivée sur de nombreuses graines et prouve que la sortie est atteignable. Une sortie inatteignable est une partie ingagnable, et c'est parfaitement silencieux. |
| `Entity_Check.py` | Compile la vraie IA, place une créature dans le vrai Niveau 0 et regarde : la vue bloquée par les murs, l'ouïe qui suit le bruit, et le cycle retraite-retour qui ne doit jamais se bloquer. |
| `Code_To_Sound.py` | Restitue les générateurs C++ effectivement livrés et les compare échantillon par échantillon à une référence Python. Écrit aussi des WAV, pour que des sons qui n'existent que sous forme de code puissent vraiment être écoutés. |

Tout lancer :

```bash
for t in Shaders Assets Native Entity; do python3 Tools/${t}_Check.py; done
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
    Frame/      cosmétiques de cadre de profil
    Trail/      cosmétiques de traces de pas
    Shield/     les détecteurs, et ce que le binaire prétend être
  Assets/                        textures, maillages, histoire
  res/values*/                   dix langues
Tools/                           les huit vérifications
```

## Corrections récentes

Les plus récentes en premier. Cette liste est mise à jour à chaque correction.

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
