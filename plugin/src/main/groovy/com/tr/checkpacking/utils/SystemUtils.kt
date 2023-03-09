@file:JvmName("SystemUtils")

package com.tr.checkpacking.utils

fun isWin(): Boolean {
    return System.getProperty("os.name").lowercase().startsWith("win")
}

fun getenv(name: String): String? {
    return System.getenv(name)
}