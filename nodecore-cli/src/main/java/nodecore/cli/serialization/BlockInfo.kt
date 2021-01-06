// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import com.google.gson.annotations.SerializedName
import nodecore.api.grpc.VeriBlockMessages
import nodecore.api.grpc.VeriBlockMessages.TransactionUnion
import nodecore.api.grpc.utilities.ByteStringUtility
import org.veriblock.core.utilities.BlockUtility
import org.veriblock.core.utilities.Utility

class BlockInfo(
    block: VeriBlockMessages.Block
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
        ByteStringUtility.byteStringToHex(block.thirdPreviousHash)
    } else {
        null
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

    @SerializedName("encoded_difficulty")
    val encoded_difficulty = block.encodedDifficulty

    @SerializedName("winning_nonce")
    val winningNonce = block.winningNonce

    @SerializedName("bitcoin_block_headers")
    val bitcoinBlockHeaders = block.bitcoinBlockHeadersList.map {
        ByteStringUtility.byteStringToHex(it)
    }

    @SerializedName("regular_transactions")
    val regular_transactions = block.regularTransactionsList.map { transactionUnion ->
        if (transactionUnion.transactionCase == TransactionUnion.TransactionCase.SIGNED) {
            ByteStringUtility.byteStringToHex(transactionUnion.signed.transaction.txId)
        } else {
            ByteStringUtility.byteStringToHex(transactionUnion.signedMultisig.transaction.txId)
        }
    }

    @SerializedName("pop_transactions")
    val pop_transactions = block.popTransactionsList.map { transactionUnion ->
        ByteStringUtility.byteStringToHex(transactionUnion.signed.transaction.txId)
    }

    @SerializedName("total_fees")
    val totalFees = Utility.formatAtomicLongWithDecimal(block.totalFees)

    @SerializedName("pow_coinbase_reward")
    val powCoinbaseReward = Utility.formatAtomicLongWithDecimal(block.powCoinbaseReward)

    @SerializedName("pop_coinbase_reward")
    val popCoinbaseReward = Utility.formatAtomicLongWithDecimal(block.popCoinbaseReward)

    @SerializedName("block_content_metapackage")
    val blockContentMetapackage = BlockContentMetapackageInfo(block.blockContentMetapackage)

    @SerializedName("size")
    val size = block.size

    @SerializedName("header")
    val header = if (number % 20 == 0) {
        Utility.bytesToHex(
            BlockUtility.assembleBlockHeader(
                number,
                version,
                previousHash,
                previousKeystoneHash,
                secondPreviousKeystoneHash,
                merkleRoot,
                timestamp,
                encoded_difficulty,
                winningNonce
            )
        )
    } else {
        Utility.bytesToHex(
            BlockUtility.assembleBlockHeader(
                number,
                version,
                previousHash,
                secondPreviousHash,
                thirdPreviousHash,
                merkleRoot,
                timestamp,
                encoded_difficulty,
                winningNonce
            )
        )
    }
}
