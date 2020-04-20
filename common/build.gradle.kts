import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackOutput.Target

plugins {
    id("com.techshroom.incise-blue")
    kotlin("multiplatform")
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
