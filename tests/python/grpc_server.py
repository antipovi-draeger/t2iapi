# This Source Code Form is subject to the terms of the MIT License.
#
# Copyright (c) 2026 Draegerwerk AG & Co. KGaA.
# SPDX-License-Identifier: MIT

"""gRPC server for cross-language integration testing. Intended to be invoked by PythonClientJavaServerTest."""

from concurrent import futures

import grpc
from google.protobuf import json_format

from common import _build_json, get_expected_response_and_merge, _load
from t2iapi.integration import service_pb2_grpc


class IntegrationServiceServicer(service_pb2_grpc.IntegrationServiceServicer):
    """Validates received requests and returns the next scenario as the response."""

    def __init__(self, testdata_path, validation_errors):
        """Load scenarios from testdata_path, collect validation errors."""
        self._validation_errors = validation_errors
        self._cases = _load(testdata_path)

    def _validate(self, received):
        """Check received against the stored expected scenario, append any mismatch."""
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

    def _build_next_response(self, received):
        """Parse the next scenario into a new message instance."""
        try:
            return get_expected_response_and_merge(self._cases, received.rpc_call, type(received)())
        except Exception as e:
            self._validation_errors.append(f"Parse error building next response for '{received.rpc_call}': {e}")
            return type(received)()

    def TestString(self, request, context):
        self._validate(request)
        return self._build_next_response(request)

    def TestBool(self, request, context):
        self._validate(request)
        return self._build_next_response(request)

    def TestUint32(self, request, context):
        self._validate(request)
        return self._build_next_response(request)

    def TestEnum(self, request, context):
        self._validate(request)
        return self._build_next_response(request)

    def TestRepeatedString(self, request, context):
        self._validate(request)
        return self._build_next_response(request)

    def TestRepeatedEnum(self, request, context):
        self._validate(request)
        return self._build_next_response(request)

    def TestRepeatedMessage(self, request, context):
        self._validate(request)
        return self._build_next_response(request)

    def TestMessage(self, request, context):
        self._validate(request)
        return self._build_next_response(request)

    def TestOptionalString(self, request, context):
        self._validate(request)
        return self._build_next_response(request)

    def TestOptionalUint64(self, request, context):
        self._validate(request)
        return self._build_next_response(request)

    def TestDuration(self, request, context):
        self._validate(request)
        return self._build_next_response(request)


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
