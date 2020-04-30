// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.miners.pop.service

import com.google.protobuf.ByteString
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.CancellationException
import nodecore.api.grpc.AdminGrpc
import nodecore.api.grpc.VeriBlockMessages
import nodecore.api.grpc.utilities.ByteStringAddressUtility
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.extensions.asHexBytes
import org.veriblock.core.utilities.extensions.toBase58
import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.miners.pop.model.PopEndorsementInfo
import org.veriblock.miners.pop.model.PopMiningInstruction
import org.veriblock.miners.pop.model.PopMiningTransaction
import org.veriblock.miners.pop.model.VeriBlockHeader
import org.veriblock.miners.pop.model.result.Result
import java.util.Arrays
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException
import kotlin.math.abs

private val logger = createLogger {}

class NodeCoreGateway(
    private val channelBuilder: ChannelBuilder
) {
    private lateinit var channel: ManagedChannel
    private lateinit var blockingStub: AdminGrpc.AdminBlockingStub

    init {
        initializeClient()
    }

    private fun initializeClient() {
        try {
            channel = channelBuilder.buildManagedChannel()
            blockingStub = AdminGrpc.newBlockingStub(channelBuilder.attachPasswordInterceptor(channel))
        } catch (e: SSLException) {
            logger.error("NodeCore SSL configuration error", e)
        }
    }

    fun shutdown() {
        channel.shutdown().awaitTermination(15, TimeUnit.SECONDS)
    }

    fun ping(): Boolean {
        return if (!::blockingStub.isInitialized) {
            false
        } else try {
            blockingStub.withDeadlineAfter(5, TimeUnit.SECONDS).ping(VeriBlockMessages.PingRequest.newBuilder().build())
            true
        } catch (e: StatusRuntimeException) {
            logger.debug("Unable to connect ping NodeCore at this time")
            false
        }
    }

    /**
     * Verify if the connected NodeCore is synchronized with the network (the block difference between the networkHeight and the localBlockchainHeight
     * should be smaller than 4 blocks)
     *
     * This function might return false (StatusRuntimeException) if NodeCore is not accessible or if NodeCore still loading (networkHeight = 0)
     */
    fun isNodeCoreSynchronized(): Boolean {
        return if (!::blockingStub.isInitialized) {
            false
        } else try {
            val request = checkGrpcError {
                blockingStub
                    .withDeadlineAfter(5L, TimeUnit.SECONDS)
                    .getStateInfo(VeriBlockMessages.GetStateInfoRequest.newBuilder().build())
            }
            val blockDifference = abs(request.networkHeight - request.localBlockchainHeight)
            request.networkHeight > 0 && blockDifference < 4
        } catch (e: StatusRuntimeException) {
            logger.warn("Unable to perform GetStateInfoRequest to NodeCore")
            false
        }
    }

    fun getPop(blockNumber: Int?): PopMiningInstruction {
        val requestBuilder = VeriBlockMessages.GetPopRequest.newBuilder()
        if (blockNumber != null && blockNumber > 0) {
            requestBuilder.blockNum = blockNumber
        }
        val request = requestBuilder.build()
        val reply = checkGrpcError {
            blockingStub
                .withDeadlineAfter(15, TimeUnit.SECONDS)
                .getPop(request)
        }
        if (reply.success) {
            val publicationData = reply.fullPop.toByteArray()
            val instruction = PopMiningInstruction(
                publicationData = publicationData,
                minerAddressBytes = reply.popMinerAddress.toByteArray(),
                lastBitcoinBlock = reply.lastKnownBlock.header.toByteArray(),
                endorsedBlockHeader = Arrays.copyOfRange(publicationData, 0, 64),
                endorsedBlockContextHeaders = reply.lastKnownBlockContextList.map { it.header.toByteArray() }
            )
            return instruction
        } else {
            val message = StringBuilder()
            for (r in reply.resultsList) {
                if (r.message != null) {
                    message.append(r.message).append("\n")
                }
                if (r.details != null) {
                    message.append("\t").append(r.details).append("\n")
                }
            }
            error(message.toString())
        }
    }

    fun submitPop(popMiningTransaction: PopMiningTransaction): String {
        val blockOfProofBuilder = VeriBlockMessages.BitcoinBlockHeader.newBuilder()
        blockOfProofBuilder.header = ByteString.copyFrom(popMiningTransaction.bitcoinBlockHeaderOfProof)
        val request = VeriBlockMessages.SubmitPopRequest.newBuilder().apply {
            endorsedBlockHeader = ByteString.copyFrom(popMiningTransaction.endorsedBlockHeader)
            bitcoinTransaction = ByteString.copyFrom(popMiningTransaction.bitcoinTransaction)
            bitcoinMerklePathToRoot = ByteString.copyFrom(popMiningTransaction.bitcoinMerklePathToRoot)
            setBitcoinBlockHeaderOfProof(blockOfProofBuilder)
            address = ByteString.copyFrom(popMiningTransaction.popMinerAddress)
            for (contextBlockHeader in popMiningTransaction.bitcoinContextBlocks) {
                val header = VeriBlockMessages.BitcoinBlockHeader.newBuilder().apply {
                    header = ByteString.copyFrom(contextBlockHeader)
                }.build()
                addContextBitcoinBlockHeaders(header)
            }
        }.build()
        val reply = blockingStub.submitPop(request)
        if (reply.success) {
            return reply.getResults(0).details
        }
        throw PopSubmitRejected()
    }

    fun getTransactionConfirmationsById(txId: String): Int? {
        val request = VeriBlockMessages.GetTransactionsRequest.newBuilder().apply {
            addIds(ByteString.copyFrom(txId.asHexBytes()))
        }.build()
        val reply = checkGrpcError {
            blockingStub
                .withDeadlineAfter(90, TimeUnit.SECONDS)
                .getTransactions(request)
        }

        return reply.transactionsList.firstOrNull()?.confirmations
    }

    fun getPopEndorsementInfo(): List<PopEndorsementInfo> {
        val request = VeriBlockMessages.GetPoPEndorsementsInfoRequest.newBuilder().apply {
            searchLength = 750
        }.build()
        val reply = blockingStub.getPoPEndorsementsInfo(request)
        return reply.popEndorsementsList.map {
            PopEndorsementInfo(it)
        }
    }

    fun getBitcoinBlockIndex(blockHeader: ByteArray): Int? {
        val request = VeriBlockMessages.GetBitcoinBlockIndexRequest.newBuilder().apply {
            setBlockHeader(ByteString.copyFrom(blockHeader))
            searchLength = 20
        }.build()
        val reply = blockingStub.getBitcoinBlockIndex(request)
        return if (reply.success && reply.resultsCount > 0) {
            reply.getResults(0).details.toInt()
        } else null
    }

    fun getMinerAddress(): String {
        val request = VeriBlockMessages.GetInfoRequest.newBuilder().build()
        val reply = blockingStub.getInfo(request)
        return reply.defaultAddress.address.toByteArray().toBase58()
    }

    fun getLastBlock(): VeriBlockHeader {
        val reply = checkGrpcError {
            blockingStub
                .withDeadlineAfter(10, TimeUnit.SECONDS)
                .getLastBlock(VeriBlockMessages.GetLastBlockRequest.newBuilder().build())
        }
        return VeriBlockHeader(reply.header.header.toByteArray())
    }

    fun getBlockHash(height: Int): String? {
        val request = VeriBlockMessages.GetBlocksRequest.newBuilder().addFilters(
            VeriBlockMessages.BlockFilter.newBuilder().setIndex(height)
        ).build()

        val reply = checkGrpcError {
            blockingStub
                .withDeadlineAfter(10, TimeUnit.SECONDS)
                .getBlocks(request)
        }
        if (reply.success && reply.blocksCount > 0) {
            val deserialized = reply.getBlocks(0)
            return deserialized.hash.toByteArray().toHex()
        }

        return null
    }

    fun unlockWallet(passphrase: String?): Result {
        val request = VeriBlockMessages.UnlockWalletRequest.newBuilder().setPassphrase(
            passphrase
        ).build()
        val protocolReply = blockingStub.unlockWallet(request)
        val result = Result()
        if (!protocolReply.success) {
            result.fail()
        }
        for (r in protocolReply.resultsList) {
            result.addMessage(r.code, r.message, r.details, r.error)
        }
        return result
    }

    fun lockWallet(): Result {
        val request = VeriBlockMessages.LockWalletRequest.newBuilder().build()
        val protocolReply = blockingStub.lockWallet(request)
        val result = Result()
        if (!protocolReply.success) {
            result.fail()
        }
        for (r in protocolReply.resultsList) {
            result.addMessage(r.code, r.message, r.details, r.error)
        }
        return result
    }

    fun getPopEstimates(keystonesToSearch: Int): List<VeriBlockMessages.RewardEstimate> {
        val request = VeriBlockMessages.GetPoPRewardEstimatesRequest.newBuilder().apply {
            this.keystonesToSearch = keystonesToSearch
        }.build()
        val reply = checkGrpcError {
            blockingStub
                .withDeadlineAfter(15, TimeUnit.SECONDS)
                .getPoPRewardEstimates(request)
        }
        if (reply.success) {
            return reply.rewardEstimatesList
        } else {
            val message = StringBuilder()
            for (r in reply.resultsList) {
                if (r.message != null) {
                    message.append(r.message).append("\n")
                }
                if (r.details != null) {
                    message.append("\t").append(r.details).append("\n")
                }
            }
            error(message.toString())
        }
    }

    fun sendCoins(address: String, amount: Long, takeFeeFromOutputs: Boolean): List<String> {
        val request = VeriBlockMessages.SendCoinsRequest.newBuilder().apply {
            addAmounts(VeriBlockMessages.Output.newBuilder().apply {
                this.address = ByteStringAddressUtility.createProperByteStringAutomatically(address)
                this.amount = amount
            })
            this.takeFeeFromOutputs = takeFeeFromOutputs
        }.build()
        val reply = checkGrpcError {
            blockingStub
                .withDeadlineAfter(15, TimeUnit.SECONDS)
                .sendCoins(request)
        }
        if (reply.success) {
            return reply.txIdsList.map { it.toByteArray().toHex() }
        } else {
            val message = StringBuilder()
            for (r in reply.resultsList) {
                if (r.message != null) {
                    message.append(r.message).append("\n")
                }
                if (r.details != null) {
                    message.append("\t").append(r.details).append("\n")
                }
            }
            error(message.toString())
        }
    }

    private inline fun <T> checkGrpcError(block: () -> T): T = try {
        block()
    } catch (e: StatusRuntimeException) {
        when (e.status.code) {
            Status.Code.DEADLINE_EXCEEDED ->
                throw TimeoutError(e.message ?: "")
            Status.Code.CANCELLED ->
                throw CancellationException(e.message ?: "")
            else ->
                throw e
        }
    }
}

class PopSubmitRejected : RuntimeException("PoP submission rejected")

class TimeoutError(
    override val message: String
) : RuntimeException()
