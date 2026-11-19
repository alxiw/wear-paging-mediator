import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.hilt)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

android {
    namespace = "io.github.alxiw.wearpagingmediator"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "io.github.alxiw.wearpagingmediator"
        minSdk = 30
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

    }

    signingConfigs {
        create("release") {
            storeFile = localProperties.getProperty("keystore_path")?.let { file(it) }
            storePassword = localProperties.getProperty("keystore_password")
            keyAlias = localProperties.getProperty("key_alias")
            keyPassword = localProperties.getProperty("key_password")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            optimization {
                enable = true
            }
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    useLibrary("wear-sdk")
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":presentation"))
    implementation(project(":data"))

    // ui
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.wear.compose.material3)
    implementation(libs.androidx.core.splashscreen)

    // di
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // unit tests
    testImplementation(libs.junit)

    // instrumented tests
    androidTestImplementation(libs.androidx.junit)
}
