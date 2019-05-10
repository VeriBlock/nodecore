// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.contracts;

public interface ProgramOptions {
    boolean parse(String[] args);

    String getConfigPath();

    String getDataDirectory();

    String getProperty(String key);

    void removeProperty(String key);
}
