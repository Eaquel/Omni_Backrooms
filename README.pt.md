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
| `Shaders_Check.py` | O GLSL mora dentro de strings brutas do Kotlin, então um shader que não compila fica invisível até a tela que o usa abrir e ficar preta. Todos são compilados com `glslangValidator`. Também se liga cada par nomeado numa chamada `linkGlProgram(V, F)`, porque dois shaders que compilam isoladamente podem mesmo assim recusar-se a formar um programa — um uniforme com o mesmo nome tem de concordar entre etapas em tipo e precisão. |
| `Assets_Check.py` | Ícones vetoriais escritos à mão que o `aapt2` aceita e desenha torto; UVs de malha que não batem mais com a posição no mundo; a câmera de inspeção saindo do fundo; recursos duplicados e nunca referenciados; um idioma que ficou para trás; o disfarce de Unity se contradizendo. Também `--optimise`, um recodificador PNG sem perdas. |
| `Native_Check.py` | O contrato JNI. O Kotlin declara `external fun`, o C++ define `Java_..._name`, e em tempo de build **nada** liga os dois lados: nem o compilador Kotlin, nem o do C++, nem o linker. Renomear de um lado só é um `UnsatisfiedLinkError` na primeira chamada; mudar a quantidade de argumentos é pior, porque o JNI liga por nome e lê os argumentos sobrando da pilha sem reclamar. Também executa os detetores da proteção contra um `/proc` montado em disco, porque uma verificação de root que ninguém consegue executar é uma verificação de root que acusa telemóveis comuns. |
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
    Ending/     como uma run termina, função pura do tempo
    Frame/      cosméticos de moldura de perfil
    Trail/      cosméticos de rastro de passos
    Shield/     os detectores, e o que o binário aparenta ser
  Assets/                        texturas, malhas, história
  res/values*/                   dez idiomas
Tools/                           as oito verificações
```

## Correções recentes

Mais recentes primeiro. Esta lista é atualizada a cada correção.
- **Um único ficheiro Kotlin, e nenhum comentário em lado nenhum.** O `Service.kt` e o `Settings.kt` foram fundidos no `Backrooms.kt` — um
  pacote, sem colisões de nomes, 180 importações desduplicadas — e todos os
  comentários do projeto desapareceram: `//` e `/* */` do Kotlin, Gradle KTS,
  C++ e do GLSL e AGSL dentro das strings cruas de Kotlin; `#` de Python, YAML,
  TOML, CMake, ProGuard e properties; `<!-- -->` do XML. 8059 linhas removidas.
  Feito com um tokenizador por linguagem e não com um regex, porque um regex
  come `https://` dentro de uma string e `#` dentro de um valor YAML;
  `#version`, `#include` e os shebangs ficam, que são diretivas e não
  comentários. Três verificações nomeavam o `Service.kt` e partiram — um nome de
  ficheiro tinha passado a fazer parte do contrato quando o que testavam era uma
  declaração; passam a ler as fontes Kotlin que existirem. Verificado: as oito
  ferramentas verdes, todos os ficheiros Python compilam, os 31 XML analisam,
  os 22 shaders continuam a compilar e a ligar.
- **O único shader cuja falha é um estoiro era o único que nada verificava.** A reescrita da borda da entrada anterior usava `fwidth(d)`
  para dimensionar a sua faixa. O AGSL não tem função de derivada nenhuma — nem
  `fwidth`, nem `dFdx`, nem `dFdy` — e um RuntimeShader compila a fonte no
  *construtor*, no fio principal, dentro da composição. Por isso não degradou
  para um botão mais simples: lançou `IllegalArgumentException` no primeiro
  fotograma do átrio e a aplicação não arrancava. O `Shaders_Check.py` nunca o
  viu, porque a varredura exigia uma linha `#version` e o AGSL não tem: o único
  shader capaz de derrubar a aplicação ficava fora do filtro. Passa a verificar
  AGSL também — numa máquina de compilação não há compilador SkSL, por isso
  rejeita os builtins do GLSL que o AGSL não tem e exige que todo o
  identificador esteja declarado, que é o que transforma uma linha má nos seis
  erros deste estoiro. Verificado repondo `fwidth`, mais uma gralha,
  `texture()` e `gl_FragCoord`: 4/4.
- **E a decoração ainda podia terminar a aplicação.** O brilho estava protegido ao nível da API, não na fonte: um shader que não
  compila era fatal em vez de ausente. Agora vai embrulhado, e um botão que não
  consegue brilhar é apenas um botão — a mesma regra que a camada das
  trepadeiras já segue.
