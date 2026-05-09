-optimizationpasses 7
-allowaccessmodification
-overloadaggressively
-repackageclasses "o"
-flattenpackagehierarchy "o"
-mergeinterfacesaggressively
-dontusemixedcaseclassnames
-verbose

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute S

-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# ─── Native Bridge ────────────────────────────────────────────────────────────
-keepclasseswithmembernames class com.omni.backrooms.Native_Bridge {
    native <methods>;
}
-keep class com.omni.backrooms.Native_Bridge { *; }

# ─── Application & Main Activity ─────────────────────────────────────────────
-keep class com.omni.backrooms.App             { *; }
-keep class com.omni.backrooms.Main_Activity   { *; }
-keep class com.omni.backrooms.Session_Service { *; }
-keep class com.omni.backrooms.Voice_Chat      { *; }
-keep class com.omni.backrooms.Firebase_Messaging_Service { *; }
-keep class com.omni.backrooms.NotificationActionReceiver { *; }

# ─── Hilt ─────────────────────────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }
-keepclasseswithmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}

# ─── Kotlin ───────────────────────────────────────────────────────────────────
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { <fields>; }
-keepclassmembers class kotlin.Lazy { <methods>; }
-dontwarn kotlin.**

# ─── Kotlin Serialization ─────────────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.omni.backrooms.**$$serializer { *; }
-keepclassmembers class com.omni.backrooms.** {
    *** Companion;
}
-keepclasseswithmembers class com.omni.backrooms.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ─── Retrofit & OkHttp ────────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class retrofit2.** { *; }
-keepattributes RuntimeVisibleAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

# ─── Room ─────────────────────────────────────────────────────────────────────
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class *          { *; }
-keep @androidx.room.Dao class *             { *; }
-keep @androidx.room.Database class *        { *; }
-keep @androidx.room.TypeConverter class *   { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    abstract !public *;
}

# ─── Firebase ─────────────────────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ─── Crashlytics NDK ──────────────────────────────────────────────────────────
-keepattributes *Annotation*
-keep class com.google.firebase.crashlytics.** { *; }
-dontwarn com.google.firebase.crashlytics.**

# ─── Compose ──────────────────────────────────────────────────────────────────
-keep class androidx.compose.** { *; }
-keep class androidx.lifecycle.** { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel { <init>(...); }
-keepclassmembers class * extends androidx.lifecycle.AndroidViewModel { <init>(...); }
-dontwarn androidx.compose.**

# ─── Agora RTC ────────────────────────────────────────────────────────────────
-keep class io.agora.** { *; }
-keep class io.agora.rtc.** { *; }
-dontwarn io.agora.**

# ─── Media3 / ExoPlayer ───────────────────────────────────────────────────────
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ─── Coil ─────────────────────────────────────────────────────────────────────
-keep class coil.** { *; }
-dontwarn coil.**

# ─── Lottie ───────────────────────────────────────────────────────────────────
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# ─── Billing ──────────────────────────────────────────────────────────────────
-keep class com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**

# ─── DataStore ────────────────────────────────────────────────────────────────
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# ─── Enums ────────────────────────────────────────────────────────────────────
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ─── Parcelable ───────────────────────────────────────────────────────────────
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# ─── Serializable ─────────────────────────────────────────────────────────────
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ─── R classes ────────────────────────────────────────────────────────────────
-keep class **.R { *; }
-keep class **.R$* { *; }

# ─── Remove Logging ───────────────────────────────────────────────────────────
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
