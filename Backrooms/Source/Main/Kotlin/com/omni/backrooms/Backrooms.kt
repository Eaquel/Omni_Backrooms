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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import kotlinx.coroutines.withContext
import androidx.core.content.ContextCompat
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
import androidx.compose.ui.graphics.drawscope.clipRect
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
import androidx.navigation.navArgument
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
        OmniLog.attach(this)
        installCrashLogger()
        OmniLog.i("App", "onCreate device=${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} sdk=${android.os.Build.VERSION.SDK_INT}")
        runCatching { System.loadLibrary("il2cpp") }
            .onFailure { OmniLog.e("App", "native library load FAILED", it) }
            .onSuccess { OmniLog.i("App", "native library loaded") }
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
            appendLine("----- recent log (oldest first) -----")
            appendLine(OmniLog.recentHistory())
            appendLine("----- stack trace -----")
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

    override fun attachBaseContext(newBase: Context) {
        // Runs before Hilt injection is available, and before any resource is
        // resolved — which is exactly why the locale has to be applied here
        // rather than in onCreate.
        val language = runCatching { LocaleStore(newBase).currentLanguageBlocking() }
            .getOrDefault(AppLanguage.ENGLISH)
        super.attachBaseContext(applyAppLanguage(newBase, language))
    }

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
        NotificationPermissionGate()
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
                    onPlay        = { resume -> nav.navigate("${Route.GAME}?resume=$resume") },
                    onOnline      = { nav.navigate(Route.ROOM) },
                    onSettings    = { nav.navigate(Route.SETTINGS) },
                    onStory       = { nav.navigate(Route.STORY) },
                    onMarket      = { nav.navigate(Route.MARKET) },
                    onLeaderboard = { nav.navigate(Route.LEADERBOARD) },
                    onProfile     = { nav.navigate(Route.PROFILE) }
                )
            }
            composable(
                "${Route.GAME}?resume={resume}",
                arguments = listOf(navArgument("resume") { defaultValue = "false" }),
                enterTransition = { fadeIn(tween(800)) },
                exitTransition  = { fadeOut(tween(500)) }
            ) { entry ->
                GameScreen(
                    onExit = { nav.popBackStack() },
                    resume = entry.arguments?.getString("resume") == "true"
                )
            }
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
            composable(Route.ROOM)        { Room(onJoined = { nav.navigate("${Route.GAME}?resume=false") }, onBack = { nav.popBackStack() }, onCreate = { nav.navigate(Route.CREATE_ROOM) }) }
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

/** Internal intermediate between the bundled per-language story files and the
 *  UI. "Localised" is whichever language was loaded; "source" is the English
 *  original used as a per-chapter fallback. Not a wire type — the server-facing
 *  [StoryChapterDto] keeps its own field names for that reason. */
data class StoryChapterRaw(
    val id                  : Int,
    val titleLocalised      : String,
    val titleSource         : String,
    val unlocked            : Boolean,
    val paragraphsLocalised : List<String>,
    val paragraphsSource    : List<String>
)

