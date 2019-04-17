// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli;

import io.grpc.Metadata;

public final class Constants {
    public static final int EARLIEST_BITCOIN_BLOCK_TIMESTAMP = 1231006504;

    public static final String EMPTY_HASH = "000000000000000000000000000000000000000000000000";

    /* The starting character makes addresses easy for humans to recognize. 'V' for VeriBlock. */
    public static final char STARTING_CHAR = 'V';

    public static final String BASE58_ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";

    public static final String APPLICATION_NAME = Constants.class.getPackage().getSpecificationTitle();
    public static final String APPLICATION_VERSION = Constants.class.getPackage().getSpecificationVersion();
    public static final String FULL_APPLICATION_NAME_VERSION = APPLICATION_NAME + " v" + APPLICATION_VERSION;

    public static final String HEX_ALPHABET = "0123456789ABCDEF";

    public static final char[] HEX_ALPHABET_ARRAY = HEX_ALPHABET.toCharArray();

    public static final int HEARTBEAT_FREQUENCY = 500;

    public static final String DEFAULT_PROPERTIES = "nodecore-cli-default.properties";

    public static final Metadata.Key<String> RPC_PASSWORD_HEADER_NAME = Metadata.Key.of("X-VBK-RPC-PASSWORD", Metadata.ASCII_STRING_MARSHALLER);

    public final static class Errors {
        public static final int ERROR_WALLET_MISFORMATTED = -4;
        public static final int ERROR_WALLET_UNACCESSIBLE = -5;
        public static final int ERROR_NO_ELLIPTICAL_CRYPTOGRAPHY = -2;
        public static final int ERROR_NO_SECP_256_R_1_ELLIPTICAL_CRYPTOGRAPHY = -3;
    }
}
