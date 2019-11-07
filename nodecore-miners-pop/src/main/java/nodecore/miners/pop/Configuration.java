// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop;

import com.google.common.net.InetAddresses;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import nodecore.miners.pop.common.BitcoinNetwork;
import nodecore.miners.pop.contracts.ConfigurationResult;
import nodecore.miners.pop.contracts.IllegalConfigurationValueResultMessage;
import nodecore.miners.pop.contracts.MissingConfigurationValueResultMessage;
import nodecore.miners.pop.contracts.SuccessResultMessage;
import nodecore.miners.pop.events.NodeCoreConfigurationChangedEvent;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veriblock.core.utilities.Utility;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.TreeMap;

public class Configuration {
    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);
    private static final String DEFAULT_PROPERTIES = "ncpop-default.properties";

    private final ProgramOptions options;
    private final Properties defaultProperties = new Properties();
    private final Properties properties;

    public Configuration(ProgramOptions options) {
        this.options = options;
        loadDefaults();
        properties = new Properties(defaultProperties);
    }

    public void load() {
        try {
            try (InputStream stream = new FileInputStream(options.getConfigPath())) {
                load(stream);
            }
        } catch (FileNotFoundException e) {
            logger.info("Unable to load custom properties file: File '{}' does not exist. Using default properties.", options.getConfigPath());
        } catch (IOException e) {
            logger.info("Unable to load custom properties file. Using default properties.", e);
        }
    }

    public void load(InputStream inputStream) {
        try {
            properties.load(inputStream);

            // Temporary
            String perByte = properties.getProperty("bitcoin.fee.perbyte");
            if (perByte != null) {
                try {
                    Integer perByteValue = Integer.parseInt(perByte);
                    setTransactionFeePerKB(Integer.toString(perByteValue * 1000));
                    properties.remove("bitcoin.fee.perbyte");
                } catch (NumberFormatException ignored) {
                }
            }
        } catch (Exception e) {
            logger.error("Unhandled exception in DefaultConfiguration.load", e);
        }
    }

    public BitcoinNetwork getBitcoinNetwork() {
        String networkValue = getPropertyOverrideOrDefault(Keys.BITCOIN_NETWORK_KEY);
        switch (networkValue.toLowerCase()) {
            case "mainnet":
                return BitcoinNetwork.MainNet;
            case "testnet":
                return BitcoinNetwork.TestNet;
            case "regtest":
                return BitcoinNetwork.RegTest;
            default:
                logger.info("Unable to parse value {} for property {}, defaulting to {}",
                        networkValue,
                        Keys.BITCOIN_NETWORK_KEY,
                        BitcoinNetwork.MainNet.toString());
                return BitcoinNetwork.MainNet;
        }
    }

    public long getMaxTransactionFee() {
        String value = getPropertyOverrideOrDefault(Keys.BITCOIN_MAX_TRANSACTION_FEE_KEY);
        Long transactionFee = Longs.tryParse(value);
        return transactionFee != null ? transactionFee : 0L;
    }

    public ConfigurationResult setMaxTransactionFee(String value) {
        ConfigurationResult result = new ConfigurationResult();
        if (StringUtils.isBlank(value)) {
            result.addMessage(new MissingConfigurationValueResultMessage(Keys.BITCOIN_MAX_TRANSACTION_FEE_KEY));
            result.fail();
            return result;
        }

        if (Utility.isPositiveLong(value)) {
            options.removeProperty(Keys.BITCOIN_MAX_TRANSACTION_FEE_KEY);
            properties.setProperty(Keys.BITCOIN_MAX_TRANSACTION_FEE_KEY, value);
            result.addMessage(new SuccessResultMessage());
            return result;
        }

        result.addMessage(new IllegalConfigurationValueResultMessage(String.format(
                "Property '%s' requires an integer value representing an amount in Satoshis, e.g. (100000 Satoshis = 0.001 BTC)",
                Keys.BITCOIN_MAX_TRANSACTION_FEE_KEY)));
        result.fail();
        return result;
    }

    public long getTransactionFeePerKB() {
        String value = getPropertyOverrideOrDefault(Keys.BITCOIN_TRANSACTION_FEE_PER_KB_KEY);
        Long transactionFee = Longs.tryParse(value);
        return transactionFee != null ? transactionFee : 0L;
    }

    public ConfigurationResult setTransactionFeePerKB(String value) {
        ConfigurationResult result = new ConfigurationResult();
        if (StringUtils.isBlank(value)) {
            result.addMessage(new MissingConfigurationValueResultMessage(Keys.BITCOIN_TRANSACTION_FEE_PER_KB_KEY));
            result.fail();
            return result;
        }

        if (Utility.isPositiveLong(value)) {
            options.removeProperty(Keys.BITCOIN_TRANSACTION_FEE_PER_KB_KEY);
            properties.setProperty(Keys.BITCOIN_TRANSACTION_FEE_PER_KB_KEY, value);
            result.addMessage(new SuccessResultMessage());
            return result;
        }

        result.addMessage(new IllegalConfigurationValueResultMessage(String.format(
                "Property '%s' requires an integer value representing an amount in Satoshis, e.g. (200 Satoshis = 0.00000200 BTC)",
                Keys.BITCOIN_TRANSACTION_FEE_PER_KB_KEY)));
        result.fail();
        return result;
    }

    public boolean isMinimumRelayFeeEnforced() {
        return getBoolean(Keys.BITCOIN_MINIMUM_RELAY_FEE_ENFORCED_KEY);
    }

    public ConfigurationResult setMinimumRelayFeeEnforced(String value) {
        return setBoolean(Keys.BITCOIN_MINIMUM_RELAY_FEE_ENFORCED_KEY, value);
    }

    public String getNodeCoreHost() {
        return getPropertyOverrideOrDefault(Keys.NODECORE_HOST_KEY);
    }

    public ConfigurationResult setNodeCoreHost(String value) {
        ConfigurationResult result = new ConfigurationResult();
        if (StringUtils.isBlank(value)) {
            result.addMessage(new MissingConfigurationValueResultMessage(Keys.NODECORE_HOST_KEY));
            result.fail();
            return result;
        }

        if (InetAddresses.isInetAddress(value)) {
            options.removeProperty(Keys.NODECORE_HOST_KEY);
            properties.setProperty(Keys.NODECORE_HOST_KEY, value);
            InternalEventBus.getInstance().post(new NodeCoreConfigurationChangedEvent());

            result.addMessage(new SuccessResultMessage());
            return result;
        }

        result.addMessage(new IllegalConfigurationValueResultMessage(String.format("Property '%s' requires a valid IP address",
                Keys.NODECORE_HOST_KEY)));
        result.fail();
        return result;
    }

    public int getNodeCorePort() {
        Integer port = Ints.tryParse(getPropertyOverrideOrDefault(Keys.NODECORE_PORT_KEY));
        if (port == null) {
            throw new NumberFormatException("Unable to load '" + Keys.NODECORE_PORT_KEY + "'. Invalid port number specified");
        }
        return port;
    }

    public ConfigurationResult setNodeCorePort(String value) {
        ConfigurationResult result = new ConfigurationResult();
        if (StringUtils.isBlank(value)) {
            result.addMessage(new MissingConfigurationValueResultMessage(Keys.NODECORE_PORT_KEY));
            result.fail();
            return result;
        }

        if (!Utility.isInteger(value)) {
            result.addMessage(new IllegalConfigurationValueResultMessage(String.format(
                    "Property '%s' requires an integer value representing a valid network port between 1024-49151",
                    Keys.NODECORE_PORT_KEY)));
            result.fail();
            return result;
        } else {
            int port = Integer.parseInt(value);

            if (port < 1024 || port > 49151) {
                result.addMessage(new IllegalConfigurationValueResultMessage(String.format(
                        "Property '%s' requires an integer value representing a valid network port between 1024-49151",
                        Keys.NODECORE_PORT_KEY)));
                result.fail();
                return result;
            }

            options.removeProperty(Keys.NODECORE_PORT_KEY);
            properties.setProperty(Keys.NODECORE_PORT_KEY, value);
            InternalEventBus.getInstance().post(new NodeCoreConfigurationChangedEvent());
            result.addMessage(new SuccessResultMessage());
            return result;
        }
    }

    public boolean getNodeCoreUseSSL() {
        return Boolean.valueOf(getPropertyOverrideOrDefault(Keys.NODECORE_USE_SSL_KEY));
    }

    public ConfigurationResult setNodeCoreUseSSL(String value) {
        ConfigurationResult result = new ConfigurationResult();

        Boolean parsed = Boolean.valueOf(value);

        options.removeProperty(Keys.NODECORE_USE_SSL_KEY);
        properties.setProperty(Keys.NODECORE_USE_SSL_KEY, parsed.toString());

        InternalEventBus.getInstance().post(new NodeCoreConfigurationChangedEvent());
        result.addMessage(new SuccessResultMessage());
        return result;
    }

    public String getNodeCorePassword() {
        return getPropertyOverrideOrDefault(Keys.NODECORE_PASSWORD_KEY);
    }

    public ConfigurationResult setNodeCorePassword(String value) {
        ConfigurationResult result = new ConfigurationResult();

        options.removeProperty(Keys.NODECORE_PASSWORD_KEY);
        properties.setProperty(Keys.NODECORE_PASSWORD_KEY, value);

        InternalEventBus.getInstance().post(new NodeCoreConfigurationChangedEvent());
        result.addMessage(new SuccessResultMessage());
        return result;
    }

    public String getCertificateChainPath() {
        return getPropertyOverrideOrDefault(Keys.NODECORE_CERT_CHAIN_PATH_KEY);
    }

    public ConfigurationResult setCertificateChainPath(String value) {
        ConfigurationResult result = new ConfigurationResult();

        File file = new File(value);
        if (file.exists() && file.isFile()) {
            options.removeProperty(Keys.NODECORE_CERT_CHAIN_PATH_KEY);
            properties.setProperty(Keys.NODECORE_CERT_CHAIN_PATH_KEY, value);

            InternalEventBus.getInstance().post(new NodeCoreConfigurationChangedEvent());
            result.addMessage(new SuccessResultMessage());
            return result;
        }

        result.addMessage(new IllegalConfigurationValueResultMessage(String.format("Value '%s' does not refer to a file that exists",
                Keys.NODECORE_CERT_CHAIN_PATH_KEY)));
        result.fail();
        return result;
    }

    public String getCronSchedule() {
        return getPropertyOverrideOrDefault(Keys.SCHEDULE);
    }

    public int getActionTimeout() {
        Integer timeout = Ints.tryParse(getPropertyOverrideOrDefault(Keys.ACTION_TIMEOUT));
        if (timeout == null) {
            throw new NumberFormatException("Unable to load '" + Keys.ACTION_TIMEOUT + "'. Invalid timeout specified");
        }
        return timeout;
    }

    public ConfigurationResult setActionTimeout(String value) {
        ConfigurationResult result = new ConfigurationResult();
        if (StringUtils.isBlank(value)) {
            result.addMessage(new MissingConfigurationValueResultMessage(Keys.ACTION_TIMEOUT));
            result.fail();
            return result;
        }

        if (Utility.isPositiveInteger(value)) {
            options.removeProperty(Keys.ACTION_TIMEOUT);
            properties.setProperty(Keys.ACTION_TIMEOUT, value);
            result.addMessage(new SuccessResultMessage());
            return result;
        }

        result.addMessage(new IllegalConfigurationValueResultMessage(String.format(
                "Property '%s' requires a positive integer value representing the number of seconds to wait before timing out.",
                Keys.ACTION_TIMEOUT)));
        result.fail();
        return result;
    }

    public String getHttpApiAddress() {
        String address = getPropertyOverrideOrDefault(Keys.HTTP_API_ADDRESS);
        if (address == null) {
            return "127.0.0.1";
        }
        return address;
    }

    public int getHttpApiPort() {
        Integer port = Ints.tryParse(getPropertyOverrideOrDefault(Keys.HTTP_API_PORT));
        if (port == null) {
            return 8600;
        }
        return port;
    }

    public List<String> list() {
        List<String> result = new ArrayList<>();

        for (String key : Keys.getAll()) {
            result.add(String.format("%s=%s", key, getPropertyOverrideOrDefault(key)));
        }

        return result;
    }

    public ConfigurationResult setProperty(String key, String value) {
        ConfigurationResult result = new ConfigurationResult();
        switch (key.toLowerCase()) {
            case Keys.AUTO_MINE_ROUND1:
            case Keys.AUTO_MINE_ROUND2:
            case Keys.AUTO_MINE_ROUND3:
            case Keys.AUTO_MINE_ROUND4:
                result = setBoolean(key, value);
                break;
            case Keys.HTTP_API_ADDRESS:
                result.addMessage("V052",
                        "Runtime configuration not allowed",
                        String.format("Property '%s' cannot be changed at runtime. Edit properties file and restart.", Keys.HTTP_API_ADDRESS),
                        false);
                result.fail();
                break;
            case Keys.HTTP_API_PORT:
                result.addMessage("V052",
                        "Runtime configuration not allowed",
                        String.format("Property '%s' cannot be changed at runtime. Edit properties file and restart.", Keys.HTTP_API_PORT),
                        false);
                result.fail();
                break;
            case Keys.BITCOIN_NETWORK_KEY:
                result.addMessage("V052",
                        "Runtime configuration not allowed",
                        String.format("Property '%s' cannot be changed at runtime. Edit properties file and restart.", Keys.BITCOIN_NETWORK_KEY),
                        false);
                result.fail();
                break;
            case Keys.BITCOIN_MAX_TRANSACTION_FEE_KEY:
                result = setMaxTransactionFee(value);
                break;
            case Keys.BITCOIN_TRANSACTION_FEE_PER_KB_KEY:
                result = setTransactionFeePerKB(value);
                break;
            case Keys.BITCOIN_MINIMUM_RELAY_FEE_ENFORCED_KEY:
                result = setMinimumRelayFeeEnforced(value);
                break;
            case Keys.NODECORE_HOST_KEY:
                result = setNodeCoreHost(value);
                break;
            case Keys.NODECORE_PORT_KEY:
                result = setNodeCorePort(value);
                break;
            case Keys.NODECORE_USE_SSL_KEY:
                result = setNodeCoreUseSSL(value);
                break;
            case Keys.NODECORE_PASSWORD_KEY:
                result = setNodeCorePassword(value);
                break;
            case Keys.NODECORE_CERT_CHAIN_PATH_KEY:
                result = setCertificateChainPath(value);
                break;
            case Keys.SCHEDULE:
                result.addMessage("V052",
                        "Runtime configuration not allowed",
                        String.format("Property '%s' cannot be changed at runtime. Edit properties file and restart.", Keys.SCHEDULE),
                        false);
                result.fail();
                break;
            case Keys.ACTION_TIMEOUT:
                result = setActionTimeout(value);
                break;
            default:
                result.addMessage("V053",
                        "No such configuration property",
                        String.format("'%s' is not a configuration property that can be set", key),
                        false);
                result.fail();
                break;
        }

        if (!result.didFail()) {
            save();
        }

        return result;
    }

    public boolean getBoolean(String key) {
        return Boolean.parseBoolean(getPropertyOverrideOrDefault(key));
    }

    public ConfigurationResult setBoolean(String key, String value) {
        ConfigurationResult result = new ConfigurationResult();

        Boolean parsed = Boolean.valueOf(value);

        options.removeProperty(key);
        properties.setProperty(key, parsed.toString());

        result.addMessage(new SuccessResultMessage());
        return result;
    }

    public void save() {
        try {
            File configFile = new File(options.getConfigPath());
            if (!configFile.exists()) {
                configFile.createNewFile();
            }
            OutputStream stream = new FileOutputStream(configFile);
            save(stream);
            stream.close();
        } catch (IOException e) {
            logger.warn("Unable to save custom properties file", e);
        }
    }

    public boolean isValid() {
        return getMaxTransactionFee() > 0L && getTransactionFeePerKB() > 0L && StringUtils.isNotBlank(getNodeCoreHost()) && getNodeCorePort() > 0 &&
                getNodeCorePort() <= 65535;
    }

    public String getDataDirectory() {
        return options.getDataDirectory();
    }

    public String getDatabasePath() {
        String dataDirectory = getDataDirectory();
        dataDirectory = (dataDirectory != null) ? dataDirectory : "";
        if (StringUtils.isNotBlank(dataDirectory)) {
            dataDirectory = dataDirectory + File.separator;
        }

        return dataDirectory + Constants.DEFAULT_DATA_FILE;
    }

    private void save(OutputStream outputStream) {
        try {
            //properties.store(outputStream, "NodeCore PoP Miner Configuration");
            properties.store(outputStream, buildSaveComments());
            outputStream.flush();
        } catch (Exception e) {
            logger.error("Unhandled exception in DefaultConfiguration.save", e);
        }
    }

    private String buildSaveComments() {
        StringBuilder sb = new StringBuilder();
        sb.append("NodeCore PoP Miner Configuration\n\n");
        sb.append("Visit https://wiki.veriblock.org/index.php?title=HowTo_run_PoP_Miner#Reference for configuration instructions.\n\n");
        sb.append("Default configuration values are below. Uncomment to override.\n\n");

        TreeMap<String, String> sorted = new TreeMap<>();
        for (Object key : defaultProperties.keySet()) {
            if (key instanceof String) {
                String keyString = (String) key;
                if (properties.getProperty(keyString).equals(defaultProperties.getProperty(keyString))) {
                    sorted.put(keyString, defaultProperties.getProperty(keyString));
                }
            }
        }

        for (String key : sorted.keySet()) {
            sb.append(key).append("=").append(defaultProperties.getProperty(key)).append("\n");
        }

        sb.append("\nCron expressions can be built using an online tool, such as http://www.cronmaker.com/\n");
        sb.append("Below is an example that runs a mine operation at :00 and :30 of every hour\n\n");
        sb.append("pop.cron.schedule=0 0/30 * * * ?\n");

        return sb.toString();
    }

    private void loadDefaults() {
        try {
            InputStream stream = Configuration.class.getClassLoader().getResourceAsStream(DEFAULT_PROPERTIES);
            try {
                defaultProperties.load(stream);
            } finally {
                stream.close();
            }
        } catch (IOException e) {
            logger.error("Unable to load default properties", e);
        }
    }

    private String getPropertyOverrideOrDefault(final String name) {
        String value = options.getProperty(name);
        if (value != null && value.length() > 0) {
            return value;
        }
        value = properties.getProperty(name);
        if (value == null) {
            return "";
        }
        return value;
    }

    private static class Keys {
        private static final String AUTO_MINE_ROUND1 = "auto.mine.round1";
        private static final String AUTO_MINE_ROUND2 = "auto.mine.round2";
        private static final String AUTO_MINE_ROUND3 = "auto.mine.round3";
        private static final String AUTO_MINE_ROUND4 = "auto.mine.round4";
        private static final String HTTP_API_ADDRESS = "http.api.address";
        private static final String HTTP_API_PORT = "http.api.port";

        private static final String BITCOIN_NETWORK_KEY = "bitcoin.network";
        private static final String BITCOIN_MAX_TRANSACTION_FEE_KEY = "bitcoin.fee.max";
        private static final String BITCOIN_TRANSACTION_FEE_PER_KB_KEY = "bitcoin.fee.perkb";
        private static final String BITCOIN_MINIMUM_RELAY_FEE_ENFORCED_KEY = "bitcoin.minrelayfee.enabled";
        private static final String NODECORE_HOST_KEY = "nodecore.rpc.host";
        private static final String NODECORE_PORT_KEY = "nodecore.rpc.port";
        private static final String NODECORE_USE_SSL_KEY = "nodecore.rpc.ssl";
        private static final String NODECORE_PASSWORD_KEY = "nodecore.rpc.password";
        private static final String NODECORE_CERT_CHAIN_PATH_KEY = "nodecore.rpc.cert.chain.path";

        private static final String SCHEDULE = "pop.cron.schedule";
        private static final String ACTION_TIMEOUT = "pop.action.timeout";

        private static List<String> getAll() {
            List<String> keys = new ArrayList<>();
            keys.add(AUTO_MINE_ROUND1);
            keys.add(AUTO_MINE_ROUND2);
            keys.add(AUTO_MINE_ROUND3);
            keys.add(AUTO_MINE_ROUND4);
            keys.add(HTTP_API_ADDRESS);
            keys.add(HTTP_API_PORT);
            keys.add(BITCOIN_NETWORK_KEY);
            keys.add(BITCOIN_MAX_TRANSACTION_FEE_KEY);
            keys.add(BITCOIN_TRANSACTION_FEE_PER_KB_KEY);
            keys.add(BITCOIN_MINIMUM_RELAY_FEE_ENFORCED_KEY);
            keys.add(NODECORE_HOST_KEY);
            keys.add(NODECORE_PORT_KEY);
            keys.add(NODECORE_USE_SSL_KEY);
            keys.add(NODECORE_PASSWORD_KEY);
            keys.add(NODECORE_CERT_CHAIN_PATH_KEY);
            keys.add(SCHEDULE);
            keys.add(ACTION_TIMEOUT);
            return keys;
        }
    }
}
