// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.services

import nodecore.cli.contracts.EndpointTransportType
import nodecore.cli.contracts.AdminService
import java.lang.InterruptedException
import nodecore.api.grpc.VeriBlockMessages.GetPopRequest
import nodecore.api.grpc.VeriBlockMessages.GetPopReply
import nodecore.api.grpc.VeriBlockMessages.NodeRequest
import nodecore.api.grpc.VeriBlockMessages.ProtocolReply
import nodecore.api.grpc.VeriBlockMessages.GetInfoRequest
import nodecore.api.grpc.VeriBlockMessages.GetInfoReply
import nodecore.api.grpc.VeriBlockMessages.SendCoinsRequest
import nodecore.api.grpc.VeriBlockMessages.SendCoinsReply
import nodecore.api.grpc.VeriBlockMessages.SubmitPopRequest
import nodecore.api.grpc.VeriBlockMessages.GetBlocksRequest
import nodecore.api.grpc.VeriBlockMessages.GetBlocksReply
import nodecore.api.grpc.VeriBlockMessages.GetPoPEndorsementsInfoRequest
import nodecore.api.grpc.VeriBlockMessages.GetPoPEndorsementsInfoReply
import nodecore.api.grpc.VeriBlockMessages.GetProtectedChildrenRequest
import nodecore.api.grpc.VeriBlockMessages.GetProtectedChildrenReply
import nodecore.api.grpc.VeriBlockMessages.GetProtectingParentsRequest
import nodecore.api.grpc.VeriBlockMessages.GetProtectingParentsReply
import nodecore.api.grpc.VeriBlockMessages.StartPoolRequest
import nodecore.api.grpc.VeriBlockMessages.StartPoolReply
import nodecore.api.grpc.VeriBlockMessages.SetAllowedRequest
import nodecore.api.grpc.VeriBlockMessages.RestartPoolWebServerRequest
import nodecore.api.grpc.VeriBlockMessages.RestartPoolWebServerReply
import nodecore.api.grpc.VeriBlockMessages.StopPoolRequest
import nodecore.api.grpc.VeriBlockMessages.StopPoolReply
import nodecore.api.grpc.VeriBlockMessages.GetHistoryRequest
import nodecore.api.grpc.VeriBlockMessages.GetHistoryReply
import nodecore.api.grpc.VeriBlockMessages.GetBalanceRequest
import nodecore.api.grpc.VeriBlockMessages.GetBalanceReply
import nodecore.api.grpc.VeriBlockMessages.SubmitBlocksRequest
import nodecore.api.grpc.VeriBlockMessages.ClearAllowedRequest
import nodecore.api.grpc.VeriBlockMessages.ListAllowedRequest
import nodecore.api.grpc.VeriBlockMessages.ListAllowedReply
import nodecore.api.grpc.VeriBlockMessages.GetPeerInfoRequest
import nodecore.api.grpc.VeriBlockMessages.GetPeerInfoReply
import nodecore.api.grpc.VeriBlockMessages.SignMessageRequest
import nodecore.api.grpc.VeriBlockMessages.SignMessageReply
import nodecore.api.grpc.VeriBlockMessages.BackupWalletRequest
import nodecore.api.grpc.VeriBlockMessages.BackupWalletReply
import nodecore.api.grpc.VeriBlockMessages.ImportWalletRequest
import nodecore.api.grpc.VeriBlockMessages.ImportWalletReply
import nodecore.api.grpc.VeriBlockMessages.GetNewAddressRequest
import nodecore.api.grpc.VeriBlockMessages.GetNewAddressReply
import nodecore.api.grpc.VeriBlockMessages.GetBlockTimesRequest
import nodecore.api.grpc.VeriBlockMessages.GetBlockTimesReply
import nodecore.api.grpc.VeriBlockMessages.StartSoloPoolRequest
import nodecore.api.grpc.VeriBlockMessages.StartSoloPoolReply
import nodecore.api.grpc.VeriBlockMessages.GetBlockchainsRequest
import nodecore.api.grpc.VeriBlockMessages.GetBlockchainsReply
import nodecore.api.grpc.VeriBlockMessages.SetTransactionFeeRequest
import nodecore.api.grpc.VeriBlockMessages.DumpPrivateKeyRequest
import nodecore.api.grpc.VeriBlockMessages.DumpPrivateKeyReply
import nodecore.api.grpc.VeriBlockMessages.ImportPrivateKeyRequest
import nodecore.api.grpc.VeriBlockMessages.ImportPrivateKeyReply
import nodecore.api.grpc.VeriBlockMessages.SubmitTransactionsRequest
import nodecore.api.grpc.VeriBlockMessages.ValidateAddressRequest
import nodecore.api.grpc.VeriBlockMessages.ValidateAddressReply
import nodecore.api.grpc.VeriBlockMessages.GenerateMultisigAddressRequest
import nodecore.api.grpc.VeriBlockMessages.GenerateMultisigAddressReply
import nodecore.api.grpc.VeriBlockMessages.MakeUnsignedMultisigTxRequest
import nodecore.api.grpc.VeriBlockMessages.MakeUnsignedMultisigTxReply
import nodecore.api.grpc.VeriBlockMessages.SubmitMultisigTxRequest
import nodecore.api.grpc.VeriBlockMessages.SubmitMultisigTxReply
import nodecore.api.grpc.VeriBlockMessages.GetTransactionsRequest
import nodecore.api.grpc.VeriBlockMessages.GetTransactionsReply
import nodecore.api.grpc.VeriBlockMessages.GetBlockTemplateRequest
import nodecore.api.grpc.VeriBlockMessages.GetBlockTemplateReply
import nodecore.api.grpc.VeriBlockMessages.GetSignatureIndexRequest
import nodecore.api.grpc.VeriBlockMessages.GetSignatureIndexReply
import nodecore.api.grpc.VeriBlockMessages.SetDefaultAddressRequest
import nodecore.api.grpc.VeriBlockMessages.SetDefaultAddressReply
import nodecore.api.grpc.VeriBlockMessages.GetLastBitcoinBlockRequest
import nodecore.api.grpc.VeriBlockMessages.GetLastBitcoinBlockReply
import nodecore.api.grpc.VeriBlockMessages.GetPendingTransactionsRequest
import nodecore.api.grpc.VeriBlockMessages.GetPendingTransactionsReply
import nodecore.api.grpc.VeriBlockMessages.GetStateInfoRequest
import nodecore.api.grpc.VeriBlockMessages.GetStateInfoReply
import nodecore.api.grpc.VeriBlockMessages.GetDiagnosticInfoRequest
import nodecore.api.grpc.VeriBlockMessages.GetDiagnosticInfoReply
import nodecore.api.grpc.VeriBlockMessages.TroubleshootPoPTransactionsRequest
import nodecore.api.grpc.VeriBlockMessages.TroubleshootPoPTransactionsReply
import nodecore.api.grpc.VeriBlockMessages.StopNodeCoreRequest
import nodecore.api.grpc.VeriBlockMessages.RefreshWalletCacheRequest
import nodecore.api.grpc.VeriBlockMessages.GetWalletTransactionsRequest
import nodecore.api.grpc.VeriBlockMessages.GetWalletTransactionsReply
import nodecore.api.grpc.VeriBlockMessages.PingReply
import nodecore.api.grpc.VeriBlockMessages
import nodecore.api.grpc.VeriBlockMessages.ClearBannedRequest
import nodecore.api.grpc.VeriBlockMessages.ListBannedRequest
import nodecore.api.grpc.VeriBlockMessages.ListBannedReply
import nodecore.api.grpc.VeriBlockMessages.ClearBannedMinersRequest
import nodecore.api.grpc.VeriBlockMessages.ListBannedMinersRequest
import nodecore.api.grpc.VeriBlockMessages.ListBannedMinersReply
import nodecore.api.grpc.VeriBlockMessages.EncryptWalletRequest
import nodecore.api.grpc.VeriBlockMessages.DecryptWalletRequest
import nodecore.api.grpc.VeriBlockMessages.UnlockWalletRequest
import nodecore.api.grpc.VeriBlockMessages.LockWalletRequest
import nodecore.api.grpc.VeriBlockMessages.DrainAddressRequest
import nodecore.api.grpc.VeriBlockMessages.DrainAddressReply
import nodecore.api.grpc.VeriBlockMessages.GetBalanceUnlockScheduleRequest
import nodecore.api.grpc.VeriBlockMessages.GetBalanceUnlockScheduleReply
import nodecore.api.grpc.VeriBlockMessages.GetPoolStateRequest
import nodecore.api.grpc.VeriBlockMessages.GetPoolStateReply
import nodecore.api.grpc.VeriBlockMessages.AbandonTransactionRequest
import nodecore.api.grpc.VeriBlockMessages.AbandonTransactionReply
import nodecore.api.grpc.VeriBlockMessages.RebroadcastTransactionRequest
import nodecore.api.grpc.VeriBlockMessages.RebroadcastTransactionReply
import nodecore.api.grpc.AdminRpcConfiguration
import nodecore.api.grpc.AdminGrpc
import nodecore.api.grpc.utilities.ChannelBuilder
import nodecore.cli.Configuration
import java.util.concurrent.TimeUnit

