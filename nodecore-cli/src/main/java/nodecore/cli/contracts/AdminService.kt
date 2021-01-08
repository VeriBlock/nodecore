// VeriBlock NodeCore CLI
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.contracts

import nodecore.api.grpc.VeriBlockMessages.AbandonTransactionReply
import nodecore.api.grpc.VeriBlockMessages.AbandonTransactionRequest
import nodecore.api.grpc.VeriBlockMessages.BackupWalletReply
import nodecore.api.grpc.VeriBlockMessages.BackupWalletRequest
import nodecore.api.grpc.VeriBlockMessages.ClearAllowedRequest
import nodecore.api.grpc.VeriBlockMessages.ClearBannedMinersRequest
import nodecore.api.grpc.VeriBlockMessages.ClearBannedRequest
import nodecore.api.grpc.VeriBlockMessages.DecryptWalletRequest
import nodecore.api.grpc.VeriBlockMessages.DrainAddressReply
import nodecore.api.grpc.VeriBlockMessages.DrainAddressRequest
import nodecore.api.grpc.VeriBlockMessages.DumpPrivateKeyReply
import nodecore.api.grpc.VeriBlockMessages.DumpPrivateKeyRequest
import nodecore.api.grpc.VeriBlockMessages.EncryptWalletRequest
import nodecore.api.grpc.VeriBlockMessages.GenerateMultisigAddressReply
import nodecore.api.grpc.VeriBlockMessages.GenerateMultisigAddressRequest
import nodecore.api.grpc.VeriBlockMessages.GetBalanceReply
import nodecore.api.grpc.VeriBlockMessages.GetBalanceRequest
import nodecore.api.grpc.VeriBlockMessages.GetBalanceUnlockScheduleReply
import nodecore.api.grpc.VeriBlockMessages.GetBalanceUnlockScheduleRequest
import nodecore.api.grpc.VeriBlockMessages.GetBlockTemplateReply
import nodecore.api.grpc.VeriBlockMessages.GetBlockTemplateRequest
import nodecore.api.grpc.VeriBlockMessages.GetBlockTimesReply
import nodecore.api.grpc.VeriBlockMessages.GetBlockTimesRequest
import nodecore.api.grpc.VeriBlockMessages.GetBlockchainsReply
import nodecore.api.grpc.VeriBlockMessages.GetBlockchainsRequest
import nodecore.api.grpc.VeriBlockMessages.GetBlocksReply
import nodecore.api.grpc.VeriBlockMessages.GetBlocksRequest
import nodecore.api.grpc.VeriBlockMessages.GetDiagnosticInfoReply
import nodecore.api.grpc.VeriBlockMessages.GetDiagnosticInfoRequest
import nodecore.api.grpc.VeriBlockMessages.GetHistoryReply
import nodecore.api.grpc.VeriBlockMessages.GetHistoryRequest
import nodecore.api.grpc.VeriBlockMessages.GetInfoReply
import nodecore.api.grpc.VeriBlockMessages.GetInfoRequest
import nodecore.api.grpc.VeriBlockMessages.GetLastBitcoinBlockReply
import nodecore.api.grpc.VeriBlockMessages.GetLastBitcoinBlockRequest
import nodecore.api.grpc.VeriBlockMessages.GetNewAddressReply
import nodecore.api.grpc.VeriBlockMessages.GetNewAddressRequest
import nodecore.api.grpc.VeriBlockMessages.GetPeerInfoReply
import nodecore.api.grpc.VeriBlockMessages.GetPeerInfoRequest
import nodecore.api.grpc.VeriBlockMessages.GetPendingTransactionsReply
import nodecore.api.grpc.VeriBlockMessages.GetPendingTransactionsRequest
import nodecore.api.grpc.VeriBlockMessages.GetPoPEndorsementsInfoReply
import nodecore.api.grpc.VeriBlockMessages.GetPoPEndorsementsInfoRequest
import nodecore.api.grpc.VeriBlockMessages.GetPoolStateReply
import nodecore.api.grpc.VeriBlockMessages.GetPoolStateRequest
import nodecore.api.grpc.VeriBlockMessages.GetPopReply
import nodecore.api.grpc.VeriBlockMessages.GetPopRequest
import nodecore.api.grpc.VeriBlockMessages.GetProtectedChildrenReply
import nodecore.api.grpc.VeriBlockMessages.GetProtectedChildrenRequest
import nodecore.api.grpc.VeriBlockMessages.GetProtectingParentsReply
import nodecore.api.grpc.VeriBlockMessages.GetProtectingParentsRequest
import nodecore.api.grpc.VeriBlockMessages.GetSignatureIndexReply
import nodecore.api.grpc.VeriBlockMessages.GetSignatureIndexRequest
import nodecore.api.grpc.VeriBlockMessages.GetStateInfoReply
import nodecore.api.grpc.VeriBlockMessages.GetStateInfoRequest
import nodecore.api.grpc.VeriBlockMessages.GetTransactionsReply
import nodecore.api.grpc.VeriBlockMessages.GetTransactionsRequest
import nodecore.api.grpc.VeriBlockMessages.GetWalletTransactionsReply
import nodecore.api.grpc.VeriBlockMessages.GetWalletTransactionsRequest
import nodecore.api.grpc.VeriBlockMessages.ImportPrivateKeyReply
import nodecore.api.grpc.VeriBlockMessages.ImportPrivateKeyRequest
import nodecore.api.grpc.VeriBlockMessages.ImportWalletReply
import nodecore.api.grpc.VeriBlockMessages.ImportWalletRequest
import nodecore.api.grpc.VeriBlockMessages.ListAllowedReply
import nodecore.api.grpc.VeriBlockMessages.ListAllowedRequest
import nodecore.api.grpc.VeriBlockMessages.ListBannedMinersReply
import nodecore.api.grpc.VeriBlockMessages.ListBannedMinersRequest
import nodecore.api.grpc.VeriBlockMessages.ListBannedReply
import nodecore.api.grpc.VeriBlockMessages.ListBannedRequest
import nodecore.api.grpc.VeriBlockMessages.LockWalletRequest
import nodecore.api.grpc.VeriBlockMessages.MakeUnsignedMultisigTxReply
import nodecore.api.grpc.VeriBlockMessages.MakeUnsignedMultisigTxRequest
import nodecore.api.grpc.VeriBlockMessages.NodeRequest
import nodecore.api.grpc.VeriBlockMessages.PingReply
import nodecore.api.grpc.VeriBlockMessages.ProtocolReply
import nodecore.api.grpc.VeriBlockMessages.RebroadcastTransactionReply
import nodecore.api.grpc.VeriBlockMessages.RebroadcastTransactionRequest
import nodecore.api.grpc.VeriBlockMessages.RefreshWalletCacheRequest
import nodecore.api.grpc.VeriBlockMessages.RestartPoolWebServerReply
import nodecore.api.grpc.VeriBlockMessages.RestartPoolWebServerRequest
import nodecore.api.grpc.VeriBlockMessages.SendCoinsReply
import nodecore.api.grpc.VeriBlockMessages.SendCoinsRequest
import nodecore.api.grpc.VeriBlockMessages.SetAllowedRequest
import nodecore.api.grpc.VeriBlockMessages.SetDefaultAddressReply
import nodecore.api.grpc.VeriBlockMessages.SetDefaultAddressRequest
import nodecore.api.grpc.VeriBlockMessages.SetTransactionFeeRequest
import nodecore.api.grpc.VeriBlockMessages.SignMessageReply
import nodecore.api.grpc.VeriBlockMessages.SignMessageRequest
import nodecore.api.grpc.VeriBlockMessages.StartPoolReply
import nodecore.api.grpc.VeriBlockMessages.StartPoolRequest
import nodecore.api.grpc.VeriBlockMessages.StartSoloPoolReply
import nodecore.api.grpc.VeriBlockMessages.StartSoloPoolRequest
import nodecore.api.grpc.VeriBlockMessages.StopNodeCoreRequest
import nodecore.api.grpc.VeriBlockMessages.StopPoolReply
import nodecore.api.grpc.VeriBlockMessages.StopPoolRequest
import nodecore.api.grpc.VeriBlockMessages.SubmitBlocksRequest
import nodecore.api.grpc.VeriBlockMessages.SubmitMultisigTxReply
import nodecore.api.grpc.VeriBlockMessages.SubmitMultisigTxRequest
import nodecore.api.grpc.VeriBlockMessages.SubmitPopRequest
import nodecore.api.grpc.VeriBlockMessages.SubmitTransactionsRequest
import nodecore.api.grpc.VeriBlockMessages.TroubleshootPoPTransactionsReply
import nodecore.api.grpc.VeriBlockMessages.TroubleshootPoPTransactionsRequest
import nodecore.api.grpc.VeriBlockMessages.UnlockWalletRequest
import nodecore.api.grpc.VeriBlockMessages.ValidateAddressReply
import nodecore.api.grpc.VeriBlockMessages.ValidateAddressRequest

