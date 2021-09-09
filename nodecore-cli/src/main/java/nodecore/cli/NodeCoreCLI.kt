// VeriBlock NodeCore CLI
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

@file:JvmName("NodeCoreCLI")

package nodecore.cli

import com.google.gson.GsonBuilder
import org.koin.core.context.startKoin
import org.veriblock.core.SharedConstants
import org.veriblock.core.utilities.checkJvmVersion
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.getDiagnosticInfo
import kotlin.system.exitProcess

private val logger = createLogger {}

private fun run(args: Array<String>): Int {
    print(SharedConstants.LICENSE)

    val koin = startKoin {
        modules(listOf(defaultModule))
    }.koin

    val strDiagnostics = GsonBuilder().setPrettyPrinting().create().toJson(getDiagnosticInfo())
    logger.info(strDiagnostics)

    val options: ProgramOptions = koin.get()
    options.parse(args)

    val configuration: Configuration = koin.get()
    configuration.load()

    val shell: CliShell = koin.get()
    shell.initialize(options)

    val jvmVersionResult = checkJvmVersion()
    if (!jvmVersionResult.wasSuccessful()) {
        shell.printError("JVM version is not correct!")
        shell.printError(jvmVersionResult.error)
        return -1
    }

    return try {
        shell.run()
        0
    } catch (e: Exception) {
        1
    }
}

fun main(args: Array<String>) {
    exitProcess(run(args))
}
