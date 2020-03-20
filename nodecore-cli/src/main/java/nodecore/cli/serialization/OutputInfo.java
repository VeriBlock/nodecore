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

public class OutputInfo {
    public OutputInfo(final VeriBlockMessages.Output output) {
        address = ByteStringAddressUtility.parseProperAddressTypeAutomatically(output.getAddress());
        amount = Utility.formatAtomicLongWithDecimal(output.getAmount());
    }

    public String address;

    public String amount;

    @Override
    public String toString() {
        return
                "address='" + address + '\'' +
                "; amount='" + amount + '\'';
    }
}
