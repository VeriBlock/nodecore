// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.params;

import org.veriblock.core.bitcoinj.BitcoinUtilities;

import java.math.BigInteger;

public class TestNetParameters implements NetworkParameters {
    public static String NETWORK = "testnet";

    private static final int DEFAULT_PEER_BIND_PORT = 7501;
    private static final int DEFAULT_RPC_BIND_PORT = 10501;

    private static final int PROTOCOL_VERSION = 2;

    private static final int INITIAL_BITCOIN_BLOCK_HEIGHT = 1580892;
    private static final int INITIAL_BITCOIN_BLOCK_VERSION = 536870912;
    private static final String INITIAL_BITCOIN_BLOCK_PREVIOUS_HASH = "00000000251E9261A15339B4BF0540A44328EC83F3797B9BAC67F47558D5F14E";
    private static final String INITIAL_BITCOIN_BLOCK_MERKLE_ROOT = "CBF519E1DC00F8FFBDC31A6AC3A73109D95890EDD9283EA71AD9BE11639249E9";
    private static final int INITIAL_BITCOIN_BLOCK_TIMESTAMP = 1570648139;
    private static final int INITIAL_BITCOIN_BLOCK_DIFFICULTY = Integer.parseUnsignedInt("486604799");
    private static final int INITIAL_BITCOIN_BLOCK_WINNING_NONCE = Integer.parseUnsignedInt("203968315");
    private static final String INITIAL_VERIBLOCK_BLOCK_HASH = "00000017EB579EC7D0CDD63379A0615DC3D68032CE248823";

    private static final BigInteger MINIMUM_POW_DIFFICULTY = BigInteger.valueOf(100_000_000L);

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
