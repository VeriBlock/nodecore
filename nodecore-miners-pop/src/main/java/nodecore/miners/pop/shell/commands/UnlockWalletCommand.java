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
import nodecore.miners.pop.shell.annotations.CommandParameterType;
import nodecore.miners.pop.shell.annotations.CommandSpec;
import nodecore.miners.pop.shell.annotations.CommandSpecParameter;

@CommandSpec(
        name = "Unlock VeriBlock Wallet",
        form = "unlockwallet",
        description = "Unlocks an encrypted VeriBlock wallet to allow creation of PoP transactions")
@CommandSpecParameter(name = "passphrase", required = true, type = CommandParameterType.PASSWORD)
public class UnlockWalletCommand implements Command {
    private final NodeCoreService nodeCoreService;

    @Inject
    public UnlockWalletCommand(NodeCoreService nodeCoreService) {
        this.nodeCoreService = nodeCoreService;
    }

    @Override
    public Result execute(CommandContext context) throws Exception {
        Result result = new DefaultResult();

        try {
            String passphrase = context.getParameter("passphrase");
            return nodeCoreService.unlockWallet(passphrase);
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
