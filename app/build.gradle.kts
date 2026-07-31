plugins {
    id("com.android.application")
}

android {
    namespace = "com.chrisawad.glowwords"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.chrisawad.glowwords"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.1")
}
