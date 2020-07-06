// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.alt.plugins.test

import io.ktor.client.request.post
import org.veriblock.alt.plugins.createHttpClient
import org.veriblock.alt.plugins.util.JsonRpcRequestBody
import org.veriblock.alt.plugins.util.RpcResponse
import org.veriblock.alt.plugins.util.handle
import org.veriblock.alt.plugins.util.toJson
import org.veriblock.core.altchain.AltchainPoPEndorsement
import org.veriblock.core.contracts.BlockEndorsement
import org.veriblock.core.contracts.BlockEndorsementHash
import org.veriblock.core.utilities.Utility
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.extensions.asHexBytes
import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.sdk.alt.ApmInstruction
import org.veriblock.sdk.alt.SecurityInheritingChain
import org.veriblock.sdk.alt.model.SecurityInheritingBlock
import org.veriblock.sdk.alt.model.SecurityInheritingTransaction
import org.veriblock.sdk.alt.model.SecurityInheritingTransactionVout
import org.veriblock.sdk.alt.plugin.PluginConfig
import org.veriblock.sdk.alt.plugin.PluginSpec
import org.veriblock.sdk.models.AltPublication
import org.veriblock.sdk.models.PublicationData
import org.veriblock.sdk.models.StateInfo
import org.veriblock.sdk.models.VeriBlockPublication
import java.util.TreeMap
import kotlin.random.Random

private val logger = createLogger {}

