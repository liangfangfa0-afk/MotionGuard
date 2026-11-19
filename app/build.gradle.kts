plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.ljj.yundongshuhuan"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.ljj.yundongshuhuan"
        minSdk = 34
        targetSdk = 35
        versionCode = 194
        versionName = "1.94"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
    buildTypes { release { isMinifyEnabled = false } }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom-alpha:2026.08.01"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("io.github.itoseo:coui:0.0.1-alpha12")
    implementation("io.github.itoseo:coui-icons:0.0.1-alpha12")
}
