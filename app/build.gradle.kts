plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services") version "4.4.4"
}

android {
    namespace = "com.example.test_kotlin_compose"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.test_kotlin_compose"
        minSdk = 24
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

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(platform(libs.androidx.compose.bom.v20240902))
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.mediation.test.suite)

    // Use the integration module (ads, remote config, etc.)
    implementation(project(":integration"))

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.8.0")

    // Hilt (still needed in app for @AndroidEntryPoint Application/Activity)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // For instrumentation tests
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.57.2")
    androidTestAnnotationProcessor("com.google.dagger:hilt-compiler:2.57.2")

    // For local unit tests
    testImplementation("com.google.dagger:hilt-android-testing:2.57.2")
    testAnnotationProcessor("com.google.dagger:hilt-compiler:2.57.2")

    // If the app directly uses these SDKs outside integration, keep them here.
    // Otherwise they can be removed because :integration already includes them.
    // implementation("com.google.android.gms:play-services-ads:23.3.0")
    // implementation("com.google.android.ump:user-messaging-platform:3.1.0")
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-config")
    // implementation("com.google.firebase:firebase-analytics")

    // Coil stays in app (UI uses it)
    implementation("io.coil-kt:coil:2.7.0")
    implementation("io.coil-kt:coil-compose:2.7.0")

}