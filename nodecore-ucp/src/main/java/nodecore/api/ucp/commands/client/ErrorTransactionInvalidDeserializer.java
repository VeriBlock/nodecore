// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.api.ucp.commands.client;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import nodecore.api.ucp.commands.DeserializationUtility;
import nodecore.api.ucp.commands.UCPCommand;

import java.lang.reflect.Type;
public class ErrorTransactionInvalidDeserializer implements JsonDeserializer<ErrorTransactionInvalid> {

    @Override
    public ErrorTransactionInvalid deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        try {
            return new ErrorTransactionInvalid(DeserializationUtility.parseArguments(json, UCPCommand.Command.ERROR_TRANSACTION_INVALID));
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to parse " + json + " in " + getClass().getCanonicalName() + "!");
        }
    }
}
