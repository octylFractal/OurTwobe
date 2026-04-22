plugins {
    application
    alias(libs.plugins.levelHeadered)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(25))

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform(kotlin("bom")))
    implementation(kotlin("stdlib-jdk8"))

    listOf(libs.lwjgl.asProvider(), libs.lwjgl.jemalloc, libs.lwjgl.opus).forEach { lwjglLib ->
        implementation(lwjglLib)
        listOf("macos", "linux", "windows").forEach { os ->
            implementation(variantOf(lwjglLib) {
                classifier("natives-$os")
            })
        }
    }

    implementation(platform(libs.coroutines.bom))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug")

    implementation(platform(libs.ktor.bom))

    implementation("io.ktor:ktor-client-core-jvm")
    implementation("io.ktor:ktor-client-okhttp-jvm")

    implementation("io.ktor:ktor-client-content-negotiation")

    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")

    implementation("io.ktor:ktor-server-auth")
    implementation("io.ktor:ktor-server-auto-head-response")
    implementation("io.ktor:ktor-server-call-logging")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-server-sessions-jvm")
    implementation("io.ktor:ktor-server-status-pages")

    implementation("io.ktor:ktor-serialization-jackson")

    implementation(libs.guava)

    implementation(libs.jda)
    implementation(libs.jdave.api)
    runtimeOnly(libs.jdave.linux.x64)

    implementation(libs.javacpp.ffmpeg)

    implementation(libs.greenishJungle)

    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)
    implementation(libs.logback.core)
    implementation(libs.kotlin.logging)

    implementation(platform(libs.jackson.bom))
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-properties")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
}

levelHeadered {
    headerTemplate(rootProject.file("HEADER.txt"))
    templateVariables.set(mapOf(
        "organization" to rootProject.property("organization").toString(),
        "url" to rootProject.property("url").toString(),
    ))
}

tasks.jar {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

application {
    mainClass = "net.octyl.ourtwobe.OurTwobeKt"
    applicationDefaultJvmArgs = listOf(
        "--enable-native-access=ALL-UNNAMED",
    )
}
