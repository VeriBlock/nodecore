// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.contracts;

public interface CommandContext {
    void quit();

    void clear();

    void writeToOutput(String fmt, Object... args);

    void flush();

    <T> T getData(String name);

    <T> T getParameter(String name);

    <T> void putData(String name, T value);
}
