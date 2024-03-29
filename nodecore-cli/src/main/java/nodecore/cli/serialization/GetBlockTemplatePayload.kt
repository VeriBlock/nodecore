// VeriBlock NodeCore CLI
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import com.google.gson.annotations.SerializedName
import nodecore.api.grpc.RpcGetBlockTemplateReply
import org.veriblock.sdk.extensions.toHex
import org.veriblock.core.utilities.Utility

class GetBlockTemplatePayload(
    reply: RpcGetBlockTemplateReply
) {
    val target = reply.target

    @SerializedName("size_limit")
    val sizeLimit = reply.sizeLimit

    @SerializedName("block_height")
    val blockHeight = reply.blockHeight

    @SerializedName("coinbase_value")
    val coinbaseValue = Utility.formatAtomicLongWithDecimal(reply.coinbaseValue)

    @SerializedName("minimum_timestamp")
    val minimumTimestamp = reply.minimumTimestamp.toLong()

    @SerializedName("current_timestamp")
    val currentTimestamp = reply.currentTimestamp.toLong()

    @SerializedName("previous_block_hash")
    val previousBlockHash = reply.previousBlockHash.toHex()

    val mutable = reply.mutableList

    val transactions = reply.transactionsList.map { candidateTransaction ->
        CandidateTransactionInfo(candidateTransaction)
    }
}
