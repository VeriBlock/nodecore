// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.shell.commands

import com.google.gson.Gson
import org.veriblock.miners.pop.Miner
import org.veriblock.miners.pop.core.MiningOperation
import org.veriblock.miners.pop.core.OperationState
import org.veriblock.shell.CommandParameter
import org.veriblock.shell.CommandParameterType
import org.veriblock.shell.Shell
import org.veriblock.shell.command
import org.veriblock.shell.core.success

fun Shell.miningCommands(miner: Miner, prettyPrintGson: Gson) {

    command(
        name = "Mine",
        form = "mine",
        description = "Begins a proof of proof mining operation",
        parameters = listOf(
            CommandParameter("chain", CommandParameterType.STRING),
            CommandParameter("block", CommandParameterType.INTEGER, false)
        )
    ) {
        val chain: String = getParameter("chain")
        val block: Int? = getParameter("block")

        miner.mine(chain, block)
    }

    command(
        name = "List Operations",
        form = "listoperations",
        description = "Lists the current running operations"
    ) {
        val operations = miner.listOperations()

        for (operation in operations) {
            printInfo(operation)
        }

        success()
    }

    command(
        name = "Get Operation",
        form = "getoperation",
        description = "Gets the details of the supplied operation",
        parameters = listOf(
            CommandParameter("id", CommandParameterType.STRING)
        )
    ) {
        val id: String = getParameter("id")
        val state = miner.getOperation(id)
        if (state == null) {
            printInfo("Operation $id not found")
        } else {
            printInfo(prettyPrintGson.toJson(WorkflowProcessInfo(state)))
        }

        success()
    }

    command(
        name = "Get Operation VTB",
        form = "getoperationvtb",
        description = "Gets the VTB details of the supplied operation",
        parameters = listOf(
            CommandParameter("id", CommandParameterType.STRING)
        )
    ) {
        val id: String = getParameter("id")
        val process = miner.getOperation(id)
        if (process == null) {
            printInfo("Operation $id not found")
        } else {
            val state = process.state
            if (state !is OperationState.VeriBlockPublications) {
                printInfo("Operation $id has no VTBs yet")
            } else {
                printInfo(prettyPrintGson.toJson(state.veriBlockPublications))
            }
        }

        success()
    }
}

class WorkflowProcessInfo(
    val operationId: String,
    val chainId: String,
    val status: String,
    val blockHeight: Int?,
    val state: String,
    val stateDetail: List<String>
) {
    constructor(operation: MiningOperation) : this(
        operation.id,
        operation.chainId,
        operation.status.name,
        operation.blockHeight,
        operation.state.toString(),
        operation.state.getDetailedInfo()
    )
}
