plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "io.github.alxiw.wearpagingmediator.domain"
    compileSdk {
        version = release(37) {
            minorApiLevel = 2
        }
    }

    defaultConfig {
        minSdk = 30

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
}

dependencies {
    // di
    implementation(libs.javax.inject)

    // async
    api(libs.coroutines.core)

    // paging
    api(libs.androidx.paging.common)

    // unit tests
    testImplementation(libs.junit)
}
