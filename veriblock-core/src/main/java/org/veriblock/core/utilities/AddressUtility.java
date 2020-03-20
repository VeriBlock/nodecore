// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.utilities;

import org.veriblock.core.AddressConstants;
import org.veriblock.core.SharedConstants;
import org.veriblock.core.bitcoinj.Base58;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veriblock.core.crypto.Crypto;
import org.veriblock.core.types.Pair;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

/**
 * A lightweight, all-static-method class designed to facilitate address validation, public-key-to-address pairing, and
 * signature validation.
 */
public class AddressUtility {
    private static final Logger _logger = LoggerFactory.getLogger(AddressUtility.class);

    private static final int ADDRESS_LENGTH = AddressConstants.ADDRESS_LENGTH;
    private static final int MULTISIG_ADDRESS_LENGTH = AddressConstants.MULTISIG_ADDRESS_LENGTH;

    private static final int ADDRESS_DATA_START = AddressConstants.ADDRESS_DATA_START;
    private static final int ADDRESS_DATA_END = AddressConstants.ADDRESS_DATA_END;
    private static final int MULTISIG_ADDRESS_DATA_START = AddressConstants.MULTISIG_ADDRESS_DATA_START;
    private static final int MULTISIG_ADDRESS_DATA_END = AddressConstants.MULTISIG_ADDRESS_DATA_END;

    private static final int ADDRESS_CHECKSUM_START = AddressConstants.ADDRESS_CHECKSUM_START;
    private static final int ADDRESS_CHECKSUM_END = AddressConstants.ADDRESS_CHECKSUM_END;
    private static final int ADDRESS_CHECKSUM_LENGTH = ADDRESS_CHECKSUM_END - ADDRESS_CHECKSUM_START;
    private static final int MULTISIG_ADDRESS_CHECKSUM_START = AddressConstants.MULTISIG_ADDRESS_CHECKSUM_START;
    private static final int MULTISIG_ADDRESS_CHECKSUM_END = AddressConstants.MULTISIG_ADDRESS_CHECKSUM_END;
    private static final int MULTISIG_ADDRESS_CHECKSUM_LENGTH = MULTISIG_ADDRESS_CHECKSUM_END - MULTISIG_ADDRESS_CHECKSUM_START;

    private static final int MULTISIG_ADDRESS_M_VALUE = AddressConstants.MULTISIG_ADDRESS_M_VALUE;
    private static final int MULTISIG_ADDRESS_N_VALUE = AddressConstants.MULTISIG_ADDRESS_N_VALUE;

    private static final int MULTISIG_ADDRESS_MIN_N_VALUE = AddressConstants.MULTISIG_ADDRESS_MIN_N_VALUE;
    private static final int MULTISIG_ADDRESS_MAX_M_VALUE = AddressConstants.MULTISIG_ADDRESS_MAX_M_VALUE;
    private static final int MULTISIG_ADDRESS_MAX_N_VALUE = AddressConstants.MULTISIG_ADDRESS_MAX_N_VALUE;

    private static final int MULTISIG_ADDRESS_IDENTIFIER_INDEX = AddressConstants.MULTISIG_ADDRESS_IDENTIFIER_INDEX;



    /**
     * Check whether the provided String is a plausible standard address, meaning it is:
     * --> 30 characters long
     * --> Encoded in Base58
     * --> Has a valid checksum
     * --> Starts with the correct starting character
     * <p>
     * There is no way to determine whether an address has a corresponding public/private keypair based on
     * the address alone, it is possible that an address was simply generated in another manner made to fit
     * the requirements of VeriBlock network addresses.
     *
     * @param toTest String to test for being an address
     * @return Whether or not the provided String is a valid address
     */
    public static boolean isValidStandardAddress(String toTest) {
        if (toTest == null)
            return false;

        /* All addresses are exactly 30 characters */
        if (toTest.length() != ADDRESS_LENGTH)
            return false;

        if (toTest.charAt(0) != SharedConstants.STARTING_CHAR)
            return false;

        /* All standard addresses are Base58 */
        if (!Base58.isBase58String(toTest))
            return false;

        /* Take the non-checksum part, recalculate the checksum */
        String checksum = new Crypto().SHA256ReturnBase58(getDataPortionFromAddress(toTest));

        /* If the checksums match, the address is valid. Otherwise, invalid. */
        return chopChecksumStandard(checksum).equals(getChecksumPortionFromAddress(toTest));
    }

