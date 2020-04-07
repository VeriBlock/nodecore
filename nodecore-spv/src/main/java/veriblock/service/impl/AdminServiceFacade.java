package veriblock.service.impl;

import io.grpc.stub.StreamObserver;
import nodecore.api.grpc.AdminGrpc;
import nodecore.api.grpc.VeriBlockMessages;
import veriblock.service.AdminApiService;

public class AdminServiceFacade extends AdminGrpc.AdminImplBase {

    private final AdminApiService adminApiService;

    public AdminServiceFacade(AdminApiService adminApiService) {
        this.adminApiService = adminApiService;
    }

    @Override
    public void getNewAddress(VeriBlockMessages.GetNewAddressRequest request,
            StreamObserver<VeriBlockMessages.GetNewAddressReply> responseObserver) {
        responseObserver.onNext(adminApiService.getNewAddress(request));
        responseObserver.onCompleted();
    }


    @Override
    public void backupWallet(VeriBlockMessages.BackupWalletRequest request,
                             StreamObserver<VeriBlockMessages.BackupWalletReply> responseObserver) {
        responseObserver.onNext(adminApiService.backupWallet(request));
        responseObserver.onCompleted();
    }

    @Override
    public void importWallet(VeriBlockMessages.ImportWalletRequest request,
                             StreamObserver<VeriBlockMessages.ImportWalletReply> responseObserver) {
        responseObserver.onNext(adminApiService.importWallet(request));
        responseObserver.onCompleted();
    }


    @Override
    public void sendCoins(
            VeriBlockMessages.SendCoinsRequest request,
            StreamObserver<VeriBlockMessages.SendCoinsReply> responseObserver) {
        responseObserver.onNext(adminApiService.sendCoins(request));
        responseObserver.onCompleted();
    }


    @Override
    public void getSignatureIndex(
            VeriBlockMessages.GetSignatureIndexRequest request,
            StreamObserver<VeriBlockMessages.GetSignatureIndexReply> responseObserver) {
        responseObserver.onNext(adminApiService.getSignatureIndex(request));
        responseObserver.onCompleted();
    }

    @Override
    public void dumpPrivateKey(
            VeriBlockMessages.DumpPrivateKeyRequest request,
            StreamObserver<VeriBlockMessages.DumpPrivateKeyReply> responseObserver) {
        responseObserver.onNext(adminApiService.dumpPrivateKey(request));
        responseObserver.onCompleted();
    }

    @Override
    public void importPrivateKey(
            VeriBlockMessages.ImportPrivateKeyRequest request,
            StreamObserver<VeriBlockMessages.ImportPrivateKeyReply> responseObserver) {
        responseObserver.onNext(adminApiService.importPrivateKey(request));
        responseObserver.onCompleted();
    }

    @Override
    public void submitTransactions(
            VeriBlockMessages.SubmitTransactionsRequest request,
            StreamObserver<VeriBlockMessages.ProtocolReply> responseObserver) {
        responseObserver.onNext(adminApiService.submitTransactions(request));
        responseObserver.onCompleted();
    }

    @Override
    public void ping(VeriBlockMessages.PingRequest request, StreamObserver<VeriBlockMessages.PingReply> responseObserver) {
        responseObserver.onNext(VeriBlockMessages.PingReply.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void getStateInfo(VeriBlockMessages.GetStateInfoRequest request, StreamObserver<VeriBlockMessages.GetStateInfoReply> responseObserver) {
        responseObserver.onNext(adminApiService.getStateInfo(request));
        responseObserver.onCompleted();
    }

    @Override
    public void encryptWallet(VeriBlockMessages.EncryptWalletRequest request, StreamObserver<VeriBlockMessages.ProtocolReply> responseObserver) {
        responseObserver.onNext(adminApiService.encryptWallet(request));
        responseObserver.onCompleted();
    }

    @Override
    public void decryptWallet(VeriBlockMessages.DecryptWalletRequest request, StreamObserver<VeriBlockMessages.ProtocolReply> responseObserver) {
        responseObserver.onNext(adminApiService.decryptWallet(request));
        responseObserver.onCompleted();
    }

    @Override
    public void unlockWallet(VeriBlockMessages.UnlockWalletRequest request, StreamObserver<VeriBlockMessages.ProtocolReply> responseObserver) {
        responseObserver.onNext(adminApiService.unlockWallet(request));
        responseObserver.onCompleted();
    }

    @Override
    public void lockWallet(VeriBlockMessages.LockWalletRequest request, StreamObserver<VeriBlockMessages.ProtocolReply> responseObserver) {
        responseObserver.onNext(adminApiService.lockWallet(request));
        responseObserver.onCompleted();
    }

    @Override
    public void getBalance(VeriBlockMessages.GetBalanceRequest request, StreamObserver<VeriBlockMessages.GetBalanceReply> responseObserver) {
        responseObserver.onNext(adminApiService.getBalance(request));
        responseObserver.onCompleted();
    }

    @Override
    public void createAltChainEndorsement(
        VeriBlockMessages.CreateAltChainEndorsementRequest request, StreamObserver<VeriBlockMessages.CreateAltChainEndorsementReply> responseObserver
    ) {
        responseObserver.onNext(adminApiService.createAltChainEndorsement(request));
        responseObserver.onCompleted();
    }

    @Override
    public void getLastBitcoinBlock(
        VeriBlockMessages.GetLastBitcoinBlockRequest request,
        StreamObserver<VeriBlockMessages.GetLastBitcoinBlockReply> responseObserver
    ) {
        responseObserver.onNext(adminApiService.getLastBitcoinBlock(request));
        responseObserver.onCompleted();
    }

    @Override
    public void getTransactions(
        VeriBlockMessages.GetTransactionsRequest request,
        StreamObserver<VeriBlockMessages.GetTransactionsReply> responseObserver
    ) {
        responseObserver.onNext(adminApiService.getTransactions(request));
        responseObserver.onCompleted();
    }
}
