/*
This Source Code Form is subject to the terms of the MIT License.
Copyright (c) 2026 Draegerwerk AG & Co. KGaA.

SPDX-License-Identifier: MIT
*/

package com.draeger.medical.t2iapi.helpers;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class CommonFunctions {

    public static final Path TEST_DATA_PATH = Path.of("src/test/resources/integration_scenarios.json")
            .toAbsolutePath().normalize();

    private CommonFunctions() {
    }

    /*
       Read the JSON and index each scenario by its rpcCall (TypeName_suffix).
    */
    static Map<String, JsonElement> load(Path testDataPath) throws IOException {
        String json = Files.readString(testDataPath);
        Map<String, JsonElement> cases = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> type : JsonParser.parseString(json).getAsJsonObject().entrySet()) {
            for (JsonElement element : type.getValue().getAsJsonArray()) {
                JsonObject scenario = element.getAsJsonObject();
                String rpcCall = type.getKey() + "_" + scenario.get("suffix").getAsString();
                cases.put(rpcCall, scenario.has("expected") ? scenario.get("expected") : JsonNull.INSTANCE);
            }
        }
        return cases;
    }

    /*
       Resolve the next rpcCall in rpcCall's type group and merge its scenario into builder.
    */
    static void getExpectedResponseAndMerge(Map<String, JsonElement> cases, String rpcCall, Message.Builder builder)
            throws InvalidProtocolBufferException {
        String prefix = rpcCall.split("_")[0] + "_";
        var group = cases.keySet().stream().filter(k -> k.startsWith(prefix)).toList();
        int idx = group.indexOf(rpcCall);
        if (idx < 0) {
            throw new IllegalStateException("No next key found for " + rpcCall + ", this should not happen.");
        }
        var nextKey = group.get((idx + 1) % group.size());
        JsonFormat.parser().merge(buildItemJson(nextKey, cases.get(nextKey)), builder);
    }

    /*
       Build the test case JSON, omit expected for _not_present cases and set it to null for _null cases.
    */
    static String buildItemJson(String rpcCall, JsonElement raw) {
        JsonObject obj = new JsonObject();
        obj.addProperty("rpcCall", rpcCall);
        if (!rpcCall.endsWith("_not_present")) {
            obj.add("expected", rpcCall.endsWith("_null") ? JsonNull.INSTANCE : raw);
        }
        return obj.toString();
    }
}