    /**
     * Returns the "data" (starting character plus public key hash) section of a standard address
     *
     * @param address Standard address to extract the data section from
     * @return The data portion from the provided address
     */
    private static String getDataPortionFromAddress(String address) {
        if (address == null) {
            throw new IllegalArgumentException("getDataPortionFromAddress cannot be called with a null address!");
        }
        if (address.length() != ADDRESS_LENGTH) {
            throw new IllegalArgumentException("getDataPortionFromAddress cannot be called with an address " +
                    "(" + address + ") which is not exactly " + ADDRESS_LENGTH + " characters long!");
        }
        return address.substring(ADDRESS_DATA_START, ADDRESS_DATA_END + 1);
    }

    /**
     * Returns the checksum portion from the standard address
     *
     * @param address Standard address to extract the checksum section from
     * @return The checksum portion from the provided address
     */
    private static String getChecksumPortionFromAddress(String address) {
        if (address == null) {
            throw new IllegalArgumentException("getChecksumPortionFromAddress cannot be called with a null address!");
        }
        if (address.length() != ADDRESS_LENGTH) {
            throw new IllegalArgumentException("getChecksumPortionFromAddress cannot be called with an address " +
                    "(" + address + ") which is not exactly " + ADDRESS_LENGTH + " characters long!");
        }

        return address.substring(ADDRESS_CHECKSUM_START);
    }

    /**
     * Returns the checksum portion from the multisig address
     *
     * @param address Multisig address to extract the checksum section from
     * @return The checksum portion from the provided multisig address
     */
    private static String getChecksumPortionFromMultisigAddress(String address) {
        if (address == null) {
            throw new IllegalArgumentException("getChecksumPortionFromAddress cannot be called with a null address!");
        }
        if (address.length() != ADDRESS_LENGTH) {
            throw new IllegalArgumentException("getChecksumPortionFromAddress cannot be called with an address " +
                    "(" + address + ") which is not exactly " + ADDRESS_LENGTH + " characters long!");
        }

        return address.substring(MULTISIG_ADDRESS_CHECKSUM_START, MULTISIG_ADDRESS_CHECKSUM_END + 1);
    }

    public static String chopStartingCharacter(String address) {
        if (address == null) {
            throw new IllegalArgumentException("The starting character cannot be chopped off of a null address!");
        }

        if (!isValidStandardOrMultisigAddress(address)) {
            throw new IllegalArgumentException("The provided address (" + address + ") is invalid!");
        }

        return address.substring(1);
    }

    /**
     * Chops a checksum to the appropriate length for use in creating a standard address
     * @param checksum The full checksum to chop
     * @return The chopped checksum appropriate for use in creating a standard address
     */
    private static String chopChecksumStandard(String checksum) {

        if (checksum == null) {
            throw new IllegalArgumentException("getChecksumPortionFromAddress cannot be called with a null checksum!");
        }
        if (checksum.length() < ADDRESS_CHECKSUM_LENGTH) {
            throw new IllegalArgumentException("getChecksumPortionFromAddress cannot be called with an checksum " +
                    "(" + checksum + ") which is not at least " + ADDRESS_CHECKSUM_LENGTH + " characters long!");
        }
        return checksum.substring(0, ADDRESS_CHECKSUM_LENGTH + 1);
    }

