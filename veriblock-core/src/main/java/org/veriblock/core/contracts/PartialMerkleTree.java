// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.contracts;

import java.util.BitSet;
import java.util.Vector;

public interface PartialMerkleTree {
    Vector<byte[]> getHashes();

    BitSet getBits();
}
