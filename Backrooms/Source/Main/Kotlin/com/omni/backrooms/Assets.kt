package com.omni.backrooms

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sin

enum class EntityType(
    val typeId    : Int,
    val baseSpeed : Float,
    val hearRange : Float,
    val sightRange: Float,
    val aggroRange: Float,
    val displayName: String
) {
    SMILER      (0, 2.8f, 12f, 18f,  9f, "Smiler"),
    HOWLER      (1, 3.5f, 20f, 22f, 12f, "Howler"),
    DULLLER     (2, 1.4f,  8f, 14f,  6f, "Duller"),
    PARTYGOER   (3, 4.2f, 16f, 20f, 10f, "Party Goer"),
    SKIN_STEALER(4, 3.0f, 14f, 16f,  8f, "Skin Stealer"),
    WRETCHED    (5, 5.5f, 10f, 12f,  7f, "Wretched"),
    FACELING    (6, 2.2f, 18f, 24f, 14f, "Faceling"),
    DEATHMOTHS  (7, 6.0f,  6f,  8f,  5f, "Deathmoth")
}

data class SpawnConfig(
    val count          : Int,
    val speedMult      : Float,
    val sightMult      : Float,
    val spawnIntervalMs: Long
)

data class PreloadEvent(val progress: Float, val stage: String)

data class LevelTheme(
    val id          : String,
    val primaryColor: Color = Yellow,
    val bgColor     : Color = DarkBg
)

@Serializable
data class StoryChapterRaw(
    val id                            : Int,
    @SerialName("title_tr")      val titleTr     : String,
    @SerialName("title_en")      val titleEn     : String,
    val unlocked                      : Boolean,
    @SerialName("paragraphs_tr") val paragraphsTr: List<String>,
    @SerialName("paragraphs_en") val paragraphsEn: List<String>
)

@Serializable
data class StoryJson(val version: Int, val chapters: List<StoryChapterRaw>)

data class CharacterDef(
    val id         : String,
    val name       : String,
    val clazz      : CharClass,
    val maxHp      : Float,
    val baseSpeed  : Float,
    val stealthMult: Float,
    val staminaMult: Float,
    val abilities  : List<String>,
    val isUnlocked : Boolean,
    val isEquipped : Boolean
)

enum class CharClass { WANDERER, SCOUT, SURVIVOR, ENGINEER, GHOST }