    /**
     * Check whether the provided String is a plausible multi-sig address, meaning it is:
     * --> 30 characters long
     * --> Encoded in Base58 (Excluding ending '0')
     * --> Has a valid checksum
     * --> Starts with the correct starting character
     * --> Contains an m value greater than 0 and less than 59
     * --> Contains an n value greater than 1 and less than 59
     */
    public static boolean isValidMultisigAddress(String toTest) {
        if (toTest == null)
            return false;

        /* All addresses are exactly 30 characters */
        if (toTest.length() != MULTISIG_ADDRESS_LENGTH) {
            return false;
        }

        if (toTest.charAt(toTest.length() - 1) != SharedConstants.ENDING_CHAR_MULTISIG) {
            return false;
        }

        /* To make the addresses 'human-readable' we add 1 to the decoded value (1 in Base58 is 0,
         * but we want an address with a '1' in the m slot to represent m=1, for example).
         * this allows addresses with m and n both <= 9 to be easily recognized. Additionally,
         * an m or n value of 0 makes no sense, so this allows multisig to range from 1 to 58,
         * rather than what would have otherwise been 0 to 57. */
        int m = Base58.decode("" + toTest.charAt(MULTISIG_ADDRESS_M_VALUE))[0] + 1;
        int n = Base58.decode("" + toTest.charAt(MULTISIG_ADDRESS_N_VALUE))[0] + 1;

        /* Need at least two addresses for it to be 'multisig' */
        if (n < MULTISIG_ADDRESS_MIN_N_VALUE) {
            return false;
        }

        /* Can't require more signatures than addresses */
        if (m > n) {
            return false;
        }

        /* Impossible */
        if (n > MULTISIG_ADDRESS_MAX_N_VALUE || m > MULTISIG_ADDRESS_MAX_M_VALUE) {
            return false;
        }

        /* Rest of address will be Base58 */
        if (!Base58.isBase58String(toTest.substring(MULTISIG_ADDRESS_DATA_START, MULTISIG_ADDRESS_CHECKSUM_END + 1)))
            return false;

        /* Take the non-checksum part, recalculate the checksum */
        String checksum = new Crypto().SHA256ReturnBase58(
                toTest.substring(MULTISIG_ADDRESS_DATA_START, MULTISIG_ADDRESS_DATA_END + 1));

        /* If the checksums match, the address is valid. Otherwise, invalid. */
        return chopChecksumMultisig(checksum).equals(getChecksumPortionFromMultisigAddress(toTest));
    }

    /**
     +
     * Chops a checksum to the appropriate length for use in creating a multisig address
     * @param checksum The full checksum to chop
     * @return The chopped checksum appropriate for use in creating a multisig address
     */
    private static String chopChecksumMultisig(String checksum) {

        if (checksum == null) {
            throw new IllegalArgumentException("getChecksumPortionFromAddress cannot be called with a null checksum!");
        }
        if (checksum.length() < ADDRESS_CHECKSUM_LENGTH) {
            throw new IllegalArgumentException("getChecksumPortionFromAddress cannot be called with an checksum " +
                    "(" + checksum + ") which is not at least " + ADDRESS_CHECKSUM_LENGTH + " characters long!");
        }

        return checksum.substring(0, MULTISIG_ADDRESS_CHECKSUM_LENGTH + 1);
    }

