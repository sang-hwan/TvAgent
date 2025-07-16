plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.protobuf)
}

val enableTr069: Boolean =
    (rootProject.findProperty("enableTr069") as? String)?.toBoolean() ?: false

android {
    namespace = "kr.co.aromit.tvagent"
    compileSdk = 34

    defaultConfig {
        applicationId = "kr.co.aromit.tvagent"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("Boolean", "ENABLE_TR069", enableTr069.toString())
    }

    buildFeatures {
        buildConfig = true
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins { create("java") }
        }
    }
}

dependencies {
    // Android Core & UI
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.material)
    implementation(libs.androidx.lifecycle.ktx)
    implementation(libs.androidx.tv.foundation)

    // Network / XML / MQTT
    implementation(libs.okhttp)
    implementation(libs.xmlutil.serialization)
    implementation(libs.ksoap2.android) {
        exclude(group = "net.sourceforge.kobjects", module = "kobjects-j2me")
        exclude(group = "net.sourceforge.me4se", module = "me4se")
        exclude(group = "com.github.simpligility.ksoap2-android", module = "ksoap2-j2se")
        exclude(group = "com.github.simpligility.ksoap2-android", module = "ksoap2-okhttp")
        exclude(group = "org.xmlpull", module = "xmlpull")
        exclude(group = "xpp3", module = "xpp3")
    }
    implementation(libs.simple.xml)
    implementation(libs.paho)
    implementation(libs.paho.android)
    implementation(libs.protobuf.java)
    implementation(libs.timber)

    // Internal modules
    implementation(project(":agent-core"))
    implementation(project(":network-mqtt"))
    implementation(project(":protocol-usp"))
    if (enableTr069) {
        implementation(project(":protocol-tr069-legacy"))
    }

    // Unit tests
    testImplementation(libs.junit)
    testImplementation(libs.androidx.junit.ext)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.espresso.core)
    testImplementation(libs.androidx.test.rules)
    testImplementation(libs.robolectric)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)

    // Instrumentation tests
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.junit.ext)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.test.rules)
}
