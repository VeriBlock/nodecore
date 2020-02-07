package nodecore.cli

import io.grpc.StatusRuntimeException
import nodecore.api.grpc.VeriBlockMessages
import nodecore.cli.annotations.CommandServiceType
import nodecore.cli.commands.rpc.addressCommands
import nodecore.cli.commands.rpc.balanceCommands
import nodecore.cli.commands.rpc.banCommands
import nodecore.cli.commands.rpc.blockCommands
import nodecore.cli.commands.rpc.infoCommands
import nodecore.cli.commands.rpc.nodeCommands
import nodecore.cli.commands.rpc.nodeCoreCommands
import nodecore.cli.commands.rpc.poolCommands
import nodecore.cli.commands.rpc.popCommands
import nodecore.cli.commands.rpc.privateKeyCommands
import nodecore.cli.commands.rpc.signatureCommands
import nodecore.cli.commands.rpc.transactionCommands
import nodecore.cli.commands.rpc.walletCommands
import nodecore.cli.commands.rpc.whitelistCommands
import nodecore.cli.commands.shell.connectionCommands
import nodecore.cli.commands.shell.standardCommands
import nodecore.cli.commands.shell.startApplicationCommands
import nodecore.cli.serialization.EmptyPayload
import org.veriblock.shell.Command
import org.veriblock.shell.CommandContext
import org.veriblock.shell.CommandFactory
import org.veriblock.shell.CommandParameter
import org.veriblock.shell.core.Result
import org.veriblock.shell.core.failure

fun CommandFactory.registerCommands() {
    // Shell commands
    standardCommands()
    startApplicationCommands()
    connectionCommands()
    // RPC commands
    addressCommands()
    balanceCommands()
    banCommands()
    blockCommands()
    infoCommands()
    nodeCommands()
    nodeCoreCommands()
    poolCommands()
    popCommands()
    privateKeyCommands()
    signatureCommands()
    transactionCommands()
    walletCommands()
    whitelistCommands()
}

fun failure(results: List<VeriBlockMessages.Result>) = org.veriblock.shell.core.failure {
    for (r in results) {
        addMessage(r.code, r.message, r.details, r.error)
    }
}

fun success(results: List<VeriBlockMessages.Result>) = org.veriblock.shell.core.success {
    for (r in results) {
        addMessage(r.code, r.message, r.details, r.error)
    }
}

inline fun CommandContext.prepareResult(
    success: Boolean,
    results: List<VeriBlockMessages.Result>,
    payloadSupplier: () -> Any = { EmptyPayload() }
): Result {
    return if (!success) {
        failure(results)
    } else {
        outputObject(payloadSupplier())
        success(results)
    }
}

val CommandContext.cliShell get() = shell as CliShell

fun CommandFactory.cliCommand(
    name: String,
    form: String,
    description: String,
    parameters: List<CommandParameter> = emptyList(),
    suggestedCommands: () -> List<String> = { emptyList() },
    commandServiceType: CommandServiceType = CommandServiceType.SHELL,
    action: CommandContext.() -> Result
) {
    val command = Command(name, form, description, parameters, suggestedCommands, commandServiceType.name) {
        try {
            it.action()
        } catch (e: StatusRuntimeException) {
            failure("V800", "Command failure", "Failed to run the command: ${e.message}")
        }
    }
    registerCommand(command)
}

fun CommandFactory.rpcCommand(
    name: String,
    form: String,
    description: String,
    parameters: List<CommandParameter> = emptyList(),
    suggestedCommands: () -> List<String> = { emptyList() },
    action: CommandContext.() -> Result
) = cliCommand(name, form, description, parameters, suggestedCommands, CommandServiceType.RPC, action)
