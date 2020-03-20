// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.model;

import org.veriblock.sdk.models.Sha256Hash;
import org.veriblock.sdk.models.VBlakeHash;
import org.veriblock.sdk.models.VeriBlockBlock;

import java.util.List;

public class FullBlock extends VeriBlockBlock {
    private List<StandardTransaction> normalTransactions;

    public List<StandardTransaction> getNormalTransactions() {
        return normalTransactions;
    }
    public void setNormalTransactions(List<StandardTransaction> normalTransactions) {
        this.normalTransactions = normalTransactions;
    }

    private List<PoPTransactionLight> popTransactionLights;
    public List<PoPTransactionLight> getPoPTransactions() {
        return popTransactionLights;
    }
    public void setPoPTransactions(List<PoPTransactionLight> popTransactionLights) {
        this.popTransactionLights = popTransactionLights;
    }

    private BlockMetaPackage metaPackage;
    public BlockMetaPackage getMetaPackage() {
        return metaPackage;
    }
    public void setMetaPackage(BlockMetaPackage metaPackage) {
        this.metaPackage = metaPackage;
    }

    public FullBlock(int height,
                 short version,
                 VBlakeHash previousBlock,
                 VBlakeHash previousKeystone,
                 VBlakeHash secondPreviousKeystone,
                 Sha256Hash merkleRoot,
                 int timestamp,
                 int difficulty,
                 int nonce) {
        super(height, version, previousBlock, previousKeystone, secondPreviousKeystone, merkleRoot, timestamp, difficulty, nonce);
    }
}
