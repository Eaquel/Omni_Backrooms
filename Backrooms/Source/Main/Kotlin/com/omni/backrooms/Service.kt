package com.omni.backrooms

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
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
    external fun generateChunk(chunkX: Int, chunkZ: Int): FloatArray?
    external fun getMoistureAt(x: Float, y: Float): Float
    external fun applyVhs(bitmap: Bitmap, t: Float, intensity: Float): Boolean
    external fun applyFlicker(bitmap: Bitmap, value: Float)
    external fun setPlayerState(x: Float, y: Float, z: Float, yaw: Float, pitch: Float)
    external fun physicsTick(dt: Float)
    external fun applyMovement(fx: Float, fy: Float, fz: Float)
    external fun setCrouch(crouched: Boolean)

    // --- Cosmetics -------------------------------------------------------
    // Frames and trails are defined in Native/Frame and Native/Trail. Nothing
    // on this side knows a cosmetic's name, shape or behaviour; it asks.
    external fun frameCount(): Int
    external fun frameId(index: Int): String?
    /** base rgb, glow rgb, highlight rgb, tube ratio, shininess. */
    external fun frameSpec(index: Int): FloatArray?
    /** (radius, thickness) per ring position. Static — fetch once and cache. */
    external fun frameProfile(index: Int, samples: Int): FloatArray?
    /** Emission per ring position at time [t], 0..1. */
    external fun frameEmission(index: Int, samples: Int, t: Float): FloatArray?

    external fun trailCount(): Int
    external fun trailId(index: Int): String?
    /** tint rgb, lifetime, scale, spread, mark kind. */
    external fun trailSpec(index: Int): FloatArray?
    external fun trailSetStyle(index: Int)
    external fun trailStep(x: Float, z: Float, yaw: Float, side: Float)
    external fun trailUpdate(dt: Float)
    external fun trailClear()
    /** x, z, yaw, age, side per live stamp. */
    external fun trailCollect(): FloatArray?
    /** Returns [exitX, exitZ, relocated] in world metres; re-anchors the exit when
     *  the player has strayed further than [maxDistM] from it. */
    external fun relocateExit(px: Float, pz: Float, maxDistM: Float): FloatArray?
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
    external fun stopFootstep()
    external fun playTorchClick()
    /** The run-over transition at `t` seconds in: eight floats, see Ending.h. */
    external fun endingParams(kind: Int, t: Float): FloatArray?
    external fun endingDuration(kind: Int): Float
    external fun triggerMonster(intensity: Float)
    /** The title sting, synthesised on the device — there is no audio file. */
    external fun playIntroSting(seconds: Float)
    external fun stopIntroSting()
    external fun stopMonster()
    external fun setListenerPos(x: Float, y: Float, z: Float)
    external fun setSpatialRolloff(ref: Float, maxDist: Float)
    external fun destroySound()
    external fun initEntities()
    external fun spawnEntity(x: Float, y: Float, z: Float, speed: Float, hear: Float, sight: Float, aggro: Float, typeId: Int): Int
    /** [noise] is how loud the player is this tick, 0..1, and it scales every
     *  creature's hearing radius. [torchX]/[torchZ] are the look direction on
     *  the ground plane; together with [torchOn] they are the flashlight cone
     *  the AI is driven off by. */
    external fun tickEntities(
        px: Float, py: Float, pz: Float, dt: Float,
        noise: Float, torchX: Float, torchZ: Float, torchOn: Boolean
    ): FloatArray?
    external fun damageEntity(id: Int, amount: Float)
    external fun getTotalFlickerInfluence(): Float
    external fun destroyEntities()
    external fun nowMs(): Long
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

