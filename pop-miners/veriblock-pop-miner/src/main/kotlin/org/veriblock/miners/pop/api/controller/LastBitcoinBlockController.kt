// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.api.controller

import com.papsign.ktor.openapigen.annotations.Path
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.auth.OpenAPIAuthenticatedRoute
import com.papsign.ktor.openapigen.route.path.auth.get
import com.papsign.ktor.openapigen.route.response.respond
import io.ktor.auth.*
import org.bitcoinj.core.Utils
import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.miners.pop.api.models.ShowLastBitcoinBlockResponse
import org.veriblock.miners.pop.service.MinerService
import java.io.ByteArrayOutputStream

class LastBitcoinBlockController(
    private val minerService: MinerService
) : ApiController {

    @Path("lastbitcoinblock")
    class LastBitcoinBlockPath

    override fun OpenAPIAuthenticatedRoute<UserIdPrincipal>.registerApi() {
        get<LastBitcoinBlockPath, ShowLastBitcoinBlockResponse, UserIdPrincipal>(
            info("Get latest bitcoin block known by the PoP Miner")
        ) {
            val lastBlock = minerService.getLastBitcoinBlock()
            val lastBlockHeader = lastBlock.header

            val headerOutputSteram = ByteArrayOutputStream()
            Utils.uint32ToByteStreamLE(lastBlockHeader.version, headerOutputSteram)
            headerOutputSteram.write(lastBlockHeader.prevBlockHash.reversedBytes)
            headerOutputSteram.write(lastBlockHeader.merkleRoot.reversedBytes)
            Utils.uint32ToByteStreamLE(lastBlockHeader.timeSeconds, headerOutputSteram)
            Utils.uint32ToByteStreamLE(lastBlockHeader.difficultyTarget, headerOutputSteram)
            Utils.uint32ToByteStreamLE(lastBlockHeader.nonce, headerOutputSteram)

            val responseModel = ShowLastBitcoinBlockResponse(
                headerOutputSteram.toByteArray().toHex(),
                lastBlockHeader.hash.bytes.toHex(),
                lastBlock.height
            )

            respond(responseModel)
        }
    }
}
