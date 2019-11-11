// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.api;

import static nodecore.miners.pop.api.annotations.Route.Verb.POST;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;

import nodecore.miners.pop.InternalEventBus;
import nodecore.miners.pop.api.annotations.Route;
import nodecore.miners.pop.api.models.ResultResponse;
import nodecore.miners.pop.events.ProgramQuitEvent;
import spark.Request;
import spark.Response;

public class QuitController extends ApiController {
    
    public class QuitTask implements Callable<Integer> {
        public Integer call() throws IOException, InterruptedException {
            Thread.sleep(1000);
            ProgramQuitEvent event = new ProgramQuitEvent(1);
            InternalEventBus.getInstance().post(event);
            return 0;
        }
    }
    
    private static final Logger logger = LoggerFactory.getLogger(QuitController.class);

    @Inject
    public QuitController() {
    }

    @Route(path = "/api/quit", verb = POST)
    public String post(Request request, Response response) {
        try {
            ResultResponse responseModel = new ResultResponse();
            responseModel.failed = false;
            responseModel.messages = new ArrayList<>();
            
            logger.info("Terminating the miner now");
            ExecutorService quitExecutor = Executors.newSingleThreadExecutor();
            quitExecutor.submit(new QuitTask());
            
            response.status(200);
            response.type(CONTENT_TYPE_JSON);
            return toJson(responseModel);
        } catch (JsonSyntaxException e) {
            response.status(400);
            return "";
        }
    }
}