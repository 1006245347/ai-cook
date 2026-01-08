enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
pluginManagement {
    repositories {
//        maven("https://maven.aliyun.com/repository/google")  // 阿里云镜像
//        maven("https://maven.aliyun.com/repository/public")
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven ("https://jitpack.io")
    }
}

dependencyResolutionManagement {
    repositories {
//        maven("https://maven.aliyun.com/repository/google")  // 阿里云镜像
//        maven("https://maven.aliyun.com/repository/public")
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://jitpack.io")
        maven("https://jogamp.org/deployment/maven") //composeWebview需要
    }
}

rootProject.name = "Cook"
include(":androidApp")
include(":desktop")
include(":shared")