@PluginSpec(name = "Test", key = "test")
class TestChain(
    override val key: String,
    configuration: PluginConfig
) : SecurityInheritingChain {

    override val config = TestConfig(configuration)

    override val id get() = -1L
    override val name get() = "Test"

    private val operations = HashMap<String, String>()
    private val blocks = TreeMap<Int, TestBlock>()
    private val transactions = HashMap<String, SecurityInheritingTransaction>()
    private val publishedAtvs = ArrayList<AltPublication>()

    private val startingHeight = (System.currentTimeMillis() / 10000).toInt() - 20

    private val httpClient = createHttpClient()

    init {
        config.checkValidity()
    }

    private suspend fun getLastBitcoinBlockHash() = httpClient.post<RpcResponse>(config.host) {
        body = JsonRpcRequestBody("getlastbitcoinblock", Any()).toJson()
    }.handle<BtcBlockData>().hash

    private suspend fun getLastBlockHash() = httpClient.post<RpcResponse>(config.host) {
        body = JsonRpcRequestBody("getlastblock", Any()).toJson()
    }.handle<BlockHeaderContainer>().header.hash

    override fun shouldAutoMine(): Boolean {
        return config.autoMinePeriod != null
    }

    override fun shouldAutoMine(blockHeight: Int): Boolean {
        return config.autoMinePeriod != null && blockHeight % config.autoMinePeriod == 0
    }

    override suspend fun getBestBlockHeight(): Int {
        val expectedHeight = (System.currentTimeMillis() / 10000).toInt()
        // "New block" every 10 seconds
        if (blocks.isEmpty() || blocks.lastKey() < expectedHeight) {
            createBlock(expectedHeight).data.height
        }
        return expectedHeight
    }

    override suspend fun getBlock(hash: String): SecurityInheritingBlock? {
        return blocks.values.find { it.data.hash == hash }?.data
    }

    override suspend fun getBlock(height: Int): SecurityInheritingBlock? {
        if (height > getBestBlockHeight()) {
            return null
        }
        return blocks.getOrPut(height) {
            createBlock(height)
        }.data
    }

    override suspend fun checkBlockIsOnMainChain(height: Int, blockHeaderToCheck: ByteArray): Boolean {
        val block = blocks[height]
            ?: return false

        return blockHeaderToCheck.toHex().contains(block.data.hash)
    }

    override suspend fun getTransaction(txId: String): SecurityInheritingTransaction? {
        return transactions[txId]
    }

    override fun getPayoutInterval(): Int {
        return config.payoutInterval
    }

    override suspend fun getMiningInstruction(blockHeight: Int?): ApmInstruction {
        logger.debug { "Retrieving last known blocks from NodeCore at ${config.host}..." }
        val lastVbkHash = getLastBlockHash().asHexBytes()
        val lastBtcHash = getLastBitcoinBlockHash().asHexBytes()

        val finalBlockHeight = blockHeight ?: getBestBlockHeight()
        if (blocks.isEmpty()) {
            // If there are no blocks, trigger their creation
            getBestBlockHeight()
        }

        val endorsedBlock = blocks[finalBlockHeight]!!
        val header = endorsedBlock.hash
        val context = endorsedBlock.hash + endorsedBlock.previousBlock.hash +
            endorsedBlock.previousKeystone.hash + endorsedBlock.secondPreviousKeystone.hash
        operations[header] = context
        val publicationData = PublicationData(
            id,
            header.asHexBytes(),
            config.payoutAddress.asHexBytes(),
            context.asHexBytes()
        )
        return ApmInstruction(finalBlockHeight, publicationData, listOf(lastVbkHash), listOf(lastBtcHash))
    }

    override suspend fun submit(proofOfProof: AltPublication, veriBlockPublications: List<VeriBlockPublication>): String {
        val publicationData = proofOfProof.transaction.publicationData
            ?: error("Proof of proof does not have publication data!")
        val publicationDataHeader = publicationData.header.toHex()
        val publicationDataContextInfo = publicationData.contextInfo.toHex()
        val expectedContextInfo = operations[publicationDataHeader]
            ?: error("Couldn't find operation with initial header $publicationDataHeader")
        if (publicationDataContextInfo != expectedContextInfo) {
            error("Expected publication data context differs from the one PoP supplied back")
        }
        val block = createBlock((System.currentTimeMillis() / 10000).toInt())
        publishedAtvs += proofOfProof
        return block.data.coinbaseTransactionId
    }

    override fun extractAddressDisplay(addressData: ByteArray): String {
        return String(addressData)
    }

    override fun extractBlockEndorsement(altchainPopEndorsement: AltchainPoPEndorsement): BlockEndorsement {
        val context = altchainPopEndorsement.getContextInfo()
        val hash = context.copyOfRange(0, 4)
        return BlockEndorsement(
            Utility.byteArrayToInt(hash),
            BlockEndorsementHash(hash.toHex()),
            BlockEndorsementHash(context.copyOfRange(4, 8).toHex()),
            BlockEndorsementHash(context.copyOfRange(8, 12).toHex()),
            BlockEndorsementHash(context.copyOfRange(12, 16).toHex())
        )
    }

    private fun createBlock(height: Int): TestBlock {
        val hash = Utility.intToByteArray(height).toHex()
        val coinbase = createTransaction(hash, config.payoutAddress)
        val previousBlockHeight = height - 1
        val previousBlock = if (previousBlockHeight > startingHeight) {
            blocks[previousBlockHeight] ?: createBlock(previousBlockHeight)
        } else {
            null
        }
        val keystoneOffset = if (height % config.keystonePeriod <= 1) {
            config.keystonePeriod
        } else {
            0
        }
        val previousKeystoneHeight = height -
            height % config.keystonePeriod -
            keystoneOffset
        val previousKeystone = if (previousKeystoneHeight > startingHeight) {
            blocks[previousKeystoneHeight] ?: createBlock(previousKeystoneHeight)
        } else {
            null
        }
        val secondPreviousKeystoneHeight = previousKeystoneHeight - config.keystonePeriod
        val secondPreviousKeystone = if (secondPreviousKeystoneHeight > startingHeight) {
            blocks[secondPreviousKeystoneHeight] ?: createBlock(secondPreviousKeystoneHeight)
        } else {
            null
        }

        val blockData = SecurityInheritingBlock(
            hash,
            height,
            previousBlock.hash,
            100,
            0,
            0,
            "",
            0.0,
            coinbase.txId,
            listOf(),
            publishedAtvs.map { it.getId().toHex() },
            previousKeystone.hash,
            secondPreviousKeystone.hash
        )

        publishedAtvs.clear()

        val block = TestBlock(blockData, previousBlock, previousKeystone, secondPreviousKeystone)
        blocks[height] = block
        return block
    }

    private fun createTransaction(
        blockHash: String,
        receiver: String
    ): SecurityInheritingTransaction {
        val transaction = SecurityInheritingTransaction(
            Random.nextBytes(22).toHex(),
            100,
            listOf(
                SecurityInheritingTransactionVout(20__000_000_00, receiver)
            ),
            blockHash
        )
        transactions[transaction.txId] = transaction
        return transaction
    }

    override suspend fun getBlockChainInfo(): StateInfo = StateInfo(50, 50, 0, true, false, "testnet")
}


private class TestBlock(
    val data: SecurityInheritingBlock,
    val previousBlock: TestBlock?,
    val previousKeystone: TestBlock?,
    val secondPreviousKeystone: TestBlock?
)

private val TestBlock?.hash get() = if (this != null) data.hash else "00000000"

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
