import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackOutput.Target

plugins {
    id("com.techshroom.incise-blue")
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

inciseBlue {
    ide()
    license()
    util {
        javaVersion = JavaVersion.VERSION_14
    }
}

kotlin {
    jvm()
    js()

    sourceSets {
        all {
            languageSettings.useExperimentalAnnotation("kotlin.RequiresOptIn")
        }
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.1")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        jvm().compilations["main"].defaultSourceSet {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
            }
        }
        // JVM-specific tests and their dependencies:
        jvm().compilations["test"].defaultSourceSet {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
        js().compilations["main"].defaultSourceSet {
            dependencies {
                implementation(kotlin("stdlib-js"))
            }
        }
        js {
            moduleName = "common"
            useCommonJs()
            browser()
            binaries.executable()
        }
    }
}

tasks.withType<KotlinJsCompile>().configureEach {
    kotlinOptions {
        moduleKind = Target.COMMONJS
    }
}

base.archivesBaseName = "common"
