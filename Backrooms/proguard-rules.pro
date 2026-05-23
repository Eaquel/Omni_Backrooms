-keepattributes *Annotation*,Signature,Exceptions,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations

-keep class com.omni.backrooms.NativeBridge { native <methods>; }

-keep class com.omni.backrooms.App { *; }
-keep class com.omni.backrooms.MainActivity { *; }
-keep class com.omni.backrooms.SessionService { *; }
-keep class com.omni.backrooms.OmniMessagingService { *; }

-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Lazy { *; }

-keep,includedescriptorclasses class com.omni.backrooms.**$$serializer { *; }
-keepclassmembers class com.omni.backrooms.** { *** Companion; }
-keepclasseswithmembers class com.omni.backrooms.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-dontwarn kotlinx.serialization.**

-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.TypeConverter class * { *; }

-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
}

-keep class **.R { *; }
-keep class **.R$* { *; }

-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn androidx.credentials.**
-dontwarn com.google.android.libraries.identity.googleid.**

-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}
