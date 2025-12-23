import com.github.triplet.gradle.androidpublisher.ReleaseStatus

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-parcelize")
    id("com.google.gms.google-services")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
    id("com.google.firebase.crashlytics")
    id("com.github.triplet.play") version "3.13.0"
}

android {
    namespace = "com.iwsocorp.vobynotes"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.iwsocorp.vobynotes"
        minSdk = 26
        targetSdk = 35
        versionCode = getVersionCode()
        versionName = System.getenv("VERSION_NAME") ?: "0.0.0-dev"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "CLIENT_ID",
            "\"${project.findProperty("CLIENT_ID") ?: ""}\""
        )
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_PATH")
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("../keystore.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt"
            )
        }
    }
    lint {
        lintConfig = file("${rootProject.projectDir}/lint.xml")
    }
}

play {
    val credentialsPath = System.getenv("PLAY_SERVICE_ACCOUNT_JSON")

    if (!credentialsPath.isNullOrBlank()) {
        serviceAccountCredentials.set(file(credentialsPath))
    }

    track.set("internal")

    releaseStatus.set(ReleaseStatus.DRAFT)
}

dependencies {

    implementation(libs.androidx.activity)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.paging.runtime.ktx)

    implementation(libs.app.update.ktx)
    implementation(libs.glide)
    implementation(libs.google.gson)
    implementation(libs.library)
    implementation(libs.material)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp.logging)
    implementation(libs.poi.ooxml)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlin.serialization)
    implementation(libs.timber)
    implementation(libs.vbpd)
    implementation(libs.github.anki.android)
    implementation(libs.androidx.webkit)

    implementation(libs.language.id)
    implementation(libs.text.recognition)
    implementation(libs.translate)
    implementation(libs.text.recognition.chinese)
    implementation(libs.text.recognition.japanese)
    implementation(libs.text.recognition.korean)
    implementation(libs.text.recognition.devanagari)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics.ndk)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.googleid)
    implementation(libs.play.publisher)

    implementation(libs.room.ktx)
    implementation(libs.room.paging)
    implementation(libs.androidx.work.runtime.ktx)
    ksp(libs.room.compiler)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    debugImplementation(libs.leakcanary.android)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

fun getVersionCode(): Int {
    return System.getenv("GITHUB_RUN_NUMBER")?.toInt() ?: 1
}