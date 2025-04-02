plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.crexative.exoplayercompose"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.crexative.exoplayercompose"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    val media3_version = "1.6.0"

    implementation("androidx.media3:media3-exoplayer:$media3_version") // [Required] androidx.media3 ExoPlayer dependency
    implementation("androidx.media3:media3-session:$media3_version") // [Required] MediaSession Extension dependency
    implementation("androidx.media3:media3-ui:$media3_version") // [Required] Base Player UI
    implementation("androidx.media3:media3-exoplayer-dash:$media3_version") // [Optional] If your media item is DASH
    implementation("androidx.media3:media3-exoplayer-hls:$media3_version") // [Optional] If your media item is HLS (m3u8..)
    implementation("androidx.media3:media3-exoplayer-smoothstreaming:$media3_version") // [Optional] If your media item is smoothStreaming

    implementation("androidx.navigation:navigation-compose:2.8.9")
}