[English](README.md) ·
**Türkçe** ·
[Deutsch](README.de.md) ·
[Español](README.es.md) ·
[Français](README.fr.md) ·
[Italiano](README.it.md) ·
[Português](README.pt.md) ·
[Русский](README.ru.md) ·
[日本語](README.ja.md) ·
[中文](README.zh.md)

# Omni Backrooms

Seviye 0'da geçen bir Android hayatta kalma korku oyunu — sonu gelmeyen tek düze
sarı ofis koridorları, rutubetli halı, uğuldayan floresanlar ve içeride sizinle
birlikte olan, öldürülemeyen bir şey.

Sıfırdan yazıldı: görüntüleyici Kotlin'den sürülen OpenGL ES 3.0, simülasyon
NDK üzerinden C++, ve seviye bir harita dosyası değil — hücre koordinatlarının
saf bir fonksiyonu. Bu yüzden hiç bitmiyor ve hiçbir ek yeri tekrar etmiyor.

## İçinde ne var

| | |
|---|---|
| **Seviye 0** | Sonsuz. Her hücre — zemin, duvar, ışık, rutubet — kendi koordinatından ve turun tohumundan türetiliyor. Yani dünya, tek bir bayt bile paylaşmayan iki oyuncu için birebir aynı. |
| **Tek canavar** | Kalabalık değil. Işın izleyerek görüyor, yani duvarlar sizi gerçekten saklıyor; ne kadar gürültü yaptığınıza göre duyuyor, yani çömelmek gerçekten bir şey kazandırıyor; ve sizi en son nerede gördüğünü hatırlıyor. |
| **El feneri** | Önce yavaşlatıyor, sonra kaçırıyor. Öldürmüyor. Backrooms'ta hiçbir şey ölmez — geri çekilir, dağılır, uzakta bekler ve sizi tekrar gördüğünde veya dikkatsizleştiğinizi duyduğunda geri gelir. |
| **Ses dosyası yok** | Her ses cihazda üretiliyor. APK'nın içinde tek bir WAV, OGG, hiçbir şey yok. |
| **On dil** | Türkçe, İngilizce, Almanca, İspanyolca, Fransızca, İtalyanca, Portekizce, Rusça, Japonca, Çince — yarım değil, tam. Oyun ilk açılışta cihazınızın dilini kendisi seçiyor. |
| **Sadece kozmetik** | Çerçeveler, izler ve karakterler. Oyunun hiçbir yerinde satılan hiçbir şey oynanışı etkilemiyor. |

## Derlemek

```bash
git clone https://github.com/Eaquel/Omni_Backrooms.git
cd Omni_Backrooms
./gradlew :Backrooms:assembleRelease
```

JDK 25, Android SDK 36, NDK ve CMake 4.3.2 gerekiyor. Sürüm derlemeleri bu
depoda bulunmayan bir anahtar deposuyla imzalanıyor; `assembleDebug` için
fazladan bir şey gerekmiyor.

## Kontroller

`Tools/` içindeki sekiz aracın yedisi her push'ta çalışıyor. Her biri Gradle
derlemesinin gerçekten göremediği bir şeyi koruduğu için varlar:

| Araç | Ne yakalar |
|---|---|
| `Shaders_Check.py` | GLSL, Kotlin ham dizelerinin içinde yaşıyor. Derlenmeyecek bir shader, onu kullanan ekran açılıp siyaha dönene kadar görünmez. Hepsi `glslangValidator` ile derleniyor. Ayrıca `linkGlProgram(V, F)` çağrılarında adı geçen her çift link ediliyor; çünkü ayrı ayrı derlenen iki shader yine de bir program oluşturmayı reddedebilir — aynı isimli bir uniform, aşamalar arasında tip ve kesinlik olarak uyuşmak zorunda. |
| `Assets_Check.py` | `aapt2`'nin kabul edip bozuk çizdiği elle yazılmış vektör ikonlar; artık dünya konumuyla eşleşmeyen mesh UV'leri; fon dışına çıkan inceleme kamerası; tekrarlanan ve hiç kullanılmayan varlıklar; geride kalmış bir dil; kendi kendisiyle çelişen Unity kamuflajı; sürdüğünü iddia ettiği geometrinin üzerinde olmayan kemiklere sahip bir karakter rig'i — animasyonu oynatıp dikişleri ölçerek kanıtlanır. Ayrıca `--optimise`: kayıpsız bir PNG yeniden kodlayıcı. |
| `Native_Check.py` | JNI sözleşmesi. Kotlin `external fun` bildiriyor, C++ `Java_..._name` tanımlıyor ve derleme anında ikisini **hiçbir şey** bağlamıyor — ne Kotlin derleyicisi, ne C++ derleyicisi, ne bağlayıcı. Tek taraflı bir isim değişikliği ilk çağrıda `UnsatisfiedLinkError` demek; argüman sayısı değişirse daha kötü, çünkü JNI isimle bağlar ve fazla argümanları yığından şikâyet etmeden okur. Ayrıca korumanın dedektörlerini diske serilen sahte bir `/proc` üzerinde çalıştırır; çünkü hiç çalıştırılamayan bir root kontrolü, sıradan telefonları suçlayan bir root kontrolüdür. |
| `Kotlin_Check.py` | Her import'u ardındaki bağımlılıkla, iki yönlü karşılaştırır. Buradaki Kotlin Android classpath'i olmadan derlendiği için gerçekten silinmiş bir kütüphane ile sadece yolda olmayan biri aynı görünür — Firebase'i kaldırırken `androidx.media3`'ün sessizce onunla gitmesi ve ancak Gradle derlemesinin doksanıncı saniyesinde ortaya çıkması böyle oldu. |
| `Level_0_Check.py` | Dünyayı doğuş noktasından birçok tohumla tarayıp çıkışın gerçekten erişilebilir olduğunu kanıtlıyor. Erişilemez bir çıkış, kazanılamaz bir tur demek ve tamamen sessiz. |
| `Entity_Check.py` | Gerçek yapay zekâyı derliyor, gerçek Seviye 0'a bir canavar koyup izliyor: duvarların engellediği görüş, gürültüyle ölçeklenen duyma, asla kilitlenmemesi gereken kaç-ve-dön döngüsü. |
| `Code_To_Sound.py` | Dağıtılan C++ üreticilerini çalıştırıp Python referansıyla örnek örnek karşılaştırıyor. Ayrıca WAV yazıyor, böylece yalnızca kod olarak var olan sesler gerçekten dinlenebiliyor. |

Hepsini çalıştırmak:

```bash
for t in Shaders Assets Native Entity Kotlin; do python3 Tools/${t}_Check.py; done
python3 Tools/Level_0_Check.py 40
python3 Tools/Code_To_Sound.py
```

Buradaki her kontrol, hatası kasten geri konularak doğrulandı. Hiç başarısız
olmamış bir kontrole güvenmek için kimsenin elinde bir sebep yoktur.

## Yerleşim

```
Backrooms/Source/Main/
  Kotlin/com/omni/backrooms/     arayüz, görüntüleyici, oyun döngüsü  (~14b satır)
  Native/                        NDK üzerinden C++                    (~3,9b satır)
    Map/        koordinatların saf fonksiyonu olarak Seviye 0
    Entity/     canavar yapay zekâsı — algı, kaçış, dönüş
    Sound/      bütün üreticiler; ses dosyası yok
    Ending/     bir turun nasıl bittiği, zamanın saf fonksiyonu
    Frame/      profil çerçevesi kozmetikleri
    Trail/      ayak izi kozmetikleri
    Shield/     dedektörler, ve ikilinin kendini ne olarak gösterdiği
  Assets/                        dokular, meshler, hikâye
  res/values*/                   on dil
Tools/                           sekiz kontrol
```

## Son düzeltmeler

