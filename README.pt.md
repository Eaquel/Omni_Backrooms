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

Seis das oito ferramentas em `Tools/` rodam a cada push. Existem porque cada
uma protege algo que a build do Gradle simplesmente não consegue ver:

| Ferramenta | O que ela pega |
|---|---|
| `Shaders_Check.py` | O GLSL mora dentro de strings brutas do Kotlin, então um shader que não compila fica invisível até a tela que o usa abrir e ficar preta. Todos são compilados com `glslangValidator`. |
| `Assets_Check.py` | Ícones vetoriais escritos à mão que o `aapt2` aceita e desenha torto; UVs de malha que não batem mais com a posição no mundo; a câmera de inspeção saindo do fundo; recursos duplicados e nunca referenciados; um idioma que ficou para trás; o disfarce de Unity se contradizendo. Também `--optimise`, um recodificador PNG sem perdas. |
| `Native_Check.py` | O contrato JNI. O Kotlin declara `external fun`, o C++ define `Java_..._name`, e em tempo de build **nada** liga os dois lados: nem o compilador Kotlin, nem o do C++, nem o linker. Renomear de um lado só é um `UnsatisfiedLinkError` na primeira chamada; mudar a quantidade de argumentos é pior, porque o JNI liga por nome e lê os argumentos sobrando da pilha sem reclamar. |
| `Level_0_Check.py` | Inunda o mundo a partir do ponto de entrada com muitas sementes e prova que a saída é alcançável. Uma saída inalcançável é uma partida impossível de vencer, e é completamente silenciosa. |
| `Entity_Check.py` | Compila a IA real, põe uma criatura no Nível 0 real e observa: visão bloqueada por paredes, audição que escala com o barulho, e o ciclo de recuo e retorno que jamais pode travar. |
| `Code_To_Sound.py` | Renderiza os geradores em C++ que de fato são distribuídos e os compara amostra a amostra com uma referência em Python. Também escreve WAVs, para que sons que só existem como código possam realmente ser ouvidos. |

Rodar todas:

```bash
for t in Shaders Assets Native Entity; do python3 Tools/${t}_Check.py; done
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
