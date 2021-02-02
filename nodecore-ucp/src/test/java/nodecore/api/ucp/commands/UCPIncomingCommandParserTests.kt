// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.api.ucp.commands

import nodecore.api.ucp.commands.InvalidUCPCommandException
import nodecore.api.ucp.commands.UCPServerCommand
import nodecore.api.ucp.commands.UCPIncomingCommandParser
import org.junit.Test

class UCPIncomingCommandParserTests {
    @Test(expected = InvalidUCPCommandException::class)
    fun parseServerCommand_whenMessageIsNotJson() {
        val notJson = "SELECT * FROM table"
        val command = UCPIncomingCommandParser.parseServerCommand(notJson)
    }

    @Test(expected = InvalidUCPCommandException::class)
    fun parseServerCommand_whenMessageIsJsonRPC() {
        val json = "{\"params\": [\"ccminer/fpga/2.0.0-linux\"], \"id\": \"1\", \"method\": \"mining.subscribe\"}"
        val command = UCPIncomingCommandParser.parseServerCommand(json)
    }

    @Test(expected = InvalidUCPCommandException::class)
    fun parseServerCommand_whenInvalidCommandName() {
        val json = "{\"command\": \"mining.subscribe\", \"foo\": \"bar\"}"
        val command = UCPIncomingCommandParser.parseServerCommand(json)
    }

    @Test(expected = InvalidUCPCommandException::class)
    fun parseServerCommand_whenInvalidCommandMessage() {
        val json = "{\"command\": \"mining_subscribe\", \"foo\": \"bar\"}"
        val command = UCPIncomingCommandParser.parseServerCommand(json)
    }
}
