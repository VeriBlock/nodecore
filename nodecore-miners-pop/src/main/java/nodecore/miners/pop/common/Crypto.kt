// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.miners.pop.common

import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

private val logger = LoggerFactory.getLogger(Crypto::class.java)

class Crypto {
    private var sha256: MessageDigest = try {
        MessageDigest.getInstance("SHA-256")
    } catch (e: NoSuchAlgorithmException) {
        logger.error(e.message, e)
        throw e
    }

    fun SHA256D(input: ByteArray?): ByteArray {
        return SHA256ReturnBytes(SHA256ReturnBytes(input))
    }

    fun SHA256ReturnBytes(input: ByteArray?): ByteArray {
        return sha256.digest(input)
    }
}
