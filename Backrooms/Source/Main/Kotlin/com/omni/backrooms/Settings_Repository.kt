package com.omni.backrooms

import androidx.compose.ui.geometry.Offset
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Settings_Repository @Inject constructor(
    private val store: DataStore<Preferences>
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
            cameraSensitivity = p[KEY_SENSITIVITY] ?: 1f
        )
    }

    suspend fun saveName(v: String)         { store.edit { it[KEY_NAME]        = v } }
    suspend fun saveQuality(v: String)      { store.edit { it[KEY_QUALITY]     = v } }
    suspend fun saveVhs(v: Boolean)         { store.edit { it[KEY_VHS]         = v } }
    suspend fun saveResolution(v: Float)    { store.edit { it[KEY_RESOLUTION]  = v } }
    suspend fun saveMusic(v: Float)         { store.edit { it[KEY_MUSIC]       = v } }
    suspend fun saveFootstep(v: Float)      { store.edit { it[KEY_FOOTSTEP]    = v } }
    suspend fun saveMonster(v: Float)       { store.edit { it[KEY_MONSTER]     = v } }
    suspend fun saveVoice(v: Float)         { store.edit { it[KEY_VOICE]       = v } }
    suspend fun saveSensitivity(v: Float)   { store.edit { it[KEY_SENSITIVITY] = v } }

    suspend fun saveUiLayout(layout: List<UiButtonLayout>) {
        store.edit { p ->
            layout.forEach { b ->
                p[floatPreferencesKey("ui_${b.buttonId}_x")] = b.offset.x
                p[floatPreferencesKey("ui_${b.buttonId}_y")] = b.offset.y
            }
        }
    }

    suspend fun connectGoogle() { }
}

    suspend fun loadUiLayout(): List<UiButtonLayout> {
        val prefs = store.data.first()
        val ids = listOf("joystick","sprint","interact","crouch","flashlight")
        return ids.mapNotNull { id ->
            val x = prefs[floatPreferencesKey("ui_${id}_x")] ?: return@mapNotNull null
            val y = prefs[floatPreferencesKey("ui_${id}_y")] ?: return@mapNotNull null
            UiButtonLayout(buttonId = id, offset = Offset(x, y))
        }
    }

    suspend fun clearAll() {
        store.edit { it.clear() }
    }

    fun observeVhsEnabled(): Flow<Boolean> = store.data.map { it[KEY_VHS] ?: true }
    fun observeGraphicsQuality(): Flow<String> = store.data.map { it[KEY_QUALITY] ?: "medium" }
    fun observeMusicVolume(): Flow<Float> = store.data.map { it[KEY_MUSIC] ?: 0.7f }
    fun observeVoiceVolume(): Flow<Float> = store.data.map { it[KEY_VOICE] ?: 0.8f }
