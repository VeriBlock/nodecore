// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.model

import org.veriblock.core.bitcoinj.Base59

class MultisigAddress(address: String) : AddressLight(address) {
    override val type: Byte
        get() = 0x03.toByte()

    override fun toByteArray(): ByteArray {
        return Base59.decode(get())
    }
}
