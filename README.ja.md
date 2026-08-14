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
| `Shaders_Check.py` | GLSL は Kotlin の raw 文字列の中にあります。コンパイルできないシェーダーは、それを使う画面を開いて真っ黒になるまで見えません。すべて `glslangValidator` でコンパイルします。 さらに `linkGlProgram(V, F)` の呼び出しに現れる組はすべてリンクされる。個別にコンパイルできる二つのシェーダでも、プログラムを組めないことがあるからだ —— 同名の uniform はステージ間で型と精度が一致していなければならない。 |
| `Assets_Check.py` | `aapt2` が受け入れて崩れて描かれる手書きのベクターアイコン、ワールド座標と一致しなくなったメッシュ UV、背景から外れる観賞用カメラ、重複や未参照のアセット、取り残された言語、自己矛盾する Unity 偽装。加えて `--optimise` — 可逆の PNG 再エンコーダ。 |
| `Native_Check.py` | JNI の契約。Kotlin が `external fun` を宣言し、C++ が `Java_..._name` を定義しますが、ビルド時に両者を結ぶものは**何もありません**。Kotlin コンパイラも、C++ コンパイラも、リンカも。片側だけの改名は初回呼び出しでの `UnsatisfiedLinkError` であり、引数の数の違いはもっと悪い — JNI は名前で結びつけ、余った引数をスタックから黙って読むからです。 さらに、ディスク上に用意した `/proc` に対してガードの検出器を実際に走らせる。誰も実行できない root チェックとは、ふつうの端末を告発する root チェックのことだからだ。 |
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
    Ending/     ランの終わり方。時間の純粋関数
    Frame/      プロフィールフレームの装飾
    Trail/      足あとの装飾
    Shield/     検知器と、バイナリが自分を何と名乗るか
  Assets/                        テクスチャ、メッシュ、ストーリー
  res/values*/                   10言語
Tools/                           8つのチェック
```

## 最近の修正

新しい順。この一覧は修正のたびに更新されます。
- **Kotlin はひとつのファイルに、そしてどこにもコメントは無い。** `Service.kt` と `Settings.kt` を `Backrooms.kt` に統合した —— 同一パッケージ、
  名前の衝突なし、import は 180 件に重複排除 —— そしてプロジェクト中のコメントを
  すべて削除した：Kotlin・Gradle KTS・C++、および Kotlin の生文字列に入っている
  GLSL と AGSL の `//` と `/* */`；Python・YAML・TOML・CMake・ProGuard・properties
  の `#`；XML の `<!-- -->`。8059 行を削除。正規表現ではなく言語ごとのトークナイザで
  行った。正規表現は文字列中の `https://` や YAML の値の中の `#` まで食べてしまう
  からだ。`#version`・`#include`・シバンは残してある —— これらはディレクティブで
  あってコメントではない。三つのチェックが `Service.kt` をファイル名で直接開いて
  いて統合で壊れた —— 検査していたのは宣言なのに、ファイル名が契約の一部になって
  いたのだ。いまは存在する Kotlin ソースを読む。検証済み：八つのツールすべて緑、
  Python は全ファイルがコンパイル、XML は 31 件すべてパース、シェーダは 22 本すべて
  引き続きコンパイルとリンクに成功。
- **失敗が即クラッシュになる唯一のシェーダが、何にも検査されていない唯一のシェーダだった。** 前項で書き直した縁は、帯の太さを `fwidth(d)` で決めていた。
  AGSL には微分関数が一切ない —— `fwidth` も `dFdx` も `dFdy` もない —— そして
  RuntimeShader はソースを*コンストラクタ*で、メインスレッド上で、コンポジションの
  中でコンパイルする。だから地味なボタンに劣化したのではなく、ロビーの最初のフレーム
  で `IllegalArgumentException` を投げ、アプリが起動しなくなった。
  `Shaders_Check.py` はこれを一度も見ていない。シェーダ走査が `#version` 行を要求
  していて、AGSL にはその行がないからだ：アプリを落としうる唯一のシェーダが、
  フィルタの外にいた。いまは AGSL も検査する —— ビルドマシンに SkSL コンパイラは
  ないので、AGSL に存在しない GLSL 組み込みを拒否し、すべての識別子が宣言済みである
  ことを要求する。一行の誤りをこのクラッシュの六件のエラーに変えたのがそれだ。
  `fwidth` を戻し、さらに綴り間違い・`texture()`・`gl_FragCoord` で検証：4/4。
