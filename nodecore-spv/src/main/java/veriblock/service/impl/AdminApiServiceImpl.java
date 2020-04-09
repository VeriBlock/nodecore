// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package veriblock.service.impl;

import com.google.protobuf.ByteString;
import nodecore.api.grpc.VeriBlockMessages;
import nodecore.api.grpc.utilities.ByteStringAddressUtility;
import nodecore.api.grpc.utilities.ByteStringUtility;
import nodecore.p2p.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veriblock.core.bitcoinj.Base58;
import org.veriblock.core.contracts.AddressManager;
import org.veriblock.core.types.Pair;
import org.veriblock.core.utilities.AddressUtility;
import org.veriblock.core.utilities.Utility;
import org.veriblock.core.wallet.Address;
import org.veriblock.core.wallet.WalletLockedException;
import org.veriblock.sdk.models.BitcoinBlock;
import org.veriblock.sdk.models.Coin;
import org.veriblock.sdk.models.Sha256Hash;
import veriblock.SpvContext;
import veriblock.model.AddressCoinsIndex;
import veriblock.model.LedgerContext;
import veriblock.model.Output;
import veriblock.model.StandardAddress;
import veriblock.model.StandardTransaction;
import veriblock.model.Transaction;
import veriblock.net.PeerTable;
import veriblock.service.AdminApiService;
import veriblock.service.PendingTransactionContainer;
import veriblock.service.TransactionFactory;
import veriblock.util.MessageIdGenerator;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class AdminApiServiceImpl implements AdminApiService {
    private static final Logger logger = LoggerFactory.getLogger(AdminApiServiceImpl.class);

    private final SpvContext spvContext;
    private final PeerTable peerTable;
    private final TransactionService transactionService;
    private final AddressManager addressManager;
    private final TransactionFactory transactionFactory;
    private final PendingTransactionContainer pendingTransactionContainer;
    private final Blockchain blockchain;

    public AdminApiServiceImpl(
        SpvContext spvContext,
        PeerTable peerTable,
        TransactionService transactionService,
        AddressManager addressManager,
        TransactionFactory transactionFactory,
        PendingTransactionContainer pendingTransactionContainer,
        Blockchain blockchain
    ) {
        this.spvContext = spvContext;
        this.peerTable = peerTable;
        this.transactionService = transactionService;
        this.addressManager = addressManager;
        this.transactionFactory = transactionFactory;
        this.pendingTransactionContainer = pendingTransactionContainer;
        this.blockchain = blockchain;
    }

    public VeriBlockMessages.GetStateInfoReply getStateInfo(VeriBlockMessages.GetStateInfoRequest request) {
        //TODO do real statuses.
        int blockchainStateValue = VeriBlockMessages.BlockchainStateInfo.State.LOADED_VALUE;
        int operatingStateValue = VeriBlockMessages.OperatingStateInfo.State.STARTED_VALUE;
        int networkStateValue = VeriBlockMessages.NetworkStateInfo.State.CONNECTED_VALUE;

        VeriBlockMessages.GetStateInfoReply.Builder replyBuilder = VeriBlockMessages.GetStateInfoReply.newBuilder();
        replyBuilder.setBlockchainState(VeriBlockMessages.BlockchainStateInfo.newBuilder().setStateValue(blockchainStateValue));
        replyBuilder.setOperatingState(VeriBlockMessages.OperatingStateInfo.newBuilder().setStateValue(operatingStateValue));
        replyBuilder.setNetworkState(VeriBlockMessages.NetworkStateInfo.newBuilder().setStateValue(networkStateValue));

        replyBuilder.setConnectedPeerCount(peerTable.getAvailablePeers());
        replyBuilder.setNetworkHeight(peerTable.getBestBlockHeight());
        replyBuilder.setLocalBlockchainHeight(blockchain.getChainHead().getHeight());

        replyBuilder.setNetworkVersion(spvContext.getNetworkParameters().getNetworkName());
        replyBuilder.setDataDirectory(spvContext.getDirectory().getPath());
        replyBuilder.setProgramVersion(Constants.PROGRAM_VERSION == null ? "UNKNOWN" : Constants.PROGRAM_VERSION);
        replyBuilder.setNodecoreStarttime(spvContext.getStartTime().getEpochSecond());
        //TODO does it need for spv?
        replyBuilder.setWalletCacheSyncHeight(blockchain.getChainHead().getHeight());
        if (!addressManager.isEncrypted()) {
            replyBuilder.setWalletState(VeriBlockMessages.GetStateInfoReply.WalletState.DEFAULT);
        } else {
            if (addressManager.isLocked()) {
                replyBuilder.setWalletState(VeriBlockMessages.GetStateInfoReply.WalletState.LOCKED);
            } else {
                replyBuilder.setWalletState(VeriBlockMessages.GetStateInfoReply.WalletState.UNLOCKED);
            }
        }
        replyBuilder.setSuccess(true);

        return replyBuilder.build();
    }


    public VeriBlockMessages.SendCoinsReply sendCoins(VeriBlockMessages.SendCoinsRequest request) {
        VeriBlockMessages.SendCoinsReply.Builder replyBuilder = VeriBlockMessages.SendCoinsReply.newBuilder();
        ByteString sourceAddress = request.getSourceAddress();
        List<AddressCoinsIndex> addressCoinsIndexList = new ArrayList<>();

        ArrayList<Output> outputList = new ArrayList<>();
        for (VeriBlockMessages.Output output : request.getAmountsList()) {
            String address = ByteStringAddressUtility.parseProperAddressTypeAutomatically(output.getAddress());
            outputList.add(new Output(new StandardAddress(address), Coin.valueOf(output.getAmount())));
        }
        long totalOutputAmount = outputList.stream()
            .map(o -> o.getAmount().getAtomicUnits())
            .reduce(0L, Long::sum);

        if (sourceAddress.isEmpty()) {
            for (Pair<String, Long> availableAddress : getAvailableAddresses(totalOutputAmount)) {
                addressCoinsIndexList.add(new AddressCoinsIndex(availableAddress.getFirst(), availableAddress.getSecond(),
                    getSignatureIndex(availableAddress.getFirst())
                ));
            }
        } else {
            String address = ByteStringAddressUtility.parseProperAddressTypeAutomatically(request.getSourceAddress());
            LedgerContext ledgerContext = peerTable.getAddressState(address);

            if (ledgerContext == null) {
                replyBuilder.setSuccess(false);
                replyBuilder.addResults(makeResult("V008", "Information about this address does not exist.",
                    "Perhaps your node is waiting for this information. Try to do it later.", true
                ));
                return replyBuilder.build();
            } else if (!ledgerContext.getLedgerProofStatus().isExists()) {
                replyBuilder.setSuccess(false);
                replyBuilder
                    .addResults(makeResult("V008", "Address doesn't exist or invalid.", "Check your address that you use for this operation.", true));
                return replyBuilder.build();
            }

            addressCoinsIndexList.add(new AddressCoinsIndex(address, ledgerContext.getLedgerValue().getAvailableAtomicUnits(),
                getSignatureIndex(address)
            ));
        }

        long totalAvailableBalance = addressCoinsIndexList.stream()
            .map(AddressCoinsIndex::getCoins)
            .reduce(0L, Long::sum);

        if (totalOutputAmount > totalAvailableBalance) {
            replyBuilder.setSuccess(false);
            replyBuilder
                .addResults(makeResult("V008", "Available balance is not enough.", "Check your address that you use for this operation.", true));
            return replyBuilder.build();
        }

        List<Transaction> transactions = transactionService.createTransactionsByOutputList(addressCoinsIndexList, outputList);

        if (transactions.size() == 0) {
            replyBuilder.setSuccess(false);
            replyBuilder
                .addResults(
                    makeResult("V008", "Transaction has not been created, there is an exception.", "Check your address balance and try later.", true));
            return replyBuilder.build();
        }

        for (Transaction transaction : transactions) {
            pendingTransactionContainer.addTransaction(transaction);
            peerTable.advertise(transaction);
            replyBuilder.addTxIds(ByteStringUtility.hexToByteString(transaction.getTxId().toString()));
        }

        replyBuilder.setSuccess(true);
        return replyBuilder.build();
    }



    @Override
    public VeriBlockMessages.GetSignatureIndexReply getSignatureIndex(VeriBlockMessages.GetSignatureIndexRequest request) {
        VeriBlockMessages.GetSignatureIndexReply.Builder replyBuilder = VeriBlockMessages.GetSignatureIndexReply.newBuilder();
        replyBuilder.setSuccess(true);

        ArrayList<String> addresses = new ArrayList<>();
        if (request.getAddressesCount() > 0) {
            for (ByteString value : request.getAddressesList()) {
                addresses.add(ByteStringAddressUtility.parseProperAddressTypeAutomatically(value));
            }
        } else {
            addresses.add(addressManager.getDefaultAddress().getHash());
        }

        for (String address : addresses) {
            byte[] addressBytes;
            if (AddressUtility.isValidMultisigAddress(address)) {
                addressBytes = Utility.base59ToBytes(address);
            } else {
                addressBytes = Utility.base58ToBytes(address);
            }
            long blockchainLastSignatureIndex = peerTable.getSignatureIndex(address);
            long poolLastSignatureIndex = getSignatureIndex(address);
            replyBuilder.addIndexes(VeriBlockMessages.AddressSignatureIndexes
                    .newBuilder()
                    .setAddress(ByteString.copyFrom(addressBytes))
                    .setBlockchainIndex(blockchainLastSignatureIndex)
                    .setPoolIndex(poolLastSignatureIndex));
        }

        return replyBuilder.build();
    }

    @Override
    public VeriBlockMessages.ProtocolReply submitTransactions(VeriBlockMessages.SubmitTransactionsRequest request) {
        VeriBlockMessages.ProtocolReply.Builder replyBuilder = VeriBlockMessages.ProtocolReply.newBuilder();
        replyBuilder.setSuccess(true);

        for (VeriBlockMessages.TransactionUnion union : request.getTransactionsList()) {
            boolean added = false;
            switch (union.getTransactionCase()) {
                case UNSIGNED:
                    replyBuilder.setSuccess(false);
                    replyBuilder.addResults(makeResult("V008",
                            "Transaction is unsigned!",
                            "Unsigned transactions cannot be submitted to the network without signing first",
                            true));
                    break;
                case SIGNED:
                    Transaction t = transactionFactory.create(union.getSigned());
                    peerTable.advertise(t);
                    added = pendingTransactionContainer.addTransaction(t);
                    break;
                case SIGNED_MULTISIG:
                    Transaction multisigTransaction = transactionFactory.create(union.getSignedMultisig());
                    peerTable.advertise(multisigTransaction);
                    added = pendingTransactionContainer.addTransaction(multisigTransaction);
                    break;
                case TRANSACTION_NOT_SET:
                    replyBuilder.setSuccess(false);
                    replyBuilder.addResults(makeResult(
                            "V008",
                            "Invalid transaction type",
                            "Either a signed or unsigned transaction should be passed",
                            true));
                    break;
            }
            if (!added) {
                replyBuilder.setSuccess(false);
                replyBuilder.addResults(makeResult(
                        "V008",
                        "Submit transaction error",
                        "The transaction was not added to the pool",
                        true));
            }
        }

        return replyBuilder.build();
    }

    @Override
    public VeriBlockMessages.DumpPrivateKeyReply dumpPrivateKey(VeriBlockMessages.DumpPrivateKeyRequest request) {
        VeriBlockMessages.DumpPrivateKeyReply.Builder replyBuilder = VeriBlockMessages.DumpPrivateKeyReply.newBuilder();

        if (!ByteStringAddressUtility.isByteStringValidAddress(request.getAddress())) {
            replyBuilder.setSuccess(false);
            replyBuilder.addResults(makeResult("V008", "Invalid Address",
                    "The provided address is not a valid address", true));
            return replyBuilder.build();
        }

        String address = ByteStringAddressUtility.parseProperAddressTypeAutomatically(request.getAddress());

        PublicKey publicKey = addressManager.getPublicKeyForAddress(address);
        PrivateKey privateKey;
        try {
            privateKey = addressManager.getPrivateKeyForAddress(address);
        } catch (IllegalStateException e) {
            replyBuilder.setSuccess(false);
            replyBuilder.addResults(makeResult("V008", "Wallet Locked", e.getMessage(), true));
            return replyBuilder.build();
        }

        if (privateKey == null || publicKey == null) {
            replyBuilder.setSuccess(false);
            replyBuilder.addResults(makeResult("V008",
                    "The provided address is not an address in this wallet!",
                    address + " is not a valid address!", true));
            return replyBuilder.build();
        }

        byte[] privateKeyBytes = privateKey.getEncoded();
        byte[] publicKeyBytes = publicKey.getEncoded();

        byte[] fullBytes = new byte[privateKeyBytes.length + publicKeyBytes.length + 1];
        fullBytes[0] = (byte) privateKeyBytes.length;
        System.arraycopy(privateKeyBytes, 0, fullBytes, 1, privateKeyBytes.length);
        System.arraycopy(publicKeyBytes, 0, fullBytes, 1 + privateKeyBytes.length, publicKeyBytes.length);

        replyBuilder.setAddress(ByteString.copyFrom(Base58.decode(address)));
        replyBuilder.setPrivateKey(ByteString.copyFrom(fullBytes));
        replyBuilder.setSuccess(true);
        return replyBuilder.build();
    }

    @Override
    public VeriBlockMessages.ImportPrivateKeyReply importPrivateKey(VeriBlockMessages.ImportPrivateKeyRequest request) {
        VeriBlockMessages.ImportPrivateKeyReply.Builder replyBuilder = VeriBlockMessages.ImportPrivateKeyReply.newBuilder();

        if (addressManager.isLocked()) {
            replyBuilder.setSuccess(false);
            replyBuilder.addResults(makeResult("V045", "Import Failed", "Wallet must be unlocked before importing a key", true));
            return replyBuilder.build();
        }

        // The "Private" key is really the public and private key together encoded
        byte[] fullBytes = request.getPrivateKey().toByteArray();
        byte[] privateKeyBytes = new byte[fullBytes[0]];
        byte[] publicKeyBytes = new byte[fullBytes.length - fullBytes[0] - 1];

        System.arraycopy(fullBytes, 1, privateKeyBytes, 0, privateKeyBytes.length);
        System.arraycopy(fullBytes, privateKeyBytes.length + 1, publicKeyBytes, 0, publicKeyBytes.length);

        Address importedAddress = addressManager.importKeyPair(publicKeyBytes, privateKeyBytes);

        if (importedAddress == null) {
            replyBuilder.setSuccess(false);
            replyBuilder.addResults(makeResult("V008", "The provided private key was invalid or corrupted!", Utility.bytesToHex(fullBytes) + " is  not a valid private key!", true));
            return replyBuilder.build();
        }

        replyBuilder.setResultantAddress(ByteStringAddressUtility.createProperByteStringAutomatically(importedAddress.getHash()));
        replyBuilder.setSuccess(true);
        return replyBuilder.build();
    }

    @Override
    public VeriBlockMessages.ProtocolReply encryptWallet(VeriBlockMessages.EncryptWalletRequest request) {
        VeriBlockMessages.ProtocolReply.Builder replyBuilder = VeriBlockMessages.ProtocolReply.newBuilder();

        if (request.getPassphraseBytes().size() <= 0) {
            replyBuilder.setSuccess(false);
            replyBuilder.addResults(makeResult("V041", "Invalid passphrase",
                    "A blank passphrase is invalid for encrypting a wallet", true));
        } else {
            char[] passphrase = request.getPassphrase().toCharArray();

            try {
                boolean result = addressManager.encryptWallet(passphrase);
                if (result) {
                    replyBuilder.setSuccess(true);
                    replyBuilder.addResults(makeResult("V200", "Success",
                            "Wallet has been encrypted with supplied passphrase", false));
                } else {
                    replyBuilder.setSuccess(false);
                    replyBuilder.addResults(makeResult("V043", "Encryption Failed",
                            "Unable to encrypt wallet, see NodeCore logs", true));
                }
            } catch (IllegalStateException e) {
                replyBuilder.setSuccess(false);
                replyBuilder.addResults(makeResult("V042", "Wallet already encrypted",
                        "Wallet is already encrypted and must be decrypted first", true));
            }
        }

        return replyBuilder.build();
    }

    @Override
    public VeriBlockMessages.ProtocolReply decryptWallet(VeriBlockMessages.DecryptWalletRequest request) {
        VeriBlockMessages.ProtocolReply.Builder replyBuilder = VeriBlockMessages.ProtocolReply.newBuilder();

        if (request.getPassphraseBytes().size() <= 0) {
            replyBuilder.setSuccess(false);
            replyBuilder.addResults(makeResult("V041", "Invalid passphrase",
                    "A blank passphrase is invalid for decrypting a wallet", true));
        } else {
            char[] passphrase = request.getPassphrase().toCharArray();

            try {
                boolean result = addressManager.decryptWallet(passphrase);
                if (result) {
                    replyBuilder.setSuccess(true);
                    replyBuilder.addResults(makeResult("V200", "Success",
                            "Wallet has been decrypted", false));
                } else {
                    replyBuilder.setSuccess(false);
                    replyBuilder.addResults(makeResult("V044", "Decryption Failed",
                            "Unable to decrypt wallet, see NodeCore logs", true));
                }
            } catch (IllegalStateException e) {
                replyBuilder.setSuccess(false);
                replyBuilder.addResults(makeResult("V042", "Wallet already encrypted",
                        "Wallet is already encrypted and must be decrypted first", true));
            }
        }

        return replyBuilder.build();
    }

    @Override
    public VeriBlockMessages.ProtocolReply unlockWallet(VeriBlockMessages.UnlockWalletRequest request) {
        VeriBlockMessages.ProtocolReply.Builder replyBuilder = VeriBlockMessages.ProtocolReply.newBuilder();

        if (request.getPassphraseBytes().size() <= 0) {
            replyBuilder.setSuccess(false);
            replyBuilder.addResults(makeResult("V041", "Invalid passphrase",
                    "Passphrase is invalid for unlocking this wallet", true));
        } else {
            char[] passphrase = request.getPassphrase().toCharArray();

            boolean result = addressManager.unlock(passphrase);
            if (result) {
                replyBuilder.setSuccess(true);
                replyBuilder.addResults(makeResult("V200", "Success",
                        "Wallet has been unlocked", false));
            } else {
                replyBuilder.setSuccess(false);
                replyBuilder.addResults(makeResult("V041", "Invalid passphrase",
                        "Passphrase is invalid for unlocking this wallet", true));
            }
        }

        return replyBuilder.build();
    }

    @Override
    public VeriBlockMessages.ProtocolReply lockWallet(VeriBlockMessages.LockWalletRequest request) {
        VeriBlockMessages.ProtocolReply.Builder replyBuilder = VeriBlockMessages.ProtocolReply.newBuilder();

        addressManager.lock();
        replyBuilder.setSuccess(true);
        replyBuilder.addResults(makeResult("V200", "Success",
                "Wallet has been locked", false));

        return replyBuilder.build();
    }

    @Override
    public VeriBlockMessages.BackupWalletReply backupWallet(VeriBlockMessages.BackupWalletRequest request) {
        VeriBlockMessages.BackupWalletReply.Builder replyBuilder = VeriBlockMessages.BackupWalletReply.newBuilder();

        try {
            String backupLocation = new String(request.getTargetLocation().toByteArray());
            Pair<Boolean, String> saveWalletResult = addressManager.saveWalletToFile(backupLocation);
            boolean success = saveWalletResult.getFirst();

            if (!success) {
                replyBuilder.setSuccess(false);
                replyBuilder.addResults(makeResult(
                        "V008",
                        "Unable to save backup wallet file!",
                        saveWalletResult.getSecond(),
                        true));
            } else {
                replyBuilder.setSuccess(true);
            }
        } catch (Exception e) {
            replyBuilder.setSuccess(false);
            replyBuilder.addResults(makeResult(
                    "V008",
                    "Writing wallet backup failed!",
                    "The following error occurred while writing the backup files: " + e.getMessage() + ".",
                    true));
        }

        return replyBuilder.build();
    }

    @Override
    public VeriBlockMessages.ImportWalletReply importWallet(VeriBlockMessages.ImportWalletRequest request) {
        VeriBlockMessages.ImportWalletReply.Builder replyBuilder = VeriBlockMessages.ImportWalletReply.newBuilder();

        if (addressManager.isLocked()) {
            replyBuilder.setSuccess(false);
            replyBuilder.addResults(makeResult("V045", "Import Failed",
                    "Wallet must be unlocked before importing another wallet", true));
            return replyBuilder.build();
        }

        try {
            String importFromLocation = new String(request.getSourceLocation().toByteArray(), StandardCharsets.UTF_8);
            String passphrase = request.getPassphrase();

            Pair<Boolean, String> result;
            if (passphrase != null && passphrase.length() > 0) {
                result = addressManager.importEncryptedWallet(new File(importFromLocation), passphrase.toCharArray());
            } else {
                result = addressManager.importWallet(new File(importFromLocation));
            }
            boolean success = result.getFirst();

            if (!success) {
                replyBuilder.setSuccess(false);
                replyBuilder.addResults(makeResult(
                        "V008",
                        "Unable to load/import wallet file!",
                        result.getSecond(),
                        true));
            } else {
                replyBuilder.setSuccess(true);
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            replyBuilder.setSuccess(false);
            replyBuilder.addResults(makeResult(
                    "V008",
                    "Reading wallet file failed!",
                    "The following error occurred while reading the wallet file: " + e.getMessage() + ".",
                    true));
        }

        return replyBuilder.build();
    }

    @Override
    public VeriBlockMessages.GetNewAddressReply getNewAddress(VeriBlockMessages.GetNewAddressRequest request) {
        VeriBlockMessages.GetNewAddressReply.Builder replyBuilder = VeriBlockMessages.GetNewAddressReply.newBuilder();
        replyBuilder.setSuccess(true);

        if (addressManager.isLocked()) {
            replyBuilder.setSuccess(false);
            replyBuilder.addResults(makeResult("V008", "Address Creation Failed", "Wallet must be unlocked before creating a new address", true));
        } else {
            try {
                int count = request.getCount();
                if (count < 1) {
                    count = 1;
                }

                boolean success = true;
                List<Address> addresses = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    Address address = addressManager.getNewAddress();
                    if (address != null) {
                        addresses.add(address);
                        logger.info("New address: {}", address.getHash());
                    } else {
                        success = false;
                        replyBuilder.setSuccess(false);
                        replyBuilder.addResults(makeResult("V008", "Address Creation Failed", "Unable to generate new address", true));
                        break;
                    }
                }

                if (success) {
                    if (addresses.size() > 0) {
                        // New addresses from the normal address manager will always be standard addresses
                        replyBuilder.setAddress(ByteStringUtility.base58ToByteString(addresses.get(0).getHash()));
                    }
                    if (addresses.size() > 1) {
                        addresses.subList(1, addresses.size())
                                .forEach(a -> replyBuilder.addAdditionalAddresses(ByteStringUtility.base58ToByteString(a.getHash())));
                    }

                    replyBuilder.addResults(makeResult("V200", "Wallet Updated", "The wallet has been modified. Please make a backup of the wallet data file.", false));
                }
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
                replyBuilder.setSuccess(false);
                replyBuilder.addResults(makeResult("V008", "Address Creation Failed", "Unable to generate new address", true));
            } catch (WalletLockedException e) {
                logger.warn(e.getMessage());
                replyBuilder.setSuccess(false);
                replyBuilder.addResults(makeResult("V008", "Address Creation Failed", "Wallet must be unlocked before creating a new address", true));
            }
        }

        return replyBuilder.build();
    }


    @Override
    public VeriBlockMessages.GetBalanceReply getBalance(VeriBlockMessages.GetBalanceRequest request) {
        VeriBlockMessages.GetBalanceReply.Builder replyBuilder = VeriBlockMessages.GetBalanceReply.newBuilder();
        // All of the addresses from the normal address manager will be standard
        Map<String, LedgerContext> addressLedgerContext = peerTable.getAddressesState();

        if (request.getAddressesCount() == 0) {
            for (String address : addressLedgerContext.keySet()) {
                LedgerContext ledgerContext = addressLedgerContext.get(address);
                formGetBalanceReply(address, ledgerContext, replyBuilder);
            }
        } else {
            for (ByteString address : request.getAddressesList()) {
                String addressString = ByteStringAddressUtility.parseProperAddressTypeAutomatically(address);
                LedgerContext ledgerContext = addressLedgerContext.get(addressString);
                if (ledgerContext != null) {
                    formGetBalanceReply(addressString, ledgerContext, replyBuilder);
                } else {
                    addressManager.monitor(new Address(addressString, null));
                    replyBuilder.addConfirmed(VeriBlockMessages.AddressBalance
                        .newBuilder()
                        .setAddress(address)
                        .setLockedAmount(0L)
                        .setUnlockedAmount(0L)
                        .setTotalAmount(0L));
                    replyBuilder.addUnconfirmed(VeriBlockMessages.Output
                        .newBuilder()
                        .setAddress(address)
                        .setAmount(0L));
                }
            }
        }

        replyBuilder.setSuccess(true);

        return replyBuilder.build();
    }

    @Override
    public VeriBlockMessages.CreateAltChainEndorsementReply createAltChainEndorsement(VeriBlockMessages.CreateAltChainEndorsementRequest request) {
        VeriBlockMessages.CreateAltChainEndorsementReply.Builder replyBuilder = VeriBlockMessages.CreateAltChainEndorsementReply.newBuilder();
        try {
            byte[] publicationData = request.getPublicationData().toByteArray();
            String sourceAddress = ByteStringAddressUtility.parseProperAddressTypeAutomatically(request.getSourceAddress());
            long signatureIndex = getSignatureIndex(sourceAddress) + 1;
            long fee = request.getFeePerByte() * transactionService.predictAltChainEndorsementTransactionSize(publicationData.length, signatureIndex);

            if (fee > request.getMaxFee()) {
                replyBuilder.setSuccess(false);
                replyBuilder.addResults(makeResult("V008", "Create Alt Endorsement Error",
                    "Calcualated fee (" + fee + ") was above the maximum configured amount (" + request.getMaxFee() + ").", true
                ));
                return replyBuilder.build();
            }

            Transaction tx = transactionService.createUnsignedAltChainEndorsementTransaction(sourceAddress, fee, publicationData, signatureIndex);

            replyBuilder.setSuccess(true);
            replyBuilder.setTransaction(TransactionService.getRegularTransactionMessageBuilder((StandardTransaction) tx));
            replyBuilder.setSignatureIndex(signatureIndex);
        } catch (Exception e) {
            logger.error("Unable to create alt chain endorsement", e);
            replyBuilder.setSuccess(false);
            replyBuilder.addResults(makeResult("V008", "Create Alt Endorsement Error", "An error occurred processing request", true));
        }
        return replyBuilder.build();
    }

    @Override
    public VeriBlockMessages.GetLastBitcoinBlockReply getLastBitcoinBlock(VeriBlockMessages.GetLastBitcoinBlockRequest request) {
        VeriBlockMessages.GetLastBitcoinBlockReply.Builder replyBuilder = VeriBlockMessages.GetLastBitcoinBlockReply.newBuilder();
        replyBuilder.setSuccess(true);

        //Mock todo SPV-111
        BitcoinBlock block = spvContext.getNetworkParameters().getBitcoinOriginBlock();

        replyBuilder.setHash(ByteString.copyFrom(block.getHash().getBytes()));
        return replyBuilder.build();
    }

    @Override
    public VeriBlockMessages.GetTransactionsReply getTransactions(VeriBlockMessages.GetTransactionsRequest request) {
        List<Sha256Hash> ids = request.getIdsList()
            .stream()
            .map(id -> Sha256Hash.wrap(id.toByteArray()))
            .collect(Collectors.toList());

        List<VeriBlockMessages.TransactionInfo> replyList = ids.stream()
            .map(pendingTransactionContainer::getTransactionInfo)
            .collect(Collectors.toList());

        return VeriBlockMessages.GetTransactionsReply.newBuilder()
            .addAllTransactions(replyList)
            .build();
    }

    @Override
    public VeriBlockMessages.GetVeriBlockPublicationsReply getVeriBlockPublications(VeriBlockMessages.GetVeriBlockPublicationsRequest getVeriBlockPublicationsRequest) {
        VeriBlockMessages.Event advertise = VeriBlockMessages.Event.newBuilder()
            .setId(MessageIdGenerator.next())
            .setAcknowledge(false)
            .setVeriblockPublicationsRequest(getVeriBlockPublicationsRequest)
            .build();

        Future<VeriBlockMessages.Event> futureEventReply = peerTable.advertiseWithReply(advertise);

        try {
            return futureEventReply.get().getVeriblockPublicationsReply();
        } catch (InterruptedException | ExecutionException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    private List<Pair<String, Long>> getAvailableAddresses(long totalOutputAmount) {
        List<Pair<String, Long>> addressCoinsForPayment = new ArrayList<>();

        //Use default address if there balance is enough.
        LedgerContext ledgerContext = peerTable.getAddressState(addressManager.getDefaultAddress().getHash());
        if (ledgerContext.getLedgerValue().getAvailableAtomicUnits() > totalOutputAmount) {
            addressCoinsForPayment.add(new Pair<>(ledgerContext.getAddress().getAddress(), ledgerContext.getLedgerValue().getAvailableAtomicUnits()));

            return addressCoinsForPayment;
        }

        List<Pair<String, Long>> addressBalanceList = new ArrayList<>();
        Map<String, LedgerContext> ledgerContextMap = peerTable.getAddressesState();

        for (Address address : addressManager.getAll()) {
            if (ledgerContextMap.containsKey(address.getHash()) && ledgerContextMap.get(address.getHash()).getLedgerValue() != null) {
                addressBalanceList
                    .add(new Pair<>(address.getHash(), ledgerContextMap.get(address.getHash()).getLedgerValue().getAvailableAtomicUnits()));
            }
        }

        return addressBalanceList.stream()
            .filter(b -> b.getSecond() > 0)
            .sorted((b1, b2) -> Long.compare(b2.getSecond(), b1.getSecond()))
            .collect(Collectors.toList());
    }

    private void formGetBalanceReply(String address, LedgerContext ledgerContext, VeriBlockMessages.GetBalanceReply.Builder replyBuilder) {
        long balance = 0L;
        long lockedCoins = 0L;

        if (ledgerContext != null && ledgerContext.getLedgerValue() != null) {
            balance = ledgerContext.getLedgerValue().getAvailableAtomicUnits();
            lockedCoins = ledgerContext.getLedgerValue().getFrozenAtomicUnits();
        }

        replyBuilder.addConfirmed(VeriBlockMessages.AddressBalance
            .newBuilder()
            .setAddress(ByteStringUtility.base58ToByteString(address))
            .setLockedAmount(lockedCoins)
            .setUnlockedAmount(balance - lockedCoins)
            .setTotalAmount(balance));

        replyBuilder.addUnconfirmed(VeriBlockMessages.Output
            .newBuilder()
            .setAddress(ByteStringUtility.base58ToByteString(address))
            .setAmount(0L));
    }

    private Long getSignatureIndex(String address){
        Long signatureIndex = pendingTransactionContainer.getPendingSignatureIndexForAddress(address);

        if(signatureIndex == null){
            return peerTable.getSignatureIndex(address);
        }
        return signatureIndex;
    }

    private VeriBlockMessages.Result makeResult(
            String code,
            String message,
            String details,
            boolean error) {
        return VeriBlockMessages.Result
                .newBuilder()
                .setCode(code)
                .setMessage(message)
                .setDetails(details)
                .setError(error)
                .build();
    }

}
