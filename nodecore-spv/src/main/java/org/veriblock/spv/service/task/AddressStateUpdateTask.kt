package org.veriblock.spv.net

import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.mapNotNull
import nodecore.api.grpc.RpcLedgerProofReply
import nodecore.api.grpc.RpcLedgerProofRequest
import nodecore.api.grpc.utilities.extensions.asBase58ByteString
import nodecore.api.grpc.utilities.extensions.toBase58
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.debugError
import org.veriblock.core.utilities.debugWarn
import org.veriblock.spv.SpvContext
import org.veriblock.spv.model.mapper.LedgerProofReplyMapper
import org.veriblock.spv.util.Threading.PEER_TABLE_SCOPE
import org.veriblock.spv.util.buildMessage
import org.veriblock.spv.util.invokeOnFailure
import org.veriblock.spv.util.launchWithFixedDelay
import org.veriblock.spv.validator.LedgerProofReplyValidator

private val logger = createLogger {}

fun SpvContext.startAddressStateUpdateTask() {
    PEER_TABLE_SCOPE.launchWithFixedDelay(10_000L, 30_000L) {
        updateAddressState()
    }.invokeOnFailure { t ->
        logger.debugError(t) { "The address state update task has failed" }
    }
}

suspend fun SpvContext.updateAddressState() {
    try {
        val addresses = addressManager.all.map { it.hash }
        if (addresses.isEmpty()) {
            logger.error { "No addresses in addressManager..." }
            return
        }

        val request = buildMessage {
            ledgerProofRequest = RpcLedgerProofRequest.newBuilder().apply {
                for (address in addresses) {
                    addAddresses(address.asBase58ByteString())
                }
            }.build()
        }

        peerTable.requestAllMessages(request)
            .mapNotNull { it.ledgerProofReply.proofsList }
            .flatMapMerge { it.asFlow() }
            // handle only ADDRESS_EXISTS replies
            .filter {
                when (it.result) {
                    RpcLedgerProofReply.Status.ADDRESS_EXISTS -> true
                    else -> {
                        logger.debug { "Received LedgerProofReply with status=${it.result}" }
                        false
                    }
                }
            }
            // handle responses with known addresses
            .filter { addressManager.contains(it.address.toBase58()) }
            // handle only cryptographically valid responses
            .filter { LedgerProofReplyValidator.validate(it) }
            // mapper returns null if it can't deserialize block header, so
            // handle responses with valid blocks
            .mapNotNull { LedgerProofReplyMapper.map(it, trustPeerHashes) }
            // block-of-proof may be new or known. if known or new and it connects, this will return true.
            .filter { blockchain.acceptBlock(it.block) }
            // handle responses with block-of-proofs that are on active chain
            .filter { blockchain.isOnActiveChain(it.block.hash) }
            .collect { remote ->
                val local = getAddressState(remote.address)

                // update local address state view if remote's block is higher and on active chain
                if (remote.block.height > local.block.height) {
                    setAddressState(remote)
                }
            }
    } catch (e: Exception) {
        logger.debugWarn(e) { "Unable to request address state" }
    }
}
