// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.core

import org.veriblock.core.Context
import org.veriblock.core.params.NetworkParameters
import org.veriblock.core.utilities.Configuration
import java.io.File
import java.util.*

class ApmContext(
    configuration: Configuration,
    val networkParameters: NetworkParameters
) {
    init {
        Context.create(networkParameters)
    }

    val dataDir = System.getenv("DATA_DIR")
        ?: configuration.extract("dataDir")
        ?: "./"

    val directory: File = File(dataDir)
    val filePrefix: String = "vbk-${networkParameters.name}"

    val vbkTokenName: String = if (networkParameters.name.lowercase(Locale.getDefault()) == "mainnet") "VBK" else "tVBK"
    val btcTokenName: String = if (networkParameters.name.lowercase(Locale.getDefault()) == "mainnet") "BTC" else "tBTC"
}