/** In-memory result of loading the story. Never serialised — the annotation was
 *  removed along with StoryChapterRaw's, since kotlinx.serialization requires
 *  every nested type to be serialisable and this one is now a plain holder. */
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
    private val storyCacheByLang = mutableMapOf<String, StoryJson>()

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

    /** Loads the story in the given language, falling back to English per-chapter
     *  where a translation isn't present yet. Cached per language, since the
     *  player can switch language and come back. */
    fun loadStory(languageTag: String = Locale.getDefault().language): StoryJson {
        storyCacheByLang[languageTag]?.let { return it }

        fun readMono(name: String): StoryFileMono? =
            runCatching { ctx.assets.open(name).bufferedReader().readText() }
                .mapCatching { json.decodeFromString<StoryFileMono>(it) }
                .getOrNull()

        val fallback = readMono("Story/en.json")
        // "en" would just re-read the fallback; anything else gets its own file.
        val localised = if (languageTag == "en") null else readMono("Story/$languageTag.json")

        val byIdFallback = fallback?.chapters?.associateBy { it.id } ?: emptyMap()
        val byIdLocal    = localised?.chapters?.associateBy { it.id } ?: emptyMap()
        val ids = (byIdFallback.keys + byIdLocal.keys).sorted()

        val merged = ids.map { id ->
            val f = byIdFallback[id]; val l = byIdLocal[id]
            // titleTr/paragraphsTr carry the *localised* text and titleEn/
            // paragraphsEn the English original; the display extensions in
            // Service.kt pick between them by locale.
            StoryChapterRaw(
                id                  = id,
                titleLocalised      = l?.title.takeUnless { it.isNullOrBlank() } ?: f?.title.orEmpty(),
                titleSource         = f?.title.takeUnless { it.isNullOrBlank() } ?: l?.title.orEmpty(),
                unlocked            = l?.unlocked ?: f?.unlocked ?: false,
                paragraphsLocalised = l?.paragraphs?.takeIf { it.isNotEmpty() } ?: f?.paragraphs.orEmpty(),
                paragraphsSource    = f?.paragraphs?.takeIf { it.isNotEmpty() } ?: l?.paragraphs.orEmpty()
            )
        }
        if (localised == null && languageTag != "en") {
            OmniLog.i("Story", "no localised story for '$languageTag'; using English")
        }
        return StoryJson(version = 1, chapters = merged).also { storyCacheByLang[languageTag] = it }
    }

    /** Maps to the wire type. The Dto's `Tr`/`En` field names are fixed by the
     *  server's JSON contract (see ApiService.getStoryChapters), so the mapping
     *  is: localised text -> the `Tr` slot, English source -> the `En` slot. */
    fun storyChapterToDto(raw: StoryChapterRaw): StoryChapterDto = StoryChapterDto(
        id        = raw.id,
        titleTr   = raw.titleLocalised,
        titleEn   = raw.titleSource,
        contentTr = raw.paragraphsLocalised.joinToString("\n\n"),
        contentEn = raw.paragraphsSource.joinToString("\n\n"),
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

        // Log every individual signal, always. Previously a warning appeared with
        // no way to tell which check caused it.
        OmniLog.i(
            "Guard",
            "scan flags=0x${Integer.toHexString(flags)} rooted=$rooted frida=$frida " +
            "debugged=$debugged emulator=$emulator sigCheckOn=$sigCheckOn sigValid=$sigValid " +
            "hook=$hook memTamper=$memTamper native='$reportStr'"
        )

        // `debugged` is deliberately NOT a threat: it trips on ordinary retail
        // devices whenever a debugger could attach (developer options enabled,
        // some vendor ROMs), and it used to escalate to CRITICAL — which killed
        // the process outright. It stays in the log as an observation only.
        // Emulators are a supported platform here, so running in one is logged
        // but never treated as a threat. Same reasoning as `debugged` above:
        // punishing an ordinary environment just breaks legitimate players.
        val level = when {
            frida || hook                       -> ThreatLevel.CRITICAL
            rooted || (sigCheckOn && !sigValid) -> ThreatLevel.HIGH
            memTamper                           -> ThreatLevel.HIGH
            flags != 0                          -> ThreatLevel.SUSPICIOUS
            else                                -> ThreatLevel.CLEAN
        }
        if (level != ThreatLevel.CLEAN) {
            val reasons = buildList {
                if (frida) add("frida")
                if (hook) add("hook")
                if (rooted) add("root")
                if (sigCheckOn && !sigValid) add("signature")
                if (memTamper) add("memory")
                if (emulator) add("emulator(allowed)")
                if (flags != 0) add("nativeFlags=0x${Integer.toHexString(flags)}")
            }
            OmniLog.w("Guard", "threat level=$level reasons=${reasons.joinToString(",")}")
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

/** Cosmetic-only storefront. There is deliberately no tab that sells power:
 *  nothing purchasable may change HP, speed, stamina, sanity drain or spawn
 *  rates, so buying is never a shortcut past the game. */
enum class MarketTab(val labelRes: Int, val icon: ImageVector) {
    Frames    (R.string.market_tab_frames,     Icons.Default.CropSquare),
    Looks     (R.string.market_tab_looks,      Icons.Default.Person),
    Trails    (R.string.market_tab_trails,     Icons.Default.AutoAwesome),
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
    val tab         : MarketTab           = MarketTab.Frames,
    val omniumBal   : Long                = 0L,
    val souliumBal  : Long                = 0L,
    val isVip       : Boolean             = false,
    val confirmItem : MarketItemDto?      = null,
    val characters  : List<CharacterDto>  = emptyList(),
    val selectedChar: CharacterDto?       = null,
    val charsLoading: Boolean             = false,
    val equipping   : String?             = null,
    /** Locally-owned item ids, so cards can show "Owned" immediately after a
     *  purchase without waiting on a server round-trip. */
    val ownedIds    : Set<String>         = emptySet()
)

@HiltViewModel
class MarketVM @Inject constructor(
    private val api         : ApiService,
    private val assetManager: AssetManager,
    private val cosmetics   : CosmeticsStore,
    @ApplicationContext private val appCtx: Context
) : ViewModel() {
    private val _state = MutableStateFlow(MarketUiState())
    val state: StateFlow<MarketUiState> = _state.asStateFlow()

    init {
        loadTab(MarketTab.Frames); loadDaily(); loadProfile()
        viewModelScope.launch {
            cosmetics.observeOwnedFrames().collect { frames ->
                _state.update { it.copy(ownedIds = frames.map { f -> "frame_$f" }.toSet()) }
            }
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            runCatching { api.getProfile() }.onSuccess { p ->
                _state.update { it.copy(omniumBal = p.omniumAmount, souliumBal = p.souliumAmount, isVip = p.isVip) }
            }
        }
    }

    fun setTab(tab: MarketTab) {
        _state.update { it.copy(tab = tab) }
        when (tab) { MarketTab.Looks -> loadCharacters(); MarketTab.Daily -> return; else -> loadTab(tab) }
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

            // Grant locally first. Cosmetics are client-side by design and
            // everything is free in this phase, so a purchase must succeed even
            // with no server reachable — previously the item was never unlocked
            // because the only path went through an API that isn't running yet.
            grantLocally(item)

            // Then tell the server, best-effort. A failure here is logged but
            // must not undo what the player already owns.
            runCatching { api.buyItem(BuyRequest(item.id, item.currency)) }
                .onSuccess { r ->
                    _state.update {
                        it.copy(
                            omniumBal  = if (item.currency == "omnium") r.newBalance else it.omniumBal,
                            souliumBal = if (item.currency == "soulium") r.newBalance else it.souliumBal
                        )
                    }
                }
                .onFailure { e -> OmniLog.w("Market", "server sync failed for ${item.id}", e) }

            _state.update {
                it.copy(
                    purchasing = null,
                    ownedIds   = it.ownedIds + item.id,
                    successMsg = item.id
                )
            }
            logPurchaseAnalytics(item)
        }
    }

    /** Applies the purchase client-side: frames become equippable (and are
     *  equipped straight away, which is what a player expects after buying one),
     *  and the privileges bundle unlocks every cosmetic at once. */
    private suspend fun grantLocally(item: MarketItemDto) {
        runCatching {
            when {
                item.id.startsWith("frame_") -> {
                    val key = item.id.removePrefix("frame_")
                    cosmetics.grantFrame(key)
                    cosmetics.setFrame(key)
                }
                item.id.startsWith("priv_") || item.category == "vip" -> {
                    listOf("gold", "soulium", "omnium", "event").forEach { cosmetics.grantFrame(it) }
                }
                item.id == "daily_frame" -> cosmetics.grantFrame("event")
                else -> Unit
            }
        }.onFailure { OmniLog.e("Market", "local grant failed for ${item.id}", it) }
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

    /** Offline catalogue. Every entry is purely visual by design — no stat
     *  changes, no consumables, nothing that alters difficulty. */
    private fun fallbackItems(tab: MarketTab): List<MarketItemDto> = when (tab) {
        // Everything is free during this phase: prices are zero and nothing is
        // gated. The currency plumbing stays in place for later.
        MarketTab.Frames -> listOf(
            MarketItemDto("frame_gold","Altın Çerçeve","Gold Frame","Profil fotoğrafını saran altın halka","A gold ring around your avatar","frames",0,"soulium",null,false,false,false,null),
            MarketItemDto("frame_soulium","Soulium Çerçeve","Soulium Frame","Mor kristal düğümlü çerçeve","Frame studded with violet crystal","frames",0,"soulium",null,false,false,false,null),
            MarketItemDto("frame_omnium","Omnium Çerçeve","Omnium Frame","Dönen camgöbeği yay","Rotating cyan arc","frames",0,"omnium",null,false,false,false,null),
            MarketItemDto("frame_event","Etkinlik Çerçevesi","Event Frame","Kırmızı dikenli etkinlik halkası","Red-spiked event ring","frames",0,"soulium",null,false,false,true,null)
        )
        MarketTab.Looks -> listOf(
            MarketItemDto(
                "char_anime", "Anime Kız", "Anime Girl",
                "Ana karakter — görsele dokunup inceleyebilirsin",
                "The main character — tap the art to inspect her",
                "characters", 0, "soulium", null, false, false, true, null
            )
        )
        MarketTab.Trails -> listOf(
            MarketItemDto("trail_dust","Toz İzi","Dust Trail","Arkanda asılı kalan ince toz","Fine dust hanging behind you","trails",0,"soulium",null,false,false,false,null),
            MarketItemDto("trail_static","Statik İz","Static Trail","VHS parazit izi","VHS static wake","trails",0,"soulium",null,false,false,false,null)
        )
        MarketTab.Vip -> listOf(
            MarketItemDto("priv_all","Tüm Ayrıcalıklar","All Privileges","Şu an ücretsiz — tüm görsel ayrıcalıklar açık","Free right now — every cosmetic privilege unlocked","vip",0,"soulium",null,false,false,true,null)
        )
        else -> emptyList()
    }

    private fun fallbackDaily(): List<MarketItemDto> = listOf(
        MarketItemDto("daily_frame","Günlük Çerçeve","Daily Frame","Bugüne özel görsel çerçeve","Today only cosmetic frame","daily",0,"soulium",null,false,false,true,null)
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
    private val saveStore   : SaveGameStore,
    private val cosmetics   : CosmeticsStore
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
    /** Master volume from Settings. The engine was previously fed hardcoded
     *  0.4/0.3 levels, so the volume slider did nothing. */
    @Volatile private var cachedVolume = 0.7f
    /** Guards against startGame running twice (re-entering the screen quickly),
     *  which would spawn a second physics loop advancing the same native sim. */
    private var started = false

    private companion object {
        /** Sized against the engine's 80 kg body and drag 8 so terminal walking
         *  speed lands near 3.6 m/s. force = speed * mass * drag. */
        const val MOVE_FORCE = 2_300f
    }

    /** Grid for the currently loaded level; kept here (not just in GameState) so the
     *  entity spawner can reuse it without depending on StateFlow emission timing. */
    private var grid: GridLevelData = GridLevelData.EMPTY

    /** [resume] = true continues the autosaved run. The level is regenerated from
     *  the saved seed, which reproduces it exactly, and the saved stats/timer are
     *  restored — previously "Continue" silently started a brand new run because
     *  the snapshot was written but never read back. */
    fun startGame(difficulty: String = "normal", seed: Long = System.currentTimeMillis(), resume: Boolean = false) {
        if (started) return
        started = true
        viewModelScope.launch {
            val saved = if (resume) saveStore.load() else null
            val useSeed = saved?.seed ?: seed
            val useDiff = saved?.difficulty ?: difficulty
            if (resume && saved == null) OmniLog.w("Game", "resume requested but no save found; starting fresh")
            val sensitivity = settings.observe().first().cameraSensitivity
            bridge.initCore(useSeed)
            bridge.initSound()
            bridge.initEntities()
            applyAudioLevels()
            bridge.setSpatialRolloff(1f, 40f)

            // Level 0 always — there is deliberately no map selection.
            val roomBudget = if (useDiff == "hard") 180 else 130
            grid = GridLevelData.parse(bridge.generateLevel(roomBudget, depth = 0))
            OmniLog.i("Game", "level dim=${grid.dim} cell=${grid.cellSize} exit=(${grid.exitX},${grid.exitZ})")

            val cfg = assetManager.getSpawnConfig(useDiff)
            spawnInitialEntities(bridge, grid, cfg)

            // Restore counters before the loops start reading them.
            elapsedMs = saved?.elapsedMs ?: 0L
            score     = saved?.score ?: 0L
            kills     = saved?.kills ?: 0

            // A resumed run puts the player exactly where they left off and
            // skips the arrival cinematic — they already landed once.
            if (saved != null) {
                bridge.setPlayerState(saved.posX, saved.posY, saved.posZ, saved.yaw, saved.pitch)
                OmniLog.i("Game", "resumed at (${saved.posX}, ${saved.posZ}) yaw=${saved.yaw}")
            }
            val base = GameState(
                seed = useSeed, difficulty = useDiff, mapId = "level_0",
                grid = grid, exitX = grid.exitX, exitZ = grid.exitZ,
                spawnPhase = if (saved != null) SpawnPhase.READY else SpawnPhase.FALLING
            )
            _state.value = if (saved != null) base.copy(
                sanity = saved.sanity,
                flashlightBattery = saved.battery,
                playerHp = saved.playerHp,
                score = saved.score,
                kills = saved.kills,
                sessionElapsed = saved.elapsedMs
            ) else base
            startPhysicsLoop(sensitivity)
            startEntitySpawner(useDiff, cfg)
            startScoreAccumulator()
            startAutosave()
            if (saved == null) playSpawnDrop()
        }
    }

    /** Scales the engine's ambience and hum by the player's master volume. */
    private fun applyAudioLevels() {
        val v = cachedVolume.coerceIn(0f, 1f)
        runCatching {
            bridge.setAmbienceLevel(0.55f * v)
            bridge.setHumVolume(0.42f * v)
        }
    }

    private fun startPhysicsLoop(sensitivity: Float) {
        cachedSensitivity = sensitivity
        // Keep following the settings so changes apply without restarting a run.
        viewModelScope.launch {
            settings.observe().collect { g ->
                cachedSensitivity = g.cameraSensitivity.coerceAtLeast(0.05f)
                if (g.musicVolume != cachedVolume) {
                    cachedVolume = g.musicVolume
                    if (!_state.value.isPaused) applyAudioLevels()
                }
            }
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

                // Real RTT from the netcode; 0 offline, where there is nothing
                // to measure and the badge is hidden.
                pingSampleTimer -= dt
                if (pingSampleTimer <= 0f) {
                    pingSampleTimer = 1f
                    val rtt = runCatching { bridge.getLocalPing() }.getOrDefault(0)
                    if (rtt != _state.value.pingMs) _state.update { it.copy(pingMs = rtt) }
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
                if (timer >= cfg.spawnIntervalMs && !grid.isEmpty) {
                    timer = 0
                    spawnOneRandomEntity(bridge, grid, cfg)
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
    private var pingSampleTimer = 0f

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

    /** Pushes the renderer's measured frame rate into game state so the HUD can
     *  show a real number. Called from the composition, which already has the
     *  renderer instance. */
    fun reportFps(fps: Float) {
        val rounded = fps.roundToInt()
        if (rounded != _state.value.fps) _state.update { it.copy(fps = rounded) }
    }

    fun onScreenResumed() {
        applyAudioLevels()
        _state.update { it.copy(isPaused = false) }
    }

    /** Writes a resumable snapshot. The level itself isn't stored — it's fully
     *  reproducible from the seed — so this stays small enough to run on a timer
     *  without hitching the game loop. */
    private fun saveNow() {
        val s = _state.value
        if (s.isGameOver || s.isEscaped) return
        if (s.grid.isEmpty) return   // nothing meaningful to resume yet
        // Detached on purpose: this is called while the screen is being torn
        // down, and a viewModelScope coroutine would be cancelled mid-write.
        val cam = s.camera
        saveStore.saveDetached(
            SavedRun(
                seed = s.seed, difficulty = s.difficulty, elapsedMs = elapsedMs,
                score = score, kills = kills, sanity = s.sanity,
                battery = s.flashlightBattery, playerHp = s.playerHp,
                savedAtMs = System.currentTimeMillis(),
                posX = cam?.posX ?: 0f, posY = cam?.posY ?: 1.7f, posZ = cam?.posZ ?: 0f,
                yaw = cam?.yaw ?: 0f, pitch = cam?.pitch ?: 0f
            )
        )
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
            _state.update { it.copy(spawnPhase = SpawnPhase.FALLING, eyeOffset = 0f) }
            // The engine's gravity does the actual falling; input stays locked
            // and the camera rides the body down from its elevated start.
            var waited = 0L
            while (waited < 4000 && _state.value.camera?.let { it.posY > 2.2f } != false) {
                delay(50); waited += 50
            }

            // Impact: the view drops to floor height, as if the body collapsed.
            _state.update { it.copy(spawnPhase = SpawnPhase.LANDED, eyeOffset = -1.45f) }
            runCatching { bridge.triggerFootstep(60f, 1.0f) }
            delay(650)

            // Then push back up to standing over roughly a second. Stepped
            // rather than a single jump so the rise is visibly gradual.
            val steps = 26
            for (i in 1..steps) {
                val t = i / steps.toFloat()
                // Ease-out: fast at first, settling near the top.
                val eased = 1f - (1f - t) * (1f - t)
                _state.update { it.copy(eyeOffset = -1.45f * (1f - eased)) }
                delay(38)
            }
            _state.update { it.copy(spawnPhase = SpawnPhase.READY, eyeOffset = 0f) }
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
            // Personal best is only meaningful for a completed run.
            runCatching { cosmetics.recordSurvival(elapsedMs) }
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
class ProfileVM @Inject constructor(
    private val api      : ApiService,
    private val cosmetics: CosmeticsStore,
    private val identity : GuestIdentityManager
) : ViewModel() {
    private val _profile = MutableStateFlow(PlayerProfile())
    val profile: StateFlow<PlayerProfile> = _profile.asStateFlow()

    val localAvatarUri : StateFlow<String?> = cosmetics.observeAvatarUri()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val equippedFrame  : StateFlow<String>  = cosmetics.observeFrame()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "default")
    val bestSurvivalMs : StateFlow<Long>    = cosmetics.observeBestSurvival()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    private var owned: List<String> = emptyList()

    init {
        viewModelScope.launch {
            // Server profile is best-effort; a guest with no account still gets a
            // usable name rather than the placeholder "Wanderer".
            runCatching { api.getProfile() }
                .onSuccess { _profile.value = it }
                .onFailure { OmniLog.w("Profile", "getProfile failed, using local identity", it) }
            if (_profile.value.name.isBlank() || _profile.value.name == "Wanderer") {
                _profile.value = _profile.value.copy(name = identity.currentName())
            }
        }
        viewModelScope.launch { cosmetics.observeOwnedFrames().collect { owned = it } }
    }

    fun ownedFrames(): List<String> = owned
    fun setLocalAvatar(uri: String) { viewModelScope.launch { cosmetics.setAvatarUri(uri) } }
    fun equipFrame(key: String)     { viewModelScope.launch { cosmetics.setFrame(key) } }
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
    onPlay       : (Boolean) -> Unit,
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
    val displayName by lobbyVm.displayName.collectAsState()
    val avatarUri by lobbyVm.avatarUri.collectAsState()
    val frame by lobbyVm.frame.collectAsState()
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
            LobbyAvatar(level = profile.level, frame = frame, localUri = avatarUri, onClick = onProfile)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    displayName.takeIf { it.isNotBlank() } ?: profile.name,
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
        // Anchored below the identity header rather than vertically centred: on
        // shorter screens centring pushed the rail up into the profile block.
        // Sized to fit without scrolling: four compact tiles plus spacing stays
        // inside the space below the identity header on a short screen.
        Column(
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = 10.dp, top = 76.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            RailItem(stringResource(R.string.menu_market),    CrtAmber,     onMarket)  { drawMarketGlyph(it) }
            RailItem(stringResource(R.string.menu_story),      Yellow,       onStory)   { drawBookGlyph(it) }
            RailItem(stringResource(R.string.menu_abilities),  TextSec,      { toast = comingSoon }) { drawAbilityGlyph(it) }
            RailItem(stringResource(R.string.menu_season),     SouliumCol,   { toast = comingSoon }) { drawSeasonGlyph(it) }
        }

        // ---- Right edge: play modes -------------------------------------------
        Column(
            Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 18.dp),
            verticalArrangement   = Arrangement.spacedBy(12.dp),
            horizontalAlignment   = Alignment.End
        ) {
            PremiumEventButton(
                label   = stringResource(R.string.menu_play_offline),
                accent  = SuccessGreen,
                onClick = { showOfflineChoice = true },
                modifier = Modifier.width(226.dp),
                glyph   = { drawOfflineGlyph(it) }
            )
            PremiumEventButton(
                label   = stringResource(R.string.menu_play_online),
                accent  = OmniumCol,
                onClick = onOnline,
                modifier = Modifier.width(226.dp),
                glyph   = { drawOnlineGlyph(it) }
            )
        }

        if (showOfflineChoice) {
            OfflineChoiceDialog(
                hasSave     = hasSave,
                onNewGame   = { showOfflineChoice = false; lobbyVm.clearSave(); onPlay(false) },
                onContinue  = { showOfflineChoice = false; onPlay(true) },
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

    /** Measured on the GL thread and read by the HUD. Exponentially smoothed so
     *  the number is readable instead of flickering every frame. */
    @Volatile var measuredFps: Float = 0f
        private set
    private var fpsAccum = 0f

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

    // Character model + its own program. Null when the asset is missing, in
    // which case entities fall back to the billboard path.
    private var charProgram = 0
    private var charVbo = 0; private var charIbo = 0; private var charIndexCount = 0
    private var charTex = 0
    private var cMVP = 0; private var cModel = 0; private var cTime = 0; private var cWalk = 0
    private var cHeight = 0; private var cTexU = 0; private var cCamPos = 0
    private var cFlashDir = 0; private var cFlashOn = 0; private var cFogD = 0
    private var cFogCol = 0; private var cAmbient = 0
    private val charModelM = FloatArray(16)
    private val charMvpM = FloatArray(16)

    private var fbo = 0; private var fboTex = 0; private var fboDepth = 0
    private var surfaceW = 1; private var surfaceH = 1
    private var renderW = 1; private var renderH = 1
    private var lastResScale = -1f

    /** World height of a character in metres. The mesh is normalised to 1.0. */
    private val CHAR_SCALE = 1.75f

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

        // Character: program, geometry and texture. Any failure leaves
        // charIndexCount at 0 and the renderer simply skips it.
        runCatching {
            charProgram = linkGlProgram(OMNI_CHAR_VERT, OMNI_CHAR_FRAG)
            cMVP = GLES30.glGetUniformLocation(charProgram, "uMVP")
            cModel = GLES30.glGetUniformLocation(charProgram, "uModel")
            cTime = GLES30.glGetUniformLocation(charProgram, "uTime")
            cWalk = GLES30.glGetUniformLocation(charProgram, "uWalk")
            cHeight = GLES30.glGetUniformLocation(charProgram, "uHeight")
            cTexU = GLES30.glGetUniformLocation(charProgram, "uTex")
            cCamPos = GLES30.glGetUniformLocation(charProgram, "uCamPos")
            cFlashDir = GLES30.glGetUniformLocation(charProgram, "uFlashDir")
            cFlashOn = GLES30.glGetUniformLocation(charProgram, "uFlashOn")
            cFogD = GLES30.glGetUniformLocation(charProgram, "uFogDensity")
            cFogCol = GLES30.glGetUniformLocation(charProgram, "uFogColor")
            cAmbient = GLES30.glGetUniformLocation(charProgram, "uAmbient")

            val mesh = CharacterMesh.load(appContext, "character.omesh")
            if (mesh != null) {
                charVbo = genGlBuffer(); charIbo = genGlBuffer()
                GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, charVbo)
                GLES30.glBufferData(
                    GLES30.GL_ARRAY_BUFFER, mesh.vertexBuffer.size * 4,
                    glFloatBuffer(mesh.vertexBuffer), GLES30.GL_STATIC_DRAW
                )
                val ib = ByteBuffer.allocateDirect(mesh.indices.size * 2)
                    .order(ByteOrder.nativeOrder()).asShortBuffer()
                ib.put(mesh.indices); ib.position(0)
                GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, charIbo)
                GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, mesh.indices.size * 2, ib, GLES30.GL_STATIC_DRAW)
                charIndexCount = mesh.indices.size
            }
            charTex = loadOmniTexture("character_texture.png", 0xFFE8D5C8.toInt())
        }.onFailure { OmniLog.e("Render", "character setup failed; falling back to billboards", it) }

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
        // Real frame-rate measurement (the HUD used to print a hardcoded 60).
        val instantFps = 1f / dt
        fpsAccum = if (fpsAccum == 0f) instantFps else fpsAccum + (instantFps - fpsAccum) * 0.08f
        measuredFps = fpsAccum

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

        val g = state.grid
        val segKey = g.dim * 73856093 xor g.spawnX.hashCode() xor g.exitZ.hashCode()
        if (!g.isEmpty && segKey != lastSegKey) {
            uploadLevelMesh(g)
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
            // Arrival sequence offsets the eye height (collapse then stand up).
            val eyeY = smoothY + state.eyeOffset
            Matrix.setLookAtM(viewM, 0, smoothX, eyeY, smoothZ, smoothX + fx, eyeY + fy, smoothZ + fz, 0f, 1f, 0f)
            Matrix.multiplyMM(vpM, 0, projM, 0, viewM, 0)

            val fogDensity = (if (rs.fogEnabled) 1.0f else 0.15f) * fogMult
            val flicker = state.flickerIntensity.coerceIn(0.35f, 1f)
            drawLevel(vpM, smoothX, eyeY, smoothZ, fx, fy, fz, state.flashlightOn, fogDensity, flicker)

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
            if (charIndexCount > 0) {
                drawCharacters(
                    vpM, state.entities, smoothX, eyeY, smoothZ, fx, fy, fz,
                    state.flashlightOn, fogDensity, entityRange, timeSec
                )
            } else {
                drawEntities(vpM, state.entities, yawRad.toFloat(), smoothX, smoothZ, entityRange, timeSec, cbMix)
            }
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


    /** Draws entities as full 3D characters when the model is available, falling
     *  back to the billboard path when it isn't. Each entity gets its own walk
     *  weight from its AI state, so a chasing creature actually strides. */
    private fun drawCharacters(
        vp: FloatArray, entities: List<EntityState>, camX: Float, camY: Float, camZ: Float,
        fx: Float, fy: Float, fz: Float, flashOn: Boolean, fogDensity: Float,
        range: Float, timeSec: Float
    ) {
        if (charIndexCount <= 0 || entities.isEmpty()) return
        GLES30.glUseProgram(charProgram)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, charTex)
        GLES30.glUniform1i(cTexU, 0)
        GLES30.glUniform3f(cCamPos, camX, camY, camZ)
        GLES30.glUniform3f(cFlashDir, fx, fy, fz)
        GLES30.glUniform1f(cFlashOn, if (flashOn) 1f else 0f)
        GLES30.glUniform1f(cFogD, fogDensity)
        GLES30.glUniform3f(cFogCol, 0.05f, 0.045f, 0.03f)
        GLES30.glUniform1f(cHeight, 1.0f)   // mesh is normalised to unit height

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, charVbo)
        val stride = CharacterMesh.FLOATS_PER_VERTEX * 4
        GLES30.glEnableVertexAttribArray(0); GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
        GLES30.glEnableVertexAttribArray(1); GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride, 3 * 4)
        GLES30.glEnableVertexAttribArray(2); GLES30.glVertexAttribPointer(2, 2, GLES30.GL_FLOAT, false, stride, 6 * 4)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, charIbo)

        val rangeSq = range * range
        for (e in entities) {
            if (!e.isActive) continue
            val sp = smoothEntities[e.id] ?: floatArrayOf(e.posX, e.posY, e.posZ)
            val dx = sp[0] - camX; val dz = sp[2] - camZ
            val d2 = dx * dx + dz * dz
            if (d2 > rangeSq) continue

            // Face the player. atan2 of the offset gives the yaw directly in the
            // engine's (sin, cos) forward convention.
            val yawDeg = Math.toDegrees(kotlin.math.atan2(dx.toDouble(), dz.toDouble())).toFloat()
            Matrix.setIdentityM(charModelM, 0)
            Matrix.translateM(charModelM, 0, sp[0], sp[1], sp[2])
            Matrix.rotateM(charModelM, 0, yawDeg + 180f, 0f, 1f, 0f)
            Matrix.scaleM(charModelM, 0, CHAR_SCALE, CHAR_SCALE, CHAR_SCALE)
            Matrix.multiplyMM(charMvpM, 0, vp, 0, charModelM, 0)

            GLES30.glUniformMatrix4fv(cMVP, 1, false, charMvpM, 0)
            GLES30.glUniformMatrix4fv(cModel, 1, false, charModelM, 0)
            // Per-entity time offset stops a crowd moving in lockstep.
            GLES30.glUniform1f(cTime, timeSec + e.id * 0.83f)
            // Chase/attack states stride; idle and wander only breathe.
            GLES30.glUniform1f(cWalk, if (e.aiState >= 2) 1f else 0.15f)
            GLES30.glUniform1f(cAmbient, if (e.playerInSight) 0.95f else 0.75f)
            GLES30.glDrawElements(GLES30.GL_TRIANGLES, charIndexCount, GLES30.GL_UNSIGNED_SHORT, 0)
        }
        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
        GLES30.glDisableVertexAttribArray(2)
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

    /** Builds the level mesh from the occupancy grid. Two things this fixes over
     *  the old segment mesh: walls are emitted only on open/solid boundaries, so
     *  there is no seam anywhere for a black gap to show through; and UVs are
     *  offset per cell from a hash, so the same small texture no longer reads as
     *  one obviously repeating pattern across the whole floor. */
    private fun uploadLevelMesh(g: GridLevelData) {
        val floorV = ArrayList<Float>(); val floorI = ArrayList<Int>(); var floorB = 0
        val wallV  = ArrayList<Float>(); val wallI  = ArrayList<Int>(); var wallB  = 0
        val roofV  = ArrayList<Float>(); val roofI  = ArrayList<Int>(); var roofB  = 0

        fun quad(
            verts: ArrayList<Float>, idx: ArrayList<Int>, base: Int,
            p0: FloatArray, p1: FloatArray, p2: FloatArray, p3: FloatArray,
            n: FloatArray, light: Float, u0: Float, v0: Float, u1: Float, v1: Float
        ): Int {
            val pts = arrayOf(p0, p1, p2, p3)
            val uvs = floatArrayOf(u0, v0, u1, v0, u1, v1, u0, v1)
            for (k in 0 until 4) {
                verts.add(pts[k][0]); verts.add(pts[k][1]); verts.add(pts[k][2])
                verts.add(n[0]); verts.add(n[1]); verts.add(n[2])
                verts.add(uvs[k * 2]); verts.add(uvs[k * 2 + 1])
                verts.add(light)
            }
            idx.add(base); idx.add(base + 1); idx.add(base + 2)
            idx.add(base); idx.add(base + 2); idx.add(base + 3)
            return base + 4
        }

        // World-space UV scale. Deriving UVs from world position (rather than
        // restarting them per cell) is what makes the surface continuous: the
        // texture flows across cell boundaries instead of resetting at each one.
        val uvPerMetre = 0.5f

        // Lighting zone -> baked vertex brightness. Zone 0 is genuinely dark,
        // so those stretches are only visible by flashlight.
        fun zoneLight(z: Int): Float = when (z) {
            0 -> 0.05f
            1 -> 0.42f
            2 -> 0.78f
            else -> 1.05f
        }

        val cs = g.cellSize
        val hgt = g.height

        for (cz in 0 until g.dim) {
            for (cx in 0 until g.dim) {
                if (g.isSolid(cx, cz)) continue
                val x0 = g.worldX(cx); val x1 = x0 + cs
                val z0 = g.worldZ(cz); val z1 = z0 + cs
                val lit = zoneLight(g.zoneAt(cx, cz))
                val feature = g.featureAt(cx, cz)
                // Continuous, world-anchored texture coordinates.
                val u0 = x0 * uvPerMetre; val u1 = x1 * uvPerMetre
                val v0 = z0 * uvPerMetre; val v1 = z1 * uvPerMetre
                val wallV0 = 0f;          val wallV1 = hgt * uvPerMetre

                // Floor (skipped for a hole feature, which becomes a pit)
                if (feature != 4) {
                    floorB = quad(
                        floorV, floorI, floorB,
                        floatArrayOf(x0, 0f, z0), floatArrayOf(x1, 0f, z0),
                        floatArrayOf(x1, 0f, z1), floatArrayOf(x0, 0f, z1),
                        floatArrayOf(0f, 1f, 0f), lit,
                        u0, v0, u1, v1
                    )
                }

                // Ceiling — a doorway threshold gets none, which reads as a gap
                // in the ceiling grid and adds vertical variety.
                if (feature != 1) {
                    roofB = quad(
                        roofV, roofI, roofB,
                        floatArrayOf(x0, hgt, z0), floatArrayOf(x0, hgt, z1),
                        floatArrayOf(x1, hgt, z1), floatArrayOf(x1, hgt, z0),
                        floatArrayOf(0f, -1f, 0f), lit * 1.25f,
                        u0, v0, u1, v1
                    )
                }

                // Walls: one quad per solid neighbour, facing inward. Emitting
                // only on boundaries is what removes the old black gaps.
                val wallLit = lit * 0.85f
                if (g.isSolid(cx - 1, cz)) {
                    wallB = quad(
                        wallV, wallI, wallB,
                        floatArrayOf(x0, 0f, z1), floatArrayOf(x0, 0f, z0),
                        floatArrayOf(x0, hgt, z0), floatArrayOf(x0, hgt, z1),
                        floatArrayOf(1f, 0f, 0f), wallLit, v1, wallV0, v0, wallV1
                    )
                }
                if (g.isSolid(cx + 1, cz)) {
                    wallB = quad(
                        wallV, wallI, wallB,
                        floatArrayOf(x1, 0f, z0), floatArrayOf(x1, 0f, z1),
                        floatArrayOf(x1, hgt, z1), floatArrayOf(x1, hgt, z0),
                        floatArrayOf(-1f, 0f, 0f), wallLit, v0, wallV0, v1, wallV1
                    )
                }
                if (g.isSolid(cx, cz - 1)) {
                    wallB = quad(
                        wallV, wallI, wallB,
                        floatArrayOf(x0, 0f, z0), floatArrayOf(x1, 0f, z0),
                        floatArrayOf(x1, hgt, z0), floatArrayOf(x0, hgt, z0),
                        floatArrayOf(0f, 0f, 1f), wallLit, u0, wallV0, u1, wallV1
                    )
                }
                if (g.isSolid(cx, cz + 1)) {
                    wallB = quad(
                        wallV, wallI, wallB,
                        floatArrayOf(x1, 0f, z1), floatArrayOf(x0, 0f, z1),
                        floatArrayOf(x0, hgt, z1), floatArrayOf(x1, hgt, z1),
                        floatArrayOf(0f, 0f, -1f), wallLit, u1, wallV0, u0, wallV1
                    )
                }
            }
        }

        floorCount = uploadMeshBuffers(floorVbo, floorIbo, floorV, floorI)
        roofCount  = uploadMeshBuffers(roofVbo,  roofIbo,  roofV,  roofI)
        wallCount  = uploadMeshBuffers(wallVbo,  wallIbo,  wallV,  wallI)
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
fun GameScreen(onExit: () -> Unit, resume: Boolean = false, vm: GameVM = hiltViewModel(), settingsVm: SettingsVM = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val settingsState by settingsVm.state.collectAsState()
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { vm.startGame(resume = resume) }

    val renderer = remember { OmniGLRenderer(ctx.applicationContext) }
    LaunchedEffect(state) { renderer.latestState = state }
    // Sampled twice a second: often enough to feel live, rare enough not to
    // trigger a recomposition storm.
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            vm.reportFps(renderer.measuredFps)
        }
    }
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
    var inspecting by remember { mutableStateOf(false) }
    LaunchedEffect(s.successMsg) { if (s.successMsg != null) { delay(2000); vm.clearSuccess() } }

    // The inspection scene takes over the whole screen; it needs the space and
    // shouldn't fight the store chrome for attention.
    if (inspecting) {
        CharacterPreviewSheet(onClose = { inspecting = false })
        return
    }

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
                    CurrencyBadge(s.omniumBal,  OmniumCol,  isOmnium = true)
                    CurrencyBadge(s.souliumBal, SouliumCol, isOmnium = false)
                }
                if (s.isVip) VipBadge()
            }
            // States the store's promise plainly, so nobody has to guess whether
            // paying makes the game easier.
            Text(
                stringResource(R.string.market_cosmetic_note),
                color = TextDim, fontSize = 9.sp, lineHeight = 12.sp,
                modifier = Modifier.fillMaxWidth()
                    .background(Color.Black.copy(0.35f))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
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
                    MarketTab.Looks -> {
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
                            items(s.dailyDeals) { item ->
                                MarketCard(item, s.purchasing == item.id, item.id in s.ownedIds,
                                    onInspect = { inspecting = true }) { vm.confirmBuy(item) }
                            }
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
                                items(s.items) { item ->
                                    MarketCard(item, s.purchasing == item.id, item.id in s.ownedIds,
                                        onInspect = { inspecting = true }) { vm.confirmBuy(item) }
                                }
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
    val localAvatar by vm.localAvatarUri.collectAsState()
    val equippedFrame by vm.equippedFrame.collectAsState()
    val bestSurvivalMs by vm.bestSurvivalMs.collectAsState()
    var showFramePicker by remember { mutableStateOf(false) }

    // The modern photo picker needs no storage permission at all, which is both
    // the Play-recommended approach and far less intrusive than READ_MEDIA_IMAGES.
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            vm.setLocalAvatar(uri.toString())
            OmniLog.i("Profile", "avatar picked")
        }
    }

    Box(Modifier.fillMaxSize().background(DarkBg)) {
        CrtScanlineOverlay(0f)
        Column(Modifier.fillMaxSize()) {
            TopBarBack(stringResource(R.string.menu_profile), onBack)
            DividerLine()
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // --- Identity: framed avatar, name, level and XP -------------
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FramedAvatar(
                        frame = equippedFrame,
                        localUri = localAvatar,
                        size = 84.dp,
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            profile.name, color = Yellow, fontSize = 16.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${stringResource(R.string.player_level_prefix)}${profile.level}",
                            color = CrtAmber, fontSize = 11.sp
                        )
                        Spacer(Modifier.height(5.dp))
                        // XP bar sits directly under the level, as requested.
                        XpBar(progress = profile.xpProgress, xp = profile.xp, xpToNext = profile.xpToNext)
                    }
                }

                DividerLine()

                // --- The three things the profile actually shows -------------
                ProfileActionRow(
                    label = stringResource(R.string.profile_frame),
                    value = frameDisplayName(equippedFrame),
                    accent = SouliumCol,
                    onClick = { showFramePicker = true }
                ) { drawFrameGlyph(it) }

                ProfileActionRow(
                    label = stringResource(R.string.profile_change_photo),
                    value = if (localAvatar != null) "✓" else "—",
                    accent = OmniumCol,
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                ) { drawCameraGlyph(it) }

                ProfileActionRow(
                    label = stringResource(R.string.profile_best_survival),
                    value = formatDuration(bestSurvivalMs),
                    accent = SuccessGreen,
                    onClick = null
                ) { drawStopwatchGlyph(it) }
            }
        }

        if (showFramePicker) {
            FramePickerSheet(
                current = equippedFrame,
                owned = vm.ownedFrames(),
                onPick = { vm.equipFrame(it); showFramePicker = false },
                onDismiss = { showFramePicker = false }
            )
        }
    }
}

