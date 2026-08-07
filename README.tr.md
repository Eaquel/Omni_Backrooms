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
