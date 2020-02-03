// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.alt

import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.sdk.models.PublicationData

class PublicationDataWithContext(
    val endorsedBlockHeight: Int,
    val publicationData: PublicationData,
    val context: List<ByteArray>,
    val btcContext: List<ByteArray>
) {
    fun getDetailedInfo(): List<String> = listOf(
        "Chain Identifier: ${publicationData.identifier}",
        "Publication Data Header: ${publicationData.header.toHex()}",
        "Publication Data Context Info: ${publicationData.contextInfo.toHex()}",
        "Publication Data Payout Info: ${publicationData.payoutInfo.toHex()}",
        "VBK Context Block Hashes: ${context.joinToString { it.toHex() }}",
        "BTC Context Block Hashes: ${btcContext.joinToString { it.toHex() }}"
    )
}
