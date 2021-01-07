// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import com.google.gson.annotations.SerializedName
import nodecore.api.grpc.VeriBlockMessages.ValidateAddressReply
import nodecore.api.grpc.utilities.ByteStringAddressUtility
import nodecore.api.grpc.utilities.ByteStringUtility

class ValidateAddressPayload(
    reply: ValidateAddressReply
) {
    val address = ByteStringAddressUtility.parseProperAddressTypeAutomatically(reply.address)

    @SerializedName("is_remote")
    val isRemote = reply.isRemote

    @SerializedName("public_key")
    val publicKey = reply.publicKey.toHex()
}
