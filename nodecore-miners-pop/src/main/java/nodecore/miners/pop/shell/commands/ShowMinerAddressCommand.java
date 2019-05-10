// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.shell.commands;

import com.google.inject.Inject;
import io.grpc.StatusRuntimeException;
import nodecore.miners.pop.contracts.*;
import nodecore.miners.pop.shell.annotations.CommandSpec;

@CommandSpec(
        name = "Show Miner Address",
        form = "showmineraddress",
        description = "Returns the NodeCore miner address")
public class ShowMinerAddressCommand implements Command {
    private final PoPMiner popMiner;

    @Inject
    public ShowMinerAddressCommand(PoPMiner popMiner) {
        this.popMiner = popMiner;
    }

    @Override
    public Result execute(CommandContext context) throws Exception {

        Result result = new DefaultResult();

        try {
            String minerAddress = popMiner.getMinerAddress();
            if (minerAddress != null) {
                result.addMessage("V200", "Success", minerAddress, false);

                context.writeToOutput("Miner Address: %s", minerAddress);
                context.flush();
            } else {
                result.fail();
                result.addMessage("V412", "NodeCore Not Ready", "NodeCore has not been detected as running", true);
            }
        } catch (StatusRuntimeException e) {
            result.fail();
            result.addMessage("V500", "NodeCore Communication Error", e.getStatus().getDescription(), true);
        }

        return result;
    }
}
