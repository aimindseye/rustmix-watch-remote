plugins {
    id("com.android.application")
}

android {
    namespace = "io.github.aimindseye.rustmixremote"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.aimindseye.rustmixremote"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
}
