# cardigan-h2

Cardigan HTTP/2 over TLS (negotiated h2, not cleartext h2c). Built from the shared `../cardigan` source tree with `CARDIGAN_HTTPARENA_MODE=h2`; listens on port 8443.

## Stack

- **Language:** Java 26 (`--enable-preview`, `jdk.incubator.vector`)
- **Framework:** Cardigan engine (shared with `cardigan`/`cardigan-h2c`/etc.)
- **TLS:** `dev.cardigan.tls.TlsConfig`, `ProtocolMode.HTTP2_ONLY`
- **Build:** no Dockerfile/pom.xml of its own — `build.sh` builds `../cardigan` with `--build-arg CARDIGAN_HTTPARENA_MODE=h2`

## Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/pipeline` | GET | Returns `ok` (plain text) |
| `/baseline11` | GET | Sums `a`+`b` query params |
| `/baseline11` | POST | Sums query params plus an integer parsed from the request body |
| `/baseline2` | GET | Sums `a`+`b` query params |
| `/static/{filename}` | GET | Serves one of 19 individually-registered static assets (css/js/html/fonts/svg/webp/json) preloaded from `CARDIGAN_STATIC_DIR` (default `/data/static`) |

## Notes

- Static routes are registered per exact filename in `HttpArenaStaticController` (not a dynamic wildcard match), and assets are preloaded into off-heap native memory at startup.
- Brotli variants (`<file>.br`) are served when present on disk and the client's `Accept-Encoding` advertises `br` with nonzero quality, via a hand-rolled Accept-Encoding parser.
- The base arithmetic routes from `HttpArenaController` are mounted alongside the static routes.
