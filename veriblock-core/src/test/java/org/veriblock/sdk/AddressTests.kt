// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.sdk

import io.kotest.assertions.fail
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.veriblock.sdk.models.Address
import java.util.*

class AddressTests {
    @Test
    fun construct_WhenValidStandard() {
        val address = "VFFDWUMLJwLRuNzH4NX8Rm32E59n6d"
        val test = Address(address)
        test.isMultisig shouldBe false
        test.toString() shouldBe address
    }

    @Test
    fun construct_WhenValidMultisig() {
        val address = "V23Cuyc34u5rdk9psJ86aFcwhB1md0"
        val test = Address(address)
        test.isMultisig shouldBe true
        test.toString() shouldBe address
    }

    @Test
    fun isDerivedFromPublicKey_WhenItIs() {
        val publicKey = Base64.getDecoder()
            .decode("MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEy0J+QaARSHQICkseKreSDiLNLRiMhxQN76RH7l/ES7hI4cDbXvIG3i5wAvbIaVK+SCOkwI5l5M2+uQSouVdjqg==")
        val test = Address("VFFDWUMLJwLRuNzH4NX8Rm32E59n6d")
        test.isDerivedFromPublicKey(publicKey) shouldBe true
    }

    @Test
    fun isDerivedFromPublicKey_WhenItIsNot() {
        val publicKey = Base64.getDecoder()
            .decode("MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEy0J+QaARSHQICkseKreSDiLNLRiMhxQN76RH7l/ES7hI4cDbXvIG3i5wAvbIaVK+SCOkwI5l5M2+uQSouVdjqg==")
        val test = Address("V23Cuyc34u5rdk9psJ86aFcwhB1md0")
        test.isDerivedFromPublicKey(publicKey) shouldBe false
    }

    @Test
    fun construct_WhenInvalid() {
        val address = "VFFDWUMLJwLRuNzH4NX8Rm32E59n6dddd"
        try {
            Address(address)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            e.message!!.startsWith("The address") shouldBe true
        }
    }
}
