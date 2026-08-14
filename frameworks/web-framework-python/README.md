# web-framework-python

Python bindings for [WebFramework](https://github.com/LazyPanda07/WebFramework), a C++ HTTP server core that also exposes C, C++, and C# APIs. The C++ layer owns the HTTP server, routing, and I/O; Python only implements request "executors" registered against routes in `web.json`.

## Stack

- **Language:** Python (via WebFramework's Python executor API)
- **Framework:** WebFramework (C++ core, `threadPool` web server)
- **Build:** Multi-stage — CMake/Ninja build of the C++ core with `-march=native`, then `pip install` of the generated Python API wheel; `ubuntu:26.04` runtime

## Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/pipeline` | GET | Returns `ok` |
| `/baseline11` | GET | Sums two query parameters |
| `/baseline11` | POST | Sums two query parameters + body (or first chunk if chunked) |
| `/json/{count:int}` | GET | Processes dataset, returns JSON |
| `/upload` | POST | Buffers or streams the body depending on size, returns byte count |
| `/ws` | WebSocket | Echoes text/binary frames, replies to close frames with a normal-closure close |

## Notes

- Python executors are thin: routing, connection handling, and HTTP parsing all happen in the compiled C++ core; `server.py` just registers executor modules and starts the server
- `/upload` switches strategy based on `Content-Length`: bodies under the 100 KB `largeBodySizeThreshold` are read whole, larger ones are streamed via `get_large_data()` chunks
- Static assets (test profile `static`) are served directly by the C++ core from `assetsPath` (`/data`), not through a Python executor
- Thread pool size configured in `server/config.json` (`threadCount`, `resourcesThreads`), overridable via the `THREADS` env var
- HTTPS is configured in `config.json` but disabled (`useHTTPS: false`) for this entry
