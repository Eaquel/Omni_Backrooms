import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

val keystoreProps = Properties().also { props ->
    val f = rootProject.file("keystore.properties")
    if (f.exists()) props.load(FileInputStream(f))
}

android {
    namespace  = "com.omni.backrooms"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.omni.backrooms"
        minSdk        = 30
        targetSdk     = 36
        versionCode   = 1
        versionName   = "1.0.0"

        // NDK 29 LTS — en son stable release (Mayıs 2026)
        ndkVersion    = "29.0.14206865"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        externalNativeBuild {
            cmake {
                cppFlags  += "-std=c++26"
                arguments += listOf(
                    "-DANDROID_STL=c++_shared",
                    "-DANDROID_PLATFORM=android-30",
                    "-DANDROID_ARM_NEON=TRUE",
                    "-DCMAKE_BUILD_TYPE=Release"
                )
            }
        }

        buildConfigField("String",  "API_BASE_URL",      "\"https://api.omnibackrooms.com/v1/\"")
        buildConfigField("String",  "EXPECTED_SIG_HASH", "\"\"")   // Prod imzadan doldurul
        buildConfigField("boolean", "ENABLE_GUARD",      "true")
    }

    signingConfigs {
        create("release") {
            val storeFilePath = keystoreProps["storeFile"]?.toString()
                ?: (findProperty("android.injected.signing.store.file") as? String)
            val storePass     = keystoreProps["storePassword"]?.toString()
                ?: (findProperty("android.injected.signing.store.password") as? String)
            val keyAliasVal   = keystoreProps["keyAlias"]?.toString()
                ?: (findProperty("android.injected.signing.key.alias") as? String)
            val keyPass       = keystoreProps["keyPassword"]?.toString()
                ?: (findProperty("android.injected.signing.key.password") as? String)

            if (storeFilePath != null && storePass != null && keyAliasVal != null && keyPass != null) {
                storeFile     = rootProject.file(storeFilePath)
                storePassword = storePass
                keyAlias      = keyAliasVal
                keyPassword   = keyPass
            }
            // V3 + V4: Play Store zorunlu kılar, APK Signature Scheme V4 = incremental install
            enableV3Signing = true
            enableV4Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled   = true
            isShrinkResources = true
            signingConfig     = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // NDK crash stack trace için full sembol — Firebase Crashlytics NDK'ya gider
            nativeDebugSymbolLevel = "FULL"
            buildConfigField("boolean", "ENABLE_GUARD", "true")
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix   = "-debug"
            buildConfigField("boolean", "ENABLE_GUARD", "false")
        }
    }

    // JVM 21: LTS, Kotlin 2.x için önerilen minimum.
    // Java 25 EA — Gradle toolchain ile sorunlu, henüz production'da kullanılmamalı.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }
    kotlin {
        jvmToolchain(25)
    }

    buildFeatures {
        compose     = true
        buildConfig = true
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("Source/Main/Kotlin")
            kotlin.srcDirs("Source/Main/Kotlin")
            res.srcDirs("Source/Main/res")
            assets.srcDirs("Source/Main/assets")
            manifest.srcFile("Source/Main/AndroidManifest.xml")
        }
    }

    externalNativeBuild {
        cmake {
            path    = file("Source/Main/Native/CMakeLists.txt")
            version = "4.3.2+"
        }
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/DEPENDENCIES",
                "META-INF/INDEX.LIST"
            )
        }
        // useLegacyPackaging=false: .so'lar APK içinde sıkıştırılmaz →
        // kurulum hızlı, cihaz direkt mmap eder (API 23+)
        jniLibs {
            useLegacyPackaging = false
        }
    }

    // AAB (Play Store) için ABI split; splits.abi yerine bundle tercih edilmeli
    // APK dağıtımı gerekiyorsa splits.abi bloğunu geri aç
    splits {
        abi {
            isEnable       = true
            reset()
            include("armeabi-v7a", "arm64-v8a")
            isUniversalApk = false
        }
    }

    bundle {
        language { enableSplit = true }
        density  { enableSplit = true }
        abi      { enableSplit = true }
    }

    lint {
        abortOnError       = false
        checkReleaseBuilds = true
        warningsAsErrors   = false
        // Kullanılmayan kaynakları lint'e raporlat (shrinkResources zaten kaldırır)
        checkGeneratedSources = false
    }
}

dependencies {
    // ── AndroidX core ────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    // ── Compose BOM ───────────────────────────────────────────
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling)

    // ── Hilt DI ───────────────────────────────────────────────
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    // ── Kotlin ────────────────────────────────────────────────
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    // ── Network ───────────────────────────────────────────────
    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    // logging interceptor sadece debug build'de — release APK'ya sızmaz
    debugImplementation(libs.okhttp.logging.interceptor)

    // ── Storage ───────────────────────────────────────────────
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // ── Media ─────────────────────────────────────────────────
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    // HLS: online oda stream'i için tutuldu; kullanılmıyorsa kaldır → APK ~1.2 MB küçülür
    implementation(libs.media3.exoplayer.hls)

    // ── Image ─────────────────────────────────────────────────
    implementation(libs.coil.compose)
    // coil-video: lobby video thumbnail preview için; ExoPlayer direkt kullanılıyorsa kaldırılabilir
    implementation(libs.coil.video)

    // ── UI extras ─────────────────────────────────────────────
    implementation(libs.lottie.compose)
    // accompanist-permissions: bildirim izni (POST_NOTIFICATIONS) için
    implementation(libs.accompanist.permissions)

    // ── Billing ───────────────────────────────────────────────
    implementation(libs.androidx.billing)

    // ── Firebase ──────────────────────────────────────────────
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.crashlytics.ndk)   // Native crash stack trace
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.remote.config)

    // ── Agora KALDIRILDI ──────────────────────────────────────
    // Voice.kt ve Agora SDK tamamen çıkarıldı.
    // Ses işleme native Sound.cpp (OpenSLES) üzerinden yapılıyor.
    // Bağımlılık: libs.agora.rtc.voice → KULLANILMIYOR
}
