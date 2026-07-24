package com.omni.backrooms

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import java.util.Locale
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.opengl.GLUtils
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer

val Yellow       = Color(0xFFD4A84B)
val YellowDim    = Color(0x80D4A84B)
val DarkBg       = Color(0xFF0A0A08)
val PanelBg      = Color(0xCC121208)
val MetalBg      = Color(0xFF1A1A14)
val CrtAmber     = Color(0xFFFFB347)
val TextSec      = Color(0xFF8A7040)
val TextDim      = Color(0xFF5A4A28)
val BorderCol    = Color(0xFF2A2018)
val SouliumCol   = Color(0xFF7B68EE)
val OmniumCol    = Color(0xFF00E5FF)
val DangerRed    = Color(0xFFCC2200)
val SuccessGreen = Color(0xFF4CAF50)

@HiltAndroidApp
class App : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("il2cpp")
        appScope.launch(Dispatchers.IO) {
            val bridge = NativeBridge()
            bridge.initGuard(applicationContext, BuildConfig.EXPECTED_SIG_HASH)
            val flags = bridge.getGuardFlags()
            if (flags != 0) {
                val report = bridge.getThreatReport()
                FirebaseCrashlytics.getInstance().log("APP_START_THREAT flags=$flags report=$report")
            }
            FirebaseMessaging.getInstance().subscribeToTopic("backrooms_global")
        }
    }
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "omni_prefs")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDataStore(@ApplicationContext ctx: Context): DataStore<Preferences> = ctx.dataStore

    @Provides @Singleton
    fun provideNativeBridge(): NativeBridge = NativeBridge()

    @Provides @Singleton
    fun provideGuardManager(@ApplicationContext ctx: Context, bridge: NativeBridge): GuardManager =
        GuardManager(ctx, bridge)

    @Provides @Singleton
    fun provideAssetManager(@ApplicationContext ctx: Context): AssetManager = AssetManager(ctx)

    @Provides @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }

    @Provides @Singleton
    fun provideAuthInterceptor(@ApplicationContext ctx: Context): Interceptor = Interceptor { chain ->
        val prefs = ctx.getSharedPreferences("omni_auth", Context.MODE_PRIVATE)
        val token = prefs.getString("access_token", "") ?: ""
        val req = if (token.isNotEmpty())
            chain.request().newBuilder().addHeader("Authorization", "Bearer $token").build()
        else chain.request()
        chain.proceed(req)
    }

    @Provides @Singleton
    fun provideOkHttp(authInterceptor: Interceptor): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .build()

    @Provides @Singleton
    fun provideRetrofit(okHttp: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttp)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService = retrofit.create(ApiService::class.java)

    @Provides @Singleton
    fun provideRoomRepository(api: ApiService): RoomRepository = RoomRepository(api)

    @Provides @Singleton
    fun provideSettingsRepository(store: DataStore<Preferences>, bridge: NativeBridge): SettingsRepository =
        SettingsRepository(store, bridge)

    @Provides @Singleton
    fun provideGoogleAuthManager(@ApplicationContext ctx: Context): GoogleAuthManager =
        GoogleAuthManager(ctx)
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val ctrl = WindowInsetsControllerCompat(window, window.decorView)
        ctrl.hide(WindowInsetsCompat.Type.systemBars())
        ctrl.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        window.decorView.keepScreenOn = true
        setContent { OmniBackroomsApp() }
    }
}

private object Route {
    const val MENU        = "menu"
    const val GAME        = "game"
    const val SETTINGS    = "settings"
    const val STORY       = "story"
    const val MARKET      = "market"
    const val ROOM        = "room"
    const val CREATE_ROOM = "create_room"
    const val LEADERBOARD = "leaderboard"
    const val PROFILE     = "profile"
    const val UI_EDITOR   = "ui_editor"
}

@Composable
fun OmniBackroomsApp() {
    val nav = rememberNavController()
    val guardVm: GuardVM = hiltViewModel()
    val guardReport by guardVm.report.collectAsState()
    var showGuardDialog by remember { mutableStateOf(false) }
    LaunchedEffect(guardReport.threatLevel) {
        if (guardReport.threatLevel >= ThreatLevel.SUSPICIOUS) showGuardDialog = true
    }
    MaterialTheme(colorScheme = darkColorScheme()) {
        if (showGuardDialog) {
            AlertDialog(
                onDismissRequest = { showGuardDialog = false },
                title    = { Text(stringResource(R.string.guard_threat_title)) },
                text     = { Text(stringResource(R.string.guard_threat_message)) },
                confirmButton = {
                    TextButton(onClick = { showGuardDialog = false }) { Text(stringResource(R.string.common_ok)) }
                }
            )
        }
        NavHost(nav, startDestination = Route.MENU) {
            composable(
                Route.MENU,
                enterTransition = { fadeIn(tween(600)) },
                exitTransition  = { fadeOut(tween(400)) }
            ) {
                MainMenu(
                    onPlay        = { nav.navigate(Route.GAME) },
                    onOnline      = { nav.navigate(Route.ROOM) },
                    onSettings    = { nav.navigate(Route.SETTINGS) },
                    onStory       = { nav.navigate(Route.STORY) },
                    onMarket      = { nav.navigate(Route.MARKET) },
                    onLeaderboard = { nav.navigate(Route.LEADERBOARD) },
                    onProfile     = { nav.navigate(Route.PROFILE) }
                )
            }
            composable(
                Route.GAME,
                enterTransition = { fadeIn(tween(800)) },
                exitTransition  = { fadeOut(tween(500)) }
            ) { GameScreen(onExit = { nav.popBackStack() }) }
            composable(
                Route.SETTINGS,
                enterTransition = { slideInHorizontally(tween(400)) { it } + fadeIn(tween(400)) },
                exitTransition  = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
            ) { SettingsScreen(onBack = { nav.popBackStack() }, onUiEditor = { nav.navigate(Route.UI_EDITOR) }) }
            composable(
                Route.STORY,
                enterTransition = { slideInHorizontally(tween(400)) { it } + fadeIn(tween(400)) },
                exitTransition  = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
            ) { Story(onBack = { nav.popBackStack() }) }
            composable(
                Route.MARKET,
                enterTransition = { slideInVertically(tween(400)) { it } + fadeIn(tween(400)) },
                exitTransition  = { slideOutVertically(tween(300)) { it } + fadeOut(tween(300)) }
            ) { MarketScreen(onBack = { nav.popBackStack() }) }
            composable(Route.ROOM)        { Room(onJoined = { nav.navigate(Route.GAME) }, onBack = { nav.popBackStack() }) }
            composable(Route.CREATE_ROOM) { CreateRoom(onCreated = { nav.navigate(Route.ROOM) }, onBack = { nav.popBackStack() }) }
            composable(Route.LEADERBOARD) { LeaderboardScreen(onBack = { nav.popBackStack() }) }
            composable(Route.PROFILE)     { ProfileScreen(onBack = { nav.popBackStack() }) }
            composable(Route.UI_EDITOR)   { UiEditor(onSave = { nav.popBackStack() }) }
        }
    }
}

