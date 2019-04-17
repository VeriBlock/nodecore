// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.contracts;

import java.util.Map;

public interface CommandFactory {
    Map<String, CommandDefinition> getDefinitions();

    CommandFactoryResult getInstance(String request);
}
