package com.omni.backrooms

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoomListUiState(
    val rooms        : List<RoomInfo> = emptyList(),
    val query        : String         = "",
    val filterLocked : Boolean?       = null,
    val lang         : String?        = null,
    val page         : Int            = 0,
    val totalPages   : Int            = 1,
    val isLoading    : Boolean        = false,
    val error        : String?        = null
)

data class CreateRoomUiState(
    val name            : String  = "",
    val nameError       : Int?    = null,
    val size            : Int     = 2,
    val difficulty      : String  = "normal",
    val passwordEnabled : Boolean = false,
    val password        : String  = "",
    val mapId           : String  = "level_0",
    val language        : String  = "TR",
    val isCreating      : Boolean = false,
    val createdRoomId   : String? = null,
    val error           : String? = null
)

data class RoomLobbyUiState(
    val detail      : RoomDetail?    = null,
    val isReady     : Boolean        = false,
    val allReady    : Boolean        = false,
    val countdown   : Int?           = null,
    val isLoading   : Boolean        = false,
    val peerPings   : Map<Int, Int>  = emptyMap()
)

@OptIn(FlowPreview::class)
@HiltViewModel
class RoomListVM @Inject constructor(private val repo: Room_Repository) : ViewModel() {

    private val _state = MutableStateFlow(RoomListUiState())
    val state: StateFlow<RoomListUiState> = _state.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            _state.map { Triple(it.query, it.filterLocked, it.lang) }
                .debounce(300).distinctUntilChanged().collect { load() }
        }
    }

    fun onQuery(q: String)     { _state.update { it.copy(query=q, page=0) } }
    fun onLocked(l: Boolean?)  { _state.update { it.copy(filterLocked=l, page=0) } }
    fun onLang(l: String?)     { _state.update { it.copy(lang=l, page=0) } }
    fun prev()                 { if (_state.value.page>0) { _state.update { it.copy(page=it.page-1) }; load() } }
    fun next()                 { val s=_state.value; if (s.page<s.totalPages-1) { _state.update { it.copy(page=it.page+1) }; load() } }

    private fun load() {
        viewModelScope.launch {
            val s = _state.value
            _state.update { it.copy(isLoading=true) }
            runCatching { repo.fetchRooms(s.query, s.filterLocked, s.lang, s.page, 20) }
                .onSuccess { r -> _state.update { it.copy(isLoading=false, rooms=r.rooms, totalPages=maxOf(1,(r.total+19)/20)) } }
                .onFailure { _state.update { it.copy(isLoading=false) } }
        }
    }
}

@HiltViewModel
class CreateRoomVM @Inject constructor(private val repo: Room_Repository) : ViewModel() {

    private val _state = MutableStateFlow(CreateRoomUiState())
    val state: StateFlow<CreateRoomUiState> = _state.asStateFlow()

    fun onName(n: String)           { _state.update { it.copy(name=n, nameError=validate(n)) } }
    fun onSize(v: Int)              { _state.update { it.copy(size=v.coerceIn(2,4)) } }
    fun onDifficulty(d: String)     { _state.update { it.copy(difficulty=d) } }
    fun onPasswordToggle(e: Boolean){ _state.update { it.copy(passwordEnabled=e, password=if(!e) "" else it.password) } }
    fun onPassword(p: String)       { _state.update { it.copy(password=p) } }
    fun onMapId(m: String)          { _state.update { it.copy(mapId=m) } }
    fun onLanguage(l: String)       { _state.update { it.copy(language=l) } }

    fun onCreate() {
        val s = _state.value
        val err = validate(s.name); if (err!=null) { _state.update { it.copy(nameError=err) }; return }
        if (s.passwordEnabled && s.password.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isCreating=true) }
            runCatching {
                repo.createRoom(s.name, s.size, s.difficulty, if(s.passwordEnabled) s.password else null)
            }.onSuccess { id -> _state.update { it.copy(isCreating=false, createdRoomId=id) } }
             .onFailure { e -> _state.update { it.copy(isCreating=false, error=e.message) } }
        }
    }

    private fun validate(n: String): Int? {
        if (n.length<4||n.length>12) return R.string.room_name_error_length
        if (!Regex("^[a-zA-Z0-9 _-]+\$").matches(n)) return R.string.room_name_error_chars
        return null
    }
}

