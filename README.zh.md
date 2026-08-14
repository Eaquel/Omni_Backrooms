[English](README.md) ·
[Türkçe](README.tr.md) ·
[Deutsch](README.de.md) ·
[Español](README.es.md) ·
[Français](README.fr.md) ·
[Italiano](README.it.md) ·
[Português](README.pt.md) ·
[Русский](README.ru.md) ·
[日本語](README.ja.md) ·
**中文**

# Omni Backrooms

一款以第0层为舞台的 Android 生存恐怖游戏——望不到尽头的单调黄色办公走廊、潮湿的
地毯、嗡鸣的日光灯，还有一个和你待在里面、杀不死的东西。

从零写起：渲染是由 Kotlin 驱动的 OpenGL ES 3.0，模拟是通过 NDK 的 C++，而关卡不是
一个地图文件，而是单元格坐标的纯函数——所以它永不结束，也不会重复任何一处接缝。

## 里面有什么

| | |
|---|---|
| **第0层** | 无限。每个单元格——地面、墙、灯光、潮气——都由它自身的坐标和本局的种子推导而来，因此对于从未交换过一个字节的两名玩家，世界是完全相同的。 |
| **一只怪物** | 不是一群。它用射线看东西，所以墙真的能藏住你；它按你发出的噪音大小来听，所以蹲下确实换来了东西；而且它记得最后一次看见你的地方。 |
| **手电筒** | 先让它变慢，然后把它赶走。不是杀死。Backrooms 里没有东西会死——它会撤退、消散、在远处等待，等再次看见你、或听见你放松警惕时回来。 |
| **没有音频文件** | 每一个声音都在设备上合成。APK 里没有一个 WAV，没有 OGG，什么都没有。 |
| **十种语言** | 土耳其语、英语、德语、西班牙语、法语、意大利语、葡萄牙语、俄语、日语、中文——完整的，不是半成品。首次启动时游戏自己选择你设备的语言。 |
| **纯外观** | 头像框、足迹和角色。游戏里出售的任何东西都不会影响玩法。 |

## 构建

```bash
git clone https://github.com/Eaquel/Omni_Backrooms.git
cd Omni_Backrooms
./gradlew :Backrooms:assembleRelease
```

需要 JDK 25、Android SDK 36、NDK 和 CMake 4.3.2。release 构建使用不在本仓库中的
密钥库签名；`assembleDebug` 不需要额外的东西。

## 各项检查

`Tools/` 里八个工具中的七个会在每次 push 时运行。它们存在，是因为每一个都守着
Gradle 构建根本看不见的东西：

| 工具 | 它能抓到什么 |
|---|---|
| `Shaders_Check.py` | GLSL 写在 Kotlin 的原始字符串里，所以一个编译不过的着色器在使用它的界面打开并变黑之前都是不可见的。每一个都用 `glslangValidator` 编译。 此外，出现在 `linkGlProgram(V, F)` 调用里的每一组都会被链接一遍，因为两个各自能编译的着色器仍可能拒绝组成一个程序 —— 同名 uniform 必须在各阶段之间在类型和精度上都一致。 |
| `Assets_Check.py` | `aapt2` 接受却画得乱七八糟的手写矢量图标；不再与世界坐标对应的网格 UV；跑出背景的观赏相机；重复以及从未被引用的资源；落后的语言；自相矛盾的 Unity 伪装。还有 `--optimise`——一个无损的 PNG 重编码器。 |
| `Native_Check.py` | JNI 契约。Kotlin 声明 `external fun`，C++ 定义 `Java_..._name`，而在构建期**没有任何东西**把两边连起来——Kotlin 编译器不会，C++ 编译器不会，链接器也不会。单边改名意味着首次调用时的 `UnsatisfiedLinkError`；参数个数变了更糟，因为 JNI 按名字绑定，会一声不吭地从栈上读走多出来的参数。 它还会让防护的各个检测器跑在一份铺在磁盘上的 `/proc` 上，因为一个谁都没法执行的 root 检查，就是一个会冤枉普通手机的 root 检查。 |
| `Kotlin_Check.py` | 把每一个 import 与其背后的依赖双向核对。这里的 Kotlin 在没有 Android classpath 的情况下编译，所以真正被删掉的库和只是不在路径上的库看起来一模一样——移除 Firebase 时 `androidx.media3` 就这样被悄悄带走了。 |
| `Level_0_Check.py` | 用大量种子从出生点淹没整个世界，证明出口确实可达。不可达的出口意味着一局无法通关，而且它完全无声。 |
| `Entity_Check.py` | 编译真正的 AI，把一只怪物放进真正的第0层并观察：被墙挡住的视线、随噪音变化的听觉，以及绝不能卡死的撤退—回归循环。 |
| `Code_To_Sound.py` | 渲染真正发布出去的那份 C++ 生成器，并与 Python 参考实现逐个采样比对。它还会写出 WAV，让只以代码形式存在的声音真的能被听到。 |

