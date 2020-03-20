// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.api.ucp.commands.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import nodecore.api.ucp.arguments.*;
import nodecore.api.ucp.commands.UCPClientCommand;
import nodecore.api.ucp.commands.UCPCommand;
import org.veriblock.core.types.Pair;

import java.util.ArrayList;

public class MiningJob extends UCPClientCommand {
    private final UCPCommand.Command command = Command.MINING_JOB; // Not static for serialization purposes

    // Required
    private final UCPArgumentRequestID request_id;
    private final UCPArgumentJobID job_id;
    private final UCPArgumentBlockIndex block_index;
    private final UCPArgumentBlockVersion block_version;
    private final UCPArgumentBlockHash previous_block_hash;
    private final UCPArgumentBlockHash second_previous_block_hash;
    private final UCPArgumentBlockHash third_previous_block_hash;
    private final UCPArgumentAddress pool_address;
    private final UCPArgumentTopLevelMerkleRoot merkle_root;
    private final UCPArgumentTimestamp timestamp;
    private final UCPArgumentDifficulty difficulty;
    private final UCPArgumentTarget mining_target;
    private final UCPArgumentLedgerHash ledger_hash;
    private final UCPArgumentTransactionID coinbase_txid;
    private final UCPArgumentPoPDatastoreHash pop_datastore_hash;
    private final UCPArgumentMinerComment miner_comment;
    private final UCPArgumentIntermediateLevelMerkleRoot pop_transaction_merkle_root;
    private final UCPArgumentIntermediateLevelMerkleRoot normal_transaction_merkle_root;
    private final UCPArgumentIntermediateMetapackageHash intermediate_metapackage_hash;

    private final UCPArgumentExtraNonce extra_nonce_start;
    private final UCPArgumentExtraNonce extra_nonce_end;


    public MiningJob(UCPArgument ... arguments) {
        ArrayList<Pair<String, UCPArgument.UCPType>> pattern = command.getPattern();

        if (arguments.length != pattern.size()) {
            throw new IllegalArgumentException(getClass().getCanonicalName() + "'s constructor cannot be called without exactly " + pattern.size() + " UCPArguments!");
        }

        for (int i = 0; i < pattern.size(); i++) {
            if (arguments[i].getType() != pattern.get(i).getSecond()) {
                throw new IllegalArgumentException(getClass().getCanonicalName()
                        + "'s constructor cannot be called with a argument at index "
                        + i + " which is a " + arguments[i].getType()
                        + " instead of a " + pattern.get(i).getSecond() + "!");
            }
        }

        this.request_id = (UCPArgumentRequestID) arguments[0];
        this.job_id = (UCPArgumentJobID) arguments[1];
        this.block_index = (UCPArgumentBlockIndex) arguments[2];
        this.block_version = (UCPArgumentBlockVersion) arguments[3];
        this.previous_block_hash = (UCPArgumentBlockHash) arguments[4];
        this.second_previous_block_hash = (UCPArgumentBlockHash) arguments[5];
        this.third_previous_block_hash = (UCPArgumentBlockHash) arguments[6];
        this.pool_address = (UCPArgumentAddress) arguments[7];
        this.merkle_root = (UCPArgumentTopLevelMerkleRoot) arguments[8];
        this.timestamp = (UCPArgumentTimestamp) arguments[9];
        this.difficulty = (UCPArgumentDifficulty) arguments[10];
        this.mining_target = (UCPArgumentTarget) arguments[11];
        this.ledger_hash = (UCPArgumentLedgerHash) arguments[12];
        this.coinbase_txid = (UCPArgumentTransactionID) arguments[13];
        this.pop_datastore_hash = (UCPArgumentPoPDatastoreHash) arguments[14];
        this.miner_comment = (UCPArgumentMinerComment) arguments[15];
        this.pop_transaction_merkle_root = (UCPArgumentIntermediateLevelMerkleRoot)arguments[16];
        this.normal_transaction_merkle_root = (UCPArgumentIntermediateLevelMerkleRoot)arguments[17];
        this.extra_nonce_start = (UCPArgumentExtraNonce) arguments[18];
        this.extra_nonce_end = (UCPArgumentExtraNonce) arguments[19];
        this.intermediate_metapackage_hash = (UCPArgumentIntermediateMetapackageHash) arguments[20];
    }

