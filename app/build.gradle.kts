import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.neri.exiftools"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.neri.exiftools"
        minSdk = 26
        targetSdk = 35
        versionCode = 10
        versionName = "1.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

val publishReleaseApk = tasks.register("publishReleaseApk") {
    doNotTrackState("正式签名的安装包只放到 dist，不进 Git。")
    doLast {
        val version = android.defaultConfig.versionName
        val apkName = "音理ExifTools-$version.apk"
        val distDir = rootProject.layout.projectDirectory.dir("dist").asFile
        val source = layout.buildDirectory.file("outputs/apk/release/app-release.apk").get().asFile
        val target = distDir.resolve(apkName)
        distDir.mkdirs()
        distDir.listFiles()
            ?.filter { file -> file.isFile && file.extension.equals("apk", ignoreCase = true) && file.name != apkName }
            ?.forEach { it.delete() }
        source.copyTo(target, overwrite = true)
        layout.buildDirectory.dir("outputs/apk").get().asFile.deleteRecursively()
    }
}

tasks.matching { it.name == "assembleRelease" }.configureEach {
    finalizedBy(publishReleaseApk)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.exifinterface)
    implementation(libs.metadata.extractor)

    debugImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
