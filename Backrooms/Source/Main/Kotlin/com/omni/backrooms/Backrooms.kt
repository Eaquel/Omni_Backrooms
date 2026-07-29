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
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import androidx.compose.ui.unit.IntOffset
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
        installCrashLogger()
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

    /** Writes every uncaught exception to Documents/OmniBackrooms/crash.txt so
     *  crashes can be read off the device directly, then delegates to the
     *  previous handler so Crashlytics still records it. */
    private fun installCrashLogger() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { writeCrashReport(thread, error) }
            previous?.uncaughtException(thread, error)
        }
    }

    private fun writeCrashReport(thread: Thread, error: Throwable) {
        val stamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(java.util.Date())
        val text = buildString {
            appendLine("===== OMNI BACKROOMS CRASH =====")
            appendLine("time    : $stamp")
            appendLine("version : ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("device  : ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            appendLine("android : ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})")
            appendLine("abi     : ${android.os.Build.SUPPORTED_ABIS.joinToString()}")
            appendLine("thread  : ${thread.name}")
            appendLine()
            appendLine(java.io.StringWriter().also { sw ->
                error.printStackTrace(java.io.PrintWriter(sw))
            }.toString())
            appendLine()
        }

        // Scoped storage (API 29+) disallows writing straight into Documents, so
        // go through MediaStore there and fall back to a direct file otherwise.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val resolver = contentResolver
            val relPath = "${android.os.Environment.DIRECTORY_DOCUMENTS}/OmniBackrooms"
            val collection = android.provider.MediaStore.Files.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val selection = "${android.provider.MediaStore.MediaColumns.RELATIVE_PATH}=? AND " +
                            "${android.provider.MediaStore.MediaColumns.DISPLAY_NAME}=?"
            val existing = resolver.query(
                collection,
                arrayOf(android.provider.MediaStore.MediaColumns._ID),
                selection,
                arrayOf("$relPath/", "crash.txt"),
                null
            )?.use { c -> if (c.moveToFirst()) c.getLong(0) else null }

            val uri = if (existing != null) {
                android.content.ContentUris.withAppendedId(collection, existing)
            } else {
                resolver.insert(collection, android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "crash.txt")
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, relPath)
                })
            }
            uri?.let { target ->
                // "wa" = append, so earlier crashes aren't lost.
                resolver.openOutputStream(target, "wa")?.use { it.write(text.toByteArray()) }
            }
        } else {
            val dir = java.io.File(
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS),
                "OmniBackrooms"
            )
            if (!dir.exists()) dir.mkdirs()
            java.io.File(dir, "crash.txt").appendText(text)
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
        // SUSPICIOUS covers emulator heuristics and single miscellaneous flags,
        // which false-positive on plenty of real retail devices — warning there
        // just cries wolf at legitimate players. Only surface HIGH and above.
        if (BuildConfig.ENABLE_GUARD && guardReport.threatLevel >= ThreatLevel.HIGH) showGuardDialog = true
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
            composable(Route.ROOM)        { Room(onJoined = { nav.navigate(Route.GAME) }, onBack = { nav.popBackStack() }, onCreate = { nav.navigate(Route.CREATE_ROOM) }) }
            composable(Route.CREATE_ROOM) { CreateRoom(onCreated = { nav.popBackStack() }, onBack = { nav.popBackStack() }) }
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

        val en = readMono("Story/en.json")
        val tr = readMono("Story/tr.json")
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
        // EXPECTED_SIG_HASH ships blank until a real release keystore's SHA-256 is
        // configured (see build.gradle.kts). Until then, skip this specific check
        // rather than flag every legitimate install as tampered.
        val sigCheckOn = BuildConfig.EXPECTED_SIG_HASH.isNotBlank()
        val sigValid  = if (sigCheckOn) bridge.isSignatureValid() else true
        val hook      = detectHooking()
        val memTamper = detectMemoryTampering()
        val reportStr = bridge.getThreatReport()
        val level     = when {
            frida || debugged || hook          -> ThreatLevel.CRITICAL
            rooted || (sigCheckOn && !sigValid) -> ThreatLevel.HIGH
            memTamper                           -> ThreatLevel.HIGH
            emulator                            -> ThreatLevel.SUSPICIOUS
            flags != 0                          -> ThreatLevel.SUSPICIOUS
            else                                -> ThreatLevel.CLEAN
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
                if (!BuildConfig.ENABLE_GUARD) return@collect
                when (level) {
                    ThreatLevel.CRITICAL -> {
                        // Log first — a silent kill on a false positive is
                        // indistinguishable from a crash to the player.
                        FirebaseCrashlytics.getInstance().log("CRITICAL_THREAT: ${report.value.report}")
                        android.os.Process.killProcess(android.os.Process.myPid())
                    }
                    ThreatLevel.HIGH -> FirebaseCrashlytics.getInstance().log("HIGH_THREAT: ${report.value.report}")
                    else -> {}
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
    private val settings    : SettingsRepository,
    private val api         : ApiService,
    private val saveStore   : SaveGameStore
) : ViewModel() {

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private var physicsJob : Job? = null
    private var entityJob  : Job? = null
    private var scoreJob   : Job? = null
    private var autosaveJob: Job? = null
    private var lastTickMs = 0L
    private var elapsedMs  = 0L
    private var score      = 0L
    private var kills      = 0
    /** Read once at start instead of per look-event: the old code opened a
     *  DataStore flow on every touch move, which is far too slow for input. */
    @Volatile private var cachedSensitivity = 1f
    /** Guards against startGame running twice (re-entering the screen quickly),
     *  which would spawn a second physics loop advancing the same native sim. */
    private var started = false

    private companion object {
        /** Sized against the engine's 80 kg body and drag 8 so terminal walking
         *  speed lands near 3.6 m/s. force = speed * mass * drag. */
        const val MOVE_FORCE = 2_300f
    }

    /** Segments for the currently loaded level; kept here (not just in GameState) so the
     *  entity spawner can reuse them without depending on StateFlow emission timing. */
    private var segments: List<LevelSegment> = emptyList()

    fun startGame(difficulty: String = "normal", seed: Long = System.currentTimeMillis()) {
        if (started) return
        started = true
        viewModelScope.launch {
            val sensitivity = settings.observe().first().cameraSensitivity
            bridge.initCore(seed)
            bridge.initSound()
            bridge.initEntities()
            bridge.setAmbienceLevel(0.4f)
            bridge.setHumVolume(0.3f)
            bridge.setSpatialRolloff(1f, 40f)

            // Level 0 always — there is deliberately no map selection.
            val nodeCount = if (difficulty == "hard") 60 else 40
            segments = LevelSegment.listFromFloatArray(bridge.generateLevel(nodeCount, depth = 0))
            val exit = segments.lastOrNull()
            val exitX = exit?.endX ?: 0f
            val exitZ = exit?.endZ ?: 0f

            val cfg = assetManager.getSpawnConfig(difficulty)
            spawnInitialEntities(bridge, segments, cfg)
            _state.value = GameState(
                seed = seed, difficulty = difficulty, mapId = "level_0",
                levelSegments = segments, exitX = exitX, exitZ = exitZ,
                spawnPhase = SpawnPhase.FALLING
            )
            startPhysicsLoop(sensitivity)
            startEntitySpawner(difficulty, cfg)
            startScoreAccumulator()
            startAutosave()
            playSpawnDrop()
        }
    }

    private fun startPhysicsLoop(sensitivity: Float) {
        cachedSensitivity = sensitivity
        // Keep following the setting so changes apply without restarting a run.
        viewModelScope.launch {
            settings.observe().collect { cachedSensitivity = it.cameraSensitivity.coerceAtLeast(0.05f) }
        }
        lastTickMs = bridge.nowMs()
        physicsJob = viewModelScope.launch {
            while (isActive) {
                if (_state.value.isPaused) { delay(16); continue }
                val now = bridge.nowMs()
                val dt  = ((now - lastTickMs).coerceIn(1, 100)).toFloat() / 1000f
                lastTickMs = now; elapsedMs += (dt * 1000).toLong()

                // Continuous movement: applied every tick from the held joystick
                // vector, so holding a direction keeps the player moving.
                val mx = moveX; val mz = moveZ
                val mag = kotlin.math.hypot(mx, mz)
                if (mag > 0.05f) {
                    val tired = _state.value.stamina <= 10f
                    val force = MOVE_FORCE * (if (tired) 0.55f else 1f)
                    bridge.applyMovement(mx * force, 0f, mz * force)
                    _state.update { it.copy(stamina = (it.stamina - 14f * dt * mag).coerceAtLeast(0f)) }
                    footstepTimer -= dt * mag
                    if (footstepTimer <= 0f) {
                        footstepTimer = 0.45f
                        bridge.triggerFootstep(120f, 0.3f)
                    }
                }

                val wasOver = _state.value.isGameOver
                val derived = stepSimulation(bridge, dt)
                _state.update { applyTickToState(it, derived, dt, elapsedMs, score) }
                if (!wasOver && _state.value.isGameOver) submitScoreToServer()
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
                    spawnOneRandomEntity(bridge, segments, cfg)
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

    /** Current joystick vector, applied continuously by the physics loop rather
     *  than on drag events — holding the stick still emits no events, so
     *  event-driven force meant the player stopped whenever their finger did. */
    @Volatile private var moveX = 0f
    @Volatile private var moveZ = 0f
    private var footstepTimer = 0f

    fun onMove(dx: Float, dy: Float, dz: Float) {
        if (_state.value.spawnPhase != SpawnPhase.READY) return
        moveX = dx.coerceIn(-1f, 1f)
        moveZ = dz.coerceIn(-1f, 1f)
        if (dy != 0f) bridge.applyMovement(0f, dy * MOVE_FORCE, 0f)
    }

    fun onLook(dx: Float, dy: Float) {
        val sensitivity = cachedSensitivity
        bridge.cameraLook(dx, dy, sensitivity)
    }

    fun onJump()   { bridge.applyMovement(0f, 26_000f, 0f) }
    fun onCrouch() { bridge.applyMovement(0f, -8_000f, 0f) }
    fun toggleFlashlight() { _state.update { it.copy(flashlightOn = !it.flashlightOn) } }
    fun togglePause()      { _state.update { it.copy(isPaused = !it.isPaused) } }

    /** True once the player is close enough to the exit for [onInteract] to work; the HUD
     *  uses this to show a prompt so the player knows the exit is reachable. */
    val canEscape: Boolean get() = _state.value.distanceToExit < 3.5f

    /** Silences the native audio engine when the game screen isn't foreground.
     *  Without this the ambience/hum kept playing after leaving the screen. */
    fun onScreenPaused() {
        _state.update { it.copy(isPaused = true) }
        runCatching { bridge.setAmbienceLevel(0f); bridge.setHumVolume(0f) }
        saveNow()
    }

    fun onScreenResumed() {
        runCatching { bridge.setAmbienceLevel(0.4f); bridge.setHumVolume(0.3f) }
        _state.update { it.copy(isPaused = false) }
    }

    /** Writes a resumable snapshot. The level itself isn't stored — it's fully
     *  reproducible from the seed — so this stays small enough to run on a timer
     *  without hitching the game loop. */
    private fun saveNow() {
        val s = _state.value
        if (s.isGameOver || s.isEscaped) return
        viewModelScope.launch(Dispatchers.IO) {
            saveStore.save(
                SavedRun(
                    seed = s.seed, difficulty = s.difficulty, elapsedMs = elapsedMs,
                    score = score, kills = kills, sanity = s.sanity,
                    battery = s.flashlightBattery, playerHp = s.playerHp,
                    savedAtMs = System.currentTimeMillis()
                )
            )
        }
    }

    private fun startAutosave() {
        autosaveJob = viewModelScope.launch {
            while (isActive) {
                delay(60_000)
                if (!_state.value.isPaused) saveNow()
            }
        }
    }

    /** Drops the player in from above and lets them stand up, rather than just
     *  appearing on the floor. Input stays locked until they're upright. */
    private fun playSpawnDrop() {
        viewModelScope.launch {
            _state.update { it.copy(spawnPhase = SpawnPhase.FALLING) }
            // The engine's gravity does the actual falling; we just hold input
            // and let the camera ride the body down from its elevated start.
            delay(1500)
            _state.update { it.copy(spawnPhase = SpawnPhase.LANDED) }
            runCatching { bridge.triggerFootstep(60f, 1.0f) }
            delay(1200)
            _state.update { it.copy(spawnPhase = SpawnPhase.READY) }
        }
    }

    fun onInteract() {
        if (canEscape) {
            _state.update { it.copy(isEscaped = true) }
            submitScoreToServer()
        }
    }

    fun onDamageEntity(id: Int) {
        bridge.damageEntity(id, 25f); kills++; score += 100L
        _state.update { it.copy(kills = kills, score = score) }
    }

    /** Reports the finished run to the leaderboard API, Firestore, and
     *  Crashlytics (for crash-context, not analytics). Best-effort: a failed
     *  submission shouldn't block the player from seeing their own results. */
    private fun submitScoreToServer() {
        viewModelScope.launch {
            // The run is over, so a stale snapshot must not linger behind
            // "Continue".
            runCatching { saveStore.clear() }
            val s = _state.value
            runCatching {
                api.submitScore(ScoreSubmitRequest(s.level, score, if (s.isEscaped) 1 else 0, s.difficulty, elapsedMs, kills))
            }
            runCatching {
                FirebaseFirestore.getInstance().collection("leaderboard").add(
                    mapOf(
                        "difficulty" to s.difficulty, "score" to score, "kills" to kills,
                        "sessionMs" to elapsedMs, "ts" to System.currentTimeMillis()
                    )
                )
            }
            runCatching {
                FirebaseCrashlytics.getInstance().setCustomKey("last_score", score)
                FirebaseCrashlytics.getInstance().setCustomKey("difficulty", s.difficulty)
            }
        }
    }

    override fun onCleared() {
        physicsJob?.cancel(); entityJob?.cancel(); scoreJob?.cancel(); autosaveJob?.cancel()
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
    onProfile    : () -> Unit,
    profileVm    : ProfileVM = hiltViewModel(),
    lobbyVm      : LobbyVM   = hiltViewModel()
) {
    val profile by profileVm.profile.collectAsState()
    val hasSave by lobbyVm.hasSave.collectAsState()
    val guestName by lobbyVm.guestName.collectAsState()
    var toast by remember { mutableStateOf<String?>(null) }
    var showOfflineChoice by remember { mutableStateOf(false) }
    val comingSoon = stringResource(R.string.menu_coming_soon)
    val noSaveMsg  = stringResource(R.string.menu_no_save)

    LaunchedEffect(toast) {
        if (toast != null) { delay(1800); toast = null }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // Centre stays deliberately empty so the lobby video reads as the scene.
        LobbyVideoBackground(Modifier.fillMaxSize())
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(0.55f)),
                    radius = 900f
                )
            )
        )

        // ---- Top-left: identity + wallet -------------------------------------
        Row(
            Modifier.align(Alignment.TopStart).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarBadge(level = profile.level, onClick = onProfile)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    profile.name.takeIf { it.isNotBlank() && it != "Wanderer" } ?: guestName,
                    color = Yellow, fontSize = 15.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp
                )
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CurrencyChip(OmniumCol, profile.omniumAmount, isOmnium = true)
                    Spacer(Modifier.width(8.dp))
                    CurrencyChip(SouliumCol, profile.souliumAmount, isOmnium = false)
                }
            }
        }

        // ---- Top-right: settings + leaderboard --------------------------------
        Row(
            Modifier.align(Alignment.TopEnd).padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconGlyphButton(40.dp, TextSec, onClick = { toast = comingSoon }) { drawLeaderboardGlyph(it) }
            IconGlyphButton(40.dp, Yellow,  onClick = onSettings)            { drawGearGlyph(it) }
        }

        // ---- Left edge: navigation rail ---------------------------------------
        Column(
            Modifier.align(Alignment.CenterStart).padding(start = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            RailItem(stringResource(R.string.menu_market),    CrtAmber,     onMarket)  { drawMarketGlyph(it) }
            RailItem(stringResource(R.string.menu_story),      Yellow,       onStory)   { drawBookGlyph(it) }
            RailItem(stringResource(R.string.menu_abilities),  TextSec,      { toast = comingSoon }) { drawAbilityGlyph(it) }
            RailItem(stringResource(R.string.menu_season),     SouliumCol,   { toast = comingSoon }) { drawSeasonGlyph(it) }
        }

        // ---- Right edge: play modes -------------------------------------------
        Column(
            Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
            verticalArrangement   = Arrangement.spacedBy(14.dp),
            horizontalAlignment   = Alignment.End
        ) {
            PlayModeButton(stringResource(R.string.menu_play_offline), SuccessGreen, { showOfflineChoice = true }) { drawOfflineGlyph(it) }
            PlayModeButton(stringResource(R.string.menu_play_online),  OmniumCol,    onOnline) { drawOnlineGlyph(it) }
        }

        if (showOfflineChoice) {
            OfflineChoiceDialog(
                hasSave     = hasSave,
                onNewGame   = { showOfflineChoice = false; lobbyVm.clearSave(); onPlay() },
                onContinue  = { showOfflineChoice = false; onPlay() },
                onNoSave    = { toast = noSaveMsg },
                onDismiss   = { showOfflineChoice = false }
            )
        }

        androidx.compose.animation.AnimatedVisibility(
            visible  = toast != null,
            enter    = fadeIn() + slideInVertically { it / 2 },
            exit     = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp)
        ) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(0.85f))
                    .border(1.dp, YellowDim, RoundedCornerShape(6.dp))
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text(toast ?: "", color = Yellow, fontSize = 12.sp, letterSpacing = 2.sp)
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
fun AtmosphericButton(
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
float hash(vec2 p){ return fract(sin(dot(p, vec2(41.3,289.1))) * 43758.5453); }
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
    // Cheap fake AO: darken wall surfaces near the floor seam so geometry reads
    // as grounded instead of floating tiles. Skipped on floor/ceiling (upward or
    // downward normals) since those aren't touching a base seam.
    float wallFactor = 1.0 - abs(n.y);
    float groundAO = mix(1.0, mix(0.76, 1.0, smoothstep(0.0, 1.4, vWorldPos.y)), wallFactor);
    // Subtle tileable micro-detail so flat texture repeats don't look sterile.
    float micro = 0.94 + 0.06 * hash(floor(vUV * 37.0));
    float lit = (0.08 + overhead*0.95 + flash) * groundAO;
    vec3 col = tex.rgb * lit * micro;
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
uniform vec3 uColor; uniform float uAlert; uniform float uAlpha; uniform float uColorBlind;
out vec4 fragColor;
void main(){
    vec2 d = (vUV - vec2(0.5, 0.42)) * vec2(1.0, 1.35);
    float body = smoothstep(0.5, 0.28, length(d));
    vec2 eyeD = (vUV - vec2(0.5, 0.62)) * vec2(1.0, 1.6);
    float core = smoothstep(0.16, 0.02, length(eyeD));
    vec3 normalRamp = mix(vec3(0.85,0.78,0.25), vec3(1.0,0.05,0.05), uAlert);
    vec3 safeRamp   = mix(vec3(0.25,0.55,0.95), vec3(1.0,0.55,0.05), uAlert);
    vec3 eyeColor = mix(normalRamp, safeRamp, uColorBlind);
    vec3 col = mix(uColor*0.12, eyeColor, core);
    float alpha = body*uAlpha;
    if (alpha < 0.02) discard;
    fragColor = vec4(col, alpha);
}
"""

private const val OMNI_SHADOW_VERT = """#version 300 es
layout(location=0) in vec2 aCorner;
uniform mat4 uVP; uniform vec3 uCenter; uniform float uSize;
out vec2 vUV;
void main(){
    vec3 worldPos = uCenter + vec3(aCorner.x*uSize, 0.02, aCorner.y*uSize);
    vUV = aCorner*0.5 + 0.5;
    gl_Position = uVP * vec4(worldPos, 1.0);
}
"""

private const val OMNI_SHADOW_FRAG = """#version 300 es
precision mediump float;
in vec2 vUV;
uniform float uAlpha;
out vec4 fragColor;
void main(){
    float d = length(vUV - vec2(0.5));
    float a = smoothstep(0.5, 0.08, d) * uAlpha;
    if (a < 0.01) discard;
    fragColor = vec4(0.0, 0.0, 0.0, a);
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
uniform float uColorBlindMix; uniform vec3 uColorBlindAxis;
out vec4 fragColor;
float rand(vec2 co){ return fract(sin(dot(co, vec2(12.9898,78.233))) * 43758.5453); }
void main(){
    // Subtle barrel (lens) distortion, strongest toward the screen edges.
    vec2 centered = vUV * 2.0 - 1.0;
    float r2 = dot(centered, centered);
    vec2 barrel = centered * (1.0 + 0.035 * r2 * uVhsStrength);
    vec2 uv = clamp(barrel * 0.5 + 0.5, 0.0, 1.0);

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

    // Colorblind-safe boost: nudges saturation onto the blue/yellow axis instead
    // of relying on red/green contrast, without changing the overall mood.
    if (uColorBlindMix > 0.001) {
        float luma = dot(col, vec3(0.299, 0.587, 0.114));
        vec3 shifted = mix(vec3(luma), col, 0.7) + uColorBlindAxis * (luma * 0.35);
        col = mix(col, shifted, uColorBlindMix);
    }

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

    private var sceneProgram = 0; private var billboardProgram = 0; private var postProgram = 0; private var shadowProgram = 0
    private var uMVP = 0; private var uTex = 0; private var uCamPos = 0
    private var uFlashDir = 0; private var uFlashOn = 0; private var uFogDensity = 0
    private var uFogColor = 0; private var uFlicker = 0
    private var bVP = 0; private var bCenter = 0; private var bRight = 0; private var bUp = 0
    private var bSize = 0; private var bColor = 0; private var bAlert = 0; private var bAlpha = 0; private var bColorBlind = 0
    private var pScene = 0; private var pTime = 0; private var pFlicker = 0; private var pVhs = 0; private var pRes = 0
    private var pCbMix = 0; private var pCbAxis = 0
    private var sVP = 0; private var sCenter = 0; private var sSize = 0; private var sAlpha = 0

    private var floorTex = 0; private var wallTex = 0; private var roofTex = 0
    private var floorVbo = 0; private var floorIbo = 0; private var floorCount = 0
    private var wallVbo  = 0; private var wallIbo  = 0; private var wallCount  = 0
    private var roofVbo  = 0; private var roofIbo  = 0; private var roofCount = 0
    private var lastSegKey = Int.MIN_VALUE

    private var billboardVbo = 0
    private var postVbo = 0

    private var fbo = 0; private var fboTex = 0; private var fboDepth = 0
    private var surfaceW = 1; private var surfaceH = 1
    private var renderW = 1; private var renderH = 1
    private var lastResScale = -1f

    private val projM = FloatArray(16)
    private val viewM = FloatArray(16)
    private val vpM   = FloatArray(16)
    private val startNanos = System.nanoTime()
    private var lastFrameNanos = 0L

    // Smoothed (rendered) camera state: the sim advances in discrete ~60Hz steps,
    // but the display can refresh faster (90/120Hz). Exponentially chasing the
    // latest snapshot each frame removes visible stepping without adding input lag.
    private var smoothX = 0f; private var smoothY = 1.7f; private var smoothZ = 0f
    private var smoothYaw = 0f; private var smoothPitch = 0f
    private var smoothInit = false
    private val smoothEntities = HashMap<Int, FloatArray>() // id -> [x,y,z]

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // The EGL context is destroyed when the app is backgrounded, so every GL
        // object below is being (re)created from scratch here. Invalidate the
        // cached mesh key too, or the level geometry is never re-uploaded and
        // the screen comes back black with only the HUD drawn over it.
        lastSegKey = Int.MIN_VALUE
        floorCount = 0; wallCount = 0; roofCount = 0
        smoothInit = false
        smoothEntities.clear()

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
        bColorBlind = GLES30.glGetUniformLocation(billboardProgram, "uColorBlind")

        postProgram = linkGlProgram(OMNI_POST_VERT, OMNI_POST_FRAG)
        pScene = GLES30.glGetUniformLocation(postProgram, "uScene")
        pTime = GLES30.glGetUniformLocation(postProgram, "uTime")
        pFlicker = GLES30.glGetUniformLocation(postProgram, "uFlicker")
        pVhs = GLES30.glGetUniformLocation(postProgram, "uVhsStrength")
        pRes = GLES30.glGetUniformLocation(postProgram, "uResolution")
        pCbMix = GLES30.glGetUniformLocation(postProgram, "uColorBlindMix")
        pCbAxis = GLES30.glGetUniformLocation(postProgram, "uColorBlindAxis")

        shadowProgram = linkGlProgram(OMNI_SHADOW_VERT, OMNI_SHADOW_FRAG)
        sVP = GLES30.glGetUniformLocation(shadowProgram, "uVP")
        sCenter = GLES30.glGetUniformLocation(shadowProgram, "uCenter")
        sSize = GLES30.glGetUniformLocation(shadowProgram, "uSize")
        sAlpha = GLES30.glGetUniformLocation(shadowProgram, "uAlpha")

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
        Matrix.perspectiveM(projM, 0, 70f, surfaceW.toFloat()/surfaceH.toFloat(), 0.05f, 55f)
        lastResScale = -1f // force an FBO (re)build on the next frame at the right scale
    }

    override fun onDrawFrame(gl: GL10?) {
        val state = latestState
        val cam = state.camera
        val rs = renderSettings
        val nowNanos = System.nanoTime()
        val timeSec = (nowNanos - startNanos) / 1_000_000_000f
        val dt = if (lastFrameNanos == 0L) 1f / 60f else ((nowNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0.001f, 0.1f)
        lastFrameNanos = nowNanos

        // Quality tier -> concrete render parameters. Every RenderSettings field
        // actually changes what gets drawn; none of it is decorative.
        val fogMult      = when (rs.quality) { "low" -> 1.35f; "high" -> 0.85f; else -> 1.0f }
        val entityRange  = when (rs.quality) { "low" -> 25f; "high" -> 45f; else -> 35f }
        val shadowsOn    = rs.shadowsEnabled && rs.quality != "low"
        val postStrength = when (rs.quality) { "low" -> 0.6f; "high" -> 1.0f; else -> 0.85f }
        val resScale     = rs.resolutionScale.coerceIn(0.5f, 1f)
        val cbAxis = colorBlindAxis(rs.colorBlindMode)
        val cbMix  = if (rs.colorBlindMode == "none") 0f else 0.55f

        if (resScale != lastResScale || renderW <= 1) {
            renderW = max((surfaceW * resScale).toInt(), 1)
            renderH = max((surfaceH * resScale).toInt(), 1)
            rebuildFbo(renderW, renderH)
            lastResScale = resScale
        }

        val segKey = state.levelSegments.size * 73856093 xor (state.levelSegments.firstOrNull()?.posX?.hashCode() ?: 0)
        if (state.levelSegments.isNotEmpty() && segKey != lastSegKey) {
            uploadLevelMesh(state.levelSegments)
            lastSegKey = segKey
        }

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbo)
        GLES30.glViewport(0, 0, renderW, renderH)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        if (cam != null) {
            // Framerate-independent exponential smoothing: the sim advances in
            // discrete ~60Hz steps, the display may refresh faster, so we chase
            // the latest snapshot each frame instead of snapping to it.
            val chase = 1f - kotlin.math.exp(-dt * 22f)
            if (!smoothInit) {
                smoothX = cam.posX; smoothY = cam.posY; smoothZ = cam.posZ
                smoothYaw = cam.yaw; smoothPitch = cam.pitch
                smoothInit = true
            } else {
                smoothX += (cam.posX - smoothX) * chase
                smoothY += (cam.posY - smoothY) * chase
                smoothZ += (cam.posZ - smoothZ) * chase
                smoothYaw += (cam.yaw - smoothYaw) * chase
                smoothPitch += (cam.pitch - smoothPitch) * chase
            }

            val yawRad = Math.toRadians(smoothYaw.toDouble())
            val pitchRad = Math.toRadians(smoothPitch.toDouble())
            val fx = (sin(yawRad) * cos(pitchRad)).toFloat()
            val fy = sin(pitchRad).toFloat()
            val fz = (cos(yawRad) * cos(pitchRad)).toFloat()
            Matrix.setLookAtM(viewM, 0, smoothX, smoothY, smoothZ, smoothX + fx, smoothY + fy, smoothZ + fz, 0f, 1f, 0f)
            Matrix.multiplyMM(vpM, 0, projM, 0, viewM, 0)

            val fogDensity = (if (rs.fogEnabled) 1.0f else 0.15f) * fogMult
            val flicker = state.flickerIntensity.coerceIn(0.35f, 1f)
            drawLevel(vpM, smoothX, smoothY, smoothZ, fx, fy, fz, state.flashlightOn, fogDensity, flicker)

            val activeIds = HashSet<Int>()
            for (e in state.entities) {
                if (!e.isActive) continue
                activeIds.add(e.id)
                val sp = smoothEntities.getOrPut(e.id) { floatArrayOf(e.posX, e.posY, e.posZ) }
                sp[0] += (e.posX - sp[0]) * chase
                sp[1] += (e.posY - sp[1]) * chase
                sp[2] += (e.posZ - sp[2]) * chase
            }
            smoothEntities.keys.retainAll(activeIds)

            if (shadowsOn) drawShadows(vpM, state.entities, smoothX, smoothZ, entityRange)
            drawEntities(vpM, state.entities, yawRad.toFloat(), smoothX, smoothZ, entityRange, timeSec, cbMix)
        }

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, surfaceW, surfaceH)
        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        drawPost(timeSec, state.flickerIntensity, (if (rs.vhsEnabled) 1f else 0f) * postStrength, cbMix, cbAxis)
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

    private fun drawEntities(vp: FloatArray, entities: List<EntityState>, yawRad: Float, camX: Float, camZ: Float, range: Float, timeSec: Float, cbMix: Float) {
        if (entities.isEmpty()) return
        GLES30.glUseProgram(billboardProgram)
        GLES30.glUniformMatrix4fv(bVP, 1, false, vp, 0)
        GLES30.glUniform3f(bRight, -cos(yawRad), 0f, sin(yawRad))
        GLES30.glUniform3f(bUp, 0f, 1f, 0f)
        GLES30.glUniform1f(bColorBlind, cbMix)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, billboardVbo)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 0, 0)
        val rangeSq = range * range
        for (e in entities) {
            if (!e.isActive) continue
            val sp = smoothEntities[e.id] ?: floatArrayOf(e.posX, e.posY, e.posZ)
            val dx = sp[0] - camX; val dz = sp[2] - camZ
            if (dx * dx + dz * dz > rangeSq) continue
            // Subtle idle bob + eye-glow pulse so nothing on screen is ever
            // perfectly static, even when an entity itself is standing still.
            val phase = e.id * 1.7f
            val bob   = sin(timeSec * 2.3f + phase) * 0.06f
            val pulse = 1.0f + sin(timeSec * 3.1f + phase) * 0.05f
            GLES30.glUniform3f(bCenter, sp[0], sp[1] + 1.0f + bob, sp[2])
            GLES30.glUniform1f(bSize, 1.8f * pulse)
            val tint = entityTint(e.typeId)
            GLES30.glUniform3f(bColor, tint.first, tint.second, tint.third)
            GLES30.glUniform1f(bAlert, (e.alertLevel + (if (e.aiState >= 3) 0.5f else 0f)).coerceIn(0f, 1f))
            GLES30.glUniform1f(bAlpha, if (e.playerInSight) 1f else 0.82f)
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        }
        GLES30.glDisableVertexAttribArray(0)
    }

    private fun drawShadows(vp: FloatArray, entities: List<EntityState>, camX: Float, camZ: Float, range: Float) {
        if (entities.isEmpty()) return
        GLES30.glUseProgram(shadowProgram)
        GLES30.glUniformMatrix4fv(sVP, 1, false, vp, 0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, billboardVbo)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 0, 0)
        val rangeSq = range * range
        for (e in entities) {
            if (!e.isActive) continue
            val sp = smoothEntities[e.id] ?: floatArrayOf(e.posX, e.posY, e.posZ)
            val dx = sp[0] - camX; val dz = sp[2] - camZ
            val d2 = dx * dx + dz * dz
            if (d2 > rangeSq) continue
            val fade = 1f - (d2 / rangeSq)
            GLES30.glUniform3f(sCenter, sp[0], 0f, sp[2])
            GLES30.glUniform1f(sSize, 0.85f)
            GLES30.glUniform1f(sAlpha, 0.45f * fade)
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        }
        GLES30.glDisableVertexAttribArray(0)
    }

    private fun drawPost(timeSec: Float, flicker: Float, vhsStrength: Float, cbMix: Float, cbAxis: Triple<Float, Float, Float>) {
        GLES30.glUseProgram(postProgram)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, fboTex)
        GLES30.glUniform1i(pScene, 0)
        GLES30.glUniform1f(pTime, timeSec)
        GLES30.glUniform1f(pFlicker, flicker.coerceIn(0.3f, 1f))
        GLES30.glUniform1f(pVhs, vhsStrength)
        GLES30.glUniform2f(pRes, surfaceW.toFloat(), surfaceH.toFloat())
        GLES30.glUniform1f(pCbMix, cbMix)
        GLES30.glUniform3f(pCbAxis, cbAxis.first, cbAxis.second, cbAxis.third)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, postVbo)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 0, 0)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glDisableVertexAttribArray(0)
    }

    /** Blue/yellow-axis nudge for colorblind modes, applied in the post shader.
     *  Not a full LMS daltonization — a deliberately light touch so the game's
     *  mood doesn't change, but red/green-dependent cues (like entity alert
     *  glow) gain a secondary, colorblind-safe channel to read from. */
    private fun colorBlindAxis(mode: String): Triple<Float, Float, Float> = when (mode) {
        "protanopia", "deuteranopia" -> Triple(0.15f, -0.05f, 0.35f)
        "tritanopia" -> Triple(0.20f, 0.25f, -0.25f)
        else -> Triple(0f, 0f, 0f)
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
private fun BoxScope.NoiseScanlineBottom() {
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
            // Avoids a full GL teardown/rebuild on every backgrounding where the
            // driver supports it; onSurfaceCreated still handles the case where
            // the context really is lost.
            preserveEGLContextOnPause = true
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> { glView.onResume(); vm.onScreenResumed() }
                Lifecycle.Event.ON_PAUSE  -> { glView.onPause();  vm.onScreenPaused() }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            // Also pause when this screen leaves composition (navigating back),
            // not just on Activity pause — otherwise the GL thread stays alive,
            // the next surface comes up black, and the ambience keeps playing.
            glView.onPause()
            vm.onScreenPaused()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { glView }, modifier = Modifier.fillMaxSize())
        CrtScanlineOverlay(0f)
        when {
            state.isGameOver -> GameOverOverlay(state)  { onExit() }
            state.isEscaped  -> EscapedOverlay(state)   { onExit() }
            state.isPaused   -> PauseOverlay(onResume = { vm.togglePause() }, onExit = { onExit() })
            state.spawnPhase != SpawnPhase.READY -> SpawnSequenceOverlay(state.spawnPhase)
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
                    androidx.compose.animation.AnimatedVisibility(
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
            VirtualJoystick(Modifier.size(140.dp), onMove = { dx, dy -> onMove(dx, 0f, -dy) })
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
    var knob by remember { mutableStateOf(Offset.Zero) }
    val knobAnim by animateOffsetAsState(knob, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "joystick")
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(MetalBg.copy(0.30f), MetalBg.copy(0.75f)),
                )
            )
            .border(1.5.dp, YellowDim.copy(0.6f), CircleShape)
            .pointerInput(Unit) {
                // Travel radius derived from the actual laid-out size, so the knob
                // stays inside the ring on every screen density.
                val travel = minOf(size.width, size.height) / 2f * 0.62f
                fun emit(raw: Offset) {
                    val len = kotlin.math.hypot(raw.x, raw.y)
                    val clamped = if (len > travel && len > 0f) raw * (travel / len) else raw
                    knob = clamped
                    val nx = clamped.x / travel
                    val ny = clamped.y / travel
                    val mag = kotlin.math.hypot(nx, ny)
                    if (mag < 0.15f) onMove(0f, 0f) else onMove(nx, ny)
                }
                detectDragGestures(
                    onDragStart  = { pos -> emit(pos - Offset(size.width / 2f, size.height / 2f)) },
                    onDragEnd    = { knob = Offset.Zero; onMove(0f, 0f) },
                    onDragCancel = { knob = Offset.Zero; onMove(0f, 0f) },
                    onDrag       = { change, drag ->
                        change.consume()
                        emit(knob + drag)
                    }
                )
            }
    ) {
        // Direction ticks so the stick reads as a physical control, not a plain dot.
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            for (i in 0 until 4) {
                val ang = (Math.PI / 2 * i).toFloat()
                val ox = center.x + kotlin.math.cos(ang) * r * 0.78f
                val oy = center.y + kotlin.math.sin(ang) * r * 0.78f
                drawCircle(YellowDim.copy(0.35f), radius = r * 0.035f, center = Offset(ox, oy))
            }
            drawCircle(YellowDim.copy(0.18f), radius = r * 0.62f, style = Stroke(1f))
        }
        Box(
            Modifier
                .size(48.dp)
                .offset { IntOffset(knobAnim.x.toInt(), knobAnim.y.toInt()) }
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Yellow, Yellow.copy(0.35f))))
                .border(1.dp, Yellow.copy(0.7f), CircleShape)
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


