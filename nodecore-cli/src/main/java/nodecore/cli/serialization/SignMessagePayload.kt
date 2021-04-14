// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import nodecore.api.grpc.RpcSignMessageReply
import nodecore.api.grpc.utilities.extensions.toHex

class SignMessagePayload(
    val address: String,
    message: RpcSignMessageReply
) {
    val signedMessage = message.signedMessage.toHex()

    val publicKey = message.publicKey.toHex()
}