class AdminServiceClient(
    host: String,
    port: Int,
    transportType: EndpointTransportType,
    configuration: Configuration,
    password: String?
) : AdminService {

    private val rpcConfiguration = AdminRpcConfiguration().apply {
        nodeCoreHost = host
        nodeCorePort = port
        isSsl = transportType == EndpointTransportType.HTTPS
        certificateChainPath = configuration.certificateChainPath
        nodeCorePassword = password
    }
    private val channelBuilder = ChannelBuilder(rpcConfiguration)
    private val channel = channelBuilder.buildManagedChannel()
    private val blockingStub = AdminGrpc.newBlockingStub(
        channelBuilder.attachPasswordInterceptor(channel)
    ).withMaxInboundMessageSize(64 * 1024 * 1024).withMaxOutboundMessageSize(20 * 1024 * 1024)

    @Throws(InterruptedException::class)
    fun shutdown() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }

    override fun getPop(request: GetPopRequest): GetPopReply = blockingStub.getPop(request)

    override fun addNode(request: NodeRequest): ProtocolReply = blockingStub.addNode(request)

    override fun getInfo(request: GetInfoRequest): GetInfoReply = blockingStub.getInfo(request)

    override fun removeNode(request: NodeRequest): ProtocolReply = blockingStub.removeNode(request)

    override fun sendCoins(request: SendCoinsRequest): SendCoinsReply = blockingStub.sendCoins(request)

    override fun submitPop(request: SubmitPopRequest): ProtocolReply = blockingStub.submitPop(request)

    override fun getBlocks(request: GetBlocksRequest): GetBlocksReply = blockingStub.getBlocks(request)

    override fun getPoPEndorsementsInfo(request: GetPoPEndorsementsInfoRequest): GetPoPEndorsementsInfoReply = blockingStub.getPoPEndorsementsInfo(request)

    override fun getProtectedChildren(request: GetProtectedChildrenRequest): GetProtectedChildrenReply = blockingStub.getProtectedChildren(request)

    override fun getProtectingParents(request: GetProtectingParentsRequest): GetProtectingParentsReply = blockingStub.getProtectingParents(request)

    override fun startPool(request: StartPoolRequest): StartPoolReply = blockingStub.startPool(request)

    override fun setAllowed(request: SetAllowedRequest): ProtocolReply = blockingStub.setAllowed(request)

    override fun restartPoolWebServer(request: RestartPoolWebServerRequest): RestartPoolWebServerReply = blockingStub.restartPoolWebServer(request)

    override fun stopPool(request: StopPoolRequest): StopPoolReply = blockingStub.stopPool(request)

    override fun getHistory(request: GetHistoryRequest): GetHistoryReply = blockingStub.getHistory(request)

    override fun getBalance(request: GetBalanceRequest): GetBalanceReply = blockingStub.getBalance(request)

    override fun submitBlocks(request: SubmitBlocksRequest): ProtocolReply = blockingStub.submitBlocks(request)

    override fun clearAllowed(request: ClearAllowedRequest): ProtocolReply = blockingStub.clearAllowed(request)

    override fun listAllowed(request: ListAllowedRequest): ListAllowedReply = blockingStub.listAllowed(request)

    override fun getPeerInfo(request: GetPeerInfoRequest): GetPeerInfoReply = blockingStub.getPeerInfo(request)

    override fun signMessage(request: SignMessageRequest): SignMessageReply = blockingStub.signMessage(request)

    override fun backupWallet(request: BackupWalletRequest): BackupWalletReply = blockingStub.backupWallet(request)

    override fun importWallet(request: ImportWalletRequest): ImportWalletReply = blockingStub.importWallet(request)

    override fun getNewAddress(request: GetNewAddressRequest): GetNewAddressReply = blockingStub.getNewAddress(request)

    override fun getBlockTimes(request: GetBlockTimesRequest): GetBlockTimesReply = blockingStub.getBlockTimes(request)

    override fun startSoloPool(request: StartSoloPoolRequest): StartSoloPoolReply = blockingStub.startSoloPool(request)

    override fun getBlockchains(request: GetBlockchainsRequest): GetBlockchainsReply = blockingStub.getBlockchains(request)

    override fun setTransactionFee(request: SetTransactionFeeRequest): ProtocolReply = blockingStub.setTransactionFee(request)

    override fun dumpPrivateKey(request: DumpPrivateKeyRequest): DumpPrivateKeyReply = blockingStub.dumpPrivateKey(request)

    override fun importPrivateKey(request: ImportPrivateKeyRequest): ImportPrivateKeyReply = blockingStub.importPrivateKey(request)

    override fun submitTransactions(request: SubmitTransactionsRequest): ProtocolReply = blockingStub.submitTransactions(request)

    override fun validateAddress(request: ValidateAddressRequest): ValidateAddressReply = blockingStub.validateAddress(request)

    override fun generateMultisigAddress(request: GenerateMultisigAddressRequest): GenerateMultisigAddressReply = blockingStub.generateMultisigAddress(request)

    override fun makeUnsignedMultisigTx(request: MakeUnsignedMultisigTxRequest): MakeUnsignedMultisigTxReply = blockingStub.makeUnsignedMultisigTx(request)

    override fun submitMultisigTx(request: SubmitMultisigTxRequest): SubmitMultisigTxReply = blockingStub.submitMultisigTx(request)

    override fun getTransactions(request: GetTransactionsRequest): GetTransactionsReply = blockingStub.getTransactions(request)

    override fun getBlockTemplate(request: GetBlockTemplateRequest): GetBlockTemplateReply = blockingStub.getBlockTemplate(request)

    override fun getSignatureIndex(request: GetSignatureIndexRequest): GetSignatureIndexReply = blockingStub.getSignatureIndex(request)

    override fun setDefaultAddress(request: SetDefaultAddressRequest): SetDefaultAddressReply = blockingStub.setDefaultAddress(request)

    override fun getLastBitcoinBlock(request: GetLastBitcoinBlockRequest): GetLastBitcoinBlockReply = blockingStub.getLastBitcoinBlock(request)

    override fun getPendingTransactions(request: GetPendingTransactionsRequest): GetPendingTransactionsReply = blockingStub.getPendingTransactions(request)

    override fun getStateInfo(request: GetStateInfoRequest): GetStateInfoReply = blockingStub.getStateInfo(request)

    override fun getDiagnosticInfo(request: GetDiagnosticInfoRequest): GetDiagnosticInfoReply = blockingStub.getDiagnosticInfo(request)

    override fun troubleshootPoPTransactions(request: TroubleshootPoPTransactionsRequest): TroubleshootPoPTransactionsReply = blockingStub.troubleshootPoPTransactions(request)

    override fun stopNodeCore(request: StopNodeCoreRequest): ProtocolReply = blockingStub.stopNodeCore(request)

    override fun refreshWalletCache(request: RefreshWalletCacheRequest): ProtocolReply = blockingStub.refreshWalletCache(request)

    override fun getWalletTransactions(request: GetWalletTransactionsRequest): GetWalletTransactionsReply = blockingStub.getWalletTransactions(request)

    override fun connect(): PingReply = blockingStub.ping(VeriBlockMessages.PingRequest.getDefaultInstance())

    override fun clearBanned(request: ClearBannedRequest): ProtocolReply = blockingStub.clearBanned(request)

    override fun listBanned(request: ListBannedRequest): ListBannedReply = blockingStub.listBanned(request)

    override fun clearBannedMiners(request: ClearBannedMinersRequest): ProtocolReply = blockingStub.clearBannedMiners(request)

    override fun listBannedMiners(request: ListBannedMinersRequest): ListBannedMinersReply = blockingStub.listBannedMiners(request)

    override fun encryptWallet(request: EncryptWalletRequest): ProtocolReply = blockingStub.encryptWallet(request)

    override fun decryptWallet(request: DecryptWalletRequest): ProtocolReply = blockingStub.decryptWallet(request)

    override fun unlockWallet(request: UnlockWalletRequest): ProtocolReply = blockingStub.unlockWallet(request)

    override fun lockWallet(request: LockWalletRequest): ProtocolReply = blockingStub.lockWallet(request)

    override fun drainAddress(request: DrainAddressRequest): DrainAddressReply = blockingStub.drainAddress(request)

    override fun getBalanceUnlockSchedule(request: GetBalanceUnlockScheduleRequest): GetBalanceUnlockScheduleReply = blockingStub.getBalanceUnlockSchedule(request)

    override fun getPoolState(request: GetPoolStateRequest): GetPoolStateReply = blockingStub.getPoolState(request)

    override fun abandonTransactionRequest(request: AbandonTransactionRequest): AbandonTransactionReply = blockingStub.abandonTransaction(request)

    override fun rebroadcastTransactionRequest(request: RebroadcastTransactionRequest): RebroadcastTransactionReply = blockingStub.rebroadcastTransaction(request)
}
