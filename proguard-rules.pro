# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

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