En yenisi üstte. Bu liste her düzeltmede güncelleniyor.
- **Doğduğun yerden uzaklaştıkça dünya karelenmeye başlıyordu.** Üç ayrı
  mekanizma, üçü de merkezden uzaklıkla büyüyor ve hiçbiri GLSL'in uyaracağı
  türden değil. Fragment shader `precision mediump float;` diyor — demek
  zorunda, fragment aşamasının varsayılanı yok — ve dünya konumu varying'i
  highp, ki bu yalnızca varying'i korur, başka hiçbir şeyi değil.
  `vec3 surfaceFloor(vec3 wp)` parametresini **niteliksiz** alıyor, yani konum
  daha hiçbir aritmetik yapılmadan, çağrı anında kırpılıyordu. mediump çoğu
  mobil parçada yarım hassasiyet: 500 m'de ızgarası **0,25 m**, halı ise metre
  başına 41 çevrimde örnekleniyor. Pikselleşme bu, ve mesafenin her iki
  katına çıkışında ikiye katlanıyor. El fenerinde önce görünmesinin sebebi
  `vWorldPos - uTorchPos`'un büyük ve neredeyse eşit iki sayıyı çıkarması ve
  `uTorchPos`'un mediump bir uniform olmasıydı — 500 m'de ışığın kaynağı çeyrek
  metrelik bir ızgaraya oturuyor, koni de duvara basamak basamak düşüyor. İkisinin
  de altında ise `fract(sin(dot(p, k)) * 43758.5453)` — standart GLSL hash'i —
  orada 9e6'ya varan bir argüman alıyor; o büyüklükte temsil edilebilir bir
  float adımı **tam bir radyan**, yani periyodun altıda biri: hash olmayı
  bırakıp bantlanmaya başlıyor. Parametreler ve konum uniform'ları artık highp,
  iki hash de kafes noktası üzerinde tamsayı hash'i — her koordinatta kesin ve
  maliyeti aynı. `Shaders_Check.py` üçünü de zorunlu tutuyor; dar tutarak,
  çünkü GLSL bir işlemi en yüksek operand hassasiyetinde yürütür, yani yerel
  değişken sorun değil — yalnızca bir parametre, bir uniform ya da o hash
  gerçekten kaybettirebilir.
- **Tek Kotlin dosyası, ve hiçbir yerde yorum yok.** `Service.kt` ile
  `Settings.kt`, `Backrooms.kt` içine birleştirildi — tek paket, isim çakışması
  yok, 180 import tekilleştirildi — ve projedeki bütün yorumlar kaldırıldı:
  Kotlin, Gradle KTS, C++ ve Kotlin raw string'lerinin içindeki GLSL ile
  AGSL'den `//` ve `/* */`; Python, YAML, TOML, CMake, ProGuard ve properties
  dosyalarından `#`; XML'den `<!-- -->`. 8059 satır silindi. Regex yerine dile
  özel bir belirteçleyiciyle yapıldı, çünkü regex bir string'in içindeki
  `https://` ile bir YAML değerinin içindeki `#` işaretini de yer; `#version`,
  `#include` ve shebang satırları korundu, onlar yorum değil yönerge. Üç
  denetim `Service.kt`'yi adıyla arıyordu ve birleştirmede kırıldı — bir dosya
  adı sözleşmenin parçası hâline gelmişti, oysa denetledikleri şey bir
  bildirimdi; artık hangi Kotlin kaynakları varsa onları okuyorlar.
  Doğrulandı: sekiz aracın hepsi yeşil, her Python dosyası derleniyor, 31 XML
  dosyasının hepsi ayrıştırılıyor, 22 shader'ın hepsi hâlâ derlenip link
  oluyor.
- **Hatası çökme olan tek shader, hiçbir şeyin denetlemediği tek shader'dı.**
  Bir önceki maddedeki kenar shader'ı, kenar bandının kalınlığını `fwidth(d)`
  ile hesaplıyordu. AGSL'de türev fonksiyonları **hiç yok** — ne `fwidth`, ne
  `dFdx`, ne `dFdy` — ve RuntimeShader kaynağını **yapıcısında**, ana iş
  parçacığında, kompozisyonun içinde derliyor. Yani daha sade bir butona geri
  çekilmedi; lobinin ilk karesinde `IllegalArgumentException` fırlattı ve
  uygulama açılmadı. `Shaders_Check.py` bunu hiç görmedi, çünkü shader taraması
  `#version` satırı şart koşuyordu ve AGSL'de öyle bir satır yok: uygulamayı
  düşürebilen tek shader, filtrenin dışında kalıyordu. Artık AGSL'i de
  denetliyor — derleme makinesinde SkSL derleyicisi yok, o yüzden AGSL'de
  bulunmayan GLSL yerleşiklerini reddediyor ve her tanımlayıcının bildirilmiş
  olmasını şart koşuyor; tek bir bozuk satırı bu çökmedeki altı hataya çeviren
  şey buydu. `fwidth`'i geri koyarak, artı bir yazım hatası, `texture()` ve
  `gl_FragCoord` ile doğrulandı: 4/4.
- **Ve süs hâlâ uygulamayı bitirebiliyordu.** Parıltı API seviyesinde
  korunuyordu ama kaynağında korunmuyordu; yani derlenmeyen bir shader eksik
  değil, ölümcüldü. Artık sarılı, ve parıldayamayan bir buton sadece bir
  butondur — sarmaşık katmanının zaten uyduğu kuralın aynısı.
- **Kare raporu: pikseller hakkında teori kurmak yerine onları geri okumak.**
  `level drawn: 1 chunk(s) resident, 1748 triangles` geçen turun sorusunun
  cevabıydı ve yeni bir sorunun başlangıcı — geometri GPU'ya ulaşıyor ve ekran
  hâlâ siyah, ki bu tamamen başka bir hata. Şimdiye kadarki her teşhis bir
  *adımın çalışıp çalışmadığını* ölçüyordu; hiçbiri o adımın *ne ürettiğini*
  ölçmüyordu. Artık renderer, hiçbir şey kompozit etmeden önce sahne
  hedefinden 24x24'lük bir yama, kompozitten sonra da varsayılan
  framebuffer'dan bir yama geri okuyor ve ikisinin de min/ortalama/maks
  parlaklığını kamera, üçgen sayısı, hedef boyutları ve bir kareyi
  karartabilecek her ayarla birlikte yazıyor. Yaklaşık 2., 5. ve 10. saniyede
  çalışıp bir daha hiç çalışmıyor. İki sayı, teoriye gerek kalmadan karar
  veriyor: sahne aydınlık ve ekran karanlıksa görüntüyü kompozit yiyor; ikisi
  de karanlıksa dünya çizilmiş ama ışıksız ya da kamera ona bakmıyor.
- **Lobideki sarmaşıklar yedigendi.** `buildVineMesh` yedi kenar süpürüyordu ve
  bunların çizildiği kalınlıkta yedi kenarlı bir tüpün silueti gözle görülür
  şekilde düz kenarlı olur — hiçbir kenar yumuşatmanın gideremeyeceği düz
  yüzeyler, çünkü geometri gerçekten o şekilde. Artık on iki kenar ve 34 halka;
  bütün lobi için 2520 vertex. Yüzey de buna yakışır bir malzeme kazandı:
  gövde boyunca uzanan lif, üzerinden geçen daha kaba bir benek, ışığı yuvarlak
  bir nokta yerine gerçek bir gövde gibi bir şerit hâlinde yakalayan
  anizotropik parlaklık, ve ince uçtan sızan ışık.
- **Ve lobide hiçbir şey çoklu örneklenmiyordu.** Sarmaşık yüzeyi
  `setEGLConfigChooser(8, 8, 8, 8, 16, 0)` istiyordu — yani hiç MSAA yok — ve
  bu ayrı bir yüzey olduğu için pencerenin kendi kenar yumuşatması ona hiç
  ulaşmıyordu. Her kenar sert bir piksel merdiveniydi. Artık 4x bir config
  seçiyor, olmazsa 2x'e, o da olmazsa hiçe düşüyor ve asla fırlatmıyor:
  GLSurfaceView'ın basit seçicisi eşleşme bulamayınca GL iş parçacığında
  IllegalArgumentException atar, bu da yüzeyi düşürür ve bu turun başladığı
  siyah dikdörtgeni bırakır.
- **Buton, dokunana kadar hiç kıpırdamıyordu.** Altındaki plaka animasyonluydu,
  içinde durduğu gövde değildi; ne kadar iyi gölgelendirilirse gölgelendirilsin
  düz bir dikdörtgen gibi okunmasının sebebi bu. Artık nefes alıyor — yüzde
  yarımdan az ölçek, derecenin küçük bir kesri kadar yalpa, ve onunla birlikte
  yükselen bir gölge; üç aralarında asal periyotta, yani döngü aynı bileşimi
  hiç tekrarlamıyor. Kenar shader'ı da yuvarlatılmış dikdörtgen mesafe alanı
  üzerine yeniden kuruldu: eskisi `min(uv.x, 1-uv.x, ...)` kullanıyordu, yani
  yuvarlak bir plaka üzerinde **kare** bir sönüm, ki bu da parıltıyı köşelerde
  toplayıp dört düz şerit hâline getiriyordu.
