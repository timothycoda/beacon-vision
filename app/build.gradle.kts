plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.beacon.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.beacon.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    flavorDimensions += "brand"
    productFlavors {
        create("beacon") {
            dimension = "brand"
            applicationId = "com.beacon.app"
            buildConfigField("String", "BRAND", "\"beacon\"")
            resValue("string", "app_name", "Beacon")
        }
        create("elenii") {
            dimension = "brand"
            applicationId = "com.elenii.app"
            buildConfigField("String", "BRAND", "\"elenii\"")
            resValue("string", "app_name", "Elenii")
        }
    }

    buildTypes {
        val liveHelpProductionHost = "elenii.zeustek.com.ng"
        debug {
            isMinifyEnabled = false
            val useProduction =
                (project.findProperty("liveHelp.useProduction") as String?) == "true"
            if (useProduction) {
                buildConfigField("String", "LIVE_HELP_HTTP", "\"https://$liveHelpProductionHost\"")
                buildConfigField("String", "LIVE_HELP_WS", "\"wss://$liveHelpProductionHost/ws\"")
                buildConfigField("String", "LIVE_HELP_WEB", "\"https://$liveHelpProductionHost\"")
            } else {
                val liveHelpHost = (project.findProperty("liveHelp.host") as String?) ?: "10.0.2.2"
                buildConfigField("String", "LIVE_HELP_HTTP", "\"http://$liveHelpHost:8787\"")
                buildConfigField("String", "LIVE_HELP_WS", "\"ws://$liveHelpHost:8787/ws\"")
                buildConfigField("String", "LIVE_HELP_WEB", "\"http://$liveHelpHost:5174\"")
            }
        }
        release {
            // Minification stays off until core features are stable (per plan).
            isMinifyEnabled = false
            buildConfigField("String", "LIVE_HELP_HTTP", "\"https://$liveHelpProductionHost\"")
            buildConfigField("String", "LIVE_HELP_WS", "\"wss://$liveHelpProductionHost/ws\"")
            buildConfigField("String", "LIVE_HELP_WEB", "\"https://$liveHelpProductionHost\"")
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
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":glasses"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.litertlm.android)
    implementation(libs.googleMlkitObjDetection)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.stream.webrtc.android)
    implementation(libs.gson)
    implementation(libs.okhttp)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
}