    /**
     * Calculates a (multisig) address based on the provided membership addresses and desired M-of-N values.
     *
     * The first two of the resultant address (after the prefix) are numbers which dictate how many of the member
     * addresses are required to provide a signature for an outgoing transaction to be considered valid by the VeriBlock protocol
     * (and are encoded in Base58 as M-1 and N-1 (to make up for the exclusion of '0' in Base58)
     *
     * The checksum is reduced from 5 to 4 characters, and a '0' (not part of the base58 alphabet) is added at the end.
     *
     * For example, an address like V23FzSo53hEppWMsiFDwZK8z8Fkkn0 is a 2-of-3 multisig address.
     *
     * @param addresses The standard addresses to create a multisig address from
     * @param m The m value to create the multisig address with
     * @return The generated multisig address
     */
    public static String multisigAddressFromAddressesAndMOfN(String[] addresses, int m) {
        if (addresses == null) {
            _logger.error("Addresses must be provided to create a multisig address from.");
            return null;
        }

        int n = addresses.length;

        if (m > n) {
            _logger.error("A multisig address is unable to exist where (m > n).");
            return null;
        }

        if (m > MULTISIG_ADDRESS_MAX_M_VALUE || n > MULTISIG_ADDRESS_MAX_N_VALUE) {
            _logger.error("A multisig address is unable to exist where m or n are greater than "
                    + MULTISIG_ADDRESS_MAX_M_VALUE + ".");
            return null;
        }

        if (m < AddressConstants.MULTISIG_ADDRESS_MIN_M_VALUE) {
            _logger.error("A multisig address requires an m value greater than 0.");
            return null;
        }

        if (n < MULTISIG_ADDRESS_MIN_N_VALUE) { // A n value of 1 would function the same as a regular address (1-of-1 "multi"-sig)
            _logger.error("A multisig address requires an n value greater than 1.");
            return null;
        }

        StringBuilder allAddresses = new StringBuilder();
        for (int addressIndex = 0; addressIndex < addresses.length; addressIndex++) {
            String address = addresses[addressIndex];
            if (!isValidStandardAddress(address)) {
                _logger.error("The address " + address + " is not a valid non-multi-sig address.");
                return null;
            }
            if (isValidMultisigAddress(address)) {
                _logger.error("Multisig addresses cannot be nested in the current VeriBlock protocol.");
                return null;
            }

            allAddresses.append(address);
        }

        Crypto crypto = new Crypto();

        /* Calculate the SHA-256 of the public key, encode as base-58, take the first 20 characters,
         * and prepend M and N values encoded in Base58 */
        String multisigAddress = SharedConstants.STARTING_CHAR + Base58.encode(new byte[]{(byte)(m - 1)}) +
                Base58.encode(new byte[]{(byte)(n - 1)}) +
                crypto.SHA256ReturnBase58(allAddresses.toString()).substring(0, AddressConstants.MULTISIG_ADDRESS_SIGNING_GROUP_LENGTH + 1);

        System.out.println("Checksum created based on " + multisigAddress);

        /* Append a four-character base-58 checksum and multisig ending char [ensures this doesn't validate according to standard address rules] */
        multisigAddress += chopChecksumMultisig(crypto.SHA256ReturnBase58(multisigAddress)) + SharedConstants.ENDING_CHAR_MULTISIG;

        return multisigAddress;
    }


    /**
     * Determines whether the provided String is valid standard OR multisig address
     * @param toTest The address to test
     * @return Whether or not the provided String is a valid standard or multisig address
     */
    public static boolean isValidStandardOrMultisigAddress(String toTest) {
        if (toTest == null) {
            throw new IllegalArgumentException("isValidStandardOrMultisigAddress cannot be called with a null address!");
        }
        return isValidStandardAddress(toTest) || isValidMultisigAddress(toTest);
    }

    /**
     * Removes the ending character of a multisig address
     * @param multisigAddress Multisig address to remove the ending character of
     * @return The multisig address with the ending identification character removed
     */
    private static String getMultisigWithoutEndingCharacter(String multisigAddress) {
        if (multisigAddress == null) {
            throw new IllegalArgumentException("getMultisigWithoutEndingCharacter cannot be called with a " +
                    "null multisigAddress!");
        }

        if (multisigAddress.length() != MULTISIG_ADDRESS_LENGTH) {
            throw new IllegalArgumentException("getMultisigWithoutEndingCharacter cannot be called with a " +
                    "multisig address (" + multisigAddress + ") with a length that isn't " +
                    MULTISIG_ADDRESS_LENGTH + "!");
        }

        return multisigAddress.substring(MULTISIG_ADDRESS_DATA_START, MULTISIG_ADDRESS_IDENTIFIER_INDEX);
    }

    private static final KeyFactory keyFactory;

    static {
        KeyFactory attempt = null;
        try {
            attempt = KeyFactory.getInstance("EC");
        } catch (Exception e) { System.exit(-1); }
        keyFactory = attempt;
    }

