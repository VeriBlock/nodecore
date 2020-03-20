// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.api.ucp.commands.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import nodecore.api.ucp.arguments.UCPArgument;
import nodecore.api.ucp.arguments.UCPArgumentFrequencyMS;
import nodecore.api.ucp.arguments.UCPArgumentRequestID;
import nodecore.api.ucp.commands.UCPCommand;
import nodecore.api.ucp.commands.UCPServerCommand;
import org.veriblock.core.types.Pair;

import java.util.ArrayList;

public class MiningSubscribe extends UCPServerCommand {
    private final UCPCommand.Command command = Command.MINING_SUBSCRIBE; // Not static for serialization purposes

    // Required
    private final UCPArgumentRequestID request_id;
    private final UCPArgumentFrequencyMS update_frequency_ms;


    public MiningSubscribe(UCPArgument ... arguments) {
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
        this.update_frequency_ms = (UCPArgumentFrequencyMS)arguments[1];
    }

    public MiningSubscribe(int request_id, int update_frequency_ms) {
        this.request_id = new UCPArgumentRequestID(request_id);
        this.update_frequency_ms = new UCPArgumentFrequencyMS(update_frequency_ms);
    }

    public static MiningSubscribe reconstitute(String commandLine) {
        if (commandLine == null) {
            throw new IllegalArgumentException(new Exception().getStackTrace()[0].getClassName() + "'s reconstitute cannot be called with a null commandLine!");
        }

        GsonBuilder deserializer = new GsonBuilder();
        deserializer.registerTypeAdapter(MiningSubscribe.class, new MiningSubscribeDeserializer());
        return deserializer.create().fromJson(commandLine, MiningSubscribe.class);
    }

    public int getRequestId() {
        return request_id.getData();
    }

    public int getUpdateFrequencyMS() { return update_frequency_ms.getData(); }

    public String compileCommand() { return new Gson().toJson(this); }
}
