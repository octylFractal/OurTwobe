package net.octyl.ourtwobe

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

val JACKSON = ObjectMapper().registerKotlinModule()

fun Any.convertToMapViaJackson(): Map<String, Any> = JACKSON.convertValue(this)
