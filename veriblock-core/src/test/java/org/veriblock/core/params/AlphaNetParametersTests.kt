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

class AlphaNetParametersTests {
    init {
        Context.set(defaultAlphaNetParameters)
    }

    @Test
    fun verifyGenesisBlock() {
        defaultAlphaNetParameters.genesisBlock.raw.toHex() shouldBe "000000000001000000000000000000000000000000000000000000000000000000000000CF2025EC0EB8A8A325495FEB59500B505C797C0204009896030D04DF"
        defaultAlphaNetParameters.genesisBlock.hash.toString() shouldBe "1389189DE88DEF39FF18E1E0DEB679D6A9D85E37D4CC7360"
    }

    /* Compute the block hash by SHA256D on the header */
    @Test
    fun getInitialBitcoinBlockHeader(): Unit {
        val header = defaultAlphaNetParameters.bitcoinOriginBlock.raw

        /* Compute the block hash by SHA256D on the header */
        val crypto = Crypto()
        val result = crypto.SHA256D(header)
        val hash = Utility.bytesToHex(Utility.flip(result))
        hash.equals("000000000000000246200f09b513e517a3bd8c591a3b692d9852ddf1ee0f8b3a", ignoreCase = true) shouldBe true
    }
}
