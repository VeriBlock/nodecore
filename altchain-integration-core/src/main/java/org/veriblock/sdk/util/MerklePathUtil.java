// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.util;

import org.veriblock.sdk.models.MerklePath;
import org.veriblock.sdk.models.Sha256Hash;
import org.veriblock.sdk.models.VeriBlockMerklePath;

import java.util.List;

public class MerklePathUtil {

    public static Sha256Hash calculateMerkleRoot(MerklePath merklePath) {
        return calculateMerkleRoot(merklePath.getIndex(), merklePath.getSubject(), merklePath.getLayers());
    }

    public static Sha256Hash calculateMerkleRoot(int layerIndex, Sha256Hash cursor, List<Sha256Hash> layers) {
        for (Sha256Hash layer : layers) {
            /* Climb one layer up the tree by concatenating the current state with the next layer in the right order */
            byte[] first = layerIndex % 2 == 0 ? cursor.getBytes() : layer.getBytes();
            byte[] second = layerIndex % 2 == 0 ? layer.getBytes() : cursor.getBytes();
            cursor = Sha256Hash.twiceOf(first, second);

            /* The position above on the tree will be floor(currentIndex / 2) */
            layerIndex /= 2;
        }

        return cursor;
    }

    public static Sha256Hash calculateVeriMerkleRoot(VeriBlockMerklePath merklePath) {
        Sha256Hash cursor = merklePath.getSubject();
        int layerIndex = merklePath.getIndex();

        for (int i = 0; i < merklePath.getLayers().size(); i++) {// Because a layer has processed but the index (i) hasn't progressed, these values are offset by 1
            if (i == merklePath.getLayers().size() - 1) {
                /* The last layer is the BlockContentMetapackage hash and will always be the "left" side,
                   so set the layerIndex to 1 */
                layerIndex = 1;
            } else if (i == merklePath.getLayers().size() - 2) {
                /* The second to last layer is the joining with the opposite transaction type group (normal vs pop),
                   so use the tree index specified in the compact format */
                layerIndex = merklePath.getTreeIndex();
            }

            /* Climb one layer up the tree by concatenating the current state with the next layer in the right order */
            byte[] first = layerIndex % 2 == 0 ? cursor.getBytes() : merklePath.getLayers().get(i).getBytes();
            byte[] second = layerIndex % 2 == 0 ? merklePath.getLayers().get(i).getBytes() : cursor.getBytes();
            cursor = Sha256Hash.of(first, second);

            /* The position above on the tree will be floor(currentIndex / 2) */
            layerIndex /= 2;
        }

        return cursor;
    }
}
