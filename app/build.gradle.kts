plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.tttn_hoangdaivms"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.tttn_hoangdaivms"
        minSdk = 27
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
    // Google Maps
    implementation("com.google.android.gms:play-services-maps:18.1.0")
    // Location (Fused Location Provider) - optional but useful
    implementation("com.google.android.gms:play-services-location:21.0.1")
    // Apache POI để tạo file .xlsx
    implementation("org.apache.poi:poi:5.2.3")
    implementation("org.apache.poi:poi-ooxml:5.2.3")
    // Material / AndroidX (nếu chưa có)
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    //Glide
    implementation("com.github.bumptech.glide:glide:4.12.0")

}