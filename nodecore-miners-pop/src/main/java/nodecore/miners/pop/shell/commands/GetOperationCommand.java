// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.shell.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import nodecore.miners.pop.shell.annotations.CommandParameterType;
import nodecore.miners.pop.shell.annotations.CommandSpec;
import nodecore.miners.pop.shell.annotations.CommandSpecParameter;
import nodecore.miners.pop.contracts.*;
import nodecore.miners.pop.contracts.PoPOperationInfo;

@CommandSpec(
        name = "Get Operation",
        form = "getoperation",
        description = "Gets the details of the supplied operation")
@CommandSpecParameter(name = "id", required = true, type = CommandParameterType.STRING)
public class GetOperationCommand implements Command {
    private final PoPMiner popMiner;

    @Inject
    public GetOperationCommand(PoPMiner popMiner) {
        this.popMiner = popMiner;
    }

    @Override
    public Result execute(CommandContext context) throws Exception {
        Result result = new DefaultResult();

        String id = context.getParameter("id");

        PreservedPoPMiningOperationState state = popMiner.getOperationState(id);
        if (state != null) {
            PoPOperationInfo viewObject = new PoPOperationInfo(state);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            context.writeToOutput("%s\n", gson.toJson(viewObject));
            context.flush();
        } else {
            result.fail();
            result.addMessage("V404", "Not found", String.format("Could not find operation '%s'", id), false);
        }

        return result;
    }
}
