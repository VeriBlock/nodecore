// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.shell.commands

import kotlinx.coroutines.runBlocking
import org.veriblock.miners.pop.common.formatBTCFriendlyString
import org.veriblock.miners.pop.service.MinerService
import org.veriblock.miners.pop.shell.toShellResult
import org.veriblock.shell.CommandFactory
import org.veriblock.shell.CommandParameter
import org.veriblock.shell.CommandParameterMappers
import org.veriblock.shell.command
import org.veriblock.shell.core.failure
import org.veriblock.shell.core.success
import java.math.BigDecimal

fun CommandFactory.bitcoinWalletCommands(
    minerService: MinerService
) {
    command(
        name = "Show Bitcoin Balance",
        form = "showbitcoinbalance",
        description = "Displays the current balance for the Bitcoin wallet"
    ) {
        val bitcoinBalance = minerService.getBitcoinBalance()
        val formattedBalance = bitcoinBalance.formatBTCFriendlyString()
        printInfo("Bitcoin Balance: $formattedBalance")
        val bitcoinPendingBalance = minerService.getBitcoinPendingBalance()
        val formattedPendingBalance = bitcoinPendingBalance.formatBTCFriendlyString()
        printInfo("Bitcoin Pending Balance: $formattedPendingBalance")
        success {
            addMessage("V200", "Success", formattedBalance)
        }
    }

    command(
        name = "Show Bitcoin Address",
        form = "showbitcoinaddress",
        description = "Displays the current address for receiving Bitcoin"
    ) {
        val address = minerService.getBitcoinReceiveAddress()
        printInfo("Bitcoin Receive Address: $address")
        success {
            addMessage("V200", "Success", address)
        }
    }

    command(
        name = "Import Bitcoin Wallet",
        form = "importwallet",
        description = "Imports a Bitcoin wallet using comma-separated list of seed words and, optionally, a wallet creation date",
        parameters = listOf(
            CommandParameter("seedWords", CommandParameterMappers.STRING),
            CommandParameter("creationTime", CommandParameterMappers.LONG, false)
        )
    ) {
        val seedWords: String = getParameter("seedWords")
        val words = seedWords.split(",")
        if (words.size != 12) {
            return@command failure {
                addMessage("V400", "Invalid input", "The seed words parameter should contain 12 words in a comma-separated format (no spaces)", true)
            }
        }

        val creationTime: Long? = getOptionalParameter("creationTime")
        if (!minerService.importWallet(words, creationTime)) {
            return@command failure {
                addMessage("V500", "Unable to Import", "Unable to import the wallet from the seed supplied. Check the logs for more detail.", true)
            }
        }

        success()
    }

    command(
        name = "Withdraw Bitcoin to Address",
        form = "withdrawbitcointoaddress",
        description = "Sends a Bitcoin amount to a given address",
        parameters = listOf(
            CommandParameter("address", CommandParameterMappers.STRING),
            CommandParameter("amount", CommandParameterMappers.AMOUNT)
        )
    ) {
        val address: String = getParameter("address")
        val amount: BigDecimal = getParameter("amount")
        runBlocking {
            minerService.sendBitcoinToAddress(address, amount).toShellResult()
        }
    }

    command(
        name = "Export Bitcoin Private Keys",
        form = "exportbitcoinkeys",
        description = "Exports the private keys in the Bitcoin wallet to a specified file in WIF format"
    ) {
        minerService.exportBitcoinPrivateKeys().toShellResult()
    }

    command(
        name = "Reset Bitcoin Wallet",
        form = "resetwallet",
        description = "Resets the Bitcoin wallet, marking it for resync"
    ) {
        minerService.resetBitcoinWallet().toShellResult()
    }
}
