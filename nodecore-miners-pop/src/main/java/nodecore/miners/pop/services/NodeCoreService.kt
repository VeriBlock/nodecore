// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.miners.pop.services

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.google.protobuf.ByteString
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import nodecore.api.grpc.AdminGrpc
import nodecore.api.grpc.VeriBlockMessages
import nodecore.miners.pop.EventBus
import nodecore.miners.pop.NewVeriBlockFoundEventDto
import nodecore.miners.pop.VpmConfig
import nodecore.miners.pop.common.Utility
import nodecore.miners.pop.model.ApplicationExceptions.PoPSubmitRejected
import nodecore.miners.pop.model.BlockStore
import nodecore.miners.pop.model.NodeCoreReply
import nodecore.miners.pop.model.PopEndorsementInfo
import nodecore.miners.pop.model.PopMiningInstruction
import nodecore.miners.pop.model.PopMiningTransaction
import nodecore.miners.pop.model.VeriBlockHeader
import nodecore.miners.pop.model.result.Result
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.extensions.asHexBytes
import org.veriblock.core.utilities.extensions.toHex
import java.util.Arrays
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.SSLException
import kotlin.math.abs

private val logger = createLogger {}

class NodeCoreService(
    private val config: VpmConfig,
    private val channelBuilder: ChannelBuilder,
    private val blockStore: BlockStore
) {
    private val healthy = AtomicBoolean(false)
    private val synchronized = AtomicBoolean(false)

    private lateinit var channel: ManagedChannel
    private lateinit var blockingStub: AdminGrpc.AdminBlockingStub
    private val scheduledExecutorService: ScheduledExecutorService

    init {
        initializeClient()
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(
            ThreadFactoryBuilder().setNameFormat("nc-poll").build()
        )
        scheduledExecutorService.scheduleWithFixedDelay({ poll() }, 5, 1, TimeUnit.SECONDS)
    }

    private fun initializeClient() {
        logger.info(
            "Connecting to NodeCore at {}:{} {}", config.nodeCoreRpc.host, config.nodeCoreRpc.port,
            if (config.nodeCoreRpc.ssl) "over SSL" else ""
        )
        try {
            channel = channelBuilder.buildManagedChannel()
            blockingStub = AdminGrpc.newBlockingStub(channelBuilder.attachPasswordInterceptor(channel))
        } catch (e: SSLException) {
            logger.error("NodeCore SSL configuration error", e)
        }
    }

    private fun isHealthy(): Boolean {
        return healthy.get()
    }

    private fun isSynchronized(): Boolean =
        synchronized.get()

    fun shutdown() {
        scheduledExecutorService.shutdown()
        channel.shutdown().awaitTermination(15, TimeUnit.SECONDS)
    }

    private fun ping(): Boolean {
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
    private fun isNodeCoreSynchronized(): Boolean {
        return if (!::blockingStub.isInitialized) {
            false
        } else try {
            val request = checkTimeoutError {
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

    fun getPop(blockNumber: Int?): NodeCoreReply<PopMiningInstruction> {
        val requestBuilder = VeriBlockMessages.GetPopRequest.newBuilder()
        if (blockNumber != null && blockNumber > 0) {
            requestBuilder.blockNum = blockNumber
        }
        val request = requestBuilder.build()
        val reply = checkTimeoutError {
            blockingStub
                .withDeadlineAfter(15, TimeUnit.SECONDS)
                .getPop(request)
        }
        val result = NodeCoreReply<PopMiningInstruction>()
        if (reply.success) {
            result.success = true
            val publicationData = reply.fullPop.toByteArray()
            val instruction = PopMiningInstruction(
                publicationData = publicationData,
                minerAddressBytes = reply.popMinerAddress.toByteArray(),
                lastBitcoinBlock = reply.lastKnownBlock.header.toByteArray(),
                endorsedBlockHeader = Arrays.copyOfRange(publicationData, 0, 64),
                endorsedBlockContextHeaders = reply.lastKnownBlockContextList.map { it.header.toByteArray() }
            )
            result.result = instruction
        } else {
            result.success = false
            val message = StringBuilder()
            for (r in reply.resultsList) {
                if (r.message != null) {
                    message.append(r.message).append("\n")
                }
                if (r.details != null) {
                    message.append("\t").append(r.details).append("\n")
                }
            }
            result.resultMessage = message.toString()
        }
        return result
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
        throw PoPSubmitRejected()
    }

    fun getTransactionConfirmationsById(txId: String): Int? {
        val request = VeriBlockMessages.GetTransactionsRequest.newBuilder().apply {
            addIds(ByteString.copyFrom(txId.asHexBytes()))
        }.build()
        val reply = checkTimeoutError {
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

    fun getBitcoinBlockIndex(blockHeader: ByteArray?): Int? {
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
        return Utility.bytesToBase58(reply.defaultAddress.address.toByteArray())
    }

    private fun getLastBlock(): VeriBlockHeader {
        val reply = checkTimeoutError {
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

        val reply = checkTimeoutError {
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

    private fun poll() {
        try {
            if (isHealthy() && isSynchronized()) {
                if (!isNodeCoreSynchronized()) {
                    synchronized.set(false)
                    logger.info("The connected node is not synchronized")
                    EventBus.nodeCoreDesynchronizedEvent.trigger()
                    return
                }
                val latestBlock = try {
                    getLastBlock()
                } catch (e: Exception) {
                    logger.error("Unable to get the last block from NodeCore")
                    healthy.set(false)
                    EventBus.nodeCoreUnhealthyEvent.trigger()
                    return
                }
                val chainHead = blockStore.getChainHead()
                if (latestBlock != chainHead) {
                    blockStore.setChainHead(latestBlock)
                    EventBus.newVeriBlockFoundEvent.trigger(NewVeriBlockFoundEventDto(latestBlock, chainHead))
                }
            } else {
                if (ping()) {
                    if (!isHealthy()) {
                        logger.info("Connected to NodeCore")
                        EventBus.nodeCoreHealthyEvent.trigger()
                    }
                    healthy.set(true)
                    if (isNodeCoreSynchronized()) {
                        if (!isSynchronized()) {
                            logger.info("The connected node is synchronized")
                            EventBus.nodeCoreSynchronizedEvent.trigger()
                        }
                        synchronized.set(true)
                    } else {
                        if (isSynchronized()) {
                            logger.info("The connected node is not synchronized")
                            EventBus.nodeCoreDesynchronizedEvent.trigger()
                        }
                        synchronized.set(false)
                    }
                } else {
                    if (isHealthy()) {
                        EventBus.nodeCoreUnhealthyEvent.trigger()
                    }
                    if (isSynchronized()) {
                        logger.info("The connected node is not synchronized")
                        EventBus.nodeCoreDesynchronizedEvent.trigger()
                    }
                    healthy.set(false)
                    synchronized.set(false)
                }
            }
        } catch (e: Exception) {
            logger.error("Error while polling NodeCore", e)
        }
    }

    private inline fun <T> checkTimeoutError(block: () -> T): T = try {
        block()
    } catch (e: StatusRuntimeException) {
        if (e.status.code == Status.Code.DEADLINE_EXCEEDED) {
            throw TimeoutError(e.message ?: "")
        } else {
            throw e
        }
    }
}

class TimeoutError(
    override val message: String
) : RuntimeException()
