// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.core.wallet

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

/**
 * The tests in this class are expensive to run and inhibitive on the build server, due to the reliance on TemporaryFolder
 * for producing real wallet.dat files.  As such, the class has been marked for Ignore.
 */
@Ignore
class DefaultAddressManagerTests {

    companion object {
        private const val SERIALIZED_WALLET =
            "AQFAWDA+AgEAMBAGByqGSM49AgEGBSuBBAAKBCcwJQIBAQQgldGZlDNRd4U8G1+O5tJQw/gJd4/" +
                "stkwzBzatO+hDws0wVjAQBgcqhkjOPQIBBgUrgQQACgNCAARGZrWZLZxoxJAenh9X9RtGKnJAK7jtU6KTyhDN71IDcQzG4taVMcEER" +
                "hYAsUFPGlx332SyBZo6YbEB18n1hty2"
        private const val PRIVATE_KEY = "MD4CAQAwEAYHKoZIzj0CAQYFK4EEAAoEJzAlAgEBBCCV0" +
            "ZmUM1F3hTwbX47m0lDD+Al3j+y2TDMHNq076EPCzQ=="
        private const val PUBLIC_KEY = "MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAERma1mS2caMSQHp" +
            "4fV/UbRipyQCu47VOik8oQze9SA3EMxuLWlTHBBEYWALFBTxpcd99ksgWaOmGxAdfJ9Ybctg=="
        private const val ADDRESS = "V44i36pPHyhaiW695Xg8PEos4G2PrC"
        private const val SERIALIZED_V2_WALLET =
            "{\"version\":2,\"keyType\":1,\"locked\":false,\"defaultAddress\":\"V6dYV2P3sd8ALsgyrnDc7QToW7unHA\",\"addresses\":[{\"publicKey\":\"MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAE1FUzpjM/Dv8gLfOND6GxuJXPHDPIDWPp0C0ju6DyiTCCVGKyb3KrVpP+o1RPyXqSlD8YxMk9qadeuCPgGkvtlA\\u003d\\u003d\",\"cipher\":{\"cipherText\":\"MD4CAQAwEAYHKoZIzj0CAQYFK4EEAAoEJzAlAgEBBCB0TYqZAfRD2u/LZhJBqTV8Dg9+VfOc8gmh2K50QmmT4w\\u003d\\u003d\"}}]}"
        private val PASSWORD = "Password123".toCharArray()
    }

    @Rule
    var tempFolder = TemporaryFolder()

    @Test
    fun load_WhenNew() {
            val walletFile = tempFolder.newFile()
            walletFile.delete()
            val addressManager = AddressManager()
            addressManager.load(walletFile)
            addressManager.numAddresses.toLong() shouldBe 1
            addressManager.defaultAddress shouldNotBe null
    }

    @Test
    fun load_WhenWalletV1File() {
            val walletFile = generateV1WalletFile()
            val addressManager = AddressManager()
            addressManager.load(walletFile)
            addressManager.numAddresses.toLong() shouldBe 1
            val address = addressManager[ADDRESS]
            addressManager.defaultAddress shouldBe address
            val expectedPrivateKey = Base64.getDecoder().decode(PRIVATE_KEY)
            val expectedPublicKey = Base64.getDecoder().decode(PUBLIC_KEY)
            address.publicKey.encoded.toList() shouldContainExactly expectedPublicKey.toList()
            addressManager.getPrivateKeyForAddress(ADDRESS).encoded.toList() shouldContainExactly expectedPrivateKey.toList()
    }

    @Test
    fun load_WhenWalletV2File() {
            val walletFile = generateV2WalletFile()
            val addressManager = AddressManager()
            addressManager.load(walletFile)
            addressManager.numAddresses.toLong() shouldBe 1
            val address = addressManager["V6dYV2P3sd8ALsgyrnDc7QToW7unHA"]
            addressManager.defaultAddress shouldBe address
    }

