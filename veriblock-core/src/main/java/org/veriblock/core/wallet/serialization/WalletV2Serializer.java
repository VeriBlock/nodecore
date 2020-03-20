// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.wallet.serialization;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Type;
import java.util.Base64;

public class WalletV2Serializer implements WalletSerializer {
    private static final Logger logger = LoggerFactory.getLogger(WalletV2Serializer.class);

    @Override
    public StoredWallet read(InputStream inputStream) {
        try (InputStreamReader inReader = new InputStreamReader(inputStream, "UTF-8");
             JsonReader reader = new JsonReader(inReader)) {

            Gson gson = new GsonBuilder().registerTypeHierarchyAdapter(byte[].class,
                    new ByteArrayToBase64TypeAdapter()).create();

            return gson.fromJson(reader, StoredWallet.class);
        } catch (IOException e) {
            logger.warn("Unable to read wallet file", e);
            return null;
        } catch (JsonSyntaxException e) {
            logger.warn("Wallet file is not in the format expected by the serializer");
            return null;
        }
    }

    @Override
    public void write(StoredWallet wallet, OutputStream outputStream) throws IOException {
        try (OutputStreamWriter outWriter = new OutputStreamWriter(outputStream, "UTF-8");
             JsonWriter writer = new JsonWriter(outWriter)) {
            Gson gson = new GsonBuilder().registerTypeHierarchyAdapter(byte[].class,
                    new ByteArrayToBase64TypeAdapter()).create();
            gson.toJson(wallet, StoredWallet.class, writer);
        }
    }

    private static class ByteArrayToBase64TypeAdapter implements JsonSerializer<byte[]>, JsonDeserializer<byte[]> {
        public byte[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return Base64.getDecoder().decode(json.getAsString());
        }

        public JsonElement serialize(byte[] src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(Base64.getEncoder().encodeToString(src));
        }
    }
}
