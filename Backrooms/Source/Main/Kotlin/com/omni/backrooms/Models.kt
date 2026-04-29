package com.omni.backrooms

import androidx.compose.ui.geometry.Offset

package com.omni.backrooms

import androidx.compose.ui.geometry.Offset

data class PlayerProfile(
    val id            : String  = "",
    val name          : String  = "Wanderer",
    val level         : Int     = 1,
    val xp            : Long    = 0L,
    val xpToNext      : Long    = 1000L,
    val xpProgress    : Float   = 0f,
    val avatarUrl     : String? = null,
    val omniumAmount  : Long    = 0L,
    val souliumAmount : Long    = 0L,
    val isVip         : Boolean = false
)

data class RoomInfo(
    val id            : String,
    val name          : String,
    val hostId        : String,
    val currentPlayers: Int,
    val maxPlayers    : Int,
    val difficulty    : String,
    val isLocked      : Boolean,
    val language      : String,
    val ping          : Int = 0
)

data class RoomPage(
    val rooms : List<RoomInfo>,
    val total : Int
)

data class GameSettings(
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

data class UiButtonLayout(
    val buttonId  : String,
    val offset    : Offset,
    val sizeScale : Float = 1f
)

data class GameSession(
    val roomId      : String,
    val difficulty  : String,
    val isOnline    : Boolean,
    val playerCount : Int  = 1,
    val currentLevel: Int  = 0,
    val seed        : Long = System.currentTimeMillis()
)

data class MarketItem(
    val id         : String,
    val nameKey    : String,
    val priceLabel : String,
    val currency   : String,
    val isOwned    : Boolean = false
)

import androidx.compose.ui.geometry.Offset

data class NetworkPlayerState(
    val peerId     : Int,
    val posX       : Float,
    val posY       : Float,
    val posZ       : Float,
    val yaw        : Float,
    val pitch      : Float,
    val ping       : Int    = 0,
    val isConnected: Boolean = true
)

data class LevelSegment(
    val posX           : Float,
    val posY           : Float,
    val width          : Float,
    val length         : Float,
    val height         : Float,
    val lightPhase     : Float,
    val lightIntensity : Float,
    val lightSpeed     : Float,
    val lightBroken    : Boolean,
    val roomType       : Int,
    val wallDamage     : Float,
    val moisture       : Float,
    val hasHazard      : Boolean,
    val decalCount     : Int
)

data class EntityState(
    val id            : Int,
    val posX          : Float,
    val posY          : Float,
    val posZ          : Float,
    val aiState       : Int,
    val alertLevel    : Float,
    val hpFraction    : Float,
    val flickerInfluence: Float,
    val typeId        : Int,
    val isActive      : Boolean
)

data class CameraState(
    val posX       : Float,
    val posY       : Float,
    val posZ       : Float,
    val yaw        : Float,
    val pitch      : Float,
    val roll       : Float,
    val fov        : Float,
    val bobAmount  : Float,
    val bobPhase   : Float
)

data class GuardStatus(
    val flags           : Int,
    val isRooted        : Boolean,
    val isFrida         : Boolean,
    val isDebugged      : Boolean,
    val isEmulator      : Boolean,
    val isSignatureValid: Boolean,
    val report          : String
) {
    val isThreatDetected: Boolean get() = flags != 0
    val threatLevel: String get() = when {
        isFrida || isDebugged        -> "CRITICAL"
        isRooted                     -> "HIGH"
        isEmulator                   -> "MEDIUM"
        !isSignatureValid            -> "HIGH"
        else                         -> "CLEAN"
    }
}

data class GameState(
    val level          : Int     = 0,
    val seed           : Long    = 0L,
    val difficulty     : String  = "normal",
    val isOnline       : Boolean = false,
    val playerHp       : Float   = 100f,
    val playerMaxHp    : Float   = 100f,
    val sanity         : Float   = 100f,
    val stamina        : Float   = 100f,
    val flashlightOn   : Boolean = true,
    val flashlightBattery: Float = 1f,
    val sessionElapsed : Long    = 0L,
    val entitiesNearby : Int     = 0,
    val flickerIntensity: Float  = 0f
)

data class VoiceFrame(
    val peerId : Int,
    val pcmData: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as VoiceFrame
        return peerId == other.peerId && pcmData.contentEquals(other.pcmData)
    }
    override fun hashCode(): Int = 31 * peerId + pcmData.contentHashCode()
}

data class ChatMessage(
    val senderId  : Int,
    val senderName: String,
    val text      : String,
    val timestampMs: Long = System.currentTimeMillis()
)

data class LeaderboardEntry(
    val rank      : Int,
    val playerName: String,
    val level     : Int,
    val score     : Long,
    val survived  : Int
)
