package com.omni.backrooms

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.os.Binder
import android.os.Build
import android.os.IBinder
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
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.*
import javax.inject.Inject

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
    suspend fun getRooms(@Query("q") query: String?, @Query("locked") locked: Boolean?, @Query("lang") language: String?, @Query("page") page: Int, @Query("pageSize") pageSize: Int): RoomPage
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
    suspend fun getLeaderboard(@Query("page") page: Int=0, @Query("pageSize") size: Int=50, @Query("difficulty") difficulty: String?=null, @Query("region") region: String?=null): LeaderboardPage
    @POST("player/score")
    suspend fun submitScore(@Body body: ScoreSubmitRequest): BaseResponse
    @POST("auth/google")
    suspend fun loginWithGoogle(@Body body: GoogleAuthRequest): AuthResponse
    @POST("auth/refresh")
    suspend fun refreshToken(@Body body: RefreshRequest): AuthResponse
    @POST("auth/logout")
    suspend fun logout(): BaseResponse
    @GET("market/items")
    suspend fun getMarketItems(@Query("category") category: String?, @Query("page") page: Int=0): MarketPage
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

data class UiButtonLayout(val buttonId: String, val offset: Offset, val sizeScale: Float=1f)

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
    val rank: Int, val playerId: Int, val playerName: String, val avatarUrl: String?,
    val level: Int, val score: Long, val survived: Int, val difficulty: String, val region: String="TR"
)

data class ChatMessage(val senderId: Int, val senderName: String, val text: String, val timestampMs: Long=System.currentTimeMillis())

data class NetworkPlayerState(
    val peerId: Int, val posX: Float, val posY: Float, val posZ: Float,
    val yaw: Float, val pitch: Float, val animState: Int=0, val hp: Float=100f,
    val ping: Int=0, val isConnected: Boolean=true, val charId: String="wanderer"
)

data class CameraSnapshot(
    val posX: Float, val posY: Float, val posZ: Float,
    val yaw: Float, val pitch: Float, val roll: Float,
    val fov: Float, val bobAmount: Float, val bobPhase: Float
) {
    companion object {
        fun fromFloatArray(data: FloatArray?): CameraSnapshot? {
            if (data==null||data.size<9) return null
            return CameraSnapshot(data[0],data[1],data[2],data[3],data[4],data[5],data[6],data[7],data[8])
        }
    }
}

data class LevelSegment(
    val posX: Float, val posY: Float, val width: Float, val length: Float, val height: Float,
    val lightPhase: Float, val lightIntensity: Float, val lightSpeed: Float, val lightBroken: Boolean,
    val roomType: Int, val wallDamage: Float, val moisture: Float, val hasHazard: Boolean, val decalCount: Int
) {
    companion object {
        fun fromFloatArray(data: FloatArray, index: Int): LevelSegment? {
            val base=index*14; if (base+13>=data.size) return null
            return LevelSegment(data[base],data[base+1],data[base+2],data[base+3],data[base+4],data[base+5],data[base+6],data[base+7],data[base+8]>0.5f,data[base+9].toInt(),data[base+10],data[base+11],data[base+12]>0.5f,data[base+13].toInt())
        }
    }
}

data class EntityState(
    val id: Int, val posX: Float, val posY: Float, val posZ: Float,
    val aiState: Int, val alertLevel: Float, val hpFraction: Float,
    val flickerInfluence: Float, val typeId: Int, val isActive: Boolean
) {
    companion object {
        fun fromFloatArray(data: FloatArray, index: Int, id: Int): EntityState? {
            val base=index*10; if(base+9>=data.size) return null
            return EntityState(id,data[base],data[base+1],data[base+2],data[base+3].toInt(),data[base+4],data[base+5],data[base+6],data[base+8].toInt(),data[base+9]>0.5f)
        }
    }
}

data class InventoryItem(val itemId: String, val quantity: Int, val slotIndex: Int)
data class PlayerInventory(
    val items     : List<InventoryItem> = emptyList(),
    val maxSlots  : Int                 = 8,
    val weight    : Float               = 0f,
    val maxWeight : Float               = 20f
) {
    val isFull      : Boolean get() = items.size>=maxSlots
    val isOverweight: Boolean get() = weight>maxWeight
}

