# Integration Tests

## Validating compatibility

If you implement t2iapi in a language other than Java or Python, or if you build the
package yourself using different versions of gRPC or protobuf than those released, we
strongly recommend verifying that your implementation correctly serializes and deserializes
all field types used by t2iapi. The repository provides reference client and server
implementations in Python together with test scenarios covering all field types used by t2iapi.

Generate stubs from `src/t2iapi/integration/service.proto`, then use the reference
client or server from `tests/python/` against your own implementation, with
`tests/java/src/test/resources/integration_scenarios.json` as test data:

```bash
# Run the reference client against your server
python grpc_client.py <your_server_address> <path/to/integration_scenarios.json>

# Run the reference server for your client to connect to
python grpc_server.py <path/to/integration_scenarios.json>
```

## Automated cross-language tests

The repository contains JUnit 5 tests that run the Java and Python implementations against each
other automatically. There are two test classes, each covering one direction:

| Test class | Client | Server |
|---|---|---|
| `JavaClientPythonServerTest` | Java | Python |
| `PythonClientJavaServerTest` | Python | Java |

Each test spawns the other language's component as a subprocess and sends every scenario from
`tests/java/src/test/resources/integration_scenarios.json` through it. The server always responds
with the next scenario in the same type group, which the client then validates. Both sides collect
validation errors independently; the test fails if either side reports any.

### Scenario file

The scenario file is a JSON object keyed by type name. Each type holds an ordered array of scenarios,
each with a `suffix` and an optional `expected` value. The full set of scenarios is in
[`tests/java/src/test/resources/integration_scenarios.json`](java/src/test/resources/integration_scenarios.json).

The suffix controls how the `expected` field is serialized before sending:

- **`_not_present`** — the `expected` key is omitted from the JSON entirely, producing the proto3 default.
- **`_null`** — the `expected` key is set to `null`. For wrapper types (`StringValue`, `UInt64Value`) this
  leaves the field unset; for scalar types the result is the proto3 default.
- **anything else** — the `expected` key carries the scenario value normally.

The `_null` and `_not_present` cases exist to verify that both ways of expressing an absent field in
JSON are handled identically by the parser on each side.

### Running the tests

The tests require a Python interpreter with the t2iapi gRPC stubs installed. Pass its path as a JVM
system property:

```bash
# Linux / macOS
./gradlew test -Dpython.executable=/path/to/venv/bin/python

# Windows
.\gradlew.bat test "-Dpython.executable=C:\path\to\venv\Scripts\python.exe"
```
