package nodecore.cli.commands.rpc

import nodecore.api.grpc.RpcAddressSet
import nodecore.api.grpc.RpcBitcoinBlockHeader
import nodecore.api.grpc.RpcGetPopEndorsementsInfoRequest
import nodecore.api.grpc.RpcGetPopRequest
import nodecore.api.grpc.RpcGetProtectedChildrenRequest
import nodecore.api.grpc.RpcGetProtectingParentsRequest
import nodecore.api.grpc.RpcStandardAddress
import nodecore.api.grpc.RpcSubmitPopRequest
import nodecore.api.grpc.RpcTroubleshootPopTransactionsRequest
import org.veriblock.sdk.extensions.ByteStringAddressUtility
import org.veriblock.sdk.extensions.asHexByteString
import org.veriblock.sdk.extensions.toByteString
import nodecore.cli.cliShell
import nodecore.cli.commands.ShellCommandParameterMappers
import nodecore.cli.prepareResult
import nodecore.cli.rpcCommand
import nodecore.cli.serialization.PopEndorsementsInfo
import nodecore.cli.serialization.PopPayload
import nodecore.cli.serialization.TroubleshootPopTransactionsPayload
import org.veriblock.core.utilities.createLogger
import org.veriblock.shell.CommandFactory
import org.veriblock.shell.CommandParameter
import org.veriblock.shell.CommandParameterMappers
import java.awt.GraphicsEnvironment

private val logger = createLogger {}

