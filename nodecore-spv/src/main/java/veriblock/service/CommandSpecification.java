// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package veriblock.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import nodecore.api.grpc.VeriBlockMessages;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class CommandSpecification {

    private final Supplier<Message.Builder> requestBuilderSupplier;
    private final Function<Message, Message> handler;

    public CommandSpecification(Supplier<Message.Builder> requestBuilderSupplier, Function<Message, Message> handler) {
        this.requestBuilderSupplier = requestBuilderSupplier;
        this.handler = handler;
    }

    public Message.Builder newRequestBuilder() {
        return requestBuilderSupplier.get();
    }

    public void translateInbound(Descriptors.Descriptor descriptor, JsonObject jsonObject) {
        traverseAndTranslate(descriptor, "params", jsonObject, true);
    }

    public void translateOutbound(Descriptors.Descriptor descriptor, JsonObject jsonObject) {
        traverseAndTranslate(descriptor, "result", jsonObject, false);
    }

    private void traverseAndTranslate(Descriptors.Descriptor descriptor, String path, JsonObject obj, boolean inbound) {
        List<Descriptors.FieldDescriptor> fields = descriptor.getFields();
        for (Descriptors.FieldDescriptor field : fields) {
            String jsonPath = path + "." + field.getJsonName();
            if (field.getType().name().equals("MESSAGE")) {
                traverseAndTranslate(field.getMessageType(), jsonPath, obj, inbound);
            } else {
                translate(obj, jsonPath, getTranslator(field.getOptions(), inbound));
            }
        }
    }

    private static Function<JsonElement, JsonElement> getTranslator(DescriptorProtos.FieldOptions options, boolean inbound) {
        if (options.getExtension(VeriBlockMessages.hexEncoded)) {
            return inbound ? Translators::hexToBase64 : Translators::base64ToHex;
        }
        if (options.getExtension(VeriBlockMessages.addressEncoded)) {
            return inbound ? Translators::arbitraryAddressTypeToBase64 : Translators::base64ToProperAddressType;
        }
        if (options.getExtension(VeriBlockMessages.asciiEncoded)) {
            return inbound ? Translators::asciiToBase64 : Translators::base64ToAscii;
        }
        if (options.getExtension(VeriBlockMessages.utf8Encoded)) {
            return inbound ? Translators::utf8ToBase64 : Translators::base64ToUtf8;
        }

        return Translators::noOp;
    }

    private void translate(JsonObject source, String path, Function<JsonElement, JsonElement> conversion) {
        translate(source, path.split("\\."), conversion);
    }

    private void translate(JsonObject source, String[] pathParts, Function<JsonElement, JsonElement> conversion) {
        if (pathParts.length == 0) {
            // Should never reach this point, but it is possible if we specify an incomplete path
            return;
        }
        String pathPart = pathParts[0];
        if (!source.has(pathPart)) {
            // Not found
            return;
        }
        // Pop the taken part
        String[] nextPathParts = Arrays.copyOfRange(pathParts, 1, pathParts.length);
        JsonElement element = source.get(pathPart);
        if (element.isJsonArray()) {
            // Recursive call for all inner elements
            JsonArray arrayElement = element.getAsJsonArray();
            for (int i = 0; i < arrayElement.size(); i++) {
                JsonElement subElement = arrayElement.get(i);
                if (subElement.isJsonPrimitive()) {
                    JsonElement converted = conversion.apply(subElement);
                    arrayElement.set(i, converted);
                } else {
                    translate(subElement.getAsJsonObject(), nextPathParts, conversion);
                }
            }
        } else if (element.isJsonObject()) {
            // Recursive call
            translate(element.getAsJsonObject(), nextPathParts, conversion);
        } else {
            // Conversion
            JsonElement converted = conversion.apply(element);
            source.remove(pathPart);
            source.add(pathPart, converted);
        }
    }

    public Message handle(Message request) {
        return handler.apply(request);
    }
}
