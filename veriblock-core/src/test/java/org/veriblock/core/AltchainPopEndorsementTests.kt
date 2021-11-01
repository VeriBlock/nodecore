// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.core

import io.kotest.matchers.shouldBe
import org.junit.Test
import org.veriblock.core.altchain.AltchainPopEndorsement
import org.veriblock.core.utilities.Utility

class AltchainPopEndorsementTests {
    @Test
    fun testMinimalEndorsement_1() {
            val identifierLength = "01"
            val identifier = "00"
            val lengthOfLengthOfHeader = "01" // 1
            val lengthOfHeader = "01"
            val header = "FF"
            val noContextInfo = "00"
            val noPayoutInfo = "00"
            val popEndorsement = AltchainPopEndorsement(
                assembleHex(
                    identifierLength,
                    identifier,
                    lengthOfLengthOfHeader,
                    lengthOfHeader,
                    header,
                    noContextInfo,
                    noPayoutInfo
                )
            )
            Utility.bytesToHex(popEndorsement.getHeader()) shouldBe header
            popEndorsement.identifier shouldBe 0
            popEndorsement.getContextInfo().size.toLong() shouldBe 0
            popEndorsement.getPayoutInfo().size.toLong() shouldBe 0
    }

    @Test
    fun testMinimalEndorsement_2() {
            val identifierLength = "01"
            val identifier = "00"
            val lengthOfLengthOfHeader = "01"
            val lengthOfHeader = "04" // = 4
            val header = "AABBCCDD"
            val noContextInfo = "00"
            val noPayoutInfo = "00"
            val popEndorsement = AltchainPopEndorsement(
                assembleHex(
                    identifierLength,
                    identifier,
                    lengthOfLengthOfHeader,
                    lengthOfHeader,
                    header,
                    noContextInfo,
                    noPayoutInfo
                )
            )
            Utility.bytesToHex(popEndorsement.getHeader()) shouldBe header
            popEndorsement.identifier shouldBe 0
            popEndorsement.getContextInfo().size.toLong() shouldBe 0
            popEndorsement.getPayoutInfo().size.toLong() shouldBe 0
    }

    @Test
    fun testMinimalEndorsement_3() {
            val identifierLength = "01"
            val identifier = "00"
            val lengthOfLengthOfHeader = "01"
            val lengthOfHeader = "10" // = 16
            val header = "00112233445566778899AABBCCDDEEFF"
            val noContextInfo = "00"
            val noPayoutInfo = "00"
            val popEndorsement = AltchainPopEndorsement(
                assembleHex(
                    identifierLength,
                    identifier,
                    lengthOfLengthOfHeader,
                    lengthOfHeader,
                    header,
                    noContextInfo,
                    noPayoutInfo
                )
            )
            Utility.bytesToHex(popEndorsement.getHeader()) shouldBe header
            popEndorsement.identifier shouldBe 0
            popEndorsement.getContextInfo().size.toLong() shouldBe 0
            popEndorsement.getPayoutInfo().size.toLong() shouldBe 0
    }

    @Test
    fun testMinimalEndorsement_4() {
            val identifierLength = "01"
            val identifier = "00"
            val lengthOfLengthOfHeader = "02"
            val lengthOfHeader = "0100" // = 256
            val headerSB = StringBuilder()
            for (i in 0..255) {
                headerSB.append("AB")
            }
            val header = headerSB.toString()
            val noContextInfo = "00"
            val noPayoutInfo = "00"
            val popEndorsement = AltchainPopEndorsement(
                assembleHex(
                    identifierLength,
                    identifier,
                    lengthOfLengthOfHeader,
                    lengthOfHeader,
                    header,
                    noContextInfo,
                    noPayoutInfo
                )
            )
            Utility.bytesToHex(popEndorsement.getHeader()) shouldBe header
            popEndorsement.identifier shouldBe 0
            popEndorsement.getContextInfo().size.toLong() shouldBe 0
            popEndorsement.getPayoutInfo().size.toLong() shouldBe 0
    }

    @Test
    fun testMinimalEndorsement_5() {
            val identifierLength = "01"
            val identifier = "00"
            val lengthOfLengthOfHeader = "01"
            val lengthOfHeader = "FF" // = 255
            val headerSB = StringBuilder()
            for (i in 0..254) {
                headerSB.append("AB")
            }
            val header = headerSB.toString()
            val noContextInfo = "00"
            val noPayoutInfo = "00"
            val popEndorsement = AltchainPopEndorsement(
                assembleHex(
                    identifierLength,
                    identifier,
                    lengthOfLengthOfHeader,
                    lengthOfHeader,
                    header,
                    noContextInfo,
                    noPayoutInfo
                )
            )
            Utility.bytesToHex(popEndorsement.getHeader()) shouldBe header
            popEndorsement.identifier shouldBe 0
            popEndorsement.getContextInfo().size.toLong() shouldBe 0
            popEndorsement.getPayoutInfo().size.toLong() shouldBe 0
    }

    @Test
    fun testMinimalEndorsement_6() {
            val identifierLength = "01"
            val identifier = "00"
            val lengthOfLengthOfHeader = "02"
            val lengthOfHeader = "F000" // = 61440
            val headerSB = StringBuilder()
            for (i in 0..61439) {
                headerSB.append("AB")
            }
            val header = headerSB.toString()
            val noContextInfo = "00"
            val noPayoutInfo = "00"
            val popEndorsement = AltchainPopEndorsement(
                assembleHex(
                    identifierLength,
                    identifier,
                    lengthOfLengthOfHeader,
                    lengthOfHeader,
                    header,
                    noContextInfo,
                    noPayoutInfo
                )
            )
            Utility.bytesToHex(popEndorsement.getHeader()) shouldBe header
            popEndorsement.identifier shouldBe 0
            popEndorsement.getContextInfo().size.toLong() shouldBe 0
            popEndorsement.getPayoutInfo().size.toLong() shouldBe 0
    }

    @Test
    fun testMinimalEndorsement_7() {
            val identifierLength = "02"
            val identifier = "0000"
            val lengthOfLengthOfHeader = "01"
            val lengthOfHeader = "06" // = 6
            val header = "ABCDEF987654"
            val noContextInfo = "00"
            val noPayoutInfo = "00"
            val popEndorsement = AltchainPopEndorsement(
                assembleHex(
                    identifierLength,
                    identifier,
                    lengthOfLengthOfHeader,
                    lengthOfHeader,
                    header,
                    noContextInfo,
                    noPayoutInfo
                )
            )
            Utility.bytesToHex(popEndorsement.getHeader()) shouldBe header
            popEndorsement.identifier shouldBe 0
            popEndorsement.getContextInfo().size.toLong() shouldBe 0
            popEndorsement.getPayoutInfo().size.toLong() shouldBe 0
    }

    @Test
    fun testMinimalEndorsement_8() {
            val identifierLength = "07"
            val identifier = "00000000000001"
            val lengthOfLengthOfHeader = "02"
            val lengthOfHeader = "0006" // = 6
            val header = "ABCDEF987654"
            val noContextInfo = "00"
            val noPayoutInfo = "00"
            val popEndorsement = AltchainPopEndorsement(
                assembleHex(
                    identifierLength,
                    identifier,
                    lengthOfLengthOfHeader,
                    lengthOfHeader,
                    header,
                    noContextInfo,
                    noPayoutInfo
                )
            )
            Utility.bytesToHex(popEndorsement.getHeader()) shouldBe header
            popEndorsement.identifier shouldBe 1
            popEndorsement.getContextInfo().size.toLong() shouldBe 0
            popEndorsement.getPayoutInfo().size.toLong() shouldBe 0
    }

