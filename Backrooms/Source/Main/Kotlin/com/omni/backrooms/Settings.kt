package com.omni.backrooms

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────
// Repository — was "Settings_Repository", renamed to SettingsRepository
// ─────────────────────────────────────────────────────────────
@Singleton
class SettingsRepository @Inject constructor(private val store: DataStore<Preferences>) {

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
        val KEY_LANGUAGE     = stringPreferencesKey("ui_language")
        val KEY_FPS_LIMIT    = intPreferencesKey("fps_limit")
        val KEY_SHADOWS      = booleanPreferencesKey("shadows_enabled")
        val KEY_ANTIALIASING = booleanPreferencesKey("antialiasing")
        val KEY_FOG          = booleanPreferencesKey("fog_enabled")
        val KEY_VIBRATION    = booleanPreferencesKey("vibration")
        val KEY_PUSH_NOTIF   = booleanPreferencesKey("push_notifications")
        val KEY_SHOW_FPS     = booleanPreferencesKey("show_fps")
        val KEY_SHOW_PING    = booleanPreferencesKey("show_ping")
        val KEY_COLOR_BLIND  = stringPreferencesKey("color_blind_mode")
    }

    fun observe(): Flow<GameSettings> = store.data.map { p ->
        GameSettings(
            playerName        = p[KEY_NAME]        ?: "Wanderer",
            graphicsQuality   = p[KEY_QUALITY]     ?: "medium",
            vhsEnabled        = p[KEY_VHS]         ?: true,
            resolutionScale   = p[KEY_RESOLUTION]  ?: 1f,
            musicVolume       = p[KEY_MUSIC]       ?: 0.7f,
            footstepVolume    = p[KEY_FOOTSTEP]    ?: 0.8f,
            monsterVolume     = p[KEY_MONSTER]     ?: 0.9f,
            voiceVolume       = p[KEY_VOICE]       ?: 0.8f,
            cameraSensitivity = p[KEY_SENSITIVITY] ?: 1f,
            fpsLimit          = p[KEY_FPS_LIMIT]   ?: 60,
            shadowsEnabled    = p[KEY_SHADOWS]     ?: true,
            antialiasingOn    = p[KEY_ANTIALIASING] ?: true,
            fogEnabled        = p[KEY_FOG]         ?: true,
            vibrationOn       = p[KEY_VIBRATION]   ?: true,
            showFps           = p[KEY_SHOW_FPS]    ?: false,
            showPing          = p[KEY_SHOW_PING]   ?: true,
            colorBlindMode    = p[KEY_COLOR_BLIND] ?: "none",
            pushNotifications = p[KEY_PUSH_NOTIF]  ?: true
        )
    }

    fun observeVhs()    : Flow<Boolean> = store.data.map { it[KEY_VHS]     ?: true      }
    fun observeMusic()  : Flow<Float>   = store.data.map { it[KEY_MUSIC]   ?: 0.7f      }
    fun observeVoice()  : Flow<Float>   = store.data.map { it[KEY_VOICE]   ?: 0.8f      }
    fun observeQuality(): Flow<String>  = store.data.map { it[KEY_QUALITY] ?: "medium"  }

    suspend fun saveName(v: String)          { store.edit { it[KEY_NAME]         = v } }
    suspend fun saveQuality(v: String)       { store.edit { it[KEY_QUALITY]      = v } }
    suspend fun saveVhs(v: Boolean)          { store.edit { it[KEY_VHS]          = v } }
    suspend fun saveResolution(v: Float)     { store.edit { it[KEY_RESOLUTION]   = v } }
    suspend fun saveMusic(v: Float)          { store.edit { it[KEY_MUSIC]        = v } }
    suspend fun saveFootstep(v: Float)       { store.edit { it[KEY_FOOTSTEP]     = v } }
    suspend fun saveMonster(v: Float)        { store.edit { it[KEY_MONSTER]      = v } }
    suspend fun saveVoice(v: Float)          { store.edit { it[KEY_VOICE]        = v } }
    suspend fun saveSensitivity(v: Float)    { store.edit { it[KEY_SENSITIVITY]  = v } }
    suspend fun saveFpsLimit(v: Int)         { store.edit { it[KEY_FPS_LIMIT]    = v } }
    suspend fun saveShadows(v: Boolean)      { store.edit { it[KEY_SHADOWS]      = v } }
    suspend fun saveAntialiasing(v: Boolean) { store.edit { it[KEY_ANTIALIASING] = v } }
    suspend fun saveFog(v: Boolean)          { store.edit { it[KEY_FOG]          = v } }
    suspend fun saveVibration(v: Boolean)    { store.edit { it[KEY_VIBRATION]    = v } }
    suspend fun saveShowFps(v: Boolean)      { store.edit { it[KEY_SHOW_FPS]     = v } }
    suspend fun saveShowPing(v: Boolean)     { store.edit { it[KEY_SHOW_PING]    = v } }
    suspend fun saveColorBlind(v: String)    { store.edit { it[KEY_COLOR_BLIND]  = v } }
    suspend fun savePushNotif(v: Boolean)    { store.edit { it[KEY_PUSH_NOTIF]   = v } }

    suspend fun saveUiLayout(layout: List<UiButtonLayout>) {
        store.edit { p ->
            layout.forEach { b ->
                p[floatPreferencesKey("ui_${b.buttonId}_x")] = b.offset.x
                p[floatPreferencesKey("ui_${b.buttonId}_y")] = b.offset.y
            }
        }
    }

    suspend fun loadUiLayout(): List<UiButtonLayout> {
        val p   = store.data.first()
        val ids = listOf("joystick","sprint","interact","crouch","flashlight","crouch2","map_btn")
        return ids.mapNotNull { id ->
            val x = p[floatPreferencesKey("ui_${id}_x")] ?: return@mapNotNull null
            val y = p[floatPreferencesKey("ui_${id}_y")] ?: return@mapNotNull null
            UiButtonLayout(buttonId = id, offset = Offset(x, y))
        }
    }

    suspend fun clearAll()      { store.edit { it.clear() } }
    suspend fun connectGoogle() { /* OAuth flow */ }
}

