# web-framework-csharp

C# bindings for [WebFramework](https://github.com/LazyPanda07/WebFramework), a C++ HTTP server core exposed to multiple language APIs (Python, C, C++, C#). Executors are plain C# classes loaded into the native server via its .NET runtime host and routed by a JSON config rather than code-based routing.

## Stack

- **Language:** C# / .NET 10 (executors), native C++ server core
- **Framework:** WebFramework (`webServerType: threadPool`)
- **Build:** `dotnet publish` produces `WebFrameworkCSharpAPI.dll` + `Server.dll`, run under `dotnet-runtime-10.0` on `ubuntu:26.04` with the native WebFramework shared libraries on `LD_LIBRARY_PATH`

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

- Routes are declared declaratively in `Executors/web.json` (`route`, `api: csharp`, `loadType: initialization`/`dynamic`), not registered in code — each executor is a class implementing `StatelessExecutor`/`StatefulExecutor`/`WebSocketExecutor`
- `THREADS` env var overrides `threadCount` in `config.json` at startup (reserving 8 threads for other purposes: `value - 8`); an `H2THREADS` var is read but not yet wired to anything (`// TODO: HTTP/2.0`)
- `Upload` is a `StatefulExecutor` (per-connection instance state) that tracks whether the body exceeds the 100 KB `largeBodySizeThreshold` and switches between `GetHttpBody()` and streaming `GetLargeData()` chunks accordingly
- `JsonExecutor` loads `dataset.json` once via `Init(ExecutorSettings)` and deep-clones each item per request before mutating it with the computed `total` field
- Static assets are served from `/data` (`assetsPath`) by the framework itself, not a custom executor
- HTTPS is configured but disabled (`useHTTPS: false`) — server listens plaintext on 8080