@HiltViewModel
class RoomLobbyVM @Inject constructor(
    private val api   : Api_Service,
    private val bridge: Native_Bridge
) : ViewModel() {

    private val _state = MutableStateFlow(RoomLobbyUiState())
    val state: StateFlow<RoomLobbyUiState> = _state.asStateFlow()

    fun load(roomId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading=true) }
            runCatching { api.getRoomDetail(roomId) }
                .onSuccess { d -> _state.update { it.copy(isLoading=false, detail=d) } }
                .onFailure {    _state.update { it.copy(isLoading=false) } }
        }
        startPingLoop()
    }

    fun toggleReady() {
        _state.update { it.copy(isReady=!it.isReady) }
        checkAllReady()
    }

    private fun checkAllReady() {
        val d = _state.value.detail ?: return
        val othersReady = d.players.filter { !it.isHost }.all { it.isReady }
        if (_state.value.isReady && othersReady) startCountdown()
    }

    private fun startCountdown() {
        viewModelScope.launch {
            for (i in 5 downTo 0) { _state.update { it.copy(countdown=i) }; delay(1000) }
        }
    }

    private fun startPingLoop() {
        viewModelScope.launch {
            while (isActive) {
                val ping = bridge.getLocalPing()
                _state.update { it.copy(peerPings=it.peerPings + (0 to ping)) }
                delay(2000)
            }
        }
    }
}

@Composable
fun Room(onBack: () -> Unit, onEnterGame: () -> Unit, vm: RoomListVM = hiltViewModel()) {
    val s by vm.state.collectAsState()
    var showCreate by remember { mutableStateOf(false) }

    if (showCreate) {
        CreateRoom(onCreated = { onEnterGame() }, onBack = { showCreate = false })
        return
    }

    Box(Modifier.fillMaxSize().background(DarkBg)) {
        CrtOverlay()
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxWidth().background(Color.Black.copy(0.6f)).padding(horizontal=12.dp, vertical=8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick=onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint=Yellow) }
                    Text(stringResource(R.string.room_list_title), color=Yellow, fontSize=15.sp, fontWeight=FontWeight.Bold, letterSpacing=2.sp)
                    Spacer(Modifier.weight(1f))
                    SearchField(s.query, vm::onQuery, Modifier.width(200.dp))
                    OmniButton(stringResource(R.string.online_create_room), { showCreate=true }, width=110.dp, height=34.dp, accent=CrtAmber)
                }
                Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    FilterChip(stringResource(R.string.room_filter_unlocked), s.filterLocked==false) { vm.onLocked(if(s.filterLocked==false) null else false) }
                    FilterChip(stringResource(R.string.room_filter_locked),   s.filterLocked==true)  { vm.onLocked(if(s.filterLocked==true)  null else true)  }
                    listOf("TR","EN","DE","RU").forEach { lang ->
                        FilterChip(lang, s.lang==lang) { vm.onLang(if(s.lang==lang) null else lang) }
                    }
                }
            }
            DividerLine()

            if (s.isLoading) {
                Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                    CircularProgressIndicator(color=Yellow, strokeWidth=2.dp)
                }
            } else {
                LazyColumn(
                    Modifier.weight(1f).fillMaxWidth().padding(horizontal=12.dp),
                    verticalArrangement=Arrangement.spacedBy(4.dp),
                    contentPadding=PaddingValues(vertical=8.dp)
                ) {
                    items(s.rooms, key={it.id}) { room -> RoomRow(room=room, onClick={ onEnterGame() }) }
                    if (s.rooms.isEmpty()) item {
                        Box(Modifier.fillMaxWidth().padding(vertical=48.dp), Alignment.Center) {
                            Text(stringResource(R.string.room_list_empty), color=TextDim, fontSize=13.sp, letterSpacing=2.sp)
                        }
                    }
                }
                PagerBar(s.page, s.totalPages, vm::prev, vm::next)
            }
        }
    }
}

@Composable
fun BackroomsGame(onExit: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        CrtOverlay()
        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment=Alignment.CenterHorizontally,
            verticalArrangement=Arrangement.spacedBy(16.dp)
        ) {
            Text("BACKROOMS", color=Yellow, fontSize=24.sp, fontWeight=FontWeight.Black, letterSpacing=6.sp)
            Spacer(Modifier.height(24.dp))
            OmniButton("Çık", onExit, width=200.dp, height=48.dp, accent=DangerRed)
        }
    }
}

