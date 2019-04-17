// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands;

import java.util.HashMap;

public class Aliases {
    private static HashMap<String, String> aliases = new HashMap<>();
    static {
        aliases.put("?", "help");
        aliases.put("??", "help");
        aliases.put("/?", "help");
        aliases.put("h", "help");
        aliases.put("/h", "help");
        aliases.put("h?", "help");
        aliases.put("showcommands", "help");

        aliases.put("leave", "quit");
        aliases.put("close", "quit");
        aliases.put("exit", "quit");

        aliases.put("dumpprivkey", "dumpprivatekey");
        aliases.put("importprivkey", "importprivatekey");

        aliases.put("getbal", "getbalance");
        aliases.put("bal", "getbalance");
        aliases.put("info", "getinfo");
        aliases.put("inf", "getinfo");
        aliases.put("i", "getinfo");
        aliases.put("stateinfo", "getstateinfo");
        aliases.put("state", "getstateinfo");
        aliases.put("getstate", "getstateinfo");
        aliases.put("sendtoaddress", "send");
        aliases.put("sendtoaddr", "send");
        aliases.put("signatureindex", "sigindex");
        aliases.put("stopsolopool", "stoppool");
        aliases.put("validateaddr", "validateaddress");

        aliases.put("launchnodecore", "startnodecore");
        aliases.put("launchcpuminer", "startcpuminer");
        aliases.put("launchpopminer", "startpopminer");
        aliases.put("startpowminer", "startcpuminer");
        aliases.put("launchpowminer", "startcpuminer");

        aliases.put("multisigaddress", "generatemultisigaddress");
        aliases.put("maddress", "generatemultisigaddress");
        aliases.put("maddr", "generatemultisigaddress");

        aliases.put("unsignedmultisigtx", "makeunsignedmultisigtx");
        aliases.put("makemultisigtx", "makeunsignedmultisigtx");
        aliases.put("multisigtx", "makeunsignedmultisigtx");
        aliases.put("generateunsignedmultisigtx", "makeunsignedmultisigtx");
    }

    public static String getCommand(String alias) {
        return aliases.get(alias.toLowerCase());
    }

    public static boolean containsAlias(String alias) {
        return aliases.containsKey(alias);
    }

}