// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.serialization;

import com.google.gson.annotations.SerializedName;
import nodecore.api.grpc.VeriBlockMessages;
import nodecore.api.grpc.utilities.ByteStringUtility;

public class BlockContentMetapackageInfo {
    public BlockContentMetapackageInfo(final VeriBlockMessages.BlockContentMetapackage metapackage) {
        coinbaseTransaction = new CoinbaseTransactionInfo(metapackage.getCoinbaseTransaction());
        popDatastore = new PoPDatastoreInfo(metapackage.getPopDatastore());
        blockFeeTable = new BlockFeeTableInfo(metapackage.getBlockFeeTable());
        minerComment = new String(metapackage.getMinerComment().toByteArray());
        ledgerHash = ByteStringUtility.byteStringToHex(metapackage.getLedgerHash());
        extraNonce = metapackage.getExtraNonce();
    }

    @SerializedName("coinbase_transaction")
    public CoinbaseTransactionInfo coinbaseTransaction;

    @SerializedName("pop_datastore")
    public PoPDatastoreInfo popDatastore;

    @SerializedName("block_fee_table")
    public BlockFeeTableInfo blockFeeTable;

    @SerializedName("miner_comment")
    public String minerComment;

    @SerializedName("ledger_hash")
    public String ledgerHash;

    @SerializedName("extra_nonce")
    public long extraNonce;
}
