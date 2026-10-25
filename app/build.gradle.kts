import java.util.Properties
plugins {
    alias(libs.plugins.android.application)
}

val localConfig = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
val dropboxAppKey = (localConfig.getProperty("DROPBOX_APP_KEY")
    ?: System.getenv("DROPBOX_APP_KEY") ?: "").trim()
require(dropboxAppKey.isEmpty() || dropboxAppKey.matches(Regex("[a-zA-Z0-9]+"))) {
    "DROPBOX_APP_KEY must contain only letters and digits"
}

android {
    buildFeatures { buildConfig = true }
    namespace = "vn.edu.usth.dropboxclient"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        buildConfigField("String", "DROPBOX_APP_KEY", "\"$dropboxAppKey\"")
        manifestPlaceholders["dropboxAuthScheme"] = "db-${dropboxAppKey.ifEmpty { "unconfigured" }}"
        applicationId = "vn.edu.usth.dropboxclient"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation("com.dropbox.core:dropbox-core-sdk:8.0.2")
    implementation("com.dropbox.core:dropbox-android-sdk:8.0.2")

    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.4.0")
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
