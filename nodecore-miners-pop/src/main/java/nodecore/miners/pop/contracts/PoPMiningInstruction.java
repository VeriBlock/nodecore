// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.contracts;

import nodecore.miners.pop.common.Utility;
import org.veriblock.core.crypto.Crypto;

import java.io.Serializable;
import java.util.List;

public class PoPMiningInstruction implements Serializable {
    public byte[] publicationData;
    public byte[] endorsedBlockHeader;
    public byte[] lastBitcoinBlock;
    public byte[] minerAddress;
    public List<byte[]> endorsedBlockContextHeaders;

    public String getBlockHashAsString() {
        return new Crypto().vBlakeReturnHex(endorsedBlockHeader);
    }

    public String getMinerAddress() {
        return Utility.bytesToBase58(minerAddress);
    }
}
