// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.serialization;

import nodecore.api.grpc.AddressBalanceSchedule;
import nodecore.api.grpc.BalanceUnlockEvent;
import nodecore.api.grpc.utilities.ByteStringAddressUtility;
import org.veriblock.core.utilities.Utility;

import java.util.ArrayList;
import java.util.List;

public class AddressBalanceSchedulePayload {

    public String address;

    public String totalBalance;

    public String unlockedBalance;

    public String lockedBalance;

    public List<BalanceScheduleItem> schedule;

    public AddressBalanceSchedulePayload(final AddressBalanceSchedule message) {
        address = ByteStringAddressUtility.parseProperAddressTypeAutomatically(message.getAddress());
        totalBalance = Utility.formatAtomicLongWithDecimal(message.getTotalBalance());
        unlockedBalance = Utility.formatAtomicLongWithDecimal(message.getUnlockBalance());
        lockedBalance = Utility.formatAtomicLongWithDecimal(message.getLockedBalance());

        schedule = new ArrayList<>();
        if (message.getScheduleCount() > 0) {
            for (BalanceUnlockEvent event : message.getScheduleList()) {
                schedule.add(new BalanceScheduleItem(event));
            }
        }
    }
}
