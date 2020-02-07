package nodecore.cli.commands.rpc

import com.google.protobuf.ByteString
import nodecore.api.grpc.VeriBlockMessages
import nodecore.api.grpc.utilities.ByteStringAddressUtility
import nodecore.api.grpc.utilities.ByteStringUtility
import nodecore.cli.cliShell
import nodecore.cli.commands.ShellCommandParameterMappers
import nodecore.cli.prepareResult
import nodecore.cli.rpcCommand
import nodecore.cli.serialization.SignMessagePayload
import nodecore.cli.serialization.SignatureIndexPayload
import org.veriblock.core.utilities.Utility
import org.veriblock.shell.CommandFactory
import org.veriblock.shell.CommandParameter
import org.veriblock.shell.CommandParameterMappers

fun CommandFactory.signatureCommands() {
    rpcCommand(
        name = "Signature Index",
        form = "sigindex|signatureindex",
        description = "Gets the signature index for the specified address",
        parameters = listOf(
            CommandParameter(name = "address", mapper = ShellCommandParameterMappers.STANDARD_OR_MULTISIG_ADDRESS, required = false)
        ),
        suggestedCommands = { listOf("getbalance", "gethistory") }
    ) {
        val address: String? = getOptionalParameter("address")
        val request = VeriBlockMessages.GetSignatureIndexRequest.newBuilder()
        if (address != null) {
            request.addAddresses(ByteStringAddressUtility.createProperByteStringAutomatically(address))
        }
        val result = cliShell.adminService.getSignatureIndex(request.build())

        prepareResult(result.success, result.resultsList) {
            SignatureIndexPayload(result)
        }
    }

    rpcCommand(
        name = "Sign Hex Message",
        form = "signhexmessage",
        description = "(Signs a hex-encoded message with the addresses private key)",
        parameters = listOf(
            CommandParameter(name = "address", mapper = ShellCommandParameterMappers.STANDARD_ADDRESS, required = true),
            CommandParameter(name = "message", mapper = CommandParameterMappers.HEX_STRING, required = true)
        )
    ) {
        val address: String = getParameter("address")
        val message: ByteArray = Utility.hexToBytes(getParameter<String>("message"))
        val request = VeriBlockMessages.SignMessageRequest.newBuilder()
            .setAddress(ByteStringAddressUtility.createProperByteStringAutomatically(address))
            .setMessage(ByteString.copyFrom(message))
        val result = cliShell.adminService.signMessage(request.build())

        prepareResult(result.success, result.resultsList) {
            SignMessagePayload(address, result)
        }
    }

    rpcCommand(
        name = "Sign Message",
        form = "signmessage",
        description = "Signs a message with the addresses private key",
        parameters = listOf(
            CommandParameter(name = "address", mapper = ShellCommandParameterMappers.STANDARD_ADDRESS, required = true),
            CommandParameter(name = "message", mapper = CommandParameterMappers.STRING, required = true)
        )
    ) {
        val address: String = getParameter("address")
        val message: String = getParameter("message")
        val request = VeriBlockMessages.SignMessageRequest.newBuilder()
            .setAddress(ByteStringAddressUtility.createProperByteStringAutomatically(address))
            .setMessage(ByteStringUtility.base64ToByteString(message))
        val result = cliShell.adminService.signMessage(request.build())

        prepareResult(result.success, result.resultsList) {
            SignMessagePayload(address, result)
        }
    }
}
