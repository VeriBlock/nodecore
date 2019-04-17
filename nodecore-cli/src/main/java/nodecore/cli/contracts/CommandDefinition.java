// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.contracts;

import nodecore.cli.annotations.CommandSpec;
import nodecore.cli.annotations.CommandSpecParameter;

public class CommandDefinition {
    private CommandSpec _spec;
    private Class<Command> _class;
    private CommandSpecParameter[] _params;

    public CommandDefinition(
            Class<Command> commandClass,
            CommandSpec spec,
            CommandSpecParameter[] params) {
        _spec = spec;
        _params = params;
        _class = commandClass;
    }

    public CommandSpec getSpec() {
        return _spec;
    }

    public Class<Command> getCommandClass() {
        return _class;
    }

    public CommandSpecParameter[] getParams() {
        return _params;
    }
}
