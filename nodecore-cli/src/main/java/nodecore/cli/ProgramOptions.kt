// VeriBlock NodeCore CLI
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli

import org.apache.commons.cli.CommandLineParser
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import org.apache.commons.cli.ParseException
import java.util.*

class ProgramOptions {
    lateinit var configPath: String
        private set

    var connect: String? = null

    private var properties = Properties()

    init {
        resetToDefaults()
    }

    fun parse(args: Array<String>): Boolean {
        val configFileOption = Option.builder("c")
            .argName("path")
            .hasArg()
            .required(false)
            .desc("The configuration file location")
            .longOpt("config")
            .build()
        val paramOption = Option.builder("D")
            .required(false)
            .desc("Specify a config override in key=value form")
            .argName("property=value")
            .numberOfArgs(2)
            .valueSeparator()
            .build()
        val connectOption = Option.builder("connect")
            .argName("address")
            .hasArg()
            .required(false)
            .desc("Specify a node to connect to on startup")
            .longOpt("connect")
            .build()
        val options = Options()
        options.addOption(configFileOption)
        options.addOption(paramOption)
        options.addOption(connectOption)
        val parser: CommandLineParser = DefaultParser()
        return try {
            val commandLine = parser.parse(options, args)
            properties = commandLine.getOptionProperties("D")
            if (commandLine.hasOption("c")) {
                configPath = commandLine.getOptionValue('c')
            }
            if (commandLine.hasOption("connect")) {
                connect = commandLine.getOptionValue("connect")
            }
            true
        } catch (e: ParseException) {
            println(e.message)
            false
        }
    }

    fun resetToDefaults() {
        properties.clear()
        configPath = "nodecore-cli.properties"
    }

    fun removeProperty(name: String) {
        properties.remove(name)
    }

    fun getProperty(name: String): String? {
        return properties.getProperty(name)
    }
}
