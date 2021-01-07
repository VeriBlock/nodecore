// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import nodecore.api.grpc.VeriBlockMessages.GetPopReply
import nodecore.api.grpc.utilities.ByteStringUtility

class PopPayload(
    reply: GetPopReply
) {
    val block_height = reply.blockHeight

    val version = reply.version

    val previous_block_hash = reply.previousBlockHash.toHex()

    val second_previous_block_hash = reply.secondPreviousBlockHash.toHex()

    val third_previous_block_hash = reply.thirdPreviousBlockHash.toHex()

    val merkle_root = reply.merkleRoot.toHex()

    val timestamp = reply.timestamp

    val encoded_difficulty = reply.encodedDifficulty

    val nonce = reply.nonce

    val pop_miner_address = ByteStringUtility.byteStringToBase58(reply.popMinerAddress)

    val full_pop = reply.fullPop.toHex()

    val last_known_block = reply.lastKnownBlock.header.toHex()
}
