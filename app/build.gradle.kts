plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "uk.mcaughtry.chargeguard"
    compileSdk = 34

    defaultConfig {
        applicationId = "uk.mcaughtry.chargeguard"
        minSdk = 30
        targetSdk = 34
        versionCode = 10
        versionName = "1.9"
    }

    // A fixed debug key, so every build is signed the same and installs over the last
    // one. It is a throwaway key for personal sideloading, not a secret. Google Play
    // uses its own separate signing key.
    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
}
