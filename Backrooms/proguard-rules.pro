# ══════════════════════════════════════════════════════════════
#  Omni Backrooms — ProGuard / R8 Rules
#  R8 full mode (build.gradle proguard-android-optimize.txt ile)
#  Kotlin + Compose + Hilt + Retrofit + Room + Firebase + Media3
# ══════════════════════════════════════════════════════════════

# ── Optimisation ──────────────────────────────────────────────
-optimizationpasses 7
-allowaccessmodification
-overloadaggressively
-repackageclasses "o"
-flattenpackagehierarchy "o"
-mergeinterfacesaggressively
-dontusemixedcaseclassnames

# Stack trace için kaynak bilgisi koru
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute S

# Reflection / serialization için zorunlu attribute'lar
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes RuntimeVisibleAnnotations

# ── Native Bridge ─────────────────────────────────────────────
# NativeBridge: JNI metodları obfuscate edilirse System.loadLibrary crash verir
-keepclasseswithmembernames class com.omni.backrooms.NativeBridge {
    native <methods>;
}
-keep class com.omni.backrooms.NativeBridge { *; }

# ── Android entrypoints ───────────────────────────────────────
-keep class com.omni.backrooms.App              { *; }
-keep class com.omni.backrooms.MainActivity     { *; }
-keep class com.omni.backrooms.SessionService   { *; }
-keep class com.omni.backrooms.OmniMessagingService { *; }

# ── Hilt ──────────────────────────────────────────────────────
# Hilt kendi consumer rules'larını sağlar; bu kurallar ek güvence içindir
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }
-keepclasseswithmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}
# Hilt generated component sınıfları (Dagger_ prefix)
-keep class **_HiltComponents$* { *; }
-keep class **_HiltModules$* { *; }

# ── Kotlin ────────────────────────────────────────────────────
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { <fields>; }
-keepclassmembers class kotlin.Lazy { <methods>; }
-dontwarn kotlin.**
# Kotlin coroutines internal
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ── Kotlin Serialization ──────────────────────────────────────
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.omni.backrooms.**$$serializer { *; }
-keepclassmembers class com.omni.backrooms.** {
    *** Companion;
}
-keepclasseswithmembers class com.omni.backrooms.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# Sealed class serializer lookup
-keepclassmembers class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**

# ── Retrofit & OkHttp ────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>
# Retrofit response/request model sınıfları
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ── Room ──────────────────────────────────────────────────────
# Room 2.6+ kendi consumer-rules.pro dosyasını içerir;
# bu kurallar sadece ek güvencedir
-keep @androidx.room.Entity class *          { *; }
-keep @androidx.room.Dao class *             { *; }
-keep @androidx.room.Database class *        { *; }
-keep @androidx.room.TypeConverter class *   { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    abstract !public *;
}
-dontwarn androidx.room.**

# ── Firebase ──────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ── Crashlytics NDK ───────────────────────────────────────────
# NDK sembol upload için fully qualified sınıf adı korunmalı
-keep class com.google.firebase.crashlytics.** { *; }
-dontwarn com.google.firebase.crashlytics.**

# ── Compose ───────────────────────────────────────────────────
# Compose kendi consumer-rules içerir; lambda & remember optimizasyonu için:
-keepclassmembers class * extends androidx.lifecycle.ViewModel { <init>(...); }
-keepclassmembers class * extends androidx.lifecycle.AndroidViewModel { <init>(...); }
-dontwarn androidx.compose.**

# ── Media3 / ExoPlayer ────────────────────────────────────────
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ── Coil 3 ────────────────────────────────────────────────────
-dontwarn coil.**

# ── Lottie ────────────────────────────────────────────────────
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# ── Billing ───────────────────────────────────────────────────
-keep class com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**

# ── DataStore ─────────────────────────────────────────────────
-dontwarn androidx.datastore.**

# ── Java standard ─────────────────────────────────────────────
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# R sınıfları — shrinkResources ile birlikte çalışır
-keep class **.R        { *; }
-keep class **.R$*      { *; }

# ── Production log silme ──────────────────────────────────────
# Release build'de Log.v / Log.d / Log.i tamamen kaldırılır
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# ── Agora KALDIRILDI ──────────────────────────────────────────
# Önceki io.agora.** keep kuralları silindi.
# Voice.kt ve Agora SDK projeden çıkarıldı.
