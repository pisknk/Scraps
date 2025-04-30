plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.playpass.scraps"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.playpass.scraps"
        minSdk = 24
        targetSdk = 35
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
    
    // Enable font support
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.google.auth)
    implementation(libs.firebase.auth)
    implementation(libs.google.identity.services)
    implementation("androidx.browser:browser:1.6.0")
    implementation("androidx.credentials:credentials:1.2.0")
    // Add support for font resources
    implementation("androidx.core:core:1.12.0")
    
    // Add preference library for SharedPreferences
    implementation("androidx.preference:preference:1.2.1")
    
    // Navigation components and Fragment support
    implementation("androidx.navigation:navigation-fragment:2.7.7")
    implementation("androidx.navigation:navigation-ui:2.7.7")
    implementation("androidx.fragment:fragment:1.6.2")
    
    // SwipeRefreshLayout support
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    
    // Retrofit for API calls
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    
    // Gson for JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Glide for image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    
    // Firebase dependencies
    // implementation("com.google.firebase:firebase-firestore:24.9.1") // Replaced with Realtime Database
    implementation("com.google.firebase:firebase-database:20.3.1") // Firebase Realtime Database
    implementation("com.google.firebase:firebase-auth:22.3.1")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}