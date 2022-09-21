// VeriBlock NodeCore CLI
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import com.google.gson.annotations.SerializedName
import nodecore.api.grpc.RpcBlockContentMetapackage
import org.veriblock.sdk.extensions.toHex

class BlockContentMetapackageInfo(
    metapackage: RpcBlockContentMetapackage
) {
    @SerializedName("coinbase_transaction")
    val coinbaseTransaction = CoinbaseTransactionInfo(metapackage.coinbaseTransaction)

    @SerializedName("pop_datastore")
    val popDatastore = PopDatastoreInfo(metapackage.popDatastore)

    @SerializedName("block_fee_table")
    val blockFeeTable = BlockFeeTableInfo(metapackage.blockFeeTable)

    @SerializedName("miner_comment")
    val minerComment = metapackage.minerComment.toByteArray().decodeToString()

    @SerializedName("ledger_hash")
    val ledgerHash = metapackage.ledgerHash.toHex()

    @SerializedName("extra_nonce")
    val extraNonce = metapackage.extraNonce
}
