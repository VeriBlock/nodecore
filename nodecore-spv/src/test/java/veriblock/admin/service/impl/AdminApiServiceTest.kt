package veriblock.admin.service.impl

import com.google.protobuf.ByteString
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import nodecore.api.grpc.VeriBlockMessages
import nodecore.api.grpc.VeriBlockMessages.*
import nodecore.api.grpc.utilities.ByteStringAddressUtility
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.veriblock.core.bitcoinj.Base58
import org.veriblock.core.contracts.AddressManager
import org.veriblock.core.types.Pair
import org.veriblock.core.wallet.Address
import org.veriblock.core.wallet.WalletLockedException
import org.veriblock.sdk.models.Coin
import org.veriblock.sdk.models.Sha256Hash
import veriblock.SpvContext
import veriblock.conf.TestNetParameters
import veriblock.model.LedgerContext
import veriblock.model.LedgerProofStatus
import veriblock.model.LedgerValue
import veriblock.model.StandardAddress
import veriblock.model.StandardTransaction
import veriblock.model.Transaction
import veriblock.net.LocalhostDiscovery
import veriblock.net.SpvPeerTable
import veriblock.service.AdminApiService
import veriblock.service.Blockchain
import veriblock.service.PendingTransactionContainer
import veriblock.service.TransactionFactory
import veriblock.service.TransactionService
import java.io.IOException
import java.security.KeyPairGenerator

class AdminApiServiceTest {
    private val spvContext = SpvContext()
    private lateinit var transactionService: TransactionService
    private lateinit var addressManager: AddressManager
    private lateinit var peerTable: SpvPeerTable
    private lateinit var adminApiService: AdminApiService
    private lateinit var transactionFactory: TransactionFactory
    private lateinit var transactionContainer: PendingTransactionContainer
    private lateinit var blockchain: Blockchain

    @Before
    fun setUp() {
        spvContext.init(TestNetParameters, LocalhostDiscovery(TestNetParameters), false)
        peerTable = mockk(relaxed = true)
        transactionService = mockk(relaxed = true)
        addressManager = mockk(relaxed = true)
        transactionFactory = mockk(relaxed = true)
        transactionContainer = mockk(relaxed = true)
        blockchain = mockk(relaxed = true)
        adminApiService = AdminApiService(
            spvContext, peerTable, transactionService, addressManager, transactionFactory, transactionContainer, blockchain
        )
    }

    @Test
    fun sendCoins() {
        val transaction: Transaction = StandardTransaction(Sha256Hash.ZERO_HASH)
        val transactions = listOf(transaction)
        val ledgerContext = LedgerContext().also {
            it.ledgerValue = LedgerValue(100L, 0L, 0L)
            it.ledgerProofStatus = LedgerProofStatus.ADDRESS_EXISTS
        }
        val output = Output.newBuilder()
            .setAddress(ByteStringAddressUtility.createProperByteStringAutomatically("VDBt3GuwPe1tA5m4duTPkBq5vF22rw"))
            .setAmount(100)
            .build()
        val request = SendCoinsRequest.newBuilder()
            .addAmounts(output)
            .setSourceAddress(ByteStringAddressUtility.createProperByteStringAutomatically("VcspPDtJNpNmLV8qFTqb2F5157JNHS"))
            .build()

        every { peerTable.getAddressState(any()) } returns ledgerContext
        every { transactionService.predictStandardTransactionToAllStandardOutputSize(any(), any(), any(), any()) } returns 500
        every { transactionService.createTransactionsByOutputList(any(), any()) } returns transactions
        every { transactionContainer.getPendingSignatureIndexForAddress(any())}
        every { transactionContainer.getPendingSignatureIndexForAddress(any()) } returns 1
        every { peerTable.advertise(any()) } returns Unit

        val reply = adminApiService.sendCoins(request)

        verify(exactly = 1) { transactionService.createTransactionsByOutputList(any(), any()) }
        verify(exactly = 1) { transactionContainer.getPendingSignatureIndexForAddress(any()) }
        verify(exactly = 1) { peerTable.advertise(any()) }

        Assert.assertTrue(reply.success)
        Assert.assertNotNull(reply.getTxIds(0))
        Assert.assertTrue(Sha256Hash.wrap(reply.getTxIds(0).toByteArray()) == transaction.txId)
    }