@Singleton
class AssetManager @Inject constructor(
    @ApplicationContext private val ctx: Context
) {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private var storyCache: StoryJson? = null

    private val levelThemes = mapOf(
        0 to LevelTheme("level_0", Yellow,                    DarkBg),
        1 to LevelTheme("level_1", CrtAmber,                  DarkBg),
        2 to LevelTheme("level_2", Color(0xFF4FC3F7),          DarkBg),
        3 to LevelTheme("level_3", Color(0xFFEF9A9A),          DarkBg),
        4 to LevelTheme("level_4", SuccessGreen,               DarkBg),
        5 to LevelTheme("level_5", SouliumCol,                 DarkBg),
        6 to LevelTheme("level_6", TextDim,                    DarkBg),
        7 to LevelTheme("level_7", OmniumCol,                  DarkBg)
    )

    val defaultCharacters: List<CharacterDef> = listOf(
        CharacterDef("wanderer","Wanderer",CharClass.WANDERER, 100f,3.0f,1.0f,1.0f, listOf("Hayatta Kalma İçgüdüsü","Çevre Adaptasyonu"), isUnlocked=true,  isEquipped=true),
        CharacterDef("scout",   "Scout",   CharClass.SCOUT,     80f,4.5f,1.6f,1.2f, listOf("Hızlı Koşu","Sessiz Adım","Erken Uyarı"),     isUnlocked=false, isEquipped=false),
        CharacterDef("survivor","Survivor",CharClass.SURVIVOR, 150f,2.5f,0.8f,0.9f, listOf("Ağır Zırh","Son Nefes","HP Rejenerasyonu"),   isUnlocked=false, isEquipped=false),
        CharacterDef("engineer","Engineer",CharClass.ENGINEER,  90f,3.2f,1.0f,1.1f, listOf("Tuzak Kurma","Işık Tamiri","Pil Uzatma"),     isUnlocked=false, isEquipped=false),
        CharacterDef("ghost",   "Ghost",   CharClass.GHOST,     70f,3.8f,1.9f,0.8f, listOf("Geçici Görünmezlik","Yankısız Hareket"),       isUnlocked=false, isEquipped=false)
    )

    fun getLevelTheme(level: Int): LevelTheme = levelThemes[level] ?: LevelTheme("level_$level")

    fun getSpawnConfig(difficulty: String): SpawnConfig = when (difficulty.lowercase()) {
        "easy" -> SpawnConfig(count=3,  speedMult=0.7f, sightMult=0.8f, spawnIntervalMs=40_000)
        "hard" -> SpawnConfig(count=8,  speedMult=1.4f, sightMult=1.3f, spawnIntervalMs=12_000)
        else   -> SpawnConfig(count=5,  speedMult=1.0f, sightMult=1.0f, spawnIntervalMs=22_000)
    }

    fun loadStory(): StoryJson {
        storyCache?.let { return it }
        return runCatching {
            val raw = ctx.assets.open("story.json").bufferedReader().readText()
            json.decodeFromString<StoryJson>(raw).also { storyCache = it }
        }.getOrElse { StoryJson(version=1, chapters=emptyList()) }
    }

    fun storyChapterToDto(raw: StoryChapterRaw): StoryChapterDto = StoryChapterDto(
        id        = raw.id,
        titleTr   = raw.titleTr,
        titleEn   = raw.titleEn,
        contentTr = raw.paragraphsTr.joinToString("\n\n"),
        contentEn = raw.paragraphsEn.joinToString("\n\n"),
        isUnlocked= raw.unlocked
    )

    fun preload(): Flow<PreloadEvent> = flow {
        val stages = listOf(
            0.10f to "loading_stage_shaders",
            0.30f to "loading_stage_assets",
            0.55f to "loading_stage_entities",
            0.75f to "loading_stage_audio",
            0.90f to "loading_stage_network",
            1.00f to "loading_stage_done"
        )
        for ((progress, stageKey) in stages) {
            if (stageKey == "loading_stage_assets") runCatching { loadStory() }
            delay(380)
            val resId = ctx.resources.getIdentifier(stageKey, "string", ctx.packageName)
            val label = if (resId != 0) ctx.getString(resId) else stageKey
            emit(PreloadEvent(progress=progress, stage=label))
        }
    }.flowOn(Dispatchers.IO)
}

enum class ThreatLevel { CLEAN, SUSPICIOUS, HIGH, CRITICAL }

data class GuardReport(
    val flags           : Int         = 0,
    val isRooted        : Boolean     = false,
    val isFrida         : Boolean     = false,
    val isDebugged      : Boolean     = false,
    val isEmulator      : Boolean     = false,
    val isSignatureValid: Boolean     = true,
    val isHookDetected  : Boolean     = false,
    val isMemoryTampered: Boolean     = false,
    val report          : String      = "CLEAN",
    val threatLevel     : ThreatLevel = ThreatLevel.CLEAN
) {
    val isThreatDetected: Boolean get() = flags != 0
}

