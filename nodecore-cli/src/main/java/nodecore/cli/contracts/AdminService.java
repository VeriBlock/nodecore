// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.contracts;

import nodecore.api.grpc.*;

public interface AdminService {
    GetPopReply getPop(GetPopRequest request);

    ProtocolReply addNode(NodeRequest request);

    GetInfoReply getInfo(GetInfoRequest request);

    ProtocolReply removeNode(NodeRequest request);

    StopPoolReply stopPool(StopPoolRequest request);

    ProtocolReply submitPop(SubmitPopRequest request);

    SendCoinsReply sendCoins(SendCoinsRequest request);

    GetBlocksReply getBlocks(GetBlocksRequest request);

    StartPoolReply startPool(StartPoolRequest request);

    ProtocolReply setAllowed(SetAllowedRequest request);

    GetHistoryReply getHistory(GetHistoryRequest request);

    GetBalanceReply getBalance(GetBalanceRequest request);

    ProtocolReply submitBlocks(SubmitBlocksRequest request);

    ProtocolReply clearAllowed(ClearAllowedRequest request);

    ListAllowedReply listAllowed(ListAllowedRequest request);

    GetPeerInfoReply getPeerInfo(GetPeerInfoRequest request);

    SignMessageReply signMessage(SignMessageRequest request);

    BackupWalletReply backupWallet(BackupWalletRequest request);

    ImportWalletReply importWallet(ImportWalletRequest request);

    GetNewAddressReply getNewAddress(GetNewAddressRequest request);

    GetBlockTimesReply getBlockTimes(GetBlockTimesRequest request);

    StartSoloPoolReply startSoloPool(StartSoloPoolRequest request);

    GetBlockchainsReply getBlockchains(GetBlockchainsRequest request);

    ProtocolReply setTransactionFee(SetTransactionFeeRequest request);

    DumpPrivateKeyReply dumpPrivateKey(DumpPrivateKeyRequest request);

    ImportPrivateKeyReply importPrivateKey(ImportPrivateKeyRequest request);

    ProtocolReply submitTransactions(SubmitTransactionsRequest request);

    ValidateAddressReply validateAddress(ValidateAddressRequest request);

    GenerateMultisigAddressReply generateMultisigAddress(GenerateMultisigAddressRequest request);

    MakeUnsignedMultisigTxReply makeUnsignedMultisigTx(MakeUnsignedMultisigTxRequest request);

    SubmitMultisigTxReply submitMultisigTx(SubmitMultisigTxRequest request);

    GetTransactionsReply getTransactions(GetTransactionsRequest request);

    GetBlockTemplateReply getBlockTemplate(GetBlockTemplateRequest request);

    GetSignatureIndexReply getSignatureIndex(GetSignatureIndexRequest request);

    SetDefaultAddressReply setDefaultAddress(SetDefaultAddressRequest request);

    GetLastBitcoinBlockReply getLastBitcoinBlock(GetLastBitcoinBlockRequest request);

    GetProtectedChildrenReply getProtectedChildren(GetProtectedChildrenRequest request);

    GetProtectingParentsReply getProtectingParents(GetProtectingParentsRequest request);

    RestartPoolWebServerReply restartPoolWebServer(RestartPoolWebServerRequest request);

    GetPoPEndorsementsInfoReply getPoPEndorsementsInfo(GetPoPEndorsementsInfoRequest request);

    GetPendingTransactionsReply getPendingTransactions(GetPendingTransactionsRequest request);

    GetStateInfoReply getStateInfo(GetStateInfoRequest request);

    GetDiagnosticInfoReply getDiagnosticInfo(GetDiagnosticInfoRequest request);

    ProtocolReply stopNodeCore(StopNodeCoreRequest request);

    ProtocolReply refreshWalletCache(RefreshWalletCacheRequest request);

    GetWalletTransactionsReply getWalletTransactions(GetWalletTransactionsRequest build);

    PingReply connect();

    TroubleshootPoPTransactionsReply troubleshootPoPTransactions(TroubleshootPoPTransactionsRequest request);

    ProtocolReply clearBanned(ClearBannedRequest request);

    ListBannedReply listBanned(ListBannedRequest request);

    ProtocolReply clearBannedMiners(ClearBannedMinersRequest request);

    ListBannedMinersReply listBannedMiners(ListBannedMinersRequest request);

    ProtocolReply encryptWallet(EncryptWalletRequest request);

    ProtocolReply decryptWallet(DecryptWalletRequest request);

    ProtocolReply unlockWallet(UnlockWalletRequest request);

    ProtocolReply lockWallet(LockWalletRequest request);

    DrainAddressReply drainAddress(DrainAddressRequest request);
    
    GetBalanceUnlockScheduleReply getBalanceUnlockSchedule(GetBalanceUnlockScheduleRequest request);

    GetPoolStateReply getPoolState(GetPoolStateRequest request);
}
