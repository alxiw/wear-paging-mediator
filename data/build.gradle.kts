plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "io.github.alxiw.wearpagingmediator.data"
    compileSdk = 37

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
    sourceSets {
        // schemas are assets of the instrumented tests: MigrationTestHelper reads them from there
        getByName("androidTest").assets.directories.add("$projectDir/schemas")
    }
}

// Room exports the schema of every version to `schemas`, so a migration can be validated against
// the real schema of the version it upgrades from.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-XXLanguage:+PropertyParamAnnotationDefaultTargetMode")
    }
}


dependencies {
    implementation(project(":domain"))

    // di
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // async
    implementation(libs.coroutines.core)

    // paging
    implementation(libs.androidx.paging.common)

    // network
    implementation(libs.retrofit)
    implementation(libs.converter.moshi)
    implementation(libs.logging.interceptor)

    // moshi
    implementation(libs.moshi)
    ksp(libs.moshi.codegen)

    // db
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.paging)

    // unit tests
    testImplementation(libs.junit)

    // ui tests
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.room.testing)
}
