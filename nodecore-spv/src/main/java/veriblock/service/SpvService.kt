// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.service

import com.google.protobuf.ByteString
import nodecore.api.grpc.VeriBlockMessages
import nodecore.api.grpc.VeriBlockMessages.GetVeriBlockPublicationsReply
import nodecore.api.grpc.VeriBlockMessages.GetVeriBlockPublicationsRequest
import nodecore.p2p.Constants
import org.veriblock.core.AddressCreationException
import org.veriblock.core.EndorsementCreationException
import org.veriblock.core.ImportException
import org.veriblock.core.SendCoinsException
import org.veriblock.core.TransactionSubmissionException
import org.veriblock.core.WalletException
import org.veriblock.core.WalletLockedException
import org.veriblock.core.types.Pair
import org.veriblock.core.wallet.AddressPubKey
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.crypto.VBlakeHash
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.debugError
import org.veriblock.core.utilities.debugInfo
import org.veriblock.core.utilities.debugWarn
import org.veriblock.core.utilities.extensions.asHexBytes
import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.core.wallet.AddressManager
import org.veriblock.sdk.models.Coin
import org.veriblock.sdk.models.asCoin
import org.veriblock.sdk.services.SerializeDeserializeService
import veriblock.SpvContext
import veriblock.model.AddressCoinsIndex
import veriblock.model.AddressLight
import veriblock.model.BlockHeader
import veriblock.model.DownloadStatus
import veriblock.model.LedgerContext
import veriblock.model.Output
import veriblock.model.StandardTransaction
import veriblock.model.Transaction
import veriblock.model.asLightAddress
import veriblock.net.SpvPeerTable
import veriblock.service.TransactionService.Companion.predictAltChainEndorsementTransactionSize
import veriblock.util.nextMessageId
import java.io.File
import java.io.IOException
import java.util.ArrayList

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
            networkHeight = peerTable.getBestBlockHeight().coerceAtLeast(blockchain.getChainHead().height),
            localBlockchainHeight = blockchain.getChainHead().height,
            networkVersion = spvContext.networkParameters.name,
            dataDirectory = spvContext.directory.path,
            programVersion = Constants.PROGRAM_VERSION ?: "UNKNOWN",
            nodecoreStartTime = spvContext.startTime.epochSecond,
            walletCacheSyncHeight = blockchain.getChainHead().height,
            walletState = when {
                addressManager.isEncrypted -> WalletState.DEFAULT
                addressManager.isLocked -> WalletState.LOCKED
                else -> WalletState.UNLOCKED
            }
        )
    }

    fun sendCoins(sourceAddress: AddressLight?, outputs: List<Output>): List<Sha256Hash> {
        val addressCoinsIndexList: MutableList<AddressCoinsIndex> = ArrayList()
        val totalOutputAmount = outputs.map {
            it.amount.atomicUnits
        }.sum()
        if (sourceAddress == null) {
            for (availableAddress in getAvailableAddresses(totalOutputAmount)) {
                addressCoinsIndexList.add(
                    AddressCoinsIndex(
                        availableAddress.address, availableAddress.availableBalance,
                        getSignatureIndex(availableAddress.address)
                    )
                )
            }
        } else {
            val address = sourceAddress.get()
            val ledgerContext = peerTable.getAddressState(address)
            if (ledgerContext == null) {
                throw SendCoinsException("Information about this address does not exist. Perhaps your node is waiting for this information. Try to do it later.")
            } else if (!ledgerContext.ledgerProofStatus!!.exists()) {
                throw SendCoinsException("Address doesn't exist or is invalid. Check the address you used for this operation.")
            }
            addressCoinsIndexList.add(
                AddressCoinsIndex(
                    address, ledgerContext.ledgerValue!!.availableAtomicUnits,
                    getSignatureIndex(address)
                )
            )
        }
        val totalAvailableBalance = addressCoinsIndexList.map(AddressCoinsIndex::coins).sum()
        if (totalOutputAmount > totalAvailableBalance) {
            throw SendCoinsException("Available balance is not enough. Check your address that you use for this operation.")
        }
        val transactions = transactionService.createTransactionsByOutputList(
            addressCoinsIndexList, outputs
        )
        return transactions.asSequence().onEach {
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
            AddressSignatureIndex(
                address = address,
                poolIndex = getSignatureIndex(address.get()),
                blockchainIndex = peerTable.getSignatureIndex(address.get())!!
            )
        }
    }

    fun submitTransactions(transactions: List<Transaction>) {
        for (transaction in transactions) {
            peerTable.advertise(transaction)
            val added = pendingTransactionContainer.addTransaction(transaction)
            if (!added) {
                throw TransactionSubmissionException("The transaction was not added to the pool")
            }
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
            } else {
                spvContext.addressManager.getAll().forEach {
                    peerTable.acknowledgeAddress(it)
                }
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
                val address = addressManager.getNewAddress()
                if (address != null) {
                    logger.info("New address: {}", address.hash)
                    peerTable.acknowledgeAddress(address)
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

    fun getBalance(addresses: List<AddressLight>): WalletBalance {
        // All of the addresses from the normal address manager will be standard
        val addressLedgerContext = peerTable.getAddressesState()
        if (addresses.isEmpty()) {
            return WalletBalance(
                confirmed = addressLedgerContext.map { (address, ledgerContext) ->
                    getAddressBalance(address.asLightAddress(), ledgerContext)
                },
                unconfirmed = emptyList()
            )
        } else {
            return WalletBalance(
                confirmed = addresses.map { address ->
                    val ledgerContext = addressLedgerContext[address.get()]
                    if (ledgerContext != null) {
                        getAddressBalance(address, ledgerContext)
                    } else {
                        addressManager.monitor(AddressPubKey(address.get(), null))

                        AddressBalance(address, Coin.ZERO, Coin.ZERO, Coin.ZERO)
                    }
                },
                unconfirmed = emptyList()
            )
        }
    }

    fun createAltChainEndorsement(publicationData: ByteArray, sourceAddress: String, feePerByte: Long, maxFee: Long): AltChainEndorsement {
        try {
            val signatureIndex = getSignatureIndex(sourceAddress) + 1
            val fee = feePerByte * predictAltChainEndorsementTransactionSize(publicationData.size, signatureIndex)
            if (fee > maxFee) {
                throw EndorsementCreationException("Calculated fee $fee was above the maximum configured amount $maxFee")
            }
            val tx = transactionService.createUnsignedAltChainEndorsementTransaction(
                sourceAddress, fee, publicationData, signatureIndex
            )
            return AltChainEndorsement((tx as StandardTransaction), signatureIndex)
        } catch (e: Exception) {
            logger.debugWarn(e) { "Failed to create alt chain endorsement" }
            throw EndorsementCreationException("Failed to create alt chain endorsement")
        }
    }

    fun getLastVBKBlockHeader(): BlockHeader {
        val lastBlock = blockchain.blockStore.getChainHead()!!
        val block = lastBlock.block
        return BlockHeader(
            SerializeDeserializeService.serializeHeaders(block),
            block.hash.bytes
        )
    }

    fun getVbkBlockHeader(hash: VBlakeHash): BlockHeader? {
        val block = blockchain.get(hash)
            ?: return null
        return BlockHeader(
            SerializeDeserializeService.serializeHeaders(block.block),
            block.hash.bytes
        )
    }

    fun getVbkBlockHeader(height: Int): BlockHeader? {
        val block = blockchain.getBlockByHeight(height)
            ?: return null
        return BlockHeader(
            block.hash.bytes,
            SerializeDeserializeService.serializeHeaders(block.block)
        )
    }

    fun getLastBitcoinBlock(): Sha256Hash = spvContext.networkParameters.bitcoinOriginBlock.hash    //Mock todo SPV-111

    fun getTransactions(ids: List<Sha256Hash>) = ids.mapNotNull {
        pendingTransactionContainer.getTransactionInfo(it)
    }

    suspend fun getVeriBlockPublications(getVeriBlockPublicationsRequest: GetVeriBlockPublicationsRequest): GetVeriBlockPublicationsReply {
        val advertise = VeriBlockMessages.Event.newBuilder()
            .setId(nextMessageId())
            .setAcknowledge(false)
            .setVeriblockPublicationsRequest(getVeriBlockPublicationsRequest)
            .build()
        val reply = peerTable.requestMessage(advertise, timeoutInMillis = 300_000L)
        return reply.veriblockPublicationsReply
    }

    private fun getAvailableAddresses(totalOutputAmount: Long): List<AddressAvailableBalance> {
        //Use default address if there balance is enough.
        val ledgerContext = peerTable.getAddressState(addressManager.defaultAddress.hash)
            ?: throw WalletException("Could not find default address' state")
        if (ledgerContext.ledgerValue == null || ledgerContext.address == null) {
            throw WalletException("Default address' state's ledger value is unknown! Ledger proof status: ${ledgerContext.ledgerProofStatus}")
        }
        if (ledgerContext.ledgerValue.availableAtomicUnits > totalOutputAmount) {
            return listOf(
                AddressAvailableBalance(ledgerContext.address.address, ledgerContext.ledgerValue.availableAtomicUnits)
            )
        }
        val addressBalanceList: MutableList<AddressAvailableBalance> = ArrayList()
        val ledgerContextMap = peerTable.getAddressesState()
        for (address in addressManager.all) {
            val lc = ledgerContextMap.getValue(address.hash)
            if (ledgerContextMap.containsKey(address.hash) && lc.ledgerValue != null) {
                addressBalanceList.add(
                    AddressAvailableBalance(address.hash, lc.ledgerValue.availableAtomicUnits)
                )
            }
        }
        return addressBalanceList.filter {
            it.availableBalance > 0
        }.sortedByDescending {
            it.availableBalance
        }
    }

    private fun getAddressBalance(address: AddressLight, ledgerContext: LedgerContext): AddressBalance {
        val balance = ledgerContext.ledgerValue?.availableAtomicUnits ?: 0L
        val lockedCoins = ledgerContext.ledgerValue?.frozenAtomicUnits ?: 0L
        return AddressBalance(
            address = address,
            unlockedAmount = (balance - lockedCoins).asCoin(),
            lockedAmount = lockedCoins.asCoin(),
            totalAmount = balance.asCoin()
        )
    }

    private fun getSignatureIndex(address: String): Long {
        return pendingTransactionContainer.getPendingSignatureIndexForAddress(address)
            ?: return peerTable.getSignatureIndex(address)!!
    }
}