// ============================================================================
// Lobby chrome. Every glyph below is drawn from vector paths in code — no
// bitmap assets and no stock icon font — so they stay crisp at any density and
// share one visual language with the rest of the UI.
// ============================================================================

private fun DrawScope.strokeW(f: Float = 0.055f) = size.minDimension * f

private fun DrawScope.drawGearGlyph(c: Color) {
    val r = size.minDimension * 0.30f
    val teeth = 8
    val w = strokeW(0.07f)
    for (i in 0 until teeth) {
        val a = (Math.PI * 2 / teeth * i).toFloat()
        val inner = r * 1.02f
        val outer = r * 1.42f
        drawLine(
            c, Offset(center.x + cos(a) * inner, center.y + sin(a) * inner),
            Offset(center.x + cos(a) * outer, center.y + sin(a) * outer),
            strokeWidth = w, cap = StrokeCap.Round
        )
    }
    drawCircle(c, radius = r, center = center, style = Stroke(w))
    drawCircle(c, radius = r * 0.38f, center = center, style = Stroke(w * 0.8f))
}

private fun DrawScope.drawLeaderboardGlyph(c: Color) {
    val w = size.width; val h = size.height
    val barW = w * 0.19f
    val baseY = h * 0.76f
    val heights = listOf(0.30f, 0.46f, 0.22f)
    val xs = listOf(w * 0.24f, w * 0.50f, w * 0.76f)
    heights.forEachIndexed { i, hf ->
        val top = baseY - h * hf
        drawRoundRect(
            c.copy(if (i == 1) 1f else 0.65f),
            topLeft = Offset(xs[i] - barW / 2f, top),
            size = Size(barW, baseY - top),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW * 0.18f)
        )
    }
    drawLine(c, Offset(w * 0.12f, baseY), Offset(w * 0.88f, baseY), strokeWidth = strokeW(0.05f), cap = StrokeCap.Round)
}

