plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.autosmart"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.autosmart"
        minSdk = 23
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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)




    implementation ("com.google.firebase:firebase-auth")

    // Dependencia para Google Sign-In
    implementation ("com.google.android.gms:play-services-auth:20.5.0")

    // Otras dependencias
    implementation ("androidx.appcompat:appcompat:1.3.1")
    implementation ("com.google.android.material:material:1.4.0")
    implementation ("androidx.constraintlayout:constraintlayout:2.1.1")

    implementation (libs.material)


    implementation ("com.google.firebase:firebase-auth")
    implementation (libs.firebase.database)


    implementation ("com.google.firebase:firebase-analytics:17.2.1")
    implementation ("com.google.firebase:firebase-database:19.2.0")




    // Room components
    val room_version="2.5.2"
    implementation ("androidx.room:room-runtime:$room_version")

    annotationProcessor ("androidx.room:room-compiler:$room_version")



}