plugins {
    val ktVersion = "1.4.20"
    id("com.techshroom.incise-blue") version "0.5.7"
    kotlin("jvm") version ktVersion apply false
    kotlin("multiplatform") version ktVersion apply false
    kotlin("plugin.serialization") version ktVersion apply false
}

inciseBlue {
    ide()
}
