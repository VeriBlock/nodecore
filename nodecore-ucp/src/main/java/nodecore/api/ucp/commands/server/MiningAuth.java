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
import nodecore.api.ucp.arguments.UCPArgumentPassword;
import nodecore.api.ucp.arguments.UCPArgumentRequestID;
import nodecore.api.ucp.arguments.UCPArgumentUsername;
import nodecore.api.ucp.commands.UCPCommand;
import nodecore.api.ucp.commands.UCPServerCommand;
import org.veriblock.core.types.Pair;

import java.util.ArrayList;

public class MiningAuth extends UCPServerCommand {
    private final UCPCommand.Command command = Command.MINING_AUTH; // Not static for serialization purposes

    // Required
    private final UCPArgumentRequestID request_id;
    private final UCPArgumentUsername username;
    private final UCPArgumentPassword password;


    public MiningAuth(UCPArgument ... arguments) {
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
        this.username = (UCPArgumentUsername) arguments[1];
        this.password = (UCPArgumentPassword) arguments[2];
    }

    public MiningAuth(int request_id, String username, String password) {
        this.request_id = new UCPArgumentRequestID(request_id);
        this.username = new UCPArgumentUsername(username);
        this.password = new UCPArgumentPassword(password);
    }

    public static MiningAuth reconstitute(String commandLine) {
        if (commandLine == null) {
            throw new IllegalArgumentException(new Exception().getStackTrace()[0].getClassName() + "'s reconstitute cannot be called with a null commandLine!");
        }

        GsonBuilder deserializer = new GsonBuilder();
        deserializer.registerTypeAdapter(MiningAuth.class, new MiningAuthDeserializer());
        return deserializer.create().fromJson(commandLine, MiningAuth.class);
    }

    public int getRequestId() {
        return request_id.getData();
    }

    public String getUsername() {
        return username.getData();
    }

    public String getPassword() {
        return password.getData();
    }

    public String compileCommand() { return new Gson().toJson(this); }
}
