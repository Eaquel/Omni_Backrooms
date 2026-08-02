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
import androidx.annotation.DrawableRes
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
// The market grids are LazyVerticalGrid, so itemsIndexed comes from grid.*.
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
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.onSizeChanged
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
fun OmniBackroomsApp(localeVm: AppLocaleVM = hiltViewModel()) {
    val language by localeVm.language.collectAsState()
    val ctx = LocalContext.current

    // Rebuild the entire UI tree when the language changes, with a Context whose
    // resources resolve in that language. This replaces the old activity
    // recreate: same effect, no flash, no back-stack disruption.
    val localisedContext = remember(language) { applyAppLanguage(ctx, language) }
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalContext provides localisedContext
    ) {
        key(language) { OmniBackroomsAppContent() }
    }
}

@Composable
private fun OmniBackroomsAppContent() {
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
            // The rest slide in from the side like Settings and Story. Half the
            // routes used to cut with no transition at all, which read as the
            // app dropping frames rather than as a deliberate change of screen.
            composable(
                Route.ROOM,
                enterTransition = { slideInHorizontally(tween(400)) { it } + fadeIn(tween(400)) },
                exitTransition  = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
            ) { Room(onJoined = { nav.navigate("${Route.GAME}?resume=false") }, onBack = { nav.popBackStack() }, onCreate = { nav.navigate(Route.CREATE_ROOM) }) }
            composable(
                Route.CREATE_ROOM,
                enterTransition = { slideInVertically(tween(380)) { it } + fadeIn(tween(380)) },
                exitTransition  = { slideOutVertically(tween(280)) { it } + fadeOut(tween(280)) }
            ) { CreateRoom(onCreated = { nav.popBackStack() }, onBack = { nav.popBackStack() }) }
            composable(
                Route.LEADERBOARD,
                enterTransition = { slideInHorizontally(tween(400)) { it } + fadeIn(tween(400)) },
                exitTransition  = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
            ) { LeaderboardScreen(onBack = { nav.popBackStack() }) }
            composable(
                Route.PROFILE,
                enterTransition = { slideInHorizontally(tween(400)) { -it } + fadeIn(tween(400)) },
                exitTransition  = { slideOutHorizontally(tween(300)) { -it } + fadeOut(tween(300)) }
            ) { ProfileScreen(onBack = { nav.popBackStack() }) }
            composable(
                Route.UI_EDITOR,
                // Scales up out of the settings row it was launched from, which
                // is the one screen where "this replaced what you were looking
                // at" is the right reading.
                enterTransition = { scaleIn(tween(360), initialScale = 0.92f) + fadeIn(tween(360)) },
                exitTransition  = { scaleOut(tween(260), targetScale = 0.94f) + fadeOut(tween(260)) }
            ) { UiEditor(onSave = { nav.popBackStack() }) }
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
                    OmniLog.w("Market", "load failed; using local data", e)
                    _state.update { it.copy(isLoading = false, error = null) }
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
    Looks     (R.string.market_tab_looks,      Icons.Default.Person),
    Frames    (R.string.market_tab_frames,     Icons.Default.CropSquare),
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
    val tab         : MarketTab           = MarketTab.Looks,
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
        loadTab(MarketTab.Looks); loadDaily(); loadProfile()
        viewModelScope.launch {
            cosmetics.observeOwnedFrames().collect { frames ->
                _state.update { it.copy(ownedIds = frames.map { f -> "frame_$f" }.toSet()) }
            }
        }
        // Omnium earned by surviving is banked locally, so it has to be added to
        // whatever the server thinks the player has — otherwise a run's payout is
        // invisible in the only place it can be spent.
        viewModelScope.launch {
            cosmetics.observeOmnium().collect { local ->
                localOmnium = local
                _state.update { it.copy(omniumBal = serverOmnium + local) }
            }
        }
    }

    private var serverOmnium = 0L
    private var localOmnium  = 0L

    private fun loadProfile() {
        viewModelScope.launch {
            runCatching { api.getProfile() }.onSuccess { p ->
                serverOmnium = p.omniumAmount
                _state.update {
                    it.copy(omniumBal = serverOmnium + localOmnium, souliumBal = p.souliumAmount, isVip = p.isVip)
                }
            }
        }
    }

    fun setTab(tab: MarketTab) {
        _state.update { it.copy(tab = tab) }
        // Looks is served from the item list like every other tab — routing it
        // to the character API meant the local character entry never showed.
        when (tab) { MarketTab.Daily -> return; else -> loadTab(tab) }
    }

    private fun loadTab(tab: MarketTab) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching { api.getMarketItems(tab.name.lowercase()) }
                .onSuccess { page ->
                    // An empty page would leave the tab blank; fall back so a
                    // silent/partial server is indistinguishable from offline.
                    val items = page.items.ifEmpty { fallbackItems(tab) }
                    _state.update { it.copy(isLoading = false, items = items) }
                }
                .onFailure { e ->
                    // The offline catalogue already covers this case, so the raw
                    // network/converter message is noise — log it, don't show it.
                    OmniLog.w("Market", "loadTab failed for $tab; using local catalogue", e)
                    _state.update { it.copy(isLoading = false, error = null, items = fallbackItems(tab)) }
                }
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
                .onFailure {
                    // No server: fall back to the local roster so the character
                    // is still browsable and inspectable offline.
                    OmniLog.w("Market", "getCharacters failed; using local roster", it)
                    _state.update { it.copy(charsLoading = false, characters = emptyList()) }
                }
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
                    listOf("halogen", "signal", "threshold").forEach { cosmetics.grantFrame(it) }
                }
                item.id == "daily_frame" -> cosmetics.grantFrame("halogen")
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
        MarketTab.Looks -> listOf(
            MarketItemDto(
                "char_anime", "Anime Kız", "Anime Girl",
                "Ana karakter — görsele dokunup inceleyebilirsin",
                "The main character — tap the art to inspect her",
                "characters", 0, "soulium", null, false, false, true, null
            )
        )
        // Frames are the one cosmetic that is visibly alive: each card renders the
        // real animated ring the player would equip, not a still of it.
        MarketTab.Frames -> listOf(
            MarketItemDto(
                "frame_halogen", "Halojen", "Halogen",
                "Tavandaki floresan gibi balast titreşimiyle yanıp söner",
                "Flickers on a failing ballast, like the ceiling fixtures",
                "frames", 0, "omnium", null, false, false, false, null
            ),
            MarketItemDto(
                "frame_signal", "Sinyal", "Signal",
                "Dönen tarama başı ve statik parazit halkası",
                "A sweeping scan head over a band of static",
                "frames", 0, "omnium", null, false, false, false, null
            ),
            MarketItemDto(
                "frame_threshold", "Eşik", "Threshold",
                "Ters yönde dönen, hiç tam kapanmayan iki kapı yayı",
                "Counter-rotating arcs of a door that never quite shuts",
                "frames", 0, "soulium", null, false, false, true, null
            ),
            MarketItemDto(
                "frame_default", "Standart", "Standard",
                "Sade metal halka — her zaman senin",
                "Plain metal ring — always yours",
                "frames", 0, "soulium", null, false, false, false, null
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

    /**
     * Deals rotate once per UTC day. Deriving the rotation from the day number
     * rather than storing it means every device shows the same offer on the
     * same day with no server involved, and it survives reinstalls.
     */
    private fun fallbackDaily(): List<MarketItemDto> {
        val day = (System.currentTimeMillis() / 86_400_000L).toInt()
        val pool = listOf(
            MarketItemDto(
                "daily_char_trial", "Anime Kız — 1 Saat", "Anime Girl — 1 Hour",
                "Bugün bir saatliğine ücretsiz dene", "Try her free for one hour today",
                "daily", 0, "soulium", null, false, false, true, null
            ),
            MarketItemDto(
                "daily_frame", "Günlük Çerçeve", "Daily Frame",
                "Bugüne özel görsel çerçeve", "Today only cosmetic frame",
                "daily", 0, "soulium", null, false, false, true, null
            ),
            MarketItemDto(
                "daily_trail", "Günlük İz", "Daily Trail",
                "Bugüne özel iz efekti", "Today only trail effect",
                "daily", 0, "soulium", null, false, false, true, null
            )
        )
        // Two of the three each day, rotating, so the tab is never identical
        // two days running.
        return listOf(pool[day % pool.size], pool[(day + 1) % pool.size])
    }

    /** Milliseconds until the daily rotation flips, for the countdown. */
    fun millisUntilDailyReset(): Long {
        val dayMs = 86_400_000L
        return dayMs - (System.currentTimeMillis() % dayMs)
    }
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
        /** Sprint is a deliberate act with a cost, not "push the stick further".
         *  Holding the run button roughly doubles pace and burns stamina. */
        const val SPRINT_MULT = 1.95f
        const val CROUCH_MULT = 0.42f
        /** Stamina per second, spent sprinting and recovered otherwise. */
        const val SPRINT_DRAIN = 22f
        /** Below this the player is too winded to sprint at all. */
        const val SPRINT_FLOOR = 5f
        /** Once the player is further than this from the exit it is re-anchored
         *  ahead of them. An endless world otherwise has no findable door. */
        const val EXIT_LEASH_M = 320f
        /** Omnium per minute survived, plus a bonus for actually getting out. */
        const val OMNIUM_PER_MINUTE = 12L
        const val OMNIUM_ESCAPE_BONUS = 150L
    }

    /** Grid for the currently loaded level; kept here (not just in GameState) so the
     *  entity spawner can reuse it without depending on StateFlow emission timing. */
    private var world: WorldInfo = WorldInfo.EMPTY

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
            world = WorldInfo.parse(bridge.generateLevel(roomBudget, depth = 0))
            OmniLog.i("Game", "infinite world cell=${world.cellSize} spawn=(${world.spawnX},${world.spawnZ}) exit=(${world.exitX},${world.exitZ})")

            val cfg = assetManager.getSpawnConfig(useDiff)
            spawnInitialEntities(bridge, world, cfg)

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
                world = world, exitX = world.exitX, exitZ = world.exitZ,
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
                //
                // Pace comes from three independent things, which is the point:
                // the stick's own deflection (walk slowly by pushing gently), the
                // run button, and the crouch state. Pushing the stick to its edge
                // no longer means sprinting — that is what the run button is for.
                val mx = moveX; val mz = moveZ
                val mag = kotlin.math.hypot(mx, mz).coerceAtMost(1f)
                val snapshot = _state.value
                val wantsSprint = sprinting && snapshot.stamina > SPRINT_FLOOR && !snapshot.isCrouching
                // A body that is going down does not get to keep walking.
                if (mag > 0.02f && !madnessRunning) {
                    val paceMult = when {
                        snapshot.isCrouching -> CROUCH_MULT
                        wantsSprint          -> SPRINT_MULT
                        else                 -> 1f
                    }
                    // mx/mz already carry the stick's magnitude, so gentle input
                    // gives gentle force — the response is proportional, linearly.
                    val force = MOVE_FORCE * paceMult
                    bridge.applyMovement(mx * force, 0f, mz * force)
                    // Footsteps track the actual pace, so a run sounds like one.
                    footstepTimer -= dt * mag * paceMult
                    if (footstepTimer <= 0f) {
                        footstepTimer = 0.45f
                        bridge.triggerFootstep(if (wantsSprint) 180f else 120f, 0.3f)
                    }
                }
                // Stamina is spent only on sprinting; walking is free. Recovery is
                // handled by applyTickToState, so this only ever subtracts.
                if (wantsSprint && mag > 0.02f) {
                    _state.update {
                        it.copy(stamina = (it.stamina - SPRINT_DRAIN * dt * mag).coerceAtLeast(0f))
                    }
                }
                // Running out of breath drops you back to a walk, and the HUD has
                // to say so — the button going dim is the only warning there is.
                val sprintingNow = wantsSprint && mag > 0.02f
                if (sprinting && snapshot.stamina <= SPRINT_FLOOR) sprinting = false
                if (snapshot.isSprinting != sprintingNow) {
                    _state.update { it.copy(isSprinting = sprintingNow) }
                }

                // Keep the exit reachable. Cheap enough to poll on a timer, and
                // polling means a player who wanders off is quietly given a new
                // door rather than left walking an endless corridor forever.
                exitCheckTimer -= dt
                if (exitCheckTimer <= 0f) {
                    exitCheckTimer = 2f
                    val cam = snapshot.camera
                    if (cam != null && snapshot.world.isValid) {
                        runCatching { bridge.relocateExit(cam.posX, cam.posZ, EXIT_LEASH_M) }
                            .getOrNull()
                            ?.takeIf { it.size >= 2 }
                            ?.let { e ->
                                if (e[0] != snapshot.exitX || e[1] != snapshot.exitZ) {
                                    _state.update { s -> s.copy(exitX = e[0], exitZ = e[1]) }
                                }
                            }
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
                if (!wasOver && _state.value.isGameOver) {
                    // Dying still pays for the time survived — just without the
                    // escape bonus. Surviving is the thing being rewarded.
                    val earned = omniumForRun(elapsedMs, escaped = false)
                    _state.update { it.copy(omniumEarned = earned) }
                    launch { runCatching { cosmetics.addOmnium(earned) } }
                    submitScoreToServer()
                }
                checkSanity(dt)
                delay(16)
            }
        }
    }

    private fun startEntitySpawner(difficulty: String, cfg: SpawnConfig) {
        entityJob = viewModelScope.launch {
            var timer = 0L
            while (isActive) {
                delay(5_000); timer += 5_000
                if (timer >= cfg.spawnIntervalMs && world.isValid) {
                    timer = 0
                    val cam = _state.value.camera
                    spawnOneRandomEntity(bridge, world, cam?.posX ?: world.spawnX, cam?.posZ ?: world.spawnZ, cfg)
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
    @Volatile private var sprinting = false
    private var footstepTimer = 0f
    private var pingSampleTimer = 0f
    private var exitCheckTimer = 0f
    /** Counts down once sanity hits zero. The break is deliberately not instant:
     *  the player gets a stretch of hallucination first, at a random moment. */
    private var madnessFuse = -1f
    private var madnessRunning = false

    fun onMove(dx: Float, dy: Float, dz: Float) {
        if (_state.value.spawnPhase != SpawnPhase.READY) return
        if (_state.value.isMadnessOver) return
        moveX = dx.coerceIn(-1f, 1f)
        moveZ = dz.coerceIn(-1f, 1f)
        if (dy != 0f) bridge.applyMovement(0f, dy * MOVE_FORCE, 0f)
    }

    fun onLook(dx: Float, dy: Float) {
        if (_state.value.isMadnessOver) return
        val sensitivity = cachedSensitivity
        bridge.cameraLook(dx, dy, sensitivity)
    }

    fun onJump() {
        if (_state.value.isCrouching) { setCrouch(false); return }
        bridge.applyMovement(0f, 26_000f, 0f)
    }

    /** Crouch is a state, not an impulse. The old version pushed the body
     *  downward, which on the ground is exactly nothing — the button did not
     *  visibly do anything because there was nothing for it to do. */
    fun setCrouch(crouched: Boolean) {
        if (_state.value.isCrouching == crouched) return
        runCatching { bridge.setCrouch(crouched) }
        if (crouched) sprinting = false
        _state.update { it.copy(isCrouching = crouched, isSprinting = if (crouched) false else it.isSprinting) }
    }

    fun toggleCrouch() = setCrouch(!_state.value.isCrouching)

    fun setSprint(on: Boolean) {
        val allowed = on && _state.value.stamina > SPRINT_FLOOR && !_state.value.isCrouching
        sprinting = allowed
        if (_state.value.isSprinting != allowed) _state.update { it.copy(isSprinting = allowed) }
    }

    fun toggleFlashlight() { _state.update { it.copy(flashlightOn = !it.flashlightOn) } }
    fun togglePause() {
        val nowPaused = !_state.value.isPaused
        _state.update { it.copy(isPaused = nowPaused) }
        // Pausing has to silence the engine too; previously only leaving the
        // screen did, so the ambience kept playing behind the pause menu.
        if (nowPaused) runCatching { bridge.setAmbienceLevel(0f); bridge.setHumVolume(0f) }
        else applyAudioLevels()
    }

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
        if (!s.world.isValid) return   // nothing meaningful to resume yet
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
        if (canEscape && !_state.value.isMadnessOver) {
            val earned = omniumForRun(elapsedMs, escaped = true)
            _state.update { it.copy(isEscaped = true, omniumEarned = earned) }
            viewModelScope.launch { runCatching { cosmetics.addOmnium(earned) } }
            submitScoreToServer()
        }
    }

    /** Survival is the whole point of the mode, so it is what pays. */
    private fun omniumForRun(elapsed: Long, escaped: Boolean): Long {
        val minutes = elapsed / 60_000f
        val base = (minutes * OMNIUM_PER_MINUTE).toLong()
        val bonus = if (escaped) OMNIUM_ESCAPE_BONUS else 0L
        return (base + bonus).coerceAtLeast(0L)
    }

    /**
     * Sanity's endgame. At zero the world does not simply stop — the player gets
     * a stretch of creeping hallucination first, then at an unpredictable moment
     * the body goes down: the view drops to the floor, the camera comes to rest
     * on its side, and that is the run.
     *
     * The delay is randomised precisely so it cannot be waited out. Knowing the
     * exact second it arrives would make it a timer instead of a threat.
     */
    private fun checkSanity(dt: Float) {
        val s = _state.value
        if (s.isMadnessOver || s.isGameOver || s.isEscaped) return

        // Hallucination ramps in over the last stretch of sanity, so the screen
        // starts lying to the player before it takes them.
        val target = ((18f - s.sanity) / 18f).coerceIn(0f, 1f)
        if (kotlin.math.abs(target - s.madness) > 0.005f) {
            _state.update { it.copy(madness = it.madness + (target - it.madness) * (dt * 1.5f)) }
        }

        if (s.sanity > 0f) { madnessFuse = -1f; return }
        if (madnessFuse < 0f) {
            // Somewhere between three and twelve seconds after the mind goes.
            madnessFuse = 3f + (Math.random() * 9f).toFloat()
            runCatching { bridge.triggerMonster(1f) }
            return
        }
        madnessFuse -= dt
        if (madnessFuse <= 0f && !madnessRunning) playMadnessCollapse()
    }

    private fun playMadnessCollapse() {
        if (madnessRunning) return
        madnessRunning = true
        moveX = 0f; moveZ = 0f; sprinting = false
        viewModelScope.launch {
            _state.update { it.copy(madness = 1f) }
            runCatching { bridge.triggerFootstep(40f, 1f) }
            // The fall: eye height collapses to the floor while the camera rolls
            // onto its side over the same second, so the two read as one motion.
            val steps = 30
            for (i in 1..steps) {
                val t = i / steps.toFloat()
                val eased = t * t                       // accelerating, like a drop
                _state.update {
                    it.copy(eyeOffset = -1.55f * eased, cameraTilt = 78f * eased)
                }
                delay(24)
            }
            _state.update {
                it.copy(eyeOffset = -1.55f, cameraTilt = 78f, isMadnessOver = true, madness = 1f)
            }
            runCatching { cosmetics.addOmnium(omniumForRun(elapsedMs, escaped = false)) }
            _state.update { it.copy(omniumEarned = omniumForRun(elapsedMs, escaped = false)) }
            submitScoreToServer()
        }
    }

    fun onDamageEntity(id: Int) {
        bridge.damageEntity(id, 25f); kills++; score += 100L
        _state.update { it.copy(kills = kills, score = score) }
    }

    /** Fetches one chunk from the native field. Called from the GL thread, which
     *  is safe: the field is stateless and the JNI call only reads. */
    fun fetchChunk(chunkX: Int, chunkZ: Int): WorldChunk? {
        val w = _state.value.world
        if (!w.isValid) return null
        return WorldChunk.parse(chunkX, chunkZ, w.chunkCells, bridge.generateChunk(chunkX, chunkZ))
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
    val localOmnium by lobbyVm.omnium.collectAsState()
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
                    CurrencyChip(OmniumCol, profile.omniumAmount + localOmnium, isOmnium = true)
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
            IconResButton(40.dp, R.drawable.ic_leaderboard, TextSec, onClick = { toast = comingSoon })
            IconResButton(40.dp, R.drawable.ic_settings,    Yellow,  onClick = onSettings)
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
            RailItem(stringResource(R.string.menu_market),   R.drawable.ic_market,      CrtAmber,   onMarket)
            RailItem(stringResource(R.string.menu_story),     R.drawable.ic_story,       Yellow,     onStory)
            RailItem(stringResource(R.string.menu_abilities), R.drawable.ic_abilities,   TextSec)    { toast = comingSoon }
            RailItem(stringResource(R.string.menu_season),    R.drawable.ic_season,      SouliumCol) { toast = comingSoon }
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
uniform vec3 uCamPos;
uniform float uFogDensity; uniform vec3 uFogColor; uniform float uFlicker;
uniform float uBumpStrength; uniform float uBumpTexel;
uniform vec3 uLampTint;
out vec4 fragColor;
void main(){
    vec4 tex = texture(uTex, vUV);

    // Derive a surface normal from the texture's own luminance slope, then
    // rotate it into the face's tangent frame. Cheap bump mapping with no
    // extra texture: the albedo doubles as a height field.
    vec3 n = normalize(vNormal);
    if (uBumpStrength > 0.001) {
        vec2 texel = vec2(uBumpTexel);
        float hL = dot(texture(uTex, vUV - vec2(texel.x, 0.0)).rgb, vec3(0.299, 0.587, 0.114));
        float hR = dot(texture(uTex, vUV + vec2(texel.x, 0.0)).rgb, vec3(0.299, 0.587, 0.114));
        float hD = dot(texture(uTex, vUV - vec2(0.0, texel.y)).rgb, vec3(0.299, 0.587, 0.114));
        float hU = dot(texture(uTex, vUV + vec2(0.0, texel.y)).rgb, vec3(0.299, 0.587, 0.114));
        // Tangent frame from the geometric normal: walls are axis-aligned, so
        // picking the least-aligned world axis gives a stable tangent.
        vec3 up = abs(n.y) > 0.9 ? vec3(0.0, 0.0, 1.0) : vec3(0.0, 1.0, 0.0);
        vec3 tangent = normalize(cross(up, n));
        vec3 bitangent = cross(n, tangent);
        vec3 bump = tangent * (hL - hR) + bitangent * (hD - hU);
        n = normalize(n + bump * uBumpStrength);
    }

    // ---- Architectural detail ---------------------------------------------
    // Anchored to WORLD position, never to UVs or cell indices, so the grid runs
    // dead straight across the whole level and cannot break at a cell or chunk
    // boundary. This is what turns three tiling swatches into a room: the
    // suspended ceiling's T-bar grid and the carpet-tile seams are most of what
    // the eye actually uses to read an office interior.
    vec3 albedo = tex.rgb;
    float dist = length(uCamPos - vWorldPos);
    // Detail fades with distance so the grid never aliases into moire.
    float detailFade = 1.0 - smoothstep(12.0, 34.0, dist);

    if (n.y < -0.5) {
        // Ceiling: 1.6 m tiles in an aluminium T-bar grid. The rails catch the
        // light rather than losing it, which is why they read as metal.
        vec2 g = fract(vWorldPos.xz / 1.6);
        vec2 d = min(g, 1.0 - g);
        float rail = 1.0 - smoothstep(0.008, 0.030, min(d.x, d.y));
        albedo = mix(albedo, albedo * 1.30 + vec3(0.035), rail * detailFade);
        // Sag: each tile dips slightly toward its middle, so a big ceiling is
        // not a mathematically flat plane.
        float sag = 1.0 - 0.05 * (1.0 - min(d.x, d.y) * 4.0);
        albedo *= mix(1.0, sag, detailFade);
    } else if (n.y > 0.5) {
        // Floor: 0.8 m carpet tiles, seams darker and the tiles alternating in
        // pile direction — the checker is subtle but it is exactly what stops a
        // large carpet reading as one flat sheet of colour.
        vec2 t = vWorldPos.xz / 0.8;
        vec2 g = fract(t);
        vec2 d = min(g, 1.0 - g);
        float seam = 1.0 - smoothstep(0.004, 0.022, min(d.x, d.y));
        float weave = mod(floor(t.x) + floor(t.y), 2.0);
        albedo *= mix(1.0, mix(0.985, 1.015, weave), detailFade);
        albedo = mix(albedo, albedo * 0.80, seam * detailFade);
    } else {
        // Walls: vertical panel joints on a 1.6 m module, plus a damp stain
        // creeping up from the skirting. Both are static — nothing here depends
        // on where the camera is.
        float u = abs(n.x) > 0.5 ? vWorldPos.z : vWorldPos.x;
        float g = fract(u / 1.6);
        float joint = 1.0 - smoothstep(0.004, 0.018, min(g, 1.0 - g));
        albedo = mix(albedo, albedo * 0.74, joint * detailFade);
        float damp = (1.0 - smoothstep(0.0, 0.55, vWorldPos.y)) * 0.16;
        albedo *= 1.0 - damp * detailFade;
    }

    // ---- Fully baked lighting -------------------------------------------
    // There is no per-fragment light source here: no flashlight cone, no
    // view-dependent specular, nothing that has to be recomputed because the
    // camera moved. vLight is a per-VERTEX value the engine gathered from every
    // fluorescent within reach of that point and the mesher interpolates across
    // the face — so brightness comes FROM the tubes, the way it does in the
    // lobby, and there is nowhere for a band to form.
    //
    // Direction still matters, but only as a fixed surface response: the floor
    // catches the most from a luminous ceiling, the ceiling panel IS the
    // emitter, walls take it at a graze.
    float facing = abs(n.y) * 0.55 + 0.45;
    float lit = 0.09 + facing * vLight * uFlicker * 1.30;

    // Cheap baked AO: darken wall surfaces near the floor seam so geometry reads
    // as grounded instead of floating tiles. Skipped on floor/ceiling (upward or
    // downward normals) since those aren't touching a base seam.
    float wallFactor = 1.0 - abs(n.y);
    float groundAO = mix(1.0, mix(0.78, 1.0, smoothstep(0.0, 1.4, vWorldPos.y)), wallFactor);

    // Fluorescent tubes are not white. Tinting by how strongly a surface is lit
    // keeps the shadowed corners neutral and the bright floor sickly-warm, which
    // is the single most recognisable thing about this palette.
    vec3 lampMix = mix(vec3(1.0), uLampTint, clamp(vLight * 0.75, 0.0, 1.0));
    vec3 col = albedo * lit * groundAO * lampMix;

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

/**
 * The way out. A doorway-shaped slab that turns to face the player, so it reads
 * as a door from every approach angle in a maze with no fixed sightlines.
 */
private const val OMNI_EXIT_VERT = """#version 300 es
layout(location=0) in vec2 aCorner;
uniform mat4 uVP; uniform vec3 uCenter; uniform vec3 uRight; uniform float uWidth; uniform float uHeight;
out vec2 vUV;
void main(){
    vec3 worldPos = uCenter + uRight*(aCorner.x*uWidth) + vec3(0.0, (aCorner.y*0.5+0.5)*uHeight, 0.0);
    vUV = aCorner*0.5 + 0.5;
    gl_Position = uVP * vec4(worldPos, 1.0);
}
"""

private const val OMNI_EXIT_FRAG = """#version 300 es
precision mediump float;
in vec2 vUV;
uniform float uTime; uniform float uNear;
out vec4 fragColor;
void main(){
    // A failing ballast, not a smooth pulse: two detuned sines gated hard, the
    // same rhythm the level's broken fixtures run on. This is the cue the player
    // is looking for across a hundred identical corridors, so it has to read as
    // wrong rather than as decoration.
    float b1 = sin(uTime * 8.3);
    float b2 = sin(uTime * 23.7 + 1.3);
    float ballast = (b1 * b2 > -0.55) ? 1.0 : 0.28;
    ballast *= 0.82 + 0.18 * sin(uTime * 2.1);

    // Door plate, inset from the frame.
    vec2 d = abs(vUV - 0.5);
    float plate = step(d.x, 0.40) * step(d.y, 0.46);
    float frame = step(d.x, 0.50) * step(d.y, 0.50) - plate;

    // Horizontal bands drifting up the plate, like light spilling round a door
    // that will not stay shut.
    float bands = 0.55 + 0.45 * sin(vUV.y * 26.0 - uTime * 3.4);

    vec3 glow = vec3(1.0, 0.96, 0.72);
    vec3 col = glow * (plate * bands * 0.85 + frame * 1.35) * ballast;
    // Brightens as you close in, so the last stretch is unmistakable.
    col *= 0.75 + uNear * 0.75;

    float a = clamp(plate * 0.80 + frame, 0.0, 1.0) * ballast;
    if (a < 0.02) discard;
    fragColor = vec4(col, a);
}
"""

private const val OMNI_POST_VERT = """#version 300 es
layout(location=0) in vec2 aPos;
out vec2 vUV;
void main(){ vUV = aPos*0.5+0.5; gl_Position = vec4(aPos, 0.0, 1.0); }
"""

/**
 * Bloom, bright pass.
 *
 * A fluorescent tube in a dim corridor does not stop at its own outline — it
 * washes the ceiling around it, hazes the air and blows out toward the camera.
 * Baked vertex light alone cannot do any of that: the fixture quad was as bright
 * as the shader allowed and still sat there as a flat white rectangle. This is
 * the pass that makes the lights actually emit.
 *
 * Soft knee rather than a hard threshold, so a surface drifting past the cutoff
 * eases into the bloom instead of popping.
 */
private const val OMNI_BRIGHT_FRAG = """#version 300 es
precision mediump float;
in vec2 vUV;
uniform sampler2D uScene;
uniform float uThreshold; uniform float uKnee;
out vec4 fragColor;
void main(){
    vec3 c = texture(uScene, vUV).rgb;
    float lum = dot(c, vec3(0.2126, 0.7152, 0.0722));
    // Quadratic knee around the threshold.
    float soft = clamp(lum - uThreshold + uKnee, 0.0, 2.0 * uKnee);
    soft = soft * soft / (4.0 * uKnee + 0.0001);
    float contribution = max(soft, lum - uThreshold) / max(lum, 0.0001);
    fragColor = vec4(c * contribution, 1.0);
}
"""

/** Separable Gaussian. Run once horizontally, once vertically, per mip level. */
private const val OMNI_BLUR_FRAG = """#version 300 es
precision mediump float;
in vec2 vUV;
uniform sampler2D uSource;
uniform vec2 uDir;          // texel-sized step, one axis at a time
out vec4 fragColor;
void main(){
    // 9-tap, weights from a sigma≈2 Gaussian. Linear filtering means the
    // off-centre taps each fetch two texels for the price of one.
    vec3 sum = texture(uSource, vUV).rgb * 0.227027;
    sum += texture(uSource, vUV + uDir * 1.3846).rgb * 0.316216;
    sum += texture(uSource, vUV - uDir * 1.3846).rgb * 0.316216;
    sum += texture(uSource, vUV + uDir * 3.2308).rgb * 0.070270;
    sum += texture(uSource, vUV - uDir * 3.2308).rgb * 0.070270;
    fragColor = vec4(sum, 1.0);
}
"""

private const val OMNI_POST_FRAG = """#version 300 es
precision mediump float;
in vec2 vUV;
uniform sampler2D uScene;
uniform sampler2D uBloom;
uniform float uTime; uniform float uFlicker; uniform float uVhsStrength; uniform vec2 uResolution;
uniform float uColorBlindMix; uniform vec3 uColorBlindAxis;
uniform float uFlashOn; uniform float uMadness; uniform float uBloomStrength;
uniform float uExposure;
out vec4 fragColor;
float rand(vec2 co){ return fract(sin(dot(co, vec2(12.9898,78.233))) * 43758.5453); }
// Filmic tonemap (ACES approximation). Without it every bloomed highlight
// clipped to flat white and the whole frame lost its shoulder.
vec3 tonemap(vec3 x){
    const float a = 2.51, b = 0.03, c = 2.43, d = 0.59, e = 0.14;
    return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
}
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

    // Bloom, added before tonemapping so the shoulder rolls the glow off the way
    // a camera would rather than letting it clip to a flat white blob.
    col += texture(uBloom, uv).rgb * uBloomStrength;
    col = tonemap(col * uExposure);

    float scan = sin(uv.y*uResolution.y*1.4 + uTime*6.0) * 0.04 * uVhsStrength;
    col -= scan;
    float grain = (rand(uv*uResolution + uTime) - 0.5) * 0.05 * uVhsStrength;
    col += grain;

    vec2 vig = uv - 0.5;
    float vigAmt = 1.0 - dot(vig,vig)*1.1;
    col *= clamp(vigAmt, 0.0, 1.0);
    col *= (0.55 + 0.45*uFlicker);

    // Torch. The scene itself is lit entirely by the baked fluorescent bake, so
    // this is a screen-space lift of what the beam would fall on rather than a
    // real light: no per-fragment normals, no cost that scales with geometry.
    if (uFlashOn > 0.5) {
        vec2 beam = (uv - vec2(0.5, 0.47)) * vec2(uResolution.x/uResolution.y, 1.0);
        float cone = 1.0 - smoothstep(0.10, 0.62, length(beam));
        col += col * cone * 0.85 + vec3(0.06, 0.055, 0.042) * cone;
    }

    // Losing your mind: the frame smears, drifts off its own colour axis and
    // breathes. Driven from sanity, so it creeps in rather than switching on.
    if (uMadness > 0.001) {
        vec2 warp = vec2(sin(uv.y * 24.0 + uTime * 2.7), cos(uv.x * 19.0 + uTime * 3.3)) * 0.012 * uMadness;
        vec3 smear = texture(uScene, clamp(uv + warp, 0.0, 1.0)).rgb;
        col = mix(col, smear, 0.6 * uMadness);
        float luma = dot(col, vec3(0.299, 0.587, 0.114));
        col = mix(col, vec3(luma) * vec3(1.25, 0.62, 0.62), 0.45 * uMadness);
        col *= 0.80 + 0.20 * sin(uTime * 5.1);
    }

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
    /** "first" or "third". Third pulls the camera back and draws the avatar. */
    @Volatile var cameraView: String = "first"

    // Avatar resources, loaded only when third person is actually available.
    private var charProgram = 0
    private var charVbo = 0; private var charIbo = 0; private var charIndexCount = 0
    private var charTex = 0
    private var shaftProgram = 0
    private var sMVP = 0; private var sFlicker = 0; private var sTint = 0
    private var cMVP = 0; private var cModel = 0; private var cTime = 0; private var cWalk = 0
    private var cTexU = 0; private var cIsChar = 0
    private val avatarModelM = FloatArray(16)
    private val avatarMvpM = FloatArray(16)
    private var lastAvatarX = 0f
    private var lastAvatarZ = 0f
    /** Smoothed ground speed in m/s, used to drive the avatar's gait. */
    private var avatarSpeed = 0f

    /** Measured on the GL thread and read by the HUD. Exponentially smoothed so
     *  the number is readable instead of flickering every frame. */
    @Volatile var measuredFps: Float = 0f
        private set
    private var fpsAccum = 0f

    private var sceneProgram = 0; private var billboardProgram = 0; private var postProgram = 0; private var shadowProgram = 0
    private var uMVP = 0; private var uTex = 0; private var uCamPos = 0
    private var uFogDensity = 0
    private var uFogColor = 0; private var uFlicker = 0
    private var uBumpStrength = 0; private var uBumpTexel = 0; private var uLampTint = 0
    private var bVP = 0; private var bCenter = 0; private var bRight = 0; private var bUp = 0
    private var bSize = 0; private var bColor = 0; private var bAlert = 0; private var bAlpha = 0; private var bColorBlind = 0
    private var pScene = 0; private var pTime = 0; private var pFlicker = 0; private var pVhs = 0; private var pRes = 0
    private var pCbMix = 0; private var pCbAxis = 0; private var pFlashOn = 0; private var pMadness = 0
    private var pBloomTex = 0; private var pBloomStrength = 0; private var pExposure = 0
    private var brightProgram = 0; private var blurProgram = 0
    private var brScene = 0; private var brThreshold = 0; private var brKnee = 0
    private var blSource = 0; private var blDir = 0
    // Half-resolution ping-pong pair for the bloom blur.
    private var bloomFbo = IntArray(2); private var bloomTex = IntArray(2)
    private var bloomW = 1; private var bloomH = 1
    private var sVP = 0; private var sCenter = 0; private var sSize = 0; private var sAlpha = 0
    private var exitProgram = 0
    private var xVP = 0; private var xCenter = 0; private var xRight = 0
    private var xWidth = 0; private var xHeight = 0; private var xTime = 0; private var xNear = 0

    private var floorTex = 0; private var wallTex = 0; private var roofTex = 0
    /** Flat near-white for the light fittings. Drawing them on the ceiling tile
     *  tinted the tubes with the ceiling's own grain, which is the one surface
     *  in the level that must not look like the ceiling. */
    private var lampTex = 0
    // Streamed chunks, keyed by chunk coordinate. Built when the player comes
    // near and released when they leave, because an unbounded world can never
    // be one buffer.
    private val chunkMeshes = HashMap<Long, ChunkMesh>()
    /** Chunks this far (in chunks) from the player are kept resident. */
    private val chunkRadius = 2
    @Volatile var chunkProvider: ((Int, Int) -> WorldChunk?)? = null

    private var billboardVbo = 0
    private var postVbo = 0


    private var fbo = 0; private var fboTex = 0; private var fboDepth = 0
    private var surfaceW = 1; private var surfaceH = 1
    private var renderW = 1; private var renderH = 1
    private var lastResScale = -1f

    /** Avatar height in metres. The mesh is normalised to unit height. */
    private val AVATAR_SCALE = 1.7f

    private val projM = FloatArray(16)
    private val viewM = FloatArray(16)
    private val vpM   = FloatArray(16)
    private val rollM = FloatArray(16)
    private val rolledViewM = FloatArray(16)
    private val startNanos = System.nanoTime()
    private var lastFrameNanos = 0L

    // Smoothed (rendered) camera state: the sim advances in discrete ~60Hz steps,
    // but the display can refresh faster (90/120Hz). Exponentially chasing the
    // latest snapshot each frame removes visible stepping without adding input lag.
    private var smoothX = 0f; private var smoothY = 1.7f; private var smoothZ = 0f
    private var smoothYaw = 0f; private var smoothPitch = 0f
    private var smoothTilt = 0f
    private var smoothInit = false
    private val smoothEntities = HashMap<Int, FloatArray>() // id -> [x,y,z]

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // The EGL context is destroyed when the app is backgrounded, so every GL
        // object below is being (re)created from scratch here. Invalidate the
        // cached mesh key too, or the level geometry is never re-uploaded and
        // the screen comes back black with only the HUD drawn over it.
        // The GL context was destroyed, so every cached chunk's buffers are
        // gone with it. Drop the cache rather than draw dangling handles. The
        // avatar buffers are recreated below in the same pass.
        chunkMeshes.clear()
        charIndexCount = 0
        smoothInit = false
        smoothTilt = 0f
        avatarSpeed = 0f
        smoothEntities.clear()
        // Every framebuffer name below belonged to the destroyed context. Zero
        // them so the rebuild allocates fresh rather than deleting names that
        // now mean something else.
        fbo = 0; fboTex = 0; fboDepth = 0
        bloomFbo = IntArray(2); bloomTex = IntArray(2)
        renderW = 1; renderH = 1; lastResScale = -1f

        GLES30.glClearColor(0.02f, 0.02f, 0.017f, 1f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        sceneProgram = linkGlProgram(OMNI_SCENE_VERT, OMNI_SCENE_FRAG)
        uMVP = GLES30.glGetUniformLocation(sceneProgram, "uMVP")
        uTex = GLES30.glGetUniformLocation(sceneProgram, "uTex")
        uCamPos = GLES30.glGetUniformLocation(sceneProgram, "uCamPos")
        uFogDensity = GLES30.glGetUniformLocation(sceneProgram, "uFogDensity")
        uFogColor = GLES30.glGetUniformLocation(sceneProgram, "uFogColor")
        uFlicker = GLES30.glGetUniformLocation(sceneProgram, "uFlicker")
        uBumpStrength = GLES30.glGetUniformLocation(sceneProgram, "uBumpStrength")
        uBumpTexel = GLES30.glGetUniformLocation(sceneProgram, "uBumpTexel")
        uLampTint = GLES30.glGetUniformLocation(sceneProgram, "uLampTint")

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
        pFlashOn = GLES30.glGetUniformLocation(postProgram, "uFlashOn")
        pMadness = GLES30.glGetUniformLocation(postProgram, "uMadness")
        pBloomTex = GLES30.glGetUniformLocation(postProgram, "uBloom")
        pBloomStrength = GLES30.glGetUniformLocation(postProgram, "uBloomStrength")
        pExposure = GLES30.glGetUniformLocation(postProgram, "uExposure")

        brightProgram = linkGlProgram(OMNI_POST_VERT, OMNI_BRIGHT_FRAG)
        brScene = GLES30.glGetUniformLocation(brightProgram, "uScene")
        brThreshold = GLES30.glGetUniformLocation(brightProgram, "uThreshold")
        brKnee = GLES30.glGetUniformLocation(brightProgram, "uKnee")

        blurProgram = linkGlProgram(OMNI_POST_VERT, OMNI_BLUR_FRAG)
        blSource = GLES30.glGetUniformLocation(blurProgram, "uSource")
        blDir = GLES30.glGetUniformLocation(blurProgram, "uDir")

        exitProgram = linkGlProgram(OMNI_EXIT_VERT, OMNI_EXIT_FRAG)
        xVP = GLES30.glGetUniformLocation(exitProgram, "uVP")
        xCenter = GLES30.glGetUniformLocation(exitProgram, "uCenter")
        xRight = GLES30.glGetUniformLocation(exitProgram, "uRight")
        xWidth = GLES30.glGetUniformLocation(exitProgram, "uWidth")
        xHeight = GLES30.glGetUniformLocation(exitProgram, "uHeight")
        xTime = GLES30.glGetUniformLocation(exitProgram, "uTime")
        xNear = GLES30.glGetUniformLocation(exitProgram, "uNear")

        shaftProgram = linkGlProgram(OMNI_SHAFT_VERT, OMNI_SHAFT_FRAG)
        sMVP = GLES30.glGetUniformLocation(shaftProgram, "uMVP")
        sFlicker = GLES30.glGetUniformLocation(shaftProgram, "uFlicker")
        sTint = GLES30.glGetUniformLocation(shaftProgram, "uTint")

        shadowProgram = linkGlProgram(OMNI_SHADOW_VERT, OMNI_SHADOW_FRAG)
        sVP = GLES30.glGetUniformLocation(shadowProgram, "uVP")
        sCenter = GLES30.glGetUniformLocation(shadowProgram, "uCenter")
        sSize = GLES30.glGetUniformLocation(shadowProgram, "uSize")
        sAlpha = GLES30.glGetUniformLocation(shadowProgram, "uAlpha")

        // Avatar: shares the preview's shader, which already implements the
        // joint rotation that breaks the source mesh's T-pose.
        runCatching {
            charProgram = linkGlProgram(OMNI_PREVIEW_VERT, OMNI_PREVIEW_FRAG)
            cMVP = GLES30.glGetUniformLocation(charProgram, "uMVP")
            cModel = GLES30.glGetUniformLocation(charProgram, "uModel")
            cTime = GLES30.glGetUniformLocation(charProgram, "uTime")
            cWalk = GLES30.glGetUniformLocation(charProgram, "uWalk")
            cTexU = GLES30.glGetUniformLocation(charProgram, "uTex")
            cIsChar = GLES30.glGetUniformLocation(charProgram, "uIsCharacter")

            CharacterMesh.load(appContext, "Models/Anime_Character.omesh")?.let { mesh ->
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
            charTex = loadOmniTexture("Models/Anime_Texture.png", 0xFFE8D5C8.toInt())
        }.onFailure { OmniLog.e("Render", "avatar setup failed; third person unavailable", it) }

        floorTex = loadOmniTexture("Level_0/Floor.png", 0xFF3A3020.toInt())
        wallTex  = loadOmniTexture("Level_0/Wall.png",  0xFF4A4030.toInt())
        roofTex  = loadOmniTexture("Level_0/Roof.png",  0xFF23210F.toInt())
        lampTex  = uploadTexture(solidTile(0xFFF4F0E2.toInt()))

        val quadCorners = floatArrayOf(-1f,-1f, 1f,-1f, -1f,1f, 1f,1f)
        billboardVbo = genGlBuffer()
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, billboardVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, quadCorners.size*4, glFloatBuffer(quadCorners), GLES30.GL_STATIC_DRAW)

        postVbo = genGlBuffer()
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, postVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, quadCorners.size*4, glFloatBuffer(quadCorners), GLES30.GL_STATIC_DRAW)

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

        val world = state.world
        if (world.isValid && cam != null) streamChunks(world, cam.posX, cam.posZ)

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

            // Third person pulls the camera back along the view axis and lifts
            // it, so the avatar sits in the lower third of frame.
            val thirdPerson = cameraView == "third" && charIndexCount > 0
            val camDist = if (thirdPerson) 2.9f else 0f
            val camLift = if (thirdPerson) 0.45f else 0f
            val eyeX = smoothX - fx * camDist
            val eyeZ = smoothZ - fz * camDist
            val camY = eyeY - fy * camDist + camLift
            Matrix.setLookAtM(
                viewM, 0,
                eyeX, camY, eyeZ,
                smoothX + fx, eyeY + fy, smoothZ + fz,
                0f, 1f, 0f
            )
            // Camera roll. Applied after the look-at rather than by tilting the
            // up vector, because a tilted up vector degenerates when the player
            // is also looking straight down — which is exactly where a body that
            // has just hit the floor ends up.
            smoothTilt += (state.cameraTilt - smoothTilt) * chase
            if (kotlin.math.abs(smoothTilt) > 0.01f) {
                Matrix.setIdentityM(rollM, 0)
                Matrix.rotateM(rollM, 0, smoothTilt, 0f, 0f, 1f)
                Matrix.multiplyMM(rolledViewM, 0, rollM, 0, viewM, 0)
                System.arraycopy(rolledViewM, 0, viewM, 0, 16)
            }
            Matrix.multiplyMM(vpM, 0, projM, 0, viewM, 0)

            val fogDensity = (if (rs.fogEnabled) 1.0f else 0.15f) * fogMult
            val flicker = state.flickerIntensity.coerceIn(0.55f, 1f)
            val bump = when (rs.quality) { "low" -> 0f; "high" -> 1.6f; else -> 0.9f }
            drawLevel(vpM, eyeX, camY, eyeZ, fogDensity, flicker, bump)

            // How fast the body is actually travelling, in metres per second and
            // smoothed. The avatar's gait is driven from this, so the limbs move
            // in proportion to the walk instead of snapping between two poses.
            val stepX = (cam.posX - lastAvatarX); val stepZ = (cam.posZ - lastAvatarZ)
            lastAvatarX = cam.posX; lastAvatarZ = cam.posZ
            val instantSpeed = kotlin.math.hypot(stepX, stepZ) / dt
            avatarSpeed += (instantSpeed - avatarSpeed) * (1f - kotlin.math.exp(-dt * 9f))

            // The avatar, only in third person — in first person the camera is
            // inside its head and it would fill the screen.
            if (thirdPerson) {
                // Feet, not eyes. The model is authored standing on y=0 and gets
                // translated by this point, so handing it the camera height left
                // her hanging a full body-length above the floor.
                val feetY = smoothY - (cam.eyeHeight + state.eyeOffset)
                drawAvatar(
                    vpM, smoothX, feetY, smoothZ, smoothYaw,
                    timeSec, walk = (avatarSpeed / 3.6f).coerceIn(0f, 1.6f)
                )
            }

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
            // Entities are the Backrooms creatures — they stay billboards. The
            // character model is the player's own avatar and belongs to the
            // preview screen, not to the corridors.
            drawEntities(vpM, state.entities, yawRad.toFloat(), smoothX, smoothZ, entityRange, timeSec, cbMix)

            // The way out, drawn last so its glow blends over everything.
            if (state.world.isValid) {
                drawExitDoor(vpM, state.exitX, state.exitZ, smoothX, smoothZ, state.world.height, timeSec)
            }
        }

        // Bloom before the composite: the lights have to be extracted from the
        // scene buffer while it still holds raw, un-tonemapped brightness.
        val bloomPasses = when (rs.quality) { "low" -> 0; "high" -> 3; else -> 2 }
        if (bloomPasses > 0) renderBloom(bloomPasses)

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, surfaceW, surfaceH)
        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        drawPost(
            timeSec, state.flickerIntensity, (if (rs.vhsEnabled) 1f else 0f) * postStrength,
            cbMix, cbAxis, state.flashlightOn, state.madness,
            bloomStrength = if (bloomPasses > 0) 0.85f else 0f
        )
    }

    /**
     * The exit. Yaw-billboarded so it presents its face from any approach, and
     * drawn additively without writing depth so the glow bleeds around the frame
     * instead of cutting a hard silhouette out of the corridor behind it.
     */
    private fun drawExitDoor(
        vp: FloatArray, exitX: Float, exitZ: Float,
        camX: Float, camZ: Float, ceiling: Float, timeSec: Float
    ) {
        val dx = exitX - camX; val dz = exitZ - camZ
        val dist = kotlin.math.hypot(dx, dz)
        // Far beyond the fog there is nothing to see anyway.
        if (dist > 48f) return

        GLES30.glUseProgram(exitProgram)
        GLES30.glUniformMatrix4fv(xVP, 1, false, vp, 0)
        GLES30.glUniform3f(xCenter, exitX, 0.03f, exitZ)
        // Face the player: right vector is perpendicular to the line of sight.
        val inv = if (dist > 0.001f) 1f / dist else 0f
        GLES30.glUniform3f(xRight, -dz * inv, 0f, dx * inv)
        GLES30.glUniform1f(xWidth, 0.95f)
        GLES30.glUniform1f(xHeight, (ceiling - 0.12f).coerceAtLeast(1.6f))
        GLES30.glUniform1f(xTime, timeSec)
        GLES30.glUniform1f(xNear, (1f - dist / 48f).coerceIn(0f, 1f))

        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE)
        GLES30.glDepthMask(false)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, billboardVbo)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 0, 0)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDepthMask(true)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
    }

    private fun drawLevel(vp: FloatArray, camX: Float, camY: Float, camZ: Float, fogDensity: Float, flicker: Float, bumpStrength: Float) {
        GLES30.glUseProgram(sceneProgram)
        GLES30.glUniformMatrix4fv(uMVP, 1, false, vp, 0)
        GLES30.glUniform3f(uCamPos, camX, camY, camZ)
        GLES30.glUniform1f(uFogDensity, fogDensity)
        // Warm, not black: distance in the lobby fades into more of the same
        // yellow haze, which is what makes the space read as endless rather than
        // as a lit room standing in a void.
        GLES30.glUniform3f(uFogColor, 0.16f, 0.145f, 0.085f)
        GLES30.glUniform1f(uFlicker, flicker)
        GLES30.glUniform3f(uLampTint, 1.0f, 0.94f, 0.66f)
        // Bump detail scales with quality: off on low, subtle on medium, full
        // on high. The texel step controls how coarse the derived relief is.
        GLES30.glUniform1f(uBumpStrength, bumpStrength)
        GLES30.glUniform1f(uBumpTexel, 1.0f / 512f)
        // Grouped by texture across all resident chunks, so the whole world
        // costs three texture binds rather than three per chunk.
        for (m in chunkMeshes.values) drawMeshGroup(m.floorVbo, m.floorIbo, m.floorCount, floorTex)
        for (m in chunkMeshes.values) drawMeshGroup(m.roofVbo,  m.roofIbo,  m.roofCount,  roofTex)
        for (m in chunkMeshes.values) drawMeshGroup(m.wallVbo,  m.wallIbo,  m.wallCount,  wallTex)
        // Fixtures last: their high baked light makes them read as emitters.
        for (m in chunkMeshes.values) drawMeshGroup(m.fixVbo, m.fixIbo, m.fixCount, lampTex)

        // Light shafts, additive and depth-tested but not depth-written, so
        // several overlapping cones accumulate instead of culling each other.
        GLES30.glUseProgram(shaftProgram)
        GLES30.glUniformMatrix4fv(sMVP, 1, false, vp, 0)
        GLES30.glUniform1f(sFlicker, flicker)
        GLES30.glUniform3f(sTint, 1.0f, 0.94f, 0.72f)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE)
        GLES30.glDepthMask(false)
        for (m in chunkMeshes.values) {
            if (m.shaftCount <= 0) continue
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, m.shaftVbo)
            val stride = 9 * 4
            GLES30.glEnableVertexAttribArray(0); GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
            GLES30.glEnableVertexAttribArray(1); GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride, 3 * 4)
            GLES30.glEnableVertexAttribArray(2); GLES30.glVertexAttribPointer(2, 2, GLES30.GL_FLOAT, false, stride, 6 * 4)
            GLES30.glEnableVertexAttribArray(3); GLES30.glVertexAttribPointer(3, 1, GLES30.GL_FLOAT, false, stride, 8 * 4)
            GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, m.shaftIbo)
            GLES30.glDrawElements(GLES30.GL_TRIANGLES, m.shaftCount, GLES30.GL_UNSIGNED_INT, 0)
        }
        GLES30.glDisableVertexAttribArray(0); GLES30.glDisableVertexAttribArray(1)
        GLES30.glDisableVertexAttribArray(2); GLES30.glDisableVertexAttribArray(3)
        GLES30.glDepthMask(true)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
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

    private fun drawPost(
        timeSec: Float, flicker: Float, vhsStrength: Float,
        cbMix: Float, cbAxis: Triple<Float, Float, Float>,
        flashOn: Boolean, madness: Float, bloomStrength: Float
    ) {
        GLES30.glUseProgram(postProgram)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, fboTex)
        GLES30.glUniform1i(pScene, 0)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, bloomTex[0])
        GLES30.glUniform1i(pBloomTex, 1)
        GLES30.glUniform1f(pBloomStrength, bloomStrength)
        // Slightly over 1 so the tonemap has something to roll off; without the
        // headroom the shoulder never engages and the curve is just a clamp.
        GLES30.glUniform1f(pExposure, 1.18f)
        GLES30.glUniform1f(pTime, timeSec)
        GLES30.glUniform1f(pFlicker, flicker.coerceIn(0.3f, 1f))
        GLES30.glUniform1f(pVhs, vhsStrength)
        GLES30.glUniform2f(pRes, surfaceW.toFloat(), surfaceH.toFloat())
        GLES30.glUniform1f(pCbMix, cbMix)
        GLES30.glUniform3f(pCbAxis, cbAxis.first, cbAxis.second, cbAxis.third)
        GLES30.glUniform1f(pFlashOn, if (flashOn) 1f else 0f)
        GLES30.glUniform1f(pMadness, madness.coerceIn(0f, 1f))
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, postVbo)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 0, 0)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glDisableVertexAttribArray(0)
        // Leave unit 0 selected: every other pass assumes it without asking.
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
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
    /**
     * Builds the mesh for one chunk. The world is unbounded, so geometry can
     * never be one buffer — each chunk gets its own, built when the player
     * comes near and released when they leave.
     *
     * Walls are emitted only on open/solid boundaries, so there is no seam for
     * a gap to show through. UVs are world-anchored rather than per-cell, which
     * is what makes the texture flow continuously across cells and across chunk
     * borders instead of restarting at every edge.
     */

    /**
     * Keeps chunks around the player resident. Builds at most one chunk per
     * frame — a full ring at once would stall visibly, and the fog hides the
     * one-frame delay entirely.
     */

    /** Draws the player's own avatar. Third person only. [py] is the FEET. */
    private fun drawAvatar(
        vp: FloatArray, px: Float, py: Float, pz: Float, yawDeg: Float,
        timeSec: Float, walk: Float
    ) {
        if (charIndexCount <= 0) return
        GLES30.glUseProgram(charProgram)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, charTex)
        GLES30.glUniform1i(cTexU, 0)
        // Without this the avatar takes the backdrop's shading branch: no alpha
        // cutout, so the texture's transparent regions render as solid blocks,
        // and none of the cel banding that gives her form any read at all.
        GLES30.glUniform1f(cIsChar, 1f)

        Matrix.setIdentityM(avatarModelM, 0)
        Matrix.translateM(avatarModelM, 0, px, py, pz)
        // The mesh faces +Z at yaw 0, matching the engine's forward convention.
        Matrix.rotateM(avatarModelM, 0, yawDeg, 0f, 1f, 0f)
        Matrix.scaleM(avatarModelM, 0, AVATAR_SCALE, AVATAR_SCALE, AVATAR_SCALE)
        Matrix.multiplyMM(avatarMvpM, 0, vp, 0, avatarModelM, 0)

        GLES30.glUniformMatrix4fv(cMVP, 1, false, avatarMvpM, 0)
        GLES30.glUniformMatrix4fv(cModel, 1, false, avatarModelM, 0)
        GLES30.glUniform1f(cTime, timeSec)
        GLES30.glUniform1f(cWalk, walk)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, charVbo)
        val stride = CharacterMesh.FLOATS_PER_VERTEX * 4
        GLES30.glEnableVertexAttribArray(0); GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
        GLES30.glEnableVertexAttribArray(1); GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride, 3 * 4)
        GLES30.glEnableVertexAttribArray(2); GLES30.glVertexAttribPointer(2, 2, GLES30.GL_FLOAT, false, stride, 6 * 4)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, charIbo)
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, charIndexCount, GLES30.GL_UNSIGNED_SHORT, 0)
        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
        GLES30.glDisableVertexAttribArray(2)
    }

    private fun streamChunks(world: WorldInfo, camX: Float, camZ: Float) {
        val provider = chunkProvider ?: return
        val chunkSpan = world.chunkCells * world.cellSize
        val pcx = kotlin.math.floor(camX / chunkSpan).toInt()
        val pcz = kotlin.math.floor(camZ / chunkSpan).toInt()

        // Release anything that fell outside the ring.
        val stale = chunkMeshes.keys.filter { key ->
            val cx = (key shr 32).toInt()
            val cz = key.toInt()
            kotlin.math.abs(cx - pcx) > chunkRadius || kotlin.math.abs(cz - pcz) > chunkRadius
        }
        for (key in stale) {
            chunkMeshes.remove(key)?.release()
        }

        // Build the nearest missing chunk, one per frame.
        var bestKey = 0L; var bestDist = Int.MAX_VALUE; var bestX = 0; var bestZ = 0
        for (dz in -chunkRadius..chunkRadius) {
            for (dx in -chunkRadius..chunkRadius) {
                val cx = pcx + dx; val cz = pcz + dz
                val key = (cx.toLong() shl 32) or (cz.toLong() and 0xFFFFFFFFL)
                if (chunkMeshes.containsKey(key)) continue
                val d = dx * dx + dz * dz
                if (d < bestDist) { bestDist = d; bestKey = key; bestX = cx; bestZ = cz }
            }
        }
        if (bestDist == Int.MAX_VALUE) return

        val chunk = provider(bestX, bestZ) ?: run {
            // Remember the miss so we don't retry it every frame.
            chunkMeshes[bestKey] = ChunkMesh()
            return
        }
        chunkMeshes[bestKey] = buildChunkMesh(chunk, world) ?: ChunkMesh()
    }

    private fun buildChunkMesh(chunk: WorldChunk, world: WorldInfo): ChunkMesh? {
        val floorV = ArrayList<Float>(); val floorI = ArrayList<Int>(); var floorB = 0
        val wallV  = ArrayList<Float>(); val wallI  = ArrayList<Int>(); var wallB  = 0
        val roofV  = ArrayList<Float>(); val roofI  = ArrayList<Int>(); var roofB  = 0
        val fixV   = ArrayList<Float>(); val fixI   = ArrayList<Int>(); var fixB   = 0
        val shaftV = ArrayList<Float>(); val shaftI = ArrayList<Int>(); var shaftB = 0

        /** Emits a quad whose four vertices each carry their own baked light. */
        fun quad(
            verts: ArrayList<Float>, idx: ArrayList<Int>, base: Int,
            p0: FloatArray, p1: FloatArray, p2: FloatArray, p3: FloatArray,
            n: FloatArray, l0: Float, l1: Float, l2: Float, l3: Float,
            u0: Float, v0: Float, u1: Float, v1: Float
        ): Int {
            val pts = arrayOf(p0, p1, p2, p3)
            val uvs = floatArrayOf(u0, v0, u1, v0, u1, v1, u0, v1)
            val lights = floatArrayOf(l0, l1, l2, l3)
            for (k in 0 until 4) {
                verts.add(pts[k][0]); verts.add(pts[k][1]); verts.add(pts[k][2])
                verts.add(n[0]); verts.add(n[1]); verts.add(n[2])
                verts.add(uvs[k * 2]); verts.add(uvs[k * 2 + 1])
                verts.add(lights[k])
            }
            idx.add(base); idx.add(base + 1); idx.add(base + 2)
            idx.add(base); idx.add(base + 2); idx.add(base + 3)
            return base + 4
        }

        /** Same, for surfaces that are genuinely uniform (fixtures, shafts). */
        fun quadFlat(
            verts: ArrayList<Float>, idx: ArrayList<Int>, base: Int,
            p0: FloatArray, p1: FloatArray, p2: FloatArray, p3: FloatArray,
            n: FloatArray, light: Float, u0: Float, v0: Float, u1: Float, v1: Float
        ): Int = quad(verts, idx, base, p0, p1, p2, p3, n, light, light, light, light, u0, v0, u1, v1)

        /**
         * Light at a cell CORNER, averaged over the four cells that meet there.
         *
         * The value per cell now comes from the engine's baked gather — the sum
         * of what every fluorescent within reach actually throws — so it is
         * already continuous. Averaging at corners and letting the GPU
         * interpolate across the face is what carries that continuity onto the
         * geometry: no face anywhere holds a single flat tone, so there is no
         * cell edge for a step to land on.
         *
         * The chunk's one-cell apron is what lets a corner on the chunk edge see
         * its neighbour, so the gradient runs across chunk borders too.
         */
        fun cornerLight(cx: Int, cz: Int): Float {
            var sum = 0f; var count = 0
            var solidSum = 0f
            for (dz in -1..0) for (dx in -1..0) {
                val ax = cx + dx; val az = cz + dz
                val l = chunk.lightAt(ax, az)
                if (chunk.solidAt(ax, az)) { solidSum += l; continue }
                sum += l; count++
            }
            // All four solid: an inside corner, where nothing is lit. Use what
            // the solid cells carry so the seam still resolves smoothly.
            return if (count == 0) solidSum * 0.25f else sum / count
        }

        val cs = world.cellSize
        val hgt = world.height
        val uvPerMetre = 0.5f
        val originX = chunk.chunkX * chunk.cells * cs
        val originZ = chunk.chunkZ * chunk.cells * cs

        for (lz in 0 until chunk.cells) {
            for (lx in 0 until chunk.cells) {
                if (chunk.solidAt(lx, lz)) continue

                val x0 = originX + lx * cs; val x1 = x0 + cs
                val z0 = originZ + lz * cs; val z1 = z0 + cs
                val lit = chunk.lightAt(lx, lz)
                val feature = chunk.featureAt(lx, lz)

                // The cell's four corners, shared with every neighbouring cell,
                // so adjacent faces agree exactly and no seam can show.
                val c00 = cornerLight(lx, lz)          // -x -z
                val c10 = cornerLight(lx + 1, lz)      // +x -z
                val c11 = cornerLight(lx + 1, lz + 1)  // +x +z
                val c01 = cornerLight(lx, lz + 1)      // -x +z

                val u0 = x0 * uvPerMetre; val u1 = x1 * uvPerMetre
                val v0 = z0 * uvPerMetre; val v1 = z1 * uvPerMetre
                val wallV0 = 0f;          val wallV1 = hgt * uvPerMetre

                if (feature != 4) {
                    floorB = quad(
                        floorV, floorI, floorB,
                        floatArrayOf(x0, 0f, z0), floatArrayOf(x1, 0f, z0),
                        floatArrayOf(x1, 0f, z1), floatArrayOf(x0, 0f, z1),
                        floatArrayOf(0f, 1f, 0f), c00, c10, c11, c01, u0, v0, u1, v1
                    )
                }
                if (feature != 1) {
                    roofB = quad(
                        roofV, roofI, roofB,
                        floatArrayOf(x0, hgt, z0), floatArrayOf(x0, hgt, z1),
                        floatArrayOf(x1, hgt, z1), floatArrayOf(x1, hgt, z0),
                        floatArrayOf(0f, -1f, 0f),
                        c00 * 1.12f, c01 * 1.12f, c11 * 1.12f, c10 * 1.12f, u0, v0, u1, v1
                    )
                }

                // Walls fall off toward the skirting because the emitters are all
                // overhead — a vertical gradient, not one flat tone per panel.
                val wallTop = 1.0f
                val wallBot = 0.62f
                /**
                 * One wall face, emitted as a skirting strip plus the panel above
                 * it. Real interiors have a hard horizontal line where the wall
                 * meets the floor; without it the two surfaces merge into one
                 * another and the room loses every cue about its own scale.
                 */
                fun wallFace(
                    ax: Float, az: Float, bx: Float, bz: Float,
                    n: FloatArray, lA: Float, lB: Float, uA: Float, uB: Float
                ) {
                    val skirtH = 0.13f
                    val skirtV = skirtH * uvPerMetre
                    wallB = quad(wallV, wallI, wallB,
                        floatArrayOf(ax, 0f, az), floatArrayOf(bx, 0f, bz),
                        floatArrayOf(bx, skirtH, bz), floatArrayOf(ax, skirtH, az),
                        n,
                        lA * 0.34f, lB * 0.34f, lB * 0.52f, lA * 0.52f,
                        uA, 0f, uB, skirtV)
                    wallB = quad(wallV, wallI, wallB,
                        floatArrayOf(ax, skirtH, az), floatArrayOf(bx, skirtH, bz),
                        floatArrayOf(bx, hgt, bz), floatArrayOf(ax, hgt, az),
                        n,
                        lA * wallBot, lB * wallBot, lB * wallTop, lA * wallTop,
                        uA, skirtV, uB, hgt * uvPerMetre)
                }

                if (chunk.solidAt(lx - 1, lz)) {
                    wallFace(x0, z1, x0, z0, floatArrayOf(1f, 0f, 0f), c01, c00, v1, v0)
                }
                if (chunk.solidAt(lx + 1, lz)) {
                    wallFace(x1, z0, x1, z1, floatArrayOf(-1f, 0f, 0f), c10, c11, v0, v1)
                }
                if (chunk.solidAt(lx, lz - 1)) {
                    wallFace(x0, z0, x1, z0, floatArrayOf(0f, 0f, 1f), c00, c10, u0, u1)
                }
                if (chunk.solidAt(lx, lz + 1)) {
                    wallFace(x1, z1, x0, z1, floatArrayOf(0f, 0f, -1f), c11, c01, u1, u0)
                }

                // Door frame. A threshold with no ceiling tile over it is just a
                // hole; a lintel and two jambs turn the same gap into a doorway,
                // and doorways are the strongest reading of "this is a building"
                // the level has.
                if (feature == 1) {
                    val jamb = cs * 0.10f
                    val head = hgt * 0.82f
                    val alongX = chunk.solidAt(lx, lz - 1) || chunk.solidAt(lx, lz + 1)
                    val frameLit = (c00 + c10 + c01 + c11) * 0.25f * 0.55f
                    if (alongX) {
                        // Opening runs along X: jambs face each other across it.
                        wallB = quad(wallV, wallI, wallB,
                            floatArrayOf(x0 + jamb, head, z0), floatArrayOf(x1 - jamb, head, z0),
                            floatArrayOf(x1 - jamb, head, z1), floatArrayOf(x0 + jamb, head, z1),
                            floatArrayOf(0f, -1f, 0f),
                            frameLit, frameLit, frameLit, frameLit, u0, v0, u1, v1)
                    } else {
                        wallB = quad(wallV, wallI, wallB,
                            floatArrayOf(x0, head, z0 + jamb), floatArrayOf(x0, head, z1 - jamb),
                            floatArrayOf(x1, head, z1 - jamb), floatArrayOf(x1, head, z0 + jamb),
                            floatArrayOf(0f, -1f, 0f),
                            frameLit, frameLit, frameLit, frameLit, u0, v0, u1, v1)
                    }
                }

                // Recessed fluorescent troffer: a bright diffuser panel set
                // under the ceiling plane. The most recognisable object here.
                val fixture = chunk.fixtureAt(lx, lz)
                if (fixture == 1) {
                    // Light in the air below the fitting. Real geometry rather
                    // than a screen-space trick, so it occludes correctly and
                    // holds up from any angle.
                    //
                    // A rectangular slab, not the cone this used to be: the
                    // emitter is a metre-long row of tubes, and a cone spreading
                    // evenly in every direction from a point is the wrong shape
                    // for it — it read as a spotlight in an office ceiling.
                    val midX = x0 + cs * 0.5f
                    val midZ = z0 + cs * 0.5f
                    val top = hgt - 0.09f
                    val halfLen = cs * 0.34f          // along the tubes
                    val topHalf = cs * 0.16f          // across them, at the fitting
                    val botHalf = cs * 0.54f          // across them, at the floor
                    val botLen  = halfLen + cs * 0.20f
                    val intensity = lit.coerceAtMost(1.5f)
                    val floorY = 0.02f

                    // Two long faces, one either side, splaying outward as they
                    // fall. UV carries (rim, fall) rather than texture coords —
                    // the shaft shader reads them as shaping parameters.
                    for (side in -1..1 step 2) {
                        val s = side.toFloat()
                        shaftB = quadFlat(
                            shaftV, shaftI, shaftB,
                            floatArrayOf(midX - halfLen, top, midZ + s * topHalf),
                            floatArrayOf(midX + halfLen, top, midZ + s * topHalf),
                            floatArrayOf(midX + botLen, floorY, midZ + s * botHalf),
                            floatArrayOf(midX - botLen, floorY, midZ + s * botHalf),
                            floatArrayOf(0f, 1f, 0f), intensity,
                            0.28f, 0f, 0.28f, 1f
                        )
                    }
                    // Two short end caps, so the slab is closed rather than a
                    // pair of floating sheets seen edge-on from the side.
                    for (side in -1..1 step 2) {
                        val s = side.toFloat()
                        shaftB = quadFlat(
                            shaftV, shaftI, shaftB,
                            floatArrayOf(midX + s * halfLen, top, midZ - topHalf),
                            floatArrayOf(midX + s * halfLen, top, midZ + topHalf),
                            floatArrayOf(midX + s * botLen, floorY, midZ + botHalf),
                            floatArrayOf(midX + s * botLen, floorY, midZ - botHalf),
                            floatArrayOf(0f, 1f, 0f), intensity * 0.85f,
                            0.55f, 0f, 0.55f, 1f
                        )
                    }
                    // Outer skirt at low density, so the haze fades out into the
                    // room instead of ending on a hard edge.
                    for (side in -1..1 step 2) {
                        val s = side.toFloat()
                        shaftB = quadFlat(
                            shaftV, shaftI, shaftB,
                            floatArrayOf(midX - halfLen * 1.3f, top, midZ + s * topHalf * 1.7f),
                            floatArrayOf(midX + halfLen * 1.3f, top, midZ + s * topHalf * 1.7f),
                            floatArrayOf(midX + botLen * 1.25f, floorY, midZ + s * botHalf * 1.30f),
                            floatArrayOf(midX - botLen * 1.25f, floorY, midZ + s * botHalf * 1.30f),
                            floatArrayOf(0f, 1f, 0f), intensity * 0.45f,
                            0.86f, 0f, 0.86f, 1f
                        )
                    }
                }
                if (fixture != 0) {
                    // A recessed fluorescent troffer, built the way the real
                    // thing is: a dark housing recessed into the ceiling grid
                    // with a row of separate tubes sitting in it. One flat panel
                    // read as a glowing sticker; the tube separation is what
                    // makes it register as a light fitting at a glance, and it is
                    // the most recognisable object in the whole level.
                    val midX = x0 + cs * 0.5f
                    val midZ = z0 + cs * 0.5f
                    val halfW = cs * 0.36f          // along the tubes
                    val housingHalfD = cs * 0.20f   // across them
                    val lit = fixture == 1
                    val down = floatArrayOf(0f, -1f, 0f)

                    // Housing, flush with the ceiling plane.
                    fixB = quadFlat(
                        fixV, fixI, fixB,
                        floatArrayOf(midX - halfW, hgt - 0.015f, midZ - housingHalfD),
                        floatArrayOf(midX - halfW, hgt - 0.015f, midZ + housingHalfD),
                        floatArrayOf(midX + halfW, hgt - 0.015f, midZ + housingHalfD),
                        floatArrayOf(midX + halfW, hgt - 0.015f, midZ - housingHalfD),
                        down, 0.10f, 0f, 0f, 1f, 1f
                    )

                    // Three tubes, hung just below the housing so they read as
                    // objects inside it rather than as paint on it.
                    val tubes = 3
                    val tubeHalfD = cs * 0.028f
                    val tubeY = hgt - 0.075f
                    val emit = if (lit) 3.1f else 0.16f
                    for (t in 0 until tubes) {
                        // Evenly spaced across the housing, inset from its rim.
                        val f = (t + 0.5f) / tubes                     // 0..1 across
                        val cz = midZ + (f - 0.5f) * (housingHalfD * 1.62f)
                        fixB = quadFlat(
                            fixV, fixI, fixB,
                            floatArrayOf(midX - halfW * 0.92f, tubeY, cz - tubeHalfD),
                            floatArrayOf(midX - halfW * 0.92f, tubeY, cz + tubeHalfD),
                            floatArrayOf(midX + halfW * 0.92f, tubeY, cz + tubeHalfD),
                            floatArrayOf(midX + halfW * 0.92f, tubeY, cz - tubeHalfD),
                            down, emit, 0f, 0f, 1f, 1f
                        )
                    }
                }
            }
        }

        if (floorI.isEmpty() && wallI.isEmpty() && roofI.isEmpty()) return null

        val mesh = ChunkMesh()
        mesh.floorVbo = genGlBuffer(); mesh.floorIbo = genGlBuffer()
        mesh.floorCount = uploadMeshBuffers(mesh.floorVbo, mesh.floorIbo, floorV, floorI)
        mesh.roofVbo = genGlBuffer(); mesh.roofIbo = genGlBuffer()
        mesh.roofCount = uploadMeshBuffers(mesh.roofVbo, mesh.roofIbo, roofV, roofI)
        mesh.wallVbo = genGlBuffer(); mesh.wallIbo = genGlBuffer()
        mesh.wallCount = uploadMeshBuffers(mesh.wallVbo, mesh.wallIbo, wallV, wallI)
        mesh.fixVbo = genGlBuffer(); mesh.fixIbo = genGlBuffer()
        mesh.fixCount = uploadMeshBuffers(mesh.fixVbo, mesh.fixIbo, fixV, fixI)
        mesh.shaftVbo = genGlBuffer(); mesh.shaftIbo = genGlBuffer()
        mesh.shaftCount = uploadMeshBuffers(mesh.shaftVbo, mesh.shaftIbo, shaftV, shaftI)
        return mesh
    }

    /** GL buffers for one streamed chunk. */
    private class ChunkMesh {
        var floorVbo = 0; var floorIbo = 0; var floorCount = 0
        var wallVbo  = 0; var wallIbo  = 0; var wallCount  = 0
        var roofVbo  = 0; var roofIbo  = 0; var roofCount  = 0
        var fixVbo   = 0; var fixIbo   = 0; var fixCount   = 0
        var shaftVbo = 0; var shaftIbo = 0; var shaftCount = 0

        fun release() {
            val bufs = intArrayOf(floorVbo, floorIbo, wallVbo, wallIbo, roofVbo, roofIbo,
                                  fixVbo, fixIbo, shaftVbo, shaftIbo)
            GLES30.glDeleteBuffers(bufs.size, bufs, 0)
        }
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
        // Half-float would give the bloom real headroom to work with, but it is
        // not universally filterable on GLES3 mobile parts, so the tonemap runs
        // on 8-bit and the bright pass compensates with a low threshold.
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

        rebuildBloomTargets(w, h)
    }

    /** Half-res ping-pong pair. Bloom is a wide, low-frequency effect, so full
     *  resolution buys nothing and costs four times the fill. */
    private fun rebuildBloomTargets(w: Int, h: Int) {
        for (i in 0 until 2) {
            if (bloomFbo[i] != 0) GLES30.glDeleteFramebuffers(1, intArrayOf(bloomFbo[i]), 0)
            if (bloomTex[i] != 0) GLES30.glDeleteTextures(1, intArrayOf(bloomTex[i]), 0)
        }
        bloomW = max(w / 2, 1); bloomH = max(h / 2, 1)
        for (i in 0 until 2) {
            bloomTex[i] = genGlTexture()
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, bloomTex[i])
            GLES30.glTexImage2D(
                GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, bloomW, bloomH, 0,
                GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null
            )
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

            bloomFbo[i] = genGlFramebuffer()
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, bloomFbo[i])
            GLES30.glFramebufferTexture2D(
                GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0,
                GLES30.GL_TEXTURE_2D, bloomTex[i], 0
            )
        }
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
    }

    /**
     * Extracts the bright parts of the scene and blurs them wide.
     *
     * [passes] blur iterations; each one roughly doubles the reach, so two gives
     * a tight halo around the tubes and three the soft room-filling wash. Ends
     * with the result in bloomTex[0].
     */
    private fun renderBloom(passes: Int) {
        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        GLES30.glDisable(GLES30.GL_BLEND)
        GLES30.glViewport(0, 0, bloomW, bloomH)

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, bloomFbo[0])
        GLES30.glUseProgram(brightProgram)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, fboTex)
        GLES30.glUniform1i(brScene, 0)
        // Set just under the level's lit-corridor value, so ordinary walls stay
        // out of it and only the fittings and blown-out hotspots glow.
        GLES30.glUniform1f(brThreshold, 0.62f)
        GLES30.glUniform1f(brKnee, 0.28f)
        drawFullscreenQuad()

        GLES30.glUseProgram(blurProgram)
        var src = 0
        for (i in 0 until passes) {
            // Horizontal into [1], vertical back into [0].
            val spread = 1f + i.toFloat()
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, bloomFbo[1 - src])
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, bloomTex[src])
            GLES30.glUniform1i(blSource, 0)
            GLES30.glUniform2f(blDir, spread / bloomW, 0f)
            drawFullscreenQuad()
            src = 1 - src

            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, bloomFbo[1 - src])
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, bloomTex[src])
            GLES30.glUniform1i(blSource, 0)
            GLES30.glUniform2f(blDir, 0f, spread / bloomH)
            drawFullscreenQuad()
            src = 1 - src
        }
        // An odd number of swaps would leave the result in [1]; copy so callers
        // can always read [0].
        if (src != 0) {
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, bloomFbo[0])
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, bloomTex[1])
            GLES30.glUniform1i(blSource, 0)
            GLES30.glUniform2f(blDir, 0f, 0f)
            drawFullscreenQuad()
        }
        GLES30.glEnable(GLES30.GL_BLEND)
    }

    private fun drawFullscreenQuad() {
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, postVbo)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 0, 0)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glDisableVertexAttribArray(0)
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
        return uploadTexture(bmp)
    }

    /** Uploads a bitmap as a repeating, mipmapped 2D texture and recycles it. */
    private fun uploadTexture(bmp: Bitmap): Int {
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

    /** A single flat colour, for surfaces whose look comes entirely from the
     *  baked light rather than from any grain of their own. */
    private fun solidTile(color: Int): Bitmap {
        val pixels = IntArray(4) { color }
        return Bitmap.createBitmap(pixels, 2, 2, Bitmap.Config.ARGB_8888)
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

    val hudLayout by settingsVm.uiLayout.collectAsState()
    val renderer = remember { OmniGLRenderer(ctx.applicationContext) }
    // The renderer pulls chunks on its own thread as the player moves; the VM
    // owns the native bridge, so it supplies the fetch.
    LaunchedEffect(renderer) { renderer.chunkProvider = vm::fetchChunk }
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
        renderer.cameraView = settingsState.cameraView
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
            state.isMadnessOver -> MadnessOverlay(state) { onExit() }
            state.isGameOver -> GameOverOverlay(state)  { onExit() }
            state.isEscaped  -> EscapedOverlay(state)   { onExit() }
            state.isPaused   -> PauseOverlay(onResume = { vm.togglePause() }, onExit = { onExit() })
            state.spawnPhase != SpawnPhase.READY -> SpawnSequenceOverlay(state.spawnPhase)
            else -> GameHud(
                gameState  = state,
                canEscape  = vm.canEscape,
                layout     = hudLayout,
                onPause    = { vm.togglePause() },
                onFlash    = { vm.toggleFlashlight() },
                onMove     = { dx, dy, dz -> vm.onMove(dx, dy, dz) },
                onLook     = { dx, dy -> vm.onLook(dx, dy) },
                onJump     = { vm.onJump() },
                onCrouch   = { vm.toggleCrouch() },
                onSprint   = { held -> vm.setSprint(held) },
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
                    // Looks shows the same card grid as every other tab; the
                    // character lives in the item list, and tapping its art
                    // opens the full 3D inspection.
                    MarketTab.Looks -> {
                        LazyVerticalGrid(
                            GridCells.Fixed(2),
                            Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(s.items) { index, item ->
                                MarketCard(
                                    item, s.purchasing == item.id, item.id in s.ownedIds, index,
                                    onInspect = { inspecting = true }
                                ) { vm.confirmBuy(item) }
                            }
                        }
                    }
                    MarketTab.Daily -> {
                        Column(Modifier.fillMaxSize()) {
                            DailyResetCountdown(vm)
                            LazyVerticalGrid(
                                GridCells.Fixed(2),
                                Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                itemsIndexed(s.dailyDeals) { index, item ->
                                    MarketCard(
                                        item, s.purchasing == item.id, item.id in s.ownedIds, index,
                                        onInspect = { inspecting = true }
                                    ) { vm.confirmBuy(item) }
                                }
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
                                itemsIndexed(s.items) { index, item ->
                                    MarketCard(
                                        item, s.purchasing == item.id, item.id in s.ownedIds, index,
                                        onInspect = { inspecting = true }
                                    ) { vm.confirmBuy(item) }
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
    val clock = rememberFrameClock()
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
            drawFrame3D(frame, r * 1.16f, clock)
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

/**
 * Frame styles are pure decoration — no gameplay effect by design.
 *
 * Animated and shaded rather than flat: [t] drives orbiting elements, and the
 * ring is built from short arc segments whose brightness varies with their
 * angle to a fixed light. That angular shading is what reads as a rounded metal
 * band catching light, instead of a printed circle.
 */

// ============================================================================
// 3D profile frames.
//
// A real solid of revolution — a torus built from actual geometry, rotated and
// perspective-projected on the CPU every frame, back-face culled, depth-sorted
// and shaded per facet against a fixed key light with a moving specular.
//
// Drawn through Compose's Canvas rather than a GLSurfaceView on purpose. A
// frame has to sit inside a scrolling grid, behind an avatar photo, and next to
// ordinary composables; a second GL surface would need z-order-on-top and would
// then punch through everything drawn around it. At this triangle count the
// transform is cheap enough that doing it in Kotlin costs less than the surface
// would, and it composites correctly everywhere.
// ============================================================================

/** Segments around the main ring and around the tube. */
private const val FRAME3D_MAJOR = 40
private const val FRAME3D_MINOR = 8

/** One torus vertex in model space, with its surface normal. */
private class TorusVertex(val x: Float, val y: Float, val z: Float,
                          val nx: Float, val ny: Float, val nz: Float,
                          /** Angle around the main ring, 0..1. Drives travelling
                           *  patterns so they follow the ring's own geometry. */
                          val u: Float)

/**
 * Unit torus: major radius 1, tube radius [minorRatio]. Dimensionless so the
 * draw can scale the whole thing by one number, and built once per style
 * because the geometry never changes — only the transform applied to it does.
 */
private fun buildTorus(minorRatio: Float): Array<TorusVertex> {
    val out = ArrayList<TorusVertex>(FRAME3D_MAJOR * FRAME3D_MINOR)
    for (i in 0 until FRAME3D_MAJOR) {
        val u = i / FRAME3D_MAJOR.toFloat()
        val a = u * 2f * Math.PI.toFloat()
        val ca = cos(a); val sa = sin(a)
        for (j in 0 until FRAME3D_MINOR) {
            val b = (j / FRAME3D_MINOR.toFloat()) * 2f * Math.PI.toFloat()
            val cb = cos(b); val sb = sin(b)
            val ringR = 1f + minorRatio * cb
            out.add(
                TorusVertex(
                    ringR * ca, ringR * sa, minorRatio * sb,
                    cb * ca, cb * sa, sb,
                    u
                )
            )
        }
    }
    return out.toTypedArray()
}

/** Geometry cache. Four styles, so this never grows. */
private val frameGeometryCache = HashMap<String, Array<TorusVertex>>()

private fun frameGeometry(key: String): Array<TorusVertex> = synchronized(frameGeometryCache) {
    frameGeometryCache.getOrPut(key) { buildTorus(frameStyleFor(key).minorScale) }
}

/** Palette and behaviour for one frame style. */
private class FrameStyle(
    val base: Color,
    val highlight: Color,
    /** Emissive band colour, used by the travelling pattern. */
    val glow: Color,
    val minorScale: Float,
    val shininess: Float,
    /** Extra brightness at a given ring position and time, 0..1. */
    val pattern: (u: Float, t: Float) -> Float
)

private fun frameStyleFor(key: String): FrameStyle = when (key) {
    // Fluorescent tube bent into a ring, running on a failing ballast — the
    // same stutter the level's own fixtures have.
    "halogen" -> FrameStyle(
        base = Color(0xFF3A3222), highlight = CrtAmber, glow = Color(0xFFFFE9A8),
        minorScale = 0.26f, shininess = 22f
    ) { u, t ->
        val ballast = if (sin(t * 11f) * sin(t * 3.7f) > -0.72f) 1f else 0.30f
        // Four tubes with dark end caps, so it reads as a fitting, not a hoop.
        val seg = ((u * 4f) % 1f)
        val body = if (seg > 0.06f && seg < 0.94f) 1f else 0.05f
        body * ballast
    }
    // A radar return: a bright sweep head dragging a decaying tail.
    "signal" -> FrameStyle(
        base = Color(0xFF15303A), highlight = OmniumCol, glow = Color(0xFFB6F4FF),
        minorScale = 0.22f, shininess = 42f
    ) { u, t ->
        val head = (t * 0.36f) % 1f
        var d = u - head
        if (d < 0f) d += 1f
        val tail = (1f - d).let { it * it * it * it }
        val static = if (sin(t * 9f + u * 41f) > 0.55f) 0.22f else 0f
        (tail + static).coerceIn(0f, 1f)
    }
    // Two counter-rotating arcs of a door that never quite shuts.
    "threshold" -> FrameStyle(
        base = Color(0xFF241B3A), highlight = SouliumCol, glow = Color(0xFFD9C9FF),
        minorScale = 0.30f, shininess = 30f
    ) { u, t ->
        fun arc(centre: Float): Float {
            var d = kotlin.math.abs(u - ((centre % 1f) + 1f) % 1f)
            if (d > 0.5f) d = 1f - d
            return (1f - smoothStep01(d / 0.16f)).coerceIn(0f, 1f)
        }
        maxOf(
            arc(t * 0.14f), arc(t * 0.14f + 0.5f),
            arc(-t * 0.11f + 0.25f) * 0.7f, arc(-t * 0.11f + 0.75f) * 0.7f
        )
    }
    // Plain machined steel. Nothing emissive; all of its life comes from the
    // specular travelling round the tube as it turns.
    else -> FrameStyle(
        base = Color(0xFF4A4A4E), highlight = Color(0xFFCFD2D8), glow = Color(0xFFF2F4F8),
        minorScale = 0.19f, shininess = 60f
    ) { _, _ -> 0f }
}

private fun smoothStep01(x: Float): Float {
    val t = x.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/**
 * Draws the frame as real 3D geometry.
 *
 * [t] is the shared frame clock, so several frames on screen turn together.
 * [radius] is the ring's major radius in pixels; the tube scales off it.
 */
private fun DrawScope.drawFrame3D(
    frame: String,
    radius: Float,
    t: Float
) {
    val style = frameStyleFor(frame)
    val geometry = frameGeometry(frame)

    // Orientation. A fixed tilt away from the viewer is what exposes the tube's
    // roundness at all — face-on, a torus is indistinguishable from a flat
    // annulus. The slow wobble on top keeps it from looking like a static
    // render, and the spin carries the pattern round the ring.
    val tilt = 0.62f + sin(t * 0.37f) * 0.10f
    val yaw  = sin(t * 0.23f) * 0.22f
    val spin = t * 0.30f

    val cosT = cos(tilt); val sinT = sin(tilt)
    val cosY = cos(yaw);  val sinY = sin(yaw)
    val cosS = cos(spin); val sinS = sin(spin)

    // Perspective. The eye sits a few ring-radii back; nearer than that and the
    // distortion reads as a fisheye rather than as depth.
    val eyeZ = radius * 5.2f

    val n = geometry.size
    val projX = FloatArray(n); val projY = FloatArray(n); val viewZ = FloatArray(n)
    val litR = FloatArray(n); val litG = FloatArray(n); val litB = FloatArray(n)

    // Key light over the viewer's left shoulder, in view space. Fixed, so the
    // highlight sweeps across the surface as the ring turns under it.
    val lx = -0.42f; val ly = -0.66f; val lz = 0.62f
    val ll = kotlin.math.sqrt(lx * lx + ly * ly + lz * lz)
    val lxn = lx / ll; val lyn = ly / ll; val lzn = lz / ll
    // Half-vector against a view direction of (0,0,1).
    val hx = lxn; val hy = lyn; val hz = lzn + 1f
    val hl = kotlin.math.sqrt(hx * hx + hy * hy + hz * hz)
    val hxn = hx / hl; val hyn = hy / hl; val hzn = hz / hl

    for (i in 0 until n) {
        val v = geometry[i]
        // model -> spin about Z -> tilt about X -> yaw about Y.
        // The geometry is a unit torus, so one scale covers the whole solid.
        val x = (v.x * cosS - v.y * sinS) * radius
        val y = (v.x * sinS + v.y * cosS) * radius
        val z = v.z * radius

        val y1 = y * cosT - z * sinT
        val z1 = y * sinT + z * cosT
        val x2 = x * cosY + z1 * sinY
        val z2 = -x * sinY + z1 * cosY

        // Normals take the same rotations, without the scale.
        var nx = v.nx * cosS - v.ny * sinS
        var ny = v.nx * sinS + v.ny * cosS
        var nz = v.nz
        val ny1 = ny * cosT - nz * sinT
        val nz1 = ny * sinT + nz * cosT
        val nx2 = nx * cosY + nz1 * sinY
        val nz2 = -nx * sinY + nz1 * cosY
        ny = ny1; nx = nx2; nz = nz2

        val persp = eyeZ / (eyeZ - z2).coerceAtLeast(radius * 0.4f)
        projX[i] = center.x + x2 * persp
        projY[i] = center.y + y1 * persp
        viewZ[i] = z2

        val diffuse = (nx * lxn + ny * lyn + nz * lzn).coerceAtLeast(0f)
        val specDot = (nx * hxn + ny * hyn + nz * hzn).coerceAtLeast(0f)
        val spec = Math.pow(specDot.toDouble(), style.shininess.toDouble()).toFloat()
        // Rim: facets turning away from the eye pick up a cool edge, which is
        // what separates the silhouette from whatever is behind it.
        val rim = (1f - kotlin.math.abs(nz)).let { it * it } * 0.55f
        val emissive = style.pattern(v.u, t)

        val ambient = 0.22f
        val kd = ambient + diffuse * 0.78f
        litR[i] = style.base.red * kd + style.highlight.red * spec + style.glow.red * emissive + rim * 0.30f
        litG[i] = style.base.green * kd + style.highlight.green * spec + style.glow.green * emissive + rim * 0.32f
        litB[i] = style.base.blue * kd + style.highlight.blue * spec + style.glow.blue * emissive + rim * 0.40f
    }

    // Facets, depth-sorted back to front. Back-face culling first: a facet whose
    // averaged normal points away contributes nothing but overdraw.
    class Facet(val a: Int, val b: Int, val c: Int, val d: Int, val depth: Float)
    val facets = ArrayList<Facet>(FRAME3D_MAJOR * FRAME3D_MINOR / 2)
    for (i in 0 until FRAME3D_MAJOR) {
        val i2 = (i + 1) % FRAME3D_MAJOR
        for (j in 0 until FRAME3D_MINOR) {
            val j2 = (j + 1) % FRAME3D_MINOR
            val a = i * FRAME3D_MINOR + j
            val b = i2 * FRAME3D_MINOR + j
            val c = i2 * FRAME3D_MINOR + j2
            val d = i * FRAME3D_MINOR + j2
            // Screen-space winding tells us which way the facet faces after the
            // projection, which is more reliable than testing the model normal.
            val cross = (projX[b] - projX[a]) * (projY[d] - projY[a]) -
                        (projY[b] - projY[a]) * (projX[d] - projX[a])
            if (cross <= 0f) continue
            facets.add(Facet(a, b, c, d, (viewZ[a] + viewZ[b] + viewZ[c] + viewZ[d]) * 0.25f))
        }
    }
    facets.sortBy { it.depth }

    val path = Path()
    for (f in facets) {
        path.reset()
        path.moveTo(projX[f.a], projY[f.a])
        path.lineTo(projX[f.b], projY[f.b])
        path.lineTo(projX[f.c], projY[f.c])
        path.lineTo(projX[f.d], projY[f.d])
        path.close()
        val r = (litR[f.a] + litR[f.b] + litR[f.c] + litR[f.d]) * 0.25f
        val g = (litG[f.a] + litG[f.b] + litG[f.c] + litG[f.d]) * 0.25f
        val bl = (litB[f.a] + litB[f.b] + litB[f.c] + litB[f.d]) * 0.25f
        drawPath(
            path,
            Color(r.coerceIn(0f, 1f), g.coerceIn(0f, 1f), bl.coerceIn(0f, 1f), 1f)
        )
    }

    // Bloom around the emissive styles, so a glowing ring throws light into the
    // space around it instead of stopping dead at its own outline.
    val glowAmount = style.pattern(0f, t)
    if (frame != "default") {
        drawCircle(
            Brush.radialGradient(
                listOf(Color.Transparent, style.glow.copy(0.16f + glowAmount * 0.10f), Color.Transparent),
                center = center, radius = radius * 1.55f
            ),
            radius = radius * 1.55f, center = center
        )
    }
}

/** Continuously advancing seconds, for Canvas art that animates. Shared so
 *  several frames on screen stay in phase with each other. */
@Composable
private fun rememberFrameClock(): Float {
    val inf = rememberInfiniteTransition(label = "frameClock")
    val t by inf.animateFloat(
        0f, (Math.PI * 2).toFloat() * 4f,
        infiniteRepeatable(tween(25_000, easing = LinearEasing), RepeatMode.Restart),
        "frameClockV"
    )
    return t
}

// The old flat drawFrameRing() lived here. It is gone: frames are real 3D
// geometry now (see drawFrame3D above), shaded and depth-sorted, so a painted
// circle of arcs pretending to be one had nothing left to offer.


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

internal fun DrawScope.drawStopwatchGlyph(c: Color) {
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
    val all = listOf("default", "halogen", "signal", "threshold")
    val pickerClock = rememberFrameClock()
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
                    // Every authored frame is available; the lock path stays for
                    // future event-gated frames.
                    val unlocked = true
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
                            drawFrame3D(f, size.minDimension * 0.30f, pickerClock)
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
    "halogen"   -> "Halogen"
    "signal"    -> "Signal"
    "threshold" -> "Threshold"
    else        -> "Default"
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
    layout    : Map<String, UiButtonLayout> = emptyMap(),
    onPause   : () -> Unit,
    onFlash   : () -> Unit,
    onMove    : (Float, Float, Float) -> Unit,
    onLook    : (Float, Float) -> Unit,
    onJump    : () -> Unit,
    onCrouch  : () -> Unit,
    onSprint  : (Boolean) -> Unit,
    onInteract: () -> Unit
) {
    val sanityTint by animateColorAsState(
        if (gameState.sanity < 30f) DangerRed.copy(0.15f * (1f - gameState.sanity / 30f)) else Color.Transparent,
        tween(500), label = "sanity_tint"
    )
    var hudSize by remember { mutableStateOf(IntSize.Zero) }

    // Places an element at its saved normalised position, falling back to the
    // built-in default when the player hasn't moved it.
    fun placed(id: String, defX: Float, defY: Float, w: Float, h: Float): Modifier {
        val l = layout[id]
        val nx = l?.offset?.x ?: defX
        val ny = l?.offset?.y ?: defY
        val sc = l?.sizeScale ?: 1f
        return Modifier
            .offset {
                IntOffset(
                    (nx * hudSize.width - w * density * sc / 2f).toInt(),
                    (ny * hudSize.height - h * density * sc / 2f).toInt()
                )
            }
    }
    fun scaleOf(id: String): Float = layout[id]?.sizeScale ?: 1f

    Box(Modifier.fillMaxSize().onSizeChanged { hudSize = it }) {
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
        // Each bar is placed independently so the editor can separate them.
        Box(placed("bar_sanity", HUD_BAR_SANITY.x, HUD_BAR_SANITY.y, 150f, 30f).width((150 * scaleOf("bar_sanity")).dp)) {
            StatusBar(stringResource(R.string.game_hud_sanity), gameState.sanity / 100f, SouliumCol)
        }
        Box(placed("bar_stamina", HUD_BAR_STAM.x, HUD_BAR_STAM.y, 150f, 30f).width((150 * scaleOf("bar_stamina")).dp)) {
            StatusBar(stringResource(R.string.game_hud_stamina), gameState.stamina / gameState.staminaMax, SuccessGreen)
        }
        Box(placed("bar_battery", HUD_BAR_BATT.x, HUD_BAR_BATT.y, 150f, 30f).width((150 * scaleOf("bar_battery")).dp)) {
            StatusBar(stringResource(R.string.game_hud_battery), gameState.flashlightBattery, CrtAmber)
        }

        // --- Top-right: session readouts and pause -------------------------
        Row(
            placed("readouts", HUD_READOUTS.x, HUD_READOUTS.y, 120f, 30f),
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
        }
        Box(placed("pause", HUD_PAUSE.x, HUD_PAUSE.y, 40f, 40f)) {
            IconGlyphButton((34 * scaleOf("pause")).dp, Yellow.copy(0.8f), onClick = onPause) {
                HudGlyph("pause", it, Modifier.fillMaxSize())
            }
        }

        // --- Centre reticle -------------------------------------------------
        // The same small dot the lobby uses. In a level with no landmarks it is
        // the only fixed reference on screen, which is what makes turning read as
        // turning instead of as the whole world sliding.
        Box(Modifier.align(Alignment.Center)) {
            androidx.compose.foundation.Canvas(Modifier.size(10.dp)) {
                val r = size.minDimension * 0.16f
                drawCircle(Color.Black.copy(0.55f), radius = r * 2.1f, center = center)
                drawCircle(Yellow.copy(0.82f), radius = r, center = center)
            }
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
        Box(placed("joystick", HUD_JOYSTICK.x, HUD_JOYSTICK.y, 140f, 140f)) {
            VirtualJoystick(
                Modifier.size((140 * scaleOf("joystick")).dp),
                onMove = { dx, dy -> onMove(dx, 0f, -dy) }
            )
        }

        // --- Bottom-right: actions -------------------------------------------
        // Laid out on the arc the right thumb actually sweeps, pivoting near the
        // bottom-right corner, with the most-used action nearest the rest
        // position and the rarest furthest out. The old grid put run and crouch
        // up in the middle of the screen where nothing can reach them.
        // Each action is placed independently so the editor can rearrange them
        // freely rather than only moving a fixed cluster.
        Box(placed("interact", HUD_INTERACT.x, HUD_INTERACT.y, 62f, 62f)) {
            HudActionButton(
                (62 * scaleOf("interact")).dp,
                if (canEscape) SuccessGreen else TextSec,
                "interact", onInteract,
                emphasised = canEscape
            )
        }
        // Run is its own control, held rather than tapped, and sits second on the
        // arc because after moving it is the thing reached for most. Deflecting
        // the stick further makes you walk faster, not sprint — sprinting costs
        // stamina and has to be something the player chooses.
        Box(placed("sprint", HUD_SPRINT.x, HUD_SPRINT.y, 56f, 56f)) {
            val canSprint = gameState.stamina > 5f && !gameState.isCrouching
            HudActionButton(
                (56 * scaleOf("sprint")).dp,
                when {
                    gameState.isSprinting -> SuccessGreen
                    canSprint             -> Yellow
                    else                  -> TextDim
                },
                "sprint",
                onClick = {},
                onHoldChange = { held -> onSprint(held && canSprint) },
                active = gameState.isSprinting
            )
        }
        Box(placed("flashlight", HUD_FLASHLIGHT.x, HUD_FLASHLIGHT.y, 52f, 52f)) {
            HudActionButton(
                (52 * scaleOf("flashlight")).dp,
                if (gameState.flashlightOn) CrtAmber else TextDim,
                "flashlight", onFlash,
                active = gameState.flashlightOn
            )
        }
        Box(placed("jump", HUD_JUMP.x, HUD_JUMP.y, 48f, 48f)) {
            HudActionButton((48 * scaleOf("jump")).dp, Yellow, "jump", onJump)
        }
        Box(placed("crouch", HUD_CROUCH.x, HUD_CROUCH.y, 48f, 48f)) {
            HudActionButton(
                (48 * scaleOf("crouch")).dp,
                if (gameState.isCrouching) CrtAmber else TextSec,
                "crouch", onCrouch,
                active = gameState.isCrouching
            )
        }
    }
}

/**
 * Built-in HUD placement, normalised to the screen.
 *
 * Kept in one place because three separate copies of these numbers existed —
 * the HUD itself, the editor's initial state and the editor's reset button — and
 * they had already drifted apart, so "reset" moved controls somewhere the game
 * had never put them.
 */
internal data class HudSlot(val x: Float, val y: Float, val scale: Float = 1f)

internal val HUD_JOYSTICK   = HudSlot(0.135f, 0.735f)
internal val HUD_INTERACT   = HudSlot(0.925f, 0.795f)
internal val HUD_SPRINT     = HudSlot(0.800f, 0.855f)
internal val HUD_CROUCH     = HudSlot(0.672f, 0.880f)
internal val HUD_FLASHLIGHT = HudSlot(0.800f, 0.640f)
internal val HUD_JUMP       = HudSlot(0.925f, 0.598f)
internal val HUD_PAUSE      = HudSlot(0.950f, 0.070f)
internal val HUD_READOUTS   = HudSlot(0.780f, 0.070f)
internal val HUD_BAR_SANITY = HudSlot(0.110f, 0.100f)
internal val HUD_BAR_STAM   = HudSlot(0.110f, 0.200f)
internal val HUD_BAR_BATT   = HudSlot(0.110f, 0.300f)

/** id -> built-in slot, so the HUD and the editor cannot disagree. */
internal val HUD_DEFAULT_SLOTS: Map<String, HudSlot> = mapOf(
    "joystick"   to HUD_JOYSTICK,
    "interact"   to HUD_INTERACT,
    "sprint"     to HUD_SPRINT,
    "crouch"     to HUD_CROUCH,
    "flashlight" to HUD_FLASHLIGHT,
    "jump"       to HUD_JUMP,
    "pause"      to HUD_PAUSE,
    "readouts"   to HUD_READOUTS,
    "bar_sanity" to HUD_BAR_SANITY,
    "bar_stamina" to HUD_BAR_STAM,
    "bar_battery" to HUD_BAR_BATT
)

/** Base size in dp of each HUD element, shared by the HUD and the editor. */
internal val HUD_DEFAULT_SIZES: Map<String, Pair<Float, Float>> = mapOf(
    "joystick"    to (140f to 140f),
    "interact"    to (62f to 62f),
    "sprint"      to (56f to 56f),
    "crouch"      to (48f to 48f),
    "flashlight"  to (52f to 52f),
    "jump"        to (48f to 48f),
    "pause"       to (40f to 40f),
    "readouts"    to (120f to 30f),
    "bar_sanity"  to (150f to 30f),
    "bar_stamina" to (150f to 30f),
    "bar_battery" to (150f to 30f)
)

/**
 * In-game action button, built as stacked layers rather than a flat circle.
 *
 * The depth comes from five things drawn in order: an outer bloom, a domed
 * body whose gradient runs light-to-dark top-to-bottom, a bright rim arc on the
 * upper edge and a dark one below (the two together read as a bevel catching a
 * light from above), the glyph, and finally a specular sheen. Pressing swaps
 * the bevel arcs and sinks the face, so the button visibly depresses instead of
 * merely shrinking.
 */
@Composable
internal fun HudActionButton(
    size: Dp,
    accent: Color,
    id: String,
    onClick: () -> Unit,
    emphasised: Boolean = false,
    /** Set for controls that act while held (sprint) rather than on release. */
    onHoldChange: ((Boolean) -> Unit)? = null,
    active: Boolean = false,
    /** The layout editor arranges buttons; it must not fire their actions. */
    interactive: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // A hold control has to know when the finger lands and when it lifts, which
    // a click callback alone cannot tell it.
    if (onHoldChange != null) {
        LaunchedEffect(pressed) { onHoldChange(pressed) }
    }
    val press by animateFloatAsState(
        if (pressed) 1f else 0f,
        spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
        label = "hudPress"
    )
    val inf = rememberInfiniteTransition(label = "hudPulse")
    val pulse by inf.animateFloat(
        0.45f, 0.9f,
        infiniteRepeatable(tween(1200, easing = EaseInOut), RepeatMode.Reverse),
        "hudPulseV"
    )
    val ring = when {
        emphasised -> pulse
        active     -> 0.95f
        else       -> 0.55f
    }

    Box(
        Modifier
            .size(size)
            .graphicsLayer {
                val sc = 1f - press * 0.07f
                scaleX = sc; scaleY = sc
                translationY = press * 3f
                // Real elevation, so the button casts onto the scene behind it.
                shadowElevation = (8f - press * 6f) * density
                spotShadowColor = accent.copy(0.5f)
                ambientShadowColor = Color.Black
                shape = CircleShape
                clip = false
            }
            .then(
                if (interactive) Modifier.clickable(interaction, indication = null, onClick = onClick)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
            val r = this.size.minDimension / 2f
            val c = center

            // 1. Bloom, strongest when the action is being called out.
            drawCircle(
                Brush.radialGradient(
                    listOf(accent.copy(0.22f * ring), Color.Transparent),
                    center = c, radius = r * 1.55f
                ),
                radius = r * 1.55f, center = c
            )

            // 2. Domed body. The gradient is what makes it read as a sphere
            // section rather than a disc.
            drawCircle(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF33312A).copy(0.95f - press * 0.15f),
                        Color(0xFF15140F).copy(0.97f),
                        accent.copy(0.20f)
                    ),
                    startY = c.y - r, endY = c.y + r
                ),
                radius = r * 0.94f, center = c
            )

            // 3. Bevel. Bright above, dark below — inverted while pressed, which
            // is exactly how a real recessed button behaves under a fixed light.
            val topAlpha = 0.55f - press * 0.45f
            val botAlpha = 0.10f + press * 0.40f
            drawArc(
                Color.White.copy(topAlpha),
                startAngle = 190f, sweepAngle = 160f, useCenter = false,
                topLeft = Offset(c.x - r * 0.94f, c.y - r * 0.94f),
                size = Size(r * 1.88f, r * 1.88f),
                style = Stroke(r * 0.10f, cap = StrokeCap.Round)
            )
            drawArc(
                Color.Black.copy(botAlpha + 0.25f),
                startAngle = 10f, sweepAngle = 160f, useCenter = false,
                topLeft = Offset(c.x - r * 0.94f, c.y - r * 0.94f),
                size = Size(r * 1.88f, r * 1.88f),
                style = Stroke(r * 0.10f, cap = StrokeCap.Round)
            )

            // 4. Accent rim.
            drawCircle(accent.copy(ring), radius = r * 0.94f, center = c, style = Stroke(r * 0.055f))
        }

        HudGlyph(
            id, accent,
            Modifier.fillMaxSize().padding(size * 0.26f)
                .graphicsLayer { translationY = press * 2f }
        )

        // 5. Specular sheen, offset upward so the light reads as overhead.
        androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
            val r = this.size.minDimension / 2f
            drawOval(
                Brush.radialGradient(
                    listOf(Color.White.copy(0.16f - press * 0.12f), Color.Transparent)
                ),
                topLeft = Offset(center.x - r * 0.52f, center.y - r * 0.78f),
                size = Size(r * 1.04f, r * 0.62f)
            )
        }
    }
}


/**
 * The single source of truth for a HUD control's artwork.
 *
 * The in-game HUD and the settings layout editor both resolve through here, so a
 * button cannot end up looking like one thing where you arrange it and another
 * where you press it. These are the shared vector assets — one drawable per
 * action, tinted at the use site — rather than two independent Canvas
 * re-implementations that drifted apart.
 */
@DrawableRes
internal fun hudIconRes(id: String): Int = when (id) {
    "pause"      -> R.drawable.ic_hud_pause
    "interact"   -> R.drawable.ic_hud_interact
    "flashlight" -> R.drawable.ic_hud_flashlight
    "jump"       -> R.drawable.ic_hud_jump
    "crouch"     -> R.drawable.ic_hud_crouch
    "sprint"     -> R.drawable.ic_hud_sprint
    else         -> R.drawable.ic_frame
}

/** Renders a HUD control's icon at the given tint. */
@Composable
internal fun HudGlyph(id: String, tint: Color, modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(hudIconRes(id)),
        contentDescription = null,
        tint = tint,
        modifier = modifier
    )
}

// The editor's miniature joystick/status-bar/readout glyphs used to live here.
// They are gone: the layout editor now instantiates the real VirtualJoystick,
// StatusBar and readout chips, so there is nothing left for a stand-in to do.

// The pause/flashlight/interact/jump/crouch glyphs that used to live here are
// gone: those controls now render the shared ic_hud_* vector assets through
// hudIconRes(), so the HUD and the layout editor cannot disagree about what a
// button looks like. Keeping a second hand-drawn copy around is what let them
// drift apart in the first place.

@Composable
private fun HudBadge(text: String, color: Color) {
    Box(
        Modifier.clip(RoundedCornerShape(2.dp)).background(MetalBg.copy(0.8f)).padding(horizontal = 6.dp, vertical = 3.dp)
    ) { Text(text, color = color, fontSize = 10.sp) }
}

/** Fraction of the stick's travel that registers as "not moving". */
private const val JOYSTICK_DEADZONE = 0.12f

@Composable
fun VirtualJoystick(modifier: Modifier, onMove: (Float, Float) -> Unit) {
    var knob by remember { mutableStateOf(Offset.Zero) }
    var dragging by remember { mutableStateOf(false) }
    // While the finger is down the knob is pinned to it exactly; the spring is
    // only for the snap back to centre on release. Animating it under the finger
    // made the stick lag behind the thumb, which is what made the control feel
    // like it was ignoring how far you had actually pushed it.
    val released by animateOffsetAsState(
        knob, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "joystick"
    )
    val knobAnim = if (dragging) knob else released
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
                    if (mag < JOYSTICK_DEADZONE) { onMove(0f, 0f); return }
                    // Remap (deadzone..1] onto (0..1] so leaving the deadzone is a
                    // crawl that builds smoothly, instead of jumping straight to
                    // deadzone-speed the instant the stick registers at all.
                    val scaled = ((mag - JOYSTICK_DEADZONE) / (1f - JOYSTICK_DEADZONE)).coerceIn(0f, 1f)
                    onMove(nx / mag * scaled, ny / mag * scaled)
                }
                detectDragGestures(
                    onDragStart  = { pos ->
                        dragging = true
                        emit(pos - Offset(size.width / 2f, size.height / 2f))
                    },
                    onDragEnd    = { dragging = false; knob = Offset.Zero; onMove(0f, 0f) },
                    onDragCancel = { dragging = false; knob = Offset.Zero; onMove(0f, 0f) },
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
    var showHudEditor by remember { mutableStateOf(false) }
    val s by settingsVm.state.collectAsState()

    // The editor takes the whole screen; it needs the room to arrange things.
    if (showHudEditor) {
        UiEditor(onSave = { showHudEditor = false })
        return
    }

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
                    // Camera view, switchable mid-run.
                    Text(stringResource(R.string.settings_camera_view), color = TextSec, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "first" to R.string.camera_first_person,
                            "third" to R.string.camera_third_person
                        ).forEach { (key, labelRes) ->
                            val sel = s.cameraView == key
                            Box(
                                Modifier.weight(1f).height(36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (sel) Yellow.copy(0.15f) else MetalBg)
                                    .border(1.dp, if (sel) Yellow else BorderCol, RoundedCornerShape(6.dp))
                                    .clickable { settingsVm.onCameraView(key) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    stringResource(labelRes),
                                    color = if (sel) Yellow else TextDim, fontSize = 11.sp,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                    DividerLine()
                    // Layout editing without leaving the run.
                    AtmosphericButton(
                        label   = stringResource(R.string.settings_hud_editor),
                        icon    = Icons.Default.DragIndicator,
                        accent  = CrtAmber,
                        width   = 240.dp, height = 44.dp,
                        onClick = { showHudEditor = true }
                    )
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
            Text(stringResource(R.string.game_survival_label), color = TextDim, fontSize = 9.sp, letterSpacing = 2.sp)
            Text(
                formatElapsed(gameState.sessionElapsed),
                color = Yellow, fontSize = 26.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp
            )
            DividerLine()
            OmniumAwardRow(gameState.omniumEarned)
            StatRow(stringResource(R.string.game_stat_score),      gameState.score.toString(),       Yellow)
            StatRow(stringResource(R.string.game_stat_kills),      gameState.kills.toString(),       DangerRed)
            StatRow(stringResource(R.string.game_stat_difficulty), gameState.difficulty.uppercase(), CrtAmber)
            DividerLine()
            AtmosphericButton(stringResource(R.string.game_return_lobby), Icons.Default.ExitToApp, DangerRed, 220.dp, 50.dp, onExit)
        }
    }
}

/**
 * Reaching the exit. Level 1 is not built yet, and the honest thing is to say so
 * on the screen the player earned rather than to drop them back in the lobby with
 * no acknowledgement that they got out.
 */
@Composable
fun EscapedOverlay(gameState: GameState, onExit: () -> Unit) {
    val inf  = rememberInfiniteTransition(label = "esc")
    val glow by inf.animateFloat(0.5f, 1f, infiniteRepeatable(tween(1200, easing = EaseInOut), RepeatMode.Reverse), "g")
    val scan by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart), "escScan")
    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.88f)), Alignment.Center) {
        Column(
            Modifier.width(300.dp).clip(RoundedCornerShape(4.dp))
                .background(MetalBg)
                .border(1.dp, SuccessGreen.copy(0.5f), RoundedCornerShape(4.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.game_escaped_title),
                color = SuccessGreen.copy(glow), fontSize = 24.sp,
                fontWeight = FontWeight.Black, letterSpacing = 3.sp
            )
            // Survival time is the headline: it is what the mode is scored on and
            // what the Omnium award is computed from.
            Text(
                stringResource(R.string.game_survival_label),
                color = TextDim, fontSize = 9.sp, letterSpacing = 2.sp
            )
            Text(
                formatElapsed(gameState.sessionElapsed),
                color = Yellow, fontSize = 30.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp
            )
            DividerLine()

            // Level 1 teaser, with a moving scanline so it does not read as a
            // static "coming soon" plate nobody looks at twice.
            Box(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(3.dp))
                    .background(SuccessGreen.copy(0.09f))
                    .border(1.dp, SuccessGreen.copy(0.35f), RoundedCornerShape(3.dp))
            ) {
                androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
                    drawRect(
                        SuccessGreen.copy(0.16f),
                        topLeft = Offset(0f, size.height * scan - 4f),
                        size = Size(size.width, 8f)
                    )
                }
                Text(
                    stringResource(R.string.game_next_levels_soon),
                    color = SuccessGreen, fontSize = 11.sp, lineHeight = 15.sp,
                    textAlign = TextAlign.Center, fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }

            DividerLine()
            OmniumAwardRow(gameState.omniumEarned)
            StatRow(stringResource(R.string.game_stat_score),      gameState.score.toString(),       Yellow)
            StatRow(stringResource(R.string.game_stat_kills),      gameState.kills.toString(),       DangerRed)
            StatRow(stringResource(R.string.game_stat_difficulty), gameState.difficulty.uppercase(), CrtAmber)
            DividerLine()
            AtmosphericButton(
                stringResource(R.string.game_return_lobby), Icons.Default.ExitToApp,
                SuccessGreen, 236.dp, 50.dp, onExit, isPrimary = true
            )
        }
    }
}

/** What the run paid out, called out rather than buried in the stat rows. */
@Composable
private fun OmniumAwardRow(amount: Long) {
    val inf = rememberInfiniteTransition(label = "award")
    val shine by inf.animateFloat(
        0.55f, 1f,
        infiniteRepeatable(tween(1400, easing = EaseInOut), RepeatMode.Reverse), "awardV"
    )
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.game_omnium_earned), color = TextSec, fontSize = 11.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            androidx.compose.foundation.Canvas(Modifier.size(13.dp)) { drawOmniumGlyph(OmniumCol.copy(shine)) }
            Text(
                "+${formatCurrency(amount)}",
                color = OmniumCol.copy(shine), fontSize = 14.sp, fontWeight = FontWeight.Black
            )
        }
    }
}

/**
 * Sanity took the player. Deliberately not the ordinary game-over card: the body
 * is on the floor, the camera has rolled onto its side behind this, and the copy
 * names what happened rather than reporting a score.
 */
@Composable
fun MadnessOverlay(gameState: GameState, onExit: () -> Unit) {
    val inf = rememberInfiniteTransition(label = "mad")
    val breathe by inf.animateFloat(
        0.35f, 0.9f,
        infiniteRepeatable(tween(1700, easing = EaseInOut), RepeatMode.Reverse), "madV"
    )
    val drift by inf.animateFloat(
        -3f, 3f,
        infiniteRepeatable(tween(2300, easing = EaseInOut), RepeatMode.Reverse), "madDrift"
    )
    // Held back a beat so the collapse itself is seen before the text lands.
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(900); revealed = true }

    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.62f)), Alignment.Center) {
        androidx.compose.animation.AnimatedVisibility(
            visible = revealed,
            modifier = Modifier,
            enter = fadeIn(tween(1200)),
            exit = fadeOut()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(horizontal = 28.dp)
            ) {
                Text(
                    stringResource(R.string.game_madness_title),
                    color = SouliumCol.copy(breathe),
                    fontSize = 26.sp, fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp, textAlign = TextAlign.Center,
                    lineHeight = 32.sp,
                    modifier = Modifier.graphicsLayer { translationX = drift; rotationZ = drift * 0.35f }
                )
                Text(
                    stringResource(R.string.game_madness_sub),
                    color = TextDim, fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight = 16.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Text(stringResource(R.string.game_survival_label), color = TextDim, fontSize = 10.sp)
                    Text(
                        formatElapsed(gameState.sessionElapsed),
                        color = Yellow, fontSize = 14.sp, fontWeight = FontWeight.Bold
                    )
                }
                OmniumAwardRow(gameState.omniumEarned)
                AtmosphericButton(
                    stringResource(R.string.game_return_lobby), Icons.Default.ExitToApp,
                    SouliumCol, 236.dp, 50.dp, onExit
                )
            }
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
    /** Position in the grid, used to stagger the entrance. */
    index: Int = 0,
    onInspect: () -> Unit = {},
    onBuy: () -> Unit
) {
    val currencyColor = when (item.currency.lowercase()) { "omnium" -> OmniumCol; "soulium" -> SouliumCol; "tl" -> SuccessGreen; else -> CrtAmber }
    val inf  = rememberInfiniteTransition(label = "card")
    val glow by inf.animateFloat(0.3f, 0.7f, infiniteRepeatable(tween(2000, easing = EaseInOut), RepeatMode.Reverse), "g")
    val interSrc  = remember { MutableInteractionSource() }
    val isPressed by interSrc.collectIsPressedAsState()
    val scale     by animateFloatAsState(if (isPressed) 0.97f else 1f, spring(), label = "card_scale")

    // Staggered entrance. Cards arriving together as one block reads as a
    // screenshot appearing; arriving in sequence reads as a list being dealt
    // out, and it gives the eye an order to follow. Keyed on the item so
    // switching tabs re-runs it rather than snapping in.
    var entered by remember(item.id) { mutableStateOf(false) }
    LaunchedEffect(item.id) {
        delay(index * 55L)
        entered = true
    }
    val enter by animateFloatAsState(
        if (entered) 1f else 0f,
        spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow),
        label = "card_enter"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .graphicsLayer {
                alpha = enter
                translationY = (1f - enter) * 26f * density
                val s = 0.94f + enter * 0.06f
                scaleX = s; scaleY = s
            }
            .scale(scale)
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
        val cardClock = rememberFrameClock()
        val artPulse by inf.animateFloat(
            0.95f, 1.05f,
            infiniteRepeatable(tween(2100, easing = EaseInOut), RepeatMode.Reverse),
            "cardArt"
        )
        val inspectable = item.category == "characters"
        // A frame is a moving object, so its card gives it room to move in and
        // skips the pulse — the ring already has its own rhythm and stacking a
        // second one on top just reads as jitter.
        val isFrame = item.category == "frames" || item.id.startsWith("frame_")
        Box(
            Modifier
                .size(if (isFrame) 84.dp else 62.dp)
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
            // Light pass: a specular lobe orbiting behind the art, so the frame
            // sits in a lit scene rather than on a flat swatch. Cheap enough to
            // run on every card in a scrolling grid.
            if (isFrame) {
                androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
                    val orbit = size.minDimension * 0.30f
                    val lx = center.x + kotlin.math.cos(cardClock * 1.3f) * orbit
                    val ly = center.y + kotlin.math.sin(cardClock * 1.3f) * orbit
                    drawCircle(
                        Brush.radialGradient(
                            listOf(currencyColor.copy(0.38f), Color.Transparent),
                            center = Offset(lx, ly), radius = size.minDimension * 0.52f
                        ),
                        radius = size.minDimension * 0.52f, center = Offset(lx, ly)
                    )
                }
            }
            androidx.compose.foundation.Canvas(
                Modifier.fillMaxSize().padding(if (isFrame) 16.dp else 9.dp)
                    .graphicsLayer {
                        val s = if (isFrame) 1f else artPulse
                        scaleX = s; scaleY = s
                    }
            ) { marketItemArt(item.id, item.category, currencyColor, cardClock) }
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
internal fun IconGlyphButton(
    size: Dp,
    accent: Color,
    onClick: () -> Unit,
    /** Composable so a caller can supply either a Canvas glyph or a shared
     *  drawable — the HUD needs the latter to match the layout editor. */
    glyph: @Composable (Color) -> Unit
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
        Box(Modifier.fillMaxSize().padding(9.dp)) { glyph(accent) }
    }
}

/** Left-rail entry: code-drawn glyph with its label underneath. */
@Composable
private fun RailItem(
    label: String,
    @DrawableRes iconRes: Int,
    accent: Color,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        if (pressed) 1f else 0f,
        spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
        label = "railPress"
    )
    val inf = rememberInfiniteTransition(label = "railGlow")
    // One shared clock drives the sheen, the corner marks and the idle breath,
    // so every part of the tile moves as one object rather than as three
    // decorations that happen to be animating near each other.
    val clock by inf.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Restart),
        "railClock"
    )
    val breath by inf.animateFloat(
        0.34f, 0.70f,
        infiniteRepeatable(tween(2800, easing = EaseInOut), RepeatMode.Reverse),
        "railBreath"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(interaction, indication = null, onClick = onClick)
    ) {
        Box(
            Modifier
                .size(46.dp)
                .graphicsLayer {
                    val s = 1f - press * 0.09f
                    scaleX = s; scaleY = s
                    translationY = press * 3f
                    // Real elevation, so the tile sits above the lobby video
                    // instead of looking pasted onto it.
                    shadowElevation = (10f - press * 8f) * density
                    spotShadowColor = accent.copy(0.55f)
                    ambientShadowColor = Color.Black
                    shape = RoundedCornerShape(14.dp)
                    clip = false
                },
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
                val corner = androidx.compose.ui.geometry.CornerRadius(size.minDimension * 0.30f)
                val rect = Size(size.width, size.height)

                // 1. Outer bloom.
                drawRoundRect(
                    Brush.radialGradient(
                        listOf(accent.copy(0.20f * breath), Color.Transparent),
                        center = center, radius = size.minDimension * 0.95f
                    ),
                    size = rect, cornerRadius = corner
                )

                // 2. Body. Light at the top edge falling to near-black at the
                // bottom is what makes a flat rectangle read as a raised face.
                drawRoundRect(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF32302A).copy(0.96f - press * 0.14f),
                            Color(0xFF15140F).copy(0.97f),
                            accent.copy(0.18f)
                        ),
                        startY = 0f, endY = size.height
                    ),
                    size = rect, cornerRadius = corner
                )

                // 3. Bevel: a bright top edge and a dark bottom one, swapping
                // while pressed — which is exactly how a real key behaves under
                // a fixed overhead light.
                val topA = 0.46f - press * 0.38f
                val botA = 0.12f + press * 0.36f
                drawLine(
                    Color.White.copy(topA),
                    Offset(size.width * 0.22f, 1.5f), Offset(size.width * 0.78f, 1.5f),
                    strokeWidth = size.minDimension * 0.045f, cap = StrokeCap.Round
                )
                drawLine(
                    Color.Black.copy(botA + 0.25f),
                    Offset(size.width * 0.22f, size.height - 1.5f),
                    Offset(size.width * 0.78f, size.height - 1.5f),
                    strokeWidth = size.minDimension * 0.045f, cap = StrokeCap.Round
                )

                // 4. Sheen sweeping across the face on the shared clock. Clipped
                // to the tile so it reads as light crossing the surface.
                clipPath(Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, size.height, corner)) }) {
                    val sweep = (clock * 2.4f - 0.7f) * size.width
                    rotate(-22f, Offset(sweep, size.height / 2f)) {
                        drawRect(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, Color.White.copy(0.13f), Color.Transparent),
                                startX = sweep - size.width * 0.30f,
                                endX = sweep + size.width * 0.30f
                            ),
                            topLeft = Offset(sweep - size.width * 0.30f, -size.height),
                            size = Size(size.width * 0.60f, size.height * 3f)
                        )
                    }
                }

                // 5. Frame, plus corner registration marks that grow on press.
                drawRoundRect(
                    accent.copy(0.30f + breath * 0.35f + press * 0.30f),
                    size = rect, cornerRadius = corner,
                    style = Stroke(size.minDimension * 0.035f)
                )
                val tick = size.minDimension * (0.20f + press * 0.08f)
                val inset = size.minDimension * 0.14f
                listOf(
                    Offset(inset, inset) to Pair(1f, 1f),
                    Offset(size.width - inset, inset) to Pair(-1f, 1f),
                    Offset(inset, size.height - inset) to Pair(1f, -1f),
                    Offset(size.width - inset, size.height - inset) to Pair(-1f, -1f)
                ).forEach { (o, dir) ->
                    val c = accent.copy(0.55f + press * 0.4f)
                    val sw = size.minDimension * 0.035f
                    drawLine(c, o, Offset(o.x + tick * dir.first, o.y), strokeWidth = sw, cap = StrokeCap.Round)
                    drawLine(c, o, Offset(o.x, o.y + tick * dir.second), strokeWidth = sw, cap = StrokeCap.Round)
                }
            }

            Icon(
                painter = painterResource(iconRes),
                contentDescription = label,
                tint = accent,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(11.dp)
                    .graphicsLayer { translationY = press * 2f }
            )

            // 6. Specular cap, offset upward so the light reads as overhead.
            androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
                drawOval(
                    Brush.radialGradient(listOf(Color.White.copy(0.15f - press * 0.11f), Color.Transparent)),
                    topLeft = Offset(size.width * 0.20f, -size.height * 0.10f),
                    size = Size(size.width * 0.60f, size.height * 0.46f)
                )
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            label,
            color = accent.copy(0.70f + press * 0.30f),
            fontSize = 8.sp, letterSpacing = 0.5.sp, maxLines = 1
        )
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
    /** Omnium banked from finished runs. */
    val omnium: StateFlow<Long> = cosmetics.observeOmnium()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

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
    val clock = rememberFrameClock()
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
            drawFrame3D(frame, r * 1.16f, clock)
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
private fun DrawScope.marketItemArt(id: String, category: String, accent: Color, artClock: Float = 0f) {
    when {
        id.startsWith("frame_") -> {
            // Show the actual frame the player would equip.
            val key = id.removePrefix("frame_")
            drawCircle(accent.copy(0.18f), radius = size.minDimension * 0.24f, center = center)
            drawFrame3D(key, size.minDimension * 0.30f, artClock)
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


private const val OMNI_SHAFT_VERT = """#version 300 es
layout(location=0) in vec3 aPos;
layout(location=1) in vec3 aNormal;
layout(location=2) in vec2 aUV;
layout(location=3) in float aLight;
uniform mat4 uMVP;
out float vFall;     // 0 at the fixture, 1 at the floor
out float vEdge;     // 0 at the cone axis, 1 at its rim
out float vIntensity;
void main(){
    vFall = aUV.y;
    vEdge = aUV.x;
    vIntensity = aLight;
    gl_Position = uMVP * vec4(aPos, 1.0);
}
"""

private const val OMNI_SHAFT_FRAG = """#version 300 es
precision mediump float;
in float vFall; in float vEdge; in float vIntensity;
uniform float uFlicker;
uniform vec3 uTint;
out vec4 fragColor;
void main(){
    // Density falls off down the shaft and toward the rim. Squaring the rim
    // term is what gives the cone a soft edge instead of a hard silhouette,
    // which is the difference between "light in dusty air" and "a glass cone".
    float down = 1.0 - vFall;
    float rim  = 1.0 - vEdge;
    float density = down * down * 0.55 + down * 0.45;
    density *= rim * rim;
    float a = density * vIntensity * uFlicker * 0.30;
    if (a < 0.004) discard;
    fragColor = vec4(uTint * a, a);
}
"""

private const val OMNI_PREVIEW_VERT = """#version 300 es
layout(location=0) in vec3 aPos;
layout(location=1) in vec3 aNormal;
layout(location=2) in vec2 aUV;
uniform mat4 uMVP;
uniform mat4 uModel;
uniform float uTime;
uniform float uWalk;
out vec3 vNormal; out vec2 vUV; out vec3 vWorldPos;

// Rotate a point about an arbitrary pivot on the Y axis.
vec3 rotY(vec3 p, vec3 pivot, float a){
    vec3 d = p - pivot;
    float c = cos(a), s = sin(a);
    return pivot + vec3(d.x * c + d.z * s, d.y, -d.x * s + d.z * c);
}
// Rotate about the X axis (forward/back swing).
vec3 rotX(vec3 p, vec3 pivot, float a){
    vec3 d = p - pivot;
    float c = cos(a), s = sin(a);
    return pivot + vec3(d.x, d.y * c - d.z * s, d.y * s + d.z * c);
}
// Rotate about the Z axis (arms dropping to the sides).
vec3 rotZ(vec3 p, vec3 pivot, float a){
    vec3 d = p - pivot;
    float c = cos(a), s = sin(a);
    return pivot + vec3(d.x * c - d.y * s, d.x * s + d.y * c, d.z);
}

void main(){
    vec3 p = aPos;
    float h = clamp(p.y, 0.0, 1.0);
    // uWalk is a continuous 0..1.6 gait blend (0 idle, 1 walk, >1 run), not a
    // yes/no flag. Everything below scales off it, which is what stops the
    // character snapping between two fixed poses.
    float gait = clamp(uWalk, 0.0, 1.6);
    float run  = clamp(gait - 1.0, 0.0, 0.6) / 0.6;
    float stride = uTime * 6.4;

    // --- 1. Break the T-pose ------------------------------------------------
    // The source mesh is modelled arms-out. Rotate each arm down about its own
    // shoulder so the character rests naturally; without this she stands with
    // both arms straight out, which is what the in-game screenshot showed.
    float armSide = sign(p.x);
    float armReach = smoothstep(0.10, 0.30, abs(p.x));          // 0 at torso, 1 at hand
    float armBand = smoothstep(0.58, 0.66, p.y) * (1.0 - smoothstep(0.80, 0.88, p.y));
    float armMask = armReach * armBand;
    if (armMask > 0.001) {
        vec3 shoulder = vec3(armSide * 0.11, 0.74, 0.0);
        // ~78 degrees down, so the arms hang close to the body.
        p = rotZ(p, shoulder, armSide * 1.36 * armMask);
        // Slight inward tuck so the hands sit beside the hips, not splayed.
        p = rotY(p, shoulder, -armSide * 0.20 * armMask);
    }

    // --- 2. Arm swing -------------------------------------------------------
    // Opposite phase per side, and opposite to the legs, as in a real gait.
    // The forearm lags the upper arm by a fraction of a cycle: that lag is the
    // single biggest thing separating a swinging limb from a rotating stick,
    // and its absence is most of what read as robotic.
    if (armMask > 0.001) {
        vec3 shoulder = vec3(armSide * 0.11, 0.74, 0.0);
        float phase = stride + (armSide > 0.0 ? 3.14159 : 0.0);
        float swing = sin(phase);
        // Idle arms are never quite still either — a slow, tiny sway.
        float idleSway = sin(uTime * 0.9 + armSide) * 0.055;
        float amount = (swing * (0.40 + 0.30 * run) * gait + idleSway) * armMask;
        p = rotX(p, shoulder, amount);

        // Elbow: same swing, delayed, applied only below the joint so the upper
        // arm keeps its own arc.
        float forearm = smoothstep(0.20, 0.34, abs(p.x));
        if (forearm > 0.001) {
            vec3 elbow = vec3(armSide * 0.21, 0.60, 0.0);
            float lag = sin(phase - 0.85);
            p = rotX(p, elbow, (lag * 0.30 * gait + 0.10) * forearm);
        }
    }

    // --- 3. Legs ------------------------------------------------------------
    float legMask = 1.0 - smoothstep(0.05, 0.48, p.y);
    if (legMask > 0.001) {
        vec3 hip = vec3(sign(p.x) * 0.05, 0.48, 0.0);
        float legPhase = stride + (p.x > 0.0 ? 0.0 : 3.14159);
        p = rotX(p, hip, sin(legPhase) * (0.52 + 0.34 * run) * gait * legMask);
        // Knee bend on the recovery half of the stride only, which is what makes
        // the trailing foot clear the floor instead of scything through it.
        float shin = 1.0 - smoothstep(0.02, 0.26, p.y);
        float bend = max(0.0, -sin(legPhase - 0.6));
        p = rotX(p, vec3(sign(p.x) * 0.05, 0.24, 0.0), -bend * (0.42 + 0.30 * run) * gait * shin);
    }

    // --- 4. Head ------------------------------------------------------------
    // Idle look-around: a slow yaw scan with an occasional downward glance, so
    // she reads as alive rather than frozen. Three detuned sines rather than one,
    // so the scan never repeats on an obvious beat.
    float headMask = smoothstep(0.80, 0.90, p.y);
    if (headMask > 0.001) {
        vec3 neck = vec3(0.0, 0.83, 0.0);
        float lookYaw = sin(uTime * 0.42) * 0.34 + sin(uTime * 0.17) * 0.16 + sin(uTime * 0.83) * 0.06;
        float lookPitch = sin(uTime * 0.31 + 2.1) * 0.14 - 0.04;
        // Walking, she mostly faces forward — but the head counter-rotates
        // slightly against the shoulders, keeping the gaze level as the torso
        // twists underneath it.
        float walkBlend = clamp(gait, 0.0, 1.0);
        lookYaw = mix(lookYaw, -sin(stride) * 0.13 + sin(stride * 0.5) * 0.07, walkBlend);
        lookPitch = mix(lookPitch, -0.05 - 0.10 * run, walkBlend);
        p = rotY(p, neck, lookYaw * headMask);
        p = rotX(p, neck, lookPitch * headMask);
        // A small vertical bob out of phase with the stride, so the head floats
        // over the gait rather than riding rigidly on top of it.
        p.y += sin(stride * 2.0 + 1.1) * 0.008 * gait * headMask;
    }

    // --- 5. Whole-body motion ----------------------------------------------
    float upper = smoothstep(0.25, 1.0, h);
    // Torso counter-twist: shoulders rotate against the hips every stride.
    p = rotY(p, vec3(0.0, 0.50, 0.0), sin(stride) * 0.10 * gait * upper);
    p.y += sin(uTime * 1.5) * 0.005 * upper;                  // breathing
    p.y += abs(sin(stride)) * (0.028 + 0.022 * run) * gait;   // gait bob
    p.x += sin(uTime * 0.7 + 1.2) * 0.006 * upper;            // idle drift
    p = rotZ(p, vec3(0.0, 0.0, 0.0), sin(stride) * 0.035 * gait);        // hip sway
    p = rotX(p, vec3(0.0, 0.0, 0.0), 0.05 * gait + 0.09 * run);          // forward lean

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

            CharacterMesh.load(appContext, "Models/Anime_Character.omesh")?.let { mesh ->
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

            charTex = loadTex("Models/Anime_Texture.png", 0xFFE8D5C8.toInt())
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
            IconGlyphButton(36.dp, Yellow, onClick = onClose) { c ->
                androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) { drawCloseGlyph(c) }
            }
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


/** Exposes the resolved app language so the whole UI can rebuild on change. */
@HiltViewModel
class AppLocaleVM @Inject constructor(private val locales: LocaleStore) : ViewModel() {
    val language: StateFlow<AppLanguage> = locales.observeSelection()
        .map { sel ->
            if (sel == AppLanguage.SYSTEM) AppLanguage.matchDevice()
            else AppLanguage.fromTag(sel) ?: AppLanguage.matchDevice()
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppLanguage.matchDevice())
}


/** Live countdown to the daily rotation. Ticks once a second — cheap, and the
 *  number would look broken updating any slower. */
@Composable
private fun DailyResetCountdown(vm: MarketVM) {
    var remaining by remember { mutableStateOf(vm.millisUntilDailyReset()) }
    LaunchedEffect(Unit) {
        while (true) {
            remaining = vm.millisUntilDailyReset()
            delay(1000)
        }
    }
    val total = remaining / 1000
    val text = String.format(
        Locale.US, "%02d:%02d:%02d",
        total / 3600, (total % 3600) / 60, total % 60
    )
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(0.45f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.Canvas(Modifier.size(14.dp)) { drawStopwatchGlyph(CrtAmber) }
        Spacer(Modifier.width(8.dp))
        Text(
            stringResource(R.string.daily_resets_in, text),
            color = CrtAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp
        )
    }
}


/** Square icon button backed by a vector drawable. The drawable-based twin of
 *  [IconGlyphButton], used wherever the artwork is static — which is most
 *  places. Code-drawn glyphs remain only where the icon animates. */
@Composable
private fun IconResButton(
    size: Dp,
    @DrawableRes iconRes: Int,
    accent: Color,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        if (pressed) 1f else 0f,
        spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
        label = "iconResPress"
    )
    val inf = rememberInfiniteTransition(label = "iconResIdle")
    val breath by inf.animateFloat(
        0.30f, 0.58f,
        infiniteRepeatable(tween(3100, easing = EaseInOut), RepeatMode.Reverse),
        "iconResBreath"
    )
    Box(
        Modifier
            .size(size)
            .graphicsLayer {
                val s = 1f - press * 0.10f
                scaleX = s; scaleY = s
                translationY = press * 2.5f
                shadowElevation = (7f - press * 6f) * density
                spotShadowColor = accent.copy(0.5f)
                ambientShadowColor = Color.Black
                shape = RoundedCornerShape(11.dp)
                clip = false
            }
            .clickable(interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Same construction as the navigation rail: bloom, a domed body lit from
        // above, a bevel that inverts on press. The lobby had three different
        // button treatments; now it has one, at three sizes.
        androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
            val corner = androidx.compose.ui.geometry.CornerRadius(this.size.minDimension * 0.26f)
            val rect = Size(this.size.width, this.size.height)
            drawRoundRect(
                Brush.radialGradient(
                    listOf(accent.copy(0.18f * breath), Color.Transparent),
                    center = center, radius = this.size.minDimension * 0.9f
                ),
                size = rect, cornerRadius = corner
            )
            drawRoundRect(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF2E2C26).copy(0.95f - press * 0.14f),
                        Color(0xFF131209).copy(0.96f),
                        accent.copy(0.16f)
                    ),
                    startY = 0f, endY = this.size.height
                ),
                size = rect, cornerRadius = corner
            )
            drawLine(
                Color.White.copy(0.42f - press * 0.34f),
                Offset(this.size.width * 0.24f, 1.4f), Offset(this.size.width * 0.76f, 1.4f),
                strokeWidth = this.size.minDimension * 0.045f, cap = StrokeCap.Round
            )
            drawLine(
                Color.Black.copy(0.32f + press * 0.32f),
                Offset(this.size.width * 0.24f, this.size.height - 1.4f),
                Offset(this.size.width * 0.76f, this.size.height - 1.4f),
                strokeWidth = this.size.minDimension * 0.045f, cap = StrokeCap.Round
            )
            drawRoundRect(
                accent.copy(0.32f + breath * 0.30f + press * 0.30f),
                size = rect, cornerRadius = corner,
                style = Stroke(this.size.minDimension * 0.038f)
            )
        }
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = accent,
            modifier = Modifier
                .fillMaxSize()
                .padding(size * 0.24f)
                .graphicsLayer { translationY = press * 2f }
        )
        androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
            drawOval(
                Brush.radialGradient(listOf(Color.White.copy(0.14f - press * 0.10f), Color.Transparent)),
                topLeft = Offset(this.size.width * 0.22f, -this.size.height * 0.10f),
                size = Size(this.size.width * 0.56f, this.size.height * 0.44f)
            )
        }
    }
}
