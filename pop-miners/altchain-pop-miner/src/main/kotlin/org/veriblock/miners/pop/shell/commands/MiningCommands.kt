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
import org.veriblock.miners.pop.service.MinerService
import org.veriblock.miners.pop.service.ApmOperationExplainer
import org.veriblock.shell.CommandFactory
import org.veriblock.shell.CommandParameter
import org.veriblock.shell.CommandParameterMappers
import org.veriblock.shell.command
import org.veriblock.shell.core.failure
import org.veriblock.shell.core.success

fun CommandFactory.miningCommands(
    minerService: MinerService,
    apmOperationExplainer: ApmOperationExplainer
) {
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

        val operationId = minerService.mine(chain, block)
        success {
            addMessage("v200", "Mining operation started", "Operation id: $operationId")
        }
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
        val operation = minerService.getOperation(id)
        if (operation == null) {
            printInfo("Operation $id not found")
            failure()
        } else {
            minerService.resubmit(operation)
            success()
        }
    }

    command(
        name = "List Operations",
        form = "listoperations",
        description = "Lists the current running operations"
    ) {
        val operations = minerService.getOperations().map {
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
        val operation = minerService.getOperation(id)
        if (operation != null) {
            printInfo(prettyPrintGson.toJson(WorkflowProcessInfo(operation)))
            success()
        } else {
            printInfo("Operation $id not found")
            failure()
        }
    }
    command(
        name = "Explain Operation",
        form = "explainoperation",
        description = "Gets the debug details of the supplied operation",
        parameters = listOf(
            CommandParameter("id", CommandParameterMappers.STRING)
        )
    ) {
        val id: String = getParameter("id")
        val operation = minerService.getOperation(id)
        if (operation != null) {
            val operationDetails = apmOperationExplainer.explainOperation(operation)

            val tableFormat = "%1$-8s %2$-33s %3\$s"
            //printInfo (String.format(tableFormat, "Status", "Step", "Details"))
            operationDetails.forEach { stage ->
                val taskName = (stage.operationState.previousState?.taskName ?: "Start").toUpperCase().replace(' ', '_')
                printInfo(String.format(
                    tableFormat,
                    if (stage.status != ApmOperationExplainer.OperationStatus.PENDING) stage.status else "",
                    "${stage.operationState.id + 1}.${taskName}",
                    stage.extraInformation.firstOrNull() ?: ""
                ))
                stage.extraInformation.drop(1).forEach { extraInformation ->
                    printInfo(String.format(tableFormat, "", "", extraInformation))
                }
            }
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
        val operation = minerService.getOperation(id)
        if (operation != null) {
            printInfo(operation.getLogs(level).joinToString("\n"))
            success()
        } else {
            printInfo("Operation $id not found")
            failure()
        }
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
        minerService.cancelOperation(id)
        success("V200", "Success", "The operation '$id' has been cancelled")
    }
}

data class WorkflowProcessInfo(
    val operationId: String,
    val chain: String,
    val state: String,
    val blockHeight: Int?,
    val task: String,
    val stateDetail: Map<String, String>
) {
    constructor(operation: ApmOperation) : this(
        operation.id,
        operation.chain.name,
        operation.state.name,
        operation.endorsedBlockHeight,
        operation.state.taskName,
        operation.getDetailedInfo()
    )
}
