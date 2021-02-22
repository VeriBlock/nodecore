// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.spv.service

import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import nodecore.api.grpc.VeriBlockMessages.GetVeriBlockPublicationsReply
import nodecore.api.grpc.VeriBlockMessages.GetVeriBlockPublicationsRequest
import nodecore.p2p.Constants
import org.veriblock.core.*
import org.veriblock.core.crypto.AnyVbkHash
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.types.Pair
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.debugError
import org.veriblock.core.utilities.debugInfo
import org.veriblock.core.utilities.debugWarn
import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.core.wallet.AddressManager
import org.veriblock.core.wallet.AddressPubKey
import org.veriblock.sdk.models.Address
import org.veriblock.sdk.models.asCoin
import org.veriblock.sdk.services.SerializeDeserializeService
import org.veriblock.spv.SpvContext
import org.veriblock.spv.model.AddressCoinsIndex
import org.veriblock.spv.model.AddressLight
import org.veriblock.spv.model.BlockHeader
import org.veriblock.spv.model.DownloadStatus
import org.veriblock.spv.model.LedgerContext
import org.veriblock.spv.model.Output
import org.veriblock.spv.model.StandardTransaction
import org.veriblock.spv.model.StoredVeriBlockBlock
import org.veriblock.spv.model.Transaction
import org.veriblock.spv.model.asLightAddress
import org.veriblock.spv.net.SpvPeerTable
import org.veriblock.spv.service.TransactionService.Companion.predictAltChainEndorsementTransactionSize
import org.veriblock.spv.util.buildMessage
import java.io.File
import java.io.IOException
import java.util.*

private val logger = createLogger {}

