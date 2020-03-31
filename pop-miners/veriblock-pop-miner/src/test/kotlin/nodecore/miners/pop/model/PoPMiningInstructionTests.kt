// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.miners.pop.model

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.veriblock.core.utilities.extensions.asHexBytes
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class PoPMiningInstructionTests {
    private lateinit var data: PopMiningInstruction

    @Before
    fun before() {
        data = PopMiningInstruction(
            publicationData = "000000010000000000000000000000000000000000000000000000002d12960b6cc8a7021286ad682f0cbefa4babc4adc613b6925aaaa1d304017d78001d415c007bccf350d249a488c6fe8621b41723".asHexBytes(),
            endorsedBlockHeader = "000000010000000000000000000000000000000000000000000000002d12960b6cc8a7021286ad682f0cbefa4babc4adc613b6925aaaa1d304017d78001d415c".asHexBytes(),
            lastBitcoinBlock = "0000002018a0b6b0013ac826c28ca5e5f8f1ee2d30c1302c1c2aac2ee004d44f5bd4fb428fb04efbf3927784bed987b3a1f319434ec44f331302c11db158a2f9416dc7838b61ce5affff7f2001000000".asHexBytes(),
            minerAddressBytes = "672a9d1281cc9d25bfb4539145702bfb2d8ae61dbbc0".asHexBytes()
        )
    }

    @Test
    fun serializationWhenAllPropertiesSet() {
        val returned = serializeThenDeserialize()
        Assert.assertArrayEquals(data.endorsedBlockHeader, returned.endorsedBlockHeader)
        Assert.assertArrayEquals(data.lastBitcoinBlock, returned.lastBitcoinBlock)
        Assert.assertArrayEquals(data.minerAddressBytes, returned.minerAddressBytes)
        Assert.assertArrayEquals(data.publicationData, returned.publicationData)
    }

    private fun serializeThenDeserialize(): PopMiningInstruction {
        val serialized = ByteArrayOutputStream().use { dataOut ->
            ObjectOutputStream(dataOut).use { out ->
                out.writeObject(data)
            }
            dataOut.toByteArray()
        }
        return ByteArrayInputStream(serialized).use { dataIn ->
            ObjectInputStream(dataIn).use {
                it.readObject() as PopMiningInstruction
            }
        }
    }
}
