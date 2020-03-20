// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.crypto;

import org.veriblock.core.utilities.Utility;

/**
 * A MerkleLayer represents a layer in a DataMerkleTree or BitcoinMerkleTree, and enables the access of elements (byte[] in
 * internal-endian order, not network-endian order) by their index.
 *
 * @author Maxwell Sanchez
 */
public class MerkleLayer {
    private final byte[][] _data;

    /**
     * This constructor for a BitcoinMerkleLayer accepts a byte[][] of _data.
     */
    public MerkleLayer(byte[][] data) {
        this._data = data;
    }

    /**
     * Creates the 'next' (higher, and half the size (round up if an odd number of _data exist in this layer)) layer of
     * the Bitcoin merkle tree.
     *
     * @return The next layer of the Bitcoin merkle tree
     */
    public MerkleLayer createNextLayer() {
        /* Create a 2D array for the new layer _data that is half the size (round up, if fractional) of this layer's _data */
        byte[][] newData = new byte[((_data.length % 2 == 0) ? _data.length / 2 : _data.length / 2 + 1)][];

        for (int i = 0; i < newData.length; i++) {
            /* Element i of newData is SHA256D of the two corresponding elements beneath it. If only one element is left, use it as both leaves. */
            newData[i] = new Crypto().SHA256D(Utility.concat(
                    _data[i * 2],
                    ((_data.length != i * 2 + 1) ? _data[i * 2 + 1] : _data[i * 2])));
        }

        return new MerkleLayer(newData);
    }

    /**
     * Returns the number of elements in this layer
     *
     * @return the number of elements in this layer
     */
    public int numElementsInLayer() {
        return _data.length;
    }

    /**
     * Returns the element at the provided index (elementNum)
     *
     * @param elementNum The index of the element to grab
     * @return A byte[], in internal order, of this layer's element at the provided index (elementNum)
     */
    public byte[] getElement(int elementNum) {
        return _data[elementNum];
    }
}
