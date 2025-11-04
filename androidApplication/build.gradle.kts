plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(projects.composeLibrary)
    implementation(libs.androidx.activity.compose)
}

android {
    namespace = "com.github.fhilgers.compose.application"
    compileSdk = 36

    defaultConfig {
        minSdk = 28
    }
}