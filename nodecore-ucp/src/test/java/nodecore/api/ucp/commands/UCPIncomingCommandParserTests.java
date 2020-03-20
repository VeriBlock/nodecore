// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.api.ucp.commands;

import org.junit.Test;

public class UCPIncomingCommandParserTests {

    @Test(expected = InvalidUCPCommandException.class)
    public void parseServerCommand_whenMessageIsNotJson() {
        String notJson = "SELECT * FROM table";
        UCPServerCommand command = UCPIncomingCommandParser.parseServerCommand(notJson);
    }

    @Test(expected = InvalidUCPCommandException.class)
    public void parseServerCommand_whenMessageIsJsonRPC() {
        String json = "{\"params\": [\"ccminer/fpga/2.0.0-linux\"], \"id\": \"1\", \"method\": \"mining.subscribe\"}";
        UCPServerCommand command = UCPIncomingCommandParser.parseServerCommand(json);
    }

    @Test(expected = InvalidUCPCommandException.class)
    public void parseServerCommand_whenInvalidCommandName() {
        String json = "{\"command\": \"mining.subscribe\", \"foo\": \"bar\"}";
        UCPServerCommand command = UCPIncomingCommandParser.parseServerCommand(json);
    }

    @Test(expected = InvalidUCPCommandException.class)
    public void parseServerCommand_whenInvalidCommandMessage() {
        String json = "{\"command\": \"mining_subscribe\", \"foo\": \"bar\"}";
        UCPServerCommand command = UCPIncomingCommandParser.parseServerCommand(json);
    }
}
