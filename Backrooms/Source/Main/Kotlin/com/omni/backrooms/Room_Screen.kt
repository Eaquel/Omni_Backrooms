package com.omni.backrooms

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MenuUiState(
    val playerName   : String  = "Wanderer",
    val playerLevel  : Int     = 1,
    val xpProgress   : Float   = 0f,
    val avatarUrl    : String? = null,
    val omniumAmount : Long    = 0L,
    val souliumAmount: Long    = 0L
)

@HiltViewModel
class MenuVM @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow(MenuUiState())
    val state: StateFlow<MenuUiState> = _state.asStateFlow()
}

@Composable
fun Menu_Screen(
    onNewGame  : () -> Unit,
    onEvents   : () -> Unit,
    onSettings : () -> Unit,
    onMarket   : () -> Unit,
    onStory    : () -> Unit,
    onCredits  : () -> Unit,
    onChangelog: () -> Unit,
    vm: MenuVM = hiltViewModel()
) {
    val s by vm.state.collectAsState()
    Box(Modifier.fillMaxSize().background(Color(0xFF080806))) {
        Crt_Overlay()
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().height(72.dp).background(Color.Black.copy(0.65f)).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Omni_Panel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).clip(CircleShape).background(MetalBg)) {
                            if (s.avatarUrl != null)
                                AsyncImage(s.avatarUrl, null, Modifier.fillMaxSize())
                            else
                                Icon(Icons.Default.Person, null, tint = TextDim,
                                    modifier = Modifier.fillMaxSize().padding(8.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(s.playerName, color = Yellow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.player_level_prefix) + s.playerLevel, color = TextSec, fontSize = 11.sp)
                            LinearProgressIndicator(
                                progress = { s.xpProgress },
                                modifier = Modifier.width(80.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
                                color = Yellow, trackColor = MetalBg
                            )
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    listOf(
                        Triple(Icons.Default.MenuBook, R.string.menu_story,    onStory),
                        Triple(Icons.Default.Groups,   R.string.menu_credits,  onCredits),
                        Triple(Icons.Default.List,     R.string.menu_changelog,onChangelog)
                    ).forEach { (icon, res, click) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clip(RoundedCornerShape(2.dp)).background(MetalBg.copy(0.7f))
                                .padding(horizontal = 8.dp, vertical = 3.dp).clickable(onClick = click)
                        ) {
                            Icon(icon, null, tint = TextSec, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(res), color = TextSec, fontSize = 10.sp, letterSpacing = 1.sp)
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Omni_Panel {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Diamond, null, tint = OmniumCol, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.currency_omnium), color = OmniumCol, fontSize = 10.sp, letterSpacing = 1.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(s.omniumAmount.toString(), color = Yellow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, null, tint = SouliumCol, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.currency_soulium), color = SouliumCol, fontSize = 10.sp, letterSpacing = 1.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(s.souliumAmount.toString(), color = Yellow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Box(Modifier.fillMaxSize()) {
                Column(
                    Modifier.align(Alignment.CenterEnd).padding(end = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Omni_Button(stringResource(R.string.menu_new_game), onNewGame,  width = 200.dp, height = 52.dp)
                    Omni_Button(stringResource(R.string.menu_events),   onEvents,   width = 200.dp, height = 52.dp)
                    Omni_Button(stringResource(R.string.menu_settings), onSettings, width = 200.dp, height = 52.dp)
                }
            }
        }
    }
}

@Composable
fun Mode_Screen(onOffline: () -> Unit, onOnline: () -> Unit, onBack: () -> Unit) {
    var v by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { v = true }
    Box(Modifier.fillMaxSize().background(Color(0xFF080806))) {
        Crt_Overlay()
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Yellow)
        }
        AnimatedVisibility(v, enter = fadeIn(tween(350)) + scaleIn(tween(350), initialScale = 0.93f),
            modifier = Modifier.align(Alignment.Center)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(stringResource(R.string.menu_new_game), color = TextSec, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                Divider_Line(Modifier.width(280.dp))
                Spacer(Modifier.height(8.dp))
                Omni_Button(stringResource(R.string.mode_offline), onOffline, width = 260.dp, height = 64.dp)
                Omni_Button(stringResource(R.string.mode_online),  onOnline,  width = 260.dp, height = 64.dp, accent = CrtAmber)
            }
        }
    }
}

@Composable
fun Difficulty_Screen(onSelect: (String) -> Unit, onBack: () -> Unit) {
    var selected by remember { mutableStateOf<String?>(null) }
    Box(Modifier.fillMaxSize().background(Color(0xFF080806))) {
        Crt_Overlay()
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Yellow)
        }
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.difficulty_title), color = TextSec, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
            Divider_Line(Modifier.width(320.dp))
            listOf(
                Triple(R.string.difficulty_easy,   "easy",   Color(0xFF4CAF50)),
                Triple(R.string.difficulty_normal, "normal", Yellow),
                Triple(R.string.difficulty_hard,   "hard",   DangerRed)
            ).forEach { (res, key, col) ->
                val sel = selected == key
                Box(
                    Alignment.Center,
                    Modifier.width(280.dp).height(56.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (sel) col.copy(0.12f) else MetalBg.copy(0.8f))
                        .border(1.dp, col.copy(if (sel) 1f else 0.3f), RoundedCornerShape(2.dp))
                        .clickable { selected = key; onSelect(key) }
                ) { Text(stringResource(res), color = if (sel) col else TextSec, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp) }
            }
        }
    }
}