@Singleton
class GuardManager @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val bridge: NativeBridge
) {
    private val _report      = MutableStateFlow(GuardReport())
    val report: StateFlow<GuardReport> = _report.asStateFlow()

    private val _threatEvent = MutableSharedFlow<ThreatLevel>(extraBufferCapacity=4)
    val threatEvent: SharedFlow<ThreatLevel> = _threatEvent.asSharedFlow()

    private val guardScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var monitorJob: Job? = null

    fun initialize() {
        bridge.initGuard(ctx, BuildConfig.EXPECTED_SIG_HASH)
        runFullScan()
        startContinuousMonitor()
    }

    fun runFullScan() {
        val flags     = bridge.runGuardScan()
        val rooted    = bridge.isRooted()
        val frida     = bridge.isFridaDetected()
        val debugged  = bridge.isDebugged()
        val emulator  = bridge.isEmulator()
        val sigValid  = bridge.isSignatureValid()
        val hook      = detectHooking()
        val memTamper = detectMemoryTampering()
        val reportStr = bridge.getThreatReport()

        val level = when {
            frida || debugged || hook -> ThreatLevel.CRITICAL
            rooted || !sigValid       -> ThreatLevel.HIGH
            memTamper                 -> ThreatLevel.HIGH
            emulator                  -> ThreatLevel.SUSPICIOUS
            flags != 0                -> ThreatLevel.SUSPICIOUS
            else                      -> ThreatLevel.CLEAN
        }

        _report.value = GuardReport(
            flags=flags, isRooted=rooted, isFrida=frida, isDebugged=debugged,
            isEmulator=emulator, isSignatureValid=sigValid, isHookDetected=hook,
            isMemoryTampered=memTamper, report=reportStr, threatLevel=level
        )
        if (level != ThreatLevel.CLEAN) _threatEvent.tryEmit(level)

        if (level >= ThreatLevel.HIGH) {
            runCatching {
                FirebaseCrashlytics.getInstance().log("GUARD_THREAT level=$level report=$reportStr")
                FirebaseFirestore.getInstance()
                    .collection("threat_reports")
                    .document(ctx.packageName)
                    .set(mapOf("level" to level.name, "report" to reportStr, "ts" to System.currentTimeMillis()), SetOptions.merge())
            }
        }
    }

    private fun startContinuousMonitor() {
        monitorJob = guardScope.launch {
            while (isActive) { delay(30_000); runFullScan() }
        }
    }

    fun destroy() { monitorJob?.cancel(); guardScope.cancel(); bridge.destroyGuard() }

    fun verifyApkSignature(): Boolean = runCatching {
        val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            ctx.packageManager.getPackageInfo(ctx.packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()))
        else @Suppress("DEPRECATION") ctx.packageManager.getPackageInfo(ctx.packageName, PackageManager.GET_SIGNATURES)
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pInfo.signingInfo?.apkContentsSigners
                         else @Suppress("DEPRECATION") pInfo.signatures
        if (signatures.isNullOrEmpty()) return false
        val hash = MessageDigest.getInstance("SHA-256").digest(signatures[0].toByteArray()).joinToString("") { "%02x".format(it) }
        hash == BuildConfig.EXPECTED_SIG_HASH
    }.getOrElse { false }

    private fun detectHooking(): Boolean = runCatching {
        Thread.currentThread().stackTrace.any { el ->
            el.className.contains("xposed",    ignoreCase=true) ||
            el.className.contains("substrate", ignoreCase=true) ||
            el.className.contains("lsposed",   ignoreCase=true) ||
            el.className.contains("frida",     ignoreCase=true)
        }
    }.getOrElse { false }

    private fun detectMemoryTampering(): Boolean = runCatching {
        val content = File("/proc/self/maps").readText()
        listOf("frida","gadget","inject","hook","substrate","xposed","lsposed")
            .any { content.contains(it, ignoreCase=true) }
    }.getOrElse { false }
}

object VideoEncryptor {
    private const val KEY_ALIAS = "omni_video_key"
    private const val KEYSTORE  = "AndroidKeyStore"
    private const val ALGO      = "AES/GCM/NoPadding"
    private const val TAG_LEN   = 128
    private const val IV_LEN    = 12

