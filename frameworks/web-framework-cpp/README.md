# web-framework-cpp

Native C++ API for [WebFramework](https://github.com/LazyPanda07/WebFramework), an HTTP server core exposed to multiple language APIs (Python, C, C++, C#). This entry runs executors compiled directly against the C++ API — no managed runtime in the request path — routed by a JSON config rather than code-based routing.

## Stack

- **Language:** C++
- **Framework:** WebFramework (`webServerType: threadPool`)
- **Build:** CMake + Ninja, `-march=native`, installed into `bin/`; runtime image is a bare `ubuntu:26.04` with the built shared libraries on `LD_LIBRARY_PATH`

## Endpoints

| Endpoint | Method | Description |
|----------|--------|--------------|
| `/pipeline` | GET | Returns `ok` (plain text) |
| `/baseline11` | GET | Sums `a`/`b` query parameters |
| `/baseline11` | POST | Sums `a`/`b` query parameters + request body (chunked or fixed-length) |
| `/json/{count}` | GET | Returns `count` dataset items with `total = price * quantity * m` |
| `/upload` | POST | Accepts request body, switching to chunked large-data reads above 100 KB; returns byte count |
| `/ws` | GET | Non-upgrade request returns `400`; WebSocket upgrade handled by a separate executor that echoes text/binary frames and closes normally on a close frame |

## Notes

- Routes are declared declaratively in `executors/web.json` (`route`, `api: cxx`, `loadType: initialization`/`dynamic`); each executor is a class registered via `DEFINE_EXECUTOR`/`DEFINE_WEB_SOCKET_EXECUTOR` macros
- `THREADS` env var overrides `threadCount` in `config.json` at startup (`value - 8`, reserving 8 threads); an `H2THREADS` var is read but not wired to anything (`// TODO: HTTP/2.0`)
- `JsonExecutor::init` parses `dataset.json` once at load time into `framework::JsonObject`s; each request copies the item and mutates it with the computed `total` field
- `Upload` tracks per-instance state (`currentSize`) and switches between `getBody()` and streaming `getLargeData()` chunks once Content-Length crosses the 100 KB `largeBodySizeThreshold`
- Static assets are served from `/data` (`assetsPath`) by the framework itself, not a custom executor
- HTTPS is configured but disabled (`useHTTPS: false`) — server listens plaintext on 8080
- Same application logic and route table as `web-framework-csharp`, compiled against the framework's native C++ API instead of hosting the .NET runtime

