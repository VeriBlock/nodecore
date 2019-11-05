// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.shell

import com.google.common.eventbus.Subscribe
import com.google.gson.GsonBuilder
import com.google.inject.Inject
import nodecore.miners.pop.Configuration
import nodecore.miners.pop.Constants
import nodecore.miners.pop.InternalEventBus
import nodecore.miners.pop.PoPMiner
import nodecore.miners.pop.contracts.MessageEvent
import nodecore.miners.pop.contracts.MessageEvent.Level
import nodecore.miners.pop.contracts.Result
import nodecore.miners.pop.contracts.ResultMessage
import nodecore.miners.pop.events.PoPMinerReadyEvent
import nodecore.miners.pop.events.PoPMiningOperationStateChangedEvent
import nodecore.miners.pop.events.WalletSeedAgreementMissingEvent
import nodecore.miners.pop.services.MessageService
import nodecore.miners.pop.services.NodeCoreService
import nodecore.miners.pop.shell.commands.*
import org.jline.utils.AttributedStyle
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.veriblock.core.utilities.DiagnosticUtility
import org.veriblock.shell.Shell
import java.util.concurrent.CompletableFuture
import kotlin.system.exitProcess

class PopShell @Inject constructor(
    private val miner: PoPMiner,
    private val messageService: MessageService,
    configuration: Configuration,
    nodeCoreService: NodeCoreService
) : Shell() {
    private var messageHandler: CompletableFuture<Void?>? = null
    private var mustAcceptWalletSeed = false

    init {
        InternalEventBus.getInstance().register(this)

        val prettyPrintGson = GsonBuilder().setPrettyPrinting().create()
        standardCommands()
        configCommands(configuration)
        miningCommands(miner, prettyPrintGson)
        bitcoinWalletCommands(miner)
        veriBlockWalletCommands(nodeCoreService, prettyPrintGson)
        diagnosticCommands(miner)
    }

    override fun initialize() {
        printInfo("===[ " + Constants.FULL_APPLICATION_NAME_VERSION + " ]===\n")
        printInfo("https://www.veriblock.org/\n\n")
        val objDiagnostics: Any? = DiagnosticUtility.getDiagnosticInfo()
        val strDiagnostics: String? = GsonBuilder().setPrettyPrinting().create().toJson(objDiagnostics)
        logger.info(strDiagnostics)
        printWarning("WARNING: This miner maintains a lightweight BTC wallet for the purpose of creating PoP transactions only. " +
            "Please deposit minimal amounts of BTC sufficient for mining.\n\n")
    }

    override fun onStart() {
        runOnce()
        watchMessages()
    }

    override fun onStop() {
        messageHandler?.complete(null)
    }

    private fun watchMessages() {
        messageHandler = CompletableFuture.supplyAsync { messageService.messages }
            .thenAccept { messages: List<MessageEvent> -> formatMessages(messages) }
            .thenRun { watchMessages() }
    }

    private fun formatMessages(messages: List<MessageEvent>) {
        for (msg in messages) {
            when {
                Level.ERROR == msg.level -> logger.error(msg.message)
                Level.WARN == msg.level -> logger.warn(msg.message)
                else -> logger.info(msg.message)
            }
        }
    }

    fun runOnce() {
        if (mustAcceptWalletSeed) {
            val walletSeed: List<String?>? = miner.walletSeed
            if (walletSeed != null) {
                printInfo("This application contains a Bitcoin wallet. The seed words which can be used to recover this wallet will be displayed below. Press 'y' to continue...")
                var counter = 0
                while (readLine()!!.toUpperCase() != "Y") {
                    counter++
                    if (counter >= 3) {
                        System.exit(1)
                    }
                    print(listOf<String?>("This application contains a Bitcoin wallet. The seed words which can be used to recover this wallet will be displayed below. Press 'y' to continue..."))
                }
                printInfo("WALLET CREATION TIME:")
                printInfo(String.format("\t%s", walletSeed[0]))
                printInfo("SEED WORDS:")
                walletSeed.subList(1, walletSeed.size).forEach {
                    printInfo("\t$it")
                }
                printInfo("\rThis information will not be displayed again. Please make sure you have recorded them securely. Press 'y' to continue...")
                counter = 0
                while (readLine()!!.toUpperCase() != "Y") {
                    counter++
                    if (counter >= 3) {
                        exitProcess(1)
                    }
                    printInfo("This information will not be displayed again. Please make sure you have recorded them securely. Press 'y' to continue...")
                }
            }
        }
    }

    @Subscribe
    fun onPoPMinerReady(event: PoPMinerReadyEvent?) {
        try {
           printInfo("**********************************************************************************************")
           printInfo("* Ready to start mining. Type 'help' to see available commands. Type 'mine' to start mining. *")
           printInfo("**********************************************************************************************")
        } catch (e: Exception) {
            logger.error(e.message, e)
        }
    }

    @Subscribe
    fun onPoPMiningOperationStateChanged(event: PoPMiningOperationStateChangedEvent) {
        try {
            val operationId: String? = event.state.operationId
            for (s in event.messages) {
                printStyled("[$operationId] $s", AttributedStyle.BOLD.foreground(AttributedStyle.CYAN))
            }
        } catch (e: Exception) {
            logger.error(e.message, e)
        }
    }

    @Subscribe
    fun onWalletSeedAgreementMissing(event: WalletSeedAgreementMissingEvent?) {
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


