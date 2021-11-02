// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.core.params

import io.kotest.matchers.shouldBe
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.Before
import org.junit.Test
import org.veriblock.core.Context
import org.veriblock.core.crypto.Crypto
import org.veriblock.core.utilities.Utility
import org.veriblock.core.utilities.extensions.toHex
import java.security.Security

class RegTestParametersTests {
    init {
        Security.addProvider(BouncyCastleProvider())
    }

    @Before
    fun setup() {
        Context.set(defaultRegTestParameters)
    }

    @Test
    fun verifyGenesisBlock() {
        defaultRegTestParameters.genesisBlock.raw.toHex() shouldBe "00000000000200000000000000000000000000000000000000000000000000000000000084E710F30BB8CFC9AF12622A8F39B9175F8C848A0101000000000000"
        defaultRegTestParameters.genesisBlock.hash.toString() shouldBe "7C08C0014554E5DD602FAE9C4D3C02F4D512C8BF3893C977"
    }

    @Test
    fun verifyProgPowGenesisBlock() {
        Context.set(defaultRegTestProgPowParameters)
        defaultRegTestProgPowParameters.genesisBlock.raw.toHex() shouldBe "00000000000200000000000000000000000000000000000000000000000000000000000080D7178046D25CA9AD283C5AF587A7C55F8C848A010100000000000000"
        defaultRegTestProgPowParameters.genesisBlock.hash.toString() shouldBe "A3E77A18A8F7B4568A062F69340D1AD4360382E5BC218A8C"
    }

    /* Compute the block hash by SHA256D on the header */
    @Test
    fun getInitialBitcoinBlockHeader(): Unit {
        val header = defaultRegTestParameters.bitcoinOriginBlock.raw

        /* Compute the block hash by SHA256D on the header */
        val crypto = Crypto()
        val result = crypto.SHA256D(header)
        val hash = Utility.bytesToHex(Utility.flip(result))
        hash shouldBe "0F9188F13CB7B2C71F2A335E3A4FC328BF5BEB436012AFCA590B1A11466E2206"
    }
}
