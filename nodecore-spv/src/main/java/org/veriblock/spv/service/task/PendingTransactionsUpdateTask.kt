package org.veriblock.spv.net


import nodecore.api.grpc.RpcGetTransactionRequest
import nodecore.api.grpc.RpcOutput
import nodecore.api.grpc.RpcTransaction
import nodecore.api.grpc.RpcTransactionInfo
import nodecore.api.grpc.utilities.extensions.toByteString
import nodecore.api.grpc.utilities.extensions.toHex
import nodecore.p2p.buildMessage
import org.veriblock.core.crypto.asVbkTxId
import org.veriblock.core.invokeOnFailure
import org.veriblock.core.launchWithFixedDelay
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.debugError
import org.veriblock.core.utilities.debugWarn
import org.veriblock.spv.SpvContext
import org.veriblock.spv.service.OutputData
import org.veriblock.spv.service.TransactionData
import org.veriblock.spv.service.TransactionInfo
import org.veriblock.spv.service.TransactionType
import org.veriblock.spv.service.advertise
import org.veriblock.spv.util.Threading.PEER_TABLE_SCOPE
import kotlin.system.exitProcess

private val logger = createLogger {}

fun SpvContext.startPendingTransactionsUpdateTask() {
    PEER_TABLE_SCOPE.launchWithFixedDelay(5_000L, 20_000L) {
        requestPendingTransactions()
    }.invokeOnFailure { t ->
        logger.debugError(t) { "The pending transactions update task has failed" }
        exitProcess(1)
    }
}

suspend fun SpvContext.requestPendingTransactions() {
    val pendingTransactionIds = pendingTransactionContainer.getPendingTransactionIds()
    for (txId in pendingTransactionIds) {
        try {
            val request = buildMessage {
                transactionRequest = RpcGetTransactionRequest.newBuilder()
                    .setId(txId.bytes.toByteString())
                    .build()
            }
            val response = peerTable.requestMessage(request)
            if (response.transactionReply.success) {
                pendingTransactionContainer.updateTransactionInfo(response.transactionReply.transaction.toModel())
            } else {
                val transaction = pendingTransactionContainer.getTransaction(txId)
                if (transaction != null) {
                    peerTable.advertise(transaction)
                }
            }
        } catch (e: Exception) {
            logger.debugWarn(e) { "Unable to request pending transaction $txId" }
        }
    }
}

private fun RpcTransactionInfo.toModel() = TransactionInfo(
    confirmations = confirmations,
    transaction = transaction.toModel(),
    blockNumber = blockNumber,
    timestamp = timestamp,
    endorsedBlockHash = endorsedBlockHash.toHex(),
    bitcoinBlockHash = bitcoinBlockHash.toHex(),
    bitcoinTxId = bitcoinTxId.toHex(),
    bitcoinConfirmations = bitcoinConfirmations,
    blockHash = blockHash.toHex(),
    merklePath = merklePath
)

private fun RpcTransaction.toModel() = TransactionData(
    type = TransactionType.valueOf(type.name),
    sourceAddress = sourceAddress.toHex(),
    sourceAmount = sourceAmount,
    outputs = outputsList.map { it.toModel() },
    transactionFee = transactionFee,
    data = data.toHex(),
    bitcoinTransaction = bitcoinTransaction.toHex(),
    endorsedBlockHeader = endorsedBlockHeader.toHex(),
    bitcoinBlockHeaderOfProof = "",
    merklePath = merklePath,
    contextBitcoinBlockHeaders = listOf(),
    timestamp = timestamp,
    size = size,
    txId = txId.toByteArray().asVbkTxId()
)

private fun RpcOutput.toModel() = OutputData(
    address = address.toHex(),
    amount = amount
)
