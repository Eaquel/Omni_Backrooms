package com.omni.backrooms

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────
// Credits Screen
// ─────────────────────────────────────────────────────────────
@Composable
fun Credits(onBack: () -> Unit) {
    val inf    = rememberInfiniteTransition(label = "credits")
    val scroll by inf.animateFloat(0f, 1f,
        infiniteRepeatable(tween(32_000, easing = LinearEasing)), "sc")

    Box(Modifier.fillMaxSize().background(Color(0xFF050503))) {
        CrtOverlay()
        IconButton(
            onClick  = onBack,
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp).graphicsLayer { alpha = 0.85f }
        ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Yellow) }

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val totalH = maxHeight
            val offset = (-scroll * 1800f) + totalH.value

            Column(
                Modifier.fillMaxWidth().graphicsLayer { translationY = offset },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Spacer(Modifier.height(totalH))

                Text(stringResource(R.string.credits_title), color = Yellow,
                    fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = 5.sp, textAlign = TextAlign.Center)

                Spacer(Modifier.height(32.dp)); DividerLine(Modifier.width(200.dp)); Spacer(Modifier.height(32.dp))

                CreditsSection("Geliştirici", listOf(stringResource(R.string.credits_developer_name)), Yellow, 20.sp)

                Spacer(Modifier.height(24.dp)); DividerLine(Modifier.width(120.dp)); Spacer(Modifier.height(24.dp))

                CreditsSection("Oyun Tasarımı",          listOf("Eaquel"), TextSec, 14.sp)
                CreditsSection("Native Engine (C++26)",  listOf("Eaquel"), TextSec, 14.sp) // C++23 → C++26
                CreditsSection("UI/UX Tasarım",          listOf("Eaquel"), TextSec, 14.sp)
                CreditsSection("Ağ Altyapısı",           listOf("Eaquel"), TextSec, 14.sp)
                CreditsSection("Ses Motoru",              listOf("Eaquel"), TextSec, 14.sp)

                Spacer(Modifier.height(24.dp)); DividerLine(Modifier.width(120.dp)); Spacer(Modifier.height(24.dp))

                CreditsSection(
                    stringResource(R.string.credits_testers_label),
                    listOf(
                        "Alpha_Wanderer", "NoclipRunner", "LevelZeroSurvivor",
                        "YellowWallHunter", "FluorescentDread", "CarpetCrawler",
                        "BackroomsExplorer_7", "VoidWatcher", "HummingLight",
                        "CorridorGhost", "StaticNoise", "LiminalDrifter"
                    ),
                    TextDim, 13.sp
                )

                Spacer(Modifier.height(24.dp)); DividerLine(Modifier.width(120.dp)); Spacer(Modifier.height(24.dp))

                CreditsSection("Kullanılan Teknolojiler", listOf(
                    "Kotlin 2.3.21",
                    "C++26 / NDK 29.0.14206865",
                    "CMake 4.3.2",
                    "Jetpack Compose BOM 2026.04.01",
                    "Hilt 2.59.2",
                    "Room 2.8.4",
                    "ExoPlayer / Media3 1.10.0",
                    "Agora RTC 4.6.3",
                    "Retrofit 2.11.0 + Kotlinx Serialization",
                    "OpenSL ES / OpenGL ES 3.0",
                    "Firebase BOM 34.12.0"
                ), TextDim, 12.sp)

                Spacer(Modifier.height(48.dp)); DividerLine(Modifier.width(80.dp)); Spacer(Modifier.height(24.dp))

                Text(stringResource(R.string.app_name), color = TextDim.copy(0.4f), fontSize = 11.sp, letterSpacing = 4.sp)
                Text("© 2026 Eaquel — Tüm Hakları Saklıdır", color = TextDim.copy(0.3f), fontSize = 9.sp, letterSpacing = 2.sp)

                Spacer(Modifier.height(totalH))
            }
        }
    }
}

@Composable
private fun CreditsSection(title: String, names: List<String>, color: Color, fontSize: TextUnit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, color = TextDim, fontSize = 10.sp, letterSpacing = 3.sp)
        names.forEach { name ->
            Text(name, color = color, fontSize = fontSize, letterSpacing = 1.sp, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(8.dp))
    }
}

// ─────────────────────────────────────────────────────────────
// Changelog — merged here (< 10 KB standalone)
// ─────────────────────────────────────────────────────────────
data class ChangelogUiState(
    val entries  : List<ChangelogDto> = emptyList(),
    val isLoading: Boolean            = false
)

