plugins {
    application
    alias(libs.plugins.licenser)
    alias(libs.plugins.kotlin.jvm)
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))

kotlin {
    target {
        compilations.configureEach {
            kotlinOptions {
                freeCompilerArgs = listOf(
                    "-opt-in=kotlin.RequiresOptIn"
                )
            }
        }
    }
}

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
    implementation("io.ktor:ktor-server-compression")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-server-sessions-jvm")
    implementation("io.ktor:ktor-server-status-pages")

    implementation("io.ktor:ktor-serialization-jackson")

    implementation(libs.guava)

    implementation(libs.jda)

    val javacppPresets = mapOf(
        "ffmpeg" to "6.1.1",
        "javacpp" to null
    )
    val javacppVersion = "1.5.10"
    // take desktop platforms, 64 bit
    val wantedPlatforms = listOf("linux", "macosx", "windows").map { "$it-x86_64" }
    for ((name, version) in javacppPresets) {
        val fullVersion = when (version) {
            null -> javacppVersion
            else -> "$version-$javacppVersion"
        }
        implementation("org.bytedeco:$name:$fullVersion")
        for (platform in wantedPlatforms) {
            implementation("org.bytedeco:$name:$fullVersion:$platform")
            implementation("org.bytedeco:$name:$fullVersion:$platform")
        }
    }


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

license {
    exclude {
        it.file.startsWith(project.layout.buildDirectory.get().asFile)
    }
    header(rootProject.file("HEADER.txt"))
    (this as ExtensionAware).extra.apply {
        for (key in listOf("organization", "url")) {
            set(key, rootProject.property(key))
        }
    }
}

tasks.jar {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

application.mainClass.set("net.octyl.ourtwobe.OurTwobeKt")