    @Test
    fun sendCoinsWhenAddressDoesntExist() {
        val transaction: Transaction = StandardTransaction(Sha256Hash.ZERO_HASH)
        val ledgerContext = LedgerContext().also {
            it.ledgerValue = LedgerValue(100L, 0L, 0L)
            it.ledgerProofStatus = LedgerProofStatus.ADDRESS_DOES_NOT_EXIST
        }
        val output = Output.newBuilder()
            .setAddress(ByteStringAddressUtility.createProperByteStringAutomatically("VDBt3GuwPe1tA5m4duTPkBq5vF22rw"))
            .setAmount(100)
            .build()
        val request = SendCoinsRequest.newBuilder()
            .addAmounts(output)
            .setSourceAddress(ByteStringAddressUtility.createProperByteStringAutomatically("VcspPDtJNpNmLV8qFTqb2F5157JNHS"))
            .build()
        every { transactionService.predictStandardTransactionToAllStandardOutputSize(any(), any(), any(), any()) } returns 500
        every { transactionService.createStandardTransaction(any(), any(), any(), any()) } returns transaction
        every { transactionContainer.getPendingSignatureIndexForAddress(any()) } returns 1L
        every { peerTable.getAddressState(any()) } returns ledgerContext
        every { peerTable.advertise(any()) } returns Unit
        val reply = adminApiService.sendCoins(request)
        Assert.assertFalse(reply.success)
        Assert.assertTrue(reply.getResults(0).message.contains("Address doesn't exist or invalid"))
    }

    @Test
    fun sendCoinsWhenAddressDoesntIsInvalid() {
        val transaction: Transaction = StandardTransaction(Sha256Hash.ZERO_HASH)
        val ledgerContext = LedgerContext().also {
            it.ledgerValue = LedgerValue(100L, 0L, 0L)
            it.ledgerProofStatus = LedgerProofStatus.ADDRESS_IS_INVALID
        }
        val output = Output.newBuilder()
            .setAddress(ByteStringAddressUtility.createProperByteStringAutomatically("VDBt3GuwPe1tA5m4duTPkBq5vF22rw"))
            .setAmount(100)
            .build()
        val request = SendCoinsRequest.newBuilder()
            .addAmounts(output)
            .setSourceAddress(ByteStringAddressUtility.createProperByteStringAutomatically("VcspPDtJNpNmLV8qFTqb2F5157JNHS"))
            .build()
        every { transactionService.predictStandardTransactionToAllStandardOutputSize(any(), any(), any(), any()) } returns 500
        every { transactionService.createStandardTransaction(any(), any(), any(), any()) } returns transaction
        every { transactionContainer.getPendingSignatureIndexForAddress(any()) } returns 1L
        every { peerTable.getAddressState(any()) } returns ledgerContext
        every { peerTable.advertise(any()) } returns Unit
        val reply = adminApiService.sendCoins(request)
        Assert.assertFalse(reply.success)
        Assert.assertTrue(reply.getResults(0).message.contains("Address doesn't exist or invalid"))
    }

    @Test
    fun sendCoinsWhenBalanceIsNotEnough() {
        val transaction: Transaction = StandardTransaction(Sha256Hash.ZERO_HASH)
        val ledgerContext = LedgerContext().also {
            it.ledgerValue = LedgerValue(50L, 0L, 0L)
            it.ledgerProofStatus = LedgerProofStatus.ADDRESS_EXISTS
        }
        val output = Output.newBuilder()
            .setAddress(ByteStringAddressUtility.createProperByteStringAutomatically("VDBt3GuwPe1tA5m4duTPkBq5vF22rw"))
            .setAmount(100)
            .build()
        val request = SendCoinsRequest.newBuilder()
            .addAmounts(output)
            .setSourceAddress(ByteStringAddressUtility.createProperByteStringAutomatically("VcspPDtJNpNmLV8qFTqb2F5157JNHS"))
            .build()
        every { transactionService.predictStandardTransactionToAllStandardOutputSize(any(), any(), any(), any()) } returns 500
        every { transactionService.createStandardTransaction(any(), any(), any(), any()) } returns transaction
        every { transactionContainer.getPendingSignatureIndexForAddress(any()) } returns 1L
        every { peerTable.getAddressState(any()) } returns ledgerContext
        every { peerTable.advertise(any()) } returns Unit
        val reply = adminApiService.sendCoins(request)
        Assert.assertFalse(reply.success)
        Assert.assertTrue(reply.getResults(0).message.contains("Available balance is not enough"))
    }

