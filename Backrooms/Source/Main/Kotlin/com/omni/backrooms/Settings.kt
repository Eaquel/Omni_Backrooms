package com.omni.backrooms

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

// ═════════════════════════════════════════════════════════════
//  SettingsRepository — DataStore + Native bridge + Firebase RC
// ═════════════════════════════════════════════════════════════
@Singleton
class SettingsRepository @Inject constructor(
    private val store : DataStore<Preferences>,
    private val bridge: NativeBridge
) {
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

    // ── Observe ───────────────────────────────────────────────
    fun observe(): Flow<GameSettings> = store.data.map { p ->
        GameSettings(
            playerName        = p[KEY_NAME]         ?: "Wanderer",
            graphicsQuality   = p[KEY_QUALITY]      ?: "medium",
            vhsEnabled        = p[KEY_VHS]          ?: true,
            resolutionScale   = p[KEY_RESOLUTION]   ?: 1f,
            musicVolume       = p[KEY_MUSIC]        ?: 0.7f,
            footstepVolume    = p[KEY_FOOTSTEP]     ?: 0.8f,
            monsterVolume     = p[KEY_MONSTER]      ?: 0.9f,
            voiceVolume       = p[KEY_VOICE]        ?: 0.8f,
            cameraSensitivity = p[KEY_SENSITIVITY]  ?: 1f,
            fpsLimit          = p[KEY_FPS_LIMIT]    ?: 60,
            shadowsEnabled    = p[KEY_SHADOWS]      ?: true,
            antialiasingOn    = p[KEY_ANTIALIASING] ?: true,
            fogEnabled        = p[KEY_FOG]          ?: true,
            vibrationOn       = p[KEY_VIBRATION]    ?: true,
            showFps           = p[KEY_SHOW_FPS]     ?: false,
            showPing          = p[KEY_SHOW_PING]    ?: true,
            colorBlindMode    = p[KEY_COLOR_BLIND]  ?: "none",
            pushNotifications = p[KEY_PUSH_NOTIF]   ?: true
        )
    }

    fun observeVhs()    : Flow<Boolean> = store.data.map { it[KEY_VHS]     ?: true     }
    fun observeMusic()  : Flow<Float>   = store.data.map { it[KEY_MUSIC]   ?: 0.7f     }
    fun observeVoice()  : Flow<Float>   = store.data.map { it[KEY_VOICE]   ?: 0.8f     }
    fun observeQuality(): Flow<String>  = store.data.map { it[KEY_QUALITY] ?: "medium" }

    // ── Persist ───────────────────────────────────────────────
    suspend fun saveName(v: String)          { store.edit { it[KEY_NAME]         = v }; syncToFirestore("player_name", v) }
    suspend fun saveQuality(v: String)       { store.edit { it[KEY_QUALITY]      = v } }
    suspend fun saveVhs(v: Boolean)          { store.edit { it[KEY_VHS]          = v } }
    suspend fun saveResolution(v: Float)     { store.edit { it[KEY_RESOLUTION]   = v } }
    suspend fun saveMusic(v: Float)          { store.edit { it[KEY_MUSIC]        = v }; withContext(Dispatchers.Main) { bridge.setMasterVolume(v) } }
    suspend fun saveFootstep(v: Float)       { store.edit { it[KEY_FOOTSTEP]     = v }; withContext(Dispatchers.Main) { bridge.setFootstepVolume(v) } }
    suspend fun saveMonster(v: Float)        { store.edit { it[KEY_MONSTER]      = v }; withContext(Dispatchers.Main) { bridge.setMonsterVolume(v) } }
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
        syncToFirestore("ui_layout_saved", true)
    }

    suspend fun loadUiLayout(): List<UiButtonLayout> {
        val p   = store.data.first()
        val ids = listOf("joystick", "sprint", "interact", "crouch", "flashlight")
        return ids.mapNotNull { id ->
            val x = p[floatPreferencesKey("ui_${id}_x")] ?: return@mapNotNull null
            val y = p[floatPreferencesKey("ui_${id}_y")] ?: return@mapNotNull null
            UiButtonLayout(buttonId = id, offset = Offset(x, y))
        }
    }

    suspend fun syncAllToServer(api: ApiService, settings: GameSettings) {
        withContext(Dispatchers.IO) {
            runCatching { api.syncSettings(settings) }
            syncToFirestore("graphics_quality",   settings.graphicsQuality)
            syncToFirestore("vhs_enabled",        settings.vhsEnabled)
            syncToFirestore("camera_sensitivity", settings.cameraSensitivity)
            syncToFirestore("push_notifications", settings.pushNotifications)
        }
    }

    // ── Remote Config ────────────────────────────────────────
    suspend fun fetchFromRemoteConfig(): Map<String, Any> = withContext(Dispatchers.IO) {
        runCatching {
            val rc = FirebaseRemoteConfig.getInstance()
            rc.setConfigSettingsAsync(
                FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(3600).build()
            ).await()
            rc.fetchAndActivate().await()
            mapOf(
                "force_vhs"           to rc.getBoolean("force_vhs"),
                "default_sensitivity" to rc.getDouble("default_sensitivity").toFloat(),
                "max_fps"             to rc.getLong("max_fps").toInt(),
                "fog_override"        to rc.getBoolean("fog_override")
            )
        }.getOrElse { emptyMap() }
    }

    suspend fun clearAll() { store.edit { it.clear() } }

    suspend fun connectGoogle() { syncToFirestore("google_connect_attempt", System.currentTimeMillis()) }

    private fun syncToFirestore(key: String, value: Any) {
        runCatching {
            FirebaseFirestore.getInstance()
                .collection("user_settings").document("local")
                .set(mapOf(key to value, "updatedAt" to System.currentTimeMillis()), SetOptions.merge())
        }
    }
}

