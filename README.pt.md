[English](README.md) ·
[Türkçe](README.tr.md) ·
[Deutsch](README.de.md) ·
[Español](README.es.md) ·
[Français](README.fr.md) ·
[Italiano](README.it.md) ·
**Português** ·
[Русский](README.ru.md) ·
[日本語](README.ja.md) ·
[中文](README.zh.md)

# Omni Backrooms

Um jogo de terror e sobrevivência para Android ambientado no Nível 0: corredores
de escritório de um amarelo monótono e sem fim, carpete úmido, lâmpadas
fluorescentes zumbindo e uma coisa lá dentro com você que não pode ser morta.

Escrito do zero: o renderizador é OpenGL ES 3.0 conduzido a partir do Kotlin, a
simulação é C++ através do NDK, e o nível não é um arquivo de mapa, mas uma
função pura de coordenadas de célula — nunca acaba e não repete uma única
emenda.

## O que há aqui

| | |
|---|---|
| **Nível 0** | Infinito. Cada célula — piso, parede, luz, umidade — é derivada das próprias coordenadas e da semente da partida, de modo que o mundo é idêntico para dois jogadores que não trocam um único byte dele. |
| **Uma criatura** | Não uma multidão. Enxerga por traçado de raio, então paredes escondem você de verdade; ouve conforme o barulho que você faz, então agachar realmente serve para alguma coisa; e lembra onde viu você pela última vez. |
| **A lanterna** | Deixa a criatura mais lenta e depois a afugenta. Não a mata. Nas Backrooms nada morre: ela recua, se dissipa, espera à distância e volta quando vê você de novo ou ouve você se descuidar. |
| **Sem arquivos de áudio** | Todo som é sintetizado no dispositivo. O APK não contém um único WAV, nenhum OGG, nada. |
| **Dez idiomas** | Turco, inglês, alemão, espanhol, francês, italiano, português, russo, japonês e chinês — completos, não pela metade. Na primeira abertura o jogo escolhe o do seu aparelho. |
| **Apenas cosméticos** | Molduras, rastros e personagens. Nada vendido em lugar algum do jogo afeta como se joga. |

## Compilando

```bash
git clone https://github.com/Eaquel/Omni_Backrooms.git
cd Omni_Backrooms
./gradlew :Backrooms:assembleRelease
```

São necessários JDK 25, Android SDK 36, o NDK e o CMake 4.3.2. As builds de
release são assinadas com um keystore que não está neste repositório;
`assembleDebug` não precisa de nada a mais.

## As verificações

Sete das oito ferramentas em `Tools/` rodam a cada push. Existem porque cada
uma protege algo que a build do Gradle simplesmente não consegue ver:

| Ferramenta | O que ela pega |
|---|---|
| `Shaders_Check.py` | O GLSL mora dentro de strings brutas do Kotlin, então um shader que não compila fica invisível até a tela que o usa abrir e ficar preta. Todos são compilados com `glslangValidator`. |
| `Assets_Check.py` | Ícones vetoriais escritos à mão que o `aapt2` aceita e desenha torto; UVs de malha que não batem mais com a posição no mundo; a câmera de inspeção saindo do fundo; recursos duplicados e nunca referenciados; um idioma que ficou para trás; o disfarce de Unity se contradizendo. Também `--optimise`, um recodificador PNG sem perdas. |
| `Native_Check.py` | O contrato JNI. O Kotlin declara `external fun`, o C++ define `Java_..._name`, e em tempo de build **nada** liga os dois lados: nem o compilador Kotlin, nem o do C++, nem o linker. Renomear de um lado só é um `UnsatisfiedLinkError` na primeira chamada; mudar a quantidade de argumentos é pior, porque o JNI liga por nome e lê os argumentos sobrando da pilha sem reclamar. |
| `Kotlin_Check.py` | Cada import contra a dependência que o sustenta, nos dois sentidos. O Kotlin aqui compila sem o classpath do Android, então uma biblioteca de fato removida é idêntica a uma que apenas não está no caminho — foi assim que remover o Firebase levou junto o `androidx.media3`. |
| `Level_0_Check.py` | Inunda o mundo a partir do ponto de entrada com muitas sementes e prova que a saída é alcançável. Uma saída inalcançável é uma partida impossível de vencer, e é completamente silenciosa. |
| `Entity_Check.py` | Compila a IA real, põe uma criatura no Nível 0 real e observa: visão bloqueada por paredes, audição que escala com o barulho, e o ciclo de recuo e retorno que jamais pode travar. |
| `Code_To_Sound.py` | Renderiza os geradores em C++ que de fato são distribuídos e os compara amostra a amostra com uma referência em Python. Também escreve WAVs, para que sons que só existem como código possam realmente ser ouvidos. |

Rodar todas:

```bash
for t in Shaders Assets Native Entity Kotlin; do python3 Tools/${t}_Check.py; done
python3 Tools/Level_0_Check.py 40
python3 Tools/Code_To_Sound.py
```

Cada verificação aqui foi validada colocando o bug de volta. Uma verificação que
nunca falhou não dá a ninguém motivo para confiar nela.

## Organização

