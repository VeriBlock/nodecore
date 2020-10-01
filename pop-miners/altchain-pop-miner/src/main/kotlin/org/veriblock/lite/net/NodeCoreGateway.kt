// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.lite.net

import nodecore.api.grpc.VeriBlockMessages
import nodecore.api.grpc.utilities.ByteStringUtility
import org.veriblock.core.contracts.Balance
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.crypto.VBlakeHash
import org.veriblock.core.params.NetworkParameters
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.debugWarn
import org.veriblock.core.wallet.AddressManager
import org.veriblock.lite.serialization.deserialize
import org.veriblock.lite.serialization.deserializeStandardTransaction
import org.veriblock.sdk.models.Coin
import org.veriblock.sdk.models.StateInfo
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.models.VeriBlockPublication
import org.veriblock.sdk.models.VeriBlockTransaction
import org.veriblock.sdk.services.SerializeDeserializeService
import veriblock.model.StandardAddress
import veriblock.model.StandardTransaction
import veriblock.service.NetworkState
import veriblock.service.SpvService
import veriblock.service.TransactionInfo
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.abs

private val logger = createLogger {}

class NodeCoreGateway(
    private val params: NetworkParameters,
    private val spvService: SpvService
) {
    @Throws(InterruptedException::class)
    fun shutdown() {
        // TODO
        //spvService.peerTable.shutdown()
    }

    fun getLastBlock(): VeriBlockBlock {
        return try {
            val lastBlock = spvService.getLastVBKBlockHeader()
            SerializeDeserializeService.parseVeriBlockBlock(lastBlock.header)
        } catch (e: Exception) {
            logger.debugWarn(e) { "Unable to get last VBK block" }
            throw e
        }
    }

    fun getBlock(hash: VBlakeHash): VeriBlockBlock? {
        logger.debug { "Requesting VBK block with hash $hash..." }

        return spvService.getVbkBlockHeader(hash)?.let {
            SerializeDeserializeService.parseVeriBlockBlock(it.header)
        }
    }

    fun getBalance(address: String): Balance {
        logger.debug { "Requesting balance for address $address..." }

        val balance = spvService.getBalance(listOf(StandardAddress(address)))

        return Balance(
            balance.confirmed.first().unlockedAmount,
            balance.unconfirmed.firstOrNull()?.amount ?: Coin.ZERO
        )
    }

    fun getTransactions(ids: List<Sha256Hash>): List<TransactionInfo> {
        return spvService.getTransactions(ids)
    }

    suspend fun getVeriBlockPublications(keystoneHash: String, contextHash: String, btcContextHash: String): List<VeriBlockPublication> {
        logger.debug { "Requesting veriblock publications for keystone $keystoneHash..." }
        val request = VeriBlockMessages.GetVeriBlockPublicationsRequest
            .newBuilder()
            .setKeystoneHash(ByteStringUtility.hexToByteString(keystoneHash))
            .setContextHash(ByteStringUtility.hexToByteString(contextHash))
            .setBtcContextHash(ByteStringUtility.hexToByteString(btcContextHash))
            .build()

        val reply = spvService.getVeriBlockPublications(request)

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
        TODO()
        //logger.debug { "Requesting debug veriblock publications..." }
        //val request = VeriBlockMessages.GetDebugVTBsRequest
        //    .newBuilder()
        //    .setVbkContextHash(ByteStringUtility.hexToByteString(vbkContextHash))
        //    .setBtcContextHash(ByteStringUtility.hexToByteString(btcContextHash))
        //    .build()

        //val reply = spvService.getDebugVeriBlockPublications(request)

        //if (reply.success) {
        //    val publications = ArrayList<VeriBlockPublication>()
        //    for (pubMsg in reply.publicationsList) {
        //        publications.add(pubMsg.deserialize(params.transactionPrefix))
        //    }
        //    return publications
        //} else {
        //    for (error in reply.resultsList) {
        //        logger.error { "NodeCore error: ${error.message} | ${error.details}" }
        //    }
        //}

        //return emptyList()
    }

    /**
     * Retrieve the 'state info' from NodeCore
     * This function will return an empty StateInfo if NodeCore is not accessible or if NodeCore still loading (networkHeight = 0)
     */
    fun getNodeCoreStateInfo(): StateInfo {
        val stateInfo = spvService.getStateInfo()
        val blockDifference = abs(stateInfo.networkHeight - stateInfo.localBlockchainHeight)
        return StateInfo(
            stateInfo.networkHeight,
            stateInfo.localBlockchainHeight,
            blockDifference,
            stateInfo.networkHeight > 0 && blockDifference < 4,
            networkVersion = stateInfo.networkVersion
        )
    }

    private val lock = ReentrantLock()

    fun submitEndorsementTransaction(
        publicationData: ByteArray, addressManager: AddressManager, feePerByte: Long, maxFee: Long
    ): VeriBlockTransaction = lock.withLock {
        logger.debug { "Creating endorsement transaction..." }

        val createReply = spvService.createAltChainEndorsement(
            publicationData, addressManager.defaultAddress.hash, feePerByte, maxFee
        )

        val signedTransaction = signTransaction(addressManager, createReply.transaction)
        logger.debug { "Submitting endorsement transaction..." }

        spvService.submitTransactions(listOf(signedTransaction))

        return signedTransaction.deserializeStandardTransaction(params.transactionPrefix)
    }

    private fun signTransaction(
        addressManager: AddressManager,
        unsignedTransaction: StandardTransaction
    ): StandardTransaction {
        val sourceAddress = unsignedTransaction.inputAddress!!.get()
        requireNotNull(addressManager.get(sourceAddress)) {
            "The address $sourceAddress is not contained in the specified wallet file!"
        }
        val transactionId = unsignedTransaction.txId.bytes
        val signature = addressManager.signMessage(transactionId, sourceAddress)
        val publicKey = addressManager.getPublicKeyForAddress(sourceAddress).encoded

        unsignedTransaction.addSignature(signature, publicKey)
        return unsignedTransaction
    }

    fun isConnected(): Boolean {
        return spvService.getStateInfo().networkState == NetworkState.CONNECTED
    }
}
