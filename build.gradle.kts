plugins {
    val ktVersion = "1.4-M3"
    id("com.techshroom.incise-blue") version "0.5.7"
    kotlin("jvm") version ktVersion apply false
    kotlin("multiplatform") version ktVersion apply false
}

inciseBlue {
    ide()
}

subprojects {
    repositories {
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
        maven("https://dl.bintray.com/kotlin/kotlin-dev")
    }
}
