package org.veriblock.extensions.stratumapi.commands;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.veriblock.core.types.Pair;
import org.veriblock.extensions.stratumapi.arguments.StratumArgument;

import java.util.ArrayList;

public class DeserializationUtility {

    public static StratumArgument[] parseArguments(JsonElement json, StratumCommand.Command command) {
        ArrayList<Pair<String, StratumArgument.StratumType>> pattern = command.getPattern();
        JsonObject arguments = json.getAsJsonObject();
        try {
            StratumArgument[] argumentsToConstructor = new StratumArgument[pattern.size()];
            for (int i = 0; i < pattern.size(); i++) {
                StratumArgument.StratumType argumentType = pattern.get(i).getSecond();
                Class<? extends Object> argumentNativeType = argumentType.getInternalType();

                String keyString = pattern.get(i).getFirst();
                JsonPrimitive data = arguments.getAsJsonObject(keyString).getAsJsonPrimitive("data");
                if (data == null) {
                    throw new IllegalArgumentException("parseArguments cannot be called with a JsonElement without a \"data\" section!");
                }
                if (argumentNativeType == Short.class) {
                    int processedData = data.getAsShort();
                    argumentsToConstructor[i] = argumentType.getImplementingClass().getConstructor(argumentNativeType).newInstance(processedData);
                } else if (argumentNativeType == Integer.class) {
                    int processedData = data.getAsInt();
                    argumentsToConstructor[i] = argumentType.getImplementingClass().getConstructor(argumentNativeType).newInstance(processedData);
                } else if (argumentNativeType == Long.class) {
                    long processedData = data.getAsLong();
                    argumentsToConstructor[i] = argumentType.getImplementingClass().getConstructor(argumentNativeType).newInstance(processedData);
                }  else if (argumentNativeType == Double.class) {
                    double processedData = data.getAsDouble();
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
