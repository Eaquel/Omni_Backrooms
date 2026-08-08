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
| `Shaders_Check.py` | GLSL, Kotlin ham dizelerinin içinde yaşıyor. Derlenmeyecek bir shader, onu kullanan ekran açılıp siyaha dönene kadar görünmez. Hepsi `glslangValidator` ile derleniyor. |
| `Assets_Check.py` | `aapt2`'nin kabul edip bozuk çizdiği elle yazılmış vektör ikonlar; artık dünya konumuyla eşleşmeyen mesh UV'leri; fon dışına çıkan inceleme kamerası; tekrarlanan ve hiç kullanılmayan varlıklar; geride kalmış bir dil; kendi kendisiyle çelişen Unity kamuflajı; sürdüğünü iddia ettiği geometrinin üzerinde olmayan kemiklere sahip bir karakter rig'i — animasyonu oynatıp dikişleri ölçerek kanıtlanır. Ayrıca `--optimise`: kayıpsız bir PNG yeniden kodlayıcı. |
| `Native_Check.py` | JNI sözleşmesi. Kotlin `external fun` bildiriyor, C++ `Java_..._name` tanımlıyor ve derleme anında ikisini **hiçbir şey** bağlamıyor — ne Kotlin derleyicisi, ne C++ derleyicisi, ne bağlayıcı. Tek taraflı bir isim değişikliği ilk çağrıda `UnsatisfiedLinkError` demek; argüman sayısı değişirse daha kötü, çünkü JNI isimle bağlar ve fazla argümanları yığından şikâyet etmeden okur. |
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
    Frame/      profil çerçevesi kozmetikleri
    Trail/      ayak izi kozmetikleri
    Shield/     dedektörler, ve ikilinin kendini ne olarak gösterdiği
  Assets/                        dokular, meshler, hikâye
  res/values*/                   on dil
Tools/                           sekiz kontrol
```

## Son düzeltmeler

En yenisi üstte. Bu liste her düzeltmede güncelleniyor.

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