```
Backrooms/Source/Main/
  Kotlin/com/omni/backrooms/     interface, renderizador, loop  (~14k linhas)
  Native/                        C++ através do NDK             (~3,9k linhas)
    Map/        o Nível 0 como função pura de coordenadas
    Entity/     IA da criatura — percepção, recuo, retorno
    Sound/      todos os geradores; não há arquivos de áudio
    Frame/      cosméticos de moldura de perfil
    Trail/      cosméticos de rastro de passos
    Shield/     os detectores, e o que o binário aparenta ser
  Assets/                        texturas, malhas, história
  res/values*/                   dez idiomas
Tools/                           as oito verificações
```

## Correções recentes

Mais recentes primeiro. Esta lista é atualizada a cada correção.

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
- **O Firebase nunca funcionou, e levou muita coisa junto.** Não há
  google-services.json aqui e a CI injeta um espaço reservado: todo log do
  Crashlytics, toda escrita no Firestore e toda leitura do Remote Config
  falhava em tempo de execução dentro de um `runCatching` que engolia. A API
  REST era a mesma história em api.omnibackrooms.com, que não resolve, e o
  netcode embaixo esvaziava um socket para o qual ninguém enviava — chat de voz
  incluído. Tudo fora, junto com Room, Billing e Credential Manager, que nada
  referenciava.
- **A lanterna era um círculo no meio da tela.** Desenhado em uv (0.5, 0.47) no
  passe de pós, sem posição no mundo: era por isso que a luz parecia sair do
  peito dela. Agora é um holofote de verdade no shader de cena, saindo da lente
  do modelo.
- **Os rastros que se possuía não podiam ser equipados.** Três falhas seguidas.
- **A permissão de notificação era pedida por cima da intro.** O portão ficava
  ao lado do NavHost em vez de dentro.
- **Duas texturas não eram potências de dois.** 1536x1024 e 1448x1086, sem
  cadeia de mipmaps. As quatro são 1024x1024; os recursos vão de 6,0 MB para
  4,7 MB.
- **A personagem tinha quatro braços.** A malha continha dois pares: um corpo com
  os braços ao lado e um vestido cujas mangas saíam retas em T-pose. Os ossos
  tinham sido postos sobre as mangas, então o rig balançava tecido vazio
  enquanto os braços que se veem ficavam soldados ao quadril. Agora as mangas
  estão sobre os braços, e a vinculação mede ao longo da superfície em vez de
  pelo ar: a barra da saia passa a 4 cm da mão, e nenhuma medida em linha reta
  distingue as duas. Com isso foram embora oito cascas duplicadas a um
  milímetro: 1139 vértices e o z-fighting que causavam.
- **O Nível 0 tinha uma multidão.** De três a oito criaturas, repostas a cada
  doze segundos. Uma multidão é atarefada, não assustadora. Agora tem
  exatamente uma, e a dificuldade muda qual é essa uma, não quantas são.
- **As criaturas enxergavam através das paredes.** A visão era um teste de
  distância que ignorava o nível por completo, então o único jeito de quebrar
  o contato era correr mais.
- **Afugentar uma a removia para sempre.** O recuo media a distância a partir da
  posição atual do jogador, então segui-la a mantinha fugindo indefinidamente; e
  o estado parado zerava o desaparecimento a cada tick, então o retorno nunca
  conseguia terminar. Ambos encontrados por simulação, nenhum deles achável em
  um aparelho.
- **Oito por cento de todo ruído sintetizado era uma amostra repetida.** Tanto o
  C++ quanto o Python tomavam o índice de ruído como `int(t * 44100)`, e em
  ponto flutuante `i/44100*44100` cai um fio abaixo de `i`. Audível, invisível
  numa forma de onda.
- **Jogadores turcos viam um `%d` literal** no rótulo do tamanho da sala: a
  string tinha um especificador de formato e era desenhada sem argumento.
- **Os recursos padrão estavam em turco.** `values/` é o que o Android usa como
  recurso para um idioma sem entrada própria, então qualquer string não
  traduzida aparecia em turco no meio de um menu alemão. Agora ali está o
  inglês.
- **A CI reportava falha com o código verde.** Os dois jobs disputavam o único
  executor; as verificações estáticas nunca começavam, estouravam o tempo na
  fila e faziam a execução falhar enquanto o APK compilava perfeitamente todas
  as vezes.
- **O personagem parecia ter quatro braços.** O rig multiplicava o ângulo de
  rotação por um gradiente de posição, o que abre um membro em leque em vez de
  girá-lo. Substituído por linear blend skinning de verdade sobre um esqueleto
  de doze ossos.
- **A proteção antiadulteração acusava aparelhos limpos** a cada abertura, por
  causa de uma busca de substring crua em `/proc/self/maps`. Agora ela relata o
  que encontrou e escreve o motivo em `Documents/Backrooms_Log/`.
- **As texturas do teto estavam espelhadas** na diagonal de cada ladrilho: o
  emissor entregava as UVs numa ordem fixa de cantos, que só está certa para um
  quadrilátero enrolado ao contrário.

## Licença

Todos os direitos reservados. O código está aqui para ser lido.
