package com.omni.backrooms

import android.app.*
import android.content.Intent
import android.media.*
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.rtc2.*
import io.agora.rtc2.audio.AudioTrackConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

data class VoicePeer(
    val uid       : Int,
    val name      : String    = "???",
    val isMuted   : Boolean   = false,
    val isSpeaking: Boolean   = false,
    val volume    : Int       = 0,
    val ping      : Int       = 0
)

data class VoiceUiState(
    val isConnected   : Boolean       = false,
    val isMuted       : Boolean       = false,
    val isPttMode     : Boolean       = false,
    val isPttActive   : Boolean       = false,
    val peers         : List<VoicePeer> = emptyList(),
    val myVolume      : Int           = 0,
    val roomId        : String        = "",
    val errorMsg      : String?       = null,
    val noiseCancel   : Boolean       = true,
    val echoCancellation: Boolean     = true,
    val spatialAudio  : Boolean       = false
)

@Singleton
class VoiceManager @Inject constructor(
    private val bridge: Native_Bridge
) {
    private val _state        = MutableStateFlow(VoiceUiState())
    val state: StateFlow<VoiceUiState> = _state.asStateFlow()

    private var rtcEngine     : RtcEngine?   = null
    private var audioRecord   : AudioRecord? = null
    private var captureJob    : Job?         = null
    private val scope         = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val rtcHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            _state.update { it.copy(isConnected=true, roomId=channel) }
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            _state.update { s ->
                val peers = s.peers.toMutableList()
                if (peers.none { it.uid == uid }) peers.add(VoicePeer(uid))
                s.copy(peers=peers)
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            _state.update { it.copy(peers=it.peers.filter { p -> p.uid != uid }) }
        }

        override fun onAudioVolumeIndication(speakers: Array<out AudioVolumeInfo>?, totalVolume: Int) {
            speakers ?: return
            val myVol = speakers.firstOrNull { it.uid == 0 }?.volume ?: 0
            _state.update { s ->
                val updated = s.peers.map { peer ->
                    val vol = speakers.firstOrNull { it.uid == peer.uid }?.volume ?: 0
                    peer.copy(volume=vol, isSpeaking=vol > 15)
                }
                s.copy(peers=updated, myVolume=myVol)
            }
        }

        override fun onRemoteAudioStats(stats: RemoteAudioStats) {
            val networkDelay = stats.networkTransportDelay
            _state.update { s ->
                s.copy(peers=s.peers.map { if (it.uid==stats.uid) it.copy(ping=networkDelay) else it })
            }
        }

        override fun onError(err: Int) {
            _state.update { it.copy(errorMsg="RTC Hata: $err") }
        }

        override fun onLeaveChannel(stats: RtcStats) {
            _state.update { it.copy(isConnected=false, peers=emptyList()) }
        }
    }

    fun initEngine(appId: String, ctx: android.content.Context) {
        runCatching {
            val cfg = RtcEngineConfig().apply {
                mContext      = ctx
                mAppId        = appId
                mEventHandler = rtcHandler
            }
            rtcEngine = RtcEngine.create(cfg).apply {
                setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
                setClientRole(Constants.CLIENT_ROLE_BROADCASTER)
                enableAudio()
                setAudioProfile(
                    Constants.AUDIO_PROFILE_SPEECH_STANDARD,
                    Constants.AUDIO_SCENARIO_GAME_STREAMING
                )
                enableAudioVolumeIndication(200, 3, true)
                setDefaultAudioRoutetoSpeakerphone(true)
                adjustRecordingSignalVolume(100)
                adjustPlaybackSignalVolume(100)
            }
        }
    }

    fun join(roomId: String, uid: Int) {
        rtcEngine?.joinChannel(null, roomId, uid, null)
        startCapture()
    }

    fun leave() {
        stopCapture()
        rtcEngine?.leaveChannel()
        _state.update { it.copy(isConnected=false, peers=emptyList()) }
    }

    fun mute(muted: Boolean) {
        _state.update { it.copy(isMuted=muted) }
        rtcEngine?.muteLocalAudioStream(muted)
    }

    fun setPttMode(enabled: Boolean) {
        _state.update { it.copy(isPttMode=enabled) }
        if (enabled) mute(true)
    }

    fun pttPress() {
        if (!_state.value.isPttMode) return
        _state.update { it.copy(isPttActive=true) }
        rtcEngine?.muteLocalAudioStream(false)
    }

    fun pttRelease() {
        if (!_state.value.isPttMode) return
        _state.update { it.copy(isPttActive=false) }
        rtcEngine?.muteLocalAudioStream(true)
    }

    fun setNoiseCancellation(on: Boolean) {
        _state.update { it.copy(noiseCancel=on) }
        rtcEngine?.setParameters("{\"che.audio.ans.enable\":$on}")
    }

    fun setEchoCancellation(on: Boolean) {
        _state.update { it.copy(echoCancellation=on) }
        rtcEngine?.setParameters("{\"che.audio.aec.enable\":$on}")
    }

    fun setSpatialAudio(on: Boolean) {
        _state.update { it.copy(spatialAudio=on) }
    }

    fun setVolume(uid: Int, vol: Int) {
        rtcEngine?.adjustUserPlaybackSignalVolume(uid, vol.coerceIn(0, 400))
    }

    fun setMasterVolume(vol: Float) {
        rtcEngine?.adjustPlaybackSignalVolume((vol * 400).toInt().coerceIn(0, 400))
        bridge.setMasterVolume(vol)
    }

    private fun startCapture() {
        val bufSize = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
            bufSize * 2
        )
        audioRecord?.startRecording()
        captureJob = scope.launch {
            val buf = ByteArray(bufSize)
            while (isActive && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val read = audioRecord?.read(buf, 0, buf.size) ?: break
                if (read > 0 && !_state.value.isMuted) {
                    val pkt = bridge.buildVoicePacket(buf, read)
                    pkt?.let { }
                }
            }
        }
    }

    private fun stopCapture() {
        captureJob?.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    fun destroy() {
        stopCapture()
        scope.cancel()
        RtcEngine.destroy()
        rtcEngine = null
    }
}

