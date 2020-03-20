// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.api.ucp.arguments;

import nodecore.api.ucp.utilities.AsciiUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veriblock.core.utilities.AddressUtility;
import org.veriblock.core.utilities.Utility;

/**
 * A UCPArgument represents an argument which was parsed from an argument which came "over the wire" or which was created
 * from the data it represents and could be serialized to send over the wire as an argument.
 */
public abstract class UCPArgument {
    private static final Logger _logger = LoggerFactory.getLogger(UCPArgument.class);

    /* Each UCPType is defined with a variable type and (optionally) preliminary filter information.
     * Some more advanced UCPTypes (like MERKLE_PATH) have additional initialization-level behavior beyond
     * The preliminary filtering performed by UCPType.preliminaryValidation(String toValidate).
     */
    public enum UCPType {
        /* Note: for a validation string, the first (required) part is the raw type (string, long, int), and the
         * second and beyond (optional) part(s) are filters (like "address" meaning it meets the address validation
         * requirements). Sections are separated by the bar |. The raw type doesn't account for more advanced
         * rules (like arrays of a type, etc.), leaving those additional validations up to the specific constructor
         * of the UCPArgument which implements functionality for the corresponding UCPType.
         */
        ADDRESS("string|address", UCPArgumentAddress.class),
        BALANCE("long|zero_or_greater", UCPArgumentBalance.class),
        BITFLAG("string|binary_string", UCPArgumentBitflag.class),
        BLOCK_HASH("string|hexadecimal|length:48", UCPArgumentBlockHash.class), // 24 bytes
        BLOCK_HEADER_LIST("string", UCPArgumentBlockHeaderList.class),
        BLOCK_INDEX("int|zero_or_greater", UCPArgumentBlockIndex.class),
        BLOCK_VERSION("int", UCPArgumentBlockVersion.class),
        BLOOM_FILTER("string|hexadecimal", UCPArgumentBloomFilter.class),
        DIFFICULTY("int|zero_or_greater", UCPArgumentDifficulty.class),
        EXTRA_NONCE("long", UCPArgumentExtraNonce.class),
        FREQUENCY_MS("int|zero_or_greater", UCPArgumentFrequencyMS.class),
        JOB_ID("int|zero_or_greater", UCPArgumentJobID.class),
        LEDGER_HASH("string|hexadecimal|length:48", UCPArgumentLedgerHash.class),
        LEDGER_MERKLE_PATH("string|printable", UCPArgumentLedgerMerklePath.class),
        MESSAGE("string", UCPArgumentMessage.class),
        MERKLE_PATH("string|printable", UCPArgumentMerklePath.class),
        TOP_LEVEL_MERKLE_ROOT("string|hexadecimal|length:48", UCPArgumentTopLevelMerkleRoot.class), // 24 bytes
        INTERMEDIATE_LEVEL_MERKLE_ROOT("string|hexadecimal|length:64", UCPArgumentIntermediateLevelMerkleRoot.class), // 32 bytes
        NONCE("int", UCPArgumentNonce.class),
        PASSWORD("string", UCPArgumentPassword.class),
        REQUEST_ID("int|zero_or_greater", UCPArgumentRequestID.class),
        SIGNATURE_INDEX("long|zero_or_greater", UCPArgumentSignatureIndex.class),
        TARGET("string|hexadecimal|length:48", UCPArgumentTarget.class), // 24 bytes
        TIMESTAMP("int", UCPArgumentTimestamp.class),
        TRANSACTIONS_WITH_CONTEXT("string", UCPArgumentTransactionsWithContext.class),
        TRANSACTION_DATA("string|hexadecimal", UCPArgumentTransactionData.class),
        TRANSACTION_DELTA_LIST("string", UCPArgumentTransactionDeltaList.class),
        TRANSACTION_ID("string|hexadecimal|length:64", UCPArgumentTransactionID.class), // 32 bytes
        TRANSACTION_LIST("string", UCPArgumentTransactionList.class),
        USERNAME("string", UCPArgumentUsername.class),
        POP_DATASTORE_HASH("string|hexadecimal|length:64", UCPArgumentPoPDatastoreHash.class), // 32 bytes
        MINER_COMMENT("string", UCPArgumentMinerComment.class),
        INTERMEDIATE_METAPACKAGE_HASH("string|hexadecimal|length:64", UCPArgumentIntermediateMetapackageHash.class); // 32 bytes

        private final String[] constraints;
        private final String pattern;
        private final Class<?> internalType;