全部运行：

```bash
for t in Shaders Assets Native Entity Kotlin; do python3 Tools/${t}_Check.py; done
python3 Tools/Level_0_Check.py 40
python3 Tools/Code_To_Sound.py
```

这里的每一项检查都通过把它对应的 bug 放回去来验证过。一项从未失败过的检查，
没有人有理由信任它。

## 目录结构

```
Backrooms/Source/Main/
  Kotlin/com/omni/backrooms/     界面、渲染、游戏循环      (约 14000 行)
  Native/                        通过 NDK 的 C++          (约 3900 行)
    Map/        作为坐标纯函数的第0层
    Entity/     怪物 AI —— 感知、撤退、回归
    Sound/      全部生成器；不存在音频文件
    Ending/     一局如何结束，时间的纯函数
    Frame/      头像框外观
    Trail/      足迹外观
    Shield/     各类检测，以及这个二进制对外自称是什么
  Assets/                        贴图、网格、故事
  res/values*/                   十种语言
Tools/                           八项检查
```

## 近期修复

最新的在最上面。每次修复都会更新这份列表。
- **离醒来的地方越远，世界就越是一块一块的。** Drei getrennte Mechanismen, alle mit dem Abstand zum Ursprung wachsend, keiner
  davon etwas, wovor GLSL warnt. Der Fragment-Shader deklariert
  `precision mediump float;` — er muss, eine Fragment-Stufe hat keinen Standard
  — und das Weltpositions-Varying ist highp, was das Varying schützt und sonst
  nichts. `vec3 surfaceFloor(vec3 wp)` nimmt einen *unqualifizierten* Parameter,
  die Position wurde also beim Aufruf abgeschnitten, vor jeder Arithmetik.
  mediump ist auf den meisten Mobil-Chips ein Half: bei 500 m ist sein Raster
  **0,25 m**, und der Teppich wird mit 41 Zyklen pro Meter abgetastet. Das ist
  die Pixeligkeit, und sie verdoppelt sich mit jeder Verdopplung der Entfernung.
  Die Taschenlampe zeigte es zuerst, weil `vWorldPos - uTorchPos` zwei große,
  fast gleiche Zahlen subtrahiert und `uTorchPos` ein mediump-Uniform war — bei
  500 m rastet der Strahlursprung auf ein Viertelmeter-Gitter, der Kegel landet
  in Stufen an der Wand. Und unter beidem `fract(sin(dot(p, k)) * 43758.5453)`,
  der Standard-GLSL-Hash, dessen Argument dort 9e6 erreicht, wo ein
  darstellbarer Float-Schritt ein **ganzes Radiant** ist, ein Sechstel einer
  Periode: er hasht nicht mehr, er bildet Bänder. Parameter und
  Positions-Uniforms sind jetzt highp, beide Hashes sind Integer-Hashes auf dem
  Gitterpunkt — exakt bei jeder Koordinate, zum selben Preis.
  `Shaders_Check.py` erzwingt alle drei, bewusst eng: GLSL rechnet mit der
  höchsten Operandenpräzision, also ist eine lokale Variable unkritisch und nur
  ein Parameter, ein Uniform oder dieser Hash kann es wirklich verlieren.