@Singleton
class GoogleAuthManager @Inject constructor(
    @ApplicationContext private val ctx: Context
) {
    private val credentialManager = CredentialManager.create(ctx)
    private val auth = FirebaseAuth.getInstance()

    val currentUser get() = auth.currentUser
    val isSignedIn  get() = auth.currentUser != null

    suspend fun signIn(activity: android.app.Activity): Result<com.google.firebase.auth.FirebaseUser> {
        return runCatching {
            val nonce      = SecureRandom().let { r -> ByteArray(16).also { r.nextBytes(it) } }
                .joinToString("") { "%02x".format(it) }
            val hashedNonce = MessageDigest.getInstance("SHA-256")
                .digest(nonce.toByteArray()).joinToString("") { "%02x".format(it) }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(ctx.getString(R.string.default_web_client_id))
                .setAutoSelectEnabled(false)
                .setNonce(hashedNonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential

            check(credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                "Unexpected credential type"
            }

            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
            val authResult = auth.signInWithCredential(firebaseCredential).await()
            authResult.user!!
        }
    }

    fun signOut() { auth.signOut() }
}

data class GoogleAuthState(
    val isSignedIn   : Boolean = false,
    val displayName  : String  = "",
    val email        : String  = "",
    val photoUrl     : String? = null,
    val isLoading    : Boolean = false,
    val error        : String? = null
)

@HiltViewModel
class GoogleAuthVM @Inject constructor(
    private val googleAuthManager: GoogleAuthManager
) : ViewModel() {
    private val _state = MutableStateFlow(GoogleAuthState())
    val state: StateFlow<GoogleAuthState> = _state.asStateFlow()

    init { refreshState() }

    fun refreshState() {
        val user = googleAuthManager.currentUser
        _state.value = if (user != null) {
            GoogleAuthState(
                isSignedIn  = true,
                displayName = user.displayName ?: "",
                email       = user.email ?: "",
                photoUrl    = user.photoUrl?.toString()
            )
        } else GoogleAuthState()
    }

    fun signIn(activity: android.app.Activity) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            googleAuthManager.signIn(activity)
                .onSuccess { user ->
                    _state.value = GoogleAuthState(
                        isSignedIn  = true,
                        displayName = user.displayName ?: "",
                        email       = user.email ?: "",
                        photoUrl    = user.photoUrl?.toString(),
                        isLoading   = false
                    )
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun signOut() {
        googleAuthManager.signOut()
        _state.value = GoogleAuthState()
    }
}

enum class EntityType(
    val typeId    : Int,
    /** Native AI id (Engine.cpp EntityType/BehaviorTree dispatch). Not the same as
     *  [typeId]: the native behavior-tree roster (Smiler=0,HoundDog=1,PartyGoer=2,
     *  Skin_Stealer=3,WanderingOne=4,Deathwatch=5,Crawler=6,FacelingDark=7) is fixed
     *  gameplay logic, so this maps each lore creature to the behavior that actually
     *  matches it instead of assuming the two rosters line up 1:1. */
    val nativeAiId: Int,
    val baseSpeed : Float,
    val hearRange : Float,
    val sightRange: Float,
    val aggroRange: Float,
    val displayName: String
) {
    SMILER      (0, 0, 2.8f, 12f, 18f,  9f, "Smiler"),
    HOWLER      (1, 1, 3.5f, 20f, 22f, 12f, "Howler"),
    PARTYGOER   (2, 2, 4.2f, 16f, 20f, 10f, "Party Goer"),
    SKIN_STEALER(3, 3, 3.0f, 14f, 16f,  8f, "Skin Stealer"),
    DULLLER     (4, 4, 1.4f,  8f, 14f,  6f, "Duller"),
    WRETCHED    (5, 5, 5.5f, 10f, 12f,  7f, "Wretched"),
    DEATHMOTHS  (6, 6, 6.0f,  6f,  8f,  5f, "Deathmoth"),
    FACELING    (7, 7, 2.2f, 18f, 24f, 14f, "Faceling")
}

data class SpawnConfig(val count: Int, val speedMult: Float, val sightMult: Float, val spawnIntervalMs: Long)
data class PreloadEvent(val progress: Float, val stage: String)
data class LevelTheme(val id: String, val primaryColor: Color = Yellow, val bgColor: Color = DarkBg)

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

/** Matches the schema actually used by the shipped en.json/tr.json assets — each
 *  file is monolingual (title/paragraphs, no _tr/_en suffix). Kept private and
 *  used only as a parsing step before merging into [StoryChapterRaw] below, so
 *  nothing downstream (StoryVM, the reader UI) needs to change. */
@Serializable
private data class StoryChapterMono(
    val id        : Int,
    val title     : String = "",
    val subtitle  : String = "",
    val unlocked  : Boolean = false,
    val paragraphs: List<String> = emptyList()
)

@Serializable
private data class StoryFileMono(val version: Int = 1, val chapters: List<StoryChapterMono> = emptyList())

data class CharacterDef(
    val id: String, val name: String, val clazz: CharClass,
    val maxHp: Float, val baseSpeed: Float, val stealthMult: Float, val staminaMult: Float,
    val abilities: List<String>, val isUnlocked: Boolean, val isEquipped: Boolean
)

enum class CharClass { WANDERER, SCOUT, SURVIVOR, ENGINEER, GHOST }

@Singleton
class AssetManager @Inject constructor(@ApplicationContext private val ctx: Context) {
    private val json       = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private var storyCache : StoryJson? = null

    private val levelThemes = mapOf(
        0 to LevelTheme("level_0", Yellow,              DarkBg),
        1 to LevelTheme("level_1", CrtAmber,            DarkBg),
        2 to LevelTheme("level_2", Color(0xFF4FC3F7),   DarkBg),
        3 to LevelTheme("level_3", Color(0xFFEF9A9A),   DarkBg),
        4 to LevelTheme("level_4", SuccessGreen,        DarkBg),
        5 to LevelTheme("level_5", SouliumCol,          DarkBg),
        6 to LevelTheme("level_6", TextDim,             DarkBg),
        7 to LevelTheme("level_7", OmniumCol,           DarkBg)
    )

    val defaultCharacters: List<CharacterDef> = listOf(
        CharacterDef("wanderer","Wanderer",CharClass.WANDERER, 100f,3.0f,1.0f,1.0f, listOf("Hayatta Kalma İçgüdüsü","Çevre Adaptasyonu"), isUnlocked=true,  isEquipped=true),
        CharacterDef("scout",   "Scout",   CharClass.SCOUT,     80f,4.5f,1.6f,1.2f, listOf("Hızlı Koşu","Sessiz Adım","Erken Uyarı"),     isUnlocked=false, isEquipped=false),
        CharacterDef("survivor","Survivor",CharClass.SURVIVOR, 150f,2.5f,0.8f,0.9f, listOf("Ağır Zırh","Son Nefes","HP Rejenerasyonu"),   isUnlocked=false, isEquipped=false),
        CharacterDef("engineer","Engineer",CharClass.ENGINEER,  90f,3.2f,1.0f,1.1f, listOf("Tuzak Kurma","Işık Tamiri","Pil Uzatma"),     isUnlocked=false, isEquipped=false),
        CharacterDef("ghost",   "Ghost",   CharClass.GHOST,     70f,3.8f,1.9f,0.8f, listOf("Geçici Görünmezlik","Yankısız Hareket"),      isUnlocked=false, isEquipped=false)
    )

    fun getLevelTheme(level: Int): LevelTheme = levelThemes[level] ?: LevelTheme("level_$level")

    fun getSpawnConfig(difficulty: String): SpawnConfig = when (difficulty.lowercase()) {
        "easy" -> SpawnConfig(count=3,  speedMult=0.7f, sightMult=0.8f, spawnIntervalMs=40_000)
        "hard" -> SpawnConfig(count=8,  speedMult=1.4f, sightMult=1.3f, spawnIntervalMs=12_000)
        else   -> SpawnConfig(count=5,  speedMult=1.0f, sightMult=1.0f, spawnIntervalMs=22_000)
    }

    fun loadStory(): StoryJson {
        storyCache?.let { return it }
        fun readMono(name: String): StoryFileMono? =
            runCatching { ctx.assets.open(name).bufferedReader().readText() }
                .mapCatching { json.decodeFromString<StoryFileMono>(it) }
                .getOrNull()

        val en = readMono("en.json")
        val tr = readMono("tr.json")
        val byIdEn = en?.chapters?.associateBy { it.id } ?: emptyMap()
        val byIdTr = tr?.chapters?.associateBy { it.id } ?: emptyMap()
        val ids = (byIdEn.keys + byIdTr.keys).sorted()

        val merged = ids.map { id ->
            val e = byIdEn[id]; val t = byIdTr[id]
            StoryChapterRaw(
                id           = id,
                titleTr      = t?.title.takeUnless { it.isNullOrBlank() } ?: e?.title.orEmpty(),
                titleEn      = e?.title.takeUnless { it.isNullOrBlank() } ?: t?.title.orEmpty(),
                unlocked     = t?.unlocked ?: e?.unlocked ?: false,
                paragraphsTr = t?.paragraphs?.takeIf { it.isNotEmpty() } ?: e?.paragraphs.orEmpty(),
                paragraphsEn = e?.paragraphs?.takeIf { it.isNotEmpty() } ?: t?.paragraphs.orEmpty()
            )
        }
        return StoryJson(version = 1, chapters = merged).also { storyCache = it }
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
        val stages = listOf(0.10f to "loading_stage_shaders", 0.30f to "loading_stage_assets",
            0.55f to "loading_stage_entities", 0.75f to "loading_stage_audio",
            0.90f to "loading_stage_network",  1.00f to "loading_stage_done")
        for ((progress, stageKey) in stages) {
            if (stageKey == "loading_stage_assets") runCatching { loadStory() }
            delay(380)
            val resId = ctx.resources.getIdentifier(stageKey, "string", ctx.packageName)
            emit(PreloadEvent(progress=progress, stage=if (resId != 0) ctx.getString(resId) else stageKey))
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

    private val _threatEvent = MutableSharedFlow<ThreatLevel>(extraBufferCapacity = 4)
    val threatEvent: SharedFlow<ThreatLevel> = _threatEvent.asSharedFlow()

    private val guardScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var monitorJob: Job? = null

    fun initialize() { bridge.initGuard(ctx, BuildConfig.EXPECTED_SIG_HASH); runFullScan(); startContinuousMonitor() }

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
        val level     = when {
            frida || debugged || hook -> ThreatLevel.CRITICAL
            rooted || !sigValid       -> ThreatLevel.HIGH
            memTamper                 -> ThreatLevel.HIGH
            emulator                  -> ThreatLevel.SUSPICIOUS
            flags != 0                -> ThreatLevel.SUSPICIOUS
            else                      -> ThreatLevel.CLEAN
        }
        _report.value = GuardReport(flags, rooted, frida, debugged, emulator, sigValid, hook, memTamper, reportStr, level)
        if (level != ThreatLevel.CLEAN) _threatEvent.tryEmit(level)
        if (level >= ThreatLevel.HIGH) runCatching {
            FirebaseCrashlytics.getInstance().log("GUARD_THREAT level=$level report=$reportStr")
            FirebaseFirestore.getInstance().collection("threat_reports").document(ctx.packageName)
                .set(mapOf("level" to level.name, "report" to reportStr, "ts" to System.currentTimeMillis()), SetOptions.merge())
        }
    }

    private fun startContinuousMonitor() {
        monitorJob = guardScope.launch { while (isActive) { delay(30_000); runFullScan() } }
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
            listOf("xposed","substrate","lsposed","frida").any { el.className.contains(it, ignoreCase=true) }
        }
    }.getOrElse { false }

    private fun detectMemoryTampering(): Boolean = runCatching {
        val content = File("/proc/self/maps").readText()
        listOf("frida","gadget","inject","hook","substrate","xposed","lsposed").any { content.contains(it, ignoreCase=true) }
    }.getOrElse { false }
}

@HiltViewModel
class GuardVM @Inject constructor(private val guardManager: GuardManager) : ViewModel() {
    val report     : StateFlow<GuardReport>  = guardManager.report
    val threatEvent: SharedFlow<ThreatLevel> = guardManager.threatEvent
    init {
        guardManager.initialize()
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

enum class MarketTab(val labelRes: Int, val icon: ImageVector) {
    Boosts    (R.string.market_tab_boosts,     Icons.Default.Bolt),
    Characters(R.string.market_tab_characters, Icons.Default.Person),
    Soulium   (R.string.market_tab_soulium,    Icons.Default.AutoAwesome),
    Vip       (R.string.market_tab_vip,        Icons.Default.Star),
    Daily     (R.string.market_tab_daily,      Icons.Default.LocalOffer)
}

private val ANON_NAME_CHARS = listOf('%','#','₺','&','@','!','?','*','§','¿','¡','†','‡','~','^','|','≈','∆','√','∞')
private const val ANON_NAME_FRAME_MS = 120L

@Composable
fun rememberAnonDisplayName(): String {
    var frame by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(ANON_NAME_FRAME_MS); frame++ } }
    return (0..5).joinToString("") { slot -> ANON_NAME_CHARS[(frame + slot * 3) % ANON_NAME_CHARS.size].toString() }
}

data class MarketUiState(
    val items       : List<MarketItemDto> = emptyList(),
    val dailyDeals  : List<MarketItemDto> = emptyList(),
    val isLoading   : Boolean             = false,
    val error       : String?             = null,
    val purchasing  : String?             = null,
    val successMsg  : String?             = null,
    val tab         : MarketTab           = MarketTab.Boosts,
    val omniumBal   : Long                = 0L,
    val souliumBal  : Long                = 0L,
    val isVip       : Boolean             = false,
    val confirmItem : MarketItemDto?      = null,
    val characters  : List<CharacterDto>  = emptyList(),
    val selectedChar: CharacterDto?       = null,
    val charsLoading: Boolean             = false,
    val equipping   : String?             = null
)

@HiltViewModel
class MarketVM @Inject constructor(
    private val api         : ApiService,
    private val assetManager: AssetManager,
    @ApplicationContext private val appCtx: Context
) : ViewModel() {
    private val _state = MutableStateFlow(MarketUiState())
    val state: StateFlow<MarketUiState> = _state.asStateFlow()

    init { loadTab(MarketTab.Boosts); loadDaily(); loadProfile() }

    private fun loadProfile() {
        viewModelScope.launch {
            runCatching { api.getProfile() }.onSuccess { p ->
                _state.update { it.copy(omniumBal = p.omniumAmount, souliumBal = p.souliumAmount, isVip = p.isVip) }
            }
        }
    }

    fun setTab(tab: MarketTab) {
        _state.update { it.copy(tab = tab) }
        when (tab) { MarketTab.Characters -> loadCharacters(); MarketTab.Daily -> return; else -> loadTab(tab) }
    }

    private fun loadTab(tab: MarketTab) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching { api.getMarketItems(tab.name.lowercase()) }
                .onSuccess { page -> _state.update { it.copy(isLoading = false, items = page.items) } }
                .onFailure { e   -> _state.update { it.copy(isLoading = false, error = e.message, items = fallbackItems(tab)) } }
        }
    }

    private fun loadDaily() {
        viewModelScope.launch {
            runCatching { api.getDailyDeals() }
                .onSuccess { deals -> _state.update { it.copy(dailyDeals = deals) } }
                .onFailure { _state.update { it.copy(dailyDeals = fallbackDaily()) } }
        }
    }

    fun loadCharacters() {
        viewModelScope.launch {
            _state.update { it.copy(charsLoading = true) }
            runCatching { api.getCharacters() }
                .onSuccess { chars ->
                    val selected = chars.firstOrNull { it.isEquipped } ?: chars.firstOrNull()
                    _state.update { it.copy(charsLoading = false, characters = chars, selectedChar = selected) }
                }
                .onFailure { _state.update { it.copy(charsLoading = false, characters = emptyList()) } }
        }
    }

    fun selectChar(char: CharacterDto) { _state.update { it.copy(selectedChar = char) } }

    fun equip(char: CharacterDto) {
        viewModelScope.launch {
            _state.update { it.copy(equipping = char.id) }
            runCatching { api.equipCharacter(char.id) }
                .onSuccess { _state.update { it.copy(equipping = null) }; loadCharacters() }
                .onFailure { _state.update { it.copy(equipping = null) } }
        }
    }

    fun confirmBuy(item: MarketItemDto) { _state.update { it.copy(confirmItem = item) } }
    fun cancelBuy()                     { _state.update { it.copy(confirmItem = null) } }

    fun buy(item: MarketItemDto) {
        viewModelScope.launch {
            _state.update { it.copy(purchasing = item.id, confirmItem = null) }
            runCatching { api.buyItem(BuyRequest(item.id, item.currency)) }
                .onSuccess { r ->
                    _state.update {
                        it.copy(
                            purchasing  = null,
                            successMsg  = "Satın alındı!",
                            omniumBal   = if (item.currency == "omnium") r.newBalance else it.omniumBal,
                            souliumBal  = if (item.currency == "soulium") r.newBalance else it.souliumBal
                        )
                    }
                    logPurchaseAnalytics(item)
                }
                .onFailure { e -> _state.update { it.copy(purchasing = null, error = e.message) } }
        }
    }

    fun clearSuccess() { _state.update { it.copy(successMsg = null) } }

    private fun logPurchaseAnalytics(item: MarketItemDto) {
        runCatching {
            val bundle = Bundle().apply {
                putString("item_id", item.id)
                putString("currency", item.currency)
                putLong("price", item.price)
            }
            FirebaseAnalytics.getInstance(appCtx).logEvent(FirebaseAnalytics.Event.PURCHASE, bundle)
        }
    }

    private fun fallbackItems(tab: MarketTab): List<MarketItemDto> = when (tab) {
        MarketTab.Boosts -> listOf(
            MarketItemDto("boost_hp","HP Boost","HP Boost","Anında HP yenileme","Instant HP restore","boosts",150,"soulium",null,false,false,false,null),
            MarketItemDto("boost_stamina","Stamina Boost","Stamina Boost","Sonsuz koşma","Infinite sprint","boosts",200,"soulium",null,false,false,false,null)
        )
        MarketTab.Vip -> listOf(
            MarketItemDto("vip_month","VIP Ay","VIP Month","30 Günlük VIP","30-Day VIP","vip",29_900,"tl",null,false,false,true,null)
        )
        else -> emptyList()
    }

    private fun fallbackDaily(): List<MarketItemDto> = listOf(
        MarketItemDto("daily_1","Günlük Paket","Daily Pack","Özel günlük fırsat","Special daily deal","daily",99,"soulium",null,false,false,true,null)
    )
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
            _state.update { it.copy(isLoading = true) }
            val local = assetManager.loadStory().chapters.map { assetManager.storyChapterToDto(it) }
            if (local.isNotEmpty()) _state.update { it.copy(isLoading = false, chapters = local) }
            runCatching {
                val snapshot = FirebaseFirestore.getInstance().collection("story_chapters").get().await()
                val remote = snapshot.documents.mapNotNull { doc ->
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
                if (remote.isNotEmpty()) _state.update { it.copy(isLoading = false, chapters = remote) }
                else runCatching { api.getStoryChapters() }.onSuccess { r ->
                    if (r.isNotEmpty()) _state.update { it.copy(isLoading = false, chapters = r) }
                }
            }.onFailure { if (_state.value.chapters.isEmpty()) _state.update { it.copy(isLoading = false) } }
        }
    }

    fun select(ch: StoryChapterDto) { if (!ch.isUnlocked) return; _state.update { it.copy(selected = ch, readingMode = true) } }
    fun exitReading() { _state.update { it.copy(readingMode = false, selected = null) } }
}