data class SessionStats(
    val sessionId: String, val startMs: Long, val endMs: Long=0L,
    val difficulty: String, val mapId: String, val finalScore: Long=0L, val survived: Boolean=false,
    val kills: Int=0, val levelsReached: Int=0, val peakSanity: Float=100f,
    val lowestHp: Float=100f, val totalDistance: Float=0f
)

data class CreateRoomRequest(val name: String, val maxPlayers: Int, val difficulty: String, val password: String?, val language: String="TR", val mapId: String="level_0")
data class CreateRoomResponse(val roomId: String, val joinCode: String, val success: Boolean)
data class JoinRoomResponse(val success: Boolean, val roomId: String?, val error: String?)
data class RoomDetail(val id: String, val name: String, val hostId: String, val currentPlayers: Int, val maxPlayers: Int, val difficulty: String, val isLocked: Boolean, val language: String, val mapId: String, val players: List<RoomPlayer>)
data class RoomPlayer(val id: Int, val name: String, val avatarUrl: String?, val isHost: Boolean, val isReady: Boolean, val ping: Int)
data class BaseResponse(val success: Boolean, val message: String?)
data class AvatarRequest(val avatarId: String)
data class PurchaseRequest(val itemType: String, val amount: Int)
data class PurchaseResponse(val success: Boolean, val newBalance: Long, val currency: String)
data class LeaderboardPage(val entries: List<LeaderboardEntry>, val total: Int, val myRank: Int?)
data class ScoreSubmitRequest(val level: Int, val score: Long, val survived: Int, val difficulty: String, val sessionMs: Long, val kills: Int=0)
data class GoogleAuthRequest(val idToken: String)
data class RefreshRequest(val refreshToken: String)
data class AuthResponse(val accessToken: String, val refreshToken: String, val expiresIn: Long, val playerId: Int)
data class MarketPage(val items: List<MarketItemDto>, val total: Int)
data class MarketItemDto(val id: String, val nameTr: String, val nameEn: String, val descTr: String, val descEn: String, val category: String, val price: Long, val currency: String, val imageUrl: String?, val isOwned: Boolean, val isEquipped: Boolean, val isLimited: Boolean, val expiresMs: Long?)
data class BuyRequest(val itemId: String, val currency: String)
data class BuyResponse(val success: Boolean, val newBalance: Long, val error: String?)
data class CharacterDto(val id: String, val nameTr: String, val nameEn: String, val clazz: String, val maxHp: Float, val baseSpeed: Float, val stealthMult: Float, val staminaMult: Float, val abilities: List<String>, val isUnlocked: Boolean, val isEquipped: Boolean, val imageUrl: String?, val price: Long, val currency: String)
data class StoryChapterDto(val id: Int, val titleTr: String, val titleEn: String, val contentTr: String, val contentEn: String, val isUnlocked: Boolean)
data class EventDto(val id: String, val titleTr: String, val titleEn: String, val descriptionTr: String, val descriptionEn: String, val rewardType: String, val rewardAmount: Long, val endMs: Long, val isActive: Boolean)
data class ReportRequest(val reportedId: Int, val reason: String, val details: String)

class RoomRepository @Inject constructor(private val api: ApiService) {
    suspend fun fetchRooms(query: String?, locked: Boolean?, lang: String?, page: Int, pageSize: Int): RoomPage =
        api.getRooms(query, locked, lang, page, pageSize)
    suspend fun createRoom(name: String, maxPlayers: Int, difficulty: String, password: String?): String =
        api.createRoom(CreateRoomRequest(name, maxPlayers, difficulty, password)).roomId
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

