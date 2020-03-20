// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.er;

package org.veriblock.extensions.ledger;

import javax.annotation.Nullable;

public class LedgerProofUtility {

    /**
     * Recompose the (partial) address from the routes contained in the provided LedgerProofLayers.
     * @param verticalProofLayers Ordered array of layers, with the lowest layer at the beginning and highest at the end.
     * @return The recomposed address built from the combined routes of the provided layers
     */
    public static String getAddressFromLedgerLayers(LedgerProofNode[] verticalProofLayers) {
        if (verticalProofLayers == null || verticalProofLayers.length == 0) {
            throw new IllegalArgumentException("getAddressFromLedgerLayers cannot be called with an empty or " +
                    "null array of vertical ledger proof layers!");
        }

        for (int i = 0; i < verticalProofLayers.length; i++) {
            if (verticalProofLayers[i] == null) {
                throw new IllegalArgumentException("getAddressFromLedgerLayers cannot be called with a null " +
                        "vertical ledger proof layer element (index " + i + ")!");
            }
        }

        StringBuilder recomposedAddress = new StringBuilder();
        for (int i = 0; i < verticalProofLayers.length; i++) {
            recomposedAddress.insert(0, new String(verticalProofLayers[i].getRoute()));
        }

        return recomposedAddress.toString();
    }

    public static byte[] calculateTopHash(LedgerProofNode[] verticalProofLayers,
                                          @Nullable LedgerProofNode[] horizontalProofLayers) {
        if (verticalProofLayers == null || verticalProofLayers.length == 0) {
            throw new IllegalArgumentException("calculateTopHash cannot be called with a null or 0-length " +
                    "array of vertical proof layers!");
        }

        for (int i = 0; i < verticalProofLayers.length; i++) {
            if (verticalProofLayers[i] == null) {
                throw new IllegalArgumentException("calculateTopHash cannot be called with a vertical proof layer " +
                        "array which has a null proof layer! (null layer found at index " + i + ")");
            }
        }

        byte[] workingHash;

        if (horizontalProofLayers != null) {
            // The horizontal proof layers are the children of the first vertical proof layer
            byte[] substituteChildren = new byte[32 * horizontalProofLayers.length];

            for (int childIndex = 0; childIndex < horizontalProofLayers.length; childIndex++) {
                byte[] childHash = horizontalProofLayers[childIndex].calculateHash(
                        null,
                        null);
                System.arraycopy(childHash,
                        0,
                        substituteChildren,
                        childIndex * childHash.length,
                        childHash.length);
            }

            workingHash = verticalProofLayers[0].calculateHash(null, substituteChildren);
        } else {
            workingHash = verticalProofLayers[0].calculateHash(null, null);
        }

        // Layers are ordered bottom-to-top, hash 'up' in that order
        for (int verticalIndex = 1; verticalIndex < verticalProofLayers.length; verticalIndex++) {
            workingHash = verticalProofLayers[verticalIndex].calculateHash(workingHash, null);
        }

        byte[] truncatedHash = new byte[24]; // Ledger hash output size is 24 bytes, not 32
        System.arraycopy(workingHash, 0, truncatedHash, 0, truncatedHash.length);

        return truncatedHash;
    }
}