    @Test
    fun testMinimalEndorsement_9() {
            val identifierLength = "08"
            val identifier = "3F00000000000000"
            val lengthOfLengthOfHeader = "02"
            val lengthOfHeader = "0006" // = 6
            val header = "ABCDEF987654"
            val noContextInfo = "00"
            val noPayoutInfo = "00"
            val popEndorsement = AltchainPopEndorsement(
                assembleHex(
                    identifierLength,
                    identifier,
                    lengthOfLengthOfHeader,
                    lengthOfHeader,
                    header,
                    noContextInfo,
                    noPayoutInfo
                )
            )
            Utility.bytesToHex(popEndorsement.getHeader()) shouldBe header
            popEndorsement.identifier shouldBe 4539628424389459968L
            popEndorsement.getContextInfo().size.toLong() shouldBe 0
            popEndorsement.getPayoutInfo().size.toLong() shouldBe 0
    }

    @Test
    fun testEndorsementWithContext_1() {
            val identifierLength = "04"
            val identifier = "FFFFFFFF"
            val lengthOfLengthOfHeader = "01"
            val lengthOfHeader = "10" // = 16
            val header = "00112233445566778899AABBCCDDEEFF"
            val lengthOfLengthOfContextInfo = "01"
            val lengthOfContextInfo = "01" // = 1
            val contextInfo = "FF"
            val noPayoutInfo = "00"
            val popEndorsement = AltchainPopEndorsement(
                assembleHex(
                    identifierLength,
                    identifier,
                    lengthOfLengthOfHeader,
                    lengthOfHeader,
                    header,
                    lengthOfLengthOfContextInfo,
                    lengthOfContextInfo,
                    contextInfo,
                    noPayoutInfo
                )
            )
            Utility.bytesToHex(popEndorsement.getHeader()) shouldBe header
            popEndorsement.identifier shouldBe 4294967295L
            Utility.byteArraysAreEqual(
                popEndorsement.getContextInfo(),
                Utility.hexToBytes(contextInfo)
            ) shouldBe true
            popEndorsement.getPayoutInfo().size.toLong() shouldBe 0
    }

    @Test
    fun testEndorsementWithContext_2() {
            val identifierLength = "04"
            val identifier = "FFFFFFFF"
            val lengthOfLengthOfHeader = "01"
            val lengthOfHeader = "10" // = 16
            val header = "00112233445566778899AABBCCDDEEFF"
            val lengthOfLengthOfContextInfo = "01"
            val lengthOfContextInfo = "0F" // = 15
            val contextInfo = "00112233445566778899AABBCCDDEE"
            val noPayoutInfo = "00"
            val popEndorsement = AltchainPopEndorsement(
                assembleHex(
                    identifierLength,
                    identifier,
                    lengthOfLengthOfHeader,
                    lengthOfHeader,
                    header,
                    lengthOfLengthOfContextInfo,
                    lengthOfContextInfo,
                    contextInfo,
                    noPayoutInfo
                )
            )
            Utility.bytesToHex(popEndorsement.getHeader()) shouldBe header
            popEndorsement.identifier shouldBe 4294967295L
            Utility.byteArraysAreEqual(
                popEndorsement.getContextInfo(),
                Utility.hexToBytes(contextInfo)
            ) shouldBe true
            popEndorsement.getPayoutInfo().size.toLong() shouldBe 0
    }

    @Test
    fun testEndorsementWithContext_3() {
            val identifierLength = "04"
            val identifier = "FFFFFFFF"
            val lengthOfLengthOfHeader = "01"
            val lengthOfHeader = "10" // = 16
            val header = "00112233445566778899AABBCCDDEEFF"
            val lengthOfLengthOfContextInfo = "01"
            val lengthOfContextInfo = "10" // = 15
            val contextInfo = "00112233445566778899AABBCCDDEEFF"
            val noPayoutInfo = "00"
            val popEndorsement = AltchainPopEndorsement(
                assembleHex(
                    identifierLength,
                    identifier,
                    lengthOfLengthOfHeader,
                    lengthOfHeader,
                    header,
                    lengthOfLengthOfContextInfo,
                    lengthOfContextInfo,
                    contextInfo,
                    noPayoutInfo
                )
            )
            Utility.bytesToHex(popEndorsement.getHeader()) shouldBe header
            popEndorsement.identifier shouldBe 4294967295L
            Utility.byteArraysAreEqual(
                popEndorsement.getContextInfo(),
                Utility.hexToBytes(contextInfo)
            ) shouldBe true
            popEndorsement.getPayoutInfo().size.toLong() shouldBe 0
    }

    @Test
    fun testEndorsementWithContext_4() {
            val identifierLength = "04"
            val identifier = "FFFFFFFF"
            val lengthOfLengthOfHeader = "01"
            val lengthOfHeader = "10" // = 16
            val header = "00112233445566778899AABBCCDDEEFF"
            val lengthOfLengthOfContextInfo = "01"
            val lengthOfContextInfo = "FF" // = 255
            val contextSB = StringBuilder()
            for (i in 0..254) {
                contextSB.append("AB")
            }
            val contextInfo = contextSB.toString()
            val noPayoutInfo = "00"
            val popEndorsement = AltchainPopEndorsement(
                assembleHex(
                    identifierLength,
                    identifier,
                    lengthOfLengthOfHeader,
                    lengthOfHeader,
                    header,
                    lengthOfLengthOfContextInfo,
                    lengthOfContextInfo,
                    contextInfo,
                    noPayoutInfo
                )
            )
            Utility.bytesToHex(popEndorsement.getHeader()) shouldBe header
            popEndorsement.identifier shouldBe 4294967295L
            Utility.byteArraysAreEqual(
                popEndorsement.getContextInfo(),
                Utility.hexToBytes(contextInfo)
            ) shouldBe true
            popEndorsement.getPayoutInfo().size.toLong() shouldBe 0
    }

    @Test
    fun testEndorsementWithContext_5() {
            val identifierLength = "04"
            val identifier = "FFFFFFFF"
            val lengthOfLengthOfHeader = "01"
            val lengthOfHeader = "10" // = 16
            val header = "00112233445566778899AABBCCDDEEFF"
            val lengthOfLengthOfContextInfo = "02"
            val lengthOfContextInfo = "0100" // = 256
            val contextSB = StringBuilder()
            for (i in 0..255) {
                contextSB.append("AB")
            }
            val contextInfo = contextSB.toString()
            val noPayoutInfo = "00"
            val popEndorsement = AltchainPopEndorsement(
                assembleHex(
                    identifierLength,
                    identifier,
                    lengthOfLengthOfHeader,
                    lengthOfHeader,
                    header,
                    lengthOfLengthOfContextInfo,
                    lengthOfContextInfo,
                    contextInfo,
                    noPayoutInfo
                )
            )
            Utility.bytesToHex(popEndorsement.getHeader()) shouldBe header
            popEndorsement.identifier shouldBe 4294967295L
            Utility.byteArraysAreEqual(
                popEndorsement.getContextInfo(),
                Utility.hexToBytes(contextInfo)
            ) shouldBe true
            popEndorsement.getPayoutInfo().size.toLong() shouldBe 0
    }

