plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.javaassignment"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.javaassignment"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)


    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")

    // ViewModel and LiveData (Fixes your MainActivity errors)
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata:2.6.2")

    // Google Play Services (Fixes the FusedLocationProviderClient)
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Google Maps SDK - for embedding the map inside the app
    implementation("com.google.android.gms:play-services-maps:18.2.0")

    // OkHttp - for making HTTP requests to Google Directions API
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // JSON parsing (org.json is already included with Android, but this is for clarity)
}