@HiltViewModel
class ChangelogVM @Inject constructor(
    private val api: ApiService // was "Api_Service"
) : ViewModel() {

    private val _state = MutableStateFlow(ChangelogUiState())
    val state: StateFlow<ChangelogUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            runCatching { api.getChangelog() }
                .onSuccess { chs -> _state.update { it.copy(isLoading = false, entries = chs) } }
                .onFailure {       _state.update { it.copy(isLoading = false, entries = fallback()) } }
        }
    }

    private fun fallback(): List<ChangelogDto> = listOf(
        ChangelogDto("v1.0.0-beta", System.currentTimeMillis(), listOf(
            "Guard: Root, Frida, Debug, Emulator, Zygisk, KSU, LSPosed tespiti",
            "Guard: İmza doğrulama + AES-256-GCM video şifreleme",
            "Sound: Stereo 44100Hz, LPF, Reverb, FootstepSynth, MonsterSynth",
            "Entity: 8 entity tipi, A* pathfinding, 8 AI state (BehaviorTree)",
            "Network: RttEstimator, ReliableChannel, JitterBuffer, VoiceMultiplexer",
            "Core: PerlinNoise FBM, PlayerPhysics AABB, CameraController bob+roll",
            "UI: 14 ekran, Jetpack Compose, Material3, Hilt 2.59.2",
            "Native Engine: C++26, NDK r29, CMake 4.3.2"
        ), "beta"),
        ChangelogDto("v0.9.5-beta", System.currentTimeMillis() - 604_800_000L, listOf(
            "Arayüz: Ana menü CRT efekti güçlendirildi",
            "Video: ExoPlayer Media3 1.10.0 entegrasyonu",
            "Ses: Floresan vızıltı native motoru"
        ), "beta"),
        ChangelogDto("v0.9.2-beta", System.currentTimeMillis() - 1_209_600_000L, listOf(
            "Online: UDP soket katmanı + ReliableChannel",
            "Online: Şifreli oda desteği",
            "Voice: Agora RTC 4.6.3 + PTT modu"
        ), "beta"),
        ChangelogDto("v0.9.0-beta", System.currentTimeMillis() - 2_592_000_000L, listOf(
            "Entity AI: C++26 A* pathfinding",
            "Entity AI: 8 davranış state'i",
            "Guard: İlk anti-cheat katmanı"
        ), "beta")
    )
}

@Composable
fun Changelog(onBack: () -> Unit, vm: ChangelogVM = hiltViewModel()) {
    val s by vm.state.collectAsState()

    Box(Modifier.fillMaxSize().background(Color(0xFF050503))) {
        CrtOverlay()
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().background(Color.Black.copy(0.6f)).padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Yellow) }
                Text(stringResource(R.string.changelog_title), color = Yellow, fontSize = 16.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                Spacer(Modifier.width(12.dp))
                Box(Modifier.clip(RoundedCornerShape(2.dp)).background(MetalBg).padding(horizontal = 8.dp, vertical = 3.dp)) {
                    Text(stringResource(R.string.changelog_branch_beta), color = CrtAmber, fontSize = 10.sp, letterSpacing = 2.sp)
                }
            }
            DividerLine()
            if (s.isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = Yellow, strokeWidth = 2.dp)
                }
            } else {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) { s.entries.forEach { ChangelogCard(it) } }
            }
        }
    }
}