    @Test
    fun testEndorsementWithContext_6() {
            val identifierLength = "04"
            val identifier = "FFFFFFFF"
            val lengthOfLengthOfHeader = "01"
            val lengthOfHeader = "10" // = 16
            val header = "00112233445566778899AABBCCDDEEFF"
            val lengthOfLengthOfContextInfo = "02"
            val lengthOfContextInfo = "1000" // = 4096
            val contextSB = StringBuilder()
            for (i in 0..4095) {
                contextSB.append("AB")
            }
            val contextInfo = contextSB.toString()
            val noPayoutInfo = "00"
            val popEndorsement = AltchainPopEndorsement(
                assembleHex(
                    identifierLength,
                    identifier,
                    lengthOfLengthOfHeader,
                    lengthOfHeader,
                    header,
                    lengthOfLengthOfContextInfo,
                    lengthOfContextInfo,
                    contextInfo,
                    noPayoutInfo
                )
            )
            Utility.bytesToHex(popEndorsement.getHeader()) shouldBe header
            popEndorsement.identifier shouldBe 4294967295L
            Utility.byteArraysAreEqual(
                popEndorsement.getContextInfo(),
                Utility.hexToBytes(contextInfo)
            ) shouldBe true
            popEndorsement.getPayoutInfo().size.toLong() shouldBe 0
    }

    @Test
    fun testEndorsementWithContext_7() {
            val identifierLength = "04"
            val identifier = "FFFFFFFF"
            val lengthOfLengthOfHeader = "01"
            val lengthOfHeader = "10" // = 16
            val header = "00112233445566778899AABBCCDDEEFF"
            val lengthOfLengthOfContextInfo = "02"
            val lengthOfContextInfo = "0001" // = 1
            val contextInfo = "AB"
            val noPayoutInfo = "00"
            val popEndorsement = AltchainPopEndorsement(
                assembleHex(
                    identifierLength,
                    identifier,
                    lengthOfLengthOfHeader,
                    lengthOfHeader,
                    header,
                    lengthOfLengthOfContextInfo,
                    lengthOfContextInfo,
                    contextInfo,
                    noPayoutInfo
                )
            )
            Utility.bytesToHex(popEndorsement.getHeader()) shouldBe header
            popEndorsement.identifier shouldBe 4294967295L
            Utility.byteArraysAreEqual(
                popEndorsement.getContextInfo(),
                Utility.hexToBytes(contextInfo)
            ) shouldBe true
            popEndorsement.getPayoutInfo().size.toLong() shouldBe 0
    }

    @Test
    fun testEndorsementWithPayout_1() {
            val identifierLength = "04"
            val identifier = "FFFFFFFF"
            val lengthOfLengthOfHeader = "01"
            val lengthOfHeader = "10" // = 16
            val header = "00112233445566778899AABBCCDDEEFF"
            val noContext = "00"
            val lengthOfLengthOfPayoutInfo = "01"
            val lengthOfPayoutInfo = "01" // = 1
            val payoutInfo = "00"
            val popEndorsement = AltchainPopEndorsement(
                assembleHex(
                    identifierLength,
                    identifier,
                    lengthOfLengthOfHeader,
                    lengthOfHeader,
                    header,
                    noContext,
                    lengthOfLengthOfPayoutInfo,
                    lengthOfPayoutInfo,
                    payoutInfo
                )
            )
            Utility.bytesToHex(popEndorsement.getHeader()) shouldBe header
            popEndorsement.identifier shouldBe 4294967295L
            popEndorsement.getContextInfo().size.toLong() shouldBe 0
            Utility.byteArraysAreEqual(
                popEndorsement.getPayoutInfo(),
                Utility.hexToBytes(payoutInfo)
            ) shouldBe true
    }

    @Test
    fun testEndorsementWithPayout_2() {
            val identifierLength = "04"
            val identifier = "FFFFFFFF"
            val lengthOfLengthOfHeader = "01"
            val lengthOfHeader = "10" // = 16
            val header = "00112233445566778899AABBCCDDEEFF"
            val noContext = "00"
            val lengthOfLengthOfPayoutInfo = "01"
            val lengthOfPayoutInfo = "08" // = 8
            val payoutInfo = "0011223344556677"
            val popEndorsement = AltchainPopEndorsement(
                assembleHex(
                    identifierLength,
                    identifier,
                    lengthOfLengthOfHeader,
                    lengthOfHeader,
                    header,
                    noContext,
                    lengthOfLengthOfPayoutInfo,
                    lengthOfPayoutInfo,
                    payoutInfo
                )
            )
            Utility.bytesToHex(popEndorsement.getHeader()) shouldBe header
            popEndorsement.identifier shouldBe 4294967295L
            popEndorsement.getContextInfo().size.toLong() shouldBe 0
            Utility.byteArraysAreEqual(
                popEndorsement.getPayoutInfo(),
                Utility.hexToBytes(payoutInfo)
            ) shouldBe true
    }

    @Test
    fun testEndorsementWithPayout_3() {
            val identifierLength = "04"
            val identifier = "FFFFFFFF"
            val lengthOfLengthOfHeader = "01"
            val lengthOfHeader = "10" // = 16
            val header = "00112233445566778899AABBCCDDEEFF"
            val noContext = "00"
            val lengthOfLengthOfPayoutInfo = "01"
            val lengthOfPayoutInfo = "FF" // = 255
            val payoutSB = StringBuilder()
            for (i in 0..254) {
                payoutSB.append("AB")
            }
            val payoutInfo = payoutSB.toString()
            val popEndorsement = AltchainPopEndorsement(
                assembleHex(
                    identifierLength,
                    identifier,
                    lengthOfLengthOfHeader,
                    lengthOfHeader,
                    header,
                    noContext,
                    lengthOfLengthOfPayoutInfo,
                    lengthOfPayoutInfo,
                    payoutInfo
                )
            )
            Utility.bytesToHex(popEndorsement.getHeader()) shouldBe header
            popEndorsement.identifier shouldBe 4294967295L
            popEndorsement.getContextInfo().size.toLong() shouldBe 0
            Utility.byteArraysAreEqual(
                popEndorsement.getPayoutInfo(),
                Utility.hexToBytes(payoutInfo)
            ) shouldBe true
    }

    @Test
    fun testEndorsementWithPayout_4() {
            val identifierLength = "04"
            val identifier = "FFFFFFFF"
            val lengthOfLengthOfHeader = "01"
            val lengthOfHeader = "10" // = 16
            val header = "00112233445566778899AABBCCDDEEFF"
            val noContext = "00"
            val lengthOfLengthOfPayoutInfo = "02"
            val lengthOfPayoutInfo = "0100" // = 256
            val payoutSB = StringBuilder()
            for (i in 0..255) {
                payoutSB.append("AB")
            }
            val payoutInfo = payoutSB.toString()
            val popEndorsement = AltchainPopEndorsement(
                assembleHex(
                    identifierLength,
                    identifier,
                    lengthOfLengthOfHeader,
                    lengthOfHeader,
                    header,
                    noContext,
                    lengthOfLengthOfPayoutInfo,
                    lengthOfPayoutInfo,
                    payoutInfo
                )
            )
            Utility.bytesToHex(popEndorsement.getHeader()) shouldBe header
            popEndorsement.identifier shouldBe 4294967295L
            popEndorsement.getContextInfo().size.toLong() shouldBe 0
            Utility.byteArraysAreEqual(
                popEndorsement.getPayoutInfo(),
                Utility.hexToBytes(payoutInfo)
            ) shouldBe true
    }

