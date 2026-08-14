# userver

[userver](https://github.com/userver-framework/userver) — Yandex's asynchronous C++ framework built around coroutines and a component/task-processor model — configured declaratively via `static_config.yaml` rather than code-based routing.

## Stack

- **Language:** C++
- **Framework:** userver (component-based, coroutine tasks)
- **Build:** `ghcr.io/userver-framework/ubuntu-24.04-userver:v3.1` image, CMake Release with `-march=native`

## Endpoints

| Endpoint | Method | Description |
|----------|--------|--------------|
| `/pipeline` | GET | Returns `ok` (plain text) |
| `/baseline11` | GET, POST | Sums query parameter values (+ body on POST) |
| `/baseline2` | GET | Sums query parameter values (HTTP/2 variant) |
| `/json/{count}` | GET | Returns `count` items from the dataset with `total = price * quantity * m`; gzip via `CompressionMiddleware` |
| `/async-db` | GET | Postgres range query: `SELECT ... WHERE price BETWEEN $1 AND $2 LIMIT $3` |
| `/upload` | POST | Returns the received body's byte count |
| `/static/*` | GET | Served via `HttpHandlerStatic` backed by an in-memory `fs-cache` component |

## Notes

- Routing, ports, and per-handler settings are all declared in `configs/static_config.yaml` (`handler-defaults`, `task_processor`, TLS per listener), not in application code
- `main-task-processor` runs 64 worker threads with `guess-cpu-limit: true`; a separate single-thread `fs-task-processor` handles filesystem and DNS work off the main pool
- Coroutine pool preallocates 10,000 coroutines at startup (cap 300,000), 64 KB stacks each
- Postgres pool pre-warms all connections at startup (`min_pool_size == max_pool_size`, both driven by `$DATABASE_MAX_CONN`)
- gzip compression is a custom `CompressionMiddleware` (raw zlib `deflateInit2`/`deflate`) applied only to `/json/{count}` via a dedicated `compression-pipeline-builder`, gated on `Accept-Encoding: gzip` and skipped for streamed bodies
- A `NoopTracingManager` disables userver's built-in distributed tracing span injection to remove that overhead from the benchmark path
- Logging set to `ERROR` with `overflow_behavior: discard` so a busy system drops logs rather than blocking
- TLS listener for the `json-tls` profile is defined alongside the plaintext listener in the same `server.listener.ports` list; an additional TLS-only port for h2/general TLS is present in the config but commented out