- **Sağlayıcının kaçırdığı bir chunk, turun geri kalanında ölü sayılıyordu.**
  Shader ve guard hataları gidince Mali-G68'den tertemiz bir log geldi —
  programlar link oluyor, framebuffer tam, guard `CLEAN` — ve dünya hâlâ
  siyahtı, HUD üstünde duruyordu. `streamChunks` bir ıskayı, önbelleğe boş bir
  `ChunkMesh()` koyarak yanıtlıyordu; `containsKey` de onu sonsuza dek
  atlıyordu: geçici bir cevaptan alınmış kalıcı bir karar. `fetchChunk`, dünya
  geçerli olana kadar null döner; yani GL iş parçacığının dünyanın
  oluşturulmasının önüne geçtiği bir cihazda halkadaki kırk dokuz chunk'ın
  hepsi ilk kırk dokuz karede ölü sayılıyor ve oyuncu hiçliğin içinde kalıyor.
  Zamanlamaya bağlı; üç testçiye ulaşıp bu makineye hiç uğramamasının sebebi
  bu. Iskalar artık bir yeniden deneme haritasına giriyor ve yirmi kare sonra
  tekrar soruluyor; `Kotlin_Check.py` de kuralı ifade ediyor — renderer bir GL
  bağlamı ve Android classpath'i istiyor, yani buradaki hiçbir araç onun tek
  bir karesini çalıştıramıyor, ve iki yazım da derleniyor.
- **Renderer bir şey çizip çizmediğini hiç söylemiyordu.** Üç siyah ekran
  raporu tam log'la geldi ve hiçbiri sorulmaya değer ilk soruyu
  cevaplamıyordu: GPU'ya bir üçgen ulaştı mı? İçinde geometri olmayan bir sahne
  0.02 griye temizlenir, ki bu siyah ekrandır — ve yukarıdaki her şey başarı
  bildirir. Artık seviye ilk çizildiğinde bir kez loglanıyor: chunk sayısı,
  üçgen sayısı, yeniden denemeyi bekleyen chunk'lar. Üç saniye boyunca hiç
  çizilmezse de bir kez uyarı veriyor ve dünyanın geçerli olup olmadığını,
  chunk sağlayıcısının atanıp atanmadığını söylüyor. "Hiçbir şey çizmedi" ile
  "çizdi ama göremedin" ortak hiçbir yanı olmayan iki ayrı hata.
- **On iki programın hepsi aşamalar arasında kendisiyle çelişiyordu.** Galaxy
  S23 bir uniform'un adını verdi; Mali'li bir Galaxy A17 aynı şeyi başka
  kelimelerle söyledi — `L0001 The fragment floating-point variable uGrowth does
  not match the vertex variable uGrowth. The precision does not match.` On iki
  çiftin hepsini ölçünce 25 tane daha çıktı: vertex aşamasında `out vec3
  vNormal` diye yazılan (varsayılan highp), fragment aşamasında `in vec3
  vNormal` diye okunan (kendi `precision` satırından mediump) bir varying. ES
  3.00 spec'i varying'ler için buna izin veriyor — sadece uniform'ların
  eşleşmesini şart koşuyor — ve glslang spec'i takip ettiği için hiçbiri
  işaretlenmiyordu. Sürücüler o kadar tekdüze değil, ve Mali'nin mesajı
  varying ile uniform'u birbirinden ayırmıyor bile. 25'inin hepsi artık vertex
  aşamasının kesinliğine sabitlendi, `Shaders_Check.py` de iki tür için de
  eşleşmeyi şart koşuyor — bilerek spec'ten katı.
- **Hiçbir yere çizmeyen bir kare, siyah ekrandan ayırt edilemez.** Renderer bir
  ekran dışı renk hedefi, bir derinlik tamponu ve yarım çözünürlüklü bir bloom
  çifti kuruyor ve bir kez bile `glCheckFramebufferStatus` çağırmıyordu. Eksik
  bir framebuffer hiçbir sürücünün raporladığı bir hata değil: içine yapılan her
  çizim atılır, tur işlemeye devam eder, HUD üstüne binmeye devam eder ve dünya
  basitçe yoktur — üstelik hiçbir log'da tutunacak bir satır olmadan. Artık her
  yeniden kurulumda tamlık denetleniyor ve hata yok olmak yerine geri çekiliyor:
  kullanılamaz bir bloom çifti haleleri götürüyor, kullanılamaz bir sahne hedefi
  ise kareyi post zinciri olmadan doğrudan ekrana gönderiyor.
- **Ve hangi GPU'nun çizdiğini hiçbir şey söylemiyordu.** İki siyah ekran
  raporu tam log'la geldi ve ikisi de sürücüyü belirtmiyordu; model numarasından
  tahmin etmek zorunda kaldım. GL üreticisi, renderer'ı, sürümü ve GLSL sürümü
  artık bağlam başına bir kez loglanıyor, ve link olmayan bir program kendi
  adını veriyor — sahne programı hiçbir şeyin içine sarılı değil, yani hatası
  eskiden on ikiden hangisinin gittiğini söylemeyen çıplak bir çökmeydi.
