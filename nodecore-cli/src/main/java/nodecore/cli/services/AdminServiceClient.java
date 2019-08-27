// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.services;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import nodecore.api.grpc.*;
import nodecore.api.grpc.utilities.ChannelBuilder;
import nodecore.cli.contracts.AdminService;
import nodecore.cli.contracts.Configuration;
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

        blockingStub = AdminGrpc.newBlockingStub(interceptorChannel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    @Override
    public GetPopReply getPop(GetPopRequest request) {
        return blockingStub.getPop(request);
    }

    @Override
    public ProtocolReply addNode(NodeRequest request) {
        return blockingStub.addNode(request);
    }

    @Override
    public GetInfoReply getInfo(GetInfoRequest request) {
        return blockingStub.getInfo(request);
    }

    @Override
    public ProtocolReply removeNode(NodeRequest request) {
        return blockingStub.removeNode(request);
    }

    @Override
    public SendCoinsReply sendCoins(SendCoinsRequest request) {
        return blockingStub.sendCoins(request);
    }

    @Override
    public ProtocolReply submitPop(SubmitPopRequest request) {
        return blockingStub.submitPop(request);
    }

    @Override
    public GetBlocksReply getBlocks(GetBlocksRequest request) {
        return blockingStub.getBlocks(request);
    }

    @Override
    public GetPoPEndorsementsInfoReply getPoPEndorsementsInfo(GetPoPEndorsementsInfoRequest request) {
        return blockingStub.getPoPEndorsementsInfo(request);
    }

    @Override
    public GetProtectedChildrenReply getProtectedChildren(GetProtectedChildrenRequest request) {
        return blockingStub.getProtectedChildren(request);
    }

    @Override
    public GetProtectingParentsReply getProtectingParents(GetProtectingParentsRequest request) {
        return blockingStub.getProtectingParents(request);
    }

    @Override
    public StartPoolReply startPool(StartPoolRequest request) {
        return blockingStub.startPool(request);
    }

    @Override
    public ProtocolReply setAllowed(SetAllowedRequest request) {
        return blockingStub.setAllowed(request);
    }

    @Override
    public RestartPoolWebServerReply restartPoolWebServer(RestartPoolWebServerRequest request) {
        return blockingStub.restartPoolWebServer(request);
    }

    @Override
    public StopPoolReply stopPool(StopPoolRequest request) {
        return blockingStub.stopPool(request);
    }

    @Override
    public GetHistoryReply getHistory(GetHistoryRequest request) {
        return blockingStub.getHistory(request);
    }

    @Override
    public GetBalanceReply getBalance(GetBalanceRequest request) {
        return blockingStub.getBalance(request);
    }

    @Override
    public ProtocolReply submitBlocks(SubmitBlocksRequest request) {
        return blockingStub.submitBlocks(request);
    }

    @Override
    public ProtocolReply clearAllowed(ClearAllowedRequest request) {
        return blockingStub.clearAllowed(request);
    }

    @Override
    public ListAllowedReply listAllowed(ListAllowedRequest request) {
        return blockingStub.listAllowed(request);
    }

    @Override
    public GetPeerInfoReply getPeerInfo(GetPeerInfoRequest request) {
        return blockingStub.getPeerInfo(request);
    }

    @Override
    public SignMessageReply signMessage(SignMessageRequest request) {
        return blockingStub.signMessage(request);
    }

    @Override
    public BackupWalletReply backupWallet(BackupWalletRequest request) {
        return blockingStub.backupWallet(request);
    }

    @Override
    public ImportWalletReply importWallet(ImportWalletRequest request) {
        return blockingStub.importWallet(request);
    }

    @Override
    public GetNewAddressReply getNewAddress(GetNewAddressRequest request) {
        return blockingStub.getNewAddress(request);
    }

    @Override
    public GetBlockTimesReply getBlockTimes(GetBlockTimesRequest request) {
        return blockingStub.getBlockTimes(request);
    }

    @Override
    public StartSoloPoolReply startSoloPool(StartSoloPoolRequest request) {
        return blockingStub.startSoloPool(request);
    }

    @Override
    public GetBlockchainsReply getBlockchains(GetBlockchainsRequest request) {
        return blockingStub.getBlockchains(request);
    }

    @Override
    public ProtocolReply setTransactionFee(SetTransactionFeeRequest request) {
        return blockingStub.setTransactionFee(request);
    }

    @Override
    public DumpPrivateKeyReply dumpPrivateKey(DumpPrivateKeyRequest request) {
        return blockingStub.dumpPrivateKey(request);
    }

    @Override
    public ImportPrivateKeyReply importPrivateKey(ImportPrivateKeyRequest request) {
        return blockingStub.importPrivateKey(request);
    }

    @Override
    public ProtocolReply submitTransactions(SubmitTransactionsRequest request) {
        return blockingStub.submitTransactions(request);
    }

    @Override
    public ValidateAddressReply validateAddress(ValidateAddressRequest request) {
        return blockingStub.validateAddress(request);
    }

    @Override
    public GenerateMultisigAddressReply generateMultisigAddress(GenerateMultisigAddressRequest request) {
        return blockingStub.generateMultisigAddress(request);
    }

    @Override
    public MakeUnsignedMultisigTxReply makeUnsignedMultisigTx(MakeUnsignedMultisigTxRequest request) {
        return blockingStub.makeUnsignedMultisigTx(request);
    }

    @Override
    public SubmitMultisigTxReply submitMultisigTx(SubmitMultisigTxRequest request) {
        return blockingStub.submitMultisigTx(request);
    }

    @Override
    public GetTransactionsReply getTransactions(GetTransactionsRequest request) {
        return blockingStub.getTransactions(request);
    }

    @Override
    public GetBlockTemplateReply getBlockTemplate(GetBlockTemplateRequest request) {
        return blockingStub.getBlockTemplate(request);
    }

    @Override
    public GetSignatureIndexReply getSignatureIndex(GetSignatureIndexRequest request) {
        return blockingStub.getSignatureIndex(request);
    }

    @Override
    public SetDefaultAddressReply setDefaultAddress(SetDefaultAddressRequest request) {
        return blockingStub.setDefaultAddress(request);
    }

    @Override
    public GetLastBitcoinBlockReply getLastBitcoinBlock(GetLastBitcoinBlockRequest request) {
        return blockingStub.getLastBitcoinBlock(request);
    }

    @Override
    public GetPendingTransactionsReply getPendingTransactions(GetPendingTransactionsRequest request) {
        return blockingStub.getPendingTransactions(request);
    }

    @Override
    public GetStateInfoReply getStateInfo(GetStateInfoRequest request) {
        return blockingStub.getStateInfo(request);
    }

    @Override
    public GetDiagnosticInfoReply getDiagnosticInfo(GetDiagnosticInfoRequest request) {
        return blockingStub.getDiagnosticInfo(request);
    }

    @Override
    public TroubleshootPoPTransactionsReply troubleshootPoPTransactions(TroubleshootPoPTransactionsRequest request) {
        return blockingStub.troubleshootPoPTransactions(request);
    }

    @Override
    public ProtocolReply stopNodeCore(StopNodeCoreRequest request) {
        return blockingStub.stopNodeCore(request);
    }

    @Override
    public ProtocolReply refreshWalletCache(RefreshWalletCacheRequest request) {
        return blockingStub.refreshWalletCache(request);
    }

    @Override
    public GetWalletTransactionsReply getWalletTransactions(GetWalletTransactionsRequest request) {
        return blockingStub.getWalletTransactions(request);
    }

    @Override
    public PingReply connect() {
        return blockingStub.ping(PingRequest.getDefaultInstance());
    }

    @Override
    public ProtocolReply clearBanned(ClearBannedRequest request) {
        return blockingStub.clearBanned(request);
    }

    @Override
    public ListBannedReply listBanned(ListBannedRequest request) {
        return blockingStub.listBanned(request);
    }

    @Override
    public ProtocolReply clearBannedMiners(ClearBannedMinersRequest request) {
        return blockingStub.clearBannedMiners(request);
    }

    @Override
    public ListBannedMinersReply listBannedMiners(ListBannedMinersRequest request) {
        return blockingStub.listBannedMiners(request);
    }

    @Override
    public ProtocolReply encryptWallet(EncryptWalletRequest request) {
        return blockingStub.encryptWallet(request);
    }

    @Override
    public ProtocolReply decryptWallet(DecryptWalletRequest request) {
        return blockingStub.decryptWallet(request);
    }

    @Override
    public ProtocolReply unlockWallet(UnlockWalletRequest request) {
        return blockingStub.unlockWallet(request);
    }

    @Override
    public ProtocolReply lockWallet(LockWalletRequest request) {
        return blockingStub.lockWallet(request);
    }

    @Override
    public DrainAddressReply drainAddress(DrainAddressRequest request) {
        return blockingStub.drainAddress(request);
    }

    @Override
    public GetBalanceUnlockScheduleReply getBalanceUnlockSchedule(GetBalanceUnlockScheduleRequest request) {
        return blockingStub.getBalanceUnlockSchedule(request);
    }

    @Override
    public GetPoolStateReply getPoolState(GetPoolStateRequest request) {
        return blockingStub.getPoolState(request);
    }
}
