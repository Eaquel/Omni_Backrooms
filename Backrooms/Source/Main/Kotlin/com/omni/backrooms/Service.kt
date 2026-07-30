package com.omni.backrooms

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.util.Log
import android.graphics.Bitmap
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Process
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
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
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.firestore.SetOptions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import retrofit2.http.*
import javax.inject.Inject
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Singleton

class NativeBridge @Inject constructor() {
    external fun initCore(seed: Long)
    external fun getFlicker(phase: Float, t: Float, broken: Boolean): Float
    external fun generateLevel(count: Int, depth: Int): FloatArray?
    external fun getMoistureAt(x: Float, y: Float): Float
    external fun applyVhs(bitmap: Bitmap, t: Float, intensity: Float): Boolean
    external fun applyFlicker(bitmap: Bitmap, value: Float)
    external fun physicsTick(dt: Float)
    external fun applyMovement(fx: Float, fy: Float, fz: Float)
    external fun cameraLook(dx: Float, dy: Float, sensitivity: Float)
    external fun getCameraState(): FloatArray?
    external fun destroyCore()
    external fun initSound(): Boolean
    external fun setMasterVolume(v: Float)
    external fun setHumVolume(v: Float)
    external fun setFootstepVolume(v: Float)
    external fun setMonsterVolume(v: Float)
    external fun setAmbienceLevel(v: Float)
    external fun triggerFootstep(bpm: Float, surface: Float)
    external fun triggerMonster(intensity: Float)
    external fun stopMonster()
    external fun setListenerPos(x: Float, y: Float, z: Float)
    external fun setSpatialRolloff(ref: Float, maxDist: Float)
    external fun destroySound()
    external fun initEntities()
    external fun spawnEntity(x: Float, y: Float, z: Float, speed: Float, hear: Float, sight: Float, aggro: Float, typeId: Int): Int
    external fun tickEntities(px: Float, py: Float, pz: Float, dt: Float): FloatArray?
    external fun damageEntity(id: Int, amount: Float)
    external fun getTotalFlickerInfluence(): Float
    external fun destroyEntities()
    external fun initSocket(port: Int): Boolean
    external fun buildPosPacket(x: Float, y: Float, z: Float, yaw: Float, pitch: Float): ByteArray?
    external fun buildPingPacket(): ByteArray?
    external fun buildVoicePacket(pcmData: ByteArray, pcmLen: Int): ByteArray?
    external fun drainRecvQueue(): Array<ByteArray>?
    external fun getLocalPing(): Int
    external fun getPeerCount(): Int
    external fun setLocalId(id: Int)
    external fun nowMs(): Long
    external fun destroySocket()
    external fun initGuard(ctx: Any, expectedSigHash: String): Boolean
    external fun getGuardFlags(): Int
    external fun runGuardScan(): Int
    external fun isRooted(): Boolean
    external fun isFridaDetected(): Boolean
    external fun isDebugged(): Boolean
    external fun isEmulator(): Boolean
    external fun isSignatureValid(): Boolean
    external fun getThreatReport(): String
    external fun destroyGuard()
}

interface ApiService {
    @GET("rooms")
    suspend fun getRooms(
        @Query("q") query: String?,
        @Query("locked") locked: Boolean?,
        @Query("lang") language: String?,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int
    ): RoomPage

    @POST("rooms")
    suspend fun createRoom(@Body body: CreateRoomRequest): CreateRoomResponse

    @POST("rooms/{id}/join")
    suspend fun joinRoom(@Path("id") roomId: String, @Query("password") password: String?): JoinRoomResponse

    @DELETE("rooms/{id}")
    suspend fun deleteRoom(@Path("id") roomId: String): BaseResponse

    @GET("rooms/{id}")
    suspend fun getRoomDetail(@Path("id") roomId: String): RoomDetail

    @POST("rooms/{id}/kick/{peerId}")
    suspend fun kickPlayer(@Path("id") roomId: String, @Path("peerId") peerId: Int): BaseResponse

    @GET("player/profile")
    suspend fun getProfile(): PlayerProfile

    @PUT("player/profile")
    suspend fun updateProfile(@Body profile: PlayerProfile): PlayerProfile

    @PUT("player/avatar")
    suspend fun updateAvatar(@Body body: AvatarRequest): PlayerProfile

    @POST("player/currency/purchase")
    suspend fun purchaseCurrency(@Body body: PurchaseRequest): PurchaseResponse

    @GET("leaderboard")
    suspend fun getLeaderboard(
        @Query("page") page: Int = 0,
        @Query("pageSize") size: Int = 50,
        @Query("difficulty") difficulty: String? = null,
        @Query("region") region: String? = null
    ): LeaderboardPage

    @POST("player/score")
    suspend fun submitScore(@Body body: ScoreSubmitRequest): BaseResponse

    @POST("auth/google")
    suspend fun loginWithGoogle(@Body body: GoogleAuthRequest): AuthResponse

    @POST("auth/refresh")
    suspend fun refreshToken(@Body body: RefreshRequest): AuthResponse

    @POST("auth/logout")
    suspend fun logout(): BaseResponse

    @GET("market/items")
    suspend fun getMarketItems(@Query("category") category: String?, @Query("page") page: Int = 0): MarketPage

    @POST("market/buy")
    suspend fun buyItem(@Body body: BuyRequest): BuyResponse

    @GET("market/daily")
    suspend fun getDailyDeals(): List<MarketItemDto>

    @GET("characters")
    suspend fun getCharacters(): List<CharacterDto>

    @POST("characters/{id}/equip")
    suspend fun equipCharacter(@Path("id") charId: String): BaseResponse

    @POST("characters/{id}/unlock")
    suspend fun unlockCharacter(@Path("id") charId: String): BaseResponse

    @GET("story/chapters")
    suspend fun getStoryChapters(): List<StoryChapterDto>

    @PUT("player/settings")
    suspend fun syncSettings(@Body body: GameSettings): BaseResponse

    @GET("player/settings")
    suspend fun fetchSettings(): GameSettings

    @GET("events/active")
    suspend fun getActiveEvents(): List<EventDto>

    @POST("events/{id}/join")
    suspend fun joinEvent(@Path("id") eventId: String): BaseResponse

    @POST("report/player")
    suspend fun reportPlayer(@Body body: ReportRequest): BaseResponse
}

data class PlayerProfile(
    val id            : String  = "",
    val name          : String  = "Wanderer",
    val level         : Int     = 1,
    val xp            : Long    = 0L,
    val xpToNext      : Long    = 1_000L,
    val xpProgress    : Float   = 0f,
    val avatarUrl     : String? = null,
    val avatarId      : String  = "default",
    val omniumAmount  : Long    = 0L,
    val souliumAmount : Long    = 0L,
    val isVip         : Boolean = false,
    val vipExpiresMs  : Long    = 0L,
    val totalGames    : Int     = 0,
    val totalSurvived : Int     = 0,
    val highScore     : Long    = 0L,
    val equippedCharId: String  = "wanderer",
    val region        : String  = "TR",
    val createdAtMs   : Long    = System.currentTimeMillis()
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
    val mapId         : String = "level_0",
    val ping          : Int    = 0
)

data class RoomPage(val rooms: List<RoomInfo>, val total: Int)

