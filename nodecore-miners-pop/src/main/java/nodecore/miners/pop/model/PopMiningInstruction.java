// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.model;

import nodecore.miners.pop.common.Utility;
import org.jetbrains.annotations.NotNull;
import org.veriblock.core.crypto.Crypto;
import org.veriblock.core.utilities.BlockUtility;

import java.io.Serializable;
import java.util.List;

public class PopMiningInstruction implements Serializable {
    public byte[] publicationData;
    public byte[] endorsedBlockHeader;
    public byte[] lastBitcoinBlock;
    public byte[] minerAddress;
    public List<byte[]> endorsedBlockContextHeaders;

    public int getEndorsedBlockHeight() {
        return BlockUtility.extractBlockHeightFromBlockHeader(endorsedBlockHeader);
    }

    public String getEndorsedBlockHash() {
        return new Crypto().vBlakeReturnHex(endorsedBlockHeader);
    }

    public String getMinerAddress() {
        return Utility.bytesToBase58(minerAddress);
    }

    @NotNull
    public String[] getDetailedInfo() {
        return new String[]{}; // TODO
    }
}
