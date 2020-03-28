package veriblock.admin.service.impl;

import com.google.protobuf.ByteString;
import nodecore.api.grpc.VeriBlockMessages;
import nodecore.api.grpc.utilities.ByteStringAddressUtility;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.veriblock.core.contracts.AddressManager;
import org.veriblock.core.types.Pair;
import org.veriblock.core.wallet.Address;
import org.veriblock.core.wallet.WalletLockedException;
import org.veriblock.core.wallet.WalletUnreadableException;
import org.veriblock.sdk.models.Coin;
import org.veriblock.sdk.models.Sha256Hash;
import org.veriblock.sdk.util.Base58;
import veriblock.SpvContext;
import veriblock.conf.TestNetParameters;
import veriblock.model.LedgerContext;
import veriblock.model.LedgerProofStatus;
import veriblock.model.LedgerValue;
import veriblock.model.StandardAddress;
import veriblock.model.StandardTransaction;
import veriblock.model.Transaction;
import veriblock.net.LocalhostDiscovery;
import veriblock.net.PeerTable;
import veriblock.service.AdminApiService;
import veriblock.service.PendingTransactionContainer;
import veriblock.service.TransactionFactory;
import veriblock.service.impl.AdminApiServiceImpl;
import veriblock.service.impl.Blockchain;
import veriblock.service.impl.TransactionService;