@HiltViewModel
class GameVM @Inject constructor(
    private val bridge      : NativeBridge,
    private val assetManager: AssetManager,
    private val settings    : SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private var physicsJob : Job? = null
    private var entityJob  : Job? = null
    private var scoreJob   : Job? = null
    private var lastTickMs = 0L
    private var elapsedMs  = 0L
    private var score      = 0L
    private var kills      = 0

    /** Segments for the currently loaded level; kept here (not just in GameState) so the
     *  entity spawner can reuse them without depending on StateFlow emission timing. */
    private var segments: List<LevelSegment> = emptyList()

    fun startGame(difficulty: String = "normal", seed: Long = System.currentTimeMillis(), mapId: String = "level_0") {
        viewModelScope.launch {
            val sensitivity = settings.observe().first().cameraSensitivity
            bridge.initCore(seed)
            bridge.initSound()
            bridge.initEntities()
            bridge.setAmbienceLevel(0.4f)
            bridge.setHumVolume(0.3f)
            bridge.setSpatialRolloff(1f, 40f)

            val nodeCount = if (difficulty == "hard") 60 else 40
            val levelDepth = mapId.substringAfterLast('_').toIntOrNull() ?: 0
            segments = LevelSegment.listFromFloatArray(bridge.generateLevel(nodeCount, depth = levelDepth))
            val exit = segments.lastOrNull()
            val exitX = exit?.endX ?: 0f
            val exitZ = exit?.endZ ?: 0f

            val cfg = assetManager.getSpawnConfig(difficulty)
            if (segments.isNotEmpty()) {
                repeat(cfg.count) { i ->
                    val entity = EntityType.entries[i % EntityType.entries.size]
                    // Skip the first couple of segments so the player isn't ambushed on spawn.
                    val seg = segments[(2 + (Math.random() * (segments.size - 2).coerceAtLeast(1))).toInt().coerceIn(0, segments.lastIndex)]
                    val lateral = (Math.random().toFloat() * 2f - 1f) * (seg.width * 0.35f)
                    val (sx, sz) = seg.pointAt(Math.random().toFloat(), lateral)
                    bridge.spawnEntity(
                        x = sx, y = 0f, z = sz,
                        speed = entity.baseSpeed * cfg.speedMult,
                        hear  = entity.hearRange,
                        sight = entity.sightRange * cfg.sightMult,
                        aggro = entity.aggroRange, typeId = entity.nativeAiId
                    )
                }
            }
            _state.value = GameState(
                seed = seed, difficulty = difficulty, mapId = mapId,
                levelSegments = segments, exitX = exitX, exitZ = exitZ
            )
            startPhysicsLoop(sensitivity)
            startEntitySpawner(difficulty, cfg)
            startScoreAccumulator()
        }
    }

    private fun startPhysicsLoop(sensitivity: Float) {
        lastTickMs = bridge.nowMs()
        physicsJob = viewModelScope.launch {
            while (isActive) {
                if (_state.value.isPaused) { delay(16); continue }
                val now = bridge.nowMs()
                val dt  = ((now - lastTickMs).coerceIn(1, 100)).toFloat() / 1000f
                lastTickMs = now; elapsedMs += (dt * 1000).toLong()
                bridge.physicsTick(dt)
                val cam = CameraSnapshot.fromFloatArray(bridge.getCameraState())
                if (cam != null) bridge.setListenerPos(cam.posX, cam.posY, cam.posZ)
                val entityList        = EntityState.listFromFloatArray(
                    bridge.tickEntities(cam?.posX ?: 0f, cam?.posY ?: 0f, cam?.posZ ?: 0f, dt)
                )
                val flickerInfluence  = bridge.getTotalFlickerInfluence()
                val nearbyCount = entityList.count { e ->
                    if (!e.isActive || cam == null) return@count false
                    val dx = e.posX - cam.posX; val dz = e.posZ - cam.posZ
                    dx * dx + dz * dz < 625f // within 25 units
                }
                // Entities in an Attack state (aiState 4) that reach melee range hurt the
                // player. HP was previously never touched anywhere, so GameOverOverlay was
                // unreachable — this is the missing fail-state.
                var damage = 0f
                if (cam != null) {
                    for (e in entityList) {
                        if (!e.isActive || e.aiState != 4) continue
                        val dx = e.posX - cam.posX; val dz = e.posZ - cam.posZ
                        if (dx * dx + dz * dz < 2.25f) damage += 16f * dt
                    }
                }
                val s = _state.value
                val drain = (nearbyCount * 0.6f + flickerInfluence * 2f) * dt
                val regen = if (nearbyCount == 0 && flickerInfluence < 0.1f) dt * 0.3f else 0f
                val nb    = (s.flashlightBattery - (if (s.flashlightOn) dt * 0.006f else 0f)).coerceAtLeast(0f)
                val newHp = (s.playerHp - damage).coerceIn(0f, s.playerMaxHp)
                val exitDist = if (cam != null)
                    kotlin.math.hypot((s.exitX - cam.posX).toDouble(), (s.exitZ - cam.posZ).toDouble()).toFloat()
                else s.distanceToExit
                _state.update {
                    it.copy(
                        sessionElapsed    = elapsedMs,
                        flickerIntensity  = flickerInfluence,
                        entitiesNearby    = nearbyCount,
                        score             = score,
                        sanity            = (s.sanity - drain + regen).coerceIn(0f, 100f),
                        flashlightBattery = nb,
                        flashlightOn      = if (!s.flashlightOn) false else nb > 0f,
                        stamina           = (s.stamina + dt * 8f).coerceAtMost(s.staminaMax),
                        playerHp          = newHp,
                        isGameOver        = newHp <= 0f || it.isGameOver,
                        camera            = cam ?: s.camera,
                        entities          = entityList,
                        distanceToExit    = exitDist
                    )
                }
                delay(16)
            }
        }
    }

    private fun startEntitySpawner(difficulty: String, cfg: SpawnConfig) {
        entityJob = viewModelScope.launch {
            var timer = 0L
            while (isActive) {
                delay(5_000); timer += 5_000
                if (timer >= cfg.spawnIntervalMs && segments.isNotEmpty()) {
                    timer = 0
                    val entity = EntityType.entries[(Math.random() * EntityType.entries.size).toInt().coerceIn(0, EntityType.entries.lastIndex)]
                    val seg = segments[(Math.random() * segments.size).toInt().coerceIn(0, segments.lastIndex)]
                    val (sx, sz) = seg.pointAt(Math.random().toFloat(), (Math.random().toFloat() * 2f - 1f) * (seg.width * 0.35f))
                    bridge.spawnEntity(
                        x = sx, y = 0f, z = sz,
                        speed = entity.baseSpeed * cfg.speedMult,
                        hear = entity.hearRange, sight = entity.sightRange * cfg.sightMult,
                        aggro = entity.aggroRange, typeId = entity.nativeAiId
                    )
                }
            }
        }
    }

    private fun startScoreAccumulator() {
        scoreJob = viewModelScope.launch {
            while (isActive) {
                if (!_state.value.isPaused)
                    score += when (_state.value.difficulty) { "hard" -> 5L; "normal" -> 3L; else -> 1L }
                delay(1_000)
            }
        }
    }

    fun onMove(dx: Float, dy: Float, dz: Float) {
        val s    = _state.value
        val mult = if (s.stamina > 10f) 1.0f else 0.5f
        bridge.applyMovement(dx * 320f * mult, dy * 320f, dz * 320f * mult)
        if (s.stamina > 0f) _state.update { it.copy(stamina = (it.stamina - 0.3f).coerceAtLeast(0f)) }
        viewModelScope.launch { bridge.triggerFootstep(120f, 0.3f) }
    }

    fun onLook(dx: Float, dy: Float) {
        viewModelScope.launch(Dispatchers.Default) {
            val sensitivity = settings.observe().first().cameraSensitivity
            bridge.cameraLook(dx, dy, sensitivity)
        }
    }

    fun onJump()   { bridge.applyMovement(0f, 5000f, 0f) }
    fun onCrouch() { bridge.applyMovement(0f, -1000f, 0f) }
    fun toggleFlashlight() { _state.update { it.copy(flashlightOn = !it.flashlightOn) } }
    fun togglePause()      { _state.update { it.copy(isPaused = !it.isPaused) } }

    /** True once the player is close enough to the exit for [onInteract] to work; the HUD
     *  uses this to show a prompt so the player knows the exit is reachable. */
    val canEscape: Boolean get() = _state.value.distanceToExit < 3.5f

    fun onInteract() {
        if (canEscape) _state.update { it.copy(isEscaped = true) }
    }

    fun onDamageEntity(id: Int) {
        bridge.damageEntity(id, 25f); kills++; score += 100L
        _state.update { it.copy(kills = kills, score = score) }
    }

    override fun onCleared() {
        physicsJob?.cancel(); entityJob?.cancel(); scoreJob?.cancel()
        runBlocking { bridge.destroyEntities(); bridge.destroySound(); bridge.destroyCore() }
        super.onCleared()
    }
}

@HiltViewModel
class LeaderboardVM @Inject constructor(private val api: ApiService) : ViewModel() {
    private val _entries = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val entries: StateFlow<List<LeaderboardEntry>> = _entries.asStateFlow()
    init { viewModelScope.launch { runCatching { api.getLeaderboard() }.onSuccess { _entries.value = it.entries } } }
}

@HiltViewModel
class ProfileVM @Inject constructor(private val api: ApiService) : ViewModel() {
    private val _profile = MutableStateFlow(PlayerProfile())
    val profile: StateFlow<PlayerProfile> = _profile.asStateFlow()
    init { viewModelScope.launch { runCatching { api.getProfile() }.onSuccess { _profile.value = it } } }
}

@Composable
fun LobbyVideoBackground(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val exoPlayer = remember(ctx) {
        ExoPlayer.Builder(ctx).build().apply {
            val uri = Uri.parse("android.resource://${ctx.packageName}/raw/lobby_video")
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_ONE
            volume     = 0f
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(exoPlayer) { onDispose { exoPlayer.release() } }
    AndroidView(
        modifier = modifier,
        factory  = { context ->
            PlayerView(context).apply {
                player               = exoPlayer
                useController        = false
                resizeMode           = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                setShutterBackgroundColor(android.graphics.Color.BLACK)
            }
        }
    )
}

@Composable
fun MainMenu(
    onPlay       : () -> Unit,
    onOnline     : () -> Unit,
    onSettings   : () -> Unit,
    onStory      : () -> Unit,
    onMarket     : () -> Unit,
    onLeaderboard: () -> Unit,
    onProfile    : () -> Unit
) {
    val inf    = rememberInfiniteTransition(label = "menu")
    val pulse  by inf.animateFloat(0.7f, 1f, infiniteRepeatable(tween(2500, easing = EaseInOut), RepeatMode.Reverse), "pulse")
    val scanY  by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(8000, easing = LinearEasing)), "scan")
    val glitch by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(60, easing = LinearEasing), RepeatMode.Reverse), "glitch")

    var titleVisible   by remember { mutableStateOf(false) }
    var buttonsVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(300); titleVisible = true
        delay(600); buttonsVisible = true
    }

    Box(Modifier.fillMaxSize()) {
        LobbyVideoBackground(Modifier.fillMaxSize())

        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(0.55f), DarkBg.copy(0.85f), Color.Black.copy(0.92f))
                )
            )
        )

        CrtScanlineOverlay(scanY)

        Column(
            Modifier
                .align(Alignment.Center)
                .width(300.dp)
                .padding(horizontal = 16.dp),
            verticalArrangement    = Arrangement.spacedBy(10.dp),
            horizontalAlignment    = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(
                visible = titleVisible,
                enter   = fadeIn(tween(800)) + slideInVertically(tween(800, easing = EaseOutBack)) { -80 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    GlitchText(
                        text      = "OMNI",
                        fontSize  = 52.sp,
                        color     = Yellow.copy(pulse),
                        glitchVal = glitch
                    )
                    Text(
                        "BACKROOMS",
                        color        = Yellow,
                        fontSize     = 28.sp,
                        fontWeight   = FontWeight.Black,
                        letterSpacing = 12.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "E A Q U E L  S T U D I O S",
                        color        = TextDim,
                        fontSize     = 9.sp,
                        letterSpacing = 5.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    FlickerDivider()
                }
            }

            AnimatedVisibility(
                visible = buttonsVisible,
                enter   = fadeIn(tween(1000)) + slideInVertically(tween(1000, easing = EaseOutCubic)) { 60 }
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AtmosphericButton(
                        label    = stringResource(R.string.menu_play_offline),
                        icon     = Icons.Default.PlayArrow,
                        accent   = Yellow,
                        width    = 280.dp,
                        height   = 58.dp,
                        isPrimary = true,
                        onClick  = onPlay
                    )
                    AtmosphericButton(
                        label  = stringResource(R.string.menu_play_online),
                        icon   = Icons.Default.Public,
                        accent = OmniumCol,
                        width  = 280.dp,
                        height = 52.dp,
                        onClick = onOnline
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AtmosphericButton(
                            label  = stringResource(R.string.market_title),
                            icon   = Icons.Default.Store,
                            accent = SouliumCol,
                            width  = 134.dp,
                            height = 46.dp,
                            onClick = onMarket,
                            modifier = Modifier.weight(1f)
                        )
                        AtmosphericButton(
                            label  = stringResource(R.string.story_title),
                            icon   = Icons.AutoMirrored.Filled.MenuBook,
                            accent = CrtAmber,
                            width  = 134.dp,
                            height = 46.dp,
                            onClick = onStory,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AtmosphericButton(
                            label  = stringResource(R.string.menu_leaderboard),
                            icon   = Icons.Default.Leaderboard,
                            accent = SuccessGreen,
                            width  = 134.dp,
                            height = 44.dp,
                            onClick = onLeaderboard,
                            modifier = Modifier.weight(1f)
                        )
                        AtmosphericButton(
                            label  = stringResource(R.string.menu_profile),
                            icon   = Icons.Default.AccountCircle,
                            accent = TextSec,
                            width  = 134.dp,
                            height = 44.dp,
                            onClick = onProfile,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    AtmosphericButton(
                        label  = stringResource(R.string.menu_settings),
                        icon   = Icons.Default.Settings,
                        accent = BorderCol,
                        width  = 280.dp,
                        height = 40.dp,
                        onClick = onSettings
                    )
                }
            }
        }

        NoiseScanlineBottom()
    }
}

@Composable
private fun GlitchText(
    text     : String,
    fontSize : androidx.compose.ui.unit.TextUnit,
    color    : Color,
    glitchVal: Float
) {
    val glitchOffset = remember(glitchVal) {
        if (glitchVal > 0.92f) (Math.random() * 6 - 3).toFloat() else 0f
    }
    Box {
        if (glitchOffset != 0f) {
            Text(
                text,
                color        = OmniumCol.copy(0.35f),
                fontSize     = fontSize,
                fontWeight   = FontWeight.Black,
                letterSpacing = 6.sp,
                modifier     = Modifier.offset(x = glitchOffset.dp, y = 1.dp)
            )
            Text(
                text,
                color        = DangerRed.copy(0.35f),
                fontSize     = fontSize,
                fontWeight   = FontWeight.Black,
                letterSpacing = 6.sp,
                modifier     = Modifier.offset(x = (-glitchOffset).dp, y = (-1).dp)
            )
        }
        Text(
            text,
            color        = color,
            fontSize     = fontSize,
            fontWeight   = FontWeight.Black,
            letterSpacing = 6.sp
        )
    }
}

@Composable
private fun AtmosphericButton(
    label   : String,
    icon    : ImageVector,
    accent  : Color,
    width   : Dp,
    height  : Dp,
    onClick : () -> Unit,
    isPrimary: Boolean = false,
    modifier: Modifier = Modifier,
    enabled : Boolean  = true
) {
    val haptic       = LocalHapticFeedback.current
    val interSrc     = remember { MutableInteractionSource() }
    val isPressed    by interSrc.collectIsPressedAsState()
    val scaleAnim    by animateFloatAsState(if (isPressed) 0.96f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "btn_scale")
    val glowAlpha    by animateFloatAsState(if (isPressed) 0.9f else if (isPrimary) 0.6f else 0.4f, tween(150), label = "btn_glow")

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .width(width)
            .height(height)
            .scale(scaleAnim)
            .alpha(if (enabled) 1f else 0.38f)
            .clip(RoundedCornerShape(4.dp))
            .background(
                Brush.verticalGradient(
                    listOf(accent.copy(if (isPrimary) 0.28f else 0.18f), accent.copy(0.06f))
                )
            )
            .border(
                width  = if (isPrimary) 1.5f.dp else 1.dp,
                brush  = Brush.verticalGradient(listOf(accent.copy(glowAlpha), accent.copy(glowAlpha * 0.4f))),
                shape  = RoundedCornerShape(4.dp)
            )
            .drawWithContent {
                drawContent()
                if (isPrimary) {
                    drawRect(
                        Brush.horizontalGradient(listOf(accent.copy(0f), accent.copy(0.12f), accent.copy(0f))),
                        topLeft = Offset(0f, size.height - 2.dp.toPx()),
                        size    = Size(size.width, 2.dp.toPx())
                    )
                }
            }
            .clickable(
                interactionSource = interSrc,
                indication        = null,
                enabled           = enabled
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = accent.copy(if (enabled) 1f else 0.38f), modifier = Modifier.size(if (isPrimary) 20.dp else 16.dp))
            Text(
                label,
                color        = accent.copy(if (enabled) 1f else 0.38f),
                fontSize     = if (isPrimary) 14.sp else 11.sp,
                fontWeight   = FontWeight.Bold,
                letterSpacing = if (isPrimary) 3.sp else 2.sp
            )
        }
    }
}