    @Test
    fun testEndorsementWithPayout_5() {
            val identifierLength = "04"
            val identifier = "FFFFFFFF"
            val lengthOfLengthOfHeader = "01"
            val lengthOfHeader = "10" // = 16
            val header = "00112233445566778899AABBCCDDEEFF"
            val noContext = "00"
            val lengthOfLengthOfPayoutInfo = "02"
            val lengthOfPayoutInfo = "0FFF" // = 4095
            val payoutSB = StringBuilder()
            for (i in 0..4094) {
                payoutSB.append("AB")
            }
            val payoutInfo = payoutSB.toString()
            val popEndorsement = AltchainPopEndorsement(
                assembleHex(
                    identifierLength,
                    identifier,
                    lengthOfLengthOfHeader,
                    lengthOfHeader,
                    header,
                    noContext,
                    lengthOfLengthOfPayoutInfo,
                    lengthOfPayoutInfo,
                    payoutInfo
                )
            )
            Utility.bytesToHex(popEndorsement.getHeader()) shouldBe header
            popEndorsement.identifier shouldBe 4294967295L
            popEndorsement.getContextInfo().size.toLong() shouldBe 0
            Utility.byteArraysAreEqual(
                popEndorsement.getPayoutInfo(),
                Utility.hexToBytes(payoutInfo)
            ) shouldBe true
    }

    @Test
    fun testEndorsementWithPayout_6() {
            val identifierLength = "04"
            val identifier = "FFFFFFFF"
            val lengthOfLengthOfHeader = "01"
            val lengthOfHeader = "10" // = 16
            val header = "00112233445566778899AABBCCDDEEFF"
            val noContext = "00"
            val lengthOfLengthOfPayoutInfo = "02"
            val lengthOfPayoutInfo = "1000" // = 4096
            val payoutSB = StringBuilder()
            for (i in 0..4095) {
                payoutSB.append("AB")
            }
            val payoutInfo = payoutSB.toString()
            val popEndorsement = AltchainPopEndorsement(
                assembleHex(
                    identifierLength,
                    identifier,
                    lengthOfLengthOfHeader,
                    lengthOfHeader,
                    header,
                    noContext,
                    lengthOfLengthOfPayoutInfo,
                    lengthOfPayoutInfo,
                    payoutInfo
                )
            )
            Utility.bytesToHex(popEndorsement.getHeader()) shouldBe header
            popEndorsement.identifier shouldBe 4294967295L
            popEndorsement.getContextInfo().size.toLong() shouldBe 0
            Utility.byteArraysAreEqual(
                popEndorsement.getPayoutInfo(),
                Utility.hexToBytes(payoutInfo)
            ) shouldBe true
    }

    @Test
    fun testEndorsementWithPayout_7() {
            val identifierLength = "04"
            val identifier = "FFFFFFFF"
            val lengthOfLengthOfHeader = "01"
            val lengthOfHeader = "10" // = 16
            val header = "00112233445566778899AABBCCDDEEFF"
            val noContext = "00"
            val lengthOfLengthOfPayoutInfo = "02"
            val lengthOfPayoutInfo = "0001" // = 1
            val payoutInfo = "FF"
            val popEndorsement = AltchainPopEndorsement(
                assembleHex(
                    identifierLength,
                    identifier,
                    lengthOfLengthOfHeader,
                    lengthOfHeader,
                    header,
                    noContext,
                    lengthOfLengthOfPayoutInfo,
                    lengthOfPayoutInfo,
                    payoutInfo
                )
            )
            Utility.bytesToHex(popEndorsement.getHeader()) shouldBe header
            popEndorsement.identifier shouldBe 4294967295L
            popEndorsement.getContextInfo().size.toLong() shouldBe 0
            Utility.byteArraysAreEqual(
                popEndorsement.getPayoutInfo(),
                Utility.hexToBytes(payoutInfo)
            ) shouldBe true
    }

    @Test
    fun testEndorsementWithContextAndPayout_1() {
            val identifierLength = "04"
            val identifier = "FFFFFFFF"
            val lengthOfLengthOfHeader = "01"
            val lengthOfHeader = "10" // = 16
            val header = "00112233445566778899AABBCCDDEEFF"
            val lengthOfLengthOfContext = "01"
            val lengthOfContext = "01" // = 1
            val contextInfo = "00"
            val lengthOfLengthOfPayoutInfo = "01"
            val lengthOfPayoutInfo = "01" // = 1
            val payoutInfo = "FF"
            val popEndorsement = AltchainPopEndorsement(
                assembleHex(
                    identifierLength,
                    identifier,
                    lengthOfLengthOfHeader,
                    lengthOfHeader,
                    header,
                    lengthOfLengthOfContext,
                    lengthOfContext,
                    contextInfo,
                    lengthOfLengthOfPayoutInfo,
                    lengthOfPayoutInfo,
                    payoutInfo
                )
            )
            Utility.bytesToHex(popEndorsement.getHeader()) shouldBe header
            popEndorsement.identifier shouldBe 4294967295L
            Utility.byteArraysAreEqual(
                popEndorsement.getContextInfo(),
                Utility.hexToBytes(contextInfo)
            ) shouldBe true
            Utility.byteArraysAreEqual(
                popEndorsement.getPayoutInfo(),
                Utility.hexToBytes(payoutInfo)
            ) shouldBe true
    }

    @Test
    fun testEndorsementWithContextAndPayout_2() {
            val identifierLength = "04"
            val identifier = "FFFFFFFF"
            val lengthOfLengthOfHeader = "01"
            val lengthOfHeader = "10" // = 16
            val header = "00112233445566778899AABBCCDDEEFF"
            val lengthOfLengthOfContext = "02"
            val lengthOfContext = "0001" // = 1
            val contextInfo = "00"
            val lengthOfLengthOfPayoutInfo = "02"
            val lengthOfPayoutInfo = "0001" // = 1
            val payoutInfo = "FF"
            val popEndorsement = AltchainPopEndorsement(
                assembleHex(
                    identifierLength,
                    identifier,
                    lengthOfLengthOfHeader,
                    lengthOfHeader,
                    header,
                    lengthOfLengthOfContext,
                    lengthOfContext,
                    contextInfo,
                    lengthOfLengthOfPayoutInfo,
                    lengthOfPayoutInfo,
                    payoutInfo
                )
            )
            Utility.bytesToHex(popEndorsement.getHeader()) shouldBe header
            popEndorsement.identifier shouldBe 4294967295L
            Utility.byteArraysAreEqual(
                popEndorsement.getContextInfo(),
                Utility.hexToBytes(contextInfo)
            ) shouldBe true
            Utility.byteArraysAreEqual(
                popEndorsement.getPayoutInfo(),
                Utility.hexToBytes(payoutInfo)
            ) shouldBe true
    }

    @Test
    fun testEndorsementWithContextAndPayout_3() {
            val identifierLength = "04"
            val identifier = "FFFFFFFF"
            val lengthOfLengthOfHeader = "01"
            val lengthOfHeader = "10" // = 16
            val header = "00112233445566778899AABBCCDDEEFF"
            val lengthOfLengthOfContext = "01"
            val lengthOfContext = "FF" // = 255
            val contextSB = StringBuilder()
            for (i in 0..254) {
                contextSB.append("AB")
            }
            val contextInfo = contextSB.toString()
            val lengthOfLengthOfPayoutInfo = "01"
            val lengthOfPayoutInfo = "01" // = 1
            val payoutSB = StringBuilder()
            for (i in 0..0) {
                payoutSB.append("CD")
            }
            val payoutInfo = payoutSB.toString()
            val popEndorsement = AltchainPopEndorsement(
                assembleHex(
                    identifierLength,
                    identifier,
                    lengthOfLengthOfHeader,
                    lengthOfHeader,
                    header,
                    lengthOfLengthOfContext,
                    lengthOfContext,
                    contextInfo,
                    lengthOfLengthOfPayoutInfo,
                    lengthOfPayoutInfo,
                    payoutInfo
                )
            )
            Utility.bytesToHex(popEndorsement.getHeader()) shouldBe header
            popEndorsement.identifier shouldBe 4294967295L
            Utility.byteArraysAreEqual(
                popEndorsement.getContextInfo(),
                Utility.hexToBytes(contextInfo)
            ) shouldBe true
            Utility.byteArraysAreEqual(
                popEndorsement.getPayoutInfo(),
                Utility.hexToBytes(payoutInfo)
            ) shouldBe true
    }

