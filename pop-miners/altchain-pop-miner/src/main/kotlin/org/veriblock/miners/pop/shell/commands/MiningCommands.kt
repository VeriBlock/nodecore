// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.shell.commands

import ch.qos.logback.classic.Level
import com.google.gson.GsonBuilder
import org.veriblock.miners.pop.core.ApmOperation
import org.veriblock.miners.pop.core.OperationState
import org.veriblock.miners.pop.service.MinerService
import org.veriblock.shell.CommandFactory
import org.veriblock.shell.CommandParameter
import org.veriblock.shell.CommandParameterMappers
import org.veriblock.shell.command
import org.veriblock.shell.core.failure
import org.veriblock.shell.core.success

fun CommandFactory.miningCommands(miner: MinerService) {

    val prettyPrintGson = GsonBuilder().setPrettyPrinting().create()

    command(
        name = "Mine",
        form = "mine",
        description = "Begins a proof of proof mining operation",
        parameters = listOf(
            CommandParameter("chain", CommandParameterMappers.STRING),
            CommandParameter("block", CommandParameterMappers.INTEGER, false)
        )
    ) {
        val chain: String = getParameter("chain")
        val block: Int? = getOptionalParameter("block")

        miner.mine(chain, block)
    }

    command(
        name = "Resubmit",
        form = "resubmit",
        description = "Submits the PoP data of a copy of an already complete proof of proof mining operation",
        parameters = listOf(
            CommandParameter("id", CommandParameterMappers.STRING)
        )
    ) {
        val id: String = getParameter("id")
        val operation = miner.getOperation(id)
        if (operation == null) {
            printInfo("Operation $id not found")
            failure()
        } else {
            miner.resubmit(operation)
            success()
        }
    }

    command(
        name = "List Operations",
        form = "listoperations",
        description = "Lists the current running operations"
    ) {
        val operations = miner.getOperations().map {
            "${it.id}: ${it.chain.name} (${it.endorsedBlockHeight}) | ${it.state} | ${it.getStateDescription()}"
        }

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
            CommandParameter("id", CommandParameterMappers.STRING)
        )
    ) {
        val id: String = getParameter("id")
        val operation = miner.getOperation(id)
        if (operation != null) {
            printInfo(prettyPrintGson.toJson(WorkflowProcessInfo(operation)))
            success()
        } else {
            printInfo("Operation $id not found")
            failure()
        }
    }

    command(
        name = "Get Operation Logs",
        form = "getoperationlogs",
        description = "Gets the logs of the supplied operation",
        parameters = listOf(
            CommandParameter("id", CommandParameterMappers.STRING),
            CommandParameter("level", CommandParameterMappers.STRING, required = false)
        )
    ) {
        val id: String = getParameter("id")
        val levelString: String? = getOptionalParameter("level")
        val level: Level = Level.toLevel(levelString, Level.INFO)
        val operation = miner.getOperation(id)
        if (operation != null) {
            printInfo(operation.getLogs(level).joinToString("\n"))
            success()
        } else {
            printInfo("Operation $id not found")
            failure()
        }
    }

    command(
        name = "Get Operation VTB",
        form = "getoperationvtb",
        description = "Gets the VTB details of the supplied operation",
        parameters = listOf(
            CommandParameter("id", CommandParameterMappers.STRING)
        )
    ) {
        val id: String = getParameter("id")
        val process = miner.getOperation(id)
        if (process == null) {
            printInfo("Operation $id not found")
        } else {
            if (!(process.state hasType OperationState.CONTEXT)) {
                printInfo("Operation $id has no VTBs yet")
            } else {
                printInfo(prettyPrintGson.toJson(process.context?.detailedInfo))
            }
        }

        success()
    }

    command(
        name = "Cancel Operation",
        form = "canceloperation",
        description = "Cancels the operations",
        parameters = listOf(
            CommandParameter("id", CommandParameterMappers.STRING)
        )
    ) {
        val id: String = getParameter("id")
        miner.cancelOperation(id)
        success("V200", "Success", "The operation '$id' has been cancelled")
    }
}

data class WorkflowProcessInfo(
    val operationId: String,
    val chain: String,
    val status: String,
    val blockHeight: Int?,
    val state: String,
    val stateDetail: Map<String, String>
) {
    constructor(operation: ApmOperation) : this(
        operation.id,
        operation.chain.name,
        operation.state.name,
        operation.endorsedBlockHeight,
        operation.state.description,
        operation.getDetailedInfo()
    )
}
