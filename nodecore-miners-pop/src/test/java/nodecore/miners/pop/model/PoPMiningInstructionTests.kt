// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.miners.pop.model

import nodecore.miners.pop.common.Utility
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class PoPMiningInstructionTests {
    private var data: PopMiningInstruction? = null

    @Before
    fun before() {
        data = PopMiningInstruction().also {
            it.publicationData = Utility.hexToBytes(
                "000000010000000000000000000000000000000000000000000000002d12960b6cc8a7021286ad682f0cbefa4babc4adc613b6925aaaa1d304017d78001d415c007bccf350d249a488c6fe8621b41723"
            )
            it.endorsedBlockHeader = Utility.hexToBytes(
                "000000010000000000000000000000000000000000000000000000002d12960b6cc8a7021286ad682f0cbefa4babc4adc613b6925aaaa1d304017d78001d415c"
            )
            it.lastBitcoinBlock = Utility.hexToBytes(
                "0000002018a0b6b0013ac826c28ca5e5f8f1ee2d30c1302c1c2aac2ee004d44f5bd4fb428fb04efbf3927784bed987b3a1f319434ec44f331302c11db158a2f9416dc7838b61ce5affff7f2001000000"
            )
            it.minerAddress = Utility.hexToBytes("672a9d1281cc9d25bfb4539145702bfb2d8ae61dbbc0")
        }
    }

    @Test
    fun serializationWhenAllPropertiesSet() {
        val returned = serializeThenDeserialize()
        Assert.assertArrayEquals(data!!.endorsedBlockHeader, returned!!.endorsedBlockHeader)
        Assert.assertArrayEquals(data!!.lastBitcoinBlock, returned.lastBitcoinBlock)
        Assert.assertArrayEquals(data!!.minerAddress, returned.minerAddress)
        Assert.assertArrayEquals(data!!.publicationData, returned.publicationData)
    }

    @Test
    fun serializationWhenNullEndorsedBlockHeader() {
        data!!.endorsedBlockHeader = null
        val returned = serializeThenDeserialize()
        Assert.assertNull(returned!!.endorsedBlockHeader)
        Assert.assertArrayEquals(data!!.lastBitcoinBlock, returned.lastBitcoinBlock)
        Assert.assertArrayEquals(data!!.minerAddress, returned.minerAddress)
        Assert.assertArrayEquals(data!!.publicationData, returned.publicationData)
    }

    @Test
    fun serializationWhenNullLastBitcoinBlock() {
        data!!.lastBitcoinBlock = null
        val returned = serializeThenDeserialize()
        Assert.assertArrayEquals(data!!.endorsedBlockHeader, returned!!.endorsedBlockHeader)
        Assert.assertNull(returned.lastBitcoinBlock)
        Assert.assertArrayEquals(data!!.minerAddress, returned.minerAddress)
        Assert.assertArrayEquals(data!!.publicationData, returned.publicationData)
    }

    @Test
    fun serializationWhenNullMinerAddress() {
        data!!.minerAddress = null
        val returned = serializeThenDeserialize()
        Assert.assertArrayEquals(data!!.endorsedBlockHeader, returned!!.endorsedBlockHeader)
        Assert.assertArrayEquals(data!!.lastBitcoinBlock, returned.lastBitcoinBlock)
        Assert.assertNull(returned.minerAddress)
        Assert.assertArrayEquals(data!!.publicationData, returned.publicationData)
    }

    @Test
    fun serializationWhenNullPublicationData() {
        data!!.publicationData = null
        val returned = serializeThenDeserialize()
        Assert.assertArrayEquals(data!!.endorsedBlockHeader, returned!!.endorsedBlockHeader)
        Assert.assertArrayEquals(data!!.lastBitcoinBlock, returned.lastBitcoinBlock)
        Assert.assertArrayEquals(data!!.minerAddress, returned.minerAddress)
        Assert.assertNull(returned.publicationData)
    }

    private fun serializeThenDeserialize(): PopMiningInstruction? {
        var serialized = ByteArray(0)
        try {
            ByteArrayOutputStream().use { dataOut ->
                ObjectOutputStream(dataOut).use { out ->
                    out.writeObject(data)
                    serialized = dataOut.toByteArray()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        try {
            ByteArrayInputStream(serialized).use { dataIn ->
                ObjectInputStream(
                    dataIn
                ).use { `in` -> return `in`.readObject() as PopMiningInstruction }
            }
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }
}
