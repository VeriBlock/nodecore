// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.services;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import nodecore.api.grpc.AdminGrpc;
import nodecore.api.grpc.AdminRpcConfiguration;
import nodecore.api.grpc.VeriBlockMessages;
import nodecore.api.grpc.utilities.ChannelBuilder;
import nodecore.cli.Configuration;
import nodecore.cli.contracts.AdminService;
import nodecore.cli.contracts.EndpointTransportType;

import javax.net.ssl.SSLException;
import java.util.concurrent.TimeUnit;

public class AdminServiceClient implements AdminService {
    private final ManagedChannel channel;
    private final AdminGrpc.AdminBlockingStub blockingStub;

    public AdminServiceClient(String host, int port, EndpointTransportType transportType, Configuration configuration, String password) throws SSLException {
        AdminRpcConfiguration rpcConfiguration = new AdminRpcConfiguration();
        rpcConfiguration.setNodeCoreHost(host);
        rpcConfiguration.setNodeCorePort(port);
        rpcConfiguration.setSsl(transportType == EndpointTransportType.HTTPS);
        rpcConfiguration.setCertificateChainPath(configuration.getCertificateChainPath());
        rpcConfiguration.setNodeCorePassword(password);

        ChannelBuilder channelBuilder = new ChannelBuilder(rpcConfiguration);
        channel = channelBuilder.buildManagedChannel();

        Channel interceptorChannel = channelBuilder.attachPasswordInterceptor(channel);

        blockingStub =
            AdminGrpc.newBlockingStub(interceptorChannel).withMaxInboundMessageSize(20 * 1024 * 1024).withMaxOutboundMessageSize(20 * 1024 * 1024);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    @Override
    public VeriBlockMessages.GetPopReply getPop(VeriBlockMessages.GetPopRequest request) {
        return blockingStub.getPop(request);
    }

    @Override
    public VeriBlockMessages.ProtocolReply addNode(VeriBlockMessages.NodeRequest request) {
        return blockingStub.addNode(request);
    }

    @Override
    public VeriBlockMessages.GetInfoReply getInfo(VeriBlockMessages.GetInfoRequest request) {
        return blockingStub.getInfo(request);
    }

    @Override
    public VeriBlockMessages.ProtocolReply removeNode(VeriBlockMessages.NodeRequest request) {
        return blockingStub.removeNode(request);
    }

    @Override
    public VeriBlockMessages.SendCoinsReply sendCoins(VeriBlockMessages.SendCoinsRequest request) {
        return blockingStub.sendCoins(request);
    }

    @Override
    public VeriBlockMessages.ProtocolReply submitPop(VeriBlockMessages.SubmitPopRequest request) {
        return blockingStub.submitPop(request);
    }

    @Override
    public VeriBlockMessages.GetBlocksReply getBlocks(VeriBlockMessages.GetBlocksRequest request) {
        return blockingStub.getBlocks(request);
    }

    @Override
    public VeriBlockMessages.GetPoPEndorsementsInfoReply getPoPEndorsementsInfo(VeriBlockMessages.GetPoPEndorsementsInfoRequest request) {
        return blockingStub.getPoPEndorsementsInfo(request);
    }

    @Override
    public VeriBlockMessages.GetProtectedChildrenReply getProtectedChildren(VeriBlockMessages.GetProtectedChildrenRequest request) {
        return blockingStub.getProtectedChildren(request);
    }

    @Override
    public VeriBlockMessages.GetProtectingParentsReply getProtectingParents(VeriBlockMessages.GetProtectingParentsRequest request) {
        return blockingStub.getProtectingParents(request);
    }

    @Override
    public VeriBlockMessages.StartPoolReply startPool(VeriBlockMessages.StartPoolRequest request) {
        return blockingStub.startPool(request);
    }

    @Override
    public VeriBlockMessages.ProtocolReply setAllowed(VeriBlockMessages.SetAllowedRequest request) {
        return blockingStub.setAllowed(request);
    }

    @Override
    public VeriBlockMessages.RestartPoolWebServerReply restartPoolWebServer(VeriBlockMessages.RestartPoolWebServerRequest request) {
        return blockingStub.restartPoolWebServer(request);
    }

    @Override
    public VeriBlockMessages.StopPoolReply stopPool(VeriBlockMessages.StopPoolRequest request) {
        return blockingStub.stopPool(request);
    }

    @Override
    public VeriBlockMessages.GetHistoryReply getHistory(VeriBlockMessages.GetHistoryRequest request) {
        return blockingStub.getHistory(request);
    }

    @Override
    public VeriBlockMessages.GetBalanceReply getBalance(VeriBlockMessages.GetBalanceRequest request) {
        return blockingStub.getBalance(request);
    }

    @Override
    public VeriBlockMessages.ProtocolReply submitBlocks(VeriBlockMessages.SubmitBlocksRequest request) {
        return blockingStub.submitBlocks(request);
    }

    @Override
    public VeriBlockMessages.ProtocolReply clearAllowed(VeriBlockMessages.ClearAllowedRequest request) {
        return blockingStub.clearAllowed(request);
    }

    @Override
    public VeriBlockMessages.ListAllowedReply listAllowed(VeriBlockMessages.ListAllowedRequest request) {
        return blockingStub.listAllowed(request);
    }

    @Override
    public VeriBlockMessages.GetPeerInfoReply getPeerInfo(VeriBlockMessages.GetPeerInfoRequest request) {
        return blockingStub.getPeerInfo(request);
    }

    @Override
    public VeriBlockMessages.SignMessageReply signMessage(VeriBlockMessages.SignMessageRequest request) {
        return blockingStub.signMessage(request);
    }

    @Override
    public VeriBlockMessages.BackupWalletReply backupWallet(VeriBlockMessages.BackupWalletRequest request) {
        return blockingStub.backupWallet(request);
    }

    @Override
    public VeriBlockMessages.ImportWalletReply importWallet(VeriBlockMessages.ImportWalletRequest request) {
        return blockingStub.importWallet(request);
    }

    @Override
    public VeriBlockMessages.GetNewAddressReply getNewAddress(VeriBlockMessages.GetNewAddressRequest request) {
        return blockingStub.getNewAddress(request);
    }

    @Override
    public VeriBlockMessages.GetBlockTimesReply getBlockTimes(VeriBlockMessages.GetBlockTimesRequest request) {
        return blockingStub.getBlockTimes(request);
    }

    @Override
    public VeriBlockMessages.StartSoloPoolReply startSoloPool(VeriBlockMessages.StartSoloPoolRequest request) {
        return blockingStub.startSoloPool(request);
    }

    @Override
    public VeriBlockMessages.GetBlockchainsReply getBlockchains(VeriBlockMessages.GetBlockchainsRequest request) {
        return blockingStub.getBlockchains(request);
    }

    @Override
    public VeriBlockMessages.ProtocolReply setTransactionFee(VeriBlockMessages.SetTransactionFeeRequest request) {
        return blockingStub.setTransactionFee(request);
    }

    @Override
    public VeriBlockMessages.DumpPrivateKeyReply dumpPrivateKey(VeriBlockMessages.DumpPrivateKeyRequest request) {
        return blockingStub.dumpPrivateKey(request);
    }

    @Override
    public VeriBlockMessages.ImportPrivateKeyReply importPrivateKey(VeriBlockMessages.ImportPrivateKeyRequest request) {
        return blockingStub.importPrivateKey(request);
    }

    @Override
    public VeriBlockMessages.ProtocolReply submitTransactions(VeriBlockMessages.SubmitTransactionsRequest request) {
        return blockingStub.submitTransactions(request);
    }

    @Override
    public VeriBlockMessages.ValidateAddressReply validateAddress(VeriBlockMessages.ValidateAddressRequest request) {
        return blockingStub.validateAddress(request);
    }

    @Override
    public VeriBlockMessages.GenerateMultisigAddressReply generateMultisigAddress(VeriBlockMessages.GenerateMultisigAddressRequest request) {
        return blockingStub.generateMultisigAddress(request);
    }

    @Override
    public VeriBlockMessages.MakeUnsignedMultisigTxReply makeUnsignedMultisigTx(VeriBlockMessages.MakeUnsignedMultisigTxRequest request) {
        return blockingStub.makeUnsignedMultisigTx(request);
    }

    @Override
    public VeriBlockMessages.SubmitMultisigTxReply submitMultisigTx(VeriBlockMessages.SubmitMultisigTxRequest request) {
        return blockingStub.submitMultisigTx(request);
    }

    @Override
    public VeriBlockMessages.GetTransactionsReply getTransactions(VeriBlockMessages.GetTransactionsRequest request) {
        return blockingStub.getTransactions(request);
    }

    @Override
    public VeriBlockMessages.GetBlockTemplateReply getBlockTemplate(VeriBlockMessages.GetBlockTemplateRequest request) {
        return blockingStub.getBlockTemplate(request);
    }

    @Override
    public VeriBlockMessages.GetSignatureIndexReply getSignatureIndex(VeriBlockMessages.GetSignatureIndexRequest request) {
        return blockingStub.getSignatureIndex(request);
    }

    @Override
    public VeriBlockMessages.SetDefaultAddressReply setDefaultAddress(VeriBlockMessages.SetDefaultAddressRequest request) {
        return blockingStub.setDefaultAddress(request);
    }

    @Override
    public VeriBlockMessages.GetLastBitcoinBlockReply getLastBitcoinBlock(VeriBlockMessages.GetLastBitcoinBlockRequest request) {
        return blockingStub.getLastBitcoinBlock(request);
    }

    @Override
    public VeriBlockMessages.GetPendingTransactionsReply getPendingTransactions(VeriBlockMessages.GetPendingTransactionsRequest request) {
        return blockingStub.getPendingTransactions(request);
    }

    @Override
    public VeriBlockMessages.GetStateInfoReply getStateInfo(VeriBlockMessages.GetStateInfoRequest request) {
        return blockingStub.getStateInfo(request);
    }

    @Override
    public VeriBlockMessages.GetDiagnosticInfoReply getDiagnosticInfo(VeriBlockMessages.GetDiagnosticInfoRequest request) {
        return blockingStub.getDiagnosticInfo(request);
    }

    @Override
    public VeriBlockMessages.TroubleshootPoPTransactionsReply troubleshootPoPTransactions(VeriBlockMessages.TroubleshootPoPTransactionsRequest request) {
        return blockingStub.troubleshootPoPTransactions(request);
    }

    @Override
    public VeriBlockMessages.ProtocolReply stopNodeCore(VeriBlockMessages.StopNodeCoreRequest request) {
        return blockingStub.stopNodeCore(request);
    }

    @Override
    public VeriBlockMessages.ProtocolReply refreshWalletCache(VeriBlockMessages.RefreshWalletCacheRequest request) {
        return blockingStub.refreshWalletCache(request);
    }

    @Override
    public VeriBlockMessages.GetWalletTransactionsReply getWalletTransactions(VeriBlockMessages.GetWalletTransactionsRequest request) {
        return blockingStub.getWalletTransactions(request);
    }

    @Override
    public VeriBlockMessages.PingReply connect() {
        return blockingStub.ping(VeriBlockMessages.PingRequest.getDefaultInstance());
    }

    @Override
    public VeriBlockMessages.ProtocolReply clearBanned(VeriBlockMessages.ClearBannedRequest request) {
        return blockingStub.clearBanned(request);
    }

    @Override
    public VeriBlockMessages.ListBannedReply listBanned(VeriBlockMessages.ListBannedRequest request) {
        return blockingStub.listBanned(request);
    }

    @Override
    public VeriBlockMessages.ProtocolReply clearBannedMiners(VeriBlockMessages.ClearBannedMinersRequest request) {
        return blockingStub.clearBannedMiners(request);
    }

    @Override
    public VeriBlockMessages.ListBannedMinersReply listBannedMiners(VeriBlockMessages.ListBannedMinersRequest request) {
        return blockingStub.listBannedMiners(request);
    }

    @Override
    public VeriBlockMessages.ProtocolReply encryptWallet(VeriBlockMessages.EncryptWalletRequest request) {
        return blockingStub.encryptWallet(request);
    }

    @Override
    public VeriBlockMessages.ProtocolReply decryptWallet(VeriBlockMessages.DecryptWalletRequest request) {
        return blockingStub.decryptWallet(request);
    }

    @Override
    public VeriBlockMessages.ProtocolReply unlockWallet(VeriBlockMessages.UnlockWalletRequest request) {
        return blockingStub.unlockWallet(request);
    }

    @Override
    public VeriBlockMessages.ProtocolReply lockWallet(VeriBlockMessages.LockWalletRequest request) {
        return blockingStub.lockWallet(request);
    }

    @Override
    public VeriBlockMessages.DrainAddressReply drainAddress(VeriBlockMessages.DrainAddressRequest request) {
        return blockingStub.drainAddress(request);
    }

    @Override
    public VeriBlockMessages.GetBalanceUnlockScheduleReply getBalanceUnlockSchedule(VeriBlockMessages.GetBalanceUnlockScheduleRequest request) {
        return blockingStub.getBalanceUnlockSchedule(request);
    }

    @Override
    public VeriBlockMessages.GetPoolStateReply getPoolState(VeriBlockMessages.GetPoolStateRequest request) {
        return blockingStub.getPoolState(request);
    }

    @Override
    public VeriBlockMessages.AbandonTransactionReply abandonTransactionRequest(VeriBlockMessages.AbandonTransactionRequest request) {
        return blockingStub.abandonTransaction(request);
    }
}
