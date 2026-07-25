plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    namespace = "com.trae.medievaltranslator"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.trae.medievaltranslator"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    // خواندن متن از روی عکس (OCR)
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.0")
    // موتور ترجمه آفلاین گوگل
    implementation("com.google.mlkit:translate:17.0.2")
}
