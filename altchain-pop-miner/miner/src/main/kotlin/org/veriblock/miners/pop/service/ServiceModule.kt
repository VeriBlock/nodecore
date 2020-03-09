// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.service

import org.koin.dsl.module
import org.veriblock.sdk.alt.plugin.PluginService

val serviceModule = module {
    single { OperationService(get()) }
    single { PluginService(get()) }
}
