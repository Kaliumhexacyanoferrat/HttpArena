# robyn

[Robyn](https://github.com/sparckles/Robyn), a Python web framework backed by a Rust (actix) runtime accessed via PyO3 bindings, combining multi-process and multi-threaded execution.

## Stack

- **Language:** Python 3.13
- **Framework:** Robyn
- **Engine:** Rust runtime (actix/uvloop) via PyO3
- **Build:** `python:3.13-slim`

## Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/pipeline` | GET | Returns `ok` (const route, cached in Rust) |
| `/baseline11` | GET, POST | Sums two query parameters (+ body on POST) |
| `/json/:count` | GET | Processes dataset, returns JSON |
| `/upload` | POST | Returns byte count of the request body |
| `/static/*` | GET | Served via `app.serve_directory("/static", "/data/static")` |

## Notes

- `/pipeline` is registered with `const=True`, letting Robyn cache and serve the response entirely from the Rust side without re-entering Python per request
- Process/thread model: `processes = WRK_COUNT` (one per core) each with `workers = 2` Rust-side async workers
- Response `Server` header pinned to `Robyn`
- Timeouts configured via environment: `ROBYN_CLIENT_TIMEOUT=30`, `ROBYN_KEEP_ALIVE_TIMEOUT=20`
