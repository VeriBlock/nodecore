// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.shell.commands

import com.google.gson.GsonBuilder
import org.veriblock.core.crypto.asVbkHash
import org.veriblock.miners.pop.securityinheriting.SecurityInheritingService
import org.veriblock.miners.pop.service.AltchainPopMinerService
import org.veriblock.shell.CommandFactory
import org.veriblock.shell.CommandParameter
import org.veriblock.shell.CommandParameterMappers
import org.veriblock.shell.command
import org.veriblock.shell.core.failure
import org.veriblock.shell.core.success

fun CommandFactory.altchainCommands(
    securityInheritingService: SecurityInheritingService,
    minerService: AltchainPopMinerService
) {
    command(
        name = "Submit Context",
        form = "submitcontext",
        description = "Submits a context block to the given altchain",
        parameters = listOf(
            CommandParameter("chain", CommandParameterMappers.STRING),
            CommandParameter("blockHash", CommandParameterMappers.STRING, required = false)
        )
    ) {
        val chainId: String = getParameter("chain")
        val monitor = securityInheritingService.getMonitor(chainId) ?: run {
            printInfo("Altchain $chainId not found")
            return@command failure()
        }
        if (!monitor.isReady()) {
            printInfo("Altchain $chainId is not ready")
            return@command failure()
        }
        val blockHash: String? = getOptionalParameter("blockHash")
        val block = if (blockHash != null) {
            minerService.gateway.getBlock(blockHash.asVbkHash()) ?: run {
                printInfo("Unable to find VBK block $blockHash")
                return@command failure()
            }
        } else {
            minerService.gateway.getLastBlock()
        }
        monitor.submitContextBlock(block)
        success()
    }
    command(
        name = "Submit VTBs",
        form = "submitvtbs",
        description = "Submits VTBs to the given altchain",
        parameters = listOf(
            CommandParameter("chain", CommandParameterMappers.STRING)
        )
    ) {
        val chainId: String = getParameter("chain")
        val monitor = securityInheritingService.getMonitor(chainId) ?: run {
            printInfo("Altchain $chainId not found")
            return@command failure()
        }
        if (!monitor.isReady()) {
            printInfo("Altchain $chainId is not ready")
            return@command failure()
        }

        monitor.submitVtbs()
        success()
    }
    command(
        name = "Handle context gap",
        form = "handlecontextgap",
        description = "Checks if there's any context gap in the given altchain and submits VTBs to it if so",
        parameters = listOf(
            CommandParameter("chain", CommandParameterMappers.STRING)
        )
    ) {
        val chainId: String = getParameter("chain")
        val monitor = securityInheritingService.getMonitor(chainId) ?: run {
            printInfo("Altchain $chainId not found")
            return@command failure()
        }
        if (!monitor.isReady()) {
            printInfo("Altchain $chainId is not ready")
            return@command failure()
        }

        monitor.handleContextGap()
        success()
    }
}