// ─────────────────────────────────────────────────────────────
// UiState
// ─────────────────────────────────────────────────────────────
data class SettingsUiState(
    val playerName        : String  = "Wanderer",
    val graphicsQuality   : String  = "medium",
    val vhsEnabled        : Boolean = true,
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
    val showPing          : Boolean = true,
    val colorBlindMode    : String  = "none",
    val pushNotifications : Boolean = true
)

// ─────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────
@HiltViewModel
class SettingsVM @Inject constructor(private val repo: SettingsRepository) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.observe().collect { g ->
                _state.update {
                    it.copy(
                        playerName        = g.playerName,
                        graphicsQuality   = g.graphicsQuality,
                        vhsEnabled        = g.vhsEnabled,
                        resolutionScale   = g.resolutionScale,
                        musicVolume       = g.musicVolume,
                        footstepVolume    = g.footstepVolume,
                        monsterVolume     = g.monsterVolume,
                        voiceVolume       = g.voiceVolume,
                        cameraSensitivity = g.cameraSensitivity,
                        fpsLimit          = g.fpsLimit,
                        shadowsEnabled    = g.shadowsEnabled,
                        antialiasingOn    = g.antialiasingOn,
                        fogEnabled        = g.fogEnabled,
                        vibrationOn       = g.vibrationOn,
                        showFps           = g.showFps,
                        showPing          = g.showPing,
                        colorBlindMode    = g.colorBlindMode,
                        pushNotifications = g.pushNotifications
                    )
                }
            }
        }
    }

    fun onName(v: String)          { _state.update { it.copy(playerName        = v) }; viewModelScope.launch { repo.saveName(v) } }
    fun onQuality(v: String)       { _state.update { it.copy(graphicsQuality   = v) }; viewModelScope.launch { repo.saveQuality(v) } }
    fun onVhs(v: Boolean)          { _state.update { it.copy(vhsEnabled        = v) }; viewModelScope.launch { repo.saveVhs(v) } }
    fun onResolution(v: Float)     { _state.update { it.copy(resolutionScale   = v) }; viewModelScope.launch { repo.saveResolution(v) } }
    fun onMusic(v: Float)          { _state.update { it.copy(musicVolume       = v) }; viewModelScope.launch { repo.saveMusic(v) } }
    fun onFootstep(v: Float)       { _state.update { it.copy(footstepVolume    = v) }; viewModelScope.launch { repo.saveFootstep(v) } }
    fun onMonster(v: Float)        { _state.update { it.copy(monsterVolume     = v) }; viewModelScope.launch { repo.saveMonster(v) } }
    fun onVoice(v: Float)          { _state.update { it.copy(voiceVolume       = v) }; viewModelScope.launch { repo.saveVoice(v) } }
    fun onSensitivity(v: Float)    { _state.update { it.copy(cameraSensitivity = v) }; viewModelScope.launch { repo.saveSensitivity(v) } }
    fun onFpsLimit(v: Int)         { _state.update { it.copy(fpsLimit          = v) }; viewModelScope.launch { repo.saveFpsLimit(v) } }
    fun onShadows(v: Boolean)      { _state.update { it.copy(shadowsEnabled    = v) }; viewModelScope.launch { repo.saveShadows(v) } }
    fun onAntialiasing(v: Boolean) { _state.update { it.copy(antialiasingOn    = v) }; viewModelScope.launch { repo.saveAntialiasing(v) } }
    fun onFog(v: Boolean)          { _state.update { it.copy(fogEnabled        = v) }; viewModelScope.launch { repo.saveFog(v) } }
    fun onVibration(v: Boolean)    { _state.update { it.copy(vibrationOn       = v) }; viewModelScope.launch { repo.saveVibration(v) } }
    fun onShowFps(v: Boolean)      { _state.update { it.copy(showFps           = v) }; viewModelScope.launch { repo.saveShowFps(v) } }
    fun onShowPing(v: Boolean)     { _state.update { it.copy(showPing          = v) }; viewModelScope.launch { repo.saveShowPing(v) } }
    fun onColorBlind(v: String)    { _state.update { it.copy(colorBlindMode    = v) }; viewModelScope.launch { repo.saveColorBlind(v) } }
    fun onPushNotif(v: Boolean)    { _state.update { it.copy(pushNotifications = v) }; viewModelScope.launch { repo.savePushNotif(v) } }
    fun onGoogleConnect()          { viewModelScope.launch { repo.connectGoogle() } }
}

