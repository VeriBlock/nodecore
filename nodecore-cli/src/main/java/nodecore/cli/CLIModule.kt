// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli

import nodecore.cli.shell.configure
import org.koin.dsl.module
import org.veriblock.shell.Shell

@JvmField
val defaultModule = module {
    single { ProgramOptions() }
    single { Configuration(get()) }
    //single { DefaultCommandContext(get(), get(), get()) } // Map?
    single {
        Shell().apply {
            configure(get())
        }
    }
}
