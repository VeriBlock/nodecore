package nodecore.cli.shell.commands

import io.grpc.StatusRuntimeException
import nodecore.api.grpc.VeriBlockMessages
import nodecore.api.grpc.utilities.ByteStringUtility
import nodecore.cli.commands.DefaultCommandContext
import nodecore.cli.commands.serialization.BitcoinBlockPayload
import nodecore.cli.commands.serialization.BlockchainPayload
import nodecore.cli.commands.serialization.BlocksPayload
import nodecore.cli.commands.serialization.FormattableObject
import nodecore.cli.commands.serialization.GetBlockTemplatePayload
import org.veriblock.core.utilities.Utility
import org.veriblock.shell.Command
import org.veriblock.shell.CommandParameter
import org.veriblock.shell.CommandParameterType
import org.veriblock.shell.Shell
import org.veriblock.shell.command
import org.veriblock.shell.core.Result
import org.veriblock.shell.core.failure
import org.veriblock.shell.core.success

fun Shell.blockCommands(
    context: DefaultCommandContext
) {
    command(
        name = "Get Blockchains",
        form = "getblockchains",
        description = "Returns blockchain information"
    ) {
        try {
            val suggestedCommands = listOf(getCommand("getinfo"))
            context.adminService().getBlockchains(VeriBlockMessages.GetBlockchainsRequest.newBuilder().build())
                .toShellResult(context, suggestedCommands)
        } catch (e: StatusRuntimeException) {
            failure("V800", "Command failure", "Failed to run the command: ${e.message}")
        }
    }

    command(
        name = "Get Block from Hash",
        form = "getblockfromhash",
        description = "Returns the block for the specified hash",
        parameters = listOf(
            CommandParameter(name = "blockHash", type = CommandParameterType.HASH, required = true)
        )
    ) {
        val hash: String = getParameter("blockHash")
        try {
            val request = VeriBlockMessages.GetBlocksRequest
                .newBuilder()
                .addFilters(VeriBlockMessages.BlockFilter.newBuilder()
                    .setHash(ByteStringUtility.hexToByteString(hash)))
                .build()
            val suggestedCommands = listOf(
                getCommand("getblockfromindex"),
                getCommand("gettransaction")
            )

            context.adminService().getBlocks(request).toShellResult(context, suggestedCommands)
        } catch (e: StatusRuntimeException) {
            failure("V800", "Command failure", "Failed to run the command: ${e.message}")
        }
    }

    command(
        name = "Get Raw Block from Index",
        form = "getblockfromindex",
        description = "Returns the block for the given block index",
        parameters = listOf(
            CommandParameter(name = "blockIndex", type = CommandParameterType.INTEGER, required = true)
        )
    ) {
        val index: Int = getParameter("blockIndex")
        try {
            val requestBuilder = VeriBlockMessages.GetBlocksRequest.newBuilder()
            requestBuilder.addFilters(VeriBlockMessages.BlockFilter.newBuilder().setIndex(index))
            val suggestedCommands = listOf(
                getCommand("getblockfromhash"),
                getCommand("gettransaction")
            )

            context.adminService().getBlocks(requestBuilder.build()).toShellResult(context, suggestedCommands)
        } catch (e: StatusRuntimeException) {
            failure("V800", "Command failure", "Failed to run the command: ${e.message}")
        }
    }

    command(
        name = "Get Block Template",
        form = "getblocktemplate",
        description = "Returns a block template along with candidate transactions",
        parameters = listOf(
            CommandParameter(name = "mode", type = CommandParameterType.STRING, required = false),
            CommandParameter(name = "capabilities", type = CommandParameterType.STRING, required = false)
        )
    ) {
        try {
            val mode: String = if (getParameter<String>("mode").isNullOrEmpty()) { "template" } else { getParameter("mode") }
            val requestBuilder = VeriBlockMessages.GetBlockTemplateRequest.newBuilder()
            requestBuilder.mode = mode

            val capabilities: String? = getParameter("capabilities")
            if (capabilities != null) {
                val flags = capabilities.split(",".toRegex())
                    .dropLastWhile {
                        it.isEmpty()
                    }.toTypedArray()

                for (flag in flags) {
                    requestBuilder.addCapabilities(flag)
                }
            }

            context.adminService().getBlockTemplate(requestBuilder.build()).toShellResult(context)
        } catch (e: StatusRuntimeException) {
            failure("V800", "Command failure", "Failed to run the command: ${e.message}")
        }
    }

    command(
        name = "Get Last Bitcoin Block",
        form = "getlastbitcoinblock",
        description = "Returns the last Bitcoin block known by NodeCore"
    ) {
        try {
            context.adminService()
                .getLastBitcoinBlock(VeriBlockMessages.GetLastBitcoinBlockRequest.newBuilder().build())
                .toShellResult(context)
        } catch (e: StatusRuntimeException) {
            failure("V800", "Command failure", "Failed to run the command: ${e.message}")
        }
    }

    command(
        name = "Submit Block",
        form = "submitblock",
        description = "Attempts to add the specified raw block",
        parameters = listOf(
            CommandParameter(name = "rawBlock", type = CommandParameterType.STRING, required = true)
        )
    ) {
        try {
            val rawBlock: String = getParameter("rawBlock")
            context.adminService()
                .submitBlocks(VeriBlockMessages.SubmitBlocksRequest.parseFrom(Utility.base64ToBytes(rawBlock)))
                .toShellResult(context)
        } catch (e: StatusRuntimeException) {
            failure("V800", "Command failure", "Failed to run the command: ${e.message}")
        }
    }
}

