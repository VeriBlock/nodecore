// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.contracts;

import com.google.gson.annotations.SerializedName;
import nodecore.miners.pop.common.Utility;
import org.bitcoinj.core.Base58;
import org.veriblock.core.crypto.Crypto;

import java.util.List;
import java.util.stream.Collectors;

public class PoPOperationInfo {
    @SerializedName("operation_id")
    public String operationId;

    @SerializedName("status")
    public String status;

    @SerializedName("detail")
    public String detail;

    @SerializedName("current_action")
    public String currentAction;

    @SerializedName("pop_publication_data")
    public String popPublicationData;

    @SerializedName("endorsed_block_header")
    public String endorsedBlockHeader;

    @SerializedName("miner_address")
    public String minerAddress;

    @SerializedName("endorsed_block_hash")
    public String endorsedBlockHash;

    @SerializedName("bitcoin_transaction")
    public String bitcoinTransaction;

    @SerializedName("bitcoin_transaction_id")
    public String bitcoinTransactionId;

    @SerializedName("bitcoin_block_header_of_proof")
    public String bitcoinBlockHeaderOfProof;

    @SerializedName("bitcoin_context_blocks")
    public List<String> bitcoinContextBlocks;

    @SerializedName("bitcoin_merkle_path")
    public String bitcoinMerklePath;

    @SerializedName("alternate_blocks_of_proof")
    public List<String> alternateBlocksOfProof;

    @SerializedName("pop_transaction_id")
    public String popTransactionId;

    public PoPOperationInfo(PreservedPoPMiningOperationState state) {
        this.operationId = state.operationId;
        this.status = state.status != null ? state.status.toString() : null;
        this.detail = state.detail;
        this.currentAction = state.currentAction != null ? state.currentAction.toString() : null;
        if (state.miningInstruction != null) {
            this.popPublicationData = Utility.bytesToHex(state.miningInstruction.publicationData);
            this.endorsedBlockHeader = Utility.bytesToHex(state.miningInstruction.endorsedBlockHeader);
            this.minerAddress = Base58.encode(state.miningInstruction.minerAddress);
            this.endorsedBlockHash = new Crypto().vBlakeReturnHex(state.miningInstruction.endorsedBlockHeader);
        }
        this.bitcoinTransaction = state.transaction != null ? Utility.bytesToHex(state.transaction) : null;
        this.bitcoinTransactionId = state.submittedTransactionId;
        this.bitcoinBlockHeaderOfProof = state.bitcoinBlockHeaderOfProof != null ? Utility.bytesToHex(state.bitcoinBlockHeaderOfProof) : null;
        this.bitcoinContextBlocks =
                state.bitcoinContextBlocks != null ? state.bitcoinContextBlocks.stream().map(Utility::bytesToHex).collect(Collectors.toList()) : null;
        this.bitcoinMerklePath = state.merklePath;
        this.alternateBlocksOfProof =
                state.alternateBlocksOfProof != null ? state.alternateBlocksOfProof.stream().map(Utility::bytesToHex).collect(Collectors.toList()) :
                        null;
        this.popTransactionId = state.popTransactionId;
    }
}
