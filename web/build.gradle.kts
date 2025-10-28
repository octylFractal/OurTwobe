import org.cadixdev.gradle.licenser.header.HeaderStyle

plugins {
    `lifecycle-base`
    alias(libs.plugins.licenser)
}

license {
    exclude {
        it.file.startsWith(project.layout.buildDirectory.get().asFile)
    }
    style.putAt("ts", HeaderStyle.BLOCK_COMMENT)
    style.putAt("tsx", HeaderStyle.BLOCK_COMMENT)
    header(rootProject.file("HEADER.txt"))
    (this as ExtensionAware).extra.apply {
        for (key in listOf("organization", "url")) {
            set(key, rootProject.property(key))
        }
    }
    tasks.register("typescript") {
        files.from("src")
    }
}
