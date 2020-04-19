// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.miners.pop.model

import com.google.gson.annotations.SerializedName
import nodecore.api.grpc.VeriBlockMessages
import org.veriblock.core.utilities.extensions.formatAtomicLongWithDecimal
import org.veriblock.core.utilities.extensions.toBase58
import org.veriblock.core.utilities.extensions.toHex

class PopEndorsementInfo(
    popEndorsementInfo: VeriBlockMessages.PoPEndorsementInfo
) {
    @SerializedName("miner_address")
    val minerAddress: String

    @SerializedName("endorsed_veriblock_block_hash")
    val endorsedVeriBlockBlockHash: String

    @SerializedName("contained_in_veriblock_block_hash")
    val containedInVeriBlockBlockHash: String

    @SerializedName("veriblock_transaction_id")
    val veriBlockTransactionId: String

    @SerializedName("bitcoin_transaction")
    val bitcoinTransaction: String

    @SerializedName("bitcoin_transaction_id")
    val bitcoinTransactionId: String

    @SerializedName("bitcoin_block_header")
    val bitcoinBlockHeader: String

    @SerializedName("bitcoin_block_header_hash")
    val bitcoinBlockHeaderHash: String

    @SerializedName("reward")
    val reward: Long

    @SerializedName("finalized")
    val finalized: Boolean

    @SerializedName("endorsed_block_number")
    val endorsedBlockNumber: Int

    init {
        minerAddress = popEndorsementInfo.minerAddress.toByteArray().toBase58()
        endorsedVeriBlockBlockHash = popEndorsementInfo.endorsedVeriblockBlockHash.toByteArray().toHex()
        containedInVeriBlockBlockHash = popEndorsementInfo.containedInVeriblockBlockHash.toByteArray().toHex()
        veriBlockTransactionId = popEndorsementInfo.veriblockTxId.toByteArray().toHex()
        bitcoinTransaction = popEndorsementInfo.bitcoinTransaction.toByteArray().toHex()
        bitcoinTransactionId = popEndorsementInfo.bitcoinTxId.toByteArray().toHex()
        bitcoinBlockHeader = popEndorsementInfo.bitcoinBlockHeader.toByteArray().toHex()
        bitcoinBlockHeaderHash = popEndorsementInfo.bitcoinBlockHeaderHash.toByteArray().toHex()
        reward = popEndorsementInfo.reward
        finalized = popEndorsementInfo.finalized
        endorsedBlockNumber = popEndorsementInfo.endorsedBlockNumber
    }
}
