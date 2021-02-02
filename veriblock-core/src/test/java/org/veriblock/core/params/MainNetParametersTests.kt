// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.core.params

import io.kotest.matchers.shouldBe
import org.junit.Test
import org.veriblock.core.Context
import org.veriblock.core.crypto.Crypto
import org.veriblock.core.utilities.Utility
import org.veriblock.core.utilities.extensions.toHex

class MainNetParametersTests {
    init {
        Context.set(defaultMainNetParameters)
    }

    @Test
    fun verifyGenesisBlock() {
        defaultMainNetParameters.genesisBlock.raw.toHex() shouldBe "000000000002000000000000000000000000000000000000000000000000000000000000A7E5F2B7EC94291767B4D67B4A33682D5C987E0B0600E8D4113D854D"
        defaultMainNetParameters.genesisBlock.hash.toString() shouldBe "0000000000F4FD66B91F0649BB3FCB137823C5CE317C105C"
    }

    /* Compute the block hash by SHA256D on the header */
    @Test
    fun getInitialBitcoinBlockHeader(): Unit {
        val header = defaultMainNetParameters.bitcoinOriginBlock.raw

        /* Compute the block hash by SHA256D on the header */
        val crypto = Crypto()
        val result = crypto.SHA256D(header)
        val hash = Utility.bytesToHex(Utility.flip(result))
        hash.equals("00000000000000000022d067f3481a353ed7f2943219d5e006721c5498b5c09d", ignoreCase = true) shouldBe true
    }
}