private fun DrawScope.drawMarketGlyph(c: Color) {
    val w = size.width; val h = size.height
    val sw = strokeW(0.06f)
    // Bag body
    val left = w * 0.24f; val right = w * 0.76f
    val top = h * 0.38f;  val bottom = h * 0.80f
    drawRoundRect(
        c, topLeft = Offset(left, top), size = Size(right - left, bottom - top),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.06f),
        style = Stroke(sw)
    )
    // Handle
    val path = Path().apply {
        moveTo(w * 0.37f, top)
        cubicTo(w * 0.37f, h * 0.18f, w * 0.63f, h * 0.18f, w * 0.63f, top)
    }
    drawPath(path, c, style = Stroke(sw, cap = StrokeCap.Round))
    drawCircle(c.copy(0.55f), radius = w * 0.035f, center = Offset(w * 0.5f, h * 0.58f))
}

private fun DrawScope.drawBookGlyph(c: Color) {
    val w = size.width; val h = size.height
    val sw = strokeW(0.055f)
    val top = h * 0.24f; val bottom = h * 0.78f
    val spine = w * 0.5f
    val path = Path().apply {
        moveTo(spine, top + h * 0.05f)
        cubicTo(w * 0.36f, top, w * 0.28f, top, w * 0.20f, top + h * 0.03f)
        lineTo(w * 0.20f, bottom - h * 0.03f)
        cubicTo(w * 0.28f, bottom - h * 0.06f, w * 0.38f, bottom - h * 0.05f, spine, bottom)
        cubicTo(w * 0.62f, bottom - h * 0.05f, w * 0.72f, bottom - h * 0.06f, w * 0.80f, bottom - h * 0.03f)
        lineTo(w * 0.80f, top + h * 0.03f)
        cubicTo(w * 0.72f, top, w * 0.64f, top, spine, top + h * 0.05f)
        close()
    }
    drawPath(path, c, style = Stroke(sw))
    drawLine(c.copy(0.7f), Offset(spine, top + h * 0.05f), Offset(spine, bottom), strokeWidth = sw * 0.8f)
}

