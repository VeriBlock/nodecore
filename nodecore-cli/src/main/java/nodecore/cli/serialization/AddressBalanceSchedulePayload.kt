// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import nodecore.api.grpc.RpcAddressBalanceSchedule
import org.veriblock.sdk.extensions.toProperAddressType
import org.veriblock.core.utilities.extensions.formatAtomicLongWithDecimal

class AddressBalanceSchedulePayload(
    message: RpcAddressBalanceSchedule
) {
    val address = message.address.toProperAddressType()

    val totalBalance = message.totalBalance.formatAtomicLongWithDecimal()

    val unlockedBalance = message.unlockBalance.formatAtomicLongWithDecimal()

    val lockedBalance = message.lockedBalance.formatAtomicLongWithDecimal()

    val schedule = message.scheduleList.map { balanceUnlockEvent ->
        BalanceScheduleItem(balanceUnlockEvent)
    }
}
