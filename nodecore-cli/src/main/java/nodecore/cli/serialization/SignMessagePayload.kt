// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import nodecore.api.grpc.VeriBlockMessages.SignMessageReply
import nodecore.api.grpc.utilities.ByteStringUtility

class SignMessagePayload(
    val address: String,
    message: SignMessageReply
) {
    val signedMessage = ByteStringUtility.byteStringToHex(message.signedMessage)

    val publicKey = ByteStringUtility.byteStringToHex(message.publicKey)
}