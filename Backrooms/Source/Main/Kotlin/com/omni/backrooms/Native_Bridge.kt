package com.omni.backrooms

import android.graphics.Bitmap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Native_Bridge @Inject constructor() {

    external fun initCore(seed: Long)
    external fun getFlicker(phase: Float, t: Float, broken: Boolean): Float
    external fun generateLevel(count: Int, depth: Int): FloatArray?
    external fun getMoistureAt(x: Float, y: Float): Float
    external fun applyVhs(bitmap: Bitmap, t: Float, intensity: Float): Boolean
    external fun applyFlicker(bitmap: Bitmap, val_: Float)
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
    external fun spawnEntity(
        x: Float, y: Float, z: Float,
        speed: Float, hear: Float, sight: Float, aggro: Float,
        typeId: Int
    ): Int
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
