// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.miners.pop.contracts

import nodecore.miners.pop.common.Utility
import org.bitcoinj.core.Block

class PoPMiningTransaction(
    popMiningInstruction: PoPMiningInstruction,
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

    val popMinerAddress: ByteArray = popMiningInstruction.minerAddress
}
