plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    // Plugin 'kapt' para procesadores de anotaciones como Room y Glide
   // id("org.jetbrains.kotlin.kapt")
    // Plugin de Google Services para Firebase
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

    // AÑADIR ESTO: Habilita ViewBinding para acceder a las vistas de forma segura
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
    implementation(libs.androidx.constraintlayout)
    implementation("androidx.cardview:cardview:1.0.0")

    // ---- FIREBASE: Se declara la BoM UNA SOLA VEZ ----
    // La BoM se encarga de gestionar las versiones de las demás librerías de Firebase
    implementation(platform(libs.firebase.bom)) // Usando la versión de libs.versions.toml

    // Ahora se añaden los componentes de Firebase SIN especificar versión
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-storage-ktx") // Añadida por si subes fotos

    // ---- ROOM DATABASE ----
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    //kapt(libs.room.compiler) // Procesador de anotaciones para Room
    ksp(libs.room.compiler)

    // ---- COROUTINES Y LIFECYCLE ----
    implementation(libs.kotlinx.coroutines.android)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1") // Para integrar coroutines con tareas de Firebase

    // ---- GLIDE (Para Cargar Imágenes) ----
    implementation("com.github.bumptech.glide:glide:4.16.0")
    //kapt("com.github.bumptech.glide:compiler:4.16.0") // Procesador de anotaciones para Glide
    //ksp("com.github.bumptech.glide:ksp:4.16.0")
    implementation("io.coil-kt:coil:2.6.0")

    // ---- DEPENDENCIAS DE TEST ----
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

