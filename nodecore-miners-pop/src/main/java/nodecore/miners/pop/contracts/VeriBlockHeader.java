// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.contracts;

import nodecore.miners.pop.common.Utility;
import org.veriblock.core.utilities.BlockUtility;

import java.util.Arrays;

public class VeriBlockHeader {
    private final byte[] bytes;

    private final String blockHeaderHash;

    public String getHash() {
        return this.blockHeaderHash;
    }

    public int getHeight() {
        return BlockUtility.extractBlockHeightFromBlockHeader(bytes);
    }

    public short getVersion() {
        return BlockUtility.extractVersionFromBlockHeader(bytes);
    }

    public String getPreviousHash() {
        return Utility.bytesToHex(BlockUtility.extractPreviousBlockHashFromBlockHeader(bytes));
    }

    public String getSecondPreviousHash() {
        return Utility.bytesToHex(BlockUtility.extractSecondPreviousBlockHashFromBlockHeader(bytes));
    }

    public String getThirdPreviousHash() {
        return Utility.bytesToHex(BlockUtility.extractThirdPreviousBlockHashFromBlockHeader(bytes));
    }

    public String getMerkleRoot() {
        return Utility.bytesToHex(BlockUtility.extractMerkleRootFromBlockHeader(bytes));
    }

    public int getTimestamp() {
        return BlockUtility.extractTimestampFromBlockHeader(bytes);
    }

    public int getDifficulty() {
        return BlockUtility.extractDifficultyFromBlockHeader(bytes);
    }

    public int getNonce() {
        return BlockUtility.extractNonceFromBlockHeader(bytes);
    }

    public VeriBlockHeader(byte[] bytes) {
        this.bytes = bytes;
        this.blockHeaderHash = BlockUtility.hashBlock(this.bytes);
    }

    public byte[] toByteArray() {
        return bytes;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof VeriBlockHeader)) return false;

        VeriBlockHeader compare = (VeriBlockHeader)obj;
        return Arrays.equals(this.bytes, compare.bytes);
    }
}