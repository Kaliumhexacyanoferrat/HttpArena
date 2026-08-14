# web-framework-cc

Benchmark server built against [WebFramework](https://github.com/LazyPanda07/WebFramework), a C++ web framework with bindings for multiple languages (Python, C, C++, C#). This entry uses its C API: routes are implemented as small C "executor" plugins, compiled into a shared library and loaded by a thread-pool web server at startup.

## Stack

- **Language:** C (against WebFramework's `cc` executor API; the framework itself is C++)
- **Framework:** WebFramework 3.4.2
- **Build:** CMake + Ninja (`-march=native`, Release), multi-stage `ubuntu:26.04`

## Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/pipeline` | GET | Returns `ok` (plain text) |
| `/baseline11` | GET/POST | Sums `a`+`b` query params (+ request/chunked body on POST) |
| `/json/{count}` | GET | Returns first `count` dataset items, `total = price * quantity * m` |
| `/upload` | POST | Accumulates request body size across chunks (large-body path kicks in above 100 KB), returns byte count |
| `/ws` | GET | Upgrades to a WebSocket echo handler (rejects plain GET with 400) |

## Notes

- Route-to-executor mapping is declared in `executors/web.json`; each executor is a `DEFINE_EXECUTOR`/`DEFINE_DEFAULT_EXECUTOR` C struct compiled into `libexecutors.so` and loaded by the server
- Server runs a `threadPool` web server type; thread count comes from `config.json` (`ThreadPoolServer.threadCount: 8`), overridable at startup via the `THREADS` env var
- `/json` dataset is parsed once at executor initialization from `dataset.json` (loaded via the framework's `assetsPath`)
- `/upload` is a stateful executor (per-connection accumulator struct) that switches to a large-data streaming path once `Content-Length` exceeds 100 KB
- Static assets are served from `assetsPath: /data`, configured in `config.json` rather than a custom executor
