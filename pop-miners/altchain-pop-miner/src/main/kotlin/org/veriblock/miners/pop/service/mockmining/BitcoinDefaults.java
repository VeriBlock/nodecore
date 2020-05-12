// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.service.mockmining;

import org.veriblock.sdk.blockchain.BitcoinBlockchainBootstrapConfig;
import org.veriblock.sdk.conf.BitcoinNetworkParameters;
import org.veriblock.sdk.conf.BitcoinRegTestParameters;
import org.veriblock.sdk.models.BitcoinBlock;
import org.veriblock.sdk.services.SerializeDeserializeService;
import org.veriblock.sdk.util.Utils;

import java.util.Arrays;

public class BitcoinDefaults {

    private static final byte[] regtestGenesis = Utils.decodeHex("0100000000000000000000000000000000000000000000000000000000000000000000003ba3edfd7a7b12b27ac72c3e67768f617fc81bc3888a51323a9fb8aa4b1e5e4adae5494dffff7f2002000000");

    public static final BitcoinBlock genesis = SerializeDeserializeService.parseBitcoinBlock(regtestGenesis);
    public static final int genesisHeight = 0;

    public static final BitcoinBlockchainBootstrapConfig bootstrap = new BitcoinBlockchainBootstrapConfig(
                                                                        Arrays.asList(genesis), genesisHeight);

    public static final BitcoinNetworkParameters networkParameters = new BitcoinRegTestParameters();
}
