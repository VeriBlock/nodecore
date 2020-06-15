// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.miners.pop.service.mockmining

import org.veriblock.core.params.BitcoinNetworkParameters
import org.veriblock.core.params.BitcoinRegTestParameters
import org.veriblock.sdk.services.SerializeDeserializeService
import org.veriblock.core.utilities.Utility
import org.veriblock.core.utilities.extensions.asHexBytes

object BitcoinDefaults {
    private val regtestGenesis = "0100000000000000000000000000000000000000000000000000000000000000000000003ba3edfd7a7b12b27ac72c3e67768f617fc81bc3888a51323a9fb8aa4b1e5e4adae5494dffff7f2002000000".asHexBytes()
    val genesis = SerializeDeserializeService.parseBitcoinBlock(regtestGenesis)
    const val genesisHeight = 0
    val networkParameters: BitcoinNetworkParameters = BitcoinRegTestParameters()
}
