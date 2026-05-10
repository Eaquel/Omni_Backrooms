package com.omni.backrooms

import android.app.*
import android.content.Intent
import android.graphics.Bitmap
import android.os.Binder
import android.os.IBinder
import androidx.compose.ui.geometry.Offset
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.*
import javax.inject.Inject
import okhttp3.MediaType.Companion.toMediaType

// ─────────────────────────────────────────────────────────────
// NativeBridge — Native_Bridge.kt dosyası kaldırıldı, buraya entegre edildi.
// Tüm JNI çağrıları bu sınıf üzerinden yapılır.
// ─────────────────────────────────────────────────────────────
class NativeBridge @Inject constructor() {

    // Core
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

    // Sound
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

    // Entities
    external fun initEntities()
    external fun spawnEntity(x: Float, y: Float, z: Float, speed: Float, hear: Float, sight: Float, aggro: Float, typeId: Int): Int
    external fun tickEntities(px: Float, py: Float, pz: Float, dt: Float): FloatArray?
    external fun damageEntity(id: Int, amount: Float)
    external fun getTotalFlickerInfluence(): Float
    external fun destroyEntities()

    // Network
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

    // Guard
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

// ─────────────────────────────────────────────────────────────
// API Interface
// ─────────────────────────────────────────────────────────────
interface ApiService {

    @GET("rooms")
    suspend fun getRooms(
        @Query("q")        query    : String?,
        @Query("locked")   locked   : Boolean?,
        @Query("lang")     language : String?,
        @Query("page")     page     : Int,
        @Query("pageSize") pageSize : Int
    ): RoomPage

    @POST("rooms")
    suspend fun createRoom(@Body body: CreateRoomRequest): CreateRoomResponse

    @POST("rooms/{id}/join")
    suspend fun joinRoom(
        @Path("id")       roomId  : String,
        @Query("password") password: String?
    ): JoinRoomResponse

    @DELETE("rooms/{id}")
    suspend fun deleteRoom(@Path("id") roomId: String): BaseResponse

    @GET("rooms/{id}")
    suspend fun getRoomDetail(@Path("id") roomId: String): RoomDetail

    @POST("rooms/{id}/kick/{peerId}")
    suspend fun kickPlayer(
        @Path("id")     roomId : String,
        @Path("peerId") peerId : Int
    ): BaseResponse

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
        @Query("page")       page       : Int     = 0,
        @Query("pageSize")   size       : Int     = 50,
        @Query("difficulty") difficulty : String? = null,
        @Query("region")     region     : String? = null
    ): LeaderboardPage

    @POST("player/score")
    suspend fun submitScore(@Body body: ScoreSubmitRequest): BaseResponse

    @GET("events/active")
    suspend fun getActiveEvents(): List<EventDto>

    @GET("events/{id}")
    suspend fun getEventDetail(@Path("id") eventId: String): EventDto

    @POST("events/{id}/join")
    suspend fun joinEvent(@Path("id") eventId: String): BaseResponse

    @POST("auth/google")
    suspend fun loginWithGoogle(@Body body: GoogleAuthRequest): AuthResponse

    @POST("auth/refresh")
    suspend fun refreshToken(@Body body: RefreshRequest): AuthResponse

    @POST("auth/logout")
    suspend fun logout(): BaseResponse

    @GET("market/items")
    suspend fun getMarketItems(
        @Query("category") category: String?,
        @Query("page")     page    : Int = 0
    ): MarketPage

    @POST("market/buy")
    suspend fun buyItem(@Body body: BuyRequest): BuyResponse

    @GET("market/daily")
    suspend fun getDailyDeals(): List<MarketItemDto>

    @GET("maps")
    suspend fun getMaps(): List<MapDto>

    @GET("maps/{id}")
    suspend fun getMapDetail(@Path("id") mapId: String): MapDto

    @GET("characters")
    suspend fun getCharacters(): List<CharacterDto>

    @POST("characters/{id}/equip")
    suspend fun equipCharacter(@Path("id") charId: String): BaseResponse

    @POST("characters/{id}/unlock")
    suspend fun unlockCharacter(@Path("id") charId: String): BaseResponse

