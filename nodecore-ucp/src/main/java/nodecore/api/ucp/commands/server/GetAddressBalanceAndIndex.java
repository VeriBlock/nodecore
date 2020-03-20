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
import nodecore.api.ucp.arguments.UCPArgumentAddress;
import nodecore.api.ucp.arguments.UCPArgumentRequestID;
import nodecore.api.ucp.commands.UCPCommand;
import nodecore.api.ucp.commands.UCPServerCommand;
import org.veriblock.core.types.Pair;

import java.util.ArrayList;

public class GetAddressBalanceAndIndex extends UCPServerCommand {
    private final UCPCommand.Command command = Command.GET_ADDRESS_BALANCE_AND_INDEX; // Not static for serialization purposes

    // Required
    private final UCPArgumentRequestID request_id;
    private final UCPArgumentAddress address;


    public GetAddressBalanceAndIndex(UCPArgument ... arguments) {
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
        this.address = (UCPArgumentAddress) arguments[1];
    }

    public GetAddressBalanceAndIndex(int request_id, int start_block, String address) {
        this.request_id = new UCPArgumentRequestID(request_id);
        this.address = new UCPArgumentAddress(address);
    }

    public static GetAddressBalanceAndIndex reconstitute(String commandLine) {
        if (commandLine == null) {
            throw new IllegalArgumentException(new Exception().getStackTrace()[0].getClassName() + "'s reconstitute cannot be called with a null commandLine!");
        }

        GsonBuilder deserializer = new GsonBuilder();
        deserializer.registerTypeAdapter(GetAddressBalanceAndIndex.class, new GetAddressBalanceAndIndexDeserializer());
        return deserializer.create().fromJson(commandLine, GetAddressBalanceAndIndex.class);
    }

    public int getRequestId() {
        return request_id.getData();
    }

    public String getAddress() {
        return address.getData();
    }

    public String compileCommand() { return new Gson().toJson(this); }
}
