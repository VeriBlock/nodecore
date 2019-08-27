// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.serialization;

import nodecore.api.grpc.BalanceUnlockEvent;
import org.veriblock.core.utilities.Utility;

public class BalanceScheduleItem {

    public int blockHeight;

    public String amountUnlocked;

    public String lockedBalance;

    public BalanceScheduleItem(final BalanceUnlockEvent message) {
        blockHeight = message.getBlockHeight();
        amountUnlocked = Utility.formatAtomicLongWithDecimal(message.getAmountUnlocked());
        lockedBalance = Utility.formatAtomicLongWithDecimal(message.getLockedBalance());
    }
}