private fun DrawScope.drawAbilityGlyph(c: Color) {
    val w = size.width; val h = size.height
    val sw = strokeW(0.055f)
    val hub = Offset(w * 0.5f, h * 0.5f)
    val nodes = listOf(
        Offset(w * 0.5f,  h * 0.20f),
        Offset(w * 0.80f, h * 0.62f),
        Offset(w * 0.20f, h * 0.62f)
    )
    nodes.forEach { n ->
        drawLine(c.copy(0.55f), hub, n, strokeWidth = sw * 0.7f, cap = StrokeCap.Round)
        drawCircle(c, radius = w * 0.085f, center = n, style = Stroke(sw * 0.8f))
    }
    drawCircle(c, radius = w * 0.10f, center = hub)
}

private fun DrawScope.drawSeasonGlyph(c: Color) {
    val w = size.width; val h = size.height
    val sw = strokeW(0.055f)
    // Trophy cup
    val path = Path().apply {
        moveTo(w * 0.33f, h * 0.24f)
        lineTo(w * 0.67f, h * 0.24f)
        lineTo(w * 0.63f, h * 0.55f)
        cubicTo(w * 0.60f, h * 0.64f, w * 0.40f, h * 0.64f, w * 0.37f, h * 0.55f)
        close()
    }
    drawPath(path, c, style = Stroke(sw))
    // Handles
    drawArc(c, 90f, 180f, false,
        topLeft = Offset(w * 0.18f, h * 0.26f), size = Size(w * 0.18f, h * 0.20f), style = Stroke(sw * 0.8f))
    drawArc(c, 270f, 180f, false,
        topLeft = Offset(w * 0.64f, h * 0.26f), size = Size(w * 0.18f, h * 0.20f), style = Stroke(sw * 0.8f))
    // Stem + base
    drawLine(c, Offset(w * 0.5f, h * 0.62f), Offset(w * 0.5f, h * 0.74f), strokeWidth = sw)
    drawLine(c, Offset(w * 0.34f, h * 0.78f), Offset(w * 0.66f, h * 0.78f), strokeWidth = sw, cap = StrokeCap.Round)
}

