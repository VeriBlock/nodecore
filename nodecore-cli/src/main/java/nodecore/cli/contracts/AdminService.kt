// VeriBlock NodeCore CLI
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.contracts

import nodecore.api.grpc.RpcAbandonTransactionReply
import nodecore.api.grpc.RpcAbandonTransactionRequest
import nodecore.api.grpc.RpcBackupWalletReply
import nodecore.api.grpc.RpcBackupWalletRequest
import nodecore.api.grpc.RpcClearAllowedRequest
import nodecore.api.grpc.RpcClearBannedMinersRequest
import nodecore.api.grpc.RpcClearBannedRequest
import nodecore.api.grpc.RpcDecryptWalletRequest
import nodecore.api.grpc.RpcDrainAddressReply
import nodecore.api.grpc.RpcDrainAddressRequest
import nodecore.api.grpc.RpcDumpPrivateKeyReply
import nodecore.api.grpc.RpcDumpPrivateKeyRequest
import nodecore.api.grpc.RpcEncryptWalletRequest
import nodecore.api.grpc.RpcGenerateMultisigAddressReply
import nodecore.api.grpc.RpcGenerateMultisigAddressRequest
import nodecore.api.grpc.RpcGetBalanceReply
import nodecore.api.grpc.RpcGetBalanceRequest
import nodecore.api.grpc.RpcGetBalanceUnlockScheduleReply
import nodecore.api.grpc.RpcGetBalanceUnlockScheduleRequest
import nodecore.api.grpc.RpcGetBlockTemplateReply
import nodecore.api.grpc.RpcGetBlockTemplateRequest
import nodecore.api.grpc.RpcGetBlockTimesReply
import nodecore.api.grpc.RpcGetBlockTimesRequest
import nodecore.api.grpc.RpcGetBlockchainsReply
import nodecore.api.grpc.RpcGetBlockchainsRequest
import nodecore.api.grpc.RpcGetBlocksReply
import nodecore.api.grpc.RpcGetBlocksRequest
import nodecore.api.grpc.RpcGetDiagnosticInfoReply
import nodecore.api.grpc.RpcGetDiagnosticInfoRequest
import nodecore.api.grpc.RpcGetHistoryReply
import nodecore.api.grpc.RpcGetHistoryRequest
import nodecore.api.grpc.RpcGetInfoReply
import nodecore.api.grpc.RpcGetInfoRequest
import nodecore.api.grpc.RpcGetLastBitcoinBlockReply
import nodecore.api.grpc.RpcGetLastBitcoinBlockRequest
import nodecore.api.grpc.RpcGetNewAddressReply
import nodecore.api.grpc.RpcGetNewAddressRequest
import nodecore.api.grpc.RpcGetPeerInfoReply
import nodecore.api.grpc.RpcGetPeerInfoRequest
import nodecore.api.grpc.RpcGetPendingTransactionsReply
import nodecore.api.grpc.RpcGetPendingTransactionsRequest
import nodecore.api.grpc.RpcGetPoPEndorsementsInfoReply
import nodecore.api.grpc.RpcGetPoPEndorsementsInfoRequest
import nodecore.api.grpc.RpcGetPoolStateReply
import nodecore.api.grpc.RpcGetPoolStateRequest
import nodecore.api.grpc.RpcGetPopReply
import nodecore.api.grpc.RpcGetPopRequest
import nodecore.api.grpc.RpcGetProtectedChildrenReply
import nodecore.api.grpc.RpcGetProtectedChildrenRequest
import nodecore.api.grpc.RpcGetProtectingParentsReply
import nodecore.api.grpc.RpcGetProtectingParentsRequest
import nodecore.api.grpc.RpcGetSignatureIndexReply
import nodecore.api.grpc.RpcGetSignatureIndexRequest
import nodecore.api.grpc.RpcGetStateInfoReply
import nodecore.api.grpc.RpcGetStateInfoRequest
import nodecore.api.grpc.RpcGetTransactionsReply
import nodecore.api.grpc.RpcGetTransactionsRequest
import nodecore.api.grpc.RpcGetWalletTransactionsReply
import nodecore.api.grpc.RpcGetWalletTransactionsRequest
import nodecore.api.grpc.RpcImportPrivateKeyReply
import nodecore.api.grpc.RpcImportPrivateKeyRequest
import nodecore.api.grpc.RpcImportWalletReply
import nodecore.api.grpc.RpcImportWalletRequest
import nodecore.api.grpc.RpcListAllowedReply
import nodecore.api.grpc.RpcListAllowedRequest
import nodecore.api.grpc.RpcListBannedMinersReply
import nodecore.api.grpc.RpcListBannedMinersRequest
import nodecore.api.grpc.RpcListBannedReply
import nodecore.api.grpc.RpcListBannedRequest
import nodecore.api.grpc.RpcLockWalletRequest
import nodecore.api.grpc.RpcMakeUnsignedMultisigTxReply
import nodecore.api.grpc.RpcMakeUnsignedMultisigTxRequest
import nodecore.api.grpc.RpcNodeRequest
import nodecore.api.grpc.RpcPingReply
import nodecore.api.grpc.RpcProtocolReply
import nodecore.api.grpc.RpcRebroadcastTransactionReply
import nodecore.api.grpc.RpcRebroadcastTransactionRequest
import nodecore.api.grpc.RpcRefreshWalletCacheRequest
import nodecore.api.grpc.RpcRestartPoolWebServerReply
import nodecore.api.grpc.RpcRestartPoolWebServerRequest
import nodecore.api.grpc.RpcSendCoinsReply
import nodecore.api.grpc.RpcSendCoinsRequest
import nodecore.api.grpc.RpcSetAllowedRequest
import nodecore.api.grpc.RpcSetDefaultAddressReply
import nodecore.api.grpc.RpcSetDefaultAddressRequest
import nodecore.api.grpc.RpcSetTransactionFeeRequest
import nodecore.api.grpc.RpcSignMessageReply
import nodecore.api.grpc.RpcSignMessageRequest
import nodecore.api.grpc.RpcStartPoolReply
import nodecore.api.grpc.RpcStartPoolRequest
import nodecore.api.grpc.RpcStartSoloPoolReply
import nodecore.api.grpc.RpcStartSoloPoolRequest
import nodecore.api.grpc.RpcStopNodeCoreRequest
import nodecore.api.grpc.RpcStopPoolReply
import nodecore.api.grpc.RpcStopPoolRequest
import nodecore.api.grpc.RpcSubmitBlocksRequest
import nodecore.api.grpc.RpcSubmitMultisigTxReply
import nodecore.api.grpc.RpcSubmitMultisigTxRequest
import nodecore.api.grpc.RpcSubmitPopRequest
import nodecore.api.grpc.RpcSubmitTransactionsRequest
import nodecore.api.grpc.RpcTroubleshootPoPTransactionsReply
import nodecore.api.grpc.RpcTroubleshootPoPTransactionsRequest
import nodecore.api.grpc.RpcUnlockWalletRequest
import nodecore.api.grpc.RpcValidateAddressReply
import nodecore.api.grpc.RpcValidateAddressRequest