    @GET("changelog")
    suspend fun getChangelog(): List<ChangelogDto>

    @GET("story/chapters")
    suspend fun getStoryChapters(): List<StoryChapterDto>

    @PUT("player/settings")
    suspend fun syncSettings(@Body body: GameSettings): BaseResponse

    @GET("player/settings")
    suspend fun fetchSettings(): GameSettings

    @POST("report/player")
    suspend fun reportPlayer(@Body body: ReportRequest): BaseResponse
}

// ─────────────────────────────────────────────────────────────
// Data Models
// ─────────────────────────────────────────────────────────────
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
    val mapId         : String  = "level_0",
    val ping          : Int     = 0
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

data class UiButtonLayout(
    val buttonId  : String,
    val offset    : Offset,
    val sizeScale : Float = 1f
)

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
    val isEscaped         : Boolean = false
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

data class VoiceFrame(val peerId: Int, val pcmData: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as VoiceFrame
        return peerId == other.peerId && pcmData.contentEquals(other.pcmData)
    }
    override fun hashCode(): Int = 31 * peerId + pcmData.contentHashCode()
}

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

data class LevelSegment(
    val posX          : Float,
    val posY          : Float,
    val width         : Float,
    val length        : Float,
    val height        : Float,
    val lightPhase    : Float,
    val lightIntensity: Float,
    val lightSpeed    : Float,
    val lightBroken   : Boolean,
    val roomType      : Int,
    val wallDamage    : Float,
    val moisture      : Float,
    val hasHazard     : Boolean,
    val decalCount    : Int
) {
    companion object {
        fun fromFloatArray(data: FloatArray, index: Int): LevelSegment? {
            val base = index * 14
            if (base + 13 >= data.size) return null
            return LevelSegment(
                posX           = data[base],
                posY           = data[base + 1],
                width          = data[base + 2],
                length         = data[base + 3],
                height         = data[base + 4],
                lightPhase     = data[base + 5],
                lightIntensity = data[base + 6],
                lightSpeed     = data[base + 7],
                lightBroken    = data[base + 8] > 0.5f,
                roomType       = data[base + 9].toInt(),
                wallDamage     = data[base + 10],
                moisture       = data[base + 11],
                hasHazard      = data[base + 12] > 0.5f,
                decalCount     = data[base + 13].toInt()
            )
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
    val typeId          : Int,
    val isActive        : Boolean
) {
    companion object {
        fun fromFloatArray(data: FloatArray, index: Int, id: Int): EntityState? {
            val base = index * 10
            if (base + 9 >= data.size) return null
            return EntityState(
                id               = id,
                posX             = data[base],
                posY             = data[base + 1],
                posZ             = data[base + 2],
                aiState          = data[base + 3].toInt(),
                alertLevel       = data[base + 4],
                hpFraction       = data[base + 5],
                flickerInfluence = data[base + 6],
                typeId           = data[base + 8].toInt(),
                isActive         = data[base + 9] > 0.5f
            )
        }
    }
}

data class InventoryItem(val itemId: String, val quantity: Int, val slotIndex: Int)

data class PlayerInventory(
    val items    : List<InventoryItem> = emptyList(),
    val maxSlots : Int   = 8,
    val weight   : Float = 0f,
    val maxWeight: Float = 20f
) {
    val isFull      : Boolean get() = items.size >= maxSlots
    val isOverweight: Boolean get() = weight > maxWeight
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

data class AchievementDto(
    val id          : String,
    val nameTr      : String,
    val nameEn      : String,
    val descTr      : String,
    val descEn      : String,
    val isUnlocked  : Boolean,
    val unlockedAtMs: Long?,
    val iconUrl     : String?,
    val rewardType  : String,
    val rewardAmount: Long
)

data class CreateRoomRequest(
    val name      : String,
    val maxPlayers: Int,
    val difficulty: String,
    val password  : String?,
    val language  : String = "TR",
    val mapId     : String = "level_0"
)

data class CreateRoomResponse(val roomId: String, val joinCode: String, val success: Boolean)
data class JoinRoomResponse(val success: Boolean, val roomId: String?, val error: String?)

data class RoomDetail(
    val id            : String,
    val name          : String,
    val hostId        : String,
    val currentPlayers: Int,
    val maxPlayers    : Int,
    val difficulty    : String,
    val isLocked      : Boolean,
    val language      : String,
    val mapId         : String,
    val players       : List<RoomPlayer>
)

data class RoomPlayer(
    val id       : Int,
    val name     : String,
    val avatarUrl: String?,
    val isHost   : Boolean,
    val isReady  : Boolean,
    val ping     : Int
)

data class BaseResponse(val success: Boolean, val message: String?)
data class AvatarRequest(val avatarId: String)
data class PurchaseRequest(val itemType: String, val amount: Int)
data class PurchaseResponse(val success: Boolean, val newBalance: Long, val currency: String)

data class LeaderboardPage(val entries: List<LeaderboardEntry>, val total: Int, val myRank: Int?)

data class ScoreSubmitRequest(
    val level     : Int,
    val score     : Long,
    val survived  : Int,
    val difficulty: String,
    val sessionMs : Long,
    val kills     : Int = 0
)

data class EventDto(
    val id           : String,
    val titleTr      : String,
    val titleEn      : String,
    val descriptionTr: String,
    val descriptionEn: String,
    val startMs      : Long,
    val endMs        : Long,
    val rewardType   : String,
    val rewardAmount : Long,
    val imageUrl     : String?,
    val isActive     : Boolean
)

data class GoogleAuthRequest(val idToken: String)
data class RefreshRequest(val refreshToken: String)
data class AuthResponse(val accessToken: String, val refreshToken: String, val expiresIn: Long, val playerId: Int)

data class MarketPage(val items: List<MarketItemDto>, val total: Int)

data class MarketItemDto(
    val id       : String,
    val nameTr   : String,
    val nameEn   : String,
    val descTr   : String,
    val descEn   : String,
    val category : String,
    val price    : Long,
    val currency : String,
    val imageUrl : String?,
    val isOwned  : Boolean,
    val isEquipped: Boolean,
    val isLimited: Boolean,
    val expiresMs: Long?
)

data class BuyRequest(val itemId: String, val currency: String)
data class BuyResponse(val success: Boolean, val newBalance: Long, val error: String?)

data class MapDto(
    val id          : String,
    val nameTr      : String,
    val nameEn      : String,
    val level       : Int,
    val descTr      : String,
    val descEn      : String,
    val thumbnailUrl: String?,
    val isUnlocked  : Boolean,
    val threatLevel : Int,
    val entityTypes : List<Int>
)

data class CharacterDto(
    val id         : String,
    val nameTr     : String,
    val nameEn     : String,
    val clazz      : String,
    val maxHp      : Float,
    val baseSpeed  : Float,
    val stealthMult: Float,
    val staminaMult: Float,
    val abilities  : List<String>,
    val isUnlocked : Boolean,
    val isEquipped : Boolean,
    val imageUrl   : String?,
    val price      : Long,
    val currency   : String
)

data class ChangelogDto(val version: String, val dateMs: Long, val changes: List<String>, val branch: String)

data class StoryChapterDto(
    val id       : Int,
    val titleTr  : String,
    val titleEn  : String,
    val contentTr: String,
    val contentEn: String,
    val isUnlocked: Boolean
)

data class ReportRequest(val reportedId: Int, val reason: String, val details: String)

// ─────────────────────────────────────────────────────────────
// Repository
// ─────────────────────────────────────────────────────────────
class RoomRepository @Inject constructor(private val api: ApiService) {

    suspend fun fetchRooms(
        query   : String?,
        locked  : Boolean?,
        lang    : String?,
        page    : Int,
        pageSize: Int
    ): RoomPage = api.getRooms(query, locked, lang, page, pageSize)

    suspend fun createRoom(
        name      : String,
        maxPlayers: Int,
        difficulty: String,
        password  : String?
    ): String = api.createRoom(CreateRoomRequest(name, maxPlayers, difficulty, password)).roomId
}

// ─────────────────────────────────────────────────────────────
// Session Service
// ─────────────────────────────────────────────────────────────
@AndroidEntryPoint
class SessionService : Service() {

    inner class LocalBinder : Binder() {
        fun get(): SessionService = this@SessionService
    }

    @Inject lateinit var bridge      : NativeBridge
    @Inject lateinit var assetManager: AssetManager
    @Inject lateinit var api         : ApiService

    private val binder = LocalBinder()
    private val scope  = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _gameState = MutableStateFlow(GameState())
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

    companion object {
        private const val CHANNEL_ID   = "omni_session"
        private const val NOTIF_ID     = 2001
        const val ACTION_START_OFFLINE = "start_offline"
        const val ACTION_START_ONLINE  = "start_online"
        const val ACTION_STOP          = "stop_game"
        const val ACTION_PAUSE         = "pause_game"
        const val ACTION_RESUME        = "resume_game"
        const val ACTION_FLASHLIGHT    = "flashlight"
        const val ACTION_DAMAGE_ENTITY = "damage_entity"
        const val EXTRA_DIFFICULTY     = "difficulty"
        const val EXTRA_ROOM_ID        = "room_id"
        const val EXTRA_SEED           = "seed"
        const val EXTRA_MAP_ID         = "map_id"
        const val EXTRA_ENTITY_ID      = "entity_id"
        const val EXTRA_DAMAGE         = "damage"
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIF_ID, buildNotif())
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

    // ── Game lifecycle ────────────────────────────────────────

    private fun startOffline(difficulty: String, seed: Long, mapId: String) {
        scope.launch {
            bridge.initCore(seed)
            bridge.initSound()
            bridge.initEntities()
            bridge.setAmbienceLevel(0.4f)
            bridge.setHumVolume(0.3f)
            bridge.setSpatialRolloff(1f, 40f)

            val cfg = assetManager.getSpawnConfig(difficulty)
            repeat(cfg.count) { i ->
                val typeId = i % EntityType.entries.size
                val entity = EntityType.entries[typeId]
                bridge.spawnEntity(
                    x      = (Math.random() * 60 - 30).toFloat(),
                    y      = 0f,
                    z      = (Math.random() * 60 - 30).toFloat(),
                    speed  = entity.baseSpeed * cfg.speedMult,
                    hear   = 10f + i * 1.5f,
                    sight  = 16f * cfg.sightMult,
                    aggro  = 8f,
                    typeId = typeId
                )
            }

            _gameState.value = GameState(seed = seed, difficulty = difficulty, isOnline = false, mapId = mapId)
            startPhysicsLoop()
            startEntitySpawner(difficulty, cfg)
            startScoreAccumulator()
        }
    }

    private fun startOnline(roomId: String, difficulty: String) {
        scope.launch {
            val seed = System.currentTimeMillis()
            bridge.initCore(seed)
            bridge.initSound()
            bridge.initEntities()
            bridge.initSocket(0)
            bridge.setLocalId((Math.random() * Int.MAX_VALUE).toInt())

            val cfg = assetManager.getSpawnConfig(difficulty)
            repeat(cfg.count) { i ->
                bridge.spawnEntity(
                    x = (Math.random() * 60 - 30).toFloat(), y = 0f,
                    z = (Math.random() * 60 - 30).toFloat(),
                    speed = 2.5f * cfg.speedMult, hear = 10f,
                    sight = 16f * cfg.sightMult,  aggro = 8f,
                    typeId = i % 8
                )
            }

            _gameState.value = GameState(seed = seed, difficulty = difficulty, isOnline = true)
            startPhysicsLoop()
            startEntitySpawner(difficulty, cfg)
            startNetworkSync()
            startScoreAccumulator()
        }
    }

    // ── Loops ─────────────────────────────────────────────────

    private fun startPhysicsLoop() {
        lastTickMs = bridge.nowMs()
        physicsJob = scope.launch {
            while (isActive) {
                if (_gameState.value.isPaused) { delay(16); continue }

                val now = bridge.nowMs()
                val dt  = ((now - lastTickMs).coerceIn(1, 100)).toFloat() / 1000f
                lastTickMs = now
                elapsedMs += (dt * 1000).toLong()

                bridge.physicsTick(dt)
                val cam = CameraSnapshot.fromFloatArray(bridge.getCameraState())
                if (cam != null) bridge.setListenerPos(cam.posX, cam.posY, cam.posZ)

                val entityData       = bridge.tickEntities(cam?.posX ?: 0f, cam?.posY ?: 0f, cam?.posZ ?: 0f, dt)
                val flickerInfluence = bridge.getTotalFlickerInfluence()
                val nearbyCount      = (entityData?.size ?: 0) / 10

                updateFlashlightBattery(dt)
                updateSanity(nearbyCount, flickerInfluence, dt)
                updateStamina(dt)

                _gameState.update { s ->
                    s.copy(
                        sessionElapsed   = elapsedMs,
                        flickerIntensity = flickerInfluence,
                        entitiesNearby   = nearbyCount,
                        score            = score
                    )
                }
                delay(16)
            }
        }
    }

    private fun startEntitySpawner(difficulty: String, cfg: SpawnConfig) {
        entityJob = scope.launch {
            var timer = 0L
            while (isActive) {
                delay(5_000)
                timer += 5_000
                if (timer >= cfg.spawnIntervalMs) {
                    timer = 0
                    val typeId = (Math.random() * 8).toInt()
                    val entity = EntityType.entries.getOrNull(typeId) ?: EntityType.SMILER
                    bridge.spawnEntity(
                        x = (Math.random() * 80 - 40).toFloat(), y = 0f,
                        z = (Math.random() * 80 - 40).toFloat(),
                        speed  = entity.baseSpeed * cfg.speedMult,
                        hear   = 12f, sight = 18f * cfg.sightMult, aggro = 9f,
                        typeId = typeId
                    )
                }
            }
        }
    }

    private fun startNetworkSync() {
        networkJob = scope.launch {
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
            while (isActive) {
                if (!_gameState.value.isPaused) {
                    score += when (_gameState.value.difficulty) {
                        "hard"   -> 5L
                        "normal" -> 3L
                        else     -> 1L
                    }
                }
                delay(1_000)
            }
        }
    }

    // ── State updaters ────────────────────────────────────────

    private fun processIncomingPacket(raw: ByteArray) {
        if (raw.size < 8) return
    }

    private fun updateFlashlightBattery(dt: Float) {
        val s = _gameState.value
        if (!s.flashlightOn) return
        val newBat = (s.flashlightBattery - dt * 0.005f).coerceAtLeast(0f)
        _gameState.update { it.copy(flashlightBattery = newBat, flashlightOn = newBat > 0f) }
    }

    private fun updateSanity(nearbyEntities: Int, flickerInfluence: Float, dt: Float) {
        val s = _gameState.value
        val drain = (nearbyEntities * 0.5f + flickerInfluence * 2f) * dt
        val regen = if (nearbyEntities == 0 && flickerInfluence < 0.1f) dt * 0.3f else 0f
        _gameState.update { it.copy(sanity = (s.sanity - drain + regen).coerceIn(0f, 100f)) }
    }

    private fun updateStamina(dt: Float) {
        val s = _gameState.value
        val newStamina = (s.stamina + dt * 8f).coerceAtMost(s.staminaMax)
        if (newStamina != s.stamina) _gameState.update { it.copy(stamina = newStamina) }
    }

    // ── Public API ────────────────────────────────────────────

    fun applyDamage(amount: Float) {
        val s = _gameState.value
        val newHp = (s.playerHp - amount).coerceAtLeast(0f)
        _gameState.update { it.copy(playerHp = newHp, isGameOver = newHp <= 0f) }
        if (newHp <= 0f) onGameOver()
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
            runCatching {
                api.submitScore(
                    ScoreSubmitRequest(
                        level      = s.level,
                        score      = score,
                        survived   = if (s.isEscaped) 1 else 0,
                        difficulty = s.difficulty,
                        sessionMs  = elapsedMs,
                        kills      = kills
                    )
                )
            }
        }
    }

    private fun onGameOver() {
        bridge.triggerMonster(1.0f)
        submitScoreToServer()
        physicsJob?.cancel()
        entityJob?.cancel()
        networkJob?.cancel()
        scoreJob?.cancel()
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
        val ch = NotificationChannel(CHANNEL_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW)
            .apply { setShowBadge(false) }
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    private fun buildNotif(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.loading_text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true).setSilent(true).build()
}
