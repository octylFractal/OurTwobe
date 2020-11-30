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
        javaVersion = JavaVersion.VERSION_15
    }
    lwjgl {
        lwjglVersion = "3.2.4-SNAPSHOT"
        addDependency("", true)
        addDependency("jemalloc", true)
        addDependency("opus", true)
    }
}

dependencies {
    implementation(platform(kotlin("bom")))
    implementation(kotlin("stdlib-jdk8"))

    implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.4.1"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    implementation(platform("io.ktor:ktor-bom:1.4.1"))
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-client-core")
    implementation("io.ktor:ktor-client-okhttp")
    implementation("io.ktor:ktor-client-jackson")
    implementation("io.ktor:ktor-jackson")
    implementation("io.ktor:ktor-auth")

    implementation("com.google.guava:guava:30.0-jre")

    implementation("net.dv8tion:JDA:4.2.0_214")

    val javacppPresets = mapOf(
        "ffmpeg" to "4.3.1",
        "javacpp" to null
    )
    val javacppVersion = "1.5.4"
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

    implementation("org.slf4j:slf4j-api:1.7.30")
    commonLib("ch.qos.logback", "logback", "1.2.3") {
        implementation(lib("classic"))
        implementation(lib("core"))
    }
    implementation("io.github.microutils:kotlin-logging:2.0.3")

    implementation(platform("com.fasterxml.jackson:jackson-bom:2.11.3"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-properties")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs = listOf(
        "-Xopt-in=kotlin.RequiresOptIn"
    )
}

application.mainClass.set("net.octyl.ourtwobe.OurTwobeKt")
