// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.shell;

import nodecore.miners.pop.contracts.CommandContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultCommandContext implements CommandContext {
    private Map<String, Object> data = new HashMap<>();
    private Map<String, Object> parameters;
    private DefaultShell shell;
    private final List<String> writeBuffer;

    public DefaultCommandContext(
            DefaultShell shell,
            Map<String, Object> parameters) {
        this.shell = shell;
        this.parameters = parameters;
        this.writeBuffer = new ArrayList<>();
    }

    @Override
    public void quit() {
        data.put("quit", true);
    }

    @Override
    public void clear() {
        data.put("clear", true);
    }

    @Override
    public void writeToOutput(String fmt, Object... args) {
        writeBuffer.add(String.format(fmt, args));
    }

    @Override
    public void flush() {
        shell.print(new ArrayList<>(writeBuffer));
        writeBuffer.clear();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getData(String name) {
        try {
            return (T) data.get(name);
        } catch (ClassCastException e) {
            return null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getParameter(String name) {
        try {
            return (T) parameters.get(name);
        } catch (ClassCastException e) {
            return null;
        }
    }

    @Override
    public <T> void putData(String name, T value) {

    }
}
