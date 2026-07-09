plugins {
    id("com.android.application")
}

android {
    namespace = "net.hanenashi.tilezz"
    compileSdk = 37

    defaultConfig {
        applicationId = "net.hanenashi.tilezz"
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
    }

    lint {
        // AGP 9.2 requires Gradle 9.4.1. Newer Gradle currently exposes an
        // AGP-internal deprecation, so keep this supported pair pinned.
        disable += "AndroidGradlePluginVersion"
    }
}
