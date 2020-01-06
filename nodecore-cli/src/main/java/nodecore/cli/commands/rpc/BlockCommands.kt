package nodecore.cli.commands.rpc

import nodecore.api.grpc.VeriBlockMessages
import nodecore.api.grpc.utilities.ByteStringUtility
import nodecore.cli.CliShell
import nodecore.cli.prepareResult
import nodecore.cli.rpcCommand
import nodecore.cli.serialization.BitcoinBlockPayload
import nodecore.cli.serialization.BlockchainPayload
import nodecore.cli.serialization.BlocksPayload
import nodecore.cli.serialization.GetBlockTemplatePayload
import org.veriblock.core.utilities.Utility
import org.veriblock.shell.CommandParameter
import org.veriblock.shell.CommandParameterType

fun CliShell.blockCommands() {
    rpcCommand(
        name = "Get Blockchains",
        form = "getblockchains",
        description = "Returns blockchain information",
        suggestedCommands = { listOf("getinfo") }
    ) {
        val request = VeriBlockMessages.GetBlockchainsRequest.newBuilder().build()
        val result = adminService.getBlockchains(request)

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
            CommandParameter(name = "blockHash", type = CommandParameterType.HASH, required = true)
        ),
        suggestedCommands = { listOf("getblockfromindex", "gettransaction") }
    ) {
        val hash: String = getParameter("blockHash")
        val request = VeriBlockMessages.GetBlocksRequest
            .newBuilder().addFilters(
                VeriBlockMessages.BlockFilter.newBuilder().setHash(ByteStringUtility.hexToByteString(hash))
            ).build()
        val result = adminService.getBlocks(request)

        prepareResult(result.success, result.resultsList) {
            BlocksPayload(result.blocksList)
        }
    }

    rpcCommand(
        name = "Get Raw Block from Index",
        form = "getblockfromindex",
        description = "Returns the block for the given block index",
        parameters = listOf(
            CommandParameter(name = "blockIndex", type = CommandParameterType.INTEGER, required = true)
        ),
        suggestedCommands = { listOf("getblockfromhash", "gettransaction") }
    ) {
        val index: Int = getParameter("blockIndex")
        val request = VeriBlockMessages.GetBlocksRequest.newBuilder()
            .addFilters(VeriBlockMessages.BlockFilter.newBuilder().setIndex(index))
            .build()
        val result = adminService.getBlocks(request)

        prepareResult(result.success, result.resultsList) {
            BlocksPayload(result.blocksList)
        }
    }

    rpcCommand(
        name = "Get Block Template",
        form = "getblocktemplate",
        description = "Returns a block template along with candidate transactions",
        parameters = listOf(
            CommandParameter(name = "mode", type = CommandParameterType.STRING, required = false),
            CommandParameter(name = "capabilities", type = CommandParameterType.STRING, required = false)
        )
    ) {
        val mode = if (getOptionalParameter<String>("mode").isNullOrEmpty()) {
            "template"
        } else {
            getParameter("mode")
        }
        val request = VeriBlockMessages.GetBlockTemplateRequest.newBuilder().apply {
            this.mode = mode
        }

        val capabilities: List<String> = getOptionalParameter<String>("capabilities")?.split(",") ?: emptyList()
        for (flag in capabilities) {
            request.addCapabilities(flag)
        }

        val result = adminService.getBlockTemplate(request.build())

        prepareResult(result.success, result.resultsList) {
            GetBlockTemplatePayload(result)
        }
    }

    rpcCommand(
        name = "Get Last Bitcoin Block",
        form = "getlastbitcoinblock",
        description = "Returns the last Bitcoin block known by NodeCore"
    ) {
        val request = VeriBlockMessages.GetLastBitcoinBlockRequest.newBuilder().build()
        val result = adminService.getLastBitcoinBlock(request)

        prepareResult(result.success, result.resultsList) {
            BitcoinBlockPayload(result)
        }
    }

    rpcCommand(
        name = "Submit Block",
        form = "submitblock",
        description = "Attempts to add the specified raw block",
        parameters = listOf(
            CommandParameter(name = "rawBlock", type = CommandParameterType.STRING, required = true)
        )
    ) {
        val rawBlock: String = getParameter("rawBlock")
        val request = VeriBlockMessages.SubmitBlocksRequest.parseFrom(Utility.base64ToBytes(rawBlock))
        val result = adminService.submitBlocks(request)

        prepareResult(result.success, result.resultsList)
    }
}