import java.io.File;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AdminApiServiceImplTest {

    private final SpvContext spvContext = new SpvContext();
    private TransactionService transactionService;
    private AddressManager addressManager;
    private PeerTable peerTable;
    private AdminApiService adminApiService;
    private TransactionFactory transactionFactory;
    private PendingTransactionContainer transactionContainer;
    private Blockchain blockchain;

    @Before
    public void setUp() throws IOException {
        spvContext.init(TestNetParameters.get(), new LocalhostDiscovery(TestNetParameters.get()), false);

        this.peerTable = mock(PeerTable.class);
        this.transactionService = mock(TransactionService.class);
        this.addressManager = mock(AddressManager.class);
        this.transactionFactory = mock(TransactionFactory.class);
        this.transactionContainer = mock(PendingTransactionContainer.class);
        this.blockchain = mock(Blockchain.class);
        this.adminApiService =
            new AdminApiServiceImpl(spvContext, peerTable, transactionService, addressManager, transactionFactory, transactionContainer, blockchain);
    }

    @Test
    public void sendCoins() {
        List<Transaction> transactions = new ArrayList<>();
        Transaction transaction = new StandardTransaction(Sha256Hash.ZERO_HASH);
        transactions.add(transaction);
        LedgerContext ledgerContext = new LedgerContext();
        ledgerContext.setLedgerValue(new LedgerValue(100L, 0l, 0L));
        ledgerContext.setLedgerProofStatus(LedgerProofStatus.ADDRESS_EXISTS);

        VeriBlockMessages.Output output = VeriBlockMessages.Output.newBuilder()
            .setAddress(ByteStringAddressUtility.createProperByteStringAutomatically("VDBt3GuwPe1tA5m4duTPkBq5vF22rw"))
            .setAmount(100)
            .build();

        VeriBlockMessages.SendCoinsRequest request = VeriBlockMessages.SendCoinsRequest.newBuilder()
            .addAmounts(output)
            .setSourceAddress(ByteStringAddressUtility.createProperByteStringAutomatically("VcspPDtJNpNmLV8qFTqb2F5157JNHS"))
            .build();

        when(peerTable.getAddressState(any())).thenReturn(ledgerContext);
        when(transactionService
            .predictStandardTransactionToAllStandardOutputSize(ArgumentMatchers.anyLong(), ArgumentMatchers.any(), ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyInt()
            )).thenReturn(500);
        when(transactionService
            .createTransactionsByOutputList(ArgumentMatchers.anyList(), ArgumentMatchers.anyList()))
            .thenReturn(transactions);
        when(transactionContainer.getPendingSignatureIndexForAddress(ArgumentMatchers.any())).thenReturn(1L);
        doNothing().when(peerTable).advertise(ArgumentMatchers.any());

        VeriBlockMessages.SendCoinsReply reply = adminApiService.sendCoins(request);

        verify(transactionService, times(1))
            .createTransactionsByOutputList(ArgumentMatchers.anyList(), ArgumentMatchers.anyList());
        verify(transactionContainer, times(1)).getPendingSignatureIndexForAddress(ArgumentMatchers.any());
        verify(peerTable, times(1)).advertise(ArgumentMatchers.any());

        Assert.assertTrue(reply.getSuccess());
        Assert.assertNotNull(reply.getTxIds(0));
        Assert.assertTrue(Sha256Hash.wrap(reply.getTxIds(0).toByteArray()).equals(transaction.getTxId()));
    }

    @Test
    public void sendCoinsWhenAddressDoesntExist() {
        Transaction transaction = new StandardTransaction(Sha256Hash.ZERO_HASH);
        LedgerContext ledgerContext = new LedgerContext();
        ledgerContext.setLedgerValue(new LedgerValue(100L, 0l, 0L));
        ledgerContext.setLedgerProofStatus(LedgerProofStatus.ADDRESS_DOES_NOT_EXIST);

        VeriBlockMessages.Output output = VeriBlockMessages.Output.newBuilder()
            .setAddress(ByteStringAddressUtility.createProperByteStringAutomatically("VDBt3GuwPe1tA5m4duTPkBq5vF22rw"))
            .setAmount(100)
            .build();

        VeriBlockMessages.SendCoinsRequest request = VeriBlockMessages.SendCoinsRequest.newBuilder()
            .addAmounts(output)
            .setSourceAddress(ByteStringAddressUtility.createProperByteStringAutomatically("VcspPDtJNpNmLV8qFTqb2F5157JNHS"))
            .build();

        when(transactionService
            .predictStandardTransactionToAllStandardOutputSize(ArgumentMatchers.anyLong(), ArgumentMatchers.any(), ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyInt()
            )).thenReturn(500);
        when(transactionService
            .createStandardTransaction(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.anyLong()))
            .thenReturn(transaction);
        when(transactionContainer.getPendingSignatureIndexForAddress(ArgumentMatchers.any())).thenReturn(1L);
        when(peerTable.getAddressState(anyString())).thenReturn(ledgerContext);
        doNothing().when(peerTable).advertise(ArgumentMatchers.any());

        VeriBlockMessages.SendCoinsReply reply = adminApiService.sendCoins(request);

        Assert.assertFalse(reply.getSuccess());
        Assert.assertTrue(reply.getResults(0).getMessage().contains("Address doesn't exist or invalid"));
    }

    @Test
    public void sendCoinsWhenAddressDoesntIsInvalid() {
        Transaction transaction = new StandardTransaction(Sha256Hash.ZERO_HASH);
        LedgerContext ledgerContext = new LedgerContext();
        ledgerContext.setLedgerValue(new LedgerValue(100L, 0l, 0L));
        ledgerContext.setLedgerProofStatus(LedgerProofStatus.ADDRESS_IS_INVALID);

        VeriBlockMessages.Output output = VeriBlockMessages.Output.newBuilder()
            .setAddress(ByteStringAddressUtility.createProperByteStringAutomatically("VDBt3GuwPe1tA5m4duTPkBq5vF22rw"))
            .setAmount(100)
            .build();

        VeriBlockMessages.SendCoinsRequest request = VeriBlockMessages.SendCoinsRequest.newBuilder()
            .addAmounts(output)
            .setSourceAddress(ByteStringAddressUtility.createProperByteStringAutomatically("VcspPDtJNpNmLV8qFTqb2F5157JNHS"))
            .build();

        when(transactionService
            .predictStandardTransactionToAllStandardOutputSize(ArgumentMatchers.anyLong(), ArgumentMatchers.any(), ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyInt()
            )).thenReturn(500);
        when(transactionService
            .createStandardTransaction(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.anyLong()))
            .thenReturn(transaction);
        when(transactionContainer.getPendingSignatureIndexForAddress(ArgumentMatchers.any())).thenReturn(1L);
        when(peerTable.getAddressState(anyString())).thenReturn(ledgerContext);
        doNothing().when(peerTable).advertise(ArgumentMatchers.any());

        VeriBlockMessages.SendCoinsReply reply = adminApiService.sendCoins(request);

        Assert.assertFalse(reply.getSuccess());
        Assert.assertTrue(reply.getResults(0).getMessage().contains("Address doesn't exist or invalid"));
    }

    @Test
    public void sendCoinsWhenBalanceIsNotEnough() {
        Transaction transaction = new StandardTransaction(Sha256Hash.ZERO_HASH);
        LedgerContext ledgerContext = new LedgerContext();
        ledgerContext.setLedgerValue(new LedgerValue(50L, 0l, 0L));
        ledgerContext.setLedgerProofStatus(LedgerProofStatus.ADDRESS_EXISTS);

        VeriBlockMessages.Output output = VeriBlockMessages.Output.newBuilder()
            .setAddress(ByteStringAddressUtility.createProperByteStringAutomatically("VDBt3GuwPe1tA5m4duTPkBq5vF22rw"))
            .setAmount(100)
            .build();

        VeriBlockMessages.SendCoinsRequest request = VeriBlockMessages.SendCoinsRequest.newBuilder()
            .addAmounts(output)
            .setSourceAddress(ByteStringAddressUtility.createProperByteStringAutomatically("VcspPDtJNpNmLV8qFTqb2F5157JNHS"))
            .build();

        when(transactionService
            .predictStandardTransactionToAllStandardOutputSize(ArgumentMatchers.anyLong(), ArgumentMatchers.any(), ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyInt()
            )).thenReturn(500);
        when(transactionService
            .createStandardTransaction(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.anyLong()))
            .thenReturn(transaction);
        when(transactionContainer.getPendingSignatureIndexForAddress(ArgumentMatchers.any())).thenReturn(1L);
        when(peerTable.getAddressState(anyString())).thenReturn(ledgerContext);
        doNothing().when(peerTable).advertise(ArgumentMatchers.any());

        VeriBlockMessages.SendCoinsReply reply = adminApiService.sendCoins(request);

        Assert.assertFalse(reply.getSuccess());
        Assert.assertTrue(reply.getResults(0).getMessage().contains("Available balance is not enough"));
    }

    @Test
    public void sendCoinsWhenAddressInfoDoentExist() {
        Transaction transaction = new StandardTransaction(Sha256Hash.ZERO_HASH);

        VeriBlockMessages.Output output = VeriBlockMessages.Output.newBuilder()
            .setAddress(ByteStringAddressUtility.createProperByteStringAutomatically("VDBt3GuwPe1tA5m4duTPkBq5vF22rw"))
            .setAmount(100)
            .build();

        VeriBlockMessages.SendCoinsRequest request = VeriBlockMessages.SendCoinsRequest.newBuilder()
            .addAmounts(output)
            .setSourceAddress(ByteStringAddressUtility.createProperByteStringAutomatically("VcspPDtJNpNmLV8qFTqb2F5157JNHS"))
            .build();

        when(transactionService
            .predictStandardTransactionToAllStandardOutputSize(ArgumentMatchers.anyLong(), ArgumentMatchers.any(), ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyInt()
            )).thenReturn(500);
        when(transactionService
            .createStandardTransaction(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.anyLong()))
            .thenReturn(transaction);
        when(transactionContainer.getPendingSignatureIndexForAddress(ArgumentMatchers.any())).thenReturn(1L);
        when(peerTable.getAddressState(anyString())).thenReturn(null);
        doNothing().when(peerTable).advertise(ArgumentMatchers.any());

        VeriBlockMessages.SendCoinsReply reply = adminApiService.sendCoins(request);

        Assert.assertFalse(reply.getSuccess());
        Assert.assertTrue(reply.getResults(0).getMessage().contains("Information about this address does not exist"));
    }

    @Test
    public void unlockWalletWhenWalletIsLockThenTrue() {
        VeriBlockMessages.UnlockWalletRequest unlockWalletRequest = VeriBlockMessages.UnlockWalletRequest.newBuilder()
            .setPassphrase("123")
            .build();

        when(addressManager.unlock(ArgumentMatchers.any())).thenReturn(true);

        VeriBlockMessages.ProtocolReply reply = adminApiService.unlockWallet(unlockWalletRequest);

        verify(addressManager,  times(1)).unlock(ArgumentMatchers.any());
        Assert.assertEquals(true, reply.getSuccess());
    }

    @Test
    public void unlockWalletWhenWalletIsUnlockThenFalse() {
        VeriBlockMessages.UnlockWalletRequest unlockWalletRequest = VeriBlockMessages.UnlockWalletRequest.newBuilder()
                .setPassphrase("123")
                .build();

        when(addressManager.unlock(ArgumentMatchers.any())).thenReturn(false);

        VeriBlockMessages.ProtocolReply reply = adminApiService.unlockWallet(unlockWalletRequest);

        verify(addressManager,  times(1)).unlock(ArgumentMatchers.any());
        Assert.assertEquals(false, reply.getSuccess());
    }

    @Test
    public void importWalletWhenWalletIsLockThenFalse() {
        VeriBlockMessages.ImportWalletRequest importWalletRequest = VeriBlockMessages.ImportWalletRequest.newBuilder()
                .setPassphrase("123")
                .setSourceLocation(ByteString.copyFromUtf8("test/source"))
                .build();

        when(addressManager.isLocked()).thenReturn(true);

        VeriBlockMessages.ImportWalletReply reply = adminApiService.importWallet(importWalletRequest);

        verify(addressManager,  times(1)).isLocked();
        Assert.assertEquals(false, reply.getSuccess());
    }

    @Test
    public void importWalletWhenWalletIsUnlockAndWithoutPassphraseAndResultFalseThenFalse() throws WalletUnreadableException {
        VeriBlockMessages.ImportWalletRequest importWalletRequest = VeriBlockMessages.ImportWalletRequest.newBuilder()
                .setSourceLocation(ByteString.copyFromUtf8("test/source"))
                .build();

        when(addressManager.isLocked()).thenReturn(false);
        Pair<Boolean, String> result = new Pair<>(false, "test_string");

        when(addressManager.importWallet(ArgumentMatchers.any(File.class))).thenReturn(result);

        VeriBlockMessages.ImportWalletReply reply = adminApiService.importWallet(importWalletRequest);

        verify(addressManager,  times(1)).isLocked();
        verify(addressManager,  times(1)).importWallet(ArgumentMatchers.any(File.class));
        Assert.assertEquals(false, reply.getSuccess());
    }

    @Test
    public void importWalletWhenWalletIsUnlockAndWithoutPassphraseAndResultTrueThenSuccess() throws WalletUnreadableException {
        VeriBlockMessages.ImportWalletRequest importWalletRequest = VeriBlockMessages.ImportWalletRequest.newBuilder()
                .setSourceLocation(ByteString.copyFromUtf8("test/source"))
                .build();

        when(addressManager.isLocked()).thenReturn(false);
        Pair<Boolean, String> result = new Pair<>(true, "test_string");

        when(addressManager.importWallet(ArgumentMatchers.any(File.class))).thenReturn(result);

        VeriBlockMessages.ImportWalletReply reply = adminApiService.importWallet(importWalletRequest);

        verify(addressManager,  times(1)).isLocked();
        verify(addressManager,  times(1)).importWallet(ArgumentMatchers.any(File.class));
        Assert.assertEquals(true, reply.getSuccess());
    }

    @Test
    public void importWalletWhenThrowExceptionThenFalse() throws WalletUnreadableException {
        VeriBlockMessages.ImportWalletRequest importWalletRequest = VeriBlockMessages.ImportWalletRequest.newBuilder()
                .setSourceLocation(ByteString.copyFromUtf8("test/source"))
                .build();

        when(addressManager.isLocked()).thenReturn(false);

        when(addressManager.importWallet(ArgumentMatchers.any())).thenThrow(new RuntimeException());

        VeriBlockMessages.ImportWalletReply reply = adminApiService.importWallet(importWalletRequest);

        verify(addressManager,  times(1)).isLocked();
        verify(addressManager,  times(1)).importWallet(ArgumentMatchers.any());
        Assert.assertEquals(false, reply.getSuccess());
    }

    @Test
    public void encryptWalletWhenNoPassphraseThenFalse() {

        VeriBlockMessages.EncryptWalletRequest request = VeriBlockMessages.EncryptWalletRequest.newBuilder()
                .setPassphraseBytes(ByteString.copyFrom(new byte[0]))
                .build();

        VeriBlockMessages.ProtocolReply reply = adminApiService.encryptWallet(request);

        Assert.assertEquals(false, reply.getSuccess());
    }

    @Test
    public void encryptWalletWhenEncryptFalseThenFalse() {
        VeriBlockMessages.EncryptWalletRequest request = VeriBlockMessages.EncryptWalletRequest.newBuilder()
                .setPassphraseBytes(ByteString.copyFrom("passphrase".getBytes()))
                .build();

        when(addressManager.encryptWallet(ArgumentMatchers.any())).thenReturn(false);

        VeriBlockMessages.ProtocolReply reply = adminApiService.encryptWallet(request);

        verify(addressManager,  times(1)).encryptWallet(ArgumentMatchers.any());
        Assert.assertEquals(false, reply.getSuccess());
    }

    @Test
    public void encryptWalletWhenExceptionThenFalse() {
        VeriBlockMessages.EncryptWalletRequest request = VeriBlockMessages.EncryptWalletRequest.newBuilder()
                .setPassphraseBytes(ByteString.copyFrom("passphrase".getBytes()))
                .build();

        when(addressManager.encryptWallet(ArgumentMatchers.any())).thenThrow(new IllegalStateException());

        VeriBlockMessages.ProtocolReply reply = adminApiService.encryptWallet(request);

        verify(addressManager,  times(1)).encryptWallet(ArgumentMatchers.any());
        Assert.assertEquals(false, reply.getSuccess());
    }

    @Test
    public void encryptWalletWhenEncryptTrueThenTrue() {
        VeriBlockMessages.EncryptWalletRequest request = VeriBlockMessages.EncryptWalletRequest.newBuilder()
                .setPassphraseBytes(ByteString.copyFrom("passphrase".getBytes()))
                .build();

        when(addressManager.encryptWallet(ArgumentMatchers.any())).thenReturn(true);

        VeriBlockMessages.ProtocolReply reply = adminApiService.encryptWallet(request);

        verify(addressManager,  times(1)).encryptWallet(ArgumentMatchers.any());
        Assert.assertEquals(true, reply.getSuccess());
    }

    @Test
    public void decryptWalletWhenNoPassphraseThenFalse() {

        VeriBlockMessages.DecryptWalletRequest request = VeriBlockMessages.DecryptWalletRequest.newBuilder()
                .setPassphraseBytes(ByteString.copyFrom(new byte[0]))
                .build();

        VeriBlockMessages.ProtocolReply reply = adminApiService.decryptWallet(request);

        Assert.assertEquals(false, reply.getSuccess());
    }

    @Test
    public void decryptWalletWhenEncryptFalseThenFalse() {
        VeriBlockMessages.DecryptWalletRequest request = VeriBlockMessages.DecryptWalletRequest.newBuilder()
                .setPassphraseBytes(ByteString.copyFrom("passphrase".getBytes()))
                .build();

        when(addressManager.decryptWallet(ArgumentMatchers.any())).thenReturn(false);

        VeriBlockMessages.ProtocolReply reply = adminApiService.decryptWallet(request);

        verify(addressManager,  times(1)).decryptWallet(ArgumentMatchers.any());
        Assert.assertEquals(false, reply.getSuccess());
    }

    @Test
    public void decryptWalletWhenExceptionThenFalse() {
        VeriBlockMessages.DecryptWalletRequest request = VeriBlockMessages.DecryptWalletRequest.newBuilder()
                .setPassphraseBytes(ByteString.copyFrom("passphrase".getBytes()))
                .build();

        when(addressManager.decryptWallet(ArgumentMatchers.any())).thenThrow(new IllegalStateException());

        VeriBlockMessages.ProtocolReply reply = adminApiService.decryptWallet(request);

        verify(addressManager,  times(1)).decryptWallet(ArgumentMatchers.any());
        Assert.assertEquals(false, reply.getSuccess());
    }

    @Test
    public void decryptWalletWhenEncryptTrueThenTrue() {
        VeriBlockMessages.DecryptWalletRequest request = VeriBlockMessages.DecryptWalletRequest.newBuilder()
                .setPassphraseBytes(ByteString.copyFrom("passphrase".getBytes()))
                .build();

        when(addressManager.decryptWallet(ArgumentMatchers.any())).thenReturn(true);

        VeriBlockMessages.ProtocolReply reply = adminApiService.decryptWallet(request);

        verify(addressManager,  times(1)).decryptWallet(ArgumentMatchers.any());
        Assert.assertEquals(true, reply.getSuccess());
    }

    @Test
    public void lockWallet() {
        doNothing().when(addressManager).lock();

        VeriBlockMessages.ProtocolReply reply = adminApiService.lockWallet(VeriBlockMessages.LockWalletRequest.newBuilder().build());

        verify(addressManager,  times(1)).lock();
        Assert.assertEquals(true, reply.getSuccess());
    }

    @Test
    public void backupWalletWhenSaveWalletFalseThenFalse() {
        VeriBlockMessages.BackupWalletRequest request = VeriBlockMessages.BackupWalletRequest.newBuilder()
                .setTargetLocation(ByteString.copyFromUtf8("target/location/path"))
                .build();

        Pair<Boolean, String> saveWalletResult = new Pair<>(false, "Result");

        when(addressManager.saveWalletToFile(ArgumentMatchers.any())).thenReturn(saveWalletResult);

        VeriBlockMessages.BackupWalletReply reply = adminApiService.backupWallet(request);

        verify(addressManager,  times(1)).saveWalletToFile(ArgumentMatchers.any());
        Assert.assertEquals(false, reply.getSuccess());
    }

    @Test
    public void backupWalletWhenExceptionThenFalse() {
        VeriBlockMessages.BackupWalletRequest request = VeriBlockMessages.BackupWalletRequest.newBuilder()
                .setTargetLocation(ByteString.copyFromUtf8("target/location/path"))
                .build();

        when(addressManager.saveWalletToFile(ArgumentMatchers.any())).thenThrow(new RuntimeException());

        VeriBlockMessages.BackupWalletReply reply = adminApiService.backupWallet(request);

        verify(addressManager,  times(1)).saveWalletToFile(ArgumentMatchers.any());
        Assert.assertEquals(false, reply.getSuccess());
    }

    @Test
    public void backupWalletWhenSaveWalletTrueThenTrue() {
        VeriBlockMessages.BackupWalletRequest request = VeriBlockMessages.BackupWalletRequest.newBuilder()
                .setTargetLocation(ByteString.copyFromUtf8("target/location/path"))
                .build();

        Pair<Boolean, String> saveWalletResult = new Pair<>(true, "Result");

        when(addressManager.saveWalletToFile(ArgumentMatchers.any())).thenReturn(saveWalletResult);

        VeriBlockMessages.BackupWalletReply reply = adminApiService.backupWallet(request);

        verify(addressManager,  times(1)).saveWalletToFile(ArgumentMatchers.any());
        Assert.assertEquals(true, reply.getSuccess());
    }


    @Test
    public void dumpPrivateKeyWhenAddressNotValidThenFalse() {
        VeriBlockMessages.DumpPrivateKeyRequest request = VeriBlockMessages.DumpPrivateKeyRequest
                .newBuilder()
                .setAddress(ByteString.copyFromUtf8("Not valid address"))
                .build();

        VeriBlockMessages.DumpPrivateKeyReply reply = adminApiService.dumpPrivateKey(request);

        Assert.assertEquals(false, reply.getSuccess());
    }

    @Test
    public void dumpPrivateKeyWhenPublicKeyNullThenFalse() throws NoSuchAlgorithmException {
        String validAddress = "VAhGtBDm6hq3UVkTXwNgyrFhVEfR8J";
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        KeyPair keyPair = kpg.generateKeyPair();

        VeriBlockMessages.DumpPrivateKeyRequest request = VeriBlockMessages.DumpPrivateKeyRequest
                .newBuilder()
                .setAddress(ByteString.copyFrom(Base58.decode(validAddress)))
                .build();

        when(addressManager.getPublicKeyForAddress(ArgumentMatchers.any())).thenReturn(null);
        when(addressManager.getPrivateKeyForAddress(ArgumentMatchers.any())).thenReturn(keyPair.getPrivate());

        VeriBlockMessages.DumpPrivateKeyReply reply = adminApiService.dumpPrivateKey(request);

        verify(addressManager,  times(1)).getPublicKeyForAddress(validAddress);
        verify(addressManager,  times(1)).getPrivateKeyForAddress(validAddress);
        Assert.assertEquals(false, reply.getSuccess());
    }

    @Test
    public void dumpPrivateKeyWhenPrivateKeyNullThenFalse() throws NoSuchAlgorithmException {
        String validAddress = "VAhGtBDm6hq3UVkTXwNgyrFhVEfR8J";
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        KeyPair keyPair = kpg.generateKeyPair();

        VeriBlockMessages.DumpPrivateKeyRequest request = VeriBlockMessages.DumpPrivateKeyRequest
                .newBuilder()
                .setAddress(ByteString.copyFrom(Base58.decode(validAddress)))
                .build();

        when(addressManager.getPublicKeyForAddress(ArgumentMatchers.any())).thenReturn(keyPair.getPublic());
        when(addressManager.getPrivateKeyForAddress(ArgumentMatchers.any())).thenReturn(null);

        VeriBlockMessages.DumpPrivateKeyReply reply = adminApiService.dumpPrivateKey(request);

        verify(addressManager,  times(1)).getPublicKeyForAddress(validAddress);
        verify(addressManager,  times(1)).getPrivateKeyForAddress(validAddress);
        Assert.assertEquals(false, reply.getSuccess());
    }

    @Test
    public void dumpPrivateKeyWhenSuccess() throws NoSuchAlgorithmException {
        String validAddress = "VAhGtBDm6hq3UVkTXwNgyrFhVEfR8J";
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        KeyPair keyPair = kpg.generateKeyPair();

        VeriBlockMessages.DumpPrivateKeyRequest request = VeriBlockMessages.DumpPrivateKeyRequest
                .newBuilder()
                .setAddress(ByteString.copyFrom(Base58.decode(validAddress)))
                .build();

        when(addressManager.getPublicKeyForAddress(ArgumentMatchers.any())).thenReturn(keyPair.getPublic());
        when(addressManager.getPrivateKeyForAddress(ArgumentMatchers.any())).thenReturn(keyPair.getPrivate());

        VeriBlockMessages.DumpPrivateKeyReply reply = adminApiService.dumpPrivateKey(request);

        verify(addressManager,  times(1)).getPublicKeyForAddress(validAddress);
        verify(addressManager,  times(1)).getPrivateKeyForAddress(validAddress);
        Assert.assertEquals(true, reply.getSuccess());
    }

    @Test
    public void importPrivateKeyWhenWalletLockedThenFalse() {
        VeriBlockMessages.ImportPrivateKeyRequest request = VeriBlockMessages.ImportPrivateKeyRequest.newBuilder()
                .build();

        when(addressManager.isLocked()).thenReturn(true);

        VeriBlockMessages.ImportPrivateKeyReply reply = adminApiService.importPrivateKey(request);

        verify(addressManager,  times(1)).isLocked();
        Assert.assertEquals(false, reply.getSuccess());
    }

    @Test
    public void importPrivateKeyWhenAddressNullThenFalse() throws NoSuchAlgorithmException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        KeyPair keyPair = kpg.generateKeyPair();

        VeriBlockMessages.ImportPrivateKeyRequest request = VeriBlockMessages.ImportPrivateKeyRequest.newBuilder()
                .setPrivateKey(ByteString.copyFrom(keyPair.getPrivate().getEncoded()))
                .build();

        when(addressManager.isLocked()).thenReturn(false);
        when(addressManager.importKeyPair(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(null);

        VeriBlockMessages.ImportPrivateKeyReply reply = adminApiService.importPrivateKey(request);

        verify(addressManager,  times(1)).isLocked();
        verify(addressManager,  times(1)).importKeyPair(ArgumentMatchers.any(), ArgumentMatchers.any());
        Assert.assertEquals(false, reply.getSuccess());
    }

    @Test
    public void importPrivateKeyWhen() throws NoSuchAlgorithmException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        KeyPair keyPair = kpg.generateKeyPair();

        VeriBlockMessages.ImportPrivateKeyRequest request = VeriBlockMessages.ImportPrivateKeyRequest.newBuilder()
                .setPrivateKey(ByteString.copyFrom(keyPair.getPrivate().getEncoded()))
                .build();
        Address importedAddress = new Address("VcspPDtJNpNmLV8qFTqb2F5157JNHS", keyPair.getPublic());

        when(addressManager.isLocked()).thenReturn(false);
        when(addressManager.importKeyPair(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(importedAddress);

        VeriBlockMessages.ImportPrivateKeyReply reply = adminApiService.importPrivateKey(request);

        verify(addressManager,  times(1)).isLocked();
        verify(addressManager,  times(1)).importKeyPair(ArgumentMatchers.any(), ArgumentMatchers.any());
        Assert.assertEquals(true, reply.getSuccess());
    }

    @Test
    public void getNewAddressWhenWalletLockedThenFalse() {
        VeriBlockMessages.GetNewAddressRequest request = VeriBlockMessages.GetNewAddressRequest.newBuilder()
                .build();

        when(addressManager.isLocked()).thenReturn(true);

        VeriBlockMessages.GetNewAddressReply reply = adminApiService.getNewAddress(request);

        verify(addressManager,  times(1)).isLocked();
        Assert.assertEquals(false, reply.getSuccess());

    }

    @Test
    public void getNewAddressWhenGetNewAddressNullThenFalse() throws IOException {
        VeriBlockMessages.GetNewAddressRequest request = VeriBlockMessages.GetNewAddressRequest.newBuilder()
                .build();

        when(addressManager.isLocked()).thenReturn(false);
        when(addressManager.getNewAddress()).thenReturn(null);

        VeriBlockMessages.GetNewAddressReply reply = adminApiService.getNewAddress(request);

        verify(addressManager,  times(1)).isLocked();
        verify(addressManager,  times(1)).getNewAddress();
        Assert.assertEquals(false, reply.getSuccess());

    }

    @Test
    public void getNewAddressWhenIOExceptionThenFalse() throws Exception {
        VeriBlockMessages.GetNewAddressRequest request = VeriBlockMessages.GetNewAddressRequest.newBuilder()
                .build();

        when(addressManager.isLocked()).thenReturn(false);
        when(addressManager.getNewAddress()).thenThrow(new IOException());

        VeriBlockMessages.GetNewAddressReply reply = adminApiService.getNewAddress(request);

        verify(addressManager,  times(1)).isLocked();
        verify(addressManager,  times(1)).getNewAddress();
        Assert.assertEquals(false, reply.getSuccess());
    }

    @Test
    public void getNewAddressWhenWalletLockedExceptionThenFalse() throws Exception {
        VeriBlockMessages.GetNewAddressRequest request = VeriBlockMessages.GetNewAddressRequest.newBuilder()
                .build();

        when(addressManager.isLocked()).thenReturn(false);
        when(addressManager.getNewAddress()).thenThrow(new WalletLockedException("Exception"));

        VeriBlockMessages.GetNewAddressReply reply = adminApiService.getNewAddress(request);

        verify(addressManager,  times(1)).isLocked();
        verify(addressManager,  times(1)).getNewAddress();
        Assert.assertEquals(false, reply.getSuccess());
    }

    @Test
    public void getNewAddressWhenSuccessThenTrue() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        KeyPair keyPair = kpg.generateKeyPair();

        VeriBlockMessages.GetNewAddressRequest request = VeriBlockMessages.GetNewAddressRequest.newBuilder()
                .build();
        Address address = new Address("VcspPDtJNpNmLV8qFTqb2F5157JNHS", keyPair.getPublic());

        when(addressManager.isLocked()).thenReturn(false);
        when(addressManager.getNewAddress()).thenReturn(address);

        VeriBlockMessages.GetNewAddressReply reply = adminApiService.getNewAddress(request);

        verify(addressManager,  times(1)).isLocked();
        verify(addressManager,  times(1)).getNewAddress();
        Assert.assertEquals(true, reply.getSuccess());
    }

    @Test
    public void submitTransactionsWhenTxUnsignedThenFalse() {
        VeriBlockMessages.TransactionUnion transactionUnion = VeriBlockMessages.TransactionUnion.newBuilder()
                .setUnsigned(VeriBlockMessages.Transaction.newBuilder().build())
                .build();
        VeriBlockMessages.SubmitTransactionsRequest request = VeriBlockMessages.SubmitTransactionsRequest.newBuilder()
                .addTransactions(transactionUnion)
                .build();

        VeriBlockMessages.ProtocolReply reply = adminApiService.submitTransactions(request);

        Assert.assertEquals(false, reply.getSuccess());
    }

    @Test
    public void submitTransactionsWhenTxSignedButNotAddToContainerThenFalse() {
        VeriBlockMessages.SignedTransaction signTx = VeriBlockMessages.SignedTransaction.newBuilder().build();
        VeriBlockMessages.TransactionUnion transactionUnion = VeriBlockMessages.TransactionUnion.newBuilder()
                .setSigned(signTx)
                .build();
        VeriBlockMessages.SubmitTransactionsRequest request = VeriBlockMessages.SubmitTransactionsRequest.newBuilder()
                .addTransactions(transactionUnion)
                .build();

        when(transactionFactory.create(transactionUnion)).thenReturn(new StandardTransaction(Sha256Hash.ZERO_HASH));
        when(transactionContainer.addTransaction(ArgumentMatchers.any())).thenReturn(false);

        VeriBlockMessages.ProtocolReply reply = adminApiService.submitTransactions(request);

        verify(transactionFactory,  times(1)).create(signTx);
        verify(transactionContainer,  times(1)).addTransaction(ArgumentMatchers.any());
        Assert.assertEquals(false, reply.getSuccess());
    }


    @Test
    public void submitTransactionsWhenTxSignedThenTrue() {
        VeriBlockMessages.SignedTransaction signTx = VeriBlockMessages.SignedTransaction.newBuilder().build();
        VeriBlockMessages.TransactionUnion transactionUnion = VeriBlockMessages.TransactionUnion.newBuilder()
                .setSigned(signTx)
                .build();
        VeriBlockMessages.SubmitTransactionsRequest request = VeriBlockMessages.SubmitTransactionsRequest.newBuilder()
                .addTransactions(transactionUnion)
                .build();

        when(transactionFactory.create(transactionUnion)).thenReturn(new StandardTransaction(Sha256Hash.ZERO_HASH));
        when(transactionContainer.addTransaction(ArgumentMatchers.any())).thenReturn(true);

        VeriBlockMessages.ProtocolReply reply = adminApiService.submitTransactions(request);

        verify(transactionFactory,  times(1)).create(signTx);
        verify(transactionContainer,  times(1)).addTransaction(ArgumentMatchers.any());
        Assert.assertEquals(true, reply.getSuccess());
    }

    @Test
    public void submitTransactionsWhenTxMultiSigThenTrue() {
        VeriBlockMessages.SignedMultisigTransaction signTx = VeriBlockMessages.SignedMultisigTransaction.newBuilder().build();
        VeriBlockMessages.TransactionUnion transactionUnion = VeriBlockMessages.TransactionUnion.newBuilder()
                .setSignedMultisig(signTx)
                .build();
        VeriBlockMessages.SubmitTransactionsRequest request = VeriBlockMessages.SubmitTransactionsRequest.newBuilder()
                .addTransactions(transactionUnion)
                .build();

        when(transactionFactory.create(transactionUnion)).thenReturn(new StandardTransaction(Sha256Hash.ZERO_HASH));
        when(transactionContainer.addTransaction(ArgumentMatchers.any())).thenReturn(true);

        VeriBlockMessages.ProtocolReply reply = adminApiService.submitTransactions(request);

        verify(transactionFactory,  times(1)).create(signTx);
        verify(transactionContainer,  times(1)).addTransaction(ArgumentMatchers.any());
        Assert.assertEquals(true, reply.getSuccess());
    }


    @Test
    public void getSignatureIndexWhenSuccess() throws NoSuchAlgorithmException {

        VeriBlockMessages.GetSignatureIndexRequest request = VeriBlockMessages.GetSignatureIndexRequest.newBuilder()
                .build();
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        KeyPair keyPair = kpg.generateKeyPair();
        Address address = new Address("VcspPDtJNpNmLV8qFTqb2F5157JNHS", keyPair.getPublic());


        when(addressManager.getDefaultAddress()).thenReturn(address);
        when(peerTable.getSignatureIndex(ArgumentMatchers.any())).thenReturn(1L);
        when(transactionContainer.getPendingSignatureIndexForAddress(ArgumentMatchers.any())).thenReturn(2L);

        VeriBlockMessages.GetSignatureIndexReply reply = adminApiService.getSignatureIndex(request);

        verify(addressManager,  times(1)).getDefaultAddress();
        verify(peerTable,  times(1)).getSignatureIndex(ArgumentMatchers.any());
        verify(transactionContainer,  times(1)).getPendingSignatureIndexForAddress(ArgumentMatchers.any());
        Assert.assertEquals(true, reply.getSuccess());
    }

    @Test
    public void getSignatureIndexWhenReqHasAddressSuccess() throws NoSuchAlgorithmException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        KeyPair keyPair = kpg.generateKeyPair();
        Address address = new Address("VcspPDtJNpNmLV8qFTqb2F5157JNHS", keyPair.getPublic());

        VeriBlockMessages.GetSignatureIndexRequest request = VeriBlockMessages.GetSignatureIndexRequest.newBuilder()
                .addAddresses(ByteString.copyFrom(Base58.decode(address.getHash())))
                .build();

        when(peerTable.getSignatureIndex(ArgumentMatchers.any())).thenReturn(1L);
        when(transactionContainer.getPendingSignatureIndexForAddress(ArgumentMatchers.any())).thenReturn(2L);

        VeriBlockMessages.GetSignatureIndexReply reply = adminApiService.getSignatureIndex(request);

        verify(peerTable, times(1)).getSignatureIndex(ArgumentMatchers.any());
        verify(transactionContainer, times(1)).getPendingSignatureIndexForAddress(ArgumentMatchers.any());
        Assert.assertEquals(true, reply.getSuccess());
    }

    @Test
    public void createAltChainEndorsementWhenMaxFeeLess() {
        StandardTransaction transaction = new StandardTransaction(Sha256Hash.ZERO_HASH);
        transaction.setInputAddress(new StandardAddress("VcspPDtJNpNmLV8qFTqb2F5157JNHS"));
        transaction.setInputAmount(Coin.ONE);
        transaction.setData(new byte[12]);

        VeriBlockMessages.CreateAltChainEndorsementRequest request =
            VeriBlockMessages.CreateAltChainEndorsementRequest.newBuilder().setPublicationData(ByteString.copyFrom(new byte[12]))
                .setSourceAddress(ByteStringAddressUtility.createProperByteStringAutomatically("VcspPDtJNpNmLV8qFTqb2F5157JNHS"))
                .setFeePerByte(10_000L).setMaxFee(1_000L).build();

        when(transactionService
            .createUnsignedAltChainEndorsementTransaction(ArgumentMatchers.anyString(), ArgumentMatchers.anyLong(), ArgumentMatchers.any(),
                ArgumentMatchers.anyLong()
            )).thenReturn(transaction);

        VeriBlockMessages.CreateAltChainEndorsementReply reply = adminApiService.createAltChainEndorsement(request);

        Assert.assertNotNull(reply);
        Assert.assertFalse(reply.getSuccess());
        Assert.assertFalse(reply.getResults(0).getMessage().contains("Calcualated fee"));
    }

    @Test
    public void createAltChainEndorsementWhenThrowException() {
        StandardTransaction transaction = new StandardTransaction(Sha256Hash.ZERO_HASH);
        transaction.setInputAddress(new StandardAddress("VcspPDtJNpNmLV8qFTqb2F5157JNHS"));
        transaction.setInputAmount(Coin.ONE);
        transaction.setData(new byte[12]);

        VeriBlockMessages.CreateAltChainEndorsementRequest request =
            VeriBlockMessages.CreateAltChainEndorsementRequest.newBuilder().setPublicationData(ByteString.copyFrom(new byte[12]))
                .setSourceAddress(ByteStringAddressUtility.createProperByteStringAutomatically("VcspPDtJNpNmLV8qFTqb2F5157JNHS"))
                .setFeePerByte(10_000L).setMaxFee(100_000_000L).build();

        when(transactionService
            .createUnsignedAltChainEndorsementTransaction(ArgumentMatchers.anyString(), ArgumentMatchers.anyLong(), ArgumentMatchers.any(),
                ArgumentMatchers.anyLong()
            )).thenThrow(new RuntimeException());

        VeriBlockMessages.CreateAltChainEndorsementReply reply = adminApiService.createAltChainEndorsement(request);

        Assert.assertNotNull(reply);
        Assert.assertFalse(reply.getSuccess());
        Assert.assertFalse(reply.getResults(0).getMessage().contains("An error occurred processing"));
    }

    @Test
    public void createAltChainEndorsement() {
        StandardTransaction transaction = new StandardTransaction(Sha256Hash.ZERO_HASH);
        transaction.setInputAddress(new StandardAddress("VcspPDtJNpNmLV8qFTqb2F5157JNHS"));
        transaction.setInputAmount(Coin.ONE);
        transaction.setData(new byte[12]);

        VeriBlockMessages.CreateAltChainEndorsementRequest request =
            VeriBlockMessages.CreateAltChainEndorsementRequest.newBuilder().setPublicationData(ByteString.copyFrom(new byte[12]))
                .setSourceAddress(ByteStringAddressUtility.createProperByteStringAutomatically("VcspPDtJNpNmLV8qFTqb2F5157JNHS"))
                .setFeePerByte(10_000L).setMaxFee(100_000_000L).build();

        when(transactionService
            .createUnsignedAltChainEndorsementTransaction(ArgumentMatchers.anyString(), ArgumentMatchers.anyLong(), ArgumentMatchers.any(),
                ArgumentMatchers.anyLong()
            )).thenReturn(transaction);

        VeriBlockMessages.CreateAltChainEndorsementReply reply = adminApiService.createAltChainEndorsement(request);

        Assert.assertNotNull(reply);
        Assert.assertTrue(reply.getSuccess());
    }
}
