package nodecore.cli.commands.shell

import nodecore.cli.annotations.CommandServiceType
import nodecore.cli.command
import nodecore.cli.contracts.CommandDefinition
import nodecore.cli.contracts.ProtocolEndpointType
import org.veriblock.shell.CommandParameter
import org.veriblock.shell.CommandParameterType
import org.veriblock.shell.Shell
import org.veriblock.shell.command
import org.veriblock.shell.core.failure
import org.veriblock.shell.core.success
import java.util.*

fun Shell.standardCommands() {
    command(
        name = "Clear Screen",
        form = "clear",
        description = "Clears the terminal screen"
    ) {
        clear()
        success()
    }

    command(
        name = "Quit",
        form = "quit",
        description = "Quit the command line shell"
    ) {
        quit()

        success()
    }

    command(
        name = "Help",
        form = "help",
        description = "Returns this help message",
        parameters = listOf(
            CommandParameter(name = "command", type = CommandParameterType.STRING, required = false)
        )
    ) {
        val command: String? = getOptionalParameter("command")

        if (command == null) {
            val categories = HashMap<CommandServiceType, List<CommandDefinition>>()

            for (key in getDefinitions().keys) {
                val def = _factory.getDefinitions().get(key)
                if (!isValidType(def.getSpec().service(), context.getProtocolType()))
                    continue

                if (def.getSpec().requiresConnection() && !context.isConnected())
                    continue

                val list = (categories as java.util.Map<CommandServiceType, List<CommandDefinition>>).computeIfAbsent(
                    def.getSpec().service()
                ) { k -> ArrayList() }
                list.add(def)
                list.sort(Comparator.comparing<CommandDefinition, String> { a -> a.spec.form() })
            }

            context.write().normal("Commands:\n")
            for (category in categories.keys) {
                context.write().inverted(String.format("\n %s: \n", category.name))
                val list = categories[category]
                for (def in list) {
                    context.write().normal(String.format("    %s", def.getSpec().form()))
                    formatParameters(context, def.getParams())
                    context.write().normal("\n")
                }

            }

            printInfo("\n")
            printInfo("    All RPC Commands support following selectors:\n")
            printInfo("        -o <filename>       Saves command output into a file\n")
            printInfo("        Example: getinfo -o abcde.json")
            printInfo("\n")
        } else {
            val def = getCommands()[command]
            if (def != null
                && isValidType(def!!.getSpec().service(), context.getProtocolType())
                && (!def!!.getSpec().requiresConnection() || def!!.getSpec().requiresConnection() && context.isConnected())) {
                context.write().normal(String.format("\nCommand: %s\n", def!!.getSpec().name()))
                context.write().normal(String.format("\n%s", def!!.getSpec().form()))
                formatParameters(context, def!!.getParams())
                context.write().normal(String.format("\n%s\n\n", def!!.getSpec().description()))
            } else {
                failure("V004", "Unknown protocol command", "The command $command is unknown. Type 'help' to view all commands.")
            }
        }
    }
}

private fun isValidType(serviceType: CommandServiceType, protocolType: ProtocolEndpointType): Boolean {
    return (serviceType == CommandServiceType.PEER && protocolType == ProtocolEndpointType.PEER
        || serviceType == CommandServiceType.RPC && protocolType == ProtocolEndpointType.RPC
        || serviceType == CommandServiceType.SHELL)
}
