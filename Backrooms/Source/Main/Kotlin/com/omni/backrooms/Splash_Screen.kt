package com.omni.backrooms

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.sin

@HiltViewModel
class SplashVM @Inject constructor(
    private val bridge: Native_Bridge
) : ViewModel() {
    init {
        viewModelScope.launch {
            bridge.initAudio()
            bridge.initCore(System.currentTimeMillis())
        }
    }
    override fun onCleared() {
        bridge.destroyAudio()
        bridge.destroyCore()
    }
}

@HiltViewModel
class LoadingVM @Inject constructor(
    private val repo: Asset_Repository
) : ViewModel() {
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress
    init {
        viewModelScope.launch {
            repo.preload().collect { _progress.value = it }
        }
    }
}

@Composable
fun Splash_Screen(onDone: () -> Unit, vm: SplashVM = hiltViewModel()) {
    val inf   = rememberInfiniteTransition(label = "sp")
    val alpha by animateFloatAsState(1f, tween(2000), label = "a")
    val glitch by inf.animateFloat(0f, 1f,
        infiniteRepeatable(tween(80, easing = LinearEasing), RepeatMode.Reverse), label = "g")
    val flicker by inf.animateFloat(0.92f, 1.0f,
        infiniteRepeatable(tween(120, easing = LinearEasing), RepeatMode.Reverse), label = "f")

    LaunchedEffect(Unit) { delay(3200); onDone() }

    Box(Modifier.fillMaxSize().background(Color.Black), Alignment.Center) {
        Crt_Overlay()
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Glitch_Text(
                text = stringResource(R.string.app_name),
                intensity = glitch,
                modifier = Modifier.alpha(alpha * flicker).padding(horizontal = 32.dp)
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.splash_tagline),
                color = TextSec.copy(alpha = alpha * 0.7f),
                fontSize = 13.sp, letterSpacing = 4.sp, textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun Loading_Screen(onDone: () -> Unit, vm: LoadingVM = hiltViewModel()) {
    val progress by vm.progress.collectAsState()
    val inf      = rememberInfiniteTransition(label = "ld")
    val flicker  by inf.animateFloat(0.82f, 1.0f,
        infiniteRepeatable(tween(160, easing = LinearEasing), RepeatMode.Reverse), label = "fl")
    val pan by inf.animateFloat(0f, 1f,
        infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "pan")

    LaunchedEffect(progress) { if (progress >= 1f) { delay(400); onDone() } }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Corridor_Canvas(pan = pan, flicker = flicker, modifier = Modifier.fillMaxSize())
        Crt_Overlay()
        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp, start = 64.dp, end = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Carpet_Bar(progress = progress, modifier = Modifier.fillMaxWidth().height(16.dp))
            Spacer(Modifier.height(8.dp))
            Text("${(progress * 100).toInt()}%",
                color = TextDim, fontSize = 11.sp, letterSpacing = 2.sp)
        }
    }
}

@Composable
private fun Corridor_Canvas(pan: Float, flicker: Float, modifier: Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val cx = w / 2f;    val cy = h / 2f
        drawRect(Color(0xFF1A1508))
        for (i in 0..6) {
            val t   = (i / 6f + pan * 0.15f) % 1f
            val per = 1f - t * 0.92f
            val ww  = w * per; val hh = h * per
            val alpha = (1f - t * 0.8f) * 0.6f
            drawRect(color = Yellow.copy(alpha * 0.08f),
                topLeft = Offset(cx - ww / 2f, cy - hh / 2f),
                size = Size(ww, hh),
                style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
            val ly = (cy - hh / 2f) + hh * 0.05f
            drawRect(brush = Brush.radialGradient(
                listOf(Color(0xFFEEDD99).copy(alpha * flicker * 0.9f),
                    Yellow.copy(alpha * flicker * 0.3f), Color.Transparent),
                center = Offset(cx, ly), radius = ww * 0.3f),
                topLeft = Offset(cx - ww * 0.15f, ly - 20f),
                size = Size(ww * 0.3f, 40f))
        }
        drawRect(brush = Brush.radialGradient(
            listOf(Color.Transparent, Color.Black.copy(0.7f)),
            center = Offset(cx, cy), radius = w * 0.75f))
    }
}

@Composable
private fun Carpet_Bar(progress: Float, modifier: Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        drawRoundRect(Color(0xFF1A1208), cornerRadius = CornerRadius(h / 2f))
        if (progress > 0f) {
            val fw = w * progress
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    listOf(Color(0xFF3D2B10), Color(0xFF8B6914), Color(0xFFD4A84B), Color(0xFFAA8030)),
                    startX = 0f, endX = fw),
                size = Size(fw, h), cornerRadius = CornerRadius(h / 2f))
            val sc = (fw / (h * 1.5f)).toInt()
            for (j in 0..sc) {
                val sx = j * h * 1.5f
                if (sx > fw) break
                drawLine(Color(0xFF2A1A08).copy(0.4f), Offset(sx, 0f), Offset(sx + h * 0.5f, h), 2f)
            }
            drawRect(Brush.verticalGradient(listOf(Color.White.copy(0.15f), Color.Transparent)),
                topLeft = Offset(0f, 0f), size = Size(fw, h / 2f))
        }
        drawRoundRect(Color(0xFF5A4020),
            cornerRadius = CornerRadius(h / 2f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
    }
}

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GuardVM @Inject constructor(
    private val bridge: Native_Bridge
) : ViewModel() {

    private val _status = MutableStateFlow<GuardStatus?>(null)
    val status: StateFlow<GuardStatus?> = _status

    fun runCheck(ctx: Any) {
        viewModelScope.launch {
            bridge.initGuard(ctx, BuildConfig.EXPECTED_SIG_HASH)
            val flags = bridge.runGuardScan()
            _status.value = GuardStatus(
                flags            = flags,
                isRooted         = bridge.isRooted(),
                isFrida          = bridge.isFridaDetected(),
                isDebugged       = bridge.isDebugged(),
                isEmulator        = bridge.isEmulator(),
                isSignatureValid  = bridge.isSignatureValid(),
                report            = bridge.getThreatReport()
            )
        }
    }

    override fun onCleared() {
        bridge.destroyGuard()
        super.onCleared()
    }
}
