// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.utilities

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import java.lang.ClassLoader.getSystemResourceAsStream
import java.nio.file.Paths

private const val CONFIG_FILE_ENV_VAR = "CONFIG_FILE"
private const val CONFIG_FILE = "./application.conf"
private const val CONFIG_RESOURCE_FILE = "application.conf"
private const val DEFAULT_CONFIG_RESOURCE_FILE = "application-default.conf"

object Configuration {
    private var config: Config = loadConfig()

    fun <T> getOrNull(path: String, extractor: Config.(String) -> T) = if (config.hasPath(path)) {
        config.extractor(path)
    } else {
        null
    }

    fun list(): Map<String, String> {
        val sysProperties = System.getProperties()
        return config.entrySet().filter { entry ->
            !sysProperties.containsKey(entry.key)
        }.associate {
            it.key to it.value.render()
        }
    }

    fun setProperty(key: String, value: String) {
        config = ConfigFactory.parseMap(mapOf(key to value)).withFallback(config)
    }

    fun getBoolean(path: String) = getOrNull(path) { getBoolean(it) }

    fun getInt(path: String) = getOrNull(path) { getInt(it) }

    fun getLong(path: String) = getOrNull(path) { getLong(it) }

    fun getString(path: String) = getOrNull(path) { getString(it) }

    inline fun <reified T> extract(path: String) = getOrNull(path) { extract<T>(it) }
}

private val logger = createLogger {}

private fun loadConfig(): Config {
    val isDocker = System.getenv("DOCKER")?.toBoolean() ?: false
    // Attempt to load config file
    val configFile = Paths.get(System.getenv(CONFIG_FILE_ENV_VAR) ?: CONFIG_FILE).toFile()
    val appConfig = if (configFile.exists()) {
        // Parse it if it exists
        logger.debug { "Loading config file $configFile" }
        ConfigFactory.parseFile(configFile)
    } else {
        logger.debug { "Config file $configFile does not exist! Loading defaults..." }
        if (!isDocker) {
            logger.debug { "Writing to config file with default contents..." }
            // Otherwise, write the default config resource file (in non-docker envs)
            getSystemResourceAsStream(DEFAULT_CONFIG_RESOURCE_FILE)?.let {
                // Write its contents as the config file
                configFile.writeBytes(it.readBytes())
            }
        }
        // And return the default config
        ConfigFactory.load()
    }
    val resourceConfig = ConfigFactory.load(CONFIG_RESOURCE_FILE)
    return appConfig.withFallback(resourceConfig).resolve()
}
