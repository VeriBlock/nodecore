// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli

import org.veriblock.core.utilities.createLogger
import java.io.*
import java.lang.Exception
import java.util.*

private val logger = createLogger {}

class Configuration(
    private val options: ProgramOptions
) {
    private var properties = Properties()
    private val defaultProperties = Properties()

    init {
        loadDefaults()
        properties = Properties(defaultProperties)
    }

    private fun getPropertyOverrideOrDefault(name: String): String? {
        val value = options.getProperty(name)
        if (!value.isNullOrEmpty()) {
            return value
        }
        return properties.getProperty(name)
    }

    val isDebugEnabled: Boolean
        get() = getPropertyOverrideOrDefault("debug.enabled")?.toBoolean() ?: false

    fun load() {
        try {
            FileInputStream(options.configPath).use { stream -> load(stream) }
        } catch (e: FileNotFoundException) {
            logger.info {
                "Unable to load custom properties file: File '${options.configPath}' does not exist. Using default properties."
            }
        } catch (e: IOException) {
            logger.info("Unable to load custom properties file. Using default properties.", e)
        }
    }

    fun load(inputStream: InputStream?) {
        try {
            properties.load(inputStream)
        } catch (e: Exception) {
            logger.error("Unhandled exception in DefaultConfiguration.load", e)
        }
    }

    private fun loadDefaults() {
        try {
            Configuration::class.java
                .classLoader
                .getResourceAsStream(Constants.DEFAULT_PROPERTIES).use { stream -> defaultProperties.load(stream) }
        } catch (e: IOException) {
            logger.error("Unable to load default properties", e)
        }
    }

    fun save() {
        try {
            val configFile = File(options.configPath)
            if (!configFile.exists()) {
                configFile.createNewFile()
            }
            val stream: OutputStream = FileOutputStream(configFile)
            save(stream)
            stream.close()
        } catch (e: IOException) {
            logger.warn("Unable to save custom properties file", e)
        }
    }

    fun save(outputStream: OutputStream) {
        try {
            properties.store(outputStream, "NodeCore Configuration")
            outputStream.flush()
        } catch (e: Exception) {
            logger.error("Unhandled exception in DefaultConfiguration.save", e)
        }
    }

    fun clearProperties() {
        properties.clear()
    }

    val privateKeyPath: String
        get() = getPropertyOverrideOrDefault(ConfigurationKeys.SECURITY_PRIVATE_KEY_PATH) ?: ""
    val certificateChainPath: String
        get() = getPropertyOverrideOrDefault(ConfigurationKeys.SECURITY_CERT_CHAIN_PATH) ?: ""

    private object ConfigurationKeys {
        const val SECURITY_CERT_CHAIN_PATH = "rpc.security.cert.chain.path"
        const val SECURITY_PRIVATE_KEY_PATH = "rpc.security.private.key.path"
    }
}
