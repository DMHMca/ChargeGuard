plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "uk.mcaughtry.chargeguard"
    compileSdk = 35

    defaultConfig {
        applicationId = "uk.mcaughtry.chargeguard"
        minSdk = 30
        targetSdk = 35
        versionCode = 21
        versionName = "1.20"
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
        // The Google Play UPLOAD key. The file and password come from the build
        // environment (GitHub secrets), never from the repository. When they are
        // absent (an ordinary debug build), the release signing is simply skipped.
        val uploadStore = System.getenv("UPLOAD_KEYSTORE_FILE")
        val uploadPass = System.getenv("UPLOAD_KEYSTORE_PASSWORD")
        if (uploadStore != null && uploadPass != null) {
            create("upload") {
                storeFile = file(uploadStore)
                storePassword = uploadPass
                keyAlias = "upload"
                keyPassword = uploadPass
            }
        }
    }

    buildTypes {
        release {
            signingConfigs.findByName("upload")?.let { signingConfig = it }
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
