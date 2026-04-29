package com.omni.backrooms

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val playerName        : String  = "Wanderer",
    val graphicsQuality   : String  = "medium",
    val vhsEnabled        : Boolean = true,
    val resolutionScale   : Float   = 1f,
    val musicVolume       : Float   = 0.7f,
    val footstepVolume    : Float   = 0.8f,
    val monsterVolume     : Float   = 0.9f,
    val voiceVolume       : Float   = 0.8f,
    val cameraSensitivity : Float   = 1f
)

@HiltViewModel
class SettingsVM @Inject constructor(
    private val repo: Settings_Repository
) : ViewModel() {

    private val _s = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _s.asStateFlow()

    init {
        viewModelScope.launch {
            repo.observe().collect { g ->
                _s.update {
                    it.copy(
                        playerName        = g.playerName,
                        graphicsQuality   = g.graphicsQuality,
                        vhsEnabled        = g.vhsEnabled,
                        resolutionScale   = g.resolutionScale,
                        musicVolume       = g.musicVolume,
                        footstepVolume    = g.footstepVolume,
                        monsterVolume     = g.monsterVolume,
                        voiceVolume       = g.voiceVolume,
                        cameraSensitivity = g.cameraSensitivity
                    )
                }
            }
        }
    }

    fun onName(v: String)          { _s.update { it.copy(playerName = v) };           viewModelScope.launch { repo.saveName(v) } }
    fun onQuality(v: String)       { _s.update { it.copy(graphicsQuality = v) };      viewModelScope.launch { repo.saveQuality(v) } }
    fun onVhs(v: Boolean)          { _s.update { it.copy(vhsEnabled = v) };           viewModelScope.launch { repo.saveVhs(v) } }
    fun onResolution(v: Float)     { _s.update { it.copy(resolutionScale = v) };      viewModelScope.launch { repo.saveResolution(v) } }
    fun onMusic(v: Float)          { _s.update { it.copy(musicVolume = v) };          viewModelScope.launch { repo.saveMusic(v) } }
    fun onFootstep(v: Float)       { _s.update { it.copy(footstepVolume = v) };       viewModelScope.launch { repo.saveFootstep(v) } }
    fun onMonster(v: Float)        { _s.update { it.copy(monsterVolume = v) };        viewModelScope.launch { repo.saveMonster(v) } }
    fun onVoice(v: Float)          { _s.update { it.copy(voiceVolume = v) };          viewModelScope.launch { repo.saveVoice(v) } }
    fun onSensitivity(v: Float)    { _s.update { it.copy(cameraSensitivity = v) };   viewModelScope.launch { repo.saveSensitivity(v) } }
    fun onGoogleConnect()          { viewModelScope.launch { repo.connectGoogle() } }
}

private enum class STab(val labelRes: Int, val icon: ImageVector) {
    Account (R.string.settings_tab_account,  Icons.Default.Person),
    Graphics(R.string.settings_tab_graphics, Icons.Default.Tune),
    Audio   (R.string.settings_tab_audio,    Icons.Default.VolumeUp),
    Controls(R.string.settings_tab_controls, Icons.Default.SportsEsports)
}

@Composable
fun SettingsScreen(
    onUiEditor : () -> Unit,
    onBack     : () -> Unit,
    vm         : SettingsVM = hiltViewModel()
) {
    val s   by vm.state.collectAsState()
    var tab by remember { mutableStateOf(STab.Account) }

    Box(Modifier.fillMaxSize().background(Color(0xFF050503))) {
        Crt_Overlay()
        Column(Modifier.fillMaxSize()) {

            Row(
                Modifier.fillMaxWidth().background(Color.Black.copy(0.6f)).padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Yellow)
                }
                Text(
                    stringResource(R.string.settings_title),
                    color = Yellow, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp
                )
            }

            Divider_Line()

            Row(
                Modifier.fillMaxWidth().background(MetalBg.copy(0.5f)).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                        Icon(t.icon, null,
                            tint = if (tab == t) Yellow else TextDim,
                            modifier = Modifier.size(18.dp))
                        Text(stringResource(t.labelRes),
                            color = if (tab == t) Yellow else TextDim,
                            fontSize = 10.sp,
                            fontWeight = if (tab == t) FontWeight.Bold else FontWeight.Normal,
                            letterSpacing = 1.sp)
                        if (tab == t) Box(Modifier.width(24.dp).height(2.dp).background(Yellow))
                    }
                }
            }

            Divider_Line()

            Box(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                when (tab) {
                    STab.Account  -> Account_Tab(s, vm::onName, vm::onGoogleConnect)
                    STab.Graphics -> Graphics_Tab(s, vm::onQuality, vm::onVhs, vm::onResolution)
                    STab.Audio    -> Audio_Tab(s, vm::onMusic, vm::onFootstep, vm::onMonster, vm::onVoice)
                    STab.Controls -> Controls_Tab(s, vm::onSensitivity, onUiEditor)
                }
            }
        }
    }
}

