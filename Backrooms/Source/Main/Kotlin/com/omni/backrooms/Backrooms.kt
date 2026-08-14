
package com.omni.backrooms

import android.app.Activity
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Process
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
                OmniLog.w("Guard", "APP_START_THREAT flags=$flags report=$report")
            }
        }
    }




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
    fun provideSettingsRepository(store: DataStore<Preferences>, bridge: NativeBridge): SettingsRepository =
        SettingsRepository(store, bridge)

}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {



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


@Composable
private fun IntroScreen(onDone: () -> Unit, vm: IntroVM = hiltViewModel()) {
    val presents = stringResource(R.string.splash_presents)
    var done by remember { mutableStateOf(false) }

    fun finish() { if (!done) { done = true; vm.stop(); onDone() } }

    DisposableEffect(Unit) {
        vm.play(INTRO_SECONDS)
        onDispose { vm.stop() }
    }

    val clock = rememberInfiniteTransition(label = "intro")
    val t by clock.animateFloat(
        initialValue = 0f, targetValue = INTRO_TOTAL,
        animationSpec = infiniteRepeatable(
            tween((INTRO_TOTAL * 1000).toInt(), easing = LinearEasing)),
        label = "introTime"
    )

    LaunchedEffect(Unit) { delay((INTRO_TOTAL * 1000).toLong()); finish() }



    val lock = ((t - 0.35f) / 1.1f).coerceIn(0f, 1f)
    val fade = when {
        t < 0.25f              -> t / 0.25f
        t > INTRO_TOTAL - 0.5f -> ((INTRO_TOTAL - t) / 0.5f).coerceAtLeast(0f)
        else                   -> 1f
    }


    val dropout = if (introNoise(t * 11f + 3.1f) > -0.55f) 1f else 0.25f

    val wobble = (1f - lock) * 14f * introNoise(t * 23f)
    val split  = 2.5f + (1f - lock) * 9f + introNoise(t * 17f) * 1.5f

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { finish() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .offset { IntOffset(wobble.toInt(), 0) }
                .alpha(fade * dropout),
            contentAlignment = Alignment.Center
        ) {



            IntroWord(presents, Color(0xFFFF2B2B), (-split).dp, 0.75f)
            IntroWord(presents, Color(0xFF29FFF3),   split.dp,  0.75f)
            IntroWord(presents, Color(0xFFF2F0E6),      0.dp,   1f)
        }

        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height



            var y = 0f
            while (y < h) {
                drawRect(Color.Black.copy(alpha = 0.30f), Offset(0f, y), Size(w, 1.6f))
                y += 3.2f
            }



            val bandY = h * (1f - ((t * 0.42f) % 1f))
            val bandH = 26f + (1f - lock) * 40f
            for (i in 0 until 90) {
                val ly = bandY + (i / 90f) * bandH
                if (ly < -bandH || ly > h) continue
                val n = introNoise(i * 3.7f + t * 60f)
                drawRect(
                    Color(0xFFBFB9A8).copy(alpha = (0.05f + 0.16f * kotlin.math.abs(n)) * fade),
                    Offset(w * n * 0.5f, ly),
                    Size(w * (0.35f + kotlin.math.abs(n) * 0.65f), 1.4f)
                )
            }


            drawRect(
                Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                    center = Offset(w / 2f, h / 2f),
                    radius = kotlin.math.max(w, h) * 0.62f
                )
            )
        }
    }
}

@Composable
private fun IntroWord(text: String, colour: Color, dx: Dp, alpha: Float) {
    Text(
        text,
        color = colour.copy(alpha = alpha),
        fontSize = 26.sp,
        fontWeight = FontWeight.Light,
        letterSpacing = 7.sp,
        maxLines = 1,
        modifier = Modifier.offset(x = dx)
    )
}


private const val INTRO_SECONDS = 2.6f
private const val INTRO_TOTAL   = 3.3f


private fun introHash(n: Int): Float {
    var x = n
    x = (x xor 61) xor (x ushr 16)
    x += (x shl 3)
    x = x xor (x ushr 4)
    x *= 0x27D4EB2D
    x = x xor (x ushr 15)
    return (x and 0xFFFFFF) / 0xFFFFFF.toFloat()
}

private fun introNoise(x: Float): Float {
    val i = kotlin.math.floor(x)
    var f = x - i
    f = f * f * (3f - 2f * f)
    fun w(n: Int) = introHash(n) * 2f - 1f
    val n = i.toInt()
    return w(n) + (w(n + 1) - w(n)) * f
}

@HiltViewModel
class IntroVM @Inject constructor(private val bridge: NativeBridge) : ViewModel() {



    fun play(seconds: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                bridge.initSound()
                bridge.setHumVolume(0f)
                bridge.setAmbienceLevel(0f)
                bridge.playIntroSting(seconds)
            }.onFailure { OmniLog.e("Intro", "sting failed", it) }
        }
    }

    fun stop() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { bridge.stopIntroSting(); bridge.destroySound() }
        }
    }
}

private object Route {
    const val INTRO       = "intro"
    const val MENU        = "menu"
    const val GAME        = "game"
    const val SETTINGS    = "settings"
    const val STORY       = "story"
    const val MARKET      = "market"
    const val LEADERBOARD = "leaderboard"
    const val PROFILE     = "profile"
    const val UI_EDITOR   = "ui_editor"
}

@Composable
fun OmniBackroomsApp(localeVm: AppLocaleVM = hiltViewModel()) {
    val language by localeVm.language.collectAsState()
    val ctx = LocalContext.current




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



        if (BuildConfig.ENABLE_GUARD && guardReport.threatLevel >= ThreatLevel.HIGH) showGuardDialog = true
    }
    MaterialTheme(colorScheme = darkColorScheme()) {
        if (showGuardDialog) {
            AlertDialog(
                onDismissRequest = { showGuardDialog = false },
                title    = { Text(stringResource(R.string.guard_threat_title)) },
                text     = {



                    val why = buildList {
                        if (guardReport.isFrida) add("frida")
                        if (guardReport.isHookDetected) add("hook")
                        if (guardReport.isRooted) add("root")
                        if (!guardReport.isSignatureValid) add("signature")
                        if (guardReport.isMemoryTampered) add("memory")
                        if (guardReport.flags != 0)
                            add("flags=0x" + Integer.toHexString(guardReport.flags))
                    }.joinToString(", ").ifEmpty { "—" }
                    Column {
                        Text(stringResource(R.string.guard_threat_message))
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${stringResource(R.string.guard_threat_reason)}: $why",
                            color = TextDim, fontSize = 11.sp
                        )





                        if (guardReport.report.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(guardReport.report, color = TextDim, fontSize = 9.sp)
                        }
                        OmniLog.sinkPath()?.let { path ->
                            Spacer(Modifier.height(6.dp))
                            Text(path, color = TextDim, fontSize = 9.sp)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showGuardDialog = false }) { Text(stringResource(R.string.common_ok)) }
                }
            )
        }
        NavHost(nav, startDestination = Route.INTRO) {
            composable(Route.INTRO, exitTransition = { fadeOut(tween(500)) }) {
                IntroScreen(onDone = {


                    nav.navigate(Route.MENU) {
                        popUpTo(Route.INTRO) { inclusive = true }
                    }
                })
            }
            composable(
                Route.MENU,
                enterTransition = { fadeIn(tween(600)) },
                exitTransition  = { fadeOut(tween(400)) }
            ) {



                NotificationPermissionGate()
                MainMenu(
                    onPlay        = { resume -> nav.navigate("${Route.GAME}?resume=$resume") },
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



                enterTransition = { scaleIn(tween(360), initialScale = 0.92f) + fadeIn(tween(360)) },
                exitTransition  = { scaleOut(tween(260), targetScale = 0.94f) + fadeOut(tween(260)) }
            ) { UiEditor(onSave = { nav.popBackStack() }) }
        }
    }
}


enum class EntityType(
    val typeId    : Int,

    val nativeAiId: Int,
    val baseSpeed : Float,
    val hearRange : Float,
    val sightRange: Float,
    val aggroRange: Float,
    val displayName: String
) {
    SMILER(0, 0, 2.8f, 12f, 18f, 9f, "Smiler")
}

data class SpawnConfig(val count: Int, val speedMult: Float, val sightMult: Float, val spawnIntervalMs: Long)
data class LevelTheme(val id: String, val primaryColor: Color = Yellow, val bgColor: Color = DarkBg)


data class StoryChapterRaw(
    val id                  : Int,
    val titleLocalised      : String,
    val titleSource         : String,
    val unlocked            : Boolean,
    val paragraphsLocalised : List<String>,
    val paragraphsSource    : List<String>
)


data class StoryJson(val version: Int, val chapters: List<StoryChapterRaw>)


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
        "easy" -> SpawnConfig(count=1, speedMult=0.7f, sightMult=0.8f, spawnIntervalMs=3_600_000)
        "hard" -> SpawnConfig(count=1, speedMult=1.4f, sightMult=1.3f, spawnIntervalMs=3_600_000)
        else   -> SpawnConfig(count=1, speedMult=1.0f, sightMult=1.0f, spawnIntervalMs=3_600_000)
    }




    fun loadStory(languageTag: String = Locale.getDefault().language): StoryJson {
        storyCacheByLang[languageTag]?.let { return it }

        fun readMono(name: String): StoryFileMono? =
            runCatching { ctx.assets.open(name).bufferedReader().readText() }
                .mapCatching { json.decodeFromString<StoryFileMono>(it) }
                .getOrNull()

        val fallback = readMono("Story/en.json")

        val localised = if (languageTag == "en") null else readMono("Story/$languageTag.json")

        val byIdFallback = fallback?.chapters?.associateBy { it.id } ?: emptyMap()
        val byIdLocal    = localised?.chapters?.associateBy { it.id } ?: emptyMap()
        val ids = (byIdFallback.keys + byIdLocal.keys).sorted()

        val merged = ids.map { id ->
            val f = byIdFallback[id]; val l = byIdLocal[id]



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




    fun storyChapterToDto(raw: StoryChapterRaw): StoryChapterDto = StoryChapterDto(
        id        = raw.id,
        titleTr   = raw.titleLocalised,
        titleEn   = raw.titleSource,
        contentTr = raw.paragraphsLocalised.joinToString("\n\n"),
        contentEn = raw.paragraphsSource.joinToString("\n\n"),
        isUnlocked= raw.unlocked
    )

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



        val sigCheckOn = BuildConfig.EXPECTED_SIG_HASH.isNotBlank()
        val sigValid  = if (sigCheckOn) bridge.isSignatureValid() else true
        val hookEvidence = hookingEvidence()
        val hook      = hookEvidence != null
        val memEvidence = memoryTamperEvidence()
        val memTamper = memEvidence != null
        val reportStr = bridge.getThreatReport()



        OmniLog.i(
            "Guard",
            "scan flags=0x${Integer.toHexString(flags)} rooted=$rooted frida=$frida " +
            "debugged=$debugged emulator=$emulator sigCheckOn=$sigCheckOn sigValid=$sigValid " +
            "hook=$hook memTamper=$memTamper native='$reportStr'"
        )



        hookEvidence?.let { OmniLog.w("Guard", "hook evidence: $it") }
        memEvidence?.let { OmniLog.w("Guard", "memory-map evidence: ${it.trim()}") }








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
        if (level >= ThreatLevel.HIGH) OmniLog.e("Guard", "GUARD_THREAT level=$level report=$reportStr")
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



    private fun hookingEvidence(): String? = runCatching {
        Thread.currentThread().stackTrace.firstOrNull { el ->
            listOf("xposed", "substrate", "lsposed", "frida")
                .any { el.className.contains(it, ignoreCase = true) }
        }?.className
    }.getOrNull()




    private fun memoryTamperEvidence(): String? = runCatching {
        val markers = listOf(
            "frida-agent", "frida-gadget", "libfrida", "re.frida.server",
            "libsubstrate.so", "libsubstrate-dvm.so",
            "XposedBridge.jar", "libxposed", "liblspd.so", "lspd/", "EdXposed"
        )
        File("/proc/self/maps").useLines { lines ->
            lines.firstOrNull { line ->
                markers.any { line.contains(it, ignoreCase = true) }
            }
        }
    }.getOrNull()
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


                        OmniLog.e("Guard", "CRITICAL_THREAT: ${report.value.report}")
                        android.os.Process.killProcess(android.os.Process.myPid())
                    }
                    ThreatLevel.HIGH -> OmniLog.w("Guard", "HIGH_THREAT: ${report.value.report}")
                    else -> {}
                }
            }
        }
    }
    fun refresh() { viewModelScope.launch(Dispatchers.IO) { guardManager.runFullScan() } }
    fun verifySignature(): Boolean = guardManager.verifyApkSignature()
}


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



    val ownedIds    : Set<String>         = emptySet(),


    val equippedFrame: String             = "",
    val equippedTrail: String             = ""
)

@HiltViewModel
class MarketVM @Inject constructor(
    private val assetManager: AssetManager,
    private val cosmetics   : CosmeticsStore,
    @ApplicationContext private val appCtx: Context
) : ViewModel() {
    private val _state = MutableStateFlow(MarketUiState())
    val state: StateFlow<MarketUiState> = _state.asStateFlow()

    init {
        loadTab(MarketTab.Looks); loadDaily(); loadProfile()


        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                cosmetics.observeOwnedFrames(),
                cosmetics.observeOwnedTrails()
            ) { frames, trails ->
                frames.map { "frame_$it" }.toSet() + trails.map { "trail_$it" }.toSet()
            }.collect { owned -> _state.update { it.copy(ownedIds = owned) } }
        }
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                cosmetics.observeFrame(), cosmetics.observeTrail()
            ) { f, t -> f to t }.collect { (f, t) ->
                _state.update { it.copy(equippedFrame = f, equippedTrail = t) }
            }
        }



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
            _state.update { it.copy(omniumBal = localOmnium) }
        }
    }

    fun setTab(tab: MarketTab) {
        _state.update { it.copy(tab = tab) }


        when (tab) { MarketTab.Daily -> return; else -> loadTab(tab) }
    }

    private fun loadTab(tab: MarketTab) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            _state.update { it.copy(isLoading = false, error = null, items = fallbackItems(tab)) }
        }
    }

    private fun loadDaily() {
        viewModelScope.launch {
            _state.update { it.copy(dailyDeals = fallbackDaily()) }
        }
    }

    fun loadCharacters() {
        viewModelScope.launch {
            _state.update { it.copy(charsLoading = true) }
            _state.update { it.copy(charsLoading = false, characters = emptyList()) }
        }
    }

    fun selectChar(char: CharacterDto) { _state.update { it.copy(selectedChar = char) } }

    fun equip(char: CharacterDto) {
        viewModelScope.launch {
            _state.update { it.copy(equipping = char.id) }
            _state.update { it.copy(equipping = null) }
            loadCharacters()
        }
    }

    fun confirmBuy(item: MarketItemDto) { _state.update { it.copy(confirmItem = item) } }
    fun cancelBuy()                     { _state.update { it.copy(confirmItem = null) } }

    fun buy(item: MarketItemDto) {
        viewModelScope.launch {
            _state.update { it.copy(purchasing = item.id, confirmItem = null) }





            grantLocally(item)

            _state.update {
                it.copy(
                    purchasing = null,
                    ownedIds   = it.ownedIds + item.id,
                    successMsg = item.id
                )
            }
        }
    }




    private suspend fun grantLocally(item: MarketItemDto) {
        runCatching {
            when {
                item.id.startsWith("frame_") -> {
                    val key = item.id.removePrefix("frame_")
                    cosmetics.grantFrame(key)
                    cosmetics.setFrame(key)
                }
                item.id.startsWith("trail_") -> {
                    val key = item.id.removePrefix("trail_")
                    cosmetics.grantTrail(key)
                    cosmetics.setTrail(key)
                }
                item.id.startsWith("priv_") || item.category == "vip" -> {


                    cosmetics.setVip(true)


                    runCatching {
                        val b = NativeBridge()
                        (0 until b.frameCount()).forEach { i ->
                            b.frameId(i)?.takeIf { it.isNotEmpty() }?.let { cosmetics.grantFrame(it) }
                        }
                        (0 until b.trailCount()).forEach { i ->
                            b.trailId(i)?.takeIf { it.isNotEmpty() }?.let { cosmetics.grantTrail(it) }
                        }
                    }
                }
                item.id == "daily_frame" -> cosmetics.grantFrame("Face_Of_Darkness")
                item.id == "daily_trail" -> cosmetics.grantTrail("Dust_Trail")
                else -> Unit
            }
        }.onFailure { OmniLog.e("Market", "local grant failed for ${item.id}", it) }
    }

    fun clearSuccess() { _state.update { it.copy(successMsg = null) } }




    fun equipTrail(trailId: String) {
        if ("trail_$trailId" !in _state.value.ownedIds) return
        viewModelScope.launch { runCatching { cosmetics.setTrail(trailId) } }
    }

    fun equipFrame(frameId: String) {
        if ("frame_$frameId" !in _state.value.ownedIds) return
        viewModelScope.launch { runCatching { cosmetics.setFrame(frameId) } }
    }



    private fun fallbackItems(tab: MarketTab): List<MarketItemDto> = when (tab) {


        MarketTab.Looks -> listOf(
            MarketItemDto(
                "char_anime", "Anime Kız", "Anime Girl",
                "Ana karakter — görsele dokunup inceleyebilirsin",
                "The main character — tap the art to inspect her",
                "characters", 0, "soulium", null, false, false, true, null
            )
        )






        MarketTab.Frames -> listOf(
            MarketItemDto(
                "frame_Face_Of_Darkness", "Karanlığın Yüzü", "Face of Darkness",
                "Karanlıktan iki göz ve bir sırıtış yüzeye çıkar, sonra kaybolur",
                "Two eyes and a grin surface out of the dark, then go",
                "frames", 0, "omnium", null, false, false, false, null
            ),
            MarketItemDto(
                "frame_Endless_Dimension", "Sonsuz Boyut", "Endless Dimension",
                "İçeri doğru hiç bitmeyen darbeler — asla tekrar etmez",
                "Pulses running inward without end — it never repeats",
                "frames", 0, "omnium", null, false, false, false, null
            ),
            MarketItemDto(
                "frame_Sound_Of_Rooms", "Odaların Sesi", "Sound of Rooms",
                "Odaların sesini gösteren seviye göstergesi",
                "A level meter reading the sound of the rooms",
                "frames", 0, "soulium", null, false, false, true, null
            )
        )

        MarketTab.Trails -> listOf(
            MarketItemDto(
                "trail_Dust_Trail", "Toz İzi", "Dust Trail",
                "Halının tozu ayağının altında kalkar ve yavaşça geri iner",
                "The carpet's dust, kicked up and settling back",
                "trails", 0, "soulium", null, false, false, false, null
            ),
            MarketItemDto(
                "trail_Static_Trail", "Statik İz", "Static Trail",
                "Yürüdüğün yerde görüntüyü yırtarsın — kısa ömürlü, sert kenarlı",
                "You tear the picture where you walk — short-lived, hard-edged",
                "trails", 0, "omnium", null, false, false, false, null
            ),
            MarketItemDto(
                "trail_Salt_Trail", "Tuz İzi", "Salt Trail",
                "Üstünden dökülen bir şey. En uzun kalan iz",
                "Something crystalline coming off you. The mark that lasts longest",
                "trails", 0, "soulium", null, false, false, true, null
            )
        )
        MarketTab.Vip -> listOf(
            MarketItemDto(
                "priv_all", "VIP", "VIP",
                "Her Koşudan 2 Kat Omnium, Ve Tüm Görsel Ayrıcalıklar",
                "Double Omnium From Every Run, And Every Cosmetic Privilege",
                "vip", 0, "soulium", null, false, false, true, null
            )
        )
        else -> emptyList()
    }




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


        return listOf(pool[day % pool.size], pool[(day + 1) % pool.size])
    }


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
    private val assetManager: AssetManager
) : ViewModel() {
    private val _state = MutableStateFlow(StoryUiState())
    val state: StateFlow<StoryUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val local = assetManager.loadStory().chapters.map { assetManager.storyChapterToDto(it) }
            _state.update { it.copy(isLoading = false, chapters = local) }
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


    @Volatile private var cachedSensitivity = 1f


    @Volatile private var cachedVolume = 0.7f


    private var started = false

    private companion object {


        const val MOVE_FORCE = 2_300f


        const val SPRINT_MULT = 1.95f
        const val CROUCH_MULT = 0.42f

        const val SPRINT_DRAIN = 22f

        const val SPRINT_FLOOR = 5f





        const val EXIT_LEASH_M = 620f

        const val OMNIUM_PER_MINUTE = 12L
        const val OMNIUM_ESCAPE_BONUS = 150L

        const val VIP_OMNIUM_MULTIPLIER = 2L
    }



    private var world: WorldInfo = WorldInfo.EMPTY




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


            val roomBudget = if (useDiff == "hard") 180 else 130
            world = WorldInfo.parse(bridge.generateLevel(roomBudget, depth = 0))
            OmniLog.i("Game", "infinite world cell=${world.cellSize} spawn=(${world.spawnX},${world.spawnZ}) exit=(${world.exitX},${world.exitZ})")

            val cfg = assetManager.getSpawnConfig(useDiff)
            spawnInitialEntities(bridge, world, cfg)


            vipRun = runCatching { cosmetics.observeVip().first() }.getOrDefault(false)




            runCatching {
                bridge.trailClear()
                val equipped = cosmetics.observeTrail().first()
                val idx = (0 until bridge.trailCount()).firstOrNull { bridge.trailId(it) == equipped } ?: 0
                bridge.trailSetStyle(idx)
            }


            elapsedMs = saved?.elapsedMs ?: 0L
            score     = saved?.score ?: 0L
            kills     = saved?.kills ?: 0



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


    private fun applyAudioLevels() {
        val v = cachedVolume.coerceIn(0f, 1f)
        runCatching {
            bridge.setAmbienceLevel(0.55f * v)
            bridge.setHumVolume(0.42f * v)
        }
    }

    private fun startPhysicsLoop(sensitivity: Float) {
        cachedSensitivity = sensitivity

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
                lastTickMs = now






                val runOver = _state.value.let { it.isGameOver || it.isEscaped || it.isMadnessOver }
                if (!runOver) {
                    elapsedMs += (dt * 1000).toLong()
                } else {





                    val s = _state.value
                    val kind = if (s.isEscaped) 2 else 1
                    val el = s.endingElapsed + dt
                    val p = runCatching { bridge.endingParams(kind, el) }.getOrNull()
                    if (p != null && p.size >= 8) endingSnapshot = p
                    val panel = if (p != null && p.size >= 8) p[7] else 1f
                    _state.update { it.copy(endingElapsed = el, endingPanel = panel) }
                }



                runCatching { bridge.trailUpdate(dt) }








                val mx = moveX; val mz = moveZ
                val mag = kotlin.math.hypot(mx, mz).coerceAtMost(1f)
                val snapshot = _state.value
                val wantsSprint = sprinting && snapshot.stamina > SPRINT_FLOOR && !snapshot.isCrouching




                if (mag > 0.02f && !madnessRunning && !runOver) {
                    val paceMult = when {
                        snapshot.isCrouching -> CROUCH_MULT
                        wantsSprint          -> SPRINT_MULT
                        else                 -> 1f
                    }


                    val force = MOVE_FORCE * paceMult
                    bridge.applyMovement(mx * force, 0f, mz * force)

                    footstepTimer -= dt * mag * paceMult
                    if (footstepTimer <= 0f) {
                        footstepTimer = 0.45f
                        bridge.triggerFootstep(if (wantsSprint) 180f else 120f, 0.3f)


                        footSide = -footSide
                        snapshot.camera?.let { c ->




                            val printYaw = avatarYawSource?.invoke() ?: c.yaw
                            runCatching { bridge.trailStep(c.posX, c.posZ, printYaw, footSide) }
                        }
                    }
                } else {



                    footstepTimer = 0f
                    runCatching { bridge.stopFootstep() }
                }


                if (wantsSprint && mag > 0.02f) {
                    _state.update {
                        it.copy(stamina = (it.stamina - SPRINT_DRAIN * dt * mag).coerceAtLeast(0f))
                    }
                }


                val sprintingNow = wantsSprint && mag > 0.02f
                if (sprinting && snapshot.stamina <= SPRINT_FLOOR) sprinting = false
                if (snapshot.isSprinting != sprintingNow) {
                    _state.update { it.copy(isSprinting = sprintingNow) }
                }




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

                val wasOver = _state.value.isGameOver
                val derived = stepSimulation(bridge, dt, _state.value)
                _state.update { applyTickToState(it, derived, dt, elapsedMs, score) }
                if (!wasOver && _state.value.isGameOver) {


                    val earned = omniumForRun(elapsedMs, escaped = false)
                    _state.update { it.copy(omniumEarned = earned) }
                    launch { runCatching { cosmetics.addOmnium(earned) } }
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





                val s = _state.value
                val runOver = s.isGameOver || s.isEscaped || s.isMadnessOver
                if (!s.isPaused && !runOver) {
                    score += when (s.difficulty) { "hard" -> 5L; "normal" -> 3L; else -> 1L }
                }
                delay(1_000)
            }
        }
    }







    @Volatile var endingSnapshot: FloatArray = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)
        private set




    @Volatile var avatarYawSource: (() -> Float)? = null

    @Volatile private var moveX = 0f
    @Volatile private var moveZ = 0f
    @Volatile private var sprinting = false
    private var footstepTimer = 0f

    private var footSide = 1f
    private var exitCheckTimer = 0f


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

    fun toggleFlashlight() {
        _state.update { it.copy(flashlightOn = !it.flashlightOn) }


        runCatching { bridge.playTorchClick() }
    }
    fun togglePause() {
        val nowPaused = !_state.value.isPaused
        _state.update { it.copy(isPaused = nowPaused) }








        if (nowPaused) runCatching {
            bridge.setAmbienceLevel(0f); bridge.setHumVolume(0f); bridge.stopFootstep()
        } else applyAudioLevels()
    }



    val canEscape: Boolean get() = _state.value.distanceToExit < 3.5f



    fun onScreenPaused() {
        _state.update { it.copy(isPaused = true) }
        runCatching { bridge.setAmbienceLevel(0f); bridge.setHumVolume(0f); bridge.stopFootstep() }
        saveNow()
    }




    fun reportFps(fps: Float) {
        val rounded = fps.roundToInt()
        if (rounded != _state.value.fps) _state.update { it.copy(fps = rounded) }
    }

    fun onScreenResumed() {
        applyAudioLevels()
        _state.update { it.copy(isPaused = false) }
    }




    private fun saveNow() {
        val s = _state.value









        if (s.isGameOver || s.isEscaped || s.isMadnessOver) {
            saveStore.clearDetached()
            return
        }
        if (!s.world.isValid) return


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



    private fun playSpawnDrop() {
        viewModelScope.launch {
            _state.update { it.copy(spawnPhase = SpawnPhase.FALLING, eyeOffset = 0f) }


            var waited = 0L
            while (waited < 4000 && _state.value.camera?.let { it.posY > 2.2f } != false) {
                delay(50); waited += 50
            }


            _state.update { it.copy(spawnPhase = SpawnPhase.LANDED, eyeOffset = -1.45f) }
            runCatching { bridge.triggerFootstep(60f, 1.0f) }
            delay(650)



            val steps = 26
            for (i in 1..steps) {
                val t = i / steps.toFloat()

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
            finishRun()
        }
    }




    @Volatile private var vipRun = false


    private fun omniumForRun(elapsed: Long, escaped: Boolean): Long {
        val minutes = elapsed / 60_000f
        val base = (minutes * OMNIUM_PER_MINUTE).toLong()
        val bonus = if (escaped) OMNIUM_ESCAPE_BONUS else 0L


        val vipMult = if (vipRun) VIP_OMNIUM_MULTIPLIER else 1L
        return ((base + bonus) * vipMult).coerceAtLeast(0L)
    }




    private fun checkSanity(dt: Float) {
        val s = _state.value
        if (s.isMadnessOver || s.isGameOver || s.isEscaped) return



        val target = ((18f - s.sanity) / 18f).coerceIn(0f, 1f)
        if (kotlin.math.abs(target - s.madness) > 0.005f) {
            _state.update { it.copy(madness = it.madness + (target - it.madness) * (dt * 1.5f)) }
        }

        if (s.sanity > 0f) { madnessFuse = -1f; return }
        if (madnessFuse < 0f) {

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


            val steps = 30
            for (i in 1..steps) {
                val t = i / steps.toFloat()
                val eased = t * t
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
            finishRun()
        }
    }

    fun onDamageEntity(id: Int) {
        bridge.damageEntity(id, 25f); kills++; score += 100L
        _state.update { it.copy(kills = kills, score = score) }
    }



    fun collectTrail(): FloatArray? = runCatching { bridge.trailCollect() }.getOrNull()



    val equippedTrail: StateFlow<String> = cosmetics.observeTrail()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")


    fun trailStyleSpec(): FloatArray? = runCatching {
        val equipped = runBlocking { cosmetics.observeTrail().first() }
        val idx = (0 until bridge.trailCount()).firstOrNull { bridge.trailId(it) == equipped } ?: 0
        bridge.trailSpec(idx)
    }.getOrNull()



    fun fetchChunk(chunkX: Int, chunkZ: Int): WorldChunk? {
        val w = _state.value.world
        if (!w.isValid) return null
        return WorldChunk.parse(chunkX, chunkZ, w.chunkCells, bridge.generateChunk(chunkX, chunkZ))
    }




    private fun finishRun() {
        viewModelScope.launch {




            saveStore.clearDetached()

            runCatching { cosmetics.recordSurvival(elapsedMs) }
        }
    }

    override fun onCleared() {
        physicsJob?.cancel(); entityJob?.cancel(); scoreJob?.cancel(); autosaveJob?.cancel()
        runBlocking { bridge.destroyEntities(); bridge.destroySound(); bridge.destroyCore() }
        super.onCleared()
    }
}

@HiltViewModel
class LeaderboardVM @Inject constructor() : ViewModel() {
    private val _entries = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val entries: StateFlow<List<LeaderboardEntry>> = _entries.asStateFlow()
}

@HiltViewModel
class ProfileVM @Inject constructor(
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
            _profile.value = _profile.value.copy(name = identity.currentName())
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

        LobbyVideoBackground(Modifier.fillMaxSize())
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(0.55f)),
                    radius = 900f
                )
            )
        )


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


        Row(
            Modifier.align(Alignment.TopEnd).padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconResButton(40.dp, R.drawable.ic_leaderboard, TextSec, onClick = { toast = comingSoon })
            IconResButton(40.dp, R.drawable.ic_settings,    Yellow,  onClick = onSettings)
        }






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


