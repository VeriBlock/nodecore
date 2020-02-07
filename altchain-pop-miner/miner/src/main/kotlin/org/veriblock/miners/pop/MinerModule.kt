// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop

import org.koin.dsl.module.module
import org.veriblock.core.utilities.Configuration
import org.veriblock.lite.NodeCoreLiteKit
import org.veriblock.lite.core.Context
import org.veriblock.miners.pop.securityinheriting.SecurityInheritingService
import org.veriblock.miners.pop.shell.commands.altchainCommands
import org.veriblock.miners.pop.shell.configure
import org.veriblock.shell.CommandFactory
import org.veriblock.shell.Shell

private val mockMiningEnabled = Configuration.getBoolean("miner.mock") ?: false

val minerModule = module {
    if (!mockMiningEnabled) {
        single { NodeCoreLiteKit(Context) }
        single<Miner> { AltchainPopMiner(get(), get(), get()) }
    } else {
        single<Miner> { MockMiner(get()) }
    }
    single { SecurityInheritingService(get(), get()) }
    single {
        CommandFactory().apply {
            configure(get())
            if (!mockMiningEnabled) {
                altchainCommands(get(), get())
            }
        }
    }
    single { Shell(get()) }
}
