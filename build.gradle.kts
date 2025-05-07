plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}

// Repositories for plugins are defined in settings.gradle.kts
// Versions for plugins and dependencies are defined in gradle/libs.versions.toml

// The allprojects block for repositories was removed as per modern Gradle practice.
/*
allprojects {
    repositories {
        google()
        mavenCentral()
        // Potentially other repositories like JitPack
        // maven { url = uri("https://jitpack.io") }
    }
}
*/

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}