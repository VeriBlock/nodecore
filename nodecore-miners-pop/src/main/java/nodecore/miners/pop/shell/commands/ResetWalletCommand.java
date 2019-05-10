// VeriBlock PoP Miner
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
        name = "Reset Bitcoin Wallet",
        form = "resetwallet",
        description = "Resets the Bitcoin wallet, marking it for resync.")
public class ResetWalletCommand implements Command {
    private final PoPMiner popMiner;

    @Inject
    public ResetWalletCommand(PoPMiner popMiner) {
        this.popMiner = popMiner;
    }

    @Override
    public Result execute(CommandContext context) throws Exception {
        return popMiner.resetBitcoinWallet();
    }
}
