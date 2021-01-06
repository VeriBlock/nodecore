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

    val previous_block_hash = ByteStringUtility.byteStringToHex(reply.previousBlockHash)

    val second_previous_block_hash = ByteStringUtility.byteStringToHex(reply.secondPreviousBlockHash)

    val third_previous_block_hash = ByteStringUtility.byteStringToHex(reply.thirdPreviousBlockHash)

    val merkle_root = ByteStringUtility.byteStringToHex(reply.merkleRoot)

    val timestamp = reply.timestamp

    val encoded_difficulty = reply.encodedDifficulty

    val nonce = reply.nonce

    val pop_miner_address = ByteStringUtility.byteStringToBase58(reply.popMinerAddress)

    val full_pop = ByteStringUtility.byteStringToHex(reply.fullPop)

    val last_known_block = ByteStringUtility.byteStringToHex(reply.lastKnownBlock.header)
}
