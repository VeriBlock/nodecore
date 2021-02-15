package org.veriblock.spv.standalone.commands

import org.jline.utils.AttributedStyle
import org.veriblock.shell.CommandFactory
import org.veriblock.shell.CommandParameter
import org.veriblock.shell.CommandParameterMappers
import org.veriblock.shell.Shell
import org.veriblock.shell.command
import org.veriblock.shell.core.failure
import org.veriblock.shell.core.success

fun CommandFactory.standardCommands() {
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
        form = "quit|leave|close|exit",
        description = "Quit the command line shell"
    ) {
        quit()

        success()
    }

    command(
        name = "Help",
        form = "help|?|/?|h|/h|h?|showcommands",
        description = "Displays this help message",
        parameters = listOf(
            CommandParameter(name = "command", mapper = CommandParameterMappers.STRING, required = false)
        )
    ) {
        val command: String? = getOptionalParameter("command")
        if (command == null) {
            shell.printNormal("See wiki for more detail: https://wiki.veriblock.org/index.php/HowTo_run_SPV")
            shell.printNormal("Commands:")
            for (def in shell.getCommands()){
                shell.printStyled("    $def", AttributedStyle.DEFAULT.foreground(AttributedStyle.WHITE))
            }

            success()
        } else {
            val def = shell.getCommandsByAlias()[command]
            if (def != null) {
                shell.printStyled("\nCommand: ${def.name}", AttributedStyle.DEFAULT.foreground(AttributedStyle.WHITE), newLine = false)
                shell.printStyled("\nUsage: ${def.form}", AttributedStyle.DEFAULT.foreground(AttributedStyle.WHITE), newLine = false)
                shell.formatParameters(def.parameters)
                shell.printStyled("\nDescription: ${def.description}\n", AttributedStyle.DEFAULT.foreground(AttributedStyle.WHITE))
                success()
            } else {
                failure("V004", "Unknown command", "The command $command is unknown. Type 'help' to view all commands.")
            }
        }
    }
}

private fun Shell.formatParameters(params: List<CommandParameter>) {
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
