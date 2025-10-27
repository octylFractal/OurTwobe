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

val compileJs = tasks.register<Exec>("compileJs") {
    // this file is a stand-in for `node_modules` -- I think it should be sufficient
    inputs.file("pnpm-lock.yaml")
    inputs.files(
        "package.json", ".babelrc", "postcss.config.js", "tsconfig.json", "webpack.config.js",
        ".eslintignore", ".eslintrc.js"
    )
    inputs.dir("src")
    inputs.dir("typings")

    outputs.dir("dist")

    standardOutput = System.out
    errorOutput = System.err
    val cmdLine = mutableListOf("pnpm", "run", "build", "--")
    val consoleOutput = gradle.startParameter.consoleOutput
    if ((consoleOutput == ConsoleOutput.Auto && System.console() != null) || consoleOutput >= ConsoleOutput.Rich) {
        cmdLine += "--color"
    }
    commandLine(cmdLine)
    workingDir(project.layout.projectDirectory)
}

tasks.named("assemble") {
    dependsOn(compileJs)
}
