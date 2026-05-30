plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.beacon.glasses"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")

        // Default to the real HeyCyan/X01 SDK. Set to "true" (e.g. on the
        // emulator) to swap in FakeGlassesManager via Hilt.
        buildConfigField("boolean", "USE_FAKE_GLASSES", "false")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))

    // HeyCyan/X01 vendor SDK (flat AAR with no POM) + its required runtime deps.
    implementation(files("libs/LIB_GLASSES_SDK-release_3.aar"))
    implementation(libs.gson)
    implementation(libs.okhttp)
    implementation(libs.androidx.localbroadcastmanager)

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
