# iris

Thread-per-core C++ HTTP/1.1 gateway on epoll/`SO_REUSEPORT` with a hand-written incremental parser, built from the external [IRIS](https://github.com/Cobra007-star-source/IRIS) repository (AGPL-3.0). Aims for a zero-allocation steady state: fixed per-connection buffers, precomputed response templates, and static assets served via `sendfile` from sealed memfd-backed, precompressed `.br`/`.gz` responses.

## Stack

- **Language:** C++
- **Framework:** IRIS gateway (`iris-ha-gw`), built with `-DIRIS_PROFILE=racing`
- **Engine:** epoll / `SO_REUSEPORT`
- **TLS:** OpenSSL, on port 8081 for the `json-tls` profile
- **Build:** Cloned at tag `v0.3.2-ha`, CMake Release build on `ubuntu:24.04`; runtime image installs only `libpq5`/`libssl3`

## Endpoints

| Endpoint | Method | Description |
|----------|--------|--------------|
| `/pipeline` | GET | Returns `ok` (plain text) |
| `/baseline11` | GET | Sums query parameter values |
| `/baseline11` | POST | Sums query parameters + request body (Content-Length and chunked) |
| `/json/{count}` | GET | Per-request JSON serialization from the mounted dataset |
| `/upload` | POST | Streams the request body, counting bytes |
| `/async-db` | GET | Postgres range query via async libpq |
| `/static/*` | GET | Static assets served via precompressed `.br`/`.gz` negotiation and Linux `sendfile` |

## Notes

- Per-connection fixed buffers and precomputed response templates avoid allocation in the hot path
- Static responses are sealed memfd objects served via `sendfile`, not read from disk per request
- `async-db` uses async libpq rather than a blocking connection pool
- Source lives entirely in the external IRIS repository — this directory only holds the Dockerfile that builds it (`-DIRIS_BUILD_GATEWAY=ON -DIRIS_BUILD_DB=ON`, examples/benchmarks/tests/simdjson/bowtie disabled)

