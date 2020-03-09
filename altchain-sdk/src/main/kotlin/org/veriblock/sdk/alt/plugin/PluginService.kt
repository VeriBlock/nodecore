// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.alt.plugin

import org.reflections.Reflections
import org.reflections.ReflectionsException
import org.reflections.scanners.SubTypesScanner
import org.veriblock.core.utilities.Configuration
import org.veriblock.core.utilities.createLogger
import org.veriblock.sdk.alt.SecurityInheritingChain

private val logger = createLogger {}

class PluginService(
    private val configuration: Configuration
) {
    private var loadedPlugins: Map<String, SecurityInheritingChain> = emptyMap()

    private val configuredPlugins: Map<String, PluginConfig> = configuration.extract("securityInheriting") ?: emptyMap()

    fun loadPlugins() {
        logger.info { "Loading plugins..." }
        val chainPluginImplementations: Set<Class<out SecurityInheritingChain>> = try {
            val reflections = Reflections("org.veriblock.alt.plugin", SubTypesScanner())
            reflections.getSubTypesOf(SecurityInheritingChain::class.java)
        } catch (e: ReflectionsException) {
            logger.warn { "No plugin implementations were found in the classpath!" }
            emptySet()
        }
        val plugins = chainPluginImplementations.filter {
            it.isAnnotationPresent(PluginSpec::class.java)
        }.associate { siClass ->
            val annotation = siClass.getAnnotation(PluginSpec::class.java)
            val constructor = try {
                siClass.getDeclaredConstructor(String::class.java, PluginConfig::class.java)
            } catch (e: NoSuchMethodException) {
                error("The plugin config ${annotation.name} doesn't have a proper constructor. ${e.message}")
            }
            val supplier: (String, PluginConfig) -> SecurityInheritingChain = { key, config ->
                constructor.newInstance(key, config)
            }
            annotation.key to supplier
        }
        logger.info { "Loaded plugin implementations: ${plugins.keys.joinToString()}" }

        loadedPlugins = configuredPlugins.asSequence().mapNotNull { (key, config) ->
            val pluginKey = config.pluginKey ?: key
            val pluginSupplier = plugins[pluginKey]
                ?: return@mapNotNull null
            val plugin = pluginSupplier(key, config)
            logger.info { "Loaded plugin $key ($pluginKey impl) from config" }
            key to plugin
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
