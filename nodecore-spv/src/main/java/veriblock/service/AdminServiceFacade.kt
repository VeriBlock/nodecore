package veriblock.service

import io.grpc.stub.StreamObserver
import nodecore.api.grpc.AdminGrpc.AdminImplBase
import nodecore.api.grpc.VeriBlockMessages.BackupWalletReply
import nodecore.api.grpc.VeriBlockMessages.BackupWalletRequest
import nodecore.api.grpc.VeriBlockMessages.CreateAltChainEndorsementReply
import nodecore.api.grpc.VeriBlockMessages.CreateAltChainEndorsementRequest
import nodecore.api.grpc.VeriBlockMessages.DecryptWalletRequest
import nodecore.api.grpc.VeriBlockMessages.DumpPrivateKeyReply
import nodecore.api.grpc.VeriBlockMessages.DumpPrivateKeyRequest
import nodecore.api.grpc.VeriBlockMessages.EncryptWalletRequest
import nodecore.api.grpc.VeriBlockMessages.GetBalanceReply
import nodecore.api.grpc.VeriBlockMessages.GetBalanceRequest
import nodecore.api.grpc.VeriBlockMessages.GetLastBitcoinBlockReply
import nodecore.api.grpc.VeriBlockMessages.GetLastBitcoinBlockRequest
import nodecore.api.grpc.VeriBlockMessages.GetNewAddressReply
import nodecore.api.grpc.VeriBlockMessages.GetNewAddressRequest
import nodecore.api.grpc.VeriBlockMessages.GetSignatureIndexReply
import nodecore.api.grpc.VeriBlockMessages.GetSignatureIndexRequest
import nodecore.api.grpc.VeriBlockMessages.GetStateInfoReply
import nodecore.api.grpc.VeriBlockMessages.GetStateInfoRequest
import nodecore.api.grpc.VeriBlockMessages.GetTransactionsReply
import nodecore.api.grpc.VeriBlockMessages.GetTransactionsRequest
import nodecore.api.grpc.VeriBlockMessages.ImportPrivateKeyReply
import nodecore.api.grpc.VeriBlockMessages.ImportPrivateKeyRequest
import nodecore.api.grpc.VeriBlockMessages.ImportWalletReply
import nodecore.api.grpc.VeriBlockMessages.ImportWalletRequest
import nodecore.api.grpc.VeriBlockMessages.LockWalletRequest
import nodecore.api.grpc.VeriBlockMessages.PingReply
import nodecore.api.grpc.VeriBlockMessages.PingRequest
import nodecore.api.grpc.VeriBlockMessages.ProtocolReply
import nodecore.api.grpc.VeriBlockMessages.SendCoinsReply
import nodecore.api.grpc.VeriBlockMessages.SendCoinsRequest
import nodecore.api.grpc.VeriBlockMessages.SubmitTransactionsRequest
import nodecore.api.grpc.VeriBlockMessages.UnlockWalletRequest

class AdminServiceFacade(private val adminApiService: AdminApiService) : AdminImplBase() {
    override fun getNewAddress(
        request: GetNewAddressRequest,
        responseObserver: StreamObserver<GetNewAddressReply>
    ) {
        responseObserver.onNext(adminApiService.getNewAddress(request))
        responseObserver.onCompleted()
    }

    override fun backupWallet(
        request: BackupWalletRequest,
        responseObserver: StreamObserver<BackupWalletReply>
    ) {
        responseObserver.onNext(adminApiService.backupWallet(request))
        responseObserver.onCompleted()
    }

    override fun importWallet(
        request: ImportWalletRequest,
        responseObserver: StreamObserver<ImportWalletReply>
    ) {
        responseObserver.onNext(adminApiService.importWallet(request))
        responseObserver.onCompleted()
    }

    override fun sendCoins(
        request: SendCoinsRequest,
        responseObserver: StreamObserver<SendCoinsReply>
    ) {
        responseObserver.onNext(adminApiService.sendCoins(request))
        responseObserver.onCompleted()
    }

    override fun getSignatureIndex(
        request: GetSignatureIndexRequest,
        responseObserver: StreamObserver<GetSignatureIndexReply>
    ) {
        responseObserver.onNext(adminApiService.getSignatureIndex(request))
        responseObserver.onCompleted()
    }

    override fun dumpPrivateKey(
        request: DumpPrivateKeyRequest,
        responseObserver: StreamObserver<DumpPrivateKeyReply>
    ) {
        responseObserver.onNext(adminApiService.dumpPrivateKey(request))
        responseObserver.onCompleted()
    }

    override fun importPrivateKey(
        request: ImportPrivateKeyRequest,
        responseObserver: StreamObserver<ImportPrivateKeyReply>
    ) {
        responseObserver.onNext(adminApiService.importPrivateKey(request))
        responseObserver.onCompleted()
    }

    override fun submitTransactions(
        request: SubmitTransactionsRequest,
        responseObserver: StreamObserver<ProtocolReply>
    ) {
        responseObserver.onNext(adminApiService.submitTransactions(request))
        responseObserver.onCompleted()
    }

    override fun ping(request: PingRequest, responseObserver: StreamObserver<PingReply>) {
        responseObserver.onNext(PingReply.getDefaultInstance())
        responseObserver.onCompleted()
    }

    override fun getStateInfo(request: GetStateInfoRequest, responseObserver: StreamObserver<GetStateInfoReply>) {
        responseObserver.onNext(adminApiService.getStateInfo())
        responseObserver.onCompleted()
    }

    override fun encryptWallet(request: EncryptWalletRequest, responseObserver: StreamObserver<ProtocolReply>) {
        responseObserver.onNext(adminApiService.encryptWallet(request))
        responseObserver.onCompleted()
    }

    override fun decryptWallet(request: DecryptWalletRequest, responseObserver: StreamObserver<ProtocolReply>) {
        responseObserver.onNext(adminApiService.decryptWallet(request))
        responseObserver.onCompleted()
    }

    override fun unlockWallet(request: UnlockWalletRequest, responseObserver: StreamObserver<ProtocolReply>) {
        responseObserver.onNext(adminApiService.unlockWallet(request))
        responseObserver.onCompleted()
    }

    override fun lockWallet(request: LockWalletRequest, responseObserver: StreamObserver<ProtocolReply>) {
        responseObserver.onNext(adminApiService.lockWallet(request))
        responseObserver.onCompleted()
    }

    override fun getBalance(request: GetBalanceRequest, responseObserver: StreamObserver<GetBalanceReply>) {
        responseObserver.onNext(adminApiService.getBalance(request))
        responseObserver.onCompleted()
    }

    override fun createAltChainEndorsement(
        request: CreateAltChainEndorsementRequest, responseObserver: StreamObserver<CreateAltChainEndorsementReply>
    ) {
        responseObserver.onNext(adminApiService.createAltChainEndorsement(request))
        responseObserver.onCompleted()
    }

    override fun getLastBitcoinBlock(
        request: GetLastBitcoinBlockRequest,
        responseObserver: StreamObserver<GetLastBitcoinBlockReply>
    ) {
        responseObserver.onNext(adminApiService.getLastBitcoinBlock(request))
        responseObserver.onCompleted()
    }

    override fun getTransactions(
        request: GetTransactionsRequest,
        responseObserver: StreamObserver<GetTransactionsReply>
    ) {
        responseObserver.onNext(adminApiService.getTransactions(request))
        responseObserver.onCompleted()
    }

}
