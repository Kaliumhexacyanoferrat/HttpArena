# http-kit

Plain Ring handler served by http-kit's embedded HTTP server, using blocking JDBC (HikariCP) on http-kit's own worker threads for the database test.

## Stack

- **Language:** Clojure 1.12 (deps.edn / `clojure` CLI), requires JDK 25
- **Framework:** Ring middleware (`ring.middleware.params`, `ring.middleware.gzip`) over a plain handler function
- **Server:** http-kit 2.9.0-beta4 (embedded)
- **Build:** `clojure:tools-deps-trixie-slim`, `clojure -X:deps prep`

## Endpoints

| Endpoint | Method | Description |
|----------|--------|--------------|
| `/pipeline` | GET | Returns `ok` (plain text) |
| `/baseline11` | GET/POST | Sums query parameters (+ body on POST) |
| `/json/:count` | GET | Processes first N dataset items, serializes JSON |
| `/async-db` | GET | Blocking JDBC range query via HikariCP |
| `/upload` | POST | Reads and discards request body, returns byte count |
| `/static/:filename` | GET | Serves files from `/data/static` |

## Notes

- Dockerfile asserts the JVM is exactly Java specification version 25 at build time
- HikariCP datasource lazily created on first `/async-db` request (double-checked locking)
- `/static/*` bypasses the params/gzip middleware chain and is served directly via `ring.util.response/file-response`
- Server configured with a 32 MB max request body and runs on http-kit's own request workers (not a thread-per-request model)
