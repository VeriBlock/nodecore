// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package veriblock.service;

import com.google.protobuf.ByteString;
import nodecore.api.grpc.VeriBlockMessages;

/**
 * Process requests form admin api.
 */
public interface AdminApiService {

    VeriBlockMessages.GetStateInfoReply getStateInfo(VeriBlockMessages.GetStateInfoRequest request);

    VeriBlockMessages.GetSignatureIndexReply getSignatureIndex(VeriBlockMessages.GetSignatureIndexRequest request);

    VeriBlockMessages.SendCoinsReply sendCoins(VeriBlockMessages.SendCoinsRequest request);

    VeriBlockMessages.ProtocolReply submitTransactions(VeriBlockMessages.SubmitTransactionsRequest request);

    VeriBlockMessages.DumpPrivateKeyReply dumpPrivateKey(VeriBlockMessages.DumpPrivateKeyRequest request);

    VeriBlockMessages.ImportPrivateKeyReply importPrivateKey(VeriBlockMessages.ImportPrivateKeyRequest request);

    VeriBlockMessages.ProtocolReply encryptWallet(VeriBlockMessages.EncryptWalletRequest request);

    VeriBlockMessages.ProtocolReply decryptWallet(VeriBlockMessages.DecryptWalletRequest request);

    VeriBlockMessages.ProtocolReply unlockWallet(VeriBlockMessages.UnlockWalletRequest request);

    VeriBlockMessages.ProtocolReply lockWallet(VeriBlockMessages.LockWalletRequest request);

    VeriBlockMessages.BackupWalletReply backupWallet(VeriBlockMessages.BackupWalletRequest request);

    VeriBlockMessages.ImportWalletReply importWallet(VeriBlockMessages.ImportWalletRequest request);

    VeriBlockMessages.GetNewAddressReply getNewAddress(VeriBlockMessages.GetNewAddressRequest request);

    VeriBlockMessages.GetBalanceReply getBalance(VeriBlockMessages.GetBalanceRequest request);

    VeriBlockMessages.CreateAltChainEndorsementReply createAltChainEndorsement(VeriBlockMessages.CreateAltChainEndorsementRequest request);

    VeriBlockMessages.BlockHeader getLastVBKBlockHeader();

    VeriBlockMessages.BlockHeader getVbkBlockHeader(ByteString hash);

    VeriBlockMessages.BlockHeader getVbkBlockHeader(Integer height);

    //    VeriBlockMessages.BlockHeader getLastBTCBlockHeader();

    VeriBlockMessages.GetLastBitcoinBlockReply getLastBitcoinBlock(VeriBlockMessages.GetLastBitcoinBlockRequest request);

    VeriBlockMessages.GetTransactionsReply getTransactions(VeriBlockMessages.GetTransactionsRequest request);

    VeriBlockMessages.GetVeriBlockPublicationsReply getVeriBlockPublications(VeriBlockMessages.GetVeriBlockPublicationsRequest getVeriBlockPublicationsRequest);
}
