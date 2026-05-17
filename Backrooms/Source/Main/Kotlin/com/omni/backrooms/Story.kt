package com.omni.backrooms

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.sin

// SplashVM → Asset.kt içindeki GuardManager + Credits.kt LoadingVM ile değiştirildi

// LoadingVM → Credits.kt'ye taşındı

// Splash → Credits.kt'ye taşındı

// Loading → Credits.kt'ye taşındı

@Composable
private fun ScanlineSweep() {
    val inf    = rememberInfiniteTransition(label = "sw")
    val offset by inf.animateFloat(0f, 1f,
        infiniteRepeatable(tween(5000, easing = LinearEasing)), "off")
    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
        val sy = size.height * offset
        drawRect(Brush.verticalGradient(
            listOf(Color.Transparent, Color.White.copy(0.025f), Color.Transparent),
            sy - 50f, sy + 50f
        ))
    }
}

@Composable
private fun FluorescentHumVisualizer(modifier: Modifier) {
    val inf  = rememberInfiniteTransition(label = "hum")
    val time by inf.animateFloat(0f, 100f,
        infiniteRepeatable(tween(20000, easing = LinearEasing)), "t")
    androidx.compose.foundation.Canvas(modifier) {
        val w = size.width; val h = size.height; val segments = 40
        for (i in 0 until segments) {
            val x    = (i.toFloat() / segments) * w
            val wave = sin(i * 0.4f + time * 0.3f) * 0.5f + 0.5f
            drawRect(
                color    = Yellow.copy(wave * 0.6f + 0.15f),
                topLeft  = Offset(x, h * (1f - wave * 0.8f)),
                size     = Size(w / segments - 1f, h * wave * 0.8f)
            )
        }
    }
}

@Composable
fun CorridorCanvas(pan: Float, flicker: Float, modifier: Modifier) {
    androidx.compose.foundation.Canvas(modifier) {
        val w = size.width; val h = size.height
        val cx = w / 2f;   val cy = h / 2f
        drawRect(Color(0xFF1A1508))
        for (i in 0..7) {
            val t   = (i / 7f + pan * 0.12f) % 1f
            val per = 1f - t * 0.94f
            val ww  = w * per; val hh = h * per
            val lx  = cx - ww / 2f; val ty = cy - hh / 2f
            val al  = (1f - t * 0.85f) * 0.55f
            drawRect(Yellow.copy(al * 0.09f), Offset(lx, ty), Size(ww, hh), style = Stroke(1.5f))
            val lightY = ty + hh * 0.04f; val lightW = ww * 0.28f; val lf = flicker * al
            drawRect(
                Brush.radialGradient(
                    listOf(Color(0xFFEEDD88).copy(lf * 0.95f), Yellow.copy(lf * 0.3f), Color.Transparent),
                    Offset(cx, lightY), lightW
                ),
                Offset(cx - lightW / 2f, lightY - 18f), Size(lightW, 36f)
            )
            val floorY = ty + hh
            drawRect(
                Brush.verticalGradient(listOf(Color(0xFF3A2A10).copy(al * 0.4f), Color.Transparent), floorY, floorY + hh * 0.08f),
                Offset(lx, floorY), Size(ww, hh * 0.08f)
            )
            drawRect(Color(0xFFD4A84B).copy(al * (sin(i * 7.3f + pan * 13.1f) * 0.15f + 0.85f) * 0.04f), Offset(lx, ty), Size(ww, hh))
        }
        drawRect(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(0.75f)), Offset(cx, cy), w * 0.76f))
    }
}