    @Test
    fun sendCoinsWhenAddressInfoDoentExist() {
        val transaction: Transaction = StandardTransaction(Sha256Hash.ZERO_HASH)
        val output = Output.newBuilder()
            .setAddress(ByteStringAddressUtility.createProperByteStringAutomatically("VDBt3GuwPe1tA5m4duTPkBq5vF22rw"))
            .setAmount(100)
            .build()
        val request = SendCoinsRequest.newBuilder()
            .addAmounts(output)
            .setSourceAddress(ByteStringAddressUtility.createProperByteStringAutomatically("VcspPDtJNpNmLV8qFTqb2F5157JNHS"))
            .build()
        every { transactionService.predictStandardTransactionToAllStandardOutputSize(any(), any(), any(), any()) } returns 500
        every { transactionService.createStandardTransaction(any(), any(), any(), any()) } returns transaction
        every { transactionContainer.getPendingSignatureIndexForAddress(any()) } returns 1L
        every { peerTable.getAddressState(any()) } returns null
        every { peerTable.advertise(any()) } returns Unit
        val reply = adminApiService.sendCoins(request)
        Assert.assertFalse(reply.success)
        Assert.assertTrue(reply.getResults(0).message.contains("Information about this address does not exist"))
    }

    @Test
    fun unlockWalletWhenWalletIsLockThenTrue() {
        val unlockWalletRequest = UnlockWalletRequest.newBuilder().setPassphrase("123").build()
        every { addressManager.unlock(any()) } returns true
        val reply = adminApiService.unlockWallet(unlockWalletRequest)
        verify(exactly = 1) { addressManager.unlock(any()) }
        Assert.assertEquals(true, reply.success)
    }

    @Test
    fun unlockWalletWhenWalletIsUnlockThenFalse() {
        val unlockWalletRequest = UnlockWalletRequest.newBuilder().setPassphrase("123").build()
        every { addressManager.unlock(any()) } returns false
        val reply = adminApiService.unlockWallet(unlockWalletRequest)
        verify(exactly = 1) { addressManager.unlock(any()) }
        Assert.assertEquals(false, reply.success)
    }

    @Test
    fun importWalletWhenWalletIsLockThenFalse() {
        val importWalletRequest = ImportWalletRequest.newBuilder()
            .setPassphrase("123")
            .setSourceLocation(ByteString.copyFromUtf8("test/source"))
            .build()
        every { addressManager.isLocked } returns true
        val reply = adminApiService.importWallet(importWalletRequest)
        verify(exactly = 1) { addressManager.isLocked }
        Assert.assertEquals(false, reply.success)
    }

    @Test
    fun importWalletWhenWalletIsUnlockAndWithoutPassphraseAndResultFalseThenFalse() {
        val importWalletRequest = ImportWalletRequest.newBuilder()
            .setSourceLocation(ByteString.copyFromUtf8("test/source"))
            .build()
        every { addressManager.isLocked } returns false
        val result = Pair(
            false, "test_string"
        )
        every { addressManager.importWallet(any()) } returns result
        val reply = adminApiService.importWallet(importWalletRequest)
        verify(exactly = 1) { addressManager.isLocked }
        verify(exactly = 1) { addressManager.importWallet(any()) }
        Assert.assertEquals(false, reply.success)
    }

    @Test
    fun importWalletWhenWalletIsUnlockAndWithoutPassphraseAndResultTrueThenSuccess() {
        val importWalletRequest = ImportWalletRequest.newBuilder()
            .setSourceLocation(ByteString.copyFromUtf8("test/source"))
            .build()
        every { addressManager.isLocked } returns false
        val result = Pair(true, "test_string")
        every { addressManager.importWallet(any()) } returns result
        val reply = adminApiService.importWallet(importWalletRequest)
        verify(exactly = 1) { addressManager.isLocked }
        verify(exactly = 1) { addressManager.importWallet(any()) }
        Assert.assertEquals(true, reply.success)
    }

