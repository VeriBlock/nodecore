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

public class MiningMempoolUpdate extends UCPClientCommand {
    private final UCPCommand.Command command = Command.MINING_MEMPOOL_UPDATE; // Not static for serialization purposes

    // Required
    private final UCPArgumentRequestID request_id;
    private final UCPArgumentJobID job_id;
    private final UCPArgumentIntermediateLevelMerkleRoot pop_transaction_merkle_root;
    private final UCPArgumentIntermediateLevelMerkleRoot normal_transaction_merkle_root;
    private final UCPArgumentTransactionID coinbase_txid;
    private final UCPArgumentIntermediateMetapackageHash intermediate_metapackage_hash;

    private final UCPArgumentTopLevelMerkleRoot new_merkle_root;

    public MiningMempoolUpdate(UCPArgument... arguments) {
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

        this.request_id = (UCPArgumentRequestID)arguments[0];
        this.job_id = (UCPArgumentJobID)arguments[1];
        this.pop_transaction_merkle_root = (UCPArgumentIntermediateLevelMerkleRoot)arguments[2];
        this.normal_transaction_merkle_root = (UCPArgumentIntermediateLevelMerkleRoot)arguments[3];
        this.coinbase_txid = (UCPArgumentTransactionID)arguments[4];
        this.new_merkle_root = (UCPArgumentTopLevelMerkleRoot)arguments[5];
        this.intermediate_metapackage_hash = (UCPArgumentIntermediateMetapackageHash) arguments[6];
    }

    public MiningMempoolUpdate(int request_id, int job_id, String pop_transaction_merkle_root, String normal_transaction_merkle_root, String coinbase_txid, String new_merkle_root, String intermediateMetapackageHash) {
        this.request_id = new UCPArgumentRequestID(request_id);
        this.job_id = new UCPArgumentJobID(job_id);
        this.pop_transaction_merkle_root = new UCPArgumentIntermediateLevelMerkleRoot(pop_transaction_merkle_root);
        this.normal_transaction_merkle_root = new UCPArgumentIntermediateLevelMerkleRoot(normal_transaction_merkle_root);
        this.coinbase_txid = new UCPArgumentTransactionID(coinbase_txid);
        this.new_merkle_root = new UCPArgumentTopLevelMerkleRoot(new_merkle_root);
        this.intermediate_metapackage_hash = new UCPArgumentIntermediateMetapackageHash(intermediateMetapackageHash);
    }

    public static MiningMempoolUpdate reconstitute(String commandLine) {
        if (commandLine == null) {
            throw new IllegalArgumentException(new Exception().getStackTrace()[0].getClassName() + "'s reconstitute cannot be called with a null commandLine!");
        }

        GsonBuilder deserializer = new GsonBuilder();
        deserializer.registerTypeAdapter(MiningMempoolUpdate.class, new MiningMempoolUpdateDeserializer());
        return deserializer.create().fromJson(commandLine, MiningMempoolUpdate.class);
    }

    public int getRequestId() {
        return request_id.getData();
    }

    public int getJobId() {
        return job_id.getData();
    }

    public String getPoPTransactionMerkleROot() {
        return pop_transaction_merkle_root.getData();
    }

    public String getNormalTransactionMerkleRoot() {
        return normal_transaction_merkle_root.getData();
    }

    public String getCoinbaseTxID() {
        return coinbase_txid.getData();
    }

    public String getNewMerkleRoot() {
        return new_merkle_root.getData();
    }

    public String getIntermediateMetapackageHash() {
        return intermediate_metapackage_hash.getData();
    }

    public String compileCommand() { return new Gson().toJson(this); }
}
