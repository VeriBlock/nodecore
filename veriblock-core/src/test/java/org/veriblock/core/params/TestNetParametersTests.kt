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

class TestNetParametersTests {
    init {
        Context.set(defaultTestNetParameters)
    }

    @Test
    fun verifyGenesisBlock() {
        "000000000002000000000000000000000000000000000000000000000000000000000000A2EA7C29EF7915DB412EBD4012A9C6175D9E35480405F5E100DA4579" shouldBe defaultTestNetParameters.genesisBlock.raw.toHex()
        "00000017EB579EC7D0CDD63379A0615DC3D68032CE248823" shouldBe defaultTestNetParameters.genesisBlock.hash.toString()
    }

    /* Compute the block hash by SHA256D on the header */
    @Test
    fun getInitialBitcoinBlockHeader() {
        val header = defaultTestNetParameters.bitcoinOriginBlock.raw

        /* Compute the block hash by SHA256D on the header */
        val crypto = Crypto()
        val result = crypto.SHA256D(header)
        val hash = Utility.bytesToHex(Utility.flip(result))
        hash shouldBe "000000007843C8E24359279C64BD4E4EF1B62D2A8C0557807C03124739566DD8"
    }
}
