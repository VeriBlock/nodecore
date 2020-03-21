// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.api.controller

import de.nielsfalk.ktor.swagger.description
import de.nielsfalk.ktor.swagger.get
import de.nielsfalk.ktor.swagger.ok
import de.nielsfalk.ktor.swagger.responds
import de.nielsfalk.ktor.swagger.version.shared.Group
import io.ktor.application.call
import io.ktor.locations.Location
import io.ktor.response.respond
import io.ktor.routing.Route
import nodecore.miners.pop.MinerService
import nodecore.miners.pop.api.models.ShowLastBitcoinBlockResponse
import nodecore.miners.pop.common.Utility
import org.bitcoinj.core.Utils
import java.io.ByteArrayOutputStream

@Group("Bitcoin") @Location("/api/lastbitcoinblock") class lastBitcoinBlock

class LastBitcoinBlockController(
    private val minerService: MinerService
) : ApiController {

    override fun Route.registerApi() {
        get<lastBitcoinBlock>(
            "lastbitcoinblock"
                .description("Get latest bitcoin block known by the PoP Miner")
                .responds(
                    ok<ShowLastBitcoinBlockResponse>()
                )
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
            responseModel.header = Utility.bytesToHex(headerOutputSteram.toByteArray())
            responseModel.hash = Utility.bytesToHex(lastBlockHeader.hash.bytes)
            responseModel.height = lastBlock.height

            call.respond(responseModel)
        }
    }
}
