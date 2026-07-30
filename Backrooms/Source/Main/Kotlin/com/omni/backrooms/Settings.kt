package com.omni.backrooms

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import android.os.Build
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
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
            cameraSensitivity = (p[KEY_SENSITIVITY] ?: 1f).let { if (it <= 0f) 1f else it },
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

    suspend fun fetchFromRemoteConfig(): Map<String, Any> = withContext(Dispatchers.IO) {
        runCatching {
            val rc = FirebaseRemoteConfig.getInstance()
            rc.setConfigSettingsAsync(
                FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(3600)
                    .build()
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
                .collection("user_settings")
                .document("local")
                .set(mapOf(key to value, "updatedAt" to System.currentTimeMillis()), SetOptions.merge())
        }
    }
}

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
    val remoteOverrides   : Map<String,Any> = emptyMap(),
    val googleState       : GoogleAuthState = GoogleAuthState()
)

@HiltViewModel
class SettingsVM @Inject constructor(
    private val repo             : SettingsRepository,
    private val api              : ApiService,
    private val googleAuthManager: GoogleAuthManager,
    private val identity         : GuestIdentityManager,
    private val locales          : LocaleStore
) : ViewModel() {

    val languageSelection: StateFlow<String> = locales.observeSelection()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppLanguage.SYSTEM)

    /** Persists the choice; the caller recreates the activity so every screen
     *  re-resolves its strings in the new language. */
    fun onLanguage(value: String) { viewModelScope.launch { locales.setSelection(value) } }

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

        viewModelScope.launch {
            // Keep the field in step with the shared identity name.
            identity.observeDisplayName().collect { shared ->
                if (shared.isNotBlank()) _state.update { it.copy(playerName = shared) }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            val overrides = repo.fetchFromRemoteConfig()
            if (overrides.isNotEmpty()) {
                _state.update { it.copy(remoteOverrides = overrides) }
                (overrides["force_vhs"]           as? Boolean)?.let { v -> repo.saveVhs(v) }
                (overrides["default_sensitivity"] as? Float)  ?.let { v -> if (v > 0f) repo.saveSensitivity(v) }
                (overrides["max_fps"]             as? Int)    ?.let { v -> if (v > 0) repo.saveFpsLimit(v) }
                (overrides["fog_override"]        as? Boolean)?.let { v -> repo.saveFog(v) }
            }
        }

        refreshGoogleState()
    }

    private fun refreshGoogleState() {
        val user = googleAuthManager.currentUser
        _state.update {
            it.copy(
                googleState = if (user != null) {
                    GoogleAuthState(
                        isSignedIn  = true,
                        displayName = user.displayName ?: "",
                        email       = user.email ?: "",
                        photoUrl    = user.photoUrl?.toString()
                    )
                } else GoogleAuthState()
            )
        }
    }

    fun signInWithGoogle(activity: Activity) {
        viewModelScope.launch {
            _state.update { it.copy(googleState = it.googleState.copy(isLoading = true, error = null)) }
            googleAuthManager.signIn(activity)
                .onSuccess { user ->
                    _state.update {
                        it.copy(
                            googleState = GoogleAuthState(
                                isSignedIn  = true,
                                displayName = user.displayName ?: "",
                                email       = user.email ?: "",
                                photoUrl    = user.photoUrl?.toString(),
                                isLoading   = false
                            )
                        )
                    }
                    runCatching { repo.connectGoogle() }
                }
                .onFailure { e ->
                    _state.update { it.copy(googleState = it.googleState.copy(isLoading = false, error = e.message)) }
                }
        }
    }

    fun signOutGoogle() {
        googleAuthManager.signOut()
        _state.update { it.copy(googleState = GoogleAuthState()) }
    }

    /** Writes both the settings copy and the shared identity record, which the
     *  lobby and profile observe — previously the name only lived here, so those
     *  screens kept showing the old one. */
    fun onName(v: String) {
        _state.update { it.copy(playerName = v) }
        save { repo.saveName(v) }
        viewModelScope.launch { identity.setDisplayName(v) }
    }
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
                .onFailure { _state.update { it.copy(isSyncing = false) } }
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

