package org.veriblock.extensions.stratumapi.arguments;

import nodecore.api.ucp.utilities.AsciiUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veriblock.core.utilities.AddressUtility;
import org.veriblock.core.utilities.Utility;

/**
 * A StratumArgument represents an argument which was parsed from an argument which came "over the wire" or which was created
 * from the data it represents and could be serialized to send over the wire as an argument.
 */
public abstract class StratumArgument {
    private static final Logger _logger = LoggerFactory.getLogger(StratumArgument.class);

    /* Each StratumType is defined with a variable type and (optionally) preliminary filter information.
     * Some more advanced StratumTypes can have additional initialization-level behavior beyond
     * The preliminary filtering performed by StratumType.preliminaryValidation(String toValidate).
     */
    public enum StratumType {
        /* Note: for a validation string, the first (required) part is the raw type (string, long, int), and the
         * second and beyond (optional) part(s) are filters (like "address" meaning it meets the address validation
         * requirements). Sections are separated by the bar |. The raw type doesn't account for more advanced
         * rules (like arrays of a type, etc.), leaving those additional validations up to the specific constructor
         * of the StratumArgument which implements functionality for the corresponding StratumType.
         */
        ADDRESS("string|address", StratumArgumentAddress.class),
        AGENT("string", StratumArgumentAgent.class),
        HOST("string",StratumArgumentHost.class),
        PORT("int|zero_or_greater", StratumArgumentPort.class), // Integer rather than short because shorts are signed, and ports need to go up to 65535
        PROTOCOL("string", StratumArgumentProtocol.class),
        ID("int|zero_or_greater", StratumArgumentId.class),
        JOB_ID("int|zero_or_greater", StratumArgumentJobId.class),
        SUBSCRIPTION_TRIPLE("string", StratumArgumentSubscriptionTriple.class),
        ERROR("string", StratumArgumentError.class),
        SYNTHETIC_EXTRA_NONCE("string|hexadecimal|length:8", StratumArgumentSyntheticExtraNonce.class), // 4 bytes TODO FIX
        SEED_HASH("string|hexadecimal|length:64", StratumArgumentSeedHash.class), // 32 bytes
        HEADER_HASH("string|hexadecimal|length:64", StratumArgumentHeaderHash.class), // 32 bytes
        BOOLEAN("boolean", StratumArgumentBoolean.class),
        USERNAME("string", StratumArgumentUsername.class),
        PASSWORD("string", StratumArgumentPassword.class),
        DIFFICULTY("double|positive", StratumArgumentDifficulty.class),
        NONCE("string|hexadecimal", StratumArgumentNonce.class),
        BLOCK_HEIGHT("int|zero_or_greater", StratumArgumentBlockHeight.class);

        private final String[] constraints;
        private final String pattern;
        private final Class<?> internalType;

        StratumType(String pattern, Class<? extends StratumArgument> implementation) {
            if (pattern == null) {
                throw new IllegalArgumentException("A StratumType enum cannot be instantiated with a null pattern!");
            }

            if (implementation == null) {
                throw new IllegalArgumentException("A StratumType enum cannot be instantiated with a null implementing class!");
            }

            String[] patternParts = pattern.split("\\|");

            if (patternParts.length == 0) {
                throw new IllegalArgumentException("A StratumType enum cannot be instantiated with an empty pattern (\"" + pattern + "\")!");
            }

            String type = patternParts[0];

            switch (type.toLowerCase()) {
                case "string":
                    internalType = String.class;
                    break;
                case "short":
                    internalType = Short.class;
                    break;
                case "int":
                    internalType = Integer.class;
                    break;
                case "long":
                    internalType = Long.class;
                    break;
                case "double":
                    internalType = Double.class;
                    break;
                case "boolean":
                    internalType = Boolean.class;
                    break;
                default:
                    throw new IllegalArgumentException("A StratumType enum cannot be instantiated with an unknown type: " + type + "!");
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

        public Class<? extends StratumArgument> getArgumentImplementation() {
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
         * <p>
         * Note that it is possible for an argument to pass preliminaryValidation but fail construction as a specific StratumArgument,
         * specifically those which represent more advanced data types.
         *
         * @param toValidate String to perform preliminary validation against
         * @return Whether the provided String adheres to the preliminary validation rules
         */
        protected boolean preliminaryValidation(String toValidate) {
            if (toValidate == null) {
                throw new IllegalArgumentException("preliminaryValidation cannot be called with a null validation String!");
            }
            if (internalType == Short.class) {
                if (!Utility.isShort(toValidate)) {
                    return false;
                }
            } else if (internalType == Integer.class) {
                if (!Utility.isInteger(toValidate)) {
                    return false;
                }
            } else if (internalType == Long.class) {
                if (!Utility.isLong(toValidate)) {
                    return false;
                }
            } else if (internalType == Double.class) {
                if (!Utility.isDouble(toValidate)) {
                    return false;
                }
            } else if (internalType == Boolean.class) {
                try {
                    Boolean.parseBoolean(toValidate);
                } catch (Exception e) {
                    return false;
                }
            } else if (internalType == String.class) {
                // no further validation for the raw type
            } else {
                _logger.error("A StratumType enum exists with an unknown internal type (" + internalType + ")!");
                return false;
            }

            if (constraints == null) {
                return true; // No constraints, passed type validation!
            }

            for (int i = 0; i < constraints.length; i++) {
                String filter = constraints[i];
                // First route by type, then route by filter as it relates to the type
                if (internalType == Short.class) {
                    short shortVal = Short.parseShort(toValidate);
                    switch (filter) {
                        case "zero_or_greater":
                            if (shortVal < 0) {
                                return false;
                            }
                            break;
                        case "zero_or_lower":
                            if (shortVal > 0) {
                                return false;
                            }
                            break;
                        case "positive":
                            if (shortVal <= 0) {
                                return false;
                            }
                            break;
                        case "negative":
                            if (shortVal >= 0) {
                                return false;
                            }
                            break;
                        default:
                            return false; // The provided filter is not valid for shorts
                    }
                } else if (internalType == Integer.class) {
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
                } else if (internalType == Long.class) {
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
                } else if (internalType == Double.class) {
                    double doubleVal = Double.parseDouble(toValidate);

                    switch (filter) {
                        case "zero_or_greater":
                            if (doubleVal < 0) {
                                return false;
                            }
                            break;
                        case "zero_or_lower":
                            if (doubleVal > 0) {
                                return false;
                            }
                            break;
                        case "positive":
                            if (doubleVal <= 0) {
                                return false;
                            }
                            break;
                        case "negative":
                            if (doubleVal >= 0) {
                                return false;
                            }
                            break;
                        default:
                            return false; // The provided filter is not valid for doubles
                    }
                } else if (internalType == String.class) {
                    String stringVal = toValidate;

                    if (filter.startsWith("length")) { // has argument, so special pre-switch validation
                        int length = Integer.parseInt(filter.split(":")[1]);

                        if (stringVal.length() != length) {
                            return false;
                        }
                    } else {
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
                } else {
                    return false;
                }
            }

            return true; // Nothing above tripped up the validation checks
        }

        private final Class<? extends StratumArgument> implementingClass;

        public Class<? extends StratumArgument> getImplementingClass() {
            return implementingClass;
        }

        public Class<? extends Object> getInternalType() {
            return internalType;
        }
    }

    public abstract String getSerialized(); // Original data as it was passed in, or serialized data as it would be used in building a command

    public abstract StratumType getType(); // The type which the StratumArgument implementation represents

    public abstract String toString(); // Force implementation of a toString equivalent rather than leaving default
}
