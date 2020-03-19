// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.miners.pop.model

import nodecore.miners.pop.core.OperationStateType
import nodecore.miners.pop.core.OperationStatus
import nodecore.miners.pop.model.dto.PopMiningOperationStateDto
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.veriblock.core.utilities.extensions.asHexBytes
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.ArrayList
import java.util.LinkedHashSet

class PreservedPoPMiningOperationStateTests {
    private lateinit var data: PopMiningOperationStateDto

    @Before
    fun before() {
        data = PopMiningOperationStateDto().apply {
            operationId = "6d2abb8d"
            status = OperationStatus.RUNNING
            currentAction = OperationStateType.CONFIRMED
            miningInstruction = PopMiningInstruction(
                publicationData = "000000010000000000000000000000000000000000000000000000002D12960B6CC8A7021286AD682F0CBEFA4BABC4ADC613B6925AAAA1D304017D78001D415C007BCCF350D249A488C6FE8621B41723".asHexBytes(),
                endorsedBlockHeader = "000000010000000000000000000000000000000000000000000000002D12960B6CC8A7021286AD682F0CBEFA4BABC4ADC613B6925AAAA1D304017D78001D415C".asHexBytes(),
                lastBitcoinBlock = "0000002018A0B6B0013AC826C28CA5E5F8F1EE2D30C1302C1C2AAC2EE004D44F5BD4FB428FB04EFBF3927784BED987B3A1F319434EC44F331302C11DB158A2F9416DC7838B61CE5AFFFF7F2001000000".asHexBytes(),
                minerAddressBytes = "672A9D1281CC9D25BFB4539145702BFB2D8AE61DBBC0".asHexBytes()
            )
            transaction = "01000000014A41098C745A95C95152EBF0D21CC727264EEB0D3E397DEE7A81E1594DAC64DC000000006B483045022100A778D80D03E0CB9C8102B4F72A5D09FB041E26AA6FFCD76BC6ED5A4BBE3244DD02203B375BB052E4FB87BB3F982916D7F6928A7FE6DFE63EEEA7754954D33874DDDE012103B3CD160BE576FFAC4007474E85D7F4E9039F9C6A38F8DC7CB8F3BAE1C72DCF4BFFFFFFFF020C4BE905000000001976A914A6157B95A9870689D81B6E5AA9EF0D8828284E0188AC0000000000000000536A4C50000000010000000000000000000000000000000000000000000000002D12960B6CC8A7021286AD682F0CBEFA4BABC4ADC613B6925AAAA1D304017D78001D415C007BCCF350D249A488C6FE8621B4172300000000".asHexBytes()
            submittedTransactionId = "43c9b4c05a2f64d08a9ca0be7cdae61d4179cb2c77a00941145081a9aad1a8d4"
            bitcoinBlockHeaderOfProof = "03000030199FC1A896F9819A40CDE5E70E7F6E9FDB8E607D1CEC5B0ACF4FB5F10C84EA07238B75965AE74AFF18C332C811BB2D08CF87FC3F2D85A9F6D62157BD4657E6F101BDCF5AFFFF7F2000000000".asHexBytes()
            bitcoinContextBlocks = ArrayList()
            bitcoinContextBlocks.add(
                "030000303E88C722FA62089A910993B4CD1319A96412DA4A25A5F7D99F7812BC3A322C4BD8FD3A2BBA9B60D7BBD9A6D2CC4F64CFF18075C9793490340ABCD49DA02D51E7F2BCCF5AFFFF7F2003000000".asHexBytes()
            )
            bitcoinContextBlocks.add(
                "030000304F316D96D927BFF8CDCE84DDF79E17E2D738E4BAEE9A4F4FD9AE44C70D1B1159A08E0C547AC0D70AAF00D937DF2A2659E4FA797DDB7DBC2C76033B7341F1618BF2BCCF5AFFFF7F2000000000".asHexBytes()
            )
            merklePath = "1:D4A8D1AAA98150144109A0772CCB79411DE6DA7CBEA09C8AD0642F5AC0B4C943:8D94793049965927A4DDCB3911C20EDE6895504075860112779442EAE0C28610"
            alternateBlocksOfProof = LinkedHashSet()
            alternateBlocksOfProof.add(
                "03000030A035AF9E26BADA957C6533B9B3A52E12FADCDC993C9881946C6FC6ED926F171B6FF2C6049B390E3D5846D7A10D8528B4E295C6CF376D2012316756BD654BC1580EBBCF5AFFFF7F200000000000".asHexBytes()
            )
            alternateBlocksOfProof.add(
                "030000301B2030F1DCFC400A71D42AB90AEA1AED29BC6919CE3E5FCDF656B625CA5139215BA0E2F0EE78004185C09365891699641D9BB6E68E0EDD82D6EFEB8656F5EE8716BACF5AFFFF7F200200000000".asHexBytes()
            )
        }
    }