    @Test
    fun testEndorsementWithContextAndPayout_4() {
            val identifierLength = "04"
            val identifier = "FFFFFFFF"
            val lengthOfLengthOfHeader = "01"
            val lengthOfHeader = "10" // = 16
            val header = "00112233445566778899AABBCCDDEEFF"
            val lengthOfLengthOfContext = "02"
            val lengthOfContext = "0100" // = 256
            val contextSB = StringBuilder()
            for (i in 0..255) {
                contextSB.append("AB")
            }
            val contextInfo = contextSB.toString()
            val lengthOfLengthOfPayoutInfo = "01"
            val lengthOfPayoutInfo = "01" // = 1
            val payoutSB = StringBuilder()
            for (i in 0..0) {
                payoutSB.append("CD")
            }
            val payoutInfo = payoutSB.toString()
            val popEndorsement = AltchainPopEndorsement(
                assembleHex(
                    identifierLength,
                    identifier,
                    lengthOfLengthOfHeader,
                    lengthOfHeader,
                    header,
                    lengthOfLengthOfContext,
                    lengthOfContext,
                    contextInfo,
                    lengthOfLengthOfPayoutInfo,
                    lengthOfPayoutInfo,
                    payoutInfo
                )
            )
            Utility.bytesToHex(popEndorsement.getHeader()) shouldBe header
            popEndorsement.identifier shouldBe 4294967295L
            Utility.byteArraysAreEqual(
                popEndorsement.getContextInfo(),
                Utility.hexToBytes(contextInfo)
            ) shouldBe true
            Utility.byteArraysAreEqual(
                popEndorsement.getPayoutInfo(),
                Utility.hexToBytes(payoutInfo)
            ) shouldBe true
    }

    @Test
    fun testEndorsementWithContextAndPayout_5() {
            val identifierLength = "04"
            val identifier = "FFFFFFFF"
            val lengthOfLengthOfHeader = "01"
            val lengthOfHeader = "10" // = 16
            val header = "00112233445566778899AABBCCDDEEFF"
            val lengthOfLengthOfContext = "02"
            val lengthOfContext = "1000" // = 4096
            val contextSB = StringBuilder()
            for (i in 0..4095) {
                contextSB.append("AB")
            }
            val contextInfo = contextSB.toString()
            val lengthOfLengthOfPayoutInfo = "01"
            val lengthOfPayoutInfo = "01" // = 1
            val payoutSB = StringBuilder()
            for (i in 0..0) {
                payoutSB.append("CD")
            }
            val payoutInfo = payoutSB.toString()
            val popEndorsement = AltchainPopEndorsement(
                assembleHex(
                    identifierLength,
                    identifier,
                    lengthOfLengthOfHeader,
                    lengthOfHeader,
                    header,
                    lengthOfLengthOfContext,
                    lengthOfContext,
                    contextInfo,
                    lengthOfLengthOfPayoutInfo,
                    lengthOfPayoutInfo,
                    payoutInfo
                )
            )
            Utility.bytesToHex(popEndorsement.getHeader()) shouldBe header
            popEndorsement.identifier shouldBe 4294967295L
            Utility.byteArraysAreEqual(
                popEndorsement.getContextInfo(),
                Utility.hexToBytes(contextInfo)
            ) shouldBe true
            Utility.byteArraysAreEqual(
                popEndorsement.getPayoutInfo(),
                Utility.hexToBytes(payoutInfo)
            ) shouldBe true
    }

    @Test
    fun testEndorsementWithContextAndPayout_6() {
            val identifierLength = "04"
            val identifier = "FFFFFFFF"
            val lengthOfLengthOfHeader = "01"
            val lengthOfHeader = "10" // = 16
            val header = "00112233445566778899AABBCCDDEEFF"
            val lengthOfLengthOfContext = "02"
            val lengthOfContext = "0100" // = 256
            val contextSB = StringBuilder()
            for (i in 0..255) {
                contextSB.append("AB")
            }
            val contextInfo = contextSB.toString()
            val lengthOfLengthOfPayoutInfo = "02"
            val lengthOfPayoutInfo = "0001" // = 1
            val payoutSB = StringBuilder()
            for (i in 0..0) {
                payoutSB.append("CD")
            }
            val payoutInfo = payoutSB.toString()
            val popEndorsement = AltchainPopEndorsement(
                assembleHex(
                    identifierLength,
                    identifier,
                    lengthOfLengthOfHeader,
                    lengthOfHeader,
                    header,
                    lengthOfLengthOfContext,
                    lengthOfContext,
                    contextInfo,
                    lengthOfLengthOfPayoutInfo,
                    lengthOfPayoutInfo,
                    payoutInfo
                )
            )
            Utility.bytesToHex(popEndorsement.getHeader()) shouldBe header
            popEndorsement.identifier shouldBe 4294967295L
            Utility.byteArraysAreEqual(
                popEndorsement.getContextInfo(),
                Utility.hexToBytes(contextInfo)
            ) shouldBe true
            Utility.byteArraysAreEqual(
                popEndorsement.getPayoutInfo(),
                Utility.hexToBytes(payoutInfo)
            ) shouldBe true
    }

    @Test
    fun testEndorsementWithContextAndPayout_7() {
            val identifierLength = "04"
            val identifier = "FFFFFFFF"
            val lengthOfLengthOfHeader = "01"
            val lengthOfHeader = "10" // = 16
            val header = "00112233445566778899AABBCCDDEEFF"
            val lengthOfLengthOfContext = "02"
            val lengthOfContext = "0100" // = 256
            val contextSB = StringBuilder()
            for (i in 0..255) {
                contextSB.append("AB")
            }
            val contextInfo = contextSB.toString()
            val lengthOfLengthOfPayoutInfo = "01"
            val lengthOfPayoutInfo = "FF" // = 255
            val payoutSB = StringBuilder()
            for (i in 0..254) {
                payoutSB.append("CD")
            }
            val payoutInfo = payoutSB.toString()
            val popEndorsement = AltchainPopEndorsement(
                assembleHex(
                    identifierLength,
                    identifier,
                    lengthOfLengthOfHeader,
                    lengthOfHeader,
                    header,
                    lengthOfLengthOfContext,
                    lengthOfContext,
                    contextInfo,
                    lengthOfLengthOfPayoutInfo,
                    lengthOfPayoutInfo,
                    payoutInfo
                )
            )
            Utility.bytesToHex(popEndorsement.getHeader()) shouldBe header
            popEndorsement.identifier shouldBe 4294967295L
            Utility.byteArraysAreEqual(
                popEndorsement.getContextInfo(),
                Utility.hexToBytes(contextInfo)
            ) shouldBe true
            Utility.byteArraysAreEqual(
                popEndorsement.getPayoutInfo(),
                Utility.hexToBytes(payoutInfo)
            ) shouldBe true
    }

