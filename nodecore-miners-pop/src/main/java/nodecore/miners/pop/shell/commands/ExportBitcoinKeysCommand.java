// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.shell.commands;

import com.google.inject.Inject;
import nodecore.miners.pop.contracts.*;
import nodecore.miners.pop.shell.annotations.CommandSpec;

@CommandSpec(
        name = "Export Bitcoin Private Keys",
        form = "exportbitcoinkeys",
        description = "Exports the private keys in the Bitcoin wallet to a specified file in WIF format")
public class ExportBitcoinKeysCommand implements Command {
    private final PoPMiner popMiner;

    @Inject
    public ExportBitcoinKeysCommand(PoPMiner popMiner) {
        this.popMiner = popMiner;
    }

    @Override
    public Result execute(CommandContext context) throws Exception {
        return popMiner.exportBitcoinPrivateKeys();
    }
}