// ─────────────────────────────────────────────────────────────
// Settings Screen
// ─────────────────────────────────────────────────────────────
private enum class STab(val labelRes: Int, val icon: ImageVector) {
    Account (R.string.settings_tab_account,  Icons.Default.Person),
    Graphics(R.string.settings_tab_graphics, Icons.Default.Tune),
    Audio   (R.string.settings_tab_audio,    Icons.Default.VolumeUp),
    Controls(R.string.settings_tab_controls, Icons.Default.SportsEsports),
    Gameplay(R.string.settings_tab_gameplay, Icons.Default.VideogameAsset),
    Notif   (R.string.settings_tab_audio,    Icons.Default.Notifications)
}

@Composable
fun Settings(onUiEditor: () -> Unit, onBack: () -> Unit, vm: SettingsVM = hiltViewModel()) {
    val s   by vm.state.collectAsState()
    var tab by remember { mutableStateOf(STab.Account) }

    Box(Modifier.fillMaxSize().background(DarkBg)) {
        CrtOverlay()
        Column(Modifier.fillMaxSize()) {
            TopBarBack(stringResource(R.string.settings_title), onBack)
            DividerLine()
            Row(
                Modifier.fillMaxWidth().background(MetalBg.copy(0.5f))
                    .horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                STab.entries.forEach { t ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                            .background(if (tab == t) PanelBg else Color.Transparent)
                            .clickable { tab = t }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(t.icon, null, tint = if (tab == t) Yellow else TextDim, modifier = Modifier.size(18.dp))
                        Text(stringResource(t.labelRes), color = if (tab == t) Yellow else TextDim, fontSize = 10.sp,
                            fontWeight = if (tab == t) FontWeight.Bold else FontWeight.Normal)
                        if (tab == t) Box(Modifier.width(24.dp).height(2.dp).background(Yellow))
                    }
                }
            }
            DividerLine()
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (tab) {
                    STab.Account  -> AccountTab(s, vm::onName, vm::onGoogleConnect)
                    STab.Graphics -> GraphicsTab(s, vm::onQuality, vm::onVhs, vm::onResolution, vm::onShadows, vm::onAntialiasing, vm::onFog, vm::onShowFps, vm::onShowPing)
                    STab.Audio    -> AudioTab(s, vm::onMusic, vm::onFootstep, vm::onMonster, vm::onVoice, vm::onVibration)
                    STab.Controls -> ControlsTab(s, vm::onSensitivity, onUiEditor)
                    STab.Gameplay -> GameplayTab(s, vm::onColorBlind)
                    STab.Notif    -> NotifTab(s, vm::onPushNotif)
                }
            }
        }
    }
}

