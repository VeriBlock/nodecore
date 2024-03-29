// VeriBlock NodeCore CLI
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import com.google.gson.annotations.SerializedName
import nodecore.api.grpc.RpcDumpPrivateKeyReply
import org.veriblock.sdk.extensions.toHex
import org.veriblock.sdk.extensions.toProperAddressType

class PrivateKeyInfo(
    reply: RpcDumpPrivateKeyReply
) {
    @SerializedName("address")
    val address = reply.address.toProperAddressType()

    @SerializedName("private_key")
    val privateKey = reply.privateKey.toHex()
}
