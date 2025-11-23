import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt.android)
    id("kotlin-parcelize")
}

android {
    namespace = "com.a42r.pevplugshare"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.a42r.pevplugshare"
        minSdk = 26
        targetSdk = 34
        versionCode = 3
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        aidl = true
        buildConfig = true  // Enable BuildConfig generation
    }

    // Load OAuth secrets from secrets.properties (NOT in git)
    val secretsFile = rootProject.file("secrets.properties")
    val secrets = Properties()

    if (secretsFile.exists()) {
        secretsFile.inputStream().use { secrets.load(it) }
    } else {
        logger.warn("WARNING: secrets.properties not found! Using placeholder values.")
        logger.warn("Copy secrets.properties.template to secrets.properties and fill in your secrets.")
    }

    // Helper function to get secret with default value
    fun getSecret(key: String, default: String = "MISSING_SECRET"): String {
        return secrets.getProperty(key, default)
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    // Pass secrets to native code as compile definitions
    defaultConfig {
        externalNativeBuild {
            cmake {
                // Development secrets
                arguments(
                    "-DOAUTH_DEV_CALLBACK_URL=${getSecret("oauth.dev.callback.url", "https://example.com")}",
                    "-DOAUTH_DEV_SHARED_SECRET=${getSecret("oauth.dev.shared.secret", "MISSING_SECRET")}",
                    "-DOAUTH_DEV_DEEP_LINK_SCHEME=${getSecret("oauth.dev.deep.link.scheme", "yourapp://oauth/callback")}"
                )
            }
        }
    }

    buildTypes {
        debug {
            // Development OAuth settings now in native code
            // Secrets injected from secrets.properties at build time
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // Production secrets
            externalNativeBuild {
                cmake {
                    arguments(
                        "-DOAUTH_PROD_CALLBACK_URL=${getSecret("oauth.prod.callback.url", "https://example.com")}",
                        "-DOAUTH_PROD_SHARED_SECRET=${getSecret("oauth.prod.shared.secret", "MISSING_SECRET")}",
                        "-DOAUTH_PROD_DEEP_LINK_SCHEME=${getSecret("oauth.prod.deep.link.scheme", "yourapp://oauth/callback")}"
                    )
                }
            }
        }
    }

    lint {
        abortOnError = false
        warningsAsErrors = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.annotation:annotation:1.7.0") // Explicitly added annotation dependency
    implementation("androidx.browser:browser:1.7.0") // For Custom Tabs OAuth flow

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.activity.ktx)

    // Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.gson)
    implementation(libs.okhttp.logging)

    // Dependency Injection
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Background Tasks
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.hilt.work)
    kapt(libs.hilt.compiler.work)

    // Location Services
    implementation(libs.play.services.location)

    // Map
    implementation("org.osmdroid:osmdroid-android:6.1.18")

    // Preferences
    implementation("androidx.preference:preference-ktx:1.2.1")

    // Photo/EXIF
    implementation(libs.androidx.exifinterface)

    // Glide for image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // FlexboxLayout for flexible UI layouts
    implementation("com.google.android.flexbox:flexbox:3.0.0")

    // Security/Encryption
    implementation(libs.androidx.security.crypto.ktx)

    // Camera
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