private fun linkGlProgram(vertSrc: String, fragSrc: String, label: String = "?"): Int {
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
        OmniLog.e("GL", "program '$label' failed to link: $log")
        throw RuntimeException("Omni program '$label' link failed: $log")
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


private const val kCeilTileM = 0.60f


private const val kProbeSide = 24

private const val OMNI_SCENE_FRAG = """#version 300 es
precision mediump float;
in highp vec3 vNormal; in highp vec2 vUV; in highp float vLight; in highp vec3 vWorldPos;
uniform vec3 uCamPos;
uniform float uFogDensity; uniform vec3 uFogColor; uniform float uFlicker;

uniform vec3 uTorchPos; uniform vec3 uTorchDir; uniform float uTorchOn;
uniform float uBumpStrength;
uniform vec3 uLampTint;

uniform vec3 uFlatAlbedo;

const float kCeilTile  = 0.60;
const float kCarpetTile = 0.50;
const float kWallModule = 0.80;
const float kRailWidth  = 0.018;
const float kSeamWidth  = 0.004;

uniform float uTime;

out vec4 fragColor;

float vhash(vec2 p){ return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }
float vnoise(vec2 p){
    vec2 i = floor(p), f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(vhash(i), vhash(i + vec2(1.0, 0.0)), u.x),
               mix(vhash(i + vec2(0.0, 1.0)), vhash(i + vec2(1.0, 1.0)), u.x), u.y);
}
float fbm2(vec2 p){ return vnoise(p) * 0.62 + vnoise(p * 2.17 + 4.1) * 0.38; }

const vec3 kWallBase  = vec3(0.470, 0.390, 0.197);
const vec3 kFloorBase = vec3(0.432, 0.353, 0.178);
const vec3 kCeilBase  = vec3(0.827, 0.818, 0.795);
const float kWallGrain  = 0.186;
const float kFloorGrain = 0.190;
const float kCeilGrain  = 0.086;

vec3 surfaceWall(vec3 wp) {
    vec2 q = vec2(abs(wp.x) > abs(wp.z) ? wp.z : wp.x, wp.y);
    float mottle = fbm2(q * 3.1) - 0.5;
    float tooth  = fbm2(q * 27.0) - 0.5;
    float streak = (fbm2(vec2(q.x * 22.0, q.y * 0.7)) - 0.5) * 0.6;
    float g = mottle * 0.62 + tooth * 0.30 + streak * 0.28;
    return kWallBase * (1.0 + g * kWallGrain * 2.0);
}

vec3 surfaceFloor(vec3 wp) {
    float blotch = fbm2(wp.xz * 1.9) - 0.5;
    float pile   = fbm2(wp.xz * 41.0) - 0.5;
    float weave  = fbm2(vec2(wp.x * 90.0, wp.z * 12.0)) - 0.5;
    float g = blotch * 0.55 + pile * 0.34 + weave * 0.22;
    return kFloorBase * (1.0 + g * kFloorGrain * 2.0);
}

vec3 surfaceCeiling(vec3 wp) {
    float pin  = fbm2(wp.xz * 64.0) - 0.5;
    float wash = fbm2(wp.xz * 2.6) - 0.5;
    float g = pin * 0.72 + wash * 0.34;
    return kCeilBase * (1.0 + g * kCeilGrain * 2.0);
}

vec3 surfaceAlbedo(vec3 wp, vec3 nrm) {
    if (nrm.y < -0.5) return surfaceCeiling(wp);
    if (nrm.y >  0.5) return surfaceFloor(wp);
    return surfaceWall(wp);
}

void main(){
    vec3 geoN = normalize(vNormal);
    vec4 tex = vec4(uFlatAlbedo.r >= 0.0 ? uFlatAlbedo
                                         : surfaceAlbedo(vWorldPos, geoN), 1.0);

    vec3 n = geoN;
    if (uBumpStrength > 0.001 && uFlatAlbedo.r < 0.0) {
        const vec3 kLuma = vec3(0.299, 0.587, 0.114);
        vec3 up0 = abs(geoN.y) > 0.9 ? vec3(0.0, 0.0, 1.0) : vec3(0.0, 1.0, 0.0);
        vec3 tx = normalize(cross(up0, geoN));
        vec3 ty = cross(geoN, tx);
        float e = 0.011;
        float hL = dot(surfaceAlbedo(vWorldPos - tx * e, geoN), kLuma);
        float hR = dot(surfaceAlbedo(vWorldPos + tx * e, geoN), kLuma);
        float hD = dot(surfaceAlbedo(vWorldPos - ty * e, geoN), kLuma);
        float hU = dot(surfaceAlbedo(vWorldPos + ty * e, geoN), kLuma);
        vec3 bump = tx * (hL - hR) + ty * (hD - hU);
        n = normalize(n + bump * uBumpStrength);
    }

    vec3 albedo = tex.rgb;
    float dist = length(uCamPos - vWorldPos);
    float detailFade = 1.0 - smoothstep(12.0, 34.0, dist);

    if (n.y < -0.5) {
        vec2 g = fract(vWorldPos.xz / kCeilTile);
        vec2 d = min(g, 1.0 - g) * kCeilTile;
        float rail = 1.0 - smoothstep(kRailWidth * 0.35, kRailWidth, min(d.x, d.y));
        albedo = mix(albedo, albedo * 1.30 + vec3(0.035), rail * detailFade);
        vec2 tileId = floor(vWorldPos.xz / kCeilTile);
        float phase = fract(sin(dot(tileId, vec2(41.3, 289.1))) * 43758.5453);
        float breathe = 1.0 + 0.35 * sin(uTime * 0.21 + phase * 6.2831);
        float toMid = min(d.x, d.y) / (kCeilTile * 0.5);
        float sag = 1.0 - 0.05 * breathe * (1.0 - toMid * 2.0);
        albedo *= mix(1.0, sag, detailFade);

        float b = fbm2(vWorldPos.xz * 0.22 + vec2(uTime * 0.006, -uTime * 0.004));
        float creep = smoothstep(0.52 + 0.05 * sin(uTime * 0.05), 0.78, b);
        vec3 stainCol = vec3(0.52, 0.44, 0.28);
        albedo = mix(albedo, albedo * stainCol * 1.6, creep * 0.55 * detailFade);
    } else if (n.y > 0.5) {
        vec2 t = vWorldPos.xz / kCarpetTile;
        vec2 g = fract(t);
        vec2 d = min(g, 1.0 - g) * kCarpetTile;
        float seam = 1.0 - smoothstep(kSeamWidth * 0.25, kSeamWidth, min(d.x, d.y));
        float weave = mod(floor(t.x) + floor(t.y), 2.0);
        albedo *= mix(1.0, mix(0.985, 1.015, weave), detailFade);
        albedo = mix(albedo, albedo * 0.93, seam * detailFade);
    } else {
        float u = abs(n.x) > 0.5 ? vWorldPos.z : vWorldPos.x;
        float g = fract(u / kWallModule);
        float joint = 1.0 - smoothstep(0.0005, 0.0022, min(g, 1.0 - g) * kWallModule);
        albedo = mix(albedo, albedo * 0.94, joint * detailFade);

        float skirt  = smoothstep(0.102, 0.098, vWorldPos.y);
        float nosing = skirt * smoothstep(0.086, 0.098, vWorldPos.y);
        albedo = mix(albedo, albedo * vec3(0.62, 0.60, 0.55), skirt * detailFade);
        albedo += albedo * 0.20 * nosing * detailFade;
        albedo *= 1.0 - 0.45 * smoothstep(0.014, 0.0, vWorldPos.y) * detailFade;
        float damp = (1.0 - smoothstep(0.0, 0.55, vWorldPos.y)) * 0.16;
        albedo *= 1.0 - damp * detailFade;
        float tide = 0.62 + 0.30 * sin(uTime * 0.037 + fract(u * 0.13) * 6.2831);
        float wet = fbm2(vec2(u * 0.28, vWorldPos.y * 0.55) + vec2(uTime * 0.005, 0.0));
        float rise = (1.0 - smoothstep(0.0, tide, vWorldPos.y)) * smoothstep(0.40, 0.72, wet);
        albedo = mix(albedo, albedo * vec3(0.58, 0.52, 0.40), rise * 0.5 * detailFade);
    }

    float facing = abs(n.y) * 0.55 + 0.45;
    float lit = 0.035 + facing * vLight * uFlicker * 1.85;

    float wallFactor = 1.0 - abs(n.y);
    float groundAO = mix(1.0, mix(0.78, 1.0, smoothstep(0.0, 1.4, vWorldPos.y)), wallFactor);

    vec3 lampMix = mix(vec3(1.0), uLampTint, clamp(vLight * 0.75, 0.0, 1.0));
    vec3 col = albedo * lit * groundAO * lampMix;

    if (uTorchOn > 0.001) {
        vec3  toFrag = vWorldPos - uTorchPos;
        float d      = length(toFrag);
        vec3  L      = toFrag / max(d, 1e-4);
        float cosA   = dot(L, normalize(uTorchDir));
        float cone   = smoothstep(0.72, 0.93, cosA);
        float atten  = 1.0 / (1.0 + 0.14 * d + 0.035 * d * d);
        float ndl    = max(dot(n, -L), 0.0);
        float beam   = cone * atten * uTorchOn;
        col += albedo * vec3(1.00, 0.96, 0.86) * beam * (0.55 + 2.60 * ndl);
        col += vec3(0.9, 0.87, 0.76) * cone * uTorchOn * 0.09
             * smoothstep(0.5, 4.0, d) * (1.0 - smoothstep(8.0, 20.0, d));
    }

    float dustField = fbm2(vec2(vWorldPos.x * 0.5 + uTime * 0.05,
                                vWorldPos.z * 0.5 - uTime * 0.031)
                           + vec2(vWorldPos.y * 0.3, 0.0));
    float dust = smoothstep(0.55, 0.95, dustField)
               * clamp(vLight, 0.0, 1.4)
               * smoothstep(1.0, 9.0, dist) * 0.06;
    col += uLampTint * dust * uFlicker;

    float fog = 1.0 - exp(-uFogDensity * (dist * 0.016 + dist * dist * 0.0009));
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
in highp vec2 vUV;
uniform vec3 uColor; uniform float uAlert; uniform float uAlpha; uniform float uColorBlind;
uniform float uTime; uniform float uSeed; uniform float uDissolve;
out vec4 fragColor;

float hash(vec2 p){ return fract(sin(dot(p, vec2(41.3, 289.1))) * 43758.5453); }
float noise(vec2 p){
    vec2 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);
}

float fbm(vec2 p, float t){
    float v = 0.0, a = 0.52;
    vec2 drift = vec2(0.0, -t * 0.16);
    for (int i = 0; i < 4; ++i) {
        v += a * noise(p + drift);
        p = p * 2.03 + vec2(11.7, 3.1);
        drift *= vec2(-1.7, 0.62);
        a *= 0.5;
    }
    return v;
}

void main(){
    float t = uTime + uSeed * 37.0;

    vec2 p = vUV - vec2(0.5, 0.5);

    float e = 0.045;
    float n1 = fbm(vec2(vUV.x * 3.1, vUV.y * 2.2), t);
    float nx = fbm(vec2((vUV.x + e) * 3.1, vUV.y * 2.2), t) - n1;
    float ny = fbm(vec2(vUV.x * 3.1, (vUV.y + e) * 2.2), t) - n1;
    vec2 curl = vec2(ny, -nx) * 0.55;

    float rise   = clamp(vUV.y, 0.0, 1.0);
    float sway   = sin(t * 0.55 + uSeed * 6.2 + rise * 2.4) * 0.055 * rise;
    float waist  = mix(0.30, 0.135, smoothstep(0.05, 0.78, rise));
    float axis   = p.x - sway - curl.x * 0.35;
    float column = 1.0 - smoothstep(0.0, waist, abs(axis));
    column *= smoothstep(1.02, 0.72, vUV.y) * smoothstep(-0.02, 0.16, vUV.y);

    float turb = fbm(vec2(vUV.x * 4.6 + curl.x * 2.0,
                          vUV.y * 3.0 + curl.y * 2.0 - t * 0.30), t);
    float density = column * (0.55 + 0.95 * turb) - 0.16;
    density = clamp(density, 0.0, 1.0);

    float tendril = fbm(vec2(vUV.x * 9.0 - curl.y * 3.0, vUV.y * 5.0 - t * 0.85), t);
    density = max(density, smoothstep(0.72, 0.98, tendril) * column * 0.75);

    if (uDissolve > 0.001) {
        float grain = noise(vUV * 7.0 + vec2(t * 0.6, -t * 0.35));
        float threshold = uDissolve * 1.35 - (0.45 - length(p)) * 0.55;
        density -= smoothstep(threshold - 0.22, threshold + 0.10, grain + column);
        density = max(density, 0.0);
    }

    if (density < 0.015) discard;

    vec3 col = mix(uColor * 0.42, uColor * 0.045, smoothstep(0.10, 0.75, density));

    float faceLift = smoothstep(0.16, 0.52, density);
    vec2 fc = vec2(0.5 + sway * 0.6 + curl.x * 0.10, 0.615 + curl.y * 0.05);
    vec2 e2 = vec2(abs(vUV.x - fc.x), vUV.y);

    float blinkPhase = fract(t * 0.21 + uSeed);
    float blink = 1.0 - smoothstep(0.0, 0.045, abs(blinkPhase - 0.5)) * 0.94;
    vec2 eyeC = vec2(0.105, fc.y);
    vec2 eyeD = (e2 - eyeC) / vec2(0.080, 0.058 * max(blink, 0.06));
    float eye = smoothstep(1.0, 0.66, length(eyeD)) * faceLift;

    float grin;
    {
        vec2 m = (vUV - vec2(fc.x, fc.y - 0.235)) / vec2(0.195 + uAlert * 0.05, 0.11);
        float curve = m.x * m.x;
        float lip = 1.0 - smoothstep(0.0, 0.30, abs(m.y + curve * 0.85 - 0.30));
        grin = lip * smoothstep(1.25, 1.0, abs(m.x));
        float teeth = smoothstep(0.42, 0.62, abs(fract(m.x * 5.5) - 0.5) * 2.0);
        grin *= mix(1.0, teeth, 0.55);
        grin *= faceLift;
    }

    vec3 normalRamp = mix(vec3(0.92, 0.86, 0.35), vec3(1.0, 0.06, 0.04), uAlert);
    vec3 safeRamp   = mix(vec3(0.30, 0.62, 1.0),  vec3(1.0, 0.62, 0.05), uAlert);
    vec3 lit = mix(normalRamp, safeRamp, uColorBlind);

    vec2 pupilOff = vec2(sin(t * 0.7) * 0.012, sin(t * 0.53) * 0.008);
    float pupil = smoothstep(1.0, 0.55, length((e2 - eyeC - pupilOff) / vec2(0.032, 0.032))) * faceLift;

    col = mix(col, lit, eye);
    col = mix(col, vec3(0.02, 0.01, 0.01), pupil * 0.85);
    col = mix(col, lit * 0.92, grin);
    col += lit * (eye + grin) * 0.34 * faceLift;

    float alpha = clamp(density * 1.35, 0.0, 1.0) * uAlpha;
    alpha = max(alpha, (eye + grin) * 0.85 * uAlpha);
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
in highp vec2 vUV;
uniform float uAlpha; uniform float uTime; uniform float uSeed;
out vec4 fragColor;
float hash(vec2 p){ return fract(sin(dot(p, vec2(41.3, 289.1))) * 43758.5453); }
float noise(vec2 p){
    vec2 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);
}
void main(){
    vec2 d = vUV - vec2(0.5);
    float r = length(d);
    float ang = atan(d.y, d.x);
    float t = uTime + uSeed * 23.0;

    float crawl = noise(vec2(ang * 2.2, t * 0.65 + uSeed * 3.0)) - 0.5;
    float core = smoothstep(0.42 + crawl * 0.07, 0.05, r);
    float breath = 0.80 + 0.20 * sin(t * 1.3 + uSeed);
    float halo = smoothstep(0.50, 0.16, r) * 0.45 * breath;

    float a = clamp(core + halo, 0.0, 1.0) * uAlpha;
    if (a < 0.01) discard;
    fragColor = vec4(0.0, 0.0, 0.0, a);
}
"""


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
in highp vec2 vUV;
uniform float uTime; uniform float uNear;
out vec4 fragColor;
void main(){
    float b1 = sin(uTime * 8.3);
    float b2 = sin(uTime * 23.7 + 1.3);
    float ballast = (b1 * b2 > -0.55) ? 1.0 : 0.28;
    ballast *= 0.82 + 0.18 * sin(uTime * 2.1);

    vec2 d = abs(vUV - 0.5);
    float plate = step(d.x, 0.40) * step(d.y, 0.46);
    float frame = step(d.x, 0.50) * step(d.y, 0.50) - plate;

    float bands = 0.55 + 0.45 * sin(vUV.y * 26.0 - uTime * 3.4);

    vec3 glow = vec3(1.0, 0.96, 0.72);
    vec3 col = glow * (plate * bands * 0.85 + frame * 1.35) * ballast;
    col *= 0.75 + uNear * 0.75;

    float a = clamp(plate * 0.80 + frame, 0.0, 1.0) * ballast;
    if (a < 0.02) discard;
    fragColor = vec4(col, a);
}
"""


private const val OMNI_DECAL_VERT = """#version 300 es
layout(location=0) in vec3 aPos;
layout(location=1) in vec3 aNormal;
layout(location=2) in vec2 aUv;
layout(location=3) in float aAge;
uniform mat4 uMVP;
out vec2 vUv; out float vAge; out float vLit;
void main(){
    vUv = aUv; vAge = aAge; vLit = aNormal.x;
    gl_Position = uMVP * vec4(aPos, 1.0);
}
"""

private const val OMNI_DECAL_FRAG = """#version 300 es
precision mediump float;
in highp vec2 vUv; in highp float vAge; in highp float vLit;

uniform vec3 uTint;

uniform float uMark;
uniform float uTime;
out vec4 fragColor;

float hash(vec2 p){ return fract(sin(dot(p, vec2(41.3, 289.1))) * 43758.5453); }

float sole(vec2 p){
    vec2 ball = p - vec2(0.0, 0.26);
    ball.x /= 0.52; ball.y /= 0.60;
    float b = 1.0 - smoothstep(0.55, 1.0, length(ball));
    vec2 heel = p - vec2(0.0, -0.42);
    heel.x /= 0.40; heel.y /= 0.34;
    float h = 1.0 - smoothstep(0.55, 1.0, length(heel));
    float tread = 0.72 + 0.28 * step(0.0, sin(p.y * 34.0));
    return max(b, h) * tread;
}

float glyph(vec2 p, float t){
    float rows = floor((p.y * 0.5 + 0.5) * 7.0);
    float jitter = (hash(vec2(rows, floor(t * 9.0))) - 0.5) * 0.5;
    float band = step(abs(p.x + jitter), 0.72) * step(abs(p.y), 0.85);
    float noise = step(0.42, hash(vec2(floor(p.x * 14.0) + jitter * 20.0, rows)));
    return band * noise;
}

float grain(vec2 p){
    float acc = 0.0;
    for (int i = 0; i < 7; ++i) {
        float fi = float(i);
        vec2 c = vec2(hash(vec2(fi, 1.7)) - 0.5, hash(vec2(fi, 4.2)) - 0.5) * 1.4;
        acc = max(acc, 1.0 - smoothstep(0.06, 0.20, length(p - c)));
    }
    return acc;
}

void main(){
    if (dot(vUv, vUv) > 1.6) discard;

    float shape;
    if (uMark < 0.5)      shape = sole(vUv);
    else if (uMark < 1.5) shape = glyph(vUv, uTime);
    else                  shape = grain(vUv);

    float soften = mix(1.0, 0.35, vAge);
    shape *= soften;
    float fade = 1.0 - vAge;
    fade *= fade;

    float a = shape * fade * 0.85;
    if (a < 0.004) discard;
    fragColor = vec4(uTint * clamp(vLit, 0.05, 1.6), a);
}
"""

private const val OMNI_TORCH_VERT = """#version 300 es
layout(location=0) in vec3 aPos;
layout(location=1) in vec3 aNormal;
layout(location=2) in vec2 aUV;
layout(location=3) in float aPart;
uniform mat4 uMVP; uniform mat4 uModel;
out vec3 vNormal; out float vPart; out float vAxial;
void main(){
    vNormal = mat3(uModel) * aNormal;
    vPart = aPart;
    vAxial = aUV.y;
    gl_Position = uMVP * vec4(aPos, 1.0);
}
"""

private const val OMNI_TORCH_FRAG = """#version 300 es
precision mediump float;
in highp vec3 vNormal; in highp float vPart; in highp float vAxial;
uniform float uOn;
uniform vec3 uAmbient;
out vec4 fragColor;
void main(){
    vec3 n = normalize(vNormal);
    vec3 key = normalize(vec3(-0.35, 0.86, 0.38));
    float ndl = max(dot(n, key), 0.0);
    float rim = pow(1.0 - abs(n.z), 2.5) * 0.35;

    vec3 col;
    if (vPart > 1.5) {
        vec3 dark = vec3(0.10, 0.10, 0.12);
        vec3 hot  = vec3(1.0, 0.97, 0.84) * 2.6;
        col = mix(dark, hot, uOn);
    } else if (vPart > 0.5) {
        col = vec3(0.52, 0.53, 0.56) * (0.30 + ndl * 0.85) + rim;
        col += vec3(1.0, 0.94, 0.78) * uOn * (1.0 - vAxial) * 0.45;
    } else {
        float knurl = 0.86 + 0.14 * step(0.5, fract(vAxial * 26.0));
        col = vec3(0.16, 0.16, 0.17) * knurl * (0.34 + ndl * 0.80) + rim * 0.6;
    }
    fragColor = vec4(col, 1.0);
}
"""

private const val OMNI_POST_VERT = """#version 300 es
layout(location=0) in vec2 aPos;
out vec2 vUV;
void main(){ vUV = aPos*0.5+0.5; gl_Position = vec4(aPos, 0.0, 1.0); }
"""


private const val OMNI_BRIGHT_FRAG = """#version 300 es
precision mediump float;
in highp vec2 vUV;
uniform sampler2D uScene;
uniform float uThreshold; uniform float uKnee;
out vec4 fragColor;
void main(){
    vec3 c = texture(uScene, vUV).rgb;
    float lum = dot(c, vec3(0.2126, 0.7152, 0.0722));
    float soft = clamp(lum - uThreshold + uKnee, 0.0, 2.0 * uKnee);
    soft = soft * soft / (4.0 * uKnee + 0.0001);
    float contribution = max(soft, lum - uThreshold) / max(lum, 0.0001);
    fragColor = vec4(c * contribution, 1.0);
}
"""


private const val OMNI_BLUR_FRAG = """#version 300 es
precision mediump float;
in highp vec2 vUV;
uniform sampler2D uSource;
uniform vec2 uDir;
out vec4 fragColor;
void main(){
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
in highp vec2 vUV;
uniform sampler2D uScene;
uniform sampler2D uBloom;
uniform float uTime; uniform float uFlicker; uniform float uVhsStrength; uniform vec2 uResolution;
uniform float uColorBlindMix; uniform vec3 uColorBlindAxis;
uniform float uFlashOn; uniform float uMadness; uniform float uBloomStrength;
uniform float uExposure;

uniform vec4 uEnd0; uniform vec4 uEnd1;
out vec4 fragColor;
float rand(vec2 co){ return fract(sin(dot(co, vec2(12.9898,78.233))) * 43758.5453); }
vec3 tonemap(vec3 x){
    const float a = 2.51, b = 0.03, c = 2.43, d = 0.59, e = 0.14;
    return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
}
void main(){
    vec2 centered = vUV * 2.0 - 1.0;
    float r2 = dot(centered, centered);
    vec2 barrel = centered * (1.0 + 0.035 * r2 * uVhsStrength);
    vec2 uv = clamp(barrel * 0.5 + 0.5, 0.0, 1.0);

    if (uEnd0.w > 0.0) {
        float row = floor(uv.y * 84.0);
        uv.x += uEnd0.w * (rand(vec2(row, floor(uTime * 24.0))) - 0.5) * 2.0;
        uv = clamp(uv, 0.0, 1.0);
    }
    if (uEnd1.x > 0.0) {
        uv = clamp(mix(uv, vec2(0.5), uEnd1.x * 0.22 * r2), 0.0, 1.0);
    }

    float shift = (rand(vec2(uTime*0.6, uv.y*40.0)) - 0.5) * 0.004 * uVhsStrength;
    shift += uEnd0.z;
    float r = texture(uScene, uv + vec2(shift, 0.0)).r;
    float g = texture(uScene, uv).g;
    float b = texture(uScene, uv - vec2(shift, 0.0)).b;
    vec3 col = vec3(r,g,b);

    col += texture(uBloom, uv).rgb * (uBloomStrength + uEnd1.y);
    col = tonemap(col * uExposure * uEnd1.z);

    if (uEnd0.x > 0.0) {
        float luma = dot(col, vec3(0.299, 0.587, 0.114));
        col = mix(col, vec3(luma) * vec3(1.02, 1.00, 0.96), uEnd0.x);
    }

    float scan = sin(uv.y*uResolution.y*1.4 + uTime*6.0) * 0.04 * uVhsStrength;
    col -= scan;
    float grain = (rand(uv*uResolution + uTime) - 0.5) * 0.05 * uVhsStrength;
    col += grain;

    vec2 vig = uv - 0.5;
    float vigAmt = 1.0 - dot(vig,vig)*1.1;
    vigAmt *= 1.0 - uEnd0.y * smoothstep(0.02, 0.34, dot(vig, vig));
    col *= clamp(vigAmt, 0.0, 1.0);
    col *= (0.55 + 0.45*uFlicker);

    if (uFlashOn > 0.5) {
        col *= 1.02;
    }

    if (uMadness > 0.001) {
        vec2 warp = vec2(sin(uv.y * 24.0 + uTime * 2.7), cos(uv.x * 19.0 + uTime * 3.3)) * 0.012 * uMadness;
        vec3 smear = texture(uScene, clamp(uv + warp, 0.0, 1.0)).rgb;
        col = mix(col, smear, 0.6 * uMadness);
        float luma = dot(col, vec3(0.299, 0.587, 0.114));
        col = mix(col, vec3(luma) * vec3(1.25, 0.62, 0.62), 0.45 * uMadness);
        col *= 0.80 + 0.20 * sin(uTime * 5.1);
    }

    if (uColorBlindMix > 0.001) {
        float luma = dot(col, vec3(0.299, 0.587, 0.114));
        vec3 shifted = mix(vec3(luma), col, 0.7) + uColorBlindAxis * (luma * 0.35);
        col = mix(col, shifted, uColorBlindMix);
    }

    fragColor = vec4(col, 1.0);
}
"""


data class RenderSettings(
    val quality        : String  = "high",
    val vhsEnabled     : Boolean = true,
    val fogEnabled     : Boolean = true,
    val shadowsEnabled : Boolean = true,
    val resolutionScale: Float   = 1f,
    val colorBlindMode : String  = "none"
)


private val LAMP_ALBEDO = floatArrayOf(0.957f, 0.941f, 0.886f)


private fun floorDivInt(a: Int, b: Int): Int {
    val q = a / b
    return if (a % b != 0 && (a < 0) != (b < 0)) q - 1 else q
}

class OmniGLRenderer(private val appContext: Context) : GLSurfaceView.Renderer {

    @Volatile var latestState: GameState = GameState()
    @Volatile var renderSettings: RenderSettings = RenderSettings()

    @Volatile var cameraView: String = "first"


    private var avatarCollapse = 0f


    private var charProgram = 0
    private var charVbo = 0; private var charIbo = 0; private var charIndexCount = 0
    private var charTex = 0
    private var shaftProgram = 0
    private var sMVP = 0; private var sFlicker = 0; private var sTint = 0
    private var cBones = 0

    private val charPose = PoseBuilder()
    private var cMVP = 0; private var cModel = 0
    private var cTexU = 0; private var cIsChar = 0
    private var cAnimate = 0
    private var cSubject = 0


    private var torchProgram = 0
    private var torchVbo = 0; private var torchIbo = 0; private var torchIndexCount = 0
    private var tMVP = 0; private var tModel = 0; private var tOn = 0; private var tAmbient = 0



    private var decalProgram = 0
    private var dMVP = 0; private var dTint = 0; private var dMark = 0; private var dTime = 0
    private var decalVbo = 0

    private val decalVerts = FloatArray(TRAIL_CAPACITY * 4 * 9)
    private var decalQuads = 0
    private val decalBuf = ByteBuffer.allocateDirect(decalVerts.size * 4)
        .order(ByteOrder.nativeOrder()).asFloatBuffer()
    private var decalIbo = 0
    private var decalTint = floatArrayOf(0.72f, 0.66f, 0.50f)
    private var decalMark = 0f
    private var decalScale = 0.30f
    private var decalSpread = 1.9f


    @Volatile var trailSource: (() -> FloatArray?)? = null


    fun setTrailStyle(spec: FloatArray?) {
        if (spec == null || spec.size < 7) return
        decalTint = floatArrayOf(spec[0], spec[1], spec[2])
        decalScale = spec[4]
        decalSpread = spec[5]
        decalMark = spec[6]
    }
    private val torchModelM = FloatArray(16)
    private val torchMvpM = FloatArray(16)

    private var torchRaise = 0f

    private var avatarCrouch = 0f
    private var avatarAir = 0f

    private var headYaw = 0f
    private var lastBodyYaw = 0f
    private val avatarModelM = FloatArray(16)
    private val avatarMvpM = FloatArray(16)
    private var lastAvatarX = 0f
    private var lastAvatarZ = 0f

    private var avatarSpeed = 0f



    @Volatile var measuredFps: Float = 0f
        private set
    private var fpsAccum = 0f

    private var sceneProgram = 0; private var billboardProgram = 0; private var postProgram = 0; private var shadowProgram = 0
    private var uMVP = 0; private var uFlatAlbedo = 0; private var uCamPos = 0
    private var uFogDensity = 0
    private var uFogColor = 0; private var uFlicker = 0
    private var uBumpStrength = 0; private var uLampTint = 0
    private var uTorchPos = 0; private var uTorchDir = 0; private var uTorchOn = 0

    private var uSceneTime = 0
    private var bVP = 0; private var bCenter = 0; private var bRight = 0; private var bUp = 0
    private var bSize = 0; private var bColor = 0; private var bAlert = 0; private var bAlpha = 0; private var bColorBlind = 0
    private var bTime = 0; private var bSeed = 0; private var bDissolve = 0
    private var pScene = 0; private var pTime = 0; private var pFlicker = 0; private var pVhs = 0; private var pRes = 0
    private var pCbMix = 0; private var pCbAxis = 0; private var pFlashOn = 0; private var pMadness = 0
    private var pBloomTex = 0; private var pBloomStrength = 0; private var pExposure = 0
    private var pEnd0 = 0; private var pEnd1 = 0



    @Volatile var endingProvider: (() -> FloatArray)? = null




    @Volatile var avatarYawDegrees = 0f


    private val endingParams = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)
    private var brightProgram = 0; private var blurProgram = 0
    private var brScene = 0; private var brThreshold = 0; private var brKnee = 0
    private var blSource = 0; private var blDir = 0

    private var bloomFbo = IntArray(2); private var bloomTex = IntArray(2)
    private var bloomW = 1; private var bloomH = 1
    private var sVP = 0; private var sCenter = 0; private var sSize = 0; private var sAlpha = 0
    private var sTime = 0; private var sSeed = 0
    private var exitProgram = 0
    private var xVP = 0; private var xCenter = 0; private var xRight = 0
    private var xWidth = 0; private var xHeight = 0; private var xTime = 0; private var xNear = 0











    private val chunkMeshes = HashMap<Long, ChunkMesh>()




    private val chunkRadius = 3



    private val kChunkRetryFrames = 20
    @Volatile var chunkProvider: ((Int, Int) -> WorldChunk?)? = null

    private var billboardVbo = 0
    private var postVbo = 0


    private var fbo = 0; private var fboTex = 0; private var fboDepth = 0
    private var surfaceW = 1; private var surfaceH = 1
    private var renderW = 1; private var renderH = 1
    private var lastResScale = -1f



    private var fboUsable = true
    private var bloomUsable = true



    private val chunkMisses = HashMap<Long, Int>()
    private var frameCounter = 0



    private var everDrewLevel = false
    private var reportedEmpty = false


    private val AVATAR_SCALE = 1.7f

    private val projM = FloatArray(16)
    private val viewM = FloatArray(16)
    private val vpM   = FloatArray(16)
    private val rollM = FloatArray(16)
    private val rolledViewM = FloatArray(16)
    private val startNanos = System.nanoTime()
    private var lastFrameNanos = 0L




    private var smoothX = 0f; private var smoothY = 1.7f; private var smoothZ = 0f
    private var smoothYaw = 0f; private var smoothPitch = 0f
    private var smoothTilt = 0f


    private var smoothCamDist = 0f
    private var smoothInit = false
    private val smoothEntities = HashMap<Int, FloatArray>()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {







        chunkMeshes.clear()
        charIndexCount = 0
        torchIndexCount = 0
        torchRaise = 0f; avatarCrouch = 0f; avatarAir = 0f; headYaw = 0f
        smoothInit = false
        smoothTilt = 0f
        avatarSpeed = 0f
        smoothEntities.clear()



        fbo = 0; fboTex = 0; fboDepth = 0
        bloomFbo = IntArray(2); bloomTex = IntArray(2)
        renderW = 1; renderH = 1; lastResScale = -1f
        fboUsable = true; bloomUsable = true
        chunkMisses.clear(); frameCounter = 0
        everDrewLevel = false; reportedEmpty = false

        GLES30.glClearColor(0.02f, 0.02f, 0.017f, 1f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)





        OmniLog.i("GL", "vendor=${GLES30.glGetString(GLES30.GL_VENDOR)} " +
                        "renderer=${GLES30.glGetString(GLES30.GL_RENDERER)} " +
                        "version=${GLES30.glGetString(GLES30.GL_VERSION)} " +
                        "glsl=${GLES30.glGetString(GLES30.GL_SHADING_LANGUAGE_VERSION)}")

        sceneProgram = linkGlProgram(OMNI_SCENE_VERT, OMNI_SCENE_FRAG, "scene")
        uMVP = GLES30.glGetUniformLocation(sceneProgram, "uMVP")
        uFlatAlbedo = GLES30.glGetUniformLocation(sceneProgram, "uFlatAlbedo")
        uCamPos = GLES30.glGetUniformLocation(sceneProgram, "uCamPos")
        uFogDensity = GLES30.glGetUniformLocation(sceneProgram, "uFogDensity")
        uFogColor = GLES30.glGetUniformLocation(sceneProgram, "uFogColor")
        uFlicker = GLES30.glGetUniformLocation(sceneProgram, "uFlicker")
        uBumpStrength = GLES30.glGetUniformLocation(sceneProgram, "uBumpStrength")
        uLampTint = GLES30.glGetUniformLocation(sceneProgram, "uLampTint")
        uTorchPos = GLES30.glGetUniformLocation(sceneProgram, "uTorchPos")
        uTorchDir = GLES30.glGetUniformLocation(sceneProgram, "uTorchDir")
        uTorchOn = GLES30.glGetUniformLocation(sceneProgram, "uTorchOn")
        uSceneTime = GLES30.glGetUniformLocation(sceneProgram, "uTime")

        billboardProgram = linkGlProgram(OMNI_BILLBOARD_VERT, OMNI_BILLBOARD_FRAG, "billboard")
        bVP = GLES30.glGetUniformLocation(billboardProgram, "uVP")
        bCenter = GLES30.glGetUniformLocation(billboardProgram, "uCenter")
        bRight = GLES30.glGetUniformLocation(billboardProgram, "uCamRight")
        bUp = GLES30.glGetUniformLocation(billboardProgram, "uCamUp")
        bSize = GLES30.glGetUniformLocation(billboardProgram, "uSize")
        bColor = GLES30.glGetUniformLocation(billboardProgram, "uColor")
        bAlert = GLES30.glGetUniformLocation(billboardProgram, "uAlert")
        bAlpha = GLES30.glGetUniformLocation(billboardProgram, "uAlpha")
        bColorBlind = GLES30.glGetUniformLocation(billboardProgram, "uColorBlind")
        bTime = GLES30.glGetUniformLocation(billboardProgram, "uTime")
        bSeed = GLES30.glGetUniformLocation(billboardProgram, "uSeed")
        bDissolve = GLES30.glGetUniformLocation(billboardProgram, "uDissolve")

        postProgram = linkGlProgram(OMNI_POST_VERT, OMNI_POST_FRAG, "post")
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
        pEnd0 = GLES30.glGetUniformLocation(postProgram, "uEnd0")
        pEnd1 = GLES30.glGetUniformLocation(postProgram, "uEnd1")

        brightProgram = linkGlProgram(OMNI_POST_VERT, OMNI_BRIGHT_FRAG, "bright")
        brScene = GLES30.glGetUniformLocation(brightProgram, "uScene")
        brThreshold = GLES30.glGetUniformLocation(brightProgram, "uThreshold")
        brKnee = GLES30.glGetUniformLocation(brightProgram, "uKnee")

        blurProgram = linkGlProgram(OMNI_POST_VERT, OMNI_BLUR_FRAG, "blur")
        blSource = GLES30.glGetUniformLocation(blurProgram, "uSource")
        blDir = GLES30.glGetUniformLocation(blurProgram, "uDir")

        exitProgram = linkGlProgram(OMNI_EXIT_VERT, OMNI_EXIT_FRAG, "exit")
        xVP = GLES30.glGetUniformLocation(exitProgram, "uVP")
        xCenter = GLES30.glGetUniformLocation(exitProgram, "uCenter")
        xRight = GLES30.glGetUniformLocation(exitProgram, "uRight")
        xWidth = GLES30.glGetUniformLocation(exitProgram, "uWidth")
        xHeight = GLES30.glGetUniformLocation(exitProgram, "uHeight")
        xTime = GLES30.glGetUniformLocation(exitProgram, "uTime")
        xNear = GLES30.glGetUniformLocation(exitProgram, "uNear")

        shaftProgram = linkGlProgram(OMNI_SHAFT_VERT, OMNI_SHAFT_FRAG, "shaft")
        sMVP = GLES30.glGetUniformLocation(shaftProgram, "uMVP")
        sFlicker = GLES30.glGetUniformLocation(shaftProgram, "uFlicker")
        sTint = GLES30.glGetUniformLocation(shaftProgram, "uTint")

        shadowProgram = linkGlProgram(OMNI_SHADOW_VERT, OMNI_SHADOW_FRAG, "shadow")
        sVP = GLES30.glGetUniformLocation(shadowProgram, "uVP")
        sCenter = GLES30.glGetUniformLocation(shadowProgram, "uCenter")
        sSize = GLES30.glGetUniformLocation(shadowProgram, "uSize")
        sAlpha = GLES30.glGetUniformLocation(shadowProgram, "uAlpha")
        sTime = GLES30.glGetUniformLocation(shadowProgram, "uTime")
        sSeed = GLES30.glGetUniformLocation(shadowProgram, "uSeed")



        runCatching {
            charProgram = linkGlProgram(OMNI_PREVIEW_VERT, OMNI_PREVIEW_FRAG, "preview")
            cMVP = GLES30.glGetUniformLocation(charProgram, "uMVP")
            cModel = GLES30.glGetUniformLocation(charProgram, "uModel")
            cTexU = GLES30.glGetUniformLocation(charProgram, "uTex")
            cIsChar = GLES30.glGetUniformLocation(charProgram, "uIsCharacter")
            cAnimate = GLES30.glGetUniformLocation(charProgram, "uAnimate")
            cSubject = GLES30.glGetUniformLocation(charProgram, "uSubject")
            cBones = GLES30.glGetUniformLocation(charProgram, "uBones")

            torchProgram = linkGlProgram(OMNI_TORCH_VERT, OMNI_TORCH_FRAG, "torch")
            tMVP = GLES30.glGetUniformLocation(torchProgram, "uMVP")
            tModel = GLES30.glGetUniformLocation(torchProgram, "uModel")
            tOn = GLES30.glGetUniformLocation(torchProgram, "uOn")
            tAmbient = GLES30.glGetUniformLocation(torchProgram, "uAmbient")
            decalProgram = linkGlProgram(OMNI_DECAL_VERT, OMNI_DECAL_FRAG, "decal")
            dMVP  = GLES30.glGetUniformLocation(decalProgram, "uMVP")
            dTint = GLES30.glGetUniformLocation(decalProgram, "uTint")
            dMark = GLES30.glGetUniformLocation(decalProgram, "uMark")
            dTime = GLES30.glGetUniformLocation(decalProgram, "uTime")
            decalVbo = genGlBuffer()

            decalIbo = genGlBuffer()
            val di = IntArray(TRAIL_CAPACITY * 6)
            for (q in 0 until TRAIL_CAPACITY) {
                val b = q * 4
                di[q * 6] = b; di[q * 6 + 1] = b + 1; di[q * 6 + 2] = b + 2
                di[q * 6 + 3] = b; di[q * 6 + 4] = b + 2; di[q * 6 + 5] = b + 3
            }
            val dib = ByteBuffer.allocateDirect(di.size * 4).order(ByteOrder.nativeOrder()).asIntBuffer()
            dib.put(di); dib.position(0)
            GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, decalIbo)
            GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, di.size * 4, dib, GLES30.GL_STATIC_DRAW)

            val (tv, ti) = buildTorchMesh()
            torchVbo = genGlBuffer(); torchIbo = genGlBuffer()
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, torchVbo)
            GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, tv.size * 4, glFloatBuffer(tv), GLES30.GL_STATIC_DRAW)
            GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, torchIbo)
            GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, ti.size * 4, glIntBuffer(ti), GLES30.GL_STATIC_DRAW)
            torchIndexCount = ti.size

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












        Matrix.perspectiveM(projM, 0, 52f, surfaceW.toFloat()/surfaceH.toFloat(), 0.05f, 110f)
        lastResScale = -1f
    }

    override fun onDrawFrame(gl: GL10?) {
        val state = latestState
        val cam = state.camera
        val rs = renderSettings
        val nowNanos = System.nanoTime()
        val timeSec = (nowNanos - startNanos) / 1_000_000_000f
        val dt = if (lastFrameNanos == 0L) 1f / 60f else ((nowNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0.001f, 0.1f)
        lastFrameNanos = nowNanos

        val instantFps = 1f / dt
        fpsAccum = if (fpsAccum == 0f) instantFps else fpsAccum + (instantFps - fpsAccum) * 0.08f
        measuredFps = fpsAccum



        val fogMult      = when (rs.quality) { "low" -> 1.35f; "high" -> 0.85f; else -> 1.0f }
        val entityRange  = when (rs.quality) { "low" -> 25f; "high" -> 45f; else -> 35f }
        val shadowsOn    = rs.shadowsEnabled && rs.quality != "low"





        val postStrength = when (rs.quality) { "low" -> 0.30f; "high" -> 0.50f; else -> 0.42f }
        val resScale     = rs.resolutionScale.coerceIn(0.5f, 1f)
        val cbAxis = colorBlindAxis(rs.colorBlindMode)
        val cbMix  = if (rs.colorBlindMode == "none") 0f else 0.55f

        if (resScale != lastResScale || renderW <= 1) {
            renderW = max((surfaceW * resScale).toInt(), 1)
            renderH = max((surfaceH * resScale).toInt(), 1)
            rebuildFbo(renderW, renderH)
            lastResScale = resScale
        }

        frameCounter++
        val world = state.world
        if (world.isValid && cam != null) streamChunks(world, cam.posX, cam.posZ)






        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, if (fboUsable) fbo else 0)
        GLES30.glViewport(0, 0,
            if (fboUsable) renderW else surfaceW,
            if (fboUsable) renderH else surfaceH)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        if (cam != null) {



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

            val eyeY = smoothY + state.eyeOffset




            val thirdPerson = cameraView == "third" && charIndexCount > 0
            val ceiling = if (state.world.isValid) state.world.height else 2.6f






















            val spawnFall = state.spawnPhase == SpawnPhase.FALLING
            val arrive = if (state.spawnPhase == SpawnPhase.LANDED)
                (state.eyeOffset / -1.45f).coerceIn(0f, 1f) else 0f







            val pivotY = if (thirdPerson) (eyeY - 0.45f) else eyeY
            val camLift = if (thirdPerson)
                (0.12f + (if (spawnFall) 0.90f else 0f) + arrive * 0.34f) else 0f
            val wantDist = if (thirdPerson)
                (2.6f + (if (spawnFall) 1.5f else 0f) - arrive * 1.10f) else 0f



            val boomRate = if (spawnFall || arrive > 0.001f) 3.5f else 14f
            val camDist = if (thirdPerson) {
                smoothCamDist += (resolveCameraDistance(
                    smoothX, pivotY + camLift, smoothZ,
                    -fx, -fy, -fz, wantDist, state.world, ceiling
                ) - smoothCamDist) * (1f - kotlin.math.exp(-dt * boomRate))
                smoothCamDist
            } else {
                smoothCamDist = 0f
                0f
            }
            val eyeX = smoothX - fx * camDist
            val eyeZ = smoothZ - fz * camDist


            val camY = (pivotY - fy * camDist + camLift).coerceIn(0.30f, ceiling - 0.30f)
            Matrix.setLookAtM(
                viewM, 0,
                eyeX, camY, eyeZ,
                smoothX + fx, eyeY + fy, smoothZ + fz,
                0f, 1f, 0f
            )










            val wantTilt = if (thirdPerson) 0f else state.cameraTilt
            smoothTilt += (wantTilt - smoothTilt) * chase
            if (kotlin.math.abs(smoothTilt) > 0.01f) {
                Matrix.setIdentityM(rollM, 0)
                Matrix.rotateM(rollM, 0, smoothTilt, 0f, 0f, 1f)
                Matrix.multiplyMM(rolledViewM, 0, rollM, 0, viewM, 0)
                System.arraycopy(rolledViewM, 0, viewM, 0, 16)
            }
            Matrix.multiplyMM(vpM, 0, projM, 0, viewM, 0)










            if (!thirdPerson) {













                val rx = -kotlin.math.cos(Math.toRadians(smoothYaw.toDouble()).toFloat())
                val rz = kotlin.math.sin(Math.toRadians(smoothYaw.toDouble()).toFloat())
                torchLightPos[0] = eyeX + fx * 0.28f + rx * 0.20f
                torchLightPos[1] = camY + fy * 0.28f - 0.18f
                torchLightPos[2] = eyeZ + fz * 0.28f + rz * 0.20f
                torchLightDir[0] = fx; torchLightDir[1] = fy; torchLightDir[2] = fz
                torchLightOn = if (state.flashlightOn) 1f else 0f
            }

            val fogDensity = (if (rs.fogEnabled) 1.0f else 0.15f) * fogMult
            val flicker = state.flickerIntensity.coerceIn(0.55f, 1f)
            val bump = when (rs.quality) { "low" -> 0f; "high" -> 1.6f; else -> 0.9f }
            drawLevel(vpM, eyeX, camY, eyeZ, fogDensity, flicker, bump, timeSec, world)




            val stepX = (cam.posX - lastAvatarX); val stepZ = (cam.posZ - lastAvatarZ)
            lastAvatarX = cam.posX; lastAvatarZ = cam.posZ
            val instantSpeed = kotlin.math.hypot(stepX, stepZ) / dt
            avatarSpeed += (instantSpeed - avatarSpeed) * (1f - kotlin.math.exp(-dt * 9f))



            if (thirdPerson) {



                val feetY = smoothY - (cam.eyeHeight + state.eyeOffset)
                val ease = 1f - kotlin.math.exp(-dt * 9f)


                avatarCrouch += ((if (state.isCrouching) 1f else 0f) - avatarCrouch) * ease



                val airborne = if (feetY > 0.10f) 1f else 0f
                avatarAir += (airborne - avatarAir) * ease
                torchRaise += ((if (state.flashlightOn) 1f else 0f) - torchRaise) * (1f - kotlin.math.exp(-dt * 7f))




                var yawDelta = cam.yaw - lastBodyYaw
                while (yawDelta > 180f) yawDelta -= 360f
                while (yawDelta < -180f) yawDelta += 360f
                lastBodyYaw = cam.yaw
                val targetHead = (yawDelta * 0.09f).coerceIn(-0.62f, 0.62f)
                headYaw += (targetHead - headYaw) * (1f - kotlin.math.exp(-dt * 5f))
                val headPitch = (-Math.toRadians(smoothPitch.toDouble()).toFloat() * 0.45f)
                    .coerceIn(-0.40f, 0.40f)

                val walkBlend = (avatarSpeed / 3.6f).coerceIn(0f, 1.6f)










                val collapseTarget = when {
                    state.isMadnessOver || state.isGameOver -> 1f
                    state.spawnPhase == SpawnPhase.LANDED ->
                        (state.eyeOffset / -1.45f).coerceIn(0f, 1f)
                    else -> 0f
                }



                avatarCollapse =
                    if (state.spawnPhase == SpawnPhase.LANDED) collapseTarget
                    else avatarCollapse + (collapseTarget - avatarCollapse) * (1f - kotlin.math.exp(-dt * 3.2f))

                avatarYawDegrees = smoothYaw
                drawAvatar(
                    vpM, smoothX, feetY, smoothZ, smoothYaw,
                    timeSec, walkBlend,
                    avatarCrouch, avatarAir, headYaw, headPitch, torchRaise,
                    avatarCollapse
                )
                updateTorchLight(
                    smoothX, feetY, smoothZ, smoothYaw,
                    timeSec, walkBlend, torchRaise, avatarCrouch, state.flashlightOn
                )
                drawTorch(vpM, torchRaise, state.flashlightOn)
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

            if (shadowsOn) drawShadows(vpM, state.entities, smoothX, smoothZ, entityRange, timeSec)



            drawEntities(vpM, state.entities, yawRad.toFloat(), smoothX, smoothZ, entityRange, timeSec, cbMix)


            if (state.world.isValid) {
                drawExitDoor(vpM, state.exitX, state.exitZ, smoothX, smoothZ, state.world.height, timeSec)
            }
        }


        val sceneLuma = if (probeThisFrame()) probeLuma("scene") else null




        if (!fboUsable) {
            if (sceneLuma != null) reportFrame(state, rs, resScale, sceneLuma, sceneLuma)
            GLES30.glDisable(GLES30.GL_DEPTH_TEST)
            return
        }



        val bloomPasses = when {
            !bloomUsable    -> 0
            else -> when (rs.quality) { "low" -> 0; "high" -> 3; else -> 2 }
        }
        if (bloomPasses > 0) renderBloom(bloomPasses)

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, surfaceW, surfaceH)
        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)



        val endKind = when {
            state.isEscaped                       -> 2
            state.isGameOver || state.isMadnessOver -> 1
            else                                  -> 0
        }
        val src = if (endKind == 0) null else endingProvider?.invoke()
        if (src != null && src.size >= 8) {
            src.copyInto(endingParams, 0, 0, 8)
        } else {


            endingParams[0] = 0f; endingParams[1] = 0f; endingParams[2] = 0f
            endingParams[3] = 0f; endingParams[4] = 0f; endingParams[5] = 0f
            endingParams[6] = 1f; endingParams[7] = 0f
        }

        drawPost(
            timeSec, state.flickerIntensity, (if (rs.vhsEnabled) 1f else 0f) * postStrength,
            cbMix, cbAxis, state.flashlightOn, state.madness,
            bloomStrength = if (bloomPasses > 0) 0.85f else 0f,
            ending = endingParams
        )



        if (sceneLuma != null) {
            reportFrame(state, rs, resScale, sceneLuma, probeLuma("screen"))
        }
    }



























    private val probeFrames = intArrayOf(120, 300, 600)
    private val probeBuf =
        ByteBuffer.allocateDirect(kProbeSide * kProbeSide * 4).order(ByteOrder.nativeOrder())

    private fun probeThisFrame(): Boolean = probeFrames.any { it == frameCounter }



    private fun probeLuma(what: String): FloatArray {
        val w = if (what == "scene") renderW else surfaceW
        val h = if (what == "scene") renderH else surfaceH
        val x = ((w - kProbeSide) / 2).coerceAtLeast(0)
        val y = ((h - kProbeSide) / 2).coerceAtLeast(0)
        probeBuf.position(0)
        GLES30.glReadPixels(x, y, kProbeSide, kProbeSide,
                            GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, probeBuf)
        probeBuf.position(0)
        var lo = 1f; var hi = 0f; var sum = 0f
        val n = kProbeSide * kProbeSide
        for (i in 0 until n) {
            val r = (probeBuf.get().toInt() and 0xFF) / 255f
            val g = (probeBuf.get().toInt() and 0xFF) / 255f
            val b = (probeBuf.get().toInt() and 0xFF) / 255f
            probeBuf.get()
            val luma = 0.2126f * r + 0.7152f * g + 0.0722f * b
            if (luma < lo) lo = luma
            if (luma > hi) hi = luma
            sum += luma
        }
        probeBuf.position(0)
        return floatArrayOf(lo, sum / n, hi)
    }

    private fun reportFrame(
        state: GameState, rs: RenderSettings, resScale: Float,
        scene: FloatArray, screen: FloatArray
    ) {
        var tris = 0
        for (m in chunkMeshes.values) tris += (m.floorCount + m.roofCount + m.wallCount) / 3
        val cam = state.camera
        val verdict = when {
            screen[2] > 0.02f                 -> "picture on screen"
            scene[2] > 0.02f                  -> "SCENE HAS IMAGE BUT SCREEN IS BLACK — the composite is losing it"
            tris == 0                         -> "NO GEOMETRY — nothing was submitted"
            else                              -> "GEOMETRY DRAWN BUT SCENE IS BLACK — unlit, or the camera is not on it"
        }
        OmniLog.i("GL",
            "frame $frameCounter: $verdict | " +
            "scene luma min=${"%.4f".format(scene[0])} mean=${"%.4f".format(scene[1])} " +
            "max=${"%.4f".format(scene[2])} | " +
            "screen luma min=${"%.4f".format(screen[0])} mean=${"%.4f".format(screen[1])} " +
            "max=${"%.4f".format(screen[2])} | " +
            "chunks=${chunkMeshes.size} tris=$tris misses=${chunkMisses.size} | " +
            "target=${renderW}x$renderH surface=${surfaceW}x$surfaceH scale=$resScale | " +
            "fbo=$fboUsable bloom=$bloomUsable quality=${rs.quality} " +
            "fog=${rs.fogEnabled} vhs=${rs.vhsEnabled} | " +
            "cam=" + (if (cam == null) "NULL" else
                "(${"%.1f".format(cam.posX)},${"%.1f".format(cam.posY)}," +
                "${"%.1f".format(cam.posZ)}) yaw=${"%.0f".format(cam.yaw)} " +
                "pitch=${"%.0f".format(cam.pitch)}") +
            " torch=${state.flashlightOn} flicker=${"%.2f".format(state.flickerIntensity)}")
    }




    private fun drawExitDoor(
        vp: FloatArray, exitX: Float, exitZ: Float,
        camX: Float, camZ: Float, ceiling: Float, timeSec: Float
    ) {
        val dx = exitX - camX; val dz = exitZ - camZ
        val dist = kotlin.math.hypot(dx, dz)

        if (dist > 48f) return

        GLES30.glUseProgram(exitProgram)
        GLES30.glUniformMatrix4fv(xVP, 1, false, vp, 0)
        GLES30.glUniform3f(xCenter, exitX, 0.03f, exitZ)

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

    private fun drawLevel(
        vp: FloatArray, camX: Float, camY: Float, camZ: Float,
        fogDensity: Float, flicker: Float, bumpStrength: Float,


        timeSec: Float,

        world: WorldInfo
    ) {
        GLES30.glUseProgram(sceneProgram)
        GLES30.glUniformMatrix4fv(uMVP, 1, false, vp, 0)
        GLES30.glUniform3f(uCamPos, camX, camY, camZ)
        GLES30.glUniform1f(uFogDensity, fogDensity)



        GLES30.glUniform3f(uFogColor, 0.16f, 0.145f, 0.085f)
        GLES30.glUniform1f(uFlicker, flicker)
        GLES30.glUniform3f(uLampTint, 1.0f, 0.94f, 0.66f)
        GLES30.glUniform3f(uTorchPos, torchLightPos[0], torchLightPos[1], torchLightPos[2])
        GLES30.glUniform3f(uTorchDir, torchLightDir[0], torchLightDir[1], torchLightDir[2])
        GLES30.glUniform1f(uTorchOn, torchLightOn)
        GLES30.glUniform1f(uSceneTime, timeSec)


        GLES30.glUniform1f(uBumpStrength, bumpStrength)


        for (m in chunkMeshes.values) drawMeshGroup(m.floorVbo, m.floorIbo, m.floorCount)
        for (m in chunkMeshes.values) drawMeshGroup(m.roofVbo,  m.roofIbo,  m.roofCount)
        for (m in chunkMeshes.values) drawMeshGroup(m.wallVbo,  m.wallIbo,  m.wallCount)














        var levelIndices = 0
        for (m in chunkMeshes.values) levelIndices += m.floorCount + m.roofCount + m.wallCount
        if (levelIndices > 0) {
            if (!everDrewLevel) {
                everDrewLevel = true
                OmniLog.i("GL", "level drawn: ${chunkMeshes.size} chunk(s) resident, " +
                                "${levelIndices / 3} triangles, ${chunkMisses.size} awaiting retry")
            }
        } else if (!everDrewLevel && !reportedEmpty && frameCounter > 180) {
            reportedEmpty = true
            OmniLog.w("GL", "no level geometry after $frameCounter frames: " +
                            "${chunkMeshes.size} chunk(s) resident, " +
                            "${chunkMisses.size} unanswered, world=${world.isValid}, " +
                            "provider=${if (chunkProvider != null) "set" else "NULL"}")
        }





        for (m in chunkMeshes.values) drawMeshGroup(m.fixVbo, m.fixIbo, m.fixCount, LAMP_ALBEDO)




        drawTrailDecals(vp, timeSec) { wx, wz -> lightAtWorld(wx, wz, world) }



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

    private fun drawMeshGroup(vbo: Int, ibo: Int, indexCount: Int, flat: FloatArray? = null) {
        if (indexCount <= 0) return
        if (flat == null) GLES30.glUniform3f(uFlatAlbedo, -1f, -1f, -1f)
        else GLES30.glUniform3f(uFlatAlbedo, flat[0], flat[1], flat[2])
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
        GLES30.glUniform1f(bTime, timeSec)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, billboardVbo)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 0, 0)
        val rangeSq = range * range
        for (e in entities) {



            if (!e.isActive || e.isAway) continue
            val sp = smoothEntities[e.id] ?: floatArrayOf(e.posX, e.posY, e.posZ)
            val dx = sp[0] - camX; val dz = sp[2] - camZ
            if (dx * dx + dz * dz > rangeSq) continue


            val phase = e.id * 1.7f
            val bob   = sin(timeSec * 2.3f + phase) * 0.06f
            val pulse = 1.0f + sin(timeSec * 3.1f + phase) * 0.05f
            GLES30.glUniform3f(bCenter, sp[0], sp[1] + 1.0f + bob, sp[2])
            GLES30.glUniform1f(bSize, 1.8f * pulse)
            val tint = smilerTint
            GLES30.glUniform3f(bColor, tint.first, tint.second, tint.third)
            GLES30.glUniform1f(bAlert, (e.alertLevel + (if (e.aiState >= 3) 0.5f else 0f)).coerceIn(0f, 1f))
            GLES30.glUniform1f(bAlpha, if (e.playerInSight) 1f else 0.82f)


            GLES30.glUniform1f(bSeed, (e.id * 0.618f) % 1f)
            GLES30.glUniform1f(bDissolve, e.dissolve)
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        }
        GLES30.glDisableVertexAttribArray(0)
    }


    private fun drawShadows(vp: FloatArray, entities: List<EntityState>, camX: Float, camZ: Float, range: Float, timeSec: Float) {
        if (entities.isEmpty()) return
        GLES30.glUseProgram(shadowProgram)
        GLES30.glUniformMatrix4fv(sVP, 1, false, vp, 0)
        GLES30.glUniform1f(sTime, timeSec)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, billboardVbo)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 0, 0)
        val rangeSq = range * range
        for (e in entities) {
            if (!e.isActive || e.isAway) continue
            val sp = smoothEntities[e.id] ?: floatArrayOf(e.posX, e.posY, e.posZ)
            val dx = sp[0] - camX; val dz = sp[2] - camZ
            val d2 = dx * dx + dz * dz
            if (d2 > rangeSq) continue


            val fade = (1f - (d2 / rangeSq)) * (1f - e.dissolve)
            GLES30.glUniform3f(sCenter, sp[0], 0f, sp[2])
            GLES30.glUniform1f(sSize, 0.95f)
            GLES30.glUniform1f(sAlpha, 0.55f * fade)
            GLES30.glUniform1f(sSeed, (e.id * 0.618f) % 1f)
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        }
        GLES30.glDisableVertexAttribArray(0)
    }

    private fun drawPost(
        timeSec: Float, flicker: Float, vhsStrength: Float,
        cbMix: Float, cbAxis: Triple<Float, Float, Float>,
        flashOn: Boolean, madness: Float, bloomStrength: Float,
        ending: FloatArray
    ) {
        GLES30.glUseProgram(postProgram)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, fboTex)
        GLES30.glUniform1i(pScene, 0)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, bloomTex[0])
        GLES30.glUniform1i(pBloomTex, 1)
        GLES30.glUniform1f(pBloomStrength, bloomStrength)


        GLES30.glUniform1f(pExposure, 1.18f)
        GLES30.glUniform1f(pTime, timeSec)
        GLES30.glUniform1f(pFlicker, flicker.coerceIn(0.3f, 1f))
        GLES30.glUniform1f(pVhs, vhsStrength)
        GLES30.glUniform2f(pRes, surfaceW.toFloat(), surfaceH.toFloat())
        GLES30.glUniform1f(pCbMix, cbMix)
        GLES30.glUniform3f(pCbAxis, cbAxis.first, cbAxis.second, cbAxis.third)
        GLES30.glUniform1f(pFlashOn, if (flashOn) 1f else 0f)
        GLES30.glUniform1f(pMadness, madness.coerceIn(0f, 1f))
        GLES30.glUniform4f(pEnd0, ending[0], ending[1], ending[2], ending[3])
        GLES30.glUniform4f(pEnd1, ending[4], ending[5], ending[6], ending[7])
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, postVbo)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 0, 0)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glDisableVertexAttribArray(0)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
    }




    private fun colorBlindAxis(mode: String): Triple<Float, Float, Float> = when (mode) {
        "protanopia", "deuteranopia" -> Triple(0.15f, -0.05f, 0.35f)
        "tritanopia" -> Triple(0.20f, 0.25f, -0.25f)
        else -> Triple(0f, 0f, 0f)
    }




    private val smilerTint = Triple(0.90f, 0.90f, 0.85f)













    private fun buildTorchMesh(): Pair<FloatArray, IntArray> {

        val profile = arrayOf(
            floatArrayOf(-0.070f, 0.000f, 0f),
            floatArrayOf(-0.070f, 0.020f, 0f),
            floatArrayOf(-0.030f, 0.023f, 0f),
            floatArrayOf( 0.020f, 0.022f, 0f),
            floatArrayOf( 0.052f, 0.024f, 0f),
            floatArrayOf( 0.058f, 0.034f, 1f),
            floatArrayOf( 0.086f, 0.041f, 1f),
            floatArrayOf( 0.092f, 0.040f, 2f),
            floatArrayOf( 0.093f, 0.036f, 2f)
        )
        val sides = 10
        val verts = ArrayList<Float>()
        val idx = ArrayList<Int>()

        for (r in profile.indices) {
            val z = profile[r][0]; val rad = profile[r][1]; val part = profile[r][2]

            val prev = profile[max(r - 1, 0)]
            val next = profile[min(r + 1, profile.lastIndex)]
            val dz = next[0] - prev[0]
            val dr = next[1] - prev[1]
            val len = kotlin.math.hypot(dz, dr).coerceAtLeast(1e-5f)
            val nRad = dz / len
            val nAxial = -dr / len
            for (s in 0 until sides) {
                val a = (s / sides.toFloat()) * (Math.PI * 2).toFloat()
                val ca = cos(a); val sa = sin(a)
                verts.add(ca * rad); verts.add(sa * rad); verts.add(z)
                verts.add(ca * nRad); verts.add(sa * nRad); verts.add(nAxial)
                verts.add(s / sides.toFloat()); verts.add(r / (profile.size - 1f))
                verts.add(part)
            }
        }
        for (r in 0 until profile.size - 1) {
            for (s in 0 until sides) {
                val s2 = (s + 1) % sides
                val a = r * sides + s
                val b = r * sides + s2
                val c = (r + 1) * sides + s2
                val d = (r + 1) * sides + s
                idx.add(a); idx.add(b); idx.add(c)
                idx.add(a); idx.add(c); idx.add(d)
            }
        }

        val lensBase = verts.size / 9
        val lensZ = profile.last()[0]
        val lensR = profile.last()[1]
        verts.add(0f); verts.add(0f); verts.add(lensZ)
        verts.add(0f); verts.add(0f); verts.add(1f)
        verts.add(0.5f); verts.add(1f); verts.add(2f)
        for (s in 0 until sides) {
            val a = (s / sides.toFloat()) * (Math.PI * 2).toFloat()
            verts.add(cos(a) * lensR); verts.add(sin(a) * lensR); verts.add(lensZ)
            verts.add(0f); verts.add(0f); verts.add(1f)
            verts.add(s / sides.toFloat()); verts.add(1f); verts.add(2f)
        }
        for (s in 0 until sides) {
            idx.add(lensBase); idx.add(lensBase + 1 + s); idx.add(lensBase + 1 + (s + 1) % sides)
        }
        return FloatArray(verts.size) { verts[it] } to IntArray(idx.size) { idx[it] }
    }





    private val torchLightPos = floatArrayOf(0f, 0f, 0f)
    private val torchLightDir = floatArrayOf(0f, 0f, 1f)
    private var torchLightOn = 0f




    private fun updateTorchLight(
        px: Float, py: Float, pz: Float, yawDeg: Float,
        timeSec: Float, walk: Float, torch: Float, crouch: Float, on: Boolean
    ) {
        buildTorchMatrix(px, py, pz, yawDeg, timeSec, walk, torch, crouch)



        val m = torchModelM
        val tipLocal = 0.093f
        torchLightPos[0] = m[8] * tipLocal + m[12]
        torchLightPos[1] = m[9] * tipLocal + m[13]
        torchLightPos[2] = m[10] * tipLocal + m[14]
        val dx = m[8]; val dy = m[9]; val dz = m[10]
        val len = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(1e-6f)
        torchLightDir[0] = dx / len; torchLightDir[1] = dy / len; torchLightDir[2] = dz / len
        torchLightOn = if (on) torch.coerceIn(0f, 1f) else 0f
    }

    private fun buildTorchMatrix(
        px: Float, py: Float, pz: Float, yawDeg: Float,
        timeSec: Float, walk: Float, torch: Float, crouch: Float
    ) {

        val gait = walk.coerceIn(0f, 1.6f)
        val run = ((gait - 1f).coerceIn(0f, 0.6f)) / 0.6f
        val stride = timeSec * 6.4f
        val shoulderX = 0.11f; val shoulderY = 0.74f
        val phase = stride + Math.PI.toFloat()
        val idleSway = sin(timeSec * 0.9f + 1f) * 0.055f
        val swung = sin(phase) * (0.40f + 0.30f * run) * gait + idleSway
        val shoulderPitch = swung + (-1.24f - swung) * torch
        val shoulderRoll = -0.34f * torch
        val elbowPitch = (sin(phase - 0.85f) * 0.30f * gait + 0.10f) +
            (-0.52f - (sin(phase - 0.85f) * 0.30f * gait + 0.10f)) * torch



        val upperLen = 0.14f
        val foreLen = 0.16f

        var hx = 0f; var hy = -upperLen; var hz = 0f

        var ry = hy * cos(shoulderPitch) - hz * sin(shoulderPitch)
        var rz = hy * sin(shoulderPitch) + hz * cos(shoulderPitch)
        hy = ry; hz = rz

        var rx = hx * cos(shoulderRoll) - hy * sin(shoulderRoll)
        ry = hx * sin(shoulderRoll) + hy * cos(shoulderRoll)
        hx = rx; hy = ry
        val elbowX = shoulderX + hx; val elbowY = shoulderY + hy; val elbowZ = hz

        val totalPitch = shoulderPitch + elbowPitch
        var fx2 = 0f; var fy2 = -foreLen; var fz2 = 0f
        ry = fy2 * cos(totalPitch) - fz2 * sin(totalPitch)
        rz = fy2 * sin(totalPitch) + fz2 * cos(totalPitch)
        fy2 = ry; fz2 = rz
        rx = fx2 * cos(shoulderRoll) - fy2 * sin(shoulderRoll)
        ry = fx2 * sin(shoulderRoll) + fy2 * cos(shoulderRoll)
        fx2 = rx; fy2 = ry
        var handX = elbowX + fx2
        var handY = elbowY + fy2
        var handZ = elbowZ + fz2

        handY -= 0.38f * crouch

        Matrix.setIdentityM(torchModelM, 0)
        Matrix.translateM(torchModelM, 0, px, py, pz)
        Matrix.rotateM(torchModelM, 0, yawDeg, 0f, 1f, 0f)
        Matrix.scaleM(torchModelM, 0, AVATAR_SCALE, AVATAR_SCALE, AVATAR_SCALE)
        Matrix.translateM(torchModelM, 0, handX, handY, handZ)


        Matrix.rotateM(torchModelM, 0, -78f + 78f * torch, 1f, 0f, 0f)
    }

    private fun drawTorch(
        vp: FloatArray, torch: Float, on: Boolean
    ) {
        if (torchIndexCount <= 0 || torch <= 0.01f) return
        GLES30.glUseProgram(torchProgram)
        Matrix.multiplyMM(torchMvpM, 0, vp, 0, torchModelM, 0)

        GLES30.glUniformMatrix4fv(tMVP, 1, false, torchMvpM, 0)
        GLES30.glUniformMatrix4fv(tModel, 1, false, torchModelM, 0)
        GLES30.glUniform1f(tOn, if (on) 1f else 0f)
        GLES30.glUniform3f(tAmbient, 0.18f, 0.17f, 0.13f)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, torchVbo)
        val stride2 = 9 * 4
        GLES30.glEnableVertexAttribArray(0); GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride2, 0)
        GLES30.glEnableVertexAttribArray(1); GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride2, 3 * 4)
        GLES30.glEnableVertexAttribArray(2); GLES30.glVertexAttribPointer(2, 2, GLES30.GL_FLOAT, false, stride2, 6 * 4)
        GLES30.glEnableVertexAttribArray(3); GLES30.glVertexAttribPointer(3, 1, GLES30.GL_FLOAT, false, stride2, 8 * 4)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, torchIbo)
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, torchIndexCount, GLES30.GL_UNSIGNED_INT, 0)
        GLES30.glDisableVertexAttribArray(0); GLES30.glDisableVertexAttribArray(1)
        GLES30.glDisableVertexAttribArray(2); GLES30.glDisableVertexAttribArray(3)
    }




    private fun drawTrailDecals(vp: FloatArray, timeSec: Float, lightAt: (Float, Float) -> Float) {
        if (decalProgram == 0) return
        val flat = runCatching { trailSource?.invoke() }.getOrNull() ?: return
        val n = flat.size / 5
        if (n <= 0) return

        var v = 0
        var quads = 0
        for (i in 0 until minOf(n, TRAIL_CAPACITY)) {
            val sx = flat[i * 5]
            val sz = flat[i * 5 + 1]
            val yaw = flat[i * 5 + 2]
            val age = flat[i * 5 + 3]
            if (age >= 1f) continue

            val half = decalScale * 0.5f * (1f + (decalSpread - 1f) * age)

            val fx = cos(yaw); val fz = -sin(yaw)
            val rx = -fz;      val rz = fx
            val lit = lightAt(sx, sz)


            val y = 0.012f


            val cx = floatArrayOf(-1f, 1f, 1f, -1f)
            val cz2 = floatArrayOf(-1f, -1f, 1f, 1f)
            for (k in 0 until 4) {
                val ox = (rx * cx[k] + fx * cz2[k]) * half
                val oz = (rz * cx[k] + fz * cz2[k]) * half
                decalVerts[v++] = sx + ox; decalVerts[v++] = y; decalVerts[v++] = sz + oz

                decalVerts[v++] = lit; decalVerts[v++] = 1f; decalVerts[v++] = 0f
                decalVerts[v++] = cx[k]; decalVerts[v++] = cz2[k]
                decalVerts[v++] = age
            }
            quads++
        }
        decalQuads = quads
        if (quads == 0) return

        decalBuf.position(0); decalBuf.put(decalVerts, 0, v); decalBuf.position(0)
        GLES30.glUseProgram(decalProgram)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, decalVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, v * 4, decalBuf, GLES30.GL_DYNAMIC_DRAW)

        GLES30.glUniformMatrix4fv(dMVP, 1, false, vp, 0)
        GLES30.glUniform3f(dTint, decalTint[0], decalTint[1], decalTint[2])
        GLES30.glUniform1f(dMark, decalMark)
        GLES30.glUniform1f(dTime, timeSec)

        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        GLES30.glDepthMask(false)

        val stride = 9 * 4
        GLES30.glEnableVertexAttribArray(0); GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
        GLES30.glEnableVertexAttribArray(1); GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride, 3 * 4)
        GLES30.glEnableVertexAttribArray(2); GLES30.glVertexAttribPointer(2, 2, GLES30.GL_FLOAT, false, stride, 6 * 4)
        GLES30.glEnableVertexAttribArray(3); GLES30.glVertexAttribPointer(3, 1, GLES30.GL_FLOAT, false, stride, 8 * 4)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, decalIbo)
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, quads * 6, GLES30.GL_UNSIGNED_INT, 0)
        GLES30.glDisableVertexAttribArray(0); GLES30.glDisableVertexAttribArray(1)
        GLES30.glDisableVertexAttribArray(2); GLES30.glDisableVertexAttribArray(3)

        GLES30.glDepthMask(true)
        GLES30.glDisable(GLES30.GL_BLEND)
    }


    private fun drawAvatar(
        vp: FloatArray, px: Float, py: Float, pz: Float, yawDeg: Float,
        timeSec: Float, walk: Float,
        crouch: Float, air: Float, headYawRad: Float, headPitchRad: Float, torch: Float,
        collapse: Float
    ) {
        if (charIndexCount <= 0) return
        GLES30.glUseProgram(charProgram)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, charTex)
        GLES30.glUniform1i(cTexU, 0)



        GLES30.glUniform1f(cIsChar, 1f)
        GLES30.glUniform1f(cAnimate, 1f)


        GLES30.glUniform3f(cSubject, px, py, pz)

        Matrix.setIdentityM(avatarModelM, 0)
        Matrix.translateM(avatarModelM, 0, px, py, pz)

        Matrix.rotateM(avatarModelM, 0, yawDeg, 0f, 1f, 0f)
        Matrix.scaleM(avatarModelM, 0, AVATAR_SCALE, AVATAR_SCALE, AVATAR_SCALE)
        Matrix.multiplyMM(avatarMvpM, 0, vp, 0, avatarModelM, 0)

        GLES30.glUniformMatrix4fv(cMVP, 1, false, avatarMvpM, 0)
        GLES30.glUniformMatrix4fv(cModel, 1, false, avatarModelM, 0)
        charPose.build(timeSec, walk, crouch, air, headYawRad, headPitchRad, torch, collapse)
        GLES30.glUniformMatrix4fv(cBones, Skeleton.BONES, false, charPose.matrices, 0)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, charVbo)
        val stride = CharacterMesh.FLOATS_PER_VERTEX * 4
        GLES30.glEnableVertexAttribArray(0); GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
        GLES30.glEnableVertexAttribArray(1); GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride, 3 * 4)
        GLES30.glEnableVertexAttribArray(2); GLES30.glVertexAttribPointer(2, 2, GLES30.GL_FLOAT, false, stride, 6 * 4)

        GLES30.glEnableVertexAttribArray(3); GLES30.glVertexAttribPointer(3, 4, GLES30.GL_FLOAT, false, stride, 8 * 4)
        GLES30.glEnableVertexAttribArray(4); GLES30.glVertexAttribPointer(4, 4, GLES30.GL_FLOAT, false, stride, 12 * 4)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, charIbo)
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, charIndexCount, GLES30.GL_UNSIGNED_SHORT, 0)
        for (a in 0..4) GLES30.glDisableVertexAttribArray(a)
    }

    private fun streamChunks(world: WorldInfo, camX: Float, camZ: Float) {
        val provider = chunkProvider ?: return
        val chunkSpan = world.chunkCells * world.cellSize
        val pcx = kotlin.math.floor(camX / chunkSpan).toInt()
        val pcz = kotlin.math.floor(camZ / chunkSpan).toInt()


        val stale = chunkMeshes.keys.filter { key ->
            val cx = (key shr 32).toInt()
            val cz = key.toInt()
            kotlin.math.abs(cx - pcx) > chunkRadius || kotlin.math.abs(cz - pcz) > chunkRadius
        }
        for (key in stale) {
            chunkMeshes.remove(key)?.release()
            chunkMisses.remove(key)
        }


        var bestKey = 0L; var bestDist = Int.MAX_VALUE; var bestX = 0; var bestZ = 0
        for (dz in -chunkRadius..chunkRadius) {
            for (dx in -chunkRadius..chunkRadius) {
                val cx = pcx + dx; val cz = pcz + dz
                val key = (cx.toLong() shl 32) or (cz.toLong() and 0xFFFFFFFFL)
                if (chunkMeshes.containsKey(key)) continue

                val retryAt = chunkMisses[key]
                if (retryAt != null && frameCounter < retryAt) continue
                val d = dx * dx + dz * dz
                if (d < bestDist) { bestDist = d; bestKey = key; bestX = cx; bestZ = cz }
            }
        }
        if (bestDist == Int.MAX_VALUE) return









        val chunk = provider(bestX, bestZ)
        val mesh = if (chunk != null) buildChunkMesh(chunk, world) else null
        if (mesh == null) {
            chunkMisses[bestKey] = frameCounter + kChunkRetryFrames
            return
        }
        chunkMisses.remove(bestKey)
        chunkMeshes[bestKey] = mesh
    }

    private fun buildChunkMesh(chunk: WorldChunk, world: WorldInfo): ChunkMesh? {
        val floorV = ArrayList<Float>(); val floorI = ArrayList<Int>(); var floorB = 0
        val wallV  = ArrayList<Float>(); val wallI  = ArrayList<Int>(); var wallB  = 0
        val roofV  = ArrayList<Float>(); val roofI  = ArrayList<Int>(); var roofB  = 0
        val fixV   = ArrayList<Float>(); val fixI   = ArrayList<Int>(); var fixB   = 0
        val shaftV = ArrayList<Float>(); val shaftI = ArrayList<Int>(); var shaftB = 0


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




        fun quadUv(
            verts: ArrayList<Float>, idx: ArrayList<Int>, base: Int,
            p0: FloatArray, p1: FloatArray, p2: FloatArray, p3: FloatArray,
            n: FloatArray, l0: Float, l1: Float, l2: Float, l3: Float,
            uv: FloatArray
        ): Int {
            val pts = arrayOf(p0, p1, p2, p3)
            val lights = floatArrayOf(l0, l1, l2, l3)
            for (k in 0 until 4) {
                verts.add(pts[k][0]); verts.add(pts[k][1]); verts.add(pts[k][2])
                verts.add(n[0]); verts.add(n[1]); verts.add(n[2])
                verts.add(uv[k * 2]); verts.add(uv[k * 2 + 1])
                verts.add(lights[k])
            }
            idx.add(base); idx.add(base + 1); idx.add(base + 2)
            idx.add(base); idx.add(base + 2); idx.add(base + 3)
            return base + 4
        }


        fun quadFlat(
            verts: ArrayList<Float>, idx: ArrayList<Int>, base: Int,
            p0: FloatArray, p1: FloatArray, p2: FloatArray, p3: FloatArray,
            n: FloatArray, light: Float, u0: Float, v0: Float, u1: Float, v1: Float
        ): Int = quad(verts, idx, base, p0, p1, p2, p3, n, light, light, light, light, u0, v0, u1, v1)




        fun cornerLight(cx: Int, cz: Int): Float {
            var sum = 0f; var count = 0
            var solidSum = 0f
            for (dz in -1..0) for (dx in -1..0) {
                val ax = cx + dx; val az = cz + dz
                val l = chunk.lightAt(ax, az)
                if (chunk.solidAt(ax, az)) { solidSum += l; continue }
                sum += l; count++
            }


            return if (count == 0) solidSum * 0.25f else sum / count
        }

        val cs = world.cellSize
        val hgt = world.height



        val originX = chunk.chunkX * chunk.cells * cs
        val originZ = chunk.chunkZ * chunk.cells * cs

        for (lz in 0 until chunk.cells) {
            for (lx in 0 until chunk.cells) {
                if (chunk.solidAt(lx, lz)) continue

                val x0 = originX + lx * cs; val x1 = x0 + cs
                val z0 = originZ + lz * cs; val z1 = z0 + cs
                val lit = chunk.lightAt(lx, lz)
                val feature = chunk.featureAt(lx, lz)



                val c00 = cornerLight(lx, lz)
                val c10 = cornerLight(lx + 1, lz)
                val c11 = cornerLight(lx + 1, lz + 1)
                val c01 = cornerLight(lx, lz + 1)

                val u0 = x0; val u1 = x1
                val v0 = z0; val v1 = z1
                val wallV0 = 0f; val wallV1 = hgt
























                val floorDim = 1f
                floorB = quad(
                    floorV, floorI, floorB,
                    floatArrayOf(x0, 0f, z0), floatArrayOf(x1, 0f, z0),
                    floatArrayOf(x1, 0f, z1), floatArrayOf(x0, 0f, z1),
                    floatArrayOf(0f, 1f, 0f),
                    c00 * floorDim, c10 * floorDim, c11 * floorDim, c01 * floorDim,
                    u0, v0, u1, v1
                )


                roofB = quadUv(
                    roofV, roofI, roofB,
                    floatArrayOf(x0, hgt, z0), floatArrayOf(x0, hgt, z1),
                    floatArrayOf(x1, hgt, z1), floatArrayOf(x1, hgt, z0),
                    floatArrayOf(0f, -1f, 0f),
                    c00 * 1.12f, c01 * 1.12f, c11 * 1.12f, c10 * 1.12f,



                    floatArrayOf(u0, v0,  u0, v1,  u1, v1,  u1, v0)
                )



                val wallTop = 1.0f
                val wallBot = 0.62f



                fun wallFace(
                    ax: Float, az: Float, bx: Float, bz: Float,
                    n: FloatArray, lA: Float, lB: Float, uA: Float, uB: Float
                ) {
                    val skirtH = 0.13f
                    val skirtV = skirtH
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
                        uA, skirtV, uB, hgt)
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












                val fixture = chunk.fixtureAt(lx, lz)
                if (fixture == 1) {








                    val midX = x0 + cs * 0.5f
                    val midZ = z0 + cs * 0.5f
                    val top = hgt - 0.09f
                    val halfLen = cs * 0.34f
                    val topHalf = cs * 0.16f
                    val botHalf = cs * 0.54f
                    val botLen  = halfLen + cs * 0.20f
                    val intensity = lit.coerceAtMost(1.5f)
                    val floorY = 0.02f




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


























                    val midX = kotlin.math.round((x0 + cs * 0.5f) / kCeilTileM) * kCeilTileM
                    val midZ = kotlin.math.round((z0 + cs * 0.5f) / kCeilTileM) * kCeilTileM
                    val halfL = 0.610f
                    val panHalfW = 0.290f
                    val mouthHalfW = 0.305f
                    val lit = fixture == 1
                    val down = floatArrayOf(0f, -1f, 0f)





                    val panY = hgt - 0.04f
                    val mouthY = hgt - 0.105f
                    val tubeY = hgt - 0.070f



                    val jitter = ((lx * 73 + lz * 151) % 17) / 17f
                    val emit = if (lit) 3.0f + jitter * 0.5f else 0.14f
                    val panLight = if (lit) 0.60f else 0.09f
                    val reflectorLight = if (lit) 1.45f else 0.12f


                    fixB = quadFlat(
                        fixV, fixI, fixB,
                        floatArrayOf(midX - halfL, panY, midZ - panHalfW),
                        floatArrayOf(midX - halfL, panY, midZ + panHalfW),
                        floatArrayOf(midX + halfL, panY, midZ + panHalfW),
                        floatArrayOf(midX + halfL, panY, midZ - panHalfW),
                        down, panLight, 0f, 0f, 1f, 1f
                    )




                    for (side in -1..1 step 2) {
                        val s = side.toFloat()
                        fixB = quadFlat(
                            fixV, fixI, fixB,
                            floatArrayOf(midX - halfL, panY, midZ + s * panHalfW),
                            floatArrayOf(midX + halfL, panY, midZ + s * panHalfW),
                            floatArrayOf(midX + halfL, mouthY, midZ + s * mouthHalfW),
                            floatArrayOf(midX - halfL, mouthY, midZ + s * mouthHalfW),
                            down, reflectorLight, 0f, 0f, 1f, 1f
                        )
                    }


                    for (side in -1..1 step 2) {
                        val s = side.toFloat()
                        fixB = quadFlat(
                            fixV, fixI, fixB,
                            floatArrayOf(midX + s * halfL, panY, midZ - panHalfW),
                            floatArrayOf(midX + s * halfL, panY, midZ + panHalfW),
                            floatArrayOf(midX + s * halfL, mouthY, midZ + mouthHalfW),
                            floatArrayOf(midX + s * halfL, mouthY, midZ - mouthHalfW),
                            down, panLight * 0.75f, 0f, 0f, 1f, 1f
                        )
                    }





                    val tubes = 4



                    val tubeHalfD = 0.013f
                    val tubeDrop = 0.016f
                    for (t in 0 until tubes) {
                        val f = (t + 0.5f) / tubes
                        val tz = midZ + (f - 0.5f) * (panHalfW * 1.72f)

                        fixB = quadFlat(
                            fixV, fixI, fixB,
                            floatArrayOf(midX - halfL * 0.90f, tubeY - tubeDrop, tz - tubeHalfD),
                            floatArrayOf(midX - halfL * 0.90f, tubeY - tubeDrop, tz + tubeHalfD),
                            floatArrayOf(midX + halfL * 0.90f, tubeY - tubeDrop, tz + tubeHalfD),
                            floatArrayOf(midX + halfL * 0.90f, tubeY - tubeDrop, tz - tubeHalfD),
                            down, emit, 0f, 0f, 1f, 1f
                        )

                        for (side in -1..1 step 2) {
                            val s = side.toFloat()
                            fixB = quadFlat(
                                fixV, fixI, fixB,
                                floatArrayOf(midX - halfL * 0.90f, tubeY - tubeDrop, tz + s * tubeHalfD),
                                floatArrayOf(midX + halfL * 0.90f, tubeY - tubeDrop, tz + s * tubeHalfD),
                                floatArrayOf(midX + halfL * 0.90f, tubeY, tz + s * tubeHalfD * 1.35f),
                                floatArrayOf(midX - halfL * 0.90f, tubeY, tz + s * tubeHalfD * 1.35f),
                                down, emit * 0.72f, 0f, 0f, 1f, 1f
                            )
                        }


                        for (side in -1..1 step 2) {
                            val s = side.toFloat()
                            fixB = quadFlat(
                                fixV, fixI, fixB,
                                floatArrayOf(midX + s * halfL * 0.90f, tubeY - tubeDrop, tz - tubeHalfD),
                                floatArrayOf(midX + s * halfL * 0.98f, tubeY - tubeDrop, tz - tubeHalfD),
                                floatArrayOf(midX + s * halfL * 0.98f, tubeY - tubeDrop, tz + tubeHalfD),
                                floatArrayOf(midX + s * halfL * 0.90f, tubeY - tubeDrop, tz + tubeHalfD),
                                down, 0.08f, 0f, 0f, 1f, 1f
                            )
                        }
                    }




                    if (lit) {
                        fixB = quadFlat(
                            fixV, fixI, fixB,
                            floatArrayOf(midX - halfL * 0.96f, mouthY, midZ - mouthHalfW * 0.94f),
                            floatArrayOf(midX - halfL * 0.96f, mouthY, midZ + mouthHalfW * 0.94f),
                            floatArrayOf(midX + halfL * 0.96f, mouthY, midZ + mouthHalfW * 0.94f),
                            floatArrayOf(midX + halfL * 0.96f, mouthY, midZ - mouthHalfW * 0.94f),
                            down, 1.15f, 0f, 0f, 1f, 1f
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
        mesh.source = chunk
        return mesh
    }






    private fun lightAtWorld(wx: Float, wz: Float, world: WorldInfo): Float {
        if (!world.isValid) return 1f
        val cs = world.cellSize
        val cellsPerChunk = world.chunkCells
        val cx = kotlin.math.floor(wx / cs).toInt()
        val cz = kotlin.math.floor(wz / cs).toInt()
        val chx = floorDivInt(cx, cellsPerChunk)
        val chz = floorDivInt(cz, cellsPerChunk)
        val key = (chx.toLong() shl 32) or (chz.toLong() and 0xFFFFFFFFL)
        val chunk = chunkMeshes[key]?.source ?: return 1f
        return chunk.lightAt(cx - chx * cellsPerChunk, cz - chz * cellsPerChunk)
    }

    private fun isSolidWorld(wx: Float, wz: Float, world: WorldInfo): Boolean {
        if (!world.isValid) return false
        val cs = world.cellSize
        val cellsPerChunk = world.chunkCells
        val cx = kotlin.math.floor(wx / cs).toInt()
        val cz = kotlin.math.floor(wz / cs).toInt()
        val chx = floorDivInt(cx, cellsPerChunk)
        val chz = floorDivInt(cz, cellsPerChunk)
        val key = (chx.toLong() shl 32) or (chz.toLong() and 0xFFFFFFFFL)
        val chunk = chunkMeshes[key]?.source ?: return true
        return chunk.solidAt(cx - chx * cellsPerChunk, cz - chz * cellsPerChunk)
    }




    private fun resolveCameraDistance(
        px: Float, py: Float, pz: Float,
        bx: Float, by: Float, bz: Float,
        dist: Float, world: WorldInfo, ceiling: Float
    ): Float {
        if (dist <= 0f) return 0f

        val pad = 0.30f
        val steps = 12
        var safe = dist
        for (i in 1..steps) {
            val t = dist * i / steps
            val sx = px + bx * t
            val sy = py + by * t
            val sz = pz + bz * t
            val blocked = sy < pad || sy > ceiling - pad ||
                isSolidWorld(sx, sz, world) ||


                isSolidWorld(sx + pad, sz, world) || isSolidWorld(sx - pad, sz, world) ||
                isSolidWorld(sx, sz + pad, world) || isSolidWorld(sx, sz - pad, world)
            if (blocked) {
                safe = dist * (i - 1) / steps
                break
            }
        }
        return safe.coerceAtLeast(0f)
    }


    private class ChunkMesh {


        var source: WorldChunk? = null

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






        GLES30.glRenderbufferStorage(GLES30.GL_RENDERBUFFER, GLES30.GL_DEPTH_COMPONENT24, w, h)

        fbo = genGlFramebuffer()
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbo)
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, fboTex, 0)
        GLES30.glFramebufferRenderbuffer(GLES30.GL_FRAMEBUFFER, GLES30.GL_DEPTH_ATTACHMENT, GLES30.GL_RENDERBUFFER, fboDepth)





        fboUsable = checkFramebuffer("scene", w, h)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)

        rebuildBloomTargets(w, h)
    }


    private fun checkFramebuffer(what: String, w: Int, h: Int): Boolean {
        val status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
        if (status == GLES30.GL_FRAMEBUFFER_COMPLETE) return true
        val name = when (status) {
            GLES30.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT -> "INCOMPLETE_ATTACHMENT"
            GLES30.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT -> "MISSING_ATTACHMENT"
            GLES30.GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE -> "INCOMPLETE_MULTISAMPLE"
            GLES30.GL_FRAMEBUFFER_UNSUPPORTED -> "UNSUPPORTED"
            else -> "0x" + Integer.toHexString(status)
        }
        OmniLog.e("GL", "$what framebuffer incomplete at ${w}x$h: $name — " +
                        "falling back so the world still draws")
        return false
    }



    private fun rebuildBloomTargets(w: Int, h: Int) {
        bloomUsable = true
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



            if (!checkFramebuffer("bloom", bloomW, bloomH)) bloomUsable = false
        }
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
    }




    private fun renderBloom(passes: Int) {
        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        GLES30.glDisable(GLES30.GL_BLEND)
        GLES30.glViewport(0, 0, bloomW, bloomH)

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, bloomFbo[0])
        GLES30.glUseProgram(brightProgram)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, fboTex)
        GLES30.glUniform1i(brScene, 0)


        GLES30.glUniform1f(brThreshold, 0.62f)
        GLES30.glUniform1f(brKnee, 0.28f)
        drawFullscreenQuad()

        GLES30.glUseProgram(blurProgram)
        var src = 0
        for (i in 0 until passes) {

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




    private val TEXEL_DENSITY = 320f




    private fun loadOmniTexture(assetPath: String, fallbackColor: Int, uvOut: FloatArray? = null): Int {
        val bmp: Bitmap = try {
            appContext.assets.open(assetPath).use { BitmapFactory.decodeStream(it) } ?: proceduralTile(fallbackColor)
        } catch (t: Throwable) {
            proceduralTile(fallbackColor)
        }
        if (uvOut != null) {
            uvOut[0] = TEXEL_DENSITY / bmp.width.coerceAtLeast(1)
            uvOut[1] = TEXEL_DENSITY / bmp.height.coerceAtLeast(1)
        }
        return uploadTexture(bmp)
    }


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


    LaunchedEffect(renderer) {
        renderer.chunkProvider = vm::fetchChunk
        renderer.endingProvider = { vm.endingSnapshot }
        vm.avatarYawSource = { renderer.avatarYawDegrees }
    }






    val equippedTrail by vm.equippedTrail.collectAsState()
    LaunchedEffect(renderer, equippedTrail) {
        renderer.trailSource = vm::collectTrail
        renderer.setTrailStyle(vm.trailStyleSpec())
    }
    LaunchedEffect(state) { renderer.latestState = state }


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



            glView.onPause()
            vm.onScreenPaused()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { glView }, modifier = Modifier.fillMaxSize())







        if (settingsState.vhsEnabled) CrtScanlineOverlay(0f)
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


    var inspectTrail by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(s.successMsg) { if (s.successMsg != null) { delay(2000); vm.clearSuccess() } }



    if (inspecting) {
        val trail = inspectTrail
        if (trail != null) TrailPreviewSheet(
            trailId    = trail,
            isOwned    = "trail_$trail" in s.ownedIds,
            isEquipped = s.equippedTrail == trail,
            onEquip    = { vm.equipTrail(trail) },
            onClose    = { inspecting = false; inspectTrail = null }
        )
        else CharacterPreviewSheet(onClose = { inspecting = false })
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
                                    onInspect = {
                                        inspectTrail = item.id.takeIf { id -> id.startsWith("trail_") }
                                            ?.removePrefix("trail_")
                                        inspecting = true
                                    }
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
                                        onInspect = {
                                            inspectTrail = item.id.takeIf { id -> id.startsWith("trail_") }
                                                ?.removePrefix("trail_")
                                            inspecting = true
                                        }
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
                                        onInspect = {
                                            inspectTrail = item.id.takeIf { id -> id.startsWith("trail_") }
                                                ?.removePrefix("trail_")
                                            inspecting = true
                                        }
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

                        XpBar(progress = profile.xpProgress, xp = profile.xp, xpToNext = profile.xpToNext)
                    }
                }

                DividerLine()


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


@Composable
private fun FramedAvatar(frame: String, localUri: String?, size: Dp, onClick: () -> Unit) {
    Box(Modifier.size(size).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val r = this.size.minDimension * 0.36f
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
        }




















        val frameClock = rememberFrameClock()
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            drawFrame3D(frame, this.size.minDimension * 0.42f, frameClock)
        }


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
                modifier = Modifier.fillMaxSize(0.62f).clip(CircleShape)
            )
        }
    }
}


private const val FRAME3D_MAJOR = 40
private const val FRAME3D_MINOR = 8


private class TorusVertex(val x: Float, val y: Float, val z: Float,
                          val nx: Float, val ny: Float, val nz: Float,


                          val u: Float)


private object FrameCatalog {


    const val SAMPLES = FRAME3D_MAJOR

    class Entry(
        val id: String,
        val base: Color,
        val glow: Color,
        val highlight: Color,

        val tubeRatio: Float,
        val shininess: Float,
        val geometry: Array<TorusVertex>,

        val nativeIndex: Int
    )

    private val bridge by lazy { runCatching { NativeBridge() }.getOrNull() }

    val entries: List<Entry> by lazy { load() }

    private fun load(): List<Entry> {
        val b = bridge
        val n = runCatching { b?.frameCount() ?: 0 }.getOrDefault(0)
        if (b == null || n <= 0) return listOf(fallback())
        return (0 until n).mapNotNull { i -> runCatching { entryAt(b, i) }.getOrNull() }
            .ifEmpty { listOf(fallback()) }
    }

    private fun entryAt(b: NativeBridge, i: Int): Entry {
        val id = b.frameId(i).orEmpty().ifEmpty { "frame_$i" }
        val s = b.frameSpec(i) ?: error("no spec for frame $i")
        val prof = b.frameProfile(i, SAMPLES) ?: error("no profile for frame $i")
        var tubeSum = 0f
        for (k in 0 until SAMPLES) tubeSum += prof[k * 2 + 1]
        return Entry(
            id = id,
            base      = Color(s[0], s[1], s[2], 1f),
            glow      = Color(s[3], s[4], s[5], 1f),
            highlight = Color(s[6], s[7], s[8], 1f),
            tubeRatio = tubeSum / SAMPLES,
            shininess = s[10],
            geometry  = sweep(prof),
            nativeIndex = i
        )
    }


    private fun fallback(): Entry {
        val prof = FloatArray(SAMPLES * 2)
        for (i in 0 until SAMPLES) { prof[i * 2] = 1f; prof[i * 2 + 1] = 0.16f }
        return Entry(
            "Face_Of_Darkness",
            Color(0.18f, 0.17f, 0.20f, 1f),
            Color(0.90f, 0.35f, 0.28f, 1f),
            Color(0.80f, 0.78f, 0.82f, 1f),
            0.16f, 30f, sweep(prof), -1
        )
    }




    private fun sweep(profile: FloatArray): Array<TorusVertex> {
        val out = ArrayList<TorusVertex>(SAMPLES * FRAME3D_MINOR)
        for (i in 0 until SAMPLES) {
            val u = i / SAMPLES.toFloat()
            val a = u * 2f * Math.PI.toFloat()
            val ca = cos(a); val sa = sin(a)
            val pathR = profile[i * 2]
            val minor = profile[i * 2 + 1]
            for (j in 0 until FRAME3D_MINOR) {
                val b = (j / FRAME3D_MINOR.toFloat()) * 2f * Math.PI.toFloat()
                val cb = cos(b); val sb = sin(b)
                val ringR = pathR + minor * cb
                out.add(
                    TorusVertex(ringR * ca, ringR * sa, minor * sb, cb * ca, cb * sa, sb, u)
                )
            }
        }
        return out.toTypedArray()
    }

    fun indexOf(id: String): Int =
        entries.indexOfFirst { it.id == id }.takeIf { it >= 0 } ?: 0

    fun entryFor(id: String): Entry = entries[indexOf(id)]


    fun ids(): List<String> = entries.map { it.id }




    private val scratch = ThreadLocal.withInitial { FloatArray(SAMPLES) }

    fun emission(entry: Entry, t: Float): FloatArray {
        val buf = scratch.get()!!
        if (entry.nativeIndex < 0) { java.util.Arrays.fill(buf, 0f); return buf }
        val got = runCatching { bridge?.frameEmission(entry.nativeIndex, SAMPLES, t) }.getOrNull()
        if (got == null || got.size < SAMPLES) { java.util.Arrays.fill(buf, 0f); return buf }
        System.arraycopy(got, 0, buf, 0, SAMPLES)
        return buf
    }
}


private fun DrawScope.drawFrame3D(
    frame: String,
    radius: Float,
    t: Float
) {
    val style = FrameCatalog.entryFor(frame)
    val geometry = style.geometry
    val emission = FrameCatalog.emission(style, t)














    val tilt = 0f
    val yaw  = 0f
    val spin = t * 0.30f

    val cosT = cos(tilt); val sinT = sin(tilt)
    val cosY = cos(yaw);  val sinY = sin(yaw)
    val cosS = cos(spin); val sinS = sin(spin)



    val eyeZ = radius * 5.2f

    val n = geometry.size
    val projX = FloatArray(n); val projY = FloatArray(n); val viewZ = FloatArray(n)
    val litR = FloatArray(n); val litG = FloatArray(n); val litB = FloatArray(n)



    val lx = -0.42f; val ly = -0.66f; val lz = 0.62f
    val ll = kotlin.math.sqrt(lx * lx + ly * ly + lz * lz)
    val lxn = lx / ll; val lyn = ly / ll; val lzn = lz / ll

    val hx = lxn; val hy = lyn; val hz = lzn + 1f
    val hl = kotlin.math.sqrt(hx * hx + hy * hy + hz * hz)
    val hxn = hx / hl; val hyn = hy / hl; val hzn = hz / hl

    for (i in 0 until n) {
        val v = geometry[i]


        val x = (v.x * cosS - v.y * sinS) * radius
        val y = (v.x * sinS + v.y * cosS) * radius
        val z = v.z * radius

        val y1 = y * cosT - z * sinT
        val z1 = y * sinT + z * cosT
        val x2 = x * cosY + z1 * sinY
        val z2 = -x * sinY + z1 * cosY


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


        val rim = (1f - kotlin.math.abs(nz)).let { it * it } * 0.55f


        val emissive = emission[(v.u * FrameCatalog.SAMPLES).toInt()
            .coerceIn(0, FrameCatalog.SAMPLES - 1)]

        val ambient = 0.22f
        val kd = ambient + diffuse * 0.78f
        litR[i] = style.base.red * kd + style.highlight.red * spec + style.glow.red * emissive + rim * 0.30f
        litG[i] = style.base.green * kd + style.highlight.green * spec + style.glow.green * emissive + rim * 0.32f
        litB[i] = style.base.blue * kd + style.highlight.blue * spec + style.glow.blue * emissive + rim * 0.40f
    }



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












    var glowSum = 0f
    for (e in emission) glowSum += e
    val glowAmount = glowSum / FrameCatalog.SAMPLES
    drawCircle(
        Brush.radialGradient(
            listOf(Color.Transparent, style.glow.copy(0.22f + glowAmount * 0.14f), Color.Transparent),
            center = center, radius = radius * 1.42f
        ),
        radius = radius * 1.42f, center = center
    )
    drawCircle(
        Brush.radialGradient(
            listOf(Color.Transparent, style.glow.copy(0.09f), Color.Transparent),
            center = center, radius = radius * 2.05f
        ),
        radius = radius * 2.05f, center = center
    )




    val fringe = radius * 0.030f
    val fringeStroke = radius * style.tubeRatio * 0.9f
    drawCircle(
        style.glow.copy(0.16f), radius = radius,
        center = Offset(center.x - fringe, center.y - fringe * 0.5f),
        style = Stroke(fringeStroke)
    )
    drawCircle(
        style.highlight.copy(0.14f), radius = radius,
        center = Offset(center.x + fringe, center.y + fringe * 0.5f),
        style = Stroke(fringeStroke)
    )



    val sparks = 18
    for (i in 0 until sparks) {
        val seed = i * 12.9898f
        val life = ((t * (0.30f + (i % 5) * 0.055f) + i * 0.137f) % 1f)

        val a = (i / sparks.toFloat()) * 6.2831853f + t * (0.18f + (i % 3) * 0.07f)
        val drift = radius * (1f + life * 0.55f)
        val wobble = sin(t * 2.1f + seed) * radius * 0.05f
        val px = center.x + cos(a) * drift + wobble
        val py = center.y + sin(a) * drift * 0.42f + sin(t * 1.6f + seed) * radius * 0.16f


        val born = emission[(i * FrameCatalog.SAMPLES / sparks).coerceIn(0, FrameCatalog.SAMPLES - 1)]
        val fade = (1f - life) * (1f - life) * born.coerceAtLeast(0.25f)
        if (fade <= 0.01f) continue
        drawCircle(
            style.glow.copy((0.55f * fade).coerceIn(0f, 1f)),
            radius = radius * (0.035f - life * 0.018f).coerceAtLeast(0.004f),
            center = Offset(px, py)
        )
    }




    val arcs = 3
    for (i in 0 until arcs) {
        val gate = sin(t * (3.1f + i * 1.7f) + i * 2.2f)
        if (gate < 0.86f) continue
        val a0 = (t * 0.7f + i * 2.09f) % 6.2831853f
        val span = 0.5f + (i % 2) * 0.35f
        val steps = 7
        var prevX = center.x + cos(a0) * radius
        var prevY = center.y + sin(a0) * radius * 0.62f
        for (k in 1..steps) {
            val f = k / steps.toFloat()
            val a1 = a0 + span * f


            val j = sin(f * 11.3f + t * 24f + i) * radius * 0.055f * (1f - kotlin.math.abs(f * 2f - 1f))
            val nx = center.x + cos(a1) * (radius + j)
            val ny = center.y + sin(a1) * (radius * 0.62f + j)
            drawLine(
                style.glow.copy(0.85f * (gate - 0.86f) / 0.14f),
                Offset(prevX, prevY), Offset(nx, ny),
                strokeWidth = radius * 0.020f, cap = StrokeCap.Round
            )
            prevX = nx; prevY = ny
        }
    }
}


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


    val all = remember { FrameCatalog.ids() }
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


@Composable
private fun frameDisplayName(key: String): String = when (key) {
    "Face_Of_Darkness"   -> stringResource(R.string.frame_face_of_darkness)
    "Endless_Dimension"  -> stringResource(R.string.frame_endless_dimension)
    "Sound_Of_Rooms"     -> stringResource(R.string.frame_sound_of_rooms)
    else -> key.replace('_', ' ')
}

@Composable
private fun trailDisplayName(key: String): String = when (key) {
    "Dust_Trail"   -> stringResource(R.string.trail_dust)
    "Static_Trail" -> stringResource(R.string.trail_static)
    "Salt_Trail"   -> stringResource(R.string.trail_salt)
    else -> key.replace('_', ' ')
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

            drawRoundRect(MetalBg, size = size, cornerRadius = corner)
            drawRoundRect(
                color.copy(0.20f), size = size, cornerRadius = corner,
                style = Stroke(1f)
            )

            val w = size.width * animProgress
            if (w > 0.5f) {
                clipRect(right = w) {
                    drawRoundRect(color, size = size, cornerRadius = corner)
                }

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

        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(onDrag = { change, drag ->
                        change.consume()








                        onLook(drag.x.toDp().value, drag.y.toDp().value)
                    })
                }
        )
        if (sanityTint != Color.Transparent) Box(Modifier.fillMaxSize().background(sanityTint))




        Box(placed("bar_sanity", HUD_BAR_SANITY.x, HUD_BAR_SANITY.y, 150f, 30f).width((150 * scaleOf("bar_sanity")).dp)) {
            StatusBar(stringResource(R.string.game_hud_sanity), gameState.sanity / 100f, SouliumCol)
        }
        Box(placed("bar_stamina", HUD_BAR_STAM.x, HUD_BAR_STAM.y, 150f, 30f).width((150 * scaleOf("bar_stamina")).dp)) {
            StatusBar(stringResource(R.string.game_hud_stamina), gameState.stamina / gameState.staminaMax, SuccessGreen)
        }
        Box(placed("bar_battery", HUD_BAR_BATT.x, HUD_BAR_BATT.y, 150f, 30f).width((150 * scaleOf("bar_battery")).dp)) {
            StatusBar(stringResource(R.string.game_hud_battery), gameState.flashlightBattery, CrtAmber)
        }


        Row(
            placed("readouts", HUD_READOUTS.x, HUD_READOUTS.y, 120f, 30f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            HudBadge(formatDuration(gameState.sessionElapsed), TextSec)
            if (gameState.entitiesNearby > 0) {
                HudBadge("◉ ${gameState.entitiesNearby}", DangerRed)
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





        Box(Modifier.align(Alignment.Center)) {
            androidx.compose.foundation.Canvas(Modifier.size(10.dp)) {
                val r = size.minDimension * 0.16f
                drawCircle(Color.Black.copy(0.55f), radius = r * 2.1f, center = center)
                drawCircle(Yellow.copy(0.82f), radius = r, center = center)
            }
        }


        androidx.compose.animation.AnimatedVisibility(
            visible = canEscape,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 58.dp),
            enter = fadeIn(), exit = fadeOut()
        ) {
            HudBadge(stringResource(R.string.game_hud_exit_near), SuccessGreen)
        }


        Box(placed("joystick", HUD_JOYSTICK.x, HUD_JOYSTICK.y, 140f, 140f)) {
            VirtualJoystick(
                Modifier.size((140 * scaleOf("joystick")).dp),
                onMove = { dx, dy -> onMove(dx, 0f, -dy) }
            )
        }








        Box(placed("interact", HUD_INTERACT.x, HUD_INTERACT.y, 62f, 62f)) {
            HudActionButton(
                (62 * scaleOf("interact")).dp,
                if (canEscape) SuccessGreen else TextSec,
                "interact", onInteract,
                emphasised = canEscape
            )
        }




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


@Composable
internal fun HudActionButton(
    size: Dp,
    accent: Color,
    id: String,
    onClick: () -> Unit,
    emphasised: Boolean = false,

    onHoldChange: ((Boolean) -> Unit)? = null,
    active: Boolean = false,

    interactive: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()


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


            drawCircle(
                Brush.radialGradient(
                    listOf(accent.copy(0.22f * ring), Color.Transparent),
                    center = c, radius = r * 1.55f
                ),
                radius = r * 1.55f, center = c
            )



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


            drawCircle(accent.copy(ring), radius = r * 0.94f, center = c, style = Stroke(r * 0.055f))
        }

        HudGlyph(
            id, accent,
            Modifier.fillMaxSize().padding(size * 0.26f)
                .graphicsLayer { translationY = press * 2f }
        )


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


@Composable
internal fun HudGlyph(id: String, tint: Color, modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(hudIconRes(id)),
        contentDescription = null,
        tint = tint,
        modifier = modifier
    )
}


@Composable
private fun HudBadge(text: String, color: Color) {
    Box(
        Modifier.clip(RoundedCornerShape(2.dp)).background(MetalBg.copy(0.8f)).padding(horizontal = 6.dp, vertical = 3.dp)
    ) { Text(text, color = color, fontSize = 10.sp) }
}


private const val JOYSTICK_DEADZONE = 0.12f


internal const val TRAIL_CAPACITY = 48


private const val JOYSTICK_BASE_FOLLOW = 1.4f

@Composable
fun VirtualJoystick(
    modifier: Modifier,



    interactive: Boolean = true,
    onMove: (Float, Float) -> Unit
) {









    var raw by remember { mutableStateOf(Offset.Zero) }
    var dragging by remember { mutableStateOf(false) }




    var knob by remember { mutableStateOf(Offset.Zero) }
    var base by remember { mutableStateOf(Offset.Zero) }




    val knobReleased by animateOffsetAsState(
        knob, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "joystickKnob"
    )
    val baseReleased by animateOffsetAsState(
        base, spring(dampingRatio = Spring.DampingRatioLowBouncy), label = "joystickBase"
    )
    val knobAnim = if (dragging) knob else knobReleased
    val baseAnim = if (dragging) base else baseReleased

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .offset { IntOffset(baseAnim.x.toInt(), baseAnim.y.toInt()) }
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(MetalBg.copy(0.30f), MetalBg.copy(0.75f)),
                )
            )
            .border(1.5.dp, YellowDim.copy(0.6f), CircleShape)
            .then(if (!interactive) Modifier else Modifier
            .pointerInput(Unit) {


                val travel = minOf(size.width, size.height) / 2f * 0.62f
                val maxFollow = travel * JOYSTICK_BASE_FOLLOW

                fun apply(next: Offset) {
                    raw = next
                    val len = kotlin.math.hypot(next.x, next.y)



                    val over = (len - travel).coerceAtLeast(0f).coerceAtMost(maxFollow)
                    base = if (len > 0f) next * (over / len) else Offset.Zero
                    val local = next - base
                    val localLen = kotlin.math.hypot(local.x, local.y)
                    val clamped =
                        if (localLen > travel && localLen > 0f) local * (travel / localLen) else local
                    knob = clamped

                    val nx = clamped.x / travel
                    val ny = clamped.y / travel
                    val mag = kotlin.math.hypot(nx, ny)
                    if (mag < JOYSTICK_DEADZONE) { onMove(0f, 0f); return }



                    val scaled = ((mag - JOYSTICK_DEADZONE) / (1f - JOYSTICK_DEADZONE)).coerceIn(0f, 1f)
                    onMove(nx / mag * scaled, ny / mag * scaled)
                }
                fun home() {
                    dragging = false; raw = Offset.Zero
                    knob = Offset.Zero; base = Offset.Zero; onMove(0f, 0f)
                }
                detectDragGestures(
                    onDragStart  = { pos ->
                        dragging = true
                        apply(pos - Offset(size.width / 2f, size.height / 2f))
                    },
                    onDragEnd    = { home() },
                    onDragCancel = { home() },
                    onDrag       = { change, drag ->
                        change.consume()
                        apply(raw + drag)
                    }
                )
            })
    ) {

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








                Column(
                    Modifier.fillMaxSize()
                        .background(MetalBg)
                        .padding(horizontal = 24.dp, vertical = 18.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        stringResource(R.string.menu_settings), color = CrtAmber, fontSize = 16.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 3.sp
                    )
                    DividerLine()





                    Text(stringResource(R.string.graphics_quality_label), color = TextSec, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "low"    to R.string.graphics_quality_low,
                            "medium" to R.string.graphics_quality_medium,
                            "high"   to R.string.graphics_quality_high
                        ).forEach { (key, labelRes) ->
                            val sel = s.graphicsQuality == key
                            Box(
                                Modifier.weight(1f).height(34.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (sel) CrtAmber.copy(0.16f) else MetalBg)
                                    .border(1.dp, if (sel) CrtAmber else BorderCol, RoundedCornerShape(6.dp))
                                    .clickable { settingsVm.onQuality(key) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    stringResource(labelRes),
                                    color = if (sel) CrtAmber else TextDim, fontSize = 11.sp,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                    DividerLine()
                    InGameSlider(stringResource(R.string.controls_camera_sensitivity), s.cameraSensitivity, 0.25f, 3f, settingsVm::onSensitivity)
                    InGameSlider(stringResource(R.string.audio_master_volume),  s.musicVolume,       0f,   1f, settingsVm::onMusic)
                    InGameSlider(stringResource(R.string.graphics_resolution_scale),  s.resolutionScale,   0.5f, 1f, settingsVm::onResolution)
                    InGameToggle(stringResource(R.string.graphics_fog),      s.fogEnabled,     settingsVm::onFog)
                    InGameToggle(stringResource(R.string.graphics_shadows),  s.shadowsEnabled, settingsVm::onShadows)
                    InGameToggle(stringResource(R.string.graphics_vhs_effect),      s.vhsEnabled,     settingsVm::onVhs)
                    InGameToggle(stringResource(R.string.graphics_show_fps),      s.showFps,        settingsVm::onShowFps)
                    DividerLine()

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






    val rise = gameState.endingPanel.coerceIn(0f, 1f)
    Box(Modifier.fillMaxSize().alpha(rise), Alignment.Center) {
        Column(
            Modifier.width(280.dp).clip(RoundedCornerShape(4.dp))
                .background(MetalBg.copy(alpha = 0.90f))
                .border(1.dp, DangerRed.copy(0.5f * rise), RoundedCornerShape(4.dp))
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
            StatRow(stringResource(R.string.game_stat_difficulty), gameState.difficulty.titleCase(), CrtAmber)
            DividerLine()
            AtmosphericButton(stringResource(R.string.game_return_lobby), Icons.Default.ExitToApp, DangerRed, 220.dp, 50.dp, onExit)
        }
    }
}


@Composable
fun EscapedOverlay(gameState: GameState, onExit: () -> Unit) {
    val inf  = rememberInfiniteTransition(label = "esc")
    val glow by inf.animateFloat(0.5f, 1f, infiniteRepeatable(tween(1200, easing = EaseInOut), RepeatMode.Reverse), "g")
    val scan by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart), "escScan")


    val rise = gameState.endingPanel.coerceIn(0f, 1f)
    Box(Modifier.fillMaxSize().alpha(rise), Alignment.Center) {
        Column(
            Modifier.width(300.dp).clip(RoundedCornerShape(4.dp))
                .background(MetalBg.copy(alpha = 0.90f))
                .border(1.dp, SuccessGreen.copy(0.5f * rise), RoundedCornerShape(4.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.game_escaped_title),
                color = SuccessGreen.copy(glow), fontSize = 24.sp,
                fontWeight = FontWeight.Black, letterSpacing = 3.sp
            )


            Text(
                stringResource(R.string.game_survival_label),
                color = TextDim, fontSize = 9.sp, letterSpacing = 2.sp
            )
            Text(
                formatElapsed(gameState.sessionElapsed),
                color = Yellow, fontSize = 30.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp
            )
            DividerLine()



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
            StatRow(stringResource(R.string.game_stat_difficulty), gameState.difficulty.titleCase(), CrtAmber)
            DividerLine()
            AtmosphericButton(
                stringResource(R.string.game_return_lobby), Icons.Default.ExitToApp,
                SuccessGreen, 236.dp, 50.dp, onExit, isPrimary = true
            )
        }
    }
}


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
            val coinClock = rememberFrameClock()
            androidx.compose.foundation.Canvas(Modifier.size(18.dp)) {
                drawOmniumCoin(OmniumCol.copy(shine), coinClock)
            }
            Text(
                "+${formatCurrency(amount)}",
                color = OmniumCol.copy(shine), fontSize = 14.sp, fontWeight = FontWeight.Black
            )
        }
    }
}


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


        val cardClock = rememberFrameClock()
        val artPulse by inf.animateFloat(
            0.95f, 1.05f,
            infiniteRepeatable(tween(2100, easing = EaseInOut), RepeatMode.Reverse),
            "cardArt"
        )



        val inspectable = item.category == "characters" ||
            item.category == "trails" || item.id.startsWith("trail_")



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
                    Text(item.currency.titleCase(), color = currencyColor.copy(0.7f), fontSize = 9.sp)
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
            Text("${item.price} ${item.currency.titleCase()}", color = OmniumCol, fontSize = 20.sp, fontWeight = FontWeight.Black)
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


        val clock = rememberFrameClock()
        androidx.compose.foundation.Canvas(Modifier.size(16.dp)) {
            if (isOmnium) drawOmniumCoin(color.copy(shimmer), clock)
            else drawSouliumCrystal(color.copy(shimmer), clock + 1.7f)
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


                Text(
                    profile.name.take(1).uppercase(Locale.getDefault()),
                    color = Yellow, fontSize = 18.sp, fontWeight = FontWeight.Black
                )
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

private val GameState.showFps   : Boolean get() = false


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

    drawArc(c.copy(0.8f), 0f, 360f, false,
        topLeft = Offset(w * 0.36f, h * 0.16f), size = Size(w * 0.28f, h * 0.68f), style = Stroke(sw * 0.75f))
    drawLine(c.copy(0.8f), Offset(w * 0.16f, h * 0.5f), Offset(w * 0.84f, h * 0.5f), strokeWidth = sw * 0.75f)
}


private fun currencyShade(
    base: Color, nx: Float, ny: Float, nz: Float, shininess: Float, emissive: Float
): Color {

    val lx = -0.46f; val ly = -0.62f; val lz = 0.64f
    val diffuse = (nx * lx + ny * ly + nz * lz).coerceAtLeast(0f)

    val hx = lx; val hy = ly; val hz = lz + 1f
    val hl = kotlin.math.sqrt(hx * hx + hy * hy + hz * hz)
    val spec = Math.pow(
        ((nx * hx + ny * hy + nz * hz) / hl).coerceAtLeast(0f).toDouble(), shininess.toDouble()
    ).toFloat()


    val rim = (1f - kotlin.math.abs(nz)).let { it * it } * 0.35f
    val kd = 0.26f + diffuse * 0.80f
    return Color(
        (base.red * kd + spec * 0.9f + rim * 0.5f + emissive).coerceIn(0f, 1f),
        (base.green * kd + spec * 0.9f + rim * 0.55f + emissive).coerceIn(0f, 1f),
        (base.blue * kd + spec * 0.9f + rim * 0.75f + emissive).coerceIn(0f, 1f),
        base.alpha
    )
}


private fun DrawScope.drawOmniumCoin(c: Color, t: Float) {
    val r = size.minDimension * 0.36f
    val half = r * 0.17f
    val spin = t * 1.15f
    val tilt = 0.42f
    val cs = cos(spin); val sn = sin(spin)
    val ct = cos(tilt); val st = sin(tilt)
    val seg = 22


    val px = FloatArray(seg); val pyTop = FloatArray(seg); val pyBot = FloatArray(seg)
    val depth = FloatArray(seg); val nOut = FloatArray(seg * 3)
    for (i in 0 until seg) {
        val a = i / seg.toFloat() * 6.2831853f
        val ox = cos(a) * r; val oz = sin(a) * r

        val sx = ox * cs + oz * sn
        val sz = -ox * sn + oz * cs

        px[i] = sx
        pyTop[i] = -half * ct - sz * st
        pyBot[i] = half * ct - sz * st
        depth[i] = -half * st + sz * ct

        val nx0 = cos(a); val nz0 = sin(a)
        val nsx = nx0 * cs + nz0 * sn
        val nsz = -nx0 * sn + nz0 * cs
        nOut[i * 3] = nsx; nOut[i * 3 + 1] = -nsz * st; nOut[i * 3 + 2] = nsz * ct
    }

    val faceNy = -ct; val faceNz = -st


    data class Rim(val i: Int, val d: Float)
    val rims = (0 until seg).map { Rim(it, (depth[it] + depth[(it + 1) % seg]) * 0.5f) }
        .sortedBy { it.d }
    val path = Path()
    for (rim in rims) {
        val i = rim.i; val j = (i + 1) % seg
        path.reset()
        path.moveTo(center.x + px[i], center.y + pyTop[i])
        path.lineTo(center.x + px[j], center.y + pyTop[j])
        path.lineTo(center.x + px[j], center.y + pyBot[j])
        path.lineTo(center.x + px[i], center.y + pyBot[i])
        path.close()
        drawPath(path, currencyShade(c, nOut[i * 3], nOut[i * 3 + 1], nOut[i * 3 + 2], 26f, 0f))
    }


    val towardTop = faceNz < 0f
    path.reset()
    for (i in 0 until seg) {
        val y = if (towardTop) pyTop[i] else pyBot[i]
        if (i == 0) path.moveTo(center.x + px[i], center.y + y)
        else path.lineTo(center.x + px[i], center.y + y)
    }
    path.close()
    val fn = if (towardTop) 1f else -1f
    drawPath(path, currencyShade(c, 0f, faceNy * fn, faceNz * fn, 42f, 0.06f))



    val squash = kotlin.math.abs(cs)
    val faceY = center.y + (if (towardTop) -half * ct else half * ct)
    if (squash > 0.12f) {
        drawOval(
            c.copy(0.55f),
            topLeft = Offset(center.x - r * 0.62f * squash, faceY - r * 0.62f * ct),
            size = Size(r * 1.24f * squash, r * 1.24f * ct),
            style = Stroke(r * 0.10f)
        )
        drawOval(
            Color.White.copy(0.85f),
            topLeft = Offset(center.x - r * 0.20f * squash, faceY - r * 0.20f * ct),
            size = Size(r * 0.40f * squash, r * 0.40f * ct)
        )
    }
}


private fun DrawScope.drawSouliumCrystal(c: Color, t: Float) {
    val r = size.minDimension * 0.40f
    val spin = t * 0.95f
    val tilt = 0.34f
    val cs = cos(spin); val sn = sin(spin)
    val ct = cos(tilt); val st = sin(tilt)


    val verts = arrayOf(
        floatArrayOf(0f, -1.35f, 0f), floatArrayOf(0f, 1.35f, 0f),
        floatArrayOf(1f, 0f, 0f), floatArrayOf(0f, 0f, 1f),
        floatArrayOf(-1f, 0f, 0f), floatArrayOf(0f, 0f, -1f)
    )
    val sx = FloatArray(6); val sy = FloatArray(6); val sz = FloatArray(6)
    for (i in 0 until 6) {
        val v = verts[i]
        val x = v[0] * cs + v[2] * sn
        val z = -v[0] * sn + v[2] * cs
        sx[i] = x * r
        sy[i] = (v[1] * ct - z * st) * r
        sz[i] = (v[1] * st + z * ct) * r
    }

    val faces = arrayOf(
        intArrayOf(1, 2, 3), intArrayOf(1, 3, 4), intArrayOf(1, 4, 5), intArrayOf(1, 5, 2),
        intArrayOf(0, 3, 2), intArrayOf(0, 4, 3), intArrayOf(0, 5, 4), intArrayOf(0, 2, 5)
    )
    val path = Path()
    faces.map { f -> f to (sz[f[0]] + sz[f[1]] + sz[f[2]]) / 3f }
        .sortedBy { it.second }
        .forEach { (f, _) ->
            val ax = sx[f[1]] - sx[f[0]]; val ay = sy[f[1]] - sy[f[0]]; val az = sz[f[1]] - sz[f[0]]
            val bx = sx[f[2]] - sx[f[0]]; val by = sy[f[2]] - sy[f[0]]; val bz = sz[f[2]] - sz[f[0]]
            var nx = ay * bz - az * by
            var ny = az * bx - ax * bz
            var nz = ax * by - ay * bx
            val nl = kotlin.math.sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-5f)
            nx /= nl; ny /= nl; nz /= nl



            val facing = nz > 0f
            path.reset()
            path.moveTo(center.x + sx[f[0]], center.y + sy[f[0]])
            path.lineTo(center.x + sx[f[1]], center.y + sy[f[1]])
            path.lineTo(center.x + sx[f[2]], center.y + sy[f[2]])
            path.close()
            val shaded = currencyShade(c, nx, ny, nz, 34f, if (facing) 0.10f else 0.02f)
            drawPath(path, shaded.copy(alpha = if (facing) 1f else 0.34f))
        }
}


@Composable
internal fun IconGlyphButton(
    size: Dp,
    accent: Color,
    onClick: () -> Unit,


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


                drawRoundRect(
                    Brush.radialGradient(
                        listOf(accent.copy(0.20f * breath), Color.Transparent),
                        center = center, radius = size.minDimension * 0.95f
                    ),
                    size = rect, cornerRadius = corner
                )



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


@Composable
private fun CurrencyChip(accent: Color, amount: Long, isOmnium: Boolean) {


    val clock = rememberFrameClock()
    Row(verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.foundation.Canvas(Modifier.size(16.dp)) {
            if (isOmnium) drawOmniumCoin(accent, clock)
            else drawSouliumCrystal(accent, clock + 1.7f)
        }
        Spacer(Modifier.width(4.dp))
        Text(
            formatCompactAmount(amount),
            color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp
        )
    }
}


internal fun String.titleCase(): String {
    val locale = Locale.getDefault()
    return split(' ').joinToString(" ") { word ->
        if (word.isEmpty()) word
        else word.substring(0, 1).uppercase(locale) + word.substring(1).lowercase(locale)
    }
}

private fun formatCompactAmount(v: Long): String = when {
    v >= 1_000_000 -> String.format(Locale.US, "%.1fM", v / 1_000_000.0)
    v >= 1_000     -> String.format(Locale.US, "%.1fK", v / 1_000.0)
    else           -> v.toString()
}


@Composable
fun SpawnSequenceOverlay(phase: SpawnPhase, modifier: Modifier = Modifier) {
    if (phase == SpawnPhase.READY) return

    val falling = phase == SpawnPhase.FALLING

    val veil by animateFloatAsState(
        targetValue   = if (falling) 0f else 1f,
        animationSpec = tween(durationMillis = if (falling) 1100 else 250, easing = EaseOutCubic),
        label         = "spawnVeil"
    )


    var blink by remember { mutableStateOf(0f) }
    LaunchedEffect(phase) {
        if (phase == SpawnPhase.LANDED) {
            delay(180); blink = 1f; delay(110); blink = 0f
            delay(220); blink = 1f; delay(130); blink = 0f
        }
    }
    val lid by animateFloatAsState(blink, tween(120), label = "blinkLid")

    Box(modifier.fillMaxSize()) {

        if (veil < 1f) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(1f - veil)))
        }

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



    val avatarUri: StateFlow<String?> = cosmetics.observeAvatarUri()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val frame: StateFlow<String> = cosmetics.observeFrame()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "default")
    val displayName: StateFlow<String> = identity.observeDisplayName()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val omnium: StateFlow<Long> = cosmetics.observeOmnium()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    init {
        viewModelScope.launch(Dispatchers.IO) {

            identity.currentName()
        }
    }

    fun clearSave() { viewModelScope.launch(Dispatchers.IO) { saveStore.clear() } }
}


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


private var notificationAskedThisRun = false

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
        } else if (!notificationAskedThisRun) {


            notificationAskedThisRun = true
            showRationale = true
        } else {
            resolved = true
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
        }


        val frameClock = rememberFrameClock()
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            drawFrame3D(frame, this.size.minDimension * 0.42f, frameClock)
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


private fun DrawScope.marketItemArt(id: String, category: String, accent: Color, artClock: Float = 0f) {
    when {
        id.startsWith("frame_") -> {

            val key = id.removePrefix("frame_")
            drawCircle(accent.copy(0.18f), radius = size.minDimension * 0.24f, center = center)
            drawFrame3D(key, size.minDimension * 0.30f, artClock)
        }
        id.startsWith("trail_") -> {

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









            val w = size.width; val h = size.height
            val cx = w * 0.5f
            val minD = size.minDimension

            drawCircle(
                Brush.radialGradient(
                    listOf(accent.copy(0.26f), Color.Transparent),
                    center = Offset(w * 0.34f, h * 0.30f), radius = minD * 0.75f
                ),
                radius = minD * 0.75f, center = center
            )

            val hairDark = Color(0xFF2B2440)
            val hairLit  = Color(0xFF4A3F6B)
            val skin     = Color(0xFFF6E2D6)
            val skinShade= Color(0xFFE0BFB0)



            val back = Path().apply {
                moveTo(cx, h * 0.08f)
                cubicTo(w * 0.97f, h * 0.18f, w * 0.93f, h * 0.74f, w * 0.84f, h * 0.94f)
                lineTo(w * 0.16f, h * 0.94f)
                cubicTo(w * 0.07f, h * 0.74f, w * 0.03f, h * 0.18f, cx, h * 0.08f)
                close()
            }
            drawPath(back, hairDark)


            val body = Path().apply {
                moveTo(w * 0.20f, h * 1.02f)
                cubicTo(w * 0.24f, h * 0.80f, w * 0.38f, h * 0.74f, cx, h * 0.74f)
                cubicTo(w * 0.62f, h * 0.74f, w * 0.76f, h * 0.80f, w * 0.80f, h * 1.02f)
                close()
            }
            drawPath(body, Color(0xFF1E2430))


            val face = Path().apply {
                moveTo(cx, h * 0.22f)
                cubicTo(w * 0.74f, h * 0.24f, w * 0.73f, h * 0.52f, w * 0.66f, h * 0.66f)
                cubicTo(w * 0.60f, h * 0.77f, w * 0.40f, h * 0.77f, w * 0.34f, h * 0.66f)
                cubicTo(w * 0.27f, h * 0.52f, w * 0.26f, h * 0.24f, cx, h * 0.22f)
                close()
            }
            drawPath(face, skin)

            clipPath(face) {
                drawRect(
                    Brush.horizontalGradient(
                        0.45f to Color.Transparent, 1f to skinShade.copy(0.75f),
                        startX = w * 0.30f, endX = w * 0.76f
                    ),
                    topLeft = Offset(0f, 0f), size = Size(w, h)
                )
            }




            val fringe = Path().apply {
                moveTo(cx, h * 0.14f)
                cubicTo(w * 0.76f, h * 0.17f, w * 0.76f, h * 0.34f, w * 0.72f, h * 0.48f)
                cubicTo(w * 0.70f, h * 0.34f, w * 0.64f, h * 0.28f, w * 0.54f, h * 0.30f)
                cubicTo(w * 0.58f, h * 0.40f, w * 0.55f, h * 0.44f, w * 0.50f, h * 0.46f)
                cubicTo(w * 0.45f, h * 0.44f, w * 0.42f, h * 0.40f, w * 0.46f, h * 0.30f)
                cubicTo(w * 0.36f, h * 0.28f, w * 0.30f, h * 0.34f, w * 0.28f, h * 0.48f)
                cubicTo(w * 0.24f, h * 0.34f, w * 0.24f, h * 0.17f, cx, h * 0.14f)
                close()
            }
            drawPath(fringe, hairDark)

            drawPath(
                Path().apply {
                    moveTo(w * 0.30f, h * 0.20f)
                    cubicTo(w * 0.38f, h * 0.13f, w * 0.62f, h * 0.13f, w * 0.70f, h * 0.20f)
                },
                hairLit, style = Stroke(minD * 0.045f, cap = StrokeCap.Round)
            )



            listOf(0.395f to -1f, 0.605f to 1f).forEach { (fx, _) ->
                val ex = w * fx
                val ey = h * 0.535f
                val ew = w * 0.115f
                val eh = h * 0.115f

                drawOval(
                    Color(0xFFFBF6F4),
                    topLeft = Offset(ex - ew * 0.5f, ey - eh * 0.5f),
                    size = Size(ew, eh)
                )

                drawCircle(accent.copy(0.92f), radius = eh * 0.40f, center = Offset(ex, ey + eh * 0.06f))
                drawCircle(Color(0xFF17121F), radius = eh * 0.20f, center = Offset(ex, ey + eh * 0.06f))
                drawCircle(
                    Color.White.copy(0.95f), radius = eh * 0.11f,
                    center = Offset(ex - ew * 0.16f, ey - eh * 0.14f)
                )

                drawArc(
                    Color(0xFF241C33), 190f, 160f, false,
                    topLeft = Offset(ex - ew * 0.60f, ey - eh * 0.66f),
                    size = Size(ew * 1.20f, eh * 1.20f),
                    style = Stroke(minD * 0.030f, cap = StrokeCap.Round)
                )
            }


            listOf(0.395f, 0.605f).forEach { fx ->
                drawArc(
                    hairDark.copy(0.85f), 200f, 140f, false,
                    topLeft = Offset(w * fx - w * 0.070f, h * 0.435f),
                    size = Size(w * 0.140f, h * 0.070f),
                    style = Stroke(minD * 0.024f, cap = StrokeCap.Round)
                )
            }
            drawArc(
                Color(0xFFB9736B), 20f, 140f, false,
                topLeft = Offset(cx - w * 0.045f, h * 0.640f),
                size = Size(w * 0.090f, h * 0.045f),
                style = Stroke(minD * 0.022f, cap = StrokeCap.Round)
            )
        }
        id.startsWith("priv_") || category == "vip" -> {

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


private fun heartbeat(t: Float): Float {
    val x = t % 1f
    fun beat(center: Float, width: Float) =
        kotlin.math.exp((-((x - center) * (x - center)) / (2f * width * width)).toDouble()).toFloat()
    return (beat(0.10f, 0.045f) * 1.0f + beat(0.26f, 0.055f) * 0.62f).coerceIn(0f, 1f)
}


private const val EVENT_SHIMMER_AGSL = """
uniform shader content;
uniform float2 size;
uniform float time;
uniform float intensity;
uniform float3 accent;

float roundedBox(float2 p, float2 half_, float r) {
    float2 q = abs(p) - half_ + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

half4 main(float2 coord) {
    half4 src = content.eval(coord);
    float2 uv = coord / size;
    float2 p  = coord - size * 0.5;
    float radius = min(size.x, size.y) * 0.28;
    float d = roundedBox(p, size * 0.5, radius);

    float px    = max(min(size.x, size.y) * 0.010, 1.0);
    float rim   = 1.0 - smoothstep(0.0, 2.2 * px, abs(d + 1.5 * px));
    float inner = 1.0 - smoothstep(0.0, min(size.x, size.y) * 0.30, -d);

    float b1 = sin((uv.x * 3.2 + uv.y * 1.4 - time * 0.55) * 6.2831);
    float b2 = sin((uv.x * 1.7 - uv.y * 2.6 + time * 0.31) * 6.2831 + 1.9);
    float band = pow(max(b1, 0.0), 6.0) * 0.7 + pow(max(b2, 0.0), 9.0) * 0.4;

    float ang   = atan(p.y, p.x);
    float sweep = pow(max(sin(ang * 0.5 - time * 0.9), 0.0), 16.0);

    float glow = (band * 0.42 + inner * 0.30 + rim * (0.55 + sweep * 0.9)) * intensity;
    half3 tint = mix(half3(accent), half3(1.0), half(sweep * 0.45));
    half3 lit  = src.rgb + tint * half(glow) * src.a;
    return half4(lit, src.a);
}
"""


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




    val sweepTilt by inf.animateFloat(
        -1f, 1f,
        infiniteRepeatable(tween(11_000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        "eventTilt"
    )
    val pressDepth by animateFloatAsState(
        if (pressed) 1f else 0f,
        spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
        label = "eventPress"
    )
    val tint = if (enabled) accent else TextDim










    val shaderModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val shader = remember {
            runCatching { android.graphics.RuntimeShader(EVENT_SHIMMER_AGSL) }
                .onFailure { OmniLog.e("GL", "event shimmer AGSL did not compile", it) }
                .getOrNull()
        }
        if (shader == null) Modifier else Modifier.graphicsLayer {
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











                val breathe = (1f - pressDepth) * pulse
                val s = (1f - pressDepth * 0.045f) * (1f + breathe * 0.004f)
                scaleX = s


                scaleY = s * (1f + breathe * 0.003f)
                translationY = pressDepth * 4f - breathe * 0.9f * density
                rotationZ = (1f - pressDepth) * sweepTilt * 0.22f


                shadowElevation = (10f - pressDepth * 7f + breathe * 2.6f) * density
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
                    label,
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


private fun DrawScope.drawEventButtonPlate(
    accent: Color,
    pulse: Float,
    sweep: Float,
    pressDepth: Float,
    progress: Float?
) {
    val r = size.minDimension * 0.28f
    val corner = androidx.compose.ui.geometry.CornerRadius(r)


    drawRoundRect(
        Brush.radialGradient(
            listOf(accent.copy(0.30f * pulse + 0.06f), Color.Transparent),
            center = center, radius = size.maxDimension * 0.75f
        ),
        cornerRadius = corner
    )


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


    drawRoundRect(
        Brush.verticalGradient(
            listOf(Color.White.copy(0.14f - pressDepth * 0.10f), Color.Transparent),
            endY = size.height * 0.42f
        ),
        cornerRadius = corner
    )


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


    val bandW = size.width * 0.22f
    val bx = size.width * sweep
    drawRoundRect(
        Brush.horizontalGradient(
            listOf(Color.Transparent, Color.White.copy(0.13f), Color.Transparent),
            startX = bx - bandW / 2f, endX = bx + bandW / 2f
        ),
        cornerRadius = corner
    )


    drawRoundRect(accent.copy(0.85f), cornerRadius = corner, style = Stroke(1.6f))
    drawRoundRect(
        accent.copy(0.22f + pulse * 0.38f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(r + 3f),
        style = Stroke(3.2f)
    )


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


internal object Skeleton {
    const val BONES = 12

    const val HIPS = 0; const val SPINE = 1; const val CHEST = 2; const val HEAD = 3
    const val UPPER_ARM_L = 4; const val FORE_ARM_L = 5
    const val UPPER_ARM_R = 6; const val FORE_ARM_R = 7
    const val THIGH_L = 8; const val SHIN_L = 9
    const val THIGH_R = 10; const val SHIN_R = 11


    val parent = intArrayOf(-1, HIPS, SPINE, CHEST,
                            CHEST, UPPER_ARM_L, CHEST, UPPER_ARM_R,
                            HIPS, THIGH_L, HIPS, THIGH_R)


    val head = arrayOf(
        floatArrayOf(0f, 0.480f, 0f),
        floatArrayOf(0f, 0.480f, 0f),
        floatArrayOf(0f, 0.630f, 0f),
        floatArrayOf(0f, 0.820f, 0f),
        floatArrayOf(-0.075f, 0.780f, 0f),
        floatArrayOf(-0.115f, 0.615f, 0f),
        floatArrayOf(0.075f, 0.780f, 0f),
        floatArrayOf(0.115f, 0.615f, 0f),
        floatArrayOf(-0.052f, 0.460f, 0f),
        floatArrayOf(-0.052f, 0.245f, 0f),
        floatArrayOf(0.052f, 0.460f, 0f),
        floatArrayOf(0.052f, 0.245f, 0f)
    )


    val tail = arrayOf(
        floatArrayOf(0f, 0.560f, 0f),
        floatArrayOf(0f, 0.630f, 0f),
        floatArrayOf(0f, 0.800f, 0f),
        floatArrayOf(0f, 1.000f, 0f),
        floatArrayOf(-0.115f, 0.615f, 0f),
        floatArrayOf(-0.168f, 0.452f, 0f),
        floatArrayOf(0.115f, 0.615f, 0f),
        floatArrayOf(0.168f, 0.452f, 0f),
        floatArrayOf(-0.052f, 0.245f, 0f),
        floatArrayOf(-0.052f, 0.008f, 0.030f),
        floatArrayOf(0.052f, 0.245f, 0f),
        floatArrayOf(0.052f, 0.008f, 0.030f)
    )




    val radius = floatArrayOf(
        0.130f, 0.120f, 0.130f, 0.120f,
        0.055f, 0.050f, 0.055f, 0.050f,
        0.075f, 0.065f, 0.075f, 0.065f
    )


    private fun distSq(bone: Int, px: Float, py: Float, pz: Float): Float {
        val a = head[bone]; val b = tail[bone]
        val abx = b[0] - a[0]; val aby = b[1] - a[1]; val abz = b[2] - a[2]
        val apx = px - a[0];   val apy = py - a[1];   val apz = pz - a[2]
        val denom = abx * abx + aby * aby + abz * abz
        val t = if (denom <= 1e-8f) 0f
                else ((apx * abx + apy * aby + apz * abz) / denom).coerceIn(0f, 1f)
        val dx = apx - abx * t; val dy = apy - aby * t; val dz = apz - abz * t
        return dx * dx + dy * dy + dz * dz
    }


    private const val SOFT = 0.35f




    private fun falloff(d: Float, r: Float): Float {
        val s = SOFT * r
        val q = kotlin.math.sqrt(d * d + s * s) / r
        return 1f / (q * q * q * q)
    }




    fun bindMesh(
        verts: FloatArray, posStride: Int, posOffset: Int, vertexCount: Int,
        indices: ShortArray, outIdx: IntArray, outWt: FloatArray
    ) {

        val nodeOf = IntArray(vertexCount)
        val byKey = HashMap<Long, Int>(vertexCount * 2)
        val nx = FloatArray(vertexCount); val ny = FloatArray(vertexCount)
        val nz = FloatArray(vertexCount)
        var nodes = 0
        for (v in 0 until vertexCount) {
            val o = posOffset + v * posStride
            val x = verts[o]; val y = verts[o + 1]; val z = verts[o + 2]


            val key = (Math.round(x * 10000f).toLong() and 0x1FFFFF shl 42) or
                      (Math.round(y * 10000f).toLong() and 0x1FFFFF shl 21) or
                      (Math.round(z * 10000f).toLong() and 0x1FFFFF)
            val existing = byKey[key]
            if (existing != null) {
                nodeOf[v] = existing
            } else {
                byKey[key] = nodes
                nodeOf[v] = nodes
                nx[nodes] = x; ny[nodes] = y; nz[nodes] = z
                nodes++
            }
        }


        val triCount = indices.size / 3
        val degree = IntArray(nodes + 1)
        val ea = IntArray(triCount * 6); val eb = IntArray(triCount * 6)
        var edges = 0
        for (t in 0 until triCount) {
            val a = nodeOf[indices[t * 3].toInt() and 0xFFFF]
            val b = nodeOf[indices[t * 3 + 1].toInt() and 0xFFFF]
            val c = nodeOf[indices[t * 3 + 2].toInt() and 0xFFFF]
            if (a != b) { ea[edges] = a; eb[edges] = b; edges++ }
            if (b != c) { ea[edges] = b; eb[edges] = c; edges++ }
            if (c != a) { ea[edges] = c; eb[edges] = a; edges++ }
        }
        for (e in 0 until edges) { degree[ea[e]]++; degree[eb[e]]++ }
        val start = IntArray(nodes + 1)
        for (n in 0 until nodes) start[n + 1] = start[n] + degree[n]
        val cursor = start.copyOf()
        val adj = IntArray(start[nodes]); val cost = FloatArray(start[nodes])
        for (e in 0 until edges) {
            val a = ea[e]; val b = eb[e]
            val dx = nx[a] - nx[b]; val dy = ny[a] - ny[b]; val dz = nz[a] - nz[b]
            val len = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
            adj[cursor[a]] = b; cost[cursor[a]] = len; cursor[a]++
            adj[cursor[b]] = a; cost[cursor[b]] = len; cursor[b]++
        }


        val geo = Array(BONES) { FloatArray(nodes) }



        val heap = LongArray(adj.size + nodes + 16)
        for (b in 0 until BONES) {
            val g = geo[b]
            java.util.Arrays.fill(g, Float.MAX_VALUE)
            var size = 0



            val seedR = 0.6f * radius[b]
            var seeded = false
            for (n in 0 until nodes) {
                val d = kotlin.math.sqrt(distSq(b, nx[n], ny[n], nz[n]))
                if (d <= seedR) { g[n] = d; size = heapPush(heap, size, d, n); seeded = true }
            }
            if (!seeded) {
                var best = 0; var bestD = Float.MAX_VALUE
                for (n in 0 until nodes) {
                    val d = distSq(b, nx[n], ny[n], nz[n])
                    if (d < bestD) { bestD = d; best = n }
                }
                g[best] = kotlin.math.sqrt(bestD)
                size = heapPush(heap, size, g[best], best)
            }
            while (size > 0) {
                val top = heap[0]
                val du = java.lang.Float.intBitsToFloat((top ushr 32).toInt())
                val u = (top and 0xFFFFFFFFL).toInt()
                size = heapPop(heap, size)
                if (du > g[u]) continue
                var k = start[u]
                val end = start[u + 1]
                while (k < end) {
                    val w = adj[k]
                    val nd = du + cost[k]
                    if (nd < g[w]) { g[w] = nd; size = heapPush(heap, size, nd, w) }
                    k++
                }
            }
        }


        for (v in 0 until vertexCount) {
            val n = nodeOf[v]
            var i0 = HIPS; var i1 = HIPS; var i2 = HIPS; var i3 = HIPS
            var w0 = -1f; var w1 = -1f; var w2 = -1f; var w3 = -1f
            for (b in 0 until BONES) {
                val d = geo[b][n]
                if (d == Float.MAX_VALUE) continue
                val w = falloff(d, radius[b])
                when {
                    w > w0 -> { i3=i2; w3=w2; i2=i1; w2=w1; i1=i0; w1=w0; i0=b; w0=w }
                    w > w1 -> { i3=i2; w3=w2; i2=i1; w2=w1; i1=b;  w1=w }
                    w > w2 -> { i3=i2; w3=w2; i2=b;  w2=w }
                    w > w3 -> { i3=b;  w3=w }
                }
            }
            val sum = (if (w0 > 0f) w0 else 0f) + (if (w1 > 0f) w1 else 0f) +
                      (if (w2 > 0f) w2 else 0f) + (if (w3 > 0f) w3 else 0f)
            val o = v * 4
            if (sum > 1e-8f) {
                val inv = 1f / sum
                outIdx[o] = i0; outIdx[o+1] = i1; outIdx[o+2] = i2; outIdx[o+3] = i3
                outWt[o] = if (w0 > 0f) w0 * inv else 0f
                outWt[o+1] = if (w1 > 0f) w1 * inv else 0f
                outWt[o+2] = if (w2 > 0f) w2 * inv else 0f
                outWt[o+3] = if (w3 > 0f) w3 * inv else 0f
            } else {


                var best = HIPS; var bestD = Float.MAX_VALUE
                for (b in 0 until BONES) {
                    val d = distSq(b, nx[n], ny[n], nz[n])
                    if (d < bestD) { bestD = d; best = b }
                }
                outIdx[o] = best; outIdx[o+1] = best; outIdx[o+2] = best; outIdx[o+3] = best
                outWt[o] = 1f; outWt[o+1] = 0f; outWt[o+2] = 0f; outWt[o+3] = 0f
            }
        }
    }


    private fun heapPush(h: LongArray, size: Int, d: Float, n: Int): Int {
        if (size >= h.size) return size
        var i = size
        h[i] = (java.lang.Float.floatToRawIntBits(d).toLong() shl 32) or n.toLong()
        while (i > 0) {
            val p = (i - 1) / 2
            if (h[p] <= h[i]) break
            val t = h[p]; h[p] = h[i]; h[i] = t
            i = p
        }
        return size + 1
    }

    private fun heapPop(h: LongArray, size: Int): Int {
        val n = size - 1
        h[0] = h[n]
        var i = 0
        while (true) {
            val l = i * 2 + 1; val r = l + 1
            var m = i
            if (l < n && h[l] < h[m]) m = l
            if (r < n && h[r] < h[m]) m = r
            if (m == i) break
            val t = h[m]; h[m] = h[i]; h[i] = t
            i = m
        }
        return n
    }
}


internal class PoseBuilder {

    val matrices = FloatArray(Skeleton.BONES * 16)

    private val local = FloatArray(16)
    private val tmp = FloatArray(16)
    private val work = FloatArray(16)

    private fun composeBone(b: Int, rx: Float, ry: Float, rz: Float) {
        val h = Skeleton.head[b]
        Matrix.setIdentityM(local, 0)
        Matrix.translateM(local, 0, h[0], h[1], h[2])
        if (rz != 0f) Matrix.rotateM(local, 0, rz, 0f, 0f, 1f)
        if (ry != 0f) Matrix.rotateM(local, 0, ry, 0f, 1f, 0f)
        if (rx != 0f) Matrix.rotateM(local, 0, rx, 1f, 0f, 0f)
        Matrix.translateM(local, 0, -h[0], -h[1], -h[2])

        val p = Skeleton.parent[b]
        if (p < 0) {
            System.arraycopy(local, 0, matrices, b * 16, 16)
        } else {
            System.arraycopy(matrices, p * 16, tmp, 0, 16)
            Matrix.multiplyMM(work, 0, tmp, 0, local, 0)
            System.arraycopy(work, 0, matrices, b * 16, 16)
        }
    }


    private fun rootOffset(dy: Float) {
        Matrix.setIdentityM(local, 0)
        Matrix.translateM(local, 0, 0f, dy, 0f)
        System.arraycopy(matrices, Skeleton.HIPS * 16, tmp, 0, 16)
        Matrix.multiplyMM(work, 0, local, 0, tmp, 0)
        System.arraycopy(work, 0, matrices, Skeleton.HIPS * 16, 16)
    }







    fun build(
        time: Float, walk: Float, crouch: Float, air: Float,
        headYaw: Float, headPitch: Float, torch: Float,
        collapse: Float = 0f
    ) {
        val gait = walk.coerceIn(0f, 1.6f)
        val run = ((gait - 1f) / 0.6f).coerceIn(0f, 1f)
        val stride = time * 6.4f
        val deg = 57.29578f

        val down = collapse.coerceIn(0f, 1f)



        val bob = sin(stride * 2f) * 0.012f * gait
        val breath = sin(time * 1.6f) * 0.004f
        composeBone(Skeleton.HIPS,
            rx = (-8f * crouch - 62f * down) ,
            ry = sin(time * 0.5f) * 1.5f * (1f - down),
            rz = sin(stride) * 2.2f * gait)
        rootOffset(bob + breath - 0.30f * crouch - 0.52f * down + 0.10f * air)

        composeBone(Skeleton.SPINE,
            rx = 6f * crouch + 14f * down,
            ry = sin(stride + 0.4f) * 2.5f * gait,
            rz = 0f)
        composeBone(Skeleton.CHEST,
            rx = 4f * crouch + 10f * down - 6f * torch,
            ry = -sin(stride) * 4.5f * gait,
            rz = 0f)




        composeBone(Skeleton.HEAD,
            rx = (-headPitch * deg).coerceIn(-38f, 38f) + 26f * down,
            ry = (headYaw * deg).coerceIn(-58f, 58f) * (1f - down),
            rz = sin(time * 0.7f) * 1.6f * (1f - down))







        for (side in 0..1) {
            val s = if (side == 0) -1f else 1f
            val upper = if (side == 0) Skeleton.UPPER_ARM_L else Skeleton.UPPER_ARM_R
            val fore = if (side == 0) Skeleton.FORE_ARM_L else Skeleton.FORE_ARM_R
            val isRight = if (side == 1) 1f else 0f
            val torchArm = torch * isRight

            val rest = 0f
            val phase = stride + if (side == 1) Math.PI.toFloat() else 0f
            val swing = sin(phase) * (23f + 17f * run) * gait
            val idle = sin(time * 0.9f + s) * 3.0f


            val swingX = mix(swing + idle, -71f, torchArm)
            val tuck = -11f * s + mix(0f, -19f * s, torchArm)
            composeBone(upper,
                rx = swingX - 26f * down,
                ry = tuck,
                rz = rest + 8f * s * crouch + 30f * s * down)



            val lag = sin(phase - 0.85f)
            val bend = mix(lag * 17f * gait + 6f, -30f, torchArm)
            composeBone(fore, rx = bend + 34f * down, ry = 0f, rz = 0f)
        }


        for (side in 0..1) {
            val s = if (side == 0) -1f else 1f
            val thigh = if (side == 0) Skeleton.THIGH_L else Skeleton.THIGH_R
            val shin = if (side == 0) Skeleton.SHIN_L else Skeleton.SHIN_R
            val phase = stride + if (side == 1) Math.PI.toFloat() else 0f



            val hipFold = 52f * crouch
            val kneeFold = -96f * crouch

            val swing = sin(phase) * (30f + 19f * run) * gait
            val lift = max(0f, -sin(phase - 0.6f)) * (24f + 17f * run) * gait

            composeBone(thigh,
                rx = swing + hipFold + 40f * air + 64f * down,
                ry = 0f,
                rz = 3f * s * gait)
            composeBone(shin,
                rx = -lift + kneeFold - 54f * air - 78f * down,
                ry = 0f, rz = 0f)
        }
    }

    private fun mix(a: Float, b: Float, t: Float) = a + (b - a) * t.coerceIn(0f, 1f)
}

class CharacterMesh(
    val vertexBuffer: FloatArray,
    val indices: ShortArray
) {
    companion object {
        private const val MAGIC = 0x48534D4F



        const val FLOATS_PER_VERTEX = 16
        private const val FILE_FLOATS_PER_VERTEX = 8



        fun load(ctx: Context, assetPath: String): CharacterMesh? = runCatching {
            val bytes = ctx.assets.open(assetPath).use { it.readBytes() }
            val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val magic = bb.int
            require(magic == MAGIC) { "bad magic 0x${Integer.toHexString(magic)}" }
            bb.short; bb.short
            val vertexCount = bb.int
            val indexCount = bb.int
            require(vertexCount in 1..500_000 && indexCount in 3..2_000_000) {
                "implausible counts v=$vertexCount i=$indexCount"
            }
            val fileVerts = FloatArray(vertexCount * FILE_FLOATS_PER_VERTEX)
            bb.asFloatBuffer().get(fileVerts)
            bb.position(bb.position() + fileVerts.size * 4)
            val idx = ShortArray(indexCount)
            bb.asShortBuffer().get(idx)





            val verts = FloatArray(vertexCount * FLOATS_PER_VERTEX)
            val bi = IntArray(vertexCount * 4)
            val bw = FloatArray(vertexCount * 4)
            Skeleton.bindMesh(fileVerts, FILE_FLOATS_PER_VERTEX, 0, vertexCount, idx, bi, bw)
            for (v in 0 until vertexCount) {
                val src = v * FILE_FLOATS_PER_VERTEX
                val dst = v * FLOATS_PER_VERTEX
                System.arraycopy(fileVerts, src, verts, dst, FILE_FLOATS_PER_VERTEX)
                for (k in 0 until 4) {
                    verts[dst + 8 + k] = bi[v * 4 + k].toFloat()
                    verts[dst + 12 + k] = bw[v * 4 + k]
                }
            }
            OmniLog.i("Model", "loaded $assetPath: $vertexCount verts, ${indexCount / 3} tris, skinned to ${Skeleton.BONES} bones")
            CharacterMesh(verts, idx)
        }.onFailure { OmniLog.e("Model", "failed to load $assetPath", it) }.getOrNull()
    }
}


private class VineSpec(
    val rootU: Float, val rootV: Float,
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
layout(location=2) in float aGrow;

uniform mat4 uMVP;
uniform highp float uGrowth;
uniform float uSway;
out vec3 vNormal; out float vGrow; out vec3 vLocal;

void main(){
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
in highp vec3 vNormal; in highp float vGrow; in highp vec3 vLocal;
uniform vec3 uAccent;
uniform float uPulse;
uniform highp float uGrowth;
out vec4 fragColor;
void main(){
    if (vGrow > uGrowth) discard;
    vec3 n = normalize(vNormal);
    vec3 key  = normalize(vec3(-0.45, 0.75, 0.5));
    vec3 fill = normalize(vec3(0.7, -0.2, 0.35));
    float kd = max(dot(n, key), 0.0);
    float fd = max(dot(n, fill), 0.0) * 0.35;

    vec3 view = vec3(0.0, 0.0, 1.0);
    vec3 h = normalize(key + view);
    float spec = pow(max(dot(n, h), 0.0), 24.0) * 0.7;

    vec3 deep = uAccent * 0.28;
    vec3 tip  = uAccent * (1.0 + uPulse * 0.6);
    vec3 base = mix(deep, tip, vGrow);

    float around = atan(vLocal.y, vLocal.x);
    float fibre  = sin(vGrow * 190.0 + around * 2.0) * 0.5 + 0.5;
    float mottle = sin(vGrow * 41.0 + 1.7) * sin(around * 5.0 + vGrow * 12.0);
    float grain  = 1.0 + (fibre - 0.5) * 0.13 + mottle * 0.07;

    vec3 along = normalize(vec3(-n.y, n.x, 0.0));
    float aniso = 1.0 - abs(dot(along, h)) * 0.75;
    float sheen = pow(max(dot(n, h), 0.0), 42.0) * aniso * 0.55;

    float rim = pow(1.0 - max(dot(n, view), 0.0), 3.0) * 0.5 * (0.4 + uPulse);
    vec3 col = base * grain * (0.22 + kd * 0.85 + fd)
             + vec3(spec * 0.55 + sheen) * mix(uAccent, vec3(1.0), 0.35)
             + uAccent * rim;

    float front = smoothstep(uGrowth - 0.09, uGrowth, vGrow);
    col += uAccent * front * (0.55 + uPulse * 0.9);
    col += uAccent * pow(vGrow, 3.0) * 0.22 * (0.6 + uPulse * 0.5);

    float edge = smoothstep(0.02, 0.30, abs(dot(n, view)));
    fragColor = vec4(col, 0.35 + 0.65 * edge);
}
"""


private fun buildVineMesh(spec: VineSpec, segments: Int = 34, sides: Int = 12):
        Pair<FloatArray, ShortArray> {

    val verts = ArrayList<Float>((segments + 1) * sides * 7)
    val idx = ArrayList<Short>(segments * sides * 6)


    val px = spec.rootU * 2f - 1f
    val py = 1f - spec.rootV * 2f
    val perpX = -spec.dirY
    val perpY = spec.dirX

    fun spineAt(t: Float): Triple<Float, Float, Float> {

        val bow = kotlin.math.sin(t * Math.PI).toFloat() * spec.curl
        val x = px + spec.dirX * spec.length * t + perpX * bow
        val y = py + spec.dirY * spec.length * t + perpY * bow


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


        var ux = -ty; var uy = tx; var uz = 0f
        val ul = kotlin.math.sqrt(ux * ux + uy * uy + uz * uz).coerceAtLeast(1e-5f)
        ux /= ul; uy /= ul; uz /= ul
        val vx = ty * uz - tz * uy
        val vy = tz * ux - tx * uz
        val vz = tx * uy - ty * ux


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


    var base = (segments + 1) * sides
    for (l in 1..spec.leaves) {
        val t = l / (spec.leaves + 1f)
        val (cx, cy, cz) = spineAt(t)
        val side = if (l % 2 == 0) 1f else -1f
        val lx = perpX * side; val ly = perpY * side
        val size = 0.085f * (1f - t * 0.4f)
        val nz = 0.75f

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


private class MultisampleConfigChooser(
    private val samples: Int = 4
) : GLSurfaceView.EGLConfigChooser {

    override fun chooseConfig(egl: EGL10, display: EGLDisplay): EGLConfig {
        for (want in intArrayOf(samples, 2, 0)) {
            pick(egl, display, want)?.let { return it }
        }


        return pick(egl, display, 0, depth = 0)
            ?: throw IllegalArgumentException("no EGL config on this device")
    }

    private fun pick(
        egl: EGL10, display: EGLDisplay, want: Int, depth: Int = 16
    ): EGLConfig? {
        val spec = mutableListOf(
            EGL10.EGL_RED_SIZE, 8,
            EGL10.EGL_GREEN_SIZE, 8,
            EGL10.EGL_BLUE_SIZE, 8,
            EGL10.EGL_ALPHA_SIZE, 8,
            EGL10.EGL_DEPTH_SIZE, depth,
            EGL10.EGL_RENDERABLE_TYPE, 0x0040
        )
        if (want > 0) {
            spec += listOf(EGL10.EGL_SAMPLE_BUFFERS, 1, EGL10.EGL_SAMPLES, want)
        }
        spec += EGL10.EGL_NONE
        val attrs = spec.toIntArray()

        val count = IntArray(1)
        if (!egl.eglChooseConfig(display, attrs, null, 0, count) || count[0] <= 0) return null
        val configs = arrayOfNulls<EGLConfig>(count[0])
        if (!egl.eglChooseConfig(display, attrs, configs, count[0], count)) return null


        return configs.firstOrNull()
    }
}


class VineRenderer(private val onFailed: () -> Unit = {}) : GLSurfaceView.Renderer {
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




        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        runCatching {
            program = linkGlProgram(OMNI_VINE_VERT, OMNI_VINE_FRAG, "vine")
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
        }.onFailure {
            OmniLog.e("Vine", "vine setup failed", it)







            onFailed()
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / height.coerceAtLeast(1)


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


@Composable
fun VineLayer(accent: Color, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var failed by remember { mutableStateOf(false) }
    val renderer = remember { VineRenderer(onFailed = { failed = true }) }
    LaunchedEffect(accent) {
        renderer.accent = Triple(accent.red, accent.green, accent.blue)
    }
    val glView = remember {
        GLSurfaceView(ctx).apply {
            setEGLContextClientVersion(3)









            setEGLConfigChooser(MultisampleConfigChooser(samples = 4))
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


    if (!failed) AndroidView(factory = { glView }, modifier = modifier)
}


private const val OMNI_SHAFT_VERT = """#version 300 es
layout(location=0) in vec3 aPos;
layout(location=1) in vec3 aNormal;
layout(location=2) in vec2 aUV;
layout(location=3) in float aLight;
uniform mat4 uMVP;
out float vFall;
out float vEdge;
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
in highp float vFall; in highp float vEdge; in highp float vIntensity;
uniform float uFlicker;
uniform vec3 uTint;
out vec4 fragColor;
void main(){
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

layout(location=3) in vec4 aBoneIdx;
layout(location=4) in vec4 aBoneWt;

uniform mat4 uMVP;
uniform mat4 uModel;

uniform mat4 uBones[12];

uniform float uAnimate;

out vec3 vNormal; out vec2 vUV; out vec3 vWorldPos;

void main(){
    vec3 p = aPos;
    vec3 n = aNormal;

    if (uAnimate > 0.5) {
        mat4 skin =
            uBones[int(aBoneIdx.x)] * aBoneWt.x +
            uBones[int(aBoneIdx.y)] * aBoneWt.y +
            uBones[int(aBoneIdx.z)] * aBoneWt.z +
            uBones[int(aBoneIdx.w)] * aBoneWt.w;
        p = (skin * vec4(aPos, 1.0)).xyz;
        n = normalize(mat3(skin) * aNormal);
    }

    vec4 world = uModel * vec4(p, 1.0);
    vWorldPos = world.xyz;
    vNormal = mat3(uModel) * n;
    vUV = aUV;
    gl_Position = uMVP * vec4(p, 1.0);
}
"""


private const val OMNI_PREVIEW_FRAG = """#version 300 es
precision mediump float;
in highp vec3 vNormal; in highp vec2 vUV; in highp vec3 vWorldPos;
uniform sampler2D uTex;
uniform float uIsCharacter;
uniform float uTime;

uniform vec3 uSubject;
out vec4 fragColor;

float pHash(vec2 p){ return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }

float pNoise(vec2 p){
    vec2 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(pHash(i), pHash(i + vec2(1.0, 0.0)), f.x),
               mix(pHash(i + vec2(0.0, 1.0)), pHash(i + vec2(1.0, 1.0)), f.x), f.y);
}

float pFbm(vec2 p){
    float v = 0.0, a = 0.5;
    for (int i = 0; i < 5; i++) { v += a * pNoise(p); p *= 2.03; a *= 0.5; }
    return v;
}

void main(){
    vec4 tex = texture(uTex, vUV);
    if (uIsCharacter > 0.5 && tex.a < 0.35) discard;
    vec3 n = normalize(vNormal);
    vec3 view = normalize(vec3(0.0, 0.15, 1.0));

    if (uIsCharacter > 0.5) {
        vec3 keyDir  = normalize(vec3(-0.55, 0.82, 0.62));
        vec3 fillDir = normalize(vec3( 0.78, 0.22, 0.42));
        vec3 rimDir  = normalize(vec3( 0.15, 0.45, -0.92));

        float key  = max(dot(n, keyDir), 0.0);
        float fill = max(dot(n, fillDir), 0.0);
        float rim  = pow(max(dot(n, rimDir), 0.0), 2.2);
        float wrapped = max((dot(n, keyDir) + 0.35) / 1.35, 0.0);

        vec3 keyCol  = vec3(1.00, 0.95, 0.86) * (wrapped * 0.95 + key * 0.25);
        vec3 fillCol = vec3(0.42, 0.52, 0.72) * fill * 0.38;
        vec3 rimCol  = vec3(1.00, 0.92, 0.74) * rim * 0.85;

        vec3 half0 = normalize(keyDir + view);
        float spec = pow(max(dot(n, half0), 0.0), 34.0) * 0.28;

        vec3 col = tex.rgb * (vec3(0.16, 0.17, 0.21) + keyCol + fillCol) + rimCol + spec;
        fragColor = vec4(col, 1.0);
        return;
    }

    float radial = length(vWorldPos.xz - uSubject.xz);
    float sweep = 1.0 - smoothstep(0.4, 3.4, radial);
    float height = 1.0 - smoothstep(0.0, 2.6, vWorldPos.y);
    vec3 cove = mix(vec3(0.030, 0.030, 0.036), vec3(0.155, 0.150, 0.140),
                    max(sweep * 0.85, height * 0.35));

    vec2 sky = vec2(vWorldPos.x * 0.34, vWorldPos.y * 0.30) + vec2(uTime * 0.011, uTime * 0.004);
    float warp = pFbm(sky * 0.7 + vec2(uTime * 0.006, 0.0));
    float far  = pFbm(sky * 0.9 + warp * 0.85);
    float near = pFbm(sky * 1.9 - vec2(uTime * 0.019, uTime * 0.007) + warp * 0.4);

    vec3 deep  = vec3(0.055, 0.042, 0.098);
    vec3 glow  = vec3(0.030, 0.088, 0.115);
    vec3 cosmic = mix(deep, glow, smoothstep(0.35, 0.85, far));
    cosmic += glow * smoothstep(0.55, 0.95, near) * 0.55;

    float dark = 1.0 - max(sweep * 0.85, height * 0.35);
    cove += cosmic * dark * dark * 0.9;

    float lit = max(sweep, height * 0.5);
    vec2 d0 = vec2(vWorldPos.x * 7.0, vWorldPos.y * 7.0 - uTime * 0.09);
    vec2 d1 = vec2(vWorldPos.x * 13.0 + 31.7, vWorldPos.y * 13.0 - uTime * 0.16);
    float motes = pow(pNoise(d0), 15.0) * 1.6 + pow(pNoise(d1), 19.0) * 1.1;
    motes *= 0.65 + 0.35 * sin(uTime * 1.7 + pHash(floor(d0)) * 24.0);
    cove += vec3(1.00, 0.96, 0.88) * motes * lit * 0.85;

    float contact = 1.0 - smoothstep(0.0, 0.62, radial);
    cove *= 1.0 - contact * 0.80 * (1.0 - smoothstep(0.0, 0.22, vWorldPos.y));

    fragColor = vec4(cove, 1.0);
}
"""


class CharacterPreviewRenderer(private val appContext: Context) : GLSurfaceView.Renderer {





    @Volatile var yawDegrees: Float = 18f
    @Volatile var walkAmount: Float = 0f

    @Volatile var pitchDegrees: Float = 7f

    @Volatile var distance: Float = 3.3f

    private var program = 0
    private var uMVP = 0; private var uModel = 0; private var uTime = 0
    private var uTex = 0; private var uIsChar = 0
    private var uAnimate = 0; private var uSubject = 0; private var uBones = 0


    private val pose = PoseBuilder()

    private var charVbo = 0; private var charIbo = 0; private var charCount = 0
    private var roomVbo = 0; private var roomIbo = 0
    private var wallCount = 0; private var floorCount = 0
    private var wallVbo = 0; private var wallIbo = 0
    private var charTex = 0

    private val proj = FloatArray(16)
    private val view = FloatArray(16)
    private val vp = FloatArray(16)
    private val model = FloatArray(16)
    private val mvp = FloatArray(16)
    private val start = System.nanoTime()

    companion object {

        const val MIN_DIST = 1.7f
        const val MAX_DIST = 5.2f
        const val MIN_PITCH = -10f
        const val MAX_PITCH = 38f
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {


        GLES30.glClearColor(0.030f, 0.030f, 0.036f, 1f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        runCatching {
            program = linkGlProgram(OMNI_PREVIEW_VERT, OMNI_PREVIEW_FRAG, "preview")
            uMVP = GLES30.glGetUniformLocation(program, "uMVP")
            uModel = GLES30.glGetUniformLocation(program, "uModel")
            uTime = GLES30.glGetUniformLocation(program, "uTime")
            uBones = GLES30.glGetUniformLocation(program, "uBones")
            uTex = GLES30.glGetUniformLocation(program, "uTex")
            uIsChar = GLES30.glGetUniformLocation(program, "uIsCharacter")
            uAnimate = GLES30.glGetUniformLocation(program, "uAnimate")
            uSubject = GLES30.glGetUniformLocation(program, "uSubject")

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










            val floorQuad = quadMesh(
                floatArrayOf(-14f, 0f, 14f), floatArrayOf(14f, 0f, 14f),
                floatArrayOf(14f, 0f, -14f), floatArrayOf(-14f, 0f, -14f),
                floatArrayOf(0f, 1f, 0f), 1f
            )
            roomVbo = genBuf(); roomIbo = genBuf()
            uploadQuad(roomVbo, roomIbo, floorQuad)
            floorCount = 6

            val wallQuad = quadMesh(
                floatArrayOf(-14f, 0f, -14f), floatArrayOf(14f, 0f, -14f),
                floatArrayOf(14f, 14f, -14f), floatArrayOf(-14f, 14f, -14f),
                floatArrayOf(0f, 0f, 1f), 1f
            )
            wallVbo = genBuf(); wallIbo = genBuf()
            uploadQuad(wallVbo, wallIbo, wallQuad)
            wallCount = 6





            charTex = loadTex("Models/Anime_Texture.png", 0xFFE8D5C8.toInt())
        }.onFailure { OmniLog.e("Preview", "setup failed", it) }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)


        Matrix.perspectiveM(proj, 0, 34f, width.toFloat() / height.coerceAtLeast(1), 0.1f, 40f)
    }




    private fun updateView() {
        val d = distance.coerceIn(MIN_DIST, MAX_DIST)
        val far = ((d - MIN_DIST) / (MAX_DIST - MIN_DIST)).coerceIn(0f, 1f)
        val targetY = 1.38f - 0.46f * far
        val p = Math.toRadians(pitchDegrees.toDouble())


        val eyeY = (targetY + (sin(p) * d).toFloat()).coerceAtLeast(0.22f)
        val eyeZ = (cos(p) * d).toFloat()
        Matrix.setLookAtM(view, 0, 0f, eyeY, eyeZ, 0f, targetY, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(vp, 0, proj, 0, view, 0)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        if (program == 0) return
        val t = (System.nanoTime() - start) / 1_000_000_000f
        updateView()
        GLES30.glUseProgram(program)
        GLES30.glUniform1f(uTime, t)


        Matrix.setIdentityM(model, 0)
        GLES30.glUniformMatrix4fv(uMVP, 1, false, vp, 0)
        GLES30.glUniformMatrix4fv(uModel, 1, false, model, 0)
        GLES30.glUniform1f(uIsChar, 0f)

        GLES30.glUniform1f(uAnimate, 0f)


        GLES30.glUniform3f(uSubject, 0f, 0f, 0f)
        drawIndexed(roomVbo, roomIbo, floorCount, charTex)
        drawIndexed(wallVbo, wallIbo, wallCount, charTex)



        if (charCount > 0) {
            Matrix.setIdentityM(model, 0)
            Matrix.rotateM(model, 0, yawDegrees, 0f, 1f, 0f)
            Matrix.scaleM(model, 0, 1.7f, 1.7f, 1.7f)
            Matrix.multiplyMM(mvp, 0, vp, 0, model, 0)
            GLES30.glUniformMatrix4fv(uMVP, 1, false, mvp, 0)
            GLES30.glUniformMatrix4fv(uModel, 1, false, model, 0)
            GLES30.glUniform1f(uIsChar, 1f)
            GLES30.glUniform1f(uAnimate, 1f)



            pose.build(t, walkAmount, 0f, 0f, 0f, 0f, 0f)
            GLES30.glUniformMatrix4fv(uBones, Skeleton.BONES, false, pose.matrices, 0)
            drawIndexed(charVbo, charIbo, charCount, charTex, skinned = true)
        }
    }




    private fun drawIndexed(vbo: Int, ibo: Int, count: Int, tex: Int, skinned: Boolean = false) {
        if (count <= 0) return
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex)
        GLES30.glUniform1i(uTex, 0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo)
        val stride = (if (skinned) CharacterMesh.FLOATS_PER_VERTEX else 8) * 4
        GLES30.glEnableVertexAttribArray(0); GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
        GLES30.glEnableVertexAttribArray(1); GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride, 3 * 4)
        GLES30.glEnableVertexAttribArray(2); GLES30.glVertexAttribPointer(2, 2, GLES30.GL_FLOAT, false, stride, 6 * 4)
        if (skinned) {
            GLES30.glEnableVertexAttribArray(3); GLES30.glVertexAttribPointer(3, 4, GLES30.GL_FLOAT, false, stride, 8 * 4)
            GLES30.glEnableVertexAttribArray(4); GLES30.glVertexAttribPointer(4, 4, GLES30.GL_FLOAT, false, stride, 12 * 4)
        }
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, ibo)
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, count, GLES30.GL_UNSIGNED_SHORT, 0)
        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
        GLES30.glDisableVertexAttribArray(2)
        if (skinned) { GLES30.glDisableVertexAttribArray(3); GLES30.glDisableVertexAttribArray(4) }
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


private class PreviewTurntable {

    var holdOff: Float = 0f
}


@Composable
fun TrailPreviewSheet(
    trailId   : String,
    isOwned   : Boolean,
    isEquipped: Boolean,
    onEquip   : () -> Unit,
    onClose   : () -> Unit
) {
    val spec = remember(trailId) {
        runCatching {
            val b = NativeBridge()
            val idx = (0 until b.trailCount()).firstOrNull { b.trailId(it) == trailId } ?: 0
            b.trailSpec(idx)
        }.getOrNull()
    }
    val tint = spec?.let { Color(it[0], it[1], it[2], 1f) } ?: Yellow
    val lifetime = spec?.get(3) ?: 7f
    val markScale = spec?.get(4) ?: 0.30f
    val spread = spec?.get(5) ?: 1.6f
    val mark = spec?.get(6)?.toInt() ?: 0


    class Stamp(val x: Float, val y: Float, val ang: Float, val side: Float, val born: Float)

    val stamps = remember(trailId) { mutableStateListOf<Stamp>() }
    var clock by remember(trailId) { mutableFloatStateOf(0f) }

    LaunchedEffect(trailId) {
        stamps.clear()
        var last = withFrameNanos { it }
        var nextStep = 0f
        var side = 1f
        while (true) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1_000_000_000.0).toFloat().coerceIn(0f, 0.1f)
            last = now
            clock += dt



            if (clock >= nextStep) {
                nextStep = clock + 0.42f
                side = -side
                val t = clock * 0.32f
                val px = sin(t) * 0.34f
                val py = sin(t * 1.7f) * 0.26f

                val ang = kotlin.math.atan2(
                    (kotlin.math.cos(t * 1.7f) * 1.7f * 0.26f),
                    (kotlin.math.cos(t) * 0.34f)
                )
                stamps.add(Stamp(px, py, ang, side, clock))
            }

            while (stamps.isNotEmpty() && (clock - stamps[0].born) > lifetime) stamps.removeAt(0)
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {


            val r = size.minDimension * 0.62f
            drawCircle(
                Brush.radialGradient(
                    listOf(Color(0xFF2A2618), Color(0xFF14120C), Color.Black),
                    center = center, radius = r
                ),
                radius = r, center = center
            )
            val unit = size.minDimension
            stamps.forEach { st ->
                val age = ((clock - st.born) / lifetime).coerceIn(0f, 1f)
                val half = unit * markScale * 0.10f * (1f + (spread - 1f) * age)
                val fade = (1f - age) * (1f - age)
                if (fade <= 0.01f) return@forEach
                val cx = center.x + st.x * unit
                val cy = center.y + st.y * unit


                val ox = cos(st.ang) * unit * 0.035f * st.side
                val oy = -sin(st.ang) * unit * 0.035f * st.side
                drawTrailMark(
                    mark, Offset(cx + ox, cy + oy), half,
                    st.ang, tint.copy(alpha = (fade * 0.9f).coerceIn(0f, 1f)), clock
                )
            }
        }

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
                stringResource(R.string.trail_preview_title),
                color = Yellow, fontSize = 13.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 2.sp
            )
        }
        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(trailDisplayName(trailId), color = tint, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.trail_preview_hint),
                color = TextDim, fontSize = 10.sp, letterSpacing = 1.sp
            )
            Spacer(Modifier.height(10.dp))



            when {
                isEquipped -> Text(
                    stringResource(R.string.trail_equipped),
                    color = SuccessGreen, fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                )
                isOwned -> AtmosphericButton(
                    stringResource(R.string.trail_equip),
                    Icons.Default.Check, tint, 170.dp, 44.dp, onEquip
                )
                else -> Text(
                    stringResource(R.string.trail_locked),
                    color = TextDim, fontSize = 11.sp, letterSpacing = 1.sp
                )
            }
        }
    }
}


private fun DrawScope.drawTrailMark(
    mark: Int, at: Offset, half: Float, ang: Float, colour: Color, t: Float
) {
    when (mark) {

        0 -> {
            val fx = cos(ang); val fy = -sin(ang)
            fun along(d: Float, w: Float, h: Float) {
                drawOval(
                    colour,
                    topLeft = Offset(at.x + fx * d - w, at.y + fy * d - h),
                    size = Size(w * 2f, h * 2f)
                )
            }
            along(half * 0.30f, half * 0.52f, half * 0.60f)
            along(-half * 0.44f, half * 0.40f, half * 0.34f)
        }

        1 -> {
            val rows = 6
            for (i in 0 until rows) {
                val fy = (i / (rows - 1f)) * 2f - 1f
                val j = (sin(t * 9f + i * 2.3f) * 0.35f)
                if (sin(t * 13f + i * 5.1f) < -0.2f) continue
                drawRect(
                    colour,
                    topLeft = Offset(at.x - half * 0.7f + j * half, at.y + fy * half * 0.8f),
                    size = Size(half * 1.4f, half * 0.22f)
                )
            }
        }

        else -> {
            for (i in 0 until 7) {
                val a = i * 2.399f
                val d = half * (0.15f + 0.55f * ((i * 37 % 11) / 11f))
                drawCircle(
                    colour,
                    radius = half * 0.16f,
                    center = Offset(at.x + cos(a) * d, at.y + sin(a) * d)
                )
            }
        }
    }
}

@Composable
fun CharacterPreviewSheet(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val renderer = remember { CharacterPreviewRenderer(ctx.applicationContext) }
    var walking by remember { mutableStateOf(false) }
    val walkAnim by animateFloatAsState(
        if (walking) 1f else 0f, tween(420, easing = EaseInOutCubic), label = "previewWalk"
    )




    val turntable = remember { PreviewTurntable() }






    LaunchedEffect(renderer) {
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1_000_000_000.0).toFloat().coerceIn(0f, 0.1f)
            last = now


            renderer.yawDegrees = renderer.yawDegrees.mod(360f)
            if (turntable.holdOff > 0f) {
                turntable.holdOff -= dt
            } else {
                renderer.yawDegrees -= dt * 11f


                val k = 1f - kotlin.math.exp(-dt * 1.6f)
                renderer.pitchDegrees += (7f - renderer.pitchDegrees) * k
                renderer.distance += (3.3f - renderer.distance) * k
            }
        }
    }
    SideEffect { renderer.walkAmount = walkAnim }

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


                detectTransformGestures { _, pan, zoom, _ ->
                    turntable.holdOff = 2.5f






                    renderer.yawDegrees += pan.x * 0.4f



                    renderer.pitchDegrees = (renderer.pitchDegrees + pan.y * 0.14f)
                        .coerceIn(CharacterPreviewRenderer.MIN_PITCH, CharacterPreviewRenderer.MAX_PITCH)
                    if (zoom != 0f) {
                        renderer.distance = (renderer.distance / zoom)
                            .coerceIn(CharacterPreviewRenderer.MIN_DIST, CharacterPreviewRenderer.MAX_DIST)
                    }
                }
            }
        )

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


@HiltViewModel
class AppLocaleVM @Inject constructor(private val locales: LocaleStore) : ViewModel() {
    val language: StateFlow<AppLanguage> = locales.observeSelection()
        .map { sel ->
            if (sel == AppLanguage.SYSTEM) AppLanguage.matchDevice()
            else AppLanguage.fromTag(sel) ?: AppLanguage.matchDevice()
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppLanguage.matchDevice())
}


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


class NativeBridge @Inject constructor() {
    external fun initCore(seed: Long)
    external fun getFlicker(phase: Float, t: Float, broken: Boolean): Float
    external fun generateLevel(count: Int, depth: Int): FloatArray?
    external fun generateChunk(chunkX: Int, chunkZ: Int): FloatArray?
    external fun getMoistureAt(x: Float, y: Float): Float
    external fun applyVhs(bitmap: Bitmap, t: Float, intensity: Float): Boolean
    external fun applyFlicker(bitmap: Bitmap, value: Float)
    external fun setPlayerState(x: Float, y: Float, z: Float, yaw: Float, pitch: Float)
    external fun physicsTick(dt: Float)
    external fun applyMovement(fx: Float, fy: Float, fz: Float)
    external fun setCrouch(crouched: Boolean)




    external fun frameCount(): Int
    external fun frameId(index: Int): String?

    external fun frameSpec(index: Int): FloatArray?

    external fun frameProfile(index: Int, samples: Int): FloatArray?

    external fun frameEmission(index: Int, samples: Int, t: Float): FloatArray?

    external fun trailCount(): Int
    external fun trailId(index: Int): String?

    external fun trailSpec(index: Int): FloatArray?
    external fun trailSetStyle(index: Int)
    external fun trailStep(x: Float, z: Float, yaw: Float, side: Float)
    external fun trailUpdate(dt: Float)
    external fun trailClear()

    external fun trailCollect(): FloatArray?


    external fun relocateExit(px: Float, pz: Float, maxDistM: Float): FloatArray?
    external fun cameraLook(dx: Float, dy: Float, sensitivity: Float)
    external fun getCameraState(): FloatArray?
    external fun destroyCore()
    external fun initSound(): Boolean
    external fun setMasterVolume(v: Float)
    external fun setHumVolume(v: Float)
    external fun setFootstepVolume(v: Float)
    external fun setMonsterVolume(v: Float)
    external fun setAmbienceLevel(v: Float)
    external fun triggerFootstep(bpm: Float, surface: Float)
    external fun stopFootstep()
    external fun playTorchClick()

    external fun endingParams(kind: Int, t: Float): FloatArray?
    external fun endingDuration(kind: Int): Float
    external fun triggerMonster(intensity: Float)

    external fun playIntroSting(seconds: Float)
    external fun stopIntroSting()
    external fun stopMonster()
    external fun setListenerPos(x: Float, y: Float, z: Float)
    external fun setSpatialRolloff(ref: Float, maxDist: Float)
    external fun destroySound()
    external fun initEntities()
    external fun spawnEntity(x: Float, y: Float, z: Float, speed: Float, hear: Float, sight: Float, aggro: Float, typeId: Int): Int



    external fun tickEntities(
        px: Float, py: Float, pz: Float, dt: Float,
        noise: Float, torchX: Float, torchZ: Float, torchOn: Boolean
    ): FloatArray?
    external fun damageEntity(id: Int, amount: Float)
    external fun getTotalFlickerInfluence(): Float
    external fun destroyEntities()
    external fun nowMs(): Long
    external fun initGuard(ctx: Any, expectedSigHash: String): Boolean
    external fun getGuardFlags(): Int
    external fun runGuardScan(): Int
    external fun isRooted(): Boolean
    external fun isFridaDetected(): Boolean
    external fun isDebugged(): Boolean
    external fun isEmulator(): Boolean
    external fun isSignatureValid(): Boolean
    external fun getThreatReport(): String
    external fun destroyGuard()
}

data class PlayerProfile(
    val id            : String  = "",
    val name          : String  = "Wanderer",
    val level         : Int     = 1,
    val xp            : Long    = 0L,
    val xpToNext      : Long    = 1_000L,
    val xpProgress    : Float   = 0f,
    val avatarUrl     : String? = null,
    val avatarId      : String  = "default",
    val omniumAmount  : Long    = 0L,
    val souliumAmount : Long    = 0L,
    val isVip         : Boolean = false,
    val vipExpiresMs  : Long    = 0L,
    val totalGames    : Int     = 0,
    val totalSurvived : Int     = 0,
    val highScore     : Long    = 0L,
    val equippedCharId: String  = "wanderer",
    val region        : String  = "TR",
    val createdAtMs   : Long    = System.currentTimeMillis()
)

data class GameSettings(
    val playerName        : String  = "Wanderer",
    val graphicsQuality   : String  = "medium",





    val vhsEnabled        : Boolean = false,
    val resolutionScale   : Float   = 1f,
    val musicVolume       : Float   = 0.7f,
    val footstepVolume    : Float   = 0.8f,
    val monsterVolume     : Float   = 0.9f,
    val voiceVolume       : Float   = 0.8f,
    val cameraSensitivity : Float   = 1f,
    val fpsLimit          : Int     = 60,
    val shadowsEnabled    : Boolean = true,
    val antialiasingOn    : Boolean = true,
    val fogEnabled        : Boolean = true,
    val vibrationOn       : Boolean = true,
    val showFps           : Boolean = false,
    val colorBlindMode    : String  = "none",

    val cameraView        : String  = "first",
    val pushNotifications : Boolean = true
)

data class UiButtonLayout(val buttonId: String, val offset: Offset, val sizeScale: Float = 1f)


enum class SpawnPhase { FALLING, LANDED, READY }

data class GameState(
    val level             : Int     = 0,
    val seed              : Long    = 0L,
    val difficulty        : String  = "normal",
    val playerHp          : Float   = 100f,
    val playerMaxHp       : Float   = 100f,
    val sanity            : Float   = 100f,
    val stamina           : Float   = 100f,
    val staminaMax        : Float   = 100f,
    val flashlightOn      : Boolean = true,
    val flashlightBattery : Float   = 1f,
    val sessionElapsed    : Long    = 0L,
    val entitiesNearby    : Int     = 0,
    val flickerIntensity  : Float   = 0f,
    val score             : Long    = 0L,
    val kills             : Int     = 0,
    val mapId             : String  = "level_0",
    val isPaused          : Boolean = false,
    val isGameOver        : Boolean = false,
    val isEscaped         : Boolean = false,
    val camera            : CameraSnapshot?      = null,
    val entities          : List<EntityState>    = emptyList(),
    val world             : WorldInfo = WorldInfo.EMPTY,
    val exitX             : Float   = 0f,
    val exitZ             : Float   = 0f,
    val distanceToExit    : Float   = Float.MAX_VALUE,
    val spawnPhase        : SpawnPhase = SpawnPhase.READY,

    val fps               : Int     = 0,


    val eyeOffset         : Float   = 0f,
    val isCrouching       : Boolean = false,
    val isSprinting       : Boolean = false,


    val madness           : Float   = 0f,


    val isMadnessOver     : Boolean = false,

    val cameraTilt        : Float   = 0f,

    val omniumEarned      : Long    = 0L,



    val endingElapsed     : Float   = 0f,



    val endingPanel       : Float   = 0f
)

data class LeaderboardEntry(
    val rank       : Int,
    val playerId   : Int,
    val playerName : String,
    val avatarUrl  : String?,
    val level      : Int,
    val score      : Long,
    val survived   : Int,
    val difficulty : String,
    val region     : String = "TR"
)

data class CameraSnapshot(
    val posX     : Float,
    val posY     : Float,
    val posZ     : Float,
    val yaw      : Float,
    val pitch    : Float,
    val roll     : Float,
    val fov      : Float,
    val bobAmount: Float,
    val bobPhase : Float,


    val eyeHeight: Float = 1.7f
) {
    companion object {
        fun fromFloatArray(data: FloatArray?): CameraSnapshot? {
            if (data == null || data.size < 9) return null
            return CameraSnapshot(
                data[0], data[1], data[2], data[3], data[4], data[5], data[6], data[7], data[8],
                if (data.size > 9) data[9] else 1.7f
            )
        }
    }
}

data class EntityState(
    val id              : Int,
    val posX            : Float,
    val posY            : Float,
    val posZ            : Float,
    val aiState         : Int,
    val alertLevel      : Float,
    val hpFraction      : Float,
    val flickerInfluence: Float,
    val playerInSight   : Boolean,
    val typeId          : Int,
    val isActive        : Boolean,



    val dissolve        : Float = 0f
) {

    val isAway: Boolean get() = dissolve >= 0.999f

    companion object {
        const val FLOATS_PER_ENTITY = 11

        fun fromFloatArray(data: FloatArray, index: Int, id: Int): EntityState? {
            val base = index * FLOATS_PER_ENTITY
            if (base + (FLOATS_PER_ENTITY - 1) >= data.size) return null
            return EntityState(
                id, data[base], data[base+1], data[base+2], data[base+3].toInt(),
                data[base+4], data[base+5], data[base+6], data[base+7] > 0.5f,
                data[base+8].toInt(), data[base+9] > 0.5f, data[base+10]
            )
        }

        fun listFromFloatArray(data: FloatArray?): List<EntityState> {
            if (data == null || data.isEmpty()) return emptyList()
            val count = data.size / FLOATS_PER_ENTITY
            return (0 until count).mapNotNull { fromFloatArray(data, it, it) }
        }
    }
}

data class MarketItemDto(val id: String, val nameTr: String, val nameEn: String, val descTr: String, val descEn: String, val category: String, val price: Long, val currency: String, val imageUrl: String?, val isOwned: Boolean, val isEquipped: Boolean, val isLimited: Boolean, val expiresMs: Long?)
data class CharacterDto(val id: String, val nameTr: String, val nameEn: String, val clazz: String, val maxHp: Float, val baseSpeed: Float, val stealthMult: Float, val staminaMult: Float, val abilities: List<String>, val isUnlocked: Boolean, val isEquipped: Boolean, val imageUrl: String?, val price: Long, val currency: String)
data class StoryChapterDto(val id: Int, val titleTr: String, val titleEn: String, val contentTr: String, val contentEn: String, val isUnlocked: Boolean)


val StoryChapterDto.displayTitle: String
    get() = if (java.util.Locale.getDefault().language == "en") titleEn else titleTr
val StoryChapterDto.displayContent: String
    get() = if (java.util.Locale.getDefault().language == "en") contentEn else contentTr


private fun pickRingPoint(world: WorldInfo, aroundX: Float, aroundZ: Float, minDist: Float): Pair<Float, Float> {
    val angle = Math.random() * Math.PI * 2
    val dist = minDist + Math.random().toFloat() * 26f
    return (aroundX + (kotlin.math.cos(angle) * dist).toFloat()) to
           (aroundZ + (kotlin.math.sin(angle) * dist).toFloat())
}


fun spawnInitialEntities(bridge: NativeBridge, world: WorldInfo, cfg: SpawnConfig) {
    if (!world.isValid) return
    val entity = EntityType.SMILER
    repeat(cfg.count) {
        val (sx, sz) = pickRingPoint(world, world.spawnX, world.spawnZ, minDist = 24f)
        bridge.spawnEntity(
            x = sx, y = 0f, z = sz,
            speed = entity.baseSpeed * cfg.speedMult,
            hear  = entity.hearRange,
            sight = entity.sightRange * cfg.sightMult,
            aggro = entity.aggroRange, typeId = entity.nativeAiId
        )
    }
}


fun spawnOneRandomEntity(bridge: NativeBridge, world: WorldInfo, aroundX: Float, aroundZ: Float, cfg: SpawnConfig) {
    if (!world.isValid) return
    val (sx, sz) = pickRingPoint(world, aroundX, aroundZ, minDist = 16f)
    val entity = EntityType.SMILER
    bridge.spawnEntity(
        x = sx, y = 0f, z = sz,
        speed = entity.baseSpeed * cfg.speedMult,
        hear  = entity.hearRange, sight = entity.sightRange * cfg.sightMult,
        aggro = entity.aggroRange, typeId = entity.nativeAiId
    )
}


data class TickDerived(
    val camera     : CameraSnapshot?,
    val entities   : List<EntityState>,
    val flicker    : Float,
    val nearbyCount: Int,
    val damage     : Float
)


data class PlayerSense(
    val noise  : Float,
    val torchX : Float,
    val torchZ : Float,
    val torchOn: Boolean
) {
    companion object {



        private const val BOB_TO_METRES_PER_SECOND = 25f
        private const val SPRINT_SPEED = 6f

        fun from(state: GameState, cam: CameraSnapshot?): PlayerSense {
            val speed = ((cam?.bobAmount ?: 0f) * BOB_TO_METRES_PER_SECOND)
                .coerceIn(0f, SPRINT_SPEED)
            val effort = speed / SPRINT_SPEED
            val noise = if (state.isCrouching) 0.05f + effort * 0.14f
                        else                   0.20f + effort * 0.80f




            val yaw = Math.toRadians((cam?.yaw ?: 0f).toDouble())
            return PlayerSense(
                noise,
                kotlin.math.sin(yaw).toFloat(),
                kotlin.math.cos(yaw).toFloat(),
                state.flashlightOn && state.flashlightBattery > 0f
            )
        }
    }
}


fun stepSimulation(bridge: NativeBridge, dt: Float, state: GameState): TickDerived {
    bridge.physicsTick(dt)
    val cam = CameraSnapshot.fromFloatArray(bridge.getCameraState())
    if (cam != null) bridge.setListenerPos(cam.posX, cam.posY, cam.posZ)
    val sense = PlayerSense.from(state, cam)
    val entityList = EntityState.listFromFloatArray(
        bridge.tickEntities(
            cam?.posX ?: 0f, cam?.posY ?: 0f, cam?.posZ ?: 0f, dt,
            sense.noise, sense.torchX, sense.torchZ, sense.torchOn
        )
    )
    val flicker = bridge.getTotalFlickerInfluence()
    val nearbyCount = entityList.count { e ->



        if (!e.isActive || e.isAway || cam == null) return@count false
        val dx = e.posX - cam.posX; val dz = e.posZ - cam.posZ
        dx * dx + dz * dz < 625f
    }

    var damage = 0f
    if (cam != null) {
        for (e in entityList) {
            if (!e.isActive || e.aiState != 4) continue
            val dx = e.posX - cam.posX; val dz = e.posZ - cam.posZ
            if (dx * dx + dz * dz < 2.25f) damage += 16f * dt
        }
    }
    return TickDerived(cam, entityList, flicker, nearbyCount, damage)
}


fun applyTickToState(s: GameState, derived: TickDerived, dt: Float, elapsedMs: Long, score: Long): GameState {
    val drain = (derived.nearbyCount * 0.6f + derived.flicker * 2f) * dt
    val regen = if (derived.nearbyCount == 0 && derived.flicker < 0.1f) dt * 0.3f else 0f
    val nb    = (s.flashlightBattery - (if (s.flashlightOn) dt * 0.006f else 0f)).coerceAtLeast(0f)
    val newHp = (s.playerHp - derived.damage).coerceIn(0f, s.playerMaxHp)
    val cam   = derived.camera
    val exitDist = if (cam != null)
        kotlin.math.hypot((s.exitX - cam.posX).toDouble(), (s.exitZ - cam.posZ).toDouble()).toFloat()
    else s.distanceToExit
    return s.copy(
        sessionElapsed    = elapsedMs,
        flickerIntensity  = derived.flicker,
        entitiesNearby    = derived.nearbyCount,
        score             = score,
        sanity            = (s.sanity - drain + regen).coerceIn(0f, 100f),
        flashlightBattery = nb,
        flashlightOn      = if (!s.flashlightOn) false else nb > 0f,
        stamina           = (s.stamina + dt * 8f).coerceAtMost(s.staminaMax),
        playerHp          = newHp,
        isGameOver        = newHp <= 0f || s.isGameOver,
        camera            = cam ?: s.camera,
        entities          = derived.entities,
        distanceToExit    = exitDist
    )
}

@AndroidEntryPoint
class SessionService : Service() {

    inner class LocalBinder : Binder() { fun get(): SessionService = this@SessionService }

    @Inject lateinit var bridge      : NativeBridge
    @Inject lateinit var assetManager: AssetManager
    @Inject lateinit var settings    : SettingsRepository

    private val binder = LocalBinder()
    private val scope  = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _gameState      = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private var physicsJob: Job? = null
    private var entityJob : Job? = null
    private var scoreJob  : Job? = null

    private var lastTickMs = 0L
    private var elapsedMs  = 0L
    private var score      = 0L
    private var kills      = 0


    private var world: WorldInfo = WorldInfo.EMPTY

    companion object {
        private const val CHANNEL_ID    = "omni_session"
        private const val NOTIF_ID      = 2001
        const val ACTION_START_OFFLINE  = "start_offline"
        const val ACTION_STOP           = "stop_game"
        const val ACTION_PAUSE          = "pause_game"
        const val ACTION_RESUME         = "resume_game"
        const val ACTION_FLASHLIGHT     = "flashlight"
        const val ACTION_DAMAGE_ENTITY  = "damage_entity"
        const val EXTRA_DIFFICULTY      = "difficulty"
        const val EXTRA_SEED            = "seed"
        const val EXTRA_MAP_ID          = "map_id"
        const val EXTRA_ENTITY_ID       = "entity_id"
        const val EXTRA_DAMAGE          = "damage"
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()





        ServiceCompat.startForeground(
            this,
            NOTIF_ID,
            buildNotif(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            else 0
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_OFFLINE -> {
                val diff  = intent.getStringExtra(EXTRA_DIFFICULTY) ?: "normal"
                val seed  = intent.getLongExtra(EXTRA_SEED, System.currentTimeMillis())
                val mapId = intent.getStringExtra(EXTRA_MAP_ID) ?: "level_0"
                startOffline(diff, seed, mapId)
            }
            ACTION_STOP       -> stopSession()
            ACTION_PAUSE      -> _gameState.update { it.copy(isPaused = true) }
            ACTION_RESUME     -> _gameState.update { it.copy(isPaused = false) }
            ACTION_FLASHLIGHT -> _gameState.update { it.copy(flashlightOn = !it.flashlightOn) }
            ACTION_DAMAGE_ENTITY -> {
                val id     = intent.getIntExtra(EXTRA_ENTITY_ID, -1)
                val damage = intent.getFloatExtra(EXTRA_DAMAGE, 10f)
                if (id >= 0) {
                    bridge.damageEntity(id, damage)
                    kills++; score += 100L
                    _gameState.update { it.copy(kills = kills, score = score) }
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder
    override fun onDestroy() { scope.cancel(); stopForeground(STOP_FOREGROUND_REMOVE); super.onDestroy() }

    private fun startOffline(difficulty: String, seed: Long, mapId: String) {
        scope.launch {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            bridge.initCore(seed)
            bridge.initSound()
            bridge.initEntities()
            bridge.setAmbienceLevel(0.4f)
            bridge.setHumVolume(0.3f)
            bridge.setSpatialRolloff(1f, 40f)


            val roomBudget = if (difficulty == "hard") 180 else 130
            world = WorldInfo.parse(bridge.generateLevel(roomBudget, depth = 0))

            val cfg = assetManager.getSpawnConfig(difficulty)
            spawnInitialEntities(bridge, world, cfg)
            _gameState.value = GameState(
                seed = seed, difficulty = difficulty, mapId = "level_0",
                world = world, exitX = world.exitX, exitZ = world.exitZ
            )
            startPhysicsLoop()
            startEntitySpawner(difficulty, cfg)
            startScoreAccumulator()
        }
    }

    private fun startScoreAccumulator() {
        scoreJob = scope.launch {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            while (isActive) {
                if (!_gameState.value.isPaused)
                    score += when (_gameState.value.difficulty) { "hard" -> 5L; "normal" -> 3L; else -> 1L }
                delay(1_000)
            }
        }
    }

    private fun startPhysicsLoop() {
        lastTickMs = bridge.nowMs()
        physicsJob = scope.launch {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            while (isActive) {
                if (_gameState.value.isPaused) { delay(16); continue }
                val now = bridge.nowMs()
                val dt  = ((now - lastTickMs).coerceIn(1, 100)).toFloat() / 1000f
                lastTickMs = now; elapsedMs += (dt * 1000).toLong()
                val wasGameOver = _gameState.value.isGameOver
                val derived = stepSimulation(bridge, dt, _gameState.value)
                _gameState.update { applyTickToState(it, derived, dt, elapsedMs, score) }
                if (!wasGameOver && _gameState.value.isGameOver) onGameOver()
                delay(16)
            }
        }
    }

    private fun startEntitySpawner(difficulty: String, cfg: SpawnConfig) {
        entityJob = scope.launch {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            var timer = 0L
            while (isActive) {
                delay(5_000); timer += 5_000
                if (timer >= cfg.spawnIntervalMs && world.isValid) {
                    timer = 0
                    val cam = _gameState.value.camera
                    spawnOneRandomEntity(bridge, world, cam?.posX ?: world.spawnX, cam?.posZ ?: world.spawnZ, cfg)
                }
            }
        }
    }


    fun applyDamage(amount: Float) {
        val s  = _gameState.value
        val hp = (s.playerHp - amount).coerceAtLeast(0f)
        _gameState.update { it.copy(playerHp = hp, isGameOver = hp <= 0f) }
        if (hp <= 0f) onGameOver()
    }

    fun heal(amount: Float) {
        val s = _gameState.value
        _gameState.update { it.copy(playerHp = (s.playerHp + amount).coerceAtMost(s.playerMaxHp)) }
    }

    fun consumeStamina(amount: Float) {
        _gameState.update { it.copy(stamina = (_gameState.value.stamina - amount).coerceAtLeast(0f)) }
    }

    private fun onGameOver() {
        bridge.triggerMonster(1.0f)
        physicsJob?.cancel(); entityJob?.cancel(); scoreJob?.cancel()
    }

    private fun stopSession() {
        onGameOver()
        scope.launch {
            bridge.destroyEntities()
            bridge.destroySound()
            bridge.destroyCore()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }




    private fun createChannel() {
        val wanted = runCatching {
            runBlocking { settings.observe().first() }.pushNotifications
        }.getOrDefault(true)
        val importance = if (wanted) NotificationManager.IMPORTANCE_LOW
                         else NotificationManager.IMPORTANCE_MIN
        val ch = NotificationChannel(CHANNEL_ID, getString(R.string.notif_channel_session), importance)
            .apply { setShowBadge(false) }
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    private fun buildNotif(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.loading_text))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setSilent(true)
            .build()
}


private val Context.identityStore: DataStore<Preferences> by preferencesDataStore(name = "omni_identity")

@Singleton
class GuestIdentityManager @Inject constructor(@ApplicationContext private val ctx: Context) {

    private object Keys {
        val NAME      = stringPreferencesKey("guest_name")
        val CREATED   = longPreferencesKey("guest_created_ms")
        val LAST_SEEN = longPreferencesKey("guest_last_seen_ms")
    }

    companion object {

        val INACTIVITY_LIMIT_MS = TimeUnit.DAYS.toMillis(7)
    }




    suspend fun currentName(): String {
        val now = System.currentTimeMillis()
        val prefs = ctx.identityStore.data.first()
        val existing = prefs[Keys.NAME]
        val lastSeen = prefs[Keys.LAST_SEEN] ?: 0L

        if (existing != null && now - lastSeen <= INACTIVITY_LIMIT_MS) {
            ctx.identityStore.edit { it[Keys.LAST_SEEN] = now }
            return existing
        }



        if (existing != null) SaveGameStore(ctx).clear()
        val minted = mintName()
        ctx.identityStore.edit {
            it[Keys.NAME] = minted
            it[Keys.CREATED] = now
            it[Keys.LAST_SEEN] = now
        }
        return minted
    }



    private fun mintName(): String {
        val rng = SecureRandom()
        val a = rng.nextInt(9000) + 1000
        val b = rng.nextInt(9000) + 1000
        return "Unknown Player $a-$b"
    }

    suspend fun touch() {
        ctx.identityStore.edit { it[Keys.LAST_SEEN] = System.currentTimeMillis() }
    }



    fun observeDisplayName(): Flow<String> = ctx.identityStore.data.map { it[Keys.NAME] ?: "" }

    suspend fun setDisplayName(name: String) {
        val clean = name.trim().take(24)
        if (clean.isEmpty()) return
        runCatching { ctx.identityStore.edit { it[Keys.NAME] = clean } }
    }
}


@Serializable
data class SavedRun(
    val seed        : Long,
    val difficulty  : String,
    val elapsedMs   : Long,
    val score       : Long,
    val kills       : Int,
    val sanity      : Float,
    val battery     : Float,
    val playerHp    : Float,
    val savedAtMs   : Long,


    val posX        : Float = 0f,
    val posY        : Float = 1.7f,
    val posZ        : Float = 0f,
    val yaw         : Float = 0f,
    val pitch       : Float = 0f
)

@Singleton
class SaveGameStore @Inject constructor(@ApplicationContext private val ctx: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val key  = stringPreferencesKey("saved_run")




    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun save(run: SavedRun) {
        runCatching { ctx.identityStore.edit { it[key] = json.encodeToString(run) } }
    }


    fun saveDetached(run: SavedRun) {
        ioScope.launch {
            runCatching { ctx.identityStore.edit { it[key] = json.encodeToString(run) } }
                .onSuccess { OmniLog.i("Save", "run saved elapsed=${run.elapsedMs}") }
                .onFailure { OmniLog.e("Save", "save failed", it) }
        }
    }

    suspend fun load(): SavedRun? = runCatching {
        ctx.identityStore.data.first()[key]?.let { json.decodeFromString<SavedRun>(it) }
    }.getOrNull()

    fun observeHasSave(): Flow<Boolean> = ctx.identityStore.data.map { it[key] != null }

    suspend fun clear() {
        runCatching { ctx.identityStore.edit { it.remove(key) } }
    }




    fun clearDetached() {
        ioScope.launch {
            runCatching { ctx.identityStore.edit { it.remove(key) } }
                .onSuccess { OmniLog.i("Save", "run cleared") }
                .onFailure { OmniLog.e("Save", "clear failed", it) }
        }
    }
}


object OmniLog {

    enum class Level { DEBUG, INFO, WARN, ERROR }

    private const val TAG = "OmniBackrooms"
    private const val RING_CAPACITY = 400
    private val ring = ArrayDeque<String>(RING_CAPACITY)
    private val lock = Any()
    private val stamp = java.text.SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    @Volatile private var sink: java.io.File? = null

    const val LOG_DIR_NAME = "Backrooms_Log"




    fun attach(ctx: Context) {
        val candidates = buildList {
            runCatching {
                add(java.io.File(
                    android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOCUMENTS), LOG_DIR_NAME))
            }
            runCatching { ctx.getExternalFilesDir(null) }.getOrNull()
                ?.let { add(java.io.File(it, LOG_DIR_NAME)) }
            add(java.io.File(ctx.filesDir, LOG_DIR_NAME))
        }

        for (dir in candidates) {
            val ok = runCatching {
                if (!dir.exists()) dir.mkdirs()
                val f = java.io.File(dir, "session.log")


                f.appendText("")
                if (f.length() > 512 * 1024) f.delete()
                sink = f
                true
            }.getOrElse { false }
            if (ok) break
        }

        i("Log", "attached; sink=${sink?.absolutePath ?: "none (in-memory only)"}")
    }


    fun sinkPath(): String? = sink?.absolutePath

    fun d(tag: String, msg: String) = write(Level.DEBUG, tag, msg, null)
    fun i(tag: String, msg: String) = write(Level.INFO,  tag, msg, null)
    fun w(tag: String, msg: String, t: Throwable? = null) = write(Level.WARN, tag, msg, t)
    fun e(tag: String, msg: String, t: Throwable? = null) = write(Level.ERROR, tag, msg, t)

    private fun write(level: Level, tag: String, msg: String, t: Throwable?) {
        val line = buildString {
            append(stamp.format(java.util.Date())); append(' ')
            append(level.name.first()); append('/')
            append(tag); append(": "); append(msg)
            if (t != null) {
                append('\n')
                append(java.io.StringWriter().also { sw -> t.printStackTrace(java.io.PrintWriter(sw)) })
            }
        }
        synchronized(lock) {
            if (ring.size >= RING_CAPACITY) ring.removeFirst()
            ring.addLast(line)
        }
        when (level) {
            Level.DEBUG -> Log.d(TAG, "[$tag] $msg")
            Level.INFO  -> Log.i(TAG, "[$tag] $msg")
            Level.WARN  -> Log.w(TAG, "[$tag] $msg", t)
            Level.ERROR -> Log.e(TAG, "[$tag] $msg", t)
        }
        runCatching { sink?.appendText(line + "\n") }
    }



    fun recentHistory(): String = synchronized(lock) { ring.joinToString("\n") }

    fun clearRing() = synchronized(lock) { ring.clear() }
}


@Singleton
class CosmeticsStore @Inject constructor(@ApplicationContext private val ctx: Context) {

    private object Keys {
        val AVATAR_URI   = stringPreferencesKey("avatar_uri")
        val FRAME        = stringPreferencesKey("frame")
        val OWNED_FRAMES = stringPreferencesKey("owned_frames")
        val TRAIL        = stringPreferencesKey("trail")
        val OWNED_TRAILS = stringPreferencesKey("owned_trails")
        val VIP          = stringPreferencesKey("vip")
        val BEST_SURVIVAL= longPreferencesKey("best_survival_ms")
        val OMNIUM       = longPreferencesKey("omnium_balance")
    }

    fun observeAvatarUri(): Flow<String?> = ctx.identityStore.data.map { it[Keys.AVATAR_URI] }




    fun observeFrame(): Flow<String> = ctx.identityStore.data.map {
        it[Keys.FRAME] ?: defaultCosmetic(frames = true)
    }
    fun observeTrail(): Flow<String> = ctx.identityStore.data.map {
        it[Keys.TRAIL] ?: defaultCosmetic(frames = false)
    }
    fun observeOwnedTrails(): Flow<List<String>> = ctx.identityStore.data.map { prefs ->
        prefs[Keys.OWNED_TRAILS]?.split(',')?.filter { it.isNotBlank() } ?: emptyList()
    }

    private fun defaultCosmetic(frames: Boolean): String = runCatching {
        val b = NativeBridge()
        (if (frames) b.frameId(0) else b.trailId(0))?.takeIf { it.isNotEmpty() }
    }.getOrNull() ?: if (frames) "Face_Of_Darkness" else "Dust_Trail"
    fun observeBestSurvival(): Flow<Long> = ctx.identityStore.data.map { it[Keys.BEST_SURVIVAL] ?: 0L }



    fun observeOmnium(): Flow<Long> = ctx.identityStore.data.map { it[Keys.OMNIUM] ?: 0L }

    suspend fun addOmnium(amount: Long) {
        if (amount <= 0L) return
        runCatching {
            ctx.identityStore.edit { prefs ->
                prefs[Keys.OMNIUM] = (prefs[Keys.OMNIUM] ?: 0L) + amount
            }
        }
    }

    fun observeOwnedFrames(): Flow<List<String>> = ctx.identityStore.data.map { prefs ->
        prefs[Keys.OWNED_FRAMES]?.split(',')?.filter { it.isNotBlank() } ?: emptyList()
    }

    suspend fun setAvatarUri(uri: String) {
        runCatching { ctx.identityStore.edit { it[Keys.AVATAR_URI] = uri } }
    }

    suspend fun setFrame(key: String) {
        runCatching { ctx.identityStore.edit { it[Keys.FRAME] = key } }
    }

    suspend fun grantFrame(key: String) = grant(Keys.OWNED_FRAMES, key)

    suspend fun setTrail(key: String) {
        runCatching { ctx.identityStore.edit { it[Keys.TRAIL] = key } }
    }




    fun observeVip(): Flow<Boolean> = ctx.identityStore.data.map { it[Keys.VIP] == "1" }

    suspend fun setVip(active: Boolean) {
        runCatching { ctx.identityStore.edit { it[Keys.VIP] = if (active) "1" else "0" } }
    }

    suspend fun grantTrail(key: String) = grant(Keys.OWNED_TRAILS, key)

    private suspend fun grant(key: Preferences.Key<String>, value: String) {
        runCatching {
            ctx.identityStore.edit { prefs ->
                val cur = prefs[key]?.split(',')?.filter { it.isNotBlank() }?.toMutableSet()
                    ?: mutableSetOf()
                cur.add(value)
                prefs[key] = cur.joinToString(",")
            }
        }
    }


    suspend fun recordSurvival(ms: Long) {
        runCatching {
            ctx.identityStore.edit { prefs ->
                val best = prefs[Keys.BEST_SURVIVAL] ?: 0L
                if (ms > best) prefs[Keys.BEST_SURVIVAL] = ms
            }
        }
    }
}


enum class AppLanguage(val tag: String, val endonym: String) {
    ENGLISH   ("en", "English"),
    TURKISH   ("tr", "Türkçe"),
    GERMAN    ("de", "Deutsch"),
    SPANISH   ("es", "Español"),
    FRENCH    ("fr", "Français"),
    ITALIAN   ("it", "Italiano"),
    PORTUGUESE("pt", "Português"),
    RUSSIAN   ("ru", "Русский"),
    JAPANESE  ("ja", "日本語"),
    CHINESE   ("zh", "中文");

    companion object {
        const val SYSTEM = "system"

        fun fromTag(tag: String?): AppLanguage? = entries.firstOrNull { it.tag == tag }




        fun matchDevice(): AppLanguage {
            val deviceTag = runCatching {
                val cfg = android.content.res.Resources.getSystem().configuration
                @Suppress("DEPRECATION")
                val loc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    cfg.locales.get(0) else cfg.locale
                loc.language.lowercase(Locale.ROOT)
            }.getOrElse { Locale.getDefault().language.lowercase(Locale.ROOT) }
            return fromTag(deviceTag) ?: ENGLISH
        }
    }
}

@Singleton
class LocaleStore @Inject constructor(@ApplicationContext private val ctx: Context) {
    private val key = stringPreferencesKey("app_language")


    fun observeSelection(): Flow<String> = ctx.identityStore.data.map { it[key] ?: AppLanguage.SYSTEM }

    suspend fun setSelection(value: String) {
        runCatching { ctx.identityStore.edit { it[key] = value } }
        OmniLog.i("Locale", "selection set to $value")
    }



    fun currentLanguageBlocking(): AppLanguage = runCatching {
        runBlocking { observeSelection().first() }
    }.getOrNull().let { sel ->
        if (sel == null || sel == AppLanguage.SYSTEM) AppLanguage.matchDevice()
        else AppLanguage.fromTag(sel) ?: AppLanguage.matchDevice()
    }
}


fun applyAppLanguage(base: Context, language: AppLanguage): Context {
    val locale = Locale.forLanguageTag(language.tag)
    Locale.setDefault(locale)
    return LocalisedContextWrapper(base, locale)
}


private class LocalisedContextWrapper(base: Context, locale: Locale) : ContextWrapper(base) {
    private val localisedResources: Resources by lazy {
        val config = Configuration(base.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        base.createConfigurationContext(config).resources
    }

    override fun getResources(): Resources = localisedResources
}


data class WorldInfo(
    val cellSize   : Float = 3.2f,
    val height     : Float = 2.6f,
    val spawnX     : Float = 0f,
    val spawnZ     : Float = 0f,
    val exitX      : Float = 0f,
    val exitZ      : Float = 0f,
    val chunkCells : Int   = 24
) {
    val isValid: Boolean get() = chunkCells > 0

    companion object {
        val EMPTY = WorldInfo(chunkCells = 0)

        fun parse(data: FloatArray?): WorldInfo {
            if (data == null || data.size < 8) return EMPTY
            return WorldInfo(
                cellSize = data[0], height = data[1],
                spawnX = data[2], spawnZ = data[3],
                exitX = data[4], exitZ = data[5],
                chunkCells = data[6].toInt()
            )
        }
    }
}


class WorldChunk(
    val chunkX: Int,
    val chunkZ: Int,
    val cells: Int,
    private val solid  : ByteArray,
    private val light  : FloatArray,
    private val feature: ByteArray,
    private val fixture: ByteArray,
    private val power  : FloatArray
) {



    private val stride = cells + 2

    private fun index(x: Int, z: Int): Int = (z + 1) * stride + (x + 1)
    private fun inRange(x: Int, z: Int): Boolean = x >= -1 && z >= -1 && x <= cells && z <= cells

    fun solidAt(x: Int, z: Int): Boolean =
        if (!inRange(x, z)) true else solid[index(x, z)] != 0.toByte()


    fun lightAt(x: Int, z: Int): Float =
        if (!inRange(x, z)) 0.6f else light[index(x, z)]


    fun powerAt(x: Int, z: Int): Float =
        if (!inRange(x, z)) 1f else power[index(x, z)]

    fun featureAt(x: Int, z: Int): Int =
        if (!inRange(x, z)) 0 else feature[index(x, z)].toInt()

    fun fixtureAt(x: Int, z: Int): Int =
        if (!inRange(x, z)) 0 else fixture[index(x, z)].toInt()

    companion object {
        const val FLOATS_PER_CELL = 5

        fun parse(chunkX: Int, chunkZ: Int, cells: Int, data: FloatArray?): WorldChunk? {
            if (data == null || cells <= 0) return null
            val padded = cells + 2
            val n = padded * padded
            if (data.size < n * FLOATS_PER_CELL) return null
            val solid = ByteArray(n); val light = FloatArray(n)
            val feature = ByteArray(n); val fixture = ByteArray(n)
            val power = FloatArray(n)
            var p = 0
            for (i in 0 until n) {
                solid[i]   = data[p].toInt().toByte()
                light[i]   = data[p + 1]
                feature[i] = data[p + 2].toInt().toByte()
                fixture[i] = data[p + 3].toInt().toByte()
                power[i]   = data[p + 4]
                p += FLOATS_PER_CELL
            }
            return WorldChunk(chunkX, chunkZ, cells, solid, light, feature, fixture, power)
        }
    }
}


@Singleton
class SettingsRepository @Inject constructor(
    private val store : DataStore<Preferences>,
    private val bridge: NativeBridge
) {
    companion object {
        val KEY_NAME         = stringPreferencesKey("player_name")
        val KEY_QUALITY      = stringPreferencesKey("graphics_quality")
        val KEY_VHS          = booleanPreferencesKey("vhs_enabled")
        val KEY_RESOLUTION   = floatPreferencesKey("resolution_scale")
        val KEY_MUSIC        = floatPreferencesKey("music_volume")
        val KEY_FOOTSTEP     = floatPreferencesKey("footstep_volume")
        val KEY_MONSTER      = floatPreferencesKey("monster_volume")
        val KEY_VOICE        = floatPreferencesKey("voice_volume")
        val KEY_SENSITIVITY  = floatPreferencesKey("camera_sensitivity")
        val KEY_FPS_LIMIT    = intPreferencesKey("fps_limit")
        val KEY_SHADOWS      = booleanPreferencesKey("shadows_enabled")
        val KEY_ANTIALIASING = booleanPreferencesKey("antialiasing")
        val KEY_FOG          = booleanPreferencesKey("fog_enabled")
        val KEY_VIBRATION    = booleanPreferencesKey("vibration")
        val KEY_PUSH_NOTIF   = booleanPreferencesKey("push_notifications")
        val KEY_SHOW_FPS     = booleanPreferencesKey("show_fps")
        val KEY_COLOR_BLIND  = stringPreferencesKey("color_blind_mode")

        val KEY_CAMERA_VIEW  = stringPreferencesKey("camera_view")




        const val VHS_DEFAULT = false

        const val FOG_DEFAULT = false
    }

    fun observe(): Flow<GameSettings> = store.data.map { p ->
        GameSettings(
            playerName        = p[KEY_NAME]         ?: "Wanderer",
            graphicsQuality   = p[KEY_QUALITY]      ?: "medium",
            vhsEnabled        = p[KEY_VHS]          ?: VHS_DEFAULT,
            resolutionScale   = p[KEY_RESOLUTION]   ?: 1f,
            musicVolume       = p[KEY_MUSIC]        ?: 0.7f,
            footstepVolume    = p[KEY_FOOTSTEP]     ?: 0.8f,
            monsterVolume     = p[KEY_MONSTER]      ?: 0.9f,
            voiceVolume       = p[KEY_VOICE]        ?: 0.8f,
            cameraSensitivity = (p[KEY_SENSITIVITY] ?: 1f).let { if (it <= 0f) 1f else it },
            fpsLimit          = p[KEY_FPS_LIMIT]    ?: 60,
            shadowsEnabled    = p[KEY_SHADOWS]      ?: true,
            antialiasingOn    = p[KEY_ANTIALIASING] ?: true,
            fogEnabled        = p[KEY_FOG]          ?: FOG_DEFAULT,
            vibrationOn       = p[KEY_VIBRATION]    ?: true,
            showFps           = p[KEY_SHOW_FPS]     ?: false,
            colorBlindMode    = p[KEY_COLOR_BLIND]  ?: "none",
            cameraView        = p[KEY_CAMERA_VIEW]  ?: "first",
            pushNotifications = p[KEY_PUSH_NOTIF]   ?: true
        )
    }





    fun observeVhs()    : Flow<Boolean> = store.data.map { it[KEY_VHS]     ?: VHS_DEFAULT }
    fun observeMusic()  : Flow<Float>   = store.data.map { it[KEY_MUSIC]   ?: 0.7f     }
    fun observeVoice()  : Flow<Float>   = store.data.map { it[KEY_VOICE]   ?: 0.8f     }
    fun observeQuality(): Flow<String>  = store.data.map { it[KEY_QUALITY] ?: "medium" }

    suspend fun saveName(v: String)          { store.edit { it[KEY_NAME]         = v } }
    suspend fun saveQuality(v: String)       { store.edit { it[KEY_QUALITY]      = v } }
    suspend fun saveVhs(v: Boolean)          { store.edit { it[KEY_VHS]          = v } }
    suspend fun saveResolution(v: Float)     { store.edit { it[KEY_RESOLUTION]   = v } }
    suspend fun saveMusic(v: Float)          { store.edit { it[KEY_MUSIC]        = v }; withContext(Dispatchers.Main) { bridge.setMasterVolume(v) } }
    suspend fun saveFootstep(v: Float)       { store.edit { it[KEY_FOOTSTEP]     = v }; withContext(Dispatchers.Main) { bridge.setFootstepVolume(v) } }
    suspend fun saveMonster(v: Float)        { store.edit { it[KEY_MONSTER]      = v }; withContext(Dispatchers.Main) { bridge.setMonsterVolume(v) } }
    suspend fun saveVoice(v: Float)          { store.edit { it[KEY_VOICE]        = v } }
    suspend fun saveSensitivity(v: Float)    { store.edit { it[KEY_SENSITIVITY]  = v } }
    suspend fun saveFpsLimit(v: Int)         { store.edit { it[KEY_FPS_LIMIT]    = v } }
    suspend fun saveShadows(v: Boolean)      { store.edit { it[KEY_SHADOWS]      = v } }
    suspend fun saveAntialiasing(v: Boolean) { store.edit { it[KEY_ANTIALIASING] = v } }
    suspend fun saveFog(v: Boolean)          { store.edit { it[KEY_FOG]          = v } }
    suspend fun saveVibration(v: Boolean)    { store.edit { it[KEY_VIBRATION]    = v } }
    suspend fun saveShowFps(v: Boolean)      { store.edit { it[KEY_SHOW_FPS]     = v } }
    suspend fun saveColorBlind(v: String)    { store.edit { it[KEY_COLOR_BLIND]  = v } }
    suspend fun saveCameraView(v: String)    { store.edit { it[KEY_CAMERA_VIEW]  = v } }
    suspend fun savePushNotif(v: Boolean)    { store.edit { it[KEY_PUSH_NOTIF]   = v } }

    suspend fun saveUiLayout(layout: List<UiButtonLayout>) {
        store.edit { p ->
            layout.forEach { b ->
                p[floatPreferencesKey("ui_${b.buttonId}_x")] = b.offset.x
                p[floatPreferencesKey("ui_${b.buttonId}_y")] = b.offset.y


                p[floatPreferencesKey("ui_${b.buttonId}_s")] = b.sizeScale
            }
        }
    }




    fun observeUiLayout(): Flow<Map<String, UiButtonLayout>> = store.data.map { p ->
        val ids = p.asMap().keys
            .map { it.name }
            .filter { it.startsWith("ui_") && it.endsWith("_x") }
            .map { it.removePrefix("ui_").removeSuffix("_x") }
        ids.mapNotNull { id ->
            val x = p[floatPreferencesKey("ui_${id}_x")] ?: return@mapNotNull null
            val y = p[floatPreferencesKey("ui_${id}_y")] ?: return@mapNotNull null
            val sc = p[floatPreferencesKey("ui_${id}_s")] ?: 1f
            id to UiButtonLayout(id, Offset(x, y), sc)
        }.toMap()
    }

    suspend fun resetUiLayout() {
        store.edit { p ->
            p.asMap().keys.map { it.name }.filter { it.startsWith("ui_") }.forEach {
                p.remove(floatPreferencesKey(it))
            }
        }
    }

    suspend fun loadUiLayout(): List<UiButtonLayout> {
        val p   = store.data.first()
        val ids = listOf("joystick", "sprint", "interact", "crouch", "flashlight")
        return ids.mapNotNull { id ->
            val x = p[floatPreferencesKey("ui_${id}_x")] ?: return@mapNotNull null
            val y = p[floatPreferencesKey("ui_${id}_y")] ?: return@mapNotNull null
            UiButtonLayout(buttonId = id, offset = Offset(x, y))
        }
    }

    suspend fun clearAll() { store.edit { it.clear() } }

}

data class SettingsUiState(
    val playerName        : String          = "Wanderer",
    val graphicsQuality   : String          = "medium",



    val vhsEnabled        : Boolean         = false,
    val resolutionScale   : Float           = 1f,
    val musicVolume       : Float           = 0.7f,
    val footstepVolume    : Float           = 0.8f,
    val monsterVolume     : Float           = 0.9f,
    val voiceVolume       : Float           = 0.8f,
    val cameraSensitivity : Float           = 1f,
    val fpsLimit          : Int             = 60,
    val shadowsEnabled    : Boolean         = true,
    val antialiasingOn    : Boolean         = true,

    val fogEnabled        : Boolean         = false,
    val vibrationOn       : Boolean         = true,
    val showFps           : Boolean         = false,
    val colorBlindMode    : String          = "none",


    val cameraView        : String          = "first",
    val pushNotifications : Boolean         = true,
    val isSyncing         : Boolean         = false,
    val syncSuccess       : Boolean         = false,
)

@HiltViewModel
class SettingsVM @Inject constructor(
    private val repo             : SettingsRepository,
    private val identity         : GuestIdentityManager,
    private val locales          : LocaleStore
) : ViewModel() {


    val uiLayout: StateFlow<Map<String, UiButtonLayout>> = repo.observeUiLayout()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val languageSelection: StateFlow<String> = locales.observeSelection()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppLanguage.SYSTEM)



    fun onLanguage(value: String) { viewModelScope.launch { locales.setSelection(value) } }

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()




    private val pendingWrites = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            repo.observe().collect { g ->
                _state.update { cur ->


                    fun <T> pick(key: String, stored: T, local: T): T =
                        if (key in pendingWrites) {
                            if (stored == local) pendingWrites.remove(key)
                            local
                        } else stored

                    cur.copy(
                        playerName        = pick("name", g.playerName, cur.playerName),
                        graphicsQuality   = pick("quality", g.graphicsQuality, cur.graphicsQuality),
                        vhsEnabled        = pick("vhs", g.vhsEnabled, cur.vhsEnabled),
                        resolutionScale   = pick("res", g.resolutionScale, cur.resolutionScale),
                        musicVolume       = pick("music", g.musicVolume, cur.musicVolume),
                        footstepVolume    = pick("foot", g.footstepVolume, cur.footstepVolume),
                        monsterVolume     = pick("monster", g.monsterVolume, cur.monsterVolume),
                        voiceVolume       = pick("voice", g.voiceVolume, cur.voiceVolume),
                        cameraSensitivity = pick("sens", g.cameraSensitivity, cur.cameraSensitivity),
                        fpsLimit          = pick("fps", g.fpsLimit, cur.fpsLimit),
                        shadowsEnabled    = pick("shadows", g.shadowsEnabled, cur.shadowsEnabled),
                        antialiasingOn    = pick("aa", g.antialiasingOn, cur.antialiasingOn),
                        fogEnabled        = pick("fog", g.fogEnabled, cur.fogEnabled),
                        vibrationOn       = pick("vibe", g.vibrationOn, cur.vibrationOn),
                        showFps           = pick("showFps", g.showFps, cur.showFps),
                        colorBlindMode    = pick("cb", g.colorBlindMode, cur.colorBlindMode),
                        cameraView        = pick("camview", g.cameraView, cur.cameraView),
                        pushNotifications = pick("push", g.pushNotifications, cur.pushNotifications)
                    )
                }
            }
        }

        viewModelScope.launch {

            identity.observeDisplayName().collect { shared ->
                if (shared.isNotBlank()) _state.update { it.copy(playerName = shared) }
            }
        }

    }




    fun onName(v: String) {
        pendingWrites.add("name")
        _state.update { it.copy(playerName = v) }
        save { repo.saveName(v) }
        viewModelScope.launch { identity.setDisplayName(v) }
    }
    fun onQuality(v: String)       { pendingWrites.add("quality"); _state.update { it.copy(graphicsQuality   = v) }; save { repo.saveQuality(v) } }
    fun onVhs(v: Boolean)          { pendingWrites.add("vhs"); _state.update { it.copy(vhsEnabled        = v) }; save { repo.saveVhs(v) } }
    fun onResolution(v: Float)     { pendingWrites.add("res"); _state.update { it.copy(resolutionScale   = v) }; save { repo.saveResolution(v) } }
    fun onMusic(v: Float)          { pendingWrites.add("music"); _state.update { it.copy(musicVolume       = v) }; save { repo.saveMusic(v) } }
    fun onFootstep(v: Float)       { pendingWrites.add("foot"); _state.update { it.copy(footstepVolume    = v) }; save { repo.saveFootstep(v) } }
    fun onMonster(v: Float)        { pendingWrites.add("monster"); _state.update { it.copy(monsterVolume     = v) }; save { repo.saveMonster(v) } }
    fun onVoice(v: Float)          { pendingWrites.add("voice"); _state.update { it.copy(voiceVolume       = v) }; save { repo.saveVoice(v) } }
    fun onSensitivity(v: Float)    { pendingWrites.add("sens"); _state.update { it.copy(cameraSensitivity = v) }; save { repo.saveSensitivity(v) } }
    fun onFpsLimit(v: Int)         { pendingWrites.add("fps"); _state.update { it.copy(fpsLimit          = v) }; save { repo.saveFpsLimit(v) } }
    fun onShadows(v: Boolean)      { pendingWrites.add("shadows"); _state.update { it.copy(shadowsEnabled    = v) }; save { repo.saveShadows(v) } }
    fun onAntialiasing(v: Boolean) { pendingWrites.add("aa"); _state.update { it.copy(antialiasingOn    = v) }; save { repo.saveAntialiasing(v) } }
    fun onFog(v: Boolean)          { pendingWrites.add("fog"); _state.update { it.copy(fogEnabled        = v) }; save { repo.saveFog(v) } }
    fun onVibration(v: Boolean)    { pendingWrites.add("vibe"); _state.update { it.copy(vibrationOn       = v) }; save { repo.saveVibration(v) } }
    fun onShowFps(v: Boolean)      { pendingWrites.add("showFps"); _state.update { it.copy(showFps           = v) }; save { repo.saveShowFps(v) } }
    fun onCameraView(v: String)    { pendingWrites.add("camview"); _state.update { it.copy(cameraView = v) }; save { repo.saveCameraView(v) } }
    fun onColorBlind(v: String)    { pendingWrites.add("cb"); _state.update { it.copy(colorBlindMode    = v) }; save { repo.saveColorBlind(v) } }
    fun onPushNotif(v: Boolean)    { pendingWrites.add("push"); _state.update { it.copy(pushNotifications = v) }; save { repo.savePushNotif(v) } }

    fun syncToServer() {
        viewModelScope.launch {
            _state.update { it.copy(isSyncing = true, syncSuccess = false) }
            val s  = _state.value
            val gs = GameSettings(
                playerName        = s.playerName,
                graphicsQuality   = s.graphicsQuality,
                vhsEnabled        = s.vhsEnabled,
                resolutionScale   = s.resolutionScale,
                musicVolume       = s.musicVolume,
                footstepVolume    = s.footstepVolume,
                monsterVolume     = s.monsterVolume,
                voiceVolume       = s.voiceVolume,
                cameraSensitivity = s.cameraSensitivity,
                fpsLimit          = s.fpsLimit,
                shadowsEnabled    = s.shadowsEnabled,
                antialiasingOn    = s.antialiasingOn,
                fogEnabled        = s.fogEnabled,
                vibrationOn       = s.vibrationOn,
                showFps           = s.showFps,
                colorBlindMode    = s.colorBlindMode,
                pushNotifications = s.pushNotifications
            )
            _state.update { it.copy(isSyncing = false, syncSuccess = true) }
        }
    }

    fun resetDefaults() {
        viewModelScope.launch {
            repo.clearAll()
        }
    }

    private fun save(block: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) { runCatching { block() } }
    }
}

private enum class SettingsTab(val labelRes: Int, val icon: ImageVector) {
    Graphics    (R.string.settings_tab_graphics,  Icons.Default.DisplaySettings),
    Audio       (R.string.settings_tab_audio,     Icons.AutoMirrored.Filled.VolumeUp),
    Controls    (R.string.settings_tab_controls,  Icons.Default.SportsEsports),
    Account     (R.string.settings_tab_account,   Icons.Default.AccountCircle),
    Gameplay    (R.string.settings_tab_gameplay,  Icons.Default.Tune),
    Notif       (R.string.settings_tab_notif,     Icons.Default.Notifications),
    Language    (R.string.settings_tab_language,  Icons.Default.Language)
}

@Composable
fun SettingsScreen(
    onBack    : () -> Unit,
    onUiEditor: () -> Unit,
    vm        : SettingsVM = hiltViewModel()
) {
    val s       by vm.state.collectAsState()
    val activity = LocalContext.current as? Activity
    var selectedTab by remember { mutableIntStateOf(0) }

    Box(Modifier.fillMaxSize().background(DarkBg)) {
        CrtOverlay()
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().background(Color.Black.copy(0.65f)).padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Yellow) }
                Text(stringResource(R.string.menu_settings), color = Yellow, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                Spacer(Modifier.weight(1f))
                androidx.compose.animation.AnimatedVisibility(visible = s.isSyncing, enter = fadeIn(), exit = fadeOut()) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = Yellow, strokeWidth = 2.dp)
                }
                androidx.compose.animation.AnimatedVisibility(visible = !s.isSyncing && s.syncSuccess, enter = fadeIn(), exit = fadeOut()) {
                    Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                }
            }
            DividerLine()

            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor   = Color.Transparent,
                contentColor     = Yellow,
                edgePadding      = 0.dp,
                indicator        = { positions ->
                    val idx = selectedTab.coerceIn(0, positions.lastIndex)
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(positions[idx]),
                        color = Yellow
                    )
                }
            ) {
                SettingsTab.entries.forEachIndexed { index, tab ->
                    val sel = selectedTab == index
                    Tab(
                        selected = sel,
                        onClick  = { selectedTab = index },
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

            AnimatedContent(
                targetState  = selectedTab,
                transitionSpec = {
                    slideInHorizontally(tween(250)) { if (targetState > initialState) it / 3 else -it / 3 } +
                    fadeIn(tween(200)) togetherWith
                    slideOutHorizontally(tween(200)) { if (targetState > initialState) -it / 3 else it / 3 } +
                    fadeOut(tween(150))
                },
                label        = "settings_tab"
            ) { tabIndex ->
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (tabIndex) {
                        0 -> GraphicsTab(s, vm::onQuality, vm::onVhs, vm::onResolution, vm::onShadows, vm::onAntialiasing, vm::onFog, vm::onShowFps)
                        1 -> AudioTab(s, vm::onMusic, vm::onVibration)
                        2 -> ControlsTab(s, vm::onSensitivity, onUiEditor)
                        3 -> AccountTab(s, vm::onName, vm::syncToServer, vm::resetDefaults)
                        4 -> GameplayTab(s, vm::onColorBlind, vm::onFpsLimit)
                        5 -> NotifTab(s, vm::onPushNotif)
                        6 -> {
                            val lang by vm.languageSelection.collectAsState()
                            LanguageSection(current = lang, onSelect = vm::onLanguage)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GraphicsTab(
    s           : SettingsUiState,
    onQuality   : (String) -> Unit,
    onVhs       : (Boolean) -> Unit,
    onResolution: (Float) -> Unit,
    onShadows   : (Boolean) -> Unit,
    onAA        : (Boolean) -> Unit,
    onFog       : (Boolean) -> Unit,
    onShowFps   : (Boolean) -> Unit,
) {
    SettingsSection(stringResource(R.string.settings_tab_graphics))

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(
            R.string.quality_low    to "low",
            R.string.quality_medium to "medium",
            R.string.quality_high   to "high",
            R.string.quality_ultra  to "ultra"
        ).forEach { (res, key) ->
            val sel   = s.graphicsQuality == key
            val scale by animateFloatAsState(if (sel) 1.04f else 1f, spring(), label = "q_$key")
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.weight(1f).height(38.dp).scale(scale)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (sel) Yellow.copy(0.15f) else MetalBg)
                    .border(1.dp, if (sel) Yellow.copy(0.6f) else BorderCol, RoundedCornerShape(2.dp))
                    .clickable { onQuality(key) }
            ) {
                Text(stringResource(res), color = if (sel) Yellow else TextDim, fontSize = 10.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }

    SettingsSlider(stringResource(R.string.graphics_resolution_scale), s.resolutionScale, onResolution, 0.5f..1f)
    SettingsToggle(stringResource(R.string.graphics_vhs_effect),   s.vhsEnabled,       onVhs)
    SettingsToggle(stringResource(R.string.graphics_shadows),      s.shadowsEnabled,   onShadows)
    SettingsToggle(stringResource(R.string.graphics_antialiasing), s.antialiasingOn,   onAA)
    SettingsToggle(stringResource(R.string.graphics_fog),          s.fogEnabled,       onFog)
    DividerLine()
    SettingsSection("HUD")
    SettingsToggle(stringResource(R.string.graphics_show_fps),  s.showFps,  onShowFps)
}

@Composable
private fun AudioTab(
    s         : SettingsUiState,
    onMusic   : (Float) -> Unit,
    onVib     : (Boolean) -> Unit
) {
    SettingsSection(stringResource(R.string.settings_tab_audio))
    SettingsSlider(stringResource(R.string.audio_master_volume),      s.musicVolume,    onMusic)
    SettingsToggle(stringResource(R.string.settings_vibration),       s.vibrationOn,    onVib)
}

@Composable
private fun ControlsTab(
    s            : SettingsUiState,
    onSensitivity: (Float) -> Unit,
    onUiEditor   : () -> Unit
) {
    SettingsSection(stringResource(R.string.settings_tab_controls))
    SettingsSlider(stringResource(R.string.controls_camera_sensitivity), s.cameraSensitivity, onSensitivity, 0.25f..3f)
    Spacer(Modifier.height(8.dp))
    AtmosphericButton(
        label   = stringResource(R.string.controls_ui_layout),
        icon    = Icons.Default.GridView,
        accent  = Yellow,
        width   = 240.dp,
        height  = 48.dp,
        onClick = onUiEditor
    )
}

@Composable
private fun AccountTab(
    s        : SettingsUiState,
    onName   : (String) -> Unit,
    onSync   : () -> Unit,
    onReset  : () -> Unit
) {
    SettingsSection(stringResource(R.string.settings_tab_account))

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.account_player_name), color = TextDim, fontSize = 10.sp, letterSpacing = 1.sp)
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(2.dp))
                .background(MetalBg)
                .border(1.dp, BorderCol, RoundedCornerShape(2.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            androidx.compose.foundation.text.BasicTextField(
                value         = s.playerName,
                onValueChange = onName,
                singleLine    = true,
                textStyle     = TextStyle(color = Yellow, fontSize = 13.sp),
                cursorBrush   = SolidColor(Yellow),
                decorationBox = { inner ->
                    if (s.playerName.isEmpty()) Text("Wanderer", color = TextDim, fontSize = 13.sp)
                    inner()
                }
            )
        }
    }


    DividerLine()

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AtmosphericButton(stringResource(R.string.account_sync),  Icons.Default.Sync,    OmniumCol, 150.dp, 46.dp, onSync)
        AtmosphericButton(stringResource(R.string.account_reset), Icons.Default.Refresh, DangerRed, 150.dp, 46.dp, onReset)
    }
}

@Composable
private fun GameplayTab(
    s           : SettingsUiState,
    onColorBlind: (String) -> Unit,
    onFpsLimit  : (Int) -> Unit
) {
    SettingsSection(stringResource(R.string.settings_color_blind))
    val cbOptions = listOf(
        Triple("none",          R.string.cb_none,          R.string.cb_none_desc),
        Triple("deuteranopia",  R.string.cb_deuteranopia,  R.string.cb_deuteranopia_desc),
        Triple("protanopia",    R.string.cb_protanopia,    R.string.cb_protanopia_desc),
        Triple("tritanopia",    R.string.cb_tritanopia,    R.string.cb_tritanopia_desc)
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        cbOptions.forEach { (key, titleRes, descRes) ->
            val sel   = s.colorBlindMode == key
            val scale by animateFloatAsState(if (sel) 1.01f else 1f, spring(), label = "cb_$key")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(scale)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (sel) Yellow.copy(0.12f) else MetalBg)
                    .border(1.dp, if (sel) Yellow.copy(0.6f) else BorderCol, RoundedCornerShape(4.dp))
                    .clickable { onColorBlind(key) }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {

                androidx.compose.foundation.Canvas(Modifier.size(16.dp)) {
                    val r = size.minDimension / 2f
                    drawCircle(if (sel) Yellow else TextDim, radius = r * 0.9f, center = center, style = Stroke(1.5f))
                    if (sel) drawCircle(Yellow, radius = r * 0.45f, center = center)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(titleRes),
                        color = if (sel) Yellow else TextSec,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        stringResource(descRes),
                        color = TextDim, fontSize = 10.sp, lineHeight = 14.sp
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    SettingsSection(stringResource(R.string.settings_fps_limit))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(30, 60, 90, 120).forEach { fps ->
            val sel   = s.fpsLimit == fps
            val scale by animateFloatAsState(if (sel) 1.05f else 1f, spring(), label = "fps_$fps")
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.scale(scale)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (sel) Yellow.copy(0.15f) else MetalBg)
                    .border(1.dp, if (sel) Yellow.copy(0.6f) else BorderCol, RoundedCornerShape(2.dp))
                    .clickable { onFpsLimit(fps) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("$fps", color = if (sel) Yellow else TextDim, fontSize = 11.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
private fun NotifTab(s: SettingsUiState, onPush: (Boolean) -> Unit) {
    val ctx = LocalContext.current
    val activity = ctx as? Activity




    val lifecycleOwner = LocalLifecycleOwner.current
    var osGranted by remember { mutableStateOf(hasNotificationPermission(ctx)) }
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) osGranted = hasNotificationPermission(ctx)
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }





    val requestPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        osGranted = granted
        onPush(granted)
        OmniLog.i("Perm", "settings re-request POST_NOTIFICATIONS granted=$granted")
    }

    SettingsSection(stringResource(R.string.settings_tab_notif))

    SettingsToggle(
        stringResource(R.string.notif_push_toggle),
        s.pushNotifications && osGranted,
        { wanted ->
            when {


                !wanted    -> onPush(false)
                osGranted  -> onPush(true)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    activity?.shouldShowRequestPermissionRationale(
                        android.Manifest.permission.POST_NOTIFICATIONS) != false ->
                        requestPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                else       -> openAppNotificationSettings(ctx)
            }
        }
    )
    Spacer(Modifier.height(6.dp))
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
            .background(if (osGranted) MetalBg else DangerRed.copy(0.10f))
            .border(1.dp, if (osGranted) BorderCol else DangerRed.copy(0.45f), RoundedCornerShape(4.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.Canvas(Modifier.size(14.dp)) {
            val c = if (osGranted) SuccessGreen else DangerRed
            drawCircle(c, radius = size.minDimension * 0.42f, center = center, style = Stroke(1.6f))
            if (osGranted) {
                drawLine(c, Offset(size.width * 0.30f, size.height * 0.52f), Offset(size.width * 0.45f, size.height * 0.68f), strokeWidth = 1.8f, cap = StrokeCap.Round)
                drawLine(c, Offset(size.width * 0.45f, size.height * 0.68f), Offset(size.width * 0.72f, size.height * 0.34f), strokeWidth = 1.8f, cap = StrokeCap.Round)
            } else {
                drawLine(c, Offset(size.width * 0.34f, size.height * 0.34f), Offset(size.width * 0.66f, size.height * 0.66f), strokeWidth = 1.8f, cap = StrokeCap.Round)
                drawLine(c, Offset(size.width * 0.66f, size.height * 0.34f), Offset(size.width * 0.34f, size.height * 0.66f), strokeWidth = 1.8f, cap = StrokeCap.Round)
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            stringResource(if (osGranted) R.string.notif_permission_granted else R.string.notif_permission_denied),
            color = if (osGranted) TextSec else DangerRed.copy(0.9f), fontSize = 11.sp,
            modifier = Modifier.weight(1f)
        )
        if (!osGranted) {
            Text(
                stringResource(R.string.notif_open_settings),
                color = Yellow, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { openAppNotificationSettings(ctx) }
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(2.dp))
            .background(MetalBg)
            .border(1.dp, BorderCol, RoundedCornerShape(2.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Info, null, tint = TextDim, modifier = Modifier.size(14.dp))
        Text(stringResource(R.string.notif_info_text), color = TextDim, fontSize = 11.sp, lineHeight = 16.sp)
    }
}

@Composable
private fun SettingsSection(text: String) {
    Text(text, color = TextSec, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun SettingsToggle(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(label, color = TextSec, fontSize = 12.sp)
        Switch(
            checked         = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor   = Yellow,
                checkedTrackColor   = YellowDim,
                uncheckedThumbColor = TextDim,
                uncheckedTrackColor = MetalBg
            )
        )
    }
}

@Composable
private fun SettingsSlider(
    label  : String,
    value  : Float,
    onValue: (Float) -> Unit,
    range  : ClosedFloatingPointRange<Float> = 0f..1f
) {
    val displayValue by remember(value) { derivedStateOf { (value * 100).toInt() } }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text(label, color = TextSec, fontSize = 12.sp)
            Text("$displayValue%", color = Yellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value         = value,
            onValueChange = onValue,
            valueRange    = range,
            colors = SliderDefaults.colors(
                thumbColor         = Yellow,
                activeTrackColor   = Yellow,
                inactiveTrackColor = MetalBg
            )
        )
    }
}

@HiltViewModel
class UiEditorVM @Inject constructor(private val repo: SettingsRepository) : ViewModel() {
    val layout: StateFlow<Map<String, UiButtonLayout>> = repo.observeUiLayout()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun saveLayout(layout: List<UiButtonLayout>) {
        viewModelScope.launch { repo.saveUiLayout(layout) }
    }

    fun reset() { viewModelScope.launch { repo.resetUiLayout() } }
}

@Composable
fun UiEditor(onSave: () -> Unit, vm: UiEditorVM = hiltViewModel()) {
    val saved by vm.layout.collectAsState()








    val elements = remember {
        fun of(id: String, labelRes: Int): HudElement {
            val slot = HUD_DEFAULT_SLOTS.getValue(id)
            val (w, h) = HUD_DEFAULT_SIZES.getValue(id)
            return HudElement(id, labelRes, slot.x, slot.y, w, h, slot.scale)
        }
        mutableStateListOf(
            of("bar_sanity",  R.string.game_hud_sanity),
            of("bar_stamina", R.string.game_hud_stamina),
            of("bar_battery", R.string.game_hud_battery),
            of("readouts",    R.string.editor_btn_readouts),
            of("pause",       R.string.editor_btn_pause),
            of("joystick",    R.string.editor_btn_move),
            of("interact",    R.string.editor_btn_interact),
            of("sprint",      R.string.editor_btn_sprint),
            of("flashlight",  R.string.editor_btn_flashlight),
            of("jump",        R.string.editor_btn_jump),
            of("crouch",      R.string.editor_btn_crouch)
        )
    }


    LaunchedEffect(saved) {
        saved.forEach { (id, layout) ->
            elements.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let { i ->
                elements[i] = elements[i].copy(
                    normX = layout.offset.x, normY = layout.offset.y, scale = layout.sizeScale
                )
            }
        }
    }

    var selectedId by remember { mutableStateOf<String?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        CrtOverlay()

        Box(
            Modifier.fillMaxSize().onSizeChanged { canvasSize = it }
        ) {
            elements.forEachIndexed { index, el ->
                val selected = el.id == selectedId
                val w = el.baseW * el.scale
                val h = el.baseH * el.scale
                Box(
                    Modifier
                        .offset {
                            IntOffset(
                                (el.normX * canvasSize.width - w * density / 2f).toInt(),
                                (el.normY * canvasSize.height - h * density / 2f).toInt()
                            )
                        }
                        .size(w.dp, h.dp)



                        .then(
                            if (selected)
                                Modifier.border(1.dp, Yellow.copy(0.85f), RoundedCornerShape(10.dp))
                            else Modifier
                        )
                        .pointerInput(el.id) {
                            detectDragGestures(
                                onDragStart = { selectedId = el.id },
                                onDrag = { change, drag ->
                                    change.consume()
                                    val cur = elements[index]


                                    elements[index] = cur.copy(
                                        normX = (cur.normX + drag.x / canvasSize.width.coerceAtLeast(1))
                                            .coerceIn(0.04f, 0.96f),
                                        normY = (cur.normY + drag.y / canvasSize.height.coerceAtLeast(1))
                                            .coerceIn(0.04f, 0.96f)
                                    )
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {


                    EditorPreview(el.id, w, selected)
                }


                Text(
                    stringResource(el.labelRes),
                    color = if (selected) Yellow else TextDim,
                    fontSize = 8.sp, maxLines = 1,
                    modifier = Modifier.offset {
                        IntOffset(
                            (el.normX * canvasSize.width - w * density / 2f).toInt(),
                            (el.normY * canvasSize.height + h * density / 2f + 2 * density).toInt()
                        )
                    }
                )
            }
        }










        Column(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
                .width(250.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black.copy(0.86f))
                .border(1.dp, BorderCol, RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val sel = elements.firstOrNull { it.id == selectedId }
            if (sel == null) {
                Text(
                    stringResource(R.string.editor_hint),
                    color = TextDim, fontSize = 10.sp, lineHeight = 14.sp
                )
            } else {
                val index = elements.indexOfFirst { it.id == sel.id }
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text(
                        stringResource(sel.labelRes),
                        color = Yellow, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1
                    )
                    Text("${(sel.scale * 100).toInt()}%", color = CrtAmber, fontSize = 11.sp)
                }
                Slider(
                    value = sel.scale,
                    onValueChange = { elements[index] = elements[index].copy(scale = it) },
                    valueRange = 0.6f..1.8f,
                    colors = SliderDefaults.colors(
                        thumbColor = Yellow,
                        activeTrackColor = Yellow.copy(0.75f),
                        inactiveTrackColor = MetalBg
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }




        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(0.86f))
                .border(1.dp, BorderCol, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AtmosphericButton(
                label = stringResource(R.string.editor_reset),
                icon = Icons.Default.Refresh,
                accent = TextSec,
                width = 140.dp, height = 42.dp,
                onClick = {
                    vm.reset()
                    selectedId = null



                    HUD_DEFAULT_SLOTS.forEach { (id, slot) ->
                        elements.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let { i ->
                            elements[i] = elements[i].copy(
                                normX = slot.x, normY = slot.y, scale = slot.scale
                            )
                        }
                    }
                }
            )
            AtmosphericButton(
                label = stringResource(R.string.controls_save_exit),
                icon = Icons.Default.Check,
                accent = Yellow,
                width = 160.dp, height = 42.dp,
                onClick = {
                    vm.saveLayout(
                        elements.map { UiButtonLayout(it.id, Offset(it.normX, it.normY), it.scale) }
                    )
                    onSave()
                },
                isPrimary = true
            )
        }
    }
}


private data class HudElement(
    val id: String,
    val labelRes: Int,
    val normX: Float,
    val normY: Float,
    val baseW: Float,
    val baseH: Float,
    val scale: Float = 1f
)


@Composable
private fun EditorPreview(id: String, widthDp: Float, selected: Boolean) {
    val accent = if (selected) Yellow else YellowDim
    when (id) {
        "bar_sanity"  -> Box(Modifier.width(widthDp.dp)) { StatusBar(stringResource(R.string.game_hud_sanity), 0.68f, SouliumCol) }
        "bar_stamina" -> Box(Modifier.width(widthDp.dp)) { StatusBar(stringResource(R.string.game_hud_stamina), 0.86f, SuccessGreen) }
        "bar_battery" -> Box(Modifier.width(widthDp.dp)) { StatusBar(stringResource(R.string.game_hud_battery), 0.74f, CrtAmber) }
        "readouts" -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            EditorBadge("04:12", TextSec)
            EditorBadge("◉ 2", DangerRed)
        }

        "joystick" -> VirtualJoystick(Modifier.size(widthDp.dp), interactive = false) { _, _ -> }
        "pause" -> IconGlyphButton((widthDp * 0.85f).dp, Yellow.copy(0.8f), onClick = {}) {
            HudGlyph("pause", it, Modifier.fillMaxSize())
        }
        else -> HudActionButton(
            widthDp.dp, accent, id,
            onClick = {},
            interactive = false
        )
    }
}


@Composable
private fun EditorBadge(text: String, color: Color) {
    Box(
        Modifier.clip(RoundedCornerShape(2.dp))
            .background(MetalBg.copy(0.8f))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) { Text(text, color = color, fontSize = 10.sp) }
}


private fun hasNotificationPermission(ctx: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    } else {
        NotificationManagerCompat.from(ctx).areNotificationsEnabled()
    }


private fun openAppNotificationSettings(ctx: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, ctx.packageName)
    } else {
        Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(android.net.Uri.fromParts("package", ctx.packageName, null))
    }
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { ctx.startActivity(intent) }
}


@Composable
fun LanguageSection(current: String, onSelect: (String) -> Unit) {
    val options = buildList {
        add(AppLanguage.SYSTEM to stringResource(R.string.settings_language_system))
        AppLanguage.entries.forEach { add(it.tag to it.endonym) }
    }
    SettingsSection(stringResource(R.string.settings_language))
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { (tag, label) ->
            val sel = current == tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (sel) Yellow.copy(0.12f) else MetalBg)
                    .border(1.dp, if (sel) Yellow.copy(0.6f) else BorderCol, RoundedCornerShape(4.dp))


                    .clickable { if (!sel) onSelect(tag) }
                    .padding(horizontal = 12.dp, vertical = 11.dp)
            ) {
                androidx.compose.foundation.Canvas(Modifier.size(16.dp)) {
                    val r = size.minDimension / 2f
                    drawCircle(if (sel) Yellow else TextDim, radius = r * 0.9f, center = center, style = Stroke(1.5f))
                    if (sel) drawCircle(Yellow, radius = r * 0.45f, center = center)
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    label,
                    color = if (sel) Yellow else TextSec,
                    fontSize = 12.sp,
                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

