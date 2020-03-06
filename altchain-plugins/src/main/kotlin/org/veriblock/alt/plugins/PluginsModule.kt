// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.alt.plugins

import org.koin.dsl.module
import org.veriblock.alt.plugins.bitcoin.BitcoinFamilyChain
import org.veriblock.alt.plugins.nxt.NxtConfig
import org.veriblock.alt.plugins.nxt.NxtFamilyChain
import org.veriblock.alt.plugins.test.TestChain
import org.veriblock.alt.plugins.test.TestConfig
import org.veriblock.core.utilities.Configuration
import org.veriblock.sdk.alt.PluginsContainer


/**
 * Module that will be used by APM and ABFI.
 * Very useful for type safety and boot speed, but it adds a dependency to the Koin library
 *
 * TODO: Migrate to a more lightweight alternative, such as reflection or mutating static plugin containers
 */
val pluginsModule = module {

    single {
        val configuration: Configuration = get()
        PluginsContainer(
            normalPlugins = mapOf(
                "test" to TestChain(configuration.extract("securityInheriting.test") ?: TestConfig())
            ),
            familyPlugins = mapOf(
                "btc" to { id, key, name ->
                    BitcoinFamilyChain(
                        configuration.extract("securityInheriting.btc")
                            ?: error("Please configure the securityInheriting.$key section"),
                        id,
                        key,
                        name
                    )
                },
                "nxt" to { id, key, name ->
                    NxtFamilyChain(
                        configuration.extract("securityInheriting.$key")
                            ?: NxtConfig(),
                        id,
                        key,
                        name
                    )
                }
            )
        )
    }
}
