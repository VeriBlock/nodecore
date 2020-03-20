// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.contracts;

import nodecore.api.grpc.VeriBlockMessages;

public interface AdminService {
    VeriBlockMessages.GetPopReply getPop(VeriBlockMessages.GetPopRequest request);

    VeriBlockMessages.ProtocolReply addNode(VeriBlockMessages.NodeRequest request);

    VeriBlockMessages.GetInfoReply getInfo(VeriBlockMessages.GetInfoRequest request);

    VeriBlockMessages.ProtocolReply removeNode(VeriBlockMessages.NodeRequest request);

    VeriBlockMessages.StopPoolReply stopPool(VeriBlockMessages.StopPoolRequest request);

    VeriBlockMessages.ProtocolReply submitPop(VeriBlockMessages.SubmitPopRequest request);

    VeriBlockMessages.SendCoinsReply sendCoins(VeriBlockMessages.SendCoinsRequest request);

    VeriBlockMessages.GetBlocksReply getBlocks(VeriBlockMessages.GetBlocksRequest request);

    VeriBlockMessages.StartPoolReply startPool(VeriBlockMessages.StartPoolRequest request);

    VeriBlockMessages.ProtocolReply setAllowed(VeriBlockMessages.SetAllowedRequest request);

    VeriBlockMessages.GetHistoryReply getHistory(VeriBlockMessages.GetHistoryRequest request);

    VeriBlockMessages.GetBalanceReply getBalance(VeriBlockMessages.GetBalanceRequest request);

    VeriBlockMessages.ProtocolReply submitBlocks(VeriBlockMessages.SubmitBlocksRequest request);

    VeriBlockMessages.ProtocolReply clearAllowed(VeriBlockMessages.ClearAllowedRequest request);

    VeriBlockMessages.ListAllowedReply listAllowed(VeriBlockMessages.ListAllowedRequest request);

    VeriBlockMessages.GetPeerInfoReply getPeerInfo(VeriBlockMessages.GetPeerInfoRequest request);

    VeriBlockMessages.SignMessageReply signMessage(VeriBlockMessages.SignMessageRequest request);

    VeriBlockMessages.BackupWalletReply backupWallet(VeriBlockMessages.BackupWalletRequest request);

    VeriBlockMessages.ImportWalletReply importWallet(VeriBlockMessages.ImportWalletRequest request);

    VeriBlockMessages.GetNewAddressReply getNewAddress(VeriBlockMessages.GetNewAddressRequest request);

    VeriBlockMessages.GetBlockTimesReply getBlockTimes(VeriBlockMessages.GetBlockTimesRequest request);

    VeriBlockMessages.StartSoloPoolReply startSoloPool(VeriBlockMessages.StartSoloPoolRequest request);

    VeriBlockMessages.GetBlockchainsReply getBlockchains(VeriBlockMessages.GetBlockchainsRequest request);

    VeriBlockMessages.ProtocolReply setTransactionFee(VeriBlockMessages.SetTransactionFeeRequest request);

    VeriBlockMessages.DumpPrivateKeyReply dumpPrivateKey(VeriBlockMessages.DumpPrivateKeyRequest request);

    VeriBlockMessages.ImportPrivateKeyReply importPrivateKey(VeriBlockMessages.ImportPrivateKeyRequest request);

    VeriBlockMessages.ProtocolReply submitTransactions(VeriBlockMessages.SubmitTransactionsRequest request);

    VeriBlockMessages.ValidateAddressReply validateAddress(VeriBlockMessages.ValidateAddressRequest request);

    VeriBlockMessages.GenerateMultisigAddressReply generateMultisigAddress(VeriBlockMessages.GenerateMultisigAddressRequest request);

    VeriBlockMessages.MakeUnsignedMultisigTxReply makeUnsignedMultisigTx(VeriBlockMessages.MakeUnsignedMultisigTxRequest request);

    VeriBlockMessages.SubmitMultisigTxReply submitMultisigTx(VeriBlockMessages.SubmitMultisigTxRequest request);

