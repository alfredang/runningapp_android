import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// Read the Google Maps API key from local.properties (gitignored — never commit keys).
val mapsApiKey: String = run {
    val p = Properties()
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { p.load(it) }
    p.getProperty("MAPS_API_KEY") ?: ""
}

android {
    namespace = "com.tertiaryinfotech.runtrackgps"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tertiaryinfotech.runtrackgps"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        // Surfaced to AndroidManifest.xml as ${MAPS_API_KEY}.
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        create("release") {
            val ks = rootProject.file("keystore/runtrackgps-release.jks")
            val kp = Properties()
            val kf = rootProject.file("keystore/keystore.properties")
            if (kf.exists()) kf.inputStream().use { kp.load(it) }
            if (ks.exists() && kp.getProperty("storePassword") != null) {
                storeFile = ks
                storePassword = kp.getProperty("storePassword")
                keyAlias = kp.getProperty("keyAlias")
                keyPassword = kp.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            val rel = signingConfigs.getByName("release")
            if (rel.storeFile != null) signingConfig = rel
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Force a modern Fragment version. The app uses ComponentActivity (no Fragments),
    // but a stale transitive fragment:1.1.0 trips the InvalidFragmentVersionForActivityResult
    // lint check on registerForActivityResult; 1.3.0+ has the corrected behavior.
    implementation("androidx.fragment:fragment:1.8.5")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Location (FusedLocationProvider) + Google Maps
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.maps.android:maps-compose:6.4.1")
    implementation("com.google.android.gms:play-services-maps:19.0.0")

    // JSON persistence
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
