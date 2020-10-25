// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.sdk

import io.kotlintest.shouldBe
import org.junit.Before
import org.junit.Test
import org.veriblock.core.Context
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.crypto.asVbkPreviousBlockHash
import org.veriblock.core.crypto.asVbkPreviousKeystoneHash
import org.veriblock.core.params.getDefaultNetworkParameters
import org.veriblock.core.utilities.Utility
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.services.parseVeriBlockBlock
import org.veriblock.sdk.services.serialize
import java.nio.ByteBuffer

private lateinit var defaultBlock: VeriBlockBlock

private const val defaultBlockEncoded =
    "41000013880002449c60619294546ad825af03b0935637860679ddd55ee4fd21082e18686e" +
        "26bbfda7d5e4462ef24ae02d67e47d785c9b90f3010100000000000001"

class VeriBlockBlockTests {
    @Before
    fun setUp() {
        val params = getDefaultNetworkParameters("regtest")
        Context.create(params)
        defaultBlock = VeriBlockBlock(
            5000,
            2,
            Utility.hexToBytes("449c60619294546ad825af03").asVbkPreviousBlockHash(),
            Utility.hexToBytes("b0935637860679ddd5").asVbkPreviousKeystoneHash(),
            Utility.hexToBytes("5ee4fd21082e18686e").asVbkPreviousKeystoneHash(),
            Sha256Hash.wrap(Utility.hexToBytes("26bbfda7d5e4462ef24ae02d67e47d78"), Sha256Hash.VERIBLOCK_MERKLE_ROOT_LENGTH),
            1553699059,
            16842752,
            1)
    }

    @Test
    fun parse() {
        val input = Utility.hexToBytes(defaultBlockEncoded)
        val decoded = ByteBuffer.wrap(input).parseVeriBlockBlock()
        decoded shouldBe defaultBlock
    }

    @Test
    fun roundtrip() {
        val bytes = defaultBlock.serialize()
        val decoded = ByteBuffer.wrap(bytes).parseVeriBlockBlock()
        defaultBlock shouldBe decoded
    }
}
