// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.lite.wallet

import com.google.protobuf.CodedInputStream
import com.google.protobuf.CodedOutputStream
import nodecore.api.grpc.utilities.ByteStringUtility
import org.veriblock.lite.core.Context
import org.veriblock.lite.core.TransactionMeta
import org.veriblock.sdk.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class TransactionMonitorProtobufSerializer {

    @Throws(IOException::class)
    fun OutputStream.writeTransactionMonitor(transactionMonitor: TransactionMonitor) {
        val codedOutputStream = CodedOutputStream.newInstance(this)
        transactionMonitor.toProto().writeTo(codedOutputStream)
        codedOutputStream.flush()
    }

    private fun TransactionMonitor.toProto(): Protos.TransactionMonitor = Protos.TransactionMonitor.newBuilder().also {
        it.network = Context.networkParameters.network
        it.address = address.toString()

        for (transaction in getTransactions()) {
            it.addTransactions(transaction.toProto())
        }
    }.build()

    private fun WalletTransaction.toProto(): Protos.WalletTransaction = Protos.WalletTransaction.newBuilder().also {
        it.txId = ByteStringUtility.bytesToByteString(id.bytes)
        it.input = buildTransactionInput(sourceAddress, sourceAmount)

        for (output in outputs) {
            it.addOutputs(buildTransactionOutput(output.address, output.amount))
        }

        it.signatureIndex = signatureIndex
        it.signature = ByteStringUtility.bytesToByteString(signature)
        it.publicKey = ByteStringUtility.bytesToByteString(publicKey)
        it.data = ByteStringUtility.bytesToByteString(data)
        merklePath?.let { merklePath ->
            it.merkleBranch = merklePath.toProto()
        }
        it.meta = transactionMeta.toProto()
    }.build()

    private fun VeriBlockMerklePath.toProto(): Protos.MerkleBranch = Protos.MerkleBranch.newBuilder().also {
        it.subject = ByteStringUtility.bytesToByteString(subject.bytes)
        it.index = index

        for (hash in layers) {
            it.addMerklePathHashes(ByteStringUtility.bytesToByteString(hash.bytes))
        }
        it.merkleSubTreeValue = treeIndex
    }.build()

    private fun TransactionMeta.toProto(): Protos.TransactionMeta = Protos.TransactionMeta.newBuilder().also {
        it.txId = ByteStringUtility.bytesToByteString(txId.bytes)
        it.state = Protos.TransactionMeta.MetaState.forNumber(state.value)
        appearsInBestChainBlock?.let { appearsInBestChainBlock ->
            it.appearsInBestChainBlock = ByteStringUtility.bytesToByteString(appearsInBestChainBlock.bytes)
        }

        for (blockHash in getAppearsInBlock()) {
            it.addAppearsInBlocks(ByteStringUtility.bytesToByteString(blockHash.bytes))
        }

        it.appearsAtHeight = appearsAtChainHeight
        it.depth = depth
    }.build()

    private fun buildTransactionInput(address: Address, amount: Coin): Protos.TransactionInput {
        return Protos.TransactionInput.newBuilder()
            .setAddress(address.toString())
            .setAmount(amount.atomicUnits)
            .build()
    }

    private fun buildTransactionOutput(address: Address, amount: Coin): Protos.TransactionOutput {
        return Protos.TransactionOutput.newBuilder()
            .setAddress(address.toString())
            .setAmount(amount.atomicUnits)
            .build()
    }

    @Throws(IOException::class)
    fun InputStream.readTransactionMonitor(): TransactionMonitor {
        val codedInput = CodedInputStream.newInstance(this)
        return Protos.TransactionMonitor.parseFrom(codedInput).toModel()
    }

    private fun Protos.TransactionMonitor.toModel(): TransactionMonitor {
        check(Context.networkParameters.network == network) {
            "Network ${Context.networkParameters.network} attempting to read wallet for $network"
        }
        return TransactionMonitor(
            Address(address),
            transactionsList.toModel()
        )
    }

    private fun List<Protos.WalletTransaction>.toModel(): List<WalletTransaction> = map {
        it.toModel()
    }

    private fun Protos.WalletTransaction.toModel(): WalletTransaction = WalletTransaction(
        0x01.toByte(),
        Address(input.address),
        Coin.valueOf(input.amount),
        outputsList.map { transactionOutput ->
            Output.of(transactionOutput.address, transactionOutput.amount)
        },
        signatureIndex,
        data.toByteArray(),
        signature.toByteArray(),
        publicKey.toByteArray(),
        Context.networkParameters.transactionPrefix,
        meta.toModel()
    ).apply {
        merklePath = merkleBranch.toModel()
    }

    private fun Protos.MerkleBranch.toModel(): VeriBlockMerklePath = VeriBlockMerklePath(
        "$merkleSubTreeValue:$index:${Sha256Hash.wrap(subject.toByteArray())}:" +
            merklePathHashesList.joinToString(":") { Sha256Hash.wrap(it.toByteArray()).toString() }
    )

    private fun Protos.TransactionMeta.toModel(): TransactionMeta = TransactionMeta(
        Sha256Hash.wrap(txId.toByteArray())
    ).also {
        it.setState(TransactionMeta.MetaState.forNumber(stateValue))
        it.appearsInBestChainBlock = VBlakeHash.wrap(appearsInBestChainBlock.toByteArray())

        appearsInBlocksList.forEach { bytes -> it.addBlockAppearance(VBlakeHash.wrap(bytes.toByteArray())) }

        it.appearsAtChainHeight = appearsAtHeight
        it.depth = depth
    }
}
