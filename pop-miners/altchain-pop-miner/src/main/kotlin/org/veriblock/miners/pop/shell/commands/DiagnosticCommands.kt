// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.shell.commands

import com.google.gson.GsonBuilder
import org.veriblock.miners.pop.service.DiagnosticService
import org.veriblock.shell.CommandFactory
import org.veriblock.shell.command
import org.veriblock.shell.core.success

fun CommandFactory.diagnosticCommands(
    diagnosticService: DiagnosticService
) {
    command(
        name = "Get Debug Information",
        form = "getdebuginfo",
        description = "Collect information about the application for troubleshooting"
    ) {
        printInfo("Running several checks, this may take a few moments...")
        val debugInformation = diagnosticService.collectDiagnosticInformation()
        printInfo(GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create().toJson(debugInformation))
        success()
    }
}
