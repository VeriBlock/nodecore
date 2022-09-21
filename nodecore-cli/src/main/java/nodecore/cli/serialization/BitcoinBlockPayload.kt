// VeriBlock NodeCore CLI
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import com.google.gson.annotations.SerializedName
import nodecore.api.grpc.RpcGetLastBitcoinBlockReply
import org.veriblock.sdk.extensions.toHex

class BitcoinBlockPayload(
    reply: RpcGetLastBitcoinBlockReply
) {
    @SerializedName("header")
    val header = reply.header.toHex()

    @SerializedName("height")
    val height = reply.height

    @SerializedName("hash")
    val hash = reply.hash.toHex()
}