- **O relatório de fotograma: reler os píxeis em vez de teorizar sobre eles.**
  `level drawn: 1 chunk(s) resident, 1748 triangles` respondia à pergunta da
  ronda anterior e abria outra: a geometria chega à GPU e o ecrã continua
  preto, o que é um erro completamente diferente. Todos os diagnósticos até
  aqui mediam se um *passo correu*, nenhum o que ele *produziu*. O renderizador
  passa a reler um quadrado de 24x24 do alvo de cena antes de qualquer
  composição, e outro do framebuffer por omissão depois, e imprime para ambos a
  luminância mín/média/máx com a câmara, o número de triângulos, os tamanhos de
  alvo e todas as definições capazes de escurecer um fotograma. Por volta dos
  2, 5 e 10 segundos, e nunca mais. Dois números decidem sem teoria: cena clara
  e ecrã escuro, a composição está a comer a imagem; ambos escuros, o mundo está
  desenhado mas sem luz ou fora da câmara.
- **As trepadeiras do átrio eram um heptágono.** O `buildVineMesh` varria sete
  lados, e nesta espessura um tubo de sete lados tem uma silhueta visivelmente
  reta — facetas planas que nenhum suavizado corrige, porque a geometria é
  mesmo assim. Agora doze lados e 34 anéis, 2520 vértices para o átrio todo.
  Com um material à altura: fibra ao longo do caule, um salpicado mais grosso a
  atravessá-lo, um brilho anisotrópico que apanha a luz em faixa em vez de em
  ponto redondo, e luz a passar pela ponta fina.
- **E nada no átrio tinha multiamostragem.** A superfície das trepadeiras pedia
  `setEGLConfigChooser(8, 8, 8, 8, 16, 0)` — nenhuma multiamostragem — e é uma
  superfície à parte, à qual o suavizado da janela nunca chegava. Cada aresta
  era uma escada de píxeis. Passa a escolher 4x, recua para 2x e depois para
  nenhum, e nunca lança: o seletor simples do GLSurfaceView lança
  IllegalArgumentException no fio GL quando não encontra correspondência, o que
  deita a superfície abaixo e deixa o retângulo preto com que esta ronda
  começou.
- **O botão não se mexia até lhe tocarmos.** A placa por baixo estava animada e
  o corpo que a envolve não, e é isso que se lê como um retângulo plano por
  melhor sombreado que esteja. Agora respira: menos de meio por cento de
  escala, uma fração de grau de inclinação e uma sombra que sobe com ele, em
  três períodos coprimos para que o ciclo nunca repita a mesma combinação. O
  shader da borda foi refeito sobre um campo de distância de retângulo
  arredondado: o anterior usava `min(uv.x, 1-uv.x, ...)`, uma queda quadrada
  numa placa redonda, que amontoava o brilho nos cantos.
- **Um chunk que o fornecedor falhou ficava dado como perdido para o resto da
  partida.** Removidas as falhas de shader e de guarda, um registo de Mali-G68
  voltou completamente limpo — programas ligados, framebuffer completo, guarda
  `CLEAN` — e o mundo continuava preto com o HUD por cima. O `streamChunks`
  respondia a uma falha pondo um `ChunkMesh()` vazio na cache, que o
  `containsKey` passava a saltar para sempre: uma decisão permanente tirada de
  uma resposta passageira. O `fetchChunk` devolve null enquanto o mundo não for
  válido, por isso num aparelho cujo fio GL se adianta à criação do mundo, os
  quarenta e nove chunks do anel são dados como perdidos nos primeiros quarenta
  e nove fotogramas e o jogador fica no nada. Depende do temporização, e foi por
  isso que atingiu três testadores e nunca esta máquina. As falhas vão agora
  para um mapa de repetição e são pedidas outra vez vinte fotogramas depois, e o
  `Kotlin_Check.py` enuncia a regra — o renderizador precisa de contexto GL e de
  classpath Android, nenhuma ferramenta aqui executa um fotograma dele, e ambas
  as escritas compilam.
