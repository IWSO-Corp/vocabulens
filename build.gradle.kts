// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    id("com.google.gms.google-services") version "4.4.3" apply false
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") version "2.0.1" apply false
    id("com.google.firebase.crashlytics") version "3.0.6" apply false
}