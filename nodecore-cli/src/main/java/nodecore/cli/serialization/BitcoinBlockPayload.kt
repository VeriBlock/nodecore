// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import com.google.gson.annotations.SerializedName
import nodecore.api.grpc.VeriBlockMessages.GetLastBitcoinBlockReply
import nodecore.api.grpc.utilities.ByteStringUtility

class BitcoinBlockPayload(
    reply: GetLastBitcoinBlockReply
) {
    @SerializedName("header")
    val header = ByteStringUtility.byteStringToHex(reply.header)

    @SerializedName("height")
    val height = reply.height

    @SerializedName("hash")
    val hash = ByteStringUtility.byteStringToHex(reply.hash)
}
