// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.api.ucp.commands;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import nodecore.api.ucp.arguments.UCPArgument;
import org.veriblock.core.types.Pair;

import java.util.ArrayList;

public class DeserializationUtility {

    public static UCPArgument[] parseArguments(JsonElement json, UCPCommand.Command command) {
        ArrayList<Pair<String, UCPArgument.UCPType>> pattern = command.getPattern();
        JsonObject arguments = json.getAsJsonObject();
        try {
            UCPArgument[] argumentsToConstructor = new UCPArgument[pattern.size()];
            for (int i = 0; i < pattern.size(); i++) {
                UCPArgument.UCPType argumentType = pattern.get(i).getSecond();
                Class<? extends Object> argumentNativeType = argumentType.getInternalType();

                String keyString = pattern.get(i).getFirst();
                JsonPrimitive data = arguments.getAsJsonObject(keyString).getAsJsonPrimitive("data");
                if (data == null) {
                    throw new IllegalArgumentException("parseArguments cannot be called with a JsonElement without a \"data\" section!");
                }
                if (argumentNativeType == Integer.class) {
                    int processedData = data.getAsInt();
                    argumentsToConstructor[i] = argumentType.getImplementingClass().getConstructor(argumentNativeType).newInstance(processedData);
                } else if (argumentNativeType == Long.class) {
                    long processedData = data.getAsLong();
                    argumentsToConstructor[i] = argumentType.getImplementingClass().getConstructor(argumentNativeType).newInstance(processedData);
                } else if (argumentNativeType == String.class) {
                    String processedData = data.getAsString();
                    argumentsToConstructor[i] = argumentType.getImplementingClass().getConstructor(argumentNativeType).newInstance(processedData);
                } else {
                    throw new IllegalArgumentException("An unknown type was encountered when attempting to read the key " + keyString + "!");
                }
            }

            return argumentsToConstructor;
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to deserialize " + arguments + "!");
        }
    }
}