@Composable
fun Online_Screen(onJoin: () -> Unit, onCreate: () -> Unit, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xFF080806))) {
        Crt_Overlay()
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Yellow)
        }
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(stringResource(R.string.mode_online), color = TextSec, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
            Divider_Line(Modifier.width(280.dp))
            Spacer(Modifier.height(8.dp))
            Omni_Button(stringResource(R.string.online_join_room),   onJoin,   width = 260.dp, height = 60.dp)
            Omni_Button(stringResource(R.string.online_create_room), onCreate, width = 260.dp, height = 60.dp, accent = CrtAmber)
        }
    }
}

data class RoomListState(
    val rooms         : List<RoomInfo> = emptyList(),
    val query         : String         = "",
    val filterLocked  : Boolean?       = null,
    val lang          : String?        = null,
    val page          : Int            = 0,
    val totalPages    : Int            = 1,
    val isLoading     : Boolean        = false
)

@OptIn(FlowPreview::class)
@HiltViewModel
class RoomListVM @Inject constructor(private val repo: Room_Repository) : ViewModel() {
    private val _s = MutableStateFlow(RoomListState())
    val state: StateFlow<RoomListState> = _s.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            _s.map { Triple(it.query, it.filterLocked, it.lang) }.debounce(300).distinctUntilChanged().collect { load() }
        }
    }

    fun onQuery(q: String)          { _s.update { it.copy(query = q, page = 0) } }
    fun onLocked(l: Boolean?)       { _s.update { it.copy(filterLocked = l, page = 0) } }
    fun onLang(l: String?)          { _s.update { it.copy(lang = l, page = 0) } }
    fun prev()                      { if (_s.value.page > 0) { _s.update { it.copy(page = it.page - 1) }; load() } }
    fun next()                      { val s = _s.value; if (s.page < s.totalPages - 1) { _s.update { it.copy(page = it.page + 1) }; load() } }

    private fun load() {
        viewModelScope.launch {
            _s.update { it.copy(isLoading = true) }
            runCatching {
                repo.fetchRooms(_s.value.query, _s.value.filterLocked, _s.value.lang, _s.value.page, 20)
            }.onSuccess { r -> _s.update { it.copy(isLoading = false, rooms = r.rooms, totalPages = maxOf(1,(r.total+19)/20)) } }
             .onFailure { _s.update { it.copy(isLoading = false) } }
        }
    }
}

