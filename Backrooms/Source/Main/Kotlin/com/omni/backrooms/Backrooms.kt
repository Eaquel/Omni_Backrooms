package com.omni.backrooms

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavHostController
import androidx.navigation.NavType
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────
// Design Tokens
// ─────────────────────────────────────────────────────────────
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

// ─────────────────────────────────────────────────────────────
// Application
// ─────────────────────────────────────────────────────────────
@HiltAndroidApp
class App : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("omni_backrooms")
        appScope.launch(Dispatchers.IO) {
            val bridge = NativeBridge()
            bridge.initGuard(applicationContext, BuildConfig.EXPECTED_SIG_HASH)
            val flags = bridge.getGuardFlags()
            if (flags != 0) android.util.Log.w("OmniApp", "Threat: ${bridge.getThreatReport()}")
        }
    }
}

private val Context.dataStore: DataStore<Preferences>
    by preferencesDataStore(name = "omni_prefs")

// ─────────────────────────────────────────────────────────────
// DI Module
// ─────────────────────────────────────────────────────────────
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDataStore(@ApplicationContext ctx: Context): DataStore<Preferences> = ctx.dataStore

    @Provides @Singleton
    fun provideNativeBridge(): NativeBridge = NativeBridge()

    @Provides @Singleton
    fun provideGuardManager(
        @ApplicationContext ctx: Context,
        bridge: NativeBridge
    ): GuardManager = GuardManager(ctx, bridge)

    @Provides @Singleton
    fun provideAssetManager(@ApplicationContext ctx: Context): AssetManager = AssetManager(ctx)

    @Provides @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

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
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                        else HttpLoggingInterceptor.Level.NONE
            })
            .retryOnConnectionFailure(true)
            .build()

    @Provides @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService = retrofit.create(ApiService::class.java)

    @Provides @Singleton
    fun provideRoomRepository(api: ApiService): RoomRepository = RoomRepository(api)
}

// ─────────────────────────────────────────────────────────────
// Activity
// ─────────────────────────────────────────────────────────────
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { false }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        setContent {
            OmniTheme {
                Surface(Modifier.fillMaxSize(), color = Color.Black) {
                    OmniNavGraph(rememberNavController())
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Theme
// ─────────────────────────────────────────────────────────────
@Composable
fun OmniTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary      = Yellow,
            onPrimary    = Color.Black,
            secondary    = CrtAmber,
            background   = DarkBg,
            surface      = MetalBg,
            onBackground = Yellow,
            onSurface    = Yellow,
            outline      = BorderCol
        ),
        content = content
    )
}

// ─────────────────────────────────────────────────────────────
// Navigation
// ─────────────────────────────────────────────────────────────
sealed class Route(val path: String) {
    data object Splash       : Route("splash")
    data object Loading      : Route("loading")
    data object Menu         : Route("menu")
    data object ModeSelect   : Route("mode_select")
    data object Difficulty   : Route("difficulty")
    data object OnlineSelect : Route("online_select")
    data object RoomList     : Route("room_list")
    data object CreateRoom   : Route("create_room")
    data object Settings     : Route("settings")
    data object UiEditor     : Route("ui_editor")
    data object Market       : Route("market")
    data object Story        : Route("story")
    data object Credits      : Route("credits")
    data object Changelog    : Route("changelog")
    data object Events       : Route("events")
    data object Maps         : Route("maps")
    data object Characters   : Route("characters")
    data object Leaderboard  : Route("leaderboard")
    data object Game : Route("game/{difficulty}/{online}") {
        fun go(d: String, o: Boolean) = "game/$d/$o"
    }
}

