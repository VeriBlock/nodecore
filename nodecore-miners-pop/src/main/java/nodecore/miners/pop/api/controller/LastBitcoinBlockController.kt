// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.api;

import static nodecore.miners.pop.api.annotations.Route.Verb.GET;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.bitcoinj.core.Block;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.core.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;

import nodecore.miners.pop.api.annotations.Route;
import nodecore.miners.pop.api.models.ShowLastBitcoinBlockResponse;
import nodecore.miners.pop.common.Utility;
import nodecore.miners.pop.contracts.PoPMiner;
import spark.Request;
import spark.Response;

public class LastBitcoinBlockController extends ApiController {
    private static final Logger logger = LoggerFactory.getLogger(LastBitcoinBlockController.class);
    private final PoPMiner miner;

    @Inject
    public LastBitcoinBlockController(PoPMiner miner) {
        this.miner = miner;
    }

    @Route(path = "/api/lastbitcoinblock", verb = GET)
    public String post(Request request, Response response) {
        try {            
            StoredBlock lastBlock = miner.getLastBitcoinBlock();
            Block lastBlockHeader = lastBlock.getHeader();

            ByteArrayOutputStream headerOutputSteram = new ByteArrayOutputStream();
            Utils.uint32ToByteStreamLE(lastBlockHeader.getVersion(), headerOutputSteram);
            headerOutputSteram.write(lastBlockHeader.getPrevBlockHash().getReversedBytes());
            headerOutputSteram.write(lastBlockHeader.getMerkleRoot().getReversedBytes());
            Utils.uint32ToByteStreamLE(lastBlockHeader.getTimeSeconds(), headerOutputSteram);
            Utils.uint32ToByteStreamLE(lastBlockHeader.getDifficultyTarget(), headerOutputSteram);
            Utils.uint32ToByteStreamLE(lastBlockHeader.getNonce(), headerOutputSteram);

            ShowLastBitcoinBlockResponse responseModel = new ShowLastBitcoinBlockResponse();
            responseModel.header = Utility.bytesToHex(headerOutputSteram.toByteArray());
            responseModel.hash = Utility.bytesToHex(lastBlockHeader.getHash().getBytes());
            responseModel.height = lastBlock.getHeight();

            response.status(200);
            response.type(CONTENT_TYPE_JSON);

            return toJson(responseModel);
        } catch (JsonSyntaxException e) {
            response.status(400);
            return "";
        } catch (IOException e) {
            logger.info("Cannot parse the block header", e);
            response.status(500);
            return "";
        }
    }
}