@Composable
private fun FlickerDivider() {
    val inf  = rememberInfiniteTransition(label = "div")
    val flkr by inf.animateFloat(0.3f, 1f, infiniteRepeatable(tween(180, easing = LinearEasing), RepeatMode.Reverse), "f")
    Canvas(Modifier.fillMaxWidth().height(1.dp)) {
        drawLine(
            brush       = Brush.horizontalGradient(listOf(Color.Transparent, Yellow.copy(flkr), Color.Transparent)),
            start       = Offset(0f, size.height / 2),
            end         = Offset(size.width, size.height / 2),
            strokeWidth = 1.5f
        )
    }
}

@Composable
private fun CrtScanlineOverlay(scanProgress: Float) {
    Canvas(Modifier.fillMaxSize()) {
        val lineH = 2.dp.toPx()
        val gap   = 4.dp.toPx()
        var y = 0f
        while (y < size.height) {
            drawRect(Color.Black.copy(0.12f), Offset(0f, y), Size(size.width, lineH))
            y += lineH + gap
        }
        val stripH = size.height * 0.03f
        val stripY = size.height * scanProgress
        drawRect(Color.White.copy(0.025f), Offset(0f, stripY), Size(size.width, stripH))
    }
}

// ============================================================================
// 3D renderer. Runs on its own GL thread; reads a volatile snapshot of
// GameState + graphics settings written from the Compose side each frame, and
// never ticks simulation itself (physics/AI stay solely in GameVM so nothing
// gets double-advanced). No new files: lives in this file by request.
// ============================================================================

private fun compileGlShader(type: Int, src: String): Int {
    val shader = GLES30.glCreateShader(type)
    GLES30.glShaderSource(shader, src)
    GLES30.glCompileShader(shader)
    val status = IntArray(1)
    GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0)
    if (status[0] == 0) {
        val log = GLES30.glGetShaderInfoLog(shader)
        GLES30.glDeleteShader(shader)
        throw RuntimeException("Omni shader compile failed: $log")
    }
    return shader
}

private fun linkGlProgram(vertSrc: String, fragSrc: String): Int {
    val vs = compileGlShader(GLES30.GL_VERTEX_SHADER, vertSrc)
    val fs = compileGlShader(GLES30.GL_FRAGMENT_SHADER, fragSrc)
    val prog = GLES30.glCreateProgram()
    GLES30.glAttachShader(prog, vs); GLES30.glAttachShader(prog, fs)
    GLES30.glLinkProgram(prog)
    val status = IntArray(1)
    GLES30.glGetProgramiv(prog, GLES30.GL_LINK_STATUS, status, 0)
    GLES30.glDeleteShader(vs); GLES30.glDeleteShader(fs)
    if (status[0] == 0) {
        val log = GLES30.glGetProgramInfoLog(prog)
        GLES30.glDeleteProgram(prog)
        throw RuntimeException("Omni program link failed: $log")
    }
    return prog
}

private fun glFloatBuffer(data: FloatArray): FloatBuffer =
    ByteBuffer.allocateDirect(data.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(data); position(0) }

private fun glIntBuffer(data: IntArray): IntBuffer =
    ByteBuffer.allocateDirect(data.size * 4).order(ByteOrder.nativeOrder()).asIntBuffer().apply { put(data); position(0) }

private const val OMNI_SCENE_VERT = """#version 300 es
layout(location=0) in vec3 aPos;
layout(location=1) in vec3 aNormal;
layout(location=2) in vec2 aUV;
layout(location=3) in float aLight;
uniform mat4 uMVP;
out vec3 vNormal; out vec2 vUV; out float vLight; out vec3 vWorldPos;
void main(){
    vWorldPos = aPos; vNormal = aNormal; vUV = aUV; vLight = aLight;
    gl_Position = uMVP * vec4(aPos, 1.0);
}
"""

private const val OMNI_SCENE_FRAG = """#version 300 es
precision mediump float;
in vec3 vNormal; in vec2 vUV; in float vLight; in vec3 vWorldPos;
uniform sampler2D uTex;
uniform vec3 uCamPos; uniform vec3 uFlashDir; uniform float uFlashOn;
uniform float uFogDensity; uniform vec3 uFogColor; uniform float uFlicker;
out vec4 fragColor;
void main(){
    vec4 tex = texture(uTex, vUV);
    vec3 n = normalize(vNormal);
    float overhead = clamp(dot(n, vec3(0.0,1.0,0.0)), 0.0, 1.0) * vLight * uFlicker;
    vec3 toCam = uCamPos - vWorldPos;
    float dist = length(toCam);
    vec3 toCamN = toCam / max(dist, 0.001);
    float flash = 0.0;
    if (uFlashOn > 0.5) {
        vec3 fd = normalize(uFlashDir);
        float spotCos = dot(-toCamN, fd);
        float coneMask = smoothstep(0.80, 0.97, spotCos);
        float diffuse = max(dot(n, toCamN), 0.0);
        float atten = clamp(1.0 - dist/20.0, 0.0, 1.0);
        flash = coneMask * diffuse * atten * 1.6;
    }
    float lit = 0.08 + overhead*0.95 + flash;
    vec3 col = tex.rgb * lit;
    float fog = 1.0 - exp(-uFogDensity * dist * dist * 0.008);
    col = mix(col, uFogColor, clamp(fog, 0.0, 1.0));
    fragColor = vec4(col, 1.0);
}
"""

private const val OMNI_BILLBOARD_VERT = """#version 300 es
layout(location=0) in vec2 aCorner;
uniform mat4 uVP; uniform vec3 uCenter; uniform vec3 uCamRight; uniform vec3 uCamUp; uniform float uSize;
out vec2 vUV;
void main(){
    vec3 worldPos = uCenter + uCamRight*(aCorner.x*uSize) + uCamUp*(aCorner.y*uSize*1.6);
    vUV = aCorner*0.5 + 0.5;
    gl_Position = uVP * vec4(worldPos, 1.0);
}
"""

private const val OMNI_BILLBOARD_FRAG = """#version 300 es
precision mediump float;
in vec2 vUV;
uniform vec3 uColor; uniform float uAlert; uniform float uAlpha;
out vec4 fragColor;
void main(){
    vec2 d = (vUV - vec2(0.5, 0.42)) * vec2(1.0, 1.35);
    float body = smoothstep(0.5, 0.28, length(d));
    vec2 eyeD = (vUV - vec2(0.5, 0.62)) * vec2(1.0, 1.6);
    float core = smoothstep(0.16, 0.02, length(eyeD));
    vec3 eyeColor = mix(vec3(0.85,0.78,0.25), vec3(1.0,0.05,0.05), uAlert);
    vec3 col = mix(uColor*0.12, eyeColor, core);
    float alpha = body*uAlpha;
    if (alpha < 0.02) discard;
    fragColor = vec4(col, alpha);
}
"""

private const val OMNI_POST_VERT = """#version 300 es
layout(location=0) in vec2 aPos;
out vec2 vUV;
void main(){ vUV = aPos*0.5+0.5; gl_Position = vec4(aPos, 0.0, 1.0); }
"""

private const val OMNI_POST_FRAG = """#version 300 es
precision mediump float;
in vec2 vUV;
uniform sampler2D uScene;
uniform float uTime; uniform float uFlicker; uniform float uVhsStrength; uniform vec2 uResolution;
out vec4 fragColor;
float rand(vec2 co){ return fract(sin(dot(co, vec2(12.9898,78.233))) * 43758.5453); }
void main(){
    vec2 uv = vUV;
    float shift = (rand(vec2(uTime*0.6, uv.y*40.0)) - 0.5) * 0.004 * uVhsStrength;
    float r = texture(uScene, uv + vec2(shift, 0.0)).r;
    float g = texture(uScene, uv).g;
    float b = texture(uScene, uv - vec2(shift, 0.0)).b;
    vec3 col = vec3(r,g,b);
    float scan = sin(uv.y*uResolution.y*1.4 + uTime*6.0) * 0.04 * uVhsStrength;
    col -= scan;
    float grain = (rand(uv*uResolution + uTime) - 0.5) * 0.05 * uVhsStrength;
    col += grain;
    vec2 vig = uv - 0.5;
    float vigAmt = 1.0 - dot(vig,vig)*1.1;
    col *= clamp(vigAmt, 0.0, 1.0);
    col *= (0.55 + 0.45*uFlicker);
    fragColor = vec4(col, 1.0);
}
"""

/** Graphics options snapshot pushed in from SettingsRepository; kept separate from
 *  GameState since it comes from a different source. */
data class RenderSettings(
    val quality        : String  = "high",
    val vhsEnabled     : Boolean = true,
    val fogEnabled     : Boolean = true,
    val shadowsEnabled : Boolean = true,
    val resolutionScale: Float   = 1f,
    val colorBlindMode : String  = "none"
)

class OmniGLRenderer(private val appContext: Context) : GLSurfaceView.Renderer {

    @Volatile var latestState: GameState = GameState()
    @Volatile var renderSettings: RenderSettings = RenderSettings()

    private var sceneProgram = 0; private var billboardProgram = 0; private var postProgram = 0
    private var uMVP = 0; private var uTex = 0; private var uCamPos = 0
    private var uFlashDir = 0; private var uFlashOn = 0; private var uFogDensity = 0
    private var uFogColor = 0; private var uFlicker = 0
    private var bVP = 0; private var bCenter = 0; private var bRight = 0; private var bUp = 0
    private var bSize = 0; private var bColor = 0; private var bAlert = 0; private var bAlpha = 0
    private var pScene = 0; private var pTime = 0; private var pFlicker = 0; private var pVhs = 0; private var pRes = 0

