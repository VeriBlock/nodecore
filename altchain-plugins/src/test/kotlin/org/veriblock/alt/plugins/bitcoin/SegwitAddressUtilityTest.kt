// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.alt.plugins.bitcoin

import io.kotlintest.shouldBe
import org.junit.Test
import org.veriblock.core.utilities.extensions.toHex

/**
 * Integration tests for development purposes
 */
class SegwitAddressUtilityTest {

    @Test
    fun test() {
        // Given
        val address = "bcrt1qv6rz22y5xmrv2n2kxmfz7jmdu9adk4myf3ejsw"

        // When
        val payoutScript = SegwitAddressUtility.generatePayoutScriptFromSegwitAddress(address)

        // Then
        payoutScript.toHex() shouldBe "0014668625289436C6C54D5636D22F4B6DE17ADB5764"
    }

    @Test
    fun test2() {
        // Given
        val address = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4"

        // When
        val payoutScript = SegwitAddressUtility.generatePayoutScriptFromSegwitAddress(address)

        // Then
        payoutScript.toHex() shouldBe "0014751E76E8199196D454941C45D1B3A323F1433BD6"
    }
}
