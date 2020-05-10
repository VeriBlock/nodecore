// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.model

import org.veriblock.core.utilities.AddressUtility

object AddressFactory {
    @kotlin.jvm.JvmStatic
    fun create(address: String): AddressLight {
        if (AddressUtility.isValidStandardAddress(address)) {
            return StandardAddress(address)
        }
        if (AddressUtility.isValidMultisigAddress(address)) {
            return MultisigAddress(address)
        }
        throw IllegalArgumentException("Supplied argument is not a valid address")
    }
}