- **そして装飾は依然としてアプリを終わらせられた。** きらめきは API レベルでは守られていたが、ソースについては守られていなかった。
  ビルドできないシェーダは「無い」ではなく「致命的」だったわけだ。いまは包まれていて、
  きらめけないボタンはただのボタンになる —— 蔓レイヤーがとうに従っている規則と同じだ。
- **フレームレポート：ピクセルについて推測せず、読み戻す。** `level drawn:
  1 chunk(s) resident, 1748 triangles` は前回の問いへの答えであり、同時に新しい
  問いの始まりだった —— ジオメトリは GPU に届いていて、それでも画面は黒い。これは
  まったく別のバグだ。これまでの診断はどれも「その工程が*走ったか*」を測っていて、
  「何を*生んだか*」を測るものは一つもなかった。いまレンダラは、何かが合成される
  前にシーンターゲットから 24x24 の領域を、合成のあとに既定のフレームバッファから
  もう一枚を読み戻し、両方の最小／平均／最大輝度を、カメラ・三角形数・ターゲット
  寸法・画面を暗くしうるあらゆる設定と並べて出力する。およそ 2 秒・5 秒・10 秒の
  三回だけで、以後は出さない。二つの数字が理屈なしに決着をつける：シーンが明るく
  画面が暗ければ合成が絵を食っている。両方暗ければ、world は描かれているが光が
  当たっていないか、カメラがそこを向いていない。
- **ロビーの蔓は七角形だった。** `buildVineMesh` は七面を掃引していた。この太さの
  七面チューブは輪郭がはっきり直線的になる —— どんなアンチエイリアスでも和らがない
  平らな面だ。ジオメトリが本当にその形なのだから。いまは十二面・34 リング、ロビー
  全体で 2520 頂点。表面にもそれ相応のマテリアルがついた：茎に沿って走る繊維、
  それを横切るより粗いむら、光を丸い点ではなく帯として捉える異方性の照り、そして
  細い先端を透ける光。
- **そしてロビーには多重サンプリングが一切なかった。** 蔓のサーフェスは
  `setEGLConfigChooser(8, 8, 8, 8, 16, 0)` を要求していた —— 多重サンプリングなし
  —— しかもこれは独立したサーフェスなので、ウィンドウ側のアンチエイリアスは決して
  届かない。すべての縁が硬いピクセルの階段だった。いまは 4x を選び、駄目なら 2x、
  それも駄目ならなしへ下がり、決して例外を投げない：GLSurfaceView の単純な chooser
  は一致が見つからないと GL スレッドで IllegalArgumentException を投げ、それが
  サーフェスを落として、この回の発端になった黒い矩形をそのまま残す。
- **ボタンは触るまで微動だにしなかった。** 下の板は動いていて、それを収めている
  本体は動いていなかった —— どれだけ丁寧に陰影をつけても平らな矩形に見えるのはこれ
  が理由だ。いまは呼吸する：スケールで 0.5% 未満、傾きで一度の何分の一、そして
  それに合わせて持ち上がる影。三つの互いに素な周期に載せてあるので、ループが同じ
  組み合わせを繰り返すことはない。縁のシェーダも角丸矩形の距離場の上に組み直した：
  以前は `min(uv.x, 1-uv.x, ...)` を使っていて、丸い板の上の**四角い**減衰だった
  ため、光が四隅に溜まっていた。
- **プロバイダが取りこぼしたチャンクは、そのプレイの残り全部で切り捨てられていた。**
  シェーダとガードの不具合が消えたあと、Mali-G68 のログはまったく綺麗に返ってきた ——
  プログラムはリンクされ、フレームバッファは完全、ガードは `CLEAN` —— それでも世界は
  黒いままで、HUD だけがその上にあった。`streamChunks` は取りこぼしに対して空の
  `ChunkMesh()` をキャッシュへ入れて答えており、以後 `containsKey` がそれを永久に
  読み飛ばしていた。一時的な答えから下した恒久的な決定だ。`fetchChunk` は世界がまだ
  有効でないあいだ null を返すので、GL スレッドが世界の生成を追い越す端末では、リング
  内の四十九チャンクが最初の四十九フレームで全部切り捨てられ、プレイヤーは何もない
  場所に立つ。タイミング依存であり、だからこそテスター三人に届いてこのマシンには一度も
  来なかった。取りこぼしはリトライ表に入り、二十フレーム後にもう一度尋ねられるように
  なった。`Kotlin_Check.py` はその規則を明文化している —— レンダラは GL コンテキストと
  Android のクラスパスを要るので、ここのどのツールも一フレームすら走らせられず、
  どちらの書き方もコンパイルは通るからだ。
