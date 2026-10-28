plugins {
    id("com.android.application")
    // Turns @Composable functions into UI code
    id("org.jetbrains.kotlin.plugin.compose")
    // Lets navigation routes be plain @Serializable classes (type-safe navigation)
    id("org.jetbrains.kotlin.plugin.serialization")
    // KSP generates code at compile time for Hilt (dependency injection) and Room (database)
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.iot.widget"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.iot.widget"
        minSdk = 26
        targetSdk = 37
        versionCode = 2
        versionName = "2.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        // Robolectric needs the merged resources (fonts, strings) to run Compose UI tests on the JVM
        unitTests.isIncludeAndroidResources = true
    }
}

// Room writes its database schema here; versioned schemas make later migrations reviewable
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Jetpack Compose (versions come from the BOM so they always match each other)
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.navigation:navigation-compose:2.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.12.0")

    // Hilt: dependency injection
    implementation("com.google.dagger:hilt-android:2.60.1")
    ksp("com.google.dagger:hilt-compiler:2.60.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.4.0")
    implementation("androidx.hilt:hilt-work:1.4.0")
    ksp("androidx.hilt:hilt-compiler:1.4.0")

    // Room: local SQLite database
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")

    // DataStore: small key-value file for "who is signed in"
    implementation("androidx.datastore:datastore-preferences:1.3.0")

    // WorkManager: background jobs that run even when the app is closed
    implementation("androidx.work:work-runtime-ktx:2.12.0")

    // Media3: ExoPlayer plays the camera clips; ui-compose draws its video surface in Compose
    implementation("androidx.media3:media3-exoplayer:1.11.1")
    implementation("androidx.media3:media3-ui-compose:1.11.1")

    // Glance: the home-screen widget, written in a Compose-like style
    implementation("androidx.glance:glance-appwidget:1.2.0")
    testImplementation("androidx.glance:glance-appwidget-testing:1.2.0")

    // Real MQTT client goes here later, e.g.:
    // implementation("com.hivemq:hivemq-mqtt-client:1.3.x")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.17")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    testImplementation("app.cash.turbine:turbine:1.2.1")
    testImplementation("androidx.test:core:1.7.0")
    testImplementation("androidx.room:room-testing:2.8.5")
    testImplementation("androidx.work:work-testing:2.12.0")
    testImplementation(composeBom)
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("com.google.dagger:hilt-android-testing:2.60.1")
    kspTest("com.google.dagger:hilt-compiler:2.60.1")
}
