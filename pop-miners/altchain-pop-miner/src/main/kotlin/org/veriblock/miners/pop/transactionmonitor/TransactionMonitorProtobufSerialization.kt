// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.transactionmonitor


import kotlinx.serialization.protobuf.ProtoBuf
import org.veriblock.miners.pop.core.ApmContext
import org.veriblock.miners.pop.core.TransactionMeta
import org.veriblock.miners.pop.proto.TxmonProto
import org.veriblock.sdk.models.Address
import org.veriblock.sdk.models.Output
import org.veriblock.core.crypto.asAnyVbkHash
import org.veriblock.core.crypto.asVbkTxId
import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.miners.pop.net.SpvGateway
import org.veriblock.sdk.models.VeriBlockMerklePath
import org.veriblock.sdk.models.asCoin
import org.veriblock.sdk.services.SerializeDeserializeService
import java.io.InputStream
import java.io.OutputStream

fun OutputStream.writeTransactionMonitor(transactionMonitor: TransactionMonitor) {
    val data = ProtoBuf.encodeToByteArray(TxmonProto.TransactionMonitor.serializer(), transactionMonitor.toProto())
    write(data)
}

fun InputStream.readTransactionMonitor(context: ApmContext, gateway: SpvGateway): TransactionMonitor {
    val data = ProtoBuf.decodeFromByteArray(TxmonProto.TransactionMonitor.serializer(), readBytes())
    return data.toModel(context, gateway)
}

private fun TransactionMonitor.toProto() = TxmonProto.TransactionMonitor(
    network = context.networkParameters.name,
    address = address.toString(),
    transactions = getTransactions().map { it.toProto() }
)

private fun WalletTransaction.toProto() = TxmonProto.WalletTransaction(
    txId = id.bytes,
    input = TxmonProto.TransactionInput(sourceAddress.toString(), sourceAmount.atomicUnits),
    outputs = outputs.map { TxmonProto.TransactionOutput(it.address.toString(), it.amount.atomicUnits) },
    signatureIndex = signatureIndex,
    signature = signature,
    publicKey = publicKey,
    data = publicationData?.let { SerializeDeserializeService.serialize(it) } ?: ByteArray(0),
    merkleBranch = merklePath?.toProto() ?: TxmonProto.MerkleBranch(),
    meta = transactionMeta.toProto()
)

private fun VeriBlockMerklePath.toProto() = TxmonProto.MerkleBranch(
    subject = subject.bytes,
    index = index,
    merklePathHashes = layers.map { it.bytes },
    merkleSubTree = treeIndex
)

private fun TransactionMeta.toProto() = TxmonProto.TransactionMeta(
    txId = txId.bytes,
    state = state.value,
    appearsInBestChainBlock = appearsInBestChainBlock?.bytes ?: ByteArray(0),
    appearsInBlocks = getAppearsInBlock().map { it.bytes },
    appearsAtHeight = appearsAtChainHeight,
    depth = depth
)

private fun TxmonProto.TransactionMonitor.toModel(context: ApmContext, gateway: SpvGateway): TransactionMonitor {
    check(context.networkParameters.name == network) {
        "Network ${context.networkParameters.name} attempting to read ${context.vbkTokenName} wallet for $network"
    }
    return TransactionMonitor(
        context,
        gateway,
        Address(address),
        transactions.map {
            it.toModel(context)
        }
    )
}

private fun TxmonProto.WalletTransaction.toModel(context: ApmContext): WalletTransaction = WalletTransaction(
    0x01.toByte(),
    Address(input.address),
    input.amount.asCoin(),
    outputs.map { transactionOutput ->
        Output.of(transactionOutput.address, transactionOutput.amount)
    },
    signatureIndex,
    SerializeDeserializeService.parsePublicationData(data),
    signature,
    publicKey,
    context.networkParameters.transactionPrefix,
    meta.toModel()
).apply {
    if (merkleBranch.subject.isNotEmpty()) {
        merklePath = merkleBranch.toModel()
    }
}

private fun TxmonProto.MerkleBranch.toModel(): VeriBlockMerklePath = VeriBlockMerklePath(
    "$merkleSubTree:$index:${subject.toHex()}:" +
        merklePathHashes.joinToString(":") { it.toHex() }
)

private fun TxmonProto.TransactionMeta.toModel(): TransactionMeta = TransactionMeta(
    txId.asVbkTxId()
).also {
    it.setState(TransactionMeta.MetaState.forNumber(state))
    if (appearsInBestChainBlock.isNotEmpty()) {
        it.appearsInBestChainBlock = appearsInBestChainBlock.asAnyVbkHash()
    }

    appearsInBlocks.forEach { bytes -> it.addBlockAppearance(bytes.asAnyVbkHash()) }

    it.appearsAtChainHeight = appearsAtHeight
    it.depth = depth
}