    private var floorTex = 0; private var wallTex = 0; private var roofTex = 0
    private var floorVbo = 0; private var floorIbo = 0; private var floorCount = 0
    private var wallVbo  = 0; private var wallIbo  = 0; private var wallCount  = 0
    private var roofVbo  = 0; private var roofIbo  = 0; private var roofCount = 0
    private var lastSegKey = Int.MIN_VALUE

    private var billboardVbo = 0
    private var postVbo = 0

    private var fbo = 0; private var fboTex = 0; private var fboDepth = 0
    private var surfaceW = 1; private var surfaceH = 1

    private val projM = FloatArray(16)
    private val viewM = FloatArray(16)
    private val vpM   = FloatArray(16)
    private val startNanos = System.nanoTime()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0.02f, 0.02f, 0.017f, 1f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        sceneProgram = linkGlProgram(OMNI_SCENE_VERT, OMNI_SCENE_FRAG)
        uMVP = GLES30.glGetUniformLocation(sceneProgram, "uMVP")
        uTex = GLES30.glGetUniformLocation(sceneProgram, "uTex")
        uCamPos = GLES30.glGetUniformLocation(sceneProgram, "uCamPos")
        uFlashDir = GLES30.glGetUniformLocation(sceneProgram, "uFlashDir")
        uFlashOn = GLES30.glGetUniformLocation(sceneProgram, "uFlashOn")
        uFogDensity = GLES30.glGetUniformLocation(sceneProgram, "uFogDensity")
        uFogColor = GLES30.glGetUniformLocation(sceneProgram, "uFogColor")
        uFlicker = GLES30.glGetUniformLocation(sceneProgram, "uFlicker")

        billboardProgram = linkGlProgram(OMNI_BILLBOARD_VERT, OMNI_BILLBOARD_FRAG)
        bVP = GLES30.glGetUniformLocation(billboardProgram, "uVP")
        bCenter = GLES30.glGetUniformLocation(billboardProgram, "uCenter")
        bRight = GLES30.glGetUniformLocation(billboardProgram, "uCamRight")
        bUp = GLES30.glGetUniformLocation(billboardProgram, "uCamUp")
        bSize = GLES30.glGetUniformLocation(billboardProgram, "uSize")
        bColor = GLES30.glGetUniformLocation(billboardProgram, "uColor")
        bAlert = GLES30.glGetUniformLocation(billboardProgram, "uAlert")
        bAlpha = GLES30.glGetUniformLocation(billboardProgram, "uAlpha")

        postProgram = linkGlProgram(OMNI_POST_VERT, OMNI_POST_FRAG)
        pScene = GLES30.glGetUniformLocation(postProgram, "uScene")
        pTime = GLES30.glGetUniformLocation(postProgram, "uTime")
        pFlicker = GLES30.glGetUniformLocation(postProgram, "uFlicker")
        pVhs = GLES30.glGetUniformLocation(postProgram, "uVhsStrength")
        pRes = GLES30.glGetUniformLocation(postProgram, "uResolution")

        floorTex = loadOmniTexture("Level_0/Floor.png", 0xFF3A3020.toInt())
        wallTex  = loadOmniTexture("Level_0/Wall.png",  0xFF4A4030.toInt())
        roofTex  = loadOmniTexture("Level_0/Roof.png",  0xFF23210F.toInt())

        val quadCorners = floatArrayOf(-1f,-1f, 1f,-1f, -1f,1f, 1f,1f)
        billboardVbo = genGlBuffer()
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, billboardVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, quadCorners.size*4, glFloatBuffer(quadCorners), GLES30.GL_STATIC_DRAW)

        postVbo = genGlBuffer()
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, postVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, quadCorners.size*4, glFloatBuffer(quadCorners), GLES30.GL_STATIC_DRAW)

        floorVbo = genGlBuffer(); floorIbo = genGlBuffer()
        wallVbo  = genGlBuffer(); wallIbo  = genGlBuffer()
        roofVbo  = genGlBuffer(); roofIbo  = genGlBuffer()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        surfaceW = max(width, 1); surfaceH = max(height, 1)
        GLES30.glViewport(0, 0, surfaceW, surfaceH)
        Matrix.perspectiveM(projM, 0, 70f, surfaceW.toFloat()/surfaceH.toFloat(), 0.05f, 55f)
        rebuildFbo(surfaceW, surfaceH)
    }

    override fun onDrawFrame(gl: GL10?) {
        val state = latestState
        val cam = state.camera
        val rs = renderSettings
        val timeSec = (System.nanoTime() - startNanos) / 1_000_000_000f

        val segKey = state.levelSegments.size * 73856093 xor (state.levelSegments.firstOrNull()?.posX?.hashCode() ?: 0)
        if (state.levelSegments.isNotEmpty() && segKey != lastSegKey) {
            uploadLevelMesh(state.levelSegments)
            lastSegKey = segKey
        }

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbo)
        GLES30.glViewport(0, 0, surfaceW, surfaceH)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        if (cam != null) {
            val yawRad = Math.toRadians(cam.yaw.toDouble())
            val pitchRad = Math.toRadians(cam.pitch.toDouble())
            val fx = (sin(yawRad) * cos(pitchRad)).toFloat()
            val fy = sin(pitchRad).toFloat()
            val fz = (cos(yawRad) * cos(pitchRad)).toFloat()
            Matrix.setLookAtM(viewM, 0, cam.posX, cam.posY, cam.posZ, cam.posX+fx, cam.posY+fy, cam.posZ+fz, 0f, 1f, 0f)
            Matrix.multiplyMM(vpM, 0, projM, 0, viewM, 0)

            val fogDensity = if (rs.fogEnabled) 1.0f else 0.15f
            val flicker = state.flickerIntensity.coerceIn(0.35f, 1f)
            drawLevel(vpM, cam.posX, cam.posY, cam.posZ, fx, fy, fz, state.flashlightOn, fogDensity, flicker)
            drawEntities(vpM, state.entities, yawRad.toFloat(), cam)
        }

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, surfaceW, surfaceH)
        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        drawPost(timeSec, state.flickerIntensity, if (rs.vhsEnabled) 1f else 0f)
    }

    private fun drawLevel(vp: FloatArray, camX: Float, camY: Float, camZ: Float, fx: Float, fy: Float, fz: Float, flashOn: Boolean, fogDensity: Float, flicker: Float) {
        GLES30.glUseProgram(sceneProgram)
        GLES30.glUniformMatrix4fv(uMVP, 1, false, vp, 0)
        GLES30.glUniform3f(uCamPos, camX, camY, camZ)
        GLES30.glUniform3f(uFlashDir, fx, fy, fz)
        GLES30.glUniform1f(uFlashOn, if (flashOn) 1f else 0f)
        GLES30.glUniform1f(uFogDensity, fogDensity)
        GLES30.glUniform3f(uFogColor, 0.05f, 0.045f, 0.03f)
        GLES30.glUniform1f(uFlicker, flicker)
        drawMeshGroup(floorVbo, floorIbo, floorCount, floorTex)
        drawMeshGroup(roofVbo,  roofIbo,  roofCount,  roofTex)
        drawMeshGroup(wallVbo,  wallIbo,  wallCount,  wallTex)
    }

    private fun drawMeshGroup(vbo: Int, ibo: Int, indexCount: Int, tex: Int) {
        if (indexCount <= 0) return
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex)
        GLES30.glUniform1i(uTex, 0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo)
        val stride = 9 * 4
        GLES30.glEnableVertexAttribArray(0); GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
        GLES30.glEnableVertexAttribArray(1); GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride, 3*4)
        GLES30.glEnableVertexAttribArray(2); GLES30.glVertexAttribPointer(2, 2, GLES30.GL_FLOAT, false, stride, 6*4)
        GLES30.glEnableVertexAttribArray(3); GLES30.glVertexAttribPointer(3, 1, GLES30.GL_FLOAT, false, stride, 8*4)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, ibo)
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, indexCount, GLES30.GL_UNSIGNED_INT, 0)
        GLES30.glDisableVertexAttribArray(0); GLES30.glDisableVertexAttribArray(1)
        GLES30.glDisableVertexAttribArray(2); GLES30.glDisableVertexAttribArray(3)
    }

    private fun drawEntities(vp: FloatArray, entities: List<EntityState>, yawRad: Float, cam: CameraSnapshot) {
        if (entities.isEmpty()) return
        GLES30.glUseProgram(billboardProgram)
        GLES30.glUniformMatrix4fv(bVP, 1, false, vp, 0)
        GLES30.glUniform3f(bRight, cos(yawRad), 0f, -sin(yawRad))
        GLES30.glUniform3f(bUp, 0f, 1f, 0f)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, billboardVbo)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 0, 0)
        for (e in entities) {
            if (!e.isActive) continue
            val dx = e.posX - cam.posX; val dz = e.posZ - cam.posZ
            if (dx*dx + dz*dz > 2025f) continue // beyond 45 units, skip
            GLES30.glUniform3f(bCenter, e.posX, e.posY + 1.0f, e.posZ)
            GLES30.glUniform1f(bSize, 1.8f)
            val tint = entityTint(e.typeId)
            GLES30.glUniform3f(bColor, tint.first, tint.second, tint.third)
            GLES30.glUniform1f(bAlert, (e.alertLevel + (if (e.aiState >= 3) 0.5f else 0f)).coerceIn(0f, 1f))
            GLES30.glUniform1f(bAlpha, if (e.playerInSight) 1f else 0.82f)
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        }
        GLES30.glDisableVertexAttribArray(0)
    }

    private fun drawPost(timeSec: Float, flicker: Float, vhsStrength: Float) {
        GLES30.glUseProgram(postProgram)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, fboTex)
        GLES30.glUniform1i(pScene, 0)
        GLES30.glUniform1f(pTime, timeSec)
        GLES30.glUniform1f(pFlicker, flicker.coerceIn(0.3f, 1f))
        GLES30.glUniform1f(pVhs, vhsStrength)
        GLES30.glUniform2f(pRes, surfaceW.toFloat(), surfaceH.toFloat())
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, postVbo)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 0, 0)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glDisableVertexAttribArray(0)
    }

    private fun entityTint(typeId: Int): Triple<Float, Float, Float> = when (typeId) {
        0 -> Triple(0.90f, 0.90f, 0.85f)  // Smiler
        1 -> Triple(0.55f, 0.40f, 0.30f)  // Hound
        2 -> Triple(0.80f, 0.70f, 0.30f)  // PartyGoer
        3 -> Triple(0.60f, 0.50f, 0.55f)  // Skin-Stealer
        4 -> Triple(0.50f, 0.50f, 0.45f)  // Wandering One
        5 -> Triple(0.35f, 0.30f, 0.35f)  // Deathwatch
        6 -> Triple(0.45f, 0.40f, 0.20f)  // Crawler
        else -> Triple(0.30f, 0.30f, 0.35f) // Faceling Dark
    }

    private fun uploadLevelMesh(segments: List<LevelSegment>) {
        val floorV = ArrayList<Float>(); val floorI = ArrayList<Int>(); var floorB = 0
        val wallV  = ArrayList<Float>(); val wallI  = ArrayList<Int>(); var wallB  = 0
        val roofV  = ArrayList<Float>(); val roofI  = ArrayList<Int>(); var roofB  = 0
        val uvScale = 0.5f

        fun quad(
            verts: ArrayList<Float>, idx: ArrayList<Int>, base: Int,
            p0: FloatArray, p1: FloatArray, p2: FloatArray, p3: FloatArray,
            n: FloatArray, light: Float, uTile: Float, vTile: Float
        ): Int {
            val pts = arrayOf(p0, p1, p2, p3)
            val uvs = floatArrayOf(0f,0f, uTile,0f, uTile,vTile, 0f,vTile)
            for (k in 0 until 4) {
                verts.add(pts[k][0]); verts.add(pts[k][1]); verts.add(pts[k][2])
                verts.add(n[0]); verts.add(n[1]); verts.add(n[2])
                verts.add(uvs[k*2]); verts.add(uvs[k*2+1])
                verts.add(light)
            }
            idx.add(base); idx.add(base+1); idx.add(base+2)
            idx.add(base); idx.add(base+2); idx.add(base+3)
            return base + 4
        }

        for (seg in segments) {
            val s = sin(seg.heading); val c = cos(seg.heading)
            val rx = c; val rz = -s
            val fxv = s; val fzv = c
            val hw = seg.width * 0.5f
            val light = seg.lightIntensity * (if (seg.lightBroken) 0.3f else 1f)
            val uTile = seg.length * uvScale; val vTile = seg.width * uvScale
            val hTile = seg.height * uvScale

            val p00 = floatArrayOf(seg.posX-rx*hw,        0f,          seg.posY-rz*hw)
            val p10 = floatArrayOf(seg.posX+rx*hw,        0f,          seg.posY+rz*hw)
            val p11 = floatArrayOf(seg.posX+rx*hw+fxv*seg.length, 0f,  seg.posY+rz*hw+fzv*seg.length)
            val p01 = floatArrayOf(seg.posX-rx*hw+fxv*seg.length, 0f,  seg.posY-rz*hw+fzv*seg.length)
            floorB = quad(floorV, floorI, floorB, p00, p10, p11, p01, floatArrayOf(0f,1f,0f), light, vTile, uTile)

            val q00 = floatArrayOf(seg.posX-rx*hw,        seg.height, seg.posY-rz*hw)
            val q01 = floatArrayOf(seg.posX-rx*hw+fxv*seg.length, seg.height, seg.posY-rz*hw+fzv*seg.length)
            val q11 = floatArrayOf(seg.posX+rx*hw+fxv*seg.length, seg.height, seg.posY+rz*hw+fzv*seg.length)
            val q10 = floatArrayOf(seg.posX+rx*hw,        seg.height, seg.posY+rz*hw)
            roofB = quad(roofV, roofI, roofB, q00, q01, q11, q10, floatArrayOf(0f,-1f,0f), light*1.3f, vTile, uTile)

            val l0 = floatArrayOf(seg.posX-rx*hw, 0f, seg.posY-rz*hw)
            val l1 = floatArrayOf(seg.posX-rx*hw+fxv*seg.length, 0f, seg.posY-rz*hw+fzv*seg.length)
            val l2 = floatArrayOf(seg.posX-rx*hw+fxv*seg.length, seg.height, seg.posY-rz*hw+fzv*seg.length)
            val l3 = floatArrayOf(seg.posX-rx*hw, seg.height, seg.posY-rz*hw)
            wallB = quad(wallV, wallI, wallB, l0, l1, l2, l3, floatArrayOf(rx,0f,rz), light*0.8f, uTile, hTile)

            val r0 = floatArrayOf(seg.posX+rx*hw+fxv*seg.length, 0f, seg.posY+rz*hw+fzv*seg.length)
            val r1 = floatArrayOf(seg.posX+rx*hw, 0f, seg.posY+rz*hw)
            val r2 = floatArrayOf(seg.posX+rx*hw, seg.height, seg.posY+rz*hw)
            val r3 = floatArrayOf(seg.posX+rx*hw+fxv*seg.length, seg.height, seg.posY+rz*hw+fzv*seg.length)
            wallB = quad(wallV, wallI, wallB, r0, r1, r2, r3, floatArrayOf(-rx,0f,-rz), light*0.8f, uTile, hTile)
        }

        uploadMeshBuffers(floorVbo, floorIbo, floorV, floorI).also { floorCount = it }
        uploadMeshBuffers(roofVbo,  roofIbo,  roofV,  roofI ).also { roofCount  = it }
        uploadMeshBuffers(wallVbo,  wallIbo,  wallV,  wallI ).also { wallCount  = it }
    }

    private fun uploadMeshBuffers(vbo: Int, ibo: Int, verts: ArrayList<Float>, idx: ArrayList<Int>): Int {
        if (idx.isEmpty()) return 0
        val vArr = FloatArray(verts.size) { verts[it] }
        val iArr = IntArray(idx.size) { idx[it] }
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vArr.size*4, glFloatBuffer(vArr), GLES30.GL_DYNAMIC_DRAW)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, ibo)
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, iArr.size*4, glIntBuffer(iArr), GLES30.GL_DYNAMIC_DRAW)
        return iArr.size
    }

    private fun rebuildFbo(w: Int, h: Int) {
        if (fbo != 0) GLES30.glDeleteFramebuffers(1, intArrayOf(fbo), 0)
        if (fboTex != 0) GLES30.glDeleteTextures(1, intArrayOf(fboTex), 0)
        if (fboDepth != 0) GLES30.glDeleteRenderbuffers(1, intArrayOf(fboDepth), 0)

        fboTex = genGlTexture()
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, fboTex)
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, w, h, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

        fboDepth = genGlRenderbuffer()
        GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, fboDepth)
        GLES30.glRenderbufferStorage(GLES30.GL_RENDERBUFFER, GLES30.GL_DEPTH_COMPONENT16, w, h)

        fbo = genGlFramebuffer()
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbo)
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, fboTex, 0)
        GLES30.glFramebufferRenderbuffer(GLES30.GL_FRAMEBUFFER, GLES30.GL_DEPTH_ATTACHMENT, GLES30.GL_RENDERBUFFER, fboDepth)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
    }

    private fun genGlBuffer(): Int { val h = IntArray(1); GLES30.glGenBuffers(1, h, 0); return h[0] }
    private fun genGlTexture(): Int { val h = IntArray(1); GLES30.glGenTextures(1, h, 0); return h[0] }
    private fun genGlFramebuffer(): Int { val h = IntArray(1); GLES30.glGenFramebuffers(1, h, 0); return h[0] }
    private fun genGlRenderbuffer(): Int { val h = IntArray(1); GLES30.glGenRenderbuffers(1, h, 0); return h[0] }

    /** Loads a texture from assets, falling back to a small procedural tile so the
     *  renderer never crashes if the art asset isn't present in a given build. */
    private fun loadOmniTexture(assetPath: String, fallbackColor: Int): Int {
        val bmp: Bitmap = try {
            appContext.assets.open(assetPath).use { BitmapFactory.decodeStream(it) } ?: proceduralTile(fallbackColor)
        } catch (t: Throwable) {
            proceduralTile(fallbackColor)
        }
        val tex = genGlTexture()
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT)
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bmp, 0)
        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D)
        if (!bmp.isRecycled) bmp.recycle()
        return tex
    }

    private fun proceduralTile(baseColor: Int): Bitmap {
        val size = 64
        val pixels = IntArray(size * size)
        val rBase = (baseColor shr 16) and 0xFF
        val gBase = (baseColor shr 8) and 0xFF
        val bBase = baseColor and 0xFF
        for (y in 0 until size) for (x in 0 until size) {
            val n = (((x / 8) + (y / 8)) % 2) * 10
            val jitter = ((x * 31 + y * 17) % 13) - 6
            val r = (rBase + n + jitter).coerceIn(0, 255)
            val g = (gBase + n + jitter).coerceIn(0, 255)
            val b = (bBase + n + jitter).coerceIn(0, 255)
            pixels[y * size + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
    }
}

@Composable
private fun NoiseScanlineBottom() {
    val inf   = rememberInfiniteTransition(label = "noise")
    val noise by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(100, easing = LinearEasing), RepeatMode.Reverse), "n")
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(80.dp)
            .align(Alignment.BottomCenter)
    ) {
        drawRect(
            Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF0A0A08).copy(0.9f)))
        )
    }
}

