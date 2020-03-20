// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.contracts;

public interface LedgerMerklePath {
    /**
     * Returns the Merkle root produced by following the layers up to the top of the tree.
     *
     * @return The Merkle root produced by following the path up to the top of the transaction tree, encoded in hexadecimal
     */
    String getMerkleRoot();

    /**
     * Returns a compact representation of this LedgerMerklePath. The path steps are stored in Hex.
     * Format: bottomIndex:bottomRoute;bottomAtomicUnits;bottomSignatureIndex:layer1Route;layer1_1;...;layer1_M:...:layerNRoute;layerN_1;...;layerN_M
     *
     * @return A compact representation of this LedgerMerklePath
     */
    String getCompactFormat();

    int getBottomDataIndex();

    String getBottomRoute();

    long getAtomicUnits();

    long getSignatureIndex();

    String getBottomDataSerialized();
}
