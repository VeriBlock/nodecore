// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.contracts;

public interface FilteredBlock {
    int getNumber();

    short getVersion();

    byte[] getPreviousHash();

    byte[] getSecondPreviousHash();

    byte[] getThirdPreviousHash();

    byte[] getMerkleRoot();

    int getTimestamp();

    int getDifficulty();

    int getNonce();

    int getTotalRegularTransactions();

    int getTotalPoPTransactions();

    PartialMerkleTree getPartialMerkleTree();
}