@Composable
fun Room_Screen(onJoined: (String) -> Unit, onBack: () -> Unit, vm: RoomListVM = hiltViewModel()) {
    val s by vm.state.collectAsState()
    Box(Modifier.fillMaxSize().background(Color(0xFF050503))) {
        Crt_Overlay()
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxWidth().background(Color.Black.copy(0.6f)).padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Yellow) }
                    Text(stringResource(R.string.room_list_title), color = Yellow, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.weight(1f))
                    Row(Modifier.width(200.dp).clip(RoundedCornerShape(2.dp)).background(MetalBg).border(1.dp, BorderCol, RoundedCornerShape(2.dp)).padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, null, tint = TextDim, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        BasicTextField(s.query, onValueChange = vm::onQuery, singleLine = true,
                            textStyle = TextStyle(color = Yellow, fontSize = 12.sp),
                            cursorBrush = SolidColor(Yellow),
                            decorationBox = { inner -> if (s.query.isEmpty()) Text(stringResource(R.string.room_search_hint), color = TextDim, fontSize = 12.sp); inner() })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf(false to R.string.room_filter_unlocked, true to R.string.room_filter_locked).forEach { (lock, res) ->
                        val sel = s.filterLocked == lock
                        Box(Alignment.Center, Modifier.clip(RoundedCornerShape(2.dp)).background(if (sel) Yellow.copy(0.15f) else MetalBg.copy(0.5f)).border(1.dp, if (sel) Yellow.copy(0.6f) else BorderCol, RoundedCornerShape(2.dp)).clickable { vm.onLocked(if (sel) null else lock) }.padding(horizontal = 10.dp, vertical = 4.dp)) {
                            Text(stringResource(res), color = if (sel) Yellow else TextDim, fontSize = 10.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal, letterSpacing = 1.sp)
                        }
                    }
                    listOf("TR","EN","DE").forEach { lang ->
                        val sel = s.lang == lang
                        Box(Alignment.Center, Modifier.clip(RoundedCornerShape(2.dp)).background(if (sel) Yellow.copy(0.15f) else MetalBg.copy(0.5f)).border(1.dp, if (sel) Yellow.copy(0.6f) else BorderCol, RoundedCornerShape(2.dp)).clickable { vm.onLang(if (sel) null else lang) }.padding(horizontal = 10.dp, vertical = 4.dp)) {
                            Text(lang, color = if (sel) Yellow else TextDim, fontSize = 10.sp, letterSpacing = 1.sp)
                        }
                    }
                }
            }
            Divider_Line()
            if (s.isLoading) {
                Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) { CircularProgressIndicator(color = Yellow, strokeWidth = 2.dp) }
            } else {
                LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(s.rooms, key = { it.id }) { room ->
                        Row(
                            Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(2.dp)).background(MetalBg.copy(0.7f)).border(1.dp, BorderCol, RoundedCornerShape(2.dp)).clickable { onJoined(room.id) }.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(if (room.isLocked) Icons.Default.Lock else Icons.Default.LockOpen, null, tint = if (room.isLocked) DangerRed.copy(0.7f) else Color(0xFF4CAF50).copy(0.7f), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(room.name, color = Yellow, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            Text(room.language, color = TextDim, fontSize = 10.sp, letterSpacing = 1.sp)
                            Spacer(Modifier.width(12.dp))
                            Text("${room.currentPlayers}/${room.maxPlayers}", color = TextSec, fontSize = 11.sp)
                            Spacer(Modifier.width(12.dp))
                            Text(room.difficulty.uppercase(), color = when (room.difficulty) { "easy" -> Color(0xFF4CAF50); "hard" -> DangerRed; else -> Yellow }, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    }
                    if (s.rooms.isEmpty()) item {
                        Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), Alignment.Center) {
                            Text(stringResource(R.string.room_list_empty), color = TextDim, fontSize = 13.sp, letterSpacing = 2.sp)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().background(Color.Black.copy(0.5f)).padding(horizontal = 16.dp, vertical = 8.dp), Arrangement.Center, Alignment.CenterVertically) {
                    Omni_Button(stringResource(R.string.room_page_prev), vm::prev, enabled = s.page > 0, width = 100.dp, height = 36.dp)
                    Spacer(Modifier.width(16.dp))
                    Text("${s.page + 1} / ${s.totalPages}", color = TextSec, fontSize = 12.sp, letterSpacing = 1.sp)
                    Spacer(Modifier.width(16.dp))
                    Omni_Button(stringResource(R.string.room_page_next), vm::next, enabled = s.page < s.totalPages - 1, width = 100.dp, height = 36.dp)
                }
            }
        }
    }
}

data class CreateRoomState(
    val name             : String  = "",
    val nameError        : Int?    = null,
    val size             : Int     = 2,
    val difficulty       : String  = "normal",
    val passwordEnabled  : Boolean = false,
    val password         : String  = "",
    val isCreating       : Boolean = false,
    val createdId        : String? = null
)

@HiltViewModel
class CreateRoomVM @Inject constructor(private val repo: Room_Repository) : ViewModel() {
    private val _s = MutableStateFlow(CreateRoomState())
    val state: StateFlow<CreateRoomState> = _s.asStateFlow()

    fun onName(n: String)        { _s.update { it.copy(name = n, nameError = validate(n)) } }
    fun onSize(v: Int)           { _s.update { it.copy(size = v.coerceIn(2, 4)) } }
    fun onDifficulty(d: String)  { _s.update { it.copy(difficulty = d) } }
    fun onPasswordToggle(e: Boolean) { _s.update { it.copy(passwordEnabled = e, password = if (!e) "" else it.password) } }
    fun onPassword(p: String)    { _s.update { it.copy(password = p) } }