- **一个 Kotlin 文件，并且任何地方都没有注释。** `Service.kt` 和 `Settings.kt` 已并入 `Backrooms.kt` —— 同一个包，没有命名冲突，
  180 个 import 去重 —— 并且项目里所有注释都清掉了：Kotlin、Gradle KTS、C++ 以及
  Kotlin 原始字符串里的 GLSL 和 AGSL 中的 `//` 与 `/* */`；Python、YAML、TOML、
  CMake、ProGuard 和 properties 里的 `#`；XML 里的 `<!-- -->`。删掉 8059 行。用的是
  按语言分别处理的词法扫描而不是正则，因为正则会连字符串里的 `https://` 和 YAML 值里
  的 `#` 一起吃掉；`#version`、`#include` 和 shebang 都保留了，它们是指令而不是注释。
  有三处检查是按文件名写死 `Service.kt` 的，合并后就坏了 —— 文件名变成了契约的一部分，
  而它们真正要检查的是声明；现在它们读取存在的任何 Kotlin 源文件。已验证：八个工具
  全绿，每个 Python 文件都能编译，31 个 XML 全部能解析，22 个着色器依然能编译和链接。
- **唯一一个失败即崩溃的着色器，正是唯一没有被检查的着色器。** 上一条里重写的边框用 `fwidth(d)` 来确定边带宽度。AGSL 根本没有
  导数函数 —— 没有 `fwidth`，没有 `dFdx`，没有 `dFdy` —— 而 RuntimeShader 是在
  *构造函数*里、在主线程上、在组合过程中编译它的源码的。所以它并没有退化成一个朴素
  一点的按钮：它在大厅的第一帧抛出 `IllegalArgumentException`，应用根本起不来。
  `Shaders_Check.py` 从来没看见过它，因为它的着色器扫描要求有 `#version` 一行，而
  AGSL 没有：项目里唯一一个能把应用干掉的着色器，恰好被过滤器排除在外。现在它也检查
  AGSL —— 构建机上没有 SkSL 编译器，所以它拒绝 AGSL 没有的 GLSL 内置函数，并要求每个
  标识符都必须已声明，这正是把一行错误变成这次崩溃里那六条报错的东西。把 `fwidth`
  放回去验证过，另加一个拼写错误、`texture()` 和 `gl_FragCoord`：4/4。
- **而且装饰依然能让应用直接结束。** 微光只在 API 级别上做了保护，源码级别没有：编译不了的着色器于是不是「缺失」而是
  「致命」。现在它被包住了，一个不会发微光的按钮就只是一个按钮 —— 藤蔓层早就在遵守
  的同一条规则。
- **帧报告：把像素读回来，而不是对它们做假设。** `level drawn: 1 chunk(s)
  resident, 1748 triangles` 回答了上一轮的问题，也开启了新的一个：几何体到达了
  GPU，屏幕却依然是黑的 —— 这是完全另一个 bug。此前所有诊断量的都是某一步*有没有
  跑*，没有一个量它*产出了什么*。现在渲染器会在任何合成之前从场景目标读回一块
  24x24 的区域，合成之后再从默认 framebuffer 读一块，并把两者的最小／平均／最大
  亮度连同相机、三角形数、目标尺寸以及一切能让画面变暗的设置一起打印出来。大约在
  第 2、5、10 秒各一次，此后不再。两个数字不靠理论就能定论：场景亮而屏幕暗，说明
  合成把画面吃掉了；两者都暗，说明世界画了但没有光，或者相机根本没对着它。
- **大厅的藤蔓是个七边形。** `buildVineMesh` 扫的是七条边，而在这个粗细下，七边形
  管子的轮廓明显是直棱直角的 —— 那些平面块面不是任何抗锯齿能磨掉的，因为几何体本身
  就是那个形状。现在是十二边、34 个环，整个大厅 2520 个顶点。表面也配上了相称的
  材质：沿茎延伸的纤维、横过它的更粗的斑驳、把光收成一条带而不是一个圆点的各向异性
  高光，以及从纤细尖端透出来的光。
- **而且大厅里什么都没有做多重采样。** 藤蔓表面申请的是
  `setEGLConfigChooser(8, 8, 8, 8, 16, 0)` —— 完全没有多重采样 —— 而它是一块独立的
  surface，窗口自己的抗锯齿根本够不着它。每一条边都是硬邦邦的像素台阶。现在它会挑
  4x，退到 2x，再退到没有，而且永远不会抛异常：GLSurfaceView 的简单选择器在匹配不到
  时会在 GL 线程上抛 IllegalArgumentException，那会让 surface 塌掉，留下的正是这一轮
  开头那块黑色矩形。
