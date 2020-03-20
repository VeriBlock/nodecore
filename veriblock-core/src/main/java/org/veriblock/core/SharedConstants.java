// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core;

import java.math.BigInteger;

public final class SharedConstants {
    public static final BigInteger DIFFICULTY_CALCULATOR_MAXIMUM_TARGET = new BigInteger(
            "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF",
            16);

    /* The starting character makes addresses easy for humans to recognize. 'V' for VeriBlock. */
    public static final char STARTING_CHAR = 'V';

    /* '0' for multisig as '0' is not part of the Base-58 alphabet */
    public static final char ENDING_CHAR_MULTISIG = '0';

    public static final String HEX_ALPHABET = "0123456789ABCDEF";

    public static final char[] HEX_ALPHABET_ARRAY = HEX_ALPHABET.toCharArray();

    public static final int VBLAKE_HASH_OUTPUT_SIZE_BYTES     = 24;

    public static final String LICENSE = "Copyright 2017-2020 Xenios SEZC" + System.lineSeparator() +
            "All rights reserved." + System.lineSeparator() +
            "Distributed under the MIT software license, see the accompanying" + System.lineSeparator() +
            "file LICENSE or http://www.opensource.org/licenses/mit-license.php." + System.lineSeparator() + System.lineSeparator();

    public static final byte STANDARD_ADDRESS_ID           = (byte)0x01;
    public static final byte MULTISIG_ADDRESS_ID           = (byte)0x03;

    public static final byte STANDARD_TRANSACTION_ID           = (byte)0x01;
    public static final byte MULTISIG_TRANSACTION_ID           = (byte)0x03;

    public final static class Errors {
        public static final int ERROR_NO_ELLIPTICAL_CRYPTOGRAPHY = -2;
        public static final int ERROR_NO_SECP_256_K_1_ELLIPTICAL_CRYPTOGRAPHY = -3;
        public static final int ERROR_WALLET_MISFORMATTED = -4;
        public static final int ERROR_WALLET_UNACCESSIBLE = -5;
        public static final int ERROR_NO_X509_KEY_SPEC = -6;
    }
}
