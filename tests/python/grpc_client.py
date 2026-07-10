# This Source Code Form is subject to the terms of the MIT License.
#
# Copyright (c) 2026 Draegerwerk AG & Co. KGaA.
# SPDX-License-Identifier: MIT

"""
Grpc client for integration testing.

Reads test scenarios from a JSON file into {rpcCall: expected_value}, dispatches
each case to the matching RPC based on the rpcCall prefix, and exits 0.
No assertions are performed here; the server owns the validation.

Intended to be invoked by JavaClientPythonServerTest.
"""

import json
import sys

import grpc
from google.protobuf import json_format

from common import _build_json
from t2iapi.integration import service_pb2
from t2iapi.integration import service_pb2_grpc


def _send(stub, rpc_call, item_json):
    if rpc_call.startswith('TestRepeatedString'):
        stub.TestRepeatedString(json_format.Parse(item_json, service_pb2.RepeatedStringCase()))
    elif rpc_call.startswith('TestRepeatedEnum'):
        stub.TestRepeatedEnum(json_format.Parse(item_json, service_pb2.RepeatedEnumCase()))
    elif rpc_call.startswith('TestRepeatedMessage'):
        stub.TestRepeatedMessage(json_format.Parse(item_json, service_pb2.RepeatedMessageCase()))
    elif rpc_call.startswith('TestOptionalString'):
        stub.TestOptionalString(json_format.Parse(item_json, service_pb2.OptionalStringCase()))
    elif rpc_call.startswith('TestOptionalUint64'):
        stub.TestOptionalUint64(json_format.Parse(item_json, service_pb2.OptionalUint64Case()))
    elif rpc_call.startswith('TestString'):
        stub.TestString(json_format.Parse(item_json, service_pb2.StringCase()))
    elif rpc_call.startswith('TestBool'):
        stub.TestBool(json_format.Parse(item_json, service_pb2.BoolCase()))
    elif rpc_call.startswith('TestUint32'):
        stub.TestUint32(json_format.Parse(item_json, service_pb2.Uint32Case()))
    elif rpc_call.startswith('TestEnum'):
        stub.TestEnum(json_format.Parse(item_json, service_pb2.EnumCase()))
    elif rpc_call.startswith('TestMessage'):
        stub.TestMessage(json_format.Parse(item_json, service_pb2.MessageCase()))
    elif rpc_call.startswith('TestDuration'):
        stub.TestDuration(json_format.Parse(item_json, service_pb2.DurationCase()))
    else:
        raise ValueError(f"No RPC mapped for rpcCall: '{rpc_call}'")


def run(server_address, testdata_path):
    with open(testdata_path, 'r', encoding='utf-8') as f:
        items = json.load(f)
    cases = {item['rpcCall']: item.get('expected') for item in items}

    with grpc.insecure_channel(server_address) as channel:
        stub = service_pb2_grpc.IntegrationServiceStub(channel)
        for rpc_call, raw in cases.items():
            _send(stub, rpc_call, _build_json(rpc_call, raw))


if __name__ == '__main__':
    if len(sys.argv) != 3:
        print(f"Usage: {sys.argv[0]} <server_address> <testdata_path>", file=sys.stderr)
        sys.exit(1)
    run(sys.argv[1], sys.argv[2])