// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop;

import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Properties;

public class ProgramOptions {
    private static Logger logger = LoggerFactory.getLogger(ProgramOptions.class);

    private String configPath;
    private String dataDirectory;
    private Properties properties;

    public ProgramOptions() {
        configPath = "ncpop.properties";
    }

    public boolean parse(String[] args) {
        Option configFileOption = Option.builder("c")
                .argName("path")
                .hasArg()
                .required(false)
                .desc("The configuration file location")
                .longOpt("config")
                .build();

        Option dataDirectoryOption = Option.builder("d")
                .argName("path")
                .hasArg()
                .required(false)
                .desc("The data directory where NodeCore generated files reside")
                .longOpt("dataDir")
                .build();

        Option bypassAcknowledgement = Option.builder("skipAck")
                .required(false)
                .desc("Bypasses acknowledgement of seed words on first run")
                .build();

        Option paramOption = Option.builder("D")
                .required(false)
                .desc("Specify a config override in key=value form")
                .argName("property=value")
                .numberOfArgs(2)
                .valueSeparator()
                .build();

        Options options = new Options();
        options.addOption(configFileOption);
        options.addOption(dataDirectoryOption);
        options.addOption(bypassAcknowledgement);
        options.addOption(paramOption);

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine commandLine = parser.parse(options, args);

            this.properties = commandLine.getOptionProperties("D");

            if (commandLine.hasOption("d"))
                setDataDirectory(commandLine.getOptionValue('d'));

            if (commandLine.hasOption("c"))
                configPath = commandLine.getOptionValue('c');

            if (commandLine.hasOption("skipAck")) {
                this.properties.setProperty(Constants.BYPASS_ACKNOWLEDGEMENT_KEY, Boolean.toString(true));
            }

            return true;
        } catch (ParseException e) {
            logger.error("Unable to parse program options", e);
            return false;
        }
    }

    public String getConfigPath() {
        return configPath;
    }

    public String getDataDirectory() {
        return dataDirectory;
    }

    public String getProperty(String key) {
        if (properties == null) return null;

        return properties.getProperty(key);
    }

    public void removeProperty(String key) {
        if (properties != null)
            properties.remove(key);
    }

    private void setDataDirectory(String value) {
        if (StringUtils.isBlank(value)) {
            dataDirectory = "";
            return;
        }

        File directory = new File(value);
        if (directory.exists() && directory.isDirectory()) {
            dataDirectory = value;
        } else {
            throw new IllegalArgumentException("The program argument '-d " + value + "' is not a valid directory.");
        }
    }
}
