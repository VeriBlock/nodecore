package nodecore.cli.commands.shell

import nodecore.cli.CliShell
import nodecore.cli.annotations.CommandServiceType
import nodecore.cli.cliCommand
import nodecore.cli.contracts.ProtocolEndpointType
import org.jline.utils.AttributedStyle
import org.veriblock.shell.Command
import org.veriblock.shell.CommandParameter
import org.veriblock.shell.CommandParameterMappers
import org.veriblock.shell.core.failure
import org.veriblock.shell.core.success
import java.util.*

fun CliShell.standardCommands() {
    cliCommand(
        name = "Clear Screen",
        form = "clear",
        description = "Clears the terminal screen"
    ) {
        clear()
        success()
    }

    cliCommand(
        name = "Quit",
        form = "quit|leave|close|exit",
        description = "Quit the command line shell"
    ) {
        quit()

        success()
    }

    cliCommand(
        name = "Help",
        form = "help|?|/?|h|/h|h?|showcommands",
        description = "Returns this help message",
        parameters = listOf(
            CommandParameter(name = "command", mapper = CommandParameterMappers.STRING, required = false)
        )
    ) {
        val command: String? = getOptionalParameter("command")

        if (command == null) {
            val categories = HashMap<CommandServiceType, MutableList<Command>>()

            for (def in getCommands()) {
                val commandServiceType = CommandServiceType.valueOf(
                    checkNotNull(def.extraData) { "Command $def's extra data must not be null!" }
                )

                val requiresConnection = commandServiceType == CommandServiceType.RPC

                if (!isValidType(commandServiceType, getProtocolType()))
                    continue

                if (requiresConnection && !isConnected())
                    continue

                val list = categories.getOrPut(commandServiceType) { ArrayList() }
                list.add(def)
                list.sortBy { it.form }
            }

            print("Commands:\n")
            for ((category, list) in categories) {
                printStyled("\n ${category.name}: ", AttributedStyle.INVERSE.foreground(AttributedStyle.WHITE))
                for (def in list) {
                    printStyled("    ${def.form.split("|").first()}", AttributedStyle.DEFAULT.foreground(AttributedStyle.WHITE), newLine = false)
                    formatParameters(def.parameters)
                    printInfo("")
                }
            }

            print("\n")
            print("    All RPC Commands support following selectors:\n")
            print("        -o <filename>       Saves command output into a file\n")
            print("        Example: getinfo -o abcde.json\n\n")
            success()
        } else {
            val def = getCommandsByAlias()[command]
            if (def != null) {
                val commandServiceType = CommandServiceType.valueOf(
                    checkNotNull(def.extraData) { "Command $def's extra data must not be null!" }
                )
                val requiresConnection = commandServiceType == CommandServiceType.RPC
                if (
                    isValidType(commandServiceType, getProtocolType())
                    && (!requiresConnection || (requiresConnection && isConnected()))
                ) {
                    printStyled("\nCommand: ${def.name}", AttributedStyle.DEFAULT.foreground(AttributedStyle.WHITE))
                    printStyled("\n${def.form}", AttributedStyle.DEFAULT.foreground(AttributedStyle.WHITE), newLine = false)
                    formatParameters(def.parameters)
                    printStyled("\n${def.description}\n", AttributedStyle.DEFAULT.foreground(AttributedStyle.WHITE))
                    success()
                } else {
                    failure("V004", "Unknown command", "The command $command is unknown. Type 'help' to view all commands.")
                }
            } else {
                failure("V004", "Unknown command", "The command $command is unknown. Type 'help' to view all commands.")
            }
        }
    }
}

private fun isValidType(serviceType: CommandServiceType, protocolType: ProtocolEndpointType): Boolean {
    return (serviceType == CommandServiceType.PEER && protocolType == ProtocolEndpointType.PEER
        || serviceType == CommandServiceType.RPC && protocolType == ProtocolEndpointType.RPC
        || serviceType == CommandServiceType.SHELL)
}

private fun CliShell.formatParameters(params: List<CommandParameter>) {
    if (params.isNotEmpty()) {
        printStyled(" ", AttributedStyle.DEFAULT.foreground(AttributedStyle.WHITE), newLine = false)
        for (param in params) {
            if (param.required) {
                printStyled("<${param.name}> ", AttributedStyle.DEFAULT.foreground(AttributedStyle.WHITE), newLine = false)
            } else {
                printStyled("[${param.name}] ", AttributedStyle.DEFAULT.foreground(AttributedStyle.WHITE), newLine = false)
            }
        }
    }
}
