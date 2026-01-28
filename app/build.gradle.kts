import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.serialization)
}

// Carrega as propriedades do arquivo local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    namespace = "com.montanhajr.petclicker"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.montanhajr.petclicker"
        minSdk = 24
        targetSdk = 36
        versionCode = 12
        versionName = "1.4.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // Lê os dados de assinatura do local.properties
            val storeFilePath = localProperties.getProperty("RELEASE_STORE_FILE")
            if (storeFilePath != null) {
                storeFile = file(storeFilePath)
                storePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD")
                keyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS")
                keyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            // IDs de Teste Oficiais
            manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
            buildConfigField("String", "BANNER_AD_UNIT_ID", "\"ca-app-pub-3940256099942544/6300978111\"")
            buildConfigField("String", "REWARDED_AD_UNIT_ID", "\"ca-app-pub-3940256099942544/5224354917\"")
            buildConfigField("String", "INTERSTITIAL_AD_UNIT_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
        }
        release {
            isMinifyEnabled = false
            
            // Atribui a configuração de assinatura de release
            signingConfig = if (signingConfigs.findByName("release")?.storeFile?.exists() == true) signingConfigs.getByName("release") else null

            // Busca os IDs reais do local.properties ou usa fallback de teste se não encontrar
            val releaseAppId = localProperties.getProperty("ADMOB_RELEASE_APP_ID") ?: "ca-app-pub-3940256099942544~3347511713"
            val releaseBannerId = localProperties.getProperty("ADMOB_RELEASE_BANNER_UNIT_ID") ?: "ca-app-pub-3940256099942544/6300978111"
            val releaseRewardedId = localProperties.getProperty("ADMOB_RELEASE_REWARDED_UNIT_ID") ?: "ca-app-pub-3940256099942544/5224354917"
            val releaseInterstitialId = localProperties.getProperty("ADMOB_RELEASE_INTERSTITIAL_UNIT_ID") ?: "ca-app-pub-3940256099942544/1033173712"
            
            manifestPlaceholders["admobAppId"] = releaseAppId
            buildConfigField("String", "BANNER_AD_UNIT_ID", "\"$releaseBannerId\"")
            buildConfigField("String", "REWARDED_AD_UNIT_ID", "\"$releaseRewardedId\"")
            buildConfigField("String", "INTERSTITIAL_AD_UNIT_ID", "\"$releaseInterstitialId\"")
            
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.media)
    implementation(libs.androidx.navigation.runtime.android)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.datastore.preferences)
    
    // AdMob
    implementation(libs.google.play.services.ads)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}