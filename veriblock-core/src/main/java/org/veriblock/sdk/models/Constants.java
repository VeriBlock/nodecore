// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.models;

import java.math.BigInteger;

public class Constants {
    public static final String BITCOIN_HEADER_MAGIC = "SPVB";
    public static final String VERIBLOCK_HEADER_MAGIC = "VSPV";

    public static final int HEADER_SIZE_BitcoinBlock = 80;

    public static final int HEADER_SIZE_VeriBlockBlock = 65;
    public static final int HEADER_SIZE_VeriBlockBlock_VBlake = 64;
    public static final int KEYSTONE_INTERVAL = 20;

    public static final int ALLOWED_TIME_DRIFT = 60 * 5;
    public static final BigInteger MAXIMUM_DIFFICULTY = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 16);

    // it contains the endorsed block header. Might contain some proof information that such block exists.
    public static final int MAX_HEADER_SIZE_PUBLICATION_DATA = 1024;
    // usually it contains the POP miner address and not exceed 30 bytes
    public static final int MAX_PAYOUT_SIZE_PUBLICATION_DATA = 10000;
    // usually it contains ContextInfoContainer which has height, previousKeystone, secondKeystone and merkle root
    public static final int MAX_CONTEXT_SIZE_PUBLICATION_DATA = 1000000;

    public static final int MINIMUM_TIMESTAMP_ONSET_BLOCK_HEIGHT = 110_000;
    public static final int POP_REWARD_PAYMENT_DELAY = 500;
    // Bitcoin's policy is 11. Given the test is for the next block, this represents
    // roughly a 2-hour backward view to mirror the future drift. We target a shorter
    // 230-minute period, but given the different cycle time, this equates to 60 blocks
    public static final int HISTORY_FOR_TIMESTAMP_AVERAGE = 20;
}
