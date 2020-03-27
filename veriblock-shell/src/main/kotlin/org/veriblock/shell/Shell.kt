// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.shell

import com.google.common.base.Stopwatch
import com.google.gson.GsonBuilder
import org.jline.reader.Completer
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.reader.impl.LineReaderImpl
import org.jline.reader.impl.completer.StringsCompleter
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.jline.utils.AttributedStringBuilder
import org.jline.utils.AttributedStyle
import org.jline.utils.InfoCmp
import org.slf4j.LoggerFactory
import org.veriblock.core.utilities.DiagnosticUtility
import org.veriblock.shell.core.ActivityLevel
import org.veriblock.shell.core.Result
import org.veriblock.shell.core.failure
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date

private val logger = LoggerFactory.getLogger(Shell::class.java)
private val printLogger = LoggerFactory.getLogger("shell-printing")

@Suppress("LeakingThis")
open class Shell(
    private val commandFactory: CommandFactory,
    testData: ShellTestData? = null
) {
    private val dateFormatter: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    private var running = false

    private val terminal: Terminal = TerminalBuilder.builder().apply {
        if (testData != null) {
            streams(testData.inputStream, testData.outputStream)
        } else {
            system(true)
        }
    }.build()

    val reader: LineReader = LineReaderBuilder.builder()
        .terminal(terminal)
        .completer(getCompleter())
        .build()

    init {
        currentShell = this
    }

    fun refreshCompleter() {
        (reader as LineReaderImpl).completer = getCompleter()
        reader.setOpt(LineReader.Option.DISABLE_EVENT_EXPANSION)
    }

    protected open fun getPrompt(): String = AttributedStringBuilder()
        .style(AttributedStyle.BOLD.foreground(AttributedStyle.GREEN))
        .append(" > ")
        .toAnsi(terminal)

    fun readLine(): String? = try {
        val read = reader.readLine(getPrompt())
        println(read)
        printLogger.info(read)
        read
    } catch (e: UserInterruptException) {
        null
    } catch (eof: EndOfFileException) {
        null
    }

    fun passwordPrompt(prompt: String): String? = reader.readLine(prompt, '*')

    private fun startRunning() {
        running = true
        onStart()
    }

    protected open fun onStart() {
    }

    private fun stopRunning() {
        onStop()
        running = false
    }

    protected open fun onStop() {
    }

    open fun initialize() {
        val objDiagnostics = DiagnosticUtility.getDiagnosticInfo()
        val strDiagnostics = GsonBuilder().setPrettyPrinting().create().toJson(objDiagnostics)
        logger.debug(strDiagnostics)
    }

    fun run() {
        startRunning()

        while (running) {
            val input = readLine()
                ?: break // Reached EOF/interrupt
            if (input.isEmpty())
                continue

            val stopwatch = Stopwatch.createStarted()

            var clear: Boolean? = null
            val executeResult: Result = try {
                val commandResult = commandFactory.getInstance(input)
                val context = CommandContext(this, commandResult.command, commandResult.parameters)
                var result = commandResult.command.action(context)

                if (!result.isFailed) {
                    if (context.quit) {
                        stopRunning()
                    }

                    clear = context.clear
                }

                result = handleResult(context, result)

                // Suggest commands after the command has been handled and only if it has not failed
                if (!result.isFailed) {
                    context.suggestCommands()
                }

                result
            } catch (se: ShellException) {
                failure {
                    se.messages.forEach {
                        addMessage(it)
                    }
                }
            } catch (e: Exception) {
                handleException(e)
            }

            printResultWithFormat(executeResult)
            if (!executeResult.isFailed) {
                printStyled("200 success ", AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN), false)
            } else {
                printStyled("500 failure ", AttributedStyle.DEFAULT.foreground(AttributedStyle.RED), false)
            }
            printStyled("($stopwatch)\n", AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW))

            if (clear != null && clear) {
                clear()
            }
        }
    }

    protected open fun handleResult(context: CommandContext, result: Result): Result = result

    protected open fun handleException(exception: Exception): Result {
        logger.error("V999: Unhandled Exception", exception)

        return failure {
            addMessage(
                "V999",
                "Unhandled exception",
                exception.toString(),
                true
            )
        }
    }

    private fun clear() {
        terminal.puts(InfoCmp.Capability.clear_screen)
        terminal.flush()
    }

    private fun printLines(messages: List<ShellMessage>) {
        if (messages.isEmpty()) {
            return
        }

        for (msg in messages) {
            terminal.writer().print(
                AttributedStringBuilder()
                    .style(AttributedStyle.BOLD.foreground(msg.getColor()))
                    .append(msg.level.toString().padStart(7, ' '))
                    .toAnsi(terminal)
            )
            terminal.writer().println(
                AttributedStringBuilder()
                    .style(AttributedStyle.DEFAULT.foreground(msg.getColor()))
                    .append(": (" + dateFormatter.format(Date()) + ") " + msg.message)
                    .toAnsi(terminal)
            )
            for (detail in msg.details) {
                terminal.writer().println(
                    AttributedStringBuilder()
                        .style(AttributedStyle.DEFAULT.foreground(msg.getColor()))
                        .append("         $detail")
                        .toAnsi(terminal)
                )
            }
            terminal.writer().flush()

            val printLogMsg = msg.message + msg.details.joinToString(prefix = "\n         ")
            when (msg.level) {
                ActivityLevel.ERROR -> printLogger.error(printLogMsg)
                ActivityLevel.WARN -> printLogger.warn(printLogMsg)
                else -> printLogger.info(printLogMsg)
            }
        }
    }

    protected fun printResultWithFormat(result: Result) {
        val formatted = ArrayList<ShellMessage>()
        for (msg in result.getMessages()) {
            formatted.add(
                ShellMessage(
                    if (msg.isError) ActivityLevel.ERROR else ActivityLevel.INFO,
                    "[${msg.code}] ${msg.message}",
                    msg.details
                )
            )
        }

        printLines(formatted)
    }

    fun printInfo(message: String) {
        printStyled(message, AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
        printLogger.info(message)
    }

    fun printWarning(message: String) {
        printStyled(message, AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW))
        printLogger.warn(message)
    }

    fun printNormal(message: String) {
        printStyled(message, AttributedStyle.BOLD.foreground(AttributedStyle.WHITE))
        printLogger.warn(message)
    }

    fun renderFromThrowable(t: Throwable) {
        printWarning("${t.message}\n\n")
    }

    private val printStyledStringBuilder = StringBuilder()

    @JvmOverloads
    fun printStyled(message: String, style: AttributedStyle, newLine: Boolean = true) {
        printStyledStringBuilder.append(
            AttributedStringBuilder()
                .style(style)
                .append(message)
                .toAnsi(terminal)
        )

        if (newLine) {
            reader.printAbove(printStyledStringBuilder.toString())
            printStyledStringBuilder.clear()
        }
    }

    fun getCommandsByAlias() = commandFactory.getCommands()

    fun getCommands() = commandFactory.getCommands().values.distinct()

    fun getCommand(alias: String) = commandFactory.getCommands()[alias]
        ?: error("Command $alias not found!")

    private fun getCompleter(): Completer {
        val commands = getCommands().asSequence().filter {
            it.shouldAutoComplete()
        }.map {
            it.form.split('|').first()
        }.toList()

        return StringsCompleter(commands)
    }

    protected open fun Command.shouldAutoComplete(): Boolean = true

    fun interrupt() {
        terminal.raise(Terminal.Signal.INT)
    }
}