- **O renderizador nunca disse se tinha desenhado alguma coisa.** Três relatos
  de ecrã preto com registo completo, e nenhum respondia à primeira pergunta
  que interessa: chegou um triângulo à GPU? Uma cena sem geometria é limpa para
  cinzento 0,02, ou seja preto, e tudo a montante reporta sucesso. Passa a
  registar uma vez quando o nível desenha pela primeira vez — número de chunks,
  de triângulos, chunks à espera — e uma vez como aviso se passarem três
  segundos sem nada, dizendo se o mundo era válido e se o fornecedor de chunks
  estava sequer definido. «Não desenhou nada» e «desenhou algo que não se via»
  são dois erros sem nada em comum.
- **Os doze programas contradiziam-se a si próprios entre etapas.** O Galaxy
  S23 nomeou um uniforme; um Galaxy A17 com Mali disse o mesmo por outras
  palavras — `L0001 The fragment floating-point variable uGrowth does not match
  the vertex variable uGrowth. The precision does not match.` Ao medir os doze
  pares apareceram mais 25: uma varying escrita `out vec3 vNormal` na etapa de
  vértices (highp por omissão) e lida `in vec3 vNormal` na de fragmentos
  (mediump, pela sua linha `precision`). A especificação ES 3.00 permite-o para
  varyings — só exige correspondência nos uniformes — e o glslang segue a
  especificação, por isso nada era assinalado. Os controladores não são assim
  tão uniformes, e a mensagem da Mali nem sequer distingue uma varying de um
  uniforme. As 25 ficam agora fixadas à precisão da etapa de vértices, e o
  `Shaders_Check.py` exige correspondência em ambos os casos —
  deliberadamente mais estrito do que a especificação.
- **Um fotograma que desenha para o nada é igualzinho a um ecrã preto.** O
  renderizador construía um alvo de cor fora do ecrã, um buffer de profundidade
  e um par de bloom a meia resolução, e nunca chamava
  `glCheckFramebufferStatus`. Um framebuffer incompleto não é um erro que
  qualquer controlador reporte: todos os desenhos nele são descartados, a
  partida continua a correr, o HUD continua a compor-se por cima, e o mundo
  simplesmente não está lá — sem uma linha acionável em registo nenhum. A
  completude passa a ser verificada em cada reconstrução e uma falha degrada em
  vez de desaparecer: um par de bloom inutilizável custa os halos, um alvo de
  cena inutilizável envia o fotograma direto para o ecrã sem a cadeia de
  pós-processamento.
- **E nada dizia qual GPU estava a desenhar.** Chegaram dois relatos de ecrã
  preto com registo completo e nenhum identificava o controlador, que teve de
  ser adivinhado pelo número do modelo. O fabricante, o renderizador, a versão
  GL e a versão GLSL são agora registados uma vez por contexto, e um programa
  que não liga diz o seu nome — o programa de cena não está envolvido em nada,
  por isso a sua falha era um estoiro seco sem dizer qual dos doze tinha caído.
- **Um retângulo preto sobre os botões do átrio, por causa de um shader que
  compila perfeitamente.** De um registo de Galaxy S23: `Omni program link
  failed: Error: Uniform uGrowth precision mismatch with other stage.` As duas
  metades do shader das trepadeiras são válidas isoladamente — foi precisamente
  por isso que esta ferramenta as aprovou durante meses. Compilar é apenas
  metade da construção de um programa. Uniformes com o mesmo nome têm de
  concordar entre etapas em tipo **e precisão**, e um vertex shader coloca
  `float` em `highp` por omissão, enquanto um fragment shader não tem omissão
  nenhuma — daí todos os fragment shaders aqui declararem `precision mediump
  float;`. Um uniforme float lido pelas duas etapas e deixado nu é, por
  construção, um conflito. Controladores permissivos ligam-no à mesma, e foi
  exatamente assim que chegou à produção. `uGrowth` é agora explicitamente
  `highp` nas duas etapas, e o `Shaders_Check.py` liga cada par nomeado numa
  chamada `linkGlProgram(V, F)` em vez de apenas compilar as metades — o que
  também apanha um shader que não pertence a programa nenhum.
- **E a falha não degradava, tapava o botão.** A camada das trepadeiras já
  apanhava a sua própria falha de arranque e não desenhava nada, o que soa
  seguro e não é: a view chama `setZOrderOnTop(true)`, colocando a sua
  superfície sobre toda a janela em vez de dentro do botão. Uma superfície que
  nunca apresenta um fotograma não é «sem trepadeiras», é um buraco opaco sobre
  o botão e a sua legenda. Uma camada decorativa que não consegue desenhar sai
  agora por completo da composição, e o botão volta à sua placa pintada.