    @Test
    fun testEndorsementWithContextAndPayout_8() {
            val identifierLength = "04"
            val identifier = "FFFFFFFF"
            val lengthOfLengthOfHeader = "01"
            val lengthOfHeader = "10" // = 16
            val header = "00112233445566778899AABBCCDDEEFF"
            val lengthOfLengthOfContext = "02"
            val lengthOfContext = "1000" // = 4096
            val contextSB = StringBuilder()
            for (i in 0..4095) {
                contextSB.append("AB")
            }
            val contextInfo = contextSB.toString()
            val lengthOfLengthOfPayoutInfo = "02"
            val lengthOfPayoutInfo = "0001" // = 1
            val payoutSB = StringBuilder()
            for (i in 0..0) {
                payoutSB.append("CD")
            }
            val payoutInfo = payoutSB.toString()
            val popEndorsement = AltchainPopEndorsement(
                assembleHex(
                    identifierLength,
                    identifier,
                    lengthOfLengthOfHeader,
                    lengthOfHeader,
                    header,
                    lengthOfLengthOfContext,
                    lengthOfContext,
                    contextInfo,
                    lengthOfLengthOfPayoutInfo,
                    lengthOfPayoutInfo,
                    payoutInfo
                )
            )
            Utility.bytesToHex(popEndorsement.getHeader()) shouldBe header
            popEndorsement.identifier shouldBe 4294967295L
            Utility.byteArraysAreEqual(
                popEndorsement.getContextInfo(),
                Utility.hexToBytes(contextInfo)
            ) shouldBe true
            Utility.byteArraysAreEqual(
                popEndorsement.getPayoutInfo(),
                Utility.hexToBytes(payoutInfo)
            ) shouldBe true
    }

    @Test
    fun testEndorsementWithContextAndPayout_9() {
            val identifierLength = "04"
            val identifier = "FFFFFFFF"
            val lengthOfLengthOfHeader = "01"
            val lengthOfHeader = "10" // = 16
            val header = "00112233445566778899AABBCCDDEEFF"
            val lengthOfLengthOfContext = "02"
            val lengthOfContext = "1001" // = 4097
            val contextSB = StringBuilder()
            for (i in 0..4096) {
                contextSB.append("FF")
            }
            val contextInfo = contextSB.toString()
            val lengthOfLengthOfPayoutInfo = "02"
            val lengthOfPayoutInfo = "1001" // = 4097
            val payoutSB = StringBuilder()
            for (i in 0..4096) {
                payoutSB.append("00")
            }
            val payoutInfo = payoutSB.toString()
            val popEndorsement = AltchainPopEndorsement(
                assembleHex(
                    identifierLength,
                    identifier,
                    lengthOfLengthOfHeader,
                    lengthOfHeader,
                    header,
                    lengthOfLengthOfContext,
                    lengthOfContext,
                    contextInfo,
                    lengthOfLengthOfPayoutInfo,
                    lengthOfPayoutInfo,
                    payoutInfo
                )
            )
            Utility.bytesToHex(popEndorsement.getHeader()) shouldBe header
            popEndorsement.identifier shouldBe 4294967295L
            Utility.byteArraysAreEqual(
                popEndorsement.getContextInfo(),
                Utility.hexToBytes(contextInfo)
            ) shouldBe true
            Utility.byteArraysAreEqual(
                popEndorsement.getPayoutInfo(),
                Utility.hexToBytes(payoutInfo)
            ) shouldBe true
    }

