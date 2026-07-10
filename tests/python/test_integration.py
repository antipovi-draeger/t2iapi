# This Source Code Form is subject to the terms of the MIT License.
#
# Copyright (c) 2025 Draegerwerk AG & Co. KGaA.
# SPDX-License-Identifier: MIT

"""
Integration test: Python server + Java client.

Running locally:
  1. Build the Java client JAR from tests/java: ./gradlew compatibilityClientJar
  2. Set the IDE working directory to tests/python
"""

import os
import subprocess
import unittest

from integration_server import start_server

_TESTDATA = os.path.normpath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'integration_scenarios.json')
)
_DEFAULT_JAR = os.path.normpath(
    os.path.join(
        os.path.dirname(os.path.abspath(__file__)),
        '..', 'java', 'build', 'libs', 't2iapitest-client-all.jar',
    )
)


class CompatibilityServerTest(unittest.TestCase):

    def test_java_client_compatibility(self):
        """Java client sends all test scenarios to the Python server; server validates received data."""
        java_client_jar = os.environ.get('T2IAPI_JAVA_CLIENT_JAR', _DEFAULT_JAR)

        validation_errors = []
        server, port = start_server(_TESTDATA, validation_errors)
        try:
            result = subprocess.run(
                ['java', '-jar', java_client_jar, f'localhost:{port}', _TESTDATA],
                timeout=30,
                capture_output=True,
                text=True,
            )
            if result.returncode != 0:
                self.fail(
                    f'Java client subprocess failed (exit {result.returncode}):\n'
                    f'stdout: {result.stdout}\nstderr: {result.stderr}'
                )
            self.assertFalse(
                validation_errors,
                f'Server validation errors:\n' + '\n'.join(validation_errors),
            )
        finally:
            server.stop(grace=None)


if __name__ == '__main__':
    unittest.main()