- **レンダラは、自分が何かを描いたかどうかを一度も言わなかった。** 黒画面の報告が
  完全なログ付きで三件届いたが、どれも最初に問うべきことに答えていなかった ——
  三角形は GPU に届いたのか？ ジオメトリのないシーンは 0.02 のグレーでクリアされる、
  つまり黒い画面であり、その上流はすべて成功を報告する。いまはレベルが初めて描かれた
  ときに一度記録する —— チャンク数、三角形数、リトライ待ちのチャンク数 —— そして三秒
  経っても一つも描かれなければ警告を一度出し、世界が有効だったか、チャンクプロバイダ
  がそもそも設定されていたかを示す。「何も描かなかった」と「描いたが見えなかった」は
  共通点のない別々のバグだ。
- **十二本のプログラムすべてが、ステージ間で自分と食い違っていた。** Galaxy S23 は
  uniform の名を挙げた。Mali の Galaxy A17 は同じことを別の言い回しで言った ——
  `L0001 The fragment floating-point variable uGrowth does not match the vertex
  variable uGrowth. The precision does not match.` 十二組すべてを測ったら、さらに
  25 件出てきた：頂点ステージで `out vec3 vNormal`（既定で highp）と書かれ、
  フラグメントステージで `in vec3 vNormal`（自身の `precision` 行により mediump）
  と読まれる varying だ。ES 3.00 仕様は varying についてはこれを許している ——
  一致を要求するのは uniform だけ —— そして glslang は仕様に従うので、何も指摘
  されなかった。ドライバはそこまで一様ではないし、Mali のメッセージは varying と
  uniform を区別すらしない。25 件はすべて頂点ステージの精度に固定され、
  `Shaders_Check.py` は両方について一致を要求するようになった。仕様より厳しいのは
  意図的だ。
- **どこにも描かれないフレームは、黒い画面とまったく同じに見える。** レンダラは
  オフスクリーンのカラーターゲットと深度バッファ、半解像度の bloom ペアを作りながら、
  `glCheckFramebufferStatus` を一度も呼んでいなかった。不完全なフレームバッファは
  どのドライバも報告しないたぐいの故障だ：そこへの描画はすべて捨てられ、プレイは
  進み続け、HUD はその上に重なり続け、世界だけが存在しない —— しかもどのログにも
  手がかりの一行がない。いまは再構築のたびに完全性を確認し、失敗したときは消える
  のではなく劣化する：bloom ペアが使えなければハローを失い、シーンターゲットが
  使えなければポスト処理を通さずそのまま画面へ送る。
- **そして、どの GPU が描いているのかをどこも言っていなかった。** 黒画面の報告が
  完全なログ付きで二件届いたが、どちらもドライバを示しておらず、型番から推測する
  しかなかった。GL のベンダ、レンダラ、バージョン、GLSL バージョンはコンテキスト
  ごとに一度記録されるようになり、リンクに失敗したプログラムは自分の名を告げる ——
  シーンプログラムは何にも包まれていないので、その失敗は十二本のどれが落ちたのかも
  言わない裸のクラッシュだった。
