package com.omni.backrooms

import android.app.*
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@AndroidEntryPoint
class Session_Service : Service() {

    inner class LocalBinder : Binder() {
        fun getService(): Session_Service = this@Session_Service
    }

    @Inject lateinit var bridge: Native_Bridge
    @Inject lateinit var settingsRepo: Settings_Repository

    private val binder    = LocalBinder()
    private val scope     = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _gameState= MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState

    private var physicsJob    : Job? = null
    private var networkJob    : Job? = null
    private var entityJob     : Job? = null
    private var lastTickMs    = 0L
    private var elapsedMs     = 0L

    companion object {
        private const val CHANNEL_ID = "omni_session"
        private const val NOTIF_ID   = 2001
        const val ACTION_START_OFFLINE = "start_offline"
        const val ACTION_START_ONLINE  = "start_online"
        const val ACTION_STOP          = "stop_game"
        const val EXTRA_DIFFICULTY     = "difficulty"
        const val EXTRA_ROOM_ID        = "room_id"
        const val EXTRA_SEED           = "seed"
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIF_ID, buildNotif())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_OFFLINE -> {
                val diff = intent.getStringExtra(EXTRA_DIFFICULTY) ?: "normal"
                val seed = intent.getLongExtra(EXTRA_SEED, System.currentTimeMillis())
                startOfflineSession(diff, seed)
            }
            ACTION_START_ONLINE -> {
                val roomId = intent.getStringExtra(EXTRA_ROOM_ID) ?: return START_NOT_STICKY
                val diff   = intent.getStringExtra(EXTRA_DIFFICULTY) ?: "normal"
                startOnlineSession(roomId, diff)
            }
            ACTION_STOP -> stopSession()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder
    override fun onDestroy() { scope.cancel(); stopForeground(STOP_FOREGROUND_REMOVE); super.onDestroy() }

    private fun startOfflineSession(difficulty: String, seed: Long) {
        scope.launch {
            bridge.initCore(seed)
            bridge.initSound()
            bridge.initEntities()
            bridge.setAmbienceLevel(0.45f)

            val segments = bridge.generateLevel(40, 0)
            if (segments != null) spawnEntitiesForDifficulty(difficulty)

            _gameState.value = GameState(
                seed = seed, difficulty = difficulty, isOnline = false)

            startGameLoops()
        }
    }

    private fun startOnlineSession(roomId: String, difficulty: String) {
        scope.launch {
            val seed = System.currentTimeMillis()
            bridge.initCore(seed)
            bridge.initSound()
            bridge.initEntities()
            bridge.initSocket(0)
            bridge.setLocalId((Math.random() * Int.MAX_VALUE).toInt())

            _gameState.value = GameState(
                seed = seed, difficulty = difficulty, isOnline = true)

            startGameLoops()
            startNetworkLoop()
        }
    }

    private fun startGameLoops() {
        lastTickMs = System.currentTimeMillis()

        physicsJob = scope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val dt  = ((now - lastTickMs).coerceIn(1, 100)).toFloat() / 1000f
                lastTickMs = now
                elapsedMs += (dt * 1000).toLong()

                bridge.physicsTick(dt)

                val cam = bridge.getCameraState()
                if (cam != null && cam.size >= 3)
                    bridge.setListenerPos(cam[0], cam[1], cam[2])

                val entityData = bridge.tickEntities(
                    _gameState.value.sanity, 0f, 0f, dt)

                val flickerInfluence = bridge.getTotalFlickerInfluence()

                _gameState.value = _gameState.value.copy(
                    sessionElapsed   = elapsedMs,
                    flickerIntensity = flickerInfluence,
                    entitiesNearby   = (entityData?.size ?: 0) / 10
                )

                delay(16)
            }
        }

        entityJob = scope.launch {
            var spawnTimer = 0L
            val spawnInterval = when (_gameState.value.difficulty) {
                "hard"   -> 15_000L
                "normal" -> 25_000L
                else     -> 45_000L
            }
            while (isActive) {
                delay(5000)
                spawnTimer += 5000
                if (spawnTimer >= spawnInterval) {
                    spawnTimer = 0
                    val typeId = (Math.random() * 8).toInt()
                    bridge.spawnEntity(
                        x = (Math.random() * 40 - 20).toFloat(),
                        y = 0f,
                        z = (Math.random() * 40 - 20).toFloat(),
                        speed = 2.5f + (Math.random() * 2).toFloat(),
                        hear  = 12f,
                        sight = 18f,
                        aggro = 8f,
                        typeId = typeId
                    )
                }
            }
        }
    }

    private fun startNetworkLoop() {
        networkJob = scope.launch {
            while (isActive) {
                val cam = bridge.getCameraState()
                if (cam != null && cam.size >= 5) {
                    bridge.buildPosPacket(cam[0], cam[1], cam[2], cam[3], cam[4])
                }
                val packets = bridge.drainRecvQueue()
                packets?.forEach { _ -> }
                delay(50)
            }
        }
    }

    private fun spawnEntitiesForDifficulty(difficulty: String) {
        val count = when (difficulty) {
            "hard"   -> 6
            "normal" -> 3
            else     -> 1
        }
        repeat(count) { i ->
            bridge.spawnEntity(
                x = (Math.random() * 60 - 30).toFloat(),
                y = 0f,
                z = (Math.random() * 60 - 30).toFloat(),
                speed = 2.0f + i * 0.5f,
                hear  = 10f + i * 2f,
                sight = 15f + i * 2f,
                aggro = 7f + i,
                typeId = i % 8
            )
        }
    }

    private fun stopSession() {
        physicsJob?.cancel()
        entityJob?.cancel()
        networkJob?.cancel()
        scope.launch {
            bridge.destroyEntities()
            bridge.destroySound()
            bridge.destroySocket()
            bridge.destroyCore()
        }
    }

    private fun createChannel() {
        val ch = NotificationChannel(CHANNEL_ID, getString(R.string.app_name),
            NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) }
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    private fun buildNotif(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.loading_text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true).setSilent(true).build()
}
