import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace  = "io.github.shnaki.kmpwebview"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.shnaki.kmpwebview"
        minSdk        = 24
        targetSdk     = 35
        versionCode   = 1
        versionName   = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures { compose = true }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    // Shared KMP module
    implementation(project(":shared"))

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.androidx.activity.compose)

    // WebView bridge
    implementation(libs.androidx.webkit)
    implementation(libs.kotlinx.coroutines.android)

    // Features
    implementation(libs.play.services.code.scanner) // QR (カメラ権限不要)
    implementation(libs.play.services.location)     // GPS
    // NFC は標準 android.nfc を使用するため追加依存なし
}
