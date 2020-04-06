// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.utilities

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import java.io.File
import java.lang.ClassLoader.getSystemResourceAsStream
import java.nio.file.Paths

private const val CONFIG_FILE_ENV_VAR = "CONFIG_FILE"
private const val DEFAULT_CONFIG_FILE = "application.conf"

class Configuration(
    configFilePath: String = "./$DEFAULT_CONFIG_FILE",
    bootOptions: BootOptions? = null
) {
    val path = computeConfigPath(configFilePath, bootOptions)

    private var config: Config = loadConfig(path, bootOptions)

    fun <T> getOrNull(path: String, extractor: Config.(String) -> T): T? {
        return if (config.hasPath(path)) {
            config.extractor(path)
        } else {
            null
        }
    }

    fun getBoolean(path: String) = getOrNull(path) { getBoolean(it) }

    fun getInt(path: String) = getOrNull(path) { getInt(it) }

    fun getLong(path: String) = getOrNull(path) { getLong(it) }

    fun getDouble(path: String) = getOrNull(path) { getDouble(it) }

    fun getString(path: String) = getOrNull(path) { getString(it) }

    inline fun <reified T> extract(path: String): T? = getOrNull(path) { extract<T>(it) }

    fun getDataDirectory() = getString(DATA_DIR_OPTION_KEY) ?: ""

    fun list(): Map<String, String> {
        val sysProperties = System.getProperties()
        return config.entrySet().filter { entry ->
            !sysProperties.containsKey(entry.key)
        }.associate {
            it.key to it.value.render().replace("\"", "")
        }.toSortedMap()
    }

    private val overriddenProperties = HashMap<String, String>()

    fun setProperty(key: String, value: String) {
        overriddenProperties[key] = value
        config = ConfigFactory.parseMap(mapOf(key to value)).withFallback(config)
    }

    fun saveOverriddenProperties() {
        val outputLines = listOf("# Overridden properties:") + overriddenProperties.map {
            "${it.key} = ${it.value}"
        }
        val configFile = Paths.get(path).toFile()
        if (configFile.exists()) {
            val content = configFile.readText().trim()
            val contentLines = content.split("\n")
            val propertyRegex = "[a-zA-Z0-9.]+ *= *.+".toRegex()
            val parsableProperties = contentLines.filter {
                it.matches(propertyRegex)
            }.map {
                it.split("=")[0].trim()
            }.toSet()
            val changedProperties = parsableProperties.filter { it in overriddenProperties.keys }
            val finalContent = contentLines.filter { line ->
                changedProperties.none { it in line }
            }.joinToString("\n")

            val addedContents = outputLines.filter {
                it !in finalContent
            }.joinToString("\n")
            if (addedContents.isNotEmpty()) {
                configFile.writeText("\n" + finalContent + "\n" + addedContents + "\n")
            }
        } else {
            val addedContents = overriddenProperties.map {
                "${it.key} = ${it.value}"
            }.joinToString("\n")
            if (addedContents.isNotEmpty()) {
                configFile.writeText("# Overridden properties:\n$addedContents\n")
            }
        }
    }
}

private val logger = createLogger {}

private fun computeConfigPath(configFilePath: String, bootOptions: BootOptions?): String {
    return if (bootOptions != null && bootOptions.config.hasPath(CONFIG_FILE_OPTION_KEY)) {
        bootOptions.config.getString(CONFIG_FILE_OPTION_KEY)
    } else {
        val baseDir = if (bootOptions != null && bootOptions.config.hasPath(DATA_DIR_OPTION_KEY)) {
            bootOptions.config.getString(DATA_DIR_OPTION_KEY) + File.separator
        } else {
            ""
        }
        baseDir + (System.getenv(CONFIG_FILE_ENV_VAR) ?: configFilePath)
    }
}

private fun loadConfig(configFilePath: String, bootOptions: BootOptions?): Config {
    val isDocker = System.getenv("DOCKER")?.toBoolean() ?: false
    // Attempt to load config file
    val configFile = Paths.get(configFilePath).toFile()
    val appConfig = if (configFile.exists()) {
        // Parse it if it exists
        logger.debug { "Loading config file $configFile" }
        ConfigFactory.parseFile(configFile)
    } else {
        logger.debug { "Config file $configFile does not exist! Loading defaults..." }
        if (!isDocker) {
            logger.debug { "Writing to config file with default contents..." }
            // Otherwise, write the default config resource file (in non-docker envs)
            getSystemResourceAsStream(configFilePath.defaultConfigResourceFile)?.let {
                // Write its contents as the config file
                configFile.writeBytes(it.readBytes())
            }
        }
        // And return the default config
        ConfigFactory.load()
    }
    val resourceConfig = ConfigFactory.load(DEFAULT_CONFIG_FILE)
    return if (bootOptions != null) {
        bootOptions.config.withFallback(appConfig).withFallback(resourceConfig).resolve()
    } else {
        appConfig.withFallback(resourceConfig).resolve()
    }
}

private val String.defaultConfigResourceFile: String
    get() {
        val file = substringAfterLast('/')
        if (!contains('.')) {
            return "$file-default"
        }
        val name = file.substringBeforeLast('.')
        val extension = file.substringAfterLast('.')
        return "$name-default.$extension"
    }