@Composable
fun OmniNavGraph(nav: NavHostController) {
    NavHost(nav, startDestination = Route.Splash.path) {

        composable(Route.Splash.path) {
            Splash(onDone = {
                nav.navigate(Route.Loading.path) { popUpTo(Route.Splash.path) { inclusive = true } }
            })
        }
        composable(Route.Loading.path) {
            Loading(onDone = {
                nav.navigate(Route.Menu.path) { popUpTo(Route.Loading.path) { inclusive = true } }
            })
        }
        composable(Route.Menu.path) {
            Menu(
                onNewGame   = { nav.navigate(Route.ModeSelect.path) },
                onEvents    = { nav.navigate(Route.Events.path) },
                onSettings  = { nav.navigate(Route.Settings.path) },
                onMarket    = { nav.navigate(Route.Market.path) },
                onStory     = { nav.navigate(Route.Story.path) },
                onCredits   = { nav.navigate(Route.Credits.path) },
                onChangelog = { nav.navigate(Route.Changelog.path) },
                onMaps      = { nav.navigate(Route.Maps.path) },
                onChars     = { nav.navigate(Route.Characters.path) },
                onLeader    = { nav.navigate(Route.Leaderboard.path) }
            )
        }
        composable(Route.ModeSelect.path) {
            ModeSelect(
                onOffline = { nav.navigate(Route.Difficulty.path) },
                onOnline  = { nav.navigate(Route.OnlineSelect.path) },
                onBack    = { nav.popBackStack() }
            )
        }
        composable(Route.Difficulty.path) {
            DifficultySelect(
                onSelect = { d -> nav.navigate(Route.Game.go(d, false)) { popUpTo(Route.Menu.path) } },
                onBack   = { nav.popBackStack() }
            )
        }
        composable(Route.OnlineSelect.path) {
            OnlineSelect(
                onJoin   = { nav.navigate(Route.RoomList.path) },
                onCreate = { nav.navigate(Route.CreateRoom.path) },
                onBack   = { nav.popBackStack() }
            )
        }
        composable(Route.RoomList.path) {
            Room(
                onJoined = { nav.navigate(Route.Game.go("normal", true)) { popUpTo(Route.Menu.path) } },
                onBack   = { nav.popBackStack() }
            )
        }
        composable(Route.CreateRoom.path) {
            CreateRoom(
                onCreated = { nav.navigate(Route.Game.go("normal", true)) { popUpTo(Route.Menu.path) } },
                onBack    = { nav.popBackStack() }
            )
        }
        composable(Route.Settings.path) {
            Settings(onUiEditor = { nav.navigate(Route.UiEditor.path) }, onBack = { nav.popBackStack() })
        }
        composable(Route.UiEditor.path)    { UiEditor(onSave = { nav.popBackStack() }) }
        composable(Route.Market.path)      { Market(onBack = { nav.popBackStack() }) }
        composable(Route.Story.path)       { Story(onBack = { nav.popBackStack() }) }
        composable(Route.Credits.path)     { Credits(onBack = { nav.popBackStack() }) }
        composable(Route.Changelog.path)   { Changelog(onBack = { nav.popBackStack() }) }
        composable(Route.Events.path)      { Events(onBack = { nav.popBackStack() }) }
        composable(Route.Maps.path)        { Maps(onBack = { nav.popBackStack() }) }
        composable(Route.Characters.path)  { Characters(onBack = { nav.popBackStack() }) }
        composable(Route.Leaderboard.path) { Leaderboard(onBack = { nav.popBackStack() }) }

        composable(
            Route.Game.path,
            arguments = listOf(
                navArgument("difficulty") { type = NavType.StringType },
                navArgument("online")     { type = NavType.BoolType }
            )
        ) { back ->
            val diff   = back.arguments?.getString("difficulty") ?: "normal"
            val online = back.arguments?.getBoolean("online")    ?: false
            GameScreen(
                difficulty = diff,
                isOnline   = online,
                onExit     = { nav.navigate(Route.Menu.path) { popUpTo(Route.Menu.path) { inclusive = true } } }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Game Screen + ViewModel
// ─────────────────────────────────────────────────────────────
@HiltViewModel
class GameScreenVM @Inject constructor(
    val bridge              : NativeBridge,
    val service             : SessionService,
    private val settingsRepo: SettingsRepository
) : ViewModel() {

    private val _camSnapshot = MutableStateFlow<CameraSnapshot?>(null)
    val camSnapshot: StateFlow<CameraSnapshot?> = _camSnapshot.asStateFlow()

    private var sensitivity = 1f

    init {
        viewModelScope.launch {
            settingsRepo.observe().collect { s -> sensitivity = s.cameraSensitivity }
        }
        viewModelScope.launch {
            while (true) {
                _camSnapshot.value = CameraSnapshot.fromFloatArray(bridge.getCameraState())
                kotlinx.coroutines.delay(16)
            }
        }
    }

    fun startGame(difficulty: String, isOnline: Boolean) {
        // Intent(Context, Class) constructor requires a Context instance, not a Class.
        // GameScreenVM receives the application context via @ApplicationContext injection.
        val intent = android.content.Intent().apply {
            setClassName("com.omni.backrooms", SessionService::class.java.name)
            action = if (isOnline) SessionService.ACTION_START_ONLINE else SessionService.ACTION_START_OFFLINE
            putExtra(SessionService.EXTRA_DIFFICULTY, difficulty)
            if (!isOnline) putExtra(SessionService.EXTRA_SEED, System.currentTimeMillis())
        }
    }

    fun stopGame() {}
    fun togglePause() {
        val s = service.gameState.value
        service.gameState // accessed via service directly
    }
    fun toggleFlashlight() {}
    fun onMove(fx: Float, fy: Float, fz: Float) { bridge.applyMovement(fx, fy, fz) }
    fun onLook(dx: Float, dy: Float)            { bridge.cameraLook(dx, dy, sensitivity) }
    fun onJump()                                { bridge.applyMovement(0f, 350f, 0f) }
    fun onCrouch()                              {}
    fun onInteract()                            {}
}

@Composable
fun GameScreen(
    difficulty : String,
    isOnline   : Boolean,
    onExit     : () -> Unit,
    vm         : GameScreenVM = hiltViewModel()
) {
    val gameState by vm.service.gameState.collectAsState()
    val camSnap   by vm.camSnapshot.collectAsState()

    DisposableEffect(difficulty, isOnline) {
        vm.startGame(difficulty, isOnline)
        onDispose { vm.stopGame() }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (gameState.vhsEnabled) VhsOverlay()
        CrtOverlay()
        GameHud(
            gameState  = gameState,
            onPause    = vm::togglePause,
            onFlash    = vm::toggleFlashlight,
            onMove     = vm::onMove,
            onLook     = vm::onLook,
            onJump     = vm::onJump,
            onCrouch   = vm::onCrouch,
            onInteract = vm::onInteract
        )
        if (gameState.isPaused)   PauseOverlay(onResume = vm::togglePause, onExit = onExit)
        if (gameState.isGameOver) GameOverOverlay(gameState = gameState, onExit = onExit)
        if (gameState.isEscaped)  EscapedOverlay(gameState = gameState, onExit = onExit)
    }
}

// ─────────────────────────────────────────────────────────────
// Shared UI Components
// ─────────────────────────────────────────────────────────────
@Composable
fun LobbyBackground() {
    val ctx    = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(ctx).build().apply {
            val uri = Uri.parse("android.resource://${ctx.packageName}/raw/lobby_video")
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode    = Player.REPEAT_MODE_ALL
            volume        = 0f
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(Unit) { onDispose { player.release() } }
    AndroidView(
        factory  = { PlayerView(ctx).apply { this.player = player; useController = false } },
        modifier = Modifier.fillMaxSize().alpha(0.35f)
    )
}

@Composable
fun CrtOverlay(modifier: Modifier = Modifier) {
    val inf   = rememberInfiniteTransition(label = "crt")
    val sweep by inf.animateFloat(0f, 1f,
        infiniteRepeatable(tween(3200, easing = LinearEasing)), "sw")
    Box(modifier.fillMaxSize().drawWithContent {
        drawContent()
        var y = 0f; while (y < size.height) {
            drawLine(Color.Black.copy(0.12f), Offset(0f, y), Offset(size.width, y), 1f); y += 4f
        }
        drawRect(Brush.radialGradient(
            listOf(Color.Transparent, Color.Black.copy(0.35f)),
            Offset(size.width / 2f, size.height / 2f), size.width * 0.8f
        ))
        val sy = size.height * sweep
        drawRect(Brush.verticalGradient(
            listOf(Color.Transparent, Color.White.copy(0.02f), Color.Transparent), sy - 40f, sy + 40f
        ))
    })
}

@Composable
fun GlitchText(
    text     : String,
    intensity: Float,
    modifier : Modifier = Modifier,
    fontSize : Int      = 36,
    color    : Color    = Yellow
) {
    Box(modifier) {
        if (intensity > 0.7f)
            Text(text, color = Color.Red.copy(0.5f), fontSize = fontSize.sp, fontWeight = FontWeight.Black,
                letterSpacing = 3.sp, modifier = Modifier.offset(2.dp, 0.dp))
        if (intensity in 0.5f..0.8f)
            Text(text, color = Color.Cyan.copy(0.4f), fontSize = fontSize.sp, fontWeight = FontWeight.Black,
                letterSpacing = 3.sp, modifier = Modifier.offset((-2).dp, 0.dp))
        Text(text, color = color, fontSize = fontSize.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp,
            modifier = Modifier.offset((sin(intensity * 31.4f) * 3f * intensity).dp, 0.dp))
    }
}

@Composable
fun OmniButton(
    text    : String,
    onClick : () -> Unit,
    modifier: Modifier = Modifier,
    enabled : Boolean  = true,
    width   : Dp       = 220.dp,
    height  : Dp       = 52.dp,
    accent  : Color    = Yellow
) {
    val haptic = LocalHapticFeedback.current
    val inf    = rememberInfiniteTransition(label = "btn")
    val glow   by inf.animateFloat(0.6f, 1.0f,
        infiniteRepeatable(tween(1800, easing = EaseInOut), RepeatMode.Reverse), "glow")
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .width(width).height(height)
            .clip(RoundedCornerShape(2.dp))
            .background(Brush.verticalGradient(listOf(MetalBg.copy(0.95f), Color(0xFF0D0D0A))))
            .border(1.dp,
                Brush.horizontalGradient(listOf(accent.copy(0.2f), accent.copy(glow * 0.8f), accent.copy(0.2f))),
                RoundedCornerShape(2.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                enabled           = enabled
            ) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick() }
    ) {
        Text(text, color = if (enabled) accent else TextDim, fontSize = 13.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 2.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun OmniPanel(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(PanelBg)
            .border(1.dp, BorderCol, RoundedCornerShape(3.dp))
            .padding(12.dp),
        content = content
    )
}

@Composable
fun DividerLine(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(
        Brush.horizontalGradient(listOf(Color.Transparent, BorderCol, YellowDim.copy(0.3f), BorderCol, Color.Transparent))
    ))
}

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
fun OmniTextField(
    value        : String,
    onValueChange: (String) -> Unit,
    hint         : String,
    error        : String?  = null,
    isPassword   : Boolean  = false,
    modifier     : Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(2.dp))
                .background(MetalBg)
                .border(1.dp, if (error != null) DangerRed.copy(0.6f) else BorderCol, RoundedCornerShape(2.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.text.BasicTextField(
                value                = value,
                onValueChange        = onValueChange,
                singleLine           = true,
                visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                textStyle            = TextStyle(color = Yellow, fontSize = 13.sp, letterSpacing = 1.sp),
                cursorBrush          = SolidColor(Yellow),
                modifier             = Modifier.weight(1f),
                decorationBox        = { inner ->
                    if (value.isEmpty()) Text(hint, color = TextDim, fontSize = 12.sp); inner()
                }
            )
        }
        if (error != null) Text(error, color = DangerRed, fontSize = 10.sp)
    }
}

@Composable
fun StatusBar(label: String, value: Float, color: Color, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text(label, color = TextSec, fontSize = 10.sp, letterSpacing = 1.sp)
            Text("${(value * 100).toInt()}%", color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress  = { value },
            modifier  = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
            color     = color, trackColor = MetalBg
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Game UI
// ─────────────────────────────────────────────────────────────
@Composable
fun VhsOverlay() {
    val inf    = rememberInfiniteTransition(label = "vhs")
    val noiseY by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(120, easing = LinearEasing), RepeatMode.Reverse), "ny")
    val roll   by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(7000, easing = LinearEasing)), "roll")
    Box(Modifier.fillMaxSize().drawWithContent {
        drawContent()
        val stripH = size.height * 0.05f
        val stripY = size.height * ((noiseY + roll) % 1f)
        drawRect(Color.White.copy(0.03f), topLeft = Offset(0f, stripY),
            size = androidx.compose.ui.geometry.Size(size.width, stripH))
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
    onPause   : () -> Unit,
    onFlash   : () -> Unit,
    onMove    : (Float, Float, Float) -> Unit,
    onLook    : (Float, Float) -> Unit,
    onJump    : () -> Unit,
    onCrouch  : () -> Unit,
    onInteract: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.align(Alignment.TopStart).padding(16.dp).width(160.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StatusBar("HP",      gameState.playerHp / gameState.playerMaxHp,    DangerRed)
            StatusBar("AKL",     gameState.sanity / 100f,                        SouliumCol)
            StatusBar("STAMINA", gameState.stamina / gameState.staminaMax,       SuccessGreen)
            StatusBar("PILLER",  gameState.flashlightBattery,                    CrtAmber)
        }

        Row(
            Modifier.align(Alignment.TopEnd).padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            if (gameState.showPing)
                HudBadge("? ms", SuccessGreen)
            if (gameState.showFps)
                HudBadge("60 FPS", Yellow)
            IconButton(onClick = onPause, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Pause, null, tint = Yellow.copy(0.7f))
            }
        }

        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
            Arrangement.SpaceBetween, Alignment.Bottom
        ) {
            VirtualJoystick(Modifier.size(120.dp), onMove = { dx, dy -> onMove(dx, 0f, dy) })
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HudButton(Icons.Default.FlashlightOn,      CrtAmber, onFlash)
                HudButton(Icons.Default.NearMe,            TextSec,  onInteract)
                HudButton(Icons.Default.KeyboardArrowUp,   Yellow,   onJump)
                HudButton(Icons.Default.ArrowDownward,     TextSec,  onCrouch)
            }
        }
    }
}