- **按钮在你碰它之前一动不动。** 底下的底板在动，包着它的主体不动 —— 无论着色做得
  多好，这都会被读成一个扁平的矩形。现在它会呼吸：不到半个百分点的缩放、零点几度的
  倾斜，以及随之抬起的阴影，跑在三个互质的周期上，所以这个循环永远不会重复同一种
  组合。边框着色器也在圆角矩形距离场上重写了：旧的用的是 `min(uv.x, 1-uv.x, ...)`，
  那是圆底板上的一个**方形**衰减，会把辉光堆到四个角上。
- **供给方漏掉的一个区块，会在这一局余下的时间里被永久注销。** 着色器和防护的毛病
  都清掉之后，一份 Mali-G68 的日志回来得干干净净 —— 程序都链接了，framebuffer 完整，
  防护 `CLEAN` —— 而世界依然是黑的，HUD 浮在上面。`streamChunks` 对一次落空的回应，
  是往缓存里放一个空的 `ChunkMesh()`，此后 `containsKey` 就永远跳过它：用一个暂时的
  答案做了一个永久的决定。`fetchChunk` 在世界尚未有效期间一直返回 null，所以在 GL
  线程跑到世界创建之前的设备上，环内四十九个区块会在头四十九帧里全部被注销，玩家就
  站在虚无里。它取决于时序，这正是它找上三位测试者却从没找上这台机器的原因。落空现在
  进入一张重试表，二十帧之后再问一次，而 `Kotlin_Check.py` 把这条规则写了下来 ——
  渲染器需要 GL 上下文和 Android classpath，这里没有任何工具能跑它的哪怕一帧，而两种
  写法都能编译。
- **渲染器从来没说过自己到底有没有画出东西。** 三份黑屏报告都带着完整日志，却没有
  一份回答那个最该先问的问题：有没有一个三角形到达 GPU？没有几何体的场景会被清成
  0.02 的灰，也就是黑屏，而上游的一切都报告成功。现在关卡第一次画出来时会记录一行 ——
  区块数、三角形数、等待重试的区块数 —— 三秒内一个都没有的话，也会警告一次，并说明
  世界是否有效、区块供给方到底有没有被赋值。「什么都没画」和「画了但你看不见」是两个
  毫无共同点的 bug。
- **十二个程序无一例外地在阶段之间自相矛盾。** Galaxy S23 报出的是一个 uniform；
  一台 Mali 的 Galaxy A17 用不同措辞说了同一件事 —— `L0001 The fragment
  floating-point variable uGrowth does not match the vertex variable uGrowth.
  The precision does not match.` 把十二组全部量了一遍，又翻出 25 处：在顶点阶段写作
  `out vec3 vNormal`（默认 highp），在片元阶段读作 `in vec3 vNormal`（按它自己的
  `precision` 行是 mediump）的 varying。ES 3.00 规范对 varying 是允许这样的 ——
  它只要求 uniform 必须一致 —— 而 glslang 遵循规范，所以什么都没标出来。驱动可没
  这么统一，而 Mali 的报错甚至不区分 varying 和 uniform。这 25 处现在全部钉死在顶点
  阶段的精度上，`Shaders_Check.py` 也对两类都强制一致 —— 这是有意比规范更严。
- **一帧画进了虚空，看起来和黑屏一模一样。** 渲染器建了一个离屏颜色目标、一个深度
  缓冲和一对半分辨率的 bloom 目标，却一次都没调用过
  `glCheckFramebufferStatus`。不完整的 framebuffer 不是任何驱动会报告的错误：画进
  去的每一笔都被丢弃，这一局照常推进，HUD 照常叠在上面，而世界干脆不在 —— 而且任何
  日志里都没有一行可供着手。现在每次重建都会检查完整性，失败时是降级而不是消失：
  bloom 一对不可用就损失光晕，场景目标不可用就把这一帧不走后处理链直接送到屏幕。
