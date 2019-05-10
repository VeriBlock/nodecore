// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.api;

import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;
import nodecore.miners.pop.api.models.ResponseMapper;
import nodecore.miners.pop.api.models.ResultResponse;
import nodecore.miners.pop.api.models.SetConfigRequestPayload;
import nodecore.miners.pop.contracts.Configuration;
import nodecore.miners.pop.contracts.Result;
import nodecore.miners.pop.api.annotations.Route;
import spark.Request;
import spark.Response;

import java.util.HashMap;
import java.util.List;

public class ConfigurationController extends ApiController {

    private final Configuration configuration;

    @Inject
    public ConfigurationController(Configuration configuration) {
        super();
        this.configuration = configuration;
    }

    @Route(path = "/api/config", verb = Route.Verb.GET)
    public String get(Request request, Response response) {
        try {
            List<String> configValues = configuration.list();
            HashMap<String, String> map = new HashMap<>();
            for (String item : configValues) {
                String[] split = item.split("=");
                if (split.length == 2) {
                    map.put(split[0], split[1]);
                } else {
                    map.put(split[0], "");
                }
            }

            response.status(200);
            response.type(CONTENT_TYPE_JSON);

            return toJson(map);
        } catch (Exception e) {
            response.status(500);
            return e.getMessage();
        }
    }

    @Route(path = "/api/config", verb = Route.Verb.PUT)
    public String put(Request request, Response response) {
        try {
            SetConfigRequestPayload payload = fromJson(request.body(), SetConfigRequestPayload.class);

            Result result = configuration.setProperty(payload.key, payload.value);
            if (result.didFail()) {
                response.status(500);
            } else {
                response.status(200);
            }

            ResultResponse responseModel = ResponseMapper.map(result);

            response.type(CONTENT_TYPE_JSON);
            return toJson(responseModel);
        } catch (JsonSyntaxException e) {
            response.status(400);
            return "";
        }
    }
}