@Composable
private fun ChangelogCard(entry: ChangelogDto) {
    OmniPanel(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(entry.version, color = Yellow, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(entry.branch.uppercase(), color = CrtAmber, fontSize = 9.sp, letterSpacing = 2.sp)
                    Text(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(entry.dateMs)),
                        color = TextDim, fontSize = 10.sp)
                }
            }
            DividerLine()
            entry.changes.forEach { item ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                    Text("›", color = YellowDim, fontSize = 14.sp)
                    Text(item, color = TextSec, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Loading Screen — merged here (< 10 KB standalone)
// ─────────────────────────────────────────────────────────────
@HiltViewModel
class LoadingVM @Inject constructor(
    private val assetManager: AssetManager
) : ViewModel() {

    private val _progress = MutableStateFlow(0f)
    private val _stage    = MutableStateFlow("")
    private val _done     = MutableStateFlow(false)

    val progress: StateFlow<Float>   = _progress.asStateFlow()
    val stage   : StateFlow<String>  = _stage.asStateFlow()
    val done    : StateFlow<Boolean> = _done.asStateFlow()

    init {
        viewModelScope.launch {
            assetManager.preload().collect { event ->
                _progress.value = event.progress
                _stage.value    = event.stage
                if (event.progress >= 1f) _done.value = true
            }
        }
    }
}

@Composable
fun Loading(onDone: () -> Unit, vm: LoadingVM = hiltViewModel()) {
    val progress by vm.progress.collectAsState()
    val stage    by vm.stage.collectAsState()
    val done     by vm.done.collectAsState()

    LaunchedEffect(done) { if (done) onDone() }

    Box(Modifier.fillMaxSize().background(DarkBg)) {
        CrtOverlay()
        Column(
            Modifier.fillMaxWidth().align(Alignment.Center).padding(horizontal = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            GlitchText(stringResource(R.string.app_name), progress, fontSize = 28, color = Yellow)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress  = { progress },
                modifier  = Modifier.fillMaxWidth().height(3.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp)),
                color     = Yellow, trackColor = MetalBg
            )
            Text(stage, color = TextSec, fontSize = 11.sp, letterSpacing = 2.sp)
            Text("${(progress * 100).toInt()}%", color = YellowDim, fontSize = 10.sp)
        }
        Text(
            "© 2026 Eaquel",
            color    = TextDim.copy(0.4f),
            fontSize = 9.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Splash Screen
// ─────────────────────────────────────────────────────────────
@Composable
fun Splash(onDone: () -> Unit) {
    val inf   = rememberInfiniteTransition(label = "splash")
    val alpha by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1200, easing = EaseInOut), RepeatMode.Reverse), "a")

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2_500)
        onDone()
    }

    Box(Modifier.fillMaxSize().background(Color.Black), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                stringResource(R.string.app_name),
                color      = Yellow.copy(alpha),
                fontSize   = 40.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 8.sp
            )
            Text(
                "THE BACKROOMS",
                color     = TextDim.copy(alpha * 0.6f),
                fontSize  = 12.sp,
                letterSpacing = 6.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Menu Screen
// ─────────────────────────────────────────────────────────────
@HiltViewModel
class MenuVM @Inject constructor(
    private val api    : ApiService,
    private val profile: PlayerProfileVM
) : ViewModel() {

    val playerProfile: StateFlow<PlayerProfile> = profile.profile

    init {
        viewModelScope.launch {
            runCatching { api.getActiveEvents() }
        }
    }
}

@Composable
fun Menu(
    onNewGame  : () -> Unit,
    onEvents   : () -> Unit,
    onSettings : () -> Unit,
    onMarket   : () -> Unit,
    onStory    : () -> Unit,
    onCredits  : () -> Unit,
    onChangelog: () -> Unit,
    onMaps     : () -> Unit,
    onChars    : () -> Unit,
    onLeader   : () -> Unit
) {
    Box(Modifier.fillMaxSize().background(DarkBg)) {
        LobbyBackground(); CrtOverlay()

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(80.dp))

            GlitchText(
                text      = stringResource(R.string.app_name),
                intensity = 0.4f,
                fontSize  = 36,
                color     = Yellow,
                modifier  = Modifier.padding(bottom = 8.dp)
            )
            Text("THE BACKROOMS", color = TextDim, fontSize = 11.sp, letterSpacing = 6.sp)

            Spacer(Modifier.height(48.dp))
            DividerLine(Modifier.width(180.dp))
            Spacer(Modifier.height(32.dp))

            val primary = listOf(
                R.string.menu_new_game    to onNewGame,
                R.string.menu_events      to onEvents,
                R.string.menu_market      to onMarket,
                R.string.menu_maps        to onMaps,
                R.string.menu_characters  to onChars,
                R.string.menu_leaderboard to onLeader,
                R.string.menu_story       to onStory
            )
            primary.forEach { (res, action) ->
                OmniButton(stringResource(res), action, width = 240.dp, height = 52.dp)
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(8.dp))
            DividerLine(Modifier.width(120.dp))
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OmniButton(stringResource(R.string.menu_settings),  onSettings,  width = 112.dp, height = 44.dp, accent = TextSec)
                OmniButton(stringResource(R.string.menu_changelog), onChangelog, width = 112.dp, height = 44.dp, accent = TextSec)
                OmniButton(stringResource(R.string.menu_credits),   onCredits,   width = 112.dp, height = 44.dp, accent = TextSec)
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}
