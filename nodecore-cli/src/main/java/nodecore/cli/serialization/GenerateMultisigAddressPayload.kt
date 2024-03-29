// VeriBlock NodeCore CLI
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import nodecore.api.grpc.RpcGenerateMultisigAddressReply
import org.veriblock.sdk.extensions.ByteStringAddressUtility
import org.veriblock.sdk.extensions.toProperAddressType

class GenerateMultisigAddressPayload(
    reply: RpcGenerateMultisigAddressReply
) {
    val sourceAddresses = Array(reply.sourceAddressesCount) { index ->
       reply.getSourceAddresses(index).toProperAddressType()
    }

    val signatureThresholdM = reply.signatureThresholdM

    val resultantMultisigAddress = ByteStringAddressUtility.parseProperAddressTypeAutomatically(reply.multisigAddress)
}
