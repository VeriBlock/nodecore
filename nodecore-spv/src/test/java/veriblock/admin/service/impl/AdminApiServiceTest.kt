package veriblock.admin.service.impl

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.veriblock.core.AddressCreationException
import org.veriblock.core.Context
import org.veriblock.core.EndorsementCreationException
import org.veriblock.core.ImportException
import org.veriblock.core.SendCoinsException
import org.veriblock.core.TransactionSubmissionException
import org.veriblock.core.WalletException
import org.veriblock.core.WalletLockedException
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.params.defaultMainNetParameters
import org.veriblock.core.params.defaultTestNetParameters
import org.veriblock.core.types.Pair
import org.veriblock.core.wallet.AddressManager
import org.veriblock.core.wallet.AddressPubKey
import org.veriblock.sdk.models.Coin
import org.veriblock.sdk.models.asCoin
import veriblock.SpvConfig
import veriblock.SpvContext
import veriblock.model.LedgerContext
import veriblock.model.LedgerProofStatus
import veriblock.model.LedgerValue
import veriblock.model.Output
import veriblock.model.StandardAddress
import veriblock.model.StandardTransaction
import veriblock.model.Transaction
import veriblock.model.asLightAddress
import veriblock.net.SpvPeerTable
import veriblock.service.SpvService
import veriblock.service.Blockchain
import veriblock.service.PendingTransactionContainer
import veriblock.service.TransactionService
import java.io.IOException
import java.security.KeyPairGenerator

class AdminApiServiceTest {
    private val spvContext = SpvContext()
    private lateinit var transactionService: TransactionService
    private lateinit var addressManager: AddressManager
    private lateinit var peerTable: SpvPeerTable
    private lateinit var spvService: SpvService
    private lateinit var transactionContainer: PendingTransactionContainer
    private lateinit var blockchain: Blockchain

    @Before
    fun setUp() {
        Context.set(defaultTestNetParameters)
        spvContext.init(SpvConfig("testnet", useLocalNode = true))
        peerTable = mockk(relaxed = true)
        transactionService = mockk(relaxed = true)
        addressManager = mockk(relaxed = true)
        transactionContainer = mockk(relaxed = true)
        blockchain = mockk(relaxed = true)
        spvService = SpvService(
            spvContext, peerTable, transactionService, addressManager, transactionContainer, blockchain
        )
    }

    @Test
    fun sendCoins() {
        val transaction: Transaction = StandardTransaction(Sha256Hash.ZERO_HASH)
        val transactions = listOf(transaction)
        val ledgerContext = LedgerContext(
            ledgerValue = LedgerValue(100L, 0L, 0L),
            ledgerProofStatus = LedgerProofStatus.ADDRESS_EXISTS
        )

        every { peerTable.getAddressState(any()) } returns ledgerContext
        every { transactionService.predictStandardTransactionToAllStandardOutputSize(any(), any(), any(), any()) } returns 500
        every { transactionService.createTransactionsByOutputList(any(), any()) } returns transactions
        every { transactionContainer.getPendingSignatureIndexForAddress(any())}
        every { transactionContainer.getPendingSignatureIndexForAddress(any()) } returns 1

        val reply = spvService.sendCoins(
            "VcspPDtJNpNmLV8qFTqb2F5157JNHS".asLightAddress(),
            listOf(
                Output(
                    "VDBt3GuwPe1tA5m4duTPkBq5vF22rw".asLightAddress(),
                    100.asCoin()
                )
            )
        )

        verify(exactly = 1) { transactionService.createTransactionsByOutputList(any(), any()) }
        verify(exactly = 1) { transactionContainer.getPendingSignatureIndexForAddress(any()) }
        verify(exactly = 1) { peerTable.advertise(any()) }

        Assert.assertNotNull(reply.firstOrNull())
        Assert.assertTrue(reply.first() == transaction.txId)
    }

