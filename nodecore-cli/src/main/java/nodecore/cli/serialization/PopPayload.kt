// VeriBlock NodeCore CLI
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import nodecore.api.grpc.RpcGetPopReply
import org.veriblock.sdk.extensions.toBase58
import org.veriblock.sdk.extensions.toHex

class PopPayload(
    reply: RpcGetPopReply
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

    val pop_miner_address = reply.popMinerAddress.toBase58()

    val full_pop = reply.fullPop.toHex()

    val last_known_block = reply.lastKnownBlock.header.toHex()
}
