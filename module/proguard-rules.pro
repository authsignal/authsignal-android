# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep Kotlinx Serialization classes
-keep class kotlinx.serialization.** { *; }

# Keep @Serializable classes and their generated serializers
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable *;
}

# Keep your data classes and their serializers (adjust the package if needed)
-keep class com.authsignal.** { *; }
-keepclassmembers class com.authsignal.** {
    @kotlinx.serialization.Serializable *;
}