// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import nodecore.api.grpc.VeriBlockMessages
import nodecore.api.grpc.utilities.extensions.toProperAddressType
import org.veriblock.core.utilities.extensions.formatAtomicLongWithDecimal

class AddressBalanceInfo(
    addressBalance: VeriBlockMessages.AddressBalance
) {
    val address = addressBalance.address.toProperAddressType()

    val totalAmount = addressBalance.totalAmount.formatAtomicLongWithDecimal()

    val unlockedAmount = addressBalance.unlockedAmount.formatAtomicLongWithDecimal()

    val lockedAmount = addressBalance.lockedAmount.formatAtomicLongWithDecimal()

    override fun toString(): String = "address='$address'; unlocked_amount='$unlockedAmount'; locked_amount='$lockedAmount'"
}