    @Test
    fun importWalletWhenThrowExceptionThenFalse() {
        val importWalletRequest = ImportWalletRequest.newBuilder()
            .setSourceLocation(ByteString.copyFromUtf8("test/source"))
            .build()
        every { addressManager.isLocked  } returns false
        every { addressManager.importWallet(any()) } throws RuntimeException()
        val reply = adminApiService.importWallet(importWalletRequest)
        verify(exactly = 1) { addressManager.isLocked }
        verify(exactly = 1) { addressManager.importWallet(any()) }
        Assert.assertEquals(false, reply.success)
    }

    @Test
    fun encryptWalletWhenNoPassphraseThenFalse() {
        val request = EncryptWalletRequest.newBuilder()
            .setPassphraseBytes(ByteString.copyFrom(ByteArray(0)))
            .build()
        val reply = adminApiService.encryptWallet(request)
        Assert.assertEquals(false, reply.success)
    }

    @Test
    fun encryptWalletWhenEncryptFalseThenFalse() {
        val request = EncryptWalletRequest.newBuilder()
            .setPassphraseBytes(ByteString.copyFrom("passphrase".toByteArray()))
            .build()
        every { addressManager.encryptWallet(any()) } returns false
        val reply = adminApiService.encryptWallet(request)
        verify(exactly = 1) { addressManager.encryptWallet(any()) }
        Assert.assertEquals(false, reply.success)
    }

    @Test
    fun encryptWalletWhenExceptionThenFalse() {
        val request = EncryptWalletRequest.newBuilder()
            .setPassphraseBytes(ByteString.copyFrom("passphrase".toByteArray()))
            .build()
        every { addressManager.encryptWallet(any()) } throws IllegalStateException()
        val reply = adminApiService.encryptWallet(request)
        verify(exactly = 1) { addressManager.encryptWallet(any()) }
        Assert.assertEquals(false, reply.success)
    }

    @Test
    fun encryptWalletWhenEncryptTrueThenTrue() {
        val request = EncryptWalletRequest.newBuilder()
            .setPassphraseBytes(ByteString.copyFrom("passphrase".toByteArray()))
            .build()
        every { addressManager.encryptWallet(any()) } returns true
        val reply = adminApiService.encryptWallet(request)
        verify(exactly = 1) { addressManager.encryptWallet(any()) }
        Assert.assertEquals(true, reply.success)
    }

    @Test
    fun decryptWalletWhenNoPassphraseThenFalse() {
        val request = DecryptWalletRequest.newBuilder()
            .setPassphraseBytes(ByteString.copyFrom(ByteArray(0)))
            .build()
        val reply = adminApiService.decryptWallet(request)
        Assert.assertEquals(false, reply.success)
    }

    @Test
    fun decryptWalletWhenEncryptFalseThenFalse() {
        val request = DecryptWalletRequest.newBuilder()
            .setPassphraseBytes(ByteString.copyFrom("passphrase".toByteArray()))
            .build()
        every { addressManager.decryptWallet(any()) } returns false
        val reply = adminApiService.decryptWallet(request)
        verify(exactly = 1) { addressManager.decryptWallet(any()) }
        Assert.assertEquals(false, reply.success)
    }

    @Test
    fun decryptWalletWhenExceptionThenFalse() {
        val request = DecryptWalletRequest.newBuilder()
            .setPassphraseBytes(ByteString.copyFrom("passphrase".toByteArray()))
            .build()
        every { addressManager.decryptWallet(any()) } throws IllegalStateException()
        val reply = adminApiService.decryptWallet(request)
        verify(exactly = 1) { addressManager.decryptWallet(any()) }
        Assert.assertEquals(false, reply.success)
    }

    @Test
    fun decryptWalletWhenEncryptTrueThenTrue() {
        val request = DecryptWalletRequest.newBuilder()
            .setPassphraseBytes(ByteString.copyFrom("passphrase".toByteArray()))
            .build()
        every { addressManager.decryptWallet(any()) } returns true
        val reply = adminApiService.decryptWallet(request)
        verify(exactly = 1) { addressManager.decryptWallet(any()) }
        Assert.assertEquals(true, reply.success)
    }

