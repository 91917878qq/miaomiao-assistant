plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.miaomiao.assistant"
    compileSdk = 37
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.miaomiao.assistant"
        minSdk = 26
        targetSdk = 34
        versionCode = 52
        versionName = "3.0.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Compose 家族（版本 1.9.4）
    implementation("androidx.compose.ui:ui:1.9.4")
    implementation("androidx.compose.ui:ui-graphics:1.9.4")
    implementation("androidx.compose.ui:ui-tooling-preview:1.9.4")
    implementation("androidx.compose.foundation:foundation:1.9.4")
    implementation("androidx.compose.foundation:foundation-layout:1.9.4")
    implementation("androidx.compose.animation:animation:1.9.4")
    implementation("androidx.compose.animation:animation-core:1.9.4")
    implementation("androidx.compose.runtime:runtime:1.9.4")
    implementation("androidx.compose.runtime:runtime-saveable:1.9.4")
    implementation("androidx.compose.material3:material3:1.3.2")
    implementation("androidx.compose.material:material-icons-core:1.7.8")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    // AndroidX
    implementation("androidx.core:core:1.15.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("androidx.savedstate:savedstate-ktx:1.3.3")
    implementation("androidx.exifinterface:exifinterface:1.3.6")

    // Kotlin coroutines (matches reversed 1.8.1)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Liquid Glass (Backdrop) - Kyant0/AndroidLiquidGlass, Apache-2.0
    implementation("io.github.kyant0:backdrop:2.0.1")
    implementation("io.github.kyant0:shapes:1.2.1")

    debugImplementation("androidx.compose.ui:ui-tooling:1.9.4")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.9.4")
}