fun CommandFactory.popCommands() {
    rpcCommand(
        name = "Troubleshoot PoP By Address",
        form = "troubleshootpopbyaddress",
        description = "(Returns a troubleshooting report of the PoP transaction(s) matching the provided address in the specified history)",
        parameters = listOf(
            CommandParameter(name = "onlyFailures", mapper = CommandParameterMappers.BOOLEAN, required = true),
            CommandParameter(name = "searchLength", mapper = CommandParameterMappers.INTEGER, required = false),
            CommandParameter(name = "address", mapper = ShellCommandParameterMappers.STANDARD_ADDRESS, required = false)
        ),
        suggestedCommands = { listOf("getbalance", "gethistory") }
    ) {
        val address: String? = getOptionalParameter("address")
        val request = RpcTroubleshootPopTransactionsRequest.newBuilder()
        if (address != null) {
            request.setAddresses(
                RpcAddressSet.newBuilder().addAddresses(
                    ByteStringAddressUtility.createProperByteStringAutomatically(address)
                )
            )
        }
        request.onlyFailures = getParameter("onlyFailures")
        request.searchLength = getOptionalParameter("searchLength") ?: 2000

        val result = cliShell.adminService.troubleshootPopTransactions(request.build())

        prepareResult(result.success, result.resultsList) {
            TroubleshootPopTransactionsPayload(result)
        }
    }

    rpcCommand(
        name = "Get Pop",
        form = "getpop",
        description = "Gets the data VeriBlock wants Proof-of-Proof published to Bitcoin",
        parameters = listOf(
            CommandParameter(name = "block", mapper = CommandParameterMappers.INTEGER, required = false)
        ),
        suggestedCommands = {
            if (!GraphicsEnvironment.isHeadless()) {
                listOf("startpopminer")
            } else {
                emptyList()
            }
        }
    ) {
        val request = RpcGetPopRequest.newBuilder()
            .setBlockNum(getOptionalParameter("block") ?: 0)
            .build()

        val result = cliShell.adminService.getPop(request)

        prepareResult(result.success, result.resultsList) {
            PopPayload(result)
        }
    }

    rpcCommand(
        name = "Get PoP Endorsements Info",
        form = "getpopendorsementsinfo",
        description = "Returns the PoP endorsements related to a particular address given the particular search length",
        parameters = listOf(
            CommandParameter(name = "address", mapper = ShellCommandParameterMappers.STANDARD_ADDRESS, required = true),
            CommandParameter(name = "searchLength", mapper = CommandParameterMappers.INTEGER, required = false)
        ),
        suggestedCommands = { listOf("getprotectedchildren", "getprotectingparents", "startpopminer") }
    ) {
        val address: String = getParameter("address")
        val searchLength: Int? = getOptionalParameter("searchLength")
        val request = RpcGetPopEndorsementsInfoRequest.newBuilder()
            .addAddresses(
                RpcStandardAddress.newBuilder()
                 .setStandardAddress(ByteStringAddressUtility.createProperByteStringAutomatically(address))
            )
        if (searchLength != null) {
            request.searchLength = searchLength
        }

        val result = cliShell.adminService.getPopEndorsementsInfo(request.build())

        prepareResult(result.success, result.resultsList) {
            result.popEndorsementsList.map { PopEndorsementsInfo(it) }
        }
    }

    rpcCommand(
        name = "Submit Proof-of-Proof",
        form = "submitpop",
        description = "Submit a Proof-of-Proof transaction",
        parameters = listOf(
            CommandParameter(name = "endorsedBlockHeader", mapper = CommandParameterMappers.STRING, required = true),
            CommandParameter(name = "bitcoinTransaction", mapper = CommandParameterMappers.STRING, required = true),
            CommandParameter(name = "bitcoinMerklePathToRoot", mapper = CommandParameterMappers.STRING, required = true),
            CommandParameter(name = "bitcoinBlockHeader", mapper = CommandParameterMappers.STRING, required = true),
            CommandParameter(name = "address", mapper = ShellCommandParameterMappers.STANDARD_ADDRESS, required = false)
        )
    ) {
        val endorsedBlockHeader: String = getParameter("endorsedBlockHeader")
        val bitcoinTransaction: String = getParameter("bitcoinTransaction")
        val bitcoinMerklePathToRoot: String = getParameter("bitcoinMerklePathToRoot")
        val bitcoinBlockHeader: String = getParameter("bitcoinBlockHeader")
        val address: String? = getOptionalParameter("address")

        //TODO: Add context Bitcoin block header parameters

        val request = RpcSubmitPopRequest.newBuilder().apply {
            this.endorsedBlockHeader = endorsedBlockHeader.asHexByteString()
            this.bitcoinTransaction = bitcoinTransaction.asHexByteString()
            this.bitcoinMerklePathToRoot = bitcoinMerklePathToRoot.toByteArray().toByteString()
            this.bitcoinBlockHeaderOfProof = RpcBitcoinBlockHeader.newBuilder().setHeader(bitcoinBlockHeader.asHexByteString())
                .build()
        }
        if (address != null) {
            request.address = ByteStringAddressUtility.createProperByteStringAutomatically(address)
        }
        val result = cliShell.adminService.submitPop(request.build())

        prepareResult(result.success, result.resultsList)
    }

    rpcCommand(
        name = "Get Protecting Parents",
        form = "getprotectingparents",
        description = "Returns the pop endorsement information of parents protecting the provided block hash",
        parameters = listOf(
            CommandParameter(name = "blockhash", mapper = ShellCommandParameterMappers.HASH, required = true),
            CommandParameter(name = "searchLength", mapper = CommandParameterMappers.INTEGER, required = false)
        ),
        suggestedCommands = { listOf("getprotectedchildren", "getpopendorsementsinfo", "startpopminer") }
    ) {
        val hash: String = getParameter("blockhash")
        val searchLength: Int? = getOptionalParameter("searchLength")
        val request = RpcGetProtectingParentsRequest.newBuilder()
            .setVeriblockBlockHash(hash.asHexByteString())

        if (searchLength != null) {
            request.searchLength = searchLength
        }

        val result = cliShell.adminService.getProtectingParents(request.build())

        logger.info("We received a total of ${result.popEndorsementsCount} pop endorsements!")

        prepareResult(result.success, result.resultsList) {
            result.popEndorsementsList.map { PopEndorsementsInfo(it)  }
        }
    }

    rpcCommand(
        name = "Get Protected Children",
        form = "getprotectedchildren",
        description = "Returns the children protected by PoP transactions in a particular VeriBlock block identified by the provided block hash",
        parameters = listOf(
            CommandParameter(name = "blockhash", mapper = ShellCommandParameterMappers.HASH, required = true),
            CommandParameter(name = "searchLength", mapper = CommandParameterMappers.INTEGER, required = false)
        ),
        suggestedCommands = { listOf("getprotectingparents", "getpopendorsementsinfo", "startpopminer") }
    ) {
        val hash: String = getParameter("blockhash")
        val searchLength: Int? = getOptionalParameter("searchLength")
        val request = RpcGetProtectedChildrenRequest.newBuilder()
            .setVeriblockBlockHash(hash.asHexByteString())

        if (searchLength != null) {
            request.searchLength = searchLength
        }

        val result = cliShell.adminService.getProtectedChildren(request.build())

        logger.info("We received a total of ${result.popEndorsementsCount} pop endorsements!")

        prepareResult(result.success, result.resultsList) {
            result.popEndorsementsList.map { PopEndorsementsInfo(it) }
        }
    }
}
