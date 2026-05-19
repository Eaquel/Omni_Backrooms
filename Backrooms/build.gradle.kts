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

val keystoreProps = Properties().also {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) it.load(FileInputStream(file))
}

android {
    namespace   = "com.omni.backrooms"
    compileSdk  = 36

    defaultConfig {
        applicationId = "com.omni.backrooms"
        minSdk        = 30
        targetSdk     = 36
        versionCode   = 1
        versionName   = "1.0.0"
        ndkVersion    = "29.0.14206865"

        vectorDrawables { useSupportLibrary = true }

        buildConfigField("String",  "API_BASE_URL",      "\"https://api.omnibackrooms.com/v1/\"")
        buildConfigField("boolean", "ENABLE_GUARD",      "true")
        buildConfigField("String",  "EXPECTED_SIG_HASH", "\"\"")
    }

    signingConfigs {
        create("release") {
            val storeFilePath = keystoreProps["storeFile"]?.toString()
            val storePass     = keystoreProps["storePassword"]?.toString()
            val keyAliasVal   = keystoreProps["keyAlias"]?.toString()
            val keyPass       = keystoreProps["keyPassword"]?.toString()
            if (storeFilePath != null) {
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
            isDebuggable      = false
            signingConfig     = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("boolean", "ENABLE_GUARD", "true")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }

    kotlin { jvmToolchain(25) }

    buildFeatures {
        compose     = true
        buildConfig = true
    }

    sourceSets {
        getByName("main") {
            java.setSrcDirs(listOf("Source/Main/Kotlin"))
            kotlin.setSrcDirs(listOf("Source/Main/Kotlin"))
            res.setSrcDirs(listOf("Source/Main/res"))
            assets.setSrcDirs(listOf("Source/Main/assets"))
            manifest.srcFile("Source/Main/AndroidManifest.xml")
        }
    }

    externalNativeBuild {
        cmake {
            path    = file("Source/Main/Native/CMakeLists.txt")
            version = "4.3.2"
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
        jniLibs { useLegacyPackaging = false }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a")
            isUniversalApk = false
        }
    }

    bundle {
        language { enableSplit = true }
        density  { enableSplit = true }
        abi      { enableSplit = true }
    }

    lint {
        abortOnError      = false
        checkReleaseBuilds = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.config)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.coil.compose)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.media3.exoplayer.hls)

    implementation(libs.androidx.billing)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.crashlytics.ndk)
    implementation(libs.firebase.messaging)
}
