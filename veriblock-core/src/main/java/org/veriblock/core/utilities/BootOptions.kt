package org.veriblock.core.utilities

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.CommandLineParser
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options

const val CONFIG_FILE_OPTION_KEY = "CONFIG_FILE"
const val DATA_DIR_OPTION_KEY = "DATA_DIR"

class BootOptions(
    options: List<BootOption>,
    args: Array<String>,
    addConfigFileOption: Boolean,
    addDataDirOption: Boolean
) {
    internal val config: Config

    init {
        val optionsContainer = Options()
        val finalOptions = ArrayList(options)
        if (addConfigFileOption) {
            finalOptions += bootOption(
                opt = "c",
                longOpt = "config",
                argName = "path",
                desc = "The configuration file location",
                configMapping = CONFIG_FILE_OPTION_KEY
            )
        }
        if (addDataDirOption) {
            finalOptions += bootOption(
                opt = "d",
                longOpt = "dataDir",
                argName = "path",
                desc = "The data directory where application files reside",
                configMapping = DATA_DIR_OPTION_KEY
            )
        }
        for (option in finalOptions) {
            optionsContainer.addOption(option.option)
        }
        val paramOption = Option.builder("D").apply {
            desc("Specify a config override in key=value form")
            argName("property=value")
            numberOfArgs(2)
            valueSeparator()
            required(false)
        }.build()
        optionsContainer.addOption(paramOption)

        val parser: CommandLineParser = DefaultParser()
        val commandLine = parser.parse(optionsContainer, args)
        val properties = commandLine.getOptionProperties("D")
        for (option in finalOptions) {
            val optionValue = with(option) {
                commandLine.getValue()
            }
            if (optionValue != null) {
                properties.setProperty(option.configMapping, optionValue)
            }
        }
        config = ConfigFactory.parseProperties(properties)
    }
}

sealed class BootOption(
    val opt: String,
    val longOpt: String?,
    val desc: String,
    val configMapping: String
) {
    abstract val option: Option
    abstract fun CommandLine.getValue(): String?
}

class EmptyBootOption(
    opt: String,
    longOpt: String?,
    desc: String,
    keyMapping: String
) : BootOption(opt, longOpt, desc, keyMapping) {
    override val option: Option = Option.builder(opt).apply {
        longOpt?.let { longOpt(longOpt) }
        desc(desc)
        required(false)
    }.build()

    override fun CommandLine.getValue(): String? =
        if (hasOption(opt)) "true" else null
}

class ArgBootOption(
    opt: String,
    longOpt: String?,
    desc: String,
    val argName: String,
    configMapping: String
) : BootOption(opt, longOpt, desc, configMapping) {
    override val option: Option = Option.builder(opt).apply {
        longOpt?.let { longOpt(longOpt) }
        desc(desc)
        hasArg()
        argName(argName)
        required(false)
    }.build()

    override fun CommandLine.getValue(): String? =
        if (hasOption(opt)) getOptionValue(opt) else null
}

fun bootOptions(
    options: List<BootOption>,
    args: Array<String>,
    addConfigFileOption: Boolean = true,
    addDataDirOption: Boolean = true
) =
    BootOptions(options, args, addConfigFileOption, addDataDirOption)

fun bootOption(opt: String, longOpt: String? = null, desc: String, keyMapping: String) =
    EmptyBootOption(opt, longOpt, desc, keyMapping)

fun bootOption(opt: String, longOpt: String? = null, desc: String, argName: String, configMapping: String) =
    ArgBootOption(opt, longOpt, desc, argName, configMapping)
