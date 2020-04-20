import java.util.regex.Pattern

plugins {
    id("com.techshroom.incise-blue")
    `lifecycle-base`
}

inciseBlue {
    license()
}

val localLibraries = project.buildDir.resolve("local-libraries")

class RestructureKtDTs(reader: java.io.Reader) : java.io.FilterReader(doRestructureKtDTs(reader)) {
    companion object {
        fun doRestructureKtDTs(reader: java.io.Reader): java.io.Reader {
            val content = reader.readLines().toMutableList()
            if (content.size > 0) {
                if (content[0].startsWith("declare namespace")) {
                    // nah, thanks
                    content[0] = ""
                    content[content.lastIndex] = ""
                }
                val regex = Regex("""^\s+namespace""")
                for ((i, line) in content.withIndex()) {
                    if (regex.find(line) != null) {
                        content[i] = line.replaceFirst("namespace", "export namespace")
                    }
                }
            }
            return content.joinToString(separator = "\n").reader()
        }
    }
}

val copyCommonJs = tasks.register<Copy>("copyCommonJs") {
    // can't be bothered to find what actually generates the .d.ts
    dependsOn(project(":common").tasks.named("build"))
    from(rootProject.buildDir.resolve("js/packages/common/kotlin/common.d.ts")) {
        filter(RestructureKtDTs::class)
    }
    from(project(":common").buildDir.resolve("distributions/common.js"))
    from(project(":common").buildDir.resolve("distributions/common.js.map"))
    rename(Pattern.compile("""common(\..*)"""), "index$1")
    into(localLibraries.resolve("common"))
}

val compileJs = tasks.register<Exec>("compileJs") {
    inputs.files(copyCommonJs)
    // this file is a stand-in for `node_modules` -- I think it should be sufficient
    inputs.file("package-lock.json")
    inputs.files("package.json", ".babelrc", "postcss.config.js", "tsconfig.json", "webpack.config.js")
    inputs.dir("src")
    inputs.dir("typings")

    outputs.dir("dist")

    dependsOn(copyCommonJs)
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