- **Lobi butonlarının üstünde siyah bir dikdörtgen — hem de sorunsuz derlenen
  bir shader yüzünden.** Galaxy S23 log'undan: `Omni program link failed:
  Error: Uniform uGrowth precision mismatch with other stage.` Vine shader'ının
  iki yarısı da tek başına geçerli; bu araç aylardır tam bu yüzden onları
  onaylıyordu — derlemek, bir program kurmanın sadece yarısı. Aynı ismi
  paylaşan uniform'lar aşamalar arasında hem tip hem **kesinlik** olarak
  uyuşmak zorunda; vertex shader'da `float` varsayılan olarak `highp`,
  fragment shader'da ise hiç varsayılan yok, o yüzden buradaki her fragment
  shader `precision mediump float;` yazıyor. İki aşamanın da okuduğu, kesinliği
  belirtilmemiş bir float uniform tanımı gereği uyuşmazlık demek. Hoşgörülü
  sürücüler yine de link ediyor; sürüme çıkması tam olarak böyle oldu. `uGrowth`
  artık iki aşamada da açıkça `highp`, ve `Shaders_Check.py` yalnızca yarıları
  derlemek yerine `linkGlProgram(V, F)` çağrılarında adı geçen her çifti link
  ediyor — bu ayrıca hiçbir programa ait olmayan bir shader'ı da yakalıyor.
- **Ve hata zarifçe geri çekilmedi, butonun üstünü kapattı.** Vine katmanı
  kendi kurulum hatasını zaten yakalayıp hiçbir şey çizmiyordu; kulağa güvenli
  geliyor ama değil: görünüm `setZOrderOnTop(true)` çağırıyor, yani yüzeyi
  butonun içine değil pencerenin tamamının üstüne koyuyor. Hiç kare sunmayan
  bir yüzey "sarmaşık yok" demek değil, butonun ve yazısının üstünde opak bir
  delik demek. Çizemeyen süs katmanı artık kompozisyondan tamamen çıkıyor,
  buton da boyalı plakasına geri dönüyor.
- **Koruma, temiz bir telefonu root'lu diye suçladı.** Bir oyuncu oyun içi
  güvenlik uyarısını fotoğrafladı: `Sebep: root, flags=0x40200`. İki bit, ve
  gerçek root bitlerinin hepsi — ROOT_BINARY, ROOT_PROPS, ROOT_PATHS, MAGISK,
  ZYGISK, KSU, SELINUX_OFF — kapalı. Cihaz root'lu değildi; sorun bizim iki
  kontrolümüzdü. **SHADOW_MOUNT**, mount tablosunun tamamında
  `containsCI(m,"overlay") && containsCI(m,"/system")` soruyordu: iki ayrı
  arama, yani `/vendor/overlay` üzerindeki bir overlay ile `/system_ext` satırı
  — ikisi de Android 11+ her telefonda standart — birleşip "root" oluyordu; o
  da HIGH, o da uyarı. **PTRACE_TRACED** ise `PTRACE_TRACEME` çağırıp bunu pid 0
  ile `PTRACE_DETACH` ederek geri almaya çalışıyordu, ki bu mümkün değil:
  detach izlenen sürecin gerçek pid'ini ister, `ESRCH` ile başarısız olur ve
  süreç ebeveyni tarafından izlenir hâlde kalır. Sonraki taramanın TRACEME'si
  bu sefer `EPERM` — "zaten izleniyor" — döner; yani her cihazda, yaklaşık beş
  saniye sonra, kalıcı olarak, çünkü bayrak sözcüğü tur boyunca OR'lanıyor. Bu
  bize çökme raporlarına da mal oldu: izlenen hâlde sıkışmış bir süreç
  sinyallerini hiç beklemeyen bir izleyiciye yollar, yani gerçek bir SIGSEGV
  çökmek yerine askıda kalır. İki kontrol de artık dosyada altdizi aramak
  yerine sorunun ilgili olduğu alanı ayrıştırıyor ve uyarı, itiraz ettiği mount
  satırını da taşıyor.
- **Frida kontrolü de bir oyuncunun turunu bitirmek üzerine yazı turaydı.**
  Yukarıdaki ikisini okurken bulundu. `fridaPort`, `/proc/net/tcp` içinde
  `6D58`, `71D4`, `2717` ve `5039` dizgilerini arıyordu — bunlar 27992, 29140,
  10007 ve 20537 portları; Frida'nın 27042-27045'i değil. Biri onaltılık
  yazılması gereken yere ondalık rakamları yazmış. Yanlış olmaları küçük
  yarısıydı. Bunlar neredeyse tamamı onaltılık olan bir dosyada altdizi olarak
  aranıyordu: bu derleme makinesinin 19 soketlik tablosunda **65536 olası
  dört-haneli-onaltılık dizgiden 220'si zaten geçiyor**, bir telefonda ise kat
  kat fazla soket var. Bir inode numarasının içine denk gelen eşleşme
  FLAG_FRIDA_PORT'u yakar, o CRITICAL'dır, CRITICAL da `killProcess` çağırır.
  Artık yerel adres sütununu ayrıştırıyor ve `0A` (LISTEN) durumunu şart
  koşuyor; böylece başkasının Frida'sına giden bir bağlantı, burada sunucu
  çalışıyor diye okunmuyor.

- **Yaratık bütün oyun boyunca kıpırdamadan duruyordu.** "Neredeyse yok"
  kelimenin tam anlamıyla doğruymuş ve bunu görmek için benzetim gerekti: sekiz
  tohum, beşer dakika, el feneri kapalı yavaş bir tur atan bir oyuncu. Sekizin
  üçünde yaratık oyuncuyu **turun %0'ında** gördü; medyan mesafe 33-51 m — aynı
  katta olacak kadar yakın, hiçbir işe yaramayacak kadar uzak. Sebebini durum
  histogramı söyledi: turun **%99'unu `AIState::Idle`'da, %0'ını Wander'da**
  geçiriyordu — ve `executeState`'te Idle için hiçbir dal yok, yani hareket
  etmiyor. Idle, davranış ağacının oyuncuyu göremediği ve alarmda olmadığı her
  an seçtiği dal; yani bir turun çoğu. Bir koridorda kıpırdamadan duruyordu.
  Artık Idle, Wander'a düşüyor; yerinde donmak pusu zamanlayıcısının işidir,
  görüş dışında olduğun her saniyenin değil.
- **Bulduğundaysa asla bırakmıyordu.** Aynı ölçümün diğer yarısı: kalan
  tohumlarda **beş dakika boyunca medyan 1.4 m** mesafede, %100 görüş içinde
  duruyordu. Onu yalnızca fener ışını ve hasar almak koparıyordu; fener kapalıyken
  karşılaşmanın sonu yoktu. Artık temas ona pahalıya patlıyor: saniyede 1.05'e
  karşı sönümün aldığı 0.8, yani üstünde yaklaşık dokuz saniye ve çekiliyor —
  yaklaş, vur, karanlığa karış, dön. Bunu `doAttack` içinde değil mesafeye göre
  hesaplıyorum: tam saldırı yarıçapında duran bir yaratık sınırın iki yanında
  salınıyor ve zamanının çoğunu Stalk'ta geçiriyor; maliyeti saldırıya koymak
  sayıları kelimenin tam anlamıyla hiç değiştirmedi, zaten bu böyle bulundu.
  Sekiz tohumda: görülme %50 → %34, ve hiçbir tohumda 0 yok.
- **Dururken duyulan tek şey baştaki tüptü.** Boş bir kat, uğultular arasında
  sessiz değildir — oturur, boruları takırdar, bir yerde bir kapı kapanır, bir
  şey halının üstünde sürüklenir. Artık uzak-olay üreteci var: on yedi saniyede
  bir, arası çoğunlukla sessiz, dört çeşit; hepsi ağır alçak-geçiren süzülmüş ve
  uzun kuyruklu, çünkü mesafe zaten bir alçak-geçiren ve bir kuyruktur. Buradaki
  her şey gibi saatte deterministik — aynı yerde aynı anda duran iki oyuncu aynı
  şeyi duyar — ve C++'ıyla sıfır farkla eşleşiyor.
- **Bir güvenlik değişmezini belgeleyen ama kimsenin uygulamadığı bir sabit — ve
  derleyici bunu her derlemede söylüyormuş.** `kMaxRoomHalf = 5`, "kSectorSize /
  2'nin altında kalmalı" yorumunu taşıyordu; bu, seviyenin bütün O(1)
  sorgusunun dayandığı kural, çünkü komşu sektörü aşan bir oda, yalnızca sekiz
  komşusunu tarayan bir hücreye görünmez olurdu. Onu hiçbir şey okumuyordu.
  Odalar, déjà vu eklendiğinden beri sekiz arketipli bir katalogdan geliyor ve
  her derleme `warning: unused variable 'kMaxRoomHalf'` yazdırıyordu — ki bunu
  aynı oturumda bir CI günlüğünde okumuş ama bağlamamıştım. Yanlış bir ölçüme de
  mal oldu: uzun görüş hatlarının kaynağını ararken o sabiti 5 → 4 → 3
  süpürdüm ve üç kez bayt bayt aynı sonucu aldım; bunu önce probumun bozukluğu
  sandım. Prob doğruydu; hiçbir yere bağlı olmayan bir düğmeyi çeviriyordum.
  Katalog artık ad alanı düzeyinde ve değişmez onun üzerinde bir
  `static_assert` — yani şikâyet eden derleyici artık uygulayan taraf.
- **Koridorlar L şeklindeydi ve zincirleniyorlardı.** Bir bacak, odadan odaya
  tek bir z'de baştan sona uzanıyordu; bir sonraki sektörün koridoru aynı satıra
  denk geldiğinde ikisi birleşiyordu: 60 tohumda ölçülen en uzun kesintisiz düz
  açık zemin hattı medyanda 253 m'ydi. Artık dirsekli — bir satır boyunca çık,
  iki uca da ait olmayan bir satıra geç, onun boyunca git, sonra içeri gir — ve
  medyan 227 m'ye iniyor. İki şeyi açıkça söylemeliyim. İlk denemem yalnızca iki
  odanın kendi satırları arasında geçiş yapıyordu; ikisi aynı satırdayken bu
  hiçbir şey yapmaz ve medyanı hiç kıpırdatmadı — asıl önemli olan sapma. İkincisi
  etkinin büyüklüğü: medyanda %10, kuyrukta hiç. Çünkü plan eksen hizalı ve uzun
  bir görüş hattının büyük kısmı koridor şeklinden değil ızgaranın kendisinden
  geliyor.
- **Ve o kontrolün sınırını yine çok küçük bir örneklemde ayarladım.** 20 tohumda
  belirlenen tohum-başına bir sınır geçti, sonra 40'ta 2 tohum düştü — bu turda
  loşluk sınırında yaptığım hatanın aynısı, aynı oturumda. Artık medyana
  bağlanıyor: 227'ye karşı L'nin 253'ü, hem 40 hem 60 tohumda birebir aynı.
  p90'a bağlanmıyor: örnekleme göre her iki şekilde de 298-320 çıkıyor, yani
  ona konacak bir sınır kontrol kılığına girmiş bir yazı-tura olurdu. Yine de
  yazdırılıyor.
- **Tavan alçak değildi; mercek çok genişti.** `Matrix.perspectiveM`'e 70
  derece veriliyordu ve o fonksiyonun ilk açısı DİKEY görüş alanıdır — 2:1 bir
  telefonda bu 109 derece yatay demek; normal bir birinci şahıs oyunu 75-90
  arasındadır. 109'da zemin ve tavan kareyi dolduruyor, kameraya yakın her şey
  yayılıyor, ve doğru boyutta bir oda sürünme boşluğu gibi okunuyor. Artık 52,
  yani 89 yatay. Tavan da 2.6 m'den 3.0 m'ye çıktı: göz 1.70'teyken 2.6 gerçek
  bir ofistir ama 3.0 aynı binanın yüksek ucu ve lobi görüntüsünün gösterdiği
  şey.
- **Işık hiçbir yerden gelmiyordu.** Ortam 0.20'ydi, tam tepedeki bir tüp ise
  yaklaşık 1.05 veriyor; yani içinde armatür olmayan bir oda aydınlık bir odanın
  beşte biri kadar aydınlanıyordu ve ışık havuzları anlamını yitiriyordu — "ışık
  olmayan yerlerde aydınlık" ve "ortamı daha karart" aynı bulgunun iki kez
  söylenmiş hâli. Ortam 0.085 oldu, shader'ın kendi taban terimi 0.09'dan
  0.035'e indi ve eğim dikleşti, armatürler 1.55 veriyor: en karanlık hücre
  eskisinden daha karanlık, aydınlık bir koridor ise şimdiye kadarki en
  parlağı. Havuz ile aradaki loşluk arasındaki ölçülen kontrast: 11.6x → 38.2x.
  Level_0_Check'in sınırı 3'tü — bozuk ayarın oradan geçmesine yetecek kadar
  gevşek; artık 18.
- **Kontrol, artık var olmayan bir formüle karşı ölçüm yapıyordu.** İki aydınlık
  eşiği shader'ın eski `lit = 0.09 + facing * light * 1.30`'una karşı elle
  hesaplanıp sabit olarak yazılmıştı; shader'ı değiştirince, az önce iyileşmiş
  bir seviyede 40 tohumun 40'ı düştü. Tek bir kuralın iki yerde yaşamasının
  altıncı örneği. Prob artık shader'ın kendi katsayılarını okuyup eşikleri
  onlardan türetiyor, ve o satır taşınırsa tahmin etmek yerine çalışmayı
  reddediyor.
- **Sis ve VHS filtresi varsayılan olarak kapalı, filtre de yarı güçte.** Her
  karenin üstüne bant efekti koymak, oyuncu adına verilmiş güçlü bir üslup
  kararıdır. Bu arada: `observeVhs()` varsayılanı `true` iken `observe()`
  `false`'tu — efektin açık olup olmaması, çağıranın hangisini kullandığına
  bağlıydı. Artık tek sabit. Sisin kendisi 0.008'de karesel'di ve 25 m'de
  doyuyordu: koridor, çizim mesafesinin çok içinde düz bir renk levhasıyla
  bitiyordu — "haritanın yüklenmediği karanlık bölge" tam olarak böyle görünür.
  Artık neredeyse doğrusal.
- **Çizim mesafesi ufuk çizgisiydi.** Uzak düzlem 55 m, chunk halkası 384 m'ydi;
  yani oyuncunun gördüğü şey kırpma düzlemiydi. Artık 110 m ve 7x7 halka.
- **Koridorlar kapı çerçeveleriyle doluydu.** Her kapı hücresinde bir kiriş ve
  iki söve inşa ediliyordu, ve kapı özelliği koridor hücrelerinin %28'ine
  düşüyor — yani koridor, birkaç metrede bir portaldan oluşuyordu. Level 0,
  bölmelerinde açıklıklar olan bir ofis katıdır, sütun dizisi değil. Üstelik her
  kapı hücresinden 6 dörtgen eksildi.
- **Zemindeki bazı kareler ışıksızdı** ve öyle olmaları amaçlanmıştı:
  kFeatureHole kendi hücresini %34'e karartıyordu — dokunduğu her şeyden üçte
  iki daha karanlık, sert kenarlı bir dikdörtgen. Gerçek bir odada hiçbir şey
  bunu yapmaz; ışığı yanmamış bir karo gibi okunuyordu. Kaldırıldı.
- **Duvarda her 800 mm'de bir %74 parlaklıkta çizgiler vardı**, halıda da %80'de
  bir ızgara. Kağıt eki ve halı karosu derzi, ancak aradığında fark ettiğin
  gölgelerdir. Artık %94 ve %93.
- **Yürürken menüyü açmak onu olduğu yerde yürüttürüyordu.** Ayak sesleri ses
  geri çağrısının içinde kendi aralıklarıyla çalışıyor ve onları yalnızca hareket
  dalı durduruyordu — duraklatılmışken çalışmayan bir dal. Artık hem duraklatma
  hem ekrandan çıkma durduruyor.
- **Ayak izleri, ayaklarının değil kameranın baktığı yeri gösteriyordu.** Kamera
  anlık görüntüsünün ham yaw'ıyla damgalanıyorlardı; avatar ise onu takip eden
  yumuşatılmış bir yaw ile çiziliyor. Dururken ikisi aynı, dönerken değil.
  Renderer artık onu hangi açıyla çizdiğini yayımlıyor ve damga onu okuyor.
- **Duvarlar, zemin ve tavan artık kodla üretiliyor; üç görsel silindi.**
  Floor.png, Wall.png ve Roof.png küçük bir APK'nın 4.6 MB'ını yiyordu ve
  silmeden önce 128x128'de ölçüldüğünde tuttukları şey üzerine gren serpilmiş
  düz bir renkti: duvar (0.470, 0.423, 0.158), luma standart sapması 0.076;
  zemin (0.432, 0.375, 0.107), 0.069; tavan nötr 0.827, 0.072. Sahne fragment
  shader'ındaki beş yüz baytlık aritmetik bunu yeniden üretiyor — ve 1024'lük
  bir görselin aksine hiç tekrarlamıyor, döşeme izi vermiyor ve hiçbir mesafede
  maliyeti yok. Eski dosyalardan gelmeyen tek şey ton: lobi arkaplan klibinin 60
  karesi ölçüldü ve aydınlık üçte biri (1.00, 0.80, 0.42) oranını veriyor —
  duvarların (1.00, 0.90, 0.34)'ünden daha sıcak ve daha kehribar; eskisi onun
  yanında yeşil okunuyordu. Tabanlar artık klibin oranında, eski dosyaların
  parlaklığında. Assets_Check altı sayıyı da bu ölçümlere karşı tutuyor ve
  görseller geri gelirse hata veriyor.
- **Yürüyüş "dıt dıt" ediyordu.** Ediyordu ve sebebi ölçülebilirdi: ayak sesi
  üretecinin baskın enerjisi ~1.1 kHz'deydi ve 53 ms'de bitiyordu — bu bir tık.
  Halıda gerçek bir adım 200 Hz altındadır ve 120-180 ms sürer. Spektrumu bir
  buçuk oktav yukarı iten şey, sürtünme teriminin filtresiz beyaz gürültüsüydü.
  Diğer yarısı daha kötüydü ve spektrumla ilgisi yoktu: sentezleyici her
  seferinde üreteci aynı üç argümanla t = 0'dan başlatıyordu, yani bir yürüyüşün
  her adımı *aynı dalga formuydu*, metronom üzerinde — hem kusursuz periyodik
  hem kusursuz özdeş olan her şey arayüz bip'i gibi duyulur. Artık birkaç
  milisaniye arayla topuk ve parmak, alçak geçiren süzülmüş bir sürtünmenin
  altında alçak bir gövde, yalnızca sert zemine ait bir klik, ve perdeyi,
  sönümü, seviyeyi kaydıran bir adım indeksi var; ardışık iki adım artık
  eşleşmiyor. Ölçüm: 1131 Hz'den 67 Hz'e, 53 ms'den 134 ms'e, ve tam olarak
  0.000 fark eden ardışık adımlar artık 0.549 fark ediyor.
- **Çıkış hiçbir zaman seviyenin söylediği yerde değildi.** findExit kapıyı
  spawn'dan 110-170 hücreye, yani 352-544 m'ye koyuyor. EXIT_LEASH_M — oyuncunun
  kapı 46 hücre ileriye taşınmadan önce ne kadar uzaklaşabileceği — 320 m'ydi.
  40 tohumun 40'ında kapı tasmanın dışında doğuyordu, yani ilk iki saniyelik
  kontrol, oyuncu tek adım atmadan onu 147 m'ye çekiyordu. Tasarlanan tur
  uzunluğu hiçbir tohumda oynanmamıştı. İki dilde, iki dosyada, tek bir şeyi
  anlatan iki sayı — ve hiçbiri tek başına yanlış değil; hiçbir şeyin görememe
  sebebi buydu. Tasma artık 620 m ve Assets_Check ikisini karşılaştırıyor.
- **Son sekansı örnekleyen kod yayın derlemesini kırdı.** `OmniGLRenderer` bir
  Context'ten başka bir şey tutmuyor — chunk'lar, iz ve artık tur-sonu
  parametreleri de composable'dan atanan sağlayıcı lambda'larla geliyor, çünkü
  renderer kendi GL iş parçacığında çalışıyor. Yeni kod `bridge.endingParams`'ı
  doğrudan onun içinden çağırıyordu ve orada `bridge` diye bir şey yok. Bütün
  statik kontroller geçti, Gradle `Unresolved reference 'bridge'` ile patladı.
  Kotlin_Check bunu göremedi ve — denendi — göremez: Android sınıf yolu olmadan
  `OmniGLRenderer` çözülemeyen bir `GLSurfaceView.Renderer`'ı genişletiyor, yani
  `bridge` de `x`, `y` ve `build` ile aynı kovaya düşüyor: kod yanlış olduğu için
  değil, bir jar eksik olduğu için çözülemiyor. Her şeyde hata verecek bir filtre
  genişletmek yerine altındaki kural doğrudan yazıldı: GL renderer'ı view
  model'e uzanmaz. Sağlayıcı üzerinden view model'in önbelleğe alınmış anlık
  görüntüsünü okumak ayrıca kare başına ikinci bir çağrı yerine tick başına tek
  bir yerel çağrı demek — ve panel ile görüntü iki ayrı an yerine aynı andan
  örnekleniyor.
- **Bir turun sonu, siyah bir dikdörtgen üzerinde bir diyalogdu.** Hem ölüm hem
  kaçış ekranı, az önce içinde durduğun seviyeyi %88 siyahla boyayıp üzerine
  yuvarlak köşeli bir kart koyuyordu. Bu, kapatılacak bir şey gibi okunur ve
  önemli olan tek kareyi çöpe atar. Artık perde yok. Yeni `Native/Ending/`,
  (hangi son, başlamasından bu yana geçen saniye) ikilisini geçişin yapıldığı
  sekiz son-işlem parametresine çeviriyor ve baktığın kare gözünün önünde
  dağılıyor: ölüm önce rengi boşaltıyor, sonra kanalları ayırıyor, bandın kilidi
  kaybetmesi gibi öbekler hâlinde satırları koparıyor, her şeyi ortaya doğru
  çekiyor ve ancak ondan sonra vinyeti kapatıp pozlamayı dörtte bire indiriyor.
  Kaçış ise her terimde tam ters eğri: rengini koruyor, hiç yırtılmıyor ve bloom
  koridoru patlatıp geri dönüyor — seni beyaz bir ekranda bırakmıyor. İstatistik
  paneli aynı sekiz sayının sonuncusuyla yükseliyor; yani görüntü çökmeden önce
  belirmesi mümkün değil ve ondan sapması da mümkün değil, çünkü ikisi de tek
  bir çağrıdan geliyor. Yerelleştirilmiş metin Compose'da kaldı: çeviri, on dil
  dosyasının bulunduğu yere aittir.
- **Bir geçiş, bir oyunda bakılması en zor şeydir; bu yüzden görüşe değil
  doğrulamaya bağlandı.** Bunu görmek için ölmen gerekiyor. `Ending::evaluate`
  kendi saati olmayan saf bir fonksiyon olduğu için Native_Check her iki sonu da
  ana makinede baştan sona örnekliyor ve tek bir olay gibi okunmasını sağlayan
  özellikleri doğruluyor: bir sonun ilk karesi tam olarak ondan önceki kare
  olmalı ki kesme gibi açılmasın; panel tekdüze yükselmeli ve yolun %55'inden
  önce yarılanmamalı ki geçiş bir kartın arkasında yaşanmasın; ikisi de 1.2
  saniyenin altına inmemeli, 3.5'in üstüne çıkmamalı; ve iki son, farklı renkte
  tek bir eğri olmamalı — ölüm asla aydınlanmamalı, kaçış aydınlanmalı. Dördü de
  hatayı kasten geri koyup kontrolün yakaladığı görülerek doğrulandı.
- **Oyundaki dört sesin üçü hiç çalınmamıştı.** `Sound/Synth.h`, duyamadığın
  kodun kimsenin denetlemediği kod olduğunu ve denetlenenin yayınlanan olduğunu
  söyleyerek başlıyor. İkisi de doğru değildi. `fluorescentHum`, `footstep` ve
  `monsterVoice` — Code_To_Sound'un WAV'a çizip Python referansıyla örnek örnek
  karşılaştırdığı üçü — motorun hiçbir yerinde çağrılmıyordu. Hoparlöre yalnızca
  jenerik sesi ulaşıyordu. Asıl çalan şey `Engine.cpp` içine satır arası
  yazılmış, çok daha kaba ikinci bir üreteç takımıydı: saniyede 800 radyanlık,
  aslında 127 Hz olan bir "klik"; frekans modülasyonu tamsayı örnek sayacına
  uygulandığı için perde her değiştiğinde fazı zıplayan bir yaratık; ve doğrudan
  `std::mt19937`'den gelen filtresiz beyaz gürültüden ibaret bir ortam katmanı —
  ki deterministik değil, yani aynı yerde duran iki oyuncu farklı şeyler
  duyuyordu. Oysa başlıktaki metin tüm tasarımın var oluş sebebinin tam da bu
  özellik olduğunu söylüyor. Araç, hiç duyulmamış üç sesi doğrularken denetimsiz
  dört ses çalıyordu. Bu, tek bir kuralın burada iki kopya hâlinde yaşayıp
  yalnızca birinin denetlendiği dördüncü olay — kapı aralığı, iki media3 bileşeni
  ve armatürlerden sonra — ve denetlenen kopyanın ölü olduğu ilk olay. Motorun
  sınıfları artık gerçek üreteçlerin önündeki kare sayaçları. Code_To_Sound,
  başlıktaki herhangi bir üretecin çağıranı yoksa ya da ses geri çağrısı yeniden
  rastgele sayı üretecine başvurursa hata veriyor.
- **Dört yeni ses ve odanın nihayet basacağı bir zemini var.** `roomTone` boş
  binanın kendisi: beş saniyede birbirine vuran, yarım hertz aralıklı iki alçak
  ton; çok aşağı alçak-geçiren süzülmüş havalandırma; boğuk durmasın diye biraz
  tiz; ve 3.4 saniyede bir, ıslaklığı şebekeyi takip eden bir damla — elektriği
  gitmiş bölge suyun girdiği bölgedir, yani baştaki tüp ile damlama aynı olgunun
  iki kez duyulmasıdır. `breath`'in nefes alışı ile verişi farklı biçimlerde,
  çünkü simetrik bir zarf rüzgâr gibi duyulur. `heartbeat` lup ve dup; ikincisi
  daha yumuşak ve vuruşun beşte biri kadar geride. `torchClick` bir kontak
  darbesi ve yay çınlaması, 40 ms'de biter; sessizce yanan bir el feneri elde
  tutulan bir nesne değil bir arayüz düğmesi gibi duyulur. Nefes ve kalp
  dinlenirken sessiz ve Kotlin'in eşlemesi gereken ayarlayıcılar yerine tick
  içinden sürülüyor — balast sağlığı ve rutubet de öyle. On bir üreteç artık
  kendi C++'ıyla 1e-6 içinde uyuşuyor.
- **Ayak sesleri hiç durmuyordu.** Bir kez tetiklendikten sonra sabit aralıkla
  sonsuza kadar çalıyorlardı; çubuğu bırakınca karakter olduğu yerde yürümeye
  devam ediyordu.
- **Beden elbisenin içinden çıkıyordu ve bacakların baldırı yoktu.** Hiçbir
  yapısal kontrolün göremeyeceği iki kusur: dosya ayrıştırılıyor, kabuklar
  kapalı, rig pozlarını atlatıyor — ve karakter yine de kıyafetine oturmuyor.
  Yayınlanan mesh'te gövde çevresinde örneklenen 34 yönden 10'unda ten kumaşın
  dışındaydı; en kötüsü bir birim boyundaki bir figürde 16 mm, insan ölçeğinde
  yaklaşık 3 cm. Ayrıca beden kendi kalıplanmış eteğini taşıyordu: 107 mm'lik
  bir etek ucunun altında 109 mm'ye açılan, birincinin altından sarkan ikinci
  bir etek. İki yüzey de elbisenin yüksekliği boyunca dikey eksen etrafında
  yıldız biçimli, yani test doğrudan: aynı yükseklikte ve aynı açıda ten
  eksenden kumaştan daha uzakta mı? Gövde artık giysinin 6 mm içine
  yerleştirildi; kollar, bacaklar, baş ve elbise değişmedi, hareket eden her
  üçgenin normali yeniden hesaplandı.
  Ayrıca bir bacağın en geniş kesiti baldır hizasında 68 mm, bilekte 63 mm
  ölçüyordu — oran 1.08, oysa gerçek bir alt bacakta bu 1.6'ya yakındır, çünkü
  baldır karnı bacağın en geniş yeridir. Bacaklar dizden bileğe düz bir
  daralmaydı ve onları çubuk gibi gösteren şey, üçgen sayısından çok buydu.
  Baldır artık 87 mm. Şişkinlik, rig'in kendi en kötü deri dikişine karşı
  tarandı — çünkü daha geniş bir yüzey aynı kemik dönüşünde daha fazla yol alır:
  1.35 katına kadar hepsi uylukta zaten var olan tek bir dikişi paylaşıyor,
  1.42 kat ise incikte yenisini doğuruyor. Yani 1.35, bedava olan iyileşmenin
  tamamı. Uyluk için de bir şişkinlik denendi ve bırakıldı: her sürümü en kötü
  dikişi 2.97 cm'den 3.2 cm'ye çıkarıyordu ve düzeltmeye çalıştığı ölçüm dört
  vertekse dayanıyordu.
- **Oda doğru boyuttaydı, içindeki her şey iki katıydı.** Tavan Backrooms
  hissi vermiyordu çünkü T profil kafesi 1.6 m modüldeydi; metrik asma tavan
  600 mm'dir. Halı kareleri gerçek 500'e karşı 800 mm'ydi. Duvar dikey
  ekleri 800 mm'lik kağıt genişliğine karşı 1.6 m modüldeydi. Armatürler bir
  yorumda kendilerine 2x4 troffer diyordu — 610'a 1220 mm — ve 1340'a 2430
  olarak, metre yerine hücrenin kesirlerinden inşa ediliyordu; yani hücre
  boyutu değişseydi sessizce ölçeklenirlerdi. Floresan tüpler 134 mm
  kalınlığındaydı; T8'in çapı 26 mm'dir, 8 zaten çapın inç sekizde biri
  cinsinden ifadesi. Level 0'ın kendi ölçüleri hiç yanlış değildi: 3.2 m hücre
  ve 2.6 m tavan sıradan ofis rakamları. Ama gözün bir mekanın büyüklüğünü
  ölçtüğü en güçlü ipucu baştaki kafestir ve sekiz kare göstermesi gereken bir
  koridorda beş kare sayıyordu — doğru ölçekli bir oda, insandan büyük biri
  için yapılmış gibi okunuyordu. Artık hepsi metre cinsinden ve taklit ettiği
  parçayla eşleşiyor. Duvarlara ilk kez süpürgelik de eklendi: altında gölge
  boşluğu olan 100 mm'lik bir profil. Seviyedeki her duvar halıya hiçbir şey
  olmadan giriyordu; inşa edilmiş hiçbir oda böyle değildir.
- **Seviyenin karanlığını bekleyen kontrol yanlış biçimdeydi.** Zeminin ne
  kadarının el feneri istediğine dair tohum başına bir sınır sekiz tohumda
  ayarlanmıştı ve iyi görünüyordu; yirmi tohumda dördü sınırı aştı. Bir tohumun
  ne kadar karanlık çıkacağı gerçekten değişir — tohum zaten budur — yani bir
  gerilemeyi yakalayacak kadar dar bir sınır dürüst tohumları da eler. Artık
  altmış tohumun tamamının dağılımını doğruluyor: medyan, p90 ve en karanlıkla
  en aydınlık tohum arasındaki fark. Üçü de yerini hak ediyor, özellikle
  sonuncusu: eski 178 metrelik şebeke arıza gürültüsünü geri koymak, yayınlanan
  seviyeden *daha iyi* bir medyan veriyor — %20.9'a karşı %16.0 — çünkü
  tohumlarının çoğu aydınlık. Onu bozan şey %1.7 ile %67.5 arasındaki aralık.
  Arıza bölgeleri artık 178 yerine 34 m genişliğinde ve bu bir taramayla
  seçildi: yayılımın küçülmeyi bıraktığı ve en karanlık tohumun en aydınlıktan
  başka bir oyun olmaktan çıktığı yer.
- **Seviyenin büyük kısmı zifiri karanlıktı ve bunu bekleyen kontrol bir
  sabiti sorguluyordu.** Tavan armatürleri global bir kafese yerleştiriliyordu:
  bir hücre, açık zeminse ve iki koordinatı da dörde bölünüyorsa tüp taşıyordu.
  Kafes global, kat planı değil — yani bir koridorun aydınlık olup olmaması
  koordinat paritesine kalmıştı. z = 7 boyunca uzanan bir hücrelik koridor hiç
  kafes satırına değmez ve boyunca tek bir armatür almaz. Altı tohumda
  ölçüldü: tüm açık zeminin %54'ü 0.08 aydınlığın altındaydı — sahne shader'ı
  bunu albedonun %9'u olarak çiziyor — ve tek adımını göremeyeceğin en uzun
  kesintisiz yürüyüş 60 hücreydi: 192 metre. Armatür artık zeminin altına
  gelmesini beklemek yerine zemini arıyor: dörde dört blok başına bir tüp, aynı
  yoğunluk, kafes noktasından sabit bir sırayla dışa doğru halkalanarak en yakın
  açık hücreye. Böylece dünya hâlâ koordinatlarının saf bir fonksiyonu. Işık
  düşüş genişliği 0.95'ten 1.70'e çıktı; 0.95'te iki tüpün tam ortası — durmanın
  en olası yeri — tek bir tüpün onda birini alıyordu. Artık hiçbir yer siyah
  çıkmıyor ve zeminin %20'si el feneri istiyor, önceki %70'e karşı.
- **Bir tohum aydınlık bir lobi, diğeri üçte biri zifiri bir yerdi.** Şebeke
  arızası her 178 metrede bir dalga boyu olan bir gürültüden geliyordu; oyuncu
  aynı anda ancak iki dalga boyu görüyor ve iki örnek bir dağılım değildir. Altı
  tohumda zeminin elektriksiz payı %0 ile %35 arasında geziyordu — aynı oyunu
  oynayan iki kişi aynı tür yerde değildi. Ölçek artık 71 metre ve arıza eşiği
  dünyanın beşte birinden onda birine indi; on tohumda %7 ile %19 ölçüyor. Yani
  denk geldiğin ölü bölgeler, sık sık karanlık olan bir dünya değil. Ortam
  tabanı da 0.055'ten 0.20'ye çıktı: elektriksiz bir koridor artık el fenerine
  uzandığın bir loşluk, kapalı bir ekran değil.
- **`fixtureAt` ile `sampleChunk` lambaların nerede asılı olduğu konusunda
  anlaşamıyordu.** Yerleştirme kuralı iki kez yazılmıştı ve toplu örnekleyici
  zemine oturmayı öğrendiğinde tek hücrelik sorgu eski kafeste kaldı. Bu tam
  şeklin üçüncü kez ortaya çıkışı — kapı aralığı kuralı, iki media3 bileşeni,
  şimdi armatürler — bu yüzden kendi doğrulaması var: Level_0_Check her tohumda
  yirmi beş chunk'ın her hücresinde iki cevabı karşılaştırıyor. Aydınlatma
  yeniden yazıldığından beri o dosyada duran zifiri karanlık doğrulaması bir kez
  bile tetiklenmemişti; aydınlığın 0.02'nin altında olup olmadığını soruyordu,
  ortam tabanı ise 0.055'ti. Yanına bir oyuncunun gerçekten ne görebildiğini
  ölçen bir tane eklendi ve sınırı her hatayı kasten geri koyup aştığını
  görerek belirlendi.
- **Birinci şahısta el feneri ters tarafa sapıyordu.** Fenerin dünya konumu,
  kameranın kendi kurulduğu ileri vektörün üç bileşeninden ikisi ters
  çevrilmiş bir vektörden hesaplanıyordu; yukarı bakınca ışın aşağı, sola
  bakınca sağa gidiyordu. Üçüncü şahıs avatarın kendi dönüşümünü okuduğu için
  hiç bozulmamıştı — hatanın yalnızca yarı yarıya görünmesinin sebebi buydu.
  İkisi artık aynı temelden geliyor. Koni genişletildi ve zayıflama
  yumuşatıldı; açık olduğunu anlamak için duvara tutman gereken bir fenerdi.
- **Dönüş hızı yaklaşık üç kat fazlaydı ve her telefonda başka türlü.** Bakış
  farkı `cameraLook`'a ham piksel olarak giriyor, derece olarak çıkıyordu;
  1080p bir ekranda tek kaydırma varsayılan hassasiyette 500 dereceden fazla
  ediyor, daha yoğun bir ekranda aynı hareket daha da çeviriyordu. Artık dp
  cinsinden, her dp 0.42 derece. Assets_Check kaydırıcının iki ucunda tam
  genişlikte bir kaydırmayı benzetiyor ve varsayılan çeyrek turun altına ya da
  dörtte üçün üstüne çıkarsa hata veriyor — ilk çalıştırmasında da duraklatma
  menüsündeki kaydırıcının 4.0'a, ayarlar ekranındakinin 3.0'a gittiğini
  yakaladı.
- **VHS efekti kapalıyken açık kalıyordu.** Ayar shader'daki gren ve kroma
  terimlerini kapatıyordu ama tarama çizgileri oyunun üzerine koşulsuz çizilen
  ayrı bir Compose katmanıydı — yani efektin en görünür parçası anahtarı hiç
  dinlemiyordu. Ayrıca sabit `true` döndüren bir `GameState.vhsEnabled` vardı;
  değeri ayarlardan değil oyun durumundan okuyan her yerin gördüğü buydu.
- **Sekiz yaratık bire indi ve Smiler'a bir beden verildi.** Sekiz yaratık
  başka bir oyun demek — hangisine baktığını okumayı öğrenirsin ve asıl keyif o
  okumadadır. Seviye 0'da, hiçbir zaman doğru dürüst göremediğin tek bir şey
  var. Smiler'ın kendisi bir mukavva kesiğiydi: kenarı gürültüyle oynatılmış bir
  yumurta, tek bir kontur, her yerinde aynı kalınlıkta. Artık bir yoğunluk
  alanı — kıvrım boyunca sürüklenen dört oktav gürültü, etekte geniş, yükseldikçe
  daralıp savrulan, kopan tutamlar bırakan bir sütun. Yüz, çevresindeki dumanla
  çarpılıyor: sütun kalınken beliriyor, inceldiğinde dağılıyor. Tam güçle
  boyandığında sadece bir çıkartmaydı. Native AI'daki yedi davranış ağacı da
  yedi yaratıkla birlikte gitti.
- **Havada asılı duran texture kapıydı.** Mesher, duvar yüksekliğinin 0.82'sinde
  tek bir yatay dörtgen çiziyordu ve uzun kenarlarının ikisi de boşlukta
  bitiyordu. Üstündeki yorum "bir lento ve iki söve" diyordu; ikisi de hiç
  yazılmamıştı. Artık gerçek bir kasa var, ve Level_0_Check dayandığı varsayımı
  açıkça yazıyor — bu da kapı kuralının iki yerde birden durduğunu yakaladı:
  `featureAt` ve `sampleChunk`. Mesher birini okuyor, tüm kontroller diğerini.
- **Çerçeveler fotoğrafın üstüne çiziliyordu.** Boşluk payı her örneğin kendi
  yarıçapının bir oranıydı, ve `frameProfile` yalnız en geniş örneği 1.0'a
  normalize ediyor; yani dar noktalarda pay da onlarla küçülüyordu: kutunun
  0.283'ü, 0.33'lük bir portreye karşı. Artık sınır mutlak. Ayrıca halka artık
  kameradan 0.62 rad eğik değil — dairesel bir çerçeveyi dairesel bir resmin
  etrafında elips olarak çizen şey oydu.
- **Sekiz yaratığın hepsi Smiler'dı.** Tek fark, shader'ın 0.055 ile çarptığı bir
  renk tonuydu. Artık her birinin kendi silueti var: Howler'ın alçak başı ve
  geniş omuzları, Party Goer'ın uzuvları, Deathmoth'un kanatları, Wretched'ın
  altı gözü, Faceling'in ifadesiz kımıltısızlığı.
- **Üçüncü şahıs girişinde kamera diye bir şey yoktu.** Beden, sabit 2.6 m'de,
  karenin köşesinde yıkılıp kalkıyordu. Artık bom düşüş için geri çekiliyor,
  çarpmada diz hizasına iniyor ve onunla birlikte yükseliyor.
- **Firebase hiç çalışmadı ve yanında çok şey götürdü.** Burada
  google-services.json yok, CI bir yer tutucu enjekte ediyor; yani her
  Crashlytics kaydı, Firestore yazımı ve Remote Config isteği çalışma anında
  hata veriyor, `runCatching` de yutuyordu. REST API'si de aynı hikâyeydi:
  api.omnibackrooms.com diye bir adres çözülmüyor. Altındaki netcode kimsenin
  veri göndermediği bir soketi boşaltıyordu — sesli sohbet dahil. Hepsi gitti;
  hiç kullanılmayan Room, Billing ve Credential Manager da öyle. Kotlin 2824
  satır eksildi.
- **El feneri ekranın ortasındaki bir daireydi.** Post aşamasında uv
  (0.5, 0.47) noktasına çiziliyordu ve dünyada hiçbir konumu yoktu; ışığın
  göğsünden çıkıyormuş gibi görünmesinin sebebi tam olarak buydu. Artık sahne
  shader'ında gerçek bir spot ışığı ve fenerin merceğinden çıkıyor.
- **Sahip olunan izler kuşanılamıyordu.** Arka arkaya üç hata: sahip olunan id
  kümesi yalnız çerçevelerden atanıp her izi eziyordu, bir izi kuşanmanın tek
  yolu satın almaktı, ve koridor kuşanılan id'yi ekran başına bir kez okuyordu.
- **Bildirim izni intronun üstüne biniyordu.** İzin kapısı NavHost'un içinde
  değil yanındaydı. Artık intronun arkasında, ve ayarlardaki anahtar izni
  yalnızca sistem ekranına yönlendirmek yerine kendisi isteyebiliyor.
- **İki doku ikinin kuvveti değildi.** 1536x1024 ve 1448x1086; ikisi de mipmap
  zinciri taşıyamıyor, uzakta titriyordu. Dördü de 1024x1024 oldu; varlıklar
  6.0MB'tan 4.7MB'a indi.
- **Karakterin dört kolu vardı.** Mesh iki çift taşıyordu: kolları yanında duran
  bir gövde, ve yenleri T-pozunda dümdüz uzanan bir elbise. Kemikler kolların
  değil yenlerin üzerine konmuştu, yani rig boş kumaşı sallarken oyuncunun
  gördüğü kollar kalçaya kaynamış duruyordu. Artık yenler kolların üstünde ve
  bağlama mesafeyi havadan değil yüzey boyunca ölçüyor — etek ucu elin 4 cm
  yakınından geçiyor, hiçbir düz çizgi ölçüsü bu ikisini ayıramaz. Bir milimetre
  ötede kopyalanmış sekiz kabuk da onunla gitti: 1139 vertex, ve yol açtıkları
  z-çakışması.
- **Seviye 0'da bir kalabalık vardı.** Üçten sekize canavar, on iki saniyede bir
  takviye. Kalabalık korkutucu değil, meşguldür. Artık tam olarak bir tane var
  ve zorluk kaç tane olduğunu değil, o tekinin ne olduğunu değiştiriyor.
- **Canavarlar duvarların arkasından görüyordu.** Görüş, seviyeyi tamamen yok
  sayan bir mesafe kontrolüydü; teması kesmenin tek yolu onu geçmekti.
- **Bir canavarı kaçırmak onu kalıcı olarak siliyordu.** Kaçış mesafesi canlı
  oyuncu konumundan ölçülüyordu, yani peşinden gitmek onu sonsuza dek kaçar
  hâlde tutuyordu; ve park hâli her tick'te solmasını sıfırlıyordu, yani dönüş
  hiç tamamlanamıyordu. İkisi de simülasyonla bulundu, ikisi de cihazda
  bulunamazdı.
- **Üretilen her gürültünün yüzde sekizi tekrarlanan bir örnekti.** Hem C++ hem
  Python gürültü indeksini `int(t * 44100)` olarak alıyordu ve float'ta
  `i/44100*44100` `i`'nin bir kıl altına düşüyor. Duyuluyor, dalga biçiminde
  görünmüyor.
- **Türk oyuncular oda boyutu etiketinde düz bir `%d` görüyordu**; dize bir
  biçim belirteciyle yazılmış ama argümansız çiziliyordu.
- **Varsayılan kaynaklar Türkçeydi.** `values/`, Android'in karşılığı olmayan
  bir dil için başvurduğu yerdir; yani çevrilmemiş her dize, Almanca bir menünün
  ortasında Türkçe çıkıyordu. Artık orada İngilizce duruyor.
- **CI, sorunsuz kodda hata bildiriyordu.** İki iş tek runner için yarışıyordu;
  statik kontroller hiç başlamıyor, kuyrukta zaman aşımına uğruyor ve APK her
  seferinde kusursuz derlenirken turu başarısız gösteriyordu.
- **Karakter dört kolluymuş gibi görünüyordu.** Rig, uzuvları döndürürken açıyı
  bir konum gradyanıyla çarpıyordu; bu bir uzvu döndürmez, yayar. Yerine on iki
  kemikli bir iskelet üzerinde gerçek linear blend skinning kondu.
- **Kurcalama koruması temiz cihazları her açılışta suçluyordu**;
  `/proc/self/maps` içinde çıplak bir alt dize aramasından. Artık ne bulduğunu
  bildiriyor ve sebebi `Documents/Backrooms_Log/` altına yazıyor.
- **Tavan dokuları her karonun köşegeninde aynalanmıştı**: yayıcı UV'leri sabit
  bir köşe sırasıyla veriyordu, ki bu yalnızca ters sarılmış bir dörtgen için
  doğrudur.

## Lisans

Tüm hakları saklıdır. Kod okunmak için burada.