    @Test
    fun lockWallet() {
        every { addressManager.lock() } returns Unit
        val reply = adminApiService.lockWallet(LockWalletRequest.newBuilder().build())
        verify(exactly = 1) { addressManager.lock() }
        Assert.assertEquals(true, reply.success)
    }

    @Test
    fun backupWalletWhenSaveWalletFalseThenFalse() {
        val request = BackupWalletRequest.newBuilder()
            .setTargetLocation(ByteString.copyFromUtf8("target/location/path"))
            .build()
        val saveWalletResult = Pair(
            false, "Result"
        )
        every { addressManager.saveWalletToFile(any()) } returns saveWalletResult
        val reply = adminApiService.backupWallet(request)
        verify { addressManager.saveWalletToFile(any()) }
        Assert.assertEquals(false, reply.success)
    }

    @Test
    fun backupWalletWhenExceptionThenFalse() {
        val request = BackupWalletRequest.newBuilder()
            .setTargetLocation(ByteString.copyFromUtf8("target/location/path"))
            .build()
        every { addressManager.saveWalletToFile(any()) } throws RuntimeException()
        val reply = adminApiService.backupWallet(request)
        verify(exactly = 1) { addressManager.saveWalletToFile(any()) }
        Assert.assertEquals(false, reply.success)
    }

    @Test
    fun backupWalletWhenSaveWalletTrueThenTrue() {
        val request = BackupWalletRequest.newBuilder()
            .setTargetLocation(ByteString.copyFromUtf8("target/location/path"))
            .build()
        val saveWalletResult = Pair(
            true, "Result"
        )
        every { addressManager.saveWalletToFile(any()) } returns saveWalletResult
        val reply = adminApiService.backupWallet(request)
        verify(exactly = 1) { addressManager.saveWalletToFile(any()) }
        Assert.assertEquals(true, reply.success)
    }

    @Test
    fun dumpPrivateKeyWhenAddressNotValidThenFalse() {
        val request = DumpPrivateKeyRequest
            .newBuilder()
            .setAddress(ByteString.copyFromUtf8("Not valid address"))
            .build()
        val reply = adminApiService.dumpPrivateKey(request)
        Assert.assertEquals(false, reply.success)
    }

    @Test
    fun dumpPrivateKeyWhenPublicKeyNullThenFalse() {
        val validAddress = "VAhGtBDm6hq3UVkTXwNgyrFhVEfR8J"
        val kpg = KeyPairGenerator.getInstance("RSA")
        val keyPair = kpg.generateKeyPair()
        val request = DumpPrivateKeyRequest
            .newBuilder()
            .setAddress(ByteString.copyFrom(Base58.decode(validAddress)))
            .build()
        every { addressManager.getPublicKeyForAddress(any()) } returns null
        every { addressManager.getPrivateKeyForAddress(any()) } returns keyPair.private
        val reply = adminApiService.dumpPrivateKey(request)
        verify(exactly = 1) { addressManager.getPublicKeyForAddress(validAddress) }
        verify(exactly = 1) { addressManager.getPrivateKeyForAddress(validAddress) }
        Assert.assertEquals(false, reply.success)
    }

    @Test
    fun dumpPrivateKeyWhenPrivateKeyNullThenFalse() {
        val validAddress = "VAhGtBDm6hq3UVkTXwNgyrFhVEfR8J"
        val kpg = KeyPairGenerator.getInstance("RSA")
        val keyPair = kpg.generateKeyPair()
        val request = DumpPrivateKeyRequest
            .newBuilder()
            .setAddress(ByteString.copyFrom(Base58.decode(validAddress)))
            .build()
        every { addressManager.getPublicKeyForAddress(any()) } returns keyPair.public
        every { addressManager.getPrivateKeyForAddress(any()) } returns null
        val reply = adminApiService.dumpPrivateKey(request)
        verify(exactly = 1) { addressManager.getPublicKeyForAddress(validAddress) }
        verify(exactly = 1) { addressManager.getPrivateKeyForAddress(validAddress) }
        Assert.assertEquals(false, reply.success)
    }

