plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.appstaticsx.app.musico"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.appstaticsx.app.musico"
        minSdk = 23
        targetSdk = 34
        versionCode = 1260
        versionName = "1.2-Alpha(Build-060)"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
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
}