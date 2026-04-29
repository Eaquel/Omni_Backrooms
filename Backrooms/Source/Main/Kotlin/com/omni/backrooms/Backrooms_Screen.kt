package com.omni.backrooms

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.material3.Text
import kotlin.math.sin

val Yellow      = Color(0xFFD4A84B)
val YellowDim   = Color(0x80D4A84B)
val YellowGlow  = Color(0xFFE8C060)
val DarkBg      = Color(0xFF0A0A08)
val PanelBg     = Color(0xCC121208)
val MetalBg     = Color(0xFF1A1A14)
val CrtAmber    = Color(0xFFFFB347)
val TextPrimary = Color(0xFFD4A84B)
val TextSec     = Color(0xFF8A7040)
val TextDim     = Color(0xFF5A4A28)
val BorderCol   = Color(0xFF2A2018)
val SouliumCol  = Color(0xFF7B68EE)
val OmniumCol   = Color(0xFF00E5FF)
val DangerRed   = Color(0xFFCC2200)

@Composable
fun Omni_Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary    = Yellow,
            onPrimary  = Color.Black,
            secondary  = CrtAmber,
            background = DarkBg,
            surface    = MetalBg,
            onBackground = Yellow,
            onSurface  = Yellow,
            outline    = BorderCol
        ),
        content = content
    )
}

sealed class Route(val path: String) {
    data object Splash      : Route("splash")
    data object Loading     : Route("loading")
    data object Menu        : Route("menu")
    data object ModeSelect  : Route("mode_select")
    data object Difficulty  : Route("difficulty")
    data object OnlineSelect: Route("online_select")
    data object RoomList    : Route("room_list")
    data object CreateRoom  : Route("create_room")
    data object Settings    : Route("settings")
    data object UiEditor    : Route("ui_editor")
    data object Market      : Route("market")
    data object Story       : Route("story")
    data object Credits     : Route("credits")
    data object Changelog   : Route("changelog")
    data object Events      : Route("events")
    data object Game        : Route("game/{difficulty}/{online}") {
        fun go(d: String, o: Boolean) = "game/$d/$o"
    }
}

@Composable
fun Backrooms_Screen(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Route.Splash.path) {

        composable(Route.Splash.path) {
            Splash_Screen(onDone = {
                navController.navigate(Route.Loading.path) {
                    popUpTo(Route.Splash.path) { inclusive = true }
                }
            })
        }

        composable(Route.Loading.path) {
            Loading_Screen(onDone = {
                navController.navigate(Route.Menu.path) {
                    popUpTo(Route.Loading.path) { inclusive = true }
                }
            })
        }

        composable(Route.Menu.path) {
            Menu_Screen(
                onNewGame  = { navController.navigate(Route.ModeSelect.path) },
                onEvents   = { navController.navigate(Route.Events.path) },
                onSettings = { navController.navigate(Route.Settings.path) },
                onMarket   = { navController.navigate(Route.Market.path) },
                onStory    = { navController.navigate(Route.Story.path) },
                onCredits  = { navController.navigate(Route.Credits.path) },
                onChangelog= { navController.navigate(Route.Changelog.path) }
            )
        }

        composable(Route.ModeSelect.path) {
            Mode_Screen(
                onOffline = { navController.navigate(Route.Difficulty.path) },
                onOnline  = { navController.navigate(Route.OnlineSelect.path) },
                onBack    = { navController.popBackStack() }
            )
        }

        composable(Route.Difficulty.path) {
            Difficulty_Screen(
                onSelect = { d ->
                    navController.navigate(Route.Game.go(d, false)) {
                        popUpTo(Route.Menu.path)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Route.OnlineSelect.path) {
            Online_Screen(
                onJoin   = { navController.navigate(Route.RoomList.path) },
                onCreate = { navController.navigate(Route.CreateRoom.path) },
                onBack   = { navController.popBackStack() }
            )
        }

        composable(Route.RoomList.path) {
            Room_Screen(
                onJoined = { navController.navigate(Route.Game.go("normal", true)) { popUpTo(Route.Menu.path) } },
                onBack   = { navController.popBackStack() }
            )
        }

        composable(Route.CreateRoom.path) {
            Create_Room_Screen(
                onCreated = { navController.navigate(Route.Game.go("normal", true)) { popUpTo(Route.Menu.path) } },
                onBack    = { navController.popBackStack() }
            )
        }

        composable(Route.Settings.path) {
            SettingsScreen(
                onUiEditor = { navController.navigate(Route.UiEditor.path) },
                onBack     = { navController.popBackStack() }
            )
        }

        composable(Route.UiEditor.path) {
            Editor_Model(onSave = { navController.popBackStack() })
        }

        composable(Route.Market.path)   { Market_Screen(onBack  = { navController.popBackStack() }) }
        composable(Route.Story.path)    { Story_Screen(onBack   = { navController.popBackStack() }) }
        composable(Route.Credits.path)  { Credits_Screen(onBack = { navController.popBackStack() }) }
        composable(Route.Changelog.path){ Changelog_Screen(onBack={ navController.popBackStack() }) }
        composable(Route.Events.path)   { Events_Screen(onBack  = { navController.popBackStack() }) }
    }
}

@Composable
fun Crt_Overlay(modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "crt")
    val sweep by inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing)),
        label = "sweep"
    )
    Box(modifier = modifier.fillMaxSize().drawWithContent {
        drawContent()
        var y = 0f
        while (y < size.height) {
            drawLine(Color.Black.copy(alpha = 0.12f), Offset(0f, y), Offset(size.width, y), 1f)
            y += 4f
        }
        drawRect(brush = Brush.radialGradient(
            listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = size.width * 0.8f
        ))
        val sy = size.height * sweep
        drawRect(brush = Brush.verticalGradient(
            listOf(Color.Transparent, Color.White.copy(alpha = 0.02f), Color.Transparent),
            startY = sy - 40f, endY = sy + 40f
        ))
    })
}

