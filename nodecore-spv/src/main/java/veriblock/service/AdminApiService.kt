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
import nodecore.api.grpc.VeriBlockMessages.BackupWalletReply
import nodecore.api.grpc.VeriBlockMessages.BackupWalletRequest
import nodecore.api.grpc.VeriBlockMessages.CreateAltChainEndorsementReply
import nodecore.api.grpc.VeriBlockMessages.CreateAltChainEndorsementRequest
import nodecore.api.grpc.VeriBlockMessages.DecryptWalletRequest
import nodecore.api.grpc.VeriBlockMessages.DumpPrivateKeyReply
import nodecore.api.grpc.VeriBlockMessages.DumpPrivateKeyRequest
import nodecore.api.grpc.VeriBlockMessages.EncryptWalletRequest
import nodecore.api.grpc.VeriBlockMessages.GetBalanceReply
import nodecore.api.grpc.VeriBlockMessages.GetBalanceRequest
import nodecore.api.grpc.VeriBlockMessages.GetLastBitcoinBlockReply
import nodecore.api.grpc.VeriBlockMessages.GetLastBitcoinBlockRequest
import nodecore.api.grpc.VeriBlockMessages.GetNewAddressReply
import nodecore.api.grpc.VeriBlockMessages.GetNewAddressRequest
import nodecore.api.grpc.VeriBlockMessages.GetSignatureIndexReply
import nodecore.api.grpc.VeriBlockMessages.GetSignatureIndexRequest
import nodecore.api.grpc.VeriBlockMessages.GetStateInfoReply
import nodecore.api.grpc.VeriBlockMessages.GetTransactionsReply
import nodecore.api.grpc.VeriBlockMessages.GetTransactionsRequest
import nodecore.api.grpc.VeriBlockMessages.GetVeriBlockPublicationsReply
import nodecore.api.grpc.VeriBlockMessages.GetVeriBlockPublicationsRequest
import nodecore.api.grpc.VeriBlockMessages.ImportPrivateKeyReply
import nodecore.api.grpc.VeriBlockMessages.ImportPrivateKeyRequest
import nodecore.api.grpc.VeriBlockMessages.ImportWalletReply
import nodecore.api.grpc.VeriBlockMessages.ImportWalletRequest
import nodecore.api.grpc.VeriBlockMessages.LockWalletRequest
import nodecore.api.grpc.VeriBlockMessages.ProtocolReply
import nodecore.api.grpc.VeriBlockMessages.SendCoinsReply
import nodecore.api.grpc.VeriBlockMessages.SendCoinsRequest
import nodecore.api.grpc.VeriBlockMessages.SubmitTransactionsRequest
import nodecore.api.grpc.VeriBlockMessages.TransactionUnion
import nodecore.api.grpc.VeriBlockMessages.UnlockWalletRequest
import nodecore.api.grpc.utilities.ByteStringAddressUtility
import nodecore.api.grpc.utilities.ByteStringUtility
import nodecore.p2p.Constants
import org.slf4j.LoggerFactory
import org.veriblock.core.NotFoundException
import org.veriblock.core.SendCoinsException
import org.veriblock.core.VeriBlockError
import org.veriblock.core.bitcoinj.Base58
import org.veriblock.core.contracts.AddressManager
import org.veriblock.core.types.Pair
import org.veriblock.core.utilities.AddressUtility
import org.veriblock.core.utilities.Utility
import org.veriblock.core.wallet.Address
import org.veriblock.core.wallet.WalletLockedException
import org.veriblock.sdk.models.Coin
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.sdk.models.VBlakeHash
import org.veriblock.sdk.services.SerializeDeserializeService
import veriblock.SpvContext
import veriblock.model.AddressCoinsIndex
import veriblock.model.AddressLight
import veriblock.model.LedgerContext
import veriblock.model.Output
import veriblock.model.StandardAddress
import veriblock.model.StandardTransaction
import veriblock.net.SpvPeerTable
import veriblock.service.TransactionService.Companion.getRegularTransactionMessageBuilder
import veriblock.service.TransactionService.Companion.predictAltChainEndorsementTransactionSize
import veriblock.util.MessageIdGenerator.next
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.PrivateKey
import java.util.ArrayList
import java.util.function.Consumer

