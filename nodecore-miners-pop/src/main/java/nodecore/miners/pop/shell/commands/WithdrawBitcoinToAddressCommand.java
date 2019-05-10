// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.shell.commands;

import com.google.inject.Inject;
import nodecore.miners.pop.contracts.PoPMiner;
import nodecore.miners.pop.shell.annotations.CommandParameterType;
import nodecore.miners.pop.shell.annotations.CommandSpec;
import nodecore.miners.pop.shell.annotations.CommandSpecParameter;
import nodecore.miners.pop.contracts.Command;
import nodecore.miners.pop.contracts.CommandContext;
import nodecore.miners.pop.contracts.Result;

import java.math.BigDecimal;

@CommandSpec(
        name = "Withdraw Bitcoin to Address",
        form = "withdrawbitcointoaddress",
        description = "Sends a Bitcoin amount to a given address")
@CommandSpecParameter(name = "address", required = true, type = CommandParameterType.STRING)
@CommandSpecParameter(name = "amount", required = true, type = CommandParameterType.AMOUNT)
public class WithdrawBitcoinToAddressCommand implements Command {
    private final PoPMiner popMiner;

    @Inject
    public WithdrawBitcoinToAddressCommand(PoPMiner popMiner) {
        this.popMiner = popMiner;
    }

    @Override
    public Result execute(CommandContext context) throws Exception {
        String address = context.getParameter("address");
        BigDecimal amount = context.getParameter("amount");

        return popMiner.sendBitcoinToAddress(address, amount);
    }
}
