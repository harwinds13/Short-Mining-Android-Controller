plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.harry.shortmining"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.harry.shortmining"
        minSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
dependencies {

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.firebase:firebase-firestore-ktx:25.1.4")

    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
    implementation("com.google.code.gson:gson:2.13.1")

    implementation("com.google.firebase:firebase-auth:24.0.1")


}