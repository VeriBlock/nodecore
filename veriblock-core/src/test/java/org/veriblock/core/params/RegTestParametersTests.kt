// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.core.params

import org.junit.Assert
import org.junit.Test
import org.veriblock.core.Context
import org.veriblock.core.crypto.Crypto
import org.veriblock.core.utilities.Utility
import org.veriblock.core.utilities.extensions.toHex

class RegTestParametersTests {
    init {
        Context.set(defaultRegTestParameters)
    }

    @Test
    fun verifyGenesisBlock() {
        Assert.assertEquals(
            "00000000000200000000000000000000000000000000000000000000000000000000000084E710F30BB8CFC9AF12622A8F39B9175F8C848A0101000000000000",
            defaultRegTestParameters.genesisBlock.raw.toHex()
        )
        Assert.assertEquals(
            "7C08C0014554E5DD602FAE9C4D3C02F4D512C8BF3893C977",
            defaultRegTestParameters.genesisBlock.hash.toString()
        )
    }

    /* Compute the block hash by SHA256D on the header */
    @Test
    fun getInitialBitcoinBlockHeader(): Unit {
        val header = defaultRegTestParameters.bitcoinOriginBlock.raw

        /* Compute the block hash by SHA256D on the header */
        val crypto = Crypto()
        val result = crypto.SHA256D(header)
        val hash = Utility.bytesToHex(Utility.flip(result))
        Assert.assertEquals("0F9188F13CB7B2C71F2A335E3A4FC328BF5BEB436012AFCA590B1A11466E2206", hash)
    }
}
