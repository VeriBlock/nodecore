// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.core.params

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.Assert
import org.junit.Test
import org.veriblock.core.Context
import org.veriblock.core.crypto.Crypto
import org.veriblock.core.utilities.Utility
import org.veriblock.core.utilities.extensions.toHex
import java.security.Security

class RegTestProgPoWParametersTests {
    init {
        Context.set(defaultRegTestProgPoWParameters)
        Security.addProvider(BouncyCastleProvider())
    }

    @Test
    fun verifyGenesisBlock() {
        Assert.assertEquals(
            "00000000000200000000000000000000000000000000000000000000000000000000000080D7178046D25CA9AD283C5AF587A7C55F8C848A010100000000000000",
            defaultRegTestProgPoWParameters.genesisBlock.raw.toHex()
        )
        Assert.assertEquals(
            "A3E77A18A8F7B4568A062F69340D1AD4360382E5BC218A8C",
            defaultRegTestProgPoWParameters.genesisBlock.hash.toString()
        )
    }

    /* Compute the block hash by SHA256D on the header */
    @Test
    fun getInitialBitcoinBlockHeader(): Unit {
        val header = defaultRegTestProgPoWParameters.bitcoinOriginBlock.raw

        /* Compute the block hash by SHA256D on the header */
        val crypto = Crypto()
        val result = crypto.SHA256D(header)
        val hash = Utility.bytesToHex(Utility.flip(result))
        Assert.assertEquals("0F9188F13CB7B2C71F2A335E3A4FC328BF5BEB436012AFCA590B1A11466E2206", hash)
    }
}
