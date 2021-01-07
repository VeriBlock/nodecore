// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import nodecore.api.grpc.VeriBlockMessages.BalanceUnlockEvent
import org.veriblock.core.utilities.Utility
import org.veriblock.core.utilities.extensions.formatAtomicLongWithDecimal

class BalanceScheduleItem(
    message: BalanceUnlockEvent
) {
    val blockHeight = message.blockHeight

    val amountUnlocked = message.amountUnlocked.formatAtomicLongWithDecimal()

    val lockedBalance = message.lockedBalance.formatAtomicLongWithDecimal()
}
