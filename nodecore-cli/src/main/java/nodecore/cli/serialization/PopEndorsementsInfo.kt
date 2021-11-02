// VeriBlock NodeCore CLI
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import com.google.gson.annotations.SerializedName
import nodecore.api.grpc.RpcPopEndorsementInfo
import nodecore.api.grpc.utilities.extensions.toHex
import nodecore.api.grpc.utilities.extensions.toProperAddressType
import org.veriblock.core.utilities.extensions.formatAtomicLongWithDecimal

class PopEndorsementsInfo(
    popEndorsementInfo: RpcPopEndorsementInfo
) {
    @SerializedName("miner_address")
    val minerAddress = popEndorsementInfo.minerAddress.toProperAddressType()

    @SerializedName("endorsed_veriblock_block_hash")
    val endorsedVeriBlockBlockHash = popEndorsementInfo.endorsedVeriblockBlockHash.toHex()

    @SerializedName("contained_in_veriblock_block_hash")
    val containedInVeriBlockBlockHash = popEndorsementInfo.containedInVeriblockBlockHash.toHex()

    @SerializedName("veriblock_transaction_id")
    val veriBlockTransactionId = popEndorsementInfo.veriblockTxId.toHex()

    @SerializedName("bitcoin_transaction")
    val bitcoinTransaction = popEndorsementInfo.bitcoinTransaction.toHex()

    @SerializedName("bitcoin_transaction_id")
    val bitcoinTransactionId = popEndorsementInfo.bitcoinTxId.toHex()

    @SerializedName("bitcoin_block_header")
    val bitcoinBlockHeader = popEndorsementInfo.bitcoinBlockHeader.toHex()

    @SerializedName("bitcoin_block_header_hash")
    val bitcoinBlockHeaderHash = popEndorsementInfo.bitcoinBlockHeaderHash.toHex()

    @SerializedName("reward")
    val reward = popEndorsementInfo.reward.formatAtomicLongWithDecimal()

    @SerializedName("finalized")
    val finalized = popEndorsementInfo.finalized

    @SerializedName("endorsed_block_number")
    val endorsedBlockNumber = popEndorsementInfo.endorsedBlockNumber
}