    @Test
    fun serializationWhenAllPropertiesSet() {
        val returned = serializeThenDeserialize()
        Assert.assertEquals(data.operationId, returned.operationId)
        Assert.assertEquals(data.status, returned.status)
        Assert.assertEquals(data.currentAction, returned.currentAction)
        Assert.assertArrayEquals(data.miningInstruction.endorsedBlockHeader, returned.miningInstruction.endorsedBlockHeader)
        Assert.assertArrayEquals(data.miningInstruction.lastBitcoinBlock, returned.miningInstruction.lastBitcoinBlock)
        Assert.assertEquals(data.miningInstruction.minerAddress, returned.miningInstruction.minerAddress)
        Assert.assertArrayEquals(data.miningInstruction.publicationData, returned.miningInstruction.publicationData)
        Assert.assertArrayEquals(data.transaction, returned.transaction)
        Assert.assertEquals(data.submittedTransactionId, returned.submittedTransactionId)
        Assert.assertArrayEquals(data.bitcoinBlockHeaderOfProof, returned.bitcoinBlockHeaderOfProof)
        Assert.assertEquals(2, returned.bitcoinContextBlocks.size.toLong())
        Assert.assertArrayEquals(data.bitcoinContextBlocks[0], returned.bitcoinContextBlocks[0])
        Assert.assertArrayEquals(data.bitcoinContextBlocks[1], returned.bitcoinContextBlocks[1])
        Assert.assertEquals(data.merklePath, returned.merklePath)
        Assert.assertEquals(2, returned.alternateBlocksOfProof.size.toLong())
    }

    @Test
    fun serializationWhenOperationIdNull() {
        data.operationId = null
        val returned = serializeThenDeserialize()
        Assert.assertNull(returned.operationId)
        Assert.assertEquals(data.status, returned.status)
        Assert.assertEquals(data.currentAction, returned.currentAction)
        Assert.assertArrayEquals(data.miningInstruction.endorsedBlockHeader, returned.miningInstruction.endorsedBlockHeader)
        Assert.assertArrayEquals(data.miningInstruction.lastBitcoinBlock, returned.miningInstruction.lastBitcoinBlock)
        Assert.assertEquals(data.miningInstruction.minerAddress, returned.miningInstruction.minerAddress)
        Assert.assertArrayEquals(data.miningInstruction.publicationData, returned.miningInstruction.publicationData)
        Assert.assertArrayEquals(data.transaction, returned.transaction)
        Assert.assertEquals(data.submittedTransactionId, returned.submittedTransactionId)
        Assert.assertArrayEquals(data.bitcoinBlockHeaderOfProof, returned.bitcoinBlockHeaderOfProof)
        Assert.assertEquals(2, returned.bitcoinContextBlocks.size.toLong())
        Assert.assertArrayEquals(data.bitcoinContextBlocks[0], returned.bitcoinContextBlocks[0])
        Assert.assertArrayEquals(data.bitcoinContextBlocks[1], returned.bitcoinContextBlocks[1])
        Assert.assertEquals(data.merklePath, returned.merklePath)
        Assert.assertEquals(2, returned.alternateBlocksOfProof.size.toLong())
    }

    @Test
    fun serializationWhenStatusNull() {
        data.status = null
        val returned = serializeThenDeserialize()
        Assert.assertEquals(data.operationId, returned.operationId)
        Assert.assertNull(returned.status)
        Assert.assertEquals(data.currentAction, returned.currentAction)
        Assert.assertArrayEquals(data.miningInstruction.endorsedBlockHeader, returned.miningInstruction.endorsedBlockHeader)
        Assert.assertArrayEquals(data.miningInstruction.lastBitcoinBlock, returned.miningInstruction.lastBitcoinBlock)
        Assert.assertEquals(data.miningInstruction.minerAddress, returned.miningInstruction.minerAddress)
        Assert.assertArrayEquals(data.miningInstruction.publicationData, returned.miningInstruction.publicationData)
        Assert.assertArrayEquals(data.transaction, returned.transaction)
        Assert.assertEquals(data.submittedTransactionId, returned.submittedTransactionId)
        Assert.assertArrayEquals(data.bitcoinBlockHeaderOfProof, returned.bitcoinBlockHeaderOfProof)
        Assert.assertEquals(2, returned.bitcoinContextBlocks.size.toLong())
        Assert.assertArrayEquals(data.bitcoinContextBlocks[0], returned.bitcoinContextBlocks[0])
        Assert.assertArrayEquals(data.bitcoinContextBlocks[1], returned.bitcoinContextBlocks[1])
        Assert.assertEquals(data.merklePath, returned.merklePath)
        Assert.assertEquals(2, returned.alternateBlocksOfProof.size.toLong())
    }

