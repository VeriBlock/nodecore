package nodecore.cli.commands.shell

import nodecore.cli.cliCommand
import nodecore.cli.utilities.ExtendedIllegalStateException
import nodecore.cli.utilities.ExternalProgramUtilities
import org.veriblock.shell.CommandFactory
import org.veriblock.shell.core.ResultMessage
import org.veriblock.shell.core.failure
import org.veriblock.shell.core.success
import java.awt.GraphicsEnvironment

fun CommandFactory.startApplicationCommands() {
    cliCommand(
        name = "Start NodeCore",
        form = "startnodecore|launchnodecore",
        description = "Attempts to start NodeCore in a new window"
    ) {
        if (GraphicsEnvironment.isHeadless()) {
            failure(
                "V004",
                "startnodecore command unavailable!",
                "The startnodecore command is not available in headless mode!"
            )
        } else {
            try {
                val successFile = ExternalProgramUtilities.startupExternalProcess("../../", "nodecore-0", "nodecore", "NodeCore")
                outputObject(ResultMessage("V200", "Started", "Successfully started NodeCore $successFile", false))
                shell.printWarning("Please note that NodeCore may take several minutes to load before you can connect to it!")
                success()
            } catch (e: ExtendedIllegalStateException) {
                failure("V004", e.message, e.extraMessage)
            }
        }
    }

    cliCommand(
        name = "Start CPU Miner",
        form = "startcpuminer|launchcpuminer",
        description = "Attempts to start the CPU PoW Miner in a new window",
        suggestedCommands = { listOf("startpool", "startsolopool") }
    ) {
        if (GraphicsEnvironment.isHeadless()) {
            failure(
                "V004",
                "startcpuminer command unavailable!",
                "The startcpuminer command is not available in headless mode!"
            )
        } else {
            try {
                val successFile = ExternalProgramUtilities.startupExternalProcess("../../", "nodecore-pow-0", "nodecore-pow", "NodeCore CPU PoW Miner")
                outputObject(ResultMessage("V200", "Started", "Successfully started the NodeCore PoW CPU Miner from location $successFile", false))

                success()
            } catch (e: ExtendedIllegalStateException) {
                failure("V004", e.message, e.extraMessage)
            }
        }
    }

    cliCommand(
        name = "Start PoP Miner",
        form = "startpopminer|launchpopminer|startpowminer|launchpowminer",
        description = "Attempts to start the PoP Miner in a new window"
    ) {
        if (GraphicsEnvironment.isHeadless()) {
            failure(
                "V004",
                "startpopminer command unavailable!",
                "The startpopminer command is not available in headless mode!"
            )
        } else {
            try {
                val successFile = ExternalProgramUtilities.startupExternalProcess("../../", "nodecore-pop-0", "nodecore-pop", "NodeCore PoP Miner")
                outputObject(ResultMessage("V200", "Started", "Successfully started the NodeCore PoP Miner from location $successFile", false))

                success()
            } catch (e: ExtendedIllegalStateException) {
                failure("V004", e.message, e.extraMessage)
            }
        }
    }
}
