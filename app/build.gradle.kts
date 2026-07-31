plugins {
    id("com.android.application")
}

val ciVersionCode = providers.environmentVariable("GLOWWORDS_VERSION_CODE").orNull?.toIntOrNull()
val ciVersionName = providers.environmentVariable("GLOWWORDS_VERSION_NAME").orNull
val signingStorePath = providers.environmentVariable("GLOWWORDS_KEYSTORE_PATH").orNull
val signingStorePassword = providers.environmentVariable("GLOWWORDS_KEYSTORE_PASSWORD").orNull
val signingKeyAlias = providers.environmentVariable("GLOWWORDS_KEY_ALIAS").orNull
val signingKeyPassword = providers.environmentVariable("GLOWWORDS_KEY_PASSWORD").orNull
val signingValues = listOf(
    signingStorePath,
    signingStorePassword,
    signingKeyAlias,
    signingKeyPassword,
)

if (signingValues.any { it != null } && signingValues.any { it == null }) {
    throw GradleException("Stable signing requires all GLOWWORDS_KEY* environment variables")
}

android {
    namespace = "com.chrisawad.glowwords"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.chrisawad.glowwords"
        minSdk = 23
        targetSdk = 36
        versionCode = ciVersionCode ?: 1
        versionName = ciVersionName ?: "1.0"
    }

    val ciReleaseSigning = if (signingValues.all { it != null }) {
        signingConfigs.create("ciRelease") {
            storeFile = file(signingStorePath!!)
            storePassword = signingStorePassword
            keyAlias = signingKeyAlias
            keyPassword = signingKeyPassword
            storeType = "PKCS12"
        }
    } else {
        null
    }

    buildTypes {
        getByName("release") {
            signingConfig = ciReleaseSigning
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.1")
}
