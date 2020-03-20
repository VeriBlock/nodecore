// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.serialization;

import com.google.gson.annotations.SerializedName;
import nodecore.api.grpc.VeriBlockMessages;
import nodecore.api.grpc.utilities.ByteStringAddressUtility;
import nodecore.api.grpc.utilities.ByteStringUtility;
import org.veriblock.core.utilities.Utility;

public class PoPEndorsementsInfo {
    public PoPEndorsementsInfo(final VeriBlockMessages.PoPEndorsementInfo popEndorsementInfo) {
        minerAddress = ByteStringAddressUtility.parseProperAddressTypeAutomatically(popEndorsementInfo.getMinerAddress());
        endorsedVeriBlockBlockHash = ByteStringUtility.byteStringToHex(popEndorsementInfo.getEndorsedVeriblockBlockHash());
        containedInVeriBlockBlockHash = ByteStringUtility.byteStringToHex(popEndorsementInfo.getContainedInVeriblockBlockHash());
        veriBlockTransactionId = ByteStringUtility.byteStringToHex(popEndorsementInfo.getVeriblockTxId());
        bitcoinTransaction = ByteStringUtility.byteStringToHex(popEndorsementInfo.getBitcoinTransaction());
        bitcoinTransactionId = ByteStringUtility.byteStringToHex(popEndorsementInfo.getBitcoinTxId());
        bitcoinBlockHeader = ByteStringUtility.byteStringToHex(popEndorsementInfo.getBitcoinBlockHeader());
        bitcoinBlockHeaderHash = ByteStringUtility.byteStringToHex(popEndorsementInfo.getBitcoinBlockHeaderHash());
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
