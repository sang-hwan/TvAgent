plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

val enableTr069: Boolean =
    (rootProject.findProperty("enableTr069") as? String)?.toBoolean() ?: false

android {
    namespace = "kr.co.aromit.core"
    compileSdk = 34

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        minSdk = 26
        buildConfigField("Boolean", "ENABLE_TR069", enableTr069.toString())
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.paho)
    implementation(libs.paho.android)
    implementation(libs.timber)
}
