package nodecore.cli.commands.rpc

import nodecore.api.grpc.RpcBlockFilter
import nodecore.api.grpc.RpcGetBlockTemplateRequest
import nodecore.api.grpc.RpcGetBlockchainsRequest
import nodecore.api.grpc.RpcGetBlocksRequest
import nodecore.api.grpc.RpcGetLastBitcoinBlockRequest
import nodecore.api.grpc.RpcSubmitBlocksRequest
import org.veriblock.sdk.extensions.asHexByteString
import nodecore.cli.cliShell
import nodecore.cli.commands.ShellCommandParameterMappers
import nodecore.cli.prepareResult
import nodecore.cli.rpcCommand
import nodecore.cli.serialization.BitcoinBlockPayload
import nodecore.cli.serialization.BlockchainPayload
import nodecore.cli.serialization.BlocksPayload
import nodecore.cli.serialization.GetBlockTemplatePayload
import org.veriblock.core.utilities.Utility
import org.veriblock.shell.CommandFactory
import org.veriblock.shell.CommandParameter
import org.veriblock.shell.CommandParameterMappers

fun CommandFactory.blockCommands() {
    rpcCommand(
        name = "Get Blockchains",
        form = "getblockchains",
        description = "Returns blockchain information",
        suggestedCommands = { listOf("getinfo") }
    ) {
        val request = RpcGetBlockchainsRequest.newBuilder().build()
        val result = cliShell.adminService.getBlockchains(request)

        prepareResult(result.success, result.resultsList) {
            BlockchainPayload().apply {
                bestLength = result.bestBlockchainLength.toLong()
                longestLength = result.longestBlockchainLength.toLong()
            }
        }
    }

    rpcCommand(
        name = "Get Block from Hash",
        form = "getblockfromhash",
        description = "Returns the block for the specified hash",
        parameters = listOf(
            CommandParameter(name = "blockHash", mapper = ShellCommandParameterMappers.HASH, required = true)
        ),
        suggestedCommands = { listOf("getblockfromindex", "gettransaction") }
    ) {
        val hash: String = getParameter("blockHash")
        val request = RpcGetBlocksRequest
            .newBuilder().addFilters(
                RpcBlockFilter.newBuilder().setHash(hash.asHexByteString())
            ).build()
        val result = cliShell.adminService.getBlocks(request)

        prepareResult(result.success, result.resultsList) {
            BlocksPayload(result.blocksList)
        }
    }

    rpcCommand(
        name = "Get Raw Block from Index",
        form = "getblockfromindex",
        description = "Returns the block for the given block index",
        parameters = listOf(
            CommandParameter(name = "blockIndex", mapper = CommandParameterMappers.INTEGER, required = true)
        ),
        suggestedCommands = { listOf("getblockfromhash", "gettransaction") }
    ) {
        val index: Int = getParameter("blockIndex")
        val request = RpcGetBlocksRequest.newBuilder()
            .addFilters(RpcBlockFilter.newBuilder().setIndex(index))
            .build()
        val result = cliShell.adminService.getBlocks(request)

        prepareResult(result.success, result.resultsList) {
            BlocksPayload(result.blocksList)
        }
    }

    rpcCommand(
        name = "Get Block Template",
        form = "getblocktemplate",
        description = "Returns a block template along with candidate transactions",
        parameters = listOf(
            CommandParameter(name = "mode", mapper = CommandParameterMappers.STRING, required = false),
            CommandParameter(name = "capabilities", mapper = CommandParameterMappers.STRING, required = false)
        )
    ) {
        val mode = if (getOptionalParameter<String>("mode").isNullOrEmpty()) {
            "template"
        } else {
            getParameter("mode")
        }
        val request = RpcGetBlockTemplateRequest.newBuilder().apply {
            this.mode = mode
        }

        val capabilities: List<String> = getOptionalParameter<String>("capabilities")?.split(",") ?: emptyList()
        for (flag in capabilities) {
            request.addCapabilities(flag)
        }

        val result = cliShell.adminService.getBlockTemplate(request.build())

        prepareResult(result.success, result.resultsList) {
            GetBlockTemplatePayload(result)
        }
    }

    rpcCommand(
        name = "Get Last Bitcoin Block",
        form = "getlastbitcoinblock",
        description = "Returns the last Bitcoin block known by NodeCore"
    ) {
        val request = RpcGetLastBitcoinBlockRequest.newBuilder().build()
        val result = cliShell.adminService.getLastBitcoinBlock(request)

        prepareResult(result.success, result.resultsList) {
            BitcoinBlockPayload(result)
        }
    }

    rpcCommand(
        name = "Submit Block",
        form = "submitblock",
        description = "Attempts to add the specified raw block",
        parameters = listOf(
            CommandParameter(name = "rawBlock", mapper = CommandParameterMappers.STRING, required = true)
        )
    ) {
        val rawBlock: String = getParameter("rawBlock")
        val request = RpcSubmitBlocksRequest.parseFrom(Utility.base64ToBytes(rawBlock))
        val result = cliShell.adminService.submitBlocks(request)

        prepareResult(result.success, result.resultsList)
    }
}