class SpvService(
    private val spvContext: SpvContext,
    private val peerTable: SpvPeerTable,
    private val transactionService: TransactionService,
    private val addressManager: AddressManager,
    private val pendingTransactionContainer: PendingTransactionContainer,
    private val blockchain: Blockchain
) {
    fun getStateInfo(): StateInfo {
        val downloadStatus = peerTable.getDownloadStatus()
        val blockchainState = if (downloadStatus.downloadStatus == DownloadStatus.READY) BlockchainState.LOADED else BlockchainState.LOADING
        val operatingState = OperatingState.STARTED
        val networkState = if (downloadStatus.downloadStatus == DownloadStatus.DISCOVERING) NetworkState.DISCONNECTED else NetworkState.CONNECTED
        return StateInfo(
            blockchainState = blockchainState,
            operatingState = operatingState,
            networkState = networkState,
            connectedPeerCount = peerTable.getAvailablePeers(),
            networkHeight = peerTable.getBestBlockHeight().coerceAtLeast(blockchain.activeChain.tip.height),
            localBlockchainHeight = blockchain.activeChain.tip.height,
            networkVersion = spvContext.networkParameters.name,
            dataDirectory = spvContext.directory.path,
            programVersion = Constants.PROGRAM_VERSION ?: "UNKNOWN",
            nodecoreStartTime = spvContext.startTime.epochSecond,
            walletCacheSyncHeight = blockchain.activeChain.tip.height,
            walletState = when {
                !addressManager.isEncrypted -> WalletState.DEFAULT
                addressManager.isLocked -> WalletState.LOCKED
                else -> WalletState.UNLOCKED
            }
        )
    }

    fun getAddressState(addr: Address): LedgerContext = spvContext.getAddressState(addr)

    // if sourceAddress is null, use all available addresses
    suspend fun sendCoins(sourceAddress: AddressLight?, outputs: List<Output>): List<Sha256Hash> {
        val addressCoinsIndexList: MutableList<AddressCoinsIndex> = ArrayList()
        val totalOutputAmount = outputs.map {
            it.amount.atomicUnits
        }.sum()
        if (sourceAddress == null) {
            addressCoinsIndexList.addAll(
                spvContext
                    .getAllAddressesState()
                    .values
                    .filter { it.ledgerValue.availableAtomicUnits > 0 }
                    .sortedByDescending { it.ledgerValue.availableAtomicUnits }
                    .map {
                        AddressCoinsIndex(
                            address = it.address.address,
                            coins = it.ledgerValue.availableAtomicUnits,
                            index = it.ledgerValue.signatureIndex
                        )
                    })
        } else {
            val address = Address(sourceAddress.get())
            val ledgerContext = getAddressState(address)
            addressCoinsIndexList.add(
                AddressCoinsIndex(
                    address = address.address,
                    coins = ledgerContext.ledgerValue.availableAtomicUnits,
                    index = getSignatureIndex(address)
                )
            )
        }
        val totalAvailableBalance = addressCoinsIndexList.sumOf { it.coins }
        if (totalOutputAmount > totalAvailableBalance) {
            throw SendCoinsException("Available balance is not enough. Check your address that you use for this operation.")
        }
        val transactions = transactionService.createTransactionsByOutputList(
            addressCoinsIndexList, outputs
        )
        return transactions.asFlow().onEach {
            pendingTransactionContainer.addTransaction(it)
            peerTable.advertise(it)
        }.map {
            it.txId
        }.toList()
    }

    fun getSignatureIndex(addresses: List<AddressLight>): List<AddressSignatureIndex> {
        return if (addresses.isNotEmpty()) {
            addresses
        } else {
            listOf(addressManager.defaultAddress.hash.asLightAddress())
        }.map { address ->
            val addr = Address(address.get())
            AddressSignatureIndex(
                address = address,
                poolIndex = getSignatureIndex(addr),
                blockchainIndex = spvContext.getSignatureIndex(addr)
                    ?: throw IllegalStateException("Invariant failed: can not get signature index - unknown address=$addr")
            )
        }
    }

    suspend fun submitTransactions(transactions: List<Transaction>) {
        for (transaction in transactions) {
            pendingTransactionContainer.addTransaction(transaction)
            peerTable.advertise(transaction)
        }
    }

    fun dumpPrivateKey(address: AddressLight): ByteArray {
        val publicKey = addressManager.getPublicKeyForAddress(address.get())
        val privateKey = try {
            addressManager.getPrivateKeyForAddress(address.get())
        } catch (e: WalletLockedException) {
            throw WalletException("Wallet is Locked")
        }
        if (privateKey == null || publicKey == null) {
            throw WalletException("The address '$address' is not an address in this wallet!")
        }
        val privateKeyBytes = privateKey.encoded
        val publicKeyBytes = publicKey.encoded
        val fullBytes = ByteArray(privateKeyBytes.size + publicKeyBytes.size + 1)
        fullBytes[0] = privateKeyBytes.size.toByte()
        privateKeyBytes.copyInto(
            destination = fullBytes,
            destinationOffset = 1
        )
        publicKeyBytes.copyInto(
            destination = fullBytes,
            destinationOffset = 1 + privateKeyBytes.size
        )
        return fullBytes
    }

    fun importPrivateKey(privateKey: ByteArray): AddressPubKey {
        if (addressManager.isLocked) {
            throw ImportException("Wallet must be unlocked before importing a key")
        }

        // The "Private" key is really the public and private key together encoded
        val privateKeyLength = privateKey[0].toInt()
        val privateKeyBytes = privateKey.copyOfRange(1, privateKeyLength + 1)
        val publicKeyBytes = privateKey.copyOfRange(privateKeyLength + 1, privateKey.size)
        val importedAddress = addressManager.importKeyPair(publicKeyBytes, privateKeyBytes)
            ?: throw ImportException("The private key ${privateKey.toHex()} is invalid or corrupted!")
        return importedAddress
    }

    fun encryptWallet(passphrase: String) {
        if (passphrase.isEmpty()) {
            throw WalletException("A blank passphrase is invalid for encrypting a wallet")
        } else {
            try {
                val result = addressManager.encryptWallet(passphrase.toCharArray())
                if (!result) {
                    throw WalletException("Unable to encrypt wallet, see NodeCore logs")
                }
            } catch (e: IllegalStateException) {
                throw WalletException("Wallet is already encrypted and must be decrypted first")
            }
        }
    }

    fun decryptWallet(passphrase: String) {
        if (passphrase.isEmpty()) {
            throw WalletException("A blank passphrase is invalid for decrypting a wallet")
        } else {
            try {
                val result = addressManager.decryptWallet(passphrase.toCharArray())
                if (!result) {
                    throw WalletException("Unable to decrypt wallet, see NodeCore logs")
                }
            } catch (e: IllegalStateException) {
                throw WalletException("Wallet is already decrypted and must be encrypted first")
            }
        }
    }

    fun unlockWallet(passphrase: String) {
        if (passphrase.isEmpty()) {
            throw WalletException("Passphrase is invalid for unlocking this wallet")
        } else {
            val result = addressManager.unlock(passphrase.toCharArray())
            if (!result) {
                throw WalletException("Passphrase is invalid for unlocking this wallet")
            }
        }
    }

    fun lockWallet() {
        addressManager.lock()
    }

    fun backupWallet(targetLocation: String) {
        try {
            val saveWalletResult = addressManager.saveWalletToFile(targetLocation)
            val success = saveWalletResult.first
            if (!success) {
                throw WalletException("Unable to save backup wallet file")
            }
        } catch (e: Exception) {
            logger.debug(e) { "Writing wallet backup failed" }
            throw WalletException("Writing wallet backup failed: ${e.message}")
        }
    }

    fun importWallet(sourceLocation: String, passphrase: String? = null) {
        if (addressManager.isLocked) {
            throw WalletException("Wallet must be unlocked before importing another wallet")
        }
        try {
            val result: Pair<Boolean, String> = if (!passphrase.isNullOrEmpty()) {
                addressManager.importEncryptedWallet(File(sourceLocation), passphrase.toCharArray())
            } else {
                addressManager.importWallet(File(sourceLocation))
            }
            val success = result.first
            if (!success) {
                throw WalletException("Unable to load/import wallet file!")
            }

            spvContext.addressManager.all.forEach {
                spvContext.getAddressState(Address(it.hash))
            }
        } catch (e: Exception) {
            logger.debugInfo(e) { "${e.message}" }
            throw WalletException("Reading wallet file failed")
        }
    }

    fun getNewAddress(count: Int = 1): List<AddressPubKey> {
        require(count >= 1) {
            "Invalid count of addresses to create: $count"
        }
        if (addressManager.isLocked) {
            throw AddressCreationException("Wallet must be unlocked before creating a new address")
        }
        try {
            return (1..count).map {
                val address = addressManager.newAddress
                if (address != null) {
                    logger.info("New address: {}", address.hash)
                    spvContext.getAddressState(Address(address.hash))
                    address
                } else {
                    throw AddressCreationException("Unable to generate new address")
                }
            }
        } catch (e: IOException) {
            logger.debugError(e) { e.toString() }
            throw AddressCreationException("Unable to generate new address")
        } catch (e: WalletLockedException) {
            logger.debugWarn(e) { e.toString() }
            throw AddressCreationException("Wallet must be unlocked before creating a new address")
        }
    }

    fun getBalance(): WalletBalance {
        // All of the addresses from the normal address manager will be standard
        val addressLedgerContext = spvContext.getAllAddressesState()
        return WalletBalance(
            confirmed = addressLedgerContext.map { (address, ledgerContext) ->
                getAddressBalance(address.address.asLightAddress(), ledgerContext)
            },
            unconfirmed = emptyList()
        )
    }

    fun createAltChainEndorsement(publicationData: ByteArray, sourceAddress: Address, feePerByte: Long, maxFee: Long): AltChainEndorsement {
        try {
            val signatureIndex = getSignatureIndex(sourceAddress) + 1
            val fee = feePerByte * predictAltChainEndorsementTransactionSize(publicationData.size, signatureIndex)
            if (fee > maxFee) {
                throw EndorsementCreationException("Calculated fee $fee was above the maximum configured amount $maxFee")
            }
            val tx = transactionService.createUnsignedAltChainEndorsementTransaction(
                sourceAddress.address, fee, publicationData, signatureIndex
            )
            return AltChainEndorsement((tx as StandardTransaction), signatureIndex)
        } catch (e: Exception) {
            logger.debugWarn(e) { "Failed to create alt chain endorsement" }
            throw EndorsementCreationException("Failed to create alt chain endorsement")
        }
    }

    fun getLastVbkBlockHeader(): BlockHeader {
        val block: StoredVeriBlockBlock = blockchain.getChainHeadBlock()
        return BlockHeader(
            SerializeDeserializeService.serializeHeaders(block.header),
            block.hash.bytes
        )
    }

    fun getVbkBlockHeader(hash: AnyVbkHash): BlockHeader? {
        val block = blockchain.getBlock(hash)
            ?: return null
        return BlockHeader(
            SerializeDeserializeService.serializeHeaders(block.header),
            block.hash.bytes
        )
    }

    fun getLastBitcoinBlock(): Sha256Hash = spvContext.networkParameters.bitcoinOriginBlock.hash    //Mock todo SPV-111

    fun getTransactions(ids: List<Sha256Hash>) = ids.mapNotNull {
        pendingTransactionContainer.getTransactionInfo(it)
    }

    suspend fun getVeriBlockPublications(getVeriBlockPublicationsRequest: GetVeriBlockPublicationsRequest): GetVeriBlockPublicationsReply {
        val request = buildMessage {
            veriblockPublicationsRequest = getVeriBlockPublicationsRequest
        }
        val reply = peerTable.requestMessage(request, timeoutInMillis = 300_000L) {
            if (it.veriblockPublicationsReply.success) {
                it.veriblockPublicationsReply.publicationsCount
            } else {
                -1
            }
        }
        return reply.veriblockPublicationsReply
    }

    private fun getAddressBalance(address: AddressLight, ledgerContext: LedgerContext): AddressBalance {
        val balance = ledgerContext.ledgerValue.availableAtomicUnits
        val lockedCoins = ledgerContext.ledgerValue.frozenAtomicUnits
        return AddressBalance(
            address = address,
            unlockedAmount = (balance - lockedCoins).asCoin(),
            lockedAmount = lockedCoins.asCoin(),
            totalAmount = balance.asCoin()
        )
    }

    private fun getSignatureIndex(address: Address): Long =
        pendingTransactionContainer.getPendingSignatureIndexForAddress(address)
            ?: spvContext.getSignatureIndex(address)
            ?: throw IllegalStateException(
                "Requested signature index for address which is not present in AddressState"
            )
}
