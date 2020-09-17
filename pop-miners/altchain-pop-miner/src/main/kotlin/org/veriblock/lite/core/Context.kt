// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.lite.core

import org.veriblock.core.params.NetworkParameters
import org.veriblock.core.utilities.Configuration
import java.io.File

class Context(
    configuration: Configuration,
    val networkParameters: NetworkParameters
) {
    init {
        org.veriblock.core.Context.create(networkParameters)
    }

    val dataDir = System.getenv("DATA_DIR")
        ?: configuration.extract("dataDir")
        ?: "./"

    val directory: File = File(dataDir)
    val filePrefix: String = "vbk-${networkParameters.name}"

    val vbkTokenName: String = if (networkParameters.name.toLowerCase() == "mainnet") "VBK" else "tVBK"
    val btcTokenName: String = if (networkParameters.name.toLowerCase() == "mainnet") "BTC" else "tBTC"
}
