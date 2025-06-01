plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.flykespice.chip8ide"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.flykespice.chip8ide"
        minSdk = 23 //api level 23 because of AudioTrack
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
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.compose.material3:material3:1.4.0-alpha04")
    implementation("androidx.compose.material:material-icons-core:1.7.5")
    implementation("androidx.compose.material:material-icons-extended:1.7.5")
    implementation("androidx.compose.ui:ui:1.8.0-alpha06")
    implementation("androidx.compose.ui:ui-tooling:1.8.0-alpha06")
    implementation("androidx.compose.ui:ui-tooling-preview:1.8.0-alpha06")
    implementation("androidx.lifecycle:lifecycle-runtime:2.9.0-alpha07")
    implementation("androidx.activity:activity-compose:1.10.0-beta01")
    implementation("androidx.navigation:navigation-compose:2.9.0-alpha03")

    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.8")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.7.5")

    debugImplementation("androidx.compose.ui:ui-tooling:1.8.0-alpha06")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.8.0-alpha06")
}