// ═════════════════════════════════════════════════════════════
//  SettingsUiState
// ═════════════════════════════════════════════════════════════
data class SettingsUiState(
    val playerName        : String          = "Wanderer",
    val graphicsQuality   : String          = "medium",
    val vhsEnabled        : Boolean         = true,
    val resolutionScale   : Float           = 1f,
    val musicVolume       : Float           = 0.7f,
    val footstepVolume    : Float           = 0.8f,
    val monsterVolume     : Float           = 0.9f,
    val voiceVolume       : Float           = 0.8f,
    val cameraSensitivity : Float           = 1f,
    val fpsLimit          : Int             = 60,
    val shadowsEnabled    : Boolean         = true,
    val antialiasingOn    : Boolean         = true,
    val fogEnabled        : Boolean         = true,
    val vibrationOn       : Boolean         = true,
    val showFps           : Boolean         = false,
    val showPing          : Boolean         = true,
    val colorBlindMode    : String          = "none",
    val pushNotifications : Boolean         = true,
    val isSyncing         : Boolean         = false,
    val syncSuccess       : Boolean         = false,
    val remoteOverrides   : Map<String,Any> = emptyMap()
)

// ═════════════════════════════════════════════════════════════
//  SettingsVM
// ═════════════════════════════════════════════════════════════
@HiltViewModel
class SettingsVM @Inject constructor(
    private val repo: SettingsRepository,
    private val api : ApiService
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        // Observe DataStore
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
        // Firebase Remote Config overrides
        viewModelScope.launch(Dispatchers.IO) {
            val overrides = repo.fetchFromRemoteConfig()
            if (overrides.isNotEmpty()) {
                _state.update { it.copy(remoteOverrides = overrides) }
                (overrides["force_vhs"]           as? Boolean)?.let { v -> repo.saveVhs(v) }
                (overrides["default_sensitivity"] as? Float)  ?.let { v -> repo.saveSensitivity(v) }
                (overrides["max_fps"]             as? Int)    ?.let { v -> repo.saveFpsLimit(v) }
                (overrides["fog_override"]        as? Boolean)?.let { v -> repo.saveFog(v) }
            }
        }
    }

    // ── Handlers ─────────────────────────────────────────────
    fun onName(v: String)          { _state.update { it.copy(playerName        = v) }; save { repo.saveName(v) } }
    fun onQuality(v: String)       { _state.update { it.copy(graphicsQuality   = v) }; save { repo.saveQuality(v) } }
    fun onVhs(v: Boolean)          { _state.update { it.copy(vhsEnabled        = v) }; save { repo.saveVhs(v) } }
    fun onResolution(v: Float)     { _state.update { it.copy(resolutionScale   = v) }; save { repo.saveResolution(v) } }
    fun onMusic(v: Float)          { _state.update { it.copy(musicVolume       = v) }; save { repo.saveMusic(v) } }
    fun onFootstep(v: Float)       { _state.update { it.copy(footstepVolume    = v) }; save { repo.saveFootstep(v) } }
    fun onMonster(v: Float)        { _state.update { it.copy(monsterVolume     = v) }; save { repo.saveMonster(v) } }
    fun onVoice(v: Float)          { _state.update { it.copy(voiceVolume       = v) }; save { repo.saveVoice(v) } }
    fun onSensitivity(v: Float)    { _state.update { it.copy(cameraSensitivity = v) }; save { repo.saveSensitivity(v) } }
    fun onFpsLimit(v: Int)         { _state.update { it.copy(fpsLimit          = v) }; save { repo.saveFpsLimit(v) } }
    fun onShadows(v: Boolean)      { _state.update { it.copy(shadowsEnabled    = v) }; save { repo.saveShadows(v) } }
    fun onAntialiasing(v: Boolean) { _state.update { it.copy(antialiasingOn    = v) }; save { repo.saveAntialiasing(v) } }
    fun onFog(v: Boolean)          { _state.update { it.copy(fogEnabled        = v) }; save { repo.saveFog(v) } }
    fun onVibration(v: Boolean)    { _state.update { it.copy(vibrationOn       = v) }; save { repo.saveVibration(v) } }
    fun onShowFps(v: Boolean)      { _state.update { it.copy(showFps           = v) }; save { repo.saveShowFps(v) } }
    fun onShowPing(v: Boolean)     { _state.update { it.copy(showPing          = v) }; save { repo.saveShowPing(v) } }
    fun onColorBlind(v: String)    { _state.update { it.copy(colorBlindMode    = v) }; save { repo.saveColorBlind(v) } }
    fun onPushNotif(v: Boolean)    { _state.update { it.copy(pushNotifications = v) }; save { repo.savePushNotif(v) } }
    fun onGoogleConnect()          { save { repo.connectGoogle() } }

    fun syncToServer() {
        viewModelScope.launch {
            _state.update { it.copy(isSyncing = true, syncSuccess = false) }
            val s  = _state.value
            val gs = GameSettings(
                playerName        = s.playerName,
                graphicsQuality   = s.graphicsQuality,
                vhsEnabled        = s.vhsEnabled,
                resolutionScale   = s.resolutionScale,
                musicVolume       = s.musicVolume,
                footstepVolume    = s.footstepVolume,
                monsterVolume     = s.monsterVolume,
                voiceVolume       = s.voiceVolume,
                cameraSensitivity = s.cameraSensitivity,
                fpsLimit          = s.fpsLimit,
                shadowsEnabled    = s.shadowsEnabled,
                antialiasingOn    = s.antialiasingOn,
                fogEnabled        = s.fogEnabled,
                vibrationOn       = s.vibrationOn,
                showFps           = s.showFps,
                showPing          = s.showPing,
                colorBlindMode    = s.colorBlindMode,
                pushNotifications = s.pushNotifications
            )
            runCatching { repo.syncAllToServer(api, gs) }
                .onSuccess { _state.update { it.copy(isSyncing = false, syncSuccess = true) } }
                .onFailure { _state.update { it.copy(isSyncing = false)) } }
        }
    }

    fun resetDefaults() {
        viewModelScope.launch {
            repo.clearAll()
            runCatching { FirebaseCrashlytics.getInstance().log("SETTINGS_RESET") }
        }
    }

    private fun save(block: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) { runCatching { block() } }
    }
}

