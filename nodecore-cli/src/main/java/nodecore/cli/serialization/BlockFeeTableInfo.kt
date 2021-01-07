// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import com.google.gson.annotations.SerializedName
import nodecore.api.grpc.VeriBlockMessages.BlockFeeTable
import org.veriblock.core.utilities.extensions.formatAtomicLongWithDecimal

class BlockFeeTableInfo(
    message: BlockFeeTable
) {
    @SerializedName("pop_fee_share")
    val popFeeShare = message.popFeeShare.formatAtomicLongWithDecimal()
}
