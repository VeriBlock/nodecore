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
import io.grpc.StatusRuntimeException;
import nodecore.miners.pop.contracts.*;
import nodecore.miners.pop.shell.annotations.CommandSpec;

import java.util.List;

@CommandSpec(
        name = "Get PoP Endoresement Info",
        form = "getpopendorsementinfo",
        description = "Returns information regarding PoP endorsements for a given address")
public class GetPoPEndorsementInfoCommand implements Command {
    private final NodeCoreService popService;

    @Inject
    public GetPoPEndorsementInfoCommand(NodeCoreService popService) {
        this.popService = popService;
    }

    @Override
    public Result execute(CommandContext context) throws Exception {
        Result result = new DefaultResult();

        try {
            List<PoPEndorsementInfo> endorsements = popService.getPoPEndorsementInfo();
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            context.writeToOutput("%s\n\n", gson.toJson(endorsements));
            context.flush();
        } catch (StatusRuntimeException e) {
            result.fail();
            result.addMessage("V500", "NodeCore Communication Error", e.getStatus().getCode().toString(), true);
        } catch (Exception e) {
            result.fail();
            result.addMessage("V500", "Command Error", e.getMessage(), true);
        }

        return result;
    }
}
