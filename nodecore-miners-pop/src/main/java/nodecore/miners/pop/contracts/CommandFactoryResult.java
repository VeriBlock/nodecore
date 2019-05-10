// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.contracts;

import java.util.HashMap;
import java.util.Map;

public class CommandFactoryResult extends DefaultResult {
    private Map<String, Object> _parameters;
    private Command _command;

    public CommandFactoryResult() {
        _parameters = new HashMap<>();
    }

    public void setInstance(Command command) {
        _command = command;
    }

    public Command getInstance() {
        return _command;
    }

    public Map<String, Object> getParameters() {
        return _parameters;
    }
}
