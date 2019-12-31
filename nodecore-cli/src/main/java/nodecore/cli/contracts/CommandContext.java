// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.contracts;

import nodecore.cli.commands.ShellWriter;
import org.veriblock.shell.Command;

import java.util.List;

public interface CommandContext {
    void quit();

    void outputObject(Object arg);

    void outputStatus(String message);

    void suggestCommands(List<Command> suggestions);

    ShellWriter write();

    boolean isConnected();

    <T> T getData(String name);

    AdminService adminService();

    <T> T getParameter(String name);

    <T> void putData(String name, T value);

    ProtocolEndpointType getProtocolType();

    String passwordPrompt(String prompt);
}