private enum class SettingsTab(val labelRes: Int, val icon: ImageVector) {
    Graphics    (R.string.settings_tab_graphics,  Icons.Default.DisplaySettings),
    Audio       (R.string.settings_tab_audio,     Icons.AutoMirrored.Filled.VolumeUp),
    Controls    (R.string.settings_tab_controls,  Icons.Default.SportsEsports),
    Account     (R.string.settings_tab_account,   Icons.Default.AccountCircle),
    Gameplay    (R.string.settings_tab_gameplay,  Icons.Default.Tune),
    Notif       (R.string.settings_tab_notif,     Icons.Default.Notifications),
    Language    (R.string.settings_tab_language,  Icons.Default.Language)
}

@Composable
fun SettingsScreen(
    onBack    : () -> Unit,
    onUiEditor: () -> Unit,
    vm        : SettingsVM = hiltViewModel()
) {
    val s       by vm.state.collectAsState()
    val activity = LocalContext.current as? Activity
    var selectedTab by remember { mutableIntStateOf(0) }

    Box(Modifier.fillMaxSize().background(DarkBg)) {
        CrtOverlay()
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().background(Color.Black.copy(0.65f)).padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Yellow) }
                Text(stringResource(R.string.menu_settings), color = Yellow, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                Spacer(Modifier.weight(1f))
                androidx.compose.animation.AnimatedVisibility(visible = s.isSyncing, enter = fadeIn(), exit = fadeOut()) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = Yellow, strokeWidth = 2.dp)
                }
                androidx.compose.animation.AnimatedVisibility(visible = !s.isSyncing && s.syncSuccess, enter = fadeIn(), exit = fadeOut()) {
                    Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                }
            }
            DividerLine()

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
                SettingsTab.entries.forEachIndexed { index, tab ->
                    val sel = selectedTab == index
                    Tab(
                        selected = sel,
                        onClick  = { selectedTab = index },
                        text = {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(tab.icon, null, modifier = Modifier.size(14.dp), tint = if (sel) Yellow else TextDim)
                                Text(stringResource(tab.labelRes), fontSize = 10.sp, color = if (sel) Yellow else TextDim)
                            }
                        }
                    )
                }
            }
            DividerLine()

            AnimatedContent(
                targetState  = selectedTab,
                transitionSpec = {
                    slideInHorizontally(tween(250)) { if (targetState > initialState) it / 3 else -it / 3 } +
                    fadeIn(tween(200)) togetherWith
                    slideOutHorizontally(tween(200)) { if (targetState > initialState) -it / 3 else it / 3 } +
                    fadeOut(tween(150))
                },
                label        = "settings_tab"
            ) { tabIndex ->
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (tabIndex) {
                        0 -> GraphicsTab(s, vm::onQuality, vm::onVhs, vm::onResolution, vm::onShadows, vm::onAntialiasing, vm::onFog, vm::onShowFps, vm::onShowPing)
                        1 -> AudioTab(s, vm::onMusic, vm::onFootstep, vm::onMonster, vm::onVoice, vm::onVibration)
                        2 -> ControlsTab(s, vm::onSensitivity, onUiEditor)
                        3 -> AccountTab(s, vm::onName, { activity?.let { a -> vm.signInWithGoogle(a) } }, vm::signOutGoogle, vm::syncToServer, vm::resetDefaults)
                        4 -> GameplayTab(s, vm::onColorBlind, vm::onFpsLimit)
                        5 -> NotifTab(s, vm::onPushNotif)
                        6 -> {
                            val lang by vm.languageSelection.collectAsState()
                            LanguageSection(current = lang, onSelect = vm::onLanguage)
                        }
                    }
                }
            }
        }
    }
}

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
    SettingsSection(stringResource(R.string.settings_tab_graphics))

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(
            R.string.quality_low    to "low",
            R.string.quality_medium to "medium",
            R.string.quality_high   to "high",
            R.string.quality_ultra  to "ultra"
        ).forEach { (res, key) ->
            val sel   = s.graphicsQuality == key
            val scale by animateFloatAsState(if (sel) 1.04f else 1f, spring(), label = "q_$key")
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.weight(1f).height(38.dp).scale(scale)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (sel) Yellow.copy(0.15f) else MetalBg)
                    .border(1.dp, if (sel) Yellow.copy(0.6f) else BorderCol, RoundedCornerShape(2.dp))
                    .clickable { onQuality(key) }
            ) {
                Text(stringResource(res), color = if (sel) Yellow else TextDim, fontSize = 10.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }

    SettingsSlider(stringResource(R.string.graphics_resolution_scale), s.resolutionScale, onResolution, 0.5f..1f)
    SettingsToggle(stringResource(R.string.graphics_vhs_effect),   s.vhsEnabled,       onVhs)
    SettingsToggle(stringResource(R.string.graphics_shadows),      s.shadowsEnabled,   onShadows)
    SettingsToggle(stringResource(R.string.graphics_antialiasing), s.antialiasingOn,   onAA)
    SettingsToggle(stringResource(R.string.graphics_fog),          s.fogEnabled,       onFog)
    DividerLine()
    SettingsSection("HUD")
    SettingsToggle(stringResource(R.string.graphics_show_fps),  s.showFps,  onShowFps)
    SettingsToggle(stringResource(R.string.graphics_show_ping), s.showPing, onShowPing)
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
    SettingsSection(stringResource(R.string.settings_tab_audio))
    SettingsSlider(stringResource(R.string.audio_master_volume),      s.musicVolume,    onMusic)
    SettingsSlider(stringResource(R.string.audio_footstep_volume),    s.footstepVolume, onFootstep)
    SettingsSlider(stringResource(R.string.audio_monster_sfx_volume), s.monsterVolume,  onMonster)
    SettingsSlider(stringResource(R.string.audio_voice_volume),       s.voiceVolume,    onVoice)
    SettingsToggle(stringResource(R.string.settings_vibration),       s.vibrationOn,    onVib)
}

