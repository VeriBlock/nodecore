// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.miners.pop.api.models

import com.papsign.ktor.openapigen.annotations.Response

@Response("Basic information from the last bitcoin block")
class ShowLastBitcoinBlockResponse(
    val header: String? = null,
    val hash: String? = null,
    val height: Int = 0
)
