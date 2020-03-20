// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.crypto;

import java.util.ArrayList;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;

/**
 * The DataMerkleTree takes in all of the data for a Merkle Tree, constructs the tree, and then
 * allows for the creation of Merkle Paths.
 *
 * @author Maxwell Sanchez
 */
public class DataMerkleTree {
    private static final Encoder _base64Encoder = java.util.Base64.getEncoder();
    private static final Decoder _base64Decoder = java.util.Base64.getDecoder();
    private final ArrayList<MerkleLayer> _layers = new ArrayList<>();

    /**
     * Creates a DataMerkleTree for use on the VeriBlock network from inputData, expressed in Base64.
     *
     * @param inputData All of the leaves, in order, of the merkle tree
     */
    public DataMerkleTree(ArrayList<String> inputData) {
        /* All of the data of the tree */
        byte[][] floorData = new byte[inputData.size()][];

        for (int i = 0; i < inputData.size(); i++)
            floorData[i] = _base64Decoder.decode(inputData.get(i));

        /* Create, at a minimum, the bottom floor */
        _layers.add(new MerkleLayer(floorData));

        buildTree();
    }

    private void buildTree() {
        /* When the top layer has a single element, the tree is finished */
        /* Create the next layer, save it above the current highest layer */
        while (_layers.get(_layers.size() - 1).numElementsInLayer() > 1)
            _layers.add(_layers.get(_layers.size() - 1).createNextLayer());
    }

    /**
     * Get the Merkle Root of this tree.
     *
     * @return The Merkle Root of this tree, encoded in base 64.
     */
    public String getMerkleRoot() {
        return _base64Encoder.encodeToString(_layers.get(_layers.size() - 1).getElement(0));
    }

}
