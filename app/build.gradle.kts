plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp) // For Hilt and other annotation processors
    id("com.diffplug.spotless")
}

android {
    namespace = "com.insnaejack.pdfgenerator"
    compileSdk = 34 // Target Android 14 as per plan (API 34)

    defaultConfig {
        applicationId = "com.insnaejack.pdfgenerator"
        minSdk = 23 // Minimum Android 6.0 (Marshmallow) as per plan
        targetSdk = 33 // Target Android 13 (Tiramisu) as per plan
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true // Enable code shrinking for release builds
            isShrinkResources = true // Enable resource shrinking
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // TODO: Add signing configurations for release builds
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf("-Xcontext-receivers") // Example, replace if not needed or add others
        // To enable experimental features like break/continue in inline lambdas,
        // you might need to specify language version or a specific opt-in.
        // For Kotlin 1.9.x, this feature might be stable or require a different opt-in.
        // If the build still fails, the code in PdfGeneratorUtil.kt might need refactoring.
    }
    buildFeatures {
        compose = true
        // viewBinding = true // If using ViewBinding alongside Compose or for older parts
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
        }
    }
}

dependencies {
    // Core Android & Kotlin
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.material) // For Material Design Components

    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Hilt for Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Coroutines for asynchronous programming
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // CameraX for camera functionalities
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.extensions) // For effects like Bokeh, HDR etc. (optional)

    // PDF Generation (Android's built-in PdfDocument for now)
    // No specific library for PdfDocument, it's part of the Android SDK

    // Image Loading (Coil)
    implementation(libs.coil.compose)

    // Google Play Billing
    implementation(libs.play.billing.ktx)

    // Google AdMob (Ads)
    implementation(libs.play.services.ads)

    // DataStore for preferences
    implementation(libs.androidx.datastore.preferences)

    // Accompanist Permissions
    implementation(libs.accompanist.permissions)

    // Google Play Services Auth (for Sign-In)
    implementation(libs.play.services.auth)

    // Google API Client & Drive API
    implementation(libs.google.api.services.drive)
    implementation(libs.google.api.client.android) // Usually not needed directly with play-services-auth
    // Add http client if needed by google-api-services-drive, e.g.,
    implementation("com.google.http-client:google-http-client-gson:1.42.3") // Check for latest compatible version

    // uCrop for image cropping
    implementation("com.github.yalantis:ucrop:2.2.8") // Check for the latest version

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// Kapt block removed as KSP is used for Hilt

spotless {
    // Optional: Specify encoding (UTF-8 is default)
    // encoding("UTF-8")

    kotlin {
        // Use ktlint for Kotlin files
        ktlint(libs.versions.ktlint.get()).editorConfigOverride(
            mapOf(
                "ktlint_standard_no-wildcard-imports" to "disabled",
            ),
        )
        // You can also specify a specific version of ktlint, e.g., ktlint("0.49.1")

        target("**/*.kt")
        targetExclude("**/build/", "**/generated/") // Exclude build and generated directories

        // Optional: Add a license header
        // licenseHeaderFile(rootProject.file("spotless/copyright.kt")) // Example path
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint(libs.versions.ktlint.get()).editorConfigOverride(
            mapOf(
                "ktlint_standard_no-wildcard-imports" to "disabled", // Also for gradle files
            ),
        )
    }
}