- **而且没有任何地方说明是哪块 GPU 在画。** 两份黑屏报告都带着完整日志，却都没有
  写出驱动，只能靠型号去猜。GL 的厂商、渲染器、版本和 GLSL 版本现在每个上下文记录
  一次，链接失败的程序也会报出自己的名字 —— 场景程序没有被任何东西包住，所以它的
  失败以前是一次赤裸的崩溃，连十二个里倒了哪一个都不说。
- **大厅按钮上出现一块黑色矩形，起因是一个编译完全正常的着色器。** 来自一台
  Galaxy S23 的日志：`Omni program link failed: Error: Uniform uGrowth precision
  mismatch with other stage.` 藤蔓着色器的两半各自都是合法的 —— 这正是本工具几个
  月来一直放行它们的原因。编译只是构建一个程序的一半。同名 uniform 必须在各阶段之
  间在类型**和精度**上都一致，而顶点着色器默认把 `float` 视为 `highp`，片元着色器
  却根本没有默认值，所以这里每个片元着色器都写着 `precision mediump float;`。一个
  被两个阶段同时读取、又没写精度限定符的 float uniform，按构造就是一处不匹配。宽松
  的驱动照样能链接，它就是这样进入正式版的。`uGrowth` 现在在两个阶段都显式写作
  `highp`，而 `Shaders_Check.py` 不再只编译两半，而是把每个出现在
  `linkGlProgram(V, F)` 调用里的组合都链接一遍 —— 顺带还能查出不属于任何程序的
  着色器。
- **而且失败时它没有优雅退化，而是盖住了按钮。** 藤蔓层原本就捕获了自己的初始化
  失败并且什么都不画，听起来安全，其实不然：这个 view 调用了
  `setZOrderOnTop(true)`，把它的 surface 放在整个窗口之上，而不是按钮内部。一个
  从不提交任何一帧的 surface 不是「没有藤蔓」，而是覆盖在按钮及其文字上的一个不透明
  空洞。画不出来的装饰层现在会彻底退出组合，按钮也就回到它那块绘制好的底板。
- **防护把一台干净的手机指认为已 root。** 一位玩家拍下了游戏内的安全提示：
  `原因：root，flags=0x40200`。两个位，而所有真正的 root 位 —— ROOT_BINARY、
  ROOT_PROPS、ROOT_PATHS、MAGISK、ZYGISK、KSU、SELINUX_OFF —— 全都没亮。那不是
  一台 root 过的设备，而是我们自己的两处检查。**SHADOW_MOUNT** 在整张挂载表上
  问 `containsCI(m,"overlay") && containsCI(m,"/system")`：两次互不相关的搜索，
  于是 `/vendor/overlay` 上的一个 overlay 和 `/system_ext` 那一行 —— 两者在任何
  Android 11+ 手机上都是原装配置 —— 合起来就成了「root」，那是 HIGH，那就是弹窗。
  **PTRACE_TRACED** 调用 `PTRACE_TRACEME`，再试图用 pid 0 的 `PTRACE_DETACH`
  撤销，这根本不可能：detach 需要被追踪者真实的 pid，会以 `ESRCH` 失败，进程就
  一直被父进程追踪着。下一轮扫描的 TRACEME 于是返回 `EPERM` ——「已被追踪」——
  也就是说，在任何设备上，大约五秒之后，永久如此，因为标志字在整局游戏中是按位
  或累积的。这还让我们丢了崩溃报告：一个卡在被追踪状态的进程，会把信号送给一个
  永远不会等待的追踪者，于是真正的 SIGSEGV 会挂起而不是崩溃。两处检查现在都改为
  解析问题真正涉及的字段，而不是在文件里找子串，弹窗也会带上它所反对的那一行挂载。
