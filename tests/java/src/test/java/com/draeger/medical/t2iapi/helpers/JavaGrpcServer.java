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
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class JavaGrpcServer {

    private final Server server;

    public JavaGrpcServer(int port, String testdataPath, List<String> validationErrors) throws IOException {
        Map<String, JsonElement> cases = CommonFunctions.load(testdataPath);
        server = ServerBuilder.forPort(port)
                .addService(new IntegrationServiceImpl(cases, validationErrors))
                .build()
                .start();
    }

    public int getPort() {
        return server.getPort();
    }

    public void stop() throws InterruptedException {
        server.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    private static class IntegrationServiceImpl
            extends IntegrationServiceGrpc.IntegrationServiceImplBase {

        private final Map<String, JsonElement> cases;
        private final List<String> validationErrors;

        IntegrationServiceImpl(Map<String, JsonElement> cases, List<String> validationErrors) {
            this.cases = cases;
            this.validationErrors = validationErrors;
        }

        /*
            Reconstructs the expected proto from stored JSON and records any mismatch.
        */
        @SuppressWarnings("unchecked")
        private <T extends Message> void validate(String rpcCall, T received, T.Builder builder) {
            if (!cases.containsKey(rpcCall)) {
                validationErrors.add("unknown rpcCall: '" + rpcCall + "'");
                return;
            }
            try {
                JsonFormat.parser().merge(CommonFunctions.buildItemJson(rpcCall, cases.get(rpcCall)), builder);
                T expected = (T) builder.build();
                if (!received.equals(expected)) {
                    validationErrors.add("Mismatch for '" + rpcCall + "':\n"
                            + "  expected: " + expected + "\n"
                            + "  received: " + received);
                }
            } catch (Exception e) {
                validationErrors.add("Parse error for '" + rpcCall + "': " + e.getMessage());
            }
        }

        @Override
        public void testString(StringCase received, StreamObserver<Empty> responseObserver) {
            validate(received.getRpcCall(), received, StringCase.newBuilder());
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        }

        @Override
        public void testBool(BoolCase received, StreamObserver<Empty> responseObserver) {
            validate(received.getRpcCall(), received, BoolCase.newBuilder());
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        }

        @Override
        public void testUint32(Uint32Case received, StreamObserver<Empty> responseObserver) {
            validate(received.getRpcCall(), received, Uint32Case.newBuilder());
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        }

        @Override
        public void testEnum(EnumCase received, StreamObserver<Empty> responseObserver) {
            validate(received.getRpcCall(), received, EnumCase.newBuilder());
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        }

        @Override
        public void testRepeatedString(RepeatedStringCase received, StreamObserver<Empty> responseObserver) {
            validate(received.getRpcCall(), received, RepeatedStringCase.newBuilder());
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        }

        @Override
        public void testRepeatedEnum(RepeatedEnumCase received, StreamObserver<Empty> responseObserver) {
            validate(received.getRpcCall(), received, RepeatedEnumCase.newBuilder());
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        }

        @Override
        public void testRepeatedMessage(RepeatedMessageCase received, StreamObserver<Empty> responseObserver) {
            validate(received.getRpcCall(), received, RepeatedMessageCase.newBuilder());
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        }

        @Override
        public void testMessage(MessageCase received, StreamObserver<Empty> responseObserver) {
            validate(received.getRpcCall(), received, MessageCase.newBuilder());
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        }

        @Override
        public void testOptionalString(OptionalStringCase received, StreamObserver<Empty> responseObserver) {
            validate(received.getRpcCall(), received, OptionalStringCase.newBuilder());
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        }

        @Override
        public void testOptionalUint64(OptionalUint64Case received, StreamObserver<Empty> responseObserver) {
            validate(received.getRpcCall(), received, OptionalUint64Case.newBuilder());
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        }

        @Override
        public void testDuration(DurationCase received, StreamObserver<Empty> responseObserver) {
            validate(received.getRpcCall(), received, DurationCase.newBuilder());
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        }
    }
}