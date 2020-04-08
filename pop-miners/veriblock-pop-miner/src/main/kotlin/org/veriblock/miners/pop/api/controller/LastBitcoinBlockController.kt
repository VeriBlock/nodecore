// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.api.controller

import io.ktor.application.call
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.response.respond
import io.ktor.routing.Route
import org.bitcoinj.core.Utils
import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.miners.pop.api.models.ShowLastBitcoinBlockResponse
import org.veriblock.miners.pop.service.MinerService
import java.io.ByteArrayOutputStream

//@Group("Bitcoin")
@Location("/api/lastbitcoinblock")
class lastBitcoinBlock

class LastBitcoinBlockController(
    private val minerService: MinerService
) : ApiController {

    override fun Route.registerApi() {
        get<lastBitcoinBlock>(
        //    "lastbitcoinblock"
        //        .description("Get latest bitcoin block known by the PoP Miner")
        //        .responds(
        //            ok<ShowLastBitcoinBlockResponse>()
        //        )
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

            val responseModel = ShowLastBitcoinBlockResponse()
            responseModel.header = headerOutputSteram.toByteArray().toHex()
            responseModel.hash = lastBlockHeader.hash.bytes.toHex()
            responseModel.height = lastBlock.height

            call.respond(responseModel)
        }
    }
}
