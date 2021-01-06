// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.contracts

import nodecore.api.grpc.VeriBlockMessages.*

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
