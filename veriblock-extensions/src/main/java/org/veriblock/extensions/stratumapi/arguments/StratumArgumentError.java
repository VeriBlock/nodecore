// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.extensions.stratumapi.arguments;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

public class StratumArgumentError extends StratumArgument {
    public enum ERROR_CODE {
        GENERIC(-1),
        UNKNOWN(20),
        JOB_NOT_FOUND(21),
        DUPLICATE_SHARE(22),
        LOW_DIFFICULTY_SHARE(23),
        UNAUTHORIZED_WORKER(24),
        NOT_SUBSCRIBED(25);

        private int code;

        ERROR_CODE(int code) {
            this.code = code;
        }

        int getCode() {
            return code;
        }
    }

    private final StratumArgument.StratumType type = StratumType.ERROR;

    private final ERROR_CODE errorCode;
    private final String message;
    private final JsonObject jsonObject;

    public void throwValidationError(String data) {
        throw new IllegalArgumentException("\"" + data + "\" did not pass the preliminary validation of "
            + getClass().getCanonicalName() + " (" + type.getPreliminaryValidationPattern() + ")");
    }

    /**
     * Constructor for the actual represented data type, useful when creating a command.
     * @param errorCode
     * @param message
     * @param jsonObject
     */
    public StratumArgumentError(ERROR_CODE errorCode, String message, JsonObject jsonObject) {
        this.errorCode = errorCode;
        this.message = message;
        this.jsonObject = jsonObject;
    }

    /**
     * Gets the processed data contained within this argument, which has a Java type appropriate to the Stratum type represented by this argument.
     *
     * @return The processed data
     */
    public String getData() {
        return getSerialized();
    }


    /**
     * Gets the serialized version of this string which could be used to create an identical copy of this object.
     * @return The original data "sent over the wire" used to create this argument, or the serialized version created for sending over the wire.
     */
    @Override
    public String getSerialized() {
        return getJsonArray().toString();
    }

    /**
     * Get the corresponding Stratum type which this class represents
     * @return The corresponding Stratum type represented by this argument implementation
     */
    @Override
    public StratumType getType() {
        return type;
    }

    /**
     * Gets the string representation of this argument's data: passthrough to .toString() for the underlying datatype
     * of the processed data (or the equivalent of the autoboxed version's toString if the processed data is a primitive).
     * @return String representation of the data represented by this argument
     */
    @Override
    public String toString() {
        return getSerialized();
    }

    public JsonArray getJsonArray() {
        JsonArray params = new JsonArray();
        params.add(errorCode.code);
        params.add(message);
        if (jsonObject == null) {
            params.add(JsonNull.INSTANCE);
        } else {
            params.add(jsonObject);
        }

        return params;
    }
}