private fun DrawScope.drawOfflineGlyph(c: Color) {
    val w = size.width; val h = size.height
    val sw = strokeW(0.055f)
    // Play triangle inside a ring = start a solo run
    drawCircle(c.copy(0.75f), radius = w * 0.36f, center = center, style = Stroke(sw))
    val tri = Path().apply {
        moveTo(w * 0.42f, h * 0.35f)
        lineTo(w * 0.68f, h * 0.50f)
        lineTo(w * 0.42f, h * 0.65f)
        close()
    }
    drawPath(tri, c)
}

private fun DrawScope.drawOnlineGlyph(c: Color) {
    val w = size.width; val h = size.height
    val sw = strokeW(0.05f)
    drawCircle(c, radius = w * 0.34f, center = center, style = Stroke(sw))
    // Meridian + equator to read as a globe
    drawArc(c.copy(0.8f), 0f, 360f, false,
        topLeft = Offset(w * 0.36f, h * 0.16f), size = Size(w * 0.28f, h * 0.68f), style = Stroke(sw * 0.75f))
    drawLine(c.copy(0.8f), Offset(w * 0.16f, h * 0.5f), Offset(w * 0.84f, h * 0.5f), strokeWidth = sw * 0.75f)
}

private fun DrawScope.drawOmniumGlyph(c: Color) {
    val r = size.minDimension * 0.34f
    drawCircle(c, radius = r, center = center, style = Stroke(size.minDimension * 0.11f))
    drawCircle(c, radius = r * 0.34f, center = center)
}

