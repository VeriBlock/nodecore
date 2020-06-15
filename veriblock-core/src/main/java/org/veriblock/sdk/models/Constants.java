// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
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

    public static final int HEADER_SIZE_VeriBlockBlock = 64;
    public static final int KEYSTONE_INTERVAL = 20;


    public static final int ALLOWED_TIME_DRIFT = 60 * 5;
    public static final BigInteger MAXIMUM_DIFFICULTY = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 16);

    // it contains the endorsed block header. Might contain some proof information that such block exists.
    public static final int MAX_HEADER_SIZE_PUBLICATION_DATA = 1024;
    // usually it contains the POP miner address and not exceed 30 bytes
    public static final int MAX_PAYOUT_SIZE_PUBLICATION_DATA = 100;
    // usually it contains ContextInfoContainer which has height, previousKeystone, secondKeystone and merkle root
    public static final int MAX_CONTEXT_SIZE_PUBLICATION_DATA = 100;
}
