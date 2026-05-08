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
import javax.inject.Inject

@Composable
fun Credits(onBack: () -> Unit) {
    val inf    = rememberInfiniteTransition(label = "credits")
    val scroll by inf.animateFloat(0f, 1f,
        infiniteRepeatable(tween(32000, easing=LinearEasing)), "sc")

    Box(Modifier.fillMaxSize().background(Color(0xFF050503))) {
        CrtOverlay()
        IconButton(
            onClick  = onBack,
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp).graphicsLayer { alpha = 0.85f }
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint=Yellow)
        }

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val totalH = maxHeight
            val offset = (-scroll * 1800f) + totalH.value

            Column(
                Modifier.fillMaxWidth().graphicsLayer { translationY = offset },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Spacer(Modifier.height(totalH))

                Text(stringResource(R.string.credits_title), color=Yellow,
                    fontSize=28.sp, fontWeight=FontWeight.Black, letterSpacing=5.sp, textAlign=TextAlign.Center)

                Spacer(Modifier.height(32.dp))
                DividerLine(Modifier.width(200.dp))
                Spacer(Modifier.height(32.dp))

                CreditsSection("Geliştirici", listOf(stringResource(R.string.credits_developer_name)), Yellow, 20.sp)

                Spacer(Modifier.height(24.dp))
                DividerLine(Modifier.width(120.dp))
                Spacer(Modifier.height(24.dp))

                CreditsSection("Oyun Tasarımı",         listOf("Eaquel"), TextSec, 14.sp)
                CreditsSection("Native Engine (C++23)", listOf("Eaquel"), TextSec, 14.sp)
                CreditsSection("UI/UX Tasarım",         listOf("Eaquel"), TextSec, 14.sp)
                CreditsSection("Ağ Altyapısı",          listOf("Eaquel"), TextSec, 14.sp)
                CreditsSection("Ses Motoru",             listOf("Eaquel"), TextSec, 14.sp)

                Spacer(Modifier.height(24.dp))
                DividerLine(Modifier.width(120.dp))
                Spacer(Modifier.height(24.dp))

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

                Spacer(Modifier.height(24.dp))
                DividerLine(Modifier.width(120.dp))
                Spacer(Modifier.height(24.dp))

                CreditsSection("Kullanılan Teknolojiler", listOf(
                    "Kotlin 2.3.21", "C++23 / NDK 29", "Jetpack Compose",
                    "Hilt 2.59.2", "Room 2.8.4", "ExoPlayer 1.10.0",
                    "Agora RTC 4.6.3", "Retrofit", "OpenSL ES"
                ), TextDim, 12.sp)

                Spacer(Modifier.height(48.dp))
                DividerLine(Modifier.width(80.dp))
                Spacer(Modifier.height(24.dp))

                Text(stringResource(R.string.app_name), color=TextDim.copy(0.4f), fontSize=11.sp, letterSpacing=4.sp)
                Text("© 2026 Eaquel — Tüm Hakları Saklıdır", color=TextDim.copy(0.3f), fontSize=9.sp, letterSpacing=2.sp)

                Spacer(Modifier.height(totalH))
            }
        }
    }
}

@Composable
private fun CreditsSection(title: String, names: List<String>, color: Color, fontSize: TextUnit) {
    Column(horizontalAlignment=Alignment.CenterHorizontally, verticalArrangement=Arrangement.spacedBy(5.dp)) {
        Text(title, color=TextDim, fontSize=10.sp, letterSpacing=3.sp)
        names.forEach { name ->
            Text(name, color=color, fontSize=fontSize, letterSpacing=1.sp, textAlign=TextAlign.Center)
        }
        Spacer(Modifier.height(8.dp))
    }
}

data class ChangelogUiState(
    val entries   : List<ChangelogDto> = emptyList(),
    val isLoading : Boolean            = false
)

