// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import nodecore.api.grpc.RpcBalanceUnlockEvent
import org.veriblock.core.utilities.extensions.formatAtomicLongWithDecimal

class BalanceScheduleItem(
    message: RpcBalanceUnlockEvent
) {
    val blockHeight = message.blockHeight

    val amountUnlocked = message.amountUnlocked.formatAtomicLongWithDecimal()

    val lockedBalance = message.lockedBalance.formatAtomicLongWithDecimal()
}
