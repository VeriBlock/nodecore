// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.api;

import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;
import nodecore.miners.pop.api.models.*;
import nodecore.miners.pop.contracts.MineResult;
import nodecore.miners.pop.contracts.OperationSummary;
import nodecore.miners.pop.contracts.PoPMiner;
import nodecore.miners.pop.contracts.PreservedPoPMiningOperationState;
import nodecore.miners.pop.api.annotations.Route;
import spark.Request;
import spark.Response;

import java.util.List;
import java.util.stream.Collectors;

import static nodecore.miners.pop.api.annotations.Route.Verb.GET;
import static nodecore.miners.pop.api.annotations.Route.Verb.POST;

public class MiningController extends ApiController {
    private final PoPMiner miner;

    @Inject
    public MiningController(PoPMiner miner) {
        this.miner = miner;
    }

    @Route(path = "/api/operations", verb = GET)
    public String listOperations(Request request, Response response) {
        try {
            List<OperationSummary> operationSummaries = miner.listOperations();
            if (operationSummaries == null) {
                response.status(404);
                return "";
            }

            List<OperationSummaryResponse> responseModel = operationSummaries.stream()
                    .map(ResponseMapper::map)
                    .collect(Collectors.toList());


            response.status(200);
            response.type(CONTENT_TYPE_JSON);

            return toJson(responseModel);
        } catch (Exception e) {
            response.status(500);
            return "";
        }
    }

    @Route(path = "/api/operations/:id", verb = GET)
    public String getOperation(Request request, Response response) {
        try {
            String id = request.params(":id");

            PreservedPoPMiningOperationState operationState = miner.getOperationState(id);
            if (operationState == null) {
                response.status(404);
                return "";
            }

            OperationDetailResponse responseModel = ResponseMapper.map(operationState);

            response.status(200);
            response.type(CONTENT_TYPE_JSON);

            return toJson(responseModel);
        } catch (Exception e) {
            response.status(500);
            return "";
        }
    }

    @Route(path = "/api/mine", verb = POST)
    public String post(Request request, Response response) {
        try {
            MineRequestPayload payload = fromJson(request.body(), MineRequestPayload.class);

            MineResult result = miner.mine(payload.block);
            MineResultResponse responseModel = ResponseMapper.map(result);

            response.status(200);
            response.type(CONTENT_TYPE_JSON);

            return toJson(responseModel);
        } catch (JsonSyntaxException e) {
            response.status(400);
            return "";
        }
    }

    @Route(path = "/api/miner", verb = GET)
    public String get(Request request, Response response) {
        try {
            MinerInfoResponse responseModel = new MinerInfoResponse();
            responseModel.bitcoinBalance = miner.getBitcoinBalance().longValue();
            responseModel.bitcoinAddress = miner.getBitcoinReceiveAddress();
            responseModel.minerAddress = miner.getMinerAddress();
            responseModel.walletSeed = miner.getWalletSeed();

            response.status(200);
            response.type(CONTENT_TYPE_JSON);

            return toJson(responseModel);
        } catch (Exception e) {
            response.status(500);
            return "";
        }
    }

}