- **ロビーのボタンの上に黒い矩形が出た。原因は、問題なくコンパイルできる
  シェーダだった。** Galaxy S23 のログから：`Omni program link failed: Error:
  Uniform uGrowth precision mismatch with other stage.` 蔓シェーダの両半分は
  それぞれ単体では正しい —— このツールが何か月も通し続けていたのはまさにそのため
  だ。コンパイルはプログラムを組み立てる作業の半分でしかない。同じ名前の uniform
  は、ステージ間で型**と精度**の両方が一致していなければならない。頂点シェーダは
  `float` を既定で `highp` にするが、フラグメントシェーダには既定がまったくない
  ので、ここのフラグメントシェーダはどれも `precision mediump float;` と書いて
  いる。両ステージが読む float の uniform を修飾子なしで置けば、それは構造上の
  不一致だ。寛容なドライバはそれでもリンクしてしまう。まさにそうやって製品版に
  乗った。`uGrowth` は両ステージで明示的に `highp` になり、`Shaders_Check.py` は
  半分ずつコンパイルするだけでなく、`linkGlProgram(V, F)` の呼び出しに現れる
  すべての組をリンクするようになった —— ついでに、どのプログラムにも属さない
  シェーダも見つかる。
- **しかも失敗は劣化ではなく、ボタンを覆い隠していた。** 蔓レイヤーは自分の
  セットアップ失敗をすでに捕まえて何も描かないようにしていた。安全そうに聞こえる
  が、そうではない：この view は `setZOrderOnTop(true)` を呼び、サーフェスを
  ボタンの中ではなくウィンドウ全体の上に置く。一度もフレームを出さないサーフェス
  は「蔓が出ない」ではなく、ボタンとそのラベルの上に空いた不透明な穴だ。描けない
  装飾レイヤーはコンポジションから完全に抜けるようになり、ボタンは描画済みの
  プレートに戻る。
- **ガードが無改造の端末を root 済みだと告発していた。** あるプレイヤーがゲーム内
  のセキュリティダイアログを撮影してくれた：`理由: root, flags=0x40200`。二つの
  ビットだけで、本物の root ビット —— ROOT_BINARY、ROOT_PROPS、ROOT_PATHS、
  MAGISK、ZYGISK、KSU、SELINUX_OFF —— はすべて消えていた。root 済みの端末では
  なく、こちらの二つのチェックが原因だった。**SHADOW_MOUNT** はマウント表全体に
  対して `containsCI(m,"overlay") && containsCI(m,"/system")` を尋ねていた。
  独立した二つの検索なので、`/vendor/overlay` 上の overlay と `/system_ext` の行
  —— どちらも Android 11 以降のあらゆる端末に最初からある —— が合わさって「root」
  になり、それが HIGH になり、ダイアログになる。**PTRACE_TRACED** は
  `PTRACE_TRACEME` を呼び、それを pid 0 の `PTRACE_DETACH` で取り消そうとして
  いた。これは成立しない：detach には被トレース側の本当の pid が要り、`ESRCH` で
  失敗し、プロセスは親にトレースされたまま残る。次のスキャンの TRACEME はそこで
  `EPERM` ——「すでにトレース中」—— を返す。つまりどの端末でも、開始から約五秒後
  に、恒久的に。フラグ語はプレイ中ずっと OR で積み上がるからだ。これはクラッシュ
  レポートも奪っていた：被トレースのまま固まったプロセスは、決して待たない
  トレーサへシグナルを送るので、本物の SIGSEGV はクラッシュせずにハングする。
  両方のチェックは、ファイルから部分文字列を探すのをやめ、問題が本当に関わって
  いるフィールドを解析するようになり、ダイアログは異議の元になったマウント行を
  一緒に表示する。
- **そして Frida のチェックは、プレイヤーの一回を終わらせるコイン投げだった。**
  上の二つを読んでいて見つかった。`fridaPort` は `/proc/net/tcp` から `6D58`、
  `71D4`、`2717`、`5039` というリテラルを探していた —— これらはポート 27992、
  29140、10007、20537 であって、Frida の 27042-27045 ではない。16 進を書くべき
  ところに 10 進の数字が書かれていた。間違っていることは小さいほうの半分だった。
  それらは、ほぼ 16 進だけでできたファイルに対して部分文字列として照合されていた：
  このビルドマシンの 19 ソケットの表でさえ、**ありうる 4 桁 16 進の針 65536 本の
  うち 220 本がすでに出現している**。電話機のソケット数はその何倍もある。inode
  番号の中の一致が FLAG_FRIDA_PORT を立て、それは CRITICAL で、CRITICAL は
  `killProcess` を呼ぶ。いまはローカルアドレス列を解析し、状態 `0A`（LISTEN）を
  要求するので、他人の Frida への外向き接続がここで動くサーバと読まれることはない。

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
