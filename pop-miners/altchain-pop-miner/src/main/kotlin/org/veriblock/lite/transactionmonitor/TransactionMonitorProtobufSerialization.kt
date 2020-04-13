// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.lite.transactionmonitor


import kotlinx.serialization.protobuf.ProtoBuf
import org.veriblock.lite.core.Context
import org.veriblock.lite.core.TransactionMeta
import org.veriblock.lite.proto.TxmonProto
import org.veriblock.sdk.models.Address
import org.veriblock.sdk.models.Coin
import org.veriblock.sdk.models.Output
import org.veriblock.sdk.models.Sha256Hash
import org.veriblock.sdk.models.VBlakeHash
import org.veriblock.sdk.models.VeriBlockMerklePath
import org.veriblock.sdk.services.SerializeDeserializeService
import java.io.InputStream
import java.io.OutputStream

fun OutputStream.writeTransactionMonitor(transactionMonitor: TransactionMonitor) {
    val data = ProtoBuf.dump(TxmonProto.TransactionMonitor.serializer(), transactionMonitor.toProto())
    write(data)
}

fun InputStream.readTransactionMonitor(context: Context): TransactionMonitor {
    val data = ProtoBuf.load(TxmonProto.TransactionMonitor.serializer(), readBytes())
    return data.toModel(context)
}

private fun TransactionMonitor.toProto() = TxmonProto.TransactionMonitor(
    network = context.networkParameters.network,
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
    data = SerializeDeserializeService.serialize(publicationData),
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

private fun TxmonProto.TransactionMonitor.toModel(context: Context): TransactionMonitor {
    check(context.networkParameters.network == network) {
        "Network ${context.networkParameters.network} attempting to read ${context.vbkTokenName} wallet for $network"
    }
    return TransactionMonitor(
        context,
        Address(address),
        transactions.map {
            it.toModel(context)
        }
    )
}

private fun TxmonProto.WalletTransaction.toModel(context: Context): WalletTransaction = WalletTransaction(
    0x01.toByte(),
    Address(input.address),
    Coin.valueOf(input.amount),
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
    "$merkleSubTree:$index:${Sha256Hash.wrap(subject)}:" +
        merklePathHashes.joinToString(":") { Sha256Hash.wrap(it).toString() }
)

private fun TxmonProto.TransactionMeta.toModel(): TransactionMeta = TransactionMeta(
    Sha256Hash.wrap(txId)
).also {
    it.setState(TransactionMeta.MetaState.forNumber(state))
    if (appearsInBestChainBlock.isNotEmpty()) {
        it.appearsInBestChainBlock = VBlakeHash.wrap(appearsInBestChainBlock)
    }

    appearsInBlocks.forEach { bytes -> it.addBlockAppearance(VBlakeHash.wrap(bytes)) }

    it.appearsAtChainHeight = appearsAtHeight
    it.depth = depth
}
