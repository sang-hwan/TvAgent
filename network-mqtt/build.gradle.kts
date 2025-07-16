plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "kr.co.aromit.network.mqtt"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        targetSdk = 35
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(project(":agent-core"))

    implementation(libs.paho)
    implementation(libs.paho.android)
    implementation(libs.timber)
}
