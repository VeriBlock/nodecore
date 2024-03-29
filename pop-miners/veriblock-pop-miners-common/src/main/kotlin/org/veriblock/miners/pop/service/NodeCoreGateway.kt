// VeriBlock PoP Miner
// Copyright 2017-2021 Xenios SEZC
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
import nodecore.api.grpc.RpcBitcoinBlockHeader
import nodecore.api.grpc.RpcBlockFilter
import nodecore.api.grpc.RpcGetBitcoinBlockIndexRequest
import nodecore.api.grpc.RpcGetBlocksRequest
import nodecore.api.grpc.RpcGetInfoRequest
import nodecore.api.grpc.RpcGetLastBlockRequest
import nodecore.api.grpc.RpcGetPopEndorsementsInfoRequest
import nodecore.api.grpc.RpcGetPopRewardEstimatesRequest
import nodecore.api.grpc.RpcGetPopRequest
import nodecore.api.grpc.RpcGetStateInfoRequest
import nodecore.api.grpc.RpcGetTransactionsRequest
import nodecore.api.grpc.RpcLockWalletRequest
import nodecore.api.grpc.RpcOutput
import nodecore.api.grpc.RpcPingRequest
import nodecore.api.grpc.RpcRewardEstimate
import nodecore.api.grpc.RpcSendCoinsRequest
import nodecore.api.grpc.RpcSubmitPopRequest
import nodecore.api.grpc.RpcUnlockWalletRequest
import org.veriblock.sdk.extensions.ByteStringAddressUtility
import org.veriblock.core.utilities.BlockUtility
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.extensions.asHexBytes
import org.veriblock.core.utilities.extensions.toBase58
import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.miners.pop.model.PopEndorsementInfo
import org.veriblock.miners.pop.model.PopMiningInstruction
import org.veriblock.miners.pop.model.PopMiningTransaction
import org.veriblock.miners.pop.model.VeriBlockHeader
import org.veriblock.miners.pop.model.result.Result
import org.veriblock.sdk.models.StateInfo
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
            blockingStub.withDeadlineAfter(5, TimeUnit.SECONDS).ping(RpcPingRequest.newBuilder().build())
            true
        } catch (e: StatusRuntimeException) {
            logger.debug("Unable to connect ping NodeCore at this time")
            false
        }
    }

    /**
     * Retrieve the 'state info' from NodeCore
     * This function will return an empty StateInfo if NodeCore is not accessible or if NodeCore still loading (networkHeight = 0)
     */
    fun getNodeCoreStateInfo(): StateInfo {
        return try {
            val request = checkGrpcError {
                blockingStub
                    .withDeadlineAfter(5L, TimeUnit.SECONDS)
                    .getStateInfo(RpcGetStateInfoRequest.newBuilder().build())
            }

            val blockDifference = abs(request.networkHeight - request.localBlockchainHeight)
            StateInfo(
                networkHeight = request.networkHeight,
                localBlockchainHeight = request.localBlockchainHeight,
                blockDifference = blockDifference,
                isSynchronized = request.networkHeight > 0 && blockDifference < 4,
                networkVersion = request.networkVersion,
                programVersion = request.programVersion
            )
        } catch (e: StatusRuntimeException) {
            logger.warn("Unable to perform the GetStateInfoRequest request to NodeCore (is it reachable?)")
            StateInfo()
        }
    }

    fun getPop(blockNumber: Int?): PopMiningInstruction {
        val requestBuilder = RpcGetPopRequest.newBuilder()
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
                endorsedBlockHeader = Arrays.copyOfRange(publicationData, 0, BlockUtility.getBlockHeaderLength(reply.blockHeight)),
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
        val blockOfProofBuilder = RpcBitcoinBlockHeader.newBuilder()
        blockOfProofBuilder.header = ByteString.copyFrom(popMiningTransaction.bitcoinBlockHeaderOfProof)
        val request = RpcSubmitPopRequest.newBuilder().apply {
            endorsedBlockHeader = ByteString.copyFrom(popMiningTransaction.endorsedBlockHeader)
            bitcoinTransaction = ByteString.copyFrom(popMiningTransaction.bitcoinTransaction)
            bitcoinMerklePathToRoot = ByteString.copyFrom(popMiningTransaction.bitcoinMerklePathToRoot)
            setBitcoinBlockHeaderOfProof(blockOfProofBuilder)
            address = ByteString.copyFrom(popMiningTransaction.popMinerAddress)
            for (contextBlockHeader in popMiningTransaction.bitcoinContextBlocks) {
                val header = RpcBitcoinBlockHeader.newBuilder().apply {
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
        val request = RpcGetTransactionsRequest.newBuilder().apply {
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
        val request = RpcGetPopEndorsementsInfoRequest.newBuilder().apply {
            searchLength = 750
        }.build()
        val reply = blockingStub.getPopEndorsementsInfo(request)
        return reply.popEndorsementsList.map {
            PopEndorsementInfo(it)
        }
    }

    fun getBitcoinBlockIndex(blockHeader: ByteArray): Int? {
        val request = RpcGetBitcoinBlockIndexRequest.newBuilder().apply {
            setBlockHeader(ByteString.copyFrom(blockHeader))
            searchLength = 20
        }.build()
        val reply = blockingStub.getBitcoinBlockIndex(request)
        return if (reply.success && reply.resultsCount > 0) {
            reply.getResults(0).details.toInt()
        } else null
    }

    fun getMinerAddress(): String {
        val request = RpcGetInfoRequest.newBuilder().build()
        val reply = blockingStub.getInfo(request)
        return reply.defaultAddress.address.toByteArray().toBase58()
    }

    fun getLastBlock(): VeriBlockHeader {
        val reply = checkGrpcError {
            blockingStub
                .withDeadlineAfter(10, TimeUnit.SECONDS)
                .getLastBlock(RpcGetLastBlockRequest.newBuilder().build())
        }
        return VeriBlockHeader(reply.header.header.toByteArray())
    }

    fun getBlockHash(height: Int): String? {
        val request = RpcGetBlocksRequest.newBuilder().addFilters(
            RpcBlockFilter.newBuilder().setIndex(height)
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
        val request = RpcUnlockWalletRequest.newBuilder().setPassphrase(
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
        val request = RpcLockWalletRequest.newBuilder().build()
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

    fun getPopEstimates(keystonesToSearch: Int): List<RpcRewardEstimate> {
        val request = RpcGetPopRewardEstimatesRequest.newBuilder().apply {
            this.keystonesToSearch = keystonesToSearch
        }.build()
        val reply = checkGrpcError {
            blockingStub
                .withDeadlineAfter(15, TimeUnit.SECONDS)
                .getPopRewardEstimates(request)
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
        val request = RpcSendCoinsRequest.newBuilder().apply {
            addAmounts(RpcOutput.newBuilder().apply {
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
