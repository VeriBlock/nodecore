package org.veriblock.spv.util

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import nodecore.api.grpc.VeriBlockMessages
import org.veriblock.core.utilities.AsyncEvent
import org.veriblock.sdk.models.Address
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.models.asCoin
import org.veriblock.spv.model.LedgerContext
import org.veriblock.spv.model.LedgerValue
import org.veriblock.spv.model.StandardTransaction
import org.veriblock.spv.model.TransactionMeta
import org.veriblock.spv.net.SpvPeer
import org.veriblock.spv.util.Threading.EVENT_EXECUTOR

object SpvEventBus {

    val peerConnectedEvent = AsyncEvent<SpvPeer>("Peer Connected", EVENT_EXECUTOR)
    val peerDisconnectedEvent = AsyncEvent<SpvPeer>("Peer Disconnected", EVENT_EXECUTOR)
    val messageReceivedEvent = AsyncEvent<MessageReceivedEvent>("Message Received", EVENT_EXECUTOR)

    val addressStateUpdatedEvent = AsyncEvent<AddressStateChangeEvent>("Address State Updated", EVENT_EXECUTOR)

    val pendingTransactionDownloadedEvent = AsyncEvent<StandardTransaction>("Pending Transaction Downloaded", EVENT_EXECUTOR)

    val transactionStateChangedEvent = AsyncEvent<TransactionMeta>("Transaction State Changed", EVENT_EXECUTOR)
    val transactionDepthChangedEvent = AsyncEvent<TransactionMeta>("Transaction Depth Changed", EVENT_EXECUTOR)

    // Block Events
    val newBestBlockEvent = AsyncEvent<VeriBlockBlock>("New Best Block", Threading.LISTENER_THREAD)
    val newBlockFlow = MutableSharedFlow<VeriBlockBlock>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    //val blockChainReorganizedEvent = AsyncEvent<BlockChainReorganizedEventData>("Blockchain Reorganized", Threading.LISTENER_THREAD)
}

data class MessageReceivedEvent(
    val peer: SpvPeer,
    val message: VeriBlockMessages.Event
)

data class AddressStateChangeEvent(
    val address: Address,
    val previousState: LedgerValue,
    val newState: LedgerValue
) {
    override fun toString(): String {
        return when {
            previousState.signatureIndex == -1L ->
                "$address balance: ${newState.availableAtomicUnits.asCoin()}"
            previousState.availableAtomicUnits < newState.availableAtomicUnits ->
                "$address received ${(newState.availableAtomicUnits - previousState.availableAtomicUnits).asCoin()} VBK. New balance: ${newState.availableAtomicUnits.asCoin()} VBK"
            previousState.availableAtomicUnits > newState.availableAtomicUnits ->
                "$address paid ${(previousState.availableAtomicUnits - newState.availableAtomicUnits).asCoin()} VBK. New balance: ${newState.availableAtomicUnits.asCoin()} VBK"
            else ->
                "$address balance has changed: ${newState.availableAtomicUnits.asCoin()}"
        }
    }
}
