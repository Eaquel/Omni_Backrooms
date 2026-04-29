plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace   = "com.omni.backrooms"
    compileSdk  = 36

    defaultConfig {
        applicationId   = "com.omni.backrooms"
        minSdk          = 30
        targetSdk       = 36
        versionCode     = 1
        versionName     = "1.0.0-beta"
        ndkVersion      = "29.0.14206865"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"



        vectorDrawables {
            useSupportLibrary = true
        }

        externalNativeBuild {
            cmake {
                cppFlags    += "-std=c++23"
                arguments   += listOf(
                    "-DANDROID_STL=c++_shared",
                    "-DANDROID_PLATFORM=android-30"
                )
            }
        }
        buildConfigField("String", "API_BASE_URL",      ""https://api.omnibackrooms.com/v1/"")
        buildConfigField("String", "AGORA_APP_ID",      ""YOUR_AGORA_APP_ID_HERE"")
        buildConfigField("String", "EXPECTED_SIG_HASH", ""0000000000000000"")
    }

    buildTypes {
        release {
            isMinifyEnabled     = true
            isShrinkResources   = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled     = false
            applicationIdSuffix = ".debug"
            versionNameSuffix   = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
    }

    buildFeatures {
        compose     = true
        buildConfig = true
    }

    sourceSets {
        getByName("main") {
            java.srcDirs += file("Source/Main/Kotlin")
            res.srcDirs += file("Source/Main/res")
            manifest.srcFile("Source/Main/AndroidManifest.xml")
        }
    }

    externalNativeBuild {
        cmake {
            path    = file("Source/Main/Native/CMakeLists.txt")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    splits {
        abi {
            isEnable        = true
            reset()
            include("armeabi-v7a", "arm64-v8a")
            isUniversalApk  = false
        }
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
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.gson)
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
    implementation(libs.agora.rtc.full)
    debugImplementation(libs.androidx.ui.tooling)
}

android.defaultConfig.apply {
    buildConfigField("String", "API_BASE_URL",       "\"https://api.omnibackrooms.com/v1/\"")
    buildConfigField("String", "AGORA_APP_ID",       "\"YOUR_AGORA_APP_ID_HERE\"")
    buildConfigField("String", "EXPECTED_SIG_HASH",  "\"0000000000000000\"")
}
