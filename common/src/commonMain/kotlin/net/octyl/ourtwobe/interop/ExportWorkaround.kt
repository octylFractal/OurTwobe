package net.octyl.ourtwobe.interop

import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FILE
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY

@Target(CLASS, PROPERTY, FUNCTION, FILE)
@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
expect annotation class Export()