    private var physicsJob: Job?=null; private var entityJob: Job?=null
    private var networkJob: Job?=null; private var scoreJob: Job?=null
    private var lastTickMs=0L; private var elapsedMs=0L; private var score=0L; private var kills=0

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
        ServiceCompat.startForeground(
            this, NOTIF_ID, buildNotif(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
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
            ACTION_PAUSE      -> _gameState.update { it.copy(isPaused=true) }
            ACTION_RESUME     -> _gameState.update { it.copy(isPaused=false) }
            ACTION_FLASHLIGHT -> _gameState.update { it.copy(flashlightOn=!it.flashlightOn) }
            ACTION_DAMAGE_ENTITY -> {
                val id     = intent.getIntExtra(EXTRA_ENTITY_ID, -1)
                val damage = intent.getFloatExtra(EXTRA_DAMAGE, 10f)
                if (id >= 0) { bridge.damageEntity(id, damage); kills++; score+=100L; _gameState.update { it.copy(kills=kills, score=score) } }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder
    override fun onDestroy() { scope.cancel(); stopForeground(STOP_FOREGROUND_REMOVE); super.onDestroy() }

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
                bridge.spawnEntity(x=(Math.random()*60-30).toFloat(), y=0f, z=(Math.random()*60-30).toFloat(),
                    speed=entity.baseSpeed*cfg.speedMult, hear=10f+i*1.5f, sight=16f*cfg.sightMult, aggro=8f, typeId=typeId)
            }
            _gameState.value = GameState(seed=seed, difficulty=difficulty, isOnline=false, mapId=mapId)
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
            bridge.setLocalId((Math.random()*Int.MAX_VALUE).toInt())
            val cfg = assetManager.getSpawnConfig(difficulty)
            repeat(cfg.count) { i ->
                bridge.spawnEntity(x=(Math.random()*60-30).toFloat(), y=0f, z=(Math.random()*60-30).toFloat(),
                    speed=2.5f*cfg.speedMult, hear=10f, sight=16f*cfg.sightMult, aggro=8f, typeId=i%8)
            }
            _gameState.value = GameState(seed=seed, difficulty=difficulty, isOnline=true)
            startPhysicsLoop()
            startEntitySpawner(difficulty, cfg)
            startNetworkSync()
            startScoreAccumulator()
        }
    }

    private fun startPhysicsLoop() {
        lastTickMs = bridge.nowMs()
        physicsJob = scope.launch {
            while (isActive) {
                if (_gameState.value.isPaused) { delay(16); continue }
                val now=bridge.nowMs(); val dt=((now-lastTickMs).coerceIn(1,100)).toFloat()/1000f
                lastTickMs=now; elapsedMs+=(dt*1000).toLong()
                bridge.physicsTick(dt)
                val cam = CameraSnapshot.fromFloatArray(bridge.getCameraState())
                if (cam != null) bridge.setListenerPos(cam.posX, cam.posY, cam.posZ)
                val entityData = bridge.tickEntities(cam?.posX?:0f, cam?.posY?:0f, cam?.posZ?:0f, dt)
                val flickerInfluence = bridge.getTotalFlickerInfluence()
                val nearbyCount = (entityData?.size?:0)/10
                updateFlashlightBattery(dt)
                updateSanity(nearbyCount, flickerInfluence, dt)
                updateStamina(dt)
                _gameState.update { s -> s.copy(sessionElapsed=elapsedMs, flickerIntensity=flickerInfluence, entitiesNearby=nearbyCount, score=score) }
                delay(16)
            }
        }
    }

