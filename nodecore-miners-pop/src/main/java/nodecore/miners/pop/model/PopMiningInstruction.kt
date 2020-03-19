// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.miners.pop.model

import nodecore.miners.pop.common.Utility
import org.veriblock.core.crypto.Crypto
import org.veriblock.core.utilities.BlockUtility
import java.io.Serializable

class PopMiningInstruction(
    val publicationData: ByteArray,
    val endorsedBlockHeader: ByteArray,
    val lastBitcoinBlock: ByteArray,
    val minerAddressBytes: ByteArray,
    val endorsedBlockContextHeaders: List<ByteArray> = emptyList()
) : Serializable {

    val endorsedBlockHeight: Int = BlockUtility.extractBlockHeightFromBlockHeader(endorsedBlockHeader)

    val endorsedBlockHash: String = Crypto().vBlakeReturnHex(endorsedBlockHeader)

    val minerAddress: String = Utility.bytesToBase58(minerAddressBytes)

    // TODO
    val detailedInfo: Array<String>
        get() = arrayOf() // TODO
}
