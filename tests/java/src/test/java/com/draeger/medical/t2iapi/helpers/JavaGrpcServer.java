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
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class JavaGrpcServer {

    private final Server server;

    /*
       Start the integration gRPC server and validate received data.
    */
    public JavaGrpcServer(int port, Path testdataPath, List<String> validationErrors) throws IOException {
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
           Check received against the expected scenario return error on missmatch.
        */
        private void validate(String rpcCall, Message received) {
            if (!cases.containsKey(rpcCall)) {
                validationErrors.add("unknown rpcCall: '" + rpcCall + "'");
                return;
            }
            try {
                var builder = received.newBuilderForType();
                JsonFormat.parser().merge(CommonFunctions.buildItemJson(rpcCall, cases.get(rpcCall)), builder);
                Message expected = builder.build();
                if (!received.equals(expected)) {
                    validationErrors.add("Mismatch for '" + rpcCall + "':\n"
                            + "  expected: " + expected + "\n"
                            + "  received: " + received);
                }
            } catch (Exception e) {
                validationErrors.add("Parse error for '" + rpcCall + "': " + e.getMessage());
            }
        }

        /*
           Merge the next scenario into builder.
        */
        private void buildResponse(String rpcCall, Message.Builder builder) {
            try {
                CommonFunctions.getExpectedResponseAndMerge(cases, rpcCall, builder);
            } catch (InvalidProtocolBufferException e) {
                validationErrors.add("Parse error building next response for '" + rpcCall + "': " + e.getMessage());
            }
        }

        @Override
        public void testString(StringCase received, StreamObserver<StringCase> responseObserver) {
            validate(received.getRpcCall(), received);
            var b = StringCase.newBuilder();
            buildResponse(received.getRpcCall(), b);
            responseObserver.onNext(b.build());
            responseObserver.onCompleted();
        }

        @Override
        public void testBool(BoolCase received, StreamObserver<BoolCase> responseObserver) {
            validate(received.getRpcCall(), received);
            var b = BoolCase.newBuilder();
            buildResponse(received.getRpcCall(), b);
            responseObserver.onNext(b.build());
            responseObserver.onCompleted();
        }

        @Override
        public void testUint32(Uint32Case received, StreamObserver<Uint32Case> responseObserver) {
            validate(received.getRpcCall(), received);
            var b = Uint32Case.newBuilder();
            buildResponse(received.getRpcCall(), b);
            responseObserver.onNext(b.build());
            responseObserver.onCompleted();
        }

        @Override
        public void testEnum(EnumCase received, StreamObserver<EnumCase> responseObserver) {
            validate(received.getRpcCall(), received);
            var b = EnumCase.newBuilder();
            buildResponse(received.getRpcCall(), b);
            responseObserver.onNext(b.build());
            responseObserver.onCompleted();
        }

        @Override
        public void testRepeatedString(RepeatedStringCase received, StreamObserver<RepeatedStringCase> responseObserver) {
            validate(received.getRpcCall(), received);
            var b = RepeatedStringCase.newBuilder();
            buildResponse(received.getRpcCall(), b);
            responseObserver.onNext(b.build());
            responseObserver.onCompleted();
        }

        @Override
        public void testRepeatedEnum(RepeatedEnumCase received, StreamObserver<RepeatedEnumCase> responseObserver) {
            validate(received.getRpcCall(), received);
            var b = RepeatedEnumCase.newBuilder();
            buildResponse(received.getRpcCall(), b);
            responseObserver.onNext(b.build());
            responseObserver.onCompleted();
        }

        @Override
        public void testRepeatedMessage(RepeatedMessageCase received, StreamObserver<RepeatedMessageCase> responseObserver) {
            validate(received.getRpcCall(), received);
            var b = RepeatedMessageCase.newBuilder();
            buildResponse(received.getRpcCall(), b);
            responseObserver.onNext(b.build());
            responseObserver.onCompleted();
        }

        @Override
        public void testMessage(MessageCase received, StreamObserver<MessageCase> responseObserver) {
            validate(received.getRpcCall(), received);
            var b = MessageCase.newBuilder();
            buildResponse(received.getRpcCall(), b);
            responseObserver.onNext(b.build());
            responseObserver.onCompleted();
        }

        @Override
        public void testOptionalString(OptionalStringCase received, StreamObserver<OptionalStringCase> responseObserver) {
            validate(received.getRpcCall(), received);
            var b = OptionalStringCase.newBuilder();
            buildResponse(received.getRpcCall(), b);
            responseObserver.onNext(b.build());
            responseObserver.onCompleted();
        }

        @Override
        public void testOptionalUint64(OptionalUint64Case received, StreamObserver<OptionalUint64Case> responseObserver) {
            validate(received.getRpcCall(), received);
            var b = OptionalUint64Case.newBuilder();
            buildResponse(received.getRpcCall(), b);
            responseObserver.onNext(b.build());
            responseObserver.onCompleted();
        }

        @Override
        public void testDuration(DurationCase received, StreamObserver<DurationCase> responseObserver) {
            validate(received.getRpcCall(), received);
            var b = DurationCase.newBuilder();
            buildResponse(received.getRpcCall(), b);
            responseObserver.onNext(b.build());
            responseObserver.onCompleted();
        }
    }
}