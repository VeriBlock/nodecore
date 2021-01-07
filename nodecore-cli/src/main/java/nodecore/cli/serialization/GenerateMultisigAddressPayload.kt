// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import nodecore.api.grpc.VeriBlockMessages.GenerateMultisigAddressReply
import nodecore.api.grpc.utilities.ByteStringAddressUtility

class GenerateMultisigAddressPayload(
    reply: GenerateMultisigAddressReply
) {
    val sourceAddresses = Array(reply.sourceAddressesCount) { index ->
        ByteStringAddressUtility.parseProperAddressTypeAutomatically(reply.getSourceAddresses(index))
    }

    val signatureThresholdM = reply.signatureThresholdM

    val resultantMultisigAddress = ByteStringAddressUtility.parseProperAddressTypeAutomatically(reply.multisigAddress)
}
