// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.shell.commands

import com.google.gson.GsonBuilder
import org.veriblock.core.utilities.Utility
import org.veriblock.lite.core.Context
import org.veriblock.miners.pop.service.MinerService
import org.veriblock.miners.pop.util.formatCoinAmount
import org.veriblock.shell.CommandFactory
import org.veriblock.shell.CommandParameter
import org.veriblock.shell.CommandParameterMappers
import org.veriblock.shell.command
import org.veriblock.shell.core.failure
import org.veriblock.shell.core.success

fun CommandFactory.walletCommands(
    context: Context,
    miner: MinerService
) {
    val prettyPrintGson = GsonBuilder().setPrettyPrinting().create()

    command(
        name = "Get Loaded Address",
        form = "getaddress",
        description = "Gets the currently loaded VeriBlock address"
    ) {
        val address = miner.getAddress()
        printInfo(address)

        success()
    }

    command(
        name = "Get Balance",
        form = "getbalance",
        description = "Gets the coin balance for the current VeriBlock address"
    ) {
        val balance = miner.getBalance() ?: run {
            return@command failure {
                addMessage("V010", "Unable to retrieve balance", "Connection to NodeCore is not healthy")
            }
        }
        printInfo("Confirmed balance: ${balance.confirmedBalance.formatCoinAmount()} ${context.vbkTokenName}")
        printInfo("Pending balance changes: ${balance.pendingBalanceChanges.formatCoinAmount()} ${context.vbkTokenName}")

        success()
    }

    command(
        name = "Withdraw VBKs to Address",
        form = "withdrawvbktoaddress",
        description = "Sends a VBK amount to a given address",
        parameters = listOf(
            CommandParameter(name = "amount", mapper = CommandParameterMappers.STRING, required = true),
            CommandParameter(name = "destinationAddress", mapper = CommandParameterMappers.STANDARD_OR_MULTISIG_ADDRESS, required = true),
            CommandParameter(name = "sourceAddress", mapper = CommandParameterMappers.STANDARD_ADDRESS, required = false)
        )
    ) {
        val atomicAmount = Utility.convertDecimalCoinToAtomicLong(getParameter("amount"))
        val destinationAddress = getParameter<String>("destinationAddress")
        val sourceAddress = getParameter<String>("sourceAddress")
        val result = miner.sendCoins(sourceAddress, destinationAddress, atomicAmount)
        result?.let {
            printInfo(prettyPrintGson.toJson(result))
            success()
        } ?: failure {
            addMessage("v404", "Unable to send coins", "Connection to NodeCore is not healthy")
        }
    }
}
