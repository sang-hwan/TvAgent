pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

// Incubating API 사용 경고 억제
@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://oss.sonatype.org/content/repositories/ksoap2-android-releases/") }
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
        mavenCentral()
    }
}

val enableTr069: Boolean = providers
    .gradleProperty("enableTr069")
    .map(String::toBooleanStrict)
    .getOrElse(false)

rootProject.name = "TvAgent"

if (enableTr069) {
    include(":protocol-tr069-legacy")
} else {
    logger.lifecycle("🚫 TR‑069 legacy 모듈 비활성화 (enableTr069=false)")
}

include(":app", ":agent-core", ":network-mqtt", ":protocol-usp")
