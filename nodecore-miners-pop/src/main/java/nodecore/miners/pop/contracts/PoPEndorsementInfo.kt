// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.miners.pop.contracts

import com.google.gson.annotations.SerializedName
import nodecore.api.grpc.VeriBlockMessages
import nodecore.miners.pop.common.Utility

class PoPEndorsementInfo(
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
    val reward: String

    @SerializedName("finalized")
    val finalized: Boolean

    @SerializedName("endorsed_block_number")
    val endorsedBlockNumber: Int

    init {
        minerAddress = Utility.bytesToBase58(popEndorsementInfo.minerAddress.toByteArray())
        endorsedVeriBlockBlockHash = Utility.bytesToHex(popEndorsementInfo.endorsedVeriblockBlockHash.toByteArray())
        containedInVeriBlockBlockHash = Utility.bytesToHex(
            popEndorsementInfo.containedInVeriblockBlockHash.toByteArray()
        )
        veriBlockTransactionId = Utility.bytesToHex(popEndorsementInfo.veriblockTxId.toByteArray())
        bitcoinTransaction = Utility.bytesToHex(popEndorsementInfo.bitcoinTransaction.toByteArray())
        bitcoinTransactionId = Utility.bytesToHex(popEndorsementInfo.bitcoinTxId.toByteArray())
        bitcoinBlockHeader = Utility.bytesToHex(popEndorsementInfo.bitcoinBlockHeader.toByteArray())
        bitcoinBlockHeaderHash = Utility.bytesToHex(popEndorsementInfo.bitcoinBlockHeaderHash.toByteArray())
        reward = Utility.formatAtomicLongWithDecimal(popEndorsementInfo.reward)
        finalized = popEndorsementInfo.finalized
        endorsedBlockNumber = popEndorsementInfo.endorsedBlockNumber
    }
}
