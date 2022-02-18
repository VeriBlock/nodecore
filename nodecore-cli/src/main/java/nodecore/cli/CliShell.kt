// VeriBlock NodeCore CLI
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nodecore.cli.annotations.CommandServiceType
import nodecore.cli.contracts.AdminService
import nodecore.cli.contracts.ConnectionFailedException
import nodecore.cli.contracts.ProtocolEndpoint
import nodecore.cli.contracts.ProtocolEndpointType
import nodecore.cli.services.AdminServiceClient
import org.jline.reader.LineReader
import org.jline.utils.AttributedStringBuilder
import org.jline.utils.AttributedStyle
import org.veriblock.core.Context
import org.veriblock.core.SharedConstants
import org.veriblock.core.params.defaultAlphaNetParameters
import org.veriblock.core.params.defaultMainNetParameters
import org.veriblock.core.params.defaultTestNetParameters
import org.veriblock.core.utilities.createLogger
import org.veriblock.shell.Command
import org.veriblock.shell.CommandContext
import org.veriblock.shell.CommandFactory
import org.veriblock.shell.Shell
import org.veriblock.shell.core.Result
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.SSLException

private val logger = createLogger {}

class CliShell(
    commandFactory: CommandFactory,
    private val configuration: Configuration
) : Shell(
    commandFactory, null
) {
    private var protocolEndpoint: ProtocolEndpoint? = null
    private val connected = AtomicBoolean(false)
    private var adminServiceClient: AdminServiceClient? = null

    public override fun onStop() {
        disconnect()
    }

    @Throws(SSLException::class, ConnectionFailedException::class)
    fun connect(endpoint: ProtocolEndpoint, save: Boolean): Boolean {
        if (save) {
            disconnect()
        }

        when {
            endpoint.port % 100 == 0 -> { Context.set(defaultMainNetParameters) }
            endpoint.port % 100 == 1 -> { Context.set(defaultTestNetParameters) }
            else -> { Context.set(defaultAlphaNetParameters) }
        }

        when (endpoint.type) {
            ProtocolEndpointType.NONE -> return false
            ProtocolEndpointType.RPC -> return if (save) {
                adminServiceClient = AdminServiceClient(endpoint.address, endpoint.port.toInt(), endpoint.transportType, configuration, endpoint.password)
                try {
                    adminServiceClient!!.connect()
                    protocolEndpoint = endpoint
                    refreshCompleter()
                    connected.set(true)
                    true
                } catch (exception: Exception) {
                    disconnect()
                    throw ConnectionFailedException("Unable to connect to the endpoint $endpoint", exception)
                }
            } else {
                val temp = AdminServiceClient(endpoint.address, endpoint.port.toInt(), endpoint.transportType, configuration, endpoint.password)
                var result = false
                try {
                    temp.connect()
                    result = true
                } catch (e: Exception) {
                    // do nothing
                } finally {
                    try {
                        temp.shutdown()
                    } catch (e: InterruptedException) {
                        // do nothing
                    }
                }
                result
            }
            ProtocolEndpointType.PEER -> return false
        }
    }

    private fun disconnect() {
        try {
            if (adminServiceClient != null) {
                adminServiceClient!!.shutdown()
                adminServiceClient = null
                refreshCompleter()
            }
            protocolEndpoint = null
        } catch (e: Exception) {
            logger.error("Unhandled exception", e)
        }
    }

    override fun getPrompt(): String {
        return if (protocolEndpoint == null) {
            AttributedStringBuilder()
                .style(AttributedStyle.BOLD)
                .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW + 8))
                .append("(VBK_CLI ")
                .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.RED + 8))
                .append("[NOT CONNECTED]")
                .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW + 8))
                .append(") > ")
                .toAnsi()
        } else {
            AttributedStringBuilder()
                .style(AttributedStyle.BOLD)
                .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN + 8))
                .append("(VBK_CLI ")
                .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN + 8))
                .append(protocolEndpoint.toString())
                .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN + 8))
                .append(") > ")
                .toAnsi()
        }
    }

    override fun handleResult(context: CommandContext, result: Result): Result {
        var failed = result.isFailed
        try {
            val disconnect = context.getExtraData<Boolean>("disconnect")
            if (disconnect != null && disconnect) {
                disconnect()
            }
            val endpoint = context.getExtraData<ProtocolEndpoint>("connect")
            if (endpoint != null) {
                if (endpoint.port.toInt() == 8500 || endpoint.port.toInt() == 8501) {
                    result.addMessage("V023", "Connect Failed", "NodeCore reserves ports 8500 and 8501 for mining pool operations", true)
                    failed = true
                } else {
                    try {
                        // TODO: Refactor all this. Shell shouldn't be responsible for establishing connection
                        connect(endpoint, true)
                        printStyled("Successfully connected to NodeCore!", AttributedStyle.BOLD.foreground(AttributedStyle.GREEN))
                    } catch (connEx: ConnectionFailedException) {
                        val errorMessage = """
                            ${connEx.message}
                            
                            Note: NodeCore does not begin listening for RPC connections until after loading the blockchain, which may take several minutes. By default, NodeCore MainNet listens on port 10500 and NodeCore TestNet listens on port 10501. 
                            """.trimIndent()
                        result.addMessage("V023",
                                "Connect Failed",
                                errorMessage + "Please ensure that you have also started up NodeCore!",
                                true)
                        failed = true
                    }
                }
            }
        } catch (argEx: IllegalArgumentException) {
            result.addMessage(
                    "V024",
                    "TLS configuration failed",
                    argEx.message!!,
                    true)
            failed = true
            logger.error("V024: TLS configuration failed", argEx)
            disconnect()
        } catch (argEx: SSLException) {
            result.addMessage(
                    "V024",
                    "TLS configuration failed",
                    argEx.message!!,
                    true)
            failed = true
            logger.error("V024: TLS configuration failed", argEx)
            disconnect()
        } catch (e: Exception) {
            //check if connected
            if (adminServiceClient == null) {
                //Error from not connected yet
                result.addMessage(
                        "V999",
                        "Not connected to NodeCore yet, please run the connect command!",
                        "Example: connect 127.0.0.1:10501",
                        true)
                failed = true
            } else {
                //Some other unhandled exception
                result.addMessage(
                        "V999",
                        "Unhandled exception",
                        e.toString(),
                        true)
                failed = true
                logger.error("V999: Unhandled Exception", e)
            }
        }
        if (failed) {
            val failure = Result(true)
            for (rm in result.getMessages()) {
                failure.addMessage(rm)
            }
            return failure
        }
        return result
    }

    override fun handleException(exception: Exception): Result {
        val result = super.handleException(exception)
        if (adminServiceClient == null) {
            //Error from not connected yet
            result.addMessage(
                    "V999",
                    "Not connected to NodeCore yet, please run the connect command!",
                    "Example: connect 127.0.0.1:10501",
                    true
            )
        }
        return result
    }

    override fun Command.shouldAutoComplete(): Boolean {
        val extraData = this.extraData
        return  protocolEndpoint != null || extraData != null && extraData == CommandServiceType.SHELL.name
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun initialize(programOptions: ProgramOptions) {
        var endpoint: ProtocolEndpoint? = null
        var host: String? = null
        printIntroStandard()

        if (programOptions.connect != null) {
            host = programOptions.connect
            val parsedAddress = host!!.replace("https://", "").replace("http://", "")
            val parts = parsedAddress.split(":").toTypedArray()
            endpoint = ProtocolEndpoint(
                address = parts[0],
                port = parts[1].toShort()
            )
        }

        if (endpoint != null && host != null) {
            try {
                connect(endpoint, true)
                if (connected.get()) {
                    val msg = AttributedStringBuilder()
                        .style(AttributedStyle.BOLD)
                        .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                        .append("\n\nConnected to node on ")
                        .append(host)
                        .append("\n\n")
                        .toAnsi()
                    printInfo(msg)
                }
            } catch (e: Exception) { }
        }

        GlobalScope.launch {
            while (!connected.get()) {
                for (port in 10500..10502) {
                    val network = when (port) {
                        10500 -> "MainNet"
                        10501 -> "TestNet"
                        else -> "AlphaNet"
                    }
                    if (bound(port)) {
                        val msg = AttributedStringBuilder()
                            .style(AttributedStyle.BOLD.foreground(AttributedStyle.GREEN))
                            .append("\n\nA local NodeCore $network Instance is present on 127.0.0.1:$port!\n ")
                            .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN))
                            .append("To connect to NodeCore $network, type: ")
                            .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.WHITE))
                            .append("connect 127.0.0.1:$port\n\n")
                            .toAnsi()
                        printInfo(msg)
                        reader.callWidget(LineReader.REDRAW_LINE)
                        reader.callWidget(LineReader.REDISPLAY)
                        return@launch
                    }
                }
                delay(1000)
            }
        }
    }

    private fun bound(port: Int): Boolean {
        return try {
            connect(
                ProtocolEndpoint(
                    address = "127.0.0.1",
                    port = port.toShort()
                ),
                false
            )
        } catch (ignored: Exception) {
            false
        }
    }

    private fun printIntroStandard() {
        printStyled(
            SharedConstants.VERIBLOCK_APPLICATION_NAME.replace("$1", Constants.FULL_APPLICATION_NAME_VERSION),
            AttributedStyle.BOLD.foreground(AttributedStyle.GREEN)
        )
        printStyled(
            """		${SharedConstants.VERIBLOCK_WEBSITE}""",
            AttributedStyle.BOLD.foreground(AttributedStyle.GREEN)
        )
        printStyled(
            """
                ${SharedConstants.VERIBLOCK_PRODUCT_WIKI_URL.replace("$1", "https://wiki.veriblock.org/index.php/NodeCore_CommandLine")}
            """.trimIndent(),
            AttributedStyle.BOLD.foreground(AttributedStyle.GREEN)
        )
        printStyled(
            """
                To begin, you must connect this CLI to a running instance of NodeCore.
                By default, NodeCore will be available on 127.0.0.1:10500.
            """.trimIndent(),
            AttributedStyle.BOLD.foreground(AttributedStyle.MAGENTA)
        )
        printStyled(
            "To connect to NodeCore type: ",
            AttributedStyle.BOLD.foreground(AttributedStyle.CYAN),
            false
        )
        printStyled(
            "connect 127.0.0.1:10500",
            AttributedStyle.BOLD.foreground(AttributedStyle.WHITE)
        )
        printStyled(
            "To see available commands, type: ",
            AttributedStyle.BOLD.foreground(AttributedStyle.CYAN),
            false
        )
        printStyled(
            "help\n",
            AttributedStyle.BOLD.foreground(AttributedStyle.WHITE)
        )
        printStyled(
            "Note: If you are using the regular \"all\" package, then the CLI can \nautomatically start many of the other VeriBlock-related services for you!",
            AttributedStyle.BOLD.foreground(AttributedStyle.MAGENTA)
        )
        printStyled(
            "  To start NodeCore, type: ",
            AttributedStyle.BOLD.foreground(AttributedStyle.CYAN),
            false
        )
        printStyled(
            "startnodecore",
            AttributedStyle.BOLD.foreground(AttributedStyle.WHITE)
        )
        printStyled(
            "  To start the Proof-of-Proof (PoP) miner, type: ",
            AttributedStyle.BOLD.foreground(AttributedStyle.CYAN),
            false
        )
        printStyled(
            "startpopminer",
            AttributedStyle.BOLD.foreground(AttributedStyle.WHITE)
        )
        printStyled(
            """    Note: for the PoP miner to work, you must have already started 
                |    NodeCore,and connected to it in this CLI (""".trimMargin(),
            AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW),
            false
        )
        printStyled(
            "connect 127.0.0.1:10500",
            AttributedStyle.BOLD.foreground(AttributedStyle.WHITE),
            false
        )
        printStyled(
            ")!",
            AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW)
        )
        introFooter()
    }

    private fun introFooter() {
        printStyled(
            """
                To stay up to date on the status of the VeriBlock Network and 
                ensure you are running the latest software, frequently check:
            """.trimIndent(),
            AttributedStyle.BOLD.foreground(AttributedStyle.CYAN)
        )
        printStyled(
            "\tDiscord: ",
            AttributedStyle.BOLD.foreground(AttributedStyle.GREEN),
            false
        )
        printStyled(
            "https://discord.gg/wJZEjry",
            AttributedStyle.BOLD.foreground(AttributedStyle.WHITE),
            true
        )
        printStyled(
            "\tVeriBlock Explorer: ",
            AttributedStyle.BOLD.foreground(AttributedStyle.GREEN),
            false
        )
        printStyled(
            "https://explore.veriblock.org\n\n",
            AttributedStyle.BOLD.foreground(AttributedStyle.WHITE)
        )
    }

    val protocolType: ProtocolEndpointType
        get() = protocolEndpoint?.type ?: ProtocolEndpointType.NONE

    val adminService: AdminService
        get() = adminServiceClient!!

    fun isConnected(): Boolean = adminServiceClient != null
}