class AdminApiService(
    private val spvContext: SpvContext,
    private val peerTable: SpvPeerTable,
    private val transactionService: TransactionService,
    private val addressManager: AddressManager,
    private val transactionFactory: TransactionFactory,
    private val pendingTransactionContainer: PendingTransactionContainer,
    private val blockchain: Blockchain
) {
    fun getStateInfo(): StateInfo {
        //TODO do real states.
        val blockchainState = BlockchainState.LOADED
        val operatingState = OperatingState.STARTED
        val networkState = NetworkState.CONNECTED
        return StateInfo(
            //TODO do real statuses.
            blockchainState = blockchainState,
            operatingState = operatingState,
            networkState = networkState,
            connectedPeerCount = peerTable.getAvailablePeers(),
            networkHeight = peerTable.getBestBlockHeight(),
            localBlockchainHeight = blockchain.getChainHead()!!.height,
            networkVersion = spvContext.networkParameters.name,
            dataDirectory = spvContext.directory.path,
            programVersion = Constants.PROGRAM_VERSION ?: "UNKNOWN",
            nodecoreStartTime = spvContext.startTime.epochSecond,
            walletCacheSyncHeight = blockchain.getChainHead()!!.height,
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

    fun getSignatureIndex(request: GetSignatureIndexRequest): GetSignatureIndexReply {
        val replyBuilder = GetSignatureIndexReply.newBuilder()
        replyBuilder.success = true
        val addresses = ArrayList<String>()
        if (request.addressesCount > 0) {
            for (value in request.addressesList) {
                addresses.add(ByteStringAddressUtility.parseProperAddressTypeAutomatically(value))
            }
        } else {
            addresses.add(addressManager.defaultAddress.hash)
        }
        for (address in addresses) {
            var addressBytes: ByteArray?
            addressBytes = if (AddressUtility.isValidMultisigAddress(address)) {
                Utility.base59ToBytes(address)
            } else {
                Utility.base58ToBytes(address)
            }
            val blockchainLastSignatureIndex = peerTable.getSignatureIndex(address)!!
            val poolLastSignatureIndex = getSignatureIndex(address)
            replyBuilder.addIndexes(
                VeriBlockMessages.AddressSignatureIndexes
                    .newBuilder()
                    .setAddress(ByteString.copyFrom(addressBytes))
                    .setBlockchainIndex(blockchainLastSignatureIndex)
                    .setPoolIndex(poolLastSignatureIndex)
            )
        }
        return replyBuilder.build()
    }

    fun submitTransactions(request: SubmitTransactionsRequest): ProtocolReply {
        val replyBuilder = ProtocolReply.newBuilder()
        replyBuilder.success = true
        for (union in request.transactionsList) {
            var added = false
            when (union.transactionCase) {
                TransactionUnion.TransactionCase.UNSIGNED -> {
                    replyBuilder.success = false
                    replyBuilder.addResults(
                        makeResult(
                            "V008",
                            "Transaction is unsigned!",
                            "Unsigned transactions cannot be submitted to the network without signing first",
                            true
                        )
                    )
                }
                TransactionUnion.TransactionCase.SIGNED -> {
                    val t = transactionFactory.create(union.signed)
                    peerTable.advertise(t)
                    added = pendingTransactionContainer.addTransaction(t)
                }
                TransactionUnion.TransactionCase.SIGNED_MULTISIG -> {
                    val multisigTransaction = transactionFactory.create(union.signedMultisig)
                    peerTable.advertise(multisigTransaction)
                    added = pendingTransactionContainer.addTransaction(multisigTransaction)
                }
                TransactionUnion.TransactionCase.TRANSACTION_NOT_SET -> {
                    replyBuilder.success = false
                    replyBuilder.addResults(
                        makeResult(
                            "V008",
                            "Invalid transaction type",
                            "Either a signed or unsigned transaction should be passed",
                            true
                        )
                    )
                }
            }
            if (!added) {
                replyBuilder.success = false
                replyBuilder.addResults(
                    makeResult(
                        "V008",
                        "Submit transaction error",
                        "The transaction was not added to the pool",
                        true
                    )
                )
            }
        }
        return replyBuilder.build()
    }

    fun dumpPrivateKey(request: DumpPrivateKeyRequest): DumpPrivateKeyReply {
        val replyBuilder = DumpPrivateKeyReply.newBuilder()
        if (!ByteStringAddressUtility.isByteStringValidAddress(request.address)) {
            replyBuilder.success = false
            replyBuilder.addResults(
                makeResult(
                    "V008", "Invalid Address",
                    "The provided address is not a valid address", true
                )
            )
            return replyBuilder.build()
        }
        val address = ByteStringAddressUtility.parseProperAddressTypeAutomatically(request.address)
        val publicKey = addressManager.getPublicKeyForAddress(address)
        val privateKey: PrivateKey?
        try {
            privateKey = addressManager.getPrivateKeyForAddress(address)
        } catch (e: IllegalStateException) {
            replyBuilder.success = false
            replyBuilder.addResults(makeResult("V008", "Wallet Locked", e.message, true))
            return replyBuilder.build()
        }
        if (privateKey == null || publicKey == null) {
            replyBuilder.success = false
            replyBuilder.addResults(
                makeResult(
                    "V008",
                    "The provided address is not an address in this wallet!",
                    "$address is not a valid address!", true
                )
            )
            return replyBuilder.build()
        }
        val privateKeyBytes = privateKey.encoded
        val publicKeyBytes = publicKey.encoded
        val fullBytes = ByteArray(privateKeyBytes.size + publicKeyBytes.size + 1)
        fullBytes[0] = privateKeyBytes.size.toByte()
        System.arraycopy(privateKeyBytes, 0, fullBytes, 1, privateKeyBytes.size)
        System.arraycopy(publicKeyBytes, 0, fullBytes, 1 + privateKeyBytes.size, publicKeyBytes.size)
        replyBuilder.address = ByteString.copyFrom(Base58.decode(address))
        replyBuilder.privateKey = ByteString.copyFrom(fullBytes)
        replyBuilder.success = true
        return replyBuilder.build()
    }

    fun importPrivateKey(request: ImportPrivateKeyRequest): ImportPrivateKeyReply {
        val replyBuilder = ImportPrivateKeyReply.newBuilder()
        if (addressManager.isLocked) {
            replyBuilder.success = false
            replyBuilder.addResults(makeResult("V045", "Import Failed", "Wallet must be unlocked before importing a key", true))
            return replyBuilder.build()
        }

        // The "Private" key is really the public and private key together encoded
        val fullBytes = request.privateKey.toByteArray()
        val privateKeyBytes = ByteArray(fullBytes[0].toInt())
        val publicKeyBytes = ByteArray(fullBytes.size - fullBytes[0] - 1)
        System.arraycopy(fullBytes, 1, privateKeyBytes, 0, privateKeyBytes.size)
        System.arraycopy(fullBytes, privateKeyBytes.size + 1, publicKeyBytes, 0, publicKeyBytes.size)
        val importedAddress = addressManager.importKeyPair(publicKeyBytes, privateKeyBytes)
        if (importedAddress == null) {
            replyBuilder.success = false
            replyBuilder.addResults(
                makeResult(
                    "V008", "The provided private key was invalid or corrupted!",
                    Utility.bytesToHex(fullBytes) + " is  not a valid private key!", true
                )
            )
            return replyBuilder.build()
        }
        replyBuilder.resultantAddress = ByteStringAddressUtility.createProperByteStringAutomatically(importedAddress.hash)
        replyBuilder.success = true
        return replyBuilder.build()
    }

    fun encryptWallet(request: EncryptWalletRequest): ProtocolReply {
        val replyBuilder = ProtocolReply.newBuilder()
        if (request.passphraseBytes.size() <= 0) {
            replyBuilder.success = false
            replyBuilder.addResults(
                makeResult(
                    "V041", "Invalid passphrase",
                    "A blank passphrase is invalid for encrypting a wallet", true
                )
            )
        } else {
            val passphrase = request.passphrase.toCharArray()
            try {
                val result = addressManager.encryptWallet(passphrase)
                if (result) {
                    replyBuilder.success = true
                    replyBuilder.addResults(
                        makeResult(
                            "V200", "Success",
                            "Wallet has been encrypted with supplied passphrase", false
                        )
                    )
                } else {
                    replyBuilder.success = false
                    replyBuilder.addResults(
                        makeResult(
                            "V043", "Encryption Failed",
                            "Unable to encrypt wallet, see NodeCore logs", true
                        )
                    )
                }
            } catch (e: IllegalStateException) {
                replyBuilder.success = false
                replyBuilder.addResults(
                    makeResult(
                        "V042", "Wallet already encrypted",
                        "Wallet is already encrypted and must be decrypted first", true
                    )
                )
            }
        }
        return replyBuilder.build()
    }

    fun decryptWallet(request: DecryptWalletRequest): ProtocolReply {
        val replyBuilder = ProtocolReply.newBuilder()
        if (request.passphraseBytes.size() <= 0) {
            replyBuilder.success = false
            replyBuilder.addResults(
                makeResult(
                    "V041", "Invalid passphrase",
                    "A blank passphrase is invalid for decrypting a wallet", true
                )
            )
        } else {
            val passphrase = request.passphrase.toCharArray()
            try {
                val result = addressManager.decryptWallet(passphrase)
                if (result) {
                    replyBuilder.success = true
                    replyBuilder.addResults(
                        makeResult(
                            "V200", "Success",
                            "Wallet has been decrypted", false
                        )
                    )
                } else {
                    replyBuilder.success = false
                    replyBuilder.addResults(
                        makeResult(
                            "V044", "Decryption Failed",
                            "Unable to decrypt wallet, see NodeCore logs", true
                        )
                    )
                }
            } catch (e: IllegalStateException) {
                replyBuilder.success = false
                replyBuilder.addResults(
                    makeResult(
                        "V042", "Wallet already encrypted",
                        "Wallet is already encrypted and must be decrypted first", true
                    )
                )
            }
        }
        return replyBuilder.build()
    }

    fun unlockWallet(request: UnlockWalletRequest): ProtocolReply {
        val replyBuilder = ProtocolReply.newBuilder()
        if (request.passphraseBytes.size() <= 0) {
            replyBuilder.success = false
            replyBuilder.addResults(
                makeResult(
                    "V041", "Invalid passphrase",
                    "Passphrase is invalid for unlocking this wallet", true
                )
            )
        } else {
            val passphrase = request.passphrase.toCharArray()
            val result = addressManager.unlock(passphrase)
            if (result) {
                replyBuilder.success = true
                replyBuilder.addResults(
                    makeResult(
                        "V200", "Success",
                        "Wallet has been unlocked", false
                    )
                )
            } else {
                replyBuilder.success = false
                replyBuilder.addResults(
                    makeResult(
                        "V041", "Invalid passphrase",
                        "Passphrase is invalid for unlocking this wallet", true
                    )
                )
            }
        }
        return replyBuilder.build()
    }

    fun lockWallet(request: LockWalletRequest?): ProtocolReply {
        val replyBuilder = ProtocolReply.newBuilder()
        addressManager.lock()
        replyBuilder.success = true
        replyBuilder.addResults(
            makeResult(
                "V200", "Success",
                "Wallet has been locked", false
            )
        )
        return replyBuilder.build()
    }

    fun backupWallet(request: BackupWalletRequest): BackupWalletReply {
        val replyBuilder = BackupWalletReply.newBuilder()
        try {
            val backupLocation = String(request.targetLocation.toByteArray())
            val saveWalletResult = addressManager.saveWalletToFile(backupLocation)
            val success = saveWalletResult.first
            if (!success) {
                replyBuilder.success = false
                replyBuilder.addResults(
                    makeResult(
                        "V008",
                        "Unable to save backup wallet file!",
                        saveWalletResult.second,
                        true
                    )
                )
            } else {
                replyBuilder.success = true
            }
        } catch (e: Exception) {
            replyBuilder.success = false
            replyBuilder.addResults(
                makeResult(
                    "V008",
                    "Writing wallet backup failed!",
                    "The following error occurred while writing the backup files: " + e.message + ".",
                    true
                )
            )
        }
        return replyBuilder.build()
    }

    fun importWallet(request: ImportWalletRequest): ImportWalletReply {
        val replyBuilder = ImportWalletReply.newBuilder()
        if (addressManager.isLocked) {
            replyBuilder.success = false
            replyBuilder.addResults(
                makeResult(
                    "V045", "Import Failed",
                    "Wallet must be unlocked before importing another wallet", true
                )
            )
            return replyBuilder.build()
        }
        try {
            val importFromLocation = String(request.sourceLocation.toByteArray(), StandardCharsets.UTF_8)
            val passphrase = request.passphrase
            val result: Pair<Boolean, String>
            result = if (passphrase != null && passphrase.length > 0) {
                addressManager.importEncryptedWallet(File(importFromLocation), passphrase.toCharArray())
            } else {
                addressManager.importWallet(File(importFromLocation))
            }
            val success = result.first
            if (!success) {
                replyBuilder.success = false
                replyBuilder.addResults(
                    makeResult(
                        "V008",
                        "Unable to load/import wallet file!",
                        result.second,
                        true
                    )
                )
            } else {
                replyBuilder.success = true
            }
        } catch (e: Exception) {
            logger.error(e.message, e)
            replyBuilder.success = false
            replyBuilder.addResults(
                makeResult(
                    "V008",
                    "Reading wallet file failed!",
                    "The following error occurred while reading the wallet file: " + e.message + ".",
                    true
                )
            )
        }
        return replyBuilder.build()
    }

    fun getNewAddress(request: GetNewAddressRequest): GetNewAddressReply {
        val replyBuilder = GetNewAddressReply.newBuilder()
        replyBuilder.success = true
        if (addressManager.isLocked) {
            replyBuilder.success = false
            replyBuilder.addResults(makeResult("V008", "Address Creation Failed", "Wallet must be unlocked before creating a new address", true))
        } else {
            try {
                var count = request.count
                if (count < 1) {
                    count = 1
                }
                var success = true
                val addresses: MutableList<Address> = ArrayList(
                    count
                )
                for (i in 0 until count) {
                    val address = addressManager.newAddress
                    if (address != null) {
                        addresses.add(address)
                        logger.info("New address: {}", address.hash)
                    } else {
                        success = false
                        replyBuilder.success = false
                        replyBuilder.addResults(makeResult("V008", "Address Creation Failed", "Unable to generate new address", true))
                        break
                    }
                }
                if (success) {
                    if (addresses.size > 0) {
                        // New addresses from the normal address manager will always be standard addresses
                        replyBuilder.address = ByteStringUtility.base58ToByteString(addresses[0].hash)
                    }
                    if (addresses.size > 1) {
                        addresses.subList(1, addresses.size)
                            .forEach(
                                Consumer { a: Address ->
                                    replyBuilder.addAdditionalAddresses(
                                        ByteStringUtility.base58ToByteString(a.hash)
                                    )
                                }
                            )
                    }
                    replyBuilder.addResults(
                        makeResult("V200", "Wallet Updated", "The wallet has been modified. Please make a backup of the wallet data file.", false)
                    )
                }
            } catch (e: IOException) {
                logger.error(e.message, e)
                replyBuilder.success = false
                replyBuilder.addResults(makeResult("V008", "Address Creation Failed", "Unable to generate new address", true))
            } catch (e: WalletLockedException) {
                logger.warn(e.message)
                replyBuilder.success = false
                replyBuilder.addResults(makeResult("V008", "Address Creation Failed", "Wallet must be unlocked before creating a new address", true))
            }
        }
        return replyBuilder.build()
    }

    fun getBalance(request: GetBalanceRequest): GetBalanceReply {
        val replyBuilder = GetBalanceReply.newBuilder()
        // All of the addresses from the normal address manager will be standard
        val addressLedgerContext = peerTable.getAddressesState()
        if (request.addressesCount == 0) {
            for (address in addressLedgerContext.keys) {
                val ledgerContext = addressLedgerContext[address]
                formGetBalanceReply(address, ledgerContext, replyBuilder)
            }
        } else {
            for (address in request.addressesList) {
                val addressString = ByteStringAddressUtility.parseProperAddressTypeAutomatically(address)
                val ledgerContext = addressLedgerContext[addressString]
                if (ledgerContext != null) {
                    formGetBalanceReply(addressString, ledgerContext, replyBuilder)
                } else {
                    addressManager.monitor(Address(addressString, null))
                    replyBuilder.addConfirmed(
                        VeriBlockMessages.AddressBalance
                            .newBuilder()
                            .setAddress(address)
                            .setLockedAmount(0L)
                            .setUnlockedAmount(0L)
                            .setTotalAmount(0L)
                    )
                    replyBuilder.addUnconfirmed(
                        VeriBlockMessages.Output
                            .newBuilder()
                            .setAddress(address)
                            .setAmount(0L)
                    )
                }
            }
        }
        replyBuilder.success = true
        return replyBuilder.build()
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

    fun getLastVBKBlockHeader(): VeriBlockMessages.BlockHeader {
        val lastBlock = blockchain.blockStore.chainHead
        val block = lastBlock.block
        return VeriBlockMessages.BlockHeader.newBuilder()
            .setHash(ByteString.copyFrom(block.hash.bytes))
            .setHeader(ByteString.copyFrom(SerializeDeserializeService.serializeHeaders(block)))
            .build()
    }

    fun getVbkBlockHeader(hash: ByteString): VeriBlockMessages.BlockHeader {
        val block = blockchain[VBlakeHash.wrap(hash.toByteArray())]
        return VeriBlockMessages.BlockHeader.newBuilder()
            .setHash(ByteString.copyFrom(block.hash.bytes))
            .setHeader(ByteString.copyFrom(SerializeDeserializeService.serializeHeaders(block.block)))
            .build()
    }

    fun getVbkBlockHeader(height: Int): VeriBlockMessages.BlockHeader? {
        val block = blockchain.getBlockByHeight(height)
            ?: return null
        return VeriBlockMessages.BlockHeader.newBuilder()
            .setHash(ByteString.copyFrom(block.hash.bytes))
            .setHeader(ByteString.copyFrom(SerializeDeserializeService.serializeHeaders(block.block)))
            .build()
    }

    fun getLastBitcoinBlock(request: GetLastBitcoinBlockRequest): GetLastBitcoinBlockReply {
        val replyBuilder = GetLastBitcoinBlockReply.newBuilder()
        replyBuilder.success = true

        //Mock todo SPV-111
        val block = spvContext.networkParameters.bitcoinOriginBlock
        replyBuilder.hash = ByteString.copyFrom(block.hash.bytes)
        return replyBuilder.build()
    }

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

    private fun formGetBalanceReply(address: String, ledgerContext: LedgerContext?, replyBuilder: GetBalanceReply.Builder) {
        var balance = 0L
        var lockedCoins = 0L
        if (ledgerContext != null && ledgerContext.ledgerValue != null) {
            balance = ledgerContext.ledgerValue!!.availableAtomicUnits
            lockedCoins = ledgerContext.ledgerValue!!.frozenAtomicUnits
        }
        replyBuilder.addConfirmed(
            VeriBlockMessages.AddressBalance
                .newBuilder()
                .setAddress(ByteStringUtility.base58ToByteString(address))
                .setLockedAmount(lockedCoins)
                .setUnlockedAmount(balance - lockedCoins)
                .setTotalAmount(balance)
        )
        replyBuilder.addUnconfirmed(
            VeriBlockMessages.Output
                .newBuilder()
                .setAddress(ByteStringUtility.base58ToByteString(address))
                .setAmount(0L)
        )
    }

    private fun getSignatureIndex(address: String?): Long {
        return pendingTransactionContainer.getPendingSignatureIndexForAddress(address!!)
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

    companion object {
        private val logger = LoggerFactory.getLogger(AdminApiService::class.java)
    }

}
