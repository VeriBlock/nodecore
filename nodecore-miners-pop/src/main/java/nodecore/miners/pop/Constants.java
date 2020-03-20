// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop;

import io.grpc.Metadata;

public class Constants {
    public static final String APPLICATION_NAME = Constants.class.getPackage().getSpecificationTitle();
    public static final String APPLICATION_VERSION = Constants.class.getPackage().getSpecificationVersion();
    public static final String FULL_APPLICATION_NAME_VERSION = APPLICATION_NAME + " v" + APPLICATION_VERSION;

    public static final String DEFAULT_DATA_FILE = "nodecore-pop.dat";

    public static final Metadata.Key<String> RPC_PASSWORD_HEADER_NAME = Metadata.Key.of("X-VBK-RPC-PASSWORD", Metadata.ASCII_STRING_MARSHALLER);
    public static final String BASE58_ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";

    public static final String WALLET_SEED_VIEWED_KEY = "wallet.seed.viewed";
    public static final long DEFAULT_WALLET_CREATION_DATE = 1514764800L;

    public static final long ESTIMATED_POP_SIZE = 283L;
    public static final long BITCOIN_KB = 1000;

    public static final int POP_SETTLEMENT_INTERVAL = 400;

    public static final String BYPASS_ACKNOWLEDGEMENT_KEY = "option.skip.seed";

    public static final int MEMPOOL_CHAIN_LIMIT = 24;
}
