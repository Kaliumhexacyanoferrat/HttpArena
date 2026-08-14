# rust-epoll

Zero-dependency-on-frameworks Rust HTTP engine built directly on raw `epoll` syscalls via `libc` — no async runtime, no HTTP framework. One thread per CPU core, each with its own listener socket bound via `SO_REUSEPORT`.

## Stack

- **Language:** Rust 1.88
- **Engine:** raw epoll (libc syscalls), one-thread-per-core with `SO_REUSEPORT`
- **Build:** Multi-stage, `-C target-cpu=native`, stripped binary, `debian:bookworm-slim` runtime

## Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/pipeline` | GET | Returns `ok` (plain text) |
| `/baseline11` | GET | Sums query parameter values |
| `/baseline11` | POST | Sums query parameter values + request body |

Any other path returns `404`.

## Notes

- Hand-rolled HTTP/1.1 request-line and header parsing operating directly on byte buffers
- Pipelined requests are batched: everything parsed out of one `recv()` is answered in a single `send()`
- Per-connection request/response buffers are pooled in fd-keyed `HashMap`s, sized 8 KiB (request) / 64 KiB (response)
- Chunked transfer-encoding is decoded manually for POST bodies
- Process priority is raised via `setpriority(PRIO_PROCESS, 0, -19)`
- The accept loop drains all pending connections on each `epoll_wait` wake before returning to wait again

