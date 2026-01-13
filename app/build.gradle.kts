import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt") // Para Room compiler
}

// Cargar propiedades del keystore
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { stream ->
        keystoreProperties.load(stream)
    }
}

android {
    namespace = "com.example.controloperador"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.controloperador"
        minSdk = 29
        targetSdk = 36
        versionCode = 9
        versionName = "1.0.9"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // URL base para desarrollo y producción
        // IP DE RED LOCAL: Debe estar en la misma subred que el emulador/dispositivo
        // IPs disponibles: 172.16.24.11 y 172.16.22.58
        
        // buildConfigField("String", "BASE_URL", "\"http://172.16.22.58:8000/api/v1/\"")  // IP actual de red local
        // buildConfigField("String", "BASE_URL", "\"http://172.16.24.11:8000/api/v1/\"")  // IP alternativa
        // buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8000/api/v1/\"")  // Para emulador (si funciona)
        buildConfigField("String", "BASE_URL", "\"https://backtransportistas.tarjetasintegrales.mx:806/api/v1/\"")  // Producción
        
        // Configuración para auto-update desde GitHub
        buildConfigField("String", "GITHUB_REPO_OWNER", "\"Tarjetas-Integrales-Software\"")
        buildConfigField("String", "GITHUB_REPO_NAME", "\"ControlOperador\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties["storeFile"] ?: "")
            storePassword = keystoreProperties["storePassword"] as String? ?: ""
            keyAlias = keystoreProperties["keyAlias"] as String? ?: ""
            keyPassword = keystoreProperties["keyPassword"] as String? ?: ""
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
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
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    
    // Retrofit y OkHttp para comunicación con API REST
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.gson)
    
    // Room para base de datos local
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)
    
    // MPAndroidChart para gráficas
    implementation(libs.mpandroidchart)
    
    // WorkManager para sincronización en background
    implementation(libs.work.runtime.ktx)
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}