- **而 Frida 检查则是在拿玩家这一局掷硬币。** 是在读上面两处时发现的。
  `fridaPort` 在 `/proc/net/tcp` 里搜索字面量 `6D58`、`71D4`、`2717` 和 `5039`
  —— 它们是端口 27992、29140、10007 和 20537，并不是 Frida 的 27042-27045；有人
  把十进制数字写在了该写十六进制的地方。写错只是较小的那一半。它们是以子串方式
  去匹配一个几乎全由十六进制组成的文件：在这台构建机 19 个套接字的表里，
  **65536 个可能的四位十六进制针中已经有 220 个出现过**，而一部手机的套接字要多
  出好几倍。命中某个 inode 号里的一段就会点亮 FLAG_FRIDA_PORT，那是 CRITICAL，
  而 CRITICAL 会调用 `killProcess`。现在它解析本地地址列并要求状态为 `0A`
  （LISTEN），这样一条通往别人 Frida 的出站连接就不会被读成这里跑着一个服务端。

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
- **Firebase 从来没有工作过，还带走了不少东西。** 这里没有 google-services.json，
  CI 注入的是占位文件：每一条 Crashlytics 日志、每一次 Firestore 写入、每一次
  Remote Config 拉取都在运行时失败，然后被 `runCatching` 吞掉。REST API 也是同样的
  故事，api.omnibackrooms.com 根本解析不出来；底下的网络代码在清空一个没人往里发东西
  的 socket——语音聊天就在那里。全部删除，连同没有任何引用的 Room、Billing 和
  Credential Manager。
- **手电筒是屏幕正中的一个圆。** 画在后处理的 uv (0.5, 0.47) 上，在世界里没有位置，
  这正是光看起来从她胸口射出的原因。现在它是场景着色器里真正的聚光灯，从模型的镜头射出。
- **已拥有的足迹无法装备。** 接连三个缺陷。
- **通知权限是压在开场动画上问的。** 那道门在 NavHost 旁边而不是里面。
- **两张贴图不是二的幂。** 1536x1024 和 1448x1086，因此无法携带 mipmap 链。四张都改成
  1024x1024；资源从 6.0MB 降到 4.7MB。
- **角色有四条手臂。** 网格里装着两对：一个双臂垂在身侧的身体，以及一件袖子以 T 字姿势笔直
  伸出的连衣裙。骨骼被摆在袖子上，于是绑定挥动的是空布料，而玩家看得见的手臂始终焊在胯部。
  现在袖子落在手臂上，绑定沿表面而非穿过空气来量距离——裙摆从手边 4 厘米处经过，任何直线
  尺度都分不开这两者。相隔一毫米重复的八个壳也随之消失：1139 个顶点，以及它们造成的
  z-fighting。
- **第0层里是一群怪物。** 三到八只，每十二秒补充一次。一群只是热闹，并不吓人。
  现在正好一只，难度改变的是这一只是什么，而不是有几只。
- **怪物能看穿墙。** 视线是一个完全无视关卡的距离判定，所以摆脱它的唯一办法
  就是跑得比它快。
- **把它赶走等于永久删除它。** 撤退是以玩家当前位置来量距离的，所以跟着它走
  会让它永远逃下去；而驻留状态每一帧都把它的消散重置回去，于是回归永远无法
  完成。两个都是靠模拟发现的，两个在真机上都找不到。
- **每一种合成噪声里有百分之八是重复的采样。** C++ 和 Python 都把噪声下标取作
  `int(t * 44100)`，而在浮点下 `i/44100*44100` 会比 `i` 低那么一点点。耳朵听
  得出来，波形上看不出来。
- **土耳其语玩家在房间人数标签上看到的是一个字面的 `%d`**：这条字符串带着格式
  占位符，却是不传参数直接画出来的。
- **默认资源是土耳其语的。** `values/` 是 Android 为没有对应条目的语言回退到的
  地方，所以任何未翻译的字符串都会以土耳其语出现在一份德语菜单中间。现在那里
  放的是英语。
- **CI 在代码没问题时报告失败。** 两个作业争抢唯一的 runner；静态检查从未开始，
  在队列里超时，然后让整次运行失败——而 APK 每一次都构建得好好的。
- **角色看起来有四条手臂。** 骨架把旋转角乘上了一个位置梯度，这会把肢体摊成
  扇形而不是转动它。已换成基于十二根骨头的真正 linear blend skinning。
- **防篡改保护每次启动都指控干净的设备**，原因是对 `/proc/self/maps` 做了一次
  赤裸裸的子串搜索。现在它会说明自己找到了什么，并把原因写进
  `Documents/Backrooms_Log/`。
- **天花板贴图沿每块砖的对角线被镜像了**：发射器按固定的角点顺序给出 UV，而这
  只对反向缠绕的四边形才是正确的。

## 许可

保留所有权利。代码放在这里是为了被阅读。