    public MiningJob(int request_id, int job_id, int block_index, int block_version, String previous_block_hash,
                     String second_previous_block_hash, String third_previous_block_hash, String pool_address,
                     String merkle_root, int timestamp, int difficulty, String mining_target, String ledger_hash,
                     String coinbaseTransactionID, String popDatastoreHash, String minerComment,
                     String popTransactionMerkleRoot, String normalTransactionMerkleRoot, String intermediateMetapackageHash, long extra_nonce_start,
                     long extra_nonce_end) {
        this.request_id = new UCPArgumentRequestID(request_id);
        this.job_id = new UCPArgumentJobID(job_id);
        this.block_index = new UCPArgumentBlockIndex(block_index);
        this.block_version = new UCPArgumentBlockVersion(block_version);
        this.previous_block_hash = new UCPArgumentBlockHash(previous_block_hash);
        this.second_previous_block_hash = new UCPArgumentBlockHash(second_previous_block_hash);
        this.third_previous_block_hash = new UCPArgumentBlockHash(third_previous_block_hash);
        this.pool_address = new UCPArgumentAddress(pool_address);
        this.merkle_root = new UCPArgumentTopLevelMerkleRoot(merkle_root);
        this.timestamp = new UCPArgumentTimestamp(timestamp);
        this.difficulty = new UCPArgumentDifficulty(difficulty);
        this.mining_target = new UCPArgumentTarget(mining_target);
        this.ledger_hash = new UCPArgumentLedgerHash(ledger_hash);
        this.coinbase_txid = new UCPArgumentTransactionID(coinbaseTransactionID);
        this.pop_datastore_hash = new UCPArgumentPoPDatastoreHash(popDatastoreHash);
        this.miner_comment = new UCPArgumentMinerComment(minerComment);
        this.pop_transaction_merkle_root = new UCPArgumentIntermediateLevelMerkleRoot(popTransactionMerkleRoot);
        this.normal_transaction_merkle_root = new UCPArgumentIntermediateLevelMerkleRoot(normalTransactionMerkleRoot);
        this.intermediate_metapackage_hash = new UCPArgumentIntermediateMetapackageHash(intermediateMetapackageHash);
        this.extra_nonce_start = new UCPArgumentExtraNonce(extra_nonce_start);
        this.extra_nonce_end = new UCPArgumentExtraNonce(extra_nonce_end);
    }

    public static MiningJob reconstitute(String commandLine) {
        if (commandLine == null) {
            throw new IllegalArgumentException(new Exception().getStackTrace()[0].getClassName() + "'s reconstitute cannot be called with a null commandLine!");
        }

        GsonBuilder deserializer = new GsonBuilder();
        deserializer.registerTypeAdapter(MiningJob.class, new MiningJobDeserializer());
        return deserializer.create().fromJson(commandLine, MiningJob.class);
    }

    public int getRequestId() {
        return request_id.getData();
    }

    public int getJobId() {
        return job_id.getData();
    }

    public int getBlockIndex() {
        return block_index.getData();
    }

    public int getBlockVersion() {
        return block_version.getData();
    }

    public String getPreviousBlockHash() {
        return previous_block_hash.getData();
    }

    public String getSecondPreviousBlockHash() {
        return second_previous_block_hash.getData();
    }

    public String getThirdPreviousBlockHash() {
        return third_previous_block_hash.getData();
    }

    public String getPoolAddress() {
        return pool_address.getData();
    }

    public String getMerkleRoot() {
        return merkle_root.getData();
    }

    public int getTimestamp() {
        return timestamp.getData();
    }

    public int getDifficulty() {
        return difficulty.getData();
    }

    public String getMiningTarget() { return mining_target.getData(); }

    public String getLedgerHash() {
        return ledger_hash.getData();
    }

    public String getCoinbaseTxID() {
        return coinbase_txid.getData();
    }

    public String getPoPDatastoreHash() {
        return coinbase_txid.getData();
    }

    public String getMinerComment() {
        return miner_comment.getData();
    }

    public String getPoPTransactionMerkleRoot() {
        return pop_transaction_merkle_root.getData();
    }

    public String getNormalTransactionMerkleRoot() {
        return normal_transaction_merkle_root.getData();
    }

    public String getIntermediateMetapackageHash() {
        return intermediate_metapackage_hash.getData();
    }

    public long getExtraNonceStart() {
        return extra_nonce_start.getData();
    }

    public long getExtraNonceEnd() {
        return extra_nonce_end.getData();
    }

    public String compileCommand() {
        return new Gson().toJson(this); }
}