interface AdminService {
    fun getPop(request: GetPopRequest): GetPopReply
    fun addNode(request: NodeRequest): ProtocolReply
    fun getInfo(request: GetInfoRequest): GetInfoReply
    fun removeNode(request: NodeRequest): ProtocolReply
    fun stopPool(request: StopPoolRequest): StopPoolReply
    fun submitPop(request: SubmitPopRequest): ProtocolReply
    fun sendCoins(request: SendCoinsRequest): SendCoinsReply
    fun getBlocks(request: GetBlocksRequest): GetBlocksReply
    fun startPool(request: StartPoolRequest): StartPoolReply
    fun setAllowed(request: SetAllowedRequest): ProtocolReply
    fun getHistory(request: GetHistoryRequest): GetHistoryReply
    fun getBalance(request: GetBalanceRequest): GetBalanceReply
    fun submitBlocks(request: SubmitBlocksRequest): ProtocolReply
    fun clearAllowed(request: ClearAllowedRequest): ProtocolReply
    fun listAllowed(request: ListAllowedRequest): ListAllowedReply
    fun getPeerInfo(request: GetPeerInfoRequest): GetPeerInfoReply
    fun signMessage(request: SignMessageRequest): SignMessageReply
    fun backupWallet(request: BackupWalletRequest): BackupWalletReply
    fun importWallet(request: ImportWalletRequest): ImportWalletReply
    fun getNewAddress(request: GetNewAddressRequest): GetNewAddressReply
    fun getBlockTimes(request: GetBlockTimesRequest): GetBlockTimesReply
    fun startSoloPool(request: StartSoloPoolRequest): StartSoloPoolReply
    fun getBlockchains(request: GetBlockchainsRequest): GetBlockchainsReply
    fun setTransactionFee(request: SetTransactionFeeRequest): ProtocolReply
    fun dumpPrivateKey(request: DumpPrivateKeyRequest): DumpPrivateKeyReply
    fun importPrivateKey(request: ImportPrivateKeyRequest): ImportPrivateKeyReply
    fun submitTransactions(request: SubmitTransactionsRequest): ProtocolReply
    fun validateAddress(request: ValidateAddressRequest): ValidateAddressReply
    fun generateMultisigAddress(request: GenerateMultisigAddressRequest): GenerateMultisigAddressReply
    fun makeUnsignedMultisigTx(request: MakeUnsignedMultisigTxRequest): MakeUnsignedMultisigTxReply
    fun submitMultisigTx(request: SubmitMultisigTxRequest): SubmitMultisigTxReply
    fun getTransactions(request: GetTransactionsRequest): GetTransactionsReply
    fun getBlockTemplate(request: GetBlockTemplateRequest): GetBlockTemplateReply
    fun getSignatureIndex(request: GetSignatureIndexRequest): GetSignatureIndexReply
    fun setDefaultAddress(request: SetDefaultAddressRequest): SetDefaultAddressReply
    fun getLastBitcoinBlock(request: GetLastBitcoinBlockRequest): GetLastBitcoinBlockReply
    fun getProtectedChildren(request: GetProtectedChildrenRequest): GetProtectedChildrenReply
    fun getProtectingParents(request: GetProtectingParentsRequest): GetProtectingParentsReply
    fun restartPoolWebServer(request: RestartPoolWebServerRequest): RestartPoolWebServerReply
    fun getPoPEndorsementsInfo(request: GetPoPEndorsementsInfoRequest): GetPoPEndorsementsInfoReply
    fun getPendingTransactions(request: GetPendingTransactionsRequest): GetPendingTransactionsReply
    fun getStateInfo(request: GetStateInfoRequest): GetStateInfoReply
    fun getDiagnosticInfo(request: GetDiagnosticInfoRequest): GetDiagnosticInfoReply
    fun stopNodeCore(request: StopNodeCoreRequest): ProtocolReply
    fun refreshWalletCache(request: RefreshWalletCacheRequest): ProtocolReply
    fun getWalletTransactions(build: GetWalletTransactionsRequest): GetWalletTransactionsReply
    fun connect(): PingReply
    fun troubleshootPoPTransactions(request: TroubleshootPoPTransactionsRequest): TroubleshootPoPTransactionsReply
    fun clearBanned(request: ClearBannedRequest): ProtocolReply
    fun listBanned(request: ListBannedRequest): ListBannedReply
    fun clearBannedMiners(request: ClearBannedMinersRequest): ProtocolReply
    fun listBannedMiners(request: ListBannedMinersRequest): ListBannedMinersReply
    fun encryptWallet(request: EncryptWalletRequest): ProtocolReply
    fun decryptWallet(request: DecryptWalletRequest): ProtocolReply
    fun unlockWallet(request: UnlockWalletRequest): ProtocolReply
    fun lockWallet(request: LockWalletRequest): ProtocolReply
    fun drainAddress(request: DrainAddressRequest): DrainAddressReply
    fun getBalanceUnlockSchedule(request: GetBalanceUnlockScheduleRequest): GetBalanceUnlockScheduleReply
    fun getPoolState(request: GetPoolStateRequest): GetPoolStateReply
    fun abandonTransactionRequest(request: AbandonTransactionRequest): AbandonTransactionReply
    fun rebroadcastTransactionRequest(request: RebroadcastTransactionRequest): RebroadcastTransactionReply
}
