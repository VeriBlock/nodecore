// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.model

import org.veriblock.core.bitcoinj.Base58

class StandardAddress(address: String) : AddressLight(address) {
    override val type: Byte
        get() = 0x01.toByte()

    override fun toByteArray(): ByteArray {
        return Base58.decode(get())
    }
}

fun String.asStandardAddress() = StandardAddress(this)
