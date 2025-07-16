plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "kr.co.aromit.tr069legacy"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        targetSdk = 35
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

    implementation(libs.okhttp)
    implementation(libs.ksoap2.android)
    implementation(libs.simple.xml)
    implementation(libs.xmlutil.serialization)
    implementation(libs.timber)
}