/** Level progress bar. Drawn by hand for the same reason the HUD bars are:
 *  Material's indicator inserts a gap and a stop cap that look broken here. */
@Composable
private fun XpBar(progress: Float, xp: Long, xpToNext: Long) {
    val anim by animateFloatAsState(progress.coerceIn(0f, 1f), tween(400, easing = EaseOutCubic), label = "xp")
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(7.dp)) {
            val r = size.height / 2f
            val corner = androidx.compose.ui.geometry.CornerRadius(r)
            drawRoundRect(MetalBg, size = size, cornerRadius = corner)
            drawRoundRect(CrtAmber.copy(0.22f), size = size, cornerRadius = corner, style = Stroke(1f))
            val w = size.width * anim
            if (w > 0.5f) {
                clipRect(right = w) {
                    drawRoundRect(
                        Brush.horizontalGradient(listOf(CrtAmber.copy(0.65f), CrtAmber)),
                        size = size, cornerRadius = corner
                    )
                }
            }
        }
        Text("$xp / $xpToNext XP", color = TextDim, fontSize = 9.sp)
    }
}

/** Avatar with an equippable decorative frame drawn in code. */
@Composable
private fun FramedAvatar(frame: String, localUri: String?, size: Dp, onClick: () -> Unit) {
    Box(Modifier.size(size).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val r = this.size.minDimension * 0.36f
            drawCircle(Color.Black.copy(0.65f), radius = r, center = center)
            // Silhouette placeholder; a picked photo is drawn over it below.
            drawCircle(Yellow.copy(0.8f), radius = r * 0.30f, center = Offset(center.x, center.y - r * 0.22f))
            val body = Path().apply {
                moveTo(center.x - r * 0.48f, center.y + r * 0.60f)
                cubicTo(
                    center.x - r * 0.44f, center.y + r * 0.10f,
                    center.x + r * 0.44f, center.y + r * 0.10f,
                    center.x + r * 0.48f, center.y + r * 0.60f
                )
                close()
            }
            drawPath(body, Yellow.copy(0.8f))
            drawFrameRing(frame, r)
        }
        // Decoded directly rather than via an image-loading library: it's one
        // small avatar, so pulling in a whole dependency for it isn't warranted.
        val ctx = LocalContext.current
        val bmp by produceState<ImageBitmap?>(null, localUri) {
            value = localUri?.let { uriStr ->
                withContext(Dispatchers.IO) {
                    runCatching {
                        ctx.contentResolver.openInputStream(Uri.parse(uriStr))?.use { stream ->
                            BitmapFactory.decodeStream(stream)?.asImageBitmap()
                        }
                    }.getOrNull()
                }
            }
        }
        bmp?.let { image ->
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(0.66f).clip(CircleShape)
            )
        }
    }
}

