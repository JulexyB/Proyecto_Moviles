plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.alquilervehiculos"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.alquilervehiculos"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
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
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // ---- DEPENDENCIAS PRINCIPALES DE ANDROIDX ----
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation("androidx.activity:activity-ktx:1.11.0") // Habilita la delegación "by viewModels()"
    implementation(libs.androidx.constraintlayout)
    implementation("androidx.cardview:cardview:1.0.0")

    // ---- FIREBASE: Se declara la BoM UNA SOLA VEZ ----
    implementation(platform(libs.firebase.bom))

    // Componentes de Firebase sin especificar versión
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")

    // ---- ROOM DATABASE ----
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // ---- COROUTINES Y LIFECYCLE ----
    implementation(libs.kotlinx.coroutines.android)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // ---- CARGA DE IMÁGENES ----
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("io.coil-kt:coil:2.6.0")

    // ---- DEPENDENCIAS DE TEST ----
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