    @Test
    fun dumpPrivateKeyWhenSuccess() {
        val validAddress = "VAhGtBDm6hq3UVkTXwNgyrFhVEfR8J"
        val kpg = KeyPairGenerator.getInstance("RSA")
        val keyPair = kpg.generateKeyPair()
        val request = DumpPrivateKeyRequest
            .newBuilder()
            .setAddress(ByteString.copyFrom(Base58.decode(validAddress)))
            .build()
        every { addressManager.getPublicKeyForAddress(any()) } returns keyPair.public
        every { addressManager.getPrivateKeyForAddress(any()) } returns keyPair.private
        val reply = adminApiService.dumpPrivateKey(request)
        verify(exactly = 1) { addressManager.getPublicKeyForAddress(validAddress) }
        verify(exactly = 1) { addressManager.getPrivateKeyForAddress(validAddress) }
        Assert.assertEquals(true, reply.success)
    }

    @Test
    fun importPrivateKeyWhenWalletLockedThenFalse() {
        val request = ImportPrivateKeyRequest.newBuilder().build()
        every { addressManager.isLocked } returns true
        val reply = adminApiService.importPrivateKey(request)
        verify(exactly = 1) { addressManager.isLocked }
        Assert.assertEquals(false, reply.success)
    }

    @Test
    fun importPrivateKeyWhenAddressNullThenFalse() {
        val kpg = KeyPairGenerator.getInstance("RSA")
        val keyPair = kpg.generateKeyPair()
        val request = ImportPrivateKeyRequest.newBuilder().setPrivateKey(ByteString.copyFrom(keyPair.private.encoded)).build()
        every { addressManager.isLocked } returns false
        every { addressManager.importKeyPair(any(), any()) } returns null
        val reply = adminApiService.importPrivateKey(request)
        verify(exactly = 1) { addressManager.isLocked }
        verify(exactly = 1) { addressManager.importKeyPair(any(), any()) }
        Assert.assertEquals(false, reply.success)
    }

    @Test
    fun importPrivateKeyWhen() {
        val kpg = KeyPairGenerator.getInstance("RSA")
        val keyPair = kpg.generateKeyPair()
        val request = ImportPrivateKeyRequest.newBuilder().setPrivateKey(ByteString.copyFrom(keyPair.private.encoded)).build()
        val importedAddress = Address(
            "VcspPDtJNpNmLV8qFTqb2F5157JNHS", keyPair.public
        )
        every { addressManager.isLocked } returns false
        every { addressManager.importKeyPair(any(), any()) } returns importedAddress
        val reply = adminApiService.importPrivateKey(request)
        verify(exactly = 1) { addressManager.isLocked }
        verify(exactly = 1) { addressManager.importKeyPair(any(), any()) }
        Assert.assertEquals(true, reply.success)
    }

    @Test
    fun newAddressWhenWalletLockedThenFalse() {
        val request = GetNewAddressRequest.newBuilder().build()
        every { addressManager.isLocked } returns true
        val reply = adminApiService.getNewAddress(request)
        verify(exactly = 1) { addressManager.isLocked }
        Assert.assertEquals(false, reply.success)
    }

    @Test
    fun newAddressWhenGetNewAddressNullThenFalse() {
        val request = GetNewAddressRequest.newBuilder().build()
        every { addressManager.isLocked } returns false
        every { addressManager.newAddress } returns null
        val reply = adminApiService.getNewAddress(request)
        verify(exactly = 1) { addressManager.isLocked }
        verify(exactly = 1) { addressManager.newAddress }
        Assert.assertEquals(false, reply.success)
    }

    @Test
    fun newAddressWhenIOExceptionThenFalse() {
        val request = GetNewAddressRequest.newBuilder().build()
        every { addressManager.isLocked } returns false
        every { addressManager.newAddress } throws IOException()
        val reply = adminApiService.getNewAddress(request)
        verify(exactly = 1) { addressManager.isLocked }
        verify(exactly = 1) { addressManager.newAddress }
        Assert.assertEquals(false, reply.success)
    }

    @Test
    fun newAddressWhenWalletLockedExceptionThenFalse() {
        val request = GetNewAddressRequest.newBuilder().build()
        every { addressManager.isLocked } returns false
        every { addressManager.newAddress } throws WalletLockedException("Exception")
        val reply = adminApiService.getNewAddress(request)
        verify(exactly = 1) { addressManager.isLocked }
        verify(exactly = 1) { addressManager.newAddress }
        Assert.assertEquals(false, reply.success)
    }

