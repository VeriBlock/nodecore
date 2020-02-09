// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.alt.plugins

import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.veriblock.sdk.alt.SecurityInheritingChain

typealias NormalPluginsContainer = Map<String, SecurityInheritingChain>
typealias FamilyPluginsContainer = Map<String, (Long, String, String) -> SecurityInheritingChain>

val pluginsModule = module {
    // Normal plugins
    single<NormalPluginsContainer>(named("normal-plugins")) {
        mapOf(
            "test" to TestChain()
        )
    }
    // Family plugins
    single<FamilyPluginsContainer>(named("family-plugins")) {
        mapOf(
            "btc" to { id, key, name -> BitcoinFamilyChain(id, key, name) },
            "nxt" to { id, key, name -> NxtFamilyChain(id, key, name) }
        )
    }
}
