# cardigan-json-tls

Cardigan HTTP/1.1 over TLS with a JSON transformation endpoint. Built from the shared `../cardigan` source tree with `CARDIGAN_HTTPARENA_MODE=json-tls`; the TLS listener runs on port 8081, alongside a second, plaintext HTTP/1.1 listener on port 8080 that serves only the base arithmetic routes.

## Stack

- **Language:** Java 26 (`--enable-preview`, `jdk.incubator.vector`)
- **Framework:** Cardigan engine (shared with `cardigan`/`cardigan-h2`/etc.)
- **TLS:** `dev.cardigan.tls.TlsConfig`, `ProtocolMode.HTTP1_ONLY`
- **Build:** no Dockerfile/pom.xml of its own — `build.sh` builds `../cardigan` with `--build-arg CARDIGAN_HTTPARENA_MODE=json-tls`

## Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/pipeline` | GET | Returns `ok` (plain text) — both listeners |
| `/baseline11` | GET | Sums `a`+`b` query params — both listeners |
| `/baseline11` | POST | Sums query params plus body integer — both listeners |
| `/baseline2` | GET | Sums `a`+`b` query params — both listeners |
| `/json/{count}` | GET | Streams `count` items (1..dataset size) from a preloaded dataset as JSON; optional `m` query multiplier (default 1) — TLS listener (8081) only |

## Notes

- Two `CardiganServer` instances run in one process: a single-event-loop plaintext server on port 8080 (base routes only, for baseline/pipeline probing) and the main TLS/HTTP1_ONLY server on port 8081 (base routes + `/json/{count}`).
- The dataset is loaded once at startup from `CARDIGAN_DATASET` (default `/data/dataset.json`) via `HttpArenaDataset.load`.
- Requesting a `count` outside `1..dataset.size()` returns `400` with a plain-text error message.