    fun getOrCreateKey(): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        if (ks.containsAlias(KEY_ALIAS)) return (ks.getEntry(KEY_ALIAS,null) as KeyStore.SecretKeyEntry).secretKey
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).apply {
            init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).setUserAuthenticationRequired(false).build())
        }.generateKey()
    }

    fun encrypt(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(ALGO).apply { init(Cipher.ENCRYPT_MODE, getOrCreateKey()) }
        return cipher.iv + cipher.doFinal(data)
    }
    fun decrypt(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(ALGO).apply { init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_LEN, data, 0, IV_LEN)) }
        return cipher.doFinal(data, IV_LEN, data.size - IV_LEN)
    }
    fun encryptVideoSegment(segment: ByteArray, segmentIndex: Int): ByteArray {
        val iv = ByteArray(IV_LEN).also { it[0]=(segmentIndex shr 24).toByte(); it[1]=(segmentIndex shr 16).toByte(); it[2]=(segmentIndex shr 8).toByte(); it[3]=segmentIndex.toByte() }
        return Cipher.getInstance(ALGO).run { init(Cipher.ENCRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_LEN, iv)); doFinal(segment) }
    }
    fun decryptVideoSegment(segment: ByteArray, segmentIndex: Int): ByteArray {
        val iv = ByteArray(IV_LEN).also { it[0]=(segmentIndex shr 24).toByte(); it[1]=(segmentIndex shr 16).toByte(); it[2]=(segmentIndex shr 8).toByte(); it[3]=segmentIndex.toByte() }
        return Cipher.getInstance(ALGO).run { init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_LEN, iv)); doFinal(segment) }
    }
}

object IntegrityChecker {
    fun checkDeviceIntegrity(ctx: Context): Boolean {
        if (Build.TAGS?.contains("test-keys") == true)                       return false
        if (Build.FINGERPRINT?.startsWith("generic") == true)               return false
        if (Build.MODEL.contains("Emulator") || Build.MODEL.contains("Android SDK")) return false
        if (Build.HARDWARE == "goldfish" || Build.HARDWARE == "ranchu")      return false
        if (Build.PRODUCT.contains("sdk") || Build.PRODUCT.contains("vbox")) return false
        if (checkDangerousPaths()) return false
        if (checkDangerousPackages(ctx)) return false
        return true
    }
    private fun checkDangerousPaths(): Boolean = listOf(
        "/sbin/su","/system/bin/su","/system/xbin/su","/data/local/su","/data/local/xbin/su",
        "/data/adb/magisk","/sbin/.magisk","/system/app/SuperSU.apk","/system/app/Superuser.apk"
    ).any { File(it).exists() }
    private fun checkDangerousPackages(ctx: Context): Boolean = listOf(
        "com.topjohnwu.magisk","eu.chainfire.supersu","com.noshufou.android.su",
        "com.koushikdutta.superuser","de.robv.android.xposed.installer",
        "org.lsposed.manager","io.github.lsposed.manager"
    ).any { pkg -> runCatching { ctx.packageManager.getPackageInfo(pkg,0); true }.getOrElse { false } }
    fun computeFileHash(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream -> val buf=ByteArray(8192); var read: Int; while(stream.read(buf).also { read=it } != -1) md.update(buf,0,read) }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}

@HiltViewModel
class GuardVM @Inject constructor(
    private val guardManager: GuardManager
) : ViewModel() {
    val report     : StateFlow<GuardReport>  = guardManager.report
    val threatEvent: SharedFlow<ThreatLevel> = guardManager.threatEvent
    init {
        viewModelScope.launch {
            guardManager.threatEvent.collect { level ->
                when (level) {
                    ThreatLevel.CRITICAL -> android.os.Process.killProcess(android.os.Process.myPid())
                    ThreatLevel.HIGH     -> FirebaseCrashlytics.getInstance().log("HIGH_THREAT: ${report.value.report}")
                    else                 -> {}
                }
            }
        }
    }
    fun refresh() { viewModelScope.launch(Dispatchers.IO) { guardManager.runFullScan() } }
    fun verifySignature(): Boolean = guardManager.verifyApkSignature()
}