    @Test
    fun importWallet_WhenImportingV1File() {
            val walletFile = generateV2WalletFile()
            val toImport = generateV1WalletFile()
            val addressManager = AddressManager()
            addressManager.load(walletFile)
            val result = addressManager.importWallet(toImport)
            result.first shouldBe true
            addressManager.numAddresses.toLong() shouldBe 2
            addressManager[ADDRESS] shouldNotBe null
            addressManager["V6dYV2P3sd8ALsgyrnDc7QToW7unHA"] shouldNotBe null
            addressManager.defaultAddress.hash shouldBe "V6dYV2P3sd8ALsgyrnDc7QToW7unHA"
    }

    @Test
    fun importWallet_WhenImportingV2File() {
            val walletFile = generateV1WalletFile()
            val toImport = generateV2WalletFile()
            val addressManager = AddressManager()
            addressManager.load(walletFile)
            val result = addressManager.importWallet(toImport)
            result.first shouldBe true
            addressManager.numAddresses.toLong() shouldBe 2
            addressManager[ADDRESS] shouldNotBe null
            addressManager["V6dYV2P3sd8ALsgyrnDc7QToW7unHA"] shouldNotBe null
            addressManager.defaultAddress.hash shouldBe ADDRESS
    }

    @Test
    fun importWallet_WhenImportingToEncryptedWallet() {
            val walletFile = generateV1WalletFile()
            val toImport = generateV2WalletFile()
            val addressManager = AddressManager()
            addressManager.load(walletFile)
            addressManager.encryptWallet(PASSWORD)
            addressManager.unlock(PASSWORD)
            val result = addressManager.importWallet(toImport)
            result.first shouldBe true
            addressManager.lock()
            addressManager.numAddresses.toLong() shouldBe 2
            addressManager[ADDRESS] shouldNotBe null
            addressManager["V6dYV2P3sd8ALsgyrnDc7QToW7unHA"] shouldNotBe null
            addressManager.defaultAddress.hash shouldBe ADDRESS
    }

    @Test
    fun importWallet_WhenImportingEncryptedWallet() {
            val walletFile = generateV1WalletFile()
            val toImport = generateV2WalletFile()
            val addressManager = AddressManager()
            addressManager.load(toImport)
            addressManager.encryptWallet("Temporary".toCharArray())
            addressManager.load(walletFile)
            addressManager.encryptWallet(PASSWORD)
            addressManager.unlock(PASSWORD)
            val result = addressManager.importWallet(toImport)
            result.first shouldBe false
            addressManager.lock()
    }

    @Test
    fun importEncryptedWallet_WhenImportingToNormalWallet() {
            val walletFile = generateV1WalletFile()
            val toImport = generateV2WalletFile()
            val addressManager = AddressManager()
            addressManager.load(toImport)
            addressManager.encryptWallet("Temporary".toCharArray())
            addressManager.load(walletFile)
            val result = addressManager.importEncryptedWallet(toImport, "Temporary".toCharArray())
            result.first shouldBe true
            addressManager.numAddresses.toLong() shouldBe 2
            addressManager[ADDRESS] shouldNotBe null
            addressManager["V6dYV2P3sd8ALsgyrnDc7QToW7unHA"] shouldNotBe null
            addressManager.defaultAddress.hash shouldBe ADDRESS
    }

    @Test
    fun importEncryptedWallet_WhenImportingToEncryptedWallet() {
            val walletFile = generateV1WalletFile()
            val toImport = generateV2WalletFile()
            val addressManager = AddressManager()
            addressManager.load(toImport)
            addressManager.encryptWallet("Temporary".toCharArray())
            addressManager.load(walletFile)
            addressManager.encryptWallet(PASSWORD)
            addressManager.unlock(PASSWORD)
            val result = addressManager.importEncryptedWallet(toImport, "Temporary".toCharArray())
            result.first shouldBe true
            addressManager.lock()
            addressManager.numAddresses.toLong() shouldBe 2
            addressManager[ADDRESS] shouldNotBe null
            addressManager["V6dYV2P3sd8ALsgyrnDc7QToW7unHA"] shouldNotBe null
            addressManager.defaultAddress.hash shouldBe ADDRESS
    }

