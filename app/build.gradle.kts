plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10"
    kotlin("plugin.serialization") version "2.0.20"
}

android {
    namespace = "com.example.bikeplanner"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.bikeplanner"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        val mt = project.findProperty("MAPTILER_KEY") as String? ?: ""
        buildConfigField("String", "MAPTILER_KEY", "\"$mt\"")

        val tf = project.findProperty("THUNDERFOREST_KEY") as String? ?: ""
        buildConfigField("String", "THUNDERFOREST_KEY", "\"$tf\"")

        // Inject ORS key from gradle.properties
        val orsKey = project.findProperty("ORS_API_KEY") as String? ?: ""
        buildConfigField("String", "ORS_API_KEY", "\"$orsKey\"")    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isProfileable = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
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
    // MapLibre (Android SDK)
    implementation("org.maplibre.gl:android-sdk:11.13.0")

    // Location
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // HTTP + JSON
    implementation("com.squareup.okhttp3:okhttp:5.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.2")
    implementation("com.google.android.material:material:1.12.0")
}
