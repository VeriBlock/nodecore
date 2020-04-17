// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.extensions.stratumapi.commands.toserver;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.veriblock.extensions.stratumapi.arguments.StratumArgument;
import org.veriblock.extensions.stratumapi.arguments.StratumArgumentId;
import org.veriblock.extensions.stratumapi.arguments.StratumArgumentJobId;
import org.veriblock.extensions.stratumapi.arguments.StratumArgumentNonce;
import org.veriblock.extensions.stratumapi.arguments.StratumArgumentUsername;
import org.veriblock.extensions.stratumapi.commands.StratumCommand;
import org.veriblock.extensions.stratumapi.commands.StratumServerCommand;
import org.veriblock.core.types.Pair;
import org.veriblock.core.utilities.Utility;

import java.util.ArrayList;

public class MiningSubmit extends StratumServerCommand {
    private final StratumCommand.Command command = Command.MINING_SUBMIT; // Not static for serialization purposes

    // Required
    private final StratumArgumentId id;
    private final StratumArgumentUsername username;
    private final StratumArgumentJobId jobId;
    private final StratumArgumentNonce nonce;

    public MiningSubmit(StratumArgument... arguments) {
        ArrayList<Pair<String, StratumArgument.StratumType>> pattern = command.getPattern();

        if (arguments.length != pattern.size()) {
            throw new IllegalArgumentException(getClass().getCanonicalName() + "'s constructor cannot be called without exactly " +
                pattern.size() + " StratumArguments!");
        }

        for (int i = 0; i < pattern.size(); i++) {
            if (arguments[i].getType() != pattern.get(i).getSecond()) {
                throw new IllegalArgumentException(getClass().getCanonicalName()
                    + "'s constructor cannot be called with a argument at index "
                    + i + " which is a " + arguments[i].getType()
                    + " instead of a " + pattern.get(i).getSecond() + "!");
            }
        }

        this.id = (StratumArgumentId) arguments[0];
        this.username = (StratumArgumentUsername) arguments[1];
        this.jobId = (StratumArgumentJobId) arguments[2];
        this.nonce = (StratumArgumentNonce) arguments[3];
    }

    public MiningSubmit(int id, String username, int jobId, String nonce) {
        this.id = new StratumArgumentId(id);
        this.username = new StratumArgumentUsername(username);
        this.jobId = new StratumArgumentJobId(jobId);
        this.nonce = new StratumArgumentNonce(nonce);
    }

    public static MiningSubmit reconstitute(JsonElement rootElement) {
        if (rootElement == null) {
            throw new IllegalArgumentException(new Exception().getStackTrace()[0].getClassName() + "'s reconstitute cannot be called with a null JSON root element!");
        }

        JsonObject rootObject = rootElement.getAsJsonObject();

        int id = rootObject.get("id").getAsInt();

        JsonArray params = rootObject.get("params").getAsJsonArray();

        StringBuilder jobIdHex = new StringBuilder(params.get(1).getAsString());
        while (jobIdHex.length() < 8) {
            jobIdHex.insert(0, "0");
        }
        int jobIdParsed = Utility.byteArrayToInt(Utility.hexToBytes(jobIdHex.toString()));

        String username = params.get(0).getAsString();

        String nonce = params.get(2).getAsString();

        return new MiningSubmit(id, username, jobIdParsed, nonce);
    }

    public int getId() {
        return id.getData();
    }

    public String getUsername() {
        return username.getData();
    }

    public int getJobId() {
        return jobId.getData();
    }

    public String getNonce() {
        return nonce.getData();
    }

    public String compileCommand() {
        JsonArray params = new JsonArray();
        params.add(username.getData());

        String jobIdHex = Utility.bytesToHex(Utility.intToByteArray(jobId.getData())).toLowerCase();
        while (jobIdHex.charAt(0) == '0' && jobIdHex.length() > 1) {
            jobIdHex = jobIdHex.substring(1);
        }

        params.add(jobIdHex);

        params.add(nonce.getData());

        JsonObject root = new JsonObject();
        root.addProperty("id", id.getData());
        root.addProperty("method", command.getFriendlyName());
        root.add("params", params);

        return root.toString();
    }
}
