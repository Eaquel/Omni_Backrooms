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
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
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
                OmniLog.w("Guard", "APP_START_THREAT flags=$flags report=$report")
            }
        }
    }

    /** Writes every uncaught exception to Documents/OmniBackrooms/crash.txt so
     *  crashes can be read off the device directly, then delegates to the
     *  previous handler so the platform still gets its turn. */
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
    fun provideSettingsRepository(store: DataStore<Preferences>, bridge: NativeBridge): SettingsRepository =
        SettingsRepository(store, bridge)

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

/**
 * "Eaquel Sunar", over a dead tape spinning up.
 *
 * Both halves are generated. The sound is Sound/Synth.cpp's vhsIntro, synthesised
 * on the device — there is no audio file in this APK — and the picture is drawn
 * here rather than being a bitmap, for the same reason: a static image of tape
 * damage looks like a static image of tape damage. Damage has to move.
 *
 * Four things are happening, and they are the four that actually read as VHS:
 *
 *   * chroma bleed — the red and cyan copies of the text sit either side of the
 *     white one, because on tape the colour-under signal is carried separately
 *     from luminance and drifts against it;
 *   * head-switching noise — the band of hash that crawls up the frame, which
 *     on real tape sits at the very bottom where the head leaves the drum;
 *   * tracking wobble — whole scanlines displaced horizontally, worst while the
 *     transport is still coming up to speed;
 *   * dropouts — brief hard gaps, not fades, in time with the audio.
 *
 * The whole thing is skippable on tap. A title card you cannot get past stops
 * being atmosphere by the third launch.
 */
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

    // How settled the tape is: 0 while the transport is spinning up, 1 once it
    // has locked. Everything unstable below is scaled by (1 - lock).
    val lock = ((t - 0.35f) / 1.1f).coerceIn(0f, 1f)
    val fade = when {
        t < 0.25f              -> t / 0.25f
        t > INTRO_TOTAL - 0.5f -> ((INTRO_TOTAL - t) / 0.5f).coerceAtLeast(0f)
        else                   -> 1f
    }
    // Dropouts, from the same value-noise the audio generator gates on, so the
    // picture goes when the sound goes.
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
            // Chroma bleed: the same word three times. Red and cyan sit either
            // side of white, which on black reads as colour separating from
            // luminance without needing a blend mode.
            IntroWord(presents, Color(0xFFFF2B2B), (-split).dp, 0.75f)
            IntroWord(presents, Color(0xFF29FFF3),   split.dp,  0.75f)
            IntroWord(presents, Color(0xFFF2F0E6),      0.dp,   1f)
        }

        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Scanlines. Two pixels on, two off, at the density a CRT actually
            // had rather than the density that looks like a grille.
            var y = 0f
            while (y < h) {
                drawRect(Color.Black.copy(alpha = 0.30f), Offset(0f, y), Size(w, 1.6f))
                y += 3.2f
            }

            // Head-switching noise: a band of hash crawling up the frame,
            // brightest while the transport is unsettled.
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

            // Vignette, so the corners fall away like a tube.
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

/** The sting's length, and how long the card is held after it. */
private const val INTRO_SECONDS = 2.6f
private const val INTRO_TOTAL   = 3.3f