data class GameSettings(
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

data class UiButtonLayout(val buttonId: String, val offset: Offset, val sizeScale: Float = 1f)

/** Stages of the arrival sequence: the player falls in from above, hits the
 *  floor, then rises to standing before regaining control. */
enum class SpawnPhase { FALLING, LANDED, READY }

data class GameState(
    val level             : Int     = 0,
    val seed              : Long    = 0L,
    val difficulty        : String  = "normal",
    val isOnline          : Boolean = false,
    val playerHp          : Float   = 100f,
    val playerMaxHp       : Float   = 100f,
    val sanity            : Float   = 100f,
    val stamina           : Float   = 100f,
    val staminaMax        : Float   = 100f,
    val flashlightOn      : Boolean = true,
    val flashlightBattery : Float   = 1f,
    val sessionElapsed    : Long    = 0L,
    val entitiesNearby    : Int     = 0,
    val flickerIntensity  : Float   = 0f,
    val score             : Long    = 0L,
    val kills             : Int     = 0,
    val mapId             : String  = "level_0",
    val isPaused          : Boolean = false,
    val isGameOver        : Boolean = false,
    val isEscaped         : Boolean = false,
    val camera            : CameraSnapshot?      = null,
    val entities          : List<EntityState>    = emptyList(),
    val grid              : GridLevelData = GridLevelData.EMPTY,
    val exitX             : Float   = 0f,
    val exitZ             : Float   = 0f,
    val distanceToExit    : Float   = Float.MAX_VALUE,
    val spawnPhase        : SpawnPhase = SpawnPhase.READY,
    /** Live telemetry, measured rather than faked. */
    val fps               : Int     = 0,
    val pingMs            : Int     = 0,
    /** Vertical camera offset in metres, used by the arrival sequence to drop
     *  the view to the floor on impact and raise it back to standing. */
    val eyeOffset         : Float   = 0f
)

data class LeaderboardEntry(
    val rank       : Int,
    val playerId   : Int,
    val playerName : String,
    val avatarUrl  : String?,
    val level      : Int,
    val score      : Long,
    val survived   : Int,
    val difficulty : String,
    val region     : String = "TR"
)

data class ChatMessage(
    val senderId   : Int,
    val senderName : String,
    val text       : String,
    val timestampMs: Long = System.currentTimeMillis()
)

data class NetworkPlayerState(
    val peerId      : Int,
    val posX        : Float,
    val posY        : Float,
    val posZ        : Float,
    val yaw         : Float,
    val pitch       : Float,
    val animState   : Int     = 0,
    val hp          : Float   = 100f,
    val ping        : Int     = 0,
    val isConnected : Boolean = true,
    val charId      : String  = "wanderer"
)

data class CameraSnapshot(
    val posX     : Float,
    val posY     : Float,
    val posZ     : Float,
    val yaw      : Float,
    val pitch    : Float,
    val roll     : Float,
    val fov      : Float,
    val bobAmount: Float,
    val bobPhase : Float
) {
    companion object {
        fun fromFloatArray(data: FloatArray?): CameraSnapshot? {
            if (data == null || data.size < 9) return null
            return CameraSnapshot(data[0], data[1], data[2], data[3], data[4], data[5], data[6], data[7], data[8])
        }
    }
}

data class EntityState(
    val id              : Int,
    val posX            : Float,
    val posY            : Float,
    val posZ            : Float,
    val aiState         : Int,
    val alertLevel      : Float,
    val hpFraction      : Float,
    val flickerInfluence: Float,
    val playerInSight   : Boolean,
    val typeId          : Int,
    val isActive        : Boolean
) {
    companion object {
        const val FLOATS_PER_ENTITY = 10

        fun fromFloatArray(data: FloatArray, index: Int, id: Int): EntityState? {
            val base = index * FLOATS_PER_ENTITY
            if (base + (FLOATS_PER_ENTITY - 1) >= data.size) return null
            return EntityState(
                id, data[base], data[base+1], data[base+2], data[base+3].toInt(),
                data[base+4], data[base+5], data[base+6], data[base+7] > 0.5f,
                data[base+8].toInt(), data[base+9] > 0.5f
            )
        }

        fun listFromFloatArray(data: FloatArray?): List<EntityState> {
            if (data == null || data.isEmpty()) return emptyList()
            val count = data.size / FLOATS_PER_ENTITY
            return (0 until count).mapNotNull { fromFloatArray(data, it, it) }
        }
    }
}

data class SessionStats(
    val sessionId    : String,
    val startMs      : Long,
    val endMs        : Long    = 0L,
    val difficulty   : String,
    val mapId        : String,
    val finalScore   : Long    = 0L,
    val survived     : Boolean = false,
    val kills        : Int     = 0,
    val levelsReached: Int     = 0,
    val peakSanity   : Float   = 100f,
    val lowestHp     : Float   = 100f,
    val totalDistance: Float   = 0f
)

data class CreateRoomRequest(val name: String, val maxPlayers: Int, val difficulty: String, val password: String?, val language: String = "TR", val mapId: String = "level_0")
data class CreateRoomResponse(val roomId: String, val joinCode: String, val success: Boolean)
data class JoinRoomResponse(val success: Boolean, val roomId: String?, val error: String?)
data class RoomDetail(val id: String, val name: String, val hostId: String, val currentPlayers: Int, val maxPlayers: Int, val difficulty: String, val isLocked: Boolean, val language: String, val mapId: String, val players: List<RoomPlayer>)
data class RoomPlayer(val id: Int, val name: String, val avatarUrl: String?, val isHost: Boolean, val isReady: Boolean, val ping: Int)
data class BaseResponse(val success: Boolean, val message: String?)
data class AvatarRequest(val avatarId: String)
data class PurchaseRequest(val itemType: String, val amount: Int)
data class PurchaseResponse(val success: Boolean, val newBalance: Long, val currency: String)
data class LeaderboardPage(val entries: List<LeaderboardEntry>, val total: Int, val myRank: Int?)
data class ScoreSubmitRequest(val level: Int, val score: Long, val survived: Int, val difficulty: String, val sessionMs: Long, val kills: Int = 0)
data class GoogleAuthRequest(val idToken: String)
data class RefreshRequest(val refreshToken: String)
data class AuthResponse(val accessToken: String, val refreshToken: String, val expiresIn: Long, val playerId: Int)
data class MarketPage(val items: List<MarketItemDto>, val total: Int)
data class MarketItemDto(val id: String, val nameTr: String, val nameEn: String, val descTr: String, val descEn: String, val category: String, val price: Long, val currency: String, val imageUrl: String?, val isOwned: Boolean, val isEquipped: Boolean, val isLimited: Boolean, val expiresMs: Long?)
data class BuyRequest(val itemId: String, val currency: String)
data class BuyResponse(val success: Boolean, val newBalance: Long, val error: String?)
data class CharacterDto(val id: String, val nameTr: String, val nameEn: String, val clazz: String, val maxHp: Float, val baseSpeed: Float, val stealthMult: Float, val staminaMult: Float, val abilities: List<String>, val isUnlocked: Boolean, val isEquipped: Boolean, val imageUrl: String?, val price: Long, val currency: String)
data class StoryChapterDto(val id: Int, val titleTr: String, val titleEn: String, val contentTr: String, val contentEn: String, val isUnlocked: Boolean)

/** Picks the chapter text matching the device locale, falling back to Turkish
 *  (this app's default locale — see values/strings.xml) for anything else. */
// NOTE ON NAMING: the "Tr" slots hold the *localised* text for whatever language
// the story was loaded in (Turkish, Spanish, Russian or German) and the "En"
// slots hold the English original. The names predate multi-language support; the
// behaviour below is what matters — English locales read the English slot, every
// other locale reads the localised one, which AssetManager already filled from
// the right file with per-chapter English fallback.
val StoryChapterDto.displayTitle: String
    get() = if (java.util.Locale.getDefault().language == "en") titleEn else titleTr
val StoryChapterDto.displayContent: String
    get() = if (java.util.Locale.getDefault().language == "en") contentEn else contentTr
data class EventDto(val id: String, val titleTr: String, val titleEn: String, val descriptionTr: String, val descriptionEn: String, val rewardType: String, val rewardAmount: Long, val endMs: Long, val isActive: Boolean)
data class ReportRequest(val reportedId: Int, val reason: String, val details: String)

class RoomRepository @Inject constructor(private val api: ApiService) {
    suspend fun fetchRooms(query: String?, locked: Boolean?, lang: String?, page: Int, pageSize: Int): RoomPage =
        api.getRooms(query, locked, lang, page, pageSize)

    suspend fun createRoom(name: String, maxPlayers: Int, difficulty: String, password: String?): String =
        api.createRoom(CreateRoomRequest(name, maxPlayers, difficulty, password)).roomId
}

// ============================================================================
// Shared simulation helpers. GameVM (the active, Compose-lifecycle-bound
// gameplay path) and SessionService (a foreground-notification-capable host,
// currently unbound but kept ready for a future "survive backgrounding" mode))
// both drive the same native engine, so they must derive state identically.
// These are pure functions with no owner-specific state, so there is exactly
// one copy of "how entities spawn" and "how a tick affects HP/sanity/battery"
// in the whole app.
// ============================================================================

/** A random walkable cell, kept a minimum distance from the player's arrival
 *  point so nothing is standing on top of them the moment they land. */
private fun pickOpenPoint(grid: GridLevelData, minDistFromSpawn: Float): Pair<Float, Float>? {
    if (grid.isEmpty) return null
    repeat(200) {
        val cx = (Math.random() * grid.dim).toInt().coerceIn(0, grid.dim - 1)
        val cz = (Math.random() * grid.dim).toInt().coerceIn(0, grid.dim - 1)
        if (grid.isSolid(cx, cz)) return@repeat
        val wx = grid.worldX(cx) + grid.cellSize * 0.5f
        val wz = grid.worldZ(cz) + grid.cellSize * 0.5f
        val dx = wx - grid.spawnX; val dz = wz - grid.spawnZ
        if (dx * dx + dz * dz >= minDistFromSpawn * minDistFromSpawn) return wx to wz
    }
    return null
}

/** Spawns cfg.count entities across the level's real walkable space, cycling
 *  through every lore creature with its correct native AI id. */
fun spawnInitialEntities(bridge: NativeBridge, grid: GridLevelData, cfg: SpawnConfig) {
    if (grid.isEmpty) return
    repeat(cfg.count) { i ->
        val entity = EntityType.entries[i % EntityType.entries.size]
        val (sx, sz) = pickOpenPoint(grid, minDistFromSpawn = 22f) ?: return@repeat
        bridge.spawnEntity(
            x = sx, y = 0f, z = sz,
            speed = entity.baseSpeed * cfg.speedMult,
            hear  = entity.hearRange,
            sight = entity.sightRange * cfg.sightMult,
            aggro = entity.aggroRange, typeId = entity.nativeAiId
        )
    }
}

/** One periodic re-spawn, used by both hosts' entity-spawner loop. */
fun spawnOneRandomEntity(bridge: NativeBridge, grid: GridLevelData, cfg: SpawnConfig) {
    val (sx, sz) = pickOpenPoint(grid, minDistFromSpawn = 14f) ?: return
    val entity = EntityType.entries[(Math.random() * EntityType.entries.size).toInt().coerceIn(0, EntityType.entries.lastIndex)]
    bridge.spawnEntity(
        x = sx, y = 0f, z = sz,
        speed = entity.baseSpeed * cfg.speedMult,
        hear  = entity.hearRange, sight = entity.sightRange * cfg.sightMult,
        aggro = entity.aggroRange, typeId = entity.nativeAiId
    )
}

/** Everything one physics/AI tick derives from the native engine, before it's
 *  folded into a GameState. Kept separate from GameState itself so it can be
 *  computed once and applied identically regardless of which host owns it. */
data class TickDerived(
    val camera     : CameraSnapshot?,
    val entities   : List<EntityState>,
    val flicker    : Float,
    val nearbyCount: Int,
    val damage     : Float
)

/** Advances the native sim by [dt] and reads back camera/entity state. Do not
 *  call this from more than one place per logical frame — physicsTick/
 *  tickEntities mutate native state, so calling it twice per frame from two
 *  hosts at once would double-advance the simulation. */
fun stepSimulation(bridge: NativeBridge, dt: Float): TickDerived {
    bridge.physicsTick(dt)
    val cam = CameraSnapshot.fromFloatArray(bridge.getCameraState())
    if (cam != null) bridge.setListenerPos(cam.posX, cam.posY, cam.posZ)
    val entityList = EntityState.listFromFloatArray(
        bridge.tickEntities(cam?.posX ?: 0f, cam?.posY ?: 0f, cam?.posZ ?: 0f, dt)
    )
    val flicker = bridge.getTotalFlickerInfluence()
    val nearbyCount = entityList.count { e ->
        if (!e.isActive || cam == null) return@count false
        val dx = e.posX - cam.posX; val dz = e.posZ - cam.posZ
        dx * dx + dz * dz < 625f // within 25 units
    }
    // Entities in an Attack state (aiState 4) that reach melee range hurt the player.
    var damage = 0f
    if (cam != null) {
        for (e in entityList) {
            if (!e.isActive || e.aiState != 4) continue
            val dx = e.posX - cam.posX; val dz = e.posZ - cam.posZ
            if (dx * dx + dz * dz < 2.25f) damage += 16f * dt
        }
    }
    return TickDerived(cam, entityList, flicker, nearbyCount, damage)
}

/** Folds one tick's derived stats into a GameState using the single shared
 *  sanity/battery/HP/exit-distance formula set. */
fun applyTickToState(s: GameState, derived: TickDerived, dt: Float, elapsedMs: Long, score: Long): GameState {
    val drain = (derived.nearbyCount * 0.6f + derived.flicker * 2f) * dt
    val regen = if (derived.nearbyCount == 0 && derived.flicker < 0.1f) dt * 0.3f else 0f
    val nb    = (s.flashlightBattery - (if (s.flashlightOn) dt * 0.006f else 0f)).coerceAtLeast(0f)
    val newHp = (s.playerHp - derived.damage).coerceIn(0f, s.playerMaxHp)
    val cam   = derived.camera
    val exitDist = if (cam != null)
        kotlin.math.hypot((s.exitX - cam.posX).toDouble(), (s.exitZ - cam.posZ).toDouble()).toFloat()
    else s.distanceToExit
    return s.copy(
        sessionElapsed    = elapsedMs,
        flickerIntensity  = derived.flicker,
        entitiesNearby    = derived.nearbyCount,
        score             = score,
        sanity            = (s.sanity - drain + regen).coerceIn(0f, 100f),
        flashlightBattery = nb,
        flashlightOn      = if (!s.flashlightOn) false else nb > 0f,
        stamina           = (s.stamina + dt * 8f).coerceAtMost(s.staminaMax),
        playerHp          = newHp,
        isGameOver        = newHp <= 0f || s.isGameOver,
        camera            = cam ?: s.camera,
        entities          = derived.entities,
        distanceToExit    = exitDist
    )
}

@AndroidEntryPoint
class SessionService : Service() {

    inner class LocalBinder : Binder() { fun get(): SessionService = this@SessionService }

    @Inject lateinit var bridge      : NativeBridge
    @Inject lateinit var assetManager: AssetManager
    @Inject lateinit var api         : ApiService

    private val binder = LocalBinder()
    private val scope  = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _gameState      = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _chatMessages   = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _networkPlayers = MutableStateFlow<List<NetworkPlayerState>>(emptyList())
    val networkPlayers: StateFlow<List<NetworkPlayerState>> = _networkPlayers.asStateFlow()

    private var physicsJob: Job? = null
    private var entityJob : Job? = null
    private var networkJob: Job? = null
    private var scoreJob  : Job? = null

    private var lastTickMs = 0L
    private var elapsedMs  = 0L
    private var score      = 0L
    private var kills      = 0
    /** Segments for the currently loaded level, kept alongside gameState the same
     *  way GameVM keeps its own copy (see stepSimulation/spawnInitialEntities). */
    private var grid: GridLevelData = GridLevelData.EMPTY

    companion object {
        private const val CHANNEL_ID    = "omni_session"
        private const val NOTIF_ID      = 2001
        const val ACTION_START_OFFLINE  = "start_offline"
        const val ACTION_START_ONLINE   = "start_online"
        const val ACTION_STOP           = "stop_game"
        const val ACTION_PAUSE          = "pause_game"
        const val ACTION_RESUME         = "resume_game"
        const val ACTION_FLASHLIGHT     = "flashlight"
        const val ACTION_DAMAGE_ENTITY  = "damage_entity"
        const val EXTRA_DIFFICULTY      = "difficulty"
        const val EXTRA_ROOM_ID         = "room_id"
        const val EXTRA_SEED            = "seed"
        const val EXTRA_MAP_ID          = "map_id"
        const val EXTRA_ENTITY_ID       = "entity_id"
        const val EXTRA_DAMAGE          = "damage"
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        ServiceCompat.startForeground(
            this,
            NOTIF_ID,
            buildNotif(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            else 0
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_OFFLINE -> {
                val diff  = intent.getStringExtra(EXTRA_DIFFICULTY) ?: "normal"
                val seed  = intent.getLongExtra(EXTRA_SEED, System.currentTimeMillis())
                val mapId = intent.getStringExtra(EXTRA_MAP_ID) ?: "level_0"
                startOffline(diff, seed, mapId)
            }
            ACTION_START_ONLINE -> {
                val roomId = intent.getStringExtra(EXTRA_ROOM_ID) ?: return START_NOT_STICKY
                val diff   = intent.getStringExtra(EXTRA_DIFFICULTY) ?: "normal"
                startOnline(roomId, diff)
            }
            ACTION_STOP       -> stopSession()
            ACTION_PAUSE      -> _gameState.update { it.copy(isPaused = true) }
            ACTION_RESUME     -> _gameState.update { it.copy(isPaused = false) }
            ACTION_FLASHLIGHT -> _gameState.update { it.copy(flashlightOn = !it.flashlightOn) }
            ACTION_DAMAGE_ENTITY -> {
                val id     = intent.getIntExtra(EXTRA_ENTITY_ID, -1)
                val damage = intent.getFloatExtra(EXTRA_DAMAGE, 10f)
                if (id >= 0) {
                    bridge.damageEntity(id, damage)
                    kills++; score += 100L
                    _gameState.update { it.copy(kills = kills, score = score) }
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder
    override fun onDestroy() { scope.cancel(); stopForeground(STOP_FOREGROUND_REMOVE); super.onDestroy() }

    private fun startOffline(difficulty: String, seed: Long, mapId: String) {
        scope.launch {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            bridge.initCore(seed)
            bridge.initSound()
            bridge.initEntities()
            bridge.setAmbienceLevel(0.4f)
            bridge.setHumVolume(0.3f)
            bridge.setSpatialRolloff(1f, 40f)

            // Level 0 always — there is deliberately no map selection anywhere.
            val roomBudget = if (difficulty == "hard") 180 else 130
            grid = GridLevelData.parse(bridge.generateLevel(roomBudget, depth = 0))

            val cfg = assetManager.getSpawnConfig(difficulty)
            spawnInitialEntities(bridge, grid, cfg)
            _gameState.value = GameState(
                seed = seed, difficulty = difficulty, isOnline = false, mapId = "level_0",
                grid = grid, exitX = grid.exitX, exitZ = grid.exitZ
            )
            startPhysicsLoop()
            startEntitySpawner(difficulty, cfg)
            startScoreAccumulator()
        }
    }

    private fun startOnline(roomId: String, difficulty: String) {
        scope.launch {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            val seed = System.currentTimeMillis()
            bridge.initCore(seed)
            bridge.initSound()
            bridge.initEntities()
            bridge.initSocket(0)
            bridge.setLocalId((Math.random() * Int.MAX_VALUE).toInt())

            val roomBudget = if (difficulty == "hard") 180 else 130
            grid = GridLevelData.parse(bridge.generateLevel(roomBudget, depth = 0))

            val cfg = assetManager.getSpawnConfig(difficulty)
            spawnInitialEntities(bridge, grid, cfg)
            _gameState.value = GameState(
                seed = seed, difficulty = difficulty, isOnline = true,
                grid = grid, exitX = grid.exitX, exitZ = grid.exitZ
            )
            startPhysicsLoop()
            startEntitySpawner(difficulty, cfg)
            startNetworkSync()
            startScoreAccumulator()
        }
    }

    private fun startPhysicsLoop() {
        lastTickMs = bridge.nowMs()
        physicsJob = scope.launch {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            while (isActive) {
                if (_gameState.value.isPaused) { delay(16); continue }
                val now = bridge.nowMs()
                val dt  = ((now - lastTickMs).coerceIn(1, 100)).toFloat() / 1000f
                lastTickMs = now; elapsedMs += (dt * 1000).toLong()
                val wasGameOver = _gameState.value.isGameOver
                val derived = stepSimulation(bridge, dt)
                _gameState.update { applyTickToState(it, derived, dt, elapsedMs, score) }
                if (!wasGameOver && _gameState.value.isGameOver) onGameOver()
                delay(16)
            }
        }
    }

    private fun startEntitySpawner(difficulty: String, cfg: SpawnConfig) {
        entityJob = scope.launch {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            var timer = 0L
            while (isActive) {
                delay(5_000); timer += 5_000
                if (timer >= cfg.spawnIntervalMs && !grid.isEmpty) {
                    timer = 0
                    spawnOneRandomEntity(bridge, grid, cfg)
                }
            }
        }
    }

    private fun startNetworkSync() {
        networkJob = scope.launch {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            while (isActive) {
                val cam = CameraSnapshot.fromFloatArray(bridge.getCameraState())
                if (cam != null) bridge.buildPosPacket(cam.posX, cam.posY, cam.posZ, cam.yaw, cam.pitch)
                bridge.drainRecvQueue()?.forEach { processIncomingPacket(it) }
                bridge.buildPingPacket()
                delay(50)
            }
        }
    }

    private fun startScoreAccumulator() {
        scoreJob = scope.launch {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            while (isActive) {
                if (!_gameState.value.isPaused)
                    score += when (_gameState.value.difficulty) { "hard" -> 5L; "normal" -> 3L; else -> 1L }
                delay(1_000)
            }
        }
    }

    private fun processIncomingPacket(raw: ByteArray) { if (raw.size < 8) return }

    fun applyDamage(amount: Float) {
        val s  = _gameState.value
        val hp = (s.playerHp - amount).coerceAtLeast(0f)
        _gameState.update { it.copy(playerHp = hp, isGameOver = hp <= 0f) }
        if (hp <= 0f) onGameOver()
    }

    fun heal(amount: Float) {
        val s = _gameState.value
        _gameState.update { it.copy(playerHp = (s.playerHp + amount).coerceAtMost(s.playerMaxHp)) }
    }

    fun consumeStamina(amount: Float) {
        _gameState.update { it.copy(stamina = (_gameState.value.stamina - amount).coerceAtLeast(0f)) }
    }

    fun submitScoreToServer() {
        scope.launch {
            val s = _gameState.value
            runCatching { api.submitScore(ScoreSubmitRequest(s.level, score, if (s.isEscaped) 1 else 0, s.difficulty, elapsedMs, kills)) }
            runCatching {
                FirebaseFirestore.getInstance().collection("leaderboard").add(
                    mapOf("difficulty" to s.difficulty, "score" to score, "kills" to kills, "sessionMs" to elapsedMs, "ts" to System.currentTimeMillis())
                )
            }
            runCatching {
                FirebaseCrashlytics.getInstance().setCustomKey("last_score", score)
                FirebaseCrashlytics.getInstance().setCustomKey("difficulty", s.difficulty)
            }
        }
    }

    private fun onGameOver() {
        bridge.triggerMonster(1.0f)
        submitScoreToServer()
        physicsJob?.cancel(); entityJob?.cancel(); networkJob?.cancel(); scoreJob?.cancel()
    }

    private fun stopSession() {
        onGameOver()
        scope.launch {
            bridge.destroyEntities()
            bridge.destroySound()
            bridge.destroySocket()
            bridge.destroyCore()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createChannel() {
        val ch = NotificationChannel(CHANNEL_ID, getString(R.string.notif_channel_session), NotificationManager.IMPORTANCE_LOW)
            .apply { setShowBadge(false) }
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    private fun buildNotif(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.loading_text))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setSilent(true)
            .build()
}

data class RoomListUiState(
    val rooms       : List<RoomInfo> = emptyList(),
    val query       : String         = "",
    val filterLocked: Boolean?       = null,
    val lang        : String?        = null,
    val page        : Int            = 0,
    val totalPages  : Int            = 1,
    val isLoading   : Boolean        = false,
    val error       : String?        = null
)

data class CreateRoomUiState(
    val name            : String  = "",
    val nameError       : Int?    = null,
    val size            : Int     = 2,
    val difficulty      : String  = "normal",
    val passwordEnabled : Boolean = false,
    val password        : String  = "",
    val passwordError   : Int?    = null,
    val mapId           : String  = "level_0",
    val language        : String  = "TR",
    val isCreating      : Boolean = false,
    val createdRoomId   : String? = null,
    val error           : String? = null
)

data class RoomLobbyUiState(
    val detail   : RoomDetail?   = null,
    val isReady  : Boolean       = false,
    val allReady : Boolean       = false,
    val countdown: Int?          = null,
    val isLoading: Boolean       = false,
    val peerPings: Map<Int, Int> = emptyMap()
)

@kotlinx.coroutines.FlowPreview
@HiltViewModel
class RoomListVM @Inject constructor(private val repo: RoomRepository) : ViewModel() {
    private val _state = MutableStateFlow(RoomListUiState())
    val state: StateFlow<RoomListUiState> = _state.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            _state.map { Triple(it.query, it.filterLocked, it.lang) }
                .debounce(300)
                .distinctUntilChanged()
                .collect { load() }
        }
    }

    fun onQuery(q: String)    { _state.update { it.copy(query = q, page = 0) } }
    fun onLocked(l: Boolean?) { _state.update { it.copy(filterLocked = l, page = 0) } }
    fun onLang(l: String?)    { _state.update { it.copy(lang = l, page = 0) } }
    fun prev() { if (_state.value.page > 0) { _state.update { it.copy(page = it.page - 1) }; load() } }
    fun next() { val s = _state.value; if (s.page < s.totalPages - 1) { _state.update { it.copy(page = it.page + 1) }; load() } }

    private fun load() {
        viewModelScope.launch {
            val s = _state.value
            _state.update { it.copy(isLoading = true) }
            runCatching { repo.fetchRooms(s.query, s.filterLocked, s.lang, s.page, 20) }
                .onSuccess { r -> _state.update { it.copy(isLoading = false, rooms = r.rooms, totalPages = maxOf(1, (r.total + 19) / 20)) } }
                .onFailure { _state.update { it.copy(isLoading = false) } }
        }
    }
}

@HiltViewModel
class CreateRoomVM @Inject constructor(private val repo: RoomRepository) : ViewModel() {
    private val _state = MutableStateFlow(CreateRoomUiState())
    val state: StateFlow<CreateRoomUiState> = _state.asStateFlow()

    fun onName(n: String)            { _state.update { it.copy(name = n, nameError = validate(n)) } }
    fun onSize(v: Int)               { _state.update { it.copy(size = v.coerceIn(2, 4)) } }
    fun onDifficulty(d: String)      { _state.update { it.copy(difficulty = d) } }
    fun onPasswordToggle(e: Boolean) { _state.update { it.copy(passwordEnabled = e, password = if (!e) "" else it.password, passwordError = null) } }
    fun onPassword(p: String)        { _state.update { it.copy(password = p, passwordError = null) } }
    fun onLanguage(l: String)        { _state.update { it.copy(language = l) } }

    fun onCreate() {
        val s   = _state.value
        val err = validate(s.name)
        if (err != null) { _state.update { it.copy(nameError = err) }; return }
        // Previously this returned silently, so tapping Create with the lock on
        // and no password simply did nothing with no explanation.
        if (s.passwordEnabled && s.password.isBlank()) {
            _state.update { it.copy(passwordError = R.string.room_password_required) }
            return
        }
        _state.update { it.copy(passwordError = null) }
        viewModelScope.launch {
            _state.update { it.copy(isCreating = true) }
            runCatching { repo.createRoom(s.name, s.size, s.difficulty, if (s.passwordEnabled) s.password else null) }
                .onSuccess { id -> _state.update { it.copy(isCreating = false, createdRoomId = id) } }
                .onFailure { e  -> _state.update { it.copy(isCreating = false, error = e.message) } }
        }
    }

    private fun validate(n: String): Int? {
        if (n.length < 4 || n.length > 12) return R.string.room_name_error_length
        if (!Regex("^[a-zA-Z0-9 _-]+$").matches(n)) return R.string.room_name_error_chars
        return null
    }
}

@HiltViewModel
class RoomLobbyVM @Inject constructor(private val api: ApiService, private val bridge: NativeBridge) : ViewModel() {
    private val _state = MutableStateFlow(RoomLobbyUiState())
    val state: StateFlow<RoomLobbyUiState> = _state.asStateFlow()

    fun load(roomId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            runCatching { api.getRoomDetail(roomId) }
                .onSuccess { d -> _state.update { it.copy(isLoading = false, detail = d) } }
                .onFailure { _state.update { it.copy(isLoading = false) } }
        }
        startPingLoop()
    }

    fun toggleReady() { _state.update { it.copy(isReady = !it.isReady) }; checkAllReady() }

    private fun checkAllReady() {
        val d = _state.value.detail ?: return
        if (_state.value.isReady && d.players.filter { !it.isHost }.all { it.isReady }) startCountdown()
    }

    private fun startCountdown() {
        viewModelScope.launch {
            for (i in 5 downTo 0) {
                _state.update { it.copy(countdown = i) }
                delay(1_000)
            }
        }
    }

    private fun startPingLoop() {
        viewModelScope.launch {
            while (isActive) {
                _state.update { it.copy(peerPings = it.peerPings + (0 to bridge.getLocalPing())) }
                delay(2_000)
            }
        }
    }
}

@kotlinx.coroutines.FlowPreview
@androidx.compose.runtime.Composable
fun Room(onJoined: () -> Unit, onBack: () -> Unit, onCreate: () -> Unit, vm: RoomListVM = hiltViewModel()) {
    val s by vm.state.collectAsState()
    Box(Modifier.fillMaxSize().background(DarkBg)) {
        CrtOverlay()
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().background(Color.Black.copy(0.65f)).padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Yellow) }
                Text(stringResource(R.string.room_list_title), color = Yellow, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                Spacer(Modifier.weight(1f))
                androidx.compose.animation.AnimatedVisibility(visible = s.isLoading, enter = fadeIn(), exit = fadeOut()) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = Yellow, strokeWidth = 2.dp)
                }
                IconButton(onClick = onCreate) { Icon(Icons.Default.Add, null, tint = SuccessGreen) }
            }
            DividerLine()
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                SearchField(s.query, vm::onQuery, Modifier.weight(1f))
                RoomFilterChip(stringResource(R.string.room_filter_unlocked), s.filterLocked == false) { vm.onLocked(if (s.filterLocked == false) null else false) }
                RoomFilterChip(stringResource(R.string.room_filter_locked),   s.filterLocked == true)  { vm.onLocked(if (s.filterLocked == true) null else true) }
            }
            DividerLine()
            if (s.rooms.isEmpty() && !s.isLoading) {
                Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                    Text(stringResource(R.string.room_list_empty), color = TextDim, fontSize = 13.sp, letterSpacing = 2.sp)
                }
            } else {
                LazyColumn(
                    Modifier.weight(1f),
                    contentPadding      = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(s.rooms, key = { it.id }) { room ->
                        androidx.compose.animation.AnimatedVisibility(
                            visible      = true,
                            enter        = slideInVertically(tween(200)) { it / 2 } + fadeIn(tween(200)),
                            modifier     = Modifier.animateItem()
                        ) {
                            RoomRow(room) { onJoined() }
                        }
                    }
                }
            }
            DividerLine()
            PagerBar(s.page, s.totalPages, vm::prev, vm::next)
        }
    }
}