@Composable
private fun HudBadge(text: String, color: Color) {
    Box(Modifier.clip(RoundedCornerShape(2.dp)).background(MetalBg.copy(0.8f)).padding(horizontal = 6.dp, vertical = 3.dp)) {
        Text(text, color = color, fontSize = 10.sp)
    }
}

@Composable
private fun HudButton(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(60.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MetalBg.copy(0.75f))
            .border(1.dp, tint.copy(0.4f), RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(28.dp)) }
}

@Composable
fun VirtualJoystick(modifier: Modifier, onMove: (Float, Float) -> Unit) {
    var knobX by remember { mutableFloatStateOf(0f) }
    var knobY by remember { mutableFloatStateOf(0f) }
    val radius = 48f
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(MetalBg.copy(0.6f))
            .border(1.dp, YellowDim, RoundedCornerShape(percent = 50))
            .pointerInput(Unit) {
                androidx.compose.foundation.gestures.detectDragGestures(
                    onDragEnd    = { knobX = 0f; knobY = 0f; onMove(0f, 0f) },
                    onDragCancel = { knobX = 0f; knobY = 0f; onMove(0f, 0f) }
                ) { _, drag ->
                    knobX = (knobX + drag.x).coerceIn(-radius, radius)
                    knobY = (knobY + drag.y).coerceIn(-radius, radius)
                    onMove(knobX / radius, knobY / radius)
                }
            }
    ) {
        Box(
            Modifier.size(40.dp)
                .offset(knobX.dp, knobY.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(Yellow.copy(0.6f))
        )
    }
}

