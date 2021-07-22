// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.net

import com.google.protobuf.ByteString
import io.grpc.ManagedChannel
import io.grpc.StatusRuntimeException
import java.util.concurrent.TimeUnit
import nodecore.api.grpc.utilities.ByteStringAddressUtility
import nodecore.api.grpc.utilities.ByteStringUtility
import org.veriblock.core.contracts.Balance
import org.veriblock.core.params.NetworkParameters
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.debugWarn
import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.core.wallet.AddressManager
import org.veriblock.sdk.models.StateInfo
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.models.VeriBlockPublication
import org.veriblock.sdk.models.VeriBlockTransaction
import org.veriblock.sdk.models.asCoin
import java.util.concurrent.locks.ReentrantLock
import nodecore.api.grpc.AdminGrpc
import nodecore.api.grpc.AdminRpcConfiguration
import nodecore.api.grpc.RpcBlockFilter
import nodecore.api.grpc.RpcCreateAltChainEndorsementRequest
import nodecore.api.grpc.RpcCreateTransactionRequest
import nodecore.api.grpc.RpcGetBalanceRequest
import nodecore.api.grpc.RpcGetBlocksRequest
import nodecore.api.grpc.RpcGetDebugVTBsRequest
import nodecore.api.grpc.RpcGetLastBlockRequest
import nodecore.api.grpc.RpcGetStateInfoRequest
import nodecore.api.grpc.RpcGetVeriBlockPublicationsRequest
import nodecore.api.grpc.RpcListBlocksSinceRequest
import nodecore.api.grpc.RpcOutput
import nodecore.api.grpc.RpcPingRequest
import nodecore.api.grpc.RpcSendCoinsRequest
import nodecore.api.grpc.RpcSignedTransaction
import nodecore.api.grpc.RpcSubmitTransactionsRequest
import nodecore.api.grpc.RpcTransaction
import nodecore.api.grpc.RpcTransactionUnion
import nodecore.api.grpc.utilities.ChannelBuilder
import org.veriblock.miners.pop.core.BlockChainDelta
import org.veriblock.miners.pop.serialization.deserialize
import org.veriblock.miners.pop.serialization.deserializeStandardTransaction
import org.veriblock.sdk.models.FullBlock
import kotlin.concurrent.withLock
import kotlin.math.abs

private val logger = createLogger {}

