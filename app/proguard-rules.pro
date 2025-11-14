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

# ================================
#           ROOM
# ================================
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep @androidx.room.* class * { *; }

-keep class com.iwsocorp.vobynotes.core.database.** { *; }

# ================================
#           VIEWMODEL
# ================================
-keep class androidx.lifecycle.** { *; }
-dontwarn androidx.lifecycle.**

# Jika pakai Hilt / dagger
-keep class dagger.hilt.** { *; }
-dontwarn dagger.hilt.**
-keep class javax.inject.** { *; }
-dontwarn javax.inject.**

# ================================
#           GSON
# ================================
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

-keep class com.iwsocorp.vobynotes.**.model.** { *; }
-keepclassmembers class com.iwsocorp.vobynotes.**.model.** { *; }

# ================================
#           RETROFIT & OKHTTP  (jika dipakai)
# ================================
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-dontwarn okio.**

# ================================
#           KOTLIN & COROUTINES
# ================================
-keep class kotlin.** { *; }
-dontwarn kotlin.**

-dontwarn kotlinx.coroutines.**

# ================================
#           ML KIT TRANSLATION
# ================================
-keep class com.google.mlkit.nl.translate.** { *; }
-dontwarn com.google.mlkit.nl.translate.**

# ================================
#           FIREBASE
# ================================
-dontwarn com.google.firebase.**

# ================================
#           WIDGET
# ================================
-keep class com.iwsocorp.vobynotes.ui.widget.** { *; }

# ================================
#           LANGUAGE JSON MAPPING
# ================================
-keep class com.iwsocorp.vobynotes.core.language.** { *; }

# ================================
#           TIMBER (opsional)
# ================================
-dontwarn timber.log.Timber