    @Test(expected = SendCoinsException::class)
    fun sendCoinsWhenAddressDoesntExist() {
        val transaction: Transaction = StandardTransaction(Sha256Hash.ZERO_HASH)
        val ledgerContext = LedgerContext(
            ledgerValue = LedgerValue(100L, 0L, 0L),
            ledgerProofStatus = LedgerProofStatus.ADDRESS_DOES_NOT_EXIST
        )
        every { transactionService.predictStandardTransactionToAllStandardOutputSize(any(), any(), any(), any()) } returns 500
        every { transactionService.createStandardTransaction(any(), any(), any(), any()) } returns transaction
        every { transactionContainer.getPendingSignatureIndexForAddress(any()) } returns 1L
        every { peerTable.getAddressState(any()) } returns ledgerContext
        spvService.sendCoins(
            "VcspPDtJNpNmLV8qFTqb2F5157JNHS".asLightAddress(),
            listOf(
                Output(
                    "VDBt3GuwPe1tA5m4duTPkBq5vF22rw".asLightAddress(),
                    100.asCoin()
                )
            )
        )
        //Assert.assertTrue(reply.getResults(0).message.contains("Address doesn't exist or invalid"))
    }

    @Test(expected = SendCoinsException::class)
    fun sendCoinsWhenAddressDoesntIsInvalid() {
        val transaction: Transaction = StandardTransaction(Sha256Hash.ZERO_HASH)
        val ledgerContext = LedgerContext(
            ledgerValue = LedgerValue(100L, 0L, 0L),
            ledgerProofStatus = LedgerProofStatus.ADDRESS_IS_INVALID
        )
        every { transactionService.predictStandardTransactionToAllStandardOutputSize(any(), any(), any(), any()) } returns 500
        every { transactionService.createStandardTransaction(any(), any(), any(), any()) } returns transaction
        every { transactionContainer.getPendingSignatureIndexForAddress(any()) } returns 1L
        every { peerTable.getAddressState(any()) } returns ledgerContext
        spvService.sendCoins(
            "VcspPDtJNpNmLV8qFTqb2F5157JNHS".asLightAddress(),
            listOf(
                Output(
                    "VDBt3GuwPe1tA5m4duTPkBq5vF22rw".asLightAddress(),
                    100.asCoin()
                )
            )
        )
        //Assert.assertTrue(reply.getResults(0).message.contains("Address doesn't exist or invalid"))
    }

    @Test(expected = SendCoinsException::class)
    fun sendCoinsWhenBalanceIsNotEnough() {
        val transaction: Transaction = StandardTransaction(Sha256Hash.ZERO_HASH)
        val ledgerContext = LedgerContext(
            ledgerValue = LedgerValue(50L, 0L, 0L),
            ledgerProofStatus = LedgerProofStatus.ADDRESS_EXISTS
        )
        every { transactionService.predictStandardTransactionToAllStandardOutputSize(any(), any(), any(), any()) } returns 500
        every { transactionService.createStandardTransaction(any(), any(), any(), any()) } returns transaction
        every { transactionContainer.getPendingSignatureIndexForAddress(any()) } returns 1L
        every { peerTable.getAddressState(any()) } returns ledgerContext
        spvService.sendCoins(
            "VcspPDtJNpNmLV8qFTqb2F5157JNHS".asLightAddress(),
            listOf(
                Output(
                    "VDBt3GuwPe1tA5m4duTPkBq5vF22rw".asLightAddress(),
                    100.asCoin()
                )
            )
        )
        //Assert.assertTrue(reply.getResults(0).message.contains("Available balance is not enough"))
    }