@Composable
fun PauseOverlay(onResume: () -> Unit, onExit: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.75f)), Alignment.Center) {
        Column(
            Modifier.width(260.dp).clip(RoundedCornerShape(4.dp))
                .background(MetalBg).border(1.dp, BorderCol, RoundedCornerShape(4.dp)).padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.game_paused), color = Yellow, fontSize = 20.sp,
                fontWeight = FontWeight.Black, letterSpacing = 4.sp)
            DividerLine()
            OmniButton(stringResource(R.string.game_resume), onResume, width = 200.dp, height = 50.dp)
            OmniButton(stringResource(R.string.game_exit_menu), onExit, width = 200.dp, height = 50.dp, accent = DangerRed)
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
                .background(MetalBg).border(1.dp, DangerRed.copy(0.5f), RoundedCornerShape(4.dp)).padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.game_over_title), color = DangerRed.copy(pulse), fontSize = 28.sp,
                fontWeight = FontWeight.Black, letterSpacing = 4.sp)
            DividerLine()
            StatRow("Skor",      gameState.score.toString(),              Yellow)
            StatRow("Öldürülen", gameState.kills.toString(),              DangerRed)
            StatRow("Süre",      formatElapsed(gameState.sessionElapsed), TextSec)
            StatRow("Zorluk",    gameState.difficulty.uppercase(),        CrtAmber)
            DividerLine()
            OmniButton(stringResource(R.string.game_exit_menu), onExit, width = 220.dp, height = 50.dp, accent = DangerRed)
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
                .background(MetalBg).border(1.dp, SuccessGreen.copy(0.5f), RoundedCornerShape(4.dp)).padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.game_escaped_title), color = SuccessGreen.copy(glow), fontSize = 24.sp,
                fontWeight = FontWeight.Black, letterSpacing = 3.sp)
            DividerLine()
            StatRow("Skor",      gameState.score.toString(),              Yellow)
            StatRow("Öldürülen", gameState.kills.toString(),              DangerRed)
            StatRow("Süre",      formatElapsed(gameState.sessionElapsed), TextSec)
            StatRow("Zorluk",    gameState.difficulty.uppercase(),        CrtAmber)
            DividerLine()
            OmniButton(stringResource(R.string.game_exit_menu), onExit, width = 220.dp, height = 50.dp, accent = SuccessGreen)
        }
    }
}