    @Test
    fun serializationWhenCurrentActionNull() {
        data.currentAction = null
        val returned = serializeThenDeserialize()
        Assert.assertEquals(data.operationId, returned.operationId)
        Assert.assertEquals(data.status, returned.status)
        Assert.assertNull(returned.currentAction)
        Assert.assertArrayEquals(data.miningInstruction.endorsedBlockHeader, returned.miningInstruction.endorsedBlockHeader)
        Assert.assertArrayEquals(data.miningInstruction.lastBitcoinBlock, returned.miningInstruction.lastBitcoinBlock)
        Assert.assertEquals(data.miningInstruction.minerAddress, returned.miningInstruction.minerAddress)
        Assert.assertArrayEquals(data.miningInstruction.publicationData, returned.miningInstruction.publicationData)
        Assert.assertArrayEquals(data.transaction, returned.transaction)
        Assert.assertEquals(data.submittedTransactionId, returned.submittedTransactionId)
        Assert.assertArrayEquals(data.bitcoinBlockHeaderOfProof, returned.bitcoinBlockHeaderOfProof)
        Assert.assertEquals(2, returned.bitcoinContextBlocks.size.toLong())
        Assert.assertArrayEquals(data.bitcoinContextBlocks[0], returned.bitcoinContextBlocks[0])
        Assert.assertArrayEquals(data.bitcoinContextBlocks[1], returned.bitcoinContextBlocks[1])
        Assert.assertEquals(data.merklePath, returned.merklePath)
        Assert.assertEquals(2, returned.alternateBlocksOfProof.size.toLong())
    }

    @Test
    fun serializationWhenMiningInstructionNull() {
        data.miningInstruction = null
        val returned = serializeThenDeserialize()
        Assert.assertEquals(data.operationId, returned.operationId)
        Assert.assertEquals(data.status, returned.status)
        Assert.assertEquals(data.currentAction, returned.currentAction)
        Assert.assertNull(returned.miningInstruction)
        Assert.assertArrayEquals(data.transaction, returned.transaction)
        Assert.assertEquals(data.submittedTransactionId, returned.submittedTransactionId)
        Assert.assertArrayEquals(data.bitcoinBlockHeaderOfProof, returned.bitcoinBlockHeaderOfProof)
        Assert.assertEquals(2, returned.bitcoinContextBlocks.size.toLong())
        Assert.assertArrayEquals(data.bitcoinContextBlocks[0], returned.bitcoinContextBlocks[0])
        Assert.assertArrayEquals(data.bitcoinContextBlocks[1], returned.bitcoinContextBlocks[1])
        Assert.assertEquals(data.merklePath, returned.merklePath)
        Assert.assertEquals(2, returned.alternateBlocksOfProof.size.toLong())
    }

    @Test
    fun serializationWhenTransactionNull() {
        data.transaction = null
        val returned = serializeThenDeserialize()
        Assert.assertEquals(data.operationId, returned.operationId)
        Assert.assertEquals(data.status, returned.status)
        Assert.assertEquals(data.currentAction, returned.currentAction)
        Assert.assertArrayEquals(data.miningInstruction.endorsedBlockHeader, returned.miningInstruction.endorsedBlockHeader)
        Assert.assertArrayEquals(data.miningInstruction.lastBitcoinBlock, returned.miningInstruction.lastBitcoinBlock)
        Assert.assertEquals(data.miningInstruction.minerAddress, returned.miningInstruction.minerAddress)
        Assert.assertArrayEquals(data.miningInstruction.publicationData, returned.miningInstruction.publicationData)
        Assert.assertNull(returned.transaction)
        Assert.assertEquals(data.submittedTransactionId, returned.submittedTransactionId)
        Assert.assertArrayEquals(data.bitcoinBlockHeaderOfProof, returned.bitcoinBlockHeaderOfProof)
        Assert.assertEquals(2, returned.bitcoinContextBlocks.size.toLong())
        Assert.assertArrayEquals(data.bitcoinContextBlocks[0], returned.bitcoinContextBlocks[0])
        Assert.assertArrayEquals(data.bitcoinContextBlocks[1], returned.bitcoinContextBlocks[1])
        Assert.assertEquals(data.merklePath, returned.merklePath)
        Assert.assertEquals(2, returned.alternateBlocksOfProof.size.toLong())
    }

