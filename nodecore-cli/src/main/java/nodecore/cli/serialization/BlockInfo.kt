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
import nodecore.api.grpc.utilities.extensions.toHex
import org.veriblock.core.utilities.BlockUtility
import org.veriblock.core.utilities.Utility
import org.veriblock.core.utilities.extensions.formatAtomicLongWithDecimal
import org.veriblock.core.utilities.extensions.toHex

class BlockInfo(
    block: VeriBlockMessages.Block
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
        block.thirdPreviousHash.toHex()
    } else {
        null
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

    @SerializedName("encoded_difficulty")
    val encoded_difficulty = block.encodedDifficulty

    @SerializedName("winning_nonce")
    val winningNonce = block.winningNonce

    @SerializedName("bitcoin_block_headers")
    val bitcoinBlockHeaders = block.bitcoinBlockHeadersList.map {
        it.toHex()
    }

    @SerializedName("regular_transactions")
    val regular_transactions = block.regularTransactionsList.map { transactionUnion ->
        if (transactionUnion.transactionCase == TransactionUnion.TransactionCase.SIGNED) {
            transactionUnion.signed.transaction.txId.toHex()
        } else {
            transactionUnion.signedMultisig.transaction.txId.toHex()
        }
    }

    @SerializedName("pop_transactions")
    val pop_transactions = block.popTransactionsList.map { transactionUnion ->
        transactionUnion.signed.transaction.txId.toHex()
    }

    @SerializedName("total_fees")
    val totalFees = block.totalFees.formatAtomicLongWithDecimal()

    @SerializedName("pow_coinbase_reward")
    val powCoinbaseReward = block.powCoinbaseReward.formatAtomicLongWithDecimal()

    @SerializedName("pop_coinbase_reward")
    val popCoinbaseReward = block.popCoinbaseReward.formatAtomicLongWithDecimal()

    @SerializedName("block_content_metapackage")
    val blockContentMetapackage = BlockContentMetapackageInfo(block.blockContentMetapackage)

    @SerializedName("size")
    val size = block.size

    @SerializedName("header")
    val header = if (number % 20 == 0) {
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
        ).toHex()
    } else {
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
        ).toHex()
    }
}
