[English](README.md) ·
[Türkçe](README.tr.md) ·
[Deutsch](README.de.md) ·
[Español](README.es.md) ·
[Français](README.fr.md) ·
[Italiano](README.it.md) ·
[Português](README.pt.md) ·
[Русский](README.ru.md) ·
**日本語** ·
[中文](README.zh.md)

# Omni Backrooms

レベル0を舞台にした Android のサバイバルホラー。果てしなく続く単調な黄色い
オフィスの廊下、湿ったカーペット、うなる蛍光灯、そして殺すことのできない何かが
そこにあなたと一緒にいます。

ゼロから書かれています。描画は Kotlin から駆動する OpenGL ES 3.0、シミュレーション
は NDK 経由の C++、そしてレベルはマップファイルではなくセル座標の純粋な関数です。
だから終わりがなく、継ぎ目がひとつも繰り返されません。

## 中身

| | |
|---|---|
| **レベル0** | 無限。床も壁も光も湿り気も、セルごとに自身の座標とそのランのシードから導かれます。だから一度もデータをやりとりしない二人のプレイヤーにとって、世界は完全に同一です。 |
| **一体のクリーチャー** | 群れではありません。レイで見るので壁は本当にあなたを隠します。あなたがどれだけ音を立てているかで聞くので、しゃがむことに本当に意味があります。そして最後にあなたを見た場所を覚えています。 |
| **懐中電灯** | まず遅くし、やがて追い払います。倒すのではありません。Backrooms では何も死にません。退き、薄れ、離れた場所で待ち、再びあなたを見つけるか、あなたの油断を聞きつければ戻ってきます。 |
| **音声ファイルなし** | すべての音は端末上で合成されます。APK には WAV も OGG も一つも入っていません。 |
| **10言語** | トルコ語、英語、ドイツ語、スペイン語、フランス語、イタリア語、ポルトガル語、ロシア語、日本語、中国語 — 中途半端ではなく完全に。初回起動時に端末の言語をゲームが自分で選びます。 |
| **見た目だけ** | フレーム、足あと、キャラクター。ゲーム内で売られているもので、遊び方に影響するものは一つもありません。 |

## ビルド

```bash
git clone https://github.com/Eaquel/Omni_Backrooms.git
cd Omni_Backrooms
./gradlew :Backrooms:assembleRelease
```

JDK 25、Android SDK 36、NDK、CMake 4.3.2 が必要です。リリースビルドはこの
リポジトリに含まれないキーストアで署名されます。`assembleDebug` には追加のものは
要りません。

## チェック

`Tools/` にある8つのツールのうち7つが push のたびに走ります。どれも Gradle の
ビルドでは本当に見えないものを守っているから存在しています。

| ツール | 何を捕まえるか |
|---|---|
| `Shaders_Check.py` | GLSL は Kotlin の raw 文字列の中にあります。コンパイルできないシェーダーは、それを使う画面を開いて真っ黒になるまで見えません。すべて `glslangValidator` でコンパイルします。 |
| `Assets_Check.py` | `aapt2` が受け入れて崩れて描かれる手書きのベクターアイコン、ワールド座標と一致しなくなったメッシュ UV、背景から外れる観賞用カメラ、重複や未参照のアセット、取り残された言語、自己矛盾する Unity 偽装。加えて `--optimise` — 可逆の PNG 再エンコーダ。 |
| `Native_Check.py` | JNI の契約。Kotlin が `external fun` を宣言し、C++ が `Java_..._name` を定義しますが、ビルド時に両者を結ぶものは**何もありません**。Kotlin コンパイラも、C++ コンパイラも、リンカも。片側だけの改名は初回呼び出しでの `UnsatisfiedLinkError` であり、引数の数の違いはもっと悪い — JNI は名前で結びつけ、余った引数をスタックから黙って読むからです。 |
| `Kotlin_Check.py` | すべての import を、その裏にある依存と双方向で突き合わせる。ここの Kotlin は Android のクラスパスなしでコンパイルするので、本当に消えたライブラリと単にパスに無いライブラリが見分けられない。Firebase を外したとき `androidx.media3` が黙って道連れになったのはこれが理由。 |
| `Level_0_Check.py` | 多数のシードで出現地点から世界を塗りつぶし、出口に本当に到達できることを証明します。到達できない出口はクリア不能なランで、しかも完全に無音です。 |
| `Entity_Check.py` | 本物の AI をコンパイルし、本物のレベル0にクリーチャーを置いて観察します。壁に遮られる視界、音に比例する聴覚、そして決して固まってはならない撤退と帰還のサイクル。 |
| `Code_To_Sound.py` | 実際に出荷される C++ の生成器をレンダリングし、Python のリファレンスとサンプル単位で比較します。WAV も書き出すので、コードとしてしか存在しない音を実際に聴けます。 |

すべて実行:

```bash
for t in Shaders Assets Native Entity Kotlin; do python3 Tools/${t}_Check.py; done
python3 Tools/Level_0_Check.py 40
python3 Tools/Code_To_Sound.py
```

ここにあるチェックはすべて、そのバグをわざと戻して失敗することを確かめてあります。
一度も失敗したことのないチェックを信じる理由は、誰にもありません。

