package com.omni.backrooms

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

enum class ThreatLevel { CLEAN, SUSPICIOUS, HIGH, CRITICAL }

data class GuardReport(
    val flags           : Int     = 0,
    val isRooted        : Boolean = false,
    val isFrida         : Boolean = false,
    val isDebugged      : Boolean = false,
    val isEmulator      : Boolean = false,
    val isSignatureValid: Boolean = true,
    val isHookDetected  : Boolean = false,
    val isMemoryTampered: Boolean = false,
    val report          : String  = "CLEAN",
    val threatLevel     : ThreatLevel = ThreatLevel.CLEAN
) {
    val isThreatDetected: Boolean get() = flags != 0
}

@Singleton
class GuardManager @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val bridge: Native_Bridge
) {
    private val _report       = MutableStateFlow(GuardReport())
    val report: StateFlow<GuardReport> = _report.asStateFlow()

    private val _threatEvent  = MutableSharedFlow<ThreatLevel>(extraBufferCapacity = 4)
    val threatEvent: SharedFlow<ThreatLevel> = _threatEvent.asSharedFlow()

    private var monitorJob: kotlinx.coroutines.Job? = null

    fun initialize() {
        val sigHash = BuildConfig.EXPECTED_SIG_HASH
        bridge.initGuard(ctx, sigHash)
        runFullScan()
        startContinuousMonitor()
    }

    fun runFullScan() {
        val flags       = bridge.runGuardScan()
        val rooted      = bridge.isRooted()
        val frida       = bridge.isFridaDetected()
        val debugged    = bridge.isDebugged()
        val emulator    = bridge.isEmulator()
        val sigValid    = bridge.isSignatureValid()
        val hook        = detectHooking()
        val memTamper   = detectMemoryTampering()
        val reportStr   = bridge.getThreatReport()

        val level = when {
            frida || debugged || hook -> ThreatLevel.CRITICAL
            rooted || !sigValid       -> ThreatLevel.HIGH
            memTamper                 -> ThreatLevel.HIGH
            emulator                  -> ThreatLevel.SUSPICIOUS
            flags != 0                -> ThreatLevel.SUSPICIOUS
            else                      -> ThreatLevel.CLEAN
        }

        _report.value = GuardReport(
            flags            = flags,
            isRooted         = rooted,
            isFrida          = frida,
            isDebugged       = debugged,
            isEmulator       = emulator,
            isSignatureValid = sigValid,
            isHookDetected   = hook,
            isMemoryTampered = memTamper,
            report           = reportStr,
            threatLevel      = level
        )

        if (level != ThreatLevel.CLEAN)
            _threatEvent.tryEmit(level)
    }

    private fun startContinuousMonitor() {
        monitorJob = kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(30_000)
                runFullScan()
            }
        }
    }

    fun destroy() {
        monitorJob?.cancel()
        bridge.destroyGuard()
    }

    private fun detectHooking(): Boolean {
        return try {
            val stackTrace = Thread.currentThread().stackTrace
            stackTrace.any { el ->
                el.className.contains("xposed", true) ||
                el.className.contains("substrate", true) ||
                el.className.contains("lsposed", true) ||
                el.className.contains("frida", true)
            }
        } catch (_: Exception) { false }
    }

    private fun detectMemoryTampering(): Boolean {
        return try {
            val mapsFile = File("/proc/self/maps")
            if (!mapsFile.exists()) return false
            val content = mapsFile.readText()
            val suspects = listOf("frida", "gadget", "inject", "hook", "substrate", "xposed", "lsposed")
            suspects.any { content.contains(it, true) }
        } catch (_: Exception) { false }
    }

    fun verifyApkSignature(): Boolean {
        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ctx.packageManager.getPackageInfo(
                    ctx.packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                ctx.packageManager.getPackageInfo(ctx.packageName, PackageManager.GET_SIGNATURES)
            }
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                pInfo.signatures
            }
            if (signatures.isNullOrEmpty()) return false
            val md = MessageDigest.getInstance("SHA-256")
            val hash = md.digest(signatures[0].toByteArray())
            val hexHash = hash.joinToString("") { "%02x".format(it) }
            hexHash == BuildConfig.EXPECTED_SIG_HASH
        } catch (_: Exception) { false }
    }
}

object VideoEncryptor {
    private const val KEY_ALIAS    = "omni_video_key"
    private const val KEYSTORE     = "AndroidKeyStore"
    private const val ALGO         = "AES/GCM/NoPadding"
    private const val TAG_LEN      = 128
    private const val IV_LEN       = 12

