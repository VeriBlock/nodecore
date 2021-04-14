// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import com.google.gson.annotations.SerializedName
import nodecore.api.grpc.RpcGetNewAddressReply
import nodecore.api.grpc.utilities.ByteStringAddressUtility
import nodecore.api.grpc.utilities.extensions.toProperAddressType

class NewAddressPayload(
    reply: RpcGetNewAddressReply
) {
    @SerializedName("address")
    val address = ByteStringAddressUtility.parseProperAddressTypeAutomatically(reply.address)

    @SerializedName("additional_address")
    val additionalAddresses = reply.additionalAddressesList.map {
        it.toProperAddressType()
    }.toTypedArray()
}