/** Frame styles are pure decoration — deliberately no gameplay effect. */
private fun DrawScope.drawFrameRing(frame: String, r: Float) {
    val w = size.minDimension * 0.035f
    when (frame) {
        "gold" -> {
            drawCircle(CrtAmber, radius = r * 1.16f, center = center, style = Stroke(w * 1.5f))
            drawCircle(CrtAmber.copy(0.45f), radius = r * 1.28f, center = center, style = Stroke(w * 0.6f))
        }
        "soulium" -> {
            drawCircle(SouliumCol, radius = r * 1.16f, center = center, style = Stroke(w * 1.3f))
            for (i in 0 until 8) {
                val a = (Math.PI * 2 / 8 * i).toFloat()
                drawCircle(
                    SouliumCol.copy(0.8f), radius = w * 0.9f,
                    center = Offset(center.x + cos(a) * r * 1.30f, center.y + sin(a) * r * 1.30f)
                )
            }
        }
        "omnium" -> {
            drawCircle(OmniumCol, radius = r * 1.16f, center = center, style = Stroke(w * 1.3f))
            drawArc(
                OmniumCol.copy(0.55f), -40f, 260f, false,
                topLeft = Offset(center.x - r * 1.30f, center.y - r * 1.30f),
                size = Size(r * 2.60f, r * 2.60f), style = Stroke(w * 0.7f)
            )
        }
        "event" -> {
            drawCircle(DangerRed.copy(0.85f), radius = r * 1.16f, center = center, style = Stroke(w * 1.4f))
            for (i in 0 until 3) {
                val a = (Math.PI * 2 / 3 * i - Math.PI / 2).toFloat()
                val p = Path().apply {
                    moveTo(center.x + cos(a) * r * 1.34f, center.y + sin(a) * r * 1.34f)
                    lineTo(center.x + cos(a + 0.22f) * r * 1.16f, center.y + sin(a + 0.22f) * r * 1.16f)
                    lineTo(center.x + cos(a - 0.22f) * r * 1.16f, center.y + sin(a - 0.22f) * r * 1.16f)
                    close()
                }
                drawPath(p, DangerRed.copy(0.85f))
            }
        }
        else -> drawCircle(BorderCol, radius = r * 1.14f, center = center, style = Stroke(w))
    }
}

private fun DrawScope.drawFrameGlyph(c: Color) {
    val w = size.width; val h = size.height
    val sw = size.minDimension * 0.07f
    drawRoundRect(
        c, topLeft = Offset(w * 0.18f, h * 0.18f), size = Size(w * 0.64f, h * 0.64f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f), style = Stroke(sw)
    )
    drawCircle(c.copy(0.6f), radius = w * 0.13f, center = center)
}

private fun DrawScope.drawCameraGlyph(c: Color) {
    val w = size.width; val h = size.height
    val sw = size.minDimension * 0.065f
    drawRoundRect(
        c, topLeft = Offset(w * 0.14f, h * 0.30f), size = Size(w * 0.72f, h * 0.44f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.07f), style = Stroke(sw)
    )
    drawCircle(c, radius = w * 0.14f, center = Offset(w * 0.5f, h * 0.52f), style = Stroke(sw * 0.85f))
    drawLine(c, Offset(w * 0.36f, h * 0.30f), Offset(w * 0.44f, h * 0.22f), strokeWidth = sw, cap = StrokeCap.Round)
    drawLine(c, Offset(w * 0.64f, h * 0.30f), Offset(w * 0.56f, h * 0.22f), strokeWidth = sw, cap = StrokeCap.Round)
}

private fun DrawScope.drawStopwatchGlyph(c: Color) {
    val w = size.width; val h = size.height
    val sw = size.minDimension * 0.065f
    drawCircle(c, radius = w * 0.30f, center = Offset(w * 0.5f, h * 0.56f), style = Stroke(sw))
    drawLine(c, Offset(w * 0.5f, h * 0.56f), Offset(w * 0.5f, h * 0.36f), strokeWidth = sw, cap = StrokeCap.Round)
    drawLine(c, Offset(w * 0.5f, h * 0.56f), Offset(w * 0.64f, h * 0.60f), strokeWidth = sw * 0.85f, cap = StrokeCap.Round)
    drawLine(c, Offset(w * 0.42f, h * 0.20f), Offset(w * 0.58f, h * 0.20f), strokeWidth = sw, cap = StrokeCap.Round)
}

@Composable
private fun ProfileActionRow(
    label: String,
    value: String,
    accent: Color,
    onClick: (() -> Unit)?,
    glyph: DrawScope.(Color) -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && onClick != null) 0.98f else 1f, spring(), label = "prow")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(6.dp))
            .background(MetalBg)
            .border(1.dp, accent.copy(0.35f), RoundedCornerShape(6.dp))
            .then(
                if (onClick != null) Modifier.clickable(interaction, indication = null, onClick = onClick)
                else Modifier
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        androidx.compose.foundation.Canvas(Modifier.size(22.dp)) { glyph(accent) }
        Spacer(Modifier.width(12.dp))
        Text(label, color = TextSec, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(value, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        if (onClick != null) {
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = accent.copy(0.55f), modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun FramePickerSheet(
    current: String,
    owned: List<String>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val all = listOf("default", "gold", "soulium", "omnium", "event")
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.75f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(PanelBg)
                .border(1.dp, YellowDim, RoundedCornerShape(14.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.profile_frame),
                color = Yellow, fontSize = 13.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 2.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                all.forEach { f ->
                    val unlocked = f == "default" || owned.contains(f)
                    val sel = f == current
                    Box(
                        Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (sel) Yellow.copy(0.12f) else MetalBg)
                            .border(1.dp, if (sel) Yellow else BorderCol, RoundedCornerShape(8.dp))
                            .clickable(enabled = unlocked) { onPick(f) },
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Canvas(Modifier.fillMaxSize().padding(8.dp)) {
                            drawFrameRing(f, size.minDimension * 0.28f)
                            if (!unlocked) {
                                drawLine(
                                    TextDim, Offset(size.width * 0.2f, size.height * 0.8f),
                                    Offset(size.width * 0.8f, size.height * 0.2f), strokeWidth = 2f
                                )
                            }
                        }
                    }
                }
            }
            Text(
                stringResource(R.string.profile_frame_locked),
                color = TextDim, fontSize = 9.sp, textAlign = TextAlign.Center
            )
        }
    }
}

private fun frameDisplayName(key: String): String = when (key) {
    "gold" -> "Gold"
    "soulium" -> "Soulium"
    "omnium" -> "Omnium"
    "event" -> "Event"
    else -> "Default"
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "—"
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%02d:%02d", m, s)
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
    val animProgress by animateFloatAsState(
        progress.coerceIn(0f, 1f),
        tween(280, easing = EaseOutCubic),
        label = "status"
    )
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = TextDim, fontSize = 9.sp, letterSpacing = 1.sp)
            Text("${(animProgress * 100).roundToInt()}", color = color.copy(0.75f), fontSize = 9.sp)
        }
        androidx.compose.foundation.Canvas(
            Modifier.fillMaxWidth().height(6.dp)
        ) {
            val r = size.height / 2f
            val corner = androidx.compose.ui.geometry.CornerRadius(r)
            // Track
            drawRoundRect(MetalBg, size = size, cornerRadius = corner)
            drawRoundRect(
                color.copy(0.20f), size = size, cornerRadius = corner,
                style = Stroke(1f)
            )
            // Fill — a single continuous rounded rect, no gaps, no stop indicator.
            val w = size.width * animProgress
            if (w > 0.5f) {
                clipRect(right = w) {
                    drawRoundRect(color, size = size, cornerRadius = corner)
                }
                // Leading-edge glow so movement is readable at a glance.
                drawCircle(
                    color.copy(0.55f),
                    radius = r * 1.15f,
                    center = Offset(w.coerceIn(r, size.width - r), r)
                )
            }
        }
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
        // Look surface first, so every control laid out after it takes priority.
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

        // --- Top-left: vitals only. HP is gone; these three are the ones the
        // player can actually act on. ---------------------------------------
        Column(
            Modifier.align(Alignment.TopStart).padding(start = 14.dp, top = 14.dp).width(150.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            StatusBar(stringResource(R.string.game_hud_sanity),  gameState.sanity / 100f,                  SouliumCol)
            StatusBar(stringResource(R.string.game_hud_stamina), gameState.stamina / gameState.staminaMax, SuccessGreen)
            StatusBar(stringResource(R.string.game_hud_battery), gameState.flashlightBattery,              CrtAmber)
        }

        // --- Top-right: session readouts and pause -------------------------
        Row(
            Modifier.align(Alignment.TopEnd).padding(end = 10.dp, top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            HudBadge(formatDuration(gameState.sessionElapsed), TextSec)
            if (gameState.entitiesNearby > 0) {
                HudBadge("◉ ${gameState.entitiesNearby}", DangerRed)
            }
            if (gameState.showPing && gameState.pingMs > 0) {
                val pingColor = when {
                    gameState.pingMs < 80  -> SuccessGreen
                    gameState.pingMs < 180 -> CrtAmber
                    else                   -> DangerRed
                }
                HudBadge("${gameState.pingMs} ms", pingColor)
            }
            if (gameState.showFps) {
                val fpsColor = when {
                    gameState.fps >= 50 -> SuccessGreen
                    gameState.fps >= 30 -> CrtAmber
                    else                -> DangerRed
                }
                HudBadge("${gameState.fps} FPS", fpsColor)
            }
            IconGlyphButton(34.dp, Yellow.copy(0.8f), onClick = onPause) { drawPauseGlyph(it) }
        }

        // --- Exit proximity prompt -----------------------------------------
        androidx.compose.animation.AnimatedVisibility(
            visible = canEscape,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 58.dp),
            enter = fadeIn(), exit = fadeOut()
        ) {
            HudBadge(stringResource(R.string.game_hud_exit_near), SuccessGreen)
        }

        // --- Bottom-left: movement ------------------------------------------
        Box(Modifier.align(Alignment.BottomStart).padding(start = 18.dp, bottom = 22.dp)) {
            VirtualJoystick(Modifier.size(140.dp), onMove = { dx, dy -> onMove(dx, 0f, -dy) })
        }

        // --- Bottom-right: actions, arranged as a thumb-reachable cluster ---
        // Jump sits highest and interact closest to the thumb, since interact is
        // the most-used action and jump the least.
        Box(Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 18.dp)) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HudActionButton(46.dp, TextSec, onCrouch) { drawCrouchGlyph(it) }
                    HudActionButton(46.dp, Yellow,  onJump)   { drawJumpGlyph(it) }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    HudActionButton(
                        52.dp,
                        if (gameState.flashlightOn) CrtAmber else TextDim,
                        onFlash
                    ) { drawFlashlightGlyph(it) }
                    HudActionButton(
                        62.dp,
                        if (canEscape) SuccessGreen else TextSec,
                        onInteract,
                        emphasised = canEscape
                    ) { drawInteractGlyph(it) }
                }
            }
        }
    }
}

/** Round in-game action button. Larger and higher-contrast than the menu glyph
 *  buttons because it has to be hit reliably while something is chasing you. */
@Composable
private fun HudActionButton(
    size: Dp,
    accent: Color,
    onClick: () -> Unit,
    emphasised: Boolean = false,
    glyph: DrawScope.(Color) -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.86f else 1f, spring(), label = "hudBtn")
    val inf = rememberInfiniteTransition(label = "hudPulse")
    val pulse by inf.animateFloat(
        0.45f, 0.9f,
        infiniteRepeatable(tween(1200, easing = EaseInOut), RepeatMode.Reverse),
        "hudPulseV"
    )
    val ringAlpha = if (emphasised) pulse else 0.5f
    Box(
        Modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(Color.Black.copy(0.5f))
            .border(1.5.dp, accent.copy(ringAlpha), CircleShape)
            .clickable(interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize().padding(size * 0.24f)) { glyph(accent) }
    }
}

private fun DrawScope.drawPauseGlyph(c: Color) {
    val w = size.width; val h = size.height
    val barW = w * 0.18f
    drawRoundRect(c, topLeft = Offset(w * 0.28f - barW / 2, h * 0.18f), size = Size(barW, h * 0.64f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW * 0.3f))
    drawRoundRect(c, topLeft = Offset(w * 0.72f - barW / 2, h * 0.18f), size = Size(barW, h * 0.64f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW * 0.3f))
}

private fun DrawScope.drawFlashlightGlyph(c: Color) {
    val w = size.width; val h = size.height
    val sw = size.minDimension * 0.09f
    // Torch body
    val body = Path().apply {
        moveTo(w * 0.38f, h * 0.16f); lineTo(w * 0.62f, h * 0.16f)
        lineTo(w * 0.58f, h * 0.44f); lineTo(w * 0.42f, h * 0.44f); close()
    }
    drawPath(body, c, style = Stroke(sw))
    // Beam
    val beam = Path().apply {
        moveTo(w * 0.42f, h * 0.46f); lineTo(w * 0.58f, h * 0.46f)
        lineTo(w * 0.80f, h * 0.92f); lineTo(w * 0.20f, h * 0.92f); close()
    }
    drawPath(beam, c.copy(0.28f))
    drawPath(beam, c.copy(0.8f), style = Stroke(sw * 0.7f))
}

private fun DrawScope.drawInteractGlyph(c: Color) {
    val w = size.width; val h = size.height
    val sw = size.minDimension * 0.085f
    // A hand reaching out — reads as "use" better than a generic arrow.
    drawRoundRect(
        c, topLeft = Offset(w * 0.34f, h * 0.42f), size = Size(w * 0.32f, h * 0.40f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.10f), style = Stroke(sw)
    )
    for (i in 0 until 3) {
        val x = w * (0.40f + i * 0.10f)
        drawLine(c, Offset(x, h * 0.42f), Offset(x, h * 0.20f), strokeWidth = sw * 0.9f, cap = StrokeCap.Round)
    }
    drawLine(c, Offset(w * 0.34f, h * 0.56f), Offset(w * 0.20f, h * 0.48f), strokeWidth = sw * 0.9f, cap = StrokeCap.Round)
}

private fun DrawScope.drawJumpGlyph(c: Color) {
    val w = size.width; val h = size.height
    val sw = size.minDimension * 0.10f
    drawLine(c, Offset(w * 0.5f, h * 0.78f), Offset(w * 0.5f, h * 0.26f), strokeWidth = sw, cap = StrokeCap.Round)
    val head = Path().apply {
        moveTo(w * 0.5f, h * 0.14f); lineTo(w * 0.72f, h * 0.40f); lineTo(w * 0.28f, h * 0.40f); close()
    }
    drawPath(head, c)
    drawLine(c.copy(0.45f), Offset(w * 0.26f, h * 0.88f), Offset(w * 0.74f, h * 0.88f), strokeWidth = sw * 0.8f, cap = StrokeCap.Round)
}

private fun DrawScope.drawCrouchGlyph(c: Color) {
    val w = size.width; val h = size.height
    val sw = size.minDimension * 0.10f
    drawLine(c, Offset(w * 0.5f, h * 0.22f), Offset(w * 0.5f, h * 0.74f), strokeWidth = sw, cap = StrokeCap.Round)
    val head = Path().apply {
        moveTo(w * 0.5f, h * 0.86f); lineTo(w * 0.72f, h * 0.60f); lineTo(w * 0.28f, h * 0.60f); close()
    }
    drawPath(head, c)
    drawLine(c.copy(0.45f), Offset(w * 0.26f, h * 0.12f), Offset(w * 0.74f, h * 0.12f), strokeWidth = sw * 0.8f, cap = StrokeCap.Round)
}

