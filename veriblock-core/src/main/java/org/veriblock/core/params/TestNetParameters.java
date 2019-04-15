// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
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

    private static final int INITIAL_BITCOIN_BLOCK_HEIGHT = 1489475;
    private static final int INITIAL_BITCOIN_BLOCK_VERSION = 536870912;
    private static final String INITIAL_BITCOIN_BLOCK_PREVIOUS_HASH = "00000000000000b345b7bbf29bda1507a679b97967f99a10ab0088899529def7";
    private static final String INITIAL_BITCOIN_BLOCK_MERKLE_ROOT = "5e16e6cef738a2eba1fe7409318e3f558bec325392427aa3d8eaf46b028654f8";
    private static final int INITIAL_BITCOIN_BLOCK_TIMESTAMP = 1555501858;
    private static final int INITIAL_BITCOIN_BLOCK_DIFFICULTY = Integer.parseUnsignedInt("436279940");
    private static final int INITIAL_BITCOIN_BLOCK_WINNING_NONCE = Integer.parseUnsignedInt("2599551022");
    private static final String INITIAL_VERIBLOCK_BLOCK_HASH = "00000025B4BF4948C520CD719B5D5A10E3E42DB430E7D88A";

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