@Composable
private fun ControlsTab(
    s            : SettingsUiState,
    onSensitivity: (Float) -> Unit,
    onUiEditor   : () -> Unit
) {
    SettingsSection(stringResource(R.string.settings_tab_controls))
    SettingsSlider(stringResource(R.string.controls_camera_sensitivity), s.cameraSensitivity, onSensitivity, 0.1f..3f)
    Spacer(Modifier.height(8.dp))
    AtmosphericButton(
        label   = stringResource(R.string.controls_ui_layout),
        icon    = Icons.Default.GridView,
        accent  = Yellow,
        width   = 240.dp,
        height  = 48.dp,
        onClick = onUiEditor
    )
}

@Composable
private fun AccountTab(
    s        : SettingsUiState,
    onName   : (String) -> Unit,
    onGoogle : () -> Unit,
    onSignOut: () -> Unit,
    onSync   : () -> Unit,
    onReset  : () -> Unit
) {
    SettingsSection(stringResource(R.string.settings_tab_account))

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.account_player_name), color = TextDim, fontSize = 10.sp, letterSpacing = 1.sp)
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(2.dp))
                .background(MetalBg)
                .border(1.dp, BorderCol, RoundedCornerShape(2.dp))
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

    GoogleConnectCard(s.googleState, onGoogle, onSignOut)

    DividerLine()

    androidx.compose.animation.AnimatedVisibility(
        visible = s.remoteOverrides.isNotEmpty(),
        enter   = expandVertically() + fadeIn(),
        exit    = shrinkVertically() + fadeOut()
    ) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(2.dp))
                .background(SouliumCol.copy(0.1f))
                .border(1.dp, SouliumCol.copy(0.3f), RoundedCornerShape(2.dp))
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Cloud, null, tint = SouliumCol, modifier = Modifier.size(14.dp))
            Text(stringResource(R.string.account_remote_config_active), color = SouliumCol, fontSize = 11.sp)
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AtmosphericButton(stringResource(R.string.account_sync),  Icons.Default.Sync,    OmniumCol, 150.dp, 46.dp, onSync)
        AtmosphericButton(stringResource(R.string.account_reset), Icons.Default.Refresh, DangerRed, 150.dp, 46.dp, onReset)
    }
}

