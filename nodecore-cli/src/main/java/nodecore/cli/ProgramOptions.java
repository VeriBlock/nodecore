// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ProgramOptions {
    private String _configPath;
    private List<String> _scripts = new ArrayList<>();
    private Properties _properties = new Properties();

    private String _connect;

    public ProgramOptions() {
        resetToDefaults();
    }

    public boolean parse(String[] args) {
        Option configFileOption = Option.builder("c")
                .argName("path")
                .hasArg()
                .required(false)
                .desc("The configuration file location")
                .longOpt("config")
                .build();

        Option paramOption = Option.builder("D")
                .required(false)
                .desc("Specify a config override in key=value form")
                .argName("property=value")
                .numberOfArgs(2)
                .valueSeparator()
                .build();

        Option connectOption = Option.builder("connect")
                .argName("address")
                .hasArg()
                .required(false)
                .desc("Specify a node to connect to on startup")
                .longOpt("connect")
                .build();

        Options options = new Options();
        options.addOption(configFileOption);
        options.addOption(paramOption);
        options.addOption(connectOption);

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine commandLine = parser.parse(options, args);

            _properties = commandLine.getOptionProperties("D");

            if (commandLine.hasOption("c"))
                _configPath = commandLine.getOptionValue('c');

            if (commandLine.hasOption("connect"))
                _connect = commandLine.getOptionValue("connect");

            return true;
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    public String getConfigPath() {
        return _configPath;
    }

    public void resetToDefaults() {
        _properties.clear();
        _configPath = "nodecore-cli.properties";
    }

    public void removeProperty(String name) {
        _properties.remove(name);
    }

    public String getProperty(String name) {
        return _properties.getProperty(name);
    }

    public String getConnect() {
        return _connect;
    }
}
