@file:JvmName("StringUtils")
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.tr.checkpacking.utils

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract


@OptIn(ExperimentalContracts::class)
fun String?.isEmpty(): Boolean {
    contract {
        returns(false) implies (this@isEmpty != null)
    }
    return this.isNullOrBlank()
}

@OptIn(ExperimentalContracts::class)
fun String?.isNotEmpty(): Boolean {
    contract {
        returns(true) implies (this@isNotEmpty != null)
    }
    return !isEmpty()
}