    @Test
    fun serializationWhenSubmittedTransactionIdNull() {
        data.submittedTransactionId = null
        val returned = serializeThenDeserialize()
        Assert.assertEquals(data.operationId, returned.operationId)
        Assert.assertEquals(data.status, returned.status)
        Assert.assertEquals(data.currentAction, returned.currentAction)
        Assert.assertArrayEquals(data.miningInstruction.endorsedBlockHeader, returned.miningInstruction.endorsedBlockHeader)
        Assert.assertArrayEquals(data.miningInstruction.lastBitcoinBlock, returned.miningInstruction.lastBitcoinBlock)
        Assert.assertEquals(data.miningInstruction.minerAddress, returned.miningInstruction.minerAddress)
        Assert.assertArrayEquals(data.miningInstruction.publicationData, returned.miningInstruction.publicationData)
        Assert.assertArrayEquals(data.transaction, returned.transaction)
        Assert.assertNull(returned.submittedTransactionId)
        Assert.assertArrayEquals(data.bitcoinBlockHeaderOfProof, returned.bitcoinBlockHeaderOfProof)
        Assert.assertEquals(2, returned.bitcoinContextBlocks.size.toLong())
        Assert.assertArrayEquals(data.bitcoinContextBlocks[0], returned.bitcoinContextBlocks[0])
        Assert.assertArrayEquals(data.bitcoinContextBlocks[1], returned.bitcoinContextBlocks[1])
        Assert.assertEquals(data.merklePath, returned.merklePath)
        Assert.assertEquals(2, returned.alternateBlocksOfProof.size.toLong())
    }

    @Test
    fun serializationWhenBitcoinBlockHeaderOfProofNull() {
        data.bitcoinBlockHeaderOfProof = null
        val returned = serializeThenDeserialize()
        Assert.assertEquals(data.operationId, returned.operationId)
        Assert.assertEquals(data.status, returned.status)
        Assert.assertEquals(data.currentAction, returned.currentAction)
        Assert.assertArrayEquals(data.miningInstruction.endorsedBlockHeader, returned.miningInstruction.endorsedBlockHeader)
        Assert.assertArrayEquals(data.miningInstruction.lastBitcoinBlock, returned.miningInstruction.lastBitcoinBlock)
        Assert.assertEquals(data.miningInstruction.minerAddress, returned.miningInstruction.minerAddress)
        Assert.assertArrayEquals(data.miningInstruction.publicationData, returned.miningInstruction.publicationData)
        Assert.assertArrayEquals(data.transaction, returned.transaction)
        Assert.assertEquals(data.submittedTransactionId, returned.submittedTransactionId)
        Assert.assertNull(returned.bitcoinBlockHeaderOfProof)
        Assert.assertEquals(2, returned.bitcoinContextBlocks.size.toLong())
        Assert.assertArrayEquals(data.bitcoinContextBlocks[0], returned.bitcoinContextBlocks[0])
        Assert.assertArrayEquals(data.bitcoinContextBlocks[1], returned.bitcoinContextBlocks[1])
        Assert.assertEquals(data.merklePath, returned.merklePath)
        Assert.assertEquals(2, returned.alternateBlocksOfProof.size.toLong())
    }