interface AdminService {
    fun getPop(request: RpcGetPopRequest): RpcGetPopReply
    fun addNode(request: RpcNodeRequest): RpcProtocolReply
    fun getInfo(request: RpcGetInfoRequest): RpcGetInfoReply
    fun removeNode(request: RpcNodeRequest): RpcProtocolReply
    fun stopPool(request: RpcStopPoolRequest): RpcStopPoolReply
    fun submitPop(request: RpcSubmitPopRequest): RpcProtocolReply
    fun sendCoins(request: RpcSendCoinsRequest): RpcSendCoinsReply
    fun getBlocks(request: RpcGetBlocksRequest): RpcGetBlocksReply
    fun startPool(request: RpcStartPoolRequest): RpcStartPoolReply
    fun setAllowed(request: RpcSetAllowedRequest): RpcProtocolReply
    fun getHistory(request: RpcGetHistoryRequest): RpcGetHistoryReply
    fun getBalance(request: RpcGetBalanceRequest): RpcGetBalanceReply
    fun submitBlocks(request: RpcSubmitBlocksRequest): RpcProtocolReply
    fun clearAllowed(request: RpcClearAllowedRequest): RpcProtocolReply
    fun listAllowed(request: RpcListAllowedRequest): RpcListAllowedReply
    fun getPeerInfo(request: RpcGetPeerInfoRequest): RpcGetPeerInfoReply
    fun signMessage(request: RpcSignMessageRequest): RpcSignMessageReply
    fun backupWallet(request: RpcBackupWalletRequest): RpcBackupWalletReply
    fun importWallet(request: RpcImportWalletRequest): RpcImportWalletReply
    fun getNewAddress(request: RpcGetNewAddressRequest): RpcGetNewAddressReply
    fun getBlockTimes(request: RpcGetBlockTimesRequest): RpcGetBlockTimesReply
    fun startSoloPool(request: RpcStartSoloPoolRequest): RpcStartSoloPoolReply
    fun getBlockchains(request: RpcGetBlockchainsRequest): RpcGetBlockchainsReply
    fun setTransactionFee(request: RpcSetTransactionFeeRequest): RpcProtocolReply
    fun dumpPrivateKey(request: RpcDumpPrivateKeyRequest): RpcDumpPrivateKeyReply
    fun importPrivateKey(request: RpcImportPrivateKeyRequest): RpcImportPrivateKeyReply
    fun submitTransactions(request: RpcSubmitTransactionsRequest): RpcProtocolReply
    fun validateAddress(request: RpcValidateAddressRequest): RpcValidateAddressReply
    fun generateMultisigAddress(request: RpcGenerateMultisigAddressRequest): RpcGenerateMultisigAddressReply
    fun makeUnsignedMultisigTx(request: RpcMakeUnsignedMultisigTxRequest): RpcMakeUnsignedMultisigTxReply
    fun submitMultisigTx(request: RpcSubmitMultisigTxRequest): RpcSubmitMultisigTxReply
    fun getTransactions(request: RpcGetTransactionsRequest): RpcGetTransactionsReply
    fun getBlockTemplate(request: RpcGetBlockTemplateRequest): RpcGetBlockTemplateReply
    fun getSignatureIndex(request: RpcGetSignatureIndexRequest): RpcGetSignatureIndexReply
    fun setDefaultAddress(request: RpcSetDefaultAddressRequest): RpcSetDefaultAddressReply
    fun getLastBitcoinBlock(request: RpcGetLastBitcoinBlockRequest): RpcGetLastBitcoinBlockReply
    fun getProtectedChildren(request: RpcGetProtectedChildrenRequest): RpcGetProtectedChildrenReply
    fun getProtectingParents(request: RpcGetProtectingParentsRequest): RpcGetProtectingParentsReply
    fun restartPoolWebServer(request: RpcRestartPoolWebServerRequest): RpcRestartPoolWebServerReply
    fun getPopEndorsementsInfo(request: RpcGetPoPEndorsementsInfoRequest): RpcGetPoPEndorsementsInfoReply
    fun getPendingTransactions(request: RpcGetPendingTransactionsRequest): RpcGetPendingTransactionsReply
    fun getStateInfo(request: RpcGetStateInfoRequest): RpcGetStateInfoReply
    fun getDiagnosticInfo(request: RpcGetDiagnosticInfoRequest): RpcGetDiagnosticInfoReply
    fun stopNodeCore(request: RpcStopNodeCoreRequest): RpcProtocolReply
    fun refreshWalletCache(request: RpcRefreshWalletCacheRequest): RpcProtocolReply
    fun getWalletTransactions(request: RpcGetWalletTransactionsRequest): RpcGetWalletTransactionsReply
    fun connect(): RpcPingReply
    fun troubleshootPopTransactions(request: RpcTroubleshootPoPTransactionsRequest): RpcTroubleshootPoPTransactionsReply
    fun clearBanned(request: RpcClearBannedRequest): RpcProtocolReply
    fun listBanned(request: RpcListBannedRequest): RpcListBannedReply
    fun clearBannedMiners(request: RpcClearBannedMinersRequest): RpcProtocolReply
    fun listBannedMiners(request: RpcListBannedMinersRequest): RpcListBannedMinersReply
    fun encryptWallet(request: RpcEncryptWalletRequest): RpcProtocolReply
    fun decryptWallet(request: RpcDecryptWalletRequest): RpcProtocolReply
    fun unlockWallet(request: RpcUnlockWalletRequest): RpcProtocolReply
    fun lockWallet(request: RpcLockWalletRequest): RpcProtocolReply
    fun drainAddress(request: RpcDrainAddressRequest): RpcDrainAddressReply
    fun getBalanceUnlockSchedule(request: RpcGetBalanceUnlockScheduleRequest): RpcGetBalanceUnlockScheduleReply
    fun getPoolState(request: RpcGetPoolStateRequest): RpcGetPoolStateReply
    fun abandonTransactionRequest(request: RpcAbandonTransactionRequest): RpcAbandonTransactionReply
    fun rebroadcastTransactionRequest(request: RpcRebroadcastTransactionRequest): RpcRebroadcastTransactionReply
}
