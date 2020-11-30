import net.minecrell.gradle.licenser.LicenseExtension
import net.minecrell.gradle.licenser.header.HeaderStyle

plugins {
    id("com.techshroom.incise-blue")
    `lifecycle-base`
}

inciseBlue {
    license()
}

configure<LicenseExtension> {
    style.putAt("ts", HeaderStyle.BLOCK_COMMENT)
    style.putAt("tsx", HeaderStyle.BLOCK_COMMENT)
    include("**/*.ts")
    include("**/*.tsx")
    tasks.register("typescript") {
        files = fileTree("src")
    }
}

val compileJs = tasks.register<Exec>("compileJs") {
    // this file is a stand-in for `node_modules` -- I think it should be sufficient
    inputs.file("package-lock.json")
    inputs.files(
        "package.json", ".babelrc", "postcss.config.js", "tsconfig.json", "webpack.config.js",
        ".eslintignore", ".eslintrc.js"
    )
    inputs.dir("src")
    inputs.dir("typings")

    outputs.dir("dist")

    standardOutput = System.out
    errorOutput = System.err
    val cmdLine = mutableListOf("npm", "run", "build", "--")
    val consoleOutput = gradle.startParameter.consoleOutput
    if ((consoleOutput == ConsoleOutput.Auto && System.console() != null) || consoleOutput >= ConsoleOutput.Rich) {
        cmdLine += "--color"
    }
    commandLine(cmdLine)
}

tasks.named("assemble") {
    dependsOn(compileJs)
}