@AndroidEntryPoint
class Voice_Chat : Service() {

    @Inject lateinit var voiceManager: VoiceManager

    companion object {
        private const val CHANNEL_ID = "omni_voice"
        private const val NOTIF_ID   = 2002
        const val ACTION_JOIN        = "action_join"
        const val ACTION_LEAVE       = "action_leave"
        const val ACTION_MUTE        = "action_mute"
        const val ACTION_UNMUTE      = "action_unmute"
        const val ACTION_PTT_PRESS   = "action_ptt_press"
        const val ACTION_PTT_RELEASE = "action_ptt_release"
        const val ACTION_PTT_MODE    = "action_ptt_mode"
        const val ACTION_VOLUME      = "action_volume"
        const val EXTRA_ROOM         = "room_id"
        const val EXTRA_UID          = "uid"
        const val EXTRA_VOLUME       = "volume"
        const val EXTRA_PTT_ENABLED  = "ptt_enabled"
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIF_ID, buildNotif())
        voiceManager.initEngine(BuildConfig.AGORA_APP_ID, applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_JOIN        -> {
                val room = intent.getStringExtra(EXTRA_ROOM) ?: return START_NOT_STICKY
                val uid  = intent.getIntExtra(EXTRA_UID, 0)
                voiceManager.join(room, uid)
            }
            ACTION_LEAVE       -> voiceManager.leave()
            ACTION_MUTE        -> voiceManager.mute(true)
            ACTION_UNMUTE      -> voiceManager.mute(false)
            ACTION_PTT_PRESS   -> voiceManager.pttPress()
            ACTION_PTT_RELEASE -> voiceManager.pttRelease()
            ACTION_PTT_MODE    -> voiceManager.setPttMode(intent.getBooleanExtra(EXTRA_PTT_ENABLED, false))
            ACTION_VOLUME      -> voiceManager.setMasterVolume(intent.getFloatExtra(EXTRA_VOLUME, 0.8f))
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        voiceManager.destroy()
        super.onDestroy()
    }

    private fun createChannel() {
        val ch = NotificationChannel(CHANNEL_ID, getString(R.string.audio_voice_volume), NotificationManager.IMPORTANCE_LOW)
            .apply { setShowBadge(false) }
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    private fun buildNotif(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.audio_voice_volume))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true).setSilent(true).build()
}

@HiltViewModel
class VoiceVM @Inject constructor(
    private val voiceManager: VoiceManager
) : ViewModel() {

    val state: StateFlow<VoiceUiState> = voiceManager.state

    fun join(roomId: String, uid: Int) {
        viewModelScope.launch(Dispatchers.IO) { voiceManager.join(roomId, uid) }
    }

    fun leave() {
        viewModelScope.launch(Dispatchers.IO) { voiceManager.leave() }
    }

    fun toggleMute() {
        val muted = !state.value.isMuted
        voiceManager.mute(muted)
    }

    fun pttPress()   { voiceManager.pttPress() }
    fun pttRelease() { voiceManager.pttRelease() }

    fun togglePttMode() {
        voiceManager.setPttMode(!state.value.isPttMode)
    }

    fun toggleNoiseCancellation() {
        voiceManager.setNoiseCancellation(!state.value.noiseCancel)
    }

    fun toggleEchoCancellation() {
        voiceManager.setEchoCancellation(!state.value.echoCancellation)
    }

    fun setVolume(uid: Int, vol: Int) {
        voiceManager.setVolume(uid, vol)
    }

    fun setMasterVolume(vol: Float) {
        voiceManager.setMasterVolume(vol)
    }
}
