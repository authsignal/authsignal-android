# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

-keep class kotlinx.serialization.** { *; }

-keepclassmembers class ** {
    @kotlinx.serialization.Serializable *;
}

-keep class com.authsignal.** { *; }
-keepclassmembers class com.authsignal.** {
    @kotlinx.serialization.Serializable *;
}