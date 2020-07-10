import com.techshroom.inciseblue.commonLib
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    id("com.techshroom.incise-blue")
    kotlin("jvm")
}

inciseBlue {
    ide()
    license()
    util {
        javaVersion = JavaVersion.VERSION_14
    }
}

dependencies {
    implementation(project(":common"))
    implementation(platform(kotlin("bom")))
    implementation(kotlin("stdlib-jdk8"))
    commonLib("org.jetbrains.kotlinx", "kotlinx-coroutines", "1.3.7-1.4-M3") {
        implementation(lib("core"))
        implementation(lib("jdk8"))
        implementation(lib("reactor"))
    }
    implementation("com.google.guava:guava:29.0-jre")
    implementation("com.discord4j:discord4j-core:3.0.15")
    implementation("com.google.firebase:firebase-admin:6.14.0")
    implementation("io.javalin:javalin-bundle:3.9.1")
    implementation("cc.vileda:kotlin-openapi3-dsl:0.21.2")
    implementation("org.slf4j:slf4j-api:1.7.30")
    implementation("com.techshroom:greenish-jungle:0.0.3")
    commonLib("ch.qos.logback", "logback", "1.2.3") {
        implementation(lib("classic"))
        implementation(lib("core"))
    }
    implementation("io.github.microutils:kotlin-logging:1.8.0.1")

    implementation("com.squareup.okhttp3:okhttp:4.7.2")

    implementation(platform("com.fasterxml.jackson:jackson-bom:2.11.1"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs = listOf(
        "-Xopt-in=kotlin.RequiresOptIn"
    )
}

application.mainClassName = "net.octyl.ourtwobe.OurTwobeKt"
