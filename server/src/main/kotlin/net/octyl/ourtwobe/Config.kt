package net.octyl.ourtwobe

import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.nio.file.Files
import java.nio.file.Path

data class Config(
    val discord: Discord,
    val youTube: YouTube,
    val owner: String,
) {
    data class Discord(
        val token: String,
    )
    data class YouTube(
        val token: String,
    )
}

fun loadConfig(file: Path): Config {
    return Files.newBufferedReader(file).use {
        JavaPropsMapper.builder()
            .addModules(MODULES)
            .build()
            .readValue(it)
    }
}