@Composable
fun CreateRoom(onCreated: (String) -> Unit, onBack: () -> Unit, vm: CreateRoomVM = hiltViewModel()) {
    val s by vm.state.collectAsState()
    LaunchedEffect(s.createdRoomId) { s.createdRoomId?.let { onCreated(it) } }

    Box(Modifier.fillMaxSize().background(DarkBg)) {
        CrtOverlay()
        Column(Modifier.fillMaxSize()) {
            TopBarBack(stringResource(R.string.room_create_title), onBack)
            DividerLine()
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                OmniPanel(Modifier.widthIn(max=480.dp).fillMaxWidth(0.9f)) {
                    Column(verticalArrangement=Arrangement.spacedBy(14.dp)) {

                        OmniTextField(s.name, vm::onName, stringResource(R.string.room_create_name_hint),
                            error=s.nameError?.let { stringResource(it) })

                        Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.room_create_size_label, s.size), color=TextSec, fontSize=11.sp)
                            Slider(s.size.toFloat(), { vm.onSize(it.toInt()) }, valueRange=2f..4f, steps=1,
                                colors=SliderDefaults.colors(thumbColor=Yellow, activeTrackColor=Yellow, inactiveTrackColor=MetalBg))
                        }

                        DifficultyRow(s.difficulty, vm::onDifficulty)
                        MapPicker(s.mapId, vm::onMapId)

                        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            listOf("TR","EN","DE","RU").forEach { l ->
                                val sel = s.language == l
                                Box(Alignment.Center,
                                    Modifier.weight(1f).height(34.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if(sel) Yellow.copy(0.15f) else MetalBg.copy(0.5f))
                                        .border(1.dp, if(sel) Yellow.copy(0.6f) else BorderCol, RoundedCornerShape(2.dp))
                                        .clickable { vm.onLanguage(l) }
                                ) { Text(l, color=if(sel) Yellow else TextDim, fontSize=11.sp) }
                            }
                        }

                        Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            Checkbox(s.passwordEnabled, vm::onPasswordToggle,
                                colors=CheckboxDefaults.colors(checkedColor=Yellow, uncheckedColor=TextDim, checkmarkColor=Color.Black))
                            Icon(if(s.passwordEnabled) Icons.Default.Lock else Icons.Default.LockOpen, null,
                                tint=if(s.passwordEnabled) Yellow else TextDim, modifier=Modifier.size(16.dp))
                            Text(stringResource(R.string.room_create_password_label),
                                color=if(s.passwordEnabled) Yellow else TextDim, fontSize=12.sp)
                        }

                        if (s.passwordEnabled) {
                            OmniTextField(s.password, vm::onPassword,
                                stringResource(R.string.room_create_password_hint), isPassword=true)
                        }

                        s.error?.let { Text(it, color=DangerRed, fontSize=11.sp) }

                        OmniButton(
                            text    = if(s.isCreating) "…" else stringResource(R.string.room_create_confirm),
                            onClick = vm::onCreate,
                            enabled = !s.isCreating && s.nameError==null && s.name.isNotBlank(),
                            width   = Double.MAX_VALUE.dp,
                            height  = 50.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OmniTextField(
    value      : String,
    onValue    : (String) -> Unit,
    hint       : String,
    modifier   : Modifier = Modifier,
    isPassword : Boolean  = false,
    error      : String?  = null
) {
    Column(modifier, verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(2.dp))
                .background(MetalBg)
                .border(1.dp, if(error!=null) DangerRed.copy(0.7f) else BorderCol, RoundedCornerShape(2.dp))
                .padding(horizontal=12.dp, vertical=10.dp),
            verticalAlignment=Alignment.CenterVertically
        ) {
            BasicTextField(
                value=value, onValueChange=onValue, singleLine=true,
                textStyle=TextStyle(color=Yellow, fontSize=13.sp),
                cursorBrush=SolidColor(Yellow),
                visualTransformation=if(isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions=if(isPassword) KeyboardOptions(keyboardType=KeyboardType.Password) else KeyboardOptions.Default,
                modifier=Modifier.weight(1f),
                decorationBox={ inner ->
                    if(value.isEmpty()) Text(hint, color=TextDim, fontSize=12.sp)
                    inner()
                }
            )
            if (isPassword) Icon(Icons.Default.Lock, null, tint=TextDim, modifier=Modifier.size(16.dp))
        }
        error?.let { Text(it, color=DangerRed, fontSize=10.sp) }
    }
}

@Composable
private fun RoomRow(room: RoomInfo, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(46.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(MetalBg.copy(0.7f))
            .border(1.dp, BorderCol, RoundedCornerShape(2.dp))
            .clickable(onClick=onClick)
            .padding(horizontal=12.dp),
        verticalAlignment=Alignment.CenterVertically
    ) {
        Icon(if(room.isLocked) Icons.Default.Lock else Icons.Default.LockOpen, null,
            tint=if(room.isLocked) DangerRed.copy(0.7f) else SuccessGreen.copy(0.7f), modifier=Modifier.size(14.dp))
        Spacer(Modifier.width(8.dp))
        Text(room.name, color=Yellow, fontSize=13.sp, fontWeight=FontWeight.Medium, modifier=Modifier.weight(1f), maxLines=1, overflow=TextOverflow.Ellipsis)
        Text(room.language, color=TextDim, fontSize=10.sp, letterSpacing=1.sp)
        Spacer(Modifier.width(10.dp))
        Text("${room.currentPlayers}/${room.maxPlayers}", color=TextSec, fontSize=11.sp)
        Spacer(Modifier.width(10.dp))
        Text(room.difficulty.uppercase(), fontSize=10.sp, fontWeight=FontWeight.Bold, letterSpacing=1.sp,
            color=when(room.difficulty) { "easy"->SuccessGreen; "hard"->DangerRed; else->Yellow })
        if (room.ping > 0) {
            Spacer(Modifier.width(8.dp))
            Text("${room.ping}ms", color=when { room.ping<60->SuccessGreen; room.ping<120->CrtAmber; else->DangerRed }, fontSize=9.sp)
        }
    }
}

