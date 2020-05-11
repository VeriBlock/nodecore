// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.wallet

import org.veriblock.core.utilities.AddressUtility
import org.veriblock.core.wallet.AddressKeyGenerator
import java.security.KeyPair
import java.security.spec.InvalidKeySpecException

class Key(
    private val keyPair: KeyPair
) {
    val address: String = AddressUtility.addressFromPublicKey(keyPair.public)

    val publicKey: ByteArray
        get() = keyPair.public.encoded

    val privateKey: ByteArray
        get() = keyPair.private.encoded

    companion object {
        @Throws(InvalidKeySpecException::class)
        fun parse(publicKeyBytes: ByteArray?, privateKeyBytes: ByteArray?): Key {
            val publicKey = AddressKeyGenerator.getPublicKey(publicKeyBytes)
            val privateKey = AddressKeyGenerator.getPrivateKey(privateKeyBytes)
            return Key(KeyPair(publicKey, privateKey))
        }
    }
}
