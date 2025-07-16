plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "kr.co.aromit.tr069legacy"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
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
    implementation(project(":agent-core"))
    implementation(libs.okhttp)
    implementation(libs.ksoap2.android)
    implementation(libs.simple.xml)
    implementation(libs.xmlutil.serialization)
    implementation(libs.timber)
}
