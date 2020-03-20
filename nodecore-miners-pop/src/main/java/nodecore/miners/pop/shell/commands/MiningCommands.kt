// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.shell.commands

import com.google.gson.Gson
import io.grpc.StatusRuntimeException
import nodecore.miners.pop.PoPMiner
import nodecore.miners.pop.contracts.PoPOperationInfo
import nodecore.miners.pop.shell.toShellResult
import org.veriblock.shell.CommandFactory
import org.veriblock.shell.CommandParameter
import org.veriblock.shell.CommandParameterMappers
import org.veriblock.shell.command
import org.veriblock.shell.core.failure
import org.veriblock.shell.core.success

fun CommandFactory.miningCommands(
    miner: PoPMiner,
    prettyPrintGson: Gson
) {
    command(
        name = "Mine",
        form = "mine",
        description = "Begins a proof of proof mining operation",
        parameters = listOf(
            CommandParameter("blockNumber", CommandParameterMappers.INTEGER, false)
        )
    ) {
        val blockNumber: Int? = getOptionalParameter("blockNumber")
        miner.mine(blockNumber).toShellResult()
    }

    command(
        name = "List Operations",
        form = "listoperations",
        description = "Lists the current running operations"
    ) {
        val operations = miner.listOperations()
        if (operations.isNotEmpty()) {
            printInfo("Running operations:")
            for (summary in operations) {
                printInfo("    '${summary.operationId}': { state: '${summary.state}', action: '${summary.action}', endorsed_block: ${summary.endorsedBlockNumber} }")
                if (!summary.message.isNullOrEmpty()) {
                    printInfo("                ${summary.message}")
                }
            }
        } else {
            printInfo("No running operations")
        }

        success()
    }

    command(
        name = "Get Operation",
        form = "getoperation",
        description = "Gets the details of the supplied operation",
        parameters = listOf(
            CommandParameter("id", CommandParameterMappers.STRING)
        )
    ) {
        val id: String = getParameter("id")
        val state = miner.getOperationState(id)
        if (state != null) {
            printInfo(prettyPrintGson.toJson(PoPOperationInfo(state)))
            success()
        } else {
            failure {
                addMessage("V404", "Not found", "Could not find operation '$id'", false)
            }
        }
    }

    command(
        name = "Resubmit Operation",
        form = "resubmit",
        description = "Resubmits an operation",
        parameters = listOf(
            CommandParameter("id", CommandParameterMappers.STRING)
        )
    ) {
        val id: String = getParameter("id")
        miner.resubmit(id).toShellResult()
    }

    command(
        name = "Show Miner Address",
        form = "showmineraddress",
        description = "Returns the NodeCore miner address"
    ) {
        try {
            val minerAddress = miner.minerAddress
            if (minerAddress != null) {
                printInfo("Miner Address: $minerAddress")
                success {
                    addMessage("V200", "Success", minerAddress)
                }
            } else {
                failure {
                    addMessage("V412", "NodeCore Not Ready", "NodeCore has not been detected as running")
                }
            }
        } catch (e: StatusRuntimeException) {
            failure {
                addMessage("V500", "NodeCore Communication Error", e.status.description!!)
            }
        }
    }
}