@Composable
fun GameScreen(onExit: () -> Unit, vm: GameVM = hiltViewModel(), settingsVm: SettingsVM = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val settingsState by settingsVm.state.collectAsState()
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { vm.startGame() }

    val renderer = remember { OmniGLRenderer(ctx.applicationContext) }
    LaunchedEffect(state) { renderer.latestState = state }
    LaunchedEffect(settingsState) {
        renderer.renderSettings = RenderSettings(
            quality         = settingsState.graphicsQuality,
            vhsEnabled      = settingsState.vhsEnabled,
            fogEnabled      = settingsState.fogEnabled,
            shadowsEnabled  = settingsState.shadowsEnabled,
            resolutionScale = settingsState.resolutionScale.coerceIn(0.5f, 1f),
            colorBlindMode  = settingsState.colorBlindMode
        )
    }

    val glView = remember {
        GLSurfaceView(ctx).apply {
            setEGLContextClientVersion(3)
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> glView.onResume()
                Lifecycle.Event.ON_PAUSE  -> glView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { glView }, modifier = Modifier.fillMaxSize())
        CrtScanlineOverlay(0f)
        when {
            state.isGameOver -> GameOverOverlay(state)  { onExit() }
            state.isEscaped  -> EscapedOverlay(state)   { onExit() }
            state.isPaused   -> PauseOverlay(onResume = { vm.togglePause() }, onExit = { onExit() })
            else -> GameHud(
                gameState  = state,
                canEscape  = vm.canEscape,
                onPause    = { vm.togglePause() },
                onFlash    = { vm.toggleFlashlight() },
                onMove     = { dx, dy, dz -> vm.onMove(dx, dy, dz) },
                onLook     = { dx, dy -> vm.onLook(dx, dy) },
                onJump     = { vm.onJump() },
                onCrouch   = { vm.onCrouch() },
                onInteract = { vm.onInteract() }
            )
        }
    }
}

@Composable
fun MarketScreen(onBack: () -> Unit, vm: MarketVM = hiltViewModel()) {
    val s by vm.state.collectAsState()
    LaunchedEffect(s.successMsg) { if (s.successMsg != null) { delay(2000); vm.clearSuccess() } }
    Box(Modifier.fillMaxSize().background(DarkBg)) {
        CrtScanlineOverlay(0f)
        Column(Modifier.fillMaxSize()) {
            TopBarBack(stringResource(R.string.market_title), onBack)
            DividerLine()
            Row(
                Modifier.fillMaxWidth().background(MetalBg.copy(0.5f)).padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CurrencyBadge(s.omniumBal,  OmniumCol,  Icons.Default.Bolt)
                    CurrencyBadge(s.souliumBal, SouliumCol, Icons.Default.AutoAwesome)
                }
                if (s.isVip) VipBadge()
            }
            DividerLine()
            ScrollableTabRow(
                selectedTabIndex = MarketTab.entries.indexOf(s.tab),
                containerColor   = Color.Transparent,
                contentColor     = Yellow,
                edgePadding      = 0.dp,
                indicator        = { tabPositions ->
                    val idx = MarketTab.entries.indexOf(s.tab).coerceIn(0, tabPositions.lastIndex)
                    TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[idx]), color = Yellow)
                }
            ) {
                MarketTab.entries.forEach { tab ->
                    val sel = s.tab == tab
                    Tab(
                        selected = sel,
                        onClick  = { vm.setTab(tab) },
                        text = {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(tab.icon, null, modifier = Modifier.size(14.dp), tint = if (sel) Yellow else TextDim)
                                Text(stringResource(tab.labelRes), fontSize = 10.sp, color = if (sel) Yellow else TextDim)
                            }
                        }
                    )
                }
            }
            DividerLine()
            Box(Modifier.weight(1f)) {
                when (s.tab) {
                    MarketTab.Characters -> {
                        if (s.charsLoading) Box(Modifier.fillMaxSize(), Alignment.Center) {
                            CircularProgressIndicator(color = Yellow, strokeWidth = 2.dp)
                        } else {
                            Row(Modifier.fillMaxSize()) {
                                LazyColumn(
                                    Modifier.width(130.dp).fillMaxHeight(),
                                    contentPadding = PaddingValues(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(s.characters) { char ->
                                        val sel = s.selectedChar?.id == char.id
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxWidth().height(44.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(if (sel) Yellow.copy(0.15f) else MetalBg)
                                                .border(1.dp, if (sel) Yellow.copy(0.6f) else BorderCol, RoundedCornerShape(2.dp))
                                                .clickable { vm.selectChar(char) }
                                                .padding(horizontal = 8.dp)
                                        ) {
                                            Text(char.nameTr, color = if (sel) Yellow else TextSec, fontSize = 11.sp,
                                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
                                        }
                                    }
                                }
                                DividerLine()
                                s.selectedChar?.let { char ->
                                    Column(
                                        Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        val classColor  = when (char.clazz.uppercase()) {
                                            "SCOUT" -> SuccessGreen; "SURVIVOR" -> DangerRed
                                            "ENGINEER" -> CrtAmber; "GHOST" -> SouliumCol; else -> Yellow
                                        }
                                        val isEquipping = s.equipping == char.id
                                        CharStatBar(stringResource(R.string.char_stats_hp),      char.maxHp / 200f,      "${char.maxHp.toInt()} HP",            DangerRed)
                                        CharStatBar(stringResource(R.string.char_stats_speed),   char.baseSpeed / 6f,    "${char.baseSpeed} m/s",               SuccessGreen)
                                        CharStatBar(stringResource(R.string.char_stats_stealth), char.stealthMult / 2f,  "${(char.stealthMult * 100).toInt()}%", SouliumCol)
                                        CharStatBar(stringResource(R.string.char_stats_stamina), char.staminaMult / 2f,  "${(char.staminaMult * 100).toInt()}%", CrtAmber)
                                        DividerLine()
                                        char.abilities.forEach { ability ->
                                            Row(
                                                Modifier.fillMaxWidth().clip(RoundedCornerShape(2.dp))
                                                    .background(MetalBg)
                                                    .border(1.dp, classColor.copy(0.3f), RoundedCornerShape(2.dp))
                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment     = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Star, null, tint = classColor, modifier = Modifier.size(12.dp))
                                                Text(ability, color = TextSec, fontSize = 12.sp)
                                            }
                                        }
                                        if (char.isUnlocked) {
                                            if (char.isEquipped) {
                                                Row(Modifier.fillMaxWidth(), Arrangement.Center, Alignment.CenterVertically) {
                                                    Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(stringResource(R.string.market_equipped), color = SuccessGreen, fontSize = 12.sp)
                                                }
                                            } else {
                                                AtmosphericButton(
                                                    label   = if (isEquipping) "Takılıyor…" else stringResource(R.string.char_select_label),
                                                    icon    = Icons.Default.CheckCircle,
                                                    accent  = classColor,
                                                    width   = 200.dp,
                                                    height  = 44.dp,
                                                    enabled = !isEquipping,
                                                    onClick = { vm.equip(char) }
                                                )
                                            }
                                        } else {
                                            AtmosphericButton(
                                                label  = "${stringResource(R.string.char_unlock_prefix)}${char.price} ${char.currency.uppercase()}",
                                                icon   = Icons.Default.Lock,
                                                accent = CrtAmber,
                                                width  = 200.dp,
                                                height = 44.dp,
                                                onClick = {}
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    MarketTab.Daily -> {
                        LazyVerticalGrid(
                            GridCells.Fixed(2),
                            Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(s.dailyDeals) { item -> MarketCard(item, s.purchasing == item.id) { vm.confirmBuy(item) } }
                        }
                    }
                    else -> {
                        if (s.isLoading) Box(Modifier.fillMaxSize(), Alignment.Center) {
                            CircularProgressIndicator(color = Yellow, strokeWidth = 2.dp)
                        } else {
                            LazyVerticalGrid(
                                GridCells.Fixed(2),
                                Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(s.items) { item -> MarketCard(item, s.purchasing == item.id) { vm.confirmBuy(item) } }
                            }
                        }
                    }
                }
                s.successMsg?.let { msg ->
                    AnimatedVisibility(
                        visible = true,
                        enter   = slideInVertically { -it } + fadeIn(),
                        exit    = slideOutVertically { -it } + fadeOut(),
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp)
                    ) {
                        Box(
                            Modifier.clip(RoundedCornerShape(3.dp))
                                .background(SuccessGreen.copy(0.9f))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) { Text(msg, color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    }
                }
                s.error?.let { err ->
                    Box(
                        Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(DangerRed.copy(0.9f))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) { Text(err, color = Color.White, fontSize = 11.sp) }
                }
            }
        }
        s.confirmItem?.let { item -> PurchaseConfirmDialog(item, { vm.buy(item) }, { vm.cancelBuy() }) }
    }
}

@Composable
fun Story(onBack: () -> Unit, vm: StoryVM = hiltViewModel()) {
    val s by vm.state.collectAsState()
    AnimatedContent(
        targetState  = s.readingMode,
        transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(300)) },
        label        = "story"
    ) { reading ->
        if (reading && s.selected != null) BookReadingView(chapter = s.selected!!, onClose = vm::exitReading)
        else ChapterListView(state = s, onBack = onBack, onSelect = vm::select)
    }
}

@Composable
fun LeaderboardScreen(onBack: () -> Unit, vm: LeaderboardVM = hiltViewModel()) {
    val entries by vm.entries.collectAsState()
    Box(Modifier.fillMaxSize().background(DarkBg)) {
        CrtScanlineOverlay(0f)
        Column(Modifier.fillMaxSize()) {
            TopBarBack(stringResource(R.string.menu_leaderboard), onBack)
            DividerLine()
            if (entries.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = Yellow, strokeWidth = 2.dp)
                }
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(entries) { entry ->
                        val rankColor = when (entry.rank) { 1 -> Color(0xFFFFD700); 2 -> Color(0xFFC0C0C0); 3 -> Color(0xFFCD7F32); else -> TextSec }
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(2.dp))
                                .background(MetalBg.copy(0.7f))
                                .border(1.dp, if (entry.rank <= 3) rankColor.copy(0.4f) else BorderCol, RoundedCornerShape(2.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("#${entry.rank}", color = rankColor, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(36.dp))
                            Text(entry.playerName, color = Yellow, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(formatCurrency(entry.score), color = OmniumCol, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(onBack: () -> Unit, vm: ProfileVM = hiltViewModel()) {
    val profile by vm.profile.collectAsState()
    Box(Modifier.fillMaxSize().background(DarkBg)) {
        CrtScanlineOverlay(0f)
        Column(Modifier.fillMaxSize()) {
            TopBarBack(stringResource(R.string.menu_profile), onBack)
            DividerLine()
            Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PlayerCard(profile)
                StatRow(stringResource(R.string.game_stat_kills), profile.totalGames.toString(), Yellow)
                StatRow("Hayatta Kalma", profile.totalSurvived.toString(), SuccessGreen)
                StatRow("En Yüksek Skor", formatCurrency(profile.highScore), OmniumCol)
            }
        }
    }
}

@Composable
fun OmniButton(
    text    : String,
    onClick : () -> Unit,
    enabled : Boolean  = true,
    width   : Dp       = 200.dp,
    height  : Dp       = 48.dp,
    accent  : Color    = Yellow,
    modifier: Modifier = Modifier
) {
    AtmosphericButton(
        label    = text,
        icon     = Icons.Default.ArrowForward,
        accent   = accent,
        width    = width,
        height   = height,
        onClick  = onClick,
        enabled  = enabled,
        modifier = modifier
    )
}

@Composable
fun OmniTextField(
    value    : String,
    onValue  : (String) -> Unit,
    hint     : String,
    error    : String?  = null,
    isPassword: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(2.dp))
                .background(MetalBg)
                .border(1.dp, if (error != null) DangerRed.copy(0.7f) else BorderCol, RoundedCornerShape(2.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            androidx.compose.foundation.text.BasicTextField(
                value                = value,
                onValueChange        = onValue,
                singleLine           = true,
                textStyle            = TextStyle(color = Yellow, fontSize = 13.sp),
                cursorBrush          = SolidColor(Yellow),
                visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                decorationBox        = { inner ->
                    if (value.isEmpty()) Text(hint, color = TextDim, fontSize = 13.sp)
                    inner()
                }
            )
        }
        error?.let { Text(it, color = DangerRed, fontSize = 10.sp) }
    }
}

@Composable
fun OmniPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(3.dp))
            .background(MetalBg)
            .border(1.dp, BorderCol, RoundedCornerShape(3.dp))
            .padding(12.dp),
        content = content
    )
}

@Composable
fun StatusBar(label: String, progress: Float, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = TextDim, fontSize = 9.sp, letterSpacing = 1.sp)
        val animProgress by animateFloatAsState(progress.coerceIn(0f, 1f), tween(300), label = "status")
        LinearProgressIndicator(
            progress  = { animProgress },
            modifier  = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(2.dp)),
            color     = color,
            trackColor = MetalBg
        )
    }
}

@Composable
fun DividerLine() = Box(Modifier.fillMaxWidth().height(1.dp).background(BorderCol))

@Composable
fun TopBarBack(title: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Color.Black.copy(0.65f)).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Yellow) }
        Text(title, color = Yellow, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
    }
}