@Composable
private fun HudBadge(text: String, color: Color) {
    Box(
        Modifier.clip(RoundedCornerShape(2.dp)).background(MetalBg.copy(0.8f)).padding(horizontal = 6.dp, vertical = 3.dp)
    ) { Text(text, color = color, fontSize = 10.sp) }
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
fun PauseOverlay(onResume: () -> Unit, onExit: () -> Unit, settingsVm: SettingsVM = hiltViewModel()) {
    var showSettings by remember { mutableStateOf(false) }
    val s by settingsVm.state.collectAsState()

    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.78f)), Alignment.Center) {
        androidx.compose.animation.AnimatedContent(
            targetState = showSettings,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
            label = "pausePanel"
        ) { inSettings ->
            if (!inSettings) {
                Column(
                    Modifier.width(268.dp).clip(RoundedCornerShape(10.dp))
                        .background(MetalBg)
                        .border(1.dp, BorderCol, RoundedCornerShape(10.dp))
                        .padding(26.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stringResource(R.string.game_paused), color = Yellow, fontSize = 20.sp,
                        fontWeight = FontWeight.Black, letterSpacing = 4.sp
                    )
                    DividerLine()
                    AtmosphericButton(stringResource(R.string.game_resume),    Icons.Default.PlayArrow, Yellow,    200.dp, 48.dp, onResume)
                    AtmosphericButton(stringResource(R.string.menu_settings),  Icons.Default.Settings,  CrtAmber,  200.dp, 48.dp, { showSettings = true })
                    AtmosphericButton(stringResource(R.string.game_exit_menu), Icons.Default.ExitToApp, DangerRed, 200.dp, 48.dp, onExit)
                }
            } else {
                // Only the settings that make sense to change without leaving a
                // run: look/feel and audio. Anything needing a restart stays in
                // the main settings screen.
                Column(
                    Modifier.width(300.dp).clip(RoundedCornerShape(10.dp))
                        .background(MetalBg)
                        .border(1.dp, CrtAmber.copy(0.5f), RoundedCornerShape(10.dp))
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        stringResource(R.string.menu_settings), color = CrtAmber, fontSize = 13.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 3.sp
                    )
                    DividerLine()
                    InGameSlider(stringResource(R.string.controls_camera_sensitivity), s.cameraSensitivity, 0.1f, 4f, settingsVm::onSensitivity)
                    InGameSlider(stringResource(R.string.audio_master_volume),  s.musicVolume,       0f,   1f, settingsVm::onMusic)
                    InGameSlider(stringResource(R.string.graphics_resolution_scale),  s.resolutionScale,   0.5f, 1f, settingsVm::onResolution)
                    InGameToggle(stringResource(R.string.graphics_fog),      s.fogEnabled,     settingsVm::onFog)
                    InGameToggle(stringResource(R.string.graphics_shadows),  s.shadowsEnabled, settingsVm::onShadows)
                    InGameToggle(stringResource(R.string.graphics_vhs_effect),      s.vhsEnabled,     settingsVm::onVhs)
                    InGameToggle(stringResource(R.string.graphics_show_fps),      s.showFps,        settingsVm::onShowFps)
                    InGameToggle(stringResource(R.string.graphics_show_ping),     s.showPing,       settingsVm::onShowPing)
                    DividerLine()
                    AtmosphericButton(stringResource(R.string.common_ok), Icons.Default.Check, Yellow, 240.dp, 44.dp, { showSettings = false })
                }
            }
        }
    }
}

/** Compact slider for the in-run settings panel. */
@Composable
private fun InGameSlider(label: String, value: Float, from: Float, to: Float, onChange: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text(label, color = TextSec, fontSize = 11.sp)
            Text(String.format(Locale.US, "%.2f", value), color = Yellow, fontSize = 11.sp)
        }
        Slider(
            value = value.coerceIn(from, to),
            onValueChange = onChange,
            valueRange = from..to,
            colors = SliderDefaults.colors(
                thumbColor = Yellow, activeTrackColor = Yellow.copy(0.75f), inactiveTrackColor = MetalBg
            )
        )
    }
}

