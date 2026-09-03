# This Source Code Form is subject to the terms of the MIT License.
#
# Copyright (c) 2026 Draegerwerk AG & Co. KGaA.
# SPDX-License-Identifier: MIT

"""Shared utilities for grpc_client and grpc_server."""

import json
import pathlib

from google.protobuf import json_format

DEFAULT_TEST_DATA_PATH = (pathlib.Path(__file__).resolve().parent.parent
                          / 'java' / 'src' / 'test' / 'resources' / 'integration_scenarios.json')


def load_testdata(testdata_path):
    """Read the JSON and index each scenario by its rpcCall."""
    with open(testdata_path, 'r', encoding='utf-8') as f:
        data = json.load(f)
    cases = {}
    for type_name, type_scenarios in data.items():
        for scenario in type_scenarios:
            cases[f"{type_name}_{scenario['suffix']}"] = scenario
    return cases


def build_json(rpc_call, scenario):
    """Build proto3 JSON. Omits expected if the key is absent in the scenario."""
    if 'expected' not in scenario:
        return json.dumps({'rpcCall': rpc_call})
    return json.dumps({'rpcCall': rpc_call, 'expected': scenario['expected']})


def get_expected_response_and_merge(cases, rpc_call, msg):
    """Resolve the next rpcCall in rpc_call's type group (wraps around) and parse its scenario into msg."""
    prefix = rpc_call.split('_')[0] + '_'
    group = [k for k in cases if k.startswith(prefix)]
    idx = group.index(rpc_call) if rpc_call in group else -1
    assert idx >= 0, f"No next key found for {rpc_call}, this should not happen."
    next_key = group[(idx + 1) % len(group)]
    return json_format.Parse(build_json(next_key, cases[next_key]), msg)
