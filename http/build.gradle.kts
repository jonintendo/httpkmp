plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)    
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)    
    alias(libs.plugins.ktor)
    alias(libs.plugins.maven.publish)
}

kotlin {

    // Target declarations - add or remove as needed below. These define
    // which platforms this KMP module supports.
    // See: https://kotlinlang.org/docs/multiplatform-discover-project.html#targets
    androidLibrary {
        namespace = "com.connection.http"
        compileSdk = 36
        minSdk = 24

        withHostTestBuilder {
        }

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    jvm()
    // For iOS targets, this is also where you should
    // configure native binary output. For more information, see:
    // https://kotlinlang.org/docs/multiplatform-build-native-binaries.html#build-xcframeworks

    // A step-by-step guide on how to include this library in an XCode
    // project can be found here:
    // https://developer.android.com/kotlin/multiplatform/migrate
    val xcfName = "httpKit"

    iosX64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    iosArm64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    iosSimulatorArm64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    // Source set declarations.
    // Declaring a target automatically creates a source set with the same name. By default, the
    // Kotlin Gradle Plugin creates additional source sets that depend on each other, since it is
    // common to share sources between related targets.
    // See: https://kotlinlang.org/docs/multiplatform-hierarchy.html
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.ktor.clientCore)
                implementation(libs.ktor.clientCio) // Or another engine like 'android', 'java'
                implementation(libs.ktor.clientNegotiation)
                implementation(libs.ktor.serializationJson)

                implementation(libs.ktor.serverCore)
                implementation(libs.ktor.serverCio) // Or another engine like 'android', 'java'
                implementation(libs.ktor.serverNegotiation)


                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                implementation(libs.androidx.lifecycle.viewmodelCompose)
                implementation(libs.androidx.lifecycle.runtimeCompose)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        androidMain {
            dependencies {
                //implementation(libs.ktor.serverNetty)
            }
        }

        getByName("androidDeviceTest") {
            dependencies {
                implementation(libs.androidx.runner)
                implementation(libs.androidx.core)
                implementation(libs.androidx.testExt.junit)
            }
        }

        iosMain {
            dependencies {
                //implementation(libs.ktor.server.darwin)
            }
        }
        jvmMain {
            dependencies{
                //implementation(libs.ktor.serverNetty)
                implementation(libs.ktor.serverCore.jvm)
            }
        }
    }

}


group = "systems.untangle"
version = "0.2.1"

mavenPublishing {
    //publishToMavenCentral()
    //signAllPublications()

    coordinates(
        group.toString(),
        "http",
        version.toString()
    )

    pom {
        name = "http"
        description = "A tile map component in pure compose multiplatform"
        inceptionYear = "2025"
        url = "https://github.com/votini/karta"

        licenses {
            license {
                name = "MIT License"
                url = "http://www.opensource.org/licenses/mit-license.php"
            }
        }

        developers {
            developer {
                name = "Jonathan Oliveira"
                email = "jonintendox@gmail.com"
            }
        }

        scm {
            url = "https://github.com/votini/karta"
            connection = "scm:git:git://github.com:vottini/karta.git"
            developerConnection = "scm:git:ssh://github.com:vottini/karta.git"
        }
    }
}