    private fun startEntitySpawner(difficulty: String, cfg: SpawnConfig) {
        entityJob = scope.launch {
            var timer=0L
            while (isActive) {
                delay(5_000); timer+=5_000
                if (timer >= cfg.spawnIntervalMs) {
                    timer=0
                    val typeId=(Math.random()*8).toInt()
                    val entity=EntityType.entries.getOrNull(typeId)?:EntityType.SMILER
                    bridge.spawnEntity(x=(Math.random()*80-40).toFloat(), y=0f, z=(Math.random()*80-40).toFloat(),
                        speed=entity.baseSpeed*cfg.speedMult, hear=12f, sight=18f*cfg.sightMult, aggro=9f, typeId=typeId)
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
                if (!_gameState.value.isPaused) score += when (_gameState.value.difficulty) { "hard" -> 5L; "normal" -> 3L; else -> 1L }
                delay(1_000)
            }
        }
    }

    private fun processIncomingPacket(raw: ByteArray) { if (raw.size < 8) return }

    private fun updateFlashlightBattery(dt: Float) {
        val s=_gameState.value; if (!s.flashlightOn) return
        val nb=(s.flashlightBattery-dt*0.005f).coerceAtLeast(0f)
        _gameState.update { it.copy(flashlightBattery=nb, flashlightOn=nb>0f) }
    }
    private fun updateSanity(nearbyEntities: Int, flickerInfluence: Float, dt: Float) {
        val s=_gameState.value
        val drain=(nearbyEntities*0.5f+flickerInfluence*2f)*dt
        val regen=if (nearbyEntities==0&&flickerInfluence<0.1f) dt*0.3f else 0f
        _gameState.update { it.copy(sanity=(s.sanity-drain+regen).coerceIn(0f,100f)) }
    }
    private fun updateStamina(dt: Float) {
        val s=_gameState.value; val ns=(s.stamina+dt*8f).coerceAtMost(s.staminaMax)
        if (ns!=s.stamina) _gameState.update { it.copy(stamina=ns) }
    }

    fun applyDamage(amount: Float) {
        val s=_gameState.value; val hp=(s.playerHp-amount).coerceAtLeast(0f)
        _gameState.update { it.copy(playerHp=hp, isGameOver=hp<=0f) }
        if (hp<=0f) onGameOver()
    }
    fun heal(amount: Float) { val s=_gameState.value; _gameState.update { it.copy(playerHp=(s.playerHp+amount).coerceAtMost(s.playerMaxHp)) } }
    fun consumeStamina(amount: Float) { _gameState.update { it.copy(stamina=(_gameState.value.stamina-amount).coerceAtLeast(0f)) } }

    fun submitScoreToServer() {
        scope.launch {
            val s=_gameState.value
            runCatching {
                api.submitScore(ScoreSubmitRequest(s.level, score, if (s.isEscaped) 1 else 0, s.difficulty, elapsedMs, kills))
            }
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
        scope.launch { bridge.destroyEntities(); bridge.destroySound(); bridge.destroySocket(); bridge.destroyCore() }
        stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
    }

    private fun createChannel() {
        val ch = NotificationChannel(CHANNEL_ID, getString(R.string.notif_channel_session), NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) }
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }
    private fun buildNotif(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.loading_text))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true).setSilent(true).build()
}

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
    val detail    : RoomDetail?      = null,
    val isReady   : Boolean          = false,
    val allReady  : Boolean          = false,
    val countdown : Int?             = null,
    val isLoading : Boolean          = false,
    val peerPings : Map<Int, Int>    = emptyMap()
)

@kotlinx.coroutines.FlowPreview
@HiltViewModel
class RoomListVM @Inject constructor(private val repo: RoomRepository) : ViewModel() {
    private val _state = MutableStateFlow(RoomListUiState())
    val state: StateFlow<RoomListUiState> = _state.asStateFlow()
    init {
        load()
        viewModelScope.launch {
            _state.map { Triple(it.query, it.filterLocked, it.lang) }.debounce(300).distinctUntilChanged().collect { load() }
        }
    }
    fun onQuery(q: String)    { _state.update { it.copy(query=q, page=0) } }
    fun onLocked(l: Boolean?) { _state.update { it.copy(filterLocked=l, page=0) } }
    fun onLang(l: String?)    { _state.update { it.copy(lang=l, page=0) } }
    fun prev() { if (_state.value.page>0) { _state.update { it.copy(page=it.page-1) }; load() } }
    fun next() { val s=_state.value; if (s.page<s.totalPages-1) { _state.update { it.copy(page=it.page+1) }; load() } }
    private fun load() {
        viewModelScope.launch {
            val s=_state.value; _state.update { it.copy(isLoading=true) }
            runCatching { repo.fetchRooms(s.query, s.filterLocked, s.lang, s.page, 20) }
                .onSuccess { r -> _state.update { it.copy(isLoading=false, rooms=r.rooms, totalPages=maxOf(1,(r.total+19)/20)) } }
                .onFailure {     _state.update { it.copy(isLoading=false) } }
        }
    }
}

@HiltViewModel
class CreateRoomVM @Inject constructor(private val repo: RoomRepository) : ViewModel() {
    private val _state = MutableStateFlow(CreateRoomUiState())
    val state: StateFlow<CreateRoomUiState> = _state.asStateFlow()
    fun onName(n: String)            { _state.update { it.copy(name=n, nameError=validate(n)) } }
    fun onSize(v: Int)               { _state.update { it.copy(size=v.coerceIn(2,4)) } }
    fun onDifficulty(d: String)      { _state.update { it.copy(difficulty=d) } }
    fun onPasswordToggle(e: Boolean) { _state.update { it.copy(passwordEnabled=e, password=if(!e)"" else it.password) } }
    fun onPassword(p: String)        { _state.update { it.copy(password=p) } }
    fun onMapId(m: String)           { _state.update { it.copy(mapId=m) } }
    fun onLanguage(l: String)        { _state.update { it.copy(language=l) } }
    fun onCreate() {
        val s=_state.value; val err=validate(s.name); if(err!=null){ _state.update { it.copy(nameError=err) }; return }
        if(s.passwordEnabled&&s.password.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isCreating=true) }
            runCatching { repo.createRoom(s.name, s.size, s.difficulty, if(s.passwordEnabled) s.password else null) }
                .onSuccess { id -> _state.update { it.copy(isCreating=false, createdRoomId=id) } }
                .onFailure { e  -> _state.update { it.copy(isCreating=false, error=e.message) } }
        }
    }
    private fun validate(n: String): Int? {
        if(n.length<4||n.length>12) return R.string.room_name_error_length
        if(!Regex("^[a-zA-Z0-9 _-]+\$").matches(n)) return R.string.room_name_error_chars
        return null
    }
}