data class StoryUiState(
    val chapters   : List<StoryChapterDto> = emptyList(),
    val selected   : StoryChapterDto?      = null,
    val isLoading  : Boolean               = false,
    val readingMode: Boolean               = false
)

@HiltViewModel
class StoryVM @Inject constructor(
    private val api         : ApiService,
    private val assetManager: AssetManager
) : ViewModel() {
    private val _state = MutableStateFlow(StoryUiState())
    val state: StateFlow<StoryUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading=true) }
            val local = assetManager.loadStory().chapters.map { assetManager.storyChapterToDto(it) }
            if (local.isNotEmpty()) _state.update { it.copy(isLoading=false, chapters=local) }
            runCatching {
                val remote = loadFromFirestore()
                if (remote.isNotEmpty()) _state.update { it.copy(isLoading=false, chapters=remote) }
                else runCatching { api.getStoryChapters() }.onSuccess { r -> if (r.isNotEmpty()) _state.update { it.copy(isLoading=false, chapters=r) } }
            }.onFailure {
                if (_state.value.chapters.isEmpty()) _state.update { it.copy(isLoading=false) }
            }
        }
    }

    private suspend fun loadFromFirestore(): List<StoryChapterDto> = withContext(Dispatchers.IO) {
        runCatching {
            val snapshot = FirebaseFirestore.getInstance().collection("story_chapters").get().await()
            snapshot.documents.mapNotNull { doc ->
                runCatching {
                    StoryChapterDto(
                        id        = doc.getLong("id")?.toInt() ?: return@runCatching null,
                        titleTr   = doc.getString("titleTr")   ?: "",
                        titleEn   = doc.getString("titleEn")   ?: "",
                        contentTr = doc.getString("contentTr") ?: "",
                        contentEn = doc.getString("contentEn") ?: "",
                        isUnlocked= doc.getBoolean("isUnlocked") ?: false
                    )
                }.getOrNull()
            }
        }.getOrElse { emptyList() }
    }

    fun select(ch: StoryChapterDto) {
        if (!ch.isUnlocked) return
        _state.update { it.copy(selected=ch, readingMode=true) }
    }
    fun exitReading() { _state.update { it.copy(readingMode=false, selected=null) } }
}

@Composable
fun Story(onBack: () -> Unit, vm: StoryVM = hiltViewModel()) {
    val s by vm.state.collectAsState()
    AnimatedContent(
        targetState   = s.readingMode,
        transitionSpec= { fadeIn(tween(400)) togetherWith fadeOut(tween(300)) },
        label         = "story"
    ) { reading ->
        if (reading && s.selected != null) BookReadingView(chapter=s.selected!!, onClose=vm::exitReading)
        else ChapterListView(state=s, onBack=onBack, onSelect=vm::select)
    }
}

@Composable
private fun ChapterListView(state: StoryUiState, onBack: () -> Unit, onSelect: (StoryChapterDto) -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xFF050503))) {
        CrtOverlay()
        Column(Modifier.fillMaxSize()) {
            TopBarBack(stringResource(R.string.story_title), onBack)
            DividerLine()
            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color=Yellow, strokeWidth=2.dp) }
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding=PaddingValues(16.dp), verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    items(state.chapters, key={ it.id }) { ch -> ChapterCard(chapter=ch, onClick={ onSelect(ch) }) }
                }
            }
        }
    }
}