@Composable
fun CrtOverlay() {
    val inf    = rememberInfiniteTransition(label = "vhs")
    val noiseY by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(120, easing = LinearEasing), RepeatMode.Reverse), "ny")
    val roll   by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(7000, easing = LinearEasing)), "roll")
    Box(Modifier.fillMaxSize().drawWithContent {
        drawContent()
        val stripH = size.height * 0.05f
        val stripY = size.height * ((noiseY + roll) % 1f)
        drawRect(Color.White.copy(0.03f), topLeft = Offset(0f, stripY), size = Size(size.width, stripH))
        drawRect(Color(0xFF002200).copy(0.04f))
        for (j in 0..3) {
            val lx = (j * size.width * 0.25f + noiseY * size.width * 0.02f) % size.width
            drawLine(Color.White.copy(0.015f), Offset(lx, 0f), Offset(lx + size.width * 0.3f, size.height), 1.5f)
        }
    })
}

@Composable
fun GameHud(
    gameState : GameState,
    canEscape : Boolean,
    onPause   : () -> Unit,
    onFlash   : () -> Unit,
    onMove    : (Float, Float, Float) -> Unit,
    onLook    : (Float, Float) -> Unit,
    onJump    : () -> Unit,
    onCrouch  : () -> Unit,
    onInteract: () -> Unit
) {
    val sanityTint by animateColorAsState(
        if (gameState.sanity < 30f) DangerRed.copy(0.15f * (1f - gameState.sanity / 30f)) else Color.Transparent,
        tween(500), label = "sanity_tint"
    )
    Box(Modifier.fillMaxSize()) {
        // Full-screen look surface, laid out first so it sits behind every other
        // control below; the joystick/buttons have their own pointerInput regions
        // and claim touches within their bounds before this one sees them.
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(onDrag = { change, drag ->
                        change.consume()
                        onLook(drag.x, drag.y)
                    })
                }
        )
        if (sanityTint != Color.Transparent) Box(Modifier.fillMaxSize().background(sanityTint))
        Column(
            Modifier.align(Alignment.TopStart).padding(16.dp).width(160.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StatusBar(stringResource(R.string.game_hud_hp),      gameState.playerHp / gameState.playerMaxHp,  DangerRed)
            StatusBar(stringResource(R.string.game_hud_sanity),  gameState.sanity / 100f,                     SouliumCol)
            StatusBar(stringResource(R.string.game_hud_stamina), gameState.stamina / gameState.staminaMax,    SuccessGreen)
            StatusBar(stringResource(R.string.game_hud_battery), gameState.flashlightBattery,                 CrtAmber)
        }
        Row(
            Modifier.align(Alignment.TopEnd).padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            if (gameState.showPing) HudBadge("${gameState.entitiesNearby * 3 + 12} ms", SuccessGreen)
            if (gameState.showFps)  HudBadge("60 FPS", Yellow)
            IconButton(onClick = onPause, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Pause, null, tint = Yellow.copy(0.7f))
            }
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = canEscape,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 64.dp),
            enter = fadeIn(), exit = fadeOut()
        ) {
            HudBadge(stringResource(R.string.game_hud_exit_near), SuccessGreen)
        }
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
            Arrangement.SpaceBetween,
            Alignment.Bottom
        ) {
            VirtualJoystick(Modifier.size(120.dp), onMove = { dx, dy -> onMove(dx, 0f, dy) })
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HudIconButton(Icons.Default.FlashlightOn,    CrtAmber, onFlash)
                HudIconButton(Icons.Default.NearMe,          if (canEscape) SuccessGreen else TextSec, onInteract)
                HudIconButton(Icons.Default.KeyboardArrowUp, Yellow,   onJump)
                HudIconButton(Icons.Default.ArrowDownward,   TextSec,  onCrouch)
            }
        }
    }
}

@Composable
private fun HudBadge(text: String, color: Color) {
    Box(
        Modifier.clip(RoundedCornerShape(2.dp)).background(MetalBg.copy(0.8f)).padding(horizontal = 6.dp, vertical = 3.dp)
    ) { Text(text, color = color, fontSize = 10.sp) }
}

@Composable
private fun HudIconButton(icon: ImageVector, tint: Color, onClick: () -> Unit) {
    val interSrc  = remember { MutableInteractionSource() }
    val isPressed by interSrc.collectIsPressedAsState()
    val scale     by animateFloatAsState(if (isPressed) 0.92f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "hud_btn")
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(60.dp).scale(scale)
            .clip(RoundedCornerShape(4.dp))
            .background(MetalBg.copy(0.75f))
            .border(1.dp, tint.copy(0.4f), RoundedCornerShape(4.dp))
            .clickable(interactionSource = interSrc, indication = null, onClick = onClick)
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(28.dp)) }
}

@Composable
fun VirtualJoystick(modifier: Modifier, onMove: (Float, Float) -> Unit) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    val knobOffsetAnim by animateOffsetAsState(knobOffset, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "joystick")
    val radius = 48f
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(MetalBg.copy(0.6f))
            .border(1.dp, YellowDim, RoundedCornerShape(percent = 50))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd    = { knobOffset = Offset.Zero; onMove(0f, 0f) },
                    onDragCancel = { knobOffset = Offset.Zero; onMove(0f, 0f) },
                    onDrag       = { _, drag ->
                        val newX = (knobOffset.x + drag.x).coerceIn(-radius, radius)
                        val newY = (knobOffset.y + drag.y).coerceIn(-radius, radius)
                        knobOffset = Offset(newX, newY)
                        onMove(newX / radius, newY / radius)
                    }
                )
            }
    ) {
        Box(
            Modifier.size(40.dp)
                .offset(knobOffsetAnim.x.dp, knobOffsetAnim.y.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(
                    Brush.radialGradient(listOf(Yellow, Yellow.copy(0.4f)))
                )
        )
    }
}

@Composable
fun PauseOverlay(onResume: () -> Unit, onExit: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.75f)), Alignment.Center) {
        Column(
            Modifier.width(260.dp).clip(RoundedCornerShape(4.dp))
                .background(MetalBg)
                .border(1.dp, BorderCol, RoundedCornerShape(4.dp))
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.game_paused), color = Yellow, fontSize = 20.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
            DividerLine()
            AtmosphericButton(stringResource(R.string.game_resume),    Icons.Default.PlayArrow, Yellow,    200.dp, 50.dp, onResume)
            AtmosphericButton(stringResource(R.string.game_exit_menu), Icons.Default.ExitToApp, DangerRed, 200.dp, 50.dp, onExit)
        }
    }
}

@Composable
fun GameOverOverlay(gameState: GameState, onExit: () -> Unit) {
    val inf   = rememberInfiniteTransition(label = "go")
    val pulse by inf.animateFloat(0.6f, 1f, infiniteRepeatable(tween(900, easing = EaseInOut), RepeatMode.Reverse), "p")
    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.88f)), Alignment.Center) {
        Column(
            Modifier.width(280.dp).clip(RoundedCornerShape(4.dp))
                .background(MetalBg)
                .border(1.dp, DangerRed.copy(0.5f), RoundedCornerShape(4.dp))
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.game_over_title), color = DangerRed.copy(pulse), fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
            DividerLine()
            StatRow(stringResource(R.string.game_stat_score),      gameState.score.toString(),              Yellow)
            StatRow(stringResource(R.string.game_stat_kills),      gameState.kills.toString(),              DangerRed)
            StatRow(stringResource(R.string.game_stat_time),       formatElapsed(gameState.sessionElapsed), TextSec)
            StatRow(stringResource(R.string.game_stat_difficulty), gameState.difficulty.uppercase(),        CrtAmber)
            DividerLine()
            AtmosphericButton(stringResource(R.string.game_exit_menu), Icons.Default.ExitToApp, DangerRed, 220.dp, 50.dp, onExit)
        }
    }
}

