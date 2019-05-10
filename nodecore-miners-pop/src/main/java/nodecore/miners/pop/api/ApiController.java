// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.api;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public abstract class ApiController {
    protected static final String CONTENT_TYPE_JSON = "application/json";

    protected final Gson serializer;

    protected ApiController() {
        this.serializer = new Gson();
    }

    protected <T> T fromJson(String json, Class<T> classOfT) throws JsonSyntaxException {
        return serializer.fromJson(json, classOfT);
    }

    protected String toJson(Object o) {
        return serializer.toJson(o);
    }
}