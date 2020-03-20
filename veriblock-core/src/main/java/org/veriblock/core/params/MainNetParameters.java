// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.params;

import org.veriblock.core.bitcoinj.BitcoinUtilities;

import java.math.BigInteger;

public class MainNetParameters implements NetworkParameters {
    public static final String NETWORK = "mainnet";

    private static final int DEFAULT_PEER_BIND_PORT = 7500;
    private static final int DEFAULT_RPC_BIND_PORT = 10500;

    private static final int PROTOCOL_VERSION = 3;

    private static final int INITIAL_BITCOIN_BLOCK_HEIGHT = 568690;
    private static final int INITIAL_BITCOIN_BLOCK_VERSION = 545259520;
    private static final String INITIAL_BITCOIN_BLOCK_PREVIOUS_HASH = "00000000000000000018f62f3a9fbec9bce00dca759407649d0ac2eaee34e45e";
    private static final String INITIAL_BITCOIN_BLOCK_MERKLE_ROOT = "11a29ab555186bde5ad5b20c54a3dc176ef9105a066df69934dcfe22f09c0984";
    private static final int INITIAL_BITCOIN_BLOCK_TIMESTAMP = 1553493015;
    private static final int INITIAL_BITCOIN_BLOCK_DIFFICULTY = Integer.parseUnsignedInt("388767596");
    private static final int INITIAL_BITCOIN_BLOCK_WINNING_NONCE = Integer.parseUnsignedInt("2328158480");
    private static final String INITIAL_VERIBLOCK_BLOCK_HASH = "0000000000F4FD66B91F0649BB3FCB137823C5CE317C105C";

    private static final BigInteger MINIMUM_POW_DIFFICULTY = BigInteger.valueOf(900_000_000_000L);

    @Override
    public String getNetworkName() {
        return NETWORK;
    }

    @Override
    public int getRpcPort() {
        return DEFAULT_RPC_BIND_PORT;
    }

    @Override
    public int getP2PPort() {
        return DEFAULT_PEER_BIND_PORT;
    }

    @Override
    public byte[] getInitialBitcoinBlockHeader() {
        return BitcoinUtilities.constructBitcoinBlockHeader(
                INITIAL_BITCOIN_BLOCK_VERSION,
                INITIAL_BITCOIN_BLOCK_PREVIOUS_HASH,
                INITIAL_BITCOIN_BLOCK_MERKLE_ROOT,
                INITIAL_BITCOIN_BLOCK_TIMESTAMP,
                INITIAL_BITCOIN_BLOCK_DIFFICULTY,
                INITIAL_BITCOIN_BLOCK_WINNING_NONCE);
    }

    @Override
    public int getInitialBitcoinBlockIndex() {
        return INITIAL_BITCOIN_BLOCK_HEIGHT;
    }

    @Override
    public String getGenesisBlockHash() {
        return INITIAL_VERIBLOCK_BLOCK_HASH;
    }

    @Override
    public int getProtocolVersion() {
        return PROTOCOL_VERSION;
    }

    @Override
    public BigInteger getMinimumDifficulty() {
        return MINIMUM_POW_DIFFICULTY;
    }
}