private fun DrawScope.drawSouliumGlyph(c: Color) {
    val w = size.width; val h = size.height
    val d = Path().apply {
        moveTo(w * 0.5f, h * 0.16f)
        lineTo(w * 0.82f, h * 0.5f)
        lineTo(w * 0.5f, h * 0.84f)
        lineTo(w * 0.18f, h * 0.5f)
        close()
    }
    drawPath(d, c, style = Stroke(size.minDimension * 0.10f))
}

/** Square, bordered icon button whose artwork is a code-drawn vector path. */
@Composable
private fun IconGlyphButton(
    size: Dp,
    accent: Color,
    onClick: () -> Unit,
    glyph: DrawScope.(Color) -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.88f else 1f, spring(), label = "glyphScale")
    Box(
        Modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(9.dp))
            .background(Color.Black.copy(0.45f))
            .border(1.dp, accent.copy(0.45f), RoundedCornerShape(9.dp))
            .clickable(interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize().padding(9.dp)) { glyph(accent) }
    }
}

/** Left-rail entry: code-drawn glyph with its label underneath. */
@Composable
private fun RailItem(
    label: String,
    accent: Color,
    onClick: () -> Unit,
    glyph: DrawScope.(Color) -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, spring(), label = "railScale")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interaction, indication = null, onClick = onClick)
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(0.42f))
                .border(1.dp, accent.copy(0.42f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize().padding(12.dp)) { glyph(accent) }
        }
        Spacer(Modifier.height(3.dp))
        Text(label, color = accent.copy(0.85f), fontSize = 9.sp, letterSpacing = 1.sp)
    }
}

