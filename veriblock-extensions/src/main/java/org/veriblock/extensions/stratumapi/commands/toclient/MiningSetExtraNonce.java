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
import org.veriblock.extensions.stratumapi.arguments.StratumArgumentSyntheticExtraNonce;
import org.veriblock.extensions.stratumapi.commands.StratumClientCommand;
import org.veriblock.extensions.stratumapi.commands.StratumCommand;
import org.veriblock.core.types.Pair;

import java.util.ArrayList;

public class MiningSetExtraNonce extends StratumClientCommand {
    private final StratumCommand.Command command = Command.MINING_SET_EXTRA_NONCE; // Not static for serialization purposes

    // Required
    private final StratumArgumentSyntheticExtraNonce syntheticExtraNonce;

    public MiningSetExtraNonce(StratumArgument... arguments) {
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

        this.syntheticExtraNonce = (StratumArgumentSyntheticExtraNonce) arguments[0];
    }

    public MiningSetExtraNonce(String extraNonce) {
        this.syntheticExtraNonce = new StratumArgumentSyntheticExtraNonce(extraNonce);
    }

    public static MiningSetExtraNonce reconstitute(JsonElement rootElement) {
        if (rootElement == null) {
            throw new IllegalArgumentException(new Exception().getStackTrace()[0].getClassName() + "'s reconstitute cannot be called with a null JSON root element!");
        }

        JsonObject rootObject = rootElement.getAsJsonObject();

        JsonArray params = rootObject.get("params").getAsJsonArray();

        String syntheticExtraNonce = params.get(0).getAsString();

        return new MiningSetExtraNonce(syntheticExtraNonce);
    }

    public String getSyntheticExtraNonce() {
        return syntheticExtraNonce.getData();
    }

    public String compileCommand() {
        JsonArray params = new JsonArray();

        params.add(syntheticExtraNonce.getData());

        JsonObject root = new JsonObject();

        // Mining notifications don't need a top-level id
        root.add("id", JsonNull.INSTANCE);

        root.addProperty("method", command.getFriendlyName());

        root.add("params", params);

        return root.toString();
    }
}
