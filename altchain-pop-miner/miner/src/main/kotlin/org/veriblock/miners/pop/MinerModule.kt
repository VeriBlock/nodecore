// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop

import org.koin.dsl.module.module
import org.veriblock.lite.NodeCoreLiteKit
import org.veriblock.lite.core.Context
import org.veriblock.miners.pop.securityinheriting.SecurityInheritingService
import org.veriblock.miners.pop.shell.configure
import org.veriblock.shell.Shell

val minerModule = module {
    single { NodeCoreLiteKit(Context) }
    single { Miner(get(), get(), get()) }
    single { SecurityInheritingService(get(), get()) }
    single {
        Shell().apply {
            configure(get())
        }
    }
}