    @Test
    fun newAddressWhenSuccessThenTrue() {
        val kpg = KeyPairGenerator.getInstance("RSA")
        val keyPair = kpg.generateKeyPair()
        val request = GetNewAddressRequest.newBuilder().build()
        val address = Address("VcspPDtJNpNmLV8qFTqb2F5157JNHS", keyPair.public)
        every { addressManager.isLocked } returns false
        every { addressManager.newAddress } returns address
        val reply = adminApiService.getNewAddress(request)
        verify(exactly = 1) { addressManager.isLocked }
        verify(exactly = 1) { addressManager.newAddress }
        Assert.assertEquals(true, reply.success)
    }

    @Test
    fun submitTransactionsWhenTxUnsignedThenFalse() {
        val transactionUnion = TransactionUnion.newBuilder()
            .setUnsigned(VeriBlockMessages.Transaction.newBuilder().build())
            .build()
        val request = SubmitTransactionsRequest.newBuilder()
            .addTransactions(transactionUnion)
            .build()
        val reply = adminApiService.submitTransactions(request)
        Assert.assertEquals(false, reply.success)
    }

    @Test
    fun submitTransactionsWhenTxSignedButNotAddToContainerThenFalse() {
        val signTx = SignedTransaction.newBuilder().build()
        val transactionUnion = TransactionUnion.newBuilder().setSigned(signTx).build()
        val request = SubmitTransactionsRequest.newBuilder().addTransactions(transactionUnion).build()
        every { transactionFactory.create(transactionUnion) } returns StandardTransaction(Sha256Hash.ZERO_HASH)
        every { transactionContainer.addTransaction(any()) } returns false
        val reply = adminApiService.submitTransactions(request)
        verify(exactly = 1) { transactionFactory.create(signTx) }
        verify(exactly = 1) { transactionContainer.addTransaction(any()) }
        Assert.assertEquals(false, reply.success)
    }

    @Test
    fun submitTransactionsWhenTxSignedThenTrue() {
        val signTx = SignedTransaction.newBuilder().build()
        val transactionUnion = TransactionUnion.newBuilder().setSigned(signTx).build()
        val request = SubmitTransactionsRequest.newBuilder().addTransactions(transactionUnion).build()
        every { transactionFactory.create(transactionUnion) } returns StandardTransaction(Sha256Hash.ZERO_HASH)
        every { transactionContainer.addTransaction(any()) } returns true
        val reply = adminApiService.submitTransactions(request)
        verify(exactly = 1) { transactionFactory.create(signTx) }
        verify(exactly = 1) { transactionContainer.addTransaction(any()) }
        Assert.assertEquals(true, reply.success)
    }

    @Test
    fun submitTransactionsWhenTxMultiSigThenTrue() {
        val signTx = SignedMultisigTransaction.newBuilder().build()
        val transactionUnion = TransactionUnion.newBuilder()
            .setSignedMultisig(signTx)
            .build()
        val request = SubmitTransactionsRequest.newBuilder()
            .addTransactions(transactionUnion)
            .build()

        every { transactionFactory.create(transactionUnion) } returns StandardTransaction(Sha256Hash.ZERO_HASH)
        every { transactionContainer.addTransaction(any()) } returns true

        val reply = adminApiService.submitTransactions(request)
        verify(exactly = 1) { transactionFactory.create(signTx) }
        verify(exactly = 1) { transactionContainer.addTransaction(any()) }

        Assert.assertEquals(true, reply.success)
    }

    @Test
    fun signatureIndexWhenSuccess() {
        val request = GetSignatureIndexRequest.newBuilder().build()
        val kpg = KeyPairGenerator.getInstance("RSA")
        val keyPair = kpg.generateKeyPair()
        val address = Address("VcspPDtJNpNmLV8qFTqb2F5157JNHS", keyPair.public)
        every { addressManager.defaultAddress } returns address
        every { peerTable.getSignatureIndex(any()) } returns 1L
        every { transactionContainer.getPendingSignatureIndexForAddress(any()) } returns 2L
        val reply = adminApiService.getSignatureIndex(request)
        verify(exactly = 1) { addressManager.defaultAddress }
        verify(exactly = 1) { peerTable.getSignatureIndex(any()) }
        verify(exactly = 1) { transactionContainer.getPendingSignatureIndexForAddress(any()) }
        Assert.assertEquals(true, reply.success)
    }