data class GameSettings(
    val playerName        : String  = "Wanderer",
    val graphicsQuality   : String  = "medium",
    // Off by default. It is a strong, permanently-on filter over the whole
    // screen, and shipping it enabled meant every player's first impression of
    // the level was through scanlines and chroma bleed they never asked for.
    // Anyone who wants the look can switch it on; nobody should have to find
    // the setting to switch it off.
    val vhsEnabled        : Boolean = false,
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
    val colorBlindMode    : String  = "none",
    /** "first" or "third" — camera perspective. */
    val cameraView        : String  = "first",
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
    val world             : WorldInfo = WorldInfo.EMPTY,
    val exitX             : Float   = 0f,
    val exitZ             : Float   = 0f,
    val distanceToExit    : Float   = Float.MAX_VALUE,
    val spawnPhase        : SpawnPhase = SpawnPhase.READY,
    /** Live telemetry, measured rather than faked. */
    val fps               : Int     = 0,
    /** Vertical camera offset in metres, used by the arrival sequence to drop
     *  the view to the floor on impact and raise it back to standing. */
    val eyeOffset         : Float   = 0f,
    val isCrouching       : Boolean = false,
    val isSprinting       : Boolean = false,
    /** 0..1. Drives the hallucination post-process; ramps up as sanity bottoms
     *  out and is pinned at 1 once the mind has gone. */
    val madness           : Float   = 0f,
    /** Set when sanity has taken the player: the body is on the floor, the
     *  camera is on its side, and the run is over. */
    val isMadnessOver     : Boolean = false,
    /** Camera roll in degrees, used by the collapse. */
    val cameraTilt        : Float   = 0f,
    /** Omnium awarded for the run, by how long the player stayed alive. */
    val omniumEarned      : Long    = 0L,
    /** Seconds since the run ended. Drives Native/Ending, which turns it into
     *  the eight post-process parameters the transition is made of — and, via
     *  the last of them, decides when the stats are allowed to appear. */
    val endingElapsed     : Float   = 0f,
    /** The last of Ending's parameters: how far up the stats panel should be.
     *  Kept on the state rather than read from the renderer because Compose
     *  draws the panel and the renderer does not. */
    val endingPanel       : Float   = 0f
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

data class CameraSnapshot(
    val posX     : Float,
    val posY     : Float,
    val posZ     : Float,
    val yaw      : Float,
    val pitch    : Float,
    val roll     : Float,
    val fov      : Float,
    val bobAmount: Float,
    val bobPhase : Float,
    /** Eye above the feet. Varies with crouch, so the renderer subtracts this
     *  rather than a constant when it needs the player's ground position. */
    val eyeHeight: Float = 1.7f
) {
    companion object {
        fun fromFloatArray(data: FloatArray?): CameraSnapshot? {
            if (data == null || data.size < 9) return null
            return CameraSnapshot(
                data[0], data[1], data[2], data[3], data[4], data[5], data[6], data[7], data[8],
                if (data.size > 9) data[9] else 1.7f
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
    val playerInSight   : Boolean,
    val typeId          : Int,
    val isActive        : Boolean,
    /** 0 solid, 1 fully faded out. Rises while the flashlight drives it off and
     *  falls again when it comes back — nothing here dies, so this is the only
     *  thing that ever takes a creature off the screen. */
    val dissolve        : Float = 0f
) {
    /** Fully dissolved and waiting somewhere out of sight. Still simulated. */
    val isAway: Boolean get() = dissolve >= 0.999f

    companion object {
        const val FLOATS_PER_ENTITY = 11

        fun fromFloatArray(data: FloatArray, index: Int, id: Int): EntityState? {
            val base = index * FLOATS_PER_ENTITY
            if (base + (FLOATS_PER_ENTITY - 1) >= data.size) return null
            return EntityState(
                id, data[base], data[base+1], data[base+2], data[base+3].toInt(),
                data[base+4], data[base+5], data[base+6], data[base+7] > 0.5f,
                data[base+8].toInt(), data[base+9] > 0.5f, data[base+10]
            )
        }

        fun listFromFloatArray(data: FloatArray?): List<EntityState> {
            if (data == null || data.isEmpty()) return emptyList()
            val count = data.size / FLOATS_PER_ENTITY
            return (0 until count).mapNotNull { fromFloatArray(data, it, it) }
        }
    }
}

data class MarketItemDto(val id: String, val nameTr: String, val nameEn: String, val descTr: String, val descEn: String, val category: String, val price: Long, val currency: String, val imageUrl: String?, val isOwned: Boolean, val isEquipped: Boolean, val isLimited: Boolean, val expiresMs: Long?)
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
/**
 * A point in a ring around the player. The world is unbounded, so there is no
 * grid to scan for open floor; instead entities are placed at a distance and
 * the native collision resolver pushes any that land in a wall onto open floor
 * on their first tick. Cheap, and correct for a field with no edges.
 */
private fun pickRingPoint(world: WorldInfo, aroundX: Float, aroundZ: Float, minDist: Float): Pair<Float, Float> {
    val angle = Math.random() * Math.PI * 2
    val dist = minDist + Math.random().toFloat() * 26f
    return (aroundX + (kotlin.math.cos(angle) * dist).toFloat()) to
           (aroundZ + (kotlin.math.sin(angle) * dist).toFloat())
}

/** Spawns cfg.count entities across the level's real walkable space, cycling
 *  through every lore creature with its correct native AI id. */
fun spawnInitialEntities(bridge: NativeBridge, world: WorldInfo, cfg: SpawnConfig) {
    if (!world.isValid) return
    val entity = EntityType.SMILER
    repeat(cfg.count) {
        val (sx, sz) = pickRingPoint(world, world.spawnX, world.spawnZ, minDist = 24f)
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
fun spawnOneRandomEntity(bridge: NativeBridge, world: WorldInfo, aroundX: Float, aroundZ: Float, cfg: SpawnConfig) {
    if (!world.isValid) return
    val (sx, sz) = pickRingPoint(world, aroundX, aroundZ, minDist = 16f)
    val entity = EntityType.SMILER
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

/**
 * What the player is giving away this tick.
 *
 * How loud you are is a decision, and it needs to reach the AI as one number
 * rather than as three booleans it has to interpret. Crouching is close enough
 * to silent that a creature has to nearly walk into you; sprinting carries
 * across a whole wing. Standing still is quieter than walking but not silent —
 * you are still breathing, and a value of zero would make "stop moving" an
 * exploit rather than a tactic.
 */
data class PlayerSense(
    val noise  : Float,
    val torchX : Float,
    val torchZ : Float,
    val torchOn: Boolean
) {
    companion object {
        /** Head-bob amplitude is set natively to `speed * 0.04`, so it is a
         *  measurement of how fast the body is actually moving rather than a
         *  guess from which button is held — which matters, because being
         *  shoved by a creature or sliding down a step is noise too. */
        private const val BOB_TO_METRES_PER_SECOND = 25f
        private const val SPRINT_SPEED = 6f

        fun from(state: GameState, cam: CameraSnapshot?): PlayerSense {
            val speed = ((cam?.bobAmount ?: 0f) * BOB_TO_METRES_PER_SECOND)
                .coerceIn(0f, SPRINT_SPEED)
            val effort = speed / SPRINT_SPEED
            val noise = if (state.isCrouching) 0.05f + effort * 0.14f
                        else                   0.20f + effort * 0.80f

            // Native yaw is in degrees, and forward on the ground plane is
            // (sin yaw, cos yaw) — the same basis the renderer builds the view
            // from, so the cone points exactly where the beam is drawn.
            val yaw = Math.toRadians((cam?.yaw ?: 0f).toDouble())
            return PlayerSense(
                noise,
                kotlin.math.sin(yaw).toFloat(),
                kotlin.math.cos(yaw).toFloat(),
                state.flashlightOn && state.flashlightBattery > 0f
            )
        }
    }
}

/** Advances the native sim by [dt] and reads back camera/entity state. Do not
 *  call this from more than one place per logical frame — physicsTick/
 *  tickEntities mutate native state, so calling it twice per frame from two
 *  hosts at once would double-advance the simulation. */
fun stepSimulation(bridge: NativeBridge, dt: Float, state: GameState): TickDerived {
    bridge.physicsTick(dt)
    val cam = CameraSnapshot.fromFloatArray(bridge.getCameraState())
    if (cam != null) bridge.setListenerPos(cam.posX, cam.posY, cam.posZ)
    val sense = PlayerSense.from(state, cam)
    val entityList = EntityState.listFromFloatArray(
        bridge.tickEntities(
            cam?.posX ?: 0f, cam?.posY ?: 0f, cam?.posZ ?: 0f, dt,
            sense.noise, sense.torchX, sense.torchZ, sense.torchOn
        )
    )
    val flicker = bridge.getTotalFlickerInfluence()
    val nearbyCount = entityList.count { e ->
        // A creature that has been driven off is not there. Counting it would
        // drain sanity from something the player cannot see, hear or do
        // anything about — a hidden penalty for having used the torch.
        if (!e.isActive || e.isAway || cam == null) return@count false
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
    @Inject lateinit var settings    : SettingsRepository

    private val binder = LocalBinder()
    private val scope  = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _gameState      = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private var physicsJob: Job? = null
    private var entityJob : Job? = null
    private var scoreJob  : Job? = null

    private var lastTickMs = 0L
    private var elapsedMs  = 0L
    private var score      = 0L
    private var kills      = 0
    /** Segments for the currently loaded level, kept alongside gameState the same
     *  way GameVM keeps its own copy (see stepSimulation/spawnInitialEntities). */
    private var world: WorldInfo = WorldInfo.EMPTY

    companion object {
        private const val CHANNEL_ID    = "omni_session"
        private const val NOTIF_ID      = 2001
        const val ACTION_START_OFFLINE  = "start_offline"
        const val ACTION_STOP           = "stop_game"
        const val ACTION_PAUSE          = "pause_game"
        const val ACTION_RESUME         = "resume_game"
        const val ACTION_FLASHLIGHT     = "flashlight"
        const val ACTION_DAMAGE_ENTITY  = "damage_entity"
        const val EXTRA_DIFFICULTY      = "difficulty"
        const val EXTRA_SEED            = "seed"
        const val EXTRA_MAP_ID          = "map_id"
        const val EXTRA_ENTITY_ID       = "entity_id"
        const val EXTRA_DAMAGE          = "damage"
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        // dataSync, not specialUse: this service exists to keep the netcode and
        // simulation ticking, which is exactly what dataSync describes. Play
        // requires a written justification for specialUse and a game session is
        // not an accepted case for it — using an honest category avoids a review
        // blocker and matches what the service actually does.
        ServiceCompat.startForeground(
            this,
            NOTIF_ID,
            buildNotif(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
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
            world = WorldInfo.parse(bridge.generateLevel(roomBudget, depth = 0))

            val cfg = assetManager.getSpawnConfig(difficulty)
            spawnInitialEntities(bridge, world, cfg)
            _gameState.value = GameState(
                seed = seed, difficulty = difficulty, mapId = "level_0",
                world = world, exitX = world.exitX, exitZ = world.exitZ
            )
            startPhysicsLoop()
            startEntitySpawner(difficulty, cfg)
            startScoreAccumulator()
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
                val derived = stepSimulation(bridge, dt, _gameState.value)
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
                if (timer >= cfg.spawnIntervalMs && world.isValid) {
                    timer = 0
                    val cam = _gameState.value.camera
                    spawnOneRandomEntity(bridge, world, cam?.posX ?: world.spawnX, cam?.posZ ?: world.spawnZ, cfg)
                }
            }
        }
    }


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

    private fun onGameOver() {
        bridge.triggerMonster(1.0f)
        physicsJob?.cancel(); entityJob?.cancel(); scoreJob?.cancel()
    }

    private fun stopSession() {
        onGameOver()
        scope.launch {
            bridge.destroyEntities()
            bridge.destroySound()
            bridge.destroyCore()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * The session channel, at the importance the player's setting asks for.
     *
     * A foreground service must show a notification — Android will not let this
     * one disappear, and killing it would kill the run with it. What the setting
     * can honestly do is decide how loudly it appears, so "off" means MIN: no
     * sound, no badge, collapsed into the status bar shade. That is as off as
     * the platform permits, and it is now actually wired to the toggle, which
     * previously changed nothing at all.
     */
    private fun createChannel() {
        val wanted = runCatching {
            runBlocking { settings.observe().first() }.pushNotifications
        }.getOrDefault(true)
        val importance = if (wanted) NotificationManager.IMPORTANCE_LOW
                         else NotificationManager.IMPORTANCE_MIN
        val ch = NotificationChannel(CHANNEL_ID, getString(R.string.notif_channel_session), importance)
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
    val savedAtMs   : Long,
    // Exact position and facing. Without these a resume restored the stats but
    // re-dropped the player at a fresh random spawn cell.
    val posX        : Float = 0f,
    val posY        : Float = 1.7f,
    val posZ        : Float = 0f,
    val yaw         : Float = 0f,
    val pitch       : Float = 0f
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

    /**
     * Fire-and-forget clear, for the same reason [saveDetached] exists.
     *
     * A finished run is wiped exactly when the game screen is going away, and a
     * ViewModel-scoped coroutine is cancelled before it can touch the store —
     * so the clear lost its race with the teardown save and the run the player
     * had just lost stayed sitting behind "Continue".
     */
    fun clearDetached() {
        ioScope.launch {
            runCatching { ctx.identityStore.edit { it.remove(key) } }
                .onSuccess { OmniLog.i("Save", "run cleared") }
                .onFailure { OmniLog.e("Save", "clear failed", it) }
        }
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

    const val LOG_DIR_NAME = "Backrooms_Log"

    /**
     * Opens the log file somewhere the player can actually reach.
     *
     * The old sink was ctx.filesDir, which is app-private: unreachable without
     * root or adb, so a player who hit a problem had no way to send anything
     * back. The candidates below are tried in order of how easy they are to
     * find with an ordinary file manager, and the first writable one wins:
     *
     *   1. <shared storage>/Documents/Backrooms_Log  — top level, obvious.
     *      Available without any permission on API 29+ via the app's own
     *      external files being scoped; on older releases it needs the legacy
     *      write permission, so it simply fails and we fall through.
     *   2. Android/data/<pkg>/files/Backrooms_Log    — visible in every file
     *      manager, no permission, works on every API level this app supports.
     *   3. ctx.filesDir/Backrooms_Log                — last resort, private,
     *      but never lets logging fail outright.
     *
     * Whichever wins is reported in the first line of the log and again on the
     * settings screen, so there is no guessing about where to look.
     */
    fun attach(ctx: Context) {
        val candidates = buildList {
            runCatching {
                add(java.io.File(
                    android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOCUMENTS), LOG_DIR_NAME))
            }
            runCatching { ctx.getExternalFilesDir(null) }.getOrNull()
                ?.let { add(java.io.File(it, LOG_DIR_NAME)) }
            add(java.io.File(ctx.filesDir, LOG_DIR_NAME))
        }

        for (dir in candidates) {
            val ok = runCatching {
                if (!dir.exists()) dir.mkdirs()
                val f = java.io.File(dir, "session.log")
                // Prove it is writable before committing to it — a directory
                // that mkdirs() reports as created can still reject writes.
                f.appendText("")
                if (f.length() > 512 * 1024) f.delete()
                sink = f
                true
            }.getOrElse { false }
            if (ok) break
        }

        i("Log", "attached; sink=${sink?.absolutePath ?: "none (in-memory only)"}")
    }

    /** Where the log actually ended up, for the settings screen to show. */
    fun sinkPath(): String? = sink?.absolutePath

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
        runCatching { sink?.appendText(line + "\n") }
    }

    /** The recent history, newest last — this is what gets attached to a crash
     *  report so the lines leading up to the failure are visible. */
    fun recentHistory(): String = synchronized(lock) { ring.joinToString("\n") }

    fun clearRing() = synchronized(lock) { ring.clear() }
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
        val TRAIL        = stringPreferencesKey("trail")
        val OWNED_TRAILS = stringPreferencesKey("owned_trails")
        val VIP          = stringPreferencesKey("vip")
        val BEST_SURVIVAL= longPreferencesKey("best_survival_ms")
        val OMNIUM       = longPreferencesKey("omnium_balance")
    }

    fun observeAvatarUri(): Flow<String?> = ctx.identityStore.data.map { it[Keys.AVATAR_URI] }

    /**
     * The equipped frame and trail.
     *
     * Both default to the first entry in the native catalogue rather than to a
     * hardcoded name — the stored value is only ever an id from Native/Frame or
     * Native/Trail, and a player who still has "halogen" or "default" written
     * from an older build resolves to the first real cosmetic instead of to
     * nothing at all.
     */
    fun observeFrame(): Flow<String> = ctx.identityStore.data.map {
        it[Keys.FRAME] ?: defaultCosmetic(frames = true)
    }
    fun observeTrail(): Flow<String> = ctx.identityStore.data.map {
        it[Keys.TRAIL] ?: defaultCosmetic(frames = false)
    }
    fun observeOwnedTrails(): Flow<List<String>> = ctx.identityStore.data.map { prefs ->
        prefs[Keys.OWNED_TRAILS]?.split(',')?.filter { it.isNotBlank() } ?: emptyList()
    }

    private fun defaultCosmetic(frames: Boolean): String = runCatching {
        val b = NativeBridge()
        (if (frames) b.frameId(0) else b.trailId(0))?.takeIf { it.isNotEmpty() }
    }.getOrNull() ?: if (frames) "Face_Of_Darkness" else "Dust_Trail"
    fun observeBestSurvival(): Flow<Long> = ctx.identityStore.data.map { it[Keys.BEST_SURVIVAL] ?: 0L }

    /** The local Omnium wallet. Runs pay into this directly so surviving is worth
     *  something with no server in the loop. */
    fun observeOmnium(): Flow<Long> = ctx.identityStore.data.map { it[Keys.OMNIUM] ?: 0L }

    suspend fun addOmnium(amount: Long) {
        if (amount <= 0L) return
        runCatching {
            ctx.identityStore.edit { prefs ->
                prefs[Keys.OMNIUM] = (prefs[Keys.OMNIUM] ?: 0L) + amount
            }
        }
    }

    fun observeOwnedFrames(): Flow<List<String>> = ctx.identityStore.data.map { prefs ->
        prefs[Keys.OWNED_FRAMES]?.split(',')?.filter { it.isNotBlank() } ?: emptyList()
    }

    suspend fun setAvatarUri(uri: String) {
        runCatching { ctx.identityStore.edit { it[Keys.AVATAR_URI] = uri } }
    }

    suspend fun setFrame(key: String) {
        runCatching { ctx.identityStore.edit { it[Keys.FRAME] = key } }
    }

    suspend fun grantFrame(key: String) = grant(Keys.OWNED_FRAMES, key)

    suspend fun setTrail(key: String) {
        runCatching { ctx.identityStore.edit { it[Keys.TRAIL] = key } }
    }

    /**
     * VIP, held locally.
     *
     * The profile carries an isVip from the server, but the reward multiplier
     * has to work on a device that is offline for a whole run — which is the
     * mode this game is mostly played in — so entitlement is mirrored here.
     */
    fun observeVip(): Flow<Boolean> = ctx.identityStore.data.map { it[Keys.VIP] == "1" }

    suspend fun setVip(active: Boolean) {
        runCatching { ctx.identityStore.edit { it[Keys.VIP] = if (active) "1" else "0" } }
    }

    suspend fun grantTrail(key: String) = grant(Keys.OWNED_TRAILS, key)

    private suspend fun grant(key: Preferences.Key<String>, value: String) {
        runCatching {
            ctx.identityStore.edit { prefs ->
                val cur = prefs[key]?.split(',')?.filter { it.isNotBlank() }?.toMutableSet()
                    ?: mutableSetOf()
                cur.add(value)
                prefs[key] = cur.joinToString(",")
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
    ENGLISH   ("en", "English"),
    TURKISH   ("tr", "Türkçe"),
    GERMAN    ("de", "Deutsch"),
    SPANISH   ("es", "Español"),
    FRENCH    ("fr", "Français"),
    ITALIAN   ("it", "Italiano"),
    PORTUGUESE("pt", "Português"),
    RUSSIAN   ("ru", "Русский"),
    JAPANESE  ("ja", "日本語"),
    CHINESE   ("zh", "中文");

    companion object {
        const val SYSTEM = "system"

        fun fromTag(tag: String?): AppLanguage? = entries.firstOrNull { it.tag == tag }

        /**
         * Resolves the DEVICE's own language to a supported one, or English.
         *
         * Read from the system resources, not from Locale.getDefault(). That
         * distinction is the whole fix for "System" not going back to the
         * device language: applyAppLanguage() calls Locale.setDefault() to make
         * the app's own strings resolve, which overwrites the process default
         * for good. So after a player picked English once, getDefault() was
         * English forever, and asking for the device language returned English
         * on a Turkish phone — the setting looked broken because it could never
         * recover the value it was supposed to fall back to.
         *
         * Resources.getSystem() is the framework's own resource table and is
         * unaffected by anything this app sets, so it still knows what the
         * device is actually configured for.
         */
        fun matchDevice(): AppLanguage {
            val deviceTag = runCatching {
                val cfg = android.content.res.Resources.getSystem().configuration
                @Suppress("DEPRECATION")
                val loc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    cfg.locales.get(0) else cfg.locale
                loc.language.lowercase(Locale.ROOT)
            }.getOrElse { Locale.getDefault().language.lowercase(Locale.ROOT) }
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
    return LocalisedContextWrapper(base, locale)
}

/**
 * Localises resources while keeping the context chain intact.
 *
 * The obvious implementation — returning `createConfigurationContext(config)`
 * directly — is wrong when the result is fed to Compose's LocalContext. That
 * call hands back a bare ContextImpl, which is neither an Activity nor a
 * ContextWrapper, so anything that walks `baseContext` upward looking for the
 * hosting Activity hits a dead end immediately. `hiltViewModel()` does exactly
 * that walk, and throws "Expected an activity context for creating a
 * HiltViewModelFactory".
 *
 * Wrapping instead of replacing keeps the Activity reachable through
 * `baseContext` while still serving localised resources, which is all the UI
 * actually needs.
 */
private class LocalisedContextWrapper(base: Context, locale: Locale) : ContextWrapper(base) {
    private val localisedResources: Resources by lazy {
        val config = Configuration(base.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        base.createConfigurationContext(config).resources
    }

    override fun getResources(): Resources = localisedResources
}


// ============================================================================
// Infinite world streaming.
//
// The level has no bounds, so it cannot be uploaded as one mesh. Geometry is
// built per chunk on demand and kept only while the player is near it.
// ============================================================================

/** Header returned by the native generator. The world itself is unbounded; this
 *  carries only the run's fixed points and the streaming granularity. */
data class WorldInfo(
    val cellSize   : Float = 3.2f,
    val height     : Float = 2.6f,
    val spawnX     : Float = 0f,
    val spawnZ     : Float = 0f,
    val exitX      : Float = 0f,
    val exitZ      : Float = 0f,
    val chunkCells : Int   = 24
) {
    val isValid: Boolean get() = chunkCells > 0

    companion object {
        val EMPTY = WorldInfo(chunkCells = 0)

        fun parse(data: FloatArray?): WorldInfo {
            if (data == null || data.size < 8) return EMPTY
            return WorldInfo(
                cellSize = data[0], height = data[1],
                spawnX = data[2], spawnZ = data[3],
                exitX = data[4], exitZ = data[5],
                chunkCells = data[6].toInt()
            )
        }
    }
}

/** One streamed chunk of cells. */
class WorldChunk(
    val chunkX: Int,
    val chunkZ: Int,
    val cells: Int,
    private val solid  : ByteArray,
    private val light  : FloatArray,
    private val feature: ByteArray,
    private val fixture: ByteArray,
    private val power  : FloatArray
) {
    /**
     * Queries are in chunk-local cells, but the arrays carry a one-cell apron on
     * every side, so -1 and [cells] are real answers read from the neighbouring
     * chunk rather than assumptions.
     *
     * That apron is what removed the phantom walls at chunk joins: the mesher used
     * to treat everything past the edge as solid and emit a wall there, while
     * collision (which queries the field directly) knew the corridor carried on —
     * so you saw a wall and walked through it.
     */
    private val stride = cells + 2

    private fun index(x: Int, z: Int): Int = (z + 1) * stride + (x + 1)
    private fun inRange(x: Int, z: Int): Boolean = x >= -1 && z >= -1 && x <= cells && z <= cells

    fun solidAt(x: Int, z: Int): Boolean =
        if (!inRange(x, z)) true else solid[index(x, z)] != 0.toByte()

    /** Baked illuminance, continuous. ~1.0 is a normally lit corridor. */
    fun lightAt(x: Int, z: Int): Float =
        if (!inRange(x, z)) 0.6f else light[index(x, z)]

    /** 0..1 mains health, used to scale how badly the lights struggle. */
    fun powerAt(x: Int, z: Int): Float =
        if (!inRange(x, z)) 1f else power[index(x, z)]

    fun featureAt(x: Int, z: Int): Int =
        if (!inRange(x, z)) 0 else feature[index(x, z)].toInt()

    fun fixtureAt(x: Int, z: Int): Int =
        if (!inRange(x, z)) 0 else fixture[index(x, z)].toInt()

    companion object {
        const val FLOATS_PER_CELL = 5

        fun parse(chunkX: Int, chunkZ: Int, cells: Int, data: FloatArray?): WorldChunk? {
            if (data == null || cells <= 0) return null
            val padded = cells + 2
            val n = padded * padded
            if (data.size < n * FLOATS_PER_CELL) return null
            val solid = ByteArray(n); val light = FloatArray(n)
            val feature = ByteArray(n); val fixture = ByteArray(n)
            val power = FloatArray(n)
            var p = 0
            for (i in 0 until n) {
                solid[i]   = data[p].toInt().toByte()
                light[i]   = data[p + 1]
                feature[i] = data[p + 2].toInt().toByte()
                fixture[i] = data[p + 3].toInt().toByte()
                power[i]   = data[p + 4]
                p += FLOATS_PER_CELL
            }
            return WorldChunk(chunkX, chunkZ, cells, solid, light, feature, fixture, power)
        }
    }
}
