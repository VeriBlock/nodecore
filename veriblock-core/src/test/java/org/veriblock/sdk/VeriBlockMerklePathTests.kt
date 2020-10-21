// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.sdk

import io.kotlintest.shouldBe
import org.junit.Assert
import org.junit.Test
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.utilities.Utility
import org.veriblock.sdk.models.MerklePath
import org.veriblock.sdk.models.VeriBlockMerklePath
import java.nio.ByteBuffer

private const val defaultPathEncoded =
    "04000000010400000000201fec8aa4983d69395010e4d18cd8b943749d5b4f575e88a375de" +
        "bdc5ed22531c04000000022000000000000000000000000000000000000000000000000000" +
        "00000000000000200000000000000000000000000000000000000000000000000000000000" +
        "000000"

private const val defaultSubject = "1fec8aa4983d69395010e4d18cd8b943749d5b4f575e88a375debdc5ed22531c"
private const val defaultMerkleCompact = "1:0:1FEC8AA4983D69395010E4D18CD8B943749D5B4F575E88A375DEBDC5ED22531C:0000000000000000000000000000000000000000000000000000000000000000:0000000000000000000000000000000000000000000000000000000000000000"

class VeriBlockMerklePathTests {
    @Test
    fun parse() {
        val input = Utility.hexToBytes(defaultPathEncoded)
        val decoded = VeriBlockMerklePath.parse(ByteBuffer.wrap(input))
        decoded.treeIndex shouldBe 1
        decoded.index shouldBe 0
        decoded.subject shouldBe Sha256Hash.wrap(Utility.hexToBytes(defaultSubject))
    }

    @Test
    fun roundtrip() {
        val input = VeriBlockMerklePath(defaultMerkleCompact)
        val bytes = input.serialize()
        val decoded = VeriBlockMerklePath.parse(ByteBuffer.wrap(bytes))
        input shouldBe decoded
    }
}