@Composable
private fun Account_Tab(s: SettingsState, onName: (String) -> Unit, onGoogle: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        S_Label(stringResource(R.string.account_change_name))
        Omni_Panel(Modifier.fillMaxWidth()) {
            androidx.compose.foundation.text.BasicTextField(
                value = s.playerName,
                onValueChange = onName,
                singleLine = true,
                textStyle = TextStyle(color = Yellow, fontSize = 13.sp),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Yellow),
                decorationBox = { inner ->
                    if (s.playerName.isEmpty())
                        Text(stringResource(R.string.account_name_hint), color = TextDim, fontSize = 12.sp)
                    inner()
                }
            )
        }
        Spacer(Modifier.height(4.dp))
        Omni_Button(
            text   = stringResource(R.string.account_google_connect),
            onClick = onGoogle,
            width  = 240.dp,
            height = 44.dp,
            accent = Color(0xFF4285F4)
        )
    }
}

@Composable
private fun Graphics_Tab(
    s            : SettingsState,
    onQuality    : (String) -> Unit,
    onVhs        : (Boolean) -> Unit,
    onResolution : (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        S_Label(stringResource(R.string.graphics_quality_label))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                Triple(R.string.graphics_quality_low,    "low",    Color(0xFF4CAF50)),
                Triple(R.string.graphics_quality_medium, "medium", Yellow),
                Triple(R.string.graphics_quality_high,   "high",   DangerRed)
            ).forEach { (res, key, col) ->
                val sel = s.graphicsQuality == key
                Box(
                    Alignment.Center,
                    Modifier
                        .weight(1f).height(40.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (sel) col.copy(0.15f) else MetalBg)
                        .border(1.dp, if (sel) col.copy(0.6f) else BorderCol, RoundedCornerShape(2.dp))
                        .clickable { onQuality(key) }
                ) {
                    Text(stringResource(res),
                        color = if (sel) col else TextDim,
                        fontSize = 11.sp,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.graphics_vhs_effect), color = TextSec, fontSize = 12.sp)
            Switch(
                checked  = s.vhsEnabled,
                onCheckedChange = onVhs,
                colors   = SwitchDefaults.colors(
                    checkedThumbColor   = Yellow,
                    checkedTrackColor   = YellowDim,
                    uncheckedThumbColor = TextDim,
                    uncheckedTrackColor = MetalBg
                )
            )
        }
        S_Slider(stringResource(R.string.graphics_resolution_label), s.resolutionScale, onResolution, 0.5f..1f)
    }
}

@Composable
private fun Audio_Tab(
    s            : SettingsState,
    onMusic      : (Float) -> Unit,
    onFootstep   : (Float) -> Unit,
    onMonster    : (Float) -> Unit,
    onVoice      : (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        S_Slider(stringResource(R.string.audio_music_volume),       s.musicVolume,    onMusic)
        S_Slider(stringResource(R.string.audio_footstep_volume),    s.footstepVolume, onFootstep)
        S_Slider(stringResource(R.string.audio_monster_sfx_volume), s.monsterVolume,  onMonster)
        S_Slider(stringResource(R.string.audio_voice_volume),       s.voiceVolume,    onVoice)
    }
}

@Composable
private fun Controls_Tab(
    s              : SettingsState,
    onSensitivity  : (Float) -> Unit,
    onUiEditor     : () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        S_Slider(stringResource(R.string.controls_camera_sensitivity), s.cameraSensitivity, onSensitivity, 0.1f..3f)
        Spacer(Modifier.height(4.dp))
        Omni_Button(
            text   = stringResource(R.string.controls_ui_layout),
            onClick = onUiEditor,
            width  = 240.dp,
            height = 48.dp
        )
    }
}

@Composable
private fun S_Label(text: String) {
    Text(text, color = TextSec, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun S_Slider(
    label        : String,
    value        : Float,
    onValueChange: (Float) -> Unit,
    range        : ClosedFloatingPointRange<Float> = 0f..1f
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = TextSec, fontSize = 12.sp)
            Text("${(value * 100).toInt()}%", color = Yellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value         = value,
            onValueChange = onValueChange,
            valueRange    = range,
            colors        = SliderDefaults.colors(
                thumbColor          = Yellow,
                activeTrackColor    = Yellow,
                inactiveTrackColor  = MetalBg
            )
        )
    }
}
