package nodecore.cli.commands.rpc

import nodecore.api.grpc.VeriBlockMessages
import nodecore.api.grpc.utilities.ByteStringAddressUtility
import nodecore.cli.cliShell
import nodecore.cli.commands.ShellCommandParameterMappers
import nodecore.cli.prepareResult
import nodecore.cli.rpcCommand
import nodecore.cli.serialization.AddressBalanceSchedulePayload
import nodecore.cli.serialization.BalancePayload
import org.veriblock.shell.CommandFactory
import org.veriblock.shell.CommandParameter

fun CommandFactory.balanceCommands() {
    rpcCommand(
        name = "Get Balance",
        form = "getbalance|getbal|bal",
        description = "See the balances of all of your addresses",
        parameters = listOf(
            CommandParameter(name = "address", mapper = ShellCommandParameterMappers.STANDARD_ADDRESS, required = false)
        ),
        suggestedCommands = { listOf("gethistory", "getnewaddress") }
    ) {
        val address: String? = getOptionalParameter("address")
        val request = VeriBlockMessages.GetBalanceRequest.newBuilder()
        if (address != null) {
            request.addAddresses(ByteStringAddressUtility.createProperByteStringAutomatically(address));
        }
        val result = cliShell.adminService.getBalance(request.build())

        prepareResult(result.success, result.resultsList) {
            BalancePayload(result)
        }
    }

    rpcCommand(
        name = "Get Balance Unlock Schedule",
        form = "getbalanceunlockschedule",
        description = "See the schedule in which locked balance become available",
        parameters = listOf(
            CommandParameter(name = "address", mapper = ShellCommandParameterMappers.STANDARD_OR_MULTISIG_ADDRESS, required = false)
        ),
        suggestedCommands = { listOf("getbalance") }
    ) {
        val address: String? = getOptionalParameter("address")
        val request = VeriBlockMessages.GetBalanceUnlockScheduleRequest.newBuilder()
        if (address != null) {
            request.addAddresses(ByteStringAddressUtility.createProperByteStringAutomatically(address))
        }
        val result = cliShell.adminService.getBalanceUnlockSchedule(request.build())

        prepareResult(result.success, result.resultsList) {
            result.addressScheduleList.map {
                AddressBalanceSchedulePayload(it)
            }
        }
    }
}
