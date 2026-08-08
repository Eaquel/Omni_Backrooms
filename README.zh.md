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
| `Shaders_Check.py` | GLSL 写在 Kotlin 的原始字符串里，所以一个编译不过的着色器在使用它的界面打开并变黑之前都是不可见的。每一个都用 `glslangValidator` 编译。 |
| `Assets_Check.py` | `aapt2` 接受却画得乱七八糟的手写矢量图标；不再与世界坐标对应的网格 UV；跑出背景的观赏相机；重复以及从未被引用的资源；落后的语言；自相矛盾的 Unity 伪装。还有 `--optimise`——一个无损的 PNG 重编码器。 |
| `Native_Check.py` | JNI 契约。Kotlin 声明 `external fun`，C++ 定义 `Java_..._name`，而在构建期**没有任何东西**把两边连起来——Kotlin 编译器不会，C++ 编译器不会，链接器也不会。单边改名意味着首次调用时的 `UnsatisfiedLinkError`；参数个数变了更糟，因为 JNI 按名字绑定，会一声不吭地从栈上读走多出来的参数。 |
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
    Frame/      头像框外观
    Trail/      足迹外观
    Shield/     各类检测，以及这个二进制对外自称是什么
  Assets/                        贴图、网格、故事
  res/values*/                   十种语言
Tools/                           八项检查
```

## 近期修复

最新的在最上面。每次修复都会更新这份列表。

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