@HiltViewModel
class ChangelogVM @Inject constructor(private val api: Api_Service) : ViewModel() {
    private val _state = MutableStateFlow(ChangelogUiState())
    val state: StateFlow<ChangelogUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading=true) }
            runCatching { api.getChangelog() }
                .onSuccess { chs -> _state.update { it.copy(isLoading=false, entries=chs) } }
                .onFailure { _state.update { it.copy(isLoading=false, entries=fallback()) } }
        }
    }

    private fun fallback(): List<ChangelogDto> = listOf(
        ChangelogDto("v1.0.0-beta", System.currentTimeMillis(), listOf(
            "Guard: Root, Frida, Debug, Emulator tespiti eklendi",
            "Guard: İmza doğrulama ve şifreli video sistemi",
            "Sound: Stereo 44100Hz, LowPassFilter, ReverbTail, FootstepSynth, MonsterSynth",
            "Entity: 8 entity tipi, A* pathfinding, 8 AI state (BehaviorTree)",
            "Network: RttEstimator, ReliableChannel, JitterBuffer, VoiceMultiplexer",
            "Core: PerlinNoise FBM, PlayerPhysics AABB, CameraController bob+roll",
            "UI: 14 screen, Jetpack Compose, Material3, Hilt 2.59.2",
            "Assets: 8 Level tema, 5 Karakter sınıfı, 8 EntityType"
        ), "beta"),
        ChangelogDto("v0.9.5-beta", System.currentTimeMillis() - 604800000, listOf(
            "Arayüz: Ana menü CRT efekti güçlendirildi",
            "Arayüz: Halı progress bar eklendi",
            "Video: ExoPlayer 1.10.0 entegrasyonu",
            "Ses: Floresan vızıltı native motoru"
        ), "beta"),
        ChangelogDto("v0.9.2-beta", System.currentTimeMillis() - 1209600000, listOf(
            "Online: UDP soket katmanı",
            "Online: Oda oluştur/bul akışı",
            "Online: Şifreli oda desteği",
            "Voice: Agora RTC 4.6.3 entegrasyonu",
            "Voice: PTT modu eklendi"
        ), "beta"),
        ChangelogDto("v0.9.0-beta", System.currentTimeMillis() - 2592000000, listOf(
            "Entity AI: C++23 A* pathfinding",
            "Entity AI: 8 davranış state'i",
            "Billboard: Dinamik uyarı tabelaları",
            "Guard: İlk anti-cheat katmanı",
            "NDK 29, CMake 4.3.2 güncellendi"
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
                Modifier.fillMaxWidth().background(Color.Black.copy(0.6f)).padding(horizontal=8.dp, vertical=4.dp),
                verticalAlignment=Alignment.CenterVertically
            ) {
                IconButton(onClick=onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint=Yellow) }
                Text(stringResource(R.string.changelog_title), color=Yellow, fontSize=16.sp, fontWeight=FontWeight.Bold, letterSpacing=3.sp)
                Spacer(Modifier.width(12.dp))
                Box(
                    Modifier.clip(RoundedCornerShape(2.dp)).background(MetalBg).padding(horizontal=8.dp, vertical=3.dp)
                ) {
                    Text(stringResource(R.string.changelog_branch_beta), color=CrtAmber, fontSize=10.sp, letterSpacing=2.sp)
                }
            }

            DividerLine()

            if (s.isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color=Yellow, strokeWidth=2.dp) }
            } else {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=20.dp, vertical=14.dp),
                    verticalArrangement=Arrangement.spacedBy(14.dp)
                ) {
                    s.entries.forEach { entry -> ChangelogCard(entry) }
                }
            }
        }
    }
}

@Composable
private fun ChangelogCard(entry: ChangelogDto) {
    OmniPanel(Modifier.fillMaxWidth()) {
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(entry.version, color=Yellow, fontSize=14.sp, fontWeight=FontWeight.Bold, letterSpacing=1.sp)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp), verticalAlignment=Alignment.CenterVertically) {
                    Text(entry.branch.uppercase(), color=CrtAmber, fontSize=9.sp, letterSpacing=2.sp)
                    Text(
                        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                            .format(java.util.Date(entry.dateMs)),
                        color=TextDim, fontSize=10.sp
                    )
                }
            }
            DividerLine()
            entry.changes.forEach { item ->
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp), verticalAlignment=Alignment.Top) {
                    Text("›", color=YellowDim, fontSize=14.sp)
                    Text(item, color=TextSec, fontSize=12.sp, lineHeight=18.sp)
                }
            }
        }
    }
}