@Composable
private fun InGameToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextSec, fontSize = 11.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Yellow, checkedTrackColor = Yellow.copy(0.35f),
                uncheckedThumbColor = TextDim, uncheckedTrackColor = MetalBg
            )
        )
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
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Codex first: the story screen now explains how the game
                    // actually works, not only its fiction.
                    item {
                        Text(
                            stringResource(R.string.story_codex_title),
                            color = CrtAmber, fontSize = 12.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = 3.sp
                        )
                    }
                    item { CodexEntry(R.string.story_codex_level,    R.string.story_codex_level_body,    Yellow)      { drawBookGlyph(it) } }
                    item { CodexEntry(R.string.story_codex_survival, R.string.story_codex_survival_body, SouliumCol)  { drawStopwatchGlyph(it) } }
                    item { CodexEntry(R.string.story_codex_entities, R.string.story_codex_entities_body, DangerRed)   { drawAbilityGlyph(it) } }
                    item { CodexEntry(R.string.story_codex_exit,     R.string.story_codex_exit_body,     SuccessGreen){ drawOnlineGlyph(it) } }
                    item { Spacer(Modifier.height(6.dp)) }
                    item {
                        Text(
                            stringResource(R.string.story_chapters_header),
                            color = CrtAmber, fontSize = 12.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = 3.sp
                        )
                    }
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
private fun MarketCard(
    item: MarketItemDto,
    isPurchasing: Boolean,
    owned: Boolean = false,
    onInspect: () -> Unit = {},
    onBuy: () -> Unit
) {
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
        // Item artwork, drawn from the item's own id so every entry has a
        // distinct picture rather than one shared placeholder icon.
        val artPulse by inf.animateFloat(
            0.95f, 1.05f,
            infiniteRepeatable(tween(2100, easing = EaseInOut), RepeatMode.Reverse),
            "cardArt"
        )
        val inspectable = item.category == "characters"
        Box(
            Modifier
                .size(62.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.radialGradient(listOf(currencyColor.copy(0.22f), Color.Black.copy(0.45f)))
                )
                .border(
                    if (inspectable) 1.5.dp else 1.dp,
                    currencyColor.copy(if (inspectable) 0.65f else 0.30f),
                    RoundedCornerShape(8.dp)
                )
                .then(if (inspectable) Modifier.clickable { onInspect() } else Modifier),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Canvas(
                Modifier.fillMaxSize().padding(9.dp)
                    .graphicsLayer { scaleX = artPulse; scaleY = artPulse }
            ) { marketItemArt(item.id, item.category, currencyColor) }
            // Small cue that this particular art opens a full 3D inspection.
            if (inspectable) {
                androidx.compose.foundation.Canvas(
                    Modifier.size(15.dp).align(Alignment.BottomEnd).padding(1.dp)
                ) {
                    drawCircle(Color.Black.copy(0.75f), radius = size.minDimension * 0.5f, center = center)
                    val r = size.minDimension * 0.26f
                    drawCircle(currencyColor, radius = r, center = center, style = Stroke(1.4f))
                    drawLine(
                        currencyColor,
                        Offset(center.x + r * 0.7f, center.y + r * 0.7f),
                        Offset(center.x + r * 1.5f, center.y + r * 1.5f),
                        strokeWidth = 1.6f, cap = StrokeCap.Round
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(item.nameTr, color = Yellow, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
            textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(item.descTr, color = TextDim, fontSize = 9.sp, textAlign = TextAlign.Center, maxLines = 2,
            overflow = TextOverflow.Ellipsis, lineHeight = 13.sp, modifier = Modifier.padding(top = 3.dp))
        Spacer(Modifier.height(10.dp))
        // `owned` covers items unlocked locally this session; item.isOwned is the
        // server's view, which lags behind (or is absent entirely).
        if (item.isOwned || owned) {
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
private fun CurrencyBadge(amount: Long, color: Color, isOmnium: Boolean) {
    val inf = rememberInfiniteTransition(label = "curShimmer")
    val shimmer by inf.animateFloat(
        0.55f, 1f,
        infiniteRepeatable(tween(2400, easing = EaseInOut), RepeatMode.Reverse),
        "curShimmerV"
    )
    Row(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.horizontalGradient(listOf(MetalBg, color.copy(0.10f)))
            )
            .border(1.dp, color.copy(0.30f * shimmer + 0.15f), RoundedCornerShape(6.dp))
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // Identical glyph to the lobby wallet, so the same currency never
        // appears with two different symbols.
        androidx.compose.foundation.Canvas(Modifier.size(13.dp)) {
            if (isOmnium) drawOmniumGlyph(color.copy(shimmer)) else drawSouliumGlyph(color.copy(shimmer))
        }
        Text(
            formatCompactAmount(amount),
            color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold
        )
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
        val inf = rememberInfiniteTransition(label = "railGlow")
        val ring by inf.animateFloat(
            0.30f, 0.62f,
            infiniteRepeatable(tween(2800, easing = EaseInOut), RepeatMode.Reverse),
            "railRing"
        )
        val lift by animateFloatAsState(if (pressed) 2.5f else 0f, spring(), label = "railLift")
        Box(
            Modifier
                .size(44.dp)
                .graphicsLayer { translationY = lift }
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(accent.copy(0.14f), Color.Black.copy(0.55f))
                    )
                )
                .border(1.dp, accent.copy(if (pressed) 0.9f else ring), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize().padding(10.dp)) { glyph(accent) }
        }
        Spacer(Modifier.height(2.dp))
        Text(label, color = accent.copy(0.85f), fontSize = 8.sp, letterSpacing = 0.5.sp, maxLines = 1)
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
    private val identity : GuestIdentityManager,
    private val cosmetics: CosmeticsStore
) : ViewModel() {

    val hasSave: StateFlow<Boolean> = saveStore.observeHasSave()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    // Same sources the profile screen reads, so the lobby can never disagree
    // with it about the player's avatar, frame or display name.
    val avatarUri: StateFlow<String?> = cosmetics.observeAvatarUri()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val frame: StateFlow<String> = cosmetics.observeFrame()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "default")
    val displayName: StateFlow<String> = identity.observeDisplayName()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    init {
        viewModelScope.launch(Dispatchers.IO) {
            // Performs the one-week guest expiry check and mints a name if needed.
            identity.currentName()
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


/** Asks for POST_NOTIFICATIONS the way Play's 2026 guidance expects: only on
 *  Android 13+, with a plain-language rationale shown *before* the system
 *  dialog, and with a graceful path when the user declines. The permission was
 *  declared in the manifest but never requested, so on modern devices no
 *  notification could ever appear. */
@Composable
fun NotificationPermissionGate() {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) return

    val ctx = LocalContext.current
    var showRationale by remember { mutableStateOf(false) }
    var resolved by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        resolved = true
        OmniLog.i("Perm", "POST_NOTIFICATIONS granted=$granted")
    }

    LaunchedEffect(Unit) {
        val already = ContextCompat.checkSelfPermission(
            ctx, android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (already) {
            resolved = true
            OmniLog.i("Perm", "POST_NOTIFICATIONS already granted")
        } else {
            showRationale = true
        }
    }

    if (showRationale && !resolved) {
        AlertDialog(
            onDismissRequest = { showRationale = false; resolved = true },
            title = { Text(stringResource(R.string.perm_notif_title), color = Yellow) },
            text  = { Text(stringResource(R.string.perm_notif_body), color = TextSec, fontSize = 12.sp) },
            confirmButton = {
                TextButton(onClick = {
                    showRationale = false
                    launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }) { Text(stringResource(R.string.perm_allow), color = SuccessGreen) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRationale = false; resolved = true
                    OmniLog.i("Perm", "POST_NOTIFICATIONS rationale declined")
                }) { Text(stringResource(R.string.perm_later), color = TextDim) }
            },
            containerColor = PanelBg
        )
    }
}


/** Lobby avatar. Shares the frame renderer and photo source with the profile
 *  screen so the two can't drift apart. */
@Composable
private fun LobbyAvatar(level: Int, frame: String, localUri: String?, onClick: () -> Unit) {
    Box(Modifier.size(50.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension * 0.36f
            drawCircle(Color.Black.copy(0.65f), radius = r, center = center)
            drawCircle(Yellow.copy(0.8f), radius = r * 0.30f, center = Offset(center.x, center.y - r * 0.22f))
            val body = Path().apply {
                moveTo(center.x - r * 0.48f, center.y + r * 0.60f)
                cubicTo(
                    center.x - r * 0.44f, center.y + r * 0.10f,
                    center.x + r * 0.44f, center.y + r * 0.10f,
                    center.x + r * 0.48f, center.y + r * 0.60f
                )
                close()
            }
            drawPath(body, Yellow.copy(0.8f))
            drawFrameRing(frame, r)
        }
        val ctx = LocalContext.current
        val bmp by produceState<ImageBitmap?>(null, localUri) {
            value = localUri?.let { uriStr ->
                withContext(Dispatchers.IO) {
                    runCatching {
                        ctx.contentResolver.openInputStream(Uri.parse(uriStr))?.use { st ->
                            BitmapFactory.decodeStream(st)?.asImageBitmap()
                        }
                    }.getOrNull()
                }
            }
        }
        bmp?.let {
            Image(
                bitmap = it, contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(0.62f).clip(CircleShape)
            )
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


/** Per-item artwork for the store, drawn from vector paths. Keyed on the item id
 *  first (so a specific item can have bespoke art) and its category second, so
 *  new server-provided items still get something meaningful rather than a blank. */
private fun DrawScope.marketItemArt(id: String, category: String, accent: Color) {
    when {
        id.startsWith("frame_") -> {
            // Show the actual frame the player would equip.
            val key = id.removePrefix("frame_")
            drawCircle(accent.copy(0.18f), radius = size.minDimension * 0.24f, center = center)
            drawFrameRing(key, size.minDimension * 0.26f)
        }
        id.startsWith("trail_") -> {
            // A comet-like wake, denser toward the head.
            val n = 9
            for (i in 0 until n) {
                val t = i / (n - 1f)
                val x = size.width * (0.16f + t * 0.68f)
                val y = size.height * (0.62f - t * 0.22f)
                val r = size.minDimension * (0.035f + t * 0.075f)
                drawCircle(accent.copy(0.18f + t * 0.7f), radius = r, center = Offset(x, y))
            }
        }
        category == "characters" -> {
            // Stylised bust: hair silhouette, face oval and the eye shape that
            // reads as this specific character at thumbnail size.
            val cx = size.width * 0.5f
            drawCircle(accent.copy(0.16f), radius = size.minDimension * 0.40f, center = center)
            val hair = Path().apply {
                moveTo(cx, size.height * 0.12f)
                cubicTo(size.width * 0.92f, size.height * 0.18f, size.width * 0.88f, size.height * 0.72f, size.width * 0.74f, size.height * 0.86f)
                lineTo(size.width * 0.26f, size.height * 0.86f)
                cubicTo(size.width * 0.12f, size.height * 0.72f, size.width * 0.08f, size.height * 0.18f, cx, size.height * 0.12f)
                close()
            }
            drawPath(hair, accent.copy(0.55f))
            drawPath(hair, accent, style = Stroke(size.minDimension * 0.05f))
            val face = Path().apply {
                moveTo(cx, size.height * 0.26f)
                cubicTo(size.width * 0.76f, size.height * 0.30f, size.width * 0.74f, size.height * 0.66f, cx, size.height * 0.80f)
                cubicTo(size.width * 0.26f, size.height * 0.66f, size.width * 0.24f, size.height * 0.30f, cx, size.height * 0.26f)
                close()
            }
            drawPath(face, Color(0xFFF2DCD3))
            // Large anime eyes.
            listOf(0.38f, 0.62f).forEach { fx ->
                drawOval(
                    Color(0xFF3A2018),
                    topLeft = Offset(size.width * (fx - 0.075f), size.height * 0.46f),
                    size = Size(size.width * 0.15f, size.height * 0.16f)
                )
                drawCircle(
                    Color.White, radius = size.minDimension * 0.028f,
                    center = Offset(size.width * (fx + 0.012f), size.height * 0.50f)
                )
            }
        }
        id.startsWith("priv_") || category == "vip" -> {
            // Laurel-style crest for privileges.
            val sw = size.minDimension * 0.075f
            drawArc(
                accent, 120f, 200f, false,
                topLeft = Offset(size.width * 0.14f, size.height * 0.16f),
                size = Size(size.width * 0.36f, size.height * 0.68f), style = Stroke(sw)
            )
            drawArc(
                accent, 200f, -200f, false,
                topLeft = Offset(size.width * 0.50f, size.height * 0.16f),
                size = Size(size.width * 0.36f, size.height * 0.68f), style = Stroke(sw)
            )
            val star = Path()
            val cx = size.width * 0.5f; val cy = size.height * 0.48f
            val outer = size.minDimension * 0.20f; val inner = outer * 0.42f
            for (i in 0 until 10) {
                val a = (-Math.PI / 2 + i * Math.PI / 5).toFloat()
                val r = if (i % 2 == 0) outer else inner
                val px = cx + cos(a) * r; val py = cy + sin(a) * r
                if (i == 0) star.moveTo(px, py) else star.lineTo(px, py)
            }
            star.close()
            drawPath(star, accent)
        }
        category == "daily" -> {
            // Wrapped gift.
            val sw = size.minDimension * 0.07f
            drawRoundRect(
                accent, topLeft = Offset(size.width * 0.18f, size.height * 0.38f),
                size = Size(size.width * 0.64f, size.height * 0.44f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.05f),
                style = Stroke(sw)
            )
            drawLine(accent, Offset(size.width * 0.5f, size.height * 0.38f), Offset(size.width * 0.5f, size.height * 0.82f), strokeWidth = sw)
            drawLine(accent, Offset(size.width * 0.18f, size.height * 0.52f), Offset(size.width * 0.82f, size.height * 0.52f), strokeWidth = sw * 0.8f)
            drawArc(accent, 180f, 180f, false,
                topLeft = Offset(size.width * 0.30f, size.height * 0.20f),
                size = Size(size.width * 0.18f, size.height * 0.22f), style = Stroke(sw * 0.8f))
            drawArc(accent, 180f, 180f, false,
                topLeft = Offset(size.width * 0.52f, size.height * 0.20f),
                size = Size(size.width * 0.18f, size.height * 0.22f), style = Stroke(sw * 0.8f))
        }
        else -> {
            // Generic mask/silhouette for character-style items.
            val sw = size.minDimension * 0.075f
            val head = Path().apply {
                moveTo(size.width * 0.5f, size.height * 0.18f)
                cubicTo(size.width * 0.82f, size.height * 0.22f, size.width * 0.82f, size.height * 0.62f, size.width * 0.5f, size.height * 0.86f)
                cubicTo(size.width * 0.18f, size.height * 0.62f, size.width * 0.18f, size.height * 0.22f, size.width * 0.5f, size.height * 0.18f)
                close()
            }
            drawPath(head, accent.copy(0.20f))
            drawPath(head, accent, style = Stroke(sw))
            drawCircle(accent, radius = size.minDimension * 0.055f, center = Offset(size.width * 0.40f, size.height * 0.46f))
            drawCircle(accent, radius = size.minDimension * 0.055f, center = Offset(size.width * 0.60f, size.height * 0.46f))
        }
    }
}


// ============================================================================
// Premium event button. Built as a parametric Canvas composition rather than a
// Lottie asset on purpose: it has to scale to any size and take any accent
// colour, and vector-drawn geometry stays crisp where a fixed-resolution asset
// would not. On Android 13+ it additionally routes through an AGSL RuntimeShader
// for a GPU energy-shimmer; below that the Canvas layers alone carry the look,
// so nothing is missing on older devices — just slightly less bloom.
// ============================================================================

/**
 * Cardiac-style pulse: two quick beats then a rest, rather than a sine wave.
 * A real heartbeat rhythm is what makes the button feel alive instead of
 * mechanically throbbing.
 */
private fun heartbeat(t: Float): Float {
    val x = t % 1f
    fun beat(center: Float, width: Float) =
        kotlin.math.exp((-((x - center) * (x - center)) / (2f * width * width)).toDouble()).toFloat()
    return (beat(0.10f, 0.045f) * 1.0f + beat(0.26f, 0.055f) * 0.62f).coerceIn(0f, 1f)
}

/** AGSL energy shimmer. Only compiled on API 33+, where RuntimeShader exists. */
private const val EVENT_SHIMMER_AGSL = """
uniform shader content;
uniform float2 size;
uniform float time;
uniform float intensity;
uniform float3 accent;

half4 main(float2 coord) {
    half4 src = content.eval(coord);
    float2 uv = coord / size;
    // Diagonal energy bands drifting across the face.
    float band = sin((uv.x * 3.2 + uv.y * 1.4 - time * 0.55) * 6.2831);
    band = pow(max(band, 0.0), 6.0);
    // Edge emphasis so the glow hugs the border like light through a gap.
    float edge = 1.0 - smoothstep(0.0, 0.34, min(min(uv.x, 1.0 - uv.x), min(uv.y, 1.0 - uv.y)));
    float glow = (band * 0.55 + edge * 0.45) * intensity;
    half3 lit = src.rgb + half3(accent) * glow * src.a;
    return half4(lit, src.a);
}
"""

/**
 * The event button. [progress] optionally drives a fill meter (0..1) for
 * event-style "collect" buttons; pass null for a plain action button.
 */
@Composable
fun PremiumEventButton(
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subLabel: String? = null,
    progress: Float? = null,
    enabled: Boolean = true,
    glyph: (DrawScope.(Color) -> Unit)? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val inf = rememberInfiniteTransition(label = "eventBtn")
    // Master clock. One shared driver keeps every layer phase-locked, which is
    // what stops the composition looking like several unrelated animations.
    val clock by inf.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        "eventClock"
    )
    val growth by inf.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(5200, easing = LinearEasing), RepeatMode.Restart),
        "vineGrowth"
    )
    val sweep by inf.animateFloat(
        -0.35f, 1.35f,
        infiniteRepeatable(tween(3100, easing = LinearEasing), RepeatMode.Restart),
        "eventSweep"
    )

    val pulse = heartbeat(clock)
    val pressDepth by animateFloatAsState(
        if (pressed) 1f else 0f,
        spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
        label = "eventPress"
    )
    val tint = if (enabled) accent else TextDim

    // GPU shimmer where available. Guarded so API < 33 simply skips it.
    val shaderModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val shader = remember { android.graphics.RuntimeShader(EVENT_SHIMMER_AGSL) }
        Modifier.graphicsLayer {
            shader.setFloatUniform("size", size.width, size.height)
            shader.setFloatUniform("time", clock * 2600f / 1000f)
            shader.setFloatUniform("intensity", (0.18f + pulse * 0.42f) * (if (enabled) 1f else 0.25f))
            shader.setFloatUniform("accent", tint.red, tint.green, tint.blue)
            renderEffect = android.graphics.RenderEffect
                .createRuntimeShaderEffect(shader, "content")
                .asComposeRenderEffect()
        }
    } else Modifier

    Box(
        modifier
            .heightIn(min = 62.dp)
            .graphicsLayer {
                // Depth: the face sinks and shrinks very slightly under the
                // finger, and lifts with a soft shadow at rest.
                val s = 1f - pressDepth * 0.045f
                scaleX = s; scaleY = s
                translationY = pressDepth * 4f
                shadowElevation = (10f - pressDepth * 7f) * density
                spotShadowColor = tint.copy(0.55f)
                ambientShadowColor = tint.copy(0.35f)
                shape = RoundedCornerShape(18.dp)
                clip = false
            }
            .then(shaderModifier)
            .clickable(interaction, indication = null, enabled = enabled, onClick = onClick)
    ) {
        androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
            drawEventButtonPlate(tint, pulse, sweep, pressDepth, progress)
        }
        // Real lit tube geometry, rendered by GL over the plate. The old flat
        // Canvas vines are gone — this is the actual 3D layer.
        VineLayer(accent = tint, modifier = Modifier.matchParentSize())

        Row(
            Modifier.align(Alignment.Center).padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (glyph != null) {
                androidx.compose.foundation.Canvas(
                    Modifier.size(26.dp).graphicsLayer {
                        val g = 1f + pulse * 0.10f
                        scaleX = g; scaleY = g
                    }
                ) { glyph(tint) }
                Spacer(Modifier.width(12.dp))
            }
            Column {
                Text(
                    label.uppercase(),
                    color = if (enabled) Color.White else TextDim,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.5.sp
                )
                if (subLabel != null) {
                    Text(
                        subLabel,
                        color = tint.copy(0.85f),
                        fontSize = 9.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

/** The layered metal/energy plate: bevel, inner glow, sweep, optional meter. */
private fun DrawScope.drawEventButtonPlate(
    accent: Color,
    pulse: Float,
    sweep: Float,
    pressDepth: Float,
    progress: Float?
) {
    val r = size.minDimension * 0.28f
    val corner = androidx.compose.ui.geometry.CornerRadius(r)

    // Outer bloom, strongest on the beat.
    drawRoundRect(
        Brush.radialGradient(
            listOf(accent.copy(0.30f * pulse + 0.06f), Color.Transparent),
            center = center, radius = size.maxDimension * 0.75f
        ),
        cornerRadius = corner
    )

    // Body: dark metal with an accent-lit lower edge for a sense of volume.
    drawRoundRect(
        Brush.verticalGradient(
            listOf(
                Color(0xFF1A1A16).copy(0.96f),
                Color(0xFF0C0C0A).copy(0.98f),
                accent.copy(0.16f)
            )
        ),
        cornerRadius = corner
    )

    // Top bevel highlight — reads as a lit chamfer, the main 3D cue.
    drawRoundRect(
        Brush.verticalGradient(
            listOf(Color.White.copy(0.14f - pressDepth * 0.10f), Color.Transparent),
            endY = size.height * 0.42f
        ),
        cornerRadius = corner
    )

    // Optional progress meter, drawn under the border.
    if (progress != null) {
        val w = size.width * progress.coerceIn(0f, 1f)
        if (w > 1f) {
            clipRect(right = w) {
                drawRoundRect(
                    Brush.horizontalGradient(listOf(accent.copy(0.30f), accent.copy(0.55f))),
                    cornerRadius = corner
                )
            }
        }
    }

    // Travelling highlight band.
    val bandW = size.width * 0.22f
    val bx = size.width * sweep
    drawRoundRect(
        Brush.horizontalGradient(
            listOf(Color.Transparent, Color.White.copy(0.13f), Color.Transparent),
            startX = bx - bandW / 2f, endX = bx + bandW / 2f
        ),
        cornerRadius = corner
    )

    // Double border: a solid inner line plus a wider soft halo that breathes.
    drawRoundRect(accent.copy(0.85f), cornerRadius = corner, style = Stroke(1.6f))
    drawRoundRect(
        accent.copy(0.22f + pulse * 0.38f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(r + 3f),
        style = Stroke(3.2f)
    )

    // Corner ticks, the small mechanical detail that sells "console UI".
    val tick = size.minDimension * 0.16f
    val inset = size.minDimension * 0.13f
    listOf(
        Triple(inset, inset, 1f),
        Triple(size.width - inset, inset, -1f)
    ).forEach { (x, y, dir) ->
        drawLine(accent.copy(0.75f), Offset(x, y), Offset(x + tick * dir, y), strokeWidth = 1.6f)
        drawLine(accent.copy(0.75f), Offset(x, y), Offset(x, y + tick * 0.7f), strokeWidth = 1.6f)
    }
    listOf(
        Triple(inset, size.height - inset, 1f),
        Triple(size.width - inset, size.height - inset, -1f)
    ).forEach { (x, y, dir) ->
        drawLine(accent.copy(0.75f), Offset(x, y), Offset(x + tick * dir, y), strokeWidth = 1.6f)
        drawLine(accent.copy(0.75f), Offset(x, y), Offset(x, y - tick * 0.7f), strokeWidth = 1.6f)
    }
}

/** Expandable codex entry. Collapsed by default so the list stays scannable,
 *  and drawn with the same glyph language as the rest of the UI. */
@Composable
private fun CodexEntry(
    titleRes: Int,
    bodyRes: Int,
    accent: Color,
    glyph: DrawScope.(Color) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 90f else 0f, tween(220), label = "codexArrow")
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.horizontalGradient(listOf(MetalBg, accent.copy(0.07f)))
            )
            .border(1.dp, accent.copy(0.30f), RoundedCornerShape(8.dp))
            .clickable { expanded = !expanded }
            .padding(horizontal = 13.dp, vertical = 11.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.Canvas(Modifier.size(20.dp)) { glyph(accent) }
            Spacer(Modifier.width(11.dp))
            Text(
                stringResource(titleRes),
                color = accent, fontSize = 11.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp,
                modifier = Modifier.weight(1f)
            )
            androidx.compose.foundation.Canvas(
                Modifier.size(12.dp).graphicsLayer { rotationZ = rotation }
            ) {
                val p = Path().apply {
                    moveTo(size.width * 0.30f, size.height * 0.16f)
                    lineTo(size.width * 0.74f, size.height * 0.50f)
                    lineTo(size.width * 0.30f, size.height * 0.84f)
                }
                drawPath(p, accent.copy(0.75f), style = Stroke(1.8f, cap = StrokeCap.Round))
            }
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(240)) + fadeIn(tween(240)),
            exit  = shrinkVertically(tween(180)) + fadeOut(tween(140))
        ) {
            Column {
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(accent.copy(0.20f)))
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(bodyRes),
                    color = TextSec, fontSize = 11.sp, lineHeight = 17.sp
                )
            }
        }
    }
}


// ============================================================================
// Character model. Loaded from a compact binary produced from the source FBX
// (position/normal/UV per vertex, 16-bit indices). A runtime FBX parser was not
// worth carrying: the format is proprietary and heavyweight, and this file gets
// the same geometry into memory with a few dozen lines and no dependency.
//
// The source FBX contains a skinned rig but ZERO animation curves, so there is
// nothing to play back. Motion is therefore generated in the vertex shader from
// a height-weighted sway model — the higher up the body a vertex sits, the more
// it moves — which yields believable idle breathing and a walk cycle without a
// skeleton.
// ============================================================================

class CharacterMesh(
    val vertexBuffer: FloatArray,
    val indices: ShortArray
) {
    companion object {
        private const val MAGIC = 0x48534D4F   // "OMSH" little-endian
        const val FLOATS_PER_VERTEX = 8        // pos3 + normal3 + uv2

        /** Returns null rather than throwing: a missing or malformed model must
         *  degrade to "no character drawn", never take the game down. */
        fun load(ctx: Context, assetPath: String): CharacterMesh? = runCatching {
            val bytes = ctx.assets.open(assetPath).use { it.readBytes() }
            val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val magic = bb.int
            require(magic == MAGIC) { "bad magic 0x${Integer.toHexString(magic)}" }
            bb.short; bb.short                       // version major/minor
            val vertexCount = bb.int
            val indexCount = bb.int
            require(vertexCount in 1..500_000 && indexCount in 3..2_000_000) {
                "implausible counts v=$vertexCount i=$indexCount"
            }
            val verts = FloatArray(vertexCount * FLOATS_PER_VERTEX)
            bb.asFloatBuffer().get(verts)
            bb.position(bb.position() + verts.size * 4)
            val idx = ShortArray(indexCount)
            bb.asShortBuffer().get(idx)
            OmniLog.i("Model", "loaded $assetPath: $vertexCount verts, ${indexCount / 3} tris")
            CharacterMesh(verts, idx)
        }.onFailure { OmniLog.e("Model", "failed to load $assetPath", it) }.getOrNull()
    }
}

private const val OMNI_CHAR_VERT = """#version 300 es
layout(location=0) in vec3 aPos;
layout(location=1) in vec3 aNormal;
layout(location=2) in vec2 aUV;

uniform mat4 uMVP;
uniform mat4 uModel;
uniform float uTime;
uniform float uWalk;      // 0 = idle, 1 = full stride
uniform float uHeight;    // model height in world units, for sway weighting

out vec3 vNormal; out vec2 vUV; out vec3 vWorldPos;

void main(){
    vec3 p = aPos;

    // Height weight: feet stay planted, the torso and head carry the motion.
    float h = clamp(p.y / max(uHeight, 0.001), 0.0, 1.0);
    float upper = smoothstep(0.25, 1.0, h);

    // Idle: slow breathing lift plus a lateral drift, out of phase so it reads
    // as a living stance rather than a bobbing object.
    float breathe = sin(uTime * 1.5) * 0.006 * upper;
    float driftX  = sin(uTime * 0.7 + 1.2) * 0.008 * upper;

    // Walk: vertical bob at twice stride frequency, counter-rotating lean, and
    // a slight forward pitch of the upper body.
    float stride  = uTime * 7.5;
    float bob     = sin(stride * 2.0) * 0.035 * uWalk;
    float leanX   = sin(stride) * 0.055 * uWalk * upper;
    float pitch   = 0.12 * uWalk * upper;

    p.y += breathe + bob;
    p.x += driftX + leanX;
    p.z += pitch * h * 0.15;

    // Legs swing opposite to each other, split by the model's centre line.
    float legMask = 1.0 - smoothstep(0.0, 0.45, h);
    float side = sign(aPos.x + 0.0001);
    p.z += sin(stride + (side > 0.0 ? 0.0 : 3.14159)) * 0.09 * uWalk * legMask;

    vec4 world = uModel * vec4(p, 1.0);
    vWorldPos = world.xyz;
    vNormal = mat3(uModel) * aNormal;
    vUV = aUV;
    gl_Position = uMVP * vec4(p, 1.0);
}
"""

private const val OMNI_CHAR_FRAG = """#version 300 es
precision mediump float;
in vec3 vNormal; in vec2 vUV; in vec3 vWorldPos;
uniform sampler2D uTex;
uniform vec3 uCamPos; uniform vec3 uFlashDir; uniform float uFlashOn;
uniform float uFogDensity; uniform vec3 uFogColor; uniform float uAmbient;
out vec4 fragColor;
void main(){
    vec4 tex = texture(uTex, vUV);
    if (tex.a < 0.35) discard;
    vec3 n = normalize(vNormal);
    vec3 toCam = uCamPos - vWorldPos;
    float dist = length(toCam);
    vec3 toCamN = toCam / max(dist, 0.001);

    // Cel shading: two flat bands plus a rim light. Matches the character's
    // anime source art far better than a smooth Lambert ramp would.
    float ndl = dot(n, normalize(vec3(0.3, 1.0, 0.4)));
    float band = ndl > 0.25 ? 1.0 : (ndl > -0.15 ? 0.72 : 0.5);
    float rim = pow(1.0 - max(dot(n, toCamN), 0.0), 2.5) * 0.45;

    float flash = 0.0;
    if (uFlashOn > 0.5) {
        float spotCos = dot(-toCamN, normalize(uFlashDir));
        float cone = smoothstep(0.80, 0.97, spotCos);
        float atten = clamp(1.0 - dist / 20.0, 0.0, 1.0);
        flash = cone * max(dot(n, toCamN), 0.0) * atten * 1.5;
    }

    vec3 col = tex.rgb * (uAmbient * band + flash) + vec3(rim) * 0.6;
    float fog = 1.0 - exp(-uFogDensity * dist * dist * 0.008);
    col = mix(col, uFogColor, clamp(fog, 0.0, 1.0));
    fragColor = vec4(col, 1.0);
}
"""


// ============================================================================
// Real 3D vines. The earlier version drew flat Canvas strokes — no amount of
// glow makes those read as three-dimensional. This builds actual tube geometry:
// a Catmull-Rom spine is swept with a ring of vertices, producing genuine
// surface normals, so the vine is lit, shaded and self-occluding like a solid
// object. It renders through GLSurfaceView into the button, not onto a Canvas.
// ============================================================================

private class VineSpec(
    val rootU: Float, val rootV: Float,   // 0..1 anchor on the button face
    val dirX: Float, val dirY: Float,
    val length: Float,
    val curl: Float,
    val twist: Float,
    val leaves: Int,
    val phase: Float
)

private val VINE_SPECS = listOf(
    VineSpec(0.03f, 0.50f,  0.05f, -1f, 0.95f,  0.38f,  2.1f, 4, 0.00f),
    VineSpec(0.03f, 0.55f,  0.10f,  1f, 0.80f, -0.30f, -1.7f, 3, 0.21f),
    VineSpec(0.97f, 0.45f, -0.05f, -1f, 0.88f, -0.34f,  1.9f, 4, 0.42f),
    VineSpec(0.97f, 0.52f, -0.10f,  1f, 0.74f,  0.28f, -2.3f, 3, 0.63f),
    VineSpec(0.28f, 0.03f,  1.00f,  0.15f, 0.58f, 0.24f, 1.4f, 3, 0.15f),
    VineSpec(0.72f, 0.97f, -1.00f, -0.15f, 0.58f,-0.24f,-1.4f, 3, 0.52f)
)

private const val OMNI_VINE_VERT = """#version 300 es
layout(location=0) in vec3 aPos;
layout(location=1) in vec3 aNormal;
layout(location=2) in float aGrow;   // 0..1 position along the spine

uniform mat4 uMVP;
uniform float uGrowth;    // how far the vine has extended, 0..1
uniform float uSway;
out vec3 vNormal; out float vGrow; out vec3 vLocal;

void main(){
    // Vertices beyond the growth front collapse onto the spine tip, so the vine
    // grows out of the surface instead of popping into existence.
    float visible = step(aGrow, uGrowth);
    vec3 p = aPos;
    float sway = sin(uSway + aGrow * 4.0) * 0.018 * aGrow;
    p.x += sway;
    p.y += cos(uSway * 0.8 + aGrow * 3.0) * 0.012 * aGrow;
    p *= visible;

    vLocal = p;
    vNormal = aNormal;
    vGrow = aGrow;
    gl_Position = uMVP * vec4(p, 1.0);
}
"""

private const val OMNI_VINE_FRAG = """#version 300 es
precision mediump float;
in vec3 vNormal; in float vGrow; in vec3 vLocal;
uniform vec3 uAccent;
uniform float uPulse;
uniform float uGrowth;
out vec4 fragColor;
void main(){
    if (vGrow > uGrowth) discard;
    vec3 n = normalize(vNormal);
    // Two-point lighting: a key from upper-left and a cool fill from the right,
    // which is what makes the tube read as round rather than as a flat ribbon.
    vec3 key  = normalize(vec3(-0.45, 0.75, 0.5));
    vec3 fill = normalize(vec3(0.7, -0.2, 0.35));
    float kd = max(dot(n, key), 0.0);
    float fd = max(dot(n, fill), 0.0) * 0.35;

    // Specular highlight along the top of the tube — a real 3D cue.
    vec3 view = vec3(0.0, 0.0, 1.0);
    vec3 h = normalize(key + view);
    float spec = pow(max(dot(n, h), 0.0), 24.0) * 0.7;

    // Colour ramps from deep at the root to bright at the growing tip.
    vec3 deep = uAccent * 0.28;
    vec3 tip  = uAccent * (1.0 + uPulse * 0.6);
    vec3 base = mix(deep, tip, vGrow);

    float rim = pow(1.0 - max(dot(n, view), 0.0), 3.0) * 0.5 * (0.4 + uPulse);
    vec3 col = base * (0.22 + kd * 0.85 + fd) + vec3(spec) * uAccent + uAccent * rim;

    // The growing front glows hotter.
    float front = smoothstep(uGrowth - 0.09, uGrowth, vGrow);
    col += uAccent * front * (0.55 + uPulse * 0.9);

    fragColor = vec4(col, 1.0);
}
"""

/** Builds swept-tube geometry for one vine: interleaved pos3 + normal3 + grow1. */
private fun buildVineMesh(spec: VineSpec, segments: Int = 26, sides: Int = 7):
        Pair<FloatArray, ShortArray> {

    val verts = ArrayList<Float>((segments + 1) * sides * 7)
    val idx = ArrayList<Short>(segments * sides * 6)

    // Spine control points: root, curl outward, taper back toward the tip.
    val px = spec.rootU * 2f - 1f
    val py = 1f - spec.rootV * 2f
    val perpX = -spec.dirY
    val perpY = spec.dirX

    fun spineAt(t: Float): Triple<Float, Float, Float> {
        // Quadratic-ish bow along the direction, bowed by curl on the perpendicular.
        val bow = kotlin.math.sin(t * Math.PI).toFloat() * spec.curl
        val x = px + spec.dirX * spec.length * t + perpX * bow
        val y = py + spec.dirY * spec.length * t + perpY * bow
        // Depth: the vine lifts off the surface in the middle, which is what
        // gives it visible thickness against the button face.
        val z = kotlin.math.sin(t * Math.PI).toFloat() * 0.16f + 0.02f
        return Triple(x, y, z)
    }

    for (i in 0..segments) {
        val t = i / segments.toFloat()
        val (cx, cy, cz) = spineAt(t)
        val (nx2, ny2, nz2) = spineAt((t + 0.02f).coerceAtMost(1f))
        var tx = nx2 - cx; var ty = ny2 - cy; var tz = nz2 - cz
        val tl = kotlin.math.sqrt(tx * tx + ty * ty + tz * tz).coerceAtLeast(1e-5f)
        tx /= tl; ty /= tl; tz /= tl

        // Frame perpendicular to the tangent.
        var ux = -ty; var uy = tx; var uz = 0f
        val ul = kotlin.math.sqrt(ux * ux + uy * uy + uz * uz).coerceAtLeast(1e-5f)
        ux /= ul; uy /= ul; uz /= ul
        val vx = ty * uz - tz * uy
        val vy = tz * ux - tx * uz
        val vz = tx * uy - ty * ux

        // Tapers to a point at the tip; slight bulge near the root.
        val radius = 0.030f * (1f - t * 0.75f) * (1f + 0.25f * kotlin.math.sin(t * 9f))

        for (j in 0 until sides) {
            val a = (j / sides.toFloat()) * (Math.PI * 2).toFloat() + spec.twist * t
            val ca = kotlin.math.cos(a); val sa = kotlin.math.sin(a)
            val nX = ux * ca + vx * sa
            val nY = uy * ca + vy * sa
            val nZ = uz * ca + vz * sa
            verts.add(cx + nX * radius); verts.add(cy + nY * radius); verts.add(cz + nZ * radius)
            verts.add(nX); verts.add(nY); verts.add(nZ)
            verts.add(t)
        }
    }

    for (i in 0 until segments) {
        for (j in 0 until sides) {
            val a = (i * sides + j)
            val b = (i * sides + (j + 1) % sides)
            val c = ((i + 1) * sides + j)
            val d = ((i + 1) * sides + (j + 1) % sides)
            idx.add(a.toShort()); idx.add(c.toShort()); idx.add(b.toShort())
            idx.add(b.toShort()); idx.add(c.toShort()); idx.add(d.toShort())
        }
    }

    // Leaves: flat quads angled off the spine, alternating sides.
    var base = (segments + 1) * sides
    for (l in 1..spec.leaves) {
        val t = l / (spec.leaves + 1f)
        val (cx, cy, cz) = spineAt(t)
        val side = if (l % 2 == 0) 1f else -1f
        val lx = perpX * side; val ly = perpY * side
        val size = 0.085f * (1f - t * 0.4f)
        val nz = 0.75f
        // Simple diamond, normal tilted toward the viewer so it catches light.
        val pts = arrayOf(
            floatArrayOf(cx, cy, cz),
            floatArrayOf(cx + lx * size * 0.5f - ly * size * 0.35f, cy + ly * size * 0.5f + lx * size * 0.35f, cz + 0.012f),
            floatArrayOf(cx + lx * size, cy + ly * size, cz + 0.02f),
            floatArrayOf(cx + lx * size * 0.5f + ly * size * 0.35f, cy + ly * size * 0.5f - lx * size * 0.35f, cz + 0.012f)
        )
        pts.forEach { p ->
            verts.add(p[0]); verts.add(p[1]); verts.add(p[2])
            verts.add(lx * 0.35f); verts.add(ly * 0.35f); verts.add(nz)
            verts.add(t)
        }
        idx.add(base.toShort()); idx.add((base + 1).toShort()); idx.add((base + 2).toShort())
        idx.add(base.toShort()); idx.add((base + 2).toShort()); idx.add((base + 3).toShort())
        base += 4
    }

    return FloatArray(verts.size) { verts[it] } to ShortArray(idx.size) { idx[it] }
}


/** Renders the 3D vines for one button. A small dedicated GLSurfaceView sits
 *  behind the button's content with a transparent background, so genuine lit
 *  geometry composites over the UI. */
class VineRenderer : GLSurfaceView.Renderer {
    @Volatile var accent: Triple<Float, Float, Float> = Triple(0.3f, 0.85f, 0.4f)
    @Volatile var enabled: Boolean = true

    private var program = 0
    private var uMVP = 0; private var uGrowth = 0; private var uSway = 0
    private var uAccent = 0; private var uPulse = 0
    private val vbos = IntArray(VINE_SPECS.size)
    private val ibos = IntArray(VINE_SPECS.size)
    private val counts = IntArray(VINE_SPECS.size)
    private val mvp = FloatArray(16)
    private val proj = FloatArray(16)
    private val view = FloatArray(16)
    private val start = System.nanoTime()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0f, 0f, 0f, 0f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        runCatching {
            program = linkGlProgram(OMNI_VINE_VERT, OMNI_VINE_FRAG)
            uMVP = GLES30.glGetUniformLocation(program, "uMVP")
            uGrowth = GLES30.glGetUniformLocation(program, "uGrowth")
            uSway = GLES30.glGetUniformLocation(program, "uSway")
            uAccent = GLES30.glGetUniformLocation(program, "uAccent")
            uPulse = GLES30.glGetUniformLocation(program, "uPulse")

            VINE_SPECS.forEachIndexed { i, spec ->
                val (v, idx) = buildVineMesh(spec)
                val vb = IntArray(1); GLES30.glGenBuffers(1, vb, 0); vbos[i] = vb[0]
                GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbos[i])
                GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, v.size * 4, glFloatBuffer(v), GLES30.GL_STATIC_DRAW)
                val ib = IntArray(1); GLES30.glGenBuffers(1, ib, 0); ibos[i] = ib[0]
                val buf = ByteBuffer.allocateDirect(idx.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer()
                buf.put(idx); buf.position(0)
                GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, ibos[i])
                GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, idx.size * 2, buf, GLES30.GL_STATIC_DRAW)
                counts[i] = idx.size
            }
            OmniLog.i("Vine", "built ${VINE_SPECS.size} vine meshes")
        }.onFailure { OmniLog.e("Vine", "vine setup failed", it) }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / height.coerceAtLeast(1)
        // Mild perspective: enough for the tubes to show depth without the
        // button looking like it is floating in a 3D scene.
        Matrix.frustumM(proj, 0, -aspect * 0.5f, aspect * 0.5f, -0.5f, 0.5f, 1.2f, 12f)
        Matrix.setLookAtM(view, 0, 0f, 0f, 2.6f, 0f, 0f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(mvp, 0, proj, 0, view, 0)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        if (program == 0 || !enabled) return
        val t = (System.nanoTime() - start) / 1_000_000_000f

        GLES30.glUseProgram(program)
        GLES30.glUniformMatrix4fv(uMVP, 1, false, mvp, 0)
        GLES30.glUniform3f(uAccent, accent.first, accent.second, accent.third)
        GLES30.glUniform1f(uPulse, heartbeat((t / 2.6f) % 1f))

        VINE_SPECS.forEachIndexed { i, spec ->
            if (counts[i] <= 0) return@forEachIndexed
            // Grow, hold, recede — each vine on its own offset.
            val local = ((t / 5.2f) + spec.phase) % 1f
            val growth = when {
                local < 0.55f -> local / 0.55f
                local < 0.78f -> 1f
                else -> 1f - (local - 0.78f) / 0.22f
            }.coerceIn(0f, 1f)
            if (growth <= 0.02f) return@forEachIndexed

            GLES30.glUniform1f(uGrowth, growth)
            GLES30.glUniform1f(uSway, t * 1.4f + spec.phase * 6f)
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbos[i])
            val stride = 7 * 4
            GLES30.glEnableVertexAttribArray(0); GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
            GLES30.glEnableVertexAttribArray(1); GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride, 3 * 4)
            GLES30.glEnableVertexAttribArray(2); GLES30.glVertexAttribPointer(2, 1, GLES30.GL_FLOAT, false, stride, 6 * 4)
            GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, ibos[i])
            GLES30.glDrawElements(GLES30.GL_TRIANGLES, counts[i], GLES30.GL_UNSIGNED_SHORT, 0)
        }
        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
        GLES30.glDisableVertexAttribArray(2)
    }
}

