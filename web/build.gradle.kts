import net.octyl.levelheadered.HeaderApplyTask
import net.octyl.levelheadered.HeaderVerifyTask
import net.octyl.levelheadered.rewriter.StandardFileHeaderRewriter

plugins {
    `lifecycle-base`
    alias(libs.plugins.levelHeadered)
}


levelHeadered {
    headerTemplate(rootProject.file("HEADER.txt"))
    templateVariables.set(mapOf(
        "organization" to rootProject.property("organization").toString(),
        "url" to rootProject.property("url").toString(),
    ))
    val existing = rewriters.get()
    rewriters.set(existing + listOf(
        "ts" to StandardFileHeaderRewriter.SLASH_STAR_COMMENT,
        "tsx" to StandardFileHeaderRewriter.SLASH_STAR_COMMENT,
    ))
}
val applyHeader = tasks.register<HeaderApplyTask>("applyHeader") {
    source.from("src")
}
val verifyHeader = tasks.register<HeaderVerifyTask>("verifyHeader") {
    source.from("src")
    headerApplyTaskPath.set(applyHeader.map { it.path })
}

tasks.register("applyHeaderToAll") {
    group = "formatting"
    description = "Applies file headers to all source sets."
    dependsOn(applyHeader)
}

tasks.named("check") {
    dependsOn(verifyHeader)
}
