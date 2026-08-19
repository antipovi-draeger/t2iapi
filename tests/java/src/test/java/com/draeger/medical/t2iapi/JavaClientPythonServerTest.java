/*
This Source Code Form is subject to the terms of the MIT License.
Copyright (c) 2026 Draegerwerk AG & Co. KGaA.

SPDX-License-Identifier: MIT
*/

package com.draeger.medical.t2iapi;

import com.draeger.medical.t2iapi.helpers.CommonFunctions;
import com.draeger.medical.t2iapi.helpers.JavaGrpcClient;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Java client -> Python server integration test.

 * Spawns the Python grpc server as a subprocess, runs the Java client in-process against it, then asserts the
 * server exited cleanly with no validation errors.
 *
 * For running locally add VM options add path to your python's venv e.g.
 * -Dpython.executable=/venv/Scripts/python.exe
 */
class JavaClientPythonServerTest {

    private static final String PYTHON_GRPC_SERVER_PY = Path.of("../python/grpc_server.py")
            .toAbsolutePath().normalize().toString();

    @Test
    void javaClientIntegrationTest() throws Exception {
        String pythonExe = System.getProperty("python.executable");
        assertNotNull(pythonExe, "python.executable system property must be set");

        Process serverProcess =
                new ProcessBuilder(pythonExe, PYTHON_GRPC_SERVER_PY, CommonFunctions.TEST_DATA_PATH.toString())
                        .start();

        List<String> stderrLines = new ArrayList<>();
        Thread stderrReader = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(serverProcess.getErrorStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    stderrLines.add(line);
                }
            } catch (Exception ignored) {
            }
        });
        stderrReader.setDaemon(true);
        stderrReader.start();

        try (BufferedReader stdout = new BufferedReader(
                new InputStreamReader(serverProcess.getInputStream()))) {
            String portLine = stdout.readLine();

            assertNotNull(portLine, "Python server did not print a port - stderr:\n"
                    + String.join("\n", stderrLines));

            int port = Integer.parseInt(portLine.trim());
            JavaGrpcClient.run("localhost:" + port, CommonFunctions.TEST_DATA_PATH);
        }

        serverProcess.getOutputStream().close();
        boolean finished = serverProcess.waitFor(30, TimeUnit.SECONDS);
        stderrReader.join(5000);

        assertTrue(finished, "Python server subprocess timed out after 30 seconds");
        assertEquals(0, serverProcess.exitValue(),
                "Python server validation errors:\n" + String.join("\n", stderrLines));
    }
}