/**
 * The same smooth value noise the audio generator uses, so the picture tears on
 * the beat the sound drops out on rather than on a schedule of its own.
 *
 * Ported rather than shared: reaching this one function across JNI, once per
 * frame, to save nine lines is not a trade worth making. It is a faithful port
 * — `ushr` and `and` on a signed Int operate on the same 32-bit pattern the C++
 * and the Python mask by hand — but nothing asserts that, because nothing needs
 * to. This drives a wobble, not a waveform; drift here shows up as the picture
 * tearing slightly off the sound, not as a defect. The generator that has to be
 * exact is the audio one, and Code_To_Sound.py checks that against the shipped
 * C++ sample for sample.
 */
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
    /**
     * Opens the audio stream just for the sting and closes it again.
     *
     * The hum and ambience layers are silenced first. They are the sound of a
     * corridor, and the title card is not in a corridor — left at their
     * defaults they would drone under the tape and then carry on into the menu,
     * which is a change to the whole app smuggled in behind a splash screen.
     */
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
        if (showGuardDialog) {
            AlertDialog(
                onDismissRequest = { showGuardDialog = false },
                title    = { Text(stringResource(R.string.guard_threat_title)) },
                text     = {
                    // The reason, on screen. Without it the player is told
                    // their device is unauthorised and has nothing to report
                    // back but the fact that it happened.
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
                    // popUpTo with inclusive, so back from the menu leaves the
                    // app instead of replaying the tape.
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
                // Asked here rather than at app start: the gate used to sit
                // beside the NavHost, so the system dialog landed on top of the
                // intro before the player had seen anything of the game.
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
            // The rest slide in from the side like Settings and Story. Half the
            // routes used to cut with no transition at all, which read as the
            // app dropping frames rather than as a deliberate change of screen.
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

    /**
     * Level 0 carries exactly one creature, on every difficulty.
     *
     * Three to eight of them, topped up every twelve seconds, turned an empty
     * yellow maze into a crowd — and a crowd is not frightening, it is busy.
     * One thing that is somewhere, that you have driven off twice already and
     * know is coming back, is the whole level.
     *
     * Difficulty now changes what the one creature is rather than how many
     * there are: how fast it moves and how far it can see. `spawnIntervalMs` is
     * kept because Level 1 will want a spawner, but it is set beyond any run
     * length so nothing is topped up here.
     */
    fun getSpawnConfig(difficulty: String): SpawnConfig = when (difficulty.lowercase()) {
        "easy" -> SpawnConfig(count=1, speedMult=0.7f, sightMult=0.8f, spawnIntervalMs=3_600_000)
        "hard" -> SpawnConfig(count=1, speedMult=1.4f, sightMult=1.3f, spawnIntervalMs=3_600_000)
        else   -> SpawnConfig(count=1, speedMult=1.0f, sightMult=1.0f, spawnIntervalMs=3_600_000)
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
     *  shape the story screen renders, so the mapping
     *  is: localised text -> the `Tr` slot, English source -> the `En` slot. */
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
        // EXPECTED_SIG_HASH ships blank until a real release keystore's SHA-256 is
        // configured (see build.gradle.kts). Until then, skip this specific check
        // rather than flag every legitimate install as tampered.
        val sigCheckOn = BuildConfig.EXPECTED_SIG_HASH.isNotBlank()
        val sigValid  = if (sigCheckOn) bridge.isSignatureValid() else true
        val hookEvidence = hookingEvidence()
        val hook      = hookEvidence != null
        val memEvidence = memoryTamperEvidence()
        val memTamper = memEvidence != null
        val reportStr = bridge.getThreatReport()

        // Log every individual signal, always. Previously a warning appeared with
        // no way to tell which check caused it.
        OmniLog.i(
            "Guard",
            "scan flags=0x${Integer.toHexString(flags)} rooted=$rooted frida=$frida " +
            "debugged=$debugged emulator=$emulator sigCheckOn=$sigCheckOn sigValid=$sigValid " +
            "hook=$hook memTamper=$memTamper native='$reportStr'"
        )
        // The evidence itself, not just the verdict. A boolean tells the player
        // their device is unauthorised and tells us nothing about why; these two
        // lines are what make a false positive diagnosable from a log file.
        hookEvidence?.let { OmniLog.w("Guard", "hook evidence: $it") }
        memEvidence?.let { OmniLog.w("Guard", "memory-map evidence: ${it.trim()}") }

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

    /** The offending frame, or null. Returned rather than a boolean so the log
     *  can name what was actually found. */
    private fun hookingEvidence(): String? = runCatching {
        Thread.currentThread().stackTrace.firstOrNull { el ->
            listOf("xposed", "substrate", "lsposed", "frida")
                .any { el.className.contains(it, ignoreCase = true) }
        }?.className
    }.getOrNull()

    /**
     * Injected instrumentation, found by name in the process's own memory map.
     *
     * The patterns have to be specific, and this is why. The previous version
     * searched the whole of /proc/self/maps for the bare substrings "hook",
     * "inject" and "gadget". That file lists the path of every mapped file in
     * the process — the framework, the vendor blobs, the webview, the app's own
     * randomised install directory — and on a great many perfectly ordinary
     * devices one of those paths contains one of those words. The result was
     * memTamper = true on a clean phone, which escalated to HIGH, which put
     * "unauthorised software detected" on the screen at every single launch.
     *
     * These match tool artefacts by their actual filenames instead, and the
     * matching line is returned so the log says WHICH one fired rather than
     * only that something did.
     */
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
                        // Log first — a silent kill on a false positive is
                        // indistinguishable from a crash to the player.
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
     *  purchase. Frames and trails both land here — it used to be assigned from
     *  the frame list alone, which overwrote every trail the player owned and
     *  left the trail cards permanently locked. */
    val ownedIds    : Set<String>         = emptySet(),
    /** The cosmetics actually worn, so a card can say so and the equip button
     *  can turn itself off on the one already in use. */
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
        // Frames and trails are two independent lists that share one owned-id
        // set, so they have to be combined rather than assigned.
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
            _state.update { it.copy(omniumBal = localOmnium) }
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

            // Grant locally first. Cosmetics are client-side by design and
            // everything is free in this phase, so a purchase must succeed even
            // with no server reachable — previously the item was never unlocked
            // because the only path went through an API that isn't running yet.
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
                item.id.startsWith("trail_") -> {
                    val key = item.id.removePrefix("trail_")
                    cosmetics.grantTrail(key)
                    cosmetics.setTrail(key)
                }
                item.id.startsWith("priv_") || item.category == "vip" -> {
                    // VIP is an entitlement, not a cosmetic: it doubles what a
                    // run pays out, so it has to be readable offline.
                    cosmetics.setVip(true)
                    // Straight from the native catalogue, so a frame added there
                    // is covered without anyone remembering to update a list.
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

    /**
     * Wears an owned cosmetic.
     *
     * Buying one used to be the only way to end up wearing it, so a player who
     * owned three trails was stuck in whichever they bought last with no way
     * back. Refuses anything not owned rather than silently equipping it.
     */
    fun equipTrail(trailId: String) {
        if ("trail_$trailId" !in _state.value.ownedIds) return
        viewModelScope.launch { runCatching { cosmetics.setTrail(trailId) } }
    }

    fun equipFrame(frameId: String) {
        if ("frame_$frameId" !in _state.value.ownedIds) return
        viewModelScope.launch { runCatching { cosmetics.setFrame(frameId) } }
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
        // Three, not four. A "plain metal ring" entry used to sit at the end of
        // this list and it was the one nobody would ever choose: it existed
        // because the set had grown a default, and it made the tab read as
        // three ideas and a spare. The ids match Native/Frame exactly.
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
        // Ids match Native/Trail exactly.
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
        /** What VIP is actually worth: every run pays double. */
        const val VIP_OMNIUM_MULTIPLIER = 2L
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

            // Entitlement for this run, sampled now — see vipRun.
            vipRun = runCatching { cosmetics.observeVip().first() }.getOrDefault(false)

            // Equip the trail and start it empty. Without the clear, a second
            // run inherits the marks from the first and the player spawns
            // standing in someone else's footprints.
            runCatching {
                bridge.trailClear()
                val equipped = cosmetics.observeTrail().first()
                val idx = (0 until bridge.trailCount()).firstOrNull { bridge.trailId(it) == equipped } ?: 0
                bridge.trailSetStyle(idx)
            }

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
                lastTickMs = now
                // Whether the run has already ended — died, went mad, or got
                // out. The clock stops when the run does; it used to keep
                // counting behind the game-over overlay, so the survival time
                // the player was being shown climbed for as long as they left
                // the results on screen, and the Omnium award was computed off
                // a number that no longer matched what they had survived.
                val runOver = _state.value.let { it.isGameOver || it.isEscaped || it.isMadnessOver }
                if (!runOver) elapsedMs += (dt * 1000).toLong()

                // Marks on the floor age on their own clock in Native/Trail,
                // whether or not the player is still walking.
                runCatching { bridge.trailUpdate(dt) }

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
                // A body that is going down does not get to keep walking — and
                // neither does one that has already stopped. Only the madness
                // collapse was blocking movement, so a player on zero health
                // could still stroll around underneath their own death screen.
                if (mag > 0.02f && !madnessRunning && !runOver) {
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
                        // Leave a mark. Feet alternate, so prints land either
                        // side of the line of travel instead of in one furrow.
                        footSide = -footSide
                        snapshot.camera?.let { c ->
                            runCatching { bridge.trailStep(c.posX, c.posZ, c.yaw, footSide) }
                        }
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

                val wasOver = _state.value.isGameOver
                val derived = stepSimulation(bridge, dt, _state.value)
                _state.update { applyTickToState(it, derived, dt, elapsedMs, score) }
                if (!wasOver && _state.value.isGameOver) {
                    // Dying still pays for the time survived — just without the
                    // escape bonus. Surviving is the thing being rewarded.
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
                // Exactly the same guard the clock needs, and for exactly the
                // same reason: this only ever checked isPaused, so after the
                // player died the score carried on ticking up once a second
                // behind the results screen they were reading. A finished run's
                // score is a fact, not a running total.
                val s = _state.value
                val runOver = s.isGameOver || s.isEscaped || s.isMadnessOver
                if (!s.isPaused && !runOver) {
                    score += when (s.difficulty) { "hard" -> 5L; "normal" -> 3L; else -> 1L }
                }
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
    /** Which foot is next. Flips on every footfall so the trail has a gait. */
    private var footSide = 1f
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
        // A finished run must not be resumable, and simply declining to write
        // was not enough: the last periodic autosave was still on disk, so
        // "Continue" dropped the player back into the run a minute before they
        // lost it. Wipe it here instead.
        //
        // isMadnessOver was missing from this guard entirely, which is the case
        // that actually shipped. Sanity death sets isMadnessOver and leaves
        // isGameOver false, so leaving the screen after one ran straight past
        // both checks and wrote a fresh save of the lost run on the way out.
        if (s.isGameOver || s.isEscaped || s.isMadnessOver) {
            saveStore.clearDetached()
            return
        }
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
            finishRun()
        }
    }

    /**
     * VIP, sampled once when the run starts.
     *
     * Read at the start rather than at payout so a player cannot buy VIP from
     * another device mid-run and have it apply retroactively to time they
     * already survived without it.
     */
    @Volatile private var vipRun = false

    /** Survival is the whole point of the mode, so it is what pays. */
    private fun omniumForRun(elapsed: Long, escaped: Boolean): Long {
        val minutes = elapsed / 60_000f
        val base = (minutes * OMNIUM_PER_MINUTE).toLong()
        val bonus = if (escaped) OMNIUM_ESCAPE_BONUS else 0L
        // VIP doubles the whole payout, escape bonus included — a multiplier
        // that skipped the bonus would quietly punish the players who finish.
        val vipMult = if (vipRun) VIP_OMNIUM_MULTIPLIER else 1L
        return ((base + bonus) * vipMult).coerceAtLeast(0L)
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
            finishRun()
        }
    }

    fun onDamageEntity(id: Int) {
        bridge.damageEntity(id, 25f); kills++; score += 100L
        _state.update { it.copy(kills = kills, score = score) }
    }

    /** The live footstep marks. Called from the GL thread once a frame; the
     *  native side hands back a fresh array, so nothing is shared. */
    fun collectTrail(): FloatArray? = runCatching { bridge.trailCollect() }.getOrNull()

    /** The trail the player is wearing, watched so the corridor can follow a
     *  change made in the market without a restart. */
    val equippedTrail: StateFlow<String> = cosmetics.observeTrail()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    /** The equipped trail's own entry from Native/Trail — tint, size, mark. */
    fun trailStyleSpec(): FloatArray? = runCatching {
        val equipped = runBlocking { cosmetics.observeTrail().first() }
        val idx = (0 until bridge.trailCount()).firstOrNull { bridge.trailId(it) == equipped } ?: 0
        bridge.trailSpec(idx)
    }.getOrNull()

    /** Fetches one chunk from the native field. Called from the GL thread, which
     *  is safe: the field is stateless and the JNI call only reads. */
    fun fetchChunk(chunkX: Int, chunkZ: Int): WorldChunk? {
        val w = _state.value.world
        if (!w.isValid) return null
        return WorldChunk.parse(chunkX, chunkZ, w.chunkCells, bridge.generateChunk(chunkX, chunkZ))
    }

    /** Closes out a finished run: drops the resume snapshot and records the
     *  personal best. Both are local — this used to also post the score to a
     *  leaderboard API, Firestore and Crashlytics, none of which ever had a
     *  server behind them. */
    private fun finishRun() {
        viewModelScope.launch {
            // The run is over, so a stale snapshot must not linger behind
            // "Continue". Detached, because the player usually leaves the screen
            // within a second of this firing and a viewModelScope clear gets
            // cancelled on the way out.
            saveStore.clearDetached()
            // Personal best is only meaningful for a completed run.
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
/**
 * The torch, as an actual light in the world.
 *
 * uTorchPos is the lens face, uTorchDir the way the barrel points, both handed
 * over by drawTorch from the same arm chain that positions the model — so the
 * beam cannot drift away from the object casting it.
 *
 * This replaces a circle drawn at uv (0.5, 0.47) in the post pass. That circle
 * had no position in the world at all: it sat in the middle of the screen
 * whatever the torch was doing, which is exactly why the light read as coming
 * out of the player's chest. A screen-space disc also cannot respect a surface
 * normal, so a wall the beam grazed lit up as brightly as one it hit square on.
 */
uniform vec3 uTorchPos; uniform vec3 uTorchDir; uniform float uTorchOn;
uniform float uBumpStrength; uniform float uBumpTexel;
uniform vec3 uLampTint;
/**
 * Seconds. Everything the level's surfaces do over time is driven from here.
 *
 * The room used to be entirely static: every stain, seam and sag was a fixed
 * function of world position, so the only thing that ever moved was the flicker
 * on the tubes. A space that holds completely still reads as a diorama, and
 * that is what made the level look flat and lifeless no matter how much detail
 * was packed into it. Nothing below is fast — damp creeps over minutes, the
 * ceiling breathes over tens of seconds — because in a place like this the
 * point is that you are not sure whether it moved.
 */
uniform float uTime;
/**
 * Metres-to-UV, per texture.
 *
 * The mesher emits UVs in world METRES and this converts them, because the
 * three level textures are neither the same size nor the same aspect:
 * 1536x1024, 1448x1086 and 1024x1024. Mapping all three through one scale
 * stretched two of them onto square tiles and gave each surface a different
 * texel density — the floor finer than the wall, the wall finer than the
 * ceiling, and both of the non-square ones squashed along one axis. That is
 * what "the textures don't match" was.
 *
 * Set per draw group to (density/width, density/height), so one metre of world
 * covers the same number of texels on every surface and on both axes.
 */
uniform vec2 uUvScale;
out vec4 fragColor;

// Value noise and two octaves of it. Cheap, and this only ever runs at a very
// low spatial frequency — it is shaping metre-wide blotches, not texture.
float vhash(vec2 p){ return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }
float vnoise(vec2 p){
    vec2 i = floor(p), f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(vhash(i), vhash(i + vec2(1.0, 0.0)), u.x),
               mix(vhash(i + vec2(0.0, 1.0)), vhash(i + vec2(1.0, 1.0)), u.x), u.y);
}
float fbm2(vec2 p){ return vnoise(p) * 0.62 + vnoise(p * 2.17 + 4.1) * 0.38; }

void main(){
    vec2 uv = vUV * uUvScale;
    vec4 tex = texture(uTex, uv);

    // Derive a surface normal from the texture's own luminance slope, then
    // rotate it into the face's tangent frame. Cheap bump mapping with no
    // extra texture: the albedo doubles as a height field.
    vec3 n = normalize(vNormal);
    if (uBumpStrength > 0.001) {
        vec2 texel = vec2(uBumpTexel);
        float hL = dot(texture(uTex, uv - vec2(texel.x, 0.0)).rgb, vec3(0.299, 0.587, 0.114));
        float hR = dot(texture(uTex, uv + vec2(texel.x, 0.0)).rgb, vec3(0.299, 0.587, 0.114));
        float hD = dot(texture(uTex, uv - vec2(0.0, texel.y)).rgb, vec3(0.299, 0.587, 0.114));
        float hU = dot(texture(uTex, uv + vec2(0.0, texel.y)).rgb, vec3(0.299, 0.587, 0.114));
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
        //
        // Each tile breathes on its own phase, seeded from its own coordinates.
        // A ceiling where every panel sags in unison is a wave; one where they
        // drift independently is a suspended grid with something above it.
        vec2 tileId = floor(vWorldPos.xz / 1.6);
        float phase = fract(sin(dot(tileId, vec2(41.3, 289.1))) * 43758.5453);
        float breathe = 1.0 + 0.35 * sin(uTime * 0.21 + phase * 6.2831);
        float sag = 1.0 - 0.05 * breathe * (1.0 - min(d.x, d.y) * 4.0);
        albedo *= mix(1.0, sag, detailFade);

        // Water damage, creeping. A blotch field whose threshold drifts, so a
        // stain slowly spreads and pulls back over minutes rather than sitting
        // there as printed decoration. Concentrated near the grid lines, which
        // is where a leak actually tracks.
        float b = fbm2(vWorldPos.xz * 0.22 + vec2(uTime * 0.006, -uTime * 0.004));
        float creep = smoothstep(0.52 + 0.05 * sin(uTime * 0.05), 0.78, b);
        vec3 stainCol = vec3(0.52, 0.44, 0.28);
        albedo = mix(albedo, albedo * stainCol * 1.6, creep * 0.55 * detailFade);
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
        // Rising damp, creeping. The tide line drifts up and down the wall on a
        // long period, so the wall is wetter some minutes than others.
        float tide = 0.62 + 0.30 * sin(uTime * 0.037 + fract(u * 0.13) * 6.2831);
        float wet = fbm2(vec2(u * 0.28, vWorldPos.y * 0.55) + vec2(uTime * 0.005, 0.0));
        float rise = (1.0 - smoothstep(0.0, tide, vWorldPos.y)) * smoothstep(0.40, 0.72, wet);
        albedo = mix(albedo, albedo * vec3(0.58, 0.52, 0.40), rise * 0.5 * detailFade);
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

    // Torch. A real spotlight: cone about the barrel's axis, inverse-square
    // falloff, and Lambert against the bumped normal, so it slides across the
    // wall as she turns and dims on surfaces it only grazes.
    if (uTorchOn > 0.001) {
        vec3  toFrag = vWorldPos - uTorchPos;
        float d      = length(toFrag);
        vec3  L      = toFrag / max(d, 1e-4);
        float cosA   = dot(L, normalize(uTorchDir));
        // Hot core inside ~14 degrees, soft edge out to ~34.
        float cone   = smoothstep(0.83, 0.97, cosA);
        float atten  = 1.0 / (1.0 + 0.22 * d + 0.11 * d * d);
        float ndl    = max(dot(n, -L), 0.0);
        float beam   = cone * atten * uTorchOn;
        col += albedo * vec3(1.00, 0.96, 0.86) * beam * (0.25 + 1.75 * ndl);
        // A little of the beam catches the air in front of the lens.
        col += vec3(0.9, 0.87, 0.76) * cone * uTorchOn * 0.05
             * smoothstep(0.5, 4.0, d) * (1.0 - smoothstep(6.0, 16.0, d));
    }

    // Dust in the air, drifting across the lit volume between the surface and
    // the eye. Keyed to how brightly the surface is lit, because dust is only
    // ever visible where there is light to catch it — under a working tube you
    // see the air, in a dead hall you do not. This is what stops a long
    // corridor from being an empty plane of colour receding to fog.
    float dustField = fbm2(vec2(vWorldPos.x * 0.5 + uTime * 0.05,
                                vWorldPos.z * 0.5 - uTime * 0.031)
                           + vec2(vWorldPos.y * 0.3, 0.0));
    float dust = smoothstep(0.55, 0.95, dustField)
               * clamp(vLight, 0.0, 1.4)
               * smoothstep(1.0, 9.0, dist) * 0.06;
    col += uLampTint * dust * uFlicker;

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

/**
 * The things in the corridors.
 *
 * A dark, roiling silhouette with two large eyes and a wide grin — the Smiler
 * read, which is the one image this place is known for. The previous version
 * drew a soft blob with a single dot in it and had nothing to say; the point of
 * a creature glimpsed at the end of a corridor is that you recognise the face
 * before you recognise anything else.
 *
 * Everything is animated off uTime: the body edge boils, the grin widens as it
 * closes in, and the eyes blink on their own irregular schedule.
 */
private const val OMNI_BILLBOARD_FRAG = """#version 300 es
precision mediump float;
in vec2 vUV;
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

void main(){
    float t = uTime + uSeed * 37.0;

    // --- Body -------------------------------------------------------------
    // An upright ovoid whose edge is displaced by drifting noise, so the
    // silhouette never holds still. A shape that boils reads as alive; a
    // smooth ellipse reads as a decal.
    vec2 d = (vUV - vec2(0.5, 0.46)) * vec2(1.25, 1.0);
    float r = length(d);
    float ang = atan(d.y, d.x);
    float boil = noise(vec2(ang * 1.6, t * 0.9 + uSeed * 5.0)) - 0.5;
    float edge = 0.40 + boil * 0.055;
    float body = smoothstep(edge, edge - 0.13, r);

    // Wisps trailing off the bottom, densest at the hem.
    float hem = smoothstep(0.30, 0.85, vUV.y);
    float wisp = noise(vec2(vUV.x * 9.0, vUV.y * 4.0 - t * 1.4));
    body = max(body, smoothstep(0.62, 0.95, wisp) * (1.0 - hem) * 0.55);

    // --- Driven off ---------------------------------------------------------
    // The flashlight does not kill it, it makes it leave — so it has to come
    // apart rather than fade out. A uniform alpha ramp reads as a UI element
    // being hidden; eating the silhouette away along a noise field, edges
    // first, reads as something losing its hold on being there.
    if (uDissolve > 0.001) {
        float grain = noise(vUV * 7.0 + vec2(t * 0.6, -t * 0.35));
        // Bias the threshold by how far out the fragment is, so it comes apart
        // from the outside in and the face is the last thing left.
        float threshold = uDissolve * 1.35 - (0.45 - r) * 0.55;
        body -= smoothstep(threshold - 0.22, threshold + 0.10, grain + (1.0 - r));
    }

    if (body < 0.02) discard;

    // Near-black, with a faint tint so different creatures stay tellable apart.
    vec3 col = uColor * 0.055;

    // --- Eyes -------------------------------------------------------------
    // Two of them, large and set wide. Mirrored by folding x about the centre,
    // so one expression drives both.
    vec2 e = vec2(abs(vUV.x - 0.5), vUV.y);
    vec2 eyeC = vec2(0.115, 0.60);
    // Blink: a rare, quick squash. Irregular, because a metronome blink is
    // worse than none at all.
    float blinkPhase = fract(t * 0.21 + uSeed);
    float blink = 1.0 - smoothstep(0.0, 0.045, abs(blinkPhase - 0.5)) * 0.94;
    vec2 eyeD = (e - eyeC) / vec2(0.085, 0.062 * max(blink, 0.06));
    float eye = smoothstep(1.0, 0.72, length(eyeD));

    // --- Grin -------------------------------------------------------------
    // A crescent that widens and lifts as it takes an interest in you, with a
    // row of teeth cut into it.
    float grin = 0.0;
    {
        vec2 m = (vUV - vec2(0.5, 0.365)) / vec2(0.20 + uAlert * 0.045, 0.115);
        // Upper edge is a parabola, lower edge a wider one: the gap between is
        // the mouth.
        float curve = m.x * m.x;
        float lip = 1.0 - smoothstep(0.0, 0.30, abs(m.y + curve * 0.85 - 0.30));
        grin = lip * smoothstep(1.25, 1.0, abs(m.x));
        // Teeth: vertical cuts across the opening.
        float teeth = smoothstep(0.42, 0.62, abs(fract(m.x * 5.5) - 0.5) * 2.0);
        grin *= mix(1.0, teeth, 0.55);
    }

    vec3 normalRamp = mix(vec3(0.92, 0.86, 0.35), vec3(1.0, 0.06, 0.04), uAlert);
    vec3 safeRamp   = mix(vec3(0.30, 0.62, 1.0),  vec3(1.0, 0.62, 0.05), uAlert);
    vec3 lit = mix(normalRamp, safeRamp, uColorBlind);

    // Pupils sit inside the eye and track very slightly, which is the detail
    // that makes it feel watched rather than merely looked at.
    vec2 pupilOff = vec2(sin(t * 0.7) * 0.012, sin(t * 0.53) * 0.008);
    float pupil = smoothstep(1.0, 0.55, length((e - eyeC - pupilOff) / vec2(0.034, 0.034)));

    col = mix(col, lit, eye);
    col = mix(col, vec3(0.02, 0.01, 0.01), pupil * 0.85);
    col = mix(col, lit * 0.92, grin);

    // Glow bleeding out of the features into the body around them.
    col += lit * (eye + grin) * 0.30;

    fragColor = vec4(col, body * uAlpha);
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
    // The pool of dark a creature drags along the floor with it. Not a static
    // ellipse: the rim crawls, and a second, wider stain breathes underneath —
    // so the thing looks like it is displacing the light rather than having a
    // sprite pasted beneath it.
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

/**
 * The torch she carries.
 *
 * A lathed body — barrel, knurled grip, flared head, lens — built as a triangle
 * strip of revolution. Modelled rather than downloaded: it is nine rings of
 * eight segments, so authoring it in code costs less than an asset would, and
 * it inherits the scene's own palette instead of arriving with a baked one.
 *
 * Vertex layout matches the level mesh (pos, normal, uv, light) so it can share
 * the buffer conventions, but it has its own program because the shading is
 * metal-and-lens rather than baked room light.
 */
/**
 * Footstep decals.
 *
 * One quad per mark, laid flat on the carpet. The mark itself is drawn
 * procedurally in the fragment shader rather than sampled from an atlas: at
 * three mark kinds and a few dozen marks on screen, a texture would be more
 * bytes and more plumbing than the arithmetic it replaces, and a procedural
 * sole can spread and soften with age instead of just fading.
 *
 * aUv is the position within the mark, -1..1 on both axes. aLight carries the
 * mark's age, 0 at birth and 1 when it is gone.
 */
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
in vec2 vUv; in float vAge; in float vLit;
/** Tint from the trail's own entry in Native/Trail. */
uniform vec3 uTint;
/** 0 sole, 1 static glyph, 2 grain. */
uniform float uMark;
uniform float uTime;
out vec4 fragColor;

float hash(vec2 p){ return fract(sin(dot(p, vec2(41.3, 289.1))) * 43758.5453); }

/** A shoe print: ball of the foot and a separate heel. */
float sole(vec2 p){
    vec2 ball = p - vec2(0.0, 0.26);
    ball.x /= 0.52; ball.y /= 0.60;
    float b = 1.0 - smoothstep(0.55, 1.0, length(ball));
    vec2 heel = p - vec2(0.0, -0.42);
    heel.x /= 0.40; heel.y /= 0.34;
    float h = 1.0 - smoothstep(0.55, 1.0, length(heel));
    // Tread: bands across the sole, so it reads as a shoe and not a blob.
    float tread = 0.72 + 0.28 * step(0.0, sin(p.y * 34.0));
    return max(b, h) * tread;
}

/** A torn block of interference. */
float glyph(vec2 p, float t){
    float rows = floor((p.y * 0.5 + 0.5) * 7.0);
    float jitter = (hash(vec2(rows, floor(t * 9.0))) - 0.5) * 0.5;
    float band = step(abs(p.x + jitter), 0.72) * step(abs(p.y), 0.85);
    float noise = step(0.42, hash(vec2(floor(p.x * 14.0) + jitter * 20.0, rows)));
    return band * noise;
}

/** A scatter of crystalline grains. */
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
    // Outside the stamp entirely: nothing to blend.
    if (dot(vUv, vUv) > 1.6) discard;

    float shape;
    if (uMark < 0.5)      shape = sole(vUv);
    else if (uMark < 1.5) shape = glyph(vUv, uTime);
    else                  shape = grain(vUv);

    // Fade out over the mark's life, and soften the edge as it goes — an old
    // print has spread into the pile rather than merely gone faint.
    float soften = mix(1.0, 0.35, vAge);
    shape *= soften;
    float fade = 1.0 - vAge;
    fade *= fade;

    float a = shape * fade * 0.85;
    if (a < 0.004) discard;
    // Modulated by the floor's own baked light, so a print in a dark hall is
    // dark. A decal that ignores the lighting reads as a sticker.
    fragColor = vec4(uTint * clamp(vLit, 0.05, 1.6), a);
}
"""

private const val OMNI_TORCH_VERT = """#version 300 es
layout(location=0) in vec3 aPos;
layout(location=1) in vec3 aNormal;
layout(location=2) in vec2 aUV;
layout(location=3) in float aPart;   // 0 body, 1 head, 2 lens
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
in vec3 vNormal; in float vPart; in float vAxial;
uniform float uOn;
uniform vec3 uAmbient;
out vec4 fragColor;
void main(){
    vec3 n = normalize(vNormal);
    // Fixed key light from above-front, matching the level's ceiling sources.
    vec3 key = normalize(vec3(-0.35, 0.86, 0.38));
    float ndl = max(dot(n, key), 0.0);
    float rim = pow(1.0 - abs(n.z), 2.5) * 0.35;

    vec3 col;
    if (vPart > 1.5) {
        // Lens. Dark glass when off; when on it is the brightest thing in the
        // frame, which is what sells the torch actually being the light source.
        vec3 dark = vec3(0.10, 0.10, 0.12);
        vec3 hot  = vec3(1.0, 0.97, 0.84) * 2.6;
        col = mix(dark, hot, uOn);
    } else if (vPart > 0.5) {
        // Head: brushed aluminium, brighter than the body.
        col = vec3(0.52, 0.53, 0.56) * (0.30 + ndl * 0.85) + rim;
        // Spill from the lens washes back over the head when lit.
        col += vec3(1.0, 0.94, 0.78) * uOn * (1.0 - vAxial) * 0.45;
    } else {
        // Body: dark rubberised grip with a knurled band.
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

    // The torch is a real light in the scene pass now — see uTorchPos there.
    // What is left here is only the glare the lens itself throws into the lens
    // of the camera, which is a screen-space effect and belongs in a screen-
    // space pass.
    if (uFlashOn > 0.5) {
        col *= 1.02;
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

/** Fittings are a flat colour, so their UVs pass through unscaled. */
private val LAMP_UV = floatArrayOf(1f, 1f)

/**
 * Floor division that behaves for negative numerators.
 *
 * The world runs in every direction, so cell -1 has to land in chunk -1, not
 * chunk 0 — Kotlin's `/` truncates toward zero and would put it in 0, which
 * mirrors a whole quadrant of chunk lookups onto the wrong chunk. Written out
 * rather than taken from the stdlib because `floorDiv` there is an extension
 * added in a later version than this module's language level guarantees.
 */
private fun floorDivInt(a: Int, b: Int): Int {
    val q = a / b
    return if (a % b != 0 && (a < 0) != (b < 0)) q - 1 else q
}

class OmniGLRenderer(private val appContext: Context) : GLSurfaceView.Renderer {

    @Volatile var latestState: GameState = GameState()
    @Volatile var renderSettings: RenderSettings = RenderSettings()
    /** "first" or "third". Third pulls the camera back and draws the avatar. */
    @Volatile var cameraView: String = "first"
    /** 0 standing, 1 flat on the floor. Drives both the collapse and the
     *  arrival's recovery, since they are one motion run in two directions. */
    private var avatarCollapse = 0f

    // Avatar resources, loaded only when third person is actually available.
    private var charProgram = 0
    private var charVbo = 0; private var charIbo = 0; private var charIndexCount = 0
    private var charTex = 0
    private var shaftProgram = 0
    private var sMVP = 0; private var sFlicker = 0; private var sTint = 0
    private var cBones = 0
    /** Pose is built on the CPU now; see PoseBuilder. */
    private val charPose = PoseBuilder()
    private var cMVP = 0; private var cModel = 0
    private var cTexU = 0; private var cIsChar = 0
    private var cAnimate = 0
    private var cSubject = 0

    // The torch she carries in third person.
    private var torchProgram = 0
    private var torchVbo = 0; private var torchIbo = 0; private var torchIndexCount = 0
    private var tMVP = 0; private var tModel = 0; private var tOn = 0; private var tAmbient = 0

    // Footstep decals. The mesh is rebuilt every frame from the native trail,
    // so it lives in one preallocated buffer rather than being reallocated.
    private var decalProgram = 0
    private var dMVP = 0; private var dTint = 0; private var dMark = 0; private var dTime = 0
    private var decalVbo = 0
    /** 4 verts * 9 floats per stamp, capped at the native ring's capacity. */
    private val decalVerts = FloatArray(TRAIL_CAPACITY * 4 * 9)
    private var decalQuads = 0
    private val decalBuf = ByteBuffer.allocateDirect(decalVerts.size * 4)
        .order(ByteOrder.nativeOrder()).asFloatBuffer()
    private var decalIbo = 0
    private var decalTint = floatArrayOf(0.72f, 0.66f, 0.50f)
    private var decalMark = 0f
    private var decalScale = 0.30f
    private var decalSpread = 1.9f

    /** Supplies the live stamps. Set by the game screen; null before a run. */
    @Volatile var trailSource: (() -> FloatArray?)? = null

    /** Applies a trail's own entry from Native/Trail: tint, size and mark kind. */
    fun setTrailStyle(spec: FloatArray?) {
        if (spec == null || spec.size < 7) return
        decalTint = floatArrayOf(spec[0], spec[1], spec[2])
        decalScale = spec[4]
        decalSpread = spec[5]
        decalMark = spec[6]
    }
    private val torchModelM = FloatArray(16)
    private val torchMvpM = FloatArray(16)
    /** Eased 0..1 raise of the torch arm, so switching it on is a movement. */
    private var torchRaise = 0f
    /** Eased crouch and airborne blends for the avatar rig. */
    private var avatarCrouch = 0f
    private var avatarAir = 0f
    /** Where the head is turned relative to the body. */
    private var headYaw = 0f
    private var lastBodyYaw = 0f
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
    private var uTorchPos = 0; private var uTorchDir = 0; private var uTorchOn = 0
    /** Drives the level's creeping damp, breathing ceiling and airborne dust. */
    private var uSceneTime = 0
    private var uUvScale = 0
    private var bVP = 0; private var bCenter = 0; private var bRight = 0; private var bUp = 0
    private var bSize = 0; private var bColor = 0; private var bAlert = 0; private var bAlpha = 0; private var bColorBlind = 0
    private var bTime = 0; private var bSeed = 0; private var bDissolve = 0
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
    private var sTime = 0; private var sSeed = 0
    private var exitProgram = 0
    private var xVP = 0; private var xCenter = 0; private var xRight = 0
    private var xWidth = 0; private var xHeight = 0; private var xTime = 0; private var xNear = 0

    private var floorTex = 0; private var wallTex = 0; private var roofTex = 0
    /** Metres-to-UV scale per texture, derived from its pixel size so every
     *  surface ends up at the same texel density. See uUvScale in the shader. */
    private var floorUv = floatArrayOf(0.5f, 0.5f)
    private var wallUv  = floatArrayOf(0.5f, 0.5f)
    private var roofUv  = floatArrayOf(0.5f, 0.5f)
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
    /** Third-person boom length after collision, eased so the camera slides in
     *  and out rather than snapping when it clears an obstruction. */
    private var smoothCamDist = 0f
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
        torchIndexCount = 0
        torchRaise = 0f; avatarCrouch = 0f; avatarAir = 0f; headYaw = 0f
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
        uTorchPos = GLES30.glGetUniformLocation(sceneProgram, "uTorchPos")
        uTorchDir = GLES30.glGetUniformLocation(sceneProgram, "uTorchDir")
        uTorchOn = GLES30.glGetUniformLocation(sceneProgram, "uTorchOn")
        uSceneTime = GLES30.glGetUniformLocation(sceneProgram, "uTime")
        uUvScale = GLES30.glGetUniformLocation(sceneProgram, "uUvScale")

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
        bTime = GLES30.glGetUniformLocation(billboardProgram, "uTime")
        bSeed = GLES30.glGetUniformLocation(billboardProgram, "uSeed")
        bDissolve = GLES30.glGetUniformLocation(billboardProgram, "uDissolve")

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
        sTime = GLES30.glGetUniformLocation(shadowProgram, "uTime")
        sSeed = GLES30.glGetUniformLocation(shadowProgram, "uSeed")

        // Avatar: shares the preview's shader, which already implements the
        // joint rotation that breaks the source mesh's T-pose.
        runCatching {
            charProgram = linkGlProgram(OMNI_PREVIEW_VERT, OMNI_PREVIEW_FRAG)
            cMVP = GLES30.glGetUniformLocation(charProgram, "uMVP")
            cModel = GLES30.glGetUniformLocation(charProgram, "uModel")
            cTexU = GLES30.glGetUniformLocation(charProgram, "uTex")
            cIsChar = GLES30.glGetUniformLocation(charProgram, "uIsCharacter")
            cAnimate = GLES30.glGetUniformLocation(charProgram, "uAnimate")
            cSubject = GLES30.glGetUniformLocation(charProgram, "uSubject")
            cBones = GLES30.glGetUniformLocation(charProgram, "uBones")

            torchProgram = linkGlProgram(OMNI_TORCH_VERT, OMNI_TORCH_FRAG)
            tMVP = GLES30.glGetUniformLocation(torchProgram, "uMVP")
            tModel = GLES30.glGetUniformLocation(torchProgram, "uModel")
            tOn = GLES30.glGetUniformLocation(torchProgram, "uOn")
            tAmbient = GLES30.glGetUniformLocation(torchProgram, "uAmbient")
            decalProgram = linkGlProgram(OMNI_DECAL_VERT, OMNI_DECAL_FRAG)
            dMVP  = GLES30.glGetUniformLocation(decalProgram, "uMVP")
            dTint = GLES30.glGetUniformLocation(decalProgram, "uTint")
            dMark = GLES30.glGetUniformLocation(decalProgram, "uMark")
            dTime = GLES30.glGetUniformLocation(decalProgram, "uTime")
            decalVbo = genGlBuffer()
            // Index buffer is fixed: two triangles per stamp, forever.
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

        floorTex = loadOmniTexture("Level_0/Floor.png", 0xFF3A3020.toInt(), floorUv)
        wallTex  = loadOmniTexture("Level_0/Wall.png",  0xFF4A4030.toInt(), wallUv)
        roofTex  = loadOmniTexture("Level_0/Roof.png",  0xFF23210F.toInt(), roofUv)
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
            // it, so the avatar sits in the lower third of frame — but only as
            // far back as the room allows.
            val thirdPerson = cameraView == "third" && charIndexCount > 0
            val ceiling = if (state.world.isValid) state.world.height else 2.6f
            // Orbit her chest, not her eyes.
            //
            // The boom used to pivot on the eye and then add 0.42 m on top, which
            // in a 2.6 m room put the lens at 2.12 m — a hand's width under the
            // ceiling, scraping every doorway, and looking down at her steeply
            // enough that she filled the frame and read as far taller than the
            // 1.70 m she is. Pivoting lower keeps the lens in the middle of the
            // corridor where there is actually room for it.
            val pivotY = if (thirdPerson) (eyeY - 0.45f) else eyeY
            val camLift = if (thirdPerson) 0.12f else 0f
            val wantDist = if (thirdPerson) 2.6f else 0f
            val camDist = if (thirdPerson) {
                smoothCamDist += (resolveCameraDistance(
                    smoothX, pivotY + camLift, smoothZ,
                    -fx, -fy, -fz, wantDist, state.world, ceiling
                ) - smoothCamDist) * (1f - kotlin.math.exp(-dt * 14f))
                smoothCamDist
            } else {
                smoothCamDist = 0f
                0f
            }
            val eyeX = smoothX - fx * camDist
            val eyeZ = smoothZ - fz * camDist
            // Even at zero distance the lens must stay inside the room: the lift
            // alone can push it into the ceiling in a low corridor.
            val camY = (pivotY - fy * camDist + camLift).coerceIn(0.30f, ceiling - 0.30f)
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
            // Rolling the camera is right in first person — the camera IS the
            // head, and a head that has hit the floor is on its side. In third
            // person it is wrong in a way that reads instantly as a bug: the
            // body stays upright in frame and the whole SCREEN rotates around
            // it. The collapse now happens in the skeleton instead, so third
            // person watches her go down rather than watching the picture spin.
            val wantTilt = if (thirdPerson) 0f else state.cameraTilt
            smoothTilt += (wantTilt - smoothTilt) * chase
            if (kotlin.math.abs(smoothTilt) > 0.01f) {
                Matrix.setIdentityM(rollM, 0)
                Matrix.rotateM(rollM, 0, smoothTilt, 0f, 0f, 1f)
                Matrix.multiplyMM(rolledViewM, 0, rollM, 0, viewM, 0)
                System.arraycopy(rolledViewM, 0, viewM, 0, 16)
            }
            Matrix.multiplyMM(vpM, 0, projM, 0, viewM, 0)

            // Where the beam comes from.
            //
            // Third person takes it off the torch model's lens, computed in
            // updateTorchLight just before the model is drawn -- so the level
            // reads it one frame late, which at 60Hz is 16ms of lag on a light
            // held in a slowly-moving hand and is not visible. First person has
            // no model to take it from, so the beam starts where a torch held
            // in the right hand would be: forward of the eye, a little right
            // and below it, pointing where she looks.
            if (!thirdPerson) {
                val yawR = Math.toRadians(smoothYaw.toDouble()).toFloat()
                val pitR = Math.toRadians(smoothPitch.toDouble()).toFloat()
                val fx = kotlin.math.sin(yawR) * kotlin.math.cos(pitR)
                val fy = -kotlin.math.sin(pitR)
                val fz = -kotlin.math.cos(yawR) * kotlin.math.cos(pitR)
                val rx = kotlin.math.cos(yawR)
                val rz = kotlin.math.sin(yawR)
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
                val ease = 1f - kotlin.math.exp(-dt * 9f)

                // Pose blends, all eased so nothing in the rig ever snaps.
                avatarCrouch += ((if (state.isCrouching) 1f else 0f) - avatarCrouch) * ease
                // Airborne is read off the body, not off a flag: any upward or
                // rapid downward motion of the feet counts, which covers jumps,
                // falls and the arrival drop without three separate signals.
                val airborne = if (feetY > 0.10f) 1f else 0f
                avatarAir += (airborne - avatarAir) * ease
                torchRaise += ((if (state.flashlightOn) 1f else 0f) - torchRaise) * (1f - kotlin.math.exp(-dt * 7f))

                // Head lead: she turns her head into a turn before her body
                // follows. Driven from how fast the view is yawing, decaying
                // back to centre when the player stops turning.
                var yawDelta = cam.yaw - lastBodyYaw
                while (yawDelta > 180f) yawDelta -= 360f
                while (yawDelta < -180f) yawDelta += 360f
                lastBodyYaw = cam.yaw
                val targetHead = (yawDelta * 0.09f).coerceIn(-0.62f, 0.62f)
                headYaw += (targetHead - headYaw) * (1f - kotlin.math.exp(-dt * 5f))
                val headPitch = (-Math.toRadians(smoothPitch.toDouble()).toFloat() * 0.45f)
                    .coerceIn(-0.40f, 0.40f)

                val walkBlend = (avatarSpeed / 3.6f).coerceIn(0f, 1.6f)

                // On the floor, or getting off it.
                //
                // The arrival already eases eyeOffset from -1.45 back to 0 while
                // the body picks itself up, and the sanity collapse already eases
                // it the other way. Reading the collapse off that number rather
                // than starting a second timer is what keeps the body and the
                // camera on the same schedule — two independent easings of the
                // same event drift, and a body that stands up before the view
                // does is worse than no animation at all.
                val collapseTarget = when {
                    state.isMadnessOver || state.isGameOver -> 1f
                    state.spawnPhase == SpawnPhase.LANDED ->
                        (state.eyeOffset / -1.45f).coerceIn(0f, 1f)
                    else -> 0f
                }
                // The arrival's value is already eased by the view model; only
                // the death needs easing here, and easing it twice would make
                // the recovery lag the camera by a visible fraction of a second.
                avatarCollapse =
                    if (state.spawnPhase == SpawnPhase.LANDED) collapseTarget
                    else avatarCollapse + (collapseTarget - avatarCollapse) * (1f - kotlin.math.exp(-dt * 3.2f))

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

    private fun drawLevel(
        vp: FloatArray, camX: Float, camY: Float, camZ: Float,
        fogDensity: Float, flicker: Float, bumpStrength: Float,
        /** Seconds since start — drives the level's creeping damp, the
         *  breathing ceiling and the airborne dust. */
        timeSec: Float,
        /** Needed to look up the baked light under a footstep decal. */
        world: WorldInfo
    ) {
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
        GLES30.glUniform3f(uTorchPos, torchLightPos[0], torchLightPos[1], torchLightPos[2])
        GLES30.glUniform3f(uTorchDir, torchLightDir[0], torchLightDir[1], torchLightDir[2])
        GLES30.glUniform1f(uTorchOn, torchLightOn)
        GLES30.glUniform1f(uSceneTime, timeSec)
        // Bump detail scales with quality: off on low, subtle on medium, full
        // on high. The texel step controls how coarse the derived relief is.
        GLES30.glUniform1f(uBumpStrength, bumpStrength)
        GLES30.glUniform1f(uBumpTexel, 1.0f / 512f)
        // Grouped by texture across all resident chunks, so the whole world
        // costs three texture binds rather than three per chunk.
        for (m in chunkMeshes.values) drawMeshGroup(m.floorVbo, m.floorIbo, m.floorCount, floorTex, floorUv)
        for (m in chunkMeshes.values) drawMeshGroup(m.roofVbo,  m.roofIbo,  m.roofCount,  roofTex,  roofUv)
        for (m in chunkMeshes.values) drawMeshGroup(m.wallVbo,  m.wallIbo,  m.wallCount,  wallTex,  wallUv)
        // Fixtures last: their high baked light makes them read as emitters.
        // Flat colour, so its UVs need no scaling at all.
        for (m in chunkMeshes.values) drawMeshGroup(m.fixVbo, m.fixIbo, m.fixCount, lampTex, LAMP_UV)

        // Footsteps go down after the floor and before anything translucent, so
        // they blend against the carpet they are lying on rather than against
        // whatever a light shaft has already added over it.
        drawTrailDecals(vp, timeSec) { wx, wz -> lightAtWorld(wx, wz, world) }

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

    private fun drawMeshGroup(vbo: Int, ibo: Int, indexCount: Int, tex: Int, uvScale: FloatArray) {
        if (indexCount <= 0) return
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex)
        GLES30.glUniform1i(uTex, 0)
        GLES30.glUniform2f(uUvScale, uvScale[0], uvScale[1])
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
            // isAway is a creature that has been driven off and is waiting out
            // of sight. It is still being simulated — it has to be, or it could
            // never come back — but there is nothing left of it to draw.
            if (!e.isActive || e.isAway) continue
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
            // Per-creature offset, so a row of them never boils or blinks in
            // unison — nothing gives away a shared shader faster than that.
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
            // Thins out with the body, or a creature that has been driven off
            // leaves its shadow behind on the carpet.
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

    /**
     * Builds the torch as a solid of revolution.
     *
     * Nine rings along the barrel, each with a radius and a part tag; the strip
     * between consecutive rings becomes the surface. Modelled in code because it
     * is a lathe form — nothing an imported asset would give us is worth the
     * loader, the file, or the licence.
     *
     * Local axes: +Z is the direction the beam leaves, origin at the grip so the
     * hand transform can place it without an offset.
     */
    private fun buildTorchMesh(): Pair<FloatArray, IntArray> {
        // (z along the barrel, radius, part tag)
        val profile = arrayOf(
            floatArrayOf(-0.070f, 0.000f, 0f),  // butt cap centre
            floatArrayOf(-0.070f, 0.020f, 0f),  // butt rim
            floatArrayOf(-0.030f, 0.023f, 0f),  // grip
            floatArrayOf( 0.020f, 0.022f, 0f),  // knurled barrel
            floatArrayOf( 0.052f, 0.024f, 0f),  // step up to the head
            floatArrayOf( 0.058f, 0.034f, 1f),  // head shoulder
            floatArrayOf( 0.086f, 0.041f, 1f),  // head flare
            floatArrayOf( 0.092f, 0.040f, 2f),  // bezel
            floatArrayOf( 0.093f, 0.036f, 2f)   // lens face
        )
        val sides = 10
        val verts = ArrayList<Float>()
        val idx = ArrayList<Int>()

        for (r in profile.indices) {
            val z = profile[r][0]; val rad = profile[r][1]; val part = profile[r][2]
            // Slope of the profile gives the correct normal for a lathe surface.
            val prev = profile[max(r - 1, 0)]
            val next = profile[min(r + 1, profile.lastIndex)]
            val dz = next[0] - prev[0]
            val dr = next[1] - prev[1]
            val len = kotlin.math.hypot(dz, dr).coerceAtLeast(1e-5f)
            val nRad = dz / len          // radial component of the normal
            val nAxial = -dr / len       // axial component
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
        // Lens disc, so the beam face is solid rather than an open tube.
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

    /**
     * Places the torch in her right hand.
     *
     * The hand's position is derived from the same shoulder pivot and the same
     * rotations the vertex shader applies to the arm, evaluated here on the CPU.
     * Keeping the two in step is the price of skinning in a vertex shader with
     * no bone buffer to read back; the alternative — a second, authoritative
     * skeleton — is far more machinery than one prop is worth.
     */
    /** Lens position and beam direction in world space, and how lit it is. */
    private val torchLightPos = floatArrayOf(0f, 0f, 0f)
    private val torchLightDir = floatArrayOf(0f, 0f, 1f)
    private var torchLightOn = 0f

    /**
     * Builds torchModelM and reads the lens position and beam axis off it.
     *
     * Split out of drawTorch because the level is drawn first and needs the
     * light before the torch itself is drawn. Both go through here, so the beam
     * and the object it comes out of cannot disagree — which is the whole point:
     * the old screen-space circle had no relationship to the model at all.
     */
    private fun updateTorchLight(
        px: Float, py: Float, pz: Float, yawDeg: Float,
        timeSec: Float, walk: Float, torch: Float, crouch: Float, on: Boolean
    ) {
        buildTorchMatrix(px, py, pz, yawDeg, timeSec, walk, torch, crouch)
        // The barrel is modelled along local +Z with the lens face at z=0.093
        // (see buildTorchMesh's profile), so the tip and the beam axis are that
        // point and that axis pushed through the model matrix.
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
        // --- Mirror of the shader's arm chain, right side only ---------------
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

        // Upper arm down to the elbow, then forearm out to the hand. Lengths are
        // in the mesh's own unit-height space.
        val upperLen = 0.14f
        val foreLen = 0.16f
        // Start hanging straight down from the shoulder, then apply the chain.
        var hx = 0f; var hy = -upperLen; var hz = 0f
        // Shoulder pitch about X.
        var ry = hy * cos(shoulderPitch) - hz * sin(shoulderPitch)
        var rz = hy * sin(shoulderPitch) + hz * cos(shoulderPitch)
        hy = ry; hz = rz
        // Shoulder roll about Z.
        var rx = hx * cos(shoulderRoll) - hy * sin(shoulderRoll)
        ry = hx * sin(shoulderRoll) + hy * cos(shoulderRoll)
        hx = rx; hy = ry
        val elbowX = shoulderX + hx; val elbowY = shoulderY + hy; val elbowZ = hz
        // Forearm, carrying the shoulder's rotation plus the elbow's.
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
        // Crouching drops the whole upper body; the hand rides down with it.
        handY -= 0.38f * crouch

        Matrix.setIdentityM(torchModelM, 0)
        Matrix.translateM(torchModelM, 0, px, py, pz)
        Matrix.rotateM(torchModelM, 0, yawDeg, 0f, 1f, 0f)
        Matrix.scaleM(torchModelM, 0, AVATAR_SCALE, AVATAR_SCALE, AVATAR_SCALE)
        Matrix.translateM(torchModelM, 0, handX, handY, handZ)
        // Point the beam along the arm: level and forward when raised, angled
        // down at her side when stowed.
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

    /**
     * Rebuilds and draws the footstep decals.
     *
     * The stamps come straight out of Native/Trail — the ring buffer there is
     * the only record of what is on the floor, and it keeps ageing whether or
     * not the player is still walking. This is the read side: one quad per live
     * stamp, laid flat just above the carpet, oriented to the direction of
     * travel and grown by its own age.
     *
     * Depth WRITES are off. A decal that writes depth fights the floor it is
     * lying on and z-fights along every edge; testing against the floor while
     * not writing is what lets a print sit on the carpet and still be occluded
     * by a wall in front of it.
     */
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
            // Marks spread as they age, by the amount the style asks for.
            val half = decalScale * 0.5f * (1f + (decalSpread - 1f) * age)
            // The print's own axes: forward along the walk, right across it.
            val fx = cos(yaw); val fz = -sin(yaw)
            val rx = -fz;      val rz = fx
            val lit = lightAt(sx, sz)
            // Slightly proud of the floor. Any less and the depth buffer cannot
            // separate them at range; any more and the mark visibly floats.
            val y = 0.012f
            // Corner order matches the fixed index buffer: (-r,-f) (+r,-f)
            // (+r,+f) (-r,+f), with UV -1..1 across the mark.
            val cx = floatArrayOf(-1f, 1f, 1f, -1f)
            val cz2 = floatArrayOf(-1f, -1f, 1f, 1f)
            for (k in 0 until 4) {
                val ox = (rx * cx[k] + fx * cz2[k]) * half
                val oz = (rz * cx[k] + fz * cz2[k]) * half
                decalVerts[v++] = sx + ox; decalVerts[v++] = y; decalVerts[v++] = sz + oz
                // aNormal.x carries the baked light; the rest is unused here.
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

    /** Draws the player's own avatar. Third person only. [py] is the FEET. */
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
        // Without this the avatar takes the backdrop's shading branch: no alpha
        // cutout, so the texture's transparent regions render as solid blocks,
        // and none of the cel banding that gives her form any read at all.
        GLES30.glUniform1f(cIsChar, 1f)
        GLES30.glUniform1f(cAnimate, 1f)
        // Only the studio backdrop reads this; in the corridors the
        // character branch returns before it is touched.
        GLES30.glUniform3f(cSubject, px, py, pz)

        Matrix.setIdentityM(avatarModelM, 0)
        Matrix.translateM(avatarModelM, 0, px, py, pz)
        // The mesh faces +Z at yaw 0, matching the engine's forward convention.
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
        // Skinning: four bone indices then four weights, both derived at load.
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

        /**
         * Same, but with a UV given explicitly per corner.
         *
         * [quad] hands out UVs in a fixed corner order — p0 gets (u0,v0), p1
         * gets (u1,v0), and so on — which is only correct for a quad whose
         * vertices are wound in that same order. The ceiling's are not: it is
         * wound the other way round so it faces down, so p1 sits at (x0,z1)
         * while being handed the UV for (x1,z0). The result was a ceiling
         * texture mirrored across its own diagonal, on every single tile, which
         * meant the pattern could not run continuously from one tile into the
         * next no matter what the texel density was.
         */
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
        // UVs are emitted in world METRES. The shader scales them per texture,
        // which is the only way three differently-sized, differently-shaped
        // textures can end up at one texel density.
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

                val u0 = x0; val u1 = x1
                val v0 = z0; val v1 = z1
                val wallV0 = 0f; val wallV1 = hgt

                // Floor and ceiling are emitted for EVERY open cell, always.
                //
                // They used to be skipped on two features, and each skip left a
                // one-cell hole with nothing behind it — the player saw straight
                // through the world and read it as a corrupted tile. Both were
                // single cells, and both were common enough to meet regularly:
                // kFeatureHole lands on 0.8% of fully-open floor, scattered one
                // at a time in the middle of a room, and kFeatureDoorway on 28%
                // of corridor cells.
                //
                // A hole in the floor was never coherent anyway: the cell is
                // walkable as far as collision is concerned, so the player
                // strolled across a gap they could see the void through. It is
                // now a damaged patch — same floor, sunk into shadow — which is
                // the reading the feature was always after.
                val floorDim = if (feature == 4) 0.34f else 1f
                floorB = quad(
                    floorV, floorI, floorB,
                    floatArrayOf(x0, 0f, z0), floatArrayOf(x1, 0f, z0),
                    floatArrayOf(x1, 0f, z1), floatArrayOf(x0, 0f, z1),
                    floatArrayOf(0f, 1f, 0f),
                    c00 * floorDim, c10 * floorDim, c11 * floorDim, c01 * floorDim,
                    u0, v0, u1, v1
                )
                // A doorway still has a ceiling over it — the lintel below is a
                // soffit under the tile, not a replacement for it.
                roofB = quadUv(
                    roofV, roofI, roofB,
                    floatArrayOf(x0, hgt, z0), floatArrayOf(x0, hgt, z1),
                    floatArrayOf(x1, hgt, z1), floatArrayOf(x1, hgt, z0),
                    floatArrayOf(0f, -1f, 0f),
                    c00 * 1.12f, c01 * 1.12f, c11 * 1.12f, c10 * 1.12f,
                    // One UV per corner, in the ceiling's OWN winding order, so
                    // each vertex gets the texture coordinate for where it
                    // actually is in the world.
                    floatArrayOf(u0, v0,  u0, v1,  u1, v1,  u1, v0)
                )

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
                    // ---- Recessed 2x4 fluorescent troffer -------------------
                    //
                    // Built the way the real fitting is, because it is the most
                    // looked-at object in the level and a single glowing
                    // rectangle read as a sticker on the ceiling. From the top
                    // down: a steel pan recessed into the grid, parabolic side
                    // reflectors angling light downward, solid end plates,
                    // four tubes on their sockets, and a diffuser haze under
                    // the whole assembly.
                    //
                    // Everything is emitted with an explicit downward normal so
                    // the baked shading treats the parts as ceiling-facing even
                    // where a face is really vertical — a light fitting reads
                    // wrong if its own reflectors fall into shadow.
                    val midX = x0 + cs * 0.5f
                    val midZ = z0 + cs * 0.5f
                    val halfL = cs * 0.38f          // along the tubes (the long axis)
                    val panHalfW = cs * 0.21f       // across them, at the ceiling
                    val mouthHalfW = cs * 0.27f     // across them, at the open face
                    val lit = fixture == 1
                    val down = floatArrayOf(0f, -1f, 0f)

                    // Recessed a clear 4 cm into the ceiling rather than 5 mm.
                    // Coplanar-ish surfaces are what let the depth buffer flip
                    // between them at distance; the gap has to be bigger than
                    // the buffer's resolution at the far end of a corridor.
                    val panY = hgt - 0.04f          // steel pan, recessed into the tile
                    val mouthY = hgt - 0.105f       // the open face of the fitting
                    val tubeY = hgt - 0.070f

                    // Ballast whine varies fitting to fitting: a tiny per-cell
                    // offset so a row of them is never uniformly bright.
                    val jitter = ((lx * 73 + lz * 151) % 17) / 17f
                    val emit = if (lit) 3.0f + jitter * 0.5f else 0.14f
                    val panLight = if (lit) 0.60f else 0.09f
                    val reflectorLight = if (lit) 1.45f else 0.12f

                    // 1. Pan.
                    fixB = quadFlat(
                        fixV, fixI, fixB,
                        floatArrayOf(midX - halfL, panY, midZ - panHalfW),
                        floatArrayOf(midX - halfL, panY, midZ + panHalfW),
                        floatArrayOf(midX + halfL, panY, midZ + panHalfW),
                        floatArrayOf(midX + halfL, panY, midZ - panHalfW),
                        down, panLight, 0f, 0f, 1f, 1f
                    )

                    // 2. Side reflectors, splaying out and down from the pan.
                    // Bright, because in the real thing they are polished and
                    // throwing the tubes' light back into the room.
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

                    // 3. End plates, closing the fitting off at both ends.
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

                    // 4. Four T8 tubes. Each is a shallow triangular prism
                    // rather than a flat strip: two faces angled off the
                    // vertical give it a lit edge and a shaded one, which is
                    // what makes a tube look round instead of painted on.
                    val tubes = 4
                    val tubeHalfD = cs * 0.021f
                    val tubeDrop = 0.016f
                    for (t in 0 until tubes) {
                        val f = (t + 0.5f) / tubes
                        val tz = midZ + (f - 0.5f) * (panHalfW * 1.72f)
                        // Underside, the brightest face.
                        fixB = quadFlat(
                            fixV, fixI, fixB,
                            floatArrayOf(midX - halfL * 0.90f, tubeY - tubeDrop, tz - tubeHalfD),
                            floatArrayOf(midX - halfL * 0.90f, tubeY - tubeDrop, tz + tubeHalfD),
                            floatArrayOf(midX + halfL * 0.90f, tubeY - tubeDrop, tz + tubeHalfD),
                            floatArrayOf(midX + halfL * 0.90f, tubeY - tubeDrop, tz - tubeHalfD),
                            down, emit, 0f, 0f, 1f, 1f
                        )
                        // Two shoulders rolling up to the socket line.
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
                        // Socket caps: short dark stubs at each end. Cheap, and
                        // they are what stop the tube looking like it floats.
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

                    // 5. Diffuser: one faint sheet across the mouth, sitting
                    // below the tubes. Softens the gaps between them without
                    // hiding that there are four distinct tubes up there.
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

    /**
     * Is this world point inside solid fill?
     *
     * Answered from the resident chunk data rather than from the engine, because
     * this runs on the GL thread every frame and must not reach across a JNI
     * call to do it. Points outside the resident ring report solid, which is the
     * conservative answer: it keeps the third-person camera pulled in rather
     * than letting it drift into geometry that has not streamed yet.
     */
    /** Baked illuminance at a world position, for decals that must obey the
     *  room's own lighting instead of glowing in a dark hall. */
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

    /**
     * Pulls the third-person camera in until it is clear of the level.
     *
     * Without this the camera simply sat [dist] behind the player wherever that
     * landed — inside the wall behind them, above the suspended ceiling, under
     * the floor. Looking up drove it through the ceiling and the shot became the
     * room seen from inside the slab above it; looking down did the same through
     * the floor. Marching the ray and stopping at the first obstruction is what
     * keeps the shot inside the room the player is actually in.
     *
     * Returns the safe distance along the backward ray.
     */
    private fun resolveCameraDistance(
        px: Float, py: Float, pz: Float,
        bx: Float, by: Float, bz: Float,
        dist: Float, world: WorldInfo, ceiling: Float
    ): Float {
        if (dist <= 0f) return 0f
        // Keep the lens out of the surface it is about to touch.
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
                // Probe the lens's own girth, not just its centre, or it clips a
                // corner before the centre point ever enters the wall.
                isSolidWorld(sx + pad, sz, world) || isSolidWorld(sx - pad, sz, world) ||
                isSolidWorld(sx, sz + pad, world) || isSolidWorld(sx, sz - pad, world)
            if (blocked) {
                safe = dist * (i - 1) / steps
                break
            }
        }
        return safe.coerceAtLeast(0f)
    }

    /** GL buffers for one streamed chunk. */
    private class ChunkMesh {
        /** Kept alongside the buffers so the camera can test solidity without a
         *  JNI round-trip on the render thread. */
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
        // 24-bit, not 16. The light fittings sit millimetres under the ceiling
        // plane, and at 16 bits the depth buffer cannot separate them past about
        // twenty metres — so down a long corridor the ceiling tile and the
        // troffer under it swapped back and forth, which is the ceiling texture
        // that appeared to slide onto a middle layer. GLES3 guarantees this
        // format, so there is no fallback to write.
        GLES30.glRenderbufferStorage(GLES30.GL_RENDERBUFFER, GLES30.GL_DEPTH_COMPONENT24, w, h)

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

    /**
     * Texel density every level surface is mapped at, in pixels per world metre.
     *
     * 320 is chosen so the square 1024px ceiling tile repeats exactly every
     * 3.2 m — one cell — while the two non-square textures land at the same
     * density on both of their axes instead of being stretched to fit a square.
     */
    private val TEXEL_DENSITY = 320f

    /** Loads a texture from assets, falling back to a small procedural tile so the
     *  renderer never crashes if the art asset isn't present in a given build.
     *  Writes the metres-to-UV scale for that texture into [uvOut]. */
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
    // Footsteps. The stamps live in Native/Trail and the renderer reads them
    // straight off the GL thread — the buffer is a plain fixed ring with no
    // allocation, so there is nothing to marshal and nothing to lock.
    // Keyed on the equipped trail, not just the renderer: this used to run once
    // per screen, so equipping a different trail in the market changed nothing
    // until the process was restarted.
    val equippedTrail by vm.equippedTrail.collectAsState()
    LaunchedEffect(renderer, equippedTrail) {
        renderer.trailSource = vm::collectTrail
        renderer.setTrailStyle(vm.trailStyleSpec())
    }
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
    // Which item is being inspected. Null means the character, which was the
    // only inspectable thing before trails had a screen of their own.
    var inspectTrail by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(s.successMsg) { if (s.successMsg != null) { delay(2000); vm.clearSuccess() } }

    // The inspection scene takes over the whole screen; it needs the space and
    // shouldn't fight the store chrome for attention.
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
        }
        // The equipped frame, drawn AROUND the portrait.
        //
        // This had been removed outright, which meant a player could buy a
        // frame, equip it, and never see it anywhere — the cosmetic did not
        // exist outside the store. It was taken out because it covered the
        // picture, and the fix for covering the picture is to stop covering
        // the picture, not to delete the feature.
        //
        // The photo occupies the middle 66% of the box, so its radius is 0.33
        // of the width. The ring's centre line sits at 0.42, and Native/Frame
        // caps every tube at kInnerClearance of the radius, which puts the
        // innermost edge no closer than about 0.36 — clear of the portrait at
        // every point of every silhouette. Tools/cosmetic_probe.cpp asserts
        // that bound, so a newly authored frame cannot quietly break it.
        val frameClock = rememberFrameClock()
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            drawFrame3D(frame, this.size.minDimension * 0.42f, frameClock)
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
 * The frame catalogue, as read from Native/Frame.
 *
 * Nothing here decides what a frame looks like. The silhouette the tube is
 * swept along, how thick it is at each point, its palette and what lights up
 * when all come out of the native table — so adding or restyling a cosmetic
 * never means editing a Compose file.
 *
 * Every native call is guarded. A frame that fails to resolve falls back to a
 * plain lit ring rather than to nothing: a cosmetic the player has paid
 * attention to must never render as an empty box, which is exactly how the
 * previews were failing.
 */
private object FrameCatalog {

    /** Ring positions. Matches the geometry's major resolution. */
    const val SAMPLES = FRAME3D_MAJOR

    class Entry(
        val id: String,
        val base: Color,
        val glow: Color,
        val highlight: Color,
        /** Mean tube thickness as a fraction of the radius, for the lens fringe. */
        val tubeRatio: Float,
        val shininess: Float,
        val geometry: Array<TorusVertex>,
        /** -1 when this entry is the offline fallback. */
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

    /** A plain ring, used only when the native table is unreachable. */
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

    /**
     * Sweeps the tube along the native silhouette. Dimensionless — the widest
     * radius is 1 — so the draw scales the whole solid by one number. Built
     * once per frame; only the transform applied to it changes.
     */
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

    /** Ids in catalogue order — the store's list and the picker's list. */
    fun ids(): List<String> = entries.map { it.id }

    /**
     * Emission for one entry at time [t]. Reused scratch, because this runs on
     * every drawn frame of every visible ring and a fresh array per draw is
     * pure garbage on the UI thread — which is what made a grid of animated
     * cards stutter badly enough to look like it had failed to load.
     */
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
    val style = FrameCatalog.entryFor(frame)
    val geometry = style.geometry
    val emission = FrameCatalog.emission(style, t)

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
        // Emission comes from the native table, sampled at this vertex's own
        // position around the ring.
        val emissive = emission[(v.u * FrameCatalog.SAMPLES).toInt()
            .coerceIn(0, FrameCatalog.SAMPLES - 1)]

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

    // ---- Volumetrics and particles ----------------------------------------
    // Everything below sits in FRONT of the solid, so it reads as light and
    // matter in the air around the ring rather than as more of the ring. The
    // geometry alone was correct but inert; this is what gives it presence.
    // Halo. Two lobes at different radii, because a single gradient reads as a
    // flat glow sticker while a tight core inside a wide wash reads as light
    // falling off through air.
    //
    // Averaged rather than sampled at one point: with the old catalogue this
    // read position zero, which on a frame whose bright band happened to sit
    // elsewhere left the halo dark while the ring itself was blazing.
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

    // Chromatic fringe: the ring's silhouette split into two offset arcs, one
    // warm and one cool. A lens does this to a bright object; faking it is the
    // cheapest thing that makes an effect look photographed rather than drawn.
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

    // Sparks thrown off the ring, each on its own orbit and lifetime. Seeded
    // from its index so the swarm is deterministic and never resets.
    val sparks = 18
    for (i in 0 until sparks) {
        val seed = i * 12.9898f
        val life = ((t * (0.30f + (i % 5) * 0.055f) + i * 0.137f) % 1f)
        // Born on the ring, drifting outward and fading as they go.
        val a = (i / sparks.toFloat()) * 6.2831853f + t * (0.18f + (i % 3) * 0.07f)
        val drift = radius * (1f + life * 0.55f)
        val wobble = sin(t * 2.1f + seed) * radius * 0.05f
        val px = center.x + cos(a) * drift + wobble
        val py = center.y + sin(a) * drift * 0.42f + sin(t * 1.6f + seed) * radius * 0.16f
        // Each spark takes its brightness from the part of the ring it was
        // thrown off, so they flare where the frame is actually lit.
        val born = emission[(i * FrameCatalog.SAMPLES / sparks).coerceIn(0, FrameCatalog.SAMPLES - 1)]
        val fade = (1f - life) * (1f - life) * born.coerceAtLeast(0.25f)
        if (fade <= 0.01f) continue
        drawCircle(
            style.glow.copy((0.55f * fade).coerceIn(0f, 1f)),
            radius = radius * (0.035f - life * 0.018f).coerceAtLeast(0.004f),
            center = Offset(px, py)
        )
    }

    // Energy arcs jumping the ring: short chords that appear for a few frames
    // where the pattern is brightest. Deliberately sparse — an effect that
    // fires constantly stops registering as an event.
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
            // Jitter perpendicular to the chord so the arc forks like a
            // discharge rather than curving like a drawn line.
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
    // Straight from the native catalogue, so the picker cannot drift out of step
    // with what actually exists.
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

/**
 * Human-readable name for a frame id.
 *
 * Ids are English and structural (Face_Of_Darkness); this is what the player
 * reads. Unknown ids fall back to the id with its underscores opened out, so a
 * cosmetic added natively still shows something sensible before anyone writes a
 * translation for it.
 */
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

/** Must match TrailField::kCapacity in Native/Trail/Trail.h — the native ring
 *  never hands back more stamps than this, so the decal buffers are sized once
 *  and never grow. */
internal const val TRAIL_CAPACITY = 48

/** How far the ring itself may slide from home to chase the thumb, as a
 *  multiple of the knob's travel. Enough to stay under a thumb that has run off
 *  the control, not so much that the stick wanders across the HUD. */
private const val JOYSTICK_BASE_FOLLOW = 1.4f

@Composable
fun VirtualJoystick(
    modifier: Modifier,
    /** False in the layout editor, where dragging must move the whole control
     *  rather than work it. The editor instantiates the real stick so the player
     *  arranges what they will actually press — but a live stick swallows the
     *  drag for its own knob, so the element could not be picked up and the
     *  joystick appeared to wander on its own. */
    interactive: Boolean = true,
    onMove: (Float, Float) -> Unit
) {
    // The thumb's TRUE offset from the control's home centre, never clamped.
    //
    // This used to be kept only in clamped form and fed back into itself as the
    // accumulator. Once the thumb had pushed past the rim, the very first pixel
    // of movement back toward the middle pulled the knob off the edge — while
    // the thumb was still far outside the ring. That is the disagreement
    // between finger and stick: they were no longer measuring from the same
    // place. Tracking the raw offset and deriving everything else from it keeps
    // the knob out at the rim until the thumb genuinely comes back inside.
    var raw by remember { mutableStateOf(Offset.Zero) }
    var dragging by remember { mutableStateOf(false) }

    // Knob position relative to the ring, and how far the ring has slid to keep
    // up. Both are pure functions of `raw` and the travel radius, resolved in
    // the pointer block where the laid-out size is known.
    var knob by remember { mutableStateOf(Offset.Zero) }
    var base by remember { mutableStateOf(Offset.Zero) }

    // While the finger is down both are pinned to it exactly; the springs are
    // only for the snap home on release. Animating under the finger made the
    // stick lag behind the thumb.
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
                // Travel radius derived from the actual laid-out size, so the knob
                // stays inside the ring on every screen density.
                val travel = minOf(size.width, size.height) / 2f * 0.62f
                val maxFollow = travel * JOYSTICK_BASE_FOLLOW

                fun apply(next: Offset) {
                    raw = next
                    val len = kotlin.math.hypot(next.x, next.y)
                    // The ring chases the thumb once the thumb leaves it, so the
                    // knob stays under the finger instead of being abandoned at
                    // the rim. Capped, or the control walks off its own corner.
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
                    // Remap (deadzone..1] onto (0..1] so leaving the deadzone is a
                    // crawl that builds smoothly, instead of jumping straight to
                    // deadzone-speed the instant the stick registers at all.
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
                //
                // Full screen. As a 300dp card it was a postage stamp in the
                // middle of the display with its own scrollbar, and every
                // control inside it was squeezed into a third of the width the
                // same controls get everywhere else in the game.
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
                    // Graphics quality, switchable mid-run. It drives bloom
                    // passes, bump detail, entity draw range and post strength,
                    // and the renderer reads it from a volatile snapshot every
                    // frame — so a player who finds the game heavy can drop it
                    // without abandoning the run they are in.
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
                    InGameSlider(stringResource(R.string.controls_camera_sensitivity), s.cameraSensitivity, 0.1f, 4f, settingsVm::onSensitivity)
                    InGameSlider(stringResource(R.string.audio_master_volume),  s.musicVolume,       0f,   1f, settingsVm::onMusic)
                    InGameSlider(stringResource(R.string.graphics_resolution_scale),  s.resolutionScale,   0.5f, 1f, settingsVm::onResolution)
                    InGameToggle(stringResource(R.string.graphics_fog),      s.fogEnabled,     settingsVm::onFog)
                    InGameToggle(stringResource(R.string.graphics_shadows),  s.shadowsEnabled, settingsVm::onShadows)
                    InGameToggle(stringResource(R.string.graphics_vhs_effect),      s.vhsEnabled,     settingsVm::onVhs)
                    InGameToggle(stringResource(R.string.graphics_show_fps),      s.showFps,        settingsVm::onShowFps)
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
            StatRow(stringResource(R.string.game_stat_difficulty), gameState.difficulty.titleCase(), CrtAmber)
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
            StatRow(stringResource(R.string.game_stat_difficulty), gameState.difficulty.titleCase(), CrtAmber)
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
        // Trails get an inspection screen of their own: a footprint on a store
        // card is a few pixels of smudge, and the whole point of buying one is
        // what it does behind you as you walk.
        val inspectable = item.category == "characters" ||
            item.category == "trails" || item.id.startsWith("trail_")
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
        // Identical solid to the lobby wallet, so the same currency never
        // appears as two different objects.
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
                // Locale-aware: a Turkish name starting with "i" has to show
                // "İ", and the locale-invariant uppercase() gives "I".
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

private val GameState.vhsEnabled: Boolean get() = true
private val GameState.showFps   : Boolean get() = false


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

// ============================================================================
// Currency.
//
// Both were flat outlines — a stroked circle with a dot, and a stroked diamond.
// They are the two things the player is asked to care about accumulating, and
// they looked like placeholder icons.
//
// They are real solids now, turning: Omnium is a struck coin, Soulium a cut
// crystal. Rendered the same way the frames are — project, sort back to front,
// fill — because at chip size that is a few dozen triangles and it buys an
// actual specular sweeping across a surface as it turns, which no amount of
// gradient stops on a flat shape will imitate.
// ============================================================================

/** Shades one face against a fixed key, in view space. */
private fun currencyShade(
    base: Color, nx: Float, ny: Float, nz: Float, shininess: Float, emissive: Float
): Color {
    // Key over the viewer's left shoulder.
    val lx = -0.46f; val ly = -0.62f; val lz = 0.64f
    val diffuse = (nx * lx + ny * ly + nz * lz).coerceAtLeast(0f)
    // Half-vector against a view direction of (0,0,1).
    val hx = lx; val hy = ly; val hz = lz + 1f
    val hl = kotlin.math.sqrt(hx * hx + hy * hy + hz * hz)
    val spec = Math.pow(
        ((nx * hx + ny * hy + nz * hz) / hl).coerceAtLeast(0f).toDouble(), shininess.toDouble()
    ).toFloat()
    // Facets turned away from the eye take a cool edge, which is what separates
    // the silhouette from whatever is behind it.
    val rim = (1f - kotlin.math.abs(nz)).let { it * it } * 0.35f
    val kd = 0.26f + diffuse * 0.80f
    return Color(
        (base.red * kd + spec * 0.9f + rim * 0.5f + emissive).coerceIn(0f, 1f),
        (base.green * kd + spec * 0.9f + rim * 0.55f + emissive).coerceIn(0f, 1f),
        (base.blue * kd + spec * 0.9f + rim * 0.75f + emissive).coerceIn(0f, 1f),
        base.alpha
    )
}

/**
 * A struck coin, spun about its vertical axis.
 *
 * It passes through edge-on once a turn, which is the moment that sells it as a
 * solid object rather than a circle with a highlight painted on.
 */
private fun DrawScope.drawOmniumCoin(c: Color, t: Float) {
    val r = size.minDimension * 0.36f
    val half = r * 0.17f                     // half the coin's thickness
    val spin = t * 1.15f
    val tilt = 0.42f                          // fixed lean, so the face is legible
    val cs = cos(spin); val sn = sin(spin)
    val ct = cos(tilt); val st = sin(tilt)
    val seg = 22

    // A ring of points on the coin's edge, spun then tilted.
    val px = FloatArray(seg); val pyTop = FloatArray(seg); val pyBot = FloatArray(seg)
    val depth = FloatArray(seg); val nOut = FloatArray(seg * 3)
    for (i in 0 until seg) {
        val a = i / seg.toFloat() * 6.2831853f
        val ox = cos(a) * r; val oz = sin(a) * r
        // Spin about Y.
        val sx = ox * cs + oz * sn
        val sz = -ox * sn + oz * cs
        // Tilt about X: y is the coin's own face normal direction.
        px[i] = sx
        pyTop[i] = -half * ct - sz * st
        pyBot[i] = half * ct - sz * st
        depth[i] = -half * st + sz * ct
        // Outward normal at the rim, through the same rotations.
        val nx0 = cos(a); val nz0 = sin(a)
        val nsx = nx0 * cs + nz0 * sn
        val nsz = -nx0 * sn + nz0 * cs
        nOut[i * 3] = nsx; nOut[i * 3 + 1] = -nsz * st; nOut[i * 3 + 2] = nsz * ct
    }
    // Face normals: the two flat sides.
    val faceNy = -ct; val faceNz = -st

    // Rim segments, back to front, then whichever face is toward the viewer.
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
    // The face toward the viewer. Its edge is the same ring, so the disc is
    // built from the ring rather than from a circle that would not match it.
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

    // Struck detail: a raised inner ring and a core, squashed by the same tilt
    // and the same spin so they sit ON the face instead of floating over it.
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

/**
 * A cut crystal: an octahedron, turning. Chosen against the coin deliberately —
 * the two currencies have to be told apart at eleven pixels, and a different
 * silhouette does that where a different hue does not.
 */
private fun DrawScope.drawSouliumCrystal(c: Color, t: Float) {
    val r = size.minDimension * 0.40f
    val spin = t * 0.95f
    val tilt = 0.34f
    val cs = cos(spin); val sn = sin(spin)
    val ct = cos(tilt); val st = sin(tilt)

    // Elongated on the vertical axis, so it reads as a cut gem rather than a die.
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
    // Eight faces: four to the top point, four to the bottom.
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
            // Back faces still contribute: a crystal is translucent, and letting
            // the far facets show through at low alpha is what makes it read as
            // glass instead of painted metal.
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
    // Coin and crystal both turn; the two are offset so a row of them does not
    // pulse in unison, which reads as a UI animation rather than as objects.
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

/**
 * Title Case, in the device's own language.
 *
 * Used for values that arrive as bare lowercase identifiers — "normal",
 * "omnium" — and are then shown to the player. Locale-aware on purpose:
 * Kotlin's own uppercase() is locale-invariant, so on a Turkish device it turns
 * "i" into "I" rather than "İ", and every label that went through it came out
 * misspelled.
 */
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
/** Whether the rationale has already been offered in this process. */
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
            // Once per process. Re-asking on every visit to the menu is how a
            // permission prompt turns into something people dismiss on reflex.
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
        }
        // The equipped frame, drawn AROUND the portrait — see FramedAvatar for
        // why the ring is back and how it is kept off the picture.
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
            // Portrait bust.
            //
            // The old one was a flat brown oval on a blob of hair with two dark
            // ovals for eyes, and it was the picture representing the game's
            // only character. Everything below is still vector paths — this
            // draws at 62dp in a scrolling grid, so it has to stay cheap — but
            // it is built the way a portrait is: hair BEHIND the face and a
            // fringe in front of it, shading that follows one light, and eyes
            // with an iris, a pupil, a catchlight and a lash line.
            val w = size.width; val h = size.height
            val cx = w * 0.5f
            val minD = size.minDimension
            // A soft key from the upper left, so the shading below has a source.
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

            // Back hair: a wide mass falling past the shoulders, so the head
            // sits in front of something instead of floating.
            val back = Path().apply {
                moveTo(cx, h * 0.08f)
                cubicTo(w * 0.97f, h * 0.18f, w * 0.93f, h * 0.74f, w * 0.84f, h * 0.94f)
                lineTo(w * 0.16f, h * 0.94f)
                cubicTo(w * 0.07f, h * 0.74f, w * 0.03f, h * 0.18f, cx, h * 0.08f)
                close()
            }
            drawPath(back, hairDark)

            // Shoulders, cut off by the card edge.
            val body = Path().apply {
                moveTo(w * 0.20f, h * 1.02f)
                cubicTo(w * 0.24f, h * 0.80f, w * 0.38f, h * 0.74f, cx, h * 0.74f)
                cubicTo(w * 0.62f, h * 0.74f, w * 0.76f, h * 0.80f, w * 0.80f, h * 1.02f)
                close()
            }
            drawPath(body, Color(0xFF1E2430))

            // Face: a rounded jaw tapering to a chin, not an egg.
            val face = Path().apply {
                moveTo(cx, h * 0.22f)
                cubicTo(w * 0.74f, h * 0.24f, w * 0.73f, h * 0.52f, w * 0.66f, h * 0.66f)
                cubicTo(w * 0.60f, h * 0.77f, w * 0.40f, h * 0.77f, w * 0.34f, h * 0.66f)
                cubicTo(w * 0.27f, h * 0.52f, w * 0.26f, h * 0.24f, cx, h * 0.22f)
                close()
            }
            drawPath(face, skin)
            // Shading down the right side, away from the key.
            clipPath(face) {
                drawRect(
                    Brush.horizontalGradient(
                        0.45f to Color.Transparent, 1f to skinShade.copy(0.75f),
                        startX = w * 0.30f, endX = w * 0.76f
                    ),
                    topLeft = Offset(0f, 0f), size = Size(w, h)
                )
            }

            // Fringe, in front of the face: a centre part with two swept
            // sections and a strand between them. This is the single thing that
            // most makes a bust read as a character rather than as a mannequin.
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
            // A lit edge along the top of the fringe, catching the same key.
            drawPath(
                Path().apply {
                    moveTo(w * 0.30f, h * 0.20f)
                    cubicTo(w * 0.38f, h * 0.13f, w * 0.62f, h * 0.13f, w * 0.70f, h * 0.20f)
                },
                hairLit, style = Stroke(minD * 0.045f, cap = StrokeCap.Round)
            )

            // Eyes. Iris, pupil, catchlight and a heavier lash line along the
            // top lid — the lash is what carries the expression at this size.
            listOf(0.395f to -1f, 0.605f to 1f).forEach { (fx, _) ->
                val ex = w * fx
                val ey = h * 0.535f
                val ew = w * 0.115f
                val eh = h * 0.115f
                // White of the eye.
                drawOval(
                    Color(0xFFFBF6F4),
                    topLeft = Offset(ex - ew * 0.5f, ey - eh * 0.5f),
                    size = Size(ew, eh)
                )
                // Iris, sat low in the eye so she is looking at the viewer.
                drawCircle(accent.copy(0.92f), radius = eh * 0.40f, center = Offset(ex, ey + eh * 0.06f))
                drawCircle(Color(0xFF17121F), radius = eh * 0.20f, center = Offset(ex, ey + eh * 0.06f))
                drawCircle(
                    Color.White.copy(0.95f), radius = eh * 0.11f,
                    center = Offset(ex - ew * 0.16f, ey - eh * 0.14f)
                )
                // Lash line.
                drawArc(
                    Color(0xFF241C33), 190f, 160f, false,
                    topLeft = Offset(ex - ew * 0.60f, ey - eh * 0.66f),
                    size = Size(ew * 1.20f, eh * 1.20f),
                    style = Stroke(minD * 0.030f, cap = StrokeCap.Round)
                )
            }
            // Brows, and a small mouth. Both are two strokes and both are the
            // difference between a face and a doll.
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

// ============================================================================
// The skeleton.
//
// The mesh carries no skin data — the .omesh format has never had any and the
// source .fbx is not in the repository — so the binding is derived here, once,
// from the rest pose. Each bone is a capsule; a vertex belongs to the bones
// whose capsule it is nearest, with a smooth falloff, and the four strongest
// are kept and normalised.
//
// The rest pose was measured off the shipped mesh rather than guessed, and the
// measurement is what found the real bug. Splitting the mesh into connected
// shells showed it held FOUR arms:
//
//     shell #0  1897 verts  x +-0.203  y 0.000..0.999   the body — and its
//                                                       arms hang at the sides
//     shell #1  1686 verts  x +-0.291  y 0.481..0.799   a dress whose sleeves
//                                                       stick straight out
//
// A T-posed garment worn over an A-posed body. The arm bones had been placed
// along the sleeves, at y 0.775 running out to |x| 0.300, so the rig animated
// the empty sleeves while the arms the player actually sees stayed bound to the
// hips and never moved with them. Two limbs that move plus two that do not is
// exactly the four pieces that read on screen.
//
// The mesh is the fix: the sleeves are swung -73.9 degrees onto the arms the
// body already has, which is the angle between the sleeve axis and the measured
// shoulder->hand line. The bones then follow the real arm:
//
//     shoulder (0.075, 0.780)  ->  elbow (0.115, 0.615)  ->  hand (0.168, 0.452)
//
// and PoseBuilder's -76 degree "rest" rotation goes away, because that number
// was only ever there to shove the sleeves down over the arms at runtime.
//
// Binding is geodesic — distance measured ALONG the surface, not through the
// air. That is not decoration. The skirt hem passes within 4cm of the hand, so
// no straight-line metric can tell them apart, and every Euclidean falloff
// tested bound part of the skirt to the forearm and tore it open when the arm
// swung. Over the surface the two are 30cm apart and the ambiguity disappears.
// ============================================================================

internal object Skeleton {
    const val BONES = 12

    const val HIPS = 0; const val SPINE = 1; const val CHEST = 2; const val HEAD = 3
    const val UPPER_ARM_L = 4; const val FORE_ARM_L = 5
    const val UPPER_ARM_R = 6; const val FORE_ARM_R = 7
    const val THIGH_L = 8; const val SHIN_L = 9
    const val THIGH_R = 10; const val SHIN_R = 11

    /** Parent of each bone; HIPS is the root. */
    val parent = intArrayOf(-1, HIPS, SPINE, CHEST,
                            CHEST, UPPER_ARM_L, CHEST, UPPER_ARM_R,
                            HIPS, THIGH_L, HIPS, THIGH_R)

    /** Head of each bone, in rest space (mesh is unit height). */
    val head = arrayOf(
        floatArrayOf(0f, 0.480f, 0f),        // hips
        floatArrayOf(0f, 0.480f, 0f),        // spine
        floatArrayOf(0f, 0.630f, 0f),        // chest
        floatArrayOf(0f, 0.820f, 0f),        // head
        floatArrayOf(-0.075f, 0.780f, 0f),   // upper arm L — the shoulder
        floatArrayOf(-0.115f, 0.615f, 0f),   // fore arm L  — the elbow
        floatArrayOf(0.075f, 0.780f, 0f),    // upper arm R
        floatArrayOf(0.115f, 0.615f, 0f),    // fore arm R
        floatArrayOf(-0.052f, 0.460f, 0f),   // thigh L
        floatArrayOf(-0.052f, 0.245f, 0f),   // shin L
        floatArrayOf(0.052f, 0.460f, 0f),    // thigh R
        floatArrayOf(0.052f, 0.245f, 0f)     // shin R
    )

    /** Tail of each bone, in rest space. */
    val tail = arrayOf(
        floatArrayOf(0f, 0.560f, 0f),
        floatArrayOf(0f, 0.630f, 0f),
        floatArrayOf(0f, 0.800f, 0f),
        floatArrayOf(0f, 1.000f, 0f),
        floatArrayOf(-0.115f, 0.615f, 0f),   // arms run down and slightly out,
        floatArrayOf(-0.168f, 0.452f, 0f),   // which is where the body's are
        floatArrayOf(0.115f, 0.615f, 0f),
        floatArrayOf(0.168f, 0.452f, 0f),
        floatArrayOf(-0.052f, 0.245f, 0f),
        floatArrayOf(-0.052f, 0.008f, 0.030f),   // tipped forward into the foot
        floatArrayOf(0.052f, 0.245f, 0f),
        floatArrayOf(0.052f, 0.008f, 0.030f)
    )

    /**
     * Falloff radius per bone, in the same units as the mesh.
     *
     * These are the body's own measurements, not taste. The pelvis is 0.13 wide
     * at the hip, so hips is 0.130 — it used to be 0.230, and a radius that
     * large is not a wide bone but a bone that competes with every other one:
     * at 0.230 the hips still outweighed the forearm on vertices 30cm away
     * along the surface, which welded the hands to the pelvis and tore them off
     * on the first arm swing. Worst tear across a walk, a stand and a run fell
     * from 11.20cm to 2.91cm on this table alone.
     */
    val radius = floatArrayOf(
        0.130f, 0.120f, 0.130f, 0.120f,
        0.055f, 0.050f, 0.055f, 0.050f,
        0.075f, 0.065f, 0.075f, 0.065f
    )

    /** Squared distance from [p] to the capsule segment of [bone]. */
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

    /** How far past its radius a bone's influence is allowed to travel. */
    private const val SOFT = 0.35f

    /**
     * `1 / (d/r)^4`, softened so that a vertex sitting exactly on a bone's axis
     * gets a large weight rather than an infinite one.
     *
     * The softening is the difference between a blend and a hard edge. Without
     * it `d` can be zero, the weight is 1e12 against a competitor's 1, and the
     * transition from one bone to the next happens between two adjacent
     * vertices — a crease, not a shoulder.
     */
    private fun falloff(d: Float, r: Float): Float {
        val s = SOFT * r
        val q = kotlin.math.sqrt(d * d + s * s) / r
        return 1f / (q * q * q * q)
    }

    /**
     * Bind a whole mesh at once, measuring distance ALONG THE SURFACE.
     *
     * Doing the whole mesh in one call rather than a vertex at a time is not an
     * optimisation, it is the point: geodesic distance is a property of the
     * mesh, so it cannot be computed from a position alone. Vertices are welded
     * by position first, because a seam that duplicates vertices for its UVs
     * would otherwise cut every path that crosses it.
     *
     * [posStride] and [posOffset] describe where the positions sit inside
     * [verts]; [outIdx] and [outWt] receive four entries per vertex.
     */
    fun bindMesh(
        verts: FloatArray, posStride: Int, posOffset: Int, vertexCount: Int,
        indices: ShortArray, outIdx: IntArray, outWt: FloatArray
    ) {
        // --- weld ------------------------------------------------------------
        val nodeOf = IntArray(vertexCount)
        val byKey = HashMap<Long, Int>(vertexCount * 2)
        val nx = FloatArray(vertexCount); val ny = FloatArray(vertexCount)
        val nz = FloatArray(vertexCount)
        var nodes = 0
        for (v in 0 until vertexCount) {
            val o = posOffset + v * posStride
            val x = verts[o]; val y = verts[o + 1]; val z = verts[o + 2]
            // 0.1mm buckets: fine enough to keep distinct surfaces apart, coarse
            // enough to close the float noise a seam leaves behind.
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

        // --- adjacency, as CSR ------------------------------------------------
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

        // --- one Dijkstra per bone -------------------------------------------
        val geo = Array(BONES) { FloatArray(nodes) }
        // Lazy deletion means one entry per successful relaxation plus one per
        // seed, and there cannot be more relaxations than directed edges. Sized
        // to that bound so a push can never be refused.
        val heap = LongArray(adj.size + nodes + 16)
        for (b in 0 until BONES) {
            val g = geo[b]
            java.util.Arrays.fill(g, Float.MAX_VALUE)
            var size = 0
            // Seeds: everything already well inside the capsule. Starting from a
            // patch rather than a point is what stops a single unlucky vertex
            // deciding where a limb begins.
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

        // --- weights ----------------------------------------------------------
        for (v in 0 until vertexCount) {
            val n = nodeOf[v]
            var i0 = HIPS; var i1 = HIPS; var i2 = HIPS; var i3 = HIPS
            var w0 = -1f; var w1 = -1f; var w2 = -1f; var w3 = -1f
            for (b in 0 until BONES) {
                val d = geo[b][n]
                if (d == Float.MAX_VALUE) continue          // not on this shell
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
                // A shell no bone reaches at all — a stray prop, or a mesh that
                // changed under us. Rigid to the nearest bone beats scattered.
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

    /** Binary min-heap of (distance, node) packed into a long, distance high. */
    private fun heapPush(h: LongArray, size: Int, d: Float, n: Int): Int {
        if (size >= h.size) return size                       // cannot happen; refuses to corrupt
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

/**
 * Turns animation state into the twelve matrices the skinning shader wants.
 *
 * This lives on the CPU deliberately. The pose is a dozen matrix multiplies per
 * frame — nothing next to the per-vertex work — and having it here means it can
 * be reasoned about, printed and asserted on, which a rig buried in GLSL never
 * could be. That is most of why the old one stayed broken for so long.
 *
 * Every bone's matrix is  parent * T(head) * R * T(-head)  : rotate about the
 * bone's own head, then inherit everything the parent did. The bind pose is the
 * rest pose itself, so no inverse-bind matrix is needed.
 */
internal class PoseBuilder {
    /** Column-major 4x4 per bone, ready for glUniformMatrix4fv. */
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

    /** Extra translation on the root, for crouch drop and jump lift. */
    private fun rootOffset(dy: Float) {
        Matrix.setIdentityM(local, 0)
        Matrix.translateM(local, 0, 0f, dy, 0f)
        System.arraycopy(matrices, Skeleton.HIPS * 16, tmp, 0, 16)
        Matrix.multiplyMM(work, 0, local, 0, tmp, 0)
        System.arraycopy(work, 0, matrices, Skeleton.HIPS * 16, 16)
    }

    /**
     * [walk] is a continuous gait blend: 0 idle, 1 walk, up to 1.6 running.
     * [crouch], [air] and [torch] are 0..1. [headYaw]/[headPitch] are radians.
     * [death] 0..1 collapses her; [getUp] 0..1 is the reverse, used on spawn.
     */
    /**
     * [collapse] is how far down the body is: 0 standing, 1 flat on the floor.
     *
     * One number rather than the `death` and `getUp` pair it replaces. That pair
     * had a trap in it — `getUp` of exactly 0 fell through to `death`, so the
     * first frame of standing up was indistinguishable from dying — and worse,
     * it let a caller ask for both at once and get whichever the branch happened
     * to pick. A collapse and a recovery are the same poses in opposite
     * directions, so they should be the same number in opposite directions.
     */
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

        // --- Root -----------------------------------------------------------
        // Breathing, the vertical bob of a stride, and the crouch drop.
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

        // --- Head -------------------------------------------------------------
        // A rigid skull on a blending neck: the whole reason the head used to
        // shear was a wide gradient rotating the crown further than the jaw.
        composeBone(Skeleton.HEAD,
            rx = (-headPitch * deg).coerceIn(-38f, 38f) + 26f * down,
            ry = (headYaw * deg).coerceIn(-58f, 58f) * (1f - down),
            rz = sin(time * 0.7f) * 1.6f * (1f - down))

        // --- Arms -------------------------------------------------------------
        // The mesh holds them at her sides, so there is no rest angle to apply:
        // zero here means the arms stay exactly where the model puts them. This
        // used to be -76 degrees about Z, which existed only to shove the
        // dress's T-posed sleeves down over arms that were already down. The
        // sleeves are on the arms now, so the compensation is gone with them.
        for (side in 0..1) {
            val s = if (side == 0) -1f else 1f          // -1 left, +1 right
            val upper = if (side == 0) Skeleton.UPPER_ARM_L else Skeleton.UPPER_ARM_R
            val fore = if (side == 0) Skeleton.FORE_ARM_L else Skeleton.FORE_ARM_R
            val isRight = if (side == 1) 1f else 0f
            val torchArm = torch * isRight

            val rest = 0f                                // the mesh already is
            val phase = stride + if (side == 1) Math.PI.toFloat() else 0f
            val swing = sin(phase) * (23f + 17f * run) * gait
            val idle = sin(time * 0.9f + s) * 3.0f

            // A raised torch stops the swing and brings the arm forward instead.
            val swingX = mix(swing + idle, -71f, torchArm)
            val tuck = -11f * s + mix(0f, -19f * s, torchArm)
            composeBone(upper,
                rx = swingX - 26f * down,
                ry = tuck,
                rz = rest + 8f * s * crouch + 30f * s * down)

            // Elbow lags the shoulder — that lag is most of what separates a
            // swinging limb from a rotating stick.
            val lag = sin(phase - 0.85f)
            val bend = mix(lag * 17f * gait + 6f, -30f, torchArm)
            composeBone(fore, rx = bend + 34f * down, ry = 0f, rz = 0f)
        }

        // --- Legs -------------------------------------------------------------
        for (side in 0..1) {
            val s = if (side == 0) -1f else 1f
            val thigh = if (side == 0) Skeleton.THIGH_L else Skeleton.THIGH_R
            val shin = if (side == 0) Skeleton.SHIN_L else Skeleton.SHIN_R
            val phase = stride + if (side == 1) Math.PI.toFloat() else 0f

            // Crouch: hip folds and knee closes, and the two are matched so the
            // foot stays on the floor rather than sinking through it.
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
        private const val MAGIC = 0x48534D4F   // "OMSH" little-endian
        /** pos3 + normal3 + uv2 + boneIdx4 + boneWeight4. The last eight are
         *  derived at load from the rest pose; the file carries only the first
         *  eight. */
        const val FLOATS_PER_VERTEX = 16
        private const val FILE_FLOATS_PER_VERTEX = 8

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
            val fileVerts = FloatArray(vertexCount * FILE_FLOATS_PER_VERTEX)
            bb.asFloatBuffer().get(fileVerts)
            bb.position(bb.position() + fileVerts.size * 4)
            val idx = ShortArray(indexCount)
            bb.asShortBuffer().get(idx)

            // Bind to the skeleton. Once, here, off the rest pose, so the shader
            // does nothing per frame but a weighted sum of four matrices per
            // vertex. Twelve Dijkstras over the welded mesh is a few
            // milliseconds and it only ever happens on this path.
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
/** Four bone indices, and the weights that go with them. Derived at load from
 *  the rest pose — see Skeleton in the Kotlin. */
layout(location=3) in vec4 aBoneIdx;
layout(location=4) in vec4 aBoneWt;

uniform mat4 uMVP;
uniform mat4 uModel;
/**
 * The pose, built on the CPU by PoseBuilder.
 *
 * Everything this shader used to do itself — arms, legs, head, crouch, torch —
 * is gone, and that is the fix rather than a tidy-up. It computed each joint's
 * rotation ANGLE by multiplying a fixed angle by a mask that varied with the
 * vertex's own position, so a limb was never rotated: it was fanned, the
 * shoulder end by nothing and the hand end by the full amount. On this mesh
 * that dropped the hand to y 0.56 while the upper arm stayed stretched out at
 * y 0.78, leaving a V per side — which is what appeared on screen as four arms.
 * It also killed the walk: the swing was scaled by the same mask, so it
 * vanished toward the body and never read as motion.
 *
 * A bone matrix applies to every vertex bound to that bone equally. That is the
 * whole difference.
 */
uniform mat4 uBones[12];
/** 1 for the character, 0 for scenery — the studio backdrop shares this
 *  program, and a fragment uniform cannot gate vertex code. */
uniform float uAnimate;

out vec3 vNormal; out vec2 vUV; out vec3 vWorldPos;

void main(){
    vec3 p = aPos;
    vec3 n = aNormal;

    if (uAnimate > 0.5) {
        // Linear blend skinning. The weights are normalised at bind time, so
        // this is a plain weighted sum with no renormalisation needed.
        mat4 skin =
            uBones[int(aBoneIdx.x)] * aBoneWt.x +
            uBones[int(aBoneIdx.y)] * aBoneWt.y +
            uBones[int(aBoneIdx.z)] * aBoneWt.z +
            uBones[int(aBoneIdx.w)] * aBoneWt.w;
        p = (skin * vec4(aPos, 1.0)).xyz;
        // Normals take the rotation but not the translation.
        n = normalize(mat3(skin) * aNormal);
    }

    vec4 world = uModel * vec4(p, 1.0);
    vWorldPos = world.xyz;
    vNormal = mat3(uModel) * n;
    vUV = aUV;
    gl_Position = uMVP * vec4(p, 1.0);
}
"""

/**
 * Inspection studio.
 *
 * Lit the way a product shot is, not the way a room is: a three-point rig with
 * a warm key over the viewer's shoulder, a cool fill opposite it to keep the
 * shadow side readable, and a hard rim behind to separate her from the
 * backdrop. The backdrop itself is an infinity cove — a graded sweep with no
 * visible horizon — plus a contact shadow so she is standing on it rather than
 * hovering in front of it.
 */
private const val OMNI_PREVIEW_FRAG = """#version 300 es
precision mediump float;
in vec3 vNormal; in vec2 vUV; in vec3 vWorldPos;
uniform sampler2D uTex;
uniform float uIsCharacter;
uniform float uTime;
/** World-space footprint of the subject, for the contact shadow. */
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
        // --- Three-point rig ---------------------------------------------
        vec3 keyDir  = normalize(vec3(-0.55, 0.82, 0.62));
        vec3 fillDir = normalize(vec3( 0.78, 0.22, 0.42));
        vec3 rimDir  = normalize(vec3( 0.15, 0.45, -0.92));

        float key  = max(dot(n, keyDir), 0.0);
        float fill = max(dot(n, fillDir), 0.0);
        float rim  = pow(max(dot(n, rimDir), 0.0), 2.2);
        // Wrapped diffuse on the key: light bends around a subject rather than
        // terminating on the exact horizon, and a hard terminator is the single
        // most plastic-looking thing a character render can have.
        float wrapped = max((dot(n, keyDir) + 0.35) / 1.35, 0.0);

        vec3 keyCol  = vec3(1.00, 0.95, 0.86) * (wrapped * 0.95 + key * 0.25);
        vec3 fillCol = vec3(0.42, 0.52, 0.72) * fill * 0.38;
        vec3 rimCol  = vec3(1.00, 0.92, 0.74) * rim * 0.85;

        // Tight specular from the key, so skin and fabric read differently.
        vec3 half0 = normalize(keyDir + view);
        float spec = pow(max(dot(n, half0), 0.0), 34.0) * 0.28;

        vec3 col = tex.rgb * (vec3(0.16, 0.17, 0.21) + keyCol + fillCol) + rimCol + spec;
        fragColor = vec4(col, 1.0);
        return;
    }

    // --- Infinity cove ---------------------------------------------------
    // No texture and no horizon line: a smooth sweep from a pool of light at
    // her feet out into darkness. The level's wall and floor swatches were
    // being used here and they fought the subject for attention.
    float radial = length(vWorldPos.xz - uSubject.xz);
    float sweep = 1.0 - smoothstep(0.4, 3.4, radial);
    float height = 1.0 - smoothstep(0.0, 2.6, vWorldPos.y);
    vec3 cove = mix(vec3(0.030, 0.030, 0.036), vec3(0.155, 0.150, 0.140),
                    max(sweep * 0.85, height * 0.35));

    // --- What is behind the dark ------------------------------------------
    // A flat grade is a backdrop; it reads as a wall two feet behind her no
    // matter how far the numbers say it is. Structure at a scale much larger
    // than the subject is what tells the eye there is distance there, so this
    // is deliberately low-frequency, very dim, and slow — three octaves of it
    // moving against each other, warped by a fourth so it never resolves into
    // a pattern you can name.
    //
    // Two layers drifting at different rates give parallax for free: the far
    // one barely moves, the near one visibly does, and the eye reads the gap
    // between them as depth without a single extra triangle.
    vec2 sky = vec2(vWorldPos.x * 0.34, vWorldPos.y * 0.30) + vec2(uTime * 0.011, uTime * 0.004);
    float warp = pFbm(sky * 0.7 + vec2(uTime * 0.006, 0.0));
    float far  = pFbm(sky * 0.9 + warp * 0.85);
    float near = pFbm(sky * 1.9 - vec2(uTime * 0.019, uTime * 0.007) + warp * 0.4);

    // Cold, and barely there. The subject is warm-lit by the key, so the space
    // behind her has to sit on the other side of the colour wheel or she stops
    // separating from it — which is the actual job of a backdrop.
    vec3 deep  = vec3(0.055, 0.042, 0.098);
    vec3 glow  = vec3(0.030, 0.088, 0.115);
    vec3 cosmic = mix(deep, glow, smoothstep(0.35, 0.85, far));
    cosmic += glow * smoothstep(0.55, 0.95, near) * 0.55;

    // Only in the dark. Where the cove is lit it stays a clean studio sweep;
    // the cosmos is what the darkness turns out to have been all along.
    float dark = 1.0 - max(sweep * 0.85, height * 0.35);
    cove += cosmic * dark * dark * 0.9;

    // --- Dust -------------------------------------------------------------
    // Motes hanging in the light. They exist everywhere, but you only ever see
    // the ones a beam catches, so the whole field is multiplied by how lit that
    // part of the cove is — dust in the dark is not dust, it is noise.
    //
    // Two sheets at different scales and speeds, both drifting upward and
    // sideways the way real dust does in still air: never falling, never quite
    // still. The pow() is what keeps them as separate specks instead of a haze.
    float lit = max(sweep, height * 0.5);
    vec2 d0 = vec2(vWorldPos.x * 7.0, vWorldPos.y * 7.0 - uTime * 0.09);
    vec2 d1 = vec2(vWorldPos.x * 13.0 + 31.7, vWorldPos.y * 13.0 - uTime * 0.16);
    float motes = pow(pNoise(d0), 15.0) * 1.6 + pow(pNoise(d1), 19.0) * 1.1;
    // Each mote breathes on its own clock, so the field twinkles rather than
    // pulsing as one sheet.
    motes *= 0.65 + 0.35 * sin(uTime * 1.7 + pHash(floor(d0)) * 24.0);
    cove += vec3(1.00, 0.96, 0.88) * motes * lit * 0.85;

    // Contact shadow: an elliptical pool directly under her, densest at the
    // feet. Without it a figure on a graded backdrop reads as a cut-out.
    float contact = 1.0 - smoothstep(0.0, 0.62, radial);
    cove *= 1.0 - contact * 0.80 * (1.0 - smoothstep(0.0, 0.22, vWorldPos.y));

    fragColor = vec4(cove, 1.0);
}
"""

/** Renders the character alone on an infinity cove, lit like a product shot. */
class CharacterPreviewRenderer(private val appContext: Context) : GLSurfaceView.Renderer {

    // The model turns; the camera orbits and dollies. Turning the MODEL rather
    // than the camera is what makes this read as a turntable — the studio lights
    // stay put and sweep across her as she comes round, which is the whole
    // reason to put something on a turntable in the first place.
    @Volatile var yawDegrees: Float = 18f
    @Volatile var walkAmount: Float = 0f
    /** Camera elevation. Positive looks down at her. */
    @Volatile var pitchDegrees: Float = 7f
    /** Camera distance from the framing target, in metres. */
    @Volatile var distance: Float = 3.3f

    private var program = 0
    private var uMVP = 0; private var uModel = 0; private var uTime = 0
    private var uTex = 0; private var uIsChar = 0
    private var uAnimate = 0; private var uSubject = 0; private var uBones = 0
    /** Same pose builder the corridor uses, so the studio cannot show a pose
     *  the game never produces. */
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
        /** Close enough to read the face, far enough to hold the whole figure. */
        const val MIN_DIST = 1.7f
        const val MAX_DIST = 5.2f
        const val MIN_PITCH = -10f
        const val MAX_PITCH = 38f
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // Matches the cove's far tone, so anything past the backdrop's edge is
        // indistinguishable from the backdrop itself.
        GLES30.glClearColor(0.030f, 0.030f, 0.036f, 1f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        runCatching {
            program = linkGlProgram(OMNI_PREVIEW_VERT, OMNI_PREVIEW_FRAG)
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

            // Infinity cove. The graded sweep in the fragment shader fades out by
            // about 3.4 m, so the geometry has to run far past that or the
            // gradient ends on a visible plate edge instead of on nothing.
            //
            // Sized off the widest shot the camera can take: pulled fully back
            // and tilted fully down, the top of the frame lands about 6 m up the
            // back wall and 6 m out across the floor, and a wide tablet stretches
            // that sideways again. 14 m clears all of it with room to spare, and
            // two quads cost nothing.
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

            // Only the character is textured. The cove is shaded procedurally —
            // the level's own wall and floor swatches used to be pasted behind
            // her and they competed with the subject for attention, which is
            // the one thing a product shot must not do.
            charTex = loadTex("Models/Anime_Texture.png", 0xFFE8D5C8.toInt())
        }.onFailure { OmniLog.e("Preview", "setup failed", it) }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        // A long lens. Wide angles distort a face badly at inspection range, and
        // the distortion lands exactly where the player is looking.
        Matrix.perspectiveM(proj, 0, 34f, width.toFloat() / height.coerceAtLeast(1), 0.1f, 40f)
    }

    /**
     * Orbits the camera around her, reframing as it dollies: pulled back it
     * holds the whole figure, pushed in it settles on the head and shoulders.
     * A fixed aim point would put her chin at the bottom of the frame the
     * moment the player zoomed in on the face.
     */
    private fun updateView() {
        val d = distance.coerceIn(MIN_DIST, MAX_DIST)
        val far = ((d - MIN_DIST) / (MAX_DIST - MIN_DIST)).coerceIn(0f, 1f)
        val targetY = 1.38f - 0.46f * far
        val p = Math.toRadians(pitchDegrees.toDouble())
        // Never let the eye drop through the cove floor; from underneath the
        // backdrop swallows her completely.
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

        // Backdrop first.
        Matrix.setIdentityM(model, 0)
        GLES30.glUniformMatrix4fv(uMVP, 1, false, vp, 0)
        GLES30.glUniformMatrix4fv(uModel, 1, false, model, 0)
        GLES30.glUniform1f(uIsChar, 0f)
        // Scenery holds still.
        GLES30.glUniform1f(uAnimate, 0f)
        // She stands on the origin, so the cove's pool of light and the contact
        // shadow are both centred there.
        GLES30.glUniform3f(uSubject, 0f, 0f, 0f)
        drawIndexed(roomVbo, roomIbo, floorCount, charTex)
        drawIndexed(wallVbo, wallIbo, wallCount, charTex)

        // The mesh is authored unit-height, so this is literally her height in
        // metres — the same 1.7 m the in-game avatar stands at.
        if (charCount > 0) {
            Matrix.setIdentityM(model, 0)
            Matrix.rotateM(model, 0, yawDegrees, 0f, 1f, 0f)
            Matrix.scaleM(model, 0, 1.7f, 1.7f, 1.7f)
            Matrix.multiplyMM(mvp, 0, vp, 0, model, 0)
            GLES30.glUniformMatrix4fv(uMVP, 1, false, mvp, 0)
            GLES30.glUniformMatrix4fv(uModel, 1, false, model, 0)
            GLES30.glUniform1f(uIsChar, 1f)
            GLES30.glUniform1f(uAnimate, 1f)
            // walkAmount is the Idle/Walk toggle. It used to feed a uniform the
            // shader scaled by a position mask, so the swing died out toward
            // the body and pressing Walk did nothing visible.
            pose.build(t, walkAmount, 0f, 0f, 0f, 0f, 0f)
            GLES30.glUniformMatrix4fv(uBones, Skeleton.BONES, false, pose.matrices, 0)
            drawIndexed(charVbo, charIbo, charCount, charTex, skinned = true)
        }
    }

    /** [skinned] selects the character's 16-float layout over the backdrop
     *  quads' 8-float one. Both go through this program, so the stride cannot
     *  be a constant. */
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

/**
 * Interaction state for the inspection sheet, deliberately outside Compose.
 * The frame loop and the gesture detector both write to it every frame; nothing
 * reads it during composition, so making it observable would only buy sixty
 * needless recompositions a second.
 */
private class PreviewTurntable {
    /** Seconds left before the automatic spin resumes. */
    var holdOff: Float = 0f
}

/** Full-screen character inspection sheet, opened by tapping the market art. */
/**
 * Trail inspection.
 *
 * A walker crossing a lit floor, laying the trail the player is looking at. It
 * is shown from above rather than from the player's own eyeline for a plain
 * reason: from eye level a footprint is a smear a few pixels tall, and the
 * whole point of an inspection screen is that you can actually see the thing.
 *
 * Everything that decides how the trail LOOKS — tint, lifetime, size, how much
 * it spreads, which mark it stamps — is read from Native/Trail, the same table
 * the in-game decals use. The ageing is done here rather than by driving the
 * native TrailField, because that field is the live player's trail and a store
 * preview has no business writing to it.
 */
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

    // One stamp: where it landed, which way it was facing, which foot, and when.
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
            // The walker follows a slow lissajous, so the path curves back over
            // itself and you can see how an old mark differs from a new one
            // without waiting for a lap.
            if (clock >= nextStep) {
                nextStep = clock + 0.42f
                side = -side
                val t = clock * 0.32f
                val px = sin(t) * 0.34f
                val py = sin(t * 1.7f) * 0.26f
                // Facing is the path's own tangent.
                val ang = kotlin.math.atan2(
                    (kotlin.math.cos(t * 1.7f) * 1.7f * 0.26f),
                    (kotlin.math.cos(t) * 0.34f)
                )
                stamps.add(Stamp(px, py, ang, side, clock))
            }
            // Retire what has aged out, oldest first.
            while (stamps.isNotEmpty() && (clock - stamps[0].born) > lifetime) stamps.removeAt(0)
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            // The floor: a pool of light with the room falling away around it,
            // so the marks are read against something rather than against black.
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
                // Offset to the correct side of the line of travel, exactly as
                // TrailField::step does in world space.
                val ox = cos(st.ang) * unit * 0.035f * st.side
                val oy = -sin(st.ang) * unit * 0.035f * st.side
                drawTrailMark(
                    mark, Offset(cx + ox, cy + oy), half,
                    st.ang, tint.copy(alpha = (fade * 0.9f).coerceIn(0f, 1f)), clock
                )
            }
        }
        // Vignette, matching the character studio.
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
            // Owning a trail and wearing one are different things. Before this
            // the only way to end up wearing a trail was to buy it, so a player
            // who owned three was stuck in whichever came last.
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

/**
 * One trail mark, in 2D. Mirrors the shapes OMNI_DECAL_FRAG draws in the world
 * so the preview and the corridor agree about what a trail looks like.
 */
private fun DrawScope.drawTrailMark(
    mark: Int, at: Offset, half: Float, ang: Float, colour: Color, t: Float
) {
    when (mark) {
        // Sole: ball of the foot and a separate heel, along the walk.
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
        // Static: a torn block that reshuffles on its own beat.
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
        // Grain: a fixed scatter, so a mark does not shimmer as it ages.
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
    // Camera angles live on the renderer and are written straight from the frame
    // loop and the gesture detector. Holding them in Compose state instead meant
    // a state write, a recomposition and a relaunched effect for every one of
    // sixty frames a second, to move a number the UI never displays.
    val turntable = remember { PreviewTurntable() }

    // A display model that only moves when you touch it looks broken; a slow
    // drift shows the silhouette from every side without being asked. It stands
    // off for a beat after the last touch rather than snapping back into the
    // spin the instant a finger lifts, which would drag the pose the player had
    // just lined up out from under them.
    LaunchedEffect(renderer) {
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1_000_000_000.0).toFloat().coerceIn(0f, 0.1f)
            last = now
            // Keep the angle bounded. A float that only ever grows loses
            // precision, and a flick can add hundreds of degrees in a second.
            renderer.yawDegrees = renderer.yawDegrees.mod(360f)
            if (turntable.holdOff > 0f) {
                turntable.holdOff -= dt
            } else {
                renderer.yawDegrees -= dt * 11f
                // Ease the camera back to the house angle once she is spinning
                // again, so an abandoned inspection tidies itself up.
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
                // One detector for all three axes: a single finger turns her and
                // tilts the camera, two fingers dolly in and out.
                detectTransformGestures { _, pan, zoom, _ ->
                    turntable.holdOff = 2.5f
                    // Plus, not minus. The model is spun about +Y, and by the
                    // right-hand rule a positive angle carries the point facing
                    // the camera (+Z) round to +X — the viewer's right. So a
                    // rightward drag needs a positive delta to push her round to
                    // the right; subtracting sent her the other way, which is
                    // why dragging right turned her left.
                    renderer.yawDegrees += pan.x * 0.4f
                    // Dragging down tips her top toward the viewer, which means
                    // the camera rises — the same way grabbing a real figure and
                    // pulling it forward shows you the top of its head.
                    renderer.pitchDegrees = (renderer.pitchDegrees + pan.y * 0.14f)
                        .coerceIn(CharacterPreviewRenderer.MIN_PITCH, CharacterPreviewRenderer.MAX_PITCH)
                    if (zoom != 0f) {
                        renderer.distance = (renderer.distance / zoom)
                            .coerceIn(CharacterPreviewRenderer.MIN_DIST, CharacterPreviewRenderer.MAX_DIST)
                    }
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