    fun getOrCreateKey(): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        if (ks.containsAlias(KEY_ALIAS))
            return (ks.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey

        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
         .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
         .setKeySize(256)
         .setUserAuthenticationRequired(false)
         .build()

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
            .apply { init(spec) }
            .generateKey()
    }

    fun encrypt(data: ByteArray): ByteArray {
        val key    = getOrCreateKey()
        val cipher = Cipher.getInstance(ALGO)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv         = cipher.iv
        val encrypted  = cipher.doFinal(data)
        return iv + encrypted
    }

    fun decrypt(data: ByteArray): ByteArray {
        val key    = getOrCreateKey()
        val iv     = data.copyOfRange(0, IV_LEN)
        val body   = data.copyOfRange(IV_LEN, data.size)
        val cipher = Cipher.getInstance(ALGO)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LEN, iv))
        return cipher.doFinal(body)
    }

    fun encryptVideoSegment(segment: ByteArray, segmentIndex: Int): ByteArray {
        val key    = getOrCreateKey()
        val cipher = Cipher.getInstance(ALGO)
        val iv     = ByteArray(IV_LEN).also {
            it[0] = (segmentIndex shr 24).toByte()
            it[1] = (segmentIndex shr 16).toByte()
            it[2] = (segmentIndex shr  8).toByte()
            it[3] = (segmentIndex       ).toByte()
        }
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_LEN, iv))
        return cipher.doFinal(segment)
    }

    fun decryptVideoSegment(segment: ByteArray, segmentIndex: Int): ByteArray {
        val key    = getOrCreateKey()
        val cipher = Cipher.getInstance(ALGO)
        val iv     = ByteArray(IV_LEN).also {
            it[0] = (segmentIndex shr 24).toByte()
            it[1] = (segmentIndex shr 16).toByte()
            it[2] = (segmentIndex shr  8).toByte()
            it[3] = (segmentIndex       ).toByte()
        }
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LEN, iv))
        return cipher.doFinal(segment)
    }
}

object IntegrityChecker {
    fun checkDeviceIntegrity(ctx: Context): Boolean {
        if (Build.TAGS?.contains("test-keys") == true) return false
        if (Build.FINGERPRINT?.startsWith("generic") == true) return false
        if (Build.MODEL.contains("Emulator") || Build.MODEL.contains("Android SDK")) return false
        if (Build.HARDWARE == "goldfish" || Build.HARDWARE == "ranchu") return false
        if (Build.PRODUCT.contains("sdk") || Build.PRODUCT.contains("vbox")) return false
        if (checkDangerousPaths()) return false
        if (checkDangerousPackages(ctx)) return false
        return true
    }

    private fun checkDangerousPaths(): Boolean {
        val paths = listOf(
            "/sbin/su", "/system/bin/su", "/system/xbin/su",
            "/data/local/su", "/data/local/xbin/su",
            "/data/adb/magisk", "/sbin/.magisk",
            "/system/app/SuperSU.apk", "/system/app/Superuser.apk"
        )
        return paths.any { File(it).exists() }
    }

    private fun checkDangerousPackages(ctx: Context): Boolean {
        val packages = listOf(
            "com.topjohnwu.magisk", "eu.chainfire.supersu",
            "com.noshufou.android.su", "com.koushikdutta.superuser",
            "de.robv.android.xposed.installer", "org.lsposed.manager",
            "io.github.lsposed.manager"
        )
        val pm = ctx.packageManager
        return packages.any { pkg ->
            try { pm.getPackageInfo(pkg, 0); true } catch (_: Exception) { false }
        }
    }

    fun computeFileHash(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buf = ByteArray(8192)
            var read: Int
            while (stream.read(buf).also { read = it } != -1)
                md.update(buf, 0, read)
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}

@HiltViewModel
class GuardVM @Inject constructor(
    private val guardManager: GuardManager
) : ViewModel() {

    val report: StateFlow<GuardReport> = guardManager.report
    val threatEvent: SharedFlow<ThreatLevel> = guardManager.threatEvent

    init {
        viewModelScope.launch {
            guardManager.threatEvent.collect { level ->
                when (level) {
                    ThreatLevel.CRITICAL -> handleCriticalThreat()
                    ThreatLevel.HIGH     -> handleHighThreat()
                    else                 -> {}
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            guardManager.runFullScan()
        }
    }

    fun verifySignature(): Boolean = guardManager.verifyApkSignature()

    private fun handleCriticalThreat() {
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    private fun handleHighThreat() {
        android.util.Log.w("Guard", "High threat detected: ${report.value.report}")
    }
}
