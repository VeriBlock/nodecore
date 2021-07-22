// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.shell.commands

import org.veriblock.core.utilities.Utility
import org.veriblock.miners.pop.core.ApmContext
import org.veriblock.miners.pop.service.AltchainPopMinerService
import org.veriblock.miners.pop.util.formatCoinAmount
import org.veriblock.shell.CommandFactory
import org.veriblock.shell.CommandParameter
import org.veriblock.shell.CommandParameterMappers
import org.veriblock.shell.command
import org.veriblock.shell.core.success

fun CommandFactory.walletCommands(
    context: ApmContext,
    miner: AltchainPopMinerService
) {
    command(
        name = "Get Loaded Address",
        form = "getaddress",
        description = "Gets the currently loaded VeriBlock address"
    ) {
        val address = miner.getAddress()
        printInfo("${context.vbkTokenName} address")
        printInfo(address)

        success()
    }

    command(
        name = "Get Balance",
        form = "getbalance",
        description = "Gets the coin balance for the current VeriBlock address"
    ) {
        val balance = miner.getBalance()
        printInfo("Confirmed balance: ${balance.confirmedBalance.formatCoinAmount()} ${context.vbkTokenName}")
        printInfo("Pending balance changes: ${balance.pendingBalanceChanges.formatCoinAmount()} ${context.vbkTokenName}")

        success()
    }

    command(
        name = "Withdraw VBKs to Address",
        form = "withdrawvbktoaddress",
        description = "Sends a VBK amount to a given address",
        parameters = listOf(
            CommandParameter(name = "destinationAddress", mapper = CommandParameterMappers.STANDARD_OR_MULTISIG_ADDRESS, required = true),
            CommandParameter(name = "amount", mapper = CommandParameterMappers.STRING, required = true)
        )
    ) {
        val atomicAmount = Utility.convertDecimalCoinToAtomicLong(getParameter("amount"))
        val destinationAddress: String = getParameter("destinationAddress")
        val result = miner.sendCoins(destinationAddress, atomicAmount)
        printInfo(result.joinToString())
        success()

    }
}
