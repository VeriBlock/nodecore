// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.net

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import nodecore.api.grpc.VeriBlockMessages
import nodecore.api.grpc.utilities.ByteStringUtility
import org.veriblock.core.contracts.Balance
import org.veriblock.core.crypto.AnyVbkHash
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.crypto.asVbkHash
import org.veriblock.core.params.NetworkParameters
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.debugWarn
import org.veriblock.core.wallet.AddressManager
import org.veriblock.miners.pop.serialization.deserialize
import org.veriblock.miners.pop.serialization.deserializeStandardTransaction
import org.veriblock.sdk.models.Coin
import org.veriblock.sdk.models.StateInfo
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.models.VeriBlockPublication
import org.veriblock.sdk.models.VeriBlockTransaction
import org.veriblock.sdk.services.SerializeDeserializeService
import org.veriblock.spv.model.StandardAddress
import org.veriblock.spv.model.StandardTransaction
import org.veriblock.spv.service.NetworkState
import org.veriblock.spv.service.SpvService
import org.veriblock.spv.service.TransactionInfo
import kotlin.math.abs

private val logger = createLogger {}

class SpvGateway(
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
            val lastBlock = spvService.getLastVbkBlockHeader()
            SerializeDeserializeService.parseVeriBlockBlock(lastBlock.header, lastBlock.hash.asVbkHash())
        } catch (e: Exception) {
            logger.debugWarn(e) { "Unable to get last VBK block" }
            throw e
        }
    }

    fun getBlock(hash: AnyVbkHash): VeriBlockBlock? {
        logger.debug { "Requesting VBK block with hash $hash..." }

        return spvService.getVbkBlockHeader(hash)?.let {
            SerializeDeserializeService.parseVeriBlockBlock(it.header, it.hash.asVbkHash())
        }
    }

    fun getBalance(address: String): Balance {
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
                    prefix = " External Full Node errors:\n"
                ) {
                    "${it.message} | ${it.details}"
                }
            } else {
                ""
            }
            error(
                "Unable to get VeriBlock Publications linking keystone $keystoneHash to VBK block $contextHash and BTC block $btcContextHash$errors"
            )
        }
    }

    /**
     * Retrieve the 'state info' from SPV
     * This function will return an empty StateInfo if SPV is not accessible or if SPV is still loading (networkHeight == null)
     */
    fun getSpvStateInfo(): StateInfo {
        val stateInfo = spvService.getStateInfo()

        val networkHeight = stateInfo.networkHeight
        val blockDifference = if (networkHeight == null) {
            Int.MAX_VALUE
        } else {
            abs(networkHeight - stateInfo.localBlockchainHeight)
        }
        return StateInfo(
            networkTipHeight = stateInfo.networkHeight,
            localBlockchainHash = stateInfo.localBlockchainHash,
            localBlockchainHeight = stateInfo.localBlockchainHeight,
            blockDifference = blockDifference,
            // when networkHeight == null -> isSynchronized=false
            isSynchronized = networkHeight != null && networkHeight > 0 && blockDifference < 4,
            networkVersion = stateInfo.networkVersion
        )
    }

    private val mutex = Mutex()

    suspend fun submitEndorsementTransaction(
        publicationData: ByteArray, addressManager: AddressManager, feePerByte: Long, maxFee: Long
    ): VeriBlockTransaction = mutex.withLock {
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
