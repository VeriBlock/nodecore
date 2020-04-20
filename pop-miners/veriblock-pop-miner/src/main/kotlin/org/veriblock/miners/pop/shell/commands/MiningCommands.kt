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
import io.grpc.StatusRuntimeException
import org.veriblock.miners.pop.core.VpmOperation
import org.veriblock.miners.pop.service.MinerService
import org.veriblock.miners.pop.shell.toShellResult
import org.veriblock.shell.CommandFactory
import org.veriblock.shell.CommandParameter
import org.veriblock.shell.CommandParameterMappers
import org.veriblock.shell.command
import org.veriblock.shell.core.failure
import org.veriblock.shell.core.success

fun CommandFactory.miningCommands(
    minerService: MinerService
) {
    val prettyPrintGson = GsonBuilder().setPrettyPrinting().create()
    command(
        name = "Mine",
        form = "mine",
        description = "Begins a proof of proof mining operation",
        parameters = listOf(
            CommandParameter("blockNumber", CommandParameterMappers.INTEGER, false)
        )
    ) {
        val blockNumber: Int? = getOptionalParameter("blockNumber")
        minerService.mine(blockNumber).toShellResult()
    }

    command(
        name = "List Operations",
        form = "listoperations",
        description = "Lists the current running operations"
    ) {
        val operations = minerService.listOperations().map {
            "${it.operationId} (${it.endorsedBlockNumber}): ${it.action}"
        }

        if (operations.isNotEmpty()) {
            printInfo("Running operations:")
            for (operation in operations) {
                printInfo("\t$operation")
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
        val operation = minerService.getOperation(id)
        if (operation != null) {
            printInfo(prettyPrintGson.toJson(OperationInfo(operation)))
            success()
        } else {
            failure {
                addMessage("V404", "Not found", "Could not find operation '$id'", false)
            }
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
        minerService.resubmit(id).toShellResult()
    }

    command(
        name = "Show Miner Address",
        form = "showmineraddress",
        description = "Returns the NodeCore miner address"
    ) {
        try {
            val minerAddress = minerService.getMinerAddress()
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

class OperationInfo(
    val operationId: String,
    val status: String,
    val endorsedBlockHeight: Int?,
    val state: String,
    val stateDetail: Map<String, String>
) {
    constructor(operation: VpmOperation) : this(
        operation.id,
        operation.state.name,
        operation.endorsedBlockHeight,
        operation.state.description,
        operation.getDetailedInfo()
    )
}
