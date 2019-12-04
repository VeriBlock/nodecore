// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.miners.pop.contracts

import nodecore.miners.pop.common.BitcoinTransactionUtility
import nodecore.miners.pop.common.Utility
import org.bitcoinj.core.Block
import org.bitcoinj.core.Transaction

class PoPMiningTransaction(
    popMiningInstruction: PoPMiningInstruction,
    bitcoinTransaction: Transaction,
    bitcoinMerklePathToRoot: String,
    bitcoinBlockHeaderOfProof: Block,
    bitcoinContextBlocks: List<Block>
) {
    val endorsedBlockHeader: ByteArray = popMiningInstruction.endorsedBlockHeader

    val bitcoinTransaction: ByteArray = BitcoinTransactionUtility.parseTxIDRelevantBits(bitcoinTransaction.bitcoinSerialize())
    val bitcoinMerklePathToRoot: ByteArray = bitcoinMerklePathToRoot.toByteArray()
    val bitcoinBlockHeaderOfProof: ByteArray = Utility.serializeBlock(bitcoinBlockHeaderOfProof)

    val bitcoinContextBlocks: Array<ByteArray> = bitcoinContextBlocks.map {
        Utility.serializeBlock(it)
    }.toTypedArray()

    val popMinerAddress: ByteArray = popMiningInstruction.minerAddress
}