        UCPType(String pattern, Class<? extends UCPArgument> implementation) {
            if (pattern == null) {
                throw new IllegalArgumentException("A UCPType enum cannot be instantiated with a null pattern!");
            }

            if (implementation == null) {
                throw new IllegalArgumentException("A UCPType enum cannot be instantiated with a null implementing class!");
            }

            String[] patternParts = pattern.split("\\|");

            if (patternParts.length == 0) {
                throw new IllegalArgumentException("A UCPType enum cannot be instantiated with an empty pattern (\"" + pattern + "\")!");
            }

            String type = patternParts[0];

            switch (type.toLowerCase()) {
                case "string": internalType = String.class; break;
                case "int": internalType = Integer.class; break;
                case "long": internalType = Long.class; break;
                default: throw new IllegalArgumentException("A UCPType enum cannot be instantiated with an unknown type: " + type + "!");
            }

            if (patternParts.length == 1) {
                constraints = null;
            } else {
                constraints = new String[patternParts.length - 1];
                System.arraycopy(patternParts, 1, constraints, 0, constraints.length);
            }

            this.pattern = pattern;

            this.implementingClass = implementation;
        }

        public Class<? extends UCPArgument> getArgumentImplementation() {
            return implementingClass;
        }

        /**
         * Returns the pattern String which dictates the preliminary validation requirements for the particular type.
         *
         * @return The pattern String for the type
         */
        protected String getPreliminaryValidationPattern() {
            return this.pattern;
        }

        /**
         * Performs basic validation on a possible argument against the known basic restraints (type, simple filtering).
         *
         * Note that it is possible for an argument to pass preliminaryValidation but fail construction as a specific UCPArgument,
         * specifically those which represent more advanced data types (ex: merkle paths, delta lists, etc.)
         *
         * @param toValidate String to perform preliminary validation against
         * @return Whether the provided String adheres to the preliminary validation rules
         */
        protected boolean preliminaryValidation(String toValidate) {
            if (toValidate == null) {
                throw new IllegalArgumentException("preliminaryValidation cannot be called with a null validation String!");
            }

            if (internalType == Integer.class) {
                if (!Utility.isInteger(toValidate)) {
                    return false;
                }
            } else if (internalType == Long.class) {
                if (!Utility.isLong(toValidate)) {
                    return false;
                }
            } else if (internalType == String.class) {
                // no further validation for the raw type
            } else {
                _logger.error("A UCPType enum exists with an unknown internal type!");
                return false;
            }

            if (constraints == null) {
                return true; // No constraints, passed type validation!
            }

            for (int i = 0; i < constraints.length; i++) {
                String filter = constraints[i];
                // First route by type, then route by filter as it relates to the type
                if (internalType == Integer.class) {
                    int intVal = Integer.parseInt(toValidate);
                    switch (filter) {
                        case "zero_or_greater":
                            if (intVal < 0) {
                                return false;
                            }
                            break;
                        case "zero_or_lower":
                            if (intVal > 0) {
                                return false;
                            }
                            break;
                        case "positive":
                            if (intVal <= 0) {
                                return false;
                            }
                            break;
                        case "negative":
                            if (intVal >= 0) {
                                return false;
                            }
                            break;
                        default:
                            return false; // The provided filter is not valid for ints
                    }
                }
                else if (internalType == Long.class) {
                    long longVal = Long.parseLong(toValidate);

                    switch (filter) {
                        case "zero_or_greater":
                            if (longVal < 0) {
                                return false;
                            }
                            break;
                        case "zero_or_lower":
                            if (longVal > 0) {
                                return false;
                            }
                            break;
                        case "positive":
                            if (longVal <= 0) {
                                return false;
                            }
                            break;
                        case "negative":
                            if (longVal >= 0) {
                                return false;
                            }
                            break;
                        default:
                            return false; // The provided filter is not valid for longs
                    }
                }
                else if (internalType == String.class) {
                    String stringVal = toValidate;

                    if (filter.startsWith("length")) { // has argument, so special pre-switch validation
                        int length = Integer.parseInt(filter.split(":")[1]);

                        if (stringVal.length() != length) {
                            return false;
                        }
                    }
                    else {
                        switch (filter) {
                            case "address":
                                if (!AddressUtility.isValidStandardOrMultisigAddress(stringVal)) {
                                    return false;
                                }
                                break;
                            case "hexadecimal":
                                if (!Utility.isHex(stringVal)) {
                                    return false;
                                }
                                break;
                            case "printable":
                                if (!AsciiUtility.isPrintableASCIIAndNotWhitespace(stringVal)) {
                                    return false;
                                }
                                break;
                            case "binary_string":
                                if (!Utility.isBitString(stringVal)) {
                                    return false;
                                }
                                break;
                            default:
                                return false; // The provided filter is not valid for strings
                        }
                    }
                }
                else {
                    return false;
                }
            }

            return true; // Nothing above tripped up the validation checks
        }

        private final Class<? extends UCPArgument> implementingClass;
        public Class<? extends UCPArgument> getImplementingClass() {
            return implementingClass;
        };
        public Class<? extends Object> getInternalType() { return internalType; };
    }

    public abstract String getSerialized(); // Original data as it was passed in, or serialized data as it would be used in building a command
    public abstract UCPType getType(); // The type which the UCPArgument implementation represents
    public abstract String toString(); // Force implementation of a toString equivalent rather than leaving default
}
