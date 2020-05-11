// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.model

import org.veriblock.sdk.models.Coin

object OutputFactory {
    fun create(address: String, amount: Long): Output {
        require(amount >= 1) {
            "Output amounts must be greater than 0 (actual amount: $amount)"
        }
        val addressObj = AddressFactory.create(address)
        val amountObj = Coin.valueOf(amount)
        return Output(addressObj, amountObj)
    }
}
