// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.miners.pop.model.merkle

import java.security.MessageDigest

class Crypto {
    private var sha256: MessageDigest = MessageDigest.getInstance("SHA-256")

    fun SHA256D(input: ByteArray): ByteArray {
        return SHA256ReturnBytes(SHA256ReturnBytes(input))
    }

    fun SHA256ReturnBytes(input: ByteArray): ByteArray {
        return sha256.digest(input)
    }
}
