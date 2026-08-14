# cardigan-h2c

Cardigan HTTP/2 over cleartext (h2c) using prior-knowledge negotiation — no TLS, no Upgrade handshake. Built from the shared `../cardigan` source tree with `CARDIGAN_HTTPARENA_MODE=h2c`; listens in plaintext on port 8082.

## Stack

- **Language:** Java 26 (`--enable-preview`, `jdk.incubator.vector`)
- **Framework:** Cardigan engine (shared with `cardigan`/`cardigan-h2`/etc.)
- **Engine:** io_uring / Panama FFI, virtual threads, `ProtocolMode.HTTP2_ONLY` with no TLS layer
- **Build:** no Dockerfile/pom.xml of its own — `build.sh` builds `../cardigan` with `--build-arg CARDIGAN_HTTPARENA_MODE=h2c`

## Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/pipeline` | GET | Returns `ok` (plain text) |
| `/baseline11` | GET | Sums `a`+`b` query params |
| `/baseline11` | POST | Sums query params plus an integer parsed from the request body |
| `/baseline2` | GET | Sums `a`+`b` query params |

## Notes

- Only `HttpArenaController` is mounted for this mode — no static/JSON/gRPC extras, unlike `cardigan-h2` or `cardigan-json-tls`.
- Clients must speak HTTP/2 cleartext prior knowledge directly; there's no ALPN negotiation or `h2c` Upgrade path since the connection is never TLS and never starts as HTTP/1.1.