    /**
     * Determines whether the provided signature is valid for the provided message and public key.
     *
     * In general, this is used only for checking transaction signatures, so "messageBytes" will be a 32-byte TxID.
     * However, this method allows arbitrary-size messageBytes input to allow other messages to be signed by
     * addresses.
     *
     * @param messageBytes   The message bytes, as were originally signed
     * @param signatureBytes The bytes of the signature
     * @param publicKeyBytes The bytes of the public key
     * @return Whether or not the signature could be validated
     */
    public static boolean isSignatureValid(byte[] messageBytes, byte[] signatureBytes, byte[] publicKeyBytes, String inputAddress) {
        if (messageBytes == null) {
            throw new IllegalArgumentException("isSignatureValid cannot be called with a null messageBytes byte array!");
        }

        if (signatureBytes == null) {
            throw new IllegalArgumentException("isSignatureValid cannot be called with a null signatureBytes byte array!");
        }

        if (publicKeyBytes == null) {
            throw new IllegalArgumentException("isSignatureValid cannot be called with a null publicKeyBytes byte array!");
        }

        try {
            /* Create a key factory for ECDSA, and load the provided public key using X509 specifications */
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);

            /* Create a public key object from the public key bytes provided */
            PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

            /* Create a signature verifier, initialize it with the public key, and update it with the message */
            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initVerify(publicKey);
            signature.update(messageBytes);

            /* Validate the signature in the context of the message and the public key
             * and also ensure that it is signed by the correct address */
            return signature.verify(signatureBytes) && addressFromPublicKey(publicKey).equals(inputAddress);
        } catch (NoSuchAlgorithmException e) {
            _logger.error("Unable to create an elliptical curve key factory!", e);
        } catch (InvalidKeySpecException e) {
            _logger.error("Unable to use the X509 key specification!", e);
        } catch (InvalidKeyException e) {
            _logger.error("Unable to initialize the signature for verification!", e);
        } catch (SignatureException e) {
            _logger.error("Unable to update the message into the signature verifier!", e);
        }

        /* Something went wrong, return false */
        return false;
    }

    /**
     * Calculates an address based on the provided public key. An address is made by taking the SHA-256 of the public key,
     * encoding it as base-58, taking the first 24 characters, prepending a network-specific character 'V' (for VeriBlock),
     * and then appending a five-character SHA-256 checksum, also encoded in base-58, of the previous 25-total characters.
     *
     * @param pubKey The public key to create an address out of
     * @return The address formed by processing the provided public key
     */
    public static String addressFromPublicKey(byte[] pubKey) {
        Crypto crypto = new Crypto();

        /* Calculate the SHA-256 of the public key, encode as base-58, take the first 24 characters, prepend a 'V' for VeriBlock */
        String address = SharedConstants.STARTING_CHAR + crypto.SHA256ReturnBase58(pubKey).substring(ADDRESS_DATA_START, ADDRESS_DATA_END);

        /* Append a five-character base-58 checksum */
        address += chopChecksumStandard(crypto.SHA256ReturnBase58(address));

        return address;
    }

    /**
     * Returns the M and N values associated in a respective Pair<Integer, Integer> from a given multisigAddress.
     * If the supplied String is not a valid multisig address, then return null.
     * @param multisigAddress The multisig address to parse M and N from
     * @return A pair containing M and N respectively
     */
    public static Pair<Integer, Integer> multisigAddressGetMandN(String multisigAddress) {
        return isValidMultisigAddress(multisigAddress) ?
                new Pair<>(
                        1 + (int)Base58.decode("" +multisigAddress.charAt(MULTISIG_ADDRESS_M_VALUE))[0],
                        1 + (int)Base58.decode("" +multisigAddress.charAt(MULTISIG_ADDRESS_N_VALUE))[0]) :
                null;
    }

    /**
     * Calculates an address based on the provided public key. An address is made by taking the SHA-256 of the public key,
     * encoding it as base-58, taking the first 24 characters, prepending a network-specific character 'V' (for VeriBlock),
     * and then appending a five-character SHA-256 checksum, also encoded in base-58, of the previous 25-total characters.
     *
     * @param pubKey The public key to create an address out of
     * @return The address formed by processing the provided public key
     */
    public static String addressFromPublicKey(PublicKey pubKey) {
        return addressFromPublicKey(pubKey.getEncoded());
    }

    public static byte[] toPoPAddressFormat(String address) {
        return Base58.decode(AddressUtility.chopStartingCharacter(address));
    }
}
