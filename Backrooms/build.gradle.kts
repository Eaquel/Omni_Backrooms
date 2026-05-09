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
        ndkVersion    = "29.0.14206865"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables { useSupportLibrary = true }

        externalNativeBuild {
            cmake {
                cppFlags    += "-std=c++26"
                arguments   += listOf(
                    "-DANDROID_STL=c++_shared",
                    "-DANDROID_PLATFORM=android-30",
                    "-DANDROID_ARM_NEON=TRUE",
                    "-DCMAKE_BUILD_TYPE=Release"
                )
            }
        }

        buildConfigField("String",  "API_BASE_URL",     "\"https://api.omnibackrooms.com/v1/\"")
        buildConfigField("String",  "AGORA_TOKEN_URL",  "\"https://agora-token.shakeofangel.workers.dev\"")
        buildConfigField("String",  "EXPECTED_SIG_HASH","\"\"")
        buildConfigField("boolean", "ENABLE_GUARD",     "true")
    }

    signingConfigs {
        create("release") {
            val storeFilePath = keystoreProps["storeFile"]?.toString()
                ?: (findProperty("android.injected.signing.store.file") as? String)
            val storePass = keystoreProps["storePassword"]?.toString()
                ?: (findProperty("android.injected.signing.store.password") as? String)
            val keyAliasVal = keystoreProps["keyAlias"]?.toString()
                ?: (findProperty("android.injected.signing.key.alias") as? String)
            val keyPass = keystoreProps["keyPassword"]?.toString()
                ?: (findProperty("android.injected.signing.key.password") as? String)

            if (storeFilePath != null && storePass != null && keyAliasVal != null && keyPass != null) {
                storeFile     = rootProject.file(storeFilePath)
                storePassword = storePass
                keyAlias      = keyAliasVal
                keyPassword   = keyPass
            }
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
            buildConfigField("boolean", "ENABLE_GUARD", "true")
            // NDK sembol yükleme CI'da uploadCrashlyticsSymbolFileRelease task'i ile yapılıyor
        }
        debug {
            isMinifyEnabled     = false
            buildConfigField("boolean", "ENABLE_GUARD", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(25)
        targetCompatibility = JavaVersion.toVersion(25)
    }

    kotlin {
        jvmToolchain(25)
    }

    buildFeatures {
        compose     = true
        buildConfig = true
    }

    @Suppress("DEPRECATION")
    sourceSets {
        getByName("main") {
            java.srcDirs("Source/Main/Kotlin")
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
        jniLibs {
            useLegacyPackaging = false
        }
    }

    splits {
        abi {
            isEnable       = true
            reset()
            include("armeabi-v7a", "arm64-v8a")
            isUniversalApk = false
        }
    }

    lint {
        abortOnError       = false
        checkReleaseBuilds = true
        warningsAsErrors   = false
    }

    bundle {
        language { enableSplit = true }
        density  { enableSplit = true }
        abi      { enableSplit = true }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    debugImplementation(libs.okhttp.logging.interceptor)

    implementation(libs.coil.compose)
    implementation(libs.coil.video)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.media3.exoplayer.hls)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.core.splashscreen)
    implementation(libs.lottie.compose)
    implementation(libs.accompanist.permissions)

    implementation(libs.agora.rtc.voice)
    implementation(libs.androidx.billing)

    debugImplementation(libs.androidx.ui.tooling)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.crashlytics.ndk)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.remote.config)
}
