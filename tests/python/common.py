# This Source Code Form is subject to the terms of the MIT License.
#
# Copyright (c) 2026 Draegerwerk AG & Co. KGaA.
# SPDX-License-Identifier: MIT

"""Shared utilities for grpc_client and grpc_server."""

import json
import pathlib

from google.protobuf import json_format

_NO_EXPECTED = '_not_present'

DEFAULT_TEST_DATA_PATH = (pathlib.Path(__file__).resolve().parent.parent
                          / 'java' / 'src' / 'test' / 'resources' / 'integration_scenarios.json')


def _load(testdata_path):
    """Parse the scenarios JSON and return {rpcCall: raw_expected} for every scenario."""
    with open(testdata_path, 'r', encoding='utf-8') as f:
        data = json.load(f)
    cases = {}
    for type_name, type_scenarios in data.items():
        for scenario in type_scenarios:
            cases[f"{type_name}_{scenario['suffix']}"] = scenario.get('expected')
    return cases


def _build_json(rpc_call, raw):
    """Build proto3 JSON; omits expected for _not_present, sets null for _null."""
    if rpc_call.endswith(_NO_EXPECTED):
        return json.dumps({'rpcCall': rpc_call})
    if rpc_call.endswith('_null'):
        return json.dumps({'rpcCall': rpc_call, 'expected': None})
    return json.dumps({'rpcCall': rpc_call, 'expected': raw})


def get_expected_response_and_merge(cases, rpc_call, msg):
    """Resolve the next rpcCall in rpc_call's type group (wraps around) and parse its scenario into msg."""
    prefix = rpc_call.split('_')[0] + '_'
    group = [k for k in cases if k.startswith(prefix)]
    idx = group.index(rpc_call) if rpc_call in group else -1
    assert idx >= 0, f"No next key found for {rpc_call}, this should not happen."
    next_key = group[(idx + 1) % len(group)]
    return json_format.Parse(_build_json(next_key, cases[next_key]), msg)
