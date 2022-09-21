// VeriBlock NodeCore CLI
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import com.google.gson.annotations.SerializedName
import nodecore.api.grpc.RpcBlockSummary
import org.veriblock.sdk.extensions.toHex
import org.veriblock.core.utilities.Utility

class BlockSummaryInfo(
    block: RpcBlockSummary
) {
    @SerializedName("hash")
    val hash = block.hash.toHex()

    @SerializedName("number")
    val number = block.number

    @SerializedName("version")
    val version = block.version.toShort()

    @SerializedName("previous_hash")
    val previousHash = block.previousHash.toHex()

    @SerializedName("second_previous_hash")
    val secondPreviousHash = if (number % 20 == 0) {
        null
    } else {
        block.secondPreviousHash.toHex()
    }

    @SerializedName("previous_keystone_hash")
    val previousKeystoneHash = if (number % 20 == 0) {
        block.secondPreviousHash.toHex()
    } else {
        null
    }

    @SerializedName("third_previous_hash")
    val thirdPreviousHash = if (number % 20 == 0) {
        null
    } else {
        block.thirdPreviousHash.toHex()
    }

    @SerializedName("second_previous_keystone_hash")
    val secondPreviousKeystoneHash = if (number % 20 == 0) {
        block.thirdPreviousHash.toHex()
    } else {
        null
    }

    @SerializedName("merkle_root")
    val merkleRoot = block.merkleRoot.toHex()

    @SerializedName("timestamp")
    val timestamp = block.timestamp

    @SerializedName("decoded_difficulty")
    val decoded_difficulty = block.decodedDifficulty

    @SerializedName("winning_nonce")
    val winningNonce = block.winningNonce

    @SerializedName("ledger_hash")
    val ledgerHash = block.ledgerHash.toHex()

    @SerializedName("size")
    val size = block.size

    @SerializedName("transaction_fees")
    val transactionFees = Utility.formatAtomicLongWithDecimal(block.totalFees)

    @SerializedName("pow_coinbase_reward")
    val powCoinbaseReward = Utility.formatAtomicLongWithDecimal(block.powCoinbaseReward)

    @SerializedName("pop_coinbase_reward")
    val popCoinbaseReward = Utility.formatAtomicLongWithDecimal(block.popCoinbaseReward)
}
