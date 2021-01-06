// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import com.google.gson.annotations.SerializedName
import nodecore.api.grpc.VeriBlockMessages.GetNewAddressReply
import nodecore.api.grpc.utilities.ByteStringAddressUtility

class NewAddressPayload(
    reply: GetNewAddressReply
) {
    @SerializedName("address")
    val address = ByteStringAddressUtility.parseProperAddressTypeAutomatically(reply.address)

    @SerializedName("additional_address")
    val additionalAddresses = reply.additionalAddressesList.map {
        ByteStringAddressUtility.parseProperAddressTypeAutomatically(it)
    }.toTypedArray()
}