/** Drop-in 3D vine layer for a button. Transparent, non-interactive, and cheap
 *  enough to sit behind two or three buttons at once. */
@Composable
fun VineLayer(accent: Color, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val renderer = remember { VineRenderer() }
    LaunchedEffect(accent) {
        renderer.accent = Triple(accent.red, accent.green, accent.blue)
    }
    val glView = remember {
        GLSurfaceView(ctx).apply {
            setEGLContextClientVersion(3)
            // Transparent surface so the vines composite over the button art.
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            holder.setFormat(android.graphics.PixelFormat.TRANSLUCENT)
            setZOrderOnTop(true)
            preserveEGLContextOnPause = true
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_RESUME -> glView.onResume()
                Lifecycle.Event.ON_PAUSE -> glView.onPause()
                else -> {}
            }
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs); glView.onPause() }
    }
    AndroidView(factory = { glView }, modifier = modifier)
}


// ============================================================================
// Character preview. A dedicated scene: the character stands on the same floor
// texture the game uses, against the same wall texture, with no ceiling — so
// the model is read against the surfaces it will actually be seen among, lit
// the same way, rather than floating on a flat swatch.
// ============================================================================

private const val OMNI_PREVIEW_VERT = """#version 300 es
layout(location=0) in vec3 aPos;
layout(location=1) in vec3 aNormal;
layout(location=2) in vec2 aUV;
uniform mat4 uMVP;
uniform mat4 uModel;
uniform float uTime;
uniform float uWalk;
out vec3 vNormal; out vec2 vUV; out vec3 vWorldPos;
void main(){
    vec3 p = aPos;
    // Same height-weighted motion model as in-game, so the preview is honest
    // about how the character will actually move.
    float h = clamp(p.y, 0.0, 1.0);
    float upper = smoothstep(0.25, 1.0, h);
    float stride = uTime * 7.0;
    p.y += sin(uTime * 1.5) * 0.006 * upper + sin(stride * 2.0) * 0.030 * uWalk;
    p.x += sin(uTime * 0.7 + 1.2) * 0.008 * upper + sin(stride) * 0.050 * uWalk * upper;
    float legMask = 1.0 - smoothstep(0.0, 0.45, h);
    float side = sign(aPos.x + 0.0001);
    p.z += sin(stride + (side > 0.0 ? 0.0 : 3.14159)) * 0.085 * uWalk * legMask;

    vec4 world = uModel * vec4(p, 1.0);
    vWorldPos = world.xyz;
    vNormal = mat3(uModel) * aNormal;
    vUV = aUV;
    gl_Position = uMVP * vec4(p, 1.0);
}
"""