- **A proteção acusou um telemóvel limpo de ter root.** Um jogador fotografou o
  diálogo de segurança: `motivo: root, flags=0x40200`. Dois bits, e todos os
  bits de root verdadeiros — ROOT_BINARY, ROOT_PROPS, ROOT_PATHS, MAGISK,
  ZYGISK, KSU, SELINUX_OFF — apagados. Não era um dispositivo com root, eram
  duas verificações nossas. **SHADOW_MOUNT** perguntava
  `containsCI(m,"overlay") && containsCI(m,"/system")` sobre toda a tabela de
  montagens: duas pesquisas independentes, portanto um overlay em
  `/vendor/overlay` e a linha `/system_ext` — ambos mobiliário de série em
  qualquer telemóvel Android 11+ — combinavam-se em «root», que é HIGH, que é o
  diálogo. **PTRACE_TRACED** chamava `PTRACE_TRACEME` e tentava desfazê-lo com
  `PTRACE_DETACH` no pid 0, o que não pode funcionar: o detach precisa do pid
  real, falha com `ESRCH`, e o processo fica traçado pelo pai. O TRACEME da
  varredura seguinte devolve então `EPERM` — «já traçado» — ou seja, a partir de
  cerca de cinco segundos, em qualquer dispositivo, para sempre, porque a
  palavra de bandeiras é OR-ada durante toda a partida. Isso também nos custou
  relatórios de falha: um processo preso como tracee encaminha os seus sinais
  para um tracer que nunca espera, por isso um SIGSEGV real fica pendurado em
  vez de falhar. Ambas as verificações agora analisam o campo em questão em vez
  de procurar uma subcadeia no ficheiro, e o diálogo transporta a linha de
  montagem que o incomodou.
- **E a verificação do Frida era cara ou coroa sobre acabar com uma partida.**
  Encontrada ao ler as duas acima. `fridaPort` procurava em `/proc/net/tcp` os
  literais `6D58`, `71D4`, `2717` e `5039` — que são as portas 27992, 29140,
  10007 e 20537, não as 27042-27045 do Frida; alguém escrevera dígitos decimais
  onde era preciso hexadecimal. Estarem errados era a metade menor. Eram
  procurados como subcadeias contra um ficheiro que é quase só hexadecimal: na
  tabela de 19 sockets desta máquina de compilação **220 das 65536 agulhas
  possíveis de quatro dígitos hexadecimais já aparecem**, e um telemóvel tem
  muitos mais sockets. Um acerto dentro de um número de inode levanta
  FLAG_FRIDA_PORT, que é CRITICAL, e CRITICAL chama `killProcess`. Agora analisa
  a coluna do endereço local e exige o estado `0A` (LISTEN), para que uma ligação
  de saída ao Frida de outra pessoa não seja lida como um servidor a correr aqui.

- **The creature stood still for the whole game.** Simulated over eight seeds
  and five minutes each, three of them had it see the player 0% of the time at a
  median 33-51 m. The state histogram said why: 99% of the run in
  `AIState::Idle`, 0% in Wander — and Idle has no case in `executeState`, so it
  does not move. It falls through to Wander now.
- **And when it did find you it never let go** — a median distance of 1.4 m for
  the full five minutes on the other seeds. Only the torch and damage ever broke
  it off. Contact costs it now, so an encounter is a cycle. Seen 50% → 34%, no
  seed at 0.
- **Standing still, the only thing you could hear was the tube overhead.** A
  distant-event generator: a door, pipes knocking, a drag across carpet, the
  building settling. One every seventeen seconds, mostly silence between.
- **A constant documenting a safety invariant that nothing enforced.**
  `kMaxRoomHalf` carried the rule the O(1) level query rests on, and every build
  printed `warning: unused variable 'kMaxRoomHalf'`. Sweeping it to find the
  source of long sightlines gave byte-identical results three times — I was
  turning a knob connected to nothing. The room catalogue is at namespace scope
  now with a `static_assert` on it.
- **Corridors were an L, and they chained** — one leg the whole way at a single
  z, so the longest straight line of open floor was 253 m at the median over 60
  seeds. They dogleg now, via a row belonging to neither end: 227 m. The effect
  is 10% on the median and nothing on the tail, because the plan is axis-aligned
  and much of a sightline is inherent to the grid.
- **I tuned that check's bound on too small a sample, again** — 20 seeds passed,
  40 failed 2. It asserts on the median now, which is stable at both sizes. The
  p90 is printed but not asserted: it does not discriminate.
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
