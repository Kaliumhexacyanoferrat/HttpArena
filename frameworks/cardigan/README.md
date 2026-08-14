# cardigan

Cardigan is a Java HTTP engine built on io_uring, Panama FFI, virtual threads, and thread-per-core event loops. This directory holds the shared source tree for all `cardigan-*` variants in this repo; built standalone (`CARDIGAN_HTTPARENA_MODE=h1`, the default) it runs the plaintext HTTP/1.1 baseline profile on port 8080.

## Stack

- **Language:** Java 26 (`--enable-preview`, `jdk.incubator.vector`)
- **Framework:** Cardigan (`dev.cardigan`) — custom native-memory HTTP engine, not a conventional web framework
- **Engine:** io_uring / Panama FFI, virtual threads, thread-per-core event loops
- **Build:** multi-stage, `maven:3.9-eclipse-temurin-26` → `eclipse-temurin:26-jre` runtime

## Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/pipeline` | GET | Returns `ok` (plain text) |
| `/baseline11` | GET | Sums `a`+`b` query params |
| `/baseline11` | POST | Sums query params plus an integer parsed from the request body |
| `/baseline2` | GET | Sums `a`+`b` query params (HTTP/2-profile variant of the same handler) |

## Notes

- A single launcher, `HttpArenaMain`, selects listener port/protocol/routes based on `CARDIGAN_HTTPARENA_MODE` (env var or arg); this directory's `build.sh` builds the `h1` mode.
- The request-body integer parser in `HttpArenaController` reads directly off a Panama `MemorySegment` scratch buffer instead of allocating.
- Virtual-thread scheduler parallelism/pool size is pinned to `CARDIGAN_THREADS` (defaults to `availableProcessors()`).
- The other `cardigan-*` directories in this repo (`cardigan-grpc`, `cardigan-grpc-tls`, `cardigan-h2`, `cardigan-h2c`, `cardigan-json-tls`) have no source of their own — their `build.sh` points at this directory as the Docker build context and passes a different `CARDIGAN_HTTPARENA_MODE` build arg.
