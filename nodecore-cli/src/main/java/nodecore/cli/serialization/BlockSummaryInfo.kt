// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import com.google.gson.annotations.SerializedName
import nodecore.api.grpc.VeriBlockMessages.BlockSummary
import nodecore.api.grpc.utilities.ByteStringUtility
import org.veriblock.core.utilities.Utility

class BlockSummaryInfo(
    block: BlockSummary
) {
    @SerializedName("hash")
    val hash = ByteStringUtility.byteStringToHex(block.hash)

    @SerializedName("number")
    val number = block.number

    @SerializedName("version")
    val version = block.version.toShort()

    @SerializedName("previous_hash")
    val previousHash = ByteStringUtility.byteStringToHex(block.previousHash)

    @SerializedName("second_previous_hash")
    val secondPreviousHash = if (number % 20 == 0) {
        null
    } else {
        ByteStringUtility.byteStringToHex(block.secondPreviousHash)
    }

    @SerializedName("previous_keystone_hash")
    val previousKeystoneHash = if (number % 20 == 0) {
        ByteStringUtility.byteStringToHex(block.secondPreviousHash)
    } else {
        null
    }

    @SerializedName("third_previous_hash")
    val thirdPreviousHash = if (number % 20 == 0) {
        null
    } else {
        ByteStringUtility.byteStringToHex(block.thirdPreviousHash)
    }

    @SerializedName("second_previous_keystone_hash")
    val secondPreviousKeystoneHash = if (number % 20 == 0) {
        ByteStringUtility.byteStringToHex(block.thirdPreviousHash)
    } else {
        null
    }

    @SerializedName("merkle_root")
    val merkleRoot = ByteStringUtility.byteStringToHex(block.merkleRoot)

    @SerializedName("timestamp")
    val timestamp = block.timestamp

    @SerializedName("decoded_difficulty")
    val decoded_difficulty = block.decodedDifficulty

    @SerializedName("winning_nonce")
    val winningNonce = block.winningNonce

    @SerializedName("ledger_hash")
    val ledgerHash = ByteStringUtility.byteStringToHex(block.ledgerHash)

    @SerializedName("size")
    val size = block.size

    @SerializedName("transaction_fees")
    val transactionFees = Utility.formatAtomicLongWithDecimal(block.totalFees)

    @SerializedName("pow_coinbase_reward")
    val powCoinbaseReward = Utility.formatAtomicLongWithDecimal(block.powCoinbaseReward)

    @SerializedName("pop_coinbase_reward")
    val popCoinbaseReward = Utility.formatAtomicLongWithDecimal(block.popCoinbaseReward)
}
