plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.seepd.tiktokpp"
    compileSdk = libs.versions.compileSdk.get().toInt()
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "com.seepd.tiktokpp"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 426
        versionName = "1.2.2"
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("TOKI_KEYSTORE_FILE") ?: "")
            storePassword = System.getenv("TOKI_STORE_PASSWORD") ?: ""
            keyAlias = System.getenv("TOKI_KEY_ALIAS") ?: ""
            keyPassword = System.getenv("TOKI_KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    lint {
        disable += setOf(
            "AndroidGradlePluginVersion",
            "BlockedPrivateApi",
            "DataExtractionRules",
            "DiscouragedApi",
            "DiscouragedPrivateApi",
            "GradleDependency",
            "MonochromeLauncherIcon",
            "OldTargetApi",
            "PrivateApi",
            "SdCardPath",
            "SoonBlockedPrivateApi",
            "UseKtx",
            "ObsoleteSdkInt",
        )
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
    implementation(libs.compose.ui)
    implementation(libs.material3)
    implementation(libs.material.icons.core)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    compileOnly(libs.libxposed.api)
    implementation(libs.libxposed.service)

    testImplementation(libs.junit)
}
