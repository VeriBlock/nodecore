// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands;

import nodecore.cli.commands.serialization.FormattableObject;
import nodecore.cli.contracts.AdminService;
import nodecore.cli.contracts.CommandContext;
import nodecore.cli.contracts.OutputWriter;
import nodecore.cli.contracts.ProtocolEndpointType;
import nodecore.cli.contracts.Shell;
import org.jline.utils.AttributedStyle;
import org.veriblock.shell.Command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

public class DefaultCommandContext implements CommandContext {
    private final ShellWriter shellWriter;
    private final List<OutputWriter> outputWriters;

    private Map<String, Object> _data = new HashMap<>();
    private Map<String, Object> _parameters;
    private AdminService _adminService;
    private Shell _shell;

    public DefaultCommandContext(
            Shell shell,
            AdminService adminService,
            Map<String, Object> parameters) {
        _shell = shell;
        _parameters = parameters;
        _adminService = adminService;

        shellWriter = new ShellWriter(shell);
        outputWriters = new ArrayList<>();
        outputWriters.add(shellWriter);

        if (_parameters.get(DefaultCommandFactory.FILENAME_SELECTOR) != null) {
            outputWriters.add(new FileOutputWriter());
        }
    }

    @Override
    public void quit() {
        _data.put("quit", true);
    }

    @Override
    public void outputObject(Object obj) {
        for (OutputWriter writer : outputWriters) {
            writer.outputObject(this, obj);
        }
    }

    @Override
    public boolean isConnected() {
        return _adminService != null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getData(String name) {
        try {
            return (T) _data.get(name);
        } catch (ClassCastException e) {
            return null;
        }
    }

    @Override
    public AdminService adminService() {
        return _adminService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getParameter(String name) {
        try {
            return (T) _parameters.get(name);
        } catch (ClassCastException e) {
            return null;
        }
    }

    @Override
    public <T> void putData(String name, T value) {
        _data.put(name, value);
    }

    @Override
    public ProtocolEndpointType getProtocolType() {
        return _shell.type();
    }

    @Override
    public String passwordPrompt(String prompt) {
        return _shell.passwordPrompt(prompt);
    }
}
