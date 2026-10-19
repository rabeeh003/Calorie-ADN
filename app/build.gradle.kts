plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    useLibrary("android.test.runner", false)
    useLibrary("android.test.base", false)
    namespace = "com.adn.calorieadn"
    compileSdk = 35
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    defaultConfig {
        applicationId = "com.adn.calorieadn"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.1"
        testInstrumentationRunner = "android.test.InstrumentationTestRunner"
    }
}