@Composable
fun ModeSelect(onOffline: () -> Unit, onOnline: () -> Unit, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(DarkBg)) {
        LobbyBackground(); CrtOverlay()
        Column(Modifier.fillMaxSize()) {
            TopBarBack(stringResource(R.string.mode_select_title), onBack); DividerLine()
            Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
                Spacer(Modifier.height(24.dp))
                OmniButton(stringResource(R.string.mode_offline), onOffline, width = 260.dp, height = 64.dp, accent = Yellow)
                Spacer(Modifier.height(20.dp))
                OmniButton(stringResource(R.string.mode_online), onOnline, width = 260.dp, height = 64.dp, accent = CrtAmber)
            }
        }
    }
}

@Composable
fun DifficultySelect(onSelect: (String) -> Unit, onBack: () -> Unit) {
    val difficulties = listOf(
        Triple("easy",   R.string.difficulty_easy,   SuccessGreen),
        Triple("normal", R.string.difficulty_normal, Yellow),
        Triple("hard",   R.string.difficulty_hard,   DangerRed)
    )
    Box(Modifier.fillMaxSize().background(DarkBg)) {
        CrtOverlay()
        Column(Modifier.fillMaxSize()) {
            TopBarBack(stringResource(R.string.difficulty_title), onBack); DividerLine()
            Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
                difficulties.forEachIndexed { i, (key, res, col) ->
                    if (i > 0) Spacer(Modifier.height(18.dp))
                    OmniButton(stringResource(res), { onSelect(key) }, width = 260.dp, height = 60.dp, accent = col)
                }
            }
        }
    }
}

