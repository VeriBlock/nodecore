// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.alt.plugin

import org.veriblock.core.utilities.Configuration
import org.veriblock.core.utilities.createLogger
import org.veriblock.sdk.alt.SecurityInheritingChain

private val logger = createLogger {}

class PluginConfig(
    val id: Long? = null,
    val family: String? = null,
    val name: String? = null
)

class PluginService(
    configuration: Configuration,
    private val plugins: PluginsContainer
) {
    private var loadedPlugins: Map<String, SecurityInheritingChain> = emptyMap()

    private val configuredPlugins: Map<String, PluginConfig> = configuration.extract("securityInheriting") ?: emptyMap()

    fun loadPlugins() {
        logger.info { "Loading plugins..." }
        logger.info { "Loaded normal plugins: ${plugins.normalPlugins.keys.joinToString()}" }
        logger.info { "Loaded family plugins: ${plugins.familyPlugins.keys.joinToString()}" }

        loadedPlugins = configuredPlugins.asSequence().mapNotNull { (key, config) ->
            val family = config.family
            if (family == null) {
                val plugin = plugins.normalPlugins[key]
                    ?: return@mapNotNull null
                logger.info { "Loaded plugin $key from config" }
                key to plugin
            } else {
                val chainId = config.id ?: run {
                    logger.warn { "Chain $key is configured to family $family but it does not have an id! Ignoring..." }
                    return@mapNotNull null
                }
                val chainSupplier = plugins.familyPlugins[family]
                    ?: return@mapNotNull null

                val chain = try {
                    chainSupplier(chainId, key, config.name ?: "")
                } catch (e: Exception) {
                    logger.warn { "Error loading chain $key: ${e.message}" }
                    return@mapNotNull null
                }
                logger.info { "Loaded plugin $key ($family family) from config" }
                key to chain
            }
        }.associate {
            it.first to it.second
        }
    }

    fun getPlugins(): Map<String, SecurityInheritingChain> =
        loadedPlugins.toMap()

    operator fun get(key: String): SecurityInheritingChain? {
        return loadedPlugins[key]
    }
}
