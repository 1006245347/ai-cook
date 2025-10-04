import org.gradle.kotlin.dsl.api
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.ksp)
}

kotlin {
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_17)
                }
            }
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    jvm {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_17) //这修改jdk
                }
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            //put your multiplatform dependencies here
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.materialIconsExtended)


            //**************************** UI相关 ************************
            implementation(libs.compose.webviews)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            //页面自适配判断库
            implementation(libs.windowSize)

            //导航  https://github.com/Tlaster/PreCompose
            implementation(libs.precompose.navigator)
            implementation(libs.precompose.koin)
            implementation(libs.precompose.viewmodel)

            //异步图片加载   //这个版本还缺其他？
            implementation(libs.coil3.svg)
            implementation(libs.coil3.ktor)
            implementation(libs.coil3.compose) //这个地址太坑了，官网没更新出来


            //https://github.com/alexzhirkevich/compottie
            implementation(libs.compottie)
            implementation(libs.compottie.resources)
            implementation(libs.compottie.dot)
            implementation(libs.compottie.network)

            //文件选择器
            api(libs.file.picker)
            api(libs.file.dialog)
            api(libs.file.dialog.compose)
            api(libs.file.coil)

//         //富文本      https://github.com/MohamedRejeb/compose-rich-editor
            implementation(libs.rich.editor)

            //首次引导使用 https://github.com/svenjacobs/reveal
            implementation(libs.reveal)

            //分页库
            implementation(libs.paging.compose)

            //**************************数据处理相关********************
            implementation(libs.stdlib)
            implementation(libs.kotlinX.serializationJson)
            implementation(libs.kotlin.datetime)
            api(libs.kermit)

            //依赖注入
            api(libs.koin.core) //在desktopApp引入了
            implementation(libs.koin.compose)

            //支持多平台的网络库
            implementation(libs.ktor.client.core) //网络请求
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.client.logging)

            //异步协程库
            implementation(libs.kotlinx.coroutines.core)

            //多平台uuid https://github.com/benasher44/uuid/tree/master
            implementation(libs.uuid)

            //key-value存储
            api(libs.multiplatform.settings)
            api(libs.multiplatform.coroutines)
            api(libs.multiplatform.serialization)

            //权限 compose multiplatform https://github.com/icerockdev/moko-permissions
            implementation(libs.stately.common)
            implementation(libs.mokoMvvmCore)
            implementation(libs.mokoMvvmCompose)

            //AI代理框架
            implementation(libs.koog.agents)


            //aallam openai  https://github.com/aallam/openai-kotlin
//            implementation(libs.openai.client)
            implementation(libs.ktoken)
        }

        androidMain.dependencies {
            //引入本地Android aar库
            implementation(
                fileTree(
                    mapOf(
                        "dir" to "libs",
                        "include" to listOf("*.jar", "*.aar")
                    )
                )
            )

            implementation(libs.androidx.perference)
            implementation(libs.accompanist.systemUIController)
            implementation(libs.androidx.core)

            implementation(libs.mokopermission)
            implementation(libs.mokopermission.compose)
            implementation(libs.mokopermission.camera)
            implementation(libs.mokopermission.gallery)
            implementation(libs.mokopermission.storage)
            implementation(libs.mokopermission.notifications)

            // Koin
            api(libs.koin.android)
            api(libs.koin.androidx.compose)

            //android平台引擎
            implementation(libs.ktor.client.android)
            implementation(libs.ktor.client.okhttp)

            implementation(libs.androidx.lifecycle)
            implementation(libs.lifecycle.extension)

            api(libs.core.splashscreen)

            //实现本地数据存储
            implementation(libs.datastore.preferences)
            implementation(libs.multiplatform.datastore)
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.coroutines)

            //远程日志上报
            implementation(libs.android.bugly)

            //权限申请
            implementation(libs.permissionX.android)

            //摄像头
            implementation(libs.camera.lifecycle)
            implementation(libs.camera.view)
            implementation(libs.camera.camera2)
            implementation(libs.accompanist.permissions)
            implementation(libs.kotlinx.coroutines.guava)

            //    //友盟统计、bug上报
            api(files("../androidApp/libs/umeng-apm-v2.0.6.aar"))
            api(files("../androidApp/libs/umeng-asms-v1.8.7.aar"))
            api(files("../androidApp/libs/umeng-common-9.8.8.aar"))
            api(files("../androidApp/libs/uyumao-1.1.4.aar"))
        }

        iosMain.dependencies {
            implementation(libs.mokopermission)
            implementation(libs.mokopermission.compose)
            implementation(libs.mokopermission.camera)
            implementation(libs.mokopermission.gallery)
            implementation(libs.mokopermission.storage)
            implementation(libs.mokopermission.notifications)

            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.coroutines)

            //网络库，提供iOS平台引擎
            implementation(libs.ktor.client.ios)

            implementation(compose.components.resources)
        }

        jvmMain.dependencies {
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.coroutines)
            implementation(libs.kotlinx.coroutines.swing)
            // Toaster for Windows
            implementation(libs.toast4j)
            // JNA for Linux
            implementation("de.jangassen:jfa:1.2.0") {
                // not excluding this leads to a strange error during build:
                // > Could not find jna-5.13.0-jpms.jar (net.java.dev.jna:jna:5.13.0)
                exclude(group = "net.java.dev.jna", module = "jna")
            }

            // JNA for Windows
            implementation(libs.jna)
            implementation(libs.jna.platform)

            //加上可以用预览注解
            implementation(compose.desktop.common)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        binaries.withType<org.jetbrains.kotlin.gradle.plugin.mpp.Framework> {
            @OptIn(ExperimentalKotlinGradlePluginApi::class)
            transitiveExport = true
            compilations.all {
                compileTaskProvider.configure {
                    compilerOptions {//-Xexpect-actual-classes
                        freeCompilerArgs.addAll(
                            listOf(
                                "-linker-options",
                                "-lsqlite3",
                                "-Xexpect-actual-classes"
                            )
                        )
                    }
                }
            }
        }
    }
    cocoapods {  //cocoapods类似gradle管理包构建依赖，这里集成日志库
        version = "1.0"
        summary = "Some description for a Kotlin/Native module"
        homepage = "Link to a Kotlin/Native module homepage"
        ios.deploymentTarget = "14.1"
        framework {
            baseName = "LiteLibs"
            isStatic = true

            // Only if you want to talk to Kermit from Swift
            export("co.touchlab:kermit-simple:2.0.8")
        }
    }
}

android {
    namespace = "com.hwj.cook"
    compileSdk = 35
    defaultConfig {
        minSdk = 29
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}