@Composable
fun EscapedOverlay(gameState: GameState, onExit: () -> Unit) {
    val inf  = rememberInfiniteTransition(label = "esc")
    val glow by inf.animateFloat(0.5f, 1f, infiniteRepeatable(tween(1200, easing = EaseInOut), RepeatMode.Reverse), "g")
    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.85f)), Alignment.Center) {
        Column(
            Modifier.width(280.dp).clip(RoundedCornerShape(4.dp))
                .background(MetalBg)
                .border(1.dp, SuccessGreen.copy(0.5f), RoundedCornerShape(4.dp))
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.game_escaped_title), color = SuccessGreen.copy(glow), fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
            DividerLine()
            StatRow(stringResource(R.string.game_stat_score),      gameState.score.toString(),              Yellow)
            StatRow(stringResource(R.string.game_stat_kills),      gameState.kills.toString(),              DangerRed)
            StatRow(stringResource(R.string.game_stat_time),       formatElapsed(gameState.sessionElapsed), TextSec)
            StatRow(stringResource(R.string.game_stat_difficulty), gameState.difficulty.uppercase(),        CrtAmber)
            DividerLine()
            AtmosphericButton(stringResource(R.string.game_exit_menu), Icons.Default.ExitToApp, SuccessGreen, 220.dp, 50.dp, onExit)
        }
    }
}

@Composable
private fun ChapterListView(state: StoryUiState, onBack: () -> Unit, onSelect: (StoryChapterDto) -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xFF050503))) {
        CrtOverlay()
        Column(Modifier.fillMaxSize()) {
            TopBarBack(stringResource(R.string.story_title), onBack)
            DividerLine()
            if (state.isLoading) Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = Yellow, strokeWidth = 2.dp)
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.chapters, key = { it.id }) { ch -> ChapterCard(chapter = ch, onClick = { onSelect(ch) }) }
                }
            }
        }
    }
}

@Composable
private fun ChapterCard(chapter: StoryChapterDto, onClick: () -> Unit) {
    val locked = !chapter.isUnlocked
    val interSrc  = remember { MutableInteractionSource() }
    val isPressed by interSrc.collectIsPressedAsState()
    val scale     by animateFloatAsState(if (isPressed) 0.98f else 1f, spring(), label = "ch_card")
    Row(
        Modifier.fillMaxWidth().scale(scale)
            .clip(RoundedCornerShape(3.dp))
            .background(MetalBg)
            .border(1.dp, if (locked) BorderCol else Yellow.copy(0.3f), RoundedCornerShape(3.dp))
            .clickable(interactionSource = interSrc, indication = null, enabled = !locked, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(3.dp))
                .background(if (locked) MetalBg else Yellow.copy(0.15f)),
            Alignment.Center
        ) {
            if (locked) Icon(Icons.Default.Lock, null, modifier = Modifier.size(20.dp), tint = TextDim)
            else Text(chapter.id.toString(), color = Yellow, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(if (locked) "???" else chapter.displayTitle, color = if (locked) TextDim else Yellow, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(
                if (locked) stringResource(R.string.story_chapter_locked) else chapter.displayContent.take(80) + "…",
                color = TextDim, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp
            )
        }
        Icon(
            if (locked) Icons.Default.Lock else Icons.AutoMirrored.Filled.ArrowForward,
            null, tint = if (locked) TextDim else Yellow, modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun BookReadingView(chapter: StoryChapterDto, onClose: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xFF050503))) {
        CrtOverlay()
        Box(
            Modifier.align(Alignment.Center).fillMaxWidth(0.82f).fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp, topEnd = 10.dp, bottomEnd = 10.dp))
                .background(Color(0xFF1A1408))
                .border(
                    2.dp,
                    Brush.verticalGradient(listOf(Color(0xFF5A3A10), Color(0xFF2A1A06), Color(0xFF5A3A10))),
                    RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp, topEnd = 10.dp, bottomEnd = 10.dp)
                )
                .drawWithContent {
                    drawContent()
                    drawRect(Brush.horizontalGradient(listOf(Color.Black.copy(0.45f), Color.Transparent), 0f, 44f))
                    drawLine(Color(0xFF3A2208), Offset(36f, 0f), Offset(36f, size.height), 3f)
                }
        ) {
            Column(
                Modifier.fillMaxSize()
                    .padding(start = 56.dp, end = 24.dp, top = 28.dp, bottom = 28.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("Bölüm ${chapter.id}", color = Color(0xFF8B6914), fontSize = 12.sp, letterSpacing = 3.sp)
                    IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp), tint = Color(0xFF8B6914))
                    }
                }
                Text(chapter.displayTitle, color = Color(0xFF8B6914), fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF5A3A10).copy(0.5f)))
                chapter.displayContent.split("\n\n").forEach { para ->
                    if (para.startsWith("\"") || para.startsWith("—"))
                        Text(para, color = Color(0xFF8A6A40), fontSize = 12.sp, fontStyle = FontStyle.Italic,
                            lineHeight = 20.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    else
                        Text(para, color = Color(0xFFC8A870), fontSize = 13.sp, lineHeight = 22.sp, textAlign = TextAlign.Justify)
                }
                Spacer(Modifier.height(16.dp))
                Text("— Son —", color = Color(0xFF5A3A10), fontSize = 11.sp, letterSpacing = 3.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun CorridorCanvas(pan: Float, flicker: Float, modifier: Modifier) {
    androidx.compose.foundation.Canvas(modifier) {
        val w = size.width; val h = size.height; val cx = w / 2f; val cy = h / 2f
        drawRect(Color(0xFF1A1508))
        for (i in 0..7) {
            val t = (i / 7f + pan * 0.12f) % 1f; val per = 1f - t * 0.94f
            val ww = w * per; val hh = h * per; val lx = cx - ww / 2f; val ty = cy - hh / 2f; val al = (1f - t * 0.85f) * 0.55f
            drawRect(Yellow.copy(al * 0.09f), Offset(lx, ty), Size(ww, hh), style = Stroke(1.5f))
            val lightY = ty + hh * 0.04f; val lightW = ww * 0.28f; val lf = flicker * al
            drawRect(
                Brush.radialGradient(listOf(Color(0xFFEEDD88).copy(lf * 0.95f), Yellow.copy(lf * 0.3f), Color.Transparent),
                    Offset(cx, lightY), lightW),
                Offset(cx - lightW / 2f, lightY - 18f), Size(lightW, 36f)
            )
            drawRect(Color(0xFFD4A84B).copy(al * (sin(i * 7.3f + pan * 13.1f) * 0.15f + 0.85f) * 0.04f), Offset(lx, ty), Size(ww, hh))
        }
        drawRect(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(0.75f)), Offset(cx, cy), w * 0.76f))
    }
}

@Composable
fun CarpetProgressBar(progress: Float, modifier: Modifier) {
    val animProg by animateFloatAsState(progress.coerceIn(0f, 1f), tween(600, easing = EaseOutCubic), label = "carpet_prog")
    androidx.compose.foundation.Canvas(modifier) {
        val w = size.width; val h = size.height
        drawRoundRect(Color(0xFF1A1208), cornerRadius = CornerRadius(h / 2f))
        if (animProg > 0f) {
            val fw = w * animProg
            drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFF3D2B10), Color(0xFF7A5A18), Color(0xFFD4A84B), Color(0xFF9A7228)), 0f, fw),
                size = Size(fw, h), cornerRadius = CornerRadius(h / 2f))
            drawRect(Brush.verticalGradient(listOf(Color.White.copy(0.18f), Color.Transparent)), Offset(0f, 0f), Size(fw, h / 2f))
        }
        drawRoundRect(Color(0xFF5A4020), cornerRadius = CornerRadius(h / 2f), style = Stroke(1f))
    }
}

@Composable
private fun MarketCard(item: MarketItemDto, isPurchasing: Boolean, onBuy: () -> Unit) {
    val currencyColor = when (item.currency.lowercase()) { "omnium" -> OmniumCol; "soulium" -> SouliumCol; "tl" -> SuccessGreen; else -> CrtAmber }
    val inf  = rememberInfiniteTransition(label = "card")
    val glow by inf.animateFloat(0.3f, 0.7f, infiniteRepeatable(tween(2000, easing = EaseInOut), RepeatMode.Reverse), "g")
    val interSrc  = remember { MutableInteractionSource() }
    val isPressed by interSrc.collectIsPressedAsState()
    val scale     by animateFloatAsState(if (isPressed) 0.97f else 1f, spring(), label = "card_scale")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.scale(scale)
            .clip(RoundedCornerShape(3.dp))
            .background(MetalBg)
            .border(1.dp, if (item.isLimited) CrtAmber.copy(glow) else BorderCol, RoundedCornerShape(3.dp))
            .padding(12.dp)
    ) {
        if (item.isLimited) Box(Modifier.fillMaxWidth().padding(bottom = 4.dp), Alignment.TopEnd) {
            Box(
                Modifier.clip(RoundedCornerShape(2.dp)).background(CrtAmber.copy(0.2f)).padding(horizontal = 6.dp, vertical = 2.dp)
            ) { Text(stringResource(R.string.market_limited), color = CrtAmber, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp) }
        }
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(4.dp)).background(currencyColor.copy(0.12f)), Alignment.Center
        ) { Icon(Icons.Default.Category, null, tint = currencyColor.copy(0.9f), modifier = Modifier.size(28.dp)) }
        Spacer(Modifier.height(8.dp))
        Text(item.nameTr, color = Yellow, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
            textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(item.descTr, color = TextDim, fontSize = 9.sp, textAlign = TextAlign.Center, maxLines = 2,
            overflow = TextOverflow.Ellipsis, lineHeight = 13.sp, modifier = Modifier.padding(top = 3.dp))
        Spacer(Modifier.height(10.dp))
        if (item.isOwned) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                Text(stringResource(R.string.market_equipped), color = SuccessGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        } else if (isPurchasing) {
            CircularProgressIndicator(color = Yellow, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth().height(32.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(currencyColor.copy(0.15f))
                    .border(1.dp, currencyColor.copy(0.5f), RoundedCornerShape(2.dp))
                    .clickable(interactionSource = interSrc, indication = null, onClick = onBuy)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(item.price.toString(), color = currencyColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(item.currency.uppercase(), color = currencyColor.copy(0.7f), fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
private fun PurchaseConfirmDialog(item: MarketItemDto, onConfirm: () -> Unit, onCancel: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.8f)).clickable(onClick = onCancel)) {
        Column(
            Modifier.align(Alignment.Center).fillMaxWidth(0.8f)
                .clip(RoundedCornerShape(4.dp))
                .background(MetalBg)
                .border(1.dp, Yellow.copy(0.4f), RoundedCornerShape(4.dp))
                .clickable {}
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.market_confirm_title), color = Yellow, fontSize = 18.sp, fontWeight = FontWeight.Black)
            DividerLine()
            Text(item.nameTr, color = Yellow, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(item.descTr, color = TextSec, fontSize = 12.sp, textAlign = TextAlign.Center)
            DividerLine()
            Text("${item.price} ${item.currency.uppercase()}", color = OmniumCol, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AtmosphericButton(stringResource(R.string.common_cancel), Icons.Default.Close,    TextDim, 120.dp, 44.dp, onCancel)
                AtmosphericButton(stringResource(R.string.market_buy),    Icons.Default.ShoppingCart, Yellow, 120.dp, 44.dp, onConfirm)
            }
        }
    }
}

@Composable
private fun CurrencyBadge(amount: Long, color: Color, icon: ImageVector) {
    Row(
        Modifier.clip(RoundedCornerShape(2.dp)).background(MetalBg).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(12.dp))
        Text(amount.toString(), color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun VipBadge() {
    Box(
        Modifier.clip(RoundedCornerShape(2.dp))
            .background(Color(0xFFFFD700).copy(0.2f))
            .border(1.dp, Color(0xFFFFD700).copy(0.6f), RoundedCornerShape(2.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) { Text("VIP", color = Color(0xFFFFD700), fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp) }
}

@Composable
private fun CharStatBar(label: String, value: Float, display: String, color: Color) {
    val animVal by animateFloatAsState(value.coerceIn(0f, 1f), tween(500, easing = EaseOutCubic), label = "stat_bar")
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text(label, color = TextSec, fontSize = 11.sp)
            Text(display, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(progress = { animVal }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)), color = color, trackColor = MetalBg)
    }
}

@Composable
private fun PlayerCard(profile: PlayerProfile) {
    OmniPanel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(46.dp).clip(CircleShape).background(MetalBg), Alignment.Center) {
                Text(profile.name.take(1).uppercase(), color = Yellow, fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(profile.name, color = Yellow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.player_level_prefix) + profile.level, color = TextSec, fontSize = 11.sp)
                    if (profile.isVip) Box(
                        Modifier.clip(RoundedCornerShape(1.dp)).background(Color(0xFFFFD700).copy(0.2f)).padding(horizontal = 4.dp, vertical = 1.dp)
                    ) { Text("VIP", color = Color(0xFFFFD700), fontSize = 7.sp, fontWeight = FontWeight.Black) }
                }
                val xpAnim by animateFloatAsState(profile.xpProgress, tween(800, easing = EaseOutCubic), label = "xp")
                LinearProgressIndicator(progress = { xpAnim }, modifier = Modifier.width(88.dp).height(4.dp).clip(RoundedCornerShape(2.dp)), color = Yellow, trackColor = MetalBg)
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, color: Color) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(label, color = TextSec, fontSize = 12.sp)
        Text(value,  color = color,   fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

fun formatCurrency(amount: Long): String = when {
    amount >= 1_000_000 -> "${amount / 1_000_000}M"
    amount >= 1_000     -> "${amount / 1_000}K"
    else                -> amount.toString()
}

fun formatElapsed(ms: Long): String {
    val m = ms / 60_000; val s = (ms % 60_000) / 1000
    return "%02d:%02d".format(m, s)
}

private val GameState.vhsEnabled: Boolean get() = true
private val GameState.showFps   : Boolean get() = false
private val GameState.showPing  : Boolean get() = true
