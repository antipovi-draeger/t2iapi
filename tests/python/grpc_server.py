# This Source Code Form is subject to the terms of the MIT License.
#
# Copyright (c) 2026 Draegerwerk AG & Co. KGaA.
# SPDX-License-Identifier: MIT

"""
Grpc server for cross-language integration testing.

Loads test scenarios from a JSON file into {rpcCall: expected_value} at startup.
For each incoming RPC the expected proto is reconstructed from the stored value
and compared against the received message. Prints any mismatch.

Intended to be invoked by PythonClientJavaServerTest.
"""

from concurrent import futures

import grpc
import json
from google.protobuf import empty_pb2
from google.protobuf import json_format

from common import _build_json
from t2iapi.integration import service_pb2_grpc


class IntegrationServiceServicer(service_pb2_grpc.IntegrationServiceServicer):
    """Validates incoming requests against expected data loaded from JSON."""

    def __init__(self, testdata_path, validation_errors):
        self._validation_errors = validation_errors
        with open(testdata_path, 'r', encoding='utf-8') as f:
            items = json.load(f)
        self._cases = {item['rpcCall']: item.get('expected') for item in items}

    def _validate(self, received):
        received_rpc_call = received.rpc_call

        if received_rpc_call not in self._cases:
            self._validation_errors.append(f"unknown rpcCall: '{received_rpc_call}'")
            return

        raw = self._cases[received_rpc_call]
        expected = json_format.Parse(_build_json(received_rpc_call, raw), type(received)())

        if received != expected:
            self._validation_errors.append(
                f"Mismatch for rpc call '{received_rpc_call}':\n"
                f"expected: {str(expected).rstrip()}\n"
                f"received: {str(received)}"
            )

    def TestString(self, request, context):
        self._validate(request)
        return empty_pb2.Empty()

    def TestBool(self, request, context):
        self._validate(request)
        return empty_pb2.Empty()

    def TestUint32(self, request, context):
        self._validate(request)
        return empty_pb2.Empty()

    def TestEnum(self, request, context):
        self._validate(request)
        return empty_pb2.Empty()

    def TestRepeatedString(self, request, context):
        self._validate(request)
        return empty_pb2.Empty()

    def TestRepeatedEnum(self, request, context):
        self._validate(request)
        return empty_pb2.Empty()

    def TestRepeatedMessage(self, request, context):
        self._validate(request)
        return empty_pb2.Empty()

    def TestMessage(self, request, context):
        self._validate(request)
        return empty_pb2.Empty()

    def TestOptionalString(self, request, context):
        self._validate(request)
        return empty_pb2.Empty()

    def TestOptionalUint64(self, request, context):
        self._validate(request)
        return empty_pb2.Empty()

    def TestDuration(self, request, context):
        self._validate(request)
        return empty_pb2.Empty()


def start_server(testdata_path, validation_errors):
    """Start the integration server on a random port. Returns (server, port)."""
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=4))
    service_pb2_grpc.add_IntegrationServiceServicer_to_server(
        IntegrationServiceServicer(testdata_path, validation_errors),
        server,
    )
    port = server.add_insecure_port('localhost:0')
    server.start()
    return server, port


if __name__ == '__main__':
    import sys

    _validation_errors = []
    _server, _port = start_server(sys.argv[1], _validation_errors)
    print(_port, flush=True)
    sys.stdin.read()
    _server.stop(grace=5).wait()
    for _err in _validation_errors:
        print(_err, file=sys.stderr)
    sys.exit(1 if _validation_errors else 0)
