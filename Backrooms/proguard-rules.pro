-keep class com.omni.backrooms.Models { *; }
-keep class com.omni.backrooms.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

-keep class io.agora.** { *; }
-dontwarn io.agora.**

-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory { *; }
-keep class * implements com.google.gson.JsonSerializer { *; }
-keep class * implements com.google.gson.JsonDeserializer { *; }

-keep class retrofit2.** { *; }
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations
-keepattributes EnclosingMethod
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

-keepclassmembers class com.omni.backrooms.Native_Bridge {
    native <methods>;
}

-dontwarn okhttp3.**
-dontwarn okio.**
