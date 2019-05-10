// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.shell.commands;

import com.google.inject.Inject;
import nodecore.miners.pop.common.Utility;
import nodecore.miners.pop.contracts.*;
import nodecore.miners.pop.shell.annotations.CommandSpec;
import org.bitcoinj.core.Coin;

@CommandSpec(
        name = "Show Bitcoin Balance",
        form = "showbitcoinbalance",
        description = "Displays the current balance for the Bitcoin wallet")
public class ShowBitcoinBalanceCommand implements Command {
    private final PoPMiner popMiner;

    @Inject
    public ShowBitcoinBalanceCommand(PoPMiner popMiner) {
        this.popMiner = popMiner;
    }

    @Override
    public Result execute(CommandContext context) throws Exception {
        Result result = new DefaultResult();

        Coin bitcoinBalance = popMiner.getBitcoinBalance();

        result.addMessage("V200", "Success", Utility.formatBTCFriendlyString(bitcoinBalance), false);

        context.writeToOutput("Bitcoin Balance: %s", Utility.formatBTCFriendlyString(bitcoinBalance));
        context.flush();

        return result;
    }
}
