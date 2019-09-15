// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.contracts;

import nodecore.miners.pop.common.BitcoinTransactionUtility;
import nodecore.miners.pop.common.Utility;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.Transaction;

import java.util.List;
import java.util.stream.Collectors;

public class PoPMiningTransaction {
    private byte[] endorsedBlockHeader_;

    public byte[] getEndorsedBlockHeader() {
        return endorsedBlockHeader_;
    }

    private void setEndorsedBlockHeader(byte[] value) {
        endorsedBlockHeader_ = value;
    }

    private byte[] bitcoinTransaction_;

    public byte[] getBitcoinTransaction() {
        return bitcoinTransaction_;
    }

    private void setBitcoinTransaction(byte[] value) {
        bitcoinTransaction_ = value;
    }

    private byte[] bitcoinMerklePathToRoot_;

    public byte[] getBitcoinMerklePathToRoot() {
        return bitcoinMerklePathToRoot_;
    }

    private void setBitcoinMerklePathToRoot(byte[] value) {
        bitcoinMerklePathToRoot_ = value;
    }

    private byte[] bitcoinBlockHeaderOfProof_;

    public byte[] getBitcoinBlockHeaderOfProof() {
        return bitcoinBlockHeaderOfProof_;
    }

    private void setBitcoinBlockHeaderOfProof(byte[] value) {
        bitcoinBlockHeaderOfProof_ = value;
    }

    private byte[][] bitcoinContextBlocks_;

    public byte[][] getBitcoinContextBlocks() {
        return bitcoinContextBlocks_;
    }

    private void setBitcoinContextBlocks(byte[][] value) {
        bitcoinContextBlocks_ = value;
    }

    private byte[] popMinerAddress_;

    public byte[] getPopMinerAddress() {
        return popMinerAddress_;
    }

    private void setPopMinerAddress(byte[] value) {
        popMinerAddress_ = value;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {
        private Builder() {

        }

        private PoPMiningInstruction popMiningInstruction_;

        public PoPMiningInstruction getPopMiningInstruction() {
            return popMiningInstruction_;
        }

        public Builder setPopMiningInstruction(PoPMiningInstruction value) {
            popMiningInstruction_ = value;
            return this;
        }

        private byte[] bitcoinTransaction_;

        public byte[] getBitcoinTransaction() {
            return bitcoinTransaction_;
        }

        public Builder setBitcoinTransaction(byte[] value) {
            bitcoinTransaction_ = value;
            return this;
        }

        private String bitcoinMerklePathToRoot_;

        public String getBitcoinMerklePathToRoot() {
            return bitcoinMerklePathToRoot_;
        }

        public Builder setBitcoinMerklePathToRoot(String value) {
            bitcoinMerklePathToRoot_ = value;
            return this;
        }

        private Block bitcoinBlockHeaderOfProof_;

        public Block getBitcoinBlockHeaderOfProof() {
            return bitcoinBlockHeaderOfProof_;
        }

        public Builder setBitcoinBlockHeaderOfProof(Block value) {
            bitcoinBlockHeaderOfProof_ = value;
            return this;
        }

        private List<Block> bitcoinContextBlocks_;

        public List<Block> getBitcoinContextBlocks() {
            return bitcoinContextBlocks_;
        }

        public Builder setBitcoinContextBlocks(List<Block> value) {
            bitcoinContextBlocks_ = value;
            return this;
        }

        public PoPMiningTransaction build() {
            PoPMiningTransaction transaction = new PoPMiningTransaction();
            transaction.setEndorsedBlockHeader(getPopMiningInstruction().endorsedBlockHeader);
            transaction.setBitcoinTransaction(getBitcoinTransaction());
            transaction.setBitcoinMerklePathToRoot(getBitcoinMerklePathToRoot().getBytes());
            transaction.setBitcoinBlockHeaderOfProof(Utility.serializeBlock(getBitcoinBlockHeaderOfProof()));

            List<byte[]> blockHeaders = getBitcoinContextBlocks().stream()
                    .map(Utility::serializeBlock)
                    .collect(Collectors.toList());
            byte[][] contextHeaders = new byte[blockHeaders.size()][];

            transaction.setBitcoinContextBlocks(blockHeaders.toArray(contextHeaders));
            transaction.setPopMinerAddress(getPopMiningInstruction().minerAddress);

            return transaction;
        }
    }
}
