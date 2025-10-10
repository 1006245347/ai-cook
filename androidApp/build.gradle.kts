import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
}

//处理依赖库重复问题
configurations.all {
    resolutionStrategy.force("androidx.compose.ui:ui-test-junit4-android:1.7.6")
        .force("androidx.compose.ui:ui-test-android:1.7.6")
    exclude(group = "io.modelcontextprotocol", module = "kotlin-sdk-client-jvm")
    // FIXME exclude netty from Koog dependencies?
    exclude(group = "io.netty", module = "*")
}

android {
    namespace = "com.hwj.cook.android"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.hwj.cook.android"
        minSdk = 27
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"),"proguard-rules.pro")

        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("libs")
        }
    }

    //自定义打包文件名
    afterEvaluate {
        tasks.named("assembleRelease") {
            finalizedBy("copyAndRenameApkTask")
        }
    }
}

val copyAndRenameApkTask by tasks.registering(Copy::class) {
    val config = project.android.defaultConfig
    val versionName = config.versionName
    val versionCode = config.versionCode
    val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm")
    val createTime = LocalDateTime.now().format(formatter)
    val gitHash = providers.exec {
        commandLine("git", "rev-parse", "--short", "HEAD")
    }.standardOutput.asText.get()
    val destDir = File(rootDir, "apkBackup/compose_${versionName}")
    from("release/androidApp-release.apk")
    into(destDir)
    rename { _ -> "compose_ai_${versionName}_${versionCode}_${createTime}.apk" }
    doLast {
        File(destDir, "App上传配置.txt").outputStream().bufferedWriter().use {
            it.appendLine("版本号:${versionCode}")
                .appendLine("版本名称:${versionName}")
                .appendLine("软件名称:AI Cook")
                .appendLine("软件包名:com.hwj.cook")
                .appendLine("版本说明:kotlin multiplatform、compose-multiplatform")
                .appendLine("发布时间:${createTime}")
                .appendLine("git记录:${gitHash}")
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))

    implementation(projects.shared)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.androidx.activity.compose)
    debugImplementation(libs.compose.ui.tooling)
}