// ═════════════════════════════════════════════════════════════
//  SettingsScreen
// ═════════════════════════════════════════════════════════════
@Composable
fun SettingsScreen(
    onBack    : () -> Unit,
    onUiEditor: () -> Unit,
    vm        : SettingsVM = hiltViewModel()
) {
    val s by vm.state.collectAsState()

    val tabs = listOf(
        R.string.settings_tab_graphics  to Icons.Default.DisplaySettings,
        R.string.settings_tab_audio     to Icons.AutoMirrored.Filled.VolumeUp,
        R.string.settings_tab_controls  to Icons.Default.SportsEsports,
        R.string.settings_tab_account   to Icons.Default.AccountCircle,
        R.string.settings_tab_gameplay  to Icons.Default.Tune,
        R.string.settings_tab_notif     to Icons.Default.Notifications
    )
    var selectedTab by remember { mutableIntStateOf(0) }

    Box(Modifier.fillMaxSize().background(DarkBg)) {
        CrtOverlay()
        Column(Modifier.fillMaxSize()) {

            // ── Top bar ─────────────────────────────────────
            Row(
                Modifier.fillMaxWidth().background(Color.Black.copy(0.65f)).padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Yellow) }
                Text(stringResource(R.string.menu_settings), color = Yellow, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                Spacer(Modifier.weight(1f))
                if (s.isSyncing) CircularProgressIndicator(Modifier.size(18.dp), color = Yellow, strokeWidth = 2.dp)
                else if (s.syncSuccess) Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
            }
            DividerLine()

            // ── Tab row ──────────────────────────────────────
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor   = Color.Transparent,
                contentColor     = Yellow,
                edgePadding      = 0.dp,
                indicator        = { positions ->
                    val idx = selectedTab.coerceIn(0, positions.lastIndex)
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(positions[idx]),
                        color = Yellow
                    )
                }
            ) {
                tabs.forEachIndexed { index, (labelRes, icon) ->
                    val sel = selectedTab == index
                    Tab(
                        selected = sel,
                        onClick  = { selectedTab = index },
                        text = {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(icon, null, modifier = Modifier.size(14.dp), tint = if (sel) Yellow else TextDim)
                                Text(stringResource(labelRes), fontSize = 10.sp, color = if (sel) Yellow else TextDim)
                            }
                        }
                    )
                }
            }
            DividerLine()

            // ── Tab content ──────────────────────────────────
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (selectedTab) {
                    0 -> GraphicsTab(s, vm::onQuality, vm::onVhs, vm::onResolution, vm::onShadows, vm::onAntialiasing, vm::onFog, vm::onShowFps, vm::onShowPing)
                    1 -> AudioTab(s, vm::onMusic, vm::onFootstep, vm::onMonster, vm::onVoice, vm::onVibration)
                    2 -> ControlsTab(s, vm::onSensitivity, onUiEditor)
                    3 -> AccountTab(s, vm::onName, vm::onGoogleConnect, vm::syncToServer, vm::resetDefaults)
                    4 -> GameplayTab(s, vm::onColorBlind, vm::onFpsLimit)
                    5 -> NotifTab(s, vm::onPushNotif)
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════
//  Tab Composables
// ═════════════════════════════════════════════════════════════
@Composable
private fun GraphicsTab(
    s           : SettingsUiState,
    onQuality   : (String) -> Unit,
    onVhs       : (Boolean) -> Unit,
    onResolution: (Float) -> Unit,
    onShadows   : (Boolean) -> Unit,
    onAA        : (Boolean) -> Unit,
    onFog       : (Boolean) -> Unit,
    onShowFps   : (Boolean) -> Unit,
    onShowPing  : (Boolean) -> Unit
) {
    SLabel(stringResource(R.string.settings_tab_graphics))

    // Graphics quality chips
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(R.string.quality_low to "low", R.string.quality_medium to "medium", R.string.quality_high to "high", R.string.quality_ultra to "ultra")
            .forEach { (res, key) ->
                val sel = s.graphicsQuality == key
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(2.dp))
                        .background(if (sel) Yellow.copy(0.15f) else MetalBg)
                        .border(1.dp, if (sel) Yellow.copy(0.6f) else BorderCol, RoundedCornerShape(2.dp))
                        .clickable { onQuality(key) }
                ) { Text(stringResource(res), color = if (sel) Yellow else TextDim, fontSize = 10.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal) }
            }
    }

    SSlider(stringResource(R.string.graphics_resolution_scale), s.resolutionScale, onResolution, 0.5f..1f)
    SToggle(stringResource(R.string.graphics_vhs_effect),     s.vhsEnabled,       onVhs)
    SToggle(stringResource(R.string.graphics_shadows),        s.shadowsEnabled,   onShadows)
    SToggle(stringResource(R.string.graphics_antialiasing),   s.antialiasingOn,   onAA)
    SToggle(stringResource(R.string.graphics_fog),            s.fogEnabled,       onFog)
    DividerLine()
    SLabel("HUD")
    SToggle(stringResource(R.string.graphics_show_fps),  s.showFps,  onShowFps)
    SToggle(stringResource(R.string.graphics_show_ping), s.showPing, onShowPing)
}

