plugins {
    id("com.android.application")
}

// The run number makes every CI release newer, which also makes Shizuku restart the helper on update.
val runNumber = System.getenv("GITHUB_RUN_NUMBER")?.toInt()
val keystorePath = System.getenv("HOMESNAP_KEYSTORE_PATH")

android {
    namespace = "moe.matthew.homesnap"
    compileSdk = 36

    defaultConfig {
        applicationId = "moe.matthew.homesnap"
        minSdk = 35
        targetSdk = 36
        versionCode = runNumber ?: 1
        versionName = runNumber?.let { "0.$it" } ?: "dev"
    }

    signingConfigs {
        // Only CI has the key, so local release builds come out unsigned.
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("HOMESNAP_KEYSTORE_PASSWORD")
                keyAlias = "homesnap"
                keyPassword = System.getenv("HOMESNAP_KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
    testImplementation("junit:junit:4.13.2")
}