/** Right-edge play button: glyph plus label, sized for a confident tap target. */
@Composable
private fun PlayModeButton(
    label: String,
    accent: Color,
    onClick: () -> Unit,
    glyph: DrawScope.(Color) -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.93f else 1f, spring(), label = "playScale")
    val inf = rememberInfiniteTransition(label = "playGlow")
    val glow by inf.animateFloat(0.35f, 0.75f, infiniteRepeatable(tween(2200, easing = EaseInOut), RepeatMode.Reverse), "glow")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(0.55f))
            .border(1.5.dp, accent.copy(glow), RoundedCornerShape(12.dp))
            .clickable(interaction, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Text(label, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(Modifier.width(9.dp))
        androidx.compose.foundation.Canvas(Modifier.size(26.dp)) { glyph(accent) }
    }
}

/** Circular avatar with the player's level on a badge, drawn in code. */
@Composable
private fun AvatarBadge(level: Int, onClick: () -> Unit) {
    Box(Modifier.size(48.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension * 0.42f
            drawCircle(Color.Black.copy(0.6f), radius = r, center = center)
            drawCircle(Yellow.copy(0.75f), radius = r, center = center, style = Stroke(size.minDimension * 0.045f))
            // Simple head-and-shoulders silhouette
            drawCircle(Yellow.copy(0.85f), radius = r * 0.30f, center = Offset(center.x, center.y - r * 0.22f))
            val body = Path().apply {
                moveTo(center.x - r * 0.48f, center.y + r * 0.60f)
                cubicTo(
                    center.x - r * 0.44f, center.y + r * 0.10f,
                    center.x + r * 0.44f, center.y + r * 0.10f,
                    center.x + r * 0.48f, center.y + r * 0.60f
                )
                close()
            }
            drawPath(body, Yellow.copy(0.85f))
        }
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .clip(RoundedCornerShape(5.dp))
                .background(Color.Black)
                .border(1.dp, CrtAmber, RoundedCornerShape(5.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            Text("$level", color = CrtAmber, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** Currency readout with its own code-drawn symbol. */
@Composable
private fun CurrencyChip(accent: Color, amount: Long, isOmnium: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.foundation.Canvas(Modifier.size(13.dp)) {
            if (isOmnium) drawOmniumGlyph(accent) else drawSouliumGlyph(accent)
        }
        Spacer(Modifier.width(4.dp))
        Text(
            formatCompactAmount(amount),
            color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp
        )
    }
}

private fun formatCompactAmount(v: Long): String = when {
    v >= 1_000_000 -> String.format(Locale.US, "%.1fM", v / 1_000_000.0)
    v >= 1_000     -> String.format(Locale.US, "%.1fK", v / 1_000.0)
    else           -> v.toString()
}


/** Arrival cinematic overlay. The first stretch of the fall is fully black —
 *  the player is "above the world" and there's nothing coherent to show — then
 *  it opens up as they descend through the ceiling. On landing the view drops
 *  to floor level, blinks, and rises back to standing height. */
@Composable
fun SpawnSequenceOverlay(phase: SpawnPhase, modifier: Modifier = Modifier) {
    if (phase == SpawnPhase.READY) return

    val falling = phase == SpawnPhase.FALLING
    // Opaque at the start of the fall, clearing as the player nears the ceiling.
    val veil by animateFloatAsState(
        targetValue   = if (falling) 0f else 1f,
        animationSpec = tween(durationMillis = if (falling) 1100 else 250, easing = EaseOutCubic),
        label         = "spawnVeil"
    )

    // Blink pattern once grounded: two quick lid closures before standing.
    var blink by remember { mutableStateOf(0f) }
    LaunchedEffect(phase) {
        if (phase == SpawnPhase.LANDED) {
            delay(180); blink = 1f; delay(110); blink = 0f
            delay(220); blink = 1f; delay(130); blink = 0f
        }
    }
    val lid by animateFloatAsState(blink, tween(120), label = "blinkLid")

    Box(modifier.fillMaxSize()) {
        // The fall veil.
        if (veil < 1f) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(1f - veil)))
        }
        // Eyelids: two panels closing from top and bottom.
        if (phase == SpawnPhase.LANDED && lid > 0.01f) {
            Box(
                Modifier.fillMaxWidth().fillMaxHeight(0.5f * lid)
                    .align(Alignment.TopCenter).background(Color.Black)
            )
            Box(
                Modifier.fillMaxWidth().fillMaxHeight(0.5f * lid)
                    .align(Alignment.BottomCenter).background(Color.Black)
            )
        }
    }
}


@HiltViewModel
class LobbyVM @Inject constructor(
    private val saveStore: SaveGameStore,
    private val identity : GuestIdentityManager
) : ViewModel() {

    val hasSave: StateFlow<Boolean> = saveStore.observeHasSave()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _guestName = MutableStateFlow("")
    val guestName: StateFlow<String> = _guestName.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            // Also performs the one-week guest expiry check.
            _guestName.value = identity.currentName()
        }
    }

    fun clearSave() { viewModelScope.launch(Dispatchers.IO) { saveStore.clear() } }
}

/** Offline entry point: start fresh, or resume the autosaved run. "Continue"
 *  is visibly disabled with no save present, and says so if tapped anyway. */
@Composable
private fun OfflineChoiceDialog(
    hasSave   : Boolean,
    onNewGame : () -> Unit,
    onContinue: () -> Unit,
    onNoSave  : () -> Unit,
    onDismiss : () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.72f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(PanelBg)
                .border(1.dp, YellowDim, RoundedCornerShape(14.dp))
                .padding(horizontal = 22.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.menu_play_offline),
                color = Yellow, fontSize = 13.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 2.sp
            )
            AtmosphericButton(
                label   = stringResource(R.string.menu_new_run),
                icon    = Icons.Default.PlayArrow,
                accent  = SuccessGreen,
                width   = 220.dp, height = 44.dp,
                onClick = onNewGame,
                isPrimary = true
            )
            AtmosphericButton(
                label   = stringResource(R.string.menu_continue),
                icon    = Icons.Default.Restore,
                accent  = if (hasSave) CrtAmber else TextDim,
                width   = 220.dp, height = 44.dp,
                onClick = { if (hasSave) onContinue() else onNoSave() },
                enabled = true
            )
        }
    }
}
