package nodecore.cli.shell.commands

import nodecore.api.grpc.VeriBlockMessages
import nodecore.api.grpc.utilities.ByteStringAddressUtility
import nodecore.cli.commands.serialization.GenerateMultisigAddressPayload
import nodecore.cli.commands.serialization.NewAddressPayload
import nodecore.cli.commands.serialization.TransactionInfo
import nodecore.cli.commands.serialization.ValidateAddressPayload
import nodecore.cli.contracts.AdminService
import nodecore.cli.shell.cliCommand
import nodecore.cli.shell.prepareResult
import org.veriblock.shell.CommandParameter
import org.veriblock.shell.CommandParameterType
import org.veriblock.shell.Shell

fun Shell.addressCommands(
    adminService: AdminService
) {
    cliCommand(
        name = "Validate Address",
        form = "validateaddress",
        description = "Returns details about an address if it is valid",
        parameters = listOf(
            CommandParameter(name = "address", type = CommandParameterType.STANDARD_ADDRESS, required = true)
        ),
        suggestedCommands = listOf("getbalance", "gethistory")
    ) {
        val request = VeriBlockMessages.ValidateAddressRequest.newBuilder().apply {
            address = ByteStringAddressUtility.createProperByteStringAutomatically(getParameter("address"))
        }.build()

        val result = adminService.validateAddress(request)

        prepareResult(result.success, result.resultsList) {
            ValidateAddressPayload(result)
        }
    }

    cliCommand(
        name = "Set Default Address",
        form = "setdefaultaddress",
        description = "Sets the default address of a NodeCore instance",
        parameters = listOf(
            CommandParameter(name = "address", type = CommandParameterType.STANDARD_ADDRESS, required = true)
        ),
        suggestedCommands = listOf("getbalance", "gethistory", "sigindex")
    ) {
        val address: String = getParameter("address")

        val result = adminService.setDefaultAddress(
            VeriBlockMessages.SetDefaultAddressRequest.newBuilder()
                .setAddress(ByteStringAddressUtility.createProperByteStringAutomatically(address))
                .build()
        )
        prepareResult(result.success, result.resultsList)
    }

    cliCommand(
        name = "Get New Address",
        form = "getnewaddress",
        description = "Gets {count} new address from the wallet (default: 1)",
        parameters = listOf(
            CommandParameter(name = "count", type = CommandParameterType.INTEGER, required = false)
        ),
        suggestedCommands = listOf("backupwallet", "getbalance", "gethistory", "sigindex")
    ) {
        val count = (getParameter("count") ?: 1).coerceAtLeast(1)

        val result = adminService.getNewAddress(VeriBlockMessages.GetNewAddressRequest.newBuilder()
            .setCount(count)
            .build()
        )
        prepareResult(result.success, result.resultsList) {
            NewAddressPayload(result)
        }
    }

    cliCommand(
        name = "Generate Multisig Address",
        form = "generatemultisigaddress",
        description = "(Generates a multisig address from the provided standard addresses)",
        parameters = listOf(
            CommandParameter(name = "csvaddresses", type = CommandParameterType.COMMA_SEPARATED_STANDARD_ADDRESSES, required = true),
            CommandParameter(name = "signatureThreshold", type = CommandParameterType.INTEGER, required = true)
        ),
        suggestedCommands = listOf("getbalance", "gethistory", "makeunsignedmultisigtx", "submitmultisigtx")
    ) {
        val requestBuilder = VeriBlockMessages.GenerateMultisigAddressRequest.newBuilder()
        val signatureThreshold: Int = getParameter("signatureThreshold")
        requestBuilder.signatureThresholdM = signatureThreshold
        val addresses = getParameter<String>("csvaddresses").split(",")
        for (address in addresses) {
            requestBuilder.addSourceAddresses(ByteStringAddressUtility.createProperByteStringAutomatically(address))
        }

        val result = adminService.generateMultisigAddress(requestBuilder.build())
        prepareResult(result.success, result.resultsList) {
            GenerateMultisigAddressPayload(result)
        }
    }

    cliCommand(
        name = "Drain Address",
        form = "drainaddress",
        description = "Transfers the entire balance of coins for an address",
        parameters = listOf(
            CommandParameter(name = "sourceAddress", type = CommandParameterType.STANDARD_ADDRESS, required = true),
            CommandParameter(name = "destinationAddress", type = CommandParameterType.STANDARD_OR_MULTISIG_ADDRESS, required = true)
        )
    ) {
        val sourceAddress: String = getParameter("sourceAddress")
        val destinationAddress: String = getParameter("destinationAddress")

        val result = adminService.drainAddress(VeriBlockMessages.DrainAddressRequest.newBuilder()
            .setSourceAddress(ByteStringAddressUtility.createProperByteStringAutomatically(sourceAddress))
            .setDestinationAddress(ByteStringAddressUtility.createProperByteStringAutomatically(destinationAddress))
            .build()
        )
        prepareResult(result.success, result.resultsList) {
            TransactionInfo(result.transaction)
        }
    }
}
