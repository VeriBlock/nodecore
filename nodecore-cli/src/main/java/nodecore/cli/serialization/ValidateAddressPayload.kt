// VeriBlock NodeCore CLI
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import com.google.gson.annotations.SerializedName
import nodecore.api.grpc.RpcValidateAddressReply
import org.veriblock.sdk.extensions.toHex
import org.veriblock.sdk.extensions.toProperAddressType

class ValidateAddressPayload(
    reply: RpcValidateAddressReply
) {
    val address = reply.address.toProperAddressType()

    @SerializedName("is_remote")
    val isRemote = reply.isRemote

    @SerializedName("public_key")
    val publicKey = reply.publicKey.toHex()
}
