// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.alt.plugins

import com.github.kittinunf.fuel.httpPost
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.extensions.asHexBytes
import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.sdk.alt.ChainConfig
import org.veriblock.sdk.alt.PluginSpec
import org.veriblock.sdk.alt.PublicationDataWithContext
import org.veriblock.sdk.alt.SecurityInheritingChain
import org.veriblock.sdk.alt.model.SecurityInheritingBlock
import org.veriblock.sdk.alt.model.SecurityInheritingTransaction
import org.veriblock.sdk.alt.model.SecurityInheritingTransactionVout
import org.veriblock.sdk.models.AltPublication
import org.veriblock.sdk.models.PublicationData
import org.veriblock.sdk.models.VeriBlockPublication
import org.veriblock.sdk.services.SerializeDeserializeService
import java.util.TreeMap
import kotlin.random.Random

private val logger = createLogger {}

class TestConfig(
    override val host: String = "http://localhost:10600/api",
    override val keystonePeriod: Int = 1,
    override val blockRoundIndices: IntArray = intArrayOf(1),
    val autoMinePeriod: Int? = null
) : ChainConfig()

//private data class VbkInfo(
//    val lastBlock: VbkBlockData
//)

//private class VbkBlockData(
//    val hash: String,
//    val number: Int
//)

private class BlockHeaderContainer(
    val header: BlockHeader
)

private class BlockHeader(
    val hash: String
    //val header: String
)

private class BtcBlockData(
    val hash: String
    //val height: Int,
    //val header: String
)

@PluginSpec(name = "Test", key = "test")
class TestChain(
    override val config: TestConfig
) : SecurityInheritingChain {

    private val operations = HashMap<String, String>()
    private val blocks = TreeMap<Int, SecurityInheritingBlock>()
    private val transactions = HashMap<String, SecurityInheritingTransaction>()

    private val minerAddress = "give it to me".toByteArray().toHex()

    init {
        config.checkValidity()
    }

    //private fun getInfo(): VbkInfo = config.host.httpPost()
    //    .body(JsonRpcRequestBody("getinfo", Any()).toJson())
    //    .rpcResponse()

    private fun getLastBitcoinBlockHash() = config.host.httpPost()
        .body(JsonRpcRequestBody("getlastbitcoinblock", Any()).toJson())
        .rpcResponse<BtcBlockData>().hash

    private fun getLastBlockHash() = config.host.httpPost()
        .body(JsonRpcRequestBody("getlastblock", Any()).toJson())
        .rpcResponse<BlockHeaderContainer>().header.hash

    override val id get() = -1L
    override val key get() = "test"
    override val name get() = "Test"

    override fun shouldAutoMine(): Boolean {
        return config.autoMinePeriod != null
    }

    override fun shouldAutoMine(blockHeight: Int): Boolean {
        return config.autoMinePeriod != null && blockHeight % config.autoMinePeriod == 0
    }

    override fun getBestBlockHeight(): Int {
        val expectedHeight = (System.currentTimeMillis() / 10000).toInt()
        // "New block" every 10 seconds
        if (blocks.isEmpty() || blocks.lastKey() < expectedHeight) {
            createBlock(expectedHeight, minerAddress).height
        }
        return expectedHeight
    }

    override fun getBlock(hash: String): SecurityInheritingBlock? {
        return blocks.values.find { it.hash == hash }
    }

    override fun getBlock(height: Int): SecurityInheritingBlock? {
        if (height > getBestBlockHeight()) {
            return null
        }
        return blocks.getOrPut(height) {
            createBlock(height, minerAddress)
        }
    }

    override fun checkBlockIsOnMainChain(height: Int, blockHeaderToCheck: ByteArray): Boolean {
        val block = blocks[height]
            ?: return false

        return blockHeaderToCheck.toHex().startsWith(block.hash)
    }

    override fun getTransaction(txId: String): SecurityInheritingTransaction? {
        return transactions[txId]
    }

    override fun getPayoutInterval(): Int {
        return 100
    }

    override fun getPublicationData(blockHeight: Int?): PublicationDataWithContext {
        logger.debug { "Retrieving last known blocks from NodeCore at ${config.host}..." }
        val lastVbkHash = getLastBlockHash().asHexBytes()
        val lastBtcHash = getLastBitcoinBlockHash().asHexBytes()

        val finalBlockHeight = blockHeight ?: getBestBlockHeight()
        if (blocks.isEmpty()) {
            // If there are no blocks, trigger their creation
            getBestBlockHeight()
        }

        val header = blocks[finalBlockHeight]!!.hash + Random.nextBytes(64 - 16).toHex()
        val context = Random.nextBytes(100)
        operations[header] = context.toHex()
        val publicationData = PublicationData(
            id,
            header.asHexBytes(),
            minerAddress.asHexBytes(),
            context
        )
        return PublicationDataWithContext(finalBlockHeight, publicationData, listOf(lastVbkHash), listOf(lastBtcHash))
    }

    override fun submit(proofOfProof: AltPublication, veriBlockPublications: List<VeriBlockPublication>): String {
        val publicationData = proofOfProof.transaction.publicationData
        val publicationDataHeader = publicationData.header.toHex()
        val publicationDataContextInfo = publicationData.contextInfo.toHex()
        val expectedContextInfo = operations[publicationDataHeader]
            ?: error("Couldn't find operation with initial header $publicationDataHeader")
        if (publicationDataContextInfo != expectedContextInfo) {
            error("Expected publication data context differs from the one PoP supplied back")
        }
        val block = createBlock((System.currentTimeMillis() / 10000).toInt(), minerAddress)
        return block.coinbaseTransactionId
    }

    override fun updateContext(veriBlockPublications: List<VeriBlockPublication>): String {
        logger.info {
            """Update context called with the following data:
            |ATVs: ${veriBlockPublications.map {
                it.transaction
            }.flatMap {
                it.blocks
            }.map {
                SerializeDeserializeService.getHeaderBytesBitcoinBlock(it).toHex()
            }}
            |VTBs: ${veriBlockPublications.flatMap {
                it.blocks
            }.map {
                SerializeDeserializeService.serializeHeaders(it).toHex()
            }}""".trimMargin()
        }
        return ""
    }

    private fun createBlock(height: Int, minerAddress: String = ""): SecurityInheritingBlock {
        val hash = Random.nextBytes(16).toHex()
        val coinbase = createTransaction(Random.nextDouble(10.0, 100.0), minerAddress)
        val block = SecurityInheritingBlock(
            hash,
            height,
            100,
            0,
            0,
            "",
            0.0,
            coinbase.txId,
            listOf()
        )
        blocks[block.height] = block
        return block
    }

    private fun createTransaction(
        amount: Double,
        receiver: String
    ): SecurityInheritingTransaction {
        val transaction = SecurityInheritingTransaction(
            Random.nextBytes(22).toHex(),
            100,
            listOf(
                SecurityInheritingTransactionVout(amount, receiver)
            )
        )
        transactions[transaction.txId] = transaction
        return transaction
    }

    override fun isConnected(): Boolean = true

    override fun isSynchronized(): Boolean = true
}