@HiltViewModel
class RoomLobbyVM @Inject constructor(private val api: ApiService, private val bridge: NativeBridge) : ViewModel() {
    private val _state = MutableStateFlow(RoomLobbyUiState())
    val state: StateFlow<RoomLobbyUiState> = _state.asStateFlow()
    fun load(roomId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading=true) }
            runCatching { api.getRoomDetail(roomId) }
                .onSuccess { d -> _state.update { it.copy(isLoading=false, detail=d) } }
                .onFailure {     _state.update { it.copy(isLoading=false) } }
        }
        startPingLoop()
    }
    fun toggleReady() { _state.update { it.copy(isReady=!it.isReady) }; checkAllReady() }
    private fun checkAllReady() {
        val d=_state.value.detail?:return
        if (_state.value.isReady && d.players.filter { !it.isHost }.all { it.isReady }) startCountdown()
    }
    private fun startCountdown() {
        viewModelScope.launch { for (i in 5 downTo 0) { _state.update { it.copy(countdown=i) }; delay(1_000) } }
    }
    private fun startPingLoop() {
        viewModelScope.launch {
            while (isActive) { _state.update { it.copy(peerPings=it.peerPings+(0 to bridge.getLocalPing())) }; delay(2_000) }
        }
    }
}

@kotlinx.coroutines.FlowPreview
@Composable
fun Room(onJoined: () -> Unit, onBack: () -> Unit, vm: RoomListVM = hiltViewModel()) {
    val s by vm.state.collectAsState()
    Box(Modifier.fillMaxSize().background(DarkBg)) {
        CrtOverlay()
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().background(Color.Black.copy(0.65f)).padding(horizontal=8.dp, vertical=4.dp), verticalAlignment=Alignment.CenterVertically) {
                IconButton(onClick=onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint=Yellow) }
                Text(stringResource(R.string.room_list_title), color=Yellow, fontSize=16.sp, fontWeight=FontWeight.Bold, letterSpacing=3.sp)
                Spacer(Modifier.weight(1f))
                if (s.isLoading) CircularProgressIndicator(Modifier.size(18.dp), color=Yellow, strokeWidth=2.dp)
            }
            DividerLine()
            Row(Modifier.fillMaxWidth().padding(horizontal=12.dp, vertical=8.dp), horizontalArrangement=Arrangement.spacedBy(8.dp), verticalAlignment=Alignment.CenterVertically) {
                SearchField(s.query, vm::onQuery, Modifier.weight(1f))
                FilterChip("Açık", s.filterLocked==false) { vm.onLocked(if(s.filterLocked==false) null else false) }
                FilterChip(stringResource(R.string.room_filter_locked), s.filterLocked==true) { vm.onLocked(if(s.filterLocked==true) null else true) }
            }
            DividerLine()
            if (s.rooms.isEmpty() && !s.isLoading) {
                Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) { Text(stringResource(R.string.room_list_empty), color=TextDim, fontSize=13.sp, letterSpacing=2.sp) }
            } else {
                LazyColumn(Modifier.weight(1f), contentPadding=PaddingValues(horizontal=12.dp, vertical=8.dp), verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    items(s.rooms) { room -> RoomRow(room) { onJoined() } }
                }
            }
            DividerLine()
            PagerBar(s.page, s.totalPages, vm::prev, vm::next)
        }
    }
}

