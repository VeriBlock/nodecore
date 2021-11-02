// VeriBlock NodeCore CLI
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.services

import nodecore.api.grpc.AdminGrpc
import nodecore.api.grpc.AdminRpcConfiguration
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
import nodecore.api.grpc.RpcGetPopEndorsementsInfoReply
import nodecore.api.grpc.RpcGetPopEndorsementsInfoRequest
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
import nodecore.api.grpc.RpcPingRequest
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
import nodecore.api.grpc.RpcTroubleshootPopTransactionsReply
import nodecore.api.grpc.RpcTroubleshootPopTransactionsRequest
import nodecore.api.grpc.RpcUnlockWalletRequest
import nodecore.api.grpc.RpcValidateAddressReply
import nodecore.api.grpc.RpcValidateAddressRequest
import nodecore.api.grpc.utilities.ChannelBuilder
import nodecore.cli.Configuration
import nodecore.cli.contracts.AdminService
import nodecore.cli.contracts.EndpointTransportType
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

    override fun getPop(request: RpcGetPopRequest): RpcGetPopReply = blockingStub.getPop(request)

    override fun addNode(request: RpcNodeRequest): RpcProtocolReply = blockingStub.addNode(request)

    override fun getInfo(request: RpcGetInfoRequest): RpcGetInfoReply = blockingStub.getInfo(request)

    override fun removeNode(request: RpcNodeRequest): RpcProtocolReply = blockingStub.removeNode(request)

    override fun sendCoins(request: RpcSendCoinsRequest): RpcSendCoinsReply = blockingStub.sendCoins(request)

    override fun submitPop(request: RpcSubmitPopRequest): RpcProtocolReply = blockingStub.submitPop(request)

    override fun getBlocks(request: RpcGetBlocksRequest): RpcGetBlocksReply = blockingStub.getBlocks(request)

    override fun getPopEndorsementsInfo(request: RpcGetPopEndorsementsInfoRequest): RpcGetPopEndorsementsInfoReply = blockingStub.getPopEndorsementsInfo(request)

    override fun getProtectedChildren(request: RpcGetProtectedChildrenRequest): RpcGetProtectedChildrenReply = blockingStub.getProtectedChildren(request)

    override fun getProtectingParents(request: RpcGetProtectingParentsRequest): RpcGetProtectingParentsReply = blockingStub.getProtectingParents(request)

    override fun startPool(request: RpcStartPoolRequest): RpcStartPoolReply = blockingStub.startPool(request)

    override fun setAllowed(request: RpcSetAllowedRequest): RpcProtocolReply = blockingStub.setAllowed(request)

    override fun restartPoolWebServer(request: RpcRestartPoolWebServerRequest): RpcRestartPoolWebServerReply = blockingStub.restartPoolWebServer(request)

    override fun stopPool(request: RpcStopPoolRequest): RpcStopPoolReply = blockingStub.stopPool(request)

    override fun getHistory(request: RpcGetHistoryRequest): RpcGetHistoryReply = blockingStub.getHistory(request)

    override fun getBalance(request: RpcGetBalanceRequest): RpcGetBalanceReply = blockingStub.getBalance(request)

    override fun submitBlocks(request: RpcSubmitBlocksRequest): RpcProtocolReply = blockingStub.submitBlocks(request)

    override fun clearAllowed(request: RpcClearAllowedRequest): RpcProtocolReply = blockingStub.clearAllowed(request)

    override fun listAllowed(request: RpcListAllowedRequest): RpcListAllowedReply = blockingStub.listAllowed(request)

    override fun getPeerInfo(request: RpcGetPeerInfoRequest): RpcGetPeerInfoReply = blockingStub.getPeerInfo(request)

    override fun signMessage(request: RpcSignMessageRequest): RpcSignMessageReply = blockingStub.signMessage(request)

    override fun backupWallet(request: RpcBackupWalletRequest): RpcBackupWalletReply = blockingStub.backupWallet(request)

    override fun importWallet(request: RpcImportWalletRequest): RpcImportWalletReply = blockingStub.importWallet(request)

    override fun getNewAddress(request: RpcGetNewAddressRequest): RpcGetNewAddressReply = blockingStub.getNewAddress(request)

    override fun getBlockTimes(request: RpcGetBlockTimesRequest): RpcGetBlockTimesReply = blockingStub.getBlockTimes(request)

    override fun startSoloPool(request: RpcStartSoloPoolRequest): RpcStartSoloPoolReply = blockingStub.startSoloPool(request)

    override fun getBlockchains(request: RpcGetBlockchainsRequest): RpcGetBlockchainsReply = blockingStub.getBlockchains(request)

    override fun setTransactionFee(request: RpcSetTransactionFeeRequest): RpcProtocolReply = blockingStub.setTransactionFee(request)

    override fun dumpPrivateKey(request: RpcDumpPrivateKeyRequest): RpcDumpPrivateKeyReply = blockingStub.dumpPrivateKey(request)

    override fun importPrivateKey(request: RpcImportPrivateKeyRequest): RpcImportPrivateKeyReply = blockingStub.importPrivateKey(request)

    override fun submitTransactions(request: RpcSubmitTransactionsRequest): RpcProtocolReply = blockingStub.submitTransactions(request)

    override fun validateAddress(request: RpcValidateAddressRequest): RpcValidateAddressReply = blockingStub.validateAddress(request)

    override fun generateMultisigAddress(request: RpcGenerateMultisigAddressRequest): RpcGenerateMultisigAddressReply = blockingStub.generateMultisigAddress(request)

    override fun makeUnsignedMultisigTx(request: RpcMakeUnsignedMultisigTxRequest): RpcMakeUnsignedMultisigTxReply = blockingStub.makeUnsignedMultisigTx(request)

    override fun submitMultisigTx(request: RpcSubmitMultisigTxRequest): RpcSubmitMultisigTxReply = blockingStub.submitMultisigTx(request)

    override fun getTransactions(request: RpcGetTransactionsRequest): RpcGetTransactionsReply = blockingStub.getTransactions(request)

    override fun getBlockTemplate(request: RpcGetBlockTemplateRequest): RpcGetBlockTemplateReply = blockingStub.getBlockTemplate(request)

    override fun getSignatureIndex(request: RpcGetSignatureIndexRequest): RpcGetSignatureIndexReply = blockingStub.getSignatureIndex(request)

    override fun setDefaultAddress(request: RpcSetDefaultAddressRequest): RpcSetDefaultAddressReply = blockingStub.setDefaultAddress(request)

    override fun getLastBitcoinBlock(request: RpcGetLastBitcoinBlockRequest): RpcGetLastBitcoinBlockReply = blockingStub.getLastBitcoinBlock(request)

    override fun getPendingTransactions(request: RpcGetPendingTransactionsRequest): RpcGetPendingTransactionsReply = blockingStub.getPendingTransactions(request)

    override fun getStateInfo(request: RpcGetStateInfoRequest): RpcGetStateInfoReply = blockingStub.getStateInfo(request)

    override fun getDiagnosticInfo(request: RpcGetDiagnosticInfoRequest): RpcGetDiagnosticInfoReply = blockingStub.getDiagnosticInfo(request)

    override fun troubleshootPopTransactions(request: RpcTroubleshootPopTransactionsRequest): RpcTroubleshootPopTransactionsReply = blockingStub.troubleshootPopTransactions(request)

    override fun stopNodeCore(request: RpcStopNodeCoreRequest): RpcProtocolReply = blockingStub.stopNodeCore(request)

    override fun refreshWalletCache(request: RpcRefreshWalletCacheRequest): RpcProtocolReply = blockingStub.refreshWalletCache(request)

    override fun getWalletTransactions(request: RpcGetWalletTransactionsRequest): RpcGetWalletTransactionsReply = blockingStub.getWalletTransactions(request)

    override fun connect(): RpcPingReply = blockingStub.ping(RpcPingRequest.getDefaultInstance())

    override fun clearBanned(request: RpcClearBannedRequest): RpcProtocolReply = blockingStub.clearBanned(request)

    override fun listBanned(request: RpcListBannedRequest): RpcListBannedReply = blockingStub.listBanned(request)

    override fun clearBannedMiners(request: RpcClearBannedMinersRequest): RpcProtocolReply = blockingStub.clearBannedMiners(request)

    override fun listBannedMiners(request: RpcListBannedMinersRequest): RpcListBannedMinersReply = blockingStub.listBannedMiners(request)

    override fun encryptWallet(request: RpcEncryptWalletRequest): RpcProtocolReply = blockingStub.encryptWallet(request)

    override fun decryptWallet(request: RpcDecryptWalletRequest): RpcProtocolReply = blockingStub.decryptWallet(request)

    override fun unlockWallet(request: RpcUnlockWalletRequest): RpcProtocolReply = blockingStub.unlockWallet(request)

    override fun lockWallet(request: RpcLockWalletRequest): RpcProtocolReply = blockingStub.lockWallet(request)

    override fun drainAddress(request: RpcDrainAddressRequest): RpcDrainAddressReply = blockingStub.drainAddress(request)

    override fun getBalanceUnlockSchedule(request: RpcGetBalanceUnlockScheduleRequest): RpcGetBalanceUnlockScheduleReply = blockingStub.getBalanceUnlockSchedule(request)

    override fun getPoolState(request: RpcGetPoolStateRequest): RpcGetPoolStateReply = blockingStub.getPoolState(request)

    override fun abandonTransactionRequest(request: RpcAbandonTransactionRequest): RpcAbandonTransactionReply = blockingStub.abandonTransaction(request)

    override fun rebroadcastTransactionRequest(request: RpcRebroadcastTransactionRequest): RpcRebroadcastTransactionReply = blockingStub.rebroadcastTransaction(request)
}