@androidx.compose.runtime.Composable
fun CreateRoom(onCreated: () -> Unit, onBack: () -> Unit, vm: CreateRoomVM = hiltViewModel()) {
    val s by vm.state.collectAsState()
    LaunchedEffect(s.createdRoomId) { if (s.createdRoomId != null) onCreated() }
    Box(Modifier.fillMaxSize().background(DarkBg)) {
        CrtOverlay()
        Column(Modifier.fillMaxSize()) {
            TopBarBack(stringResource(R.string.room_create_title), onBack)
            DividerLine()
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SectionLabel("ODA ADI")
                OmniTextField(s.name, vm::onName, stringResource(R.string.room_create_name_hint), error = s.nameError?.let { stringResource(it) })

                SectionLabel(stringResource(R.string.room_create_size_label))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("${s.size} ${stringResource(R.string.room_players_label)}", color = Yellow, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Slider(
                        s.size.toFloat(), { vm.onSize(it.toInt()) },
                        valueRange = 2f..4f,
                        steps      = 1,
                        colors     = SliderDefaults.colors(thumbColor = Yellow, activeTrackColor = Yellow, inactiveTrackColor = MetalBg),
                        modifier   = Modifier.width(180.dp)
                    )
                }

                DifficultySelector(s.difficulty, vm::onDifficulty)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("TR","EN","DE","RU").forEach { l ->
                        val sel = s.language == l
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.weight(1f).height(34.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (sel) Yellow.copy(0.15f) else MetalBg.copy(0.5f))
                                .border(1.dp, if (sel) Yellow.copy(0.6f) else BorderCol, RoundedCornerShape(2.dp))
                                .clickable { vm.onLanguage(l) }
                        ) { Text(l, color = if (sel) Yellow else TextDim, fontSize = 11.sp) }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Checkbox(
                        s.passwordEnabled,
                        vm::onPasswordToggle,
                        colors = CheckboxDefaults.colors(checkedColor = Yellow, uncheckedColor = TextDim, checkmarkColor = Color.Black)
                    )
                    Icon(
                        if (s.passwordEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                        null,
                        tint     = if (s.passwordEnabled) Yellow else TextDim,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(stringResource(R.string.room_create_password_label), color = if (s.passwordEnabled) Yellow else TextDim, fontSize = 12.sp)
                }

                androidx.compose.animation.AnimatedVisibility(visible = s.passwordEnabled, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    OmniTextField(
                        s.password, vm::onPassword,
                        stringResource(R.string.room_create_password_hint),
                        error = s.passwordError?.let { stringResource(it) },
                        isPassword = true
                    )
                }

                s.error?.let { Text(it, color = DangerRed, fontSize = 11.sp) }

                AtmosphericButton(
                    label   = if (s.isCreating) "…" else stringResource(R.string.room_create_confirm),
                    icon    = Icons.Default.Add,
                    accent  = Yellow,
                    width   = 400.dp,
                    height  = 50.dp,
                    enabled = !s.isCreating && s.nameError == null && s.name.isNotBlank(),
                    onClick = vm::onCreate
                )
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun RoomRow(room: RoomInfo, onClick: () -> Unit) {
    val interSrc  = remember { MutableInteractionSource() }
    val isPressed by interSrc.collectIsPressedAsState()
    val scale     by animateFloatAsState(if (isPressed) 0.98f else 1f, spring(), label = "room_row")
    Row(
        Modifier.fillMaxWidth().scale(scale).height(50.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(MetalBg.copy(0.7f))
            .border(1.dp, BorderCol, RoundedCornerShape(3.dp))
            .clickable(interactionSource = interSrc, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (room.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
            null,
            tint     = if (room.isLocked) DangerRed.copy(0.7f) else SuccessGreen.copy(0.7f),
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(room.name, color = Yellow, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(room.language, color = TextDim, fontSize = 10.sp, letterSpacing = 1.sp)
        Spacer(Modifier.width(10.dp))
        Text("${room.currentPlayers}/${room.maxPlayers}", color = TextSec, fontSize = 11.sp)
        Spacer(Modifier.width(10.dp))
        Text(
            room.difficulty.uppercase(),
            fontSize     = 10.sp,
            fontWeight   = FontWeight.Bold,
            letterSpacing = 1.sp,
            color        = when (room.difficulty) { "easy" -> SuccessGreen; "hard" -> DangerRed; else -> Yellow }
        )
        if (room.ping > 0) {
            Spacer(Modifier.width(8.dp))
            Text("${room.ping}ms", color = when { room.ping < 60 -> SuccessGreen; room.ping < 120 -> CrtAmber; else -> DangerRed }, fontSize = 9.sp)
        }
    }
}

@androidx.compose.runtime.Composable
private fun SearchField(query: String, onQuery: (String) -> Unit, modifier: Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(2.dp))
            .background(MetalBg)
            .border(1.dp, BorderCol, RoundedCornerShape(2.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, null, tint = TextDim, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        BasicTextField(
            query, onQuery,
            singleLine  = true,
            textStyle   = TextStyle(color = Yellow, fontSize = 12.sp),
            cursorBrush = SolidColor(Yellow),
            decorationBox = { inner ->
                if (query.isEmpty()) Text(stringResource(R.string.room_search_hint), color = TextDim, fontSize = 12.sp)
                inner()
            }
        )
    }
}

@androidx.compose.runtime.Composable
private fun RoomFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (selected) 1.04f else 1f, spring(), label = "chip")
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.scale(scale)
            .clip(RoundedCornerShape(2.dp))
            .background(if (selected) Yellow.copy(0.15f) else MetalBg.copy(0.5f))
            .border(1.dp, if (selected) Yellow.copy(0.6f) else BorderCol, RoundedCornerShape(2.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, color = if (selected) Yellow else TextDim, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, letterSpacing = 1.sp)
    }
}

@androidx.compose.runtime.Composable
private fun DifficultySelector(selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            Triple(R.string.difficulty_easy,   "easy",   SuccessGreen),
            Triple(R.string.difficulty_normal, "normal", Yellow),
            Triple(R.string.difficulty_hard,   "hard",   DangerRed)
        ).forEach { (res, key, col) ->
            val sel   = selected == key
            val scale by animateFloatAsState(if (sel) 1.04f else 1f, spring(), label = "diff_$key")
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.weight(1f).height(36.dp).scale(scale)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (sel) col.copy(0.15f) else MetalBg.copy(0.5f))
                    .border(1.dp, if (sel) col.copy(0.7f) else BorderCol, RoundedCornerShape(2.dp))
                    .clickable { onSelect(key) }
            ) {
                Text(stringResource(res), color = if (sel) col else TextDim, fontSize = 11.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun SectionLabel(text: String) {
    Text(text, color = TextSec, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
}

@androidx.compose.runtime.Composable
private fun PagerBar(page: Int, total: Int, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Color.Black.copy(0.5f)).padding(horizontal = 16.dp, vertical = 8.dp),
        Arrangement.Center,
        Alignment.CenterVertically
    ) {
        AtmosphericButton(stringResource(R.string.room_page_prev), Icons.AutoMirrored.Filled.ArrowBack,    Yellow, 110.dp, 38.dp, onPrev, enabled = page > 0)
        Spacer(Modifier.width(16.dp))
        Text("${page + 1} / $total", color = TextSec, fontSize = 12.sp, letterSpacing = 1.sp)
        Spacer(Modifier.width(16.dp))
        AtmosphericButton(stringResource(R.string.room_page_next), Icons.AutoMirrored.Filled.ArrowForward, Yellow, 110.dp, 38.dp, onNext, enabled = page < total - 1)
    }
}


// ============================================================================
// Firebase Cloud Messaging receiver. This class is declared in the manifest;
// without it Android throws ClassNotFoundException the moment a push arrives
// (the app subscribes to "backrooms_global" at startup), which is exactly the
// crash the on-device crash log captured.
// ============================================================================
class OmniMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Persisting the token is best-effort: a failure here must never crash
        // the app, since this runs outside any user-visible flow.
        runCatching {
            FirebaseFirestore.getInstance()
                .collection("device_tokens")
                .document(token)
                .set(mapOf("token" to token, "updatedAt" to System.currentTimeMillis()))
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title
            ?: message.data["title"]
            ?: getString(R.string.app_name)
        val body = message.notification?.body ?: message.data["body"] ?: return
        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "omni_push"
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(channelId) == null) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pending = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}


// ============================================================================
// Guest identity + save game. Both live in DataStore so they survive restarts
// but vanish with the app's data, which is exactly the lifetime a guest
// account is supposed to have.
// ============================================================================

private val Context.identityStore: DataStore<Preferences> by preferencesDataStore(name = "omni_identity")

@Singleton
class GuestIdentityManager @Inject constructor(@ApplicationContext private val ctx: Context) {

    private object Keys {
        val NAME      = stringPreferencesKey("guest_name")
        val CREATED   = longPreferencesKey("guest_created_ms")
        val LAST_SEEN = longPreferencesKey("guest_last_seen_ms")
    }

    companion object {
        /** Guest data is dropped after this much inactivity. */
        val INACTIVITY_LIMIT_MS = TimeUnit.DAYS.toMillis(7)
    }

    /** Returns the device's guest name, minting one on first launch. Also expires
     *  the account (and every trace of its progress) after a week of inactivity,
     *  so a returning player starts genuinely fresh rather than resuming a stale
     *  identity. */
    suspend fun currentName(): String {
        val now = System.currentTimeMillis()
        val prefs = ctx.identityStore.data.first()
        val existing = prefs[Keys.NAME]
        val lastSeen = prefs[Keys.LAST_SEEN] ?: 0L

        if (existing != null && now - lastSeen <= INACTIVITY_LIMIT_MS) {
            ctx.identityStore.edit { it[Keys.LAST_SEEN] = now }
            return existing
        }

        // Either brand new, or expired — wipe any leftover progress first so a
        // stale save can never be attached to a fresh identity.
        if (existing != null) SaveGameStore(ctx).clear()
        val minted = mintName()
        ctx.identityStore.edit {
            it[Keys.NAME] = minted
            it[Keys.CREATED] = now
            it[Keys.LAST_SEEN] = now
        }
        return minted
    }

    /** "Unknown Player 4820-7391": wide enough that collisions are effectively
     *  impossible across installs without needing a server round-trip. */
    private fun mintName(): String {
        val rng = SecureRandom()
        val a = rng.nextInt(9000) + 1000
        val b = rng.nextInt(9000) + 1000
        return "Unknown Player $a-$b"
    }

    suspend fun touch() {
        ctx.identityStore.edit { it[Keys.LAST_SEEN] = System.currentTimeMillis() }
    }

    /** The single source of truth for the player's shown name. Lobby, profile and
     *  settings all observe this, so a rename in one place appears in all of them. */
    fun observeDisplayName(): Flow<String> = ctx.identityStore.data.map { it[Keys.NAME] ?: "" }

    suspend fun setDisplayName(name: String) {
        val clean = name.trim().take(24)
        if (clean.isEmpty()) return
        runCatching { ctx.identityStore.edit { it[Keys.NAME] = clean } }
    }
}

/** A resumable run. Deliberately small: the level is fully regenerated from
 *  [seed], so only the player's own progress needs storing. */
@Serializable
data class SavedRun(
    val seed        : Long,
    val difficulty  : String,
    val elapsedMs   : Long,
    val score       : Long,
    val kills       : Int,
    val sanity      : Float,
    val battery     : Float,
    val playerHp    : Float,
    val savedAtMs   : Long
)

@Singleton
class SaveGameStore @Inject constructor(@ApplicationContext private val ctx: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val key  = stringPreferencesKey("saved_run")

    /** Application-lifetime scope. The final save happens exactly when the game
     *  screen is being torn down, so a ViewModel-scoped coroutine gets cancelled
     *  before it can write — which is why "Continue" always started over. This
     *  scope outlives the ViewModel, so the write actually lands. */
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun save(run: SavedRun) {
        runCatching { ctx.identityStore.edit { it[key] = json.encodeToString(run) } }
    }

    /** Fire-and-forget save that survives the caller being destroyed. */
    fun saveDetached(run: SavedRun) {
        ioScope.launch {
            runCatching { ctx.identityStore.edit { it[key] = json.encodeToString(run) } }
                .onSuccess { OmniLog.i("Save", "run saved elapsed=${run.elapsedMs}") }
                .onFailure { OmniLog.e("Save", "save failed", it) }
        }
    }

    suspend fun load(): SavedRun? = runCatching {
        ctx.identityStore.data.first()[key]?.let { json.decodeFromString<SavedRun>(it) }
    }.getOrNull()

    fun observeHasSave(): Flow<Boolean> = ctx.identityStore.data.map { it[key] != null }

    suspend fun clear() {
        runCatching { ctx.identityStore.edit { it.remove(key) } }
    }
}


// ============================================================================
// Diagnostics. Everything notable the app does gets appended to a ring buffer
// in memory and mirrored into Documents/OmniBackrooms/, so when something goes
// wrong the file explains *why* rather than just showing a stack trace with no
// surrounding context.
// ============================================================================

object OmniLog {

    enum class Level { DEBUG, INFO, WARN, ERROR }

    private const val TAG = "OmniBackrooms"
    private const val RING_CAPACITY = 400
    private val ring = ArrayDeque<String>(RING_CAPACITY)
    private val lock = Any()
    private val stamp = java.text.SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    @Volatile private var sink: java.io.File? = null

    /** Points the logger at app-private storage first (always writable, no
     *  permission needed) so nothing is lost even if the public Documents copy
     *  fails on a given device. */
    fun attach(ctx: Context) {
        runCatching {
            val dir = java.io.File(ctx.filesDir, "diagnostics").apply { if (!exists()) mkdirs() }
            sink = java.io.File(dir, "session.log")
            // Keep the private log from growing without bound across launches.
            sink?.let { if (it.exists() && it.length() > 512 * 1024) it.delete() }
        }
        i("Log", "attached; app-private sink=${sink?.absolutePath}")
    }

    fun d(tag: String, msg: String) = write(Level.DEBUG, tag, msg, null)
    fun i(tag: String, msg: String) = write(Level.INFO,  tag, msg, null)
    fun w(tag: String, msg: String, t: Throwable? = null) = write(Level.WARN, tag, msg, t)
    fun e(tag: String, msg: String, t: Throwable? = null) = write(Level.ERROR, tag, msg, t)

    private fun write(level: Level, tag: String, msg: String, t: Throwable?) {
        val line = buildString {
            append(stamp.format(java.util.Date())); append(' ')
            append(level.name.first()); append('/')
            append(tag); append(": "); append(msg)
            if (t != null) {
                append('\n')
                append(java.io.StringWriter().also { sw -> t.printStackTrace(java.io.PrintWriter(sw)) })
            }
        }
        synchronized(lock) {
            if (ring.size >= RING_CAPACITY) ring.removeFirst()
            ring.addLast(line)
        }
        when (level) {
            Level.DEBUG -> Log.d(TAG, "[$tag] $msg")
            Level.INFO  -> Log.i(TAG, "[$tag] $msg")
            Level.WARN  -> Log.w(TAG, "[$tag] $msg", t)
            Level.ERROR -> Log.e(TAG, "[$tag] $msg", t)
        }
        // Crashlytics keeps the same breadcrumbs, so remote reports carry the
        // identical context as the on-device file.
        runCatching { FirebaseCrashlytics.getInstance().log(line) }
        runCatching { sink?.appendText(line + "\n") }
    }

    /** The recent history, newest last — this is what gets attached to a crash
     *  report so the lines leading up to the failure are visible. */
    fun recentHistory(): String = synchronized(lock) { ring.joinToString("\n") }

    fun clearRing() = synchronized(lock) { ring.clear() }
}


/** Parsed Level 0 occupancy grid. Replaces the old corridor-segment list: a
 *  grid can't have seams between pieces, so there is nowhere left for the
 *  player to fall out of the world. */
class GridLevelData(
    val dim      : Int,
    val cellSize : Float,
    val height   : Float,
    val spawnX   : Float,
    val spawnZ   : Float,
    val exitX    : Float,
    val exitZ    : Float,
    private val solid  : ByteArray,
    private val zone   : ByteArray,
    private val feature: ByteArray
) {
    fun isSolid(x: Int, z: Int): Boolean =
        if (x < 0 || z < 0 || x >= dim || z >= dim) true else solid[z * dim + x] != 0.toByte()

    fun zoneAt(x: Int, z: Int): Int =
        if (x < 0 || z < 0 || x >= dim || z >= dim) 0 else zone[z * dim + x].toInt()

    fun featureAt(x: Int, z: Int): Int =
        if (x < 0 || z < 0 || x >= dim || z >= dim) 0 else feature[z * dim + x].toInt()

    fun worldX(cx: Int): Float = (cx - dim * 0.5f) * cellSize
    fun worldZ(cz: Int): Float = (cz - dim * 0.5f) * cellSize

    val isEmpty: Boolean get() = dim <= 0

    companion object {
        const val HEADER_FLOATS = 8
        const val FLOATS_PER_CELL = 3

        val EMPTY = GridLevelData(0, 3.2f, 3f, 0f, 0f, 0f, 0f, ByteArray(0), ByteArray(0), ByteArray(0))

        fun parse(data: FloatArray?): GridLevelData {
            if (data == null || data.size < HEADER_FLOATS) return EMPTY
            val dim = data[0].toInt()
            val cells = dim * dim
            if (dim <= 0 || data.size < HEADER_FLOATS + cells * FLOATS_PER_CELL) return EMPTY
            val solid = ByteArray(cells)
            val zone = ByteArray(cells)
            val feature = ByteArray(cells)
            var p = HEADER_FLOATS
            for (i in 0 until cells) {
                solid[i]   = data[p].toInt().toByte()
                zone[i]    = data[p + 1].toInt().toByte()
                feature[i] = data[p + 2].toInt().toByte()
                p += FLOATS_PER_CELL
            }
            return GridLevelData(
                dim = dim, cellSize = data[1], height = data[2],
                spawnX = data[3], spawnZ = data[4], exitX = data[5], exitZ = data[6],
                solid = solid, zone = zone, feature = feature
            )
        }
    }
}


/** Cosmetic + personal-best storage. Deliberately local-only: none of this
 *  affects gameplay, so it needs no server round-trip and keeps working offline.
 *  Lives in the same identity store as the guest account, so wiping the guest
 *  also wipes their cosmetics — which is the behaviour we want. */
@Singleton
class CosmeticsStore @Inject constructor(@ApplicationContext private val ctx: Context) {

    private object Keys {
        val AVATAR_URI   = stringPreferencesKey("avatar_uri")
        val FRAME        = stringPreferencesKey("frame")
        val OWNED_FRAMES = stringPreferencesKey("owned_frames")
        val BEST_SURVIVAL= longPreferencesKey("best_survival_ms")
    }

    fun observeAvatarUri(): Flow<String?> = ctx.identityStore.data.map { it[Keys.AVATAR_URI] }
    fun observeFrame(): Flow<String> = ctx.identityStore.data.map { it[Keys.FRAME] ?: "default" }
    fun observeBestSurvival(): Flow<Long> = ctx.identityStore.data.map { it[Keys.BEST_SURVIVAL] ?: 0L }

    fun observeOwnedFrames(): Flow<List<String>> = ctx.identityStore.data.map { prefs ->
        prefs[Keys.OWNED_FRAMES]?.split(',')?.filter { it.isNotBlank() } ?: emptyList()
    }

    suspend fun setAvatarUri(uri: String) {
        runCatching { ctx.identityStore.edit { it[Keys.AVATAR_URI] = uri } }
    }

    suspend fun setFrame(key: String) {
        runCatching { ctx.identityStore.edit { it[Keys.FRAME] = key } }
    }

    suspend fun grantFrame(key: String) {
        runCatching {
            ctx.identityStore.edit { prefs ->
                val cur = prefs[Keys.OWNED_FRAMES]?.split(',')?.filter { it.isNotBlank() }?.toMutableSet()
                    ?: mutableSetOf()
                cur.add(key)
                prefs[Keys.OWNED_FRAMES] = cur.joinToString(",")
            }
        }
    }

    /** Only writes when the new run actually beats the record. */
    suspend fun recordSurvival(ms: Long) {
        runCatching {
            ctx.identityStore.edit { prefs ->
                val best = prefs[Keys.BEST_SURVIVAL] ?: 0L
                if (ms > best) prefs[Keys.BEST_SURVIVAL] = ms
            }
        }
    }
}


// ============================================================================
// Language selection. Implemented directly rather than via AppCompat's
// per-app-locale API, because this is a Compose-only app (ComponentActivity)
// and pulling in the whole AppCompat theming stack just for locale switching
// isn't a good trade. Works uniformly from API 28 up.
// ============================================================================

/** Languages the UI is actually localised for. Anything else falls back to
 *  English, which is also the default. */
enum class AppLanguage(val tag: String, val endonym: String) {
    ENGLISH("en", "English"),
    TURKISH("tr", "Türkçe"),
    SPANISH("es", "Español"),
    RUSSIAN("ru", "Русский"),
    GERMAN ("de", "Deutsch");

    companion object {
        const val SYSTEM = "system"

        fun fromTag(tag: String?): AppLanguage? = entries.firstOrNull { it.tag == tag }

        /** Resolves the device's own language to a supported one, or English. */
        fun matchDevice(): AppLanguage {
            val deviceTag = Locale.getDefault().language.lowercase(Locale.ROOT)
            return fromTag(deviceTag) ?: ENGLISH
        }
    }
}

@Singleton
class LocaleStore @Inject constructor(@ApplicationContext private val ctx: Context) {
    private val key = stringPreferencesKey("app_language")

    /** "system" until the player picks explicitly. */
    fun observeSelection(): Flow<String> = ctx.identityStore.data.map { it[key] ?: AppLanguage.SYSTEM }

    suspend fun setSelection(value: String) {
        runCatching { ctx.identityStore.edit { it[key] = value } }
        OmniLog.i("Locale", "selection set to $value")
    }

    /** Blocking read, needed from attachBaseContext where suspending isn't an
     *  option. Falls back to the device language on any failure. */
    fun currentLanguageBlocking(): AppLanguage = runCatching {
        runBlocking { observeSelection().first() }
    }.getOrNull().let { sel ->
        if (sel == null || sel == AppLanguage.SYSTEM) AppLanguage.matchDevice()
        else AppLanguage.fromTag(sel) ?: AppLanguage.matchDevice()
    }
}

/** Wraps a Context so resources resolve in [language]. Applied in
 *  MainActivity.attachBaseContext, which is the only hook that runs early
 *  enough to affect the whole activity's resource lookups. */
fun applyAppLanguage(base: Context, language: AppLanguage): Context {
    val locale = Locale.forLanguageTag(language.tag)
    Locale.setDefault(locale)
    val config = Configuration(base.resources.configuration).apply {
        setLocale(locale)
        setLayoutDirection(locale)
    }
    return base.createConfigurationContext(config)
}
