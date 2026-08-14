# may-minihttp

Minimal HTTP/1.1 server on May stackful coroutines: coroutine-per-connection with cooperative scheduling, one May worker per CPU core. HTTP parsing is hand-rolled with `httparse` directly on top of May's coroutine I/O.

## Stack

- **Language:** Rust 1.94
- **Engine:** May (stackful coroutines), coroutine-per-connection, `httparse` for request parsing
- **Build:** Multi-stage, `-C target-cpu=native`, `debian:bookworm-slim` runtime

## Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/pipeline` | GET | Returns `ok` (plain text) |
| `/baseline11` | GET | Sums query parameter values |
| `/baseline11` | POST | Sums query parameter values + request body |

Any other path returns `404 not found`.

## Notes

- One May scheduler worker per CPU core (`may::config().set_workers(cpus)`)
- Each accepted connection is handled by its own coroutine (`may::coroutine::spawn`)
- Supports HTTP/1.1 pipelining: a single `read()` can contain multiple requests, which are parsed and answered in a loop before the next `read()`
- POST bodies are read via `Content-Length` or manually decoded chunked transfer-encoding
- `mimalloc` as global allocator
- Despite the crate name, this does not depend on the `may_minihttp` library — only on `may` (coroutines) and `httparse` (parsing)

