// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.alt

import org.veriblock.core.contracts.MiningInstruction
import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.sdk.models.PublicationData

class ApmInstruction(
    override val endorsedBlockHeight: Int,
    val publicationData: PublicationData,
    val context: List<ByteArray>,
    val btcContext: List<ByteArray>
) : MiningInstruction {
    override val detailedInfo: Map<String, String>
        get() = mapOf(
            "chainIdentifier" to publicationData.identifier.toString(),
            "publicationDataHeader" to publicationData.header.toHex(),
            "publicationDataContextInfo" to publicationData.contextInfo.toHex(),
            "publicationData PayoutInfo" to publicationData.payoutInfo.toHex(),
            "vbkContextBlockHashes" to context.joinToString { it.toHex() },
            "btcContextBlockHashes" to btcContext.joinToString { it.toHex() }
        )
}
