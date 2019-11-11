// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.api.controller

import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import nodecore.miners.pop.PoPMiner
import nodecore.miners.pop.api.models.ShowLastBitcoinBlockResponse
import nodecore.miners.pop.common.Utility
import org.bitcoinj.core.Utils
import java.io.ByteArrayOutputStream

class LastBitcoinBlockController(
    private val miner: PoPMiner
) : ApiController {

    override fun Route.registerApi() {
        get("/lastbitcoinblock") {
            val lastBlock = miner.lastBitcoinBlock
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
