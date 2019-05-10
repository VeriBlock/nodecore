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
        name = "Show Bitcoin Address",
        form = "showbitcoinaddress",
        description = "Displays the current address for receiving Bitcoin")
public class ShowBitcoinReceiveAddressCommand implements Command {
    private final PoPMiner popMiner;

    @Inject
    public ShowBitcoinReceiveAddressCommand(PoPMiner popMiner) {
        this.popMiner = popMiner;
    }

    @Override
    public Result execute(CommandContext context) throws Exception {

        Result result = new DefaultResult();

        String address = popMiner.getBitcoinReceiveAddress();
        result.addMessage("V200", "Success", address, false);

        context.writeToOutput("Bitcoin Receive Address: %s", address);
        context.flush();

        return result;
    }
}