    fun onCreate() {
        val s = _s.value
        val err = validate(s.name); if (err != null) { _s.update { it.copy(nameError = err) }; return }
        if (s.passwordEnabled && s.password.isBlank()) return
        viewModelScope.launch {
            _s.update { it.copy(isCreating = true) }
            runCatching { repo.createRoom(s.name, s.size, s.difficulty, if (s.passwordEnabled) s.password else null) }
                .onSuccess { id -> _s.update { it.copy(isCreating = false, createdId = id) } }
                .onFailure { _s.update { it.copy(isCreating = false) } }
        }
    }

    private fun validate(n: String): Int? {
        if (n.length < 4 || n.length > 12) return R.string.room_name_error_length
        if (!Regex("^[a-zA-Z0-9 _-]+$").matches(n)) return R.string.room_name_error_chars
        return null
    }
}

@Composable
fun Create_Room_Screen(onCreated: (String) -> Unit, onBack: () -> Unit, vm: CreateRoomVM = hiltViewModel()) {
    val s by vm.state.collectAsState()
    LaunchedEffect(s.createdId) { s.createdId?.let { onCreated(it) } }

    Box(Modifier.fillMaxSize().background(Color(0xFF050503))) {
        Crt_Overlay()
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().background(Color.Black.copy(0.6f)).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Yellow) }
                Text(stringResource(R.string.room_create_title), color = Yellow, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
            }
            Divider_Line()
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Omni_Panel(Modifier.widthIn(max = 440.dp).fillMaxWidth(0.85f)) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Omni_Field(s.name, vm::onName, stringResource(R.string.room_create_name_hint), s.nameError?.let { stringResource(it) })
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.room_create_size_label, s.size), color = TextSec, fontSize = 11.sp, letterSpacing = 1.sp)
                            Slider(s.size.toFloat(), { vm.onSize(it.toInt()) }, valueRange = 2f..4f, steps = 1,
                                colors = SliderDefaults.colors(thumbColor = Yellow, activeTrackColor = Yellow, inactiveTrackColor = MetalBg))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(Triple(R.string.difficulty_easy,"easy",Color(0xFF4CAF50)),Triple(R.string.difficulty_normal,"normal",Yellow),Triple(R.string.difficulty_hard,"hard",DangerRed)).forEach { (res,key,col) ->
                                val sel = s.difficulty == key
                                Box(Alignment.Center, Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(2.dp)).background(if (sel) col.copy(0.15f) else MetalBg.copy(0.5f)).border(1.dp, if (sel) col.copy(0.7f) else BorderCol, RoundedCornerShape(2.dp)).clickable { vm.onDifficulty(key) }) {
                                    Text(stringResource(res), color = if (sel) col else TextDim, fontSize = 11.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal, letterSpacing = 1.sp)
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Checkbox(s.passwordEnabled, vm::onPasswordToggle, colors = CheckboxDefaults.colors(checkedColor = Yellow, uncheckedColor = TextDim, checkmarkColor = Color.Black))
                            Icon(if (s.passwordEnabled) Icons.Default.Lock else Icons.Default.LockOpen, null, tint = if (s.passwordEnabled) Yellow else TextDim, modifier = Modifier.size(16.dp))
                            Text(stringResource(R.string.room_create_password_label), color = if (s.passwordEnabled) Yellow else TextDim, fontSize = 12.sp)
                        }
                        if (s.passwordEnabled) Omni_Field(s.password, vm::onPassword, stringResource(R.string.room_create_password_hint), isPassword = true)
                        Spacer(Modifier.height(4.dp))
                        Omni_Button(if (s.isCreating) "…" else stringResource(R.string.room_create_confirm),
                            vm::onCreate, enabled = !s.isCreating && s.nameError == null && s.name.isNotBlank(), width = 9999.dp, height = 50.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun Omni_Field(value: String, onChange: (String) -> Unit, hint: String, error: String? = null, isPassword: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(2.dp)).background(MetalBg).border(1.dp, if (error != null) DangerRed.copy(0.6f) else BorderCol, RoundedCornerShape(2.dp)).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(value, onChange, singleLine = true,
                visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = if (isPassword) KeyboardOptions(keyboardType = KeyboardType.Password) else KeyboardOptions.Default,
                textStyle = TextStyle(color = Yellow, fontSize = 13.sp, letterSpacing = 1.sp),
                cursorBrush = SolidColor(Yellow), modifier = Modifier.weight(1f),
                decorationBox = { inner -> if (value.isEmpty()) Text(hint, color = TextDim, fontSize = 12.sp); inner() })
        }
        if (error != null) Text(error, color = DangerRed, fontSize = 10.sp)
    }
}
