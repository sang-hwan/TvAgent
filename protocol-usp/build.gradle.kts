plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.protobuf)
}

android {
    namespace = "kr.co.aromit.protocol.usp"
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
    implementation(project(":agent-core"))
    implementation(libs.protobuf.java)
    implementation(libs.timber)
}
