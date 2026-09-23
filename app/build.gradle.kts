plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.neri.exiftools"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.neri.exiftools"
        minSdk = 26
        targetSdk = 35
        versionCode = 7
        versionName = "1.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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

val publishDebugApk = tasks.register("publishDebugApk") {
    doNotTrackState("安装包只放到 dist，不进 Git。")
    doLast {
        val version = android.defaultConfig.versionName
        val apkName = "音理ExifTools-$version.apk"
        val distDir = rootProject.layout.projectDirectory.dir("dist").asFile
        val source = layout.buildDirectory.file("outputs/apk/debug/app-debug.apk").get().asFile
        val target = distDir.resolve(apkName)
        distDir.mkdirs()
        distDir.listFiles()
            ?.filter { file -> file.isFile && file.extension.equals("apk", ignoreCase = true) && file.name != apkName }
            ?.forEach { it.delete() }
        source.copyTo(target, overwrite = true)
        layout.buildDirectory.dir("outputs/apk").get().asFile.deleteRecursively()
    }
}

tasks.matching { it.name == "assembleDebug" }.configureEach {
    finalizedBy(publishDebugApk)
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
