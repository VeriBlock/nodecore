// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.plugins

import org.reflections.Reflections
import org.reflections.scanners.SubTypesScanner
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder
import org.veriblock.sdk.Configuration
import org.veriblock.sdk.alt.FamilyPluginSpec
import org.veriblock.sdk.alt.PluginSpec
import org.veriblock.sdk.alt.SecurityInheritingChain
import org.veriblock.sdk.createLogger
import java.io.File
import java.net.URL
import java.net.URLClassLoader

private val logger = createLogger {}

class PluginConfigWithFamily(
    val id: Long? = null,
    val family: String? = null,
    val name: String? = null
)

class PluginFactory {
    private val chainFamilies: Map<String, (Long, String, String) -> SecurityInheritingChain>
    private val chains: Map<String, SecurityInheritingChain>

    private val configuredPluginFamilies: Map<String, PluginConfigWithFamily> = Configuration.extract("securityInheriting") ?: emptyMap()

    init {
        // Load plugin jars
        val urls: List<URL> = try {
            File("plugins/").walk().filter {
                it.isFile && it.extension == "jar"
            }.mapNotNull {
                it.toURI().toURL()
            }.toList()
        } catch (e: Exception) {
            logger.error(e) { "Error loading plugin jars" }
            emptyList()
        }

        // Prepare reflections object
        val jvmClassLoader = javaClass.classLoader
        val urlClassLoader = URLClassLoader(urls.toTypedArray())
        val reflections = Reflections(
            ConfigurationBuilder().apply {
                setScanners(SubTypesScanner())
                setUrls(ClasspathHelper.forClassLoader(urlClassLoader))
                setClassLoaders(arrayOf<ClassLoader>(urlClassLoader, jvmClassLoader))
            }
        )

        // Get all classes implementing SecurityInheritingChain
        val siClasses = reflections.getSubTypesOf(SecurityInheritingChain::class.java)
        val siChainFamilyClasses = siClasses.filter {
            it.getAnnotation(FamilyPluginSpec::class.java) != null
        }
        val siChainClasses = siClasses.filter {
            it.getAnnotation(PluginSpec::class.java) != null
        }
        chainFamilies = siChainFamilyClasses.associate {
            val annotation = it.getAnnotation(FamilyPluginSpec::class.java)
            logger.info { "Loading plugin family ${annotation.key}" }
            annotation.key to { id: Long, key: String, name: String ->
                it.getDeclaredConstructor(Long::class.java, String::class.java, String::class.java).newInstance(id, key, name)
            }
        }
        chains = siChainClasses.asSequence().associate {
            val annotation = it.getAnnotation(PluginSpec::class.java)
            logger.info { "Loading plugin ${annotation.key}" }
            // Create the map by name and class instance
            annotation.key to it.newInstance()
        } + configuredPluginFamilies.mapNotNull {
            val family = it.value.family
                ?: return@mapNotNull null // No family, we filter it out
            val chainId = it.value.id ?: run {
                logger.warn { "Chain ${it.key} is configured to family $family but it does not have an id! Ignoring..." }
                return@mapNotNull null
            }
            val chainSupplier = chainFamilies[family]
                ?: return@mapNotNull null

            logger.info { "Loading plugin ${it.key} ($family family) from config" }
            it.key to chainSupplier(chainId, it.key, it.value.name ?: "")
        }.associate {
            it.first to it.second
        }
    }

    fun getPlugins(): Map<String, SecurityInheritingChain> =
        chains.toMap()

    operator fun get(key: String): SecurityInheritingChain? {
        return chains[key]
    }
}
