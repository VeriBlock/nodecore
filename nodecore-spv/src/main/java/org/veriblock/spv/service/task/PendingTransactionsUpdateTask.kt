package org.veriblock.spv.net

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import nodecore.api.grpc.VeriBlockMessages
import nodecore.api.grpc.utilities.extensions.asAnyVbkHash
import nodecore.api.grpc.utilities.extensions.toByteString
import nodecore.api.grpc.utilities.extensions.toHex
import org.veriblock.core.crypto.asVbkTxId
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.debugError
import org.veriblock.core.utilities.debugWarn
import org.veriblock.spv.SpvContext
import org.veriblock.spv.service.OutputData
import org.veriblock.spv.service.TransactionData
import org.veriblock.spv.service.TransactionInfo
import org.veriblock.spv.service.TransactionType
import org.veriblock.spv.util.Threading.PEER_TABLE_SCOPE
import org.veriblock.spv.util.buildMessage
import org.veriblock.spv.util.invokeOnFailure
import org.veriblock.spv.util.launchWithFixedDelay

private val logger = createLogger {}

fun SpvContext.startPendingTransactionsUpdateTask() {
    PEER_TABLE_SCOPE.launchWithFixedDelay(5_000L, 20_000L) {
        requestPendingTransactions()
    }.invokeOnFailure { t ->
        logger.debugError(t) { "The pending transactions update task has failed" }
    }
}

suspend fun SpvContext.requestPendingTransactions() {
    val pendingTransactionIds = transactionManager.getPendingTransactionIds()
    try {
        for (txId in pendingTransactionIds) {
            val request = buildMessage {
                transactionRequest = VeriBlockMessages.GetTransactionRequest.newBuilder()
                    .setId(txId.bytes.toByteString())
                    .build()
            }
            peerTable.requestAllMessages(request)
                .map { it.transactionReply }
                .collect {
                    if (it.success) {
                        // tx exists
                        val info = it.transaction.toModel()
                        logger.info { "Tx=${txId} found: VBK:${info.blockNumber}:${info.blockHash}"}

                        transactionManager.onTransactionInfo(info)
                    } else {
                        // remote peer does not know about this tx.
                        logger.info { "Tx=${txId} NOT found"}

                        val transaction = transactionManager.getTransaction(txId)
                        if (transaction != null) {
                            // rebroadcast it
                            peerTable.advertise(transaction)
                        }

                        transactionManager.onMissingTransactionInfo(txId)
                    }
                }
        }
    } catch (e: Exception) {
        logger.debugWarn(e) { "Unable to request pending transactions" }
    }
}

private fun VeriBlockMessages.TransactionInfo.toModel() = TransactionInfo(
    confirmations = confirmations,
    transaction = transaction.toModel(),
    blockNumber = blockNumber,
    timestamp = timestamp,
    endorsedBlockHash = endorsedBlockHash.toHex(),
    bitcoinBlockHash = bitcoinBlockHash.toHex(),
    bitcoinTxId = bitcoinTxId.toHex(),
    bitcoinConfirmations = bitcoinConfirmations,
    blockHash = blockHash.asAnyVbkHash(),
    merklePath = merklePath
)

private fun VeriBlockMessages.Transaction.toModel() = TransactionData(
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

private fun VeriBlockMessages.Output.toModel() = OutputData(
    address = address.toHex(),
    amount = amount
)