@Composable
private fun GoogleConnectCard(
    state    : GoogleAuthState,
    onSignIn : () -> Unit,
    onSignOut: () -> Unit
) {
    AnimatedContent(
        targetState  = state.isSignedIn,
        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
        label        = "google_card"
    ) { isSignedIn ->
        if (isSignedIn) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(3.dp))
                    .background(MetalBg)
                    .border(1.dp, SuccessGreen.copy(0.3f), RoundedCornerShape(3.dp))
                    .padding(12.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(Modifier.size(38.dp).clip(CircleShape).background(OmniumCol.copy(0.15f)), Alignment.Center) {
                    Icon(Icons.Default.AccountCircle, null, tint = OmniumCol, modifier = Modifier.size(22.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(state.displayName.ifEmpty { "Google Kullanıcı" }, color = Yellow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(state.email, color = TextSec, fontSize = 10.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                    Text("Bağlı", color = SuccessGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onSignOut, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Logout, null, tint = DangerRed.copy(0.7f), modifier = Modifier.size(18.dp))
                }
            }
        } else {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(3.dp))
                    .background(MetalBg)
                    .border(1.dp, if (state.isLoading) Yellow.copy(0.4f) else BorderCol, RoundedCornerShape(3.dp))
                    .clickable(enabled = !state.isLoading, onClick = onSignIn)
                    .padding(12.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = Yellow, strokeWidth = 2.dp)
                } else {
                    Box(Modifier.size(38.dp).clip(CircleShape).background(OmniumCol.copy(0.1f)), Alignment.Center) {
                        Icon(Icons.Default.AccountCircle, null, tint = OmniumCol, modifier = Modifier.size(22.dp))
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        if (state.isLoading) "Bağlanıyor…" else stringResource(R.string.account_connect_google),
                        color      = if (state.isLoading) TextSec else Yellow,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    state.error?.let { err -> Text(err, color = DangerRed, fontSize = 10.sp) }
                }
                if (!state.isLoading) Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = TextDim, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun GameplayTab(
    s           : SettingsUiState,
    onColorBlind: (String) -> Unit,
    onFpsLimit  : (Int) -> Unit
) {
    SettingsSection(stringResource(R.string.settings_color_blind))
    val cbOptions = listOf(
        Triple("none",          R.string.cb_none,          R.string.cb_none_desc),
        Triple("deuteranopia",  R.string.cb_deuteranopia,  R.string.cb_deuteranopia_desc),
        Triple("protanopia",    R.string.cb_protanopia,    R.string.cb_protanopia_desc),
        Triple("tritanopia",    R.string.cb_tritanopia,    R.string.cb_tritanopia_desc)
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        cbOptions.forEach { (key, titleRes, descRes) ->
            val sel   = s.colorBlindMode == key
            val scale by animateFloatAsState(if (sel) 1.01f else 1f, spring(), label = "cb_$key")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(scale)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (sel) Yellow.copy(0.12f) else MetalBg)
                    .border(1.dp, if (sel) Yellow.copy(0.6f) else BorderCol, RoundedCornerShape(4.dp))
                    .clickable { onColorBlind(key) }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                // Radio indicator drawn in code so it matches the rest of the UI.
                androidx.compose.foundation.Canvas(Modifier.size(16.dp)) {
                    val r = size.minDimension / 2f
                    drawCircle(if (sel) Yellow else TextDim, radius = r * 0.9f, center = center, style = Stroke(1.5f))
                    if (sel) drawCircle(Yellow, radius = r * 0.45f, center = center)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(titleRes),
                        color = if (sel) Yellow else TextSec,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        stringResource(descRes),
                        color = TextDim, fontSize = 10.sp, lineHeight = 14.sp
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    SettingsSection(stringResource(R.string.settings_fps_limit))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(30, 60, 90, 120).forEach { fps ->
            val sel   = s.fpsLimit == fps
            val scale by animateFloatAsState(if (sel) 1.05f else 1f, spring(), label = "fps_$fps")
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.scale(scale)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (sel) Yellow.copy(0.15f) else MetalBg)
                    .border(1.dp, if (sel) Yellow.copy(0.6f) else BorderCol, RoundedCornerShape(2.dp))
                    .clickable { onFpsLimit(fps) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("$fps", color = if (sel) Yellow else TextDim, fontSize = 11.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
private fun NotifTab(s: SettingsUiState, onPush: (Boolean) -> Unit) {
    val ctx = LocalContext.current
    // The in-app toggle used to be a purely local preference defaulting to on,
    // so declining the system prompt still showed notifications as enabled.
    // Recomputed on every resume, because the user can change it in system
    // settings and come straight back.
    val lifecycleOwner = LocalLifecycleOwner.current
    var osGranted by remember { mutableStateOf(hasNotificationPermission(ctx)) }
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) osGranted = hasNotificationPermission(ctx)
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    SettingsSection(stringResource(R.string.settings_tab_notif))
    // Effective state = local preference AND the OS actually allowing it.
    SettingsToggle(
        stringResource(R.string.notif_push_toggle),
        s.pushNotifications && osGranted,
        { wanted -> if (osGranted) onPush(wanted) else openAppNotificationSettings(ctx) }
    )
    Spacer(Modifier.height(6.dp))
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
            .background(if (osGranted) MetalBg else DangerRed.copy(0.10f))
            .border(1.dp, if (osGranted) BorderCol else DangerRed.copy(0.45f), RoundedCornerShape(4.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.Canvas(Modifier.size(14.dp)) {
            val c = if (osGranted) SuccessGreen else DangerRed
            drawCircle(c, radius = size.minDimension * 0.42f, center = center, style = Stroke(1.6f))
            if (osGranted) {
                drawLine(c, Offset(size.width * 0.30f, size.height * 0.52f), Offset(size.width * 0.45f, size.height * 0.68f), strokeWidth = 1.8f, cap = StrokeCap.Round)
                drawLine(c, Offset(size.width * 0.45f, size.height * 0.68f), Offset(size.width * 0.72f, size.height * 0.34f), strokeWidth = 1.8f, cap = StrokeCap.Round)
            } else {
                drawLine(c, Offset(size.width * 0.34f, size.height * 0.34f), Offset(size.width * 0.66f, size.height * 0.66f), strokeWidth = 1.8f, cap = StrokeCap.Round)
                drawLine(c, Offset(size.width * 0.66f, size.height * 0.34f), Offset(size.width * 0.34f, size.height * 0.66f), strokeWidth = 1.8f, cap = StrokeCap.Round)
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            stringResource(if (osGranted) R.string.notif_permission_granted else R.string.notif_permission_denied),
            color = if (osGranted) TextSec else DangerRed.copy(0.9f), fontSize = 11.sp,
            modifier = Modifier.weight(1f)
        )
        if (!osGranted) {
            Text(
                stringResource(R.string.notif_open_settings),
                color = Yellow, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { openAppNotificationSettings(ctx) }
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(2.dp))
            .background(MetalBg)
            .border(1.dp, BorderCol, RoundedCornerShape(2.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Info, null, tint = TextDim, modifier = Modifier.size(14.dp))
        Text(stringResource(R.string.notif_info_text), color = TextDim, fontSize = 11.sp, lineHeight = 16.sp)
    }
}

@Composable
private fun SettingsSection(text: String) {
    Text(text, color = TextSec, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun SettingsToggle(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
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
private fun SettingsSlider(
    label  : String,
    value  : Float,
    onValue: (Float) -> Unit,
    range  : ClosedFloatingPointRange<Float> = 0f..1f
) {
    val displayValue by remember(value) { derivedStateOf { (value * 100).toInt() } }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text(label, color = TextSec, fontSize = 12.sp)
            Text("$displayValue%", color = Yellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value         = value,
            onValueChange = onValue,
            valueRange    = range,
            colors = SliderDefaults.colors(
                thumbColor         = Yellow,
                activeTrackColor   = Yellow,
                inactiveTrackColor = MetalBg
            )
        )
    }
}

private data class DragBtn(val id: String, val labelRes: Int, var ox: Float, var oy: Float)

@HiltViewModel
class UiEditorVM @Inject constructor(private val repo: SettingsRepository) : ViewModel() {
    fun saveLayout(layout: List<UiButtonLayout>) {
        viewModelScope.launch { repo.saveUiLayout(layout) }
    }
}

@Composable
fun UiEditor(onSave: () -> Unit, vm: UiEditorVM = hiltViewModel()) {
    // Mirrors the real HUD exactly: same controls, same relative cluster
    // positions. There is no sprint button in game, so the editor no longer
    // offers one — it used to let players arrange a control that didn't exist.
    val buttons = remember {
        mutableStateListOf(
            DragBtn("joystick",   R.string.editor_btn_move,       70f,  430f),
            DragBtn("interact",   R.string.editor_btn_interact,   1010f, 440f),
            DragBtn("flashlight", R.string.editor_btn_flashlight, 900f,  455f),
            DragBtn("jump",       R.string.editor_btn_jump,       1000f, 340f),
            DragBtn("crouch",     R.string.editor_btn_crouch,     890f,  340f)
        )
    }

    Box(Modifier.fillMaxSize().background(DarkBg.copy(0.92f))) {
        CrtOverlay()

        Box(Modifier.fillMaxWidth().padding(top = 20.dp), Alignment.TopCenter) {
            Text(
                stringResource(R.string.controls_ui_layout).uppercase(),
                color        = TextDim,
                fontSize     = 10.sp,
                letterSpacing = 3.sp
            )
        }

        buttons.forEachIndexed { index, btn ->
            var ox by remember { mutableFloatStateOf(btn.ox) }
            var oy by remember { mutableFloatStateOf(btn.oy) }
            val oxAnim by animateFloatAsState(ox, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh), label = "drag_x_$index")
            val oyAnim by animateFloatAsState(oy, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh), label = "drag_y_$index")

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset { IntOffset(oxAnim.roundToInt(), oyAnim.roundToInt()) }
                    .size(80.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.verticalGradient(listOf(MetalBg.copy(0.95f), DarkBg.copy(0.9f)))
                    )
                    .border(1.dp, YellowDim, RoundedCornerShape(4.dp))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = { buttons[index] = btn.copy(ox = ox, oy = oy) }
                        ) { ch, drag ->
                            ch.consume()
                            ox += drag.x
                            oy += drag.y
                        }
                    }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    androidx.compose.foundation.Canvas(Modifier.size(30.dp)) {
                        editorGlyph(btn.id, Yellow)
                    }
                    Text(
                        stringResource(btn.labelRes),
                        color        = Yellow,
                        fontSize     = 8.sp,
                        fontWeight   = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        AtmosphericButton(
            label    = stringResource(R.string.controls_save_exit),
            icon     = Icons.Default.Save,
            accent   = Yellow,
            width    = 200.dp,
            height   = 48.dp,
            onClick  = {
                vm.saveLayout(buttons.map { UiButtonLayout(buttonId = it.id, offset = Offset(it.ox, it.oy)) })
                onSave()
            },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)
        )
    }
}


/** Draws the same control glyphs the in-game HUD uses, so the layout editor is a
 *  true preview rather than a set of generic drag handles. Kept local to this
 *  file to avoid widening the HUD drawing API. */
private fun DrawScope.editorGlyph(id: String, c: Color) {
    val w = size.width; val h = size.height
    val sw = size.minDimension * 0.10f
    when (id) {
        "joystick" -> {
            drawCircle(c.copy(0.55f), radius = w * 0.42f, center = center, style = Stroke(sw * 0.8f))
            drawCircle(c, radius = w * 0.16f, center = center)
        }
        "interact" -> {
            drawRoundRect(
                c, topLeft = Offset(w * 0.34f, h * 0.42f), size = Size(w * 0.32f, h * 0.40f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.10f), style = Stroke(sw * 0.85f)
            )
            for (i in 0 until 3) {
                val x = w * (0.40f + i * 0.10f)
                drawLine(c, Offset(x, h * 0.42f), Offset(x, h * 0.20f), strokeWidth = sw * 0.8f, cap = StrokeCap.Round)
            }
        }
        "flashlight" -> {
            val beam = Path().apply {
                moveTo(w * 0.42f, h * 0.46f); lineTo(w * 0.58f, h * 0.46f)
                lineTo(w * 0.80f, h * 0.90f); lineTo(w * 0.20f, h * 0.90f); close()
            }
            drawPath(beam, c.copy(0.28f))
            drawPath(beam, c.copy(0.85f), style = Stroke(sw * 0.65f))
            drawRoundRect(
                c, topLeft = Offset(w * 0.38f, h * 0.16f), size = Size(w * 0.24f, h * 0.26f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.04f), style = Stroke(sw * 0.8f)
            )
        }
        "jump" -> {
            drawLine(c, Offset(w * 0.5f, h * 0.76f), Offset(w * 0.5f, h * 0.28f), strokeWidth = sw, cap = StrokeCap.Round)
            val head = Path().apply {
                moveTo(w * 0.5f, h * 0.16f); lineTo(w * 0.70f, h * 0.40f); lineTo(w * 0.30f, h * 0.40f); close()
            }
            drawPath(head, c)
        }
        "crouch" -> {
            drawLine(c, Offset(w * 0.5f, h * 0.24f), Offset(w * 0.5f, h * 0.72f), strokeWidth = sw, cap = StrokeCap.Round)
            val head = Path().apply {
                moveTo(w * 0.5f, h * 0.84f); lineTo(w * 0.70f, h * 0.60f); lineTo(w * 0.30f, h * 0.60f); close()
            }
            drawPath(head, c)
        }
        else -> drawCircle(c.copy(0.5f), radius = w * 0.30f, center = center, style = Stroke(sw))
    }
}


/** True only when the OS actually permits notifications. Below Android 13 the
 *  runtime permission doesn't exist, so channel-level enablement is the answer. */
private fun hasNotificationPermission(ctx: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    } else {
        NotificationManagerCompat.from(ctx).areNotificationsEnabled()
    }

/** Android won't re-prompt after a denial, so the only honest path is to send
 *  the player to the system screen where they can change it. */
private fun openAppNotificationSettings(ctx: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, ctx.packageName)
    } else {
        Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(android.net.Uri.fromParts("package", ctx.packageName, null))
    }
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { ctx.startActivity(intent) }
}


/** Language picker. Changing the language recreates the activity, which is the
 *  simplest correct way to make every already-composed screen re-read its
 *  strings — Compose caches resolved resources per composition. */
@Composable
fun LanguageSection(current: String, onSelect: (String) -> Unit) {
    val ctx = LocalContext.current
    val options = buildList {
        add(AppLanguage.SYSTEM to stringResource(R.string.settings_language_system))
        AppLanguage.entries.forEach { add(it.tag to it.endonym) }
    }
    SettingsSection(stringResource(R.string.settings_language))
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { (tag, label) ->
            val sel = current == tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (sel) Yellow.copy(0.12f) else MetalBg)
                    .border(1.dp, if (sel) Yellow.copy(0.6f) else BorderCol, RoundedCornerShape(4.dp))
                    .clickable {
                        if (!sel) {
                            onSelect(tag)
                            // Recreate so the whole UI picks up the new locale.
                            (ctx as? android.app.Activity)?.recreate()
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 11.dp)
            ) {
                androidx.compose.foundation.Canvas(Modifier.size(16.dp)) {
                    val r = size.minDimension / 2f
                    drawCircle(if (sel) Yellow else TextDim, radius = r * 0.9f, center = center, style = Stroke(1.5f))
                    if (sel) drawCircle(Yellow, radius = r * 0.45f, center = center)
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    label,
                    color = if (sel) Yellow else TextSec,
                    fontSize = 12.sp,
                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