@Composable
fun CreateRoom(onCreated: () -> Unit, onBack: () -> Unit, vm: CreateRoomVM = hiltViewModel()) {
    val s by vm.state.collectAsState()
    LaunchedEffect(s.createdRoomId) { if (s.createdRoomId != null) onCreated() }
    Box(Modifier.fillMaxSize().background(DarkBg)) {
        CrtOverlay()
        Column(Modifier.fillMaxSize()) {
            TopBarBack(stringResource(R.string.room_create_title), onBack)
            DividerLine()
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=24.dp, vertical=16.dp), verticalArrangement=Arrangement.spacedBy(14.dp)) {
                Text("ODA ADI", color=TextSec, fontSize=11.sp, letterSpacing=2.sp)
                OmniTextField(s.name, vm::onName, stringResource(R.string.room_create_name_hint), error=s.nameError?.let { stringResource(it) })
                Text(stringResource(R.string.room_create_size_label), color=TextSec, fontSize=11.sp, letterSpacing=2.sp)
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("${s.size} ${stringResource(R.string.room_players_label)}", color=Yellow, fontSize=14.sp, fontWeight=FontWeight.Bold)
                    Slider(s.size.toFloat(), { vm.onSize(it.toInt()) }, valueRange=2f..4f, steps=1,
                        colors=SliderDefaults.colors(thumbColor=Yellow, activeTrackColor=Yellow, inactiveTrackColor=MetalBg), modifier=Modifier.width(180.dp))
                }
                DifficultyRow(s.difficulty, vm::onDifficulty)
                MapPicker(s.mapId, vm::onMapId)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    listOf("TR","EN","DE","RU").forEach { l ->
                        val sel=s.language==l
                        Box(contentAlignment=Alignment.Center, modifier=Modifier.weight(1f).height(34.dp).clip(RoundedCornerShape(2.dp))
                            .background(if(sel) Yellow.copy(0.15f) else MetalBg.copy(0.5f))
                            .border(1.dp, if(sel) Yellow.copy(0.6f) else BorderCol, RoundedCornerShape(2.dp)).clickable { vm.onLanguage(l) }
                        ) { Text(l, color=if(sel) Yellow else TextDim, fontSize=11.sp) }
                    }
                }
                Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    Checkbox(s.passwordEnabled, vm::onPasswordToggle, colors=CheckboxDefaults.colors(checkedColor=Yellow, uncheckedColor=TextDim, checkmarkColor=Color.Black))
                    Icon(if(s.passwordEnabled) Icons.Default.Lock else Icons.Default.LockOpen, null, tint=if(s.passwordEnabled) Yellow else TextDim, modifier=Modifier.size(16.dp))
                    Text(stringResource(R.string.room_create_password_label), color=if(s.passwordEnabled) Yellow else TextDim, fontSize=12.sp)
                }
                if (s.passwordEnabled) OmniTextField(s.password, vm::onPassword, stringResource(R.string.room_create_password_hint), isPassword=true)
                s.error?.let { Text(it, color=DangerRed, fontSize=11.sp) }
                OmniButton(if(s.isCreating) "…" else stringResource(R.string.room_create_confirm), vm::onCreate, enabled=!s.isCreating&&s.nameError==null&&s.name.isNotBlank(), width=400.dp, height=50.dp)
            }
        }
    }
}

@Composable
private fun RoomRow(room: RoomInfo, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(2.dp)).background(MetalBg.copy(0.7f))
        .border(1.dp, BorderCol, RoundedCornerShape(2.dp)).clickable(onClick=onClick).padding(horizontal=12.dp),
        verticalAlignment=Alignment.CenterVertically
    ) {
        Icon(if(room.isLocked) Icons.Default.Lock else Icons.Default.LockOpen, null, tint=if(room.isLocked) DangerRed.copy(0.7f) else SuccessGreen.copy(0.7f), modifier=Modifier.size(14.dp))
        Spacer(Modifier.width(8.dp))
        Text(room.name, color=Yellow, fontSize=13.sp, fontWeight=FontWeight.Medium, modifier=Modifier.weight(1f), maxLines=1, overflow=TextOverflow.Ellipsis)
        Text(room.language, color=TextDim, fontSize=10.sp, letterSpacing=1.sp)
        Spacer(Modifier.width(10.dp))
        Text("${room.currentPlayers}/${room.maxPlayers}", color=TextSec, fontSize=11.sp)
        Spacer(Modifier.width(10.dp))
        Text(room.difficulty.uppercase(), fontSize=10.sp, fontWeight=FontWeight.Bold, letterSpacing=1.sp,
            color=when(room.difficulty){ "easy"->SuccessGreen; "hard"->DangerRed; else->Yellow })
        if (room.ping>0) { Spacer(Modifier.width(8.dp)); Text("${room.ping}ms", color=when{ room.ping<60->SuccessGreen; room.ping<120->CrtAmber; else->DangerRed }, fontSize=9.sp) }
    }
}

