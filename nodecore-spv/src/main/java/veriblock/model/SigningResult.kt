// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.model

class SigningResult(
    private val succeeded: Boolean,
    val signature: ByteArray?,
    val publicKey: ByteArray?
) {
    fun succeeded(): Boolean {
        return succeeded
    }
}
