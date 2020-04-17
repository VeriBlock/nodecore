// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.extensions.stratumapi.commands.toserver;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.veriblock.extensions.stratumapi.arguments.StratumArgument;
import org.veriblock.extensions.stratumapi.arguments.StratumArgumentAgent;
import org.veriblock.extensions.stratumapi.arguments.StratumArgumentHost;
import org.veriblock.extensions.stratumapi.arguments.StratumArgumentId;
import org.veriblock.extensions.stratumapi.arguments.StratumArgumentPort;
import org.veriblock.extensions.stratumapi.arguments.StratumArgumentProtocol;
import org.veriblock.extensions.stratumapi.commands.StratumCommand;
import org.veriblock.extensions.stratumapi.commands.StratumServerCommand;
import org.veriblock.core.types.Pair;
import org.veriblock.core.utilities.Utility;

import java.util.ArrayList;

/**
 * Note: MiningHello is not used by EthereumStratum/1.0.0, so not currently expected to be used by any VBK mining software.
 */
public class MiningHello extends StratumServerCommand {
    private static final StratumCommand.Command command = Command.MINING_HELLO; // Not static for serialization purposes

    // Required
    private final StratumArgumentId id;
    private final StratumArgumentAgent agent;
    private final StratumArgumentHost host;
    private final StratumArgumentPort port;
    private final StratumArgumentProtocol protocol;

    public MiningHello(StratumArgument... arguments) {
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
        this.host = (StratumArgumentHost) arguments[2];
        this.port = (StratumArgumentPort) arguments[3];
        this.protocol = (StratumArgumentProtocol) arguments[4];
    }

    public MiningHello(int id, String agent, String host, int port, String protocol) {
        this.id = new StratumArgumentId(id);
        this.agent = new StratumArgumentAgent(agent);
        this.host = new StratumArgumentHost(host);
        this.port = new StratumArgumentPort(port);
        this.protocol = new StratumArgumentProtocol(protocol);
    }

    public static MiningHello reconstitute(JsonElement rootElement) {
        if (rootElement == null) {
            throw new IllegalArgumentException(new Exception().getStackTrace()[0].getClassName() +
                "'s reconstitute cannot be called with a null JSON root element!");
        }

        JsonObject rootObject = rootElement.getAsJsonObject();

        int id = rootObject.get("id").getAsInt();

        JsonObject params = rootObject.get("params").getAsJsonObject();

        String agent = params.get("agent").getAsString();
        String host = params.get("host").getAsString();
        StringBuilder portHex = new StringBuilder(params.get("port").getAsString());

        while (portHex.length() != 8) { // 4 bytes in an Integer
            portHex.insert(0, "0");
        }

        int port = Utility.byteArrayToInt(Utility.hexToBytes(portHex.toString()));

        String protocol = params.get("proto").getAsString();

        return new MiningHello(id, agent, host, port, protocol);
    }

    public int getId() {
        return id.getData();
    }

    public String getAgent() {
        return agent.getData();
    }

    public String getHost() {
        return host.getData();
    }

    public int getPort() {
        return port.getData();
    }

    public String getProtocol() {
        return protocol.getData();
    }

    @Override
    public String toString() {
        JsonObject params = new JsonObject();
        params.addProperty("agent", agent.getData());
        params.addProperty("host", host.getData());

        String portHex = Utility.bytesToHex(Utility.intToByteArray(port.getData())).toLowerCase();
        while (portHex.charAt(0) == '0' && portHex.length() > 1) {
            portHex = portHex.substring(1);
        }

        params.addProperty("port", portHex);

        params.addProperty("proto", protocol.getData());

        JsonObject root = new JsonObject();
        root.addProperty("id", id.getData());
        root.addProperty("method", command.getFriendlyName());
        root.add("params", params);

        return root.toString();
    }
}
