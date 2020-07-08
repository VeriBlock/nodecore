// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.shell

import com.google.gson.GsonBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.veriblock.core.utilities.DiagnosticUtility
import org.veriblock.miners.pop.EventBus
import org.veriblock.miners.pop.model.result.Result
import org.veriblock.miners.pop.model.result.ResultMessage
import org.veriblock.miners.pop.service.MinerService
import org.veriblock.shell.CommandFactory
import org.veriblock.shell.Shell
import kotlin.system.exitProcess

class PopShell(
    private val minerService: MinerService,
    commandFactory: CommandFactory
) : Shell(commandFactory) {
    private var mustAcceptWalletSeed = false

    init {
        EventBus.walletSeedAgreementMissingEvent.register(this, ::onWalletSeedAgreementMissing)
    }

    override fun initialize() {
        val diagnosticInfo = DiagnosticUtility.getDiagnosticInfo()
        logger.debug(GsonBuilder().setPrettyPrinting().create().toJson(diagnosticInfo))
        printWarning(
            "WARNING: This miner maintains a lightweight BTC wallet for the purpose of creating PoP transactions only. " +
                "Please deposit minimal amounts of BTC sufficient for mining.\n\n"
        )
    }

    fun runOnce() {
        if (mustAcceptWalletSeed) {
            val walletSeed: List<String>? = minerService.getWalletSeed()
            if (walletSeed != null) {
                printInfo(
                    "This application contains a Bitcoin wallet. The seed words which can be used to recover this wallet will be displayed below."
                )
                var counter = 0
                while (prompt("Press 'y' to continue...").toUpperCase() != "Y") {
                    counter++
                    if (counter >= 3) {
                        exitProcess(1)
                    }
                    printInfo(
                        "This application contains a Bitcoin wallet. The seed words which can be used to recover this wallet will be displayed below."
                    )
                }
                printInfo("WALLET CREATION TIME:")
                printInfo(String.format("\t%s", walletSeed[0]))
                printInfo("SEED WORDS:")
                walletSeed.subList(1, walletSeed.size).forEach {
                    printInfo("\t$it")
                }
                printInfo(
                    "\rThis information will not be displayed again. Please make sure you have recorded them securely."
                )
                counter = 0
                while (prompt("Press 'y' to continue...").toUpperCase() != "Y") {
                    counter++
                    if (counter >= 3) {
                        exitProcess(1)
                    }
                    printInfo(
                        "This information will not be displayed again. Please make sure you have recorded them securely."
                    )
                }
            }
        }
    }

    private fun onWalletSeedAgreementMissing() {
        this.mustAcceptWalletSeed = true
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(PopShell::class.java)
        private const val MINING_DELAY_MS: Long = 6000
    }
}

fun Result.toShellResult() = org.veriblock.shell.core.Result(
    didFail()
).also {
    for (message in messages) {
        it.addMessage(message.toShellResultMessage())
    }
}

fun ResultMessage.toShellResultMessage() = org.veriblock.shell.core.ResultMessage(
    code,
    message,
    details,
    isError
)


