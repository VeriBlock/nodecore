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
import org.veriblock.sdk.models.asCoin
import org.veriblock.shell.CommandFactory
import org.veriblock.shell.CommandParameter
import org.veriblock.shell.CommandParameterMappers
import org.veriblock.shell.command
import org.veriblock.shell.core.success
import org.veriblock.spv.model.Output
import org.veriblock.spv.model.asStandardAddress

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
        val result = miner.spvContext.spvService.sendCoins(null, listOf(Output(destinationAddress.asStandardAddress(), atomicAmount.asCoin())))

        printInfo("Transaction id: ${result.map { it.toString() }}")
        success()
    }

    command(
        name = "Import Wallet",
        form = "importwallet",
        description = "Imports a wallet file",
        parameters = listOf(
            CommandParameter(name = "sourceLocation", mapper = CommandParameterMappers.STRING, required = true)
        ),
        suggestedCommands = { listOf("backupwallet") }
    ) {
        val sourceLocation: String = getParameter("sourceLocation")
        val passphrase = shell.passwordPrompt("Enter passphrase of importing wallet (Press ENTER if not password-protected): ")
        miner.spvContext.spvService.importWallet(sourceLocation, passphrase)
        success()
    }

    command(
        name = "Backup Wallet",
        form = "backupwallet",
        description = "Creates a backup from the loaded wallet",
        parameters = listOf(
            CommandParameter(name = "targetLocation", mapper = CommandParameterMappers.STRING, required = true)
        ),
        suggestedCommands = { listOf("importwallet") }
    ) {
        val targetLocation: String = getParameter("targetLocation")
        miner.spvContext.spvService.backupWallet(targetLocation)
        printInfo("Note: The backed-up wallet file is saved on the computer where APM is running.")
        printInfo("Note: If the wallet is encrypted, the backup will require the password in use at the time the backup was created.")
        success()
    }
}