    @Test
    fun signatureIndexWhenReqHasAddressSuccess() {
        val kpg = KeyPairGenerator.getInstance("RSA")
        val keyPair = kpg.generateKeyPair()
        val address = Address("VcspPDtJNpNmLV8qFTqb2F5157JNHS", keyPair.public)
        val request = GetSignatureIndexRequest.newBuilder()
            .addAddresses(ByteString.copyFrom(Base58.decode(address.hash)))
            .build()
        every { peerTable.getSignatureIndex(any()) } returns 1L
        every { transactionContainer.getPendingSignatureIndexForAddress(any()) } returns 2L
        val reply = adminApiService.getSignatureIndex(request)
        verify(exactly = 1) { peerTable.getSignatureIndex(any()) }
        verify(exactly = 1) { transactionContainer.getPendingSignatureIndexForAddress(any()) }
        Assert.assertEquals(true, reply.success)
    }

    @Test
    fun createAltChainEndorsementWhenMaxFeeLess() {
        val transaction = StandardTransaction(Sha256Hash.ZERO_HASH).also {
            it.inputAddress = StandardAddress("VcspPDtJNpNmLV8qFTqb2F5157JNHS")
            it.inputAmount = Coin.ONE
            it.data = ByteArray(12)
        }

        val request = CreateAltChainEndorsementRequest.newBuilder().setPublicationData(
            ByteString.copyFrom(ByteArray(12))
        ).setSourceAddress(ByteStringAddressUtility.createProperByteStringAutomatically("VcspPDtJNpNmLV8qFTqb2F5157JNHS"))
            .setFeePerByte(10000L).setMaxFee(1000L).build()

        every { transactionService.createUnsignedAltChainEndorsementTransaction(any(), any(), any(), any()) } returns transaction

        val reply = adminApiService.createAltChainEndorsement(request)
        Assert.assertNotNull(reply)
        Assert.assertFalse(reply.success)
        Assert.assertFalse(reply.getResults(0).message.contains("Calcualated fee"))
    }

    @Test
    fun createAltChainEndorsementWhenThrowException() {
        val transaction = StandardTransaction(Sha256Hash.ZERO_HASH).also {
            it.inputAddress = StandardAddress("VcspPDtJNpNmLV8qFTqb2F5157JNHS")
            it.inputAmount = Coin.ONE
            it.data = ByteArray(12)
        }

        val request = CreateAltChainEndorsementRequest.newBuilder().setPublicationData(
            ByteString.copyFrom(ByteArray(12))
        ).setSourceAddress(ByteStringAddressUtility.createProperByteStringAutomatically("VcspPDtJNpNmLV8qFTqb2F5157JNHS"))
            .setFeePerByte(10000L).setMaxFee(100000000L).build()

        every { transactionService.createUnsignedAltChainEndorsementTransaction(any(), any(), any(), any()) } throws RuntimeException()

        val reply = adminApiService.createAltChainEndorsement(request)
        Assert.assertNotNull(reply)
        Assert.assertFalse(reply.success)
        Assert.assertFalse(reply.getResults(0).message.contains("An error occurred processing"))
    }

    @Test
    fun createAltChainEndorsement() {
        val transaction = StandardTransaction(Sha256Hash.ZERO_HASH).also {
            it.inputAddress = StandardAddress("VcspPDtJNpNmLV8qFTqb2F5157JNHS")
            it.inputAmount = Coin.ONE
            it.data = ByteArray(12)
        }
        val request = CreateAltChainEndorsementRequest.newBuilder().setPublicationData(
            ByteString.copyFrom(ByteArray(12))
        ).setSourceAddress(ByteStringAddressUtility.createProperByteStringAutomatically("VcspPDtJNpNmLV8qFTqb2F5157JNHS"))
            .setFeePerByte(10000L).setMaxFee(100000000L).build()

        every { transactionService.createUnsignedAltChainEndorsementTransaction(any(), any(), any(), any()) } returns transaction

        val reply = adminApiService.createAltChainEndorsement(request)
        Assert.assertNotNull(reply)
        Assert.assertTrue(reply.success)
    }
}
