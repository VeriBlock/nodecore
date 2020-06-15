// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.wallet

import nodecore.api.grpc.VeriBlockMessages
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.crypto.VBlakeHash
import org.veriblock.sdk.models.asCoin
import veriblock.model.Output
import veriblock.model.OutputFactory.create
import veriblock.model.StandardAddress
import veriblock.model.StandardTransaction
import veriblock.model.TransactionMeta
import veriblock.model.TransactionMeta.MetaState.Companion.forNumber
import veriblock.model.asLightAddress

class WalletProtobufSerializer {
    fun deserializeOutputs(output: List<VeriBlockMessages.Output>): List<Output> {
        return output.map {
            deserialize(it)
        }
    }

    fun deserialize(output: VeriBlockMessages.Output): Output {
        return Output(
            StandardAddress(output.address.toString()), output.amount.asCoin()
        )
    }

    fun deserialize(transactions: List<Protos.Transaction>): List<StandardTransaction> {
        return transactions.map {
            deserialize(it)
        }
    }

    fun deserialize(proto: Protos.Transaction): StandardTransaction {
        val tx = StandardTransaction(Sha256Hash.wrap(proto.txId.toByteArray()))
        tx.inputAddress = proto.input.address.asLightAddress()
        tx.inputAmount = proto.input.amount.asCoin()
        for (o in proto.outputsList) {
            tx.addOutput(create(o.address, o.amount))
        }
        tx.setSignatureIndex(proto.signatureIndex)
        tx.data = proto.data.toByteArray()
        //TODO: tx.setMerklePath(deserialize(proto.getMerkleBranch()));
        tx.transactionMeta = deserialize(proto.meta)
        return tx
    }

    fun deserialize(proto: Protos.TransactionMeta): TransactionMeta {
        val meta = TransactionMeta(
            Sha256Hash.wrap(proto.txId.toByteArray())
        )
        meta.setState(forNumber(proto.stateValue))
        meta.appearsInBestChainBlock = VBlakeHash.wrap(proto.appearsInBestChainBlock.toByteArray())
        proto.appearsInBlocksList.forEach { bytes ->
            meta.addBlockAppearance(VBlakeHash.wrap(bytes.toByteArray()))
        }
        meta.appearsAtChainHeight = proto.appearsAtHeight
        meta.depth = proto.depth
        return meta
    }
}