    @Test(expected = SendCoinsException::class)
    fun sendCoinsWhenAddressInfoDoentExist() {
        val transaction: Transaction = StandardTransaction(Sha256Hash.ZERO_HASH)
        every { transactionService.predictStandardTransactionToAllStandardOutputSize(any(), any(), any(), any()) } returns 500
        every { transactionService.createStandardTransaction(any(), any(), any(), any()) } returns transaction
        every { transactionContainer.getPendingSignatureIndexForAddress(any()) } returns 1L
        every { peerTable.getAddressState(any()) } returns null
        spvService.sendCoins(
            "VcspPDtJNpNmLV8qFTqb2F5157JNHS".asLightAddress(),
            listOf(
                Output(
                    "VDBt3GuwPe1tA5m4duTPkBq5vF22rw".asLightAddress(),
                    100.asCoin()
                )
            )
        )
        //Assert.assertTrue(reply.getResults(0).message.contains("Information about this address does not exist"))
    }

    @Test
    fun unlockWalletWhenWalletIsLockThenTrue() {
        every { addressManager.unlock(any()) } returns true
        spvService.unlockWallet("123")
        verify(exactly = 1) { addressManager.unlock(any()) }
    }

    @Test(expected = WalletException::class)
    fun unlockWalletWhenWalletIsUnlockThenFalse() {
        every { addressManager.unlock(any()) } returns false
        spvService.unlockWallet("123")
        verify(exactly = 1) { addressManager.unlock(any()) }
    }

    @Test(expected = WalletException::class)
    fun importWalletWhenWalletIsLockThenFalse() {
        every { addressManager.isLocked } returns true
        spvService.importWallet("test/source", "123")
        verify(exactly = 1) { addressManager.isLocked }
    }

    @Test(expected = WalletException::class)
    fun importWalletWhenWalletIsUnlockAndWithoutPassphraseAndResultFalseThenFalse() {
        every { addressManager.isLocked } returns false
        val result = Pair(
            false, "test_string"
        )
        every { addressManager.importWallet(any()) } returns result
        spvService.importWallet("test/source")
        verify(exactly = 1) { addressManager.isLocked }
        verify(exactly = 1) { addressManager.importWallet(any()) }
    }

    @Test(expected = Test.None::class)
    fun importWalletWhenWalletIsUnlockAndWithoutPassphraseAndResultTrueThenSuccess() {
        every { addressManager.isLocked } returns false
        val result = Pair(true, "test_string")
        every { addressManager.importWallet(any()) } returns result
        spvService.importWallet("test/source")
        verify(exactly = 1) { addressManager.isLocked }
        verify(exactly = 1) { addressManager.importWallet(any()) }
    }

    @Test(expected = WalletException::class)
    fun importWalletWhenThrowExceptionThenFalse() {
        every { addressManager.isLocked  } returns false
        every { addressManager.importWallet(any()) } throws RuntimeException()
        spvService.importWallet("test/source")
        verify(exactly = 1) { addressManager.isLocked }
        verify(exactly = 1) { addressManager.importWallet(any()) }
    }

    @Test(expected = WalletException::class)
    fun encryptWalletWhenNoPassphraseThenFalse() {
        spvService.encryptWallet("")
    }

    @Test(expected = WalletException::class)
    fun encryptWalletWhenEncryptFalseThenFalse() {
        every { addressManager.encryptWallet(any()) } returns false
        spvService.encryptWallet("passphrase")
        verify(exactly = 1) { addressManager.encryptWallet(any()) }
    }

    @Test(expected = WalletException::class)
    fun encryptWalletWhenExceptionThenFalse() {
        every { addressManager.encryptWallet(any()) } throws IllegalStateException()
        spvService.encryptWallet("passphrase")
        verify(exactly = 1) { addressManager.encryptWallet(any()) }
    }

    @Test(expected = Test.None::class)
    fun encryptWalletWhenEncryptTrueThenTrue() {
        every { addressManager.encryptWallet(any()) } returns true
        spvService.encryptWallet("passphrase")
        verify(exactly = 1) { addressManager.encryptWallet(any()) }
    }

    @Test(expected = WalletException::class)
    fun decryptWalletWhenNoPassphraseThenFalse() {
        spvService.decryptWallet("")
    }

