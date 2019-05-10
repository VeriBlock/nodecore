// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.api;

import com.google.inject.Inject;
import nodecore.miners.pop.api.annotations.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Set;

import static spark.Spark.*;


public class ApiServer {
    private static final Logger logger = LoggerFactory.getLogger(ApiServer.class);

    private final Set<ApiController> controllers;

    private boolean running = false;

    private Integer port;
    public Integer getPort() {
        return this.port;
    }
    public void setPort(Integer value) {
        if (running) throw new IllegalStateException("Port cannot be set after the server has started");

        this.port = value;
    }

    private String address;
    public String getAddress() {
        return this.address;
    }
    public void setAddress(String value) {
        if (running) throw new IllegalStateException("Address cannot be set after the server has started");

        this.address = value;
    }

    @Inject
    public ApiServer(Set<ApiController> controllers) {
        this.controllers = controllers;
    }

    public void start() {
        initExceptionHandler(this::handleException);

        if (this.address != null) {
            ipAddress(this.address);
        }
        if (this.port != null) {
            port(this.port);
        }

        logger.info("Starting HTTP API on {}:{}", getAddress(), getPort());

        for (ApiController controller : controllers) {
            Class<? extends ApiController> clazz = controller.getClass();
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Route.class)) {
                    Route annotation = method.getAnnotation(Route.class);
                    logger.info("Adding route: {} {}", annotation.verb().name(), annotation.path());
                    switch (annotation.verb()) {
                        case GET:
                            get(annotation.path(), (request, response) -> method.invoke(controller, request, response));
                            break;
                        case POST:
                            post(annotation.path(), (request, response) -> method.invoke(controller, request, response));
                            break;
                        case PUT:
                            put(annotation.path(), (request, response) -> method.invoke(controller, request, response));
                            break;
                    }
                }
            }
        }

        running = true;
    }

    private void handleException(Exception e) {
        logger.error(e.getMessage(), e);
        logger.info("HTTP API could not be started");
    }

    public void shutdown() {
        if (running) {
            stop();
        }
    }
}