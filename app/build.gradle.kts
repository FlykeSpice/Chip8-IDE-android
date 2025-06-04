plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.flykespice.chip8ide"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.flykespice.chip8ide"
        minSdk = 26 //api level 26 because that is the min supported to use Match Groups in Kotlin
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.compose.material3:material3:1.4.0-alpha15")
    implementation("androidx.compose.material:material-icons-core:1.7.5")
    implementation("androidx.compose.material:material-icons-extended:1.7.5")
    implementation("androidx.compose.ui:ui:1.9.0-alpha03")
    implementation("androidx.compose.ui:ui-tooling:1.9.0-alpha03")
    implementation("androidx.compose.ui:ui-tooling-preview:1.9.0-alpha03")
    implementation("androidx.lifecycle:lifecycle-runtime:2.9.0")
    implementation("androidx.activity:activity-compose:1.12.0-alpha01")
    implementation("androidx.navigation:navigation-compose:2.9.0")

    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.8")

    testImplementation("junit:junit:4.13.2")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.9.0-alpha03")

    debugImplementation("androidx.compose.ui:ui-tooling:1.9.0-alpha03")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.9.0-alpha03")
}