    @Test(expected = WalletException::class)
    fun decryptWalletWhenEncryptFalseThenFalse() {
        every { addressManager.decryptWallet(any()) } returns false
        spvService.decryptWallet("passphrase")
        verify(exactly = 1) { addressManager.decryptWallet(any()) }
    }

    @Test(expected = WalletException::class)
    fun decryptWalletWhenExceptionThenFalse() {
        every { addressManager.decryptWallet(any()) } throws IllegalStateException()
        spvService.decryptWallet("passphrase")
        verify(exactly = 1) { addressManager.decryptWallet(any()) }
    }

    @Test(expected = Test.None::class)
    fun decryptWalletWhenEncryptTrueThenTrue() {
        every { addressManager.decryptWallet(any()) } returns true
        spvService.decryptWallet("passphrase")
        verify(exactly = 1) { addressManager.decryptWallet(any()) }
    }

    @Test
    fun lockWallet() {
        spvService.lockWallet()
        verify(exactly = 1) { addressManager.lock() }
    }

    @Test(expected = WalletException::class)
    fun backupWalletWhenSaveWalletFalseThenFalse() {
        val saveWalletResult = Pair(
            false, "Result"
        )
        every { addressManager.saveWalletToFile(any()) } returns saveWalletResult
        spvService.backupWallet("target/location/path")
        verify { addressManager.saveWalletToFile(any()) }
    }

    @Test(expected = WalletException::class)
    fun backupWalletWhenExceptionThenFalse() {
        every { addressManager.saveWalletToFile(any()) } throws RuntimeException()
        spvService.backupWallet("target/location/path")
        verify(exactly = 1) { addressManager.saveWalletToFile(any()) }
    }

    @Test(expected = Test.None::class) // That would be like shouldNotThrow WalletException on that case
    fun backupWalletWhenSaveWalletTrueThenTrue() {
        val saveWalletResult = Pair(
            true, "Result"
        )
        every { addressManager.saveWalletToFile(any()) } returns saveWalletResult
        spvService.backupWallet("target/location/path")
        verify(exactly = 1) { addressManager.saveWalletToFile(any()) }
    }

    @Test(expected = WalletException::class)
    fun dumpPrivateKeyWhenPublicKeyNullThenFalse() {
        val validAddress = "VAhGtBDm6hq3UVkTXwNgyrFhVEfR8J"
        val kpg = KeyPairGenerator.getInstance("RSA")
        val keyPair = kpg.generateKeyPair()
        every { addressManager.getPublicKeyForAddress(any()) } returns null
        every { addressManager.getPrivateKeyForAddress(any()) } returns keyPair.private
        spvService.dumpPrivateKey(validAddress.asLightAddress())
        verify(exactly = 1) { addressManager.getPublicKeyForAddress(validAddress) }
        verify(exactly = 1) { addressManager.getPrivateKeyForAddress(validAddress) }
    }

    @Test(expected = WalletException::class)
    fun dumpPrivateKeyWhenPrivateKeyNullThenFalse() {
        val validAddress = "VAhGtBDm6hq3UVkTXwNgyrFhVEfR8J"
        val kpg = KeyPairGenerator.getInstance("RSA")
        val keyPair = kpg.generateKeyPair()
        every { addressManager.getPublicKeyForAddress(any()) } returns keyPair.public
        every { addressManager.getPrivateKeyForAddress(any()) } returns null
        spvService.dumpPrivateKey(validAddress.asLightAddress())
        verify(exactly = 1) { addressManager.getPublicKeyForAddress(validAddress) }
        verify(exactly = 1) { addressManager.getPrivateKeyForAddress(validAddress) }
    }

    @Test
    fun dumpPrivateKeyWhenSuccess() {
        val validAddress = "VAhGtBDm6hq3UVkTXwNgyrFhVEfR8J"
        val kpg = KeyPairGenerator.getInstance("RSA")
        val keyPair = kpg.generateKeyPair()
        every { addressManager.getPublicKeyForAddress(any()) } returns keyPair.public
        every { addressManager.getPrivateKeyForAddress(any()) } returns keyPair.private
        spvService.dumpPrivateKey(validAddress.asLightAddress())
        verify(exactly = 1) { addressManager.getPublicKeyForAddress(validAddress) }
        verify(exactly = 1) { addressManager.getPrivateKeyForAddress(validAddress) }
    }

