// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.serialization;

import nodecore.api.grpc.Output;
import nodecore.api.grpc.utilities.ByteStringAddressUtility;
import org.veriblock.core.utilities.Utility;

public class OutputInfo {
    public OutputInfo(final Output output) {
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
