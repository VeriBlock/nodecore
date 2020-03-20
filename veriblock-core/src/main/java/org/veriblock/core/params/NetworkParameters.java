// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.params;

import java.math.BigInteger;

public interface NetworkParameters {
    String getNetworkName();

    int getRpcPort();

    int getP2PPort();

    byte[] getInitialBitcoinBlockHeader();

    int getInitialBitcoinBlockIndex();

    String getGenesisBlockHash();

    int getProtocolVersion();

    BigInteger getMinimumDifficulty();
}