    @Test(expected = ImportException::class)
    fun importPrivateKeyWhenWalletLockedThenFalse() {
        every { addressManager.isLocked } returns true
        spvService.importPrivateKey(byteArrayOf())
        verify(exactly = 1) { addressManager.isLocked }
    }

    @Test(expected = ImportException::class)
    fun importPrivateKeyWhenAddressNullThenFalse() {
        val kpg = KeyPairGenerator.getInstance("RSA")
        val keyPair = kpg.generateKeyPair()
        every { addressManager.isLocked } returns false
        every { addressManager.importKeyPair(any(), any()) } returns null
        spvService.importPrivateKey(keyPair.private.encoded)
        verify(exactly = 1) { addressManager.isLocked }
        verify(exactly = 1) { addressManager.importKeyPair(any(), any()) }
    }

    @Test
    fun importPrivateKeyWhen() {
        val kpg = KeyPairGenerator.getInstance("RSA")
        val keyPair = kpg.generateKeyPair()
        val importedAddress = AddressPubKey(
            "VcspPDtJNpNmLV8qFTqb2F5157JNHS", keyPair.public
        )
        every { addressManager.isLocked } returns false
        every { addressManager.importKeyPair(any(), any()) } returns importedAddress
        spvService.importPrivateKey(keyPair.private.encoded)
        verify(exactly = 1) { addressManager.isLocked }
        verify(exactly = 1) { addressManager.importKeyPair(any(), any()) }
    }

    @Test(expected = AddressCreationException::class)
    fun newAddressWhenWalletLockedThenFalse() {
        every { addressManager.isLocked } returns true
        spvService.getNewAddress()
        verify(exactly = 1) { addressManager.isLocked }
    }

    @Test(expected = AddressCreationException::class)
    fun newAddressWhenGetNewAddressNullThenFalse() {
        every { addressManager.isLocked } returns false
        every { addressManager.newAddress } returns null
        spvService.getNewAddress()
        verify(exactly = 1) { addressManager.isLocked }
        verify(exactly = 1) { addressManager.newAddress }
    }

    @Test(expected = AddressCreationException::class)
    fun newAddressWhenIOExceptionThenFalse() {
        every { addressManager.isLocked } returns false
        every { addressManager.newAddress } throws IOException()
        spvService.getNewAddress()
        verify(exactly = 1) { addressManager.isLocked }
        verify(exactly = 1) { addressManager.newAddress }
    }

    @Test(expected = AddressCreationException::class)
    fun newAddressWhenWalletLockedExceptionThenFalse() {
        every { addressManager.isLocked } returns false
        every { addressManager.newAddress } throws WalletLockedException("Exception")
        spvService.getNewAddress()
        verify(exactly = 1) { addressManager.isLocked }
        verify(exactly = 1) { addressManager.newAddress }
    }

    @Test
    fun newAddressWhenSuccessThenTrue() {
        val kpg = KeyPairGenerator.getInstance("RSA")
        val keyPair = kpg.generateKeyPair()
        val address = AddressPubKey("VcspPDtJNpNmLV8qFTqb2F5157JNHS", keyPair.public)
        every { addressManager.isLocked } returns false
        every { addressManager.newAddress } returns address
        spvService.getNewAddress()
        verify(exactly = 1) { addressManager.isLocked }
        verify(exactly = 1) { addressManager.newAddress }
    }

    @Test(expected = TransactionSubmissionException::class)
    fun submitTransactionsWhenTxSignedButNotAddToContainerThenFalse() {
        val transaction = StandardTransaction(Sha256Hash.ZERO_HASH)
        every { transactionContainer.addTransaction(any()) } returns false
        spvService.submitTransactions(listOf(transaction))
        verify(exactly = 1) { transactionContainer.addTransaction(transaction) }
    }

