// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.serialization;

import com.google.gson.annotations.SerializedName;
import nodecore.api.grpc.VeriBlockMessages;
import org.veriblock.core.utilities.Utility;

public class BlockFeeTableInfo {
    @SerializedName("pop_fee_share")
    public String popFeeShare;

    public BlockFeeTableInfo(final VeriBlockMessages.BlockFeeTable message) {
        this.popFeeShare = Utility.formatAtomicLongWithDecimal(message.getPopFeeShare());
    }
}
