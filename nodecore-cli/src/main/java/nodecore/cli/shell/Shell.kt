package nodecore.cli.shell

import io.grpc.StatusRuntimeException
import nodecore.api.grpc.VeriBlockMessages
import nodecore.cli.commands.DefaultCommandContext
import nodecore.cli.commands.serialization.EmptyPayload
import nodecore.cli.shell.commands.addressCommands
import nodecore.cli.shell.commands.allowedCommands
import nodecore.cli.shell.commands.banCommands
import nodecore.cli.shell.commands.blockCommands
import nodecore.cli.shell.commands.nodeCommands
import nodecore.cli.shell.commands.poolCommands
import nodecore.cli.shell.commands.transactionCommands
import nodecore.cli.shell.commands.walletCommands
import org.veriblock.shell.Command
import org.veriblock.shell.CommandContext
import org.veriblock.shell.CommandParameter
import org.veriblock.shell.Shell
import org.veriblock.shell.core.Result
import org.veriblock.shell.core.failure

fun Shell.configure(
    context: DefaultCommandContext
) {
    banCommands(context)
    nodeCommands(context)
    walletCommands()
    allowedCommands(context)
    poolCommands(context)
    blockCommands(context)
    transactionCommands()
    addressCommands(context)
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
        suggestCommands()
        success(results)
    }
}

fun Shell.cliCommand(
    name: String,
    form: String,
    description: String,
    parameters: List<CommandParameter> = emptyList(),
    suggestedCommands: List<String> = emptyList(),
    action: CommandContext.() -> Result
) {
    val command = Command(name, form, description, parameters, suggestedCommands) {
        try {
            it.action()
        } catch (e: StatusRuntimeException) {
            failure("V800", "Command failure", "Failed to run the command: ${e.message}")
        }
    }
    registerCommand(command)
}