@Composable
private fun ChapterCard(chapter: StoryChapterDto, onClick: () -> Unit) {
    val locked = !chapter.isUnlocked
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(3.dp)).background(MetalBg)
            .border(1.dp, if (locked) BorderCol else Yellow.copy(0.3f), RoundedCornerShape(3.dp))
            .clickable(enabled=!locked, onClick=onClick).padding(14.dp),
        verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(3.dp)).background(if (locked) MetalBg else Yellow.copy(0.15f)), Alignment.Center) {
            if (locked) Icon(Icons.Default.Lock, null, modifier=Modifier.size(20.dp), tint=TextDim)
            else Text(chapter.id.toString(), color=Yellow, fontSize=18.sp, fontWeight=FontWeight.Black)
        }
        Column(Modifier.weight(1f), verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Text(if (locked) "???" else chapter.titleTr, color=if (locked) TextDim else Yellow, fontSize=14.sp, fontWeight=FontWeight.Bold)
            Text(
                if (locked) stringResource(R.string.story_chapter_locked) else chapter.contentTr.take(80)+"…",
                color=TextDim, fontSize=11.sp, maxLines=2, overflow=TextOverflow.Ellipsis, lineHeight=16.sp
            )
        }
        Icon(if (locked) Icons.Default.Lock else Icons.Default.ArrowForward, null,
            tint=if (locked) TextDim else Yellow, modifier=Modifier.size(18.dp))
    }
}

@Composable
private fun BookReadingView(chapter: StoryChapterDto, onClose: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xFF050503))) {
        CrtOverlay()
        Box(
            Modifier.align(Alignment.Center).fillMaxWidth(0.82f).fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(topStart=2.dp, bottomStart=2.dp, topEnd=10.dp, bottomEnd=10.dp))
                .background(Color(0xFF1A1408))
                .border(2.dp, Brush.verticalGradient(listOf(Color(0xFF5A3A10),Color(0xFF2A1A06),Color(0xFF5A3A10))),
                    RoundedCornerShape(topStart=2.dp, bottomStart=2.dp, topEnd=10.dp, bottomEnd=10.dp))
                .drawWithContent {
                    drawContent()
                    drawRect(Brush.horizontalGradient(listOf(Color.Black.copy(0.45f), Color.Transparent), 0f, 44f))
                    drawLine(Color(0xFF3A2208), Offset(36f,0f), Offset(36f,size.height), 3f)
                }
        ) {
            Column(
                Modifier.fillMaxSize().padding(start=56.dp, end=24.dp, top=28.dp, bottom=28.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement=Arrangement.spacedBy(16.dp)
            ) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("Bölüm ${chapter.id}", color=Color(0xFF8B6914), fontSize=12.sp, letterSpacing=3.sp)
                    IconButton(onClick=onClose, modifier=Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, null, modifier=Modifier.size(18.dp), tint=Color(0xFF8B6914))
                    }
                }
                Text(chapter.titleTr, color=Color(0xFF8B6914), fontSize=20.sp, fontWeight=FontWeight.Bold,
                    letterSpacing=2.sp, textAlign=TextAlign.Center, modifier=Modifier.fillMaxWidth())
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF5A3A10).copy(0.5f)))
                chapter.contentTr.split("\n\n").forEach { para ->
                    if (para.startsWith("\"") || para.startsWith("—"))
                        Text(para, color=Color(0xFF8A6A40), fontSize=12.sp, fontStyle=FontStyle.Italic, lineHeight=20.sp, textAlign=TextAlign.Center, modifier=Modifier.fillMaxWidth())
                    else
                        Text(para, color=Color(0xFFC8A870), fontSize=13.sp, lineHeight=22.sp, textAlign=TextAlign.Justify)
                }
                Spacer(Modifier.height(16.dp))
                Text("— Son —", color=Color(0xFF5A3A10), fontSize=11.sp, letterSpacing=3.sp, textAlign=TextAlign.Center, modifier=Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun CorridorCanvas(pan: Float, flicker: Float, modifier: Modifier) {
    androidx.compose.foundation.Canvas(modifier) {
        val w=size.width; val h=size.height; val cx=w/2f; val cy=h/2f
        drawRect(Color(0xFF1A1508))
        for (i in 0..7) {
            val t=(i/7f+pan*0.12f)%1f; val per=1f-t*0.94f
            val ww=w*per; val hh=h*per; val lx=cx-ww/2f; val ty=cy-hh/2f; val al=(1f-t*0.85f)*0.55f
            drawRect(Yellow.copy(al*0.09f), Offset(lx,ty), Size(ww,hh), style=Stroke(1.5f))
            val lightY=ty+hh*0.04f; val lightW=ww*0.28f; val lf=flicker*al
            drawRect(
                Brush.radialGradient(listOf(Color(0xFFEEDD88).copy(lf*0.95f),Yellow.copy(lf*0.3f),Color.Transparent), Offset(cx,lightY), lightW),
                Offset(cx-lightW/2f,lightY-18f), Size(lightW,36f)
            )
            val floorY=ty+hh
            drawRect(Brush.verticalGradient(listOf(Color(0xFF3A2A10).copy(al*0.4f),Color.Transparent),floorY,floorY+hh*0.08f), Offset(lx,floorY), Size(ww,hh*0.08f))
            drawRect(Color(0xFFD4A84B).copy(al*(sin(i*7.3f+pan*13.1f)*0.15f+0.85f)*0.04f), Offset(lx,ty), Size(ww,hh))
        }
        drawRect(Brush.radialGradient(listOf(Color.Transparent,Color.Black.copy(0.75f)), Offset(cx,cy), w*0.76f))
    }
}

@Composable
fun CarpetProgressBar(progress: Float, modifier: Modifier) {
    androidx.compose.foundation.Canvas(modifier) {
        val w=size.width; val h=size.height
        drawRoundRect(Color(0xFF1A1208), cornerRadius=CornerRadius(h/2f))
        if (progress > 0f) {
            val fw=w*progress
            drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFF3D2B10),Color(0xFF7A5A18),Color(0xFFD4A84B),Color(0xFF9A7228)),0f,fw),
                size=Size(fw,h), cornerRadius=CornerRadius(h/2f))
            val sc=(fw/(h*1.6f)).toInt()
            repeat(sc) { j -> val sx=j*h*1.6f; if(sx<fw) drawLine(Color(0xFF2A1A08).copy(0.45f),Offset(sx,0f),Offset(sx+h*0.6f,h),2f) }
            drawRect(Brush.verticalGradient(listOf(Color.White.copy(0.18f),Color.Transparent)), Offset(0f,0f), Size(fw,h/2f))
        }
        drawRoundRect(Color(0xFF5A4020), cornerRadius=CornerRadius(h/2f), style=Stroke(1f))
    }
}

