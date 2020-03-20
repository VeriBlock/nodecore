// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.api.ucp.commands.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import nodecore.api.ucp.arguments.UCPArgument;
import nodecore.api.ucp.arguments.UCPArgumentRequestID;
import nodecore.api.ucp.commands.UCPClientCommand;
import nodecore.api.ucp.commands.UCPCommand;
import org.veriblock.core.types.Pair;

import java.util.ArrayList;

public class MiningSubscribeSuccess extends UCPClientCommand {
    private final UCPCommand.Command command = Command.MINING_SUBSCRIBE_SUCCESS; // Not static for serialization purposes

    // Required
    private final UCPArgumentRequestID request_id;

    public MiningSubscribeSuccess(UCPArgument ... arguments) {
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
    }

    public MiningSubscribeSuccess(int request_id) {
        this.request_id = new UCPArgumentRequestID(request_id);
    }

    public static MiningSubscribeSuccess reconstitute(String commandLine) {
        if (commandLine == null) {
            throw new IllegalArgumentException(new Exception().getStackTrace()[0].getClassName() + "'s reconstitute cannot be called with a null commandLine!");
        }

        GsonBuilder deserializer = new GsonBuilder();
        deserializer.registerTypeAdapter(MiningSubscribeSuccess.class, new MiningSubscribeSuccessDeserializer());
        return deserializer.create().fromJson(commandLine, MiningSubscribeSuccess.class);
    }

    public int getRequestId() {
        return request_id.getData();
    }

    public String compileCommand() { return new Gson().toJson(this); }
}