@Composable
private fun AccountTab(s: SettingsUiState, onName: (String) -> Unit, onGoogle: () -> Unit) {
    SLabel(stringResource(R.string.account_change_name))
    OmniPanel(Modifier.fillMaxWidth()) {
        androidx.compose.foundation.text.BasicTextField(
            value = s.playerName, onValueChange = onName, singleLine = true,
            textStyle    = TextStyle(color = Yellow, fontSize = 13.sp),
            cursorBrush  = SolidColor(Yellow),
            decorationBox = { inner ->
                if (s.playerName.isEmpty()) Text(stringResource(R.string.account_name_hint), color = TextDim, fontSize = 12.sp)
                inner()
            }
        )
    }
    Spacer(Modifier.height(4.dp))
    OmniButton(stringResource(R.string.account_google_connect), onGoogle, width = 240.dp, height = 44.dp, accent = Color(0xFF4285F4))
}

@Composable
private fun GraphicsTab(
    s         : SettingsUiState,
    onQuality : (String)  -> Unit, onVhs    : (Boolean) -> Unit, onResolution: (Float)  -> Unit,
    onShadows : (Boolean) -> Unit, onAA     : (Boolean) -> Unit, onFog       : (Boolean) -> Unit,
    onFps     : (Boolean) -> Unit, onPing   : (Boolean) -> Unit
) {
    SLabel(stringResource(R.string.graphics_quality_label))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            Triple(R.string.graphics_quality_low,    "low",    SuccessGreen),
            Triple(R.string.graphics_quality_medium, "medium", Yellow),
            Triple(R.string.graphics_quality_high,   "high",   DangerRed)
        ).forEach { (res, key, col) ->
            val sel = s.graphicsQuality == key
            Box(contentAlignment = Alignment.Center,
                modifier = Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(2.dp))
                    .background(if (sel) col.copy(0.15f) else MetalBg)
                    .border(1.dp, if (sel) col.copy(0.6f) else BorderCol, RoundedCornerShape(2.dp))
                    .clickable { onQuality(key) }
            ) { Text(stringResource(res), color = if (sel) col else TextDim, fontSize = 11.sp,
                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal) }
        }
    }
    SToggle(stringResource(R.string.graphics_vhs_effect), s.vhsEnabled,    onVhs)
    SToggle("Gölgeler",      s.shadowsEnabled, onShadows)
    SToggle("Anti-Aliasing", s.antialiasingOn, onAA)
    SToggle("Sis Efekti",    s.fogEnabled,     onFog)
    SToggle("FPS Göster",    s.showFps,        onFps)
    SToggle("Ping Göster",   s.showPing,       onPing)
    SSlider(stringResource(R.string.graphics_resolution_label), s.resolutionScale, onResolution, 0.5f..1f)
}

@Composable
private fun AudioTab(
    s        : SettingsUiState,
    onMusic  : (Float) -> Unit, onFoot  : (Float) -> Unit, onMonster: (Float) -> Unit,
    onVoice  : (Float) -> Unit, onVib   : (Boolean) -> Unit
) {
    SSlider(stringResource(R.string.audio_music_volume),       s.musicVolume,    onMusic)
    SSlider(stringResource(R.string.audio_footstep_volume),    s.footstepVolume, onFoot)
    SSlider(stringResource(R.string.audio_monster_sfx_volume), s.monsterVolume,  onMonster)
    SSlider(stringResource(R.string.audio_voice_volume),       s.voiceVolume,    onVoice)
    SToggle("Titreşim", s.vibrationOn, onVib)
}

@Composable
private fun ControlsTab(s: SettingsUiState, onSensitivity: (Float) -> Unit, onUiEditor: () -> Unit) {
    SSlider(stringResource(R.string.controls_camera_sensitivity), s.cameraSensitivity, onSensitivity, 0.1f..3f)
    Spacer(Modifier.height(8.dp))
    OmniButton(stringResource(R.string.controls_ui_layout), onUiEditor, width = 240.dp, height = 48.dp)
}

