// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.api.ucp.commands.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import nodecore.api.ucp.arguments.*;
import nodecore.api.ucp.commands.UCPCommand;
import nodecore.api.ucp.commands.UCPServerCommand;
import org.veriblock.core.types.Pair;

import java.util.ArrayList;

public class MiningSubmit extends UCPServerCommand {
    private final UCPCommand.Command command = Command.MINING_SUBMIT; // Not static for serialization purposes

    // Required
    private final UCPArgumentRequestID request_id;
    private final UCPArgumentJobID job_id;
    private final UCPArgumentTimestamp nTime;
    private final UCPArgumentNonce nonce;
    private final UCPArgumentExtraNonce extra_nonce;


    public MiningSubmit(UCPArgument... arguments) {
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
        this.nTime = (UCPArgumentTimestamp)arguments[2];
        this.nonce = (UCPArgumentNonce)arguments[3];
        this.extra_nonce = (UCPArgumentExtraNonce)arguments[4];
    }

    public MiningSubmit(int request_id, int job_id, int nTime, int nonce, long extra_nonce) {
        this.request_id = new UCPArgumentRequestID(request_id);
        this.job_id = new UCPArgumentJobID(job_id);
        this.nTime = new UCPArgumentTimestamp(nTime);
        this.nonce = new UCPArgumentNonce(nonce);
        this.extra_nonce = new UCPArgumentExtraNonce(extra_nonce);
    }

    public static MiningSubmit reconstitute(String commandLine) {
        if (commandLine == null) {
            throw new IllegalArgumentException(new Exception().getStackTrace()[0].getClassName() + "'s reconstitute cannot be called with a null commandLine!");
        }

        GsonBuilder deserializer = new GsonBuilder();
        deserializer.registerTypeAdapter(MiningSubmit.class, new MiningSubmitDeserializer());
        return deserializer.create().fromJson(commandLine, MiningSubmit.class);
    }

    public int getRequestId() {
        return request_id.getData();
    }

    public int getJobId() {
        return job_id.getData();
    }

    public int getNTime() {
        return nTime.getData();
    }

    public int getNonce() {
        return nonce.getData();
    }

    public long getExtraNonce() {
        return extra_nonce.getData();
    }

    public String compileCommand() { return new Gson().toJson(this); }
}
