// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import com.google.gson.annotations.SerializedName
import nodecore.api.grpc.VeriBlockMessages.DumpPrivateKeyReply
import nodecore.api.grpc.utilities.ByteStringAddressUtility
import nodecore.api.grpc.utilities.ByteStringUtility
import nodecore.api.grpc.utilities.extensions.toHex
import nodecore.api.grpc.utilities.extensions.toProperAddressType

class PrivateKeyInfo(
    reply: DumpPrivateKeyReply
) {
    @SerializedName("address")
    val address = reply.address.toProperAddressType()

    @SerializedName("private_key")
    val privateKey = reply.privateKey.toHex()
}
