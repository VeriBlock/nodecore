// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import com.google.gson.annotations.SerializedName
import nodecore.api.grpc.VeriBlockMessages.BlockContentMetapackage
import nodecore.api.grpc.utilities.ByteStringUtility

class BlockContentMetapackageInfo(
    metapackage: BlockContentMetapackage
) {
    @SerializedName("coinbase_transaction")
    val coinbaseTransaction = CoinbaseTransactionInfo(metapackage.coinbaseTransaction)

    @SerializedName("pop_datastore")
    val popDatastore = PoPDatastoreInfo(metapackage.popDatastore)

    @SerializedName("block_fee_table")
    val blockFeeTable = BlockFeeTableInfo(metapackage.blockFeeTable)

    @SerializedName("miner_comment")
    val minerComment = String(metapackage.minerComment.toByteArray())

    @SerializedName("ledger_hash")
    val ledgerHash = ByteStringUtility.byteStringToHex(metapackage.ledgerHash)

    @SerializedName("extra_nonce")
    val extraNonce = metapackage.extraNonce
}