    VeriBlockMessages.GetTransactionsReply getTransactions(VeriBlockMessages.GetTransactionsRequest request);

    VeriBlockMessages.GetBlockTemplateReply getBlockTemplate(VeriBlockMessages.GetBlockTemplateRequest request);

    VeriBlockMessages.GetSignatureIndexReply getSignatureIndex(VeriBlockMessages.GetSignatureIndexRequest request);

    VeriBlockMessages.SetDefaultAddressReply setDefaultAddress(VeriBlockMessages.SetDefaultAddressRequest request);

    VeriBlockMessages.GetLastBitcoinBlockReply getLastBitcoinBlock(VeriBlockMessages.GetLastBitcoinBlockRequest request);

    VeriBlockMessages.GetProtectedChildrenReply getProtectedChildren(VeriBlockMessages.GetProtectedChildrenRequest request);

    VeriBlockMessages.GetProtectingParentsReply getProtectingParents(VeriBlockMessages.GetProtectingParentsRequest request);

    VeriBlockMessages.RestartPoolWebServerReply restartPoolWebServer(VeriBlockMessages.RestartPoolWebServerRequest request);

    VeriBlockMessages.GetPoPEndorsementsInfoReply getPoPEndorsementsInfo(VeriBlockMessages.GetPoPEndorsementsInfoRequest request);

    VeriBlockMessages.GetPendingTransactionsReply getPendingTransactions(VeriBlockMessages.GetPendingTransactionsRequest request);

    VeriBlockMessages.GetStateInfoReply getStateInfo(VeriBlockMessages.GetStateInfoRequest request);

    VeriBlockMessages.GetDiagnosticInfoReply getDiagnosticInfo(VeriBlockMessages.GetDiagnosticInfoRequest request);

    VeriBlockMessages.ProtocolReply stopNodeCore(VeriBlockMessages.StopNodeCoreRequest request);

    VeriBlockMessages.ProtocolReply refreshWalletCache(VeriBlockMessages.RefreshWalletCacheRequest request);

    VeriBlockMessages.GetWalletTransactionsReply getWalletTransactions(VeriBlockMessages.GetWalletTransactionsRequest build);

    VeriBlockMessages.PingReply connect();

    VeriBlockMessages.TroubleshootPoPTransactionsReply troubleshootPoPTransactions(VeriBlockMessages.TroubleshootPoPTransactionsRequest request);

    VeriBlockMessages.ProtocolReply clearBanned(VeriBlockMessages.ClearBannedRequest request);

    VeriBlockMessages.ListBannedReply listBanned(VeriBlockMessages.ListBannedRequest request);

    VeriBlockMessages.ProtocolReply clearBannedMiners(VeriBlockMessages.ClearBannedMinersRequest request);

    VeriBlockMessages.ListBannedMinersReply listBannedMiners(VeriBlockMessages.ListBannedMinersRequest request);

    VeriBlockMessages.ProtocolReply encryptWallet(VeriBlockMessages.EncryptWalletRequest request);

    VeriBlockMessages.ProtocolReply decryptWallet(VeriBlockMessages.DecryptWalletRequest request);

    VeriBlockMessages.ProtocolReply unlockWallet(VeriBlockMessages.UnlockWalletRequest request);

    VeriBlockMessages.ProtocolReply lockWallet(VeriBlockMessages.LockWalletRequest request);

    VeriBlockMessages.DrainAddressReply drainAddress(VeriBlockMessages.DrainAddressRequest request);
    
    VeriBlockMessages.GetBalanceUnlockScheduleReply getBalanceUnlockSchedule(VeriBlockMessages.GetBalanceUnlockScheduleRequest request);

    VeriBlockMessages.GetPoolStateReply getPoolState(VeriBlockMessages.GetPoolStateRequest request);

    VeriBlockMessages.AbandonTransactionReply abandonTransactionRequest(VeriBlockMessages.AbandonTransactionRequest request);
}
