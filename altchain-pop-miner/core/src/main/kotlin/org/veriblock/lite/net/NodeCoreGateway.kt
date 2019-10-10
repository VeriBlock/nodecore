// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.lite.net

import io.grpc.ManagedChannel
import io.grpc.StatusRuntimeException
import nodecore.api.grpc.AdminGrpc
import nodecore.api.grpc.AdminRpcConfiguration
import nodecore.api.grpc.VeriBlockMessages
import nodecore.api.grpc.utilities.ByteStringUtility
import nodecore.api.grpc.utilities.ChannelBuilder
import org.veriblock.lite.core.Balance
import org.veriblock.lite.core.BlockChainDelta
import org.veriblock.lite.core.FullBlock
import org.veriblock.lite.params.NetworkParameters
import org.veriblock.lite.serialization.deserialize
import org.veriblock.lite.serialization.deserializeStandardTransaction
import org.veriblock.sdk.*
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException

private val logger = createLogger {}

class NodeCoreGateway(
    private val params: NetworkParameters
) {
    private val channelBuilder: ChannelBuilder

    private val channel: ManagedChannel
    private val blockingStub: AdminGrpc.AdminBlockingStub

    init {
        val rpcConfiguration = configure(params)
        this.channelBuilder = ChannelBuilder(rpcConfiguration)

        try {
            channel = channelBuilder.buildManagedChannel()
            blockingStub = AdminGrpc.newBlockingStub(channelBuilder.attachPasswordInterceptor(channel))
                .withMaxInboundMessageSize(20 * 1024 * 1024)
                .withMaxOutboundMessageSize(20 * 1024 * 1024)
        } catch (e: SSLException) {
            logger.error(e.message, e)
            //InternalEventBus.getInstance().post(new ErrorMessageEvent("NodeCore SSL configuration error, see log file for detail"));
            throw e
        }
    }

    private fun configure(networkParameters: NetworkParameters): AdminRpcConfiguration = AdminRpcConfiguration().apply {
        isSsl = networkParameters.isSsl
        certificateChainPath = networkParameters.certificateChainPath
        nodeCoreHost = networkParameters.adminHost
        nodeCorePort = networkParameters.adminPort
        nodeCorePassword = networkParameters.adminPassword
    }

    private fun initialize() {
    }

    @Throws(InterruptedException::class)
    fun shutdown() {
        channel.shutdown().awaitTermination(15, TimeUnit.SECONDS)
    }

    fun getLastBlock(): VeriBlockBlock {
        //logger.trace { "Requesting last block..." }
        return try {
            val lastBlock = blockingStub
                .withDeadlineAfter(10, TimeUnit.SECONDS)
                .getLastBlock(VeriBlockMessages.GetLastBlockRequest.getDefaultInstance())
            lastBlock.header.deserialize(params)
        } catch (e: Exception) {
            logger.error("Unable to get last block", e)
            throw e
        }
    }

    fun getBlock(height: Int): FullBlock? {
        logger.debug { "Requesting block at height $height..." }
        val request = VeriBlockMessages.GetBlocksRequest.newBuilder()
            .addFilters(VeriBlockMessages.BlockFilter.newBuilder()
                .setIndex(height)
                .build())
            .build()

        val reply = blockingStub
            .withDeadlineAfter(5, TimeUnit.SECONDS)
            .getBlocks(request)
        if (reply.success && reply.blocksCount > 0) {
            val deserialized = reply.getBlocks(0).deserialize(params)
            return deserialized
        }

        return null
    }

    fun getBlock(hash: String): FullBlock? {
        logger.debug { "Requesting block with hash $hash..." }
        val request = VeriBlockMessages.GetBlocksRequest.newBuilder()
            .addFilters(VeriBlockMessages.BlockFilter.newBuilder()
                .setHash(ByteStringUtility.hexToByteString(hash))
                .build())
            .build()

        val reply = blockingStub
            .withDeadlineAfter(5, TimeUnit.SECONDS)
            .getBlocks(request)
        if (reply.success && reply.blocksCount > 0) {
            val deserialized = reply.getBlocks(0).deserialize(params)
            return deserialized
        }

        return null
    }

    fun getBalance(address: String): Balance {
        logger.debug { "Requesting balance for address $address..." }
        val request = VeriBlockMessages.GetBalanceRequest.newBuilder()
            .addAddresses(ByteStringUtility.base58ToByteString(address))
            .build()

        val reply = blockingStub
            .withDeadlineAfter(10, TimeUnit.SECONDS)
            .getBalance(request)
        if (reply.success) {
            return Balance(
                Coin.valueOf(reply.getConfirmed(0).unlockedAmount),
                Coin.valueOf(reply.getUnconfirmed(0).amount)
            )
        } else {
            error("Unable to retrieve balance from address $address")
        }
    }

    fun getDefaultAddress(): String {
        logger.debug { "Requesting default address..." }
        val request = VeriBlockMessages.GetSignatureIndexRequest.newBuilder().build()
        val reply = blockingStub
            .withDeadlineAfter(5, TimeUnit.SECONDS)
            .getSignatureIndex(request)
        return if (reply.success) {
            ByteStringUtility.byteStringToBase58(reply.getIndexes(0).address)
        } else {
            error("Unable to get default address: ${reply.resultsList.joinToString { "${it.code}: ${it.message}" }}")
        }
    }

    fun getVeriBlockPublications(keystoneHash: String, contextHash: String, btcContextHash: String): List<VeriBlockPublication> {
        logger.debug { "Requesting veriblock publications for keystone $keystoneHash..." }
        val request = VeriBlockMessages.GetVeriBlockPublicationsRequest
            .newBuilder()
            .setKeystoneHash(ByteStringUtility.hexToByteString(keystoneHash))
            .setContextHash(ByteStringUtility.hexToByteString(contextHash))
            .setBtcContextHash(ByteStringUtility.hexToByteString(btcContextHash))
            .build()

        val reply = blockingStub
            .withDeadlineAfter(30, TimeUnit.SECONDS)
            .getVeriBlockPublications(request)
        if (reply.success) {
            val publications = ArrayList<VeriBlockPublication>()
            for (pubMsg in reply.publicationsList) {
                publications.add(pubMsg.deserialize(params))
            }
            return publications
        }

        return emptyList()
    }

    fun ping(): Boolean {
        return try {
            blockingStub
                .withDeadlineAfter(5L, TimeUnit.SECONDS)
                .ping(VeriBlockMessages.PingRequest.newBuilder().build())
            true
        } catch (e: StatusRuntimeException) {
            logger.warn("Unable to connect ping NodeCore at this time")
            false
        }
    }

    fun listChangesSince(hash: String?): BlockChainDelta {
        logger.debug { "Requesting delta since hash $hash..." }
        val builder = VeriBlockMessages.ListBlocksSinceRequest.newBuilder()
        if (hash != null && hash.isNotEmpty()) {
            builder.hash = ByteStringUtility.hexToByteString(hash)
        }
        val reply = blockingStub
            .withDeadlineAfter(10, TimeUnit.SECONDS)
            .listBlocksSince(builder.build())
        val removed = reply.removedList.map { msg -> msg.deserialize(params) }
        val added = reply.addedList.map { msg -> msg.deserialize(params) }.toMutableList()
        return BlockChainDelta(removed, added)
    }

    fun submitEndorsementTransaction(publicationData: ByteArray): VeriBlockTransaction {
        logger.debug { "Submitting endorsement transaction..." }
        val request = VeriBlockMessages.SendAltChainEndorsementRequest
            .newBuilder()
            .setPublicationData(ByteStringUtility.bytesToByteString(publicationData))
            .build()

        val reply = blockingStub
            .withDeadlineAfter(5, TimeUnit.SECONDS)
            .sendAltChainEndorsement(request)
        if (!reply.success) {
            for (error in reply.resultsList) {
                logger.error { "${error.message} | ${error.details}" }
            }
            error("Unable to submit endorsement transaction (Publication Data: ${publicationData.toHex()})")
        }

        return reply.transaction.deserializeStandardTransaction(params)
    }
}