    @Test
    fun serializationWhenBitcoinContextBlocksNull() {
        data.bitcoinContextBlocks = null
        val returned = serializeThenDeserialize()
        Assert.assertEquals(data.operationId, returned.operationId)
        Assert.assertEquals(data.status, returned.status)
        Assert.assertEquals(data.currentAction, returned.currentAction)
        Assert.assertArrayEquals(data.miningInstruction.endorsedBlockHeader, returned.miningInstruction.endorsedBlockHeader)
        Assert.assertArrayEquals(data.miningInstruction.lastBitcoinBlock, returned.miningInstruction.lastBitcoinBlock)
        Assert.assertEquals(data.miningInstruction.minerAddress, returned.miningInstruction.minerAddress)
        Assert.assertArrayEquals(data.miningInstruction.publicationData, returned.miningInstruction.publicationData)
        Assert.assertArrayEquals(data.transaction, returned.transaction)
        Assert.assertEquals(data.submittedTransactionId, returned.submittedTransactionId)
        Assert.assertArrayEquals(data.bitcoinBlockHeaderOfProof, returned.bitcoinBlockHeaderOfProof)
        Assert.assertNull(returned.bitcoinContextBlocks)
        Assert.assertEquals(data.merklePath, returned.merklePath)
        Assert.assertEquals(2, returned.alternateBlocksOfProof.size.toLong())
    }

    @Test
    fun serializationWhenMerklePathNull() {
        data.merklePath = null
        val returned = serializeThenDeserialize()
        Assert.assertEquals(data.operationId, returned.operationId)
        Assert.assertEquals(data.status, returned.status)
        Assert.assertEquals(data.currentAction, returned.currentAction)
        Assert.assertArrayEquals(data.miningInstruction.endorsedBlockHeader, returned.miningInstruction.endorsedBlockHeader)
        Assert.assertArrayEquals(data.miningInstruction.lastBitcoinBlock, returned.miningInstruction.lastBitcoinBlock)
        Assert.assertEquals(data.miningInstruction.minerAddress, returned.miningInstruction.minerAddress)
        Assert.assertArrayEquals(data.miningInstruction.publicationData, returned.miningInstruction.publicationData)
        Assert.assertArrayEquals(data.transaction, returned.transaction)
        Assert.assertEquals(data.submittedTransactionId, returned.submittedTransactionId)
        Assert.assertArrayEquals(data.bitcoinBlockHeaderOfProof, returned.bitcoinBlockHeaderOfProof)
        Assert.assertEquals(2, returned.bitcoinContextBlocks.size.toLong())
        Assert.assertArrayEquals(data.bitcoinContextBlocks[0], returned.bitcoinContextBlocks[0])
        Assert.assertArrayEquals(data.bitcoinContextBlocks[1], returned.bitcoinContextBlocks[1])
        Assert.assertNull(returned.merklePath)
        Assert.assertEquals(2, returned.alternateBlocksOfProof.size.toLong())
    }

    @Test
    fun serializationWhenAlternateBlocksOfProofNull() {
        data.alternateBlocksOfProof = null
        val returned = serializeThenDeserialize()
        Assert.assertEquals(data.operationId, returned.operationId)
        Assert.assertEquals(data.status, returned.status)
        Assert.assertEquals(data.currentAction, returned.currentAction)
        Assert.assertArrayEquals(data.miningInstruction.endorsedBlockHeader, returned.miningInstruction.endorsedBlockHeader)
        Assert.assertArrayEquals(data.miningInstruction.lastBitcoinBlock, returned.miningInstruction.lastBitcoinBlock)
        Assert.assertEquals(data.miningInstruction.minerAddress, returned.miningInstruction.minerAddress)
        Assert.assertArrayEquals(data.miningInstruction.publicationData, returned.miningInstruction.publicationData)
        Assert.assertArrayEquals(data.transaction, returned.transaction)
        Assert.assertEquals(data.submittedTransactionId, returned.submittedTransactionId)
        Assert.assertArrayEquals(data.bitcoinBlockHeaderOfProof, returned.bitcoinBlockHeaderOfProof)
        Assert.assertEquals(2, returned.bitcoinContextBlocks.size.toLong())
        Assert.assertArrayEquals(data.bitcoinContextBlocks[0], returned.bitcoinContextBlocks[0])
        Assert.assertArrayEquals(data.bitcoinContextBlocks[1], returned.bitcoinContextBlocks[1])
        Assert.assertEquals(data.merklePath, returned.merklePath)
        Assert.assertNull(returned.alternateBlocksOfProof)
    }

    private fun serializeThenDeserialize(): PopMiningOperationStateDto {
        val serialized = ByteArrayOutputStream().use { dataOut ->
            ObjectOutputStream(dataOut).use { out ->
                out.writeObject(data)
            }
            dataOut.toByteArray()
        }
        return ByteArrayInputStream(serialized).use { dataIn ->
            ObjectInputStream(dataIn).use {
                it.readObject() as PopMiningOperationStateDto
            }
        }
    }
}