    @Test
    fun importEncryptedWallet_WhenPasswordFails() {
            val walletFile = generateV1WalletFile()
            val toImport = generateV2WalletFile()
            val addressManager = AddressManager()
            addressManager.load(toImport)
            addressManager.encryptWallet("Temporary".toCharArray())
            addressManager.load(walletFile)
            val result = addressManager.importEncryptedWallet(toImport, "Bad".toCharArray())
            result.first shouldBe false
    }

    @Test
    fun encryptWallet() {
            val walletFile = generateV2WalletFile()
            val addressManager = AddressManager()
            addressManager.load(walletFile)
            val expectedKey = addressManager.getPrivateKeyForAddress("V6dYV2P3sd8ALsgyrnDc7QToW7unHA")
            val result = addressManager.encryptWallet(PASSWORD)
            result shouldBe true
            addressManager.unlock(PASSWORD)
            val key = addressManager.getPrivateKeyForAddress("V6dYV2P3sd8ALsgyrnDc7QToW7unHA")
            addressManager.lock()
            key.encoded.toList() shouldContainExactly expectedKey.encoded.toList()
    }

    @Test
    fun importKeyPair_WhenImportingToNormalWallet() {
            val walletFile = generateV2WalletFile()
            val addressManager = AddressManager()
            addressManager.load(walletFile)
            addressManager.importKeyPair(
                Base64.getDecoder().decode(PUBLIC_KEY),
                Base64.getDecoder().decode(PRIVATE_KEY)
            )
            addressManager.numAddresses.toLong() shouldBe 2
            addressManager[ADDRESS] shouldNotBe null
    }

    @Test
    fun importKeyPair_WhenImportingToEncryptedWallet() {
            val walletFile = generateV2WalletFile()
            val addressManager = AddressManager()
            addressManager.load(walletFile)
            addressManager.encryptWallet(PASSWORD)
            addressManager.unlock(PASSWORD)
            addressManager.importKeyPair(
                Base64.getDecoder().decode(PUBLIC_KEY),
                Base64.getDecoder().decode(PRIVATE_KEY)
            )
            addressManager.lock()
            addressManager.numAddresses.toLong() shouldBe 2
            addressManager[ADDRESS] shouldNotBe null
    }

    @Test
    fun decryptWallet() {
            val walletFile = generateV2WalletFile()
            val addressManager = AddressManager()
            addressManager.load(walletFile)
            val expectedKey = addressManager.getPrivateKeyForAddress("V6dYV2P3sd8ALsgyrnDc7QToW7unHA")
            val result = addressManager.encryptWallet(PASSWORD)
            val decryptResult = addressManager.decryptWallet(PASSWORD)
            result shouldBe true
            decryptResult shouldBe true
            addressManager.unlock(PASSWORD)
            val key = addressManager.getPrivateKeyForAddress("V6dYV2P3sd8ALsgyrnDc7QToW7unHA")
            addressManager.lock()
            key.encoded.toList() shouldContainExactly expectedKey.encoded.toList()
    }

    @Test
    fun unlock_WhenPasswordDoesNotMatch() {
            val walletFile = generateV2WalletFile()
            val addressManager = AddressManager()
            addressManager.load(walletFile)
            addressManager.encryptWallet(PASSWORD)
            addressManager.unlock("Bad".toCharArray()) shouldBe false
    }

    @Throws(IOException::class)
    private fun generateV1WalletFile(): File {
        val walletFile = tempFolder.newFile()
        FileOutputStream(walletFile).use { stream ->
            val walletBytes = Base64.getDecoder().decode(SERIALIZED_WALLET)
            stream.write(walletBytes)
            stream.flush()
        }
        return walletFile
    }

    @Throws(IOException::class)
    private fun generateV2WalletFile(): File {
        val walletFile = tempFolder.newFile()
        FileOutputStream(walletFile).use { stream ->
            stream.write(SERIALIZED_V2_WALLET.toByteArray(charset("UTF-8")))
            stream.flush()
        }
        return walletFile
    }
}
