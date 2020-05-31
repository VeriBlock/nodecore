// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.alt

import org.veriblock.core.contracts.MiningInstruction
import org.veriblock.core.utilities.AddressUtility
import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.sdk.models.PublicationData

class ApmInstruction(
    override val endorsedBlockHeight: Int,
    val publicationData: PublicationData,
    val context: List<ByteArray>,
    val btcContext: List<ByteArray>
) : MiningInstruction