## 構成

```
Backrooms/Source/Main/
  Kotlin/com/omni/backrooms/     UI・描画・ゲームループ    (約14,000行)
  Native/                        NDK 経由の C++           (約3,900行)
    Map/        座標の純粋関数としてのレベル0
    Entity/     クリーチャーの AI — 知覚、撤退、帰還
    Sound/      すべての生成器。音声ファイルは存在しません
    Frame/      プロフィールフレームの装飾
    Trail/      足あとの装飾
    Shield/     検知器と、バイナリが自分を何と名乗るか
  Assets/                        テクスチャ、メッシュ、ストーリー
  res/values*/                   10言語
Tools/                           8つのチェック
```

## 最近の修正

新しい順。この一覧は修正のたびに更新されます。

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
- **Firebase は一度も動いていなかったし、多くを道連れにした。** ここに
  google-services.json は無く、CI はプレースホルダを差し込む。つまり Crashlytics
  のログも Firestore の書き込みも Remote Config の取得も、実行時に失敗しては
  `runCatching` に飲み込まれていた。REST API も同じで、api.omnibackrooms.com は
  名前解決すらしない。その下のネットコードは誰も送らないソケットを空にしていた。
  ボイスチャットもそこにあった。全部削除。参照ゼロだった Room、Billing、
  Credential Manager も一緒に。
- **懐中電灯は画面中央の円だった。** ポスト処理で uv (0.5, 0.47) に描かれ、
  ワールド上の位置を持たなかった。光が胸から出ているように見えた理由はこれ。今は
  シーンシェーダの本物のスポットライトで、モデルのレンズから出る。
- **所持している足跡を装備できなかった。** 三つの不具合が連なっていた。
- **通知の許可をイントロの上から訊いていた。** ゲートが NavHost の中ではなく隣に
  あった。
- **二枚のテクスチャが2の冪でなかった。** 1536x1024 と 1448x1086 で、ミップマップ
  連鎖を持てない。四枚とも 1024x1024 に。アセットは 6.0MB から 4.7MB へ。
- **キャラクターの腕が四本あった。** メッシュに二組入っていた。腕を体側に下ろした身体と、
  袖がTポーズでまっすぐ張り出したドレスである。ボーンは袖の上に置かれていたので、リグは空の
  布を振り、プレイヤーに見える腕は腰に溶接されたまま動かなかった。いま袖は腕の上にあり、
  バインドは空中ではなく表面に沿って距離を測る。スカートの裾は手の 4cm 脇を通るので、直線の
  尺度ではこの二つを区別できない。1mm ずらして複製されていた八つのシェルも一緒に消えた。
  頂点 1139 個と、それが起こしていた z-fighting である。
- **レベル0に群れがいました。** 3〜8体、12秒ごとに補充。群れは忙しいだけで
  怖くありません。今はちょうど1体で、難易度は「何体いるか」ではなく「その1体が
  どういうものか」を変えます。
- **クリーチャーが壁越しに見ていました。** 視界はレベルを完全に無視した距離
  判定で、接触を切る手段は振り切ることしかありませんでした。
- **追い払うと永久に消えていました。** 撤退は現在のプレイヤー位置から距離を
  測っていたため、追いかけると永遠に逃げ続け、待機状態は毎ティック消え具合を
  リセットしていたため帰還が完了できませんでした。どちらもシミュレーションで
  発見され、どちらも実機では見つけられません。
- **合成ノイズの8パーセントが同じサンプルの繰り返しでした。** C++ も Python も
  ノイズの添字を `int(t * 44100)` としていましたが、float では `i/44100*44100`
  が `i` をわずかに下回ります。耳では分かり、波形では見えません。
- **トルコ語のプレイヤーには部屋サイズのラベルに `%d` がそのまま出ていました。**
  書式指定子を含む文字列を、引数なしで描画していたためです。
- **既定のリソースがトルコ語でした。** `values/` は Android が該当エントリの
  ない言語のために参照する場所なので、未翻訳の文字列はドイツ語メニューの中に
  トルコ語で現れていました。今は英語が置かれています。
- **CI が正常なコードで失敗を報告していました。** 2つのジョブが1つのランナーを
  奪い合い、静的チェックは一度も始まらず、キューでタイムアウトして実行全体を
  失敗させていました。APK は毎回問題なくビルドされていたのに。
- **キャラクターが四本腕に見えていました。** リグが回転角に位置勾配を掛けて
  いたためで、これは手足を回すのではなく扇状に広げます。12ボーンのスケルトンに
  よる本物の linear blend skinning に置き換えました。
- **改ざん検知が正常な端末を毎回起動時に告発していました。**
  `/proc/self/maps` の素朴な部分文字列検索が原因です。今は何を見つけたのかを
  報告し、理由を `Documents/Backrooms_Log/` に書き出します。
- **天井のテクスチャがタイルごとに対角線で反転していました。** エミッタが UV を
  固定の頂点順で渡していたためで、その順は逆巻きの四角形にしか正しくありません。

## ライセンス

無断転載を禁じます。コードは読まれるためにここにあります。
