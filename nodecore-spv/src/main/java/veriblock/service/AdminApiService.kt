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
import nodecore.api.grpc.VeriBlockMessages.CreateAltChainEndorsementReply
import nodecore.api.grpc.VeriBlockMessages.CreateAltChainEndorsementRequest
import nodecore.api.grpc.VeriBlockMessages.GetBalanceReply
import nodecore.api.grpc.VeriBlockMessages.GetNewAddressReply
import nodecore.api.grpc.VeriBlockMessages.GetNewAddressRequest
import nodecore.api.grpc.VeriBlockMessages.GetTransactionsReply
import nodecore.api.grpc.VeriBlockMessages.GetTransactionsRequest
import nodecore.api.grpc.VeriBlockMessages.GetVeriBlockPublicationsReply
import nodecore.api.grpc.VeriBlockMessages.GetVeriBlockPublicationsRequest
import nodecore.api.grpc.utilities.ByteStringAddressUtility
import nodecore.api.grpc.utilities.ByteStringUtility
import nodecore.p2p.Constants
import org.veriblock.core.AddressCreationException
import org.veriblock.core.ImportException
import org.veriblock.core.SendCoinsException
import org.veriblock.core.TransactionSubmissionException
import org.veriblock.core.WalletException
import org.veriblock.core.contracts.AddressManager
import org.veriblock.core.types.Pair
import org.veriblock.core.wallet.Address
import org.veriblock.core.wallet.WalletLockedException
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.crypto.VBlakeHash
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.debugError
import org.veriblock.core.utilities.debugInfo
import org.veriblock.core.utilities.debugWarn
import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.sdk.services.SerializeDeserializeService
import veriblock.SpvContext
import veriblock.model.AddressCoinsIndex
import veriblock.model.AddressLight
import veriblock.model.BlockHeader
import veriblock.model.LedgerContext
import veriblock.model.Output
import veriblock.model.StandardTransaction
import veriblock.model.Transaction
import veriblock.model.asLightAddress
import veriblock.net.SpvPeerTable
import veriblock.service.TransactionService.Companion.getRegularTransactionMessageBuilder
import veriblock.service.TransactionService.Companion.predictAltChainEndorsementTransactionSize
import veriblock.util.MessageIdGenerator.next
import java.io.File
import java.io.IOException
import java.util.ArrayList
import java.util.function.Consumer

private val logger = createLogger {}

