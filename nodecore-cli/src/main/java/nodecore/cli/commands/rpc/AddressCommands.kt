package nodecore.cli.commands.rpc

import nodecore.api.grpc.VeriBlockMessages
import nodecore.api.grpc.utilities.ByteStringAddressUtility
import nodecore.cli.cliShell
import nodecore.cli.commands.ShellCommandParameterMappers
import nodecore.cli.prepareResult
import nodecore.cli.rpcCommand
import nodecore.cli.serialization.GenerateMultisigAddressPayload
import nodecore.cli.serialization.NewAddressPayload
import nodecore.cli.serialization.TransactionInfo
import nodecore.cli.serialization.ValidateAddressPayload
import org.veriblock.shell.CommandFactory
import org.veriblock.shell.CommandParameter
import org.veriblock.shell.CommandParameterMappers

fun CommandFactory.addressCommands() {
    rpcCommand(
        name = "Validate Address",
        form = "validateaddress|validateaddr",
        description = "Returns details about an address if it is valid",
        parameters = listOf(
            CommandParameter(name = "address", mapper = ShellCommandParameterMappers.STANDARD_ADDRESS, required = true)
        ),
        suggestedCommands = { listOf("getbalance", "gethistory") }
    ) {
        val request = VeriBlockMessages.ValidateAddressRequest.newBuilder().apply {
            address = ByteStringAddressUtility.createProperByteStringAutomatically(getParameter("address"))
        }.build()

        val result = cliShell.adminService.validateAddress(request)

        prepareResult(result.success, result.resultsList) {
            ValidateAddressPayload(result)
        }
    }

    rpcCommand(
        name = "Set Default Address",
        form = "setdefaultaddress",
        description = "Sets the default address of a NodeCore instance",
        parameters = listOf(
            CommandParameter(name = "address", mapper = ShellCommandParameterMappers.STANDARD_ADDRESS, required = true)
        ),
        suggestedCommands = { listOf("getbalance", "gethistory", "sigindex") }
    ) {
        val address: String = getParameter("address")

        val result = cliShell.adminService.setDefaultAddress(
            VeriBlockMessages.SetDefaultAddressRequest.newBuilder()
                .setAddress(ByteStringAddressUtility.createProperByteStringAutomatically(address))
                .build()
        )
        prepareResult(result.success, result.resultsList)
    }

    rpcCommand(
        name = "Get New Address",
        form = "getnewaddress",
        description = "Gets {count} new address from the wallet (default: 1)",
        parameters = listOf(
            CommandParameter(name = "count", mapper = CommandParameterMappers.INTEGER, required = false)
        ),
        suggestedCommands = { listOf("backupwallet", "getbalance", "gethistory", "sigindex") }
    ) {
        val count = (getOptionalParameter("count") ?: 1).coerceAtLeast(1)

        val result = cliShell.adminService.getNewAddress(VeriBlockMessages.GetNewAddressRequest.newBuilder()
            .setCount(count)
            .build()
        )
        prepareResult(result.success, result.resultsList) {
            NewAddressPayload(result)
        }
    }

    rpcCommand(
        name = "Generate Multisig Address",
        form = "generatemultisigaddress|multisigaddress|maddress|maddr",
        description = "(Generates a multisig address from the provided standard addresses)",
        parameters = listOf(
            CommandParameter(name = "csvaddresses", mapper = ShellCommandParameterMappers.COMMA_SEPARATED_STANDARD_ADDRESSES, required = true),
            CommandParameter(name = "signatureThreshold", mapper = CommandParameterMappers.INTEGER, required = true)
        ),
        suggestedCommands = { listOf("getbalance", "gethistory", "makeunsignedmultisigtx", "submitmultisigtx") }
    ) {
        val requestBuilder = VeriBlockMessages.GenerateMultisigAddressRequest.newBuilder()
        val signatureThreshold: Int = getParameter("signatureThreshold")
        requestBuilder.signatureThresholdM = signatureThreshold
        val addresses = getParameter<List<String>>("csvaddresses")
        for (address in addresses) {
            requestBuilder.addSourceAddresses(ByteStringAddressUtility.createProperByteStringAutomatically(address))
        }

        val result = cliShell.adminService.generateMultisigAddress(requestBuilder.build())
        prepareResult(result.success, result.resultsList) {
            GenerateMultisigAddressPayload(result)
        }
    }

    rpcCommand(
        name = "Drain Address",
        form = "drainaddress",
        description = "Transfers the entire balance of coins for an address",
        parameters = listOf(
            CommandParameter(name = "sourceAddress", mapper = ShellCommandParameterMappers.STANDARD_ADDRESS, required = true),
            CommandParameter(name = "destinationAddress", mapper = ShellCommandParameterMappers.STANDARD_OR_MULTISIG_ADDRESS, required = true)
        )
    ) {
        val sourceAddress: String = getParameter("sourceAddress")
        val destinationAddress: String = getParameter("destinationAddress")

        val result = cliShell.adminService.drainAddress(VeriBlockMessages.DrainAddressRequest.newBuilder()
            .setSourceAddress(ByteStringAddressUtility.createProperByteStringAutomatically(sourceAddress))
            .setDestinationAddress(ByteStringAddressUtility.createProperByteStringAutomatically(destinationAddress))
            .build()
        )
        prepareResult(result.success, result.resultsList) {
            TransactionInfo(result.transaction)
        }
    }
}
