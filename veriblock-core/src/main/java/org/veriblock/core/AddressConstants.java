// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core;

public final class AddressConstants {
    private AddressConstants(){}

    public static final int ADDRESS_LENGTH = 30;
    public static final int MULTISIG_ADDRESS_LENGTH = 30;

    public static final int ADDRESS_DATA_START = 0;
    public static final int ADDRESS_DATA_END = 24;
    public static final int MULTISIG_ADDRESS_DATA_START = 0;
    public static final int MULTISIG_ADDRESS_DATA_END = 24;

    public static final int ADDRESS_CHECKSUM_START = 25;
    public static final int ADDRESS_CHECKSUM_END = 29;
    public static final int ADDRESS_CHECKSUM_LENGTH = ADDRESS_CHECKSUM_END - ADDRESS_CHECKSUM_START;
    public static final int MULTISIG_ADDRESS_CHECKSUM_START = 25;
    public static final int MULTISIG_ADDRESS_CHECKSUM_END = 28;
    public static final int MULTISIG_ADDRESS_CHECKSUM_LENGTH = MULTISIG_ADDRESS_CHECKSUM_END - MULTISIG_ADDRESS_CHECKSUM_START;

    public static final int MULTISIG_ADDRESS_M_VALUE = 1;
    public static final int MULTISIG_ADDRESS_N_VALUE = 2;

    public static final int MULTISIG_ADDRESS_MIN_M_VALUE = 1;
    public static final int MULTISIG_ADDRESS_MIN_N_VALUE = 2;
    public static final int MULTISIG_ADDRESS_MAX_M_VALUE = 58;
    public static final int MULTISIG_ADDRESS_MAX_N_VALUE = 58;
    public static final int MULTISIG_ADDRESS_SIGNING_GROUP_START = 3;
    public static final int MULTISIG_ADDRESS_SIGNING_GROUP_END = 24;
    public static final int MULTISIG_ADDRESS_SIGNING_GROUP_LENGTH = MULTISIG_ADDRESS_SIGNING_GROUP_END - MULTISIG_ADDRESS_SIGNING_GROUP_START;

    public static final int MULTISIG_ADDRESS_IDENTIFIER_INDEX = 30;
}
