/*
This Source Code Form is subject to the terms of the MIT License.
Copyright (c) 2026 Draegerwerk AG & Co. KGaA.

SPDX-License-Identifier: MIT
*/

package com.draeger.medical.t2iapi.helpers;

import com.draeger.medical.t2iapi.integration.IntegrationServiceGrpc;
import com.draeger.medical.t2iapi.integration.IntegrationServiceProto.BoolCase;
import com.draeger.medical.t2iapi.integration.IntegrationServiceProto.DurationCase;
import com.draeger.medical.t2iapi.integration.IntegrationServiceProto.EnumCase;
import com.draeger.medical.t2iapi.integration.IntegrationServiceProto.MessageCase;
import com.draeger.medical.t2iapi.integration.IntegrationServiceProto.OptionalStringCase;
import com.draeger.medical.t2iapi.integration.IntegrationServiceProto.OptionalUint64Case;
import com.draeger.medical.t2iapi.integration.IntegrationServiceProto.RepeatedEnumCase;
import com.draeger.medical.t2iapi.integration.IntegrationServiceProto.RepeatedMessageCase;
import com.draeger.medical.t2iapi.integration.IntegrationServiceProto.RepeatedStringCase;
import com.draeger.medical.t2iapi.integration.IntegrationServiceProto.StringCase;
import com.draeger.medical.t2iapi.integration.IntegrationServiceProto.Uint32Case;
import com.google.gson.JsonElement;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.Map;

public class JavaGrpcClient {

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: JavaIntegrationClient <server_address> <testdata_path>");
            System.exit(1);
        }
        run(args[0], args[1]);
    }

    public static void run(String serverAddress, String testdataPath) throws Exception {
        Map<String, JsonElement> cases = CommonFunctions.load(testdataPath);

        String[] parts = serverAddress.split(":", 2);
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);

        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        try {
            IntegrationServiceGrpc.IntegrationServiceBlockingStub stub =
                    IntegrationServiceGrpc.newBlockingStub(channel);
            for (Map.Entry<String, JsonElement> entry : cases.entrySet()) {
                send(stub, entry.getKey(), CommonFunctions.buildItemJson(entry.getKey(), entry.getValue()));
            }
        } finally {
            channel.shutdown();
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Message> T parse(String json, T.Builder builder) throws Exception {
        JsonFormat.parser().merge(json, builder);
        return (T) builder.build();
    }

    /*
        Sends the correct RPC based on the rpcCall prefix.
    */
    private static void send(
            IntegrationServiceGrpc.IntegrationServiceBlockingStub stub,
            String rpcCall,
            String itemJson) throws Exception {
        if (rpcCall.startsWith("TestRepeatedString")) {
            stub.testRepeatedString(parse(itemJson, RepeatedStringCase.newBuilder()));
        } else if (rpcCall.startsWith("TestRepeatedEnum")) {
            stub.testRepeatedEnum(parse(itemJson, RepeatedEnumCase.newBuilder()));
        } else if (rpcCall.startsWith("TestRepeatedMessage")) {
            stub.testRepeatedMessage(parse(itemJson, RepeatedMessageCase.newBuilder()));
        } else if (rpcCall.startsWith("TestOptionalString")) {
            stub.testOptionalString(parse(itemJson, OptionalStringCase.newBuilder()));
        } else if (rpcCall.startsWith("TestOptionalUint64")) {
            stub.testOptionalUint64(parse(itemJson, OptionalUint64Case.newBuilder()));
        } else if (rpcCall.startsWith("TestString")) {
            stub.testString(parse(itemJson, StringCase.newBuilder()));
        } else if (rpcCall.startsWith("TestBool")) {
            stub.testBool(parse(itemJson, BoolCase.newBuilder()));
        } else if (rpcCall.startsWith("TestUint32")) {
            stub.testUint32(parse(itemJson, Uint32Case.newBuilder()));
        } else if (rpcCall.startsWith("TestEnum")) {
            stub.testEnum(parse(itemJson, EnumCase.newBuilder()));
        } else if (rpcCall.startsWith("TestMessage")) {
            stub.testMessage(parse(itemJson, MessageCase.newBuilder()));
        } else if (rpcCall.startsWith("TestDuration")) {
            stub.testDuration(parse(itemJson, DurationCase.newBuilder()));
        } else {
            throw new IllegalArgumentException("No RPC mapped for rpcCall: '" + rpcCall + "'");
        }
    }
}