class NodeCoreGateway(
    private val params: NetworkParameters
) {
    private val blockingStub: AdminGrpc.AdminBlockingStub
    private val channel: ManagedChannel

    init {
        val rpcConfiguration = AdminRpcConfiguration().apply {
            isSsl = params.isSsl
            certificateChainPath = params.certificateChainPath
            nodeCoreHost = params.rpcHost
            nodeCorePort = params.rpcPort
            nodeCorePassword = params.adminPassword
        }
        val channelBuilder = ChannelBuilder(rpcConfiguration)
        channel = channelBuilder.buildManagedChannel()
        blockingStub = AdminGrpc.newBlockingStub(channelBuilder.attachPasswordInterceptor(channel))
            .withMaxInboundMessageSize(20 * 1024 * 1024)
            .withMaxOutboundMessageSize(20 * 1024 * 1024)
    }
    
    fun shutdown() {
        channel.shutdown().awaitTermination(15, TimeUnit.SECONDS)
    }

    fun getLastBlock(): VeriBlockBlock {
        return try {
            val lastBlock = blockingStub
                .withDeadlineAfter(120, TimeUnit.SECONDS)
                .getLastBlock(RpcGetLastBlockRequest.getDefaultInstance())

            lastBlock.header.deserialize()
        } catch (e: Exception) {
            logger.debugWarn(e) { "Unable to get last VBK block" }
            throw e
        }
    }

    fun getBlock(height: Int): FullBlock? {
        logger.debug { "Requesting VBK block at height $height..." }
        val request = RpcGetBlocksRequest.newBuilder()
            .addFilters(
                RpcBlockFilter.newBuilder()
                    .setIndex(height)
                    .build()
            )
            .build()

        val reply = blockingStub
            .withDeadlineAfter(120, TimeUnit.SECONDS)
            .getBlocks(request)
        if (reply.success && reply.blocksCount > 0) {
            val deserialized = reply.getBlocks(0).deserialize(params.transactionPrefix)
            return deserialized
        }

        return null
    }

    fun getBlock(hash: String): FullBlock? {
        logger.debug { "Requesting VBK block with hash $hash..." }
        val request = RpcGetBlocksRequest.newBuilder()
            .addFilters(
                RpcBlockFilter.newBuilder()
                    .setHash(ByteStringUtility.hexToByteString(hash))
                    .build()
            )
            .build()

        val reply = blockingStub
            .withDeadlineAfter(120, TimeUnit.SECONDS)
            .getBlocks(request)

        if (reply.success && reply.blocksCount > 0) {
            val deserialized = reply.getBlocks(0).deserialize(params.transactionPrefix)
            return deserialized
        }

        return null
    }

    fun getBalance(address: String): Balance {
        logger.debug { "Requesting balance for address $address..." }
        val request = RpcGetBalanceRequest.newBuilder()
            .addAddresses(ByteStringUtility.base58ToByteString(address))
            .build()

        val reply = blockingStub
            .withDeadlineAfter(120, TimeUnit.SECONDS)
            .getBalance(request)

        if (reply.success) {
            return Balance(
                reply.getConfirmed(0).unlockedAmount.asCoin(),
                reply.getUnconfirmed(0).amount.asCoin()
            )
        } else {
            error("Unable to retrieve balance from address $address")
        }
    }

    fun getVeriBlockPublications(keystoneHash: String, contextHash: String, btcContextHash: String): List<VeriBlockPublication> {
        logger.debug { "Requesting veriblock publications for keystone $keystoneHash..." }
        val request = RpcGetVeriBlockPublicationsRequest
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
                it.deserialize(params.transactionPrefix)
            }
        } else {
            val errors = if (reply.resultsList.isNotEmpty()) {
                reply.resultsList.joinToString(
                    separator = "\n",
                    prefix = " NodeCore Errors:\n"
                ) {
                    "${it.message} | ${it.details}"
                }
            } else {
                ""
            }
            for (error in reply.resultsList) {
                logger.error { "NodeCore error: ${error.message} | ${error.details}" }
            }
            error(
                "Unable to get VeriBlock Publications linking keystone $keystoneHash to VBK block $contextHash and BTC block $btcContextHash$errors"
            )
        }
    }

    fun getDebugVeriBlockPublications(vbkContextHash: String, btcContextHash: String): List<VeriBlockPublication> {
        logger.debug { "Requesting debug veriblock publications..." }
        val request = RpcGetDebugVTBsRequest
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
                publications.add(pubMsg.deserialize(params.transactionPrefix))
            }
            return publications
        } else {
            for (error in reply.resultsList) {
                logger.error { "NodeCore error: ${error.message} | ${error.details}" }
            }
        }

        return emptyList()
    }

    fun ping(): Boolean {
        return try {
            blockingStub
                .withDeadlineAfter(5L, TimeUnit.SECONDS)
                .ping(RpcPingRequest.getDefaultInstance())
            true
        } catch (e: StatusRuntimeException) {
            // NodeCore is not reachable at this point
            logger.debug("Unable to connect to NodeCore at this time")
            false
        }
    }

    /**
     * Retrieve the 'state info' from NodeCore
     * This function will return an empty StateInfo if NodeCore is not accessible or if NodeCore still loading (networkHeight = 0)
     */
    fun getNodeCoreStateInfo(): StateInfo {
        return try {
            val request = blockingStub
                .withDeadlineAfter(5L, TimeUnit.SECONDS)
                .getStateInfo(RpcGetStateInfoRequest.getDefaultInstance())

            val blockDifference = abs(request.networkHeight - request.localBlockchainHeight)
            StateInfo(
                request.networkHeight,
                request.localBlockchainHeight,
                blockDifference,
                request.networkHeight > 0 && blockDifference < 4,
                networkVersion = request.networkVersion
            )
        } catch (e: StatusRuntimeException) {
            logger.warn("Unable to perform the GetStateInfoRequest request to NodeCore (is it reachable?)")
            StateInfo()
        }
    }

    fun listChangesSince(hash: String): BlockChainDelta {
        logger.debug { "Requesting delta since hash $hash..." }
        val builder = RpcListBlocksSinceRequest.newBuilder()
        if (hash.isNotEmpty()) {
            builder.hash = ByteStringUtility.hexToByteString(hash)
        }
        val reply = blockingStub
            .withDeadlineAfter(10, TimeUnit.SECONDS)
            .listBlocksSince(builder.build())

        if (!reply.success) {
            error("Unable to retrieve changes since VBK block $hash")
        }
        val removed = reply.removedList.map { msg -> msg.deserialize() }
        val added = reply.addedList.map { msg -> msg.deserialize() }
        return BlockChainDelta(removed, added)
    }

    fun sendCoins(addressManager: AddressManager, destinationAddress: String, atomicAmount: Long): VeriBlockTransaction {
        val sourceAddressByteString = ByteStringAddressUtility.createProperByteStringAutomatically(
            addressManager.defaultAddress.hash
        )
        val destinationAddressByteString = ByteStringAddressUtility.createProperByteStringAutomatically(
            destinationAddress
        )
        val output = RpcOutput.newBuilder().setAddress(destinationAddressByteString).setAmount(atomicAmount)
        val createRequest = RpcCreateTransactionRequest
            .newBuilder()
            .setSourceAddress(sourceAddressByteString)
            .setAmounts(0, output)
            .build()
        val createReply = blockingStub
            .withDeadlineAfter(30, TimeUnit.SECONDS)
            .createTransaction(createRequest)

        if (!createReply.success) {
            for (error in createReply.resultsList) {
                logger.error { "NodeCore error: ${error.message} | ${error.details}" }
            }
            error("Unable to create transaction")
        }
        val signedTransaction = generateSignedRegularTransaction(addressManager, createReply.transaction, createReply.signatureIndex)
        val submitRequest = RpcSubmitTransactionsRequest
            .newBuilder()
            .addTransactions(
                RpcTransactionUnion.newBuilder().setSigned(signedTransaction)
            )
            .build()

        val submitReply = blockingStub
            .withDeadlineAfter(30, TimeUnit.SECONDS)
            .submitTransactions(submitRequest)

        if (!submitReply.success) {
            for (error in submitReply.resultsList) {
                logger.error { "NodeCore error: ${error.message} | ${error.details}" }
            }
            error("Unable to submit transaction")
        }
        return signedTransaction.deserializeStandardTransaction(params.transactionPrefix)
    }

    private val lock = ReentrantLock()

    fun submitEndorsementTransaction(
        publicationData: ByteArray, addressManager: AddressManager, feePerByte: Long, maxFee: Long
    ): VeriBlockTransaction = lock.withLock {
        logger.debug { "Creating endorsement transaction..." }
        val sourceAddressByteString = ByteStringAddressUtility.createProperByteStringAutomatically(
            addressManager.defaultAddress.hash
        )
        val createRequest = RpcCreateAltChainEndorsementRequest
            .newBuilder()
            .setPublicationData(ByteStringUtility.bytesToByteString(publicationData))
            .setSourceAddress(sourceAddressByteString)
            .setFeePerByte(feePerByte)
            .setMaxFee(maxFee)
            .build()

        val createReply = blockingStub
            .withDeadlineAfter(30, TimeUnit.SECONDS)
            .createAltChainEndorsement(createRequest)

        if (!createReply.success) {
            for (error in createReply.resultsList) {
                logger.error { "NodeCore error: ${error.message} | ${error.details}" }
            }
            error("Unable to create endorsement transaction (Publication Data: ${publicationData.toHex()})")
        }

        val signedTransaction = generateSignedRegularTransaction(addressManager, createReply.transaction, createReply.signatureIndex)
        logger.debug { "Submitting endorsement transaction..." }
        val submitRequest = RpcSubmitTransactionsRequest
            .newBuilder()
            .addTransactions(
                RpcTransactionUnion.newBuilder().setSigned(signedTransaction)
            )
            .build()

        val submitReply = blockingStub
            .withDeadlineAfter(30, TimeUnit.SECONDS)
            .submitTransactions(submitRequest)

        if (!submitReply.success) {
            for (error in submitReply.resultsList) {
                logger.error { "NodeCore error: ${error.message} | ${error.details}" }
            }
            error("Unable to submit endorsement transaction (Publication Data: ${publicationData.toHex()})")
        }

        return signedTransaction.deserializeStandardTransaction(params.transactionPrefix)
    }

    private fun generateSignedRegularTransaction(
        addressManager: AddressManager,
        unsignedTransaction: RpcTransaction,
        signatureIndex: Long
    ): RpcSignedTransaction {
        val sourceAddress = ByteStringAddressUtility.parseProperAddressTypeAutomatically(unsignedTransaction.sourceAddress)
        requireNotNull(addressManager.get(sourceAddress)) {
            "The address $sourceAddress is not contained in the specified wallet file!"
        }
        val transactionId = unsignedTransaction.txId.toByteArray()
        val signature = addressManager.signMessage(transactionId, sourceAddress)

        return RpcSignedTransaction.newBuilder()
            .setPublicKey(ByteString.copyFrom(addressManager.getPublicKeyForAddress(sourceAddress).encoded))
            .setSignatureIndex(signatureIndex)
            .setSignature(ByteString.copyFrom(signature))
            .setTransaction(unsignedTransaction)
            .build()
    }
}