@Composable
fun CarpetProgressBar(progress: Float, modifier: Modifier) {
    androidx.compose.foundation.Canvas(modifier) {
        val w = size.width; val h = size.height
        drawRoundRect(Color(0xFF1A1208), cornerRadius = CornerRadius(h / 2f))
        if (progress > 0f) {
            val fw = w * progress
            drawRoundRect(
                Brush.horizontalGradient(listOf(Color(0xFF3D2B10), Color(0xFF7A5A18), Color(0xFFD4A84B), Color(0xFF9A7228)), 0f, fw),
                size = Size(fw, h), cornerRadius = CornerRadius(h / 2f)
            )
            val sc = (fw / (h * 1.6f)).toInt()
            repeat(sc) { j ->
                val sx = j * h * 1.6f
                if (sx < fw) drawLine(Color(0xFF2A1A08).copy(0.45f), Offset(sx, 0f), Offset(sx + h * 0.6f, h), 2f)
            }
            drawRect(Brush.verticalGradient(listOf(Color.White.copy(0.18f), Color.Transparent)), Offset(0f, 0f), Size(fw, h / 2f))
        }
        drawRoundRect(Color(0xFF5A4020), cornerRadius = CornerRadius(h / 2f), style = Stroke(1f))
    }
}

// Menu → Backrooms.kt'ye taşındı

@Composable
private fun PlayerCard(profile: PlayerProfile) {
    OmniPanel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(46.dp).clip(CircleShape).background(MetalBg), Alignment.Center) {
                if (profile.avatarUrl != null) {
                    AsyncImage(profile.avatarUrl, null, Modifier.fillMaxSize())
                } else {
                    Text(profile.name.take(1).uppercase(), color = Yellow, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(profile.name, color = Yellow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.player_level_prefix) + profile.level, color = TextSec, fontSize = 11.sp)
                    if (profile.isVip) {
                        Box(Modifier.clip(RoundedCornerShape(1.dp)).background(Color(0xFFFFD700).copy(0.2f)).padding(horizontal = 4.dp, vertical = 1.dp)) {
                            Text("VIP", color = Color(0xFFFFD700), fontSize = 7.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                LinearProgressIndicator(
                    progress   = { profile.xpProgress },
                    modifier   = Modifier.width(88.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color      = Yellow, trackColor = MetalBg
                )
            }
        }
    }
}

@Composable
private fun QuickBtn(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(2.dp))
            .background(MetalBg.copy(0.75f))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(icon, null, tint = TextSec, modifier = Modifier.size(13.dp))
        Text(label, color = TextSec, fontSize = 10.sp, letterSpacing = 1.sp)
    }
}

@Composable
private fun CurrencyPanel(omnium: Long, soulium: Long, isVip: Boolean) {
    OmniPanel {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Icon(Icons.Default.Diamond,     null, modifier = Modifier.size(13.dp), tint = OmniumCol)
                Text(stringResource(R.string.currency_omnium),  color = OmniumCol,  fontSize = 9.sp,  letterSpacing = 1.sp)
                Spacer(Modifier.width(4.dp))
                Text(formatCurrency(omnium),  color = Yellow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(13.dp), tint = SouliumCol)
                Text(stringResource(R.string.currency_soulium), color = SouliumCol, fontSize = 9.sp,  letterSpacing = 1.sp)
                Spacer(Modifier.width(4.dp))
                Text(formatCurrency(soulium), color = Yellow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MenuButton(
    text   : String,
    icon   : androidx.compose.ui.graphics.vector.ImageVector,
    accent : Color,
    width  : androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val inf  = rememberInfiniteTransition(label = "mb")
    val glow by inf.animateFloat(0.5f, 1.0f,
        infiniteRepeatable(tween(2000, easing = EaseInOut), RepeatMode.Reverse), "g")
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .width(width).height(52.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Brush.horizontalGradient(listOf(MetalBg.copy(0.95f), accent.copy(0.08f))))
            .border(1.dp, Brush.horizontalGradient(listOf(accent.copy(0.15f), accent.copy(glow * 0.75f))), RoundedCornerShape(2.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp)
    ) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
        Text(text, color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
    }
}

private fun formatCurrency(amount: Long): String = when {
    amount >= 1_000_000 -> "${amount / 1_000_000}M"
    amount >= 1_000     -> "${amount / 1_000}K"
    else                -> amount.toString()
}

data class StoryUiState(
    val chapters    : List<StoryChapterDto> = emptyList(),
    val selected    : StoryChapterDto?      = null,
    val isLoading   : Boolean               = false,
    val readingMode : Boolean               = false
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
            val localChapters = loadFromAssets()
            if (localChapters.isNotEmpty()) {
                _state.update { it.copy(isLoading = false, chapters = localChapters) }
            }
            runCatching { api.getStoryChapters() }
                .onSuccess { remote ->
                    if (remote.isNotEmpty()) _state.update { it.copy(isLoading = false, chapters = remote) }
                }
                .onFailure {
                    if (_state.value.chapters.isEmpty())
                        _state.update { it.copy(isLoading = false) }
                }
        }
    }

    private fun loadFromAssets(): List<StoryChapterDto> =
        assetManager.loadStory().chapters.map { assetManager.storyChapterToDto(it) }

    fun select(ch: StoryChapterDto) {
        if (!ch.isUnlocked) return
        _state.update { it.copy(selected = ch, readingMode = true) }
    }

    fun exitReading() { _state.update { it.copy(readingMode = false, selected = null) } }
}

@Composable
fun Story(onBack: () -> Unit, vm: StoryVM = hiltViewModel()) {
    val s by vm.state.collectAsState()
    AnimatedContent(
        targetState = s.readingMode,
        transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(300)) },
        label = "story"
    ) { reading ->
        if (reading && s.selected != null) {
            BookReadingView(chapter = s.selected!!, onClose = vm::exitReading)
        } else {
            ChapterListView(state = s, onBack = onBack, onSelect = vm::select)
        }
    }
}

