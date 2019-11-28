// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk

import org.veriblock.sdk.util.Utils

fun ByteArray.toHex(): String = Utils.encodeHex(this)
fun String.asHexBytes(): ByteArray = Utils.decodeHex(this)

inline fun checkSuccess(block: () -> Any?): Boolean = try {
    block()
    true
} catch (ignored: Exception) {
    false
}