@Composable
private fun PlayerCard(profile: PlayerProfile) {
    OmniPanel {
        Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(46.dp).clip(CircleShape).background(MetalBg), Alignment.Center) {
                if (profile.avatarUrl != null) AsyncImage(profile.avatarUrl, null, Modifier.fillMaxSize())
                else Text(profile.name.take(1).uppercase(), color=Yellow, fontSize=18.sp, fontWeight=FontWeight.Black)
            }
            Column(verticalArrangement=Arrangement.spacedBy(2.dp)) {
                Text(profile.name, color=Yellow, fontSize=13.sp, fontWeight=FontWeight.Bold)
                Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.player_level_prefix)+profile.level, color=TextSec, fontSize=11.sp)
                    if (profile.isVip) {
                        Box(Modifier.clip(RoundedCornerShape(1.dp)).background(Color(0xFFFFD700).copy(0.2f)).padding(horizontal=4.dp, vertical=1.dp)) {
                            Text("VIP", color=Color(0xFFFFD700), fontSize=7.sp, fontWeight=FontWeight.Black)
                        }
                    }
                }
                LinearProgressIndicator(progress={profile.xpProgress}, modifier=Modifier.width(88.dp).height(4.dp).clip(RoundedCornerShape(2.dp)), color=Yellow, trackColor=MetalBg)
            }
        }
    }
}

fun formatCurrency(amount: Long): String = when {
    amount >= 1_000_000 -> "${amount/1_000_000}M"
    amount >= 1_000     -> "${amount/1_000}K"
    else                -> amount.toString()
}