@Composable
private fun ChapterListView(
    state   : StoryUiState,
    onBack  : () -> Unit,
    onSelect: (StoryChapterDto) -> Unit
) {
    Box(Modifier.fillMaxSize().background(Color(0xFF050503))) {
        CrtOverlay()
        Column(Modifier.fillMaxSize()) {
            TopBarBack(stringResource(R.string.story_title), onBack)
            DividerLine()
            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Yellow, strokeWidth = 2.dp) }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding     = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement= Arrangement.spacedBy(10.dp)
                ) {
                    items(state.chapters, key = { it.id }) { ch ->
                        ChapterCard(chapter = ch, onClick = { onSelect(ch) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterCard(chapter: StoryChapterDto, onClick: () -> Unit) {
    val locked = !chapter.isUnlocked
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(3.dp))
            .background(MetalBg)
            .border(1.dp, if (locked) BorderCol else Yellow.copy(0.3f), RoundedCornerShape(3.dp))
            .clickable(enabled = !locked, onClick = onClick)
            .padding(14.dp),
        verticalAlignment     = Alignment.CenterVertically,
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
            Text(
                if (locked) "???" else chapter.titleTr,
                color = if (locked) TextDim else Yellow, fontSize = 14.sp, fontWeight = FontWeight.Bold
            )
            Text(
                if (locked) stringResource(R.string.story_chapter_locked)
                else chapter.contentTr.take(80) + "…",
                color = TextDim, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp
            )
        }
        Icon(
            if (locked) Icons.Default.Lock else Icons.Default.ArrowForward, null,
            tint = if (locked) TextDim else Yellow, modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun BookReadingView(chapter: StoryChapterDto, onClose: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xFF050503))) {
        CrtOverlay()
        Box(
            Modifier.align(Alignment.Center)
                .fillMaxWidth(0.82f)
                .fillMaxHeight(0.88f)
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
                Text(
                    chapter.titleTr, color = Color(0xFF8B6914), fontSize = 20.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 2.sp,
                    textAlign  = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                )
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF5A3A10).copy(0.5f)))
                chapter.contentTr.split("\n\n").forEach { para ->
                    if (para.startsWith("\"") || para.startsWith("—")) {
                        Text(para, color = Color(0xFF8A6A40), fontSize = 12.sp, fontStyle = FontStyle.Italic,
                            lineHeight = 20.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    } else {
                        Text(para, color = Color(0xFFC8A870), fontSize = 13.sp, lineHeight = 22.sp, textAlign = TextAlign.Justify)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("— Son —", color = Color(0xFF5A3A10), fontSize = 11.sp, letterSpacing = 3.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
