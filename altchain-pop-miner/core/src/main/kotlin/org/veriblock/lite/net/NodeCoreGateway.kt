// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.lite.net

import com.google.protobuf.ByteString
import io.grpc.ManagedChannel
import io.grpc.StatusRuntimeException
import nodecore.api.grpc.AdminGrpc
import nodecore.api.grpc.AdminRpcConfiguration
import nodecore.api.grpc.VeriBlockMessages
import nodecore.api.grpc.utilities.ByteStringAddressUtility
import nodecore.api.grpc.utilities.ByteStringUtility
import nodecore.api.grpc.utilities.ChannelBuilder
import org.veriblock.core.contracts.AddressManager
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.lite.core.Balance
import org.veriblock.lite.core.BlockChainDelta
import org.veriblock.lite.core.FullBlock
import org.veriblock.lite.params.NetworkParameters
import org.veriblock.lite.serialization.deserialize
import org.veriblock.lite.serialization.deserializeStandardTransaction
import org.veriblock.sdk.models.Coin
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.models.VeriBlockPublication
import org.veriblock.sdk.models.VeriBlockTransaction
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException
import kotlin.math.abs

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
            logger.error("Unable to get last VBK block", e)
            throw e
        }
    }

    fun getBlock(height: Int): FullBlock? {
        logger.debug { "Requesting VBK block at height $height..." }
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
        logger.debug { "Requesting VBK block with hash $hash..." }
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

    fun getVeriBlockPublications(keystoneHash: String, contextHash: String, btcContextHash: String): List<VeriBlockPublication> {
        logger.debug { "Requesting veriblock publications for keystone $keystoneHash..." }
        val request = VeriBlockMessages.GetVeriBlockPublicationsRequest
            .newBuilder()
            .setKeystoneHash(ByteStringUtility.hexToByteString(keystoneHash))
            .setContextHash(ByteStringUtility.hexToByteString(contextHash))
            .setBtcContextHash(ByteStringUtility.hexToByteString(btcContextHash))
            .build()

        val reply = blockingStub
            .withDeadlineAfter(300, TimeUnit.SECONDS)
            .getVeriBlockPublications(request)
        if (reply.success) {
            return reply.publicationsList.map {
                it.deserialize(params)
            }
        } else {
            for (error in reply.resultsList) {
                logger.warn { "${error.message} | ${error.details}" }
            }
            error("Unable to get VeriBlock Publications linking keystone $keystoneHash to VBK block $contextHash and BTC block $btcContextHash")
        }
    }

    fun getDebugVeriBlockPublications(vbkContextHash: String, btcContextHash: String): List<VeriBlockPublication> {
        logger.debug { "Requesting debug veriblock publications..." }
        val request = VeriBlockMessages.GetDebugVTBsRequest
            .newBuilder()
            .setVbkContextHash(ByteStringUtility.hexToByteString(vbkContextHash))
            .setBtcContextHash(ByteStringUtility.hexToByteString(btcContextHash))
            .build()

        val reply = blockingStub
            .withDeadlineAfter(300, TimeUnit.SECONDS)
            .getDebugVTBs(request)
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
            logger.debug("Unable to connect ping NodeCore at this time")
            false
        }
    }

    /**
     * Verify if the connected NodeCore is synchronized with the network (the block difference between the networkHeight and the localBlockchainHeight
     * should be smaller than 4 blocks)
     *
     * This function will return an empty NodeCoreSyncStatus if NodeCore is not accessible or if NodeCore still loading (networkHeight = 0)
     */
    fun getNodeCoreSyncStatus(): NodeCoreSyncStatus {
        return try {
            val request = blockingStub
                .withDeadlineAfter(5L, TimeUnit.SECONDS)
                .getStateInfo(VeriBlockMessages.GetStateInfoRequest.newBuilder().build())
            val blockDifference = abs(request.networkHeight - request.localBlockchainHeight)
            NodeCoreSyncStatus(
                request.networkHeight,
                request.localBlockchainHeight,
                blockDifference,
                request.networkHeight > 0 && blockDifference < 4
            )
        } catch (e: StatusRuntimeException) {
            logger.warn("Unable to perform GetStateInfoRequest to NodeCore")
            NodeCoreSyncStatus(0,0, 0,false)
        }
    }

    data class NodeCoreSyncStatus(
        val networkHeight: Int,
        val localBlockchainHeight: Int,
        val blockDifference: Int,
        val isSynchronized: Boolean
    )

    fun listChangesSince(hash: String?): BlockChainDelta {
        logger.debug { "Requesting delta since hash $hash..." }
        val builder = VeriBlockMessages.ListBlocksSinceRequest.newBuilder()
        if (hash != null && hash.isNotEmpty()) {
            builder.hash = ByteStringUtility.hexToByteString(hash)
        }
        val reply = blockingStub
            .withDeadlineAfter(10, TimeUnit.SECONDS)
            .listBlocksSince(builder.build())
        if (!reply.success) {
            error("Unable to retrieve changes since VBK block $hash")
        }
        val removed = reply.removedList.map { msg -> msg.deserialize(params) }
        val added = reply.addedList.map { msg -> msg.deserialize(params) }
        return BlockChainDelta(removed, added)
    }

    fun submitEndorsementTransaction(
        publicationData: ByteArray, addressManager: AddressManager, feePerByte: Long, maxFee: Long
    ): VeriBlockTransaction {
        logger.debug { "Creating endorsement transaction..." }
        val sourceAddressByteString = ByteStringAddressUtility.createProperByteStringAutomatically(
            addressManager.defaultAddress.hash
        )
        val createRequest = VeriBlockMessages.CreateAltChainEndorsementRequest
            .newBuilder()
            .setPublicationData(ByteStringUtility.bytesToByteString(publicationData))
            .setSourceAddress(sourceAddressByteString)
            .setFeePerByte(feePerByte)
            .setMaxFee(maxFee)
            .build()

        val createReply = blockingStub
            .withDeadlineAfter(2, TimeUnit.SECONDS)
            .createAltChainEndorsement(createRequest)
        if (!createReply.success) {
            for (error in createReply.resultsList) {
                logger.error { "${error.message} | ${error.details}" }
            }
            error("Unable to create endorsement transaction (Publication Data: ${publicationData.toHex()})")
        }

        val signedTransaction = generateSignedRegularTransaction(addressManager, createReply.transaction, createReply.signatureIndex)
        logger.debug { "Submitting endorsement transaction..." }
        val submitRequest = VeriBlockMessages.SubmitTransactionsRequest
            .newBuilder()
            .addTransactions(
                VeriBlockMessages.TransactionUnion.newBuilder().setSigned(signedTransaction)
            )
            .build()

        val submitReply = blockingStub
            .withDeadlineAfter(3, TimeUnit.SECONDS)
            .submitTransactions(submitRequest)

        if (!submitReply.success) {
            for (error in submitReply.resultsList) {
                logger.error { "${error.message} | ${error.details}" }
            }
            error("Unable to submit endorsement transaction (Publication Data: ${publicationData.toHex()})")
        }

        return signedTransaction.deserializeStandardTransaction(params)
    }

    private fun generateSignedRegularTransaction(
        addressManager: AddressManager,
        unsignedTransaction: VeriBlockMessages.Transaction,
        signatureIndex: Long
    ): VeriBlockMessages.SignedTransaction {
        val sourceAddress = ByteStringAddressUtility.parseProperAddressTypeAutomatically(unsignedTransaction.sourceAddress)
        requireNotNull(addressManager.get(sourceAddress)) {
            "The address $sourceAddress is not contained in the specified wallet file!"
        }
        val transactionId = unsignedTransaction.txId.toByteArray()
        val signature = addressManager.signMessage(transactionId, sourceAddress)

        return VeriBlockMessages.SignedTransaction.newBuilder()
            .setPublicKey(ByteString.copyFrom(addressManager.getPublicKeyForAddress(sourceAddress).encoded))
            .setSignatureIndex(signatureIndex)
            .setSignature(ByteString.copyFrom(signature))
            .setTransaction(unsignedTransaction)
            .build()
    }
}
