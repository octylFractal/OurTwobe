plugins {
    application
    alias(libs.plugins.licenser)
    alias(libs.plugins.kotlin.jvm)
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(19))

kotlin {
    target {
        compilations.configureEach {
            kotlinOptions {
                jvmTarget = "18"
                freeCompilerArgs = listOf(
                    "-opt-in=kotlin.RequiresOptIn"
                )
            }
        }
    }
}

repositories {
    mavenCentral()
    maven {
        name = "m2-dv8tion"
        url = uri("https://m2.dv8tion.net/releases")
        mavenContent {
            includeGroupByRegex("net\\.dv8tion(\\..*|)$")
        }
    }
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

    implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.6.4"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug")

    implementation(platform("io.ktor:ktor-bom:2.2.3"))

    implementation("io.ktor:ktor-client-core")
    implementation("io.ktor:ktor-client-okhttp")

    implementation("io.ktor:ktor-client-content-negotiation")

    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-netty")

    implementation("io.ktor:ktor-server-auth")
    implementation("io.ktor:ktor-server-auto-head-response")
    implementation("io.ktor:ktor-server-call-logging")
    implementation("io.ktor:ktor-server-compression")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-server-sessions")
    implementation("io.ktor:ktor-server-status-pages")

    implementation("io.ktor:ktor-serialization-jackson")

    implementation("com.google.guava:guava:31.1-jre")

    implementation("net.dv8tion:JDA:4.4.0_352")

    val javacppPresets = mapOf(
        "ffmpeg" to "5.1.2",
        "javacpp" to null
    )
    val javacppVersion = "1.5.8"
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


    implementation("com.techshroom:greenish-jungle:0.0.3")

    implementation("org.slf4j:slf4j-api:2.0.6")
    implementation(libs.logback.classic)
    implementation(libs.logback.core)
    implementation("io.github.microutils:kotlin-logging:3.0.4")

    implementation(platform("com.fasterxml.jackson:jackson-bom:2.14.2"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-properties")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
}

license {
    exclude {
        it.file.startsWith(project.buildDir)
    }
    header(rootProject.file("HEADER.txt"))
    (this as ExtensionAware).extra.apply {
        for (key in listOf("organization", "url")) {
            set(key, rootProject.property(key))
        }
    }
}

application.mainClass.set("net.octyl.ourtwobe.OurTwobeKt")
