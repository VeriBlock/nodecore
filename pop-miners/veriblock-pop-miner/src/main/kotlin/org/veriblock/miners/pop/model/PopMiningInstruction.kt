// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.miners.pop.model

import org.veriblock.core.crypto.Crypto
import org.veriblock.core.utilities.BlockUtility
import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.miners.pop.common.Utility
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

    val detailedInfo: Map<String, String>
        get() = mapOf(
            "publicationData" to publicationData.toHex(),
            "endorsedBlockHeader" to endorsedBlockHeader.toHex(),
            "lastBitcoinBlock" to lastBitcoinBlock.toHex(),
            "minerAddressBytes" to minerAddressBytes.toHex()
            //"endorsedBlockContextHeaders" to endorsedBlockContextHeaders.joinToString { it.toHex() }
        )
}
