// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.lite.net

import com.google.protobuf.ByteString
import io.grpc.StatusRuntimeException
import nodecore.api.grpc.VeriBlockMessages
import nodecore.api.grpc.utilities.ByteStringAddressUtility
import nodecore.api.grpc.utilities.ByteStringUtility
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
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.abs

private val logger = createLogger {}

class NodeCoreGatewayImpl(
    private val params: NetworkParameters
) : NodeCoreGateway {

    private val gatewayStrategy: GatewayStrategy = NodeCoreGatewayFactory.create(params)

    @Throws(InterruptedException::class)
    override fun shutdown() {
        gatewayStrategy.shutdown()
    }

    override fun getLastBlock(): VeriBlockBlock {
        return try {
            val lastBlock = gatewayStrategy.getLastBlock(VeriBlockMessages.GetLastBlockRequest.getDefaultInstance())

            lastBlock.header.deserialize()
        } catch (e: Exception) {
            logger.warn("Unable to get last VBK block", e)
            throw e
        }
    }

    override fun getBlock(height: Int): FullBlock? {
        logger.debug { "Requesting VBK block at height $height..." }
        val request = VeriBlockMessages.GetBlocksRequest.newBuilder()
            .addFilters(
                VeriBlockMessages.BlockFilter.newBuilder()
                    .setIndex(height)
                    .build()
            )
            .build()

        val reply = gatewayStrategy.getBlock(request)
        if (reply.success && reply.blocksCount > 0) {
            val deserialized = reply.getBlocks(0).deserialize(params.transactionPrefix)
            return deserialized
        }

        return null
    }

    override fun getBlock(hash: String): FullBlock? {
        logger.debug { "Requesting VBK block with hash $hash..." }
        val request = VeriBlockMessages.GetBlocksRequest.newBuilder()
            .addFilters(
                VeriBlockMessages.BlockFilter.newBuilder()
                    .setHash(ByteStringUtility.hexToByteString(hash))
                    .build()
            )
            .build()

        val reply = gatewayStrategy.getBlock(request)

        if (reply.success && reply.blocksCount > 0) {
            val deserialized = reply.getBlocks(0).deserialize(params.transactionPrefix)
            return deserialized
        }

        return null
    }

    override fun getBalance(address: String): Balance {
        logger.debug { "Requesting balance for address $address..." }
        val request = VeriBlockMessages.GetBalanceRequest.newBuilder()
            .addAddresses(ByteStringUtility.base58ToByteString(address))
            .build()

        val reply = gatewayStrategy.getBalance(request)

        if (reply.success) {
            return Balance(
                Coin.valueOf(reply.getConfirmed(0).unlockedAmount),
                Coin.valueOf(reply.getUnconfirmed(0).amount)
            )
        } else {
            error("Unable to retrieve balance from address $address")
        }
    }

    override fun getVeriBlockPublications(keystoneHash: String, contextHash: String, btcContextHash: String): List<VeriBlockPublication> {
        logger.debug { "Requesting veriblock publications for keystone $keystoneHash..." }
        val request = VeriBlockMessages.GetVeriBlockPublicationsRequest
            .newBuilder()
            .setKeystoneHash(ByteStringUtility.hexToByteString(keystoneHash))
            .setContextHash(ByteStringUtility.hexToByteString(contextHash))
            .setBtcContextHash(ByteStringUtility.hexToByteString(btcContextHash))
            .build()

        val reply = gatewayStrategy.getVeriBlockPublications(request)

        if (reply.success) {
            return reply.publicationsList.map {
                it.deserialize(params.transactionPrefix)
            }
        } else {
            for (error in reply.resultsList) {
                logger.error { "NodeCore error: ${error.message} | ${error.details}" }
            }
            error("Unable to get VeriBlock Publications linking keystone $keystoneHash to VBK block $contextHash and BTC block $btcContextHash")
        }
    }

    override fun getDebugVeriBlockPublications(vbkContextHash: String, btcContextHash: String): List<VeriBlockPublication> {
        logger.debug { "Requesting debug veriblock publications..." }
        val request = VeriBlockMessages.GetDebugVTBsRequest
            .newBuilder()
            .setVbkContextHash(ByteStringUtility.hexToByteString(vbkContextHash))
            .setBtcContextHash(ByteStringUtility.hexToByteString(btcContextHash))
            .build()

        val reply = gatewayStrategy.getDebugVeriBlockPublications(request)

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

    override fun ping(): Boolean {
        return try {
            gatewayStrategy.ping(VeriBlockMessages.PingRequest.newBuilder().build())
            true
        } catch (e: StatusRuntimeException) {
            // NodeCore is not reachable at this point
            logger.debug("Unable to connect to NodeCore at this time")
            false
        }
    }

    /**
     * Verify if the connected NodeCore is synchronized with the network (the block difference between the networkHeight and the localBlockchainHeight
     * should be smaller than 4 blocks)
     *
     * This function will return an empty NodeCoreSyncStatus if NodeCore is not accessible or if NodeCore still loading (networkHeight = 0)
     */
    override fun getNodeCoreSyncStatus(): NodeCoreGateway.NodeCoreSyncStatus {
        return try {
            val request = gatewayStrategy.getNodeCoreSyncStatus(VeriBlockMessages.GetStateInfoRequest.newBuilder().build())

            val blockDifference = abs(request.networkHeight - request.localBlockchainHeight)
            NodeCoreGateway.NodeCoreSyncStatus(
                request.networkHeight,
                request.localBlockchainHeight,
                blockDifference,
                request.networkHeight > 0 && blockDifference < 4
            )
        } catch (e: StatusRuntimeException) {
            logger.warn("Unable to perform the GetStateInfoRequest request to NodeCore (is it reachable?)")
            NodeCoreGateway.NodeCoreSyncStatus(0, 0, 0, false)
        }
    }

    override fun listChangesSince(hash: String?): BlockChainDelta {
        logger.debug { "Requesting delta since hash $hash..." }
        val builder = VeriBlockMessages.ListBlocksSinceRequest.newBuilder()
        if (hash != null && hash.isNotEmpty()) {
            builder.hash = ByteStringUtility.hexToByteString(hash)
        }
        val reply = gatewayStrategy.listChangesSince(builder.build())

        if (!reply.success) {
            error("Unable to retrieve changes since VBK block $hash")
        }
        val removed = reply.removedList.map { msg -> msg.deserialize() }
        val added = reply.addedList.map { msg -> msg.deserialize() }
        return BlockChainDelta(removed, added)
    }

    private val lock = ReentrantLock()

    override fun submitEndorsementTransaction(
        publicationData: ByteArray, addressManager: AddressManager, feePerByte: Long, maxFee: Long
    ): VeriBlockTransaction = lock.withLock {
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

        val createReply = gatewayStrategy.createAltChainEndorsement(createRequest)

        if (!createReply.success) {
            for (error in createReply.resultsList) {
                logger.error { "NodeCore error: ${error.message} | ${error.details}" }
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

        val submitReply = gatewayStrategy.submitTransactions(submitRequest)

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
