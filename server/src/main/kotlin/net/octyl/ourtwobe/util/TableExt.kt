package net.octyl.ourtwobe.util

import com.google.common.collect.Table

operator fun <R, C, V> Table<R, C, V>.set(rowKey: R, columnKey: C, value: V): V? =
    put(rowKey, columnKey, value)
