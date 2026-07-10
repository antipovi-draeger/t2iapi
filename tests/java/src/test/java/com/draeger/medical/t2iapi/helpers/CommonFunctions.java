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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

class CommonFunctions {

    private CommonFunctions() {}

    /*
        Reads the JSON test-data array and indexes each entry by its rpcCall value.
    */
    static Map<String, JsonElement> load(String testDataPath) throws IOException {
        String json = Files.readString(Path.of(testDataPath));
        Map<String, JsonElement> cases = new LinkedHashMap<>();
        for (JsonElement element : JsonParser.parseString(json).getAsJsonArray()) {
            JsonObject obj = element.getAsJsonObject();
            String rpcCall = obj.get("rpcCall").getAsString();
            cases.put(rpcCall, obj.has("expected") ? obj.get("expected") : JsonNull.INSTANCE);
        }
        return cases;
    }

    /*
        Builds the test case JSON, omits expected for _not_present cases and sets it to null for _null cases.
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