    @Test(expected = IllegalArgumentException::class)
    fun testBadEndorsement_1() {
        val identifierLength = "01"
        val identifier = "0000" // IDENTIFIER TOO LONG
        val lengthOfLengthOfHeader = "01"
        val lengthOfHeader = "01" // = 1
        val header = "00"
        val lengthOfLengthOfContext = "01"
        val lengthOfContext = "01" // = 1
        val contextInfo = "00"
        val lengthOfLengthOfPayoutInfo = "01"
        val lengthOfPayoutInfo = "01" // = 1
        val payoutInfo = "00"
        AltchainPopEndorsement(
            assembleHex(
                identifierLength,
                identifier,
                lengthOfLengthOfHeader,
                lengthOfHeader,
                header,
                lengthOfLengthOfContext,
                lengthOfContext,
                contextInfo,
                lengthOfLengthOfPayoutInfo,
                lengthOfPayoutInfo,
                payoutInfo
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testBadEndorsement_2() {
        val identifierLength = "00" // Can't have 0-length identifier
        AltchainPopEndorsement(
            assembleHex(
                identifierLength
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testBadEndorsement_3() {
        val identifierLength = "01"
        val identifier = "00"
        val lengthOfLengthOfHeader = "01"
        val lengthOfHeader = "0100" // Too large!
        val header = "00"
        val lengthOfLengthOfContext = "01"
        val lengthOfContext = "01" // = 1
        val contextInfo = "00"
        val lengthOfLengthOfPayoutInfo = "01"
        val lengthOfPayoutInfo = "01" // = 1
        val payoutInfo = "00"
        AltchainPopEndorsement(
            assembleHex(
                identifierLength,
                identifier,
                lengthOfLengthOfHeader,
                lengthOfHeader,
                header,
                lengthOfLengthOfContext,
                lengthOfContext,
                contextInfo,
                lengthOfLengthOfPayoutInfo,
                lengthOfPayoutInfo,
                payoutInfo
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testBadEndorsement_4() {
        val identifierLength = "01"
        val identifier = "00"
        val lengthOfLengthOfHeader = "01"
        val lengthOfHeader = "01" // = 1
        val header = "0000" // Too large!
        val lengthOfLengthOfContext = "01"
        val lengthOfContext = "01" // = 1
        val contextInfo = "00"
        val lengthOfLengthOfPayoutInfo = "01"
        val lengthOfPayoutInfo = "01" // = 1
        val payoutInfo = "00"
        AltchainPopEndorsement(
            assembleHex(
                identifierLength,
                identifier,
                lengthOfLengthOfHeader,
                lengthOfHeader,
                header,
                lengthOfLengthOfContext,
                lengthOfContext,
                contextInfo,
                lengthOfLengthOfPayoutInfo,
                lengthOfPayoutInfo,
                payoutInfo
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testBadEndorsement_5() {
        val identifierLength = "01"
        val identifier = "00"
        val lengthOfLengthOfHeader = "01"
        val lengthOfHeader = "01" // = 1
        val header = "00"
        val lengthOfLengthOfContext = "04" // Too high of a value!
        val lengthOfContext = "01" // = 1
        val contextInfo = "00"
        val lengthOfLengthOfPayoutInfo = "01"
        val lengthOfPayoutInfo = "01" // = 1
        val payoutInfo = "00"
        AltchainPopEndorsement(
            assembleHex(
                identifierLength,
                identifier,
                lengthOfLengthOfHeader,
                lengthOfHeader,
                header,
                lengthOfLengthOfContext,
                lengthOfContext,
                contextInfo,
                lengthOfLengthOfPayoutInfo,
                lengthOfPayoutInfo,
                payoutInfo
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testBadEndorsement_6() {
        val identifierLength = "01"
        val identifier = "00"
        val lengthOfLengthOfHeader = "01"
        val lengthOfHeader = "01" // = 1
        val header = "00"
        val lengthOfLengthOfContext = "01"
        val lengthOfContext = "0001" // Too long!
        val contextInfo = "00"
        val lengthOfLengthOfPayoutInfo = "01"
        val lengthOfPayoutInfo = "01" // = 1
        val payoutInfo = "00"
        AltchainPopEndorsement(
            assembleHex(
                identifierLength,
                identifier,
                lengthOfLengthOfHeader,
                lengthOfHeader,
                header,
                lengthOfLengthOfContext,
                lengthOfContext,
                contextInfo,
                lengthOfLengthOfPayoutInfo,
                lengthOfPayoutInfo,
                payoutInfo
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testBadEndorsement_7() {
        val identifierLength = "01"
        val identifier = "00"
        val lengthOfLengthOfHeader = "01"
        val lengthOfHeader = "01" // = 1
        val header = "00"
        val lengthOfLengthOfContext = "01"
        val lengthOfContext = "01" // = 1
        val contextInfo = "0000" // Too long!
        val lengthOfLengthOfPayoutInfo = "01"
        val lengthOfPayoutInfo = "01" // = 1
        val payoutInfo = "00"
        AltchainPopEndorsement(
            assembleHex(
                identifierLength,
                identifier,
                lengthOfLengthOfHeader,
                lengthOfHeader,
                header,
                lengthOfLengthOfContext,
                lengthOfContext,
                contextInfo,
                lengthOfLengthOfPayoutInfo,
                lengthOfPayoutInfo,
                payoutInfo
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testBadEndorsement_8() {
        val identifierLength = "01"
        val identifier = "00"
        val lengthOfLengthOfHeader = "01"
        val lengthOfHeader = "01" // = 1
        val header = "00"
        val lengthOfLengthOfContext = "01"
        val lengthOfContext = "01" // = 1
        val contextInfo = "00"
        val lengthOfLengthOfPayoutInfo = "04" // Value too high!
        val lengthOfPayoutInfo = "01" // = 1
        val payoutInfo = "00"
        AltchainPopEndorsement(
            assembleHex(
                identifierLength,
                identifier,
                lengthOfLengthOfHeader,
                lengthOfHeader,
                header,
                lengthOfLengthOfContext,
                lengthOfContext,
                contextInfo,
                lengthOfLengthOfPayoutInfo,
                lengthOfPayoutInfo,
                payoutInfo
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testBadEndorsement_9() {
        val identifierLength = "01"
        val identifier = "00"
        val lengthOfLengthOfHeader = "01"
        val lengthOfHeader = "01" // = 1
        val header = "00"
        val lengthOfLengthOfContext = "01"
        val lengthOfContext = "01" // = 1
        val contextInfo = "00"
        val lengthOfLengthOfPayoutInfo = "01"
        val lengthOfPayoutInfo = "0001" // Too long!
        val payoutInfo = "00"
        AltchainPopEndorsement(
            assembleHex(
                identifierLength,
                identifier,
                lengthOfLengthOfHeader,
                lengthOfHeader,
                header,
                lengthOfLengthOfContext,
                lengthOfContext,
                contextInfo,
                lengthOfLengthOfPayoutInfo,
                lengthOfPayoutInfo,
                payoutInfo
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testBadEndorsement_10() {
        val identifierLength = "01"
        val identifier = "00"
        val lengthOfLengthOfHeader = "01"
        val lengthOfHeader = "01" // = 1
        val header = "00"
        val lengthOfLengthOfContext = "01"
        val lengthOfContext = "01" // = 1
        val contextInfo = "00"
        val lengthOfLengthOfPayoutInfo = "01"
        val lengthOfPayoutInfo = "01" // = 1
        val payoutInfo = "0000" // Too long!
        AltchainPopEndorsement(
            assembleHex(
                identifierLength,
                identifier,
                lengthOfLengthOfHeader,
                lengthOfHeader,
                header,
                lengthOfLengthOfContext,
                lengthOfContext,
                contextInfo,
                lengthOfLengthOfPayoutInfo,
                lengthOfPayoutInfo,
                payoutInfo
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testBadEndorsement_11() {
        val identifierLength = "09" // Value too high!
        val identifier = "00"
        val lengthOfLengthOfHeader = "01"
        val lengthOfHeader = "01" // = 1
        val header = "00"
        val lengthOfLengthOfContext = "01"
        val lengthOfContext = "01" // = 1
        val contextInfo = "00"
        val lengthOfLengthOfPayoutInfo = "01"
        val lengthOfPayoutInfo = "01" // = 1
        val payoutInfo = "00"
        AltchainPopEndorsement(
            assembleHex(
                identifierLength,
                identifier,
                lengthOfLengthOfHeader,
                lengthOfHeader,
                header,
                lengthOfLengthOfContext,
                lengthOfContext,
                contextInfo,
                lengthOfLengthOfPayoutInfo,
                lengthOfPayoutInfo,
                payoutInfo
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testBadEndorsement_12() {
        val identifierLength = "01"
        val identifier = "00"
        val lengthOfLengthOfHeader = "01"
        val lengthOfHeader = "01" // = 1
        val header = "00"
        val lengthOfLengthOfContext = "01"
        val lengthOfContext = "01" // = 1
        val contextInfo = "00"
        val lengthOfLengthOfPayoutInfo = "01"
        val lengthOfPayoutInfo = "01" // = 1
        val payoutInfo = "00"
        val extraInfoAtEnd = "00"
        AltchainPopEndorsement(
            assembleHex(
                identifierLength,
                identifier,
                lengthOfLengthOfHeader,
                lengthOfHeader,
                header,
                lengthOfLengthOfContext,
                lengthOfContext,
                contextInfo,
                lengthOfLengthOfPayoutInfo,
                lengthOfPayoutInfo,
                payoutInfo,
                extraInfoAtEnd
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testBadEndorsement_13() {
        val extraInfoAtBeginning = "01"
        val identifierLength = "01"
        val identifier = "00"
        val lengthOfLengthOfHeader = "01"
        val lengthOfHeader = "01" // = 1
        val header = "00"
        val lengthOfLengthOfContext = "01"
        val lengthOfContext = "01" // = 1
        val contextInfo = "00"
        val lengthOfLengthOfPayoutInfo = "01"
        val lengthOfPayoutInfo = "01" // = 1
        val payoutInfo = "00"
        AltchainPopEndorsement(
            assembleHex(
                extraInfoAtBeginning,
                identifierLength,
                identifier,
                lengthOfLengthOfHeader,
                lengthOfHeader,
                header,
                lengthOfLengthOfContext,
                lengthOfContext,
                contextInfo,
                lengthOfLengthOfPayoutInfo,
                lengthOfPayoutInfo,
                payoutInfo
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testBadEndorsement_14() {
        val identifierLength = "01"
        val extraInfoInMiddle = "00"
        val identifier = "00"
        val lengthOfLengthOfHeader = "01"
        val lengthOfHeader = "01" // = 1
        val header = "00"
        val lengthOfLengthOfContext = "01"
        val lengthOfContext = "01" // = 1
        val contextInfo = "00"
        val lengthOfLengthOfPayoutInfo = "01"
        val lengthOfPayoutInfo = "01" // = 1
        val payoutInfo = "00"
        AltchainPopEndorsement(
            assembleHex(
                identifierLength,
                extraInfoInMiddle,
                identifier,
                lengthOfLengthOfHeader,
                lengthOfHeader,
                header,
                lengthOfLengthOfContext,
                lengthOfContext,
                contextInfo,
                lengthOfLengthOfPayoutInfo,
                lengthOfPayoutInfo,
                payoutInfo
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testBadEndorsement_15() {
        val identifierLength = "01"
        val identifier = "00"
        val extraInfoInMiddle = "01"
        val lengthOfLengthOfHeader = "01"
        val lengthOfHeader = "01" // = 1
        val header = "00"
        val lengthOfLengthOfContext = "01"
        val lengthOfContext = "01" // = 1
        val contextInfo = "00"
        val lengthOfLengthOfPayoutInfo = "01"
        val lengthOfPayoutInfo = "01" // = 1
        val payoutInfo = "00"
        AltchainPopEndorsement(
            assembleHex(
                identifierLength,
                identifier,
                extraInfoInMiddle,
                lengthOfLengthOfHeader,
                lengthOfHeader,
                header,
                lengthOfLengthOfContext,
                lengthOfContext,
                contextInfo,
                lengthOfLengthOfPayoutInfo,
                lengthOfPayoutInfo,
                payoutInfo
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testBadEndorsement_16() {
        val identifierLength = "01"
        val identifier = "00"
        val lengthOfLengthOfHeader = "01"
        val extraInfoInMiddle = "01"
        val lengthOfHeader = "01" // = 1
        val header = "00"
        val lengthOfLengthOfContext = "01"
        val lengthOfContext = "01" // = 1
        val contextInfo = "00"
        val lengthOfLengthOfPayoutInfo = "01"
        val lengthOfPayoutInfo = "01" // = 1
        val payoutInfo = "00"
        AltchainPopEndorsement(
            assembleHex(
                identifierLength,
                identifier,
                lengthOfLengthOfHeader,
                extraInfoInMiddle,
                lengthOfHeader,
                header,
                lengthOfLengthOfContext,
                lengthOfContext,
                contextInfo,
                lengthOfLengthOfPayoutInfo,
                lengthOfPayoutInfo,
                payoutInfo
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testBadEndorsement_17() {
        val identifierLength = "01"
        val identifier = "00"
        val lengthOfLengthOfHeader = "01"
        val lengthOfHeader = "01" // = 1
        val extraInfoInMiddle = "00"
        val header = "00"
        val lengthOfLengthOfContext = "01"
        val lengthOfContext = "01" // = 1
        val contextInfo = "00"
        val lengthOfLengthOfPayoutInfo = "01"
        val lengthOfPayoutInfo = "01" // = 1
        val payoutInfo = "00"
        AltchainPopEndorsement(
            assembleHex(
                identifierLength,
                identifier,
                lengthOfLengthOfHeader,
                lengthOfHeader,
                extraInfoInMiddle,
                header,
                lengthOfLengthOfContext,
                lengthOfContext,
                contextInfo,
                lengthOfLengthOfPayoutInfo,
                lengthOfPayoutInfo,
                payoutInfo
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testBadEndorsement_18() {
        val identifierLength = "01"
        val identifier = "00"
        val lengthOfLengthOfHeader = "01"
        val lengthOfHeader = "01" // = 1
        val header = "00"
        val extraInfoInMiddle = "01"
        val lengthOfLengthOfContext = "01"
        val lengthOfContext = "01" // = 1
        val contextInfo = "00"
        val lengthOfLengthOfPayoutInfo = "01"
        val lengthOfPayoutInfo = "01" // = 1
        val payoutInfo = "00"
        AltchainPopEndorsement(
            assembleHex(
                identifierLength,
                identifier,
                lengthOfLengthOfHeader,
                lengthOfHeader,
                header,
                extraInfoInMiddle,
                lengthOfLengthOfContext,
                lengthOfContext,
                contextInfo,
                lengthOfLengthOfPayoutInfo,
                lengthOfPayoutInfo,
                payoutInfo
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testBadEndorsement_19() {
        val identifierLength = "01"
        val identifier = "00"
        val lengthOfLengthOfHeader = "01"
        val lengthOfHeader = "01" // = 1
        val header = "00"
        val lengthOfLengthOfContext = "01"
        val extraInfoInMiddle = "01"
        val lengthOfContext = "01" // = 1
        val contextInfo = "00"
        val lengthOfLengthOfPayoutInfo = "01"
        val lengthOfPayoutInfo = "01" // = 1
        val payoutInfo = "00"
        AltchainPopEndorsement(
            assembleHex(
                identifierLength,
                identifier,
                lengthOfLengthOfHeader,
                lengthOfHeader,
                header,
                lengthOfLengthOfContext,
                extraInfoInMiddle,
                lengthOfContext,
                contextInfo,
                lengthOfLengthOfPayoutInfo,
                lengthOfPayoutInfo,
                payoutInfo
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testBadEndorsement_20() {
        val identifierLength = "01"
        val identifier = "00"
        val lengthOfLengthOfHeader = "01"
        val lengthOfHeader = "01" // = 1
        val header = "00"
        val lengthOfLengthOfContext = "01"
        val lengthOfContext = "01" // = 1
        val extraInfoInMiddle = "00"
        val contextInfo = "00"
        val lengthOfLengthOfPayoutInfo = "01"
        val lengthOfPayoutInfo = "01" // = 1
        val payoutInfo = "00"
        AltchainPopEndorsement(
            assembleHex(
                identifierLength,
                identifier,
                lengthOfLengthOfHeader,
                lengthOfHeader,
                header,
                lengthOfLengthOfContext,
                lengthOfContext,
                extraInfoInMiddle,
                contextInfo,
                lengthOfLengthOfPayoutInfo,
                lengthOfPayoutInfo,
                payoutInfo
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testBadEndorsement_21() {
        val identifierLength = "01"
        val identifier = "00"
        val lengthOfLengthOfHeader = "01"
        val lengthOfHeader = "01" // = 1
        val header = "00"
        val lengthOfLengthOfContext = "01"
        val lengthOfContext = "01" // = 1
        val contextInfo = "00"
        val extraInfoInMiddle = "01"
        val lengthOfLengthOfPayoutInfo = "01"
        val lengthOfPayoutInfo = "01" // = 1
        val payoutInfo = "00"
        AltchainPopEndorsement(
            assembleHex(
                identifierLength,
                identifier,
                lengthOfLengthOfHeader,
                lengthOfHeader,
                header,
                lengthOfLengthOfContext,
                lengthOfContext,
                contextInfo,
                extraInfoInMiddle,
                lengthOfLengthOfPayoutInfo,
                lengthOfPayoutInfo,
                payoutInfo
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testBadEndorsement_22() {
        val identifierLength = "01"
        val identifier = "00"
        val lengthOfLengthOfHeader = "01"
        val lengthOfHeader = "01" // = 1
        val header = "00"
        val lengthOfLengthOfContext = "01"
        val lengthOfContext = "01" // = 1
        val contextInfo = "00"
        val lengthOfLengthOfPayoutInfo = "01"
        val extraInfoInMiddle = "01"
        val lengthOfPayoutInfo = "01" // = 1
        val payoutInfo = "00"
        AltchainPopEndorsement(
            assembleHex(
                identifierLength,
                identifier,
                lengthOfLengthOfHeader,
                lengthOfHeader,
                header,
                lengthOfLengthOfContext,
                lengthOfContext,
                contextInfo,
                lengthOfLengthOfPayoutInfo,
                extraInfoInMiddle,
                lengthOfPayoutInfo,
                payoutInfo
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testBadEndorsement_23() {
        val identifierLength = "01"
        val identifier = "00"
        val lengthOfLengthOfHeader = "01"
        val lengthOfHeader = "01" // = 1
        val header = "00"
        val lengthOfLengthOfContext = "01"
        val lengthOfContext = "01" // = 1
        val contextInfo = "00"
        val lengthOfLengthOfPayoutInfo = "01"
        val lengthOfPayoutInfo = "01" // = 1
        val extraInfoInMiddle = "01"
        val payoutInfo = "00"
        AltchainPopEndorsement(
            assembleHex(
                identifierLength,
                identifier,
                lengthOfLengthOfHeader,
                lengthOfHeader,
                header,
                lengthOfLengthOfContext,
                lengthOfContext,
                contextInfo,
                lengthOfLengthOfPayoutInfo,
                lengthOfPayoutInfo,
                extraInfoInMiddle,
                payoutInfo
            )
        )
    }

    companion object {
        private fun assembleHex(vararg parts: String): ByteArray {
            val assembled = StringBuilder()
            for (i in 0 until parts.size) {
                assembled.append(parts[i])
            }
            return Utility.hexToBytes(assembled.toString())
        }
    }
}
