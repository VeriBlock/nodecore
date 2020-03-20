// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.serialization;

import nodecore.api.grpc.VeriBlockMessages;
import nodecore.api.grpc.utilities.ByteStringAddressUtility;
import org.veriblock.core.utilities.Utility;

public class AddressBalanceInfo {
    public AddressBalanceInfo(final VeriBlockMessages.AddressBalance addressBalance) {
        address = ByteStringAddressUtility.parseProperAddressTypeAutomatically(addressBalance.getAddress());
        totalAmount = Utility.formatAtomicLongWithDecimal(addressBalance.getTotalAmount());
        unlockedAmount = Utility.formatAtomicLongWithDecimal(addressBalance.getUnlockedAmount());
        lockedAmount = Utility.formatAtomicLongWithDecimal(addressBalance.getLockedAmount());
    }

    public String address;

    public String totalAmount;

    public String unlockedAmount;

    public String lockedAmount;

    @Override
    public String toString() {
        return
                "address='" + address + '\'' +
                        "; unlocked_amount='" + unlockedAmount + '\'' +
                        "; locked_amount='" + lockedAmount + "\'";
    }
}
