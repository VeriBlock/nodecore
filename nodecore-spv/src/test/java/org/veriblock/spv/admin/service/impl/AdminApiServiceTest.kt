package org.veriblock.spv.admin.service.impl

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.veriblock.core.AddressCreationException
import org.veriblock.core.Context
import org.veriblock.core.EndorsementCreationException
import org.veriblock.core.ImportException
import org.veriblock.core.SendCoinsException
import org.veriblock.core.WalletException
import org.veriblock.core.WalletLockedException
import org.veriblock.core.crypto.EMPTY_VBK_TX
import org.veriblock.core.params.defaultTestNetParameters
import org.veriblock.core.types.Pair
import org.veriblock.core.wallet.AddressManager
import org.veriblock.core.wallet.AddressPubKey
import org.veriblock.sdk.models.Address
import org.veriblock.sdk.models.Coin
import org.veriblock.sdk.models.asCoin
import org.veriblock.spv.SpvConfig
import org.veriblock.spv.SpvContext
import org.veriblock.spv.model.LedgerContext
import org.veriblock.spv.model.LedgerValue
import org.veriblock.spv.model.Output
import org.veriblock.spv.model.StandardAddress
import org.veriblock.spv.model.StandardTransaction
import org.veriblock.spv.model.Transaction
import org.veriblock.spv.model.asLightAddress
import org.veriblock.spv.net.SpvPeerTable
import org.veriblock.spv.service.SpvService
import org.veriblock.spv.service.Blockchain
import org.veriblock.spv.service.TransactionService
import org.veriblock.spv.service.tx.TransactionManager
import java.io.IOException
import java.security.KeyPairGenerator

class AdminApiServiceTest {
    private lateinit var spvContext: SpvContext
    private lateinit var transactionService: TransactionService
    private lateinit var addressManager: AddressManager
    private lateinit var peerTable: SpvPeerTable
    private lateinit var spvService: SpvService
    private lateinit var transactionContainer: TransactionManager
    private lateinit var blockchain: Blockchain
    val address = "VHoWCZrQB4kqLHm1EoNoU8rih7ohyG"
    val block = defaultTestNetParameters.genesisBlock

    @Before
    fun setUp() {
        Context.set(defaultTestNetParameters)
        spvContext = SpvContext(SpvConfig(defaultTestNetParameters, connectDirectlyTo = listOf("localhost")))
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
        val transaction: Transaction = StandardTransaction(EMPTY_VBK_TX)
        val transactions = listOf(transaction)
        val ledgerContext = LedgerContext(
            ledgerValue = LedgerValue(100L, 0L, 0L),
            block = defaultTestNetParameters.genesisBlock,
            address = Address(address)
        )

        spvContext.setAddressState(ledgerContext)
        every { transactionService.predictStandardTransactionToAllStandardOutputSize(any(), any(), any(), any()) } returns 500
        every { transactionService.createTransactionsByOutputList(any(), any()) } returns transactions
        every { transactionContainer.getPendingSignatureIndexForAddress(any()) }
        every { transactionContainer.getPendingSignatureIndexForAddress(any()) } returns 1

        val reply = runBlocking {
            spvService.sendCoins(
                address.asLightAddress(),
                listOf(
                    Output(
                        "VDBt3GuwPe1tA5m4duTPkBq5vF22rw".asLightAddress(),
                        100.asCoin()
                    )
                )
            )
        }

        verify(exactly = 1) { transactionService.createTransactionsByOutputList(any(), any()) }
        verify(exactly = 1) { transactionContainer.getPendingSignatureIndexForAddress(any()) }
        verify(exactly = 1) { peerTable.advertise(any()) }

        reply.firstOrNull() shouldNotBe null
        reply.first() shouldBe transaction.txId
    }

    @Test(expected = SendCoinsException::class)
    fun sendCoinsWhenAddressDoesntExist() {
        val transaction: Transaction = StandardTransaction(EMPTY_VBK_TX)
        val ledgerContext = LedgerContext(
            ledgerValue = LedgerValue(100L, 0L, 0L),
            address = Address(address),
            block = defaultTestNetParameters.genesisBlock
        )
        every { transactionService.predictStandardTransactionToAllStandardOutputSize(any(), any(), any(), any()) } returns 500
        every { transactionService.createStandardTransaction(any(), any(), any(), any()) } returns transaction
        every { transactionContainer.getPendingSignatureIndexForAddress(any()) } returns 1L
        spvContext.setAddressState(ledgerContext)
        runBlocking {
            spvService.sendCoins(
                "VcspPDtJNpNmLV8qFTqb2F5157JNHS".asLightAddress(),
                listOf(
                    Output(
                        "VDBt3GuwPe1tA5m4duTPkBq5vF22rw".asLightAddress(),
                        100.asCoin()
                    )
                )
            )
        }
        //reply.getResults(0).message.contains("Address doesn't exist or invalid") shouldBe true
    }

    @Test(expected = SendCoinsException::class)
    fun sendCoinsWhenAddressIsInvalid() {
        val transaction: Transaction = StandardTransaction(EMPTY_VBK_TX)
        val ledgerContext = LedgerContext(
            address = Address(address),
            ledgerValue = LedgerValue(100L, 0L, 0L),
            block = block
        )
        every { transactionService.predictStandardTransactionToAllStandardOutputSize(any(), any(), any(), any()) } returns 500
        every { transactionService.createStandardTransaction(any(), any(), any(), any()) } returns transaction
        every { transactionContainer.getPendingSignatureIndexForAddress(any()) } returns 1L
        spvContext.setAddressState(ledgerContext)
        runBlocking {
            spvService.sendCoins(
                "VcspPDtJNpNmLV8qFTqb2F5157JNHS".asLightAddress(),
                listOf(
                    Output(
                        "VDBt3GuwPe1tA5m4duTPkBq5vF22rw".asLightAddress(),
                        100.asCoin()
                    )
                )
            )
        }
        //reply.getResults(0).message.contains("Address doesn't exist or invalid") shouldBe true
    }