@Composable
private fun AudioTab(
    s         : SettingsUiState,
    onMusic   : (Float) -> Unit,
    onFootstep: (Float) -> Unit,
    onMonster : (Float) -> Unit,
    onVoice   : (Float) -> Unit,
    onVib     : (Boolean) -> Unit
) {
    SLabel(stringResource(R.string.settings_tab_audio))
    SSlider(stringResource(R.string.audio_master_volume),    s.musicVolume,     onMusic)
    SSlider(stringResource(R.string.audio_footstep_volume),  s.footstepVolume,  onFootstep)
    SSlider(stringResource(R.string.audio_monster_sfx_volume), s.monsterVolume, onMonster)
    SSlider(stringResource(R.string.audio_voice_volume),     s.voiceVolume,     onVoice)
    SToggle(stringResource(R.string.settings_vibration),     s.vibrationOn,     onVib)
}

@Composable
private fun ControlsTab(
    s            : SettingsUiState,
    onSensitivity: (Float) -> Unit,
    onUiEditor   : () -> Unit
) {
    SLabel(stringResource(R.string.settings_tab_controls))
    SSlider(stringResource(R.string.controls_camera_sensitivity), s.cameraSensitivity, onSensitivity, 0.1f..3f)
    Spacer(Modifier.height(8.dp))
    OmniButton(stringResource(R.string.controls_ui_layout), onUiEditor, width = 240.dp, height = 48.dp)
}

