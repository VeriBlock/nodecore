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
import org.veriblock.extensions.stratumapi.arguments.StratumArgumentAgent;
import org.veriblock.extensions.stratumapi.arguments.StratumArgumentId;
import org.veriblock.extensions.stratumapi.arguments.StratumArgumentProtocol;
import org.veriblock.extensions.stratumapi.commands.StratumCommand;
import org.veriblock.extensions.stratumapi.commands.StratumServerCommand;
import org.veriblock.core.types.Pair;

import java.util.ArrayList;

public class MiningSubscribe extends StratumServerCommand {
    private final StratumCommand.Command command = Command.MINING_SUBSCRIBE; // Not static for serialization purposes

    // Required
    private final StratumArgumentId id;
    private final StratumArgumentAgent agent;
    private final StratumArgumentProtocol protocol;

    public MiningSubscribe(StratumArgument... arguments) {
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
        this.agent = (StratumArgumentAgent) arguments[1];
        this.protocol = (StratumArgumentProtocol) arguments[2];
    }

    public MiningSubscribe(int id, String agent, String protocol) {
        this.id = new StratumArgumentId(id);
        this.agent = new StratumArgumentAgent(agent);
        this.protocol = new StratumArgumentProtocol(protocol);
    }

    public static MiningSubscribe reconstitute(JsonElement rootElement) {
        if (rootElement == null) {
            throw new IllegalArgumentException(new Exception().getStackTrace()[0].getClassName() + "'s reconstitute cannot be called with a null JSON root element!");
        }

        JsonObject rootObject = rootElement.getAsJsonObject();

        int id = rootObject.get("id").getAsInt();

        JsonArray params = rootObject.get("params").getAsJsonArray();

        String agent = params.get(0).getAsString();

        String protocol = params.get(1).getAsString();

        return new MiningSubscribe(id, agent, protocol);
    }

    public int getId() {
        return id.getData();
    }

    public String getAgent() {
        return agent.getData();
    }

    public String getProtocol() {
        return protocol.getData();
    }

    public String compileCommand() {
        JsonArray params = new JsonArray();
        params.add(agent.getData());

        params.add(protocol.getData());

        JsonObject root = new JsonObject();
        root.addProperty("id", id.getData());
        root.addProperty("method", command.getFriendlyName());
        root.add("params", params);

        return root.toString();
    }
}
