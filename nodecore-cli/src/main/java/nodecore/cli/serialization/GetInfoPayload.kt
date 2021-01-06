// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import com.google.gson.annotations.SerializedName
import com.opencsv.bean.CsvBindByName
import nodecore.api.grpc.VeriBlockMessages.GetInfoReply
import org.veriblock.core.utilities.Utility

class GetInfoPayload(
    reply: GetInfoReply
) {
    @CsvBindByName
    @SerializedName("default_address")
    val defaultAddress = AddressBalanceInfo(reply.defaultAddress)

    @CsvBindByName
    @SerializedName("estimated_hash_rate")
    val estimatedHashRate = reply.estimatedHashrate

    @CsvBindByName
    @SerializedName("number_of_blocks")
    val numberOfBlocks = reply.numberOfBlocks

    @CsvBindByName
    @SerializedName("transaction_fee")
    val transactionFee = Utility.formatAtomicLongWithDecimal(reply.transactionFee)

    @SerializedName("last_block")
    val lastBlock = BlockSummaryInfo(reply.lastBlock)

    @CsvBindByName
    @SerializedName("decoded_difficulty")
    val decoded_difficulty = reply.decodedDifficulty
}
