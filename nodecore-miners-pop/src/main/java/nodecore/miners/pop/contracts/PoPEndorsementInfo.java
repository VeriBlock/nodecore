// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.contracts;

import com.google.gson.annotations.SerializedName;
import nodecore.miners.pop.common.Utility;

public class PoPEndorsementInfo {
    public PoPEndorsementInfo(final nodecore.api.grpc.PoPEndorsementInfo popEndorsementInfo) {
        minerAddress = Utility.bytesToBase58(popEndorsementInfo.getMinerAddress().toByteArray());
        endorsedVeriBlockBlockHash = Utility.bytesToHex(popEndorsementInfo.getEndorsedVeriblockBlockHash().toByteArray());
        containedInVeriBlockBlockHash = Utility.bytesToHex(popEndorsementInfo.getContainedInVeriblockBlockHash().toByteArray());
        veriBlockTransactionId = Utility.bytesToHex(popEndorsementInfo.getVeriblockTxId().toByteArray());
        bitcoinTransaction = Utility.bytesToHex(popEndorsementInfo.getBitcoinTransaction().toByteArray());
        bitcoinTransactionId = Utility.bytesToHex(popEndorsementInfo.getBitcoinTxId().toByteArray());
        bitcoinBlockHeader = Utility.bytesToHex(popEndorsementInfo.getBitcoinBlockHeader().toByteArray());
        bitcoinBlockHeaderHash = Utility.bytesToHex(popEndorsementInfo.getBitcoinBlockHeaderHash().toByteArray());
        reward = Utility.formatAtomicLongWithDecimal(popEndorsementInfo.getReward());
        finalized = popEndorsementInfo.getFinalized();
        endorsedBlockNumber = popEndorsementInfo.getEndorsedBlockNumber();
    }

    @SerializedName("miner_address")
    public String minerAddress;

    @SerializedName("endorsed_veriblock_block_hash")
    public String endorsedVeriBlockBlockHash;

    @SerializedName("contained_in_veriblock_block_hash")
    public String containedInVeriBlockBlockHash;

    @SerializedName("veriblock_transaction_id")
    public String veriBlockTransactionId;

    @SerializedName("bitcoin_transaction")
    public String bitcoinTransaction;

    @SerializedName("bitcoin_transaction_id")
    public String bitcoinTransactionId;

    @SerializedName("bitcoin_block_header")
    public String bitcoinBlockHeader;

    @SerializedName("bitcoin_block_header_hash")
    public String bitcoinBlockHeaderHash;

    @SerializedName("reward")
    public String reward;

    @SerializedName("finalized")
    public boolean finalized;

    @SerializedName("endorsed_block_number")
    public int endorsedBlockNumber;
}