fun VeriBlockMessages.GetBlockchainsReply.toShellResult(
    context: DefaultCommandContext,
    suggestedCommands: List<Class<out Command>>? = emptyList()
): Result {
    return if (!success) {
        failure {
            for (r in resultsList) {
                addMessage(r.code, r.message, r.details, r.error)
            }
        }
    } else {
        val temp = FormattableObject<BlockchainPayload>(resultsList)
        temp.success = true
        temp.payload = BlockchainPayload()
        temp.payload.bestLength = bestBlockchainLength.toLong()
        temp.payload.longestLength = longestBlockchainLength.toLong()
        context.outputObject(temp)
        context.suggestCommands(suggestedCommands)
        success{
            for (r in resultsList) {
                addMessage(r.code, r.message, r.details, r.error)
            }
        }
    }
}

fun VeriBlockMessages.GetBlocksReply.toShellResult(
    context: DefaultCommandContext,
    suggestedCommands: List<Class<out Command>>? = emptyList()
): Result {
    return if (!success) {
        failure {
            for (r in resultsList) {
                addMessage(r.code, r.message, r.details, r.error)
            }
        }
    } else {
        val temp = FormattableObject<BlocksPayload>(resultsList)
        temp.success = true
        temp.payload = BlocksPayload(blocksList)
        context.outputObject(temp)
        context.suggestCommands(suggestedCommands)
        success{
            for (r in resultsList) {
                addMessage(r.code, r.message, r.details, r.error)
            }
        }
    }
}

fun VeriBlockMessages.GetBlockTemplateReply.toShellResult(
    context: DefaultCommandContext
): Result {
    return if (!success) {
        failure {
            for (r in resultsList) {
                addMessage(r.code, r.message, r.details, r.error)
            }
        }
    } else {
        val temp = FormattableObject<GetBlockTemplatePayload>(resultsList)
        temp.success = true
        temp.payload = GetBlockTemplatePayload(this)
        context.outputObject(temp)
        success{
            for (r in resultsList) {
                addMessage(r.code, r.message, r.details, r.error)
            }
        }
    }
}

fun VeriBlockMessages.GetLastBitcoinBlockReply.toShellResult(
    context: DefaultCommandContext
): Result {
    return if (!success) {
        failure {
            for (r in resultsList) {
                addMessage(r.code, r.message, r.details, r.error)
            }
        }
    } else {
        val temp = FormattableObject<BitcoinBlockPayload>(resultsList)
        temp.success = true
        temp.payload = BitcoinBlockPayload(this)
        context.outputObject(temp)
        success{
            for (r in resultsList) {
                addMessage(r.code, r.message, r.details, r.error)
            }
        }
    }
}