    @Test(expected = SendCoinsException::class)
    fun sendCoinsWhenBalanceIsNotEnough() {
        val transaction: Transaction = StandardTransaction(EMPTY_VBK_TX)
        val ledgerContext = LedgerContext(
            address = Address(address),
            block = block,
            ledgerValue = LedgerValue(50L, 0L, 0L),
        )
        every { transactionService.predictStandardTransactionToAllStandardOutputSize(any(), any(), any(), any()) } returns 500
        every { transactionService.createStandardTransaction(any(), any(), any(), any()) } returns transaction
        every { transactionContainer.getPendingSignatureIndexForAddress(any()) } returns 1L
        spvContext.setAddressState(ledgerContext)
        runBlocking {
            spvService.sendCoins(
                address.asLightAddress(),
                listOf(
                    Output(
                        "VDBt3GuwPe1tA5m4duTPkBq5vF22rw".asLightAddress(),
                        100.asCoin()
                    )
                )
            )
        }
        //reply.getResults(0).message.contains("Available balance is not enough") shouldBe true
    }

    @Test(expected = SendCoinsException::class)
    fun sendCoinsWhenAddressInfoDoentExist() {
        val transaction: Transaction = StandardTransaction(EMPTY_VBK_TX)
        every { transactionService.predictStandardTransactionToAllStandardOutputSize(any(), any(), any(), any()) } returns 500
        every { transactionService.createStandardTransaction(any(), any(), any(), any()) } returns transaction
        every { transactionContainer.getPendingSignatureIndexForAddress(any()) } returns 1L
        runBlocking {
            spvService.sendCoins(
                "VcspPDtJNpNmLV8qFTqb2F5157JNHS".asLightAddress(),
                listOf(
                    Output(
                        "VDBt3GuwPe1tA5m4duTPkBq5vF22rw".asLightAddress(),
                        100.asCoin()
                    )
                )
            )
        }
        //reply.getResults(0).message.contains("Information about this address does not exist") shouldBe true
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
        every { addressManager.isLocked } returns false
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

    @Test
    fun submitTransactionsWhenTxSignedThenTrue() {
        val transaction = StandardTransaction(EMPTY_VBK_TX)
        runBlocking {
            spvService.submitTransactions(listOf(transaction))
        }
        coVerify(exactly = 1) { transactionContainer.addTransaction(transaction) }
    }

    @Test
    fun signatureIndexWhenSuccess() {
        val kpg = KeyPairGenerator.getInstance("RSA")
        val keyPair = kpg.generateKeyPair()
        val address = AddressPubKey("VcspPDtJNpNmLV8qFTqb2F5157JNHS", keyPair.public)
        every { addressManager.defaultAddress } returns address
        val index = 101L
        val ledgerContext = LedgerContext(
            address = Address(address.hash),
            block = block,
            ledgerValue = LedgerValue(50L, 0L, index),
        )
        spvContext.setAddressState(ledgerContext)
        val ctx = spvContext.getSignatureIndex(Address(address.hash))
        ctx shouldBe index
    }

    @Test(expected = EndorsementCreationException::class)
    fun createAltChainEndorsementWhenMaxFeeLess() {
        val transaction = StandardTransaction(EMPTY_VBK_TX).also {
            it.inputAddress = StandardAddress("VcspPDtJNpNmLV8qFTqb2F5157JNHS")
            it.inputAmount = Coin.ONE
            it.data = ByteArray(12)
        }

        every { transactionService.createUnsignedAltChainEndorsementTransaction(any(), any(), any(), any()) } returns transaction

        val reply = spvService.createAltChainEndorsement(ByteArray(12), Address("VcspPDtJNpNmLV8qFTqb2F5157JNHS"), 10000L, 1000L)
        reply shouldNotBe null
    }

    @Test(expected = EndorsementCreationException::class)
    fun createAltChainEndorsementWhenThrowException() {
        every { transactionService.createUnsignedAltChainEndorsementTransaction(any(), any(), any(), any()) } throws RuntimeException()

        val reply = spvService.createAltChainEndorsement(ByteArray(12), Address("VcspPDtJNpNmLV8qFTqb2F5157JNHS"), 10000L, 100000000L)
        reply shouldNotBe null
    }

    @Test(expected = Test.None::class)
    fun createAltChainEndorsement() {
        val transaction = StandardTransaction(EMPTY_VBK_TX).also {
            it.inputAddress = StandardAddress("VcspPDtJNpNmLV8qFTqb2F5157JNHS")
            it.inputAmount = Coin.ONE
            it.data = ByteArray(12)
        }
        every { transactionService.createUnsignedAltChainEndorsementTransaction(any(), any(), any(), any()) } returns transaction

        val reply = spvService.createAltChainEndorsement(ByteArray(12), Address("VcspPDtJNpNmLV8qFTqb2F5157JNHS"), 10000L, 100000000L)
        reply shouldNotBe null
    }
}
