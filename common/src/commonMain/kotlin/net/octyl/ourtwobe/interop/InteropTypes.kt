package net.octyl.ourtwobe.interop

import kotlin.reflect.KClassifier
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection

interface JsonCodec<T> {
    fun encode(value: T): String
    fun decode(value: String): T
}
