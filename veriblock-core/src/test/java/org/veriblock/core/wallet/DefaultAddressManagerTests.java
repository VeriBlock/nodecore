// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.wallet;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.veriblock.core.types.Pair;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.PrivateKey;
import java.util.Base64;

/**
 * The tests in this class are expensive to run and inhibitive on the build server, due to the reliance on TemporaryFolder
 * for producing real wallet.dat files.  As such, the class has been marked for Ignore.
 */
@Ignore
public class DefaultAddressManagerTests {
    private static final String SERIALIZED_WALLET =
            "AQFAWDA+AgEAMBAGByqGSM49AgEGBSuBBAAKBCcwJQIBAQQgldGZlDNRd4U8G1+O5tJQw/gJd4/" +
                    "stkwzBzatO+hDws0wVjAQBgcqhkjOPQIBBgUrgQQACgNCAARGZrWZLZxoxJAenh9X9RtGKnJAK7jtU6KTyhDN71IDcQzG4taVMcEER" +
                    "hYAsUFPGlx332SyBZo6YbEB18n1hty2";
    private static final String PRIVATE_KEY = "MD4CAQAwEAYHKoZIzj0CAQYFK4EEAAoEJzAlAgEBBCCV0" +
            "ZmUM1F3hTwbX47m0lDD+Al3j+y2TDMHNq076EPCzQ==";
    private static final String PUBLIC_KEY = "MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAERma1mS2caMSQHp" +
            "4fV/UbRipyQCu47VOik8oQze9SA3EMxuLWlTHBBEYWALFBTxpcd99ksgWaOmGxAdfJ9Ybctg==";
    private static final String ADDRESS = "V44i36pPHyhaiW695Xg8PEos4G2PrC";

    private static final String SERIALIZED_V2_WALLET = "{\"version\":2,\"keyType\":1,\"locked\":false,\"defaultAddress\":\"V6dYV2P3sd8ALsgyrnDc7QToW7unHA\",\"addresses\":[{\"publicKey\":\"MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAE1FUzpjM/Dv8gLfOND6GxuJXPHDPIDWPp0C0ju6DyiTCCVGKyb3KrVpP+o1RPyXqSlD8YxMk9qadeuCPgGkvtlA\\u003d\\u003d\",\"cipher\":{\"cipherText\":\"MD4CAQAwEAYHKoZIzj0CAQYFK4EEAAoEJzAlAgEBBCB0TYqZAfRD2u/LZhJBqTV8Dg9+VfOc8gmh2K50QmmT4w\\u003d\\u003d\"}}]}";

    private static final char[] PASSWORD = "Password123".toCharArray();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();


