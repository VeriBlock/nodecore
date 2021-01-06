// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import nodecore.api.grpc.VeriBlockMessages.AddressBalanceSchedule
import nodecore.api.grpc.utilities.ByteStringAddressUtility
import org.veriblock.core.utilities.Utility

class AddressBalanceSchedulePayload(
    message: AddressBalanceSchedule
) {
    val address = ByteStringAddressUtility.parseProperAddressTypeAutomatically(message.address)

    val totalBalance = Utility.formatAtomicLongWithDecimal(message.totalBalance)

    val unlockedBalance = Utility.formatAtomicLongWithDecimal(message.unlockBalance)

    val lockedBalance = Utility.formatAtomicLongWithDecimal(message.lockedBalance)

    val schedule = message.scheduleList.map { balanceUnlockEvent ->
        BalanceScheduleItem(balanceUnlockEvent)
    }
}
