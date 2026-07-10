# This Source Code Form is subject to the terms of the MIT License.
#
# Copyright (c) 2026 Draegerwerk AG & Co. KGaA.
# SPDX-License-Identifier: MIT

"""Shared utilities for grpc_client and grpc_server."""

import json

_NO_EXPECTED = '_not_present'


def _build_json(rpc_call, raw):
    """Build proto3 JSON for one test case.

    Omits the 'expected' field for _not_present cases, sets it to null for
    _null cases, and uses the raw value otherwise.
    """
    if rpc_call.endswith(_NO_EXPECTED):
        return json.dumps({'rpcCall': rpc_call})
    if rpc_call.endswith('_null'):
        return json.dumps({'rpcCall': rpc_call, 'expected': None})
    return json.dumps({'rpcCall': rpc_call, 'expected': raw})
