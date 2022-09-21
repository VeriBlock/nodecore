// VeriBlock NodeCore CLI
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import nodecore.api.grpc.RpcOutput
import org.veriblock.sdk.extensions.toProperAddressType
import org.veriblock.core.utilities.extensions.formatAtomicLongWithDecimal

class OutputInfo(
    output: RpcOutput
) {
    val address = output.address.toProperAddressType()

    val amount = output.amount.formatAtomicLongWithDecimal()

    override fun toString(): String = "address='$address'; amount='$amount'"
}