@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit, modifier: Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(2.dp))
            .background(MetalBg)
            .border(1.dp, BorderCol, RoundedCornerShape(2.dp))
            .padding(horizontal=8.dp, vertical=6.dp),
        verticalAlignment=Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, null, tint=TextDim, modifier=Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        BasicTextField(query, onQuery, singleLine=true,
            textStyle=TextStyle(color=Yellow, fontSize=12.sp),
            cursorBrush=SolidColor(Yellow),
            decorationBox={ inner -> if(query.isEmpty()) Text(stringResource(R.string.room_search_hint), color=TextDim, fontSize=12.sp); inner() })
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(Alignment.Center,
        Modifier.clip(RoundedCornerShape(2.dp))
            .background(if(selected) Yellow.copy(0.15f) else MetalBg.copy(0.5f))
            .border(1.dp, if(selected) Yellow.copy(0.6f) else BorderCol, RoundedCornerShape(2.dp))
            .clickable(onClick=onClick)
            .padding(horizontal=10.dp, vertical=4.dp)
    ) {
        Text(label, color=if(selected) Yellow else TextDim, fontSize=10.sp,
            fontWeight=if(selected) FontWeight.Bold else FontWeight.Normal, letterSpacing=1.sp)
    }
}

@Composable
private fun DifficultyRow(selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        listOf(
            Triple(R.string.difficulty_easy,   "easy",   SuccessGreen),
            Triple(R.string.difficulty_normal, "normal", Yellow),
            Triple(R.string.difficulty_hard,   "hard",   DangerRed)
        ).forEach { (res,key,col) ->
            val sel = selected==key
            Box(Alignment.Center,
                Modifier.weight(1f).height(36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if(sel) col.copy(0.15f) else MetalBg.copy(0.5f))
                    .border(1.dp, if(sel) col.copy(0.7f) else BorderCol, RoundedCornerShape(2.dp))
                    .clickable { onSelect(key) }
            ) { Text(stringResource(res), color=if(sel) col else TextDim, fontSize=11.sp, fontWeight=if(sel) FontWeight.Bold else FontWeight.Normal) }
        }
    }
}

@Composable
private fun MapPicker(selected: String, onSelect: (String) -> Unit) {
    val maps = listOf("level_0" to "Level 0", "level_1" to "Level 1", "level_2" to "Level 2",
        "level_3" to "Level 3", "level_4" to "Level 4")
    Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Text("Harita", color=TextSec, fontSize=11.sp, letterSpacing=2.sp)
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp), modifier=Modifier.horizontalScroll(rememberScrollState())) {
            maps.forEach { (id,name) ->
                val sel = selected==id
                Box(Alignment.Center,
                    Modifier.clip(RoundedCornerShape(2.dp))
                        .background(if(sel) Yellow.copy(0.15f) else MetalBg.copy(0.5f))
                        .border(1.dp, if(sel) Yellow.copy(0.6f) else BorderCol, RoundedCornerShape(2.dp))
                        .clickable { onSelect(id) }
                        .padding(horizontal=10.dp, vertical=6.dp)
                ) { Text(name, color=if(sel) Yellow else TextDim, fontSize=11.sp, fontWeight=if(sel) FontWeight.Bold else FontWeight.Normal) }
            }
        }
    }
}

@Composable
private fun PagerBar(page: Int, total: Int, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Color.Black.copy(0.5f)).padding(horizontal=16.dp, vertical=8.dp),
        Arrangement.Center, Alignment.CenterVertically
    ) {
        OmniButton(stringResource(R.string.room_page_prev), onPrev, enabled=page>0, width=100.dp, height=36.dp)
        Spacer(Modifier.width(16.dp))
        Text("${page+1} / $total", color=TextSec, fontSize=12.sp, letterSpacing=1.sp)
        Spacer(Modifier.width(16.dp))
        OmniButton(stringResource(R.string.room_page_next), onNext, enabled=page<total-1, width=100.dp, height=36.dp)
    }
}
