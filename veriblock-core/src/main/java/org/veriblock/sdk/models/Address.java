// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.models;

import org.veriblock.core.bitcoinj.Base58;
import org.veriblock.core.bitcoinj.Base59;
import org.veriblock.core.crypto.Sha256Hash;
import org.veriblock.core.utilities.Preconditions;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Address {
    public final static int SIZE = 30;
    public static final char STARTING_CHAR = 'V';
    private static final char MULTISIG_ENDING_CHAR = '0';

    private static final int MULTISIG_ADDRESS_M_VALUE = 1;
    private static final int MULTISIG_ADDRESS_N_VALUE = 2;

    private static final int MULTISIG_ADDRESS_MIN_N_VALUE = 2;
    private static final int MULTISIG_ADDRESS_MAX_M_VALUE = 58;
    private static final int MULTISIG_ADDRESS_MAX_N_VALUE = 58;

    private final String address;
    private final String data;
    private final String checksum;

    private final boolean multisig;

    public boolean isMultisig() {
        return this.multisig;
    }

    public Address(String address) {
        Preconditions.notNull(address,
                "Address cannot be null");
        Preconditions.argument(address.length() == SIZE && address.charAt(0) == STARTING_CHAR,
                "The address " + address + " is not a valid VBK address");

        this.address = address;
        this.data = getDataPortionFromAddress(address);
        this.multisig = (address.charAt(SIZE - 1) == MULTISIG_ENDING_CHAR);
        this.checksum = getChecksumPortionFromAddress(address, multisig);

        if (multisig) {
            Preconditions.argument(Base59.isBase59String(address),
                    "The address " + address + " is not a base59 string");

            /* To make the addresses 'human-readable' we add 1 to the decoded value (1 in Base58 is 0,
             * but we want an address with a '1' in the m slot to represent m=1, for example).
             * this allows addresses with m and n both <= 9 to be easily recognized. Additionally,
             * an m or n value of 0 makes no sense, so this allows multisig to range from 1 to 58,
             * rather than what would have otherwise been 0 to 57. */
            int m = Base58.decode("" + address.charAt(MULTISIG_ADDRESS_M_VALUE))[0] + 1;
            int n = Base58.decode("" + address.charAt(MULTISIG_ADDRESS_N_VALUE))[0] + 1;
            Preconditions.argument(n >= MULTISIG_ADDRESS_MIN_N_VALUE,
                    "The address " + address + " does not have enough addresses to be multisig");
            Preconditions.argument(m <= n,
                    "The address " + address + " has more signatures than addresses");
            Preconditions.argument(n <= MULTISIG_ADDRESS_MAX_N_VALUE && m <= MULTISIG_ADDRESS_MAX_M_VALUE,
                    "The address " + address + " has too many addresses/signatures");


            Preconditions.argument(
                Base58.isBase58String(address.substring(0, SIZE - 1)),
                    "The address " + address + "'s remainder is not a base58 string");
        } else {
            Preconditions.argument(
                Base58.isBase58String(address),
                    "The address " + address + " is not a base58 string");
        }

        if (!calculateChecksum(data, multisig).equals(checksum)) {
            throw new IllegalArgumentException("Address checksum does not match");
        }
    }

    public boolean isDerivedFromPublicKey(byte[] publicKey) {
        try {
            Sha256Hash hash = Sha256Hash.of(publicKey);
            String data = "V" + Base58.encode(hash.getBytes()).substring(0, 24);
            String checksum = calculateChecksum(data, multisig);

            return this.data.equals(data) && this.checksum.equals(checksum);
        } catch (Exception e) {
            return false;
        }
    }

    static public Address fromPublicKey(byte[] publicKey) {
        byte[] keyHash = Sha256Hash.of(publicKey).getBytes();
        String data = "V" + Base58.encode(keyHash).substring(0, 24);

        Sha256Hash hash = Sha256Hash.of(data.getBytes(StandardCharsets.UTF_8));
        String checksum = Base58.encode(hash.getBytes()).substring(0, 4 + 1);

        return new Address(data + checksum);
    }

    public byte[] getBytes() {
        return isMultisig() ? Base59.decode(this.address) : Base58.decode(this.address);
    }

    @Override
    public String toString() {
        return this.address;
    }

    @Override
    public int hashCode() {
        return this.address.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Address && this.address.equals(((Address) obj).address);
    }

    private static String calculateChecksum(String data, boolean multisig) {
        Sha256Hash hash = Sha256Hash.of(data.getBytes(StandardCharsets.UTF_8));
        if (multisig) {
            String checksum = Base58.encode(hash.getBytes());
            return checksum.substring(0, 3 + 1);
        } else {
            String checksum = Base58.encode(hash.getBytes());
            return checksum.substring(0, 4 + 1);
        }
    }

    private static String getDataPortionFromAddress(String address) {
        Preconditions.notNull(address, "The address cannot be null");
        Preconditions.argument(address.length() == SIZE,
                "The address " + address + " should be of size " + SIZE);

        return address.substring(0, 24 + 1);
    }

    private static String getChecksumPortionFromAddress(String address, boolean multisig) {
        Preconditions.notNull(address, "The address cannot be null");
        Preconditions.argument(address.length() == SIZE,
                "The address " + address + " should be of size " + SIZE);

        if (multisig) {
            return address.substring(25, 28 + 1);
        } else {
            return address.substring(25);
        }
    }

    public byte[] getPoPBytes() {
        byte[] bytes = Base58.decode(address.substring(1));
        return Arrays.copyOfRange(bytes, 0, 16);
    }

    static public Address fromPoPBytes(ByteBuffer buf) {
        byte[] bytes = new byte[16];
        buf.get(bytes);
        return new Address(STARTING_CHAR + Base58.encode(bytes));
    }


    public String getAddress() {
        return address;
    }

    public String getData() {
        return data;
    }

    public String getChecksum() {
        return checksum;
    }


}
