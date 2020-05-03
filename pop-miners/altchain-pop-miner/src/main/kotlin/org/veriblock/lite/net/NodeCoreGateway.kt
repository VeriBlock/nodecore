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
import org.veriblock.core.utilities.AddressUtility
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.lite.core.Balance
import org.veriblock.lite.params.NetworkParameters
import org.veriblock.lite.serialization.deserialize
import org.veriblock.lite.serialization.deserializeStandardTransaction
import org.veriblock.sdk.models.Coin
import org.veriblock.sdk.models.Sha256Hash
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.models.VeriBlockPublication
import org.veriblock.sdk.models.VeriBlockTransaction
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.abs

private val logger = createLogger {}

class NodeCoreGateway(
    private val params: NetworkParameters,
    private val gatewayStrategy: GatewayStrategy
) {

    fun getBalance(address: String): Balance {
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

    fun getVeriBlockPublications(keystoneHash: String, contextHash: String, btcContextHash: String): List<VeriBlockPublication> {
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

    fun getDebugVeriBlockPublications(vbkContextHash: String, btcContextHash: String): List<VeriBlockPublication> {
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

    fun ping(): Boolean {
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
    fun getNodeCoreSyncStatus(): NodeCoreSyncStatus {
        return try {
            val request = gatewayStrategy.getNodeCoreSyncStatus(VeriBlockMessages.GetStateInfoRequest.newBuilder().build())

            val blockDifference = abs(request.networkHeight - request.localBlockchainHeight)
            NodeCoreSyncStatus(
                request.networkHeight,
                request.localBlockchainHeight,
                blockDifference,
                request.networkHeight > 0 && blockDifference < 10
            )
        } catch (e: StatusRuntimeException) {
            logger.warn("Unable to perform the GetStateInfoRequest request to NodeCore (is it reachable?)")
            NodeCoreSyncStatus(0, 0, 0, false)
        }
    }

    private val lock = ReentrantLock()

    fun submitEndorsementTransaction(
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

    fun getLastVBKBlockHeader(): VeriBlockBlock {
        return gatewayStrategy.getLastVBKBlockHeader().deserialize()
    }

    fun getVBKBlockHeader(height: Int): VeriBlockBlock {
        return gatewayStrategy.getVBKBlockHeader(height).deserialize()
    }

    fun getVBKBlockHeader(blockHash: ByteArray): VeriBlockBlock {
        return gatewayStrategy.getVBKBlockHeader(blockHash).deserialize()
    }

    fun getTransactions(ids: Collection<Sha256Hash>): List<VeriBlockMessages.TransactionInfo> {
        val request =
            VeriBlockMessages.GetTransactionsRequest.newBuilder()
                .addAllIds(ids.map { ByteString.copyFrom(it.bytes) })
                .build()

        val response = gatewayStrategy.getTransactions(request)

        return response.transactionsList
    }

    fun generateSignedRegularTransaction(
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

        val valid = AddressUtility.isSignatureValid(
            transactionId, signature, addressManager.getPublicKeyForAddress(sourceAddress).encoded, sourceAddress
        )

        if (!valid) {
            error("Transaction is not valid. TxId: ${unsignedTransaction.txId})")
        }

        return VeriBlockMessages.SignedTransaction.newBuilder()
            .setPublicKey(ByteString.copyFrom(addressManager.getPublicKeyForAddress(sourceAddress).encoded))
            .setSignatureIndex(signatureIndex)
            .setSignature(ByteString.copyFrom(signature))
            .setTransaction(unsignedTransaction)
            .build()
    }
}

data class NodeCoreSyncStatus(
    val networkHeight: Int,
    val localBlockchainHeight: Int,
    val blockDifference: Int,
    val isSynchronized: Boolean
)