class AdminApiService(
    private val spvContext: SpvContext,
    private val peerTable: SpvPeerTable,
    private val transactionService: TransactionService,
    private val addressManager: AddressManager,
    private val pendingTransactionContainer: PendingTransactionContainer,
    private val blockchain: Blockchain
) {
    fun getStateInfo(): StateInfo {
        //TODO do real states
        val blockchainState = BlockchainState.LOADED
        val operatingState = OperatingState.STARTED
        val networkState = NetworkState.CONNECTED
        return StateInfo(
            blockchainState = blockchainState,
            operatingState = operatingState,
            networkState = networkState,
            connectedPeerCount = peerTable.getAvailablePeers(),
            networkHeight = peerTable.getBestBlockHeight(),
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
                        availableAddress.first!!, availableAddress.second!!,
                        getSignatureIndex(availableAddress.first)
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

    fun importPrivateKey(privateKey: ByteArray): Address {
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

    fun decryptWallet(passphrase: String?) {
        if (passphrase.isNullOrEmpty()) {
            throw WalletException("A blank passphrase is invalid for decrypting a wallet")
        } else {
            try {
                val result = addressManager.decryptWallet(passphrase.toCharArray())
                if (!result) {
                    throw WalletException("Unable to decrypt wallet, see NodeCore logs")
                }
            } catch (e: IllegalStateException) {
                throw WalletException("Wallet is already encrypted and must be decrypted first")
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
            throw WalletException("Writing wallet backup failed")
        }
    }

    fun importWallet(sourceLocation: String, passphrase: String?) {
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
        } catch (e: Exception) {
            logger.debugInfo(e) { "${e.message}" }
            throw WalletException("Reading wallet file failed")
        }
    }

    fun getNewAddress(count: Int = 1): List<Address> {
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
                confirmed = addressLedgerContext.keys.map { address ->
                    val ledgerContext = addressLedgerContext[address]
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
                        addressManager.monitor(Address(address.get(), null))

                        AddressBalance(address, 0L, 0L, 0L)
                    }
                },
                unconfirmed = emptyList()
            )
        }
    }

    fun createAltChainEndorsement(request: CreateAltChainEndorsementRequest): CreateAltChainEndorsementReply {
        val replyBuilder = CreateAltChainEndorsementReply.newBuilder()
        try {
            val publicationData = request.publicationData.toByteArray()
            val sourceAddress = ByteStringAddressUtility.parseProperAddressTypeAutomatically(request.sourceAddress)
            val signatureIndex = getSignatureIndex(sourceAddress) + 1
            val fee = request.feePerByte * predictAltChainEndorsementTransactionSize(
                publicationData.size, signatureIndex
            )
            if (fee > request.maxFee) {
                replyBuilder.success = false
                replyBuilder.addResults(
                    makeResult(
                        "V008", "Create Alt Endorsement Error",
                        "Calcualated fee (" + fee + ") was above the maximum configured amount (" + request.maxFee + ").", true
                    )
                )
                return replyBuilder.build()
            }
            val tx = transactionService.createUnsignedAltChainEndorsementTransaction(
                sourceAddress, fee, publicationData, signatureIndex
            )
            replyBuilder.success = true
            replyBuilder.setTransaction(getRegularTransactionMessageBuilder((tx as StandardTransaction)))
            replyBuilder.signatureIndex = signatureIndex
        } catch (e: Exception) {
            logger.error("Unable to create alt chain endorsement", e)
            replyBuilder.success = false
            replyBuilder.addResults(makeResult("V008", "Create Alt Endorsement Error", "An error occurred processing request", true))
        }
        return replyBuilder.build()
    }

    fun getLastVBKBlockHeader(): BlockHeader {
        val lastBlock = blockchain.blockStore.chainHead
        val block = lastBlock.block
        return BlockHeader(
            block.hash.bytes,
            SerializeDeserializeService.serializeHeaders(block)
        )
    }

    fun getVbkBlockHeader(hash: ByteString): BlockHeader {
        val block = blockchain[VBlakeHash.wrap(hash.toByteArray())]
        return BlockHeader(
            block.hash.bytes,
            SerializeDeserializeService.serializeHeaders(block.block)
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

    fun getTransactions(request: GetTransactionsRequest): GetTransactionsReply {
        val ids = request.idsList.map {
            Sha256Hash.wrap(it.toByteArray())
        }
        val replyList: List<VeriBlockMessages.TransactionInfo> = ids.map {
            pendingTransactionContainer.getTransactionInfo(it)
        }
        return GetTransactionsReply.newBuilder()
            .addAllTransactions(replyList)
            .build()
    }

    fun getVeriBlockPublications(getVeriBlockPublicationsRequest: GetVeriBlockPublicationsRequest?): GetVeriBlockPublicationsReply {
        val advertise = VeriBlockMessages.Event.newBuilder()
            .setId(next())
            .setAcknowledge(false)
            .setVeriblockPublicationsRequest(getVeriBlockPublicationsRequest)
            .build()
        val futureEventReply = peerTable.advertiseWithReply(advertise)
        return futureEventReply.get().veriblockPublicationsReply
    }

    private fun getAvailableAddresses(totalOutputAmount: Long): List<Pair<String, Long>> {
        val addressCoinsForPayment: MutableList<Pair<String, Long>> = ArrayList()

        //Use default address if there balance is enough.
        val ledgerContext = peerTable.getAddressState(addressManager.defaultAddress.hash)!!
        if (ledgerContext.ledgerValue!!.availableAtomicUnits > totalOutputAmount) {
            return listOf(
                Pair(ledgerContext.address!!.address, ledgerContext.ledgerValue!!.availableAtomicUnits)
            )
        }
        val addressBalanceList: MutableList<Pair<String, Long>> = ArrayList()
        val ledgerContextMap = peerTable.getAddressesState()
        for (address in addressManager.all) {
            if (ledgerContextMap.containsKey(address.hash) && ledgerContextMap[address.hash]!!.ledgerValue != null) {
                addressBalanceList
                    .add(
                        Pair(
                            address.hash, ledgerContextMap[address.hash]!!.ledgerValue!!.availableAtomicUnits
                        )
                    )
            }
        }
        return addressBalanceList.filter {
            it.second > 0
        }.sortedBy {
            it.second
        }
    }

    private fun getAddressBalance(address: AddressLight, ledgerContext: LedgerContext?): AddressBalance {
        val balance = ledgerContext?.ledgerValue?.availableAtomicUnits ?: 0L
        val lockedCoins = ledgerContext?.ledgerValue?.frozenAtomicUnits ?: 0L
        return AddressBalance(
            address = address,
            unlockedAmount = balance - lockedCoins,
            lockedAmount = lockedCoins,
            totalAmount = balance
        )
    }

    private fun getSignatureIndex(address: String): Long {
        return pendingTransactionContainer.getPendingSignatureIndexForAddress(address)
            ?: return peerTable.getSignatureIndex(address)!!
    }

    private fun makeResult(
        code: String,
        message: String,
        details: String?,
        error: Boolean
    ): VeriBlockMessages.Result {
        return VeriBlockMessages.Result
            .newBuilder()
            .setCode(code)
            .setMessage(message)
            .setDetails(details)
            .setError(error)
            .build()
    }
}