    @Test
    fun submitTransactionsWhenTxSignedThenTrue() {
        val transaction = StandardTransaction(Sha256Hash.ZERO_HASH)
        every { transactionContainer.addTransaction(any()) } returns true
        spvService.submitTransactions(listOf(transaction))
        verify(exactly = 1) { transactionContainer.addTransaction(transaction) }
    }

    @Test
    fun signatureIndexWhenSuccess() {
        val kpg = KeyPairGenerator.getInstance("RSA")
        val keyPair = kpg.generateKeyPair()
        val address = AddressPubKey("VcspPDtJNpNmLV8qFTqb2F5157JNHS", keyPair.public)
        every { addressManager.defaultAddress } returns address
        every { peerTable.getSignatureIndex(any()) } returns 1L
        every { transactionContainer.getPendingSignatureIndexForAddress(any()) } returns 2L
        spvService.getSignatureIndex(emptyList())
        verify(exactly = 1) { addressManager.defaultAddress }
        verify(exactly = 1) { peerTable.getSignatureIndex(any()) }
        verify(exactly = 1) { transactionContainer.getPendingSignatureIndexForAddress(any()) }
    }

    @Test
    fun signatureIndexWhenReqHasAddressSuccess() {
        val kpg = KeyPairGenerator.getInstance("RSA")
        val keyPair = kpg.generateKeyPair()
        val address = AddressPubKey("VcspPDtJNpNmLV8qFTqb2F5157JNHS", keyPair.public)
        every { peerTable.getSignatureIndex(any()) } returns 1L
        every { transactionContainer.getPendingSignatureIndexForAddress(any()) } returns 2L
        spvService.getSignatureIndex(listOf(address.hash.asLightAddress()))
        verify(exactly = 1) { peerTable.getSignatureIndex(any()) }
        verify(exactly = 1) { transactionContainer.getPendingSignatureIndexForAddress(any()) }
    }

    @Test(expected = EndorsementCreationException::class)
    fun createAltChainEndorsementWhenMaxFeeLess() {
        val transaction = StandardTransaction(Sha256Hash.ZERO_HASH).also {
            it.inputAddress = StandardAddress("VcspPDtJNpNmLV8qFTqb2F5157JNHS")
            it.inputAmount = Coin.ONE
            it.data = ByteArray(12)
        }

        every { transactionService.createUnsignedAltChainEndorsementTransaction(any(), any(), any(), any()) } returns transaction

        val reply = spvService.createAltChainEndorsement(ByteArray(12), "VcspPDtJNpNmLV8qFTqb2F5157JNHS", 10000L, 1000L)
        Assert.assertNotNull(reply)
    }

    @Test(expected = EndorsementCreationException::class)
    fun createAltChainEndorsementWhenThrowException() {
        every { transactionService.createUnsignedAltChainEndorsementTransaction(any(), any(), any(), any()) } throws RuntimeException()

        val reply = spvService.createAltChainEndorsement(ByteArray(12), "VcspPDtJNpNmLV8qFTqb2F5157JNHS", 10000L, 100000000L)
        Assert.assertNotNull(reply)
    }

    @Test(expected = Test.None::class)
    fun createAltChainEndorsement() {
        val transaction = StandardTransaction(Sha256Hash.ZERO_HASH).also {
            it.inputAddress = StandardAddress("VcspPDtJNpNmLV8qFTqb2F5157JNHS")
            it.inputAmount = Coin.ONE
            it.data = ByteArray(12)
        }
        every { transactionService.createUnsignedAltChainEndorsementTransaction(any(), any(), any(), any()) } returns transaction

        val reply = spvService.createAltChainEndorsement(ByteArray(12), "VcspPDtJNpNmLV8qFTqb2F5157JNHS", 10000L, 100000000L)
        Assert.assertNotNull(reply)
    }
}