@Composable
fun Glitch_Text(
    text: String,
    intensity: Float,
    modifier: Modifier = Modifier,
    fontSize: Int = 36,
    color: Color = Yellow
) {
    Box(modifier = modifier) {
        if (intensity > 0.7f)
            Text(text, color = Color.Red.copy(alpha = 0.5f), fontSize = fontSize.sp,
                fontWeight = FontWeight.Black, letterSpacing = 3.sp,
                modifier = Modifier.offset(2.dp, 0.dp))
        if (intensity in 0.5f..0.8f)
            Text(text, color = Color.Cyan.copy(alpha = 0.4f), fontSize = fontSize.sp,
                fontWeight = FontWeight.Black, letterSpacing = 3.sp,
                modifier = Modifier.offset((-2).dp, 0.dp))
        Text(text, color = color, fontSize = fontSize.sp,
            fontWeight = FontWeight.Black, letterSpacing = 3.sp,
            modifier = Modifier.offset((sin(intensity * 31.4f) * 3f * intensity).dp, 0.dp))
    }
}

@Composable
fun Omni_Button(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    width: Dp = 220.dp,
    height: Dp = 52.dp,
    accent: Color = Yellow
) {
    val haptic = LocalHapticFeedback.current
    val inf    = rememberInfiniteTransition(label = "btn")
    val glow by inf.animateFloat(
        0.6f, 1.0f,
        infiniteRepeatable(tween(1800, easing = EaseInOut), RepeatMode.Reverse),
        label = "glow"
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .width(width).height(height)
            .clip(RoundedCornerShape(2.dp))
            .background(Brush.verticalGradient(listOf(MetalBg.copy(0.95f), Color(0xFF0D0D0A))))
            .border(1.dp,
                Brush.horizontalGradient(listOf(accent.copy(0.2f), accent.copy(glow * 0.8f), accent.copy(0.2f))),
                RoundedCornerShape(2.dp))
            .clickable(enabled = enabled) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
    ) {
        Text(text, color = if (enabled) accent else TextDim,
            fontSize = 13.sp, fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun Omni_Panel(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
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
fun Divider_Line(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().height(1.dp).background(
        Brush.horizontalGradient(listOf(
            Color.Transparent, BorderCol, YellowDim.copy(0.3f), BorderCol, Color.Transparent
        ))
    ))
}

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerProfileVM @Inject constructor(
    private val api: Api_Service,
    private val settingsRepo: Settings_Repository
) : ViewModel() {

    private val _profile = MutableStateFlow(PlayerProfile())
    val profile: StateFlow<PlayerProfile> = _profile.asStateFlow()

    init { fetchProfile() }

    private fun fetchProfile() {
        viewModelScope.launch {
            runCatching { api.getProfile() }
                .onSuccess { _profile.value = it }
        }
    }

    fun updateName(name: String) {
        viewModelScope.launch {
            settingsRepo.saveName(name)
            val updated = _profile.value.copy(name = name)
            runCatching { api.updateProfile(updated) }
                .onSuccess { _profile.value = it }
        }
    }
}

@HiltViewModel
class GuardStatusVM @Inject constructor(
    private val bridge: Native_Bridge
) : ViewModel() {
    private val _guardStatus = MutableStateFlow<GuardStatus?>(null)
    val guardStatus: StateFlow<GuardStatus?> = _guardStatus.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val flags = bridge.runGuardScan()
            _guardStatus.value = GuardStatus(
                flags             = flags,
                isRooted          = bridge.isRooted(),
                isFrida           = bridge.isFridaDetected(),
                isDebugged        = bridge.isDebugged(),
                isEmulator        = bridge.isEmulator(),
                isSignatureValid  = bridge.isSignatureValid(),
                report            = bridge.getThreatReport()
            )
        }
    }
}
