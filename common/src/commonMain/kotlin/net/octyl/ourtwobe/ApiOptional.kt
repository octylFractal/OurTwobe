@file:Export
package net.octyl.ourtwobe

import kotlinx.serialization.Serializable
import net.octyl.ourtwobe.interop.Export

@Serializable
data class ApiOptional<T>(val value: T?)