@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit, modifier: Modifier) {
    Row(modifier.clip(RoundedCornerShape(2.dp)).background(MetalBg).border(1.dp, BorderCol, RoundedCornerShape(2.dp)).padding(horizontal=8.dp, vertical=6.dp), verticalAlignment=Alignment.CenterVertically) {
        Icon(Icons.Default.Search, null, tint=TextDim, modifier=Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        BasicTextField(query, onQuery, singleLine=true, textStyle=TextStyle(color=Yellow, fontSize=12.sp), cursorBrush=SolidColor(Yellow),
            decorationBox={ inner -> if(query.isEmpty()) Text(stringResource(R.string.room_search_hint), color=TextDim, fontSize=12.sp); inner() })
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(contentAlignment=Alignment.Center, modifier=Modifier.clip(RoundedCornerShape(2.dp))
        .background(if(selected) Yellow.copy(0.15f) else MetalBg.copy(0.5f))
        .border(1.dp, if(selected) Yellow.copy(0.6f) else BorderCol, RoundedCornerShape(2.dp)).clickable(onClick=onClick).padding(horizontal=10.dp, vertical=4.dp)
    ) { Text(label, color=if(selected) Yellow else TextDim, fontSize=10.sp, fontWeight=if(selected) FontWeight.Bold else FontWeight.Normal, letterSpacing=1.sp) }
}

@Composable
private fun DifficultyRow(selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        listOf(Triple(R.string.difficulty_easy,"easy",SuccessGreen),Triple(R.string.difficulty_normal,"normal",Yellow),Triple(R.string.difficulty_hard,"hard",DangerRed)).forEach { (res,key,col) ->
            val sel=selected==key
            Box(contentAlignment=Alignment.Center, modifier=Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(2.dp))
                .background(if(sel) col.copy(0.15f) else MetalBg.copy(0.5f)).border(1.dp, if(sel) col.copy(0.7f) else BorderCol, RoundedCornerShape(2.dp)).clickable { onSelect(key) }
            ) { Text(stringResource(res), color=if(sel) col else TextDim, fontSize=11.sp, fontWeight=if(sel) FontWeight.Bold else FontWeight.Normal) }
        }
    }
}

@Composable
private fun MapPicker(selected: String, onSelect: (String) -> Unit) {
    val maps=listOf("level_0" to "Level 0","level_1" to "Level 1","level_2" to "Level 2","level_3" to "Level 3","level_4" to "Level 4")
    Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Text("Harita", color=TextSec, fontSize=11.sp, letterSpacing=2.sp)
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp), modifier=Modifier.horizontalScroll(rememberScrollState())) {
            maps.forEach { (id,name) ->
                val sel=selected==id
                Box(contentAlignment=Alignment.Center, modifier=Modifier.clip(RoundedCornerShape(2.dp))
                    .background(if(sel) Yellow.copy(0.15f) else MetalBg.copy(0.5f)).border(1.dp, if(sel) Yellow.copy(0.6f) else BorderCol, RoundedCornerShape(2.dp))
                    .clickable { onSelect(id) }.padding(horizontal=10.dp, vertical=6.dp)
                ) { Text(name, color=if(sel) Yellow else TextDim, fontSize=11.sp, fontWeight=if(sel) FontWeight.Bold else FontWeight.Normal) }
            }
        }
    }
}

@Composable
private fun PagerBar(page: Int, total: Int, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color.Black.copy(0.5f)).padding(horizontal=16.dp, vertical=8.dp), Arrangement.Center, Alignment.CenterVertically) {
        OmniButton(stringResource(R.string.room_page_prev), onPrev, enabled=page>0, width=100.dp, height=36.dp)
        Spacer(Modifier.width(16.dp))
        Text("${page+1} / $total", color=TextSec, fontSize=12.sp, letterSpacing=1.sp)
        Spacer(Modifier.width(16.dp))
        OmniButton(stringResource(R.string.room_page_next), onNext, enabled=page<total-1, width=100.dp, height=36.dp)
    }
}