@Composable
private fun GameplayTab(s: SettingsUiState, onColorBlind: (String) -> Unit) {
    SLabel("Renk Körlüğü Modu")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("none" to "Yok", "deuteranopia" to "D.", "protanopia" to "P.", "tritanopia" to "T.")
            .forEach { (key, label) ->
                val sel = s.colorBlindMode == key
                Box(contentAlignment = Alignment.Center,
                    modifier = Modifier.clip(RoundedCornerShape(2.dp))
                        .background(if (sel) Yellow.copy(0.15f) else MetalBg)
                        .border(1.dp, if (sel) Yellow.copy(0.6f) else BorderCol, RoundedCornerShape(2.dp))
                        .clickable { onColorBlind(key) }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) { Text(label, color = if (sel) Yellow else TextDim, fontSize = 10.sp) }
            }
    }
}

@Composable
private fun NotifTab(s: SettingsUiState, onPush: (Boolean) -> Unit) {
    SToggle("Bildirimler", s.pushNotifications, onPush)
}

@Composable
private fun SLabel(text: String) {
    Text(text, color = TextSec, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun SToggle(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(label, color = TextSec, fontSize = 12.sp)
        Switch(checked, onToggle, colors = SwitchDefaults.colors(
            checkedThumbColor   = Yellow,  checkedTrackColor   = YellowDim,
            uncheckedThumbColor = TextDim, uncheckedTrackColor = MetalBg
        ))
    }
}

@Composable
private fun SSlider(
    label  : String,
    value  : Float,
    onValue: (Float) -> Unit,
    range  : ClosedFloatingPointRange<Float> = 0f..1f
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text(label, color = TextSec, fontSize = 12.sp)
            Text("${(value * 100).toInt()}%", color = Yellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Slider(value, onValue, valueRange = range,
            colors = SliderDefaults.colors(thumbColor = Yellow, activeTrackColor = Yellow, inactiveTrackColor = MetalBg))
    }
}

// ─────────────────────────────────────────────────────────────
// UI Editor
// ─────────────────────────────────────────────────────────────
private data class DragBtn(val id: String, val labelRes: Int, var ox: Float, var oy: Float)

@HiltViewModel
class UiEditorVM @Inject constructor(private val repo: SettingsRepository) : ViewModel() {
    fun saveLayout(layout: List<UiButtonLayout>) {
        viewModelScope.launch { repo.saveUiLayout(layout) }
    }
}

@Composable
fun UiEditor(onSave: () -> Unit, vm: UiEditorVM = hiltViewModel()) {
    val buttons = remember {
        mutableStateListOf(
            DragBtn("joystick",   R.string.editor_btn_move,       80f,   400f),
            DragBtn("sprint",     R.string.editor_btn_sprint,     300f,  460f),
            DragBtn("interact",   R.string.editor_btn_interact,   900f,  400f),
            DragBtn("crouch",     R.string.editor_btn_crouch,     1000f, 460f),
            DragBtn("flashlight", R.string.editor_btn_flashlight, 1100f, 400f),
            DragBtn("map_btn",    R.string.maps_title,            50f,   50f)
        )
    }
    Box(Modifier.fillMaxSize().background(DarkBg.copy(0.92f))) {
        CrtOverlay()
        Text(
            stringResource(R.string.controls_ui_layout).uppercase(),
            color = TextDim, fontSize = 10.sp, letterSpacing = 3.sp,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 20.dp)
        )
        buttons.forEachIndexed { index, btn ->
            var ox by remember { mutableFloatStateOf(btn.ox) }
            var oy by remember { mutableFloatStateOf(btn.oy) }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset { IntOffset(ox.roundToInt(), oy.roundToInt()) }
                    .size(80.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MetalBg.copy(0.88f))
                    .border(1.dp, YellowDim, RoundedCornerShape(4.dp))
                    .pointerInput(Unit) {
                        detectDragGestures { ch, drag ->
                            ch.consume()
                            ox += drag.x; oy += drag.y
                            buttons[index] = btn.copy(ox = ox, oy = oy)
                        }
                    }
            ) {
                Text(stringResource(btn.labelRes), color = Yellow, fontSize = 9.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
        OmniButton(
            text    = stringResource(R.string.controls_save_exit),
            onClick = {
                vm.saveLayout(buttons.map { UiButtonLayout(buttonId = it.id, offset = Offset(it.ox, it.oy)) })
                onSave()
            },
            width    = 200.dp, height = 48.dp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)
        )
    }
}
