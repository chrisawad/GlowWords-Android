plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.webviewtest"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.webviewtest"
        minSdk = 23
        targetSdk = 37
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
