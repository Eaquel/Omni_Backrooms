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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.onSizeChanged
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

        val KEY_CAMERA_VIEW  = stringPreferencesKey("camera_view")
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
            cameraView        = p[KEY_CAMERA_VIEW]  ?: "first",
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
    suspend fun saveCameraView(v: String)    { store.edit { it[KEY_CAMERA_VIEW]  = v } }
    suspend fun savePushNotif(v: Boolean)    { store.edit { it[KEY_PUSH_NOTIF]   = v } }

    suspend fun saveUiLayout(layout: List<UiButtonLayout>) {
        store.edit { p ->
            layout.forEach { b ->
                p[floatPreferencesKey("ui_${b.buttonId}_x")] = b.offset.x
                p[floatPreferencesKey("ui_${b.buttonId}_y")] = b.offset.y
                // Scale was previously discarded, so resizing never survived a
                // save/reload round-trip.
                p[floatPreferencesKey("ui_${b.buttonId}_s")] = b.sizeScale
            }
        }
        syncToFirestore("ui_layout_saved", true)
    }

    /** Reads the layout back. Elements absent from the store fall back to the
     *  caller's defaults, so adding a new HUD element never breaks an existing
     *  saved layout. */
    fun observeUiLayout(): Flow<Map<String, UiButtonLayout>> = store.data.map { p ->
        val ids = p.asMap().keys
            .map { it.name }
            .filter { it.startsWith("ui_") && it.endsWith("_x") }
            .map { it.removePrefix("ui_").removeSuffix("_x") }
        ids.mapNotNull { id ->
            val x = p[floatPreferencesKey("ui_${id}_x")] ?: return@mapNotNull null
            val y = p[floatPreferencesKey("ui_${id}_y")] ?: return@mapNotNull null
            val sc = p[floatPreferencesKey("ui_${id}_s")] ?: 1f
            id to UiButtonLayout(id, Offset(x, y), sc)
        }.toMap()
    }

    suspend fun resetUiLayout() {
        store.edit { p ->
            p.asMap().keys.map { it.name }.filter { it.startsWith("ui_") }.forEach {
                p.remove(floatPreferencesKey(it))
            }
        }
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
    /** "first" or "third". Third-person needs the character model, so it only
     *  applies once one is equipped. */
    val cameraView        : String          = "first",
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

    /** Saved HUD layout, consumed by GameHud so the editor's result is real. */
    val uiLayout: StateFlow<Map<String, UiButtonLayout>> = repo.observeUiLayout()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val languageSelection: StateFlow<String> = locales.observeSelection()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppLanguage.SYSTEM)

    /** Persists the choice; the caller recreates the activity so every screen
     *  re-resolves its strings in the new language. */
    fun onLanguage(value: String) { viewModelScope.launch { locales.setSelection(value) } }

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    /** Keys the user has just changed, whose DataStore write may not have landed
     *  yet. The store collector below skips these so a freshly-toggled switch is
     *  not overwritten by the stale value still in flight. */
    private val pendingWrites = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            repo.observe().collect { g ->
                _state.update { cur ->
                    // Anything the user just touched keeps its local value until
                    // the store agrees with it.
                    fun <T> pick(key: String, stored: T, local: T): T =
                        if (key in pendingWrites) {
                            if (stored == local) pendingWrites.remove(key)
                            local
                        } else stored

                    cur.copy(
                        playerName        = pick("name", g.playerName, cur.playerName),
                        graphicsQuality   = pick("quality", g.graphicsQuality, cur.graphicsQuality),
                        vhsEnabled        = pick("vhs", g.vhsEnabled, cur.vhsEnabled),
                        resolutionScale   = pick("res", g.resolutionScale, cur.resolutionScale),
                        musicVolume       = pick("music", g.musicVolume, cur.musicVolume),
                        footstepVolume    = pick("foot", g.footstepVolume, cur.footstepVolume),
                        monsterVolume     = pick("monster", g.monsterVolume, cur.monsterVolume),
                        voiceVolume       = pick("voice", g.voiceVolume, cur.voiceVolume),
                        cameraSensitivity = pick("sens", g.cameraSensitivity, cur.cameraSensitivity),
                        fpsLimit          = pick("fps", g.fpsLimit, cur.fpsLimit),
                        shadowsEnabled    = pick("shadows", g.shadowsEnabled, cur.shadowsEnabled),
                        antialiasingOn    = pick("aa", g.antialiasingOn, cur.antialiasingOn),
                        fogEnabled        = pick("fog", g.fogEnabled, cur.fogEnabled),
                        vibrationOn       = pick("vibe", g.vibrationOn, cur.vibrationOn),
                        showFps           = pick("showFps", g.showFps, cur.showFps),
                        showPing          = pick("showPing", g.showPing, cur.showPing),
                        colorBlindMode    = pick("cb", g.colorBlindMode, cur.colorBlindMode),
                        cameraView        = pick("camview", g.cameraView, cur.cameraView),
                        pushNotifications = pick("push", g.pushNotifications, cur.pushNotifications)
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
        pendingWrites.add("name")
        _state.update { it.copy(playerName = v) }
        save { repo.saveName(v) }
        viewModelScope.launch { identity.setDisplayName(v) }
    }
    fun onQuality(v: String)       { pendingWrites.add("quality"); _state.update { it.copy(graphicsQuality   = v) }; save { repo.saveQuality(v) } }
    fun onVhs(v: Boolean)          { pendingWrites.add("vhs"); _state.update { it.copy(vhsEnabled        = v) }; save { repo.saveVhs(v) } }
    fun onResolution(v: Float)     { pendingWrites.add("res"); _state.update { it.copy(resolutionScale   = v) }; save { repo.saveResolution(v) } }
    fun onMusic(v: Float)          { pendingWrites.add("music"); _state.update { it.copy(musicVolume       = v) }; save { repo.saveMusic(v) } }
    fun onFootstep(v: Float)       { pendingWrites.add("foot"); _state.update { it.copy(footstepVolume    = v) }; save { repo.saveFootstep(v) } }
    fun onMonster(v: Float)        { pendingWrites.add("monster"); _state.update { it.copy(monsterVolume     = v) }; save { repo.saveMonster(v) } }
    fun onVoice(v: Float)          { pendingWrites.add("voice"); _state.update { it.copy(voiceVolume       = v) }; save { repo.saveVoice(v) } }
    fun onSensitivity(v: Float)    { pendingWrites.add("sens"); _state.update { it.copy(cameraSensitivity = v) }; save { repo.saveSensitivity(v) } }
    fun onFpsLimit(v: Int)         { pendingWrites.add("fps"); _state.update { it.copy(fpsLimit          = v) }; save { repo.saveFpsLimit(v) } }
    fun onShadows(v: Boolean)      { pendingWrites.add("shadows"); _state.update { it.copy(shadowsEnabled    = v) }; save { repo.saveShadows(v) } }
    fun onAntialiasing(v: Boolean) { pendingWrites.add("aa"); _state.update { it.copy(antialiasingOn    = v) }; save { repo.saveAntialiasing(v) } }
    fun onFog(v: Boolean)          { pendingWrites.add("fog"); _state.update { it.copy(fogEnabled        = v) }; save { repo.saveFog(v) } }
    fun onVibration(v: Boolean)    { pendingWrites.add("vibe"); _state.update { it.copy(vibrationOn       = v) }; save { repo.saveVibration(v) } }
    fun onShowFps(v: Boolean)      { pendingWrites.add("showFps"); _state.update { it.copy(showFps           = v) }; save { repo.saveShowFps(v) } }
    fun onShowPing(v: Boolean)     { pendingWrites.add("showPing"); _state.update { it.copy(showPing          = v) }; save { repo.saveShowPing(v) } }
    fun onCameraView(v: String)    { pendingWrites.add("camview"); _state.update { it.copy(cameraView = v) }; save { repo.saveCameraView(v) } }
    fun onColorBlind(v: String)    { pendingWrites.add("cb"); _state.update { it.copy(colorBlindMode    = v) }; save { repo.saveColorBlind(v) } }
    fun onPushNotif(v: Boolean)    { pendingWrites.add("push"); _state.update { it.copy(pushNotifications = v) }; save { repo.savePushNotif(v) } }

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
                        1 -> AudioTab(s, vm::onMusic, vm::onVibration)
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
    onVib     : (Boolean) -> Unit
) {
    SettingsSection(stringResource(R.string.settings_tab_audio))
    SettingsSlider(stringResource(R.string.audio_master_volume),      s.musicVolume,    onMusic)
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

@HiltViewModel
class UiEditorVM @Inject constructor(private val repo: SettingsRepository) : ViewModel() {
    val layout: StateFlow<Map<String, UiButtonLayout>> = repo.observeUiLayout()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun saveLayout(layout: List<UiButtonLayout>) {
        viewModelScope.launch { repo.saveUiLayout(layout) }
    }

    fun reset() { viewModelScope.launch { repo.resetUiLayout() } }
}

@Composable
fun UiEditor(onSave: () -> Unit, vm: UiEditorVM = hiltViewModel()) {
    val saved by vm.layout.collectAsState()

    // Every HUD element is editable, not just the action buttons — the status
    // bars, the pause control and the readout row were previously fixed in
    // place with no way to move or resize them.
    val elements = remember {
        mutableStateListOf(
            HudElement("bar_sanity",  R.string.game_hud_sanity,      0.10f, 0.08f, 150f, 22f),
            HudElement("bar_stamina", R.string.game_hud_stamina,     0.10f, 0.14f, 150f, 22f),
            HudElement("bar_battery", R.string.game_hud_battery,     0.10f, 0.20f, 150f, 22f),
            HudElement("readouts",   R.string.editor_btn_readouts,   0.78f, 0.07f, 120f, 30f),
            HudElement("pause",      R.string.editor_btn_pause,      0.95f, 0.07f, 40f,  40f),
            HudElement("joystick",   R.string.editor_btn_move,       0.14f, 0.74f, 140f, 140f),
            HudElement("interact",   R.string.editor_btn_interact,   0.90f, 0.80f, 62f,  62f),
            HudElement("flashlight", R.string.editor_btn_flashlight, 0.78f, 0.80f, 52f,  52f),
            HudElement("jump",       R.string.editor_btn_jump,       0.90f, 0.63f, 46f,  46f),
            HudElement("crouch",     R.string.editor_btn_crouch,     0.78f, 0.63f, 46f,  46f)
        )
    }

    // Apply anything already persisted, once the store has produced a value.
    LaunchedEffect(saved) {
        saved.forEach { (id, layout) ->
            elements.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let { i ->
                elements[i] = elements[i].copy(
                    normX = layout.offset.x, normY = layout.offset.y, scale = layout.sizeScale
                )
            }
        }
    }

    var selectedId by remember { mutableStateOf<String?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        CrtOverlay()

        Box(
            Modifier.fillMaxSize().onSizeChanged { canvasSize = it }
        ) {
            elements.forEachIndexed { index, el ->
                val selected = el.id == selectedId
                val w = el.baseW * el.scale
                val h = el.baseH * el.scale
                Box(
                    Modifier
                        .offset {
                            IntOffset(
                                (el.normX * canvasSize.width - w * density / 2f).toInt(),
                                (el.normY * canvasSize.height - h * density / 2f).toInt()
                            )
                        }
                        .size(w.dp, h.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) Yellow.copy(0.16f) else Color.Black.copy(0.55f))
                        .border(
                            if (selected) 2.dp else 1.dp,
                            if (selected) Yellow else YellowDim.copy(0.55f),
                            RoundedCornerShape(8.dp)
                        )
                        .pointerInput(el.id) {
                            detectDragGestures(
                                onDragStart = { selectedId = el.id },
                                onDrag = { change, drag ->
                                    change.consume()
                                    val cur = elements[index]
                                    // Normalised coordinates keep the layout valid
                                    // across screen sizes and orientations.
                                    elements[index] = cur.copy(
                                        normX = (cur.normX + drag.x / canvasSize.width.coerceAtLeast(1))
                                            .coerceIn(0.04f, 0.96f),
                                        normY = (cur.normY + drag.y / canvasSize.height.coerceAtLeast(1))
                                            .coerceIn(0.04f, 0.96f)
                                    )
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        androidx.compose.foundation.Canvas(
                            Modifier.size((w * 0.42f).coerceIn(16f, 44f).dp)
                        ) { editorGlyph(el.id, if (selected) Yellow else YellowDim) }
                        if (h > 34f) {
                            Text(
                                stringResource(el.labelRes),
                                color = if (selected) Yellow else TextDim,
                                fontSize = 8.sp, maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // --- Controls --------------------------------------------------------
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(0.88f))
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                stringResource(R.string.editor_hint),
                color = TextDim, fontSize = 10.sp, lineHeight = 14.sp
            )

            val sel = elements.firstOrNull { it.id == selectedId }
            if (sel != null) {
                val index = elements.indexOfFirst { it.id == sel.id }
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text(
                        "${stringResource(R.string.editor_selected)}: ${stringResource(sel.labelRes)}",
                        color = Yellow, fontSize = 11.sp, fontWeight = FontWeight.Bold
                    )
                    Text("${(sel.scale * 100).toInt()}%", color = CrtAmber, fontSize = 11.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.editor_size), color = TextSec, fontSize = 11.sp)
                    Spacer(Modifier.width(10.dp))
                    Slider(
                        value = sel.scale,
                        onValueChange = { elements[index] = elements[index].copy(scale = it) },
                        valueRange = 0.6f..1.8f,
                        colors = SliderDefaults.colors(
                            thumbColor = Yellow,
                            activeTrackColor = Yellow.copy(0.75f),
                            inactiveTrackColor = MetalBg
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AtmosphericButton(
                    label = stringResource(R.string.editor_reset),
                    icon = Icons.Default.Refresh,
                    accent = TextSec,
                    width = 150.dp, height = 44.dp,
                    onClick = {
                        vm.reset()
                        selectedId = null
                        // Restore the built-in positions immediately rather than
                        // waiting for the store to round-trip.
                        val defaults = listOf(
                            Triple("bar_sanity", 0.10f to 0.08f, 1f),
                            Triple("bar_stamina", 0.10f to 0.14f, 1f),
                            Triple("bar_battery", 0.10f to 0.20f, 1f),
                            Triple("readouts", 0.78f to 0.07f, 1f),
                            Triple("pause", 0.95f to 0.07f, 1f),
                            Triple("joystick", 0.14f to 0.74f, 1f),
                            Triple("interact", 0.90f to 0.80f, 1f),
                            Triple("flashlight", 0.78f to 0.80f, 1f),
                            Triple("jump", 0.90f to 0.63f, 1f),
                            Triple("crouch", 0.78f to 0.63f, 1f)
                        )
                        defaults.forEach { (id, pos, sc) ->
                            elements.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let { i ->
                                elements[i] = elements[i].copy(normX = pos.first, normY = pos.second, scale = sc)
                            }
                        }
                    }
                )
                AtmosphericButton(
                    label = stringResource(R.string.controls_save_exit),
                    icon = Icons.Default.Check,
                    accent = Yellow,
                    width = 170.dp, height = 44.dp,
                    onClick = {
                        vm.saveLayout(
                            elements.map { UiButtonLayout(it.id, Offset(it.normX, it.normY), it.scale) }
                        )
                        onSave()
                    },
                    isPrimary = true
                )
            }
        }
    }
}

/** One editable HUD element. Position is normalised (0..1 of the screen) so a
 *  layout stays correct on a different device or orientation. */
private data class HudElement(
    val id: String,
    val labelRes: Int,
    val normX: Float,
    val normY: Float,
    val baseW: Float,
    val baseH: Float,
    val scale: Float = 1f
)


/** Draws the same control glyphs the in-game HUD uses, so the layout editor is a
 *  true preview rather than a set of generic drag handles. Kept local to this
 *  file to avoid widening the HUD drawing API. */
private fun DrawScope.editorGlyph(id: String, c: Color) {
    val w = size.width; val h = size.height
    val sw = size.minDimension * 0.10f
    when (id) {
        "bar_sanity", "bar_stamina", "bar_battery" -> {
            // A miniature of the real status bar, so the preview matches the game.
            val r = h * 0.30f
            drawRoundRect(
                c.copy(0.25f), topLeft = Offset(w * 0.06f, h * 0.36f),
                size = Size(w * 0.88f, h * 0.28f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r)
            )
            drawRoundRect(
                c, topLeft = Offset(w * 0.06f, h * 0.36f),
                size = Size(w * 0.60f, h * 0.28f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r)
            )
        }
        "readouts" -> {
            drawRoundRect(
                c.copy(0.7f), topLeft = Offset(w * 0.08f, h * 0.34f),
                size = Size(w * 0.36f, h * 0.32f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.05f),
                style = Stroke(sw * 0.7f)
            )
            drawCircle(c, radius = w * 0.07f, center = Offset(w * 0.68f, h * 0.5f))
        }
        "pause" -> {
            val barW = w * 0.16f
            drawRoundRect(c, topLeft = Offset(w * 0.32f - barW / 2, h * 0.24f), size = Size(barW, h * 0.52f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW * 0.3f))
            drawRoundRect(c, topLeft = Offset(w * 0.68f - barW / 2, h * 0.24f), size = Size(barW, h * 0.52f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW * 0.3f))
        }
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
                    // The app tree is keyed on the language, so selecting one
                    // rebuilds every screen in place — no activity recreate.
                    .clickable { if (!sel) onSelect(tag) }
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
