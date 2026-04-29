package com.omni.backrooms

import android.app.*
import android.content.Intent
import android.media.*
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import io.agora.rtc2.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@AndroidEntryPoint
class Voice_Chat : Service() {

    @Inject lateinit var bridge: Native_Bridge
    @Inject lateinit var settingsRepo: Settings_Repository

    private var rtcEngine   : RtcEngine?    = null
    private var audioRecord : AudioRecord?  = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isMuted       = MutableStateFlow(false)
    private val _connectedPeers= MutableStateFlow(0)
    val isMuted       : StateFlow<Boolean> = _isMuted
    val connectedPeers: StateFlow<Int>     = _connectedPeers

    companion object {
        private const val CHANNEL_ID   = "omni_voice"
        private const val NOTIF_ID     = 2002
        const val ACTION_JOIN          = "action_join"
        const val ACTION_LEAVE         = "action_leave"
        const val ACTION_MUTE          = "action_mute"
        const val ACTION_UNMUTE        = "action_unmute"
        const val ACTION_SET_VOLUME    = "action_volume"
        const val EXTRA_ROOM           = "room_id"
        const val EXTRA_UID            = "uid"
        const val EXTRA_VOLUME         = "volume"
        private const val SAMPLE_RATE  = 16000
        private const val CHANNEL_CFG  = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private val rtcHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            android.util.Log.i("VoiceChat", "Joined channel=$channel uid=$uid")
        }
        override fun onUserJoined(uid: Int, elapsed: Int) {
            _connectedPeers.value++
        }
        override fun onUserOffline(uid: Int, reason: Int) {
            _connectedPeers.value = maxOf(0, _connectedPeers.value - 1)
        }
        override fun onRemoteAudioStats(stats: RemoteAudioStats) {
            android.util.Log.d("VoiceChat", "peer=${stats.uid} loss=${stats.audioLossRate}%")
        }
        override fun onError(err: Int) {
            android.util.Log.e("VoiceChat", "RTC error=$err")
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIF_ID, buildNotif())
        initEngine()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_JOIN -> {
                val room = intent.getStringExtra(EXTRA_ROOM) ?: return START_NOT_STICKY
                val uid  = intent.getIntExtra(EXTRA_UID, 0)
                join(room, uid)
            }
            ACTION_LEAVE       -> leave()
            ACTION_MUTE        -> setMuted(true)
            ACTION_UNMUTE      -> setMuted(false)
            ACTION_SET_VOLUME  -> {
                val vol = intent.getFloatExtra(EXTRA_VOLUME, 0.8f)
                rtcEngine?.adjustPlaybackSignalVolume((vol * 400).toInt().coerceIn(0, 400))
                bridge.setMonsterVolume(vol)
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        leave()
        scope.cancel()
        RtcEngine.destroy()
        rtcEngine = null
        super.onDestroy()
    }

    private fun initEngine() {
        runCatching {
            val cfg = RtcEngineConfig().apply {
                mContext      = applicationContext
                mAppId        = BuildConfig.AGORA_APP_ID
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
                muteLocalAudioStream(false)
                adjustRecordingSignalVolume(100)
                adjustPlaybackSignalVolume(100)
            }
        }.onFailure {
            android.util.Log.e("VoiceChat", "RTC init failed: ${it.message}")
        }
    }

    private fun join(roomId: String, uid: Int) {
        rtcEngine?.joinChannel(null, roomId, uid, null)
        startCapture()
    }

    private fun leave() {
        rtcEngine?.leaveChannel()
        stopCapture()
        _connectedPeers.value = 0
    }

    private fun setMuted(muted: Boolean) {
        _isMuted.value = muted
        rtcEngine?.muteLocalAudioStream(muted)
    }

    private fun startCapture() {
        val bufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CFG, AUDIO_FORMAT)
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            SAMPLE_RATE, CHANNEL_CFG, AUDIO_FORMAT, bufSize * 2
        )
        audioRecord?.startRecording()
        scope.launch {
            val buf = ByteArray(bufSize)
            while (isActive && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val read = audioRecord?.read(buf, 0, buf.size) ?: break
                if (read > 0 && !_isMuted.value) {
                    val pkt = bridge.buildVoicePacket(buf, read)
                    pkt?.let { /* send via UDP or Agora data stream */ }
                }
            }
        }
    }

    private fun stopCapture() {
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    private fun createChannel() {
        val ch = NotificationChannel(CHANNEL_ID, getString(R.string.audio_voice_volume),
            NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) }
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    private fun buildNotif(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.audio_voice_volume))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true).setSilent(true).build()
}