    @Test
    public void load_WhenNew() {
        try {
            File walletFile = tempFolder.newFile();
            walletFile.delete();

            DefaultAddressManager addressManager = new DefaultAddressManager();
            addressManager.load(walletFile);

            Assert.assertEquals(1, addressManager.getNumAddresses());
            Assert.assertNotNull(addressManager.getDefaultAddress());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void load_WhenWalletV1File() {
        try {
            File walletFile = generateV1WalletFile();

            DefaultAddressManager addressManager = new DefaultAddressManager();
            addressManager.load(walletFile);

            Assert.assertEquals(1, addressManager.getNumAddresses());
            Address address = addressManager.get(ADDRESS);

            Assert.assertEquals(address, addressManager.getDefaultAddress());

            byte[] expectedPrivateKey = java.util.Base64.getDecoder().decode(PRIVATE_KEY);
            byte[] expectedPublicKey = java.util.Base64.getDecoder().decode(PUBLIC_KEY);

            Assert.assertArrayEquals(expectedPublicKey, address.getPublicKey().getEncoded());
            Assert.assertArrayEquals(expectedPrivateKey, addressManager.getPrivateKeyForAddress(ADDRESS).getEncoded());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void load_WhenWalletV2File() {
        try {
            File walletFile = generateV2WalletFile();

            DefaultAddressManager addressManager = new DefaultAddressManager();
            addressManager.load(walletFile);

            Assert.assertEquals(1, addressManager.getNumAddresses());
            Address address = addressManager.get("V6dYV2P3sd8ALsgyrnDc7QToW7unHA");

            Assert.assertEquals(address, addressManager.getDefaultAddress());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void importWallet_WhenImportingV1File() {
        try {
            File walletFile = generateV2WalletFile();
            File toImport = generateV1WalletFile();

            DefaultAddressManager addressManager = new DefaultAddressManager();
            addressManager.load(walletFile);

            Pair<Boolean, String> result = addressManager.importWallet(toImport);
            Assert.assertTrue(result.getFirst());

            Assert.assertEquals(2, addressManager.getNumAddresses());
            Assert.assertNotNull(addressManager.get(ADDRESS));
            Assert.assertNotNull(addressManager.get("V6dYV2P3sd8ALsgyrnDc7QToW7unHA"));
            Assert.assertEquals("V6dYV2P3sd8ALsgyrnDc7QToW7unHA", addressManager.getDefaultAddress().getHash());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void importWallet_WhenImportingV2File() {
        try {
            File walletFile = generateV1WalletFile();
            File toImport = generateV2WalletFile();

            DefaultAddressManager addressManager = new DefaultAddressManager();
            addressManager.load(walletFile);

            Pair<Boolean, String> result = addressManager.importWallet(toImport);
            Assert.assertTrue(result.getFirst());

            Assert.assertEquals(2, addressManager.getNumAddresses());
            Assert.assertNotNull(addressManager.get(ADDRESS));
            Assert.assertNotNull(addressManager.get("V6dYV2P3sd8ALsgyrnDc7QToW7unHA"));
            Assert.assertEquals(ADDRESS, addressManager.getDefaultAddress().getHash());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void importWallet_WhenImportingToEncryptedWallet() {
        try {
            File walletFile = generateV1WalletFile();
            File toImport = generateV2WalletFile();

            DefaultAddressManager addressManager = new DefaultAddressManager();
            addressManager.load(walletFile);

            addressManager.encryptWallet(PASSWORD);

            addressManager.unlock(PASSWORD);
            Pair<Boolean, String> result = addressManager.importWallet(toImport);
            Assert.assertTrue(result.getFirst());
            addressManager.lock();

            Assert.assertEquals(2, addressManager.getNumAddresses());
            Assert.assertNotNull(addressManager.get(ADDRESS));
            Assert.assertNotNull(addressManager.get("V6dYV2P3sd8ALsgyrnDc7QToW7unHA"));
            Assert.assertEquals(ADDRESS, addressManager.getDefaultAddress().getHash());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void importWallet_WhenImportingEncryptedWallet() {
        try {
            File walletFile = generateV1WalletFile();
            File toImport = generateV2WalletFile();

            DefaultAddressManager addressManager = new DefaultAddressManager();
            addressManager.load(toImport);
            addressManager.encryptWallet("Temporary".toCharArray());

            addressManager.load(walletFile);
            addressManager.encryptWallet(PASSWORD);

            addressManager.unlock(PASSWORD);
            Pair<Boolean, String> result = addressManager.importWallet(toImport);
            Assert.assertFalse(result.getFirst());
            addressManager.lock();
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void importEncryptedWallet_WhenImportingToNormalWallet() {
        try {
            File walletFile = generateV1WalletFile();
            File toImport = generateV2WalletFile();

            DefaultAddressManager addressManager = new DefaultAddressManager();
            addressManager.load(toImport);
            addressManager.encryptWallet("Temporary".toCharArray());

            addressManager.load(walletFile);
            Pair<Boolean, String> result = addressManager.importEncryptedWallet(toImport, "Temporary".toCharArray());
            Assert.assertTrue(result.getFirst());

            Assert.assertEquals(2, addressManager.getNumAddresses());
            Assert.assertNotNull(addressManager.get(ADDRESS));
            Assert.assertNotNull(addressManager.get("V6dYV2P3sd8ALsgyrnDc7QToW7unHA"));
            Assert.assertEquals(ADDRESS, addressManager.getDefaultAddress().getHash());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void importEncryptedWallet_WhenImportingToEncryptedWallet() {
        try {
            File walletFile = generateV1WalletFile();
            File toImport = generateV2WalletFile();

            DefaultAddressManager addressManager = new DefaultAddressManager();
            addressManager.load(toImport);
            addressManager.encryptWallet("Temporary".toCharArray());

            addressManager.load(walletFile);
            addressManager.encryptWallet(PASSWORD);

            addressManager.unlock(PASSWORD);
            Pair<Boolean, String> result = addressManager.importEncryptedWallet(toImport, "Temporary".toCharArray());
            Assert.assertTrue(result.getFirst());
            addressManager.lock();

            Assert.assertEquals(2, addressManager.getNumAddresses());
            Assert.assertNotNull(addressManager.get(ADDRESS));
            Assert.assertNotNull(addressManager.get("V6dYV2P3sd8ALsgyrnDc7QToW7unHA"));
            Assert.assertEquals(ADDRESS, addressManager.getDefaultAddress().getHash());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void importEncryptedWallet_WhenPasswordFails() {
        try {
            File walletFile = generateV1WalletFile();
            File toImport = generateV2WalletFile();

            DefaultAddressManager addressManager = new DefaultAddressManager();
            addressManager.load(toImport);
            addressManager.encryptWallet("Temporary".toCharArray());

            addressManager.load(walletFile);
            Pair<Boolean, String> result = addressManager.importEncryptedWallet(toImport, "Bad".toCharArray());

            Assert.assertFalse(result.getFirst());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void encryptWallet() {
        try {
            File walletFile = generateV2WalletFile();

            DefaultAddressManager addressManager = new DefaultAddressManager();
            addressManager.load(walletFile);

            PrivateKey expectedKey = addressManager.getPrivateKeyForAddress("V6dYV2P3sd8ALsgyrnDc7QToW7unHA");

            boolean result = addressManager.encryptWallet(PASSWORD);

            Assert.assertTrue(result);

            addressManager.unlock(PASSWORD);
            PrivateKey key = addressManager.getPrivateKeyForAddress("V6dYV2P3sd8ALsgyrnDc7QToW7unHA");
            addressManager.lock();

            Assert.assertArrayEquals(expectedKey.getEncoded(), key.getEncoded());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void importKeyPair_WhenImportingToNormalWallet() {
        try {
            File walletFile = generateV2WalletFile();

            DefaultAddressManager addressManager = new DefaultAddressManager();
            addressManager.load(walletFile);

            addressManager.importKeyPair(Base64.getDecoder().decode(PUBLIC_KEY), Base64.getDecoder().decode(PRIVATE_KEY));

            Assert.assertEquals(2, addressManager.getNumAddresses());
            Assert.assertNotNull(addressManager.get(ADDRESS));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void importKeyPair_WhenImportingToEncryptedWallet() {
        try {
            File walletFile = generateV2WalletFile();

            DefaultAddressManager addressManager = new DefaultAddressManager();
            addressManager.load(walletFile);

            addressManager.encryptWallet(PASSWORD);

            addressManager.unlock(PASSWORD);
            addressManager.importKeyPair(Base64.getDecoder().decode(PUBLIC_KEY), Base64.getDecoder().decode(PRIVATE_KEY));
            addressManager.lock();

            Assert.assertEquals(2, addressManager.getNumAddresses());
            Assert.assertNotNull(addressManager.get(ADDRESS));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void decryptWallet() {
        try {
            File walletFile = generateV2WalletFile();

            DefaultAddressManager addressManager = new DefaultAddressManager();
            addressManager.load(walletFile);

            PrivateKey expectedKey = addressManager.getPrivateKeyForAddress("V6dYV2P3sd8ALsgyrnDc7QToW7unHA");

            boolean result = addressManager.encryptWallet(PASSWORD);

            boolean decryptResult = addressManager.decryptWallet(PASSWORD);

            Assert.assertTrue(result);
            Assert.assertTrue(decryptResult);


            addressManager.unlock(PASSWORD);
            PrivateKey key = addressManager.getPrivateKeyForAddress("V6dYV2P3sd8ALsgyrnDc7QToW7unHA");
            addressManager.lock();

            Assert.assertArrayEquals(expectedKey.getEncoded(), key.getEncoded());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void unlock_WhenPasswordDoesNotMatch() {
        try {
            File walletFile = generateV2WalletFile();

            DefaultAddressManager addressManager = new DefaultAddressManager();
            addressManager.load(walletFile);

            addressManager.encryptWallet(PASSWORD);

            Assert.assertFalse(addressManager.unlock("Bad".toCharArray()));
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    private File generateV1WalletFile() throws IOException {
        File walletFile = tempFolder.newFile();

        try (FileOutputStream stream = new FileOutputStream(walletFile)) {
            byte[] walletBytes = java.util.Base64.getDecoder().decode(SERIALIZED_WALLET);
            stream.write(walletBytes);
            stream.flush();
        }

        return walletFile;
    }

    private File generateV2WalletFile() throws IOException {
        File walletFile = tempFolder.newFile();

        try (FileOutputStream stream = new FileOutputStream(walletFile)) {
            stream.write(SERIALIZED_V2_WALLET.getBytes("UTF-8"));
            stream.flush();
        }

        return walletFile;
    }
}
