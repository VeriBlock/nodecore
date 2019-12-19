// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.altchain;

import org.veriblock.core.TransactionConstants;
import org.veriblock.core.utilities.Utility;

import java.io.ByteArrayInputStream;

/**
 * An Altchain PoP Endorsement is an endorsement of an altchain's state which an altchain PoP miner has published to
 * VeriBlock.
 *
 * These are created from specially-formatted data sections in standard transactions, and pair a network identifier
 * (each altchain adopts a different network ID) with an endorsed block. Because altchains can have a variety of
 * formats, the endorsed block data is free-form: PoW blockchains will likely publish just a standard block header, but
 * other consensus mechanisms like PoS may require additional data be published (such as that which proves that a PoS
 * block was created by the consumption of valid coinage).
 *
 * Nothing prevents PoP miners from using arbitrary identifiers, nor does anything prevent two altchains from using the
 * same identifier. The identifier is simply a convenience for easy parsing, and the endorsement data contains enough
 * information to perform preliminary validation (such as hashing the block header and seeing if it is the appropriate
 * difficulty).
 */
public class AltchainPoPEndorsement {
    private final long identifier;
    private final byte[] header;
    private final byte[] contextInfo;
    private final byte[] payoutInfo;
    private final byte[] rawData;

    public AltchainPoPEndorsement(byte[] rawData) {
        if (rawData == null) {
            throw new IllegalArgumentException("An AltchainPoPEndorsement cannot be created with a null data array!");
        }

        if (rawData.length == 0) {
            throw new IllegalArgumentException("An AltchainPoPEndorsement cannot be created with an empty data array!");
        }

        if (rawData.length > TransactionConstants.MAX_TRANSACTION_DATA_SIZE_BYTES) {
            throw new IllegalArgumentException("An AltchainPoPEndorsement cannot be created with raw data that is longer than " + TransactionConstants.MAX_TRANSACTION_DATA_SIZE_BYTES);
        }

        ByteArrayInputStream endorsementDataStream = new ByteArrayInputStream(rawData);

        byte idSize = (byte)endorsementDataStream.read();

        if (idSize > AltchainConstants.MAX_ALTCHAIN_IDENTIFIER_LENGTH) {
            throw new IllegalArgumentException("An AltchainPoPEndorsement cannot be created with an identifier that is" +
                    " more than 8 bytes long (" + idSize + ")!");
        }

        if (idSize < 1) {
            throw new IllegalArgumentException("An AltchainPoPEndorsement cannot be created with an identifier that is" +
                    " zero or negative (" + idSize + ")!");
        }

        if (endorsementDataStream.available() < idSize) {
            throw new IllegalArgumentException("An AltchainPoPEndorsement was attempted to be created which claimed to have " +
                    idSize + " bytes of ID data, but only had " + (endorsementDataStream.available()) + " bytes left!");
        }

        try {
            byte[] identifierBytes = new byte[idSize];
            int bytesRead = endorsementDataStream.read(identifierBytes);

            if (bytesRead != idSize) {
                throw new IllegalArgumentException("While creating an AltchainPoPEndorsement, the identifier bytes were unable " +
                "to be read in full from the endorsement: " + Utility.bytesToHex(rawData));
            }

            long identifier = 0L;

            for (int i = 0; i < idSize; i++) {
                identifier <<= 8;
                identifier |= 0xFF & identifierBytes[i];
            }

            this.identifier = identifier;


            // Read the header data (size of header size, size of header, header)
            if (endorsementDataStream.available() == 0) {
                throw new IllegalArgumentException("An AltchainPoPEndorsement was attempted to be created with no " +
                "header, context, or payout sections!");
            }

            byte lengthOfHeaderSize = (byte)endorsementDataStream.read();

            if (lengthOfHeaderSize > AltchainConstants.MAX_ALTCHAIN_ENDORSEMENT_INFO_SECTION_LENGTH) {
                throw new IllegalArgumentException("An AltchainPoPEndorsement cannot be created with a header size length that is" +
                        " more than 8 bytes long (" + idSize + ")!");
            }

            if (lengthOfHeaderSize <= 0) {
                throw new IllegalArgumentException("An AltchainPoPEndorsement cannot be created with a header length that is" +
                        " zero or negative (" + lengthOfHeaderSize + ")!");
            }

            byte[] headerSizeBytes = new byte[lengthOfHeaderSize];
            bytesRead = endorsementDataStream.read(headerSizeBytes);

            if (bytesRead < headerSizeBytes.length) {
                throw new IllegalArgumentException("While creating an AltchainPoPEndorsement, the header size was unable " +
                        "to be read in full from the endorsement: " + Utility.bytesToHex(rawData));
            }

            int headerLength = 0;
            for (int i = 0; i < headerSizeBytes.length; i++) {
                headerLength <<= 8;
                headerLength |= 0xFF & headerSizeBytes[i];
            }

            if (headerLength < 0) {
                throw new IllegalArgumentException("An AltchainPoPEndorsement was attempted to be created which contained " +
                "a header length that was negative (" + headerLength + ")!");
            }

            byte[] header = new byte[headerLength];
            bytesRead = endorsementDataStream.read(header);

            if (bytesRead < header.length) {
                throw new IllegalArgumentException("While creating an AltchainPoPEndorsement, the header bytes were unable " +
                        "to be read in full from the endorsement: " + Utility.bytesToHex(rawData));
            }

            this.header = header;



            // Read the context info data (size of context info size, size of context info, context info)
            if (endorsementDataStream.available() == 0) {
                throw new IllegalArgumentException("An AltchainPoPEndorsement was attempted to be created with no " +
                        "context info, or payout sections!");
            }

            byte lengthOfContextInfoSize = (byte)endorsementDataStream.read();

            if (lengthOfContextInfoSize > AltchainConstants.MAX_ALTCHAIN_ENDORSEMENT_INFO_SECTION_LENGTH) {
                throw new IllegalArgumentException("An AltchainPoPEndorsement cannot be created with a context info size that is" +
                        " more than " + AltchainConstants.MAX_ALTCHAIN_ENDORSEMENT_INFO_SECTION_LENGTH + " bytes long (" + idSize + ")!");
            }

            if (lengthOfContextInfoSize < 0) {
                throw new IllegalArgumentException("An AltchainPoPEndorsement cannot be created with a context info size length that is" +
                        " negative (" + lengthOfContextInfoSize + ")!");
            }

            // Context info can be omitted, indicated by a single 0 byte (0 length size, which means 0 length)
            if (lengthOfContextInfoSize == 0) {
                this.contextInfo = new byte[0];
            } else {
                byte[] contextInfoSizeBytes = new byte[lengthOfContextInfoSize];

                if (lengthOfContextInfoSize < 0) {
                    throw new IllegalArgumentException("An AltchainPoPEndorsement was attempted to be created which contained " +
                            "a context info length that was negative (" + lengthOfContextInfoSize + ")!");
                }

                bytesRead = endorsementDataStream.read(contextInfoSizeBytes);

                if (bytesRead < contextInfoSizeBytes.length) {
                    throw new IllegalArgumentException("While creating an AltchainPoPEndorsement, the context size was unable " +
                            "to be read in full from the endorsement: " + Utility.bytesToHex(rawData));
                }

                int contextInfoLength = 0;
                for (int i = 0; i < contextInfoSizeBytes.length; i++) {
                    contextInfoLength <<= 8;
                    contextInfoLength |= 0xFF & contextInfoSizeBytes[i];
                }

                byte[] contextInfo = new byte[contextInfoLength];
                bytesRead = endorsementDataStream.read(contextInfo);

                if (bytesRead < contextInfo.length) {
                    throw new IllegalArgumentException("While creating an AltchainPoPEndorsement, the context bytes were unable " +
                            "to be read in full from the endorsement: " + Utility.bytesToHex(rawData));
                }

                this.contextInfo = contextInfo;
            }



            // Read the context info data (size of context info size, size of context info, context info)
            if (endorsementDataStream.available() == 0) {
                throw new IllegalArgumentException("An AltchainPoPEndorsement was attempted to be created with no " +
                        "or payout section!");
            }

            byte lengthOfPayoutInfoSize = (byte)endorsementDataStream.read();

            if (lengthOfPayoutInfoSize > AltchainConstants.MAX_ALTCHAIN_ENDORSEMENT_INFO_SECTION_LENGTH) {
                throw new IllegalArgumentException("An AltchainPoPEndorsement cannot be created with a payout info size that is" +
                        " more than " + AltchainConstants.MAX_ALTCHAIN_ENDORSEMENT_INFO_SECTION_LENGTH + " bytes long (" + idSize + ")!");
            }

            if (lengthOfPayoutInfoSize < 0) {
                throw new IllegalArgumentException("An AltchainPoPEndorsement cannot be created with a payout info size length that is" +
                        " negative (" + lengthOfPayoutInfoSize + ")!");
            }

            // Payout info can be omitted, indicated by a single 0 byte (0 length size, which means 0 length)
            if (lengthOfPayoutInfoSize == 0) {
                this.payoutInfo = new byte[0];
            } else {
                byte[] payoutInfoSizeBytes = new byte[lengthOfPayoutInfoSize];

                if (lengthOfPayoutInfoSize < 0) {
                    throw new IllegalArgumentException("An AltchainPoPEndorsement was attempted to be created which contained " +
                            "a payout info length that was negative (" + lengthOfPayoutInfoSize + ")!");
                }

                bytesRead = endorsementDataStream.read(payoutInfoSizeBytes);

                if (bytesRead < payoutInfoSizeBytes.length) {
                    throw new IllegalArgumentException("While creating an AltchainPoPEndorsement, the payout size was unable " +
                            "to be read in full from the endorsement: " + Utility.bytesToHex(rawData));
                }

                int payoutInfoLength = 0;
                for (int i = 0; i < payoutInfoSizeBytes.length; i++) {
                    payoutInfoLength <<= 8;
                    payoutInfoLength |= 0xFF & payoutInfoSizeBytes[i];
                }

                byte[] payoutInfo = new byte[payoutInfoLength];
                bytesRead = endorsementDataStream.read(payoutInfo);

                if (bytesRead < payoutInfo.length) {
                    throw new IllegalArgumentException("While creating an AltchainPoPEndorsement, the payout bytes were unable " +
                            "to be read in full from the endorsement: " + Utility.bytesToHex(rawData));
                }

                this.payoutInfo = payoutInfo;
            }

            if (endorsementDataStream.available() > 0) {
                byte[] extraData = new byte[endorsementDataStream.available()];
                endorsementDataStream.read(extraData);
                throw new IllegalArgumentException("While creating an AltchainPoPEndorsement, unexpected additional data was " +
                "present after the specified header, context info, and payout info sections! Extra available bytes: " +
                Utility.bytesToHex(extraData) + "!");
            }

            this.rawData = rawData;
        } catch (Exception e) {
            throw new IllegalArgumentException("An exception was encountered while attempting to parse the endorsement " + Utility.bytesToHex(rawData), e);
        }
    }

    public long getIdentifier() {
        return identifier;
    }

    public byte[] getHeader() {
        byte[] endorsementDataCopy = new byte[header.length];
        System.arraycopy(header, 0, endorsementDataCopy, 0, header.length);
        return endorsementDataCopy;
    }

    public byte[] getContextInfo() {
        byte[] contextInfoCopy = new byte[contextInfo.length];
        System.arraycopy(contextInfo, 0, contextInfoCopy, 0, contextInfo.length);
        return contextInfoCopy;
    }

    public byte[] getPayoutInfo() {
        byte[] payoutInfoCopy = new byte[payoutInfo.length];
        System.arraycopy(payoutInfo, 0, payoutInfoCopy, 0, payoutInfo.length);
        return payoutInfoCopy;
    }

    public byte[] getRawData() {
        byte[] rawDataCopy = new byte[rawData.length];
        System.arraycopy(rawData, 0, rawDataCopy, 0, rawData.length);
        return rawDataCopy;
    }

    public static boolean isValidEndorsement(byte[] rawData) {
        if (rawData == null) {
            throw new IllegalArgumentException("isValidEndorsement cannot be called with a null raw data byte array!");
        }

        try {
            new AltchainPoPEndorsement(rawData);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
