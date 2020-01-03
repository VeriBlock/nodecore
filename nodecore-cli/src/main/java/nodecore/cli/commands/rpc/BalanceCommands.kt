package nodecore.cli.commands.rpc

import nodecore.api.grpc.VeriBlockMessages
import nodecore.api.grpc.utilities.ByteStringAddressUtility
import nodecore.cli.CliShell
import nodecore.cli.serialization.AddressBalanceSchedulePayload
import nodecore.cli.serialization.BalancePayload
import nodecore.cli.prepareResult
import nodecore.cli.rpcCommand
import org.veriblock.shell.CommandParameter
import org.veriblock.shell.CommandParameterType

fun CliShell.balanceCommands() {
    rpcCommand(
        name = "Get Balance",
        form = "getbalance",
        description = "See the balances of all of your addresses",
        parameters = listOf(
            CommandParameter(name = "address", type = CommandParameterType.STANDARD_ADDRESS, required = false)
        ),
        suggestedCommands = listOf("gethistory", "getnewaddress")
    ) {
        val address: String = getParameter("address")
        val request = VeriBlockMessages.GetBalanceRequest.newBuilder()
        if (address != null) {
            request.addAddresses(ByteStringAddressUtility.createProperByteStringAutomatically(address));
        }
        val result = adminService.getBalance(request.build())

        prepareResult(result.success, result.resultsList) {
            BalancePayload(result)
        }
    }

    rpcCommand(
        name = "Get Balance Unlock Schedule",
        form = "getbalanceunlockschedule",
        description = "See the schedule in which locked balance become available",
        parameters = listOf(
            CommandParameter(name = "address", type = CommandParameterType.STANDARD_OR_MULTISIG_ADDRESS, required = false)
        ),
        suggestedCommands = listOf("getbalance")
    ) {
        val address: String = getParameter("address")
        val request = VeriBlockMessages.GetBalanceUnlockScheduleRequest.newBuilder()
        if (address != null) {
            request.addAddresses(ByteStringAddressUtility.createProperByteStringAutomatically(address))
        }
        val result = adminService.getBalanceUnlockSchedule(request.build())

        prepareResult(result.success, result.resultsList) {
            result.addressScheduleList.map {
                AddressBalanceSchedulePayload(it)
            }
        }
    }
}