@Composable
fun OnlineSelect(onJoin: () -> Unit, onCreate: () -> Unit, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(DarkBg)) {
        CrtOverlay()
        Column(Modifier.fillMaxSize()) {
            TopBarBack(stringResource(R.string.online_select_title), onBack); DividerLine()
            Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
                OmniButton(stringResource(R.string.online_find_room),   onJoin,   width = 260.dp, height = 60.dp, accent = Yellow)
                Spacer(Modifier.height(18.dp))
                OmniButton(stringResource(R.string.online_create_room), onCreate, width = 260.dp, height = 60.dp, accent = CrtAmber)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// PlayerProfile ViewModel
// ─────────────────────────────────────────────────────────────
@HiltViewModel
class PlayerProfileVM @Inject constructor(
    private val api         : ApiService,
    private val settingsRepo: SettingsRepository
) : ViewModel() {

    private val _profile = MutableStateFlow(PlayerProfile())
    val profile: StateFlow<PlayerProfile> = _profile.asStateFlow()

    init { fetch() }

    private fun fetch() {
        viewModelScope.launch {
            runCatching { api.getProfile() }.onSuccess { _profile.value = it }
        }
    }

    fun updateName(name: String) {
        viewModelScope.launch {
            settingsRepo.saveName(name)
            val updated = _profile.value.copy(name = name)
            runCatching { api.updateProfile(updated) }.onSuccess { _profile.value = it }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Private helpers
// ─────────────────────────────────────────────────────────────
@Composable
private fun StatRow(label: String, value: String, color: Color) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(label, color = TextSec, fontSize = 12.sp)
        Text(value, color = color,   fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

private fun formatElapsed(ms: Long): String {
    val m = ms / 60_000; val s = (ms % 60_000) / 1000
    return "%02d:%02d".format(m, s)
}

private val GameState.vhsEnabled: Boolean get() = true
private val GameState.showFps   : Boolean get() = false
private val GameState.showPing  : Boolean get() = true
