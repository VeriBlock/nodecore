// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.p2p

import nodecore.api.grpc.RpcFilteredBlock
import nodecore.api.grpc.utilities.ByteStringUtility
import nodecore.api.grpc.utilities.extensions.toByteString
import org.veriblock.core.contracts.FilteredBlock

fun FilteredBlock.toRpc(): RpcFilteredBlock {
    return RpcFilteredBlock.newBuilder().also {
        it.number = number
        it.version = version.toInt()
        it.previousHash = previousHash.toByteString()
        it.secondPreviousHash = secondPreviousHash.toByteString()
        it.thirdPreviousHash = thirdPreviousHash.toByteString()
        it.merkleRoot = merkleRoot.toByteString()
        it.timestamp = timestamp
        it.difficulty = difficulty
        it.nonce = nonce
        it.totalRegularTransactions = totalRegularTransactions
        it.totalPopTransactions = totalPoPTransactions
        it.addAllMerkleHashes(partialMerkleTree.hashes.map { hash -> hash.toByteString() })
        it.merkleFlags = partialMerkleTree.bits.toByteArray().toByteString()
    }.build()
}
