// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.extensions.stratumapi.commands.toclient;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import org.veriblock.extensions.stratumapi.arguments.StratumArgument;
import org.veriblock.extensions.stratumapi.arguments.StratumArgumentBlockHeight;
import org.veriblock.extensions.stratumapi.arguments.StratumArgumentBoolean;
import org.veriblock.extensions.stratumapi.arguments.StratumArgumentHeaderHash;
import org.veriblock.extensions.stratumapi.arguments.StratumArgumentJobId;
import org.veriblock.extensions.stratumapi.arguments.StratumArgumentSeedHash;
import org.veriblock.extensions.stratumapi.commands.StratumClientCommand;
import org.veriblock.extensions.stratumapi.commands.StratumCommand;
import org.veriblock.core.types.Pair;
import org.veriblock.core.utilities.Utility;

import java.util.ArrayList;

public class MiningNotify extends StratumClientCommand {
    private final StratumCommand.Command command = Command.MINING_NOTIFY; // Not static for serialization purposes

    // Required
    private final StratumArgumentJobId jobId;
    private final StratumArgumentSeedHash seedHash;
    private final StratumArgumentHeaderHash headerHash;
    private final StratumArgumentBlockHeight blockHeight;
    private final StratumArgumentBoolean cleanJobs;

    public MiningNotify(StratumArgument... arguments) {
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

        this.jobId = (StratumArgumentJobId) arguments[0];
        this.seedHash = (StratumArgumentSeedHash) arguments[1];
        this.headerHash = (StratumArgumentHeaderHash) arguments[2];
        this.blockHeight = (StratumArgumentBlockHeight) arguments[3];
        this.cleanJobs = (StratumArgumentBoolean) arguments[4];
    }

    public MiningNotify(int jobId, String seedHash, String headerHash, int blockHeight, boolean cleanJobs) {
        this.jobId = new StratumArgumentJobId(jobId);
        this.seedHash = new StratumArgumentSeedHash(seedHash);
        this.headerHash = new StratumArgumentHeaderHash(headerHash);
        this.blockHeight = new StratumArgumentBlockHeight(blockHeight);
        this.cleanJobs = new StratumArgumentBoolean(cleanJobs);
    }

    public static MiningNotify reconstitute(JsonElement rootElement) {
        if (rootElement == null) {
            throw new IllegalArgumentException(new Exception().getStackTrace()[0].getClassName() + "'s reconstitute cannot be called with a null JSON root element!");
        }

        JsonObject rootObject = rootElement.getAsJsonObject();

        JsonArray params = rootObject.get("params").getAsJsonArray();

        StringBuilder jobIdHex = new StringBuilder(params.get(0).getAsString());
        while (jobIdHex.length() < 8) {
            jobIdHex.insert(0, "0");
        }
        int jobIdParsed = Utility.byteArrayToInt(Utility.hexToBytes(jobIdHex.toString()));

        String seedHash = params.get(1).getAsString();

        String headerHash = params.get(2).getAsString();

        int blockHeight = params.get(3).getAsInt();

        boolean cleanJobs = Boolean.parseBoolean(params.get(4).getAsString());

        return new MiningNotify(jobIdParsed, seedHash, headerHash, blockHeight, cleanJobs);
    }

    public int getJobId() {
        return jobId.getData();
    }

    public String getSeedHash() {
        return seedHash.getData();
    }

    public String getHeaderHash() {
        return headerHash.getData();
    }

    public boolean getCleanJobs() {
        return cleanJobs.getData();
    }

    public String compileCommand() {
        JsonArray params = new JsonArray();

        String jobIdHex = Utility.bytesToHex(Utility.intToByteArray(jobId.getData())).toLowerCase();
        while (jobIdHex.charAt(0) == '0' && jobIdHex.length() > 1) {
            jobIdHex = jobIdHex.substring(1);
        }

        params.add(jobIdHex);
        params.add(seedHash.getData());
        params.add(headerHash.getData());
        params.add(blockHeight.getData());

        params.add(cleanJobs.getData());

        JsonObject root = new JsonObject();

        // Mining notifications don't need a top-level id
        root.add("id", JsonNull.INSTANCE);

        root.addProperty("method", command.getFriendlyName());

        root.add("params", params);

        return root.toString();
    }
}