@Composable
private fun AccountTab(
    s             : SettingsUiState,
    onName        : (String) -> Unit,
    onGoogle      : () -> Unit,
    onSync        : () -> Unit,
    onReset       : () -> Unit
) {
    SLabel(stringResource(R.string.settings_tab_account))

    // Player name field
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.account_player_name), color = TextDim, fontSize = 10.sp, letterSpacing = 1.sp)
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(2.dp))
                .background(MetalBg).border(1.dp, BorderCol, RoundedCornerShape(2.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            androidx.compose.foundation.text.BasicTextField(
                value         = s.playerName,
                onValueChange = onName,
                singleLine    = true,
                textStyle     = TextStyle(color = Yellow, fontSize = 13.sp),
                cursorBrush   = SolidColor(Yellow),
                decorationBox = { inner ->
                    if (s.playerName.isEmpty()) Text("Wanderer", color = TextDim, fontSize = 13.sp)
                    inner()
                }
            )
        }
    }

    DividerLine()

    // Google bağlantısı
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(2.dp)).background(MetalBg)
            .border(1.dp, BorderCol, RoundedCornerShape(2.dp))
            .clickable(onClick = onGoogle).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Default.AccountCircle, null, tint = OmniumCol, modifier = Modifier.size(22.dp))
        Text(stringResource(R.string.account_connect_google), color = TextSec, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ArrowForward, null, tint = TextDim, modifier = Modifier.size(16.dp))
    }

    DividerLine()

    // Sync + Reset
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OmniButton(stringResource(R.string.account_sync), onSync,  width = 150.dp, height = 46.dp, accent = OmniumCol)
        OmniButton(stringResource(R.string.account_reset), onReset, width = 150.dp, height = 46.dp, accent = DangerRed)
    }

    // Remote config overrides badge
    if (s.remoteOverrides.isNotEmpty()) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(2.dp)).background(SouliumCol.copy(0.1f))
                .border(1.dp, SouliumCol.copy(0.3f), RoundedCornerShape(2.dp)).padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Cloud, null, tint = SouliumCol, modifier = Modifier.size(14.dp))
            Text(stringResource(R.string.account_remote_config_active), color = SouliumCol, fontSize = 11.sp)
        }
    }
}

