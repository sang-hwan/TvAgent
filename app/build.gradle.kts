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
    compileSdk = 35

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "kr.co.aromit.tvagent"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("Boolean", "ENABLE_TR069", enableTr069.toString())
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
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java")
            }
        }
    }
}

dependencies {
    // Android 기본 라이브러리
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Android TV 관련 추가
    implementation(libs.androidx.tv.foundation)

    // HTTP 요청용 OkHttp
    implementation(libs.okhttp)

    // XML 파싱용 XMLUtil
    implementation(libs.xmlutil.serialization)

    // SOAP 통신용 ksoap2 및 추가 XML 파싱(Simple XML)
    implementation(libs.ksoap2.android) {
        exclude(group = "net.sourceforge.kobjects", module = "kobjects-j2me")
        exclude(group = "net.sourceforge.me4se", module = "me4se")
        exclude(group = "com.github.simpligility.ksoap2-android", module = "ksoap2-j2se")
        exclude(group = "com.github.simpligility.ksoap2-android", module = "ksoap2-okhttp")
        exclude(group = "org.xmlpull", module = "xmlpull")
        exclude(group = "xpp3", module = "xpp3")
    }
    implementation(libs.simple.xml)

    // Paho MQTT 클라이언트
    implementation(libs.paho)
    implementation(libs.paho.android)

    // 프로토콜 버퍼 라이브러리
    implementation(libs.protobuf.java)

    // 로깅용 (디버깅/로그 출력 필수)
    implementation(libs.timber)

    // 테스트 라이브러리
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // 내부 모듈 의존성
    implementation(project(":agent-core"))
    implementation(project(":network-mqtt"))
    implementation(project(":protocol-usp"))

    // enableTr069 플래그에 따라 legacy 모듈 의존성 추가
    if (enableTr069) {
        implementation(project(":protocol-tr069-legacy"))
    }
}
