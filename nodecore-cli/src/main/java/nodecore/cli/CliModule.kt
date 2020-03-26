// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli

import org.koin.dsl.module
import org.veriblock.shell.CommandFactory

val defaultModule = module {
    single { ProgramOptions() }
    single { Configuration(get()) }
    single {
        CommandFactory().apply {
            registerCommands()
        }
    }
    single { CliShell(get(), get()) }
}