private const val OMNI_PREVIEW_FRAG = """#version 300 es
precision mediump float;
in vec3 vNormal; in vec2 vUV; in vec3 vWorldPos;
uniform sampler2D uTex;
uniform float uIsCharacter;
out vec4 fragColor;
void main(){
    vec4 tex = texture(uTex, vUV);
    if (uIsCharacter > 0.5 && tex.a < 0.35) discard;
    vec3 n = normalize(vNormal);
    vec3 key = normalize(vec3(-0.4, 0.9, 0.6));
    float ndl = dot(n, key);
    // Cel bands for the character; smooth shading for the room surfaces, which
    // should recede rather than compete with the model.
    float lit = uIsCharacter > 0.5
        ? (ndl > 0.25 ? 1.0 : (ndl > -0.1 ? 0.74 : 0.55))
        : (0.45 + max(ndl, 0.0) * 0.55);
    vec3 view = normalize(vec3(0.0, 0.15, 1.0));
    float rim = pow(1.0 - max(dot(n, view), 0.0), 3.0) * (uIsCharacter > 0.5 ? 0.5 : 0.12);
    vec3 col = tex.rgb * lit + vec3(rim) * vec3(1.0, 0.92, 0.75);
    // Gentle vertical falloff so the backdrop sinks away behind the character.
    if (uIsCharacter < 0.5) col *= 0.55 + 0.45 * clamp(vWorldPos.y * 0.35, 0.0, 1.0);
    fragColor = vec4(col, 1.0);
}
"""

/** Renders the character on a floor, against a wall, with no ceiling. */
class CharacterPreviewRenderer(private val appContext: Context) : GLSurfaceView.Renderer {

    @Volatile var yawDegrees: Float = 0f
    @Volatile var walkAmount: Float = 0f

    private var program = 0
    private var uMVP = 0; private var uModel = 0; private var uTime = 0
    private var uWalk = 0; private var uTex = 0; private var uIsChar = 0

    private var charVbo = 0; private var charIbo = 0; private var charCount = 0
    private var roomVbo = 0; private var roomIbo = 0
    private var wallCount = 0; private var floorCount = 0
    private var wallVbo = 0; private var wallIbo = 0
    private var charTex = 0; private var wallTex = 0; private var floorTex = 0

    private val proj = FloatArray(16)
    private val view = FloatArray(16)
    private val vp = FloatArray(16)
    private val model = FloatArray(16)
    private val mvp = FloatArray(16)
    private val start = System.nanoTime()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0.04f, 0.038f, 0.03f, 1f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        runCatching {
            program = linkGlProgram(OMNI_PREVIEW_VERT, OMNI_PREVIEW_FRAG)
            uMVP = GLES30.glGetUniformLocation(program, "uMVP")
            uModel = GLES30.glGetUniformLocation(program, "uModel")
            uTime = GLES30.glGetUniformLocation(program, "uTime")
            uWalk = GLES30.glGetUniformLocation(program, "uWalk")
            uTex = GLES30.glGetUniformLocation(program, "uTex")
            uIsChar = GLES30.glGetUniformLocation(program, "uIsCharacter")

            CharacterMesh.load(appContext, "character.omesh")?.let { mesh ->
                charVbo = genBuf(); charIbo = genBuf()
                GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, charVbo)
                GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, mesh.vertexBuffer.size * 4,
                    glFloatBuffer(mesh.vertexBuffer), GLES30.GL_STATIC_DRAW)
                val ib = ByteBuffer.allocateDirect(mesh.indices.size * 2)
                    .order(ByteOrder.nativeOrder()).asShortBuffer()
                ib.put(mesh.indices); ib.position(0)
                GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, charIbo)
                GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, mesh.indices.size * 2, ib, GLES30.GL_STATIC_DRAW)
                charCount = mesh.indices.size
            }

            // Floor plane at y=0 and a back wall at z=-1.6. No ceiling by design.
            val floorQuad = quadMesh(
                floatArrayOf(-2.2f, 0f, 1.6f), floatArrayOf(2.2f, 0f, 1.6f),
                floatArrayOf(2.2f, 0f, -1.6f), floatArrayOf(-2.2f, 0f, -1.6f),
                floatArrayOf(0f, 1f, 0f), 3.0f
            )
            roomVbo = genBuf(); roomIbo = genBuf()
            uploadQuad(roomVbo, roomIbo, floorQuad)
            floorCount = 6

            val wallQuad = quadMesh(
                floatArrayOf(-2.2f, 0f, -1.6f), floatArrayOf(2.2f, 0f, -1.6f),
                floatArrayOf(2.2f, 2.8f, -1.6f), floatArrayOf(-2.2f, 2.8f, -1.6f),
                floatArrayOf(0f, 0f, 1f), 2.4f
            )
            wallVbo = genBuf(); wallIbo = genBuf()
            uploadQuad(wallVbo, wallIbo, wallQuad)
            wallCount = 6

            charTex = loadTex("character_texture.png", 0xFFE8D5C8.toInt())
            wallTex = loadTex("Level_0/Wall.png", 0xFF4A4030.toInt())
            floorTex = loadTex("Level_0/Floor.png", 0xFF3A3020.toInt())
        }.onFailure { OmniLog.e("Preview", "setup failed", it) }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        Matrix.perspectiveM(proj, 0, 38f, width.toFloat() / height.coerceAtLeast(1), 0.1f, 20f)
        // Slightly raised three-quarter view, aimed at chest height.
        Matrix.setLookAtM(view, 0, 0f, 1.15f, 3.15f, 0f, 0.92f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(vp, 0, proj, 0, view, 0)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        if (program == 0) return
        val t = (System.nanoTime() - start) / 1_000_000_000f
        GLES30.glUseProgram(program)
        GLES30.glUniform1f(uTime, t)

        // Backdrop first.
        Matrix.setIdentityM(model, 0)
        GLES30.glUniformMatrix4fv(uMVP, 1, false, vp, 0)
        GLES30.glUniformMatrix4fv(uModel, 1, false, model, 0)
        GLES30.glUniform1f(uWalk, 0f)
        GLES30.glUniform1f(uIsChar, 0f)
        drawIndexed(roomVbo, roomIbo, floorCount, floorTex)
        drawIndexed(wallVbo, wallIbo, wallCount, wallTex)

        // Character, scaled to a believable 1.7 m against the 2.8 m wall.
        if (charCount > 0) {
            Matrix.setIdentityM(model, 0)
            Matrix.rotateM(model, 0, yawDegrees, 0f, 1f, 0f)
            Matrix.scaleM(model, 0, 1.7f, 1.7f, 1.7f)
            Matrix.multiplyMM(mvp, 0, vp, 0, model, 0)
            GLES30.glUniformMatrix4fv(uMVP, 1, false, mvp, 0)
            GLES30.glUniformMatrix4fv(uModel, 1, false, model, 0)
            GLES30.glUniform1f(uWalk, walkAmount)
            GLES30.glUniform1f(uIsChar, 1f)
            drawIndexed(charVbo, charIbo, charCount, charTex)
        }
    }

    private fun drawIndexed(vbo: Int, ibo: Int, count: Int, tex: Int) {
        if (count <= 0) return
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex)
        GLES30.glUniform1i(uTex, 0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo)
        val stride = 8 * 4
        GLES30.glEnableVertexAttribArray(0); GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
        GLES30.glEnableVertexAttribArray(1); GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride, 3 * 4)
        GLES30.glEnableVertexAttribArray(2); GLES30.glVertexAttribPointer(2, 2, GLES30.GL_FLOAT, false, stride, 6 * 4)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, ibo)
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, count, GLES30.GL_UNSIGNED_SHORT, 0)
        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
        GLES30.glDisableVertexAttribArray(2)
    }

    private fun quadMesh(
        p0: FloatArray, p1: FloatArray, p2: FloatArray, p3: FloatArray,
        n: FloatArray, uvScale: Float
    ): FloatArray {
        val pts = arrayOf(p0, p1, p2, p3)
        val uvs = arrayOf(
            floatArrayOf(0f, 0f), floatArrayOf(uvScale, 0f),
            floatArrayOf(uvScale, uvScale), floatArrayOf(0f, uvScale)
        )
        val out = FloatArray(4 * 8)
        for (i in 0 until 4) {
            val b = i * 8
            out[b] = pts[i][0]; out[b + 1] = pts[i][1]; out[b + 2] = pts[i][2]
            out[b + 3] = n[0]; out[b + 4] = n[1]; out[b + 5] = n[2]
            out[b + 6] = uvs[i][0]; out[b + 7] = uvs[i][1]
        }
        return out
    }

    private fun uploadQuad(vbo: Int, ibo: Int, verts: FloatArray) {
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, verts.size * 4, glFloatBuffer(verts), GLES30.GL_STATIC_DRAW)
        val idx = shortArrayOf(0, 1, 2, 0, 2, 3)
        val ib = ByteBuffer.allocateDirect(idx.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer()
        ib.put(idx); ib.position(0)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, ibo)
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, idx.size * 2, ib, GLES30.GL_STATIC_DRAW)
    }

    private fun genBuf(): Int { val h = IntArray(1); GLES30.glGenBuffers(1, h, 0); return h[0] }

    private fun loadTex(path: String, fallback: Int): Int {
        val bmp = runCatching {
            appContext.assets.open(path).use { BitmapFactory.decodeStream(it) }
        }.getOrNull() ?: Bitmap.createBitmap(
            IntArray(64 * 64) { fallback }, 64, 64, Bitmap.Config.ARGB_8888
        )
        val h = IntArray(1); GLES30.glGenTextures(1, h, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, h[0])
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT)
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bmp, 0)
        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D)
        if (!bmp.isRecycled) bmp.recycle()
        return h[0]
    }
}

/** Full-screen character inspection sheet, opened by tapping the market art. */
@Composable
fun CharacterPreviewSheet(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val renderer = remember { CharacterPreviewRenderer(ctx.applicationContext) }
    var yaw by remember { mutableStateOf(18f) }
    var walking by remember { mutableStateOf(false) }
    val walkAnim by animateFloatAsState(
        if (walking) 1f else 0f, tween(420, easing = EaseInOutCubic), label = "previewWalk"
    )
    LaunchedEffect(yaw) { renderer.yawDegrees = yaw }
    LaunchedEffect(walkAnim) { renderer.walkAmount = walkAnim }

    val glView = remember {
        GLSurfaceView(ctx).apply {
            setEGLContextClientVersion(3)
            preserveEGLContextOnPause = true
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_RESUME -> glView.onResume()
                Lifecycle.Event.ON_PAUSE -> glView.onPause()
                else -> {}
            }
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs); glView.onPause() }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { glView },
            modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                detectDragGestures { change, drag ->
                    change.consume()
                    yaw -= drag.x * 0.4f
                }
            }
        )
        // Vignette to seat the scene into the UI.
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    listOf(Color.Transparent, Color.Black.copy(0.55f)), radius = 1200f
                )
            )
        )

        Row(
            Modifier.align(Alignment.TopStart).fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconGlyphButton(36.dp, Yellow, onClick = onClose) { drawCloseGlyph(it) }
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(R.string.char_preview_title),
                color = Yellow, fontSize = 13.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 2.sp
            )
        }

        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                stringResource(R.string.char_preview_rotate),
                color = TextDim, fontSize = 10.sp, letterSpacing = 1.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AnimToggle(stringResource(R.string.char_anim_idle), !walking) { walking = false }
                AnimToggle(stringResource(R.string.char_anim_walk), walking) { walking = true }
            }
        }
    }
}

@Composable
private fun AnimToggle(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Yellow.copy(0.18f) else Color.Black.copy(0.55f))
            .border(1.dp, if (selected) Yellow else BorderCol, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 9.dp)
    ) {
        Text(
            label, color = if (selected) Yellow else TextSec,
            fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

private fun DrawScope.drawCloseGlyph(c: Color) {
    val w = size.width; val h = size.height; val sw = size.minDimension * 0.11f
    drawLine(c, Offset(w * 0.28f, h * 0.28f), Offset(w * 0.72f, h * 0.72f), strokeWidth = sw, cap = StrokeCap.Round)
    drawLine(c, Offset(w * 0.72f, h * 0.28f), Offset(w * 0.28f, h * 0.72f), strokeWidth = sw, cap = StrokeCap.Round)
}
