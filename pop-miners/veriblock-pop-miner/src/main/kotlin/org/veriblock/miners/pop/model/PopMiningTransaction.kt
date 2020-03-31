// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.miners.pop.model

import org.bitcoinj.core.Block
import org.veriblock.miners.pop.common.Utility

class PopMiningTransaction(
    popMiningInstruction: PopMiningInstruction,
    val bitcoinTransaction: ByteArray,
    bitcoinMerklePathToRoot: String,
    bitcoinBlockHeaderOfProof: Block,
    bitcoinContextBlocks: List<Block>
) {
    val endorsedBlockHeader: ByteArray = popMiningInstruction.endorsedBlockHeader

    val bitcoinMerklePathToRoot: ByteArray = bitcoinMerklePathToRoot.toByteArray()
    val bitcoinBlockHeaderOfProof: ByteArray = Utility.serializeBlock(bitcoinBlockHeaderOfProof)

    val bitcoinContextBlocks: Array<ByteArray> = bitcoinContextBlocks.map {
        Utility.serializeBlock(it)
    }.toTypedArray()

    val popMinerAddress: ByteArray = popMiningInstruction.minerAddressBytes
}