@Composable
private fun GameplayTab(
    s           : SettingsUiState,
    onColorBlind: (String) -> Unit,
    onFpsLimit  : (Int) -> Unit
) {
    SLabel(stringResource(R.string.settings_color_blind))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("none" to "Yok", "deuteranopia" to "D.", "protanopia" to "P.", "tritanopia" to "T.").forEach { (key, label) ->
            val sel = s.colorBlindMode == key
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.clip(RoundedCornerShape(2.dp))
                    .background(if (sel) Yellow.copy(0.15f) else MetalBg)
                    .border(1.dp, if (sel) Yellow.copy(0.6f) else BorderCol, RoundedCornerShape(2.dp))
                    .clickable { onColorBlind(key) }.padding(horizontal = 10.dp, vertical = 8.dp)
            ) { Text(label, color = if (sel) Yellow else TextDim, fontSize = 10.sp) }
        }
    }
    Spacer(Modifier.height(8.dp))
    SLabel(stringResource(R.string.settings_fps_limit))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(30, 60, 90, 120).forEach { fps ->
            val sel = s.fpsLimit == fps
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.clip(RoundedCornerShape(2.dp))
                    .background(if (sel) Yellow.copy(0.15f) else MetalBg)
                    .border(1.dp, if (sel) Yellow.copy(0.6f) else BorderCol, RoundedCornerShape(2.dp))
                    .clickable { onFpsLimit(fps) }.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("$fps", color = if (sel) Yellow else TextDim, fontSize = 11.sp,
                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
private fun NotifTab(s: SettingsUiState, onPush: (Boolean) -> Unit) {
    SLabel(stringResource(R.string.settings_tab_notif))
    SToggle(stringResource(R.string.notif_push_toggle), s.pushNotifications, onPush)
    Spacer(Modifier.height(8.dp))
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(2.dp)).background(MetalBg).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Info, null, tint = TextDim, modifier = Modifier.size(14.dp))
        Text(stringResource(R.string.notif_info_text), color = TextDim, fontSize = 11.sp, lineHeight = 16.sp)
    }
}

// ═════════════════════════════════════════════════════════════
//  Shared Settings Sub-Composables
// ═════════════════════════════════════════════════════════════
@Composable
private fun SLabel(text: String) {
    Text(text, color = TextSec, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun SToggle(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(label, color = TextSec, fontSize = 12.sp)
        Switch(
            checked         = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor   = Yellow,
                checkedTrackColor   = YellowDim,
                uncheckedThumbColor = TextDim,
                uncheckedTrackColor = MetalBg
            )
        )
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
        Slider(
            value         = value,
            onValueChange = onValue,
            valueRange    = range,
            colors = SliderDefaults.colors(
                thumbColor        = Yellow,
                activeTrackColor  = Yellow,
                inactiveTrackColor= MetalBg
            )
        )
    }
}

// ═════════════════════════════════════════════════════════════
//  UiEditorVM + UiEditor
// ═════════════════════════════════════════════════════════════
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
            DragBtn("joystick",   R.string.editor_btn_move,       80f,    400f),
            DragBtn("sprint",     R.string.editor_btn_sprint,     300f,   460f),
            DragBtn("interact",   R.string.editor_btn_interact,   900f,   400f),
            DragBtn("crouch",     R.string.editor_btn_crouch,     1000f,  460f),
            DragBtn("flashlight", R.string.editor_btn_flashlight, 1100f,  400f)
        )
    }

    Box(Modifier.fillMaxSize().background(DarkBg.copy(0.92f))) {
        CrtOverlay()

        Box(Modifier.fillMaxWidth().padding(top = 20.dp), Alignment.TopCenter) {
            Text(
                stringResource(R.string.controls_ui_layout).uppercase(),
                color = TextDim, fontSize = 10.sp, letterSpacing = 3.sp
            )
        }

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
                Text(
                    stringResource(btn.labelRes),
                    color         = Yellow,
                    fontSize      = 9.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        OmniButton(
            text     = stringResource(R.string.controls_save_exit),
            onClick  = {
                vm.saveLayout(buttons.map { UiButtonLayout(buttonId = it.id, offset = Offset(it.ox, it.oy)) })
                onSave()
            },
            width    = 200.dp,
            height   = 48.dp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)
        )
    }
}
