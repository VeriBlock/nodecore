// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.lite.core

import org.veriblock.lite.params.NetworkParameters
import org.veriblock.core.utilities.Configuration
import java.io.File

object Context {
    val dataDir = System.getenv("DATA_DIR")
        ?: Configuration.extract("dataDir")
        ?: "./"

    val networkParameters = NetworkParameters
    val directory: File = File(dataDir)
    val filePrefix: String = "vbk-${networkParameters.network}"

    val vbkTokenName: String = if (networkParameters.network == "mainnet") "VBK" else "tVBK"
    val btcTokenName: String = if (networkParameters.network == "mainnet") "BTC" else "tBTC"
}
