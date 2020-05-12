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

    //ASN.1/DER ECDSA encoding max value
    public static final int MAX_SIGNATURE_SIZE = 72;
    //X509 encoding. Max value is based on experimental data.
    public static final int PUBLIC_KEY_SIZE = 88;
    public static final int MAX_CONTEXT_COUNT = 150000;

    public static final int MAX_LAYER_COUNT_MERKLE = 40;
    // size = (hash + hash.length) * MAX_LAYER_COUNT + (index + index.length) + (layers.size + layers.size.length) +
    //        (subject.length.size + subject.length.size.size) + (subject.length) + (data_size)
    public static final int MAX_MERKLE_BYTES = (Sha256Hash.BITCOIN_LENGTH + 1) * MAX_LAYER_COUNT_MERKLE + 5 + 5 + 5 + 5 + 4;

    public static final int HEADER_SIZE_BitcoinBlock = 80;

    public static final int HEADER_SIZE_VeriBlockBlock = 64;
    public static final int KEYSTONE_INTERVAL = 20;

    // according to BIP 141 maximum block size is 4000000 bytes
    public static final int MAX_RAWTX_SIZE = 4 * 1000 * 1000;

    // NodeCore is using byte value when serializing outputs so we limit to 255
    public final static int MAX_OUTPUTS_COUNT = 255;


    public static final int ALLOWED_TIME_DRIFT = 60 * 5;
    public static final BigInteger MAXIMUM_DIFFICULTY = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 16);


    public final static int SIZE_ADDRESS = 30;

    public final static int MAX_CONTEXT_COUNT_ALT_PUBLICATION = 15000;

    // it contains the endorsed block header. Might contain some proof information that such block exists.
    public static final int MAX_HEADER_SIZE_PUBLICATION_DATA = 1024;
    // usually it contains the POP miner address and not exceed 30 bytes
    public static final int MAX_PAYOUT_SIZE_PUBLICATION_DATA = 100;
    // usually it contains ContextInfoContainer which has height, previousKeystone, secondKeystone and merkle root
    public static final int MAX_CONTEXT_SIZE_PUBLICATION_DATA = 100;

}
