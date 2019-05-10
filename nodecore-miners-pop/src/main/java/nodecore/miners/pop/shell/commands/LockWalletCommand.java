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
        name = "Lock VeriBlock Wallet",
        form = "lockwallet",
        description = "Locks an encrypted VeriBlock wallet to disable creation of PoP transactions")
public class LockWalletCommand implements Command {
    private final NodeCoreService nodeCoreService;

    @Inject
    public LockWalletCommand(NodeCoreService nodeCoreService) {
        this.nodeCoreService = nodeCoreService;
    }

    @Override
    public Result execute(CommandContext context) throws Exception {
        Result result = new DefaultResult();

        try {
            return nodeCoreService.lockWallet();
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