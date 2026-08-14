# minima-sync

A **synchronous single-issuer io_uring** HTTP/1.1 server — the maximum-performance
adaptation of Minima toward zeemo's architecture, **NativeAOT-compiled**. Serves
the H1-isolated profiles (`baseline`, `pipelined`, `limited-conn`, `json`). It
reuses Minima's io_uring bindings (`io_uring/Native.cs`, `io_uring/Ring.cs`) but
drops the async engine for a zeemo-style synchronous reactor.

## Design — tuned for raw throughput on CPU-bound HTTP
- **Synchronous single-issuer reactor**: recv → parse → serialize → send, all
  inline on the reactor thread. No async/`IValueTaskSource`, no MPSC queues, no
  eventfd — with a synchronous handler there's never an off-reactor caller.
- **One pinned reactor thread per core** (`sched_setaffinity`), own ring
  (`SINGLE_ISSUER | DEFER_TASKRUN`) + own `SO_REUSEPORT` listener.
- **Multishot accept; single-shot recv into a per-connection buffer**,
  parse-in-place. Responses serialized **straight into the send buffer** (no
  managed→native copy). recv/send alternate per connection.
- **Zero hot-path allocation** — pooled connections + native (`NativeMemory`)
  buffers. JSON **serialized per request** from the parsed model.
- **NativeAOT** — a single native binary, no .NET runtime / no JIT warmup.

## vs `minima`
`minima` keeps the general **async** engine (it can offload blocking handlers →
`async-db`/`crud`). `minima-sync` trades that away — a synchronous reactor blocks
the core on a slow handler — for maximum throughput on the CPU-only profiles.

| Endpoint | Response |
|---|---|
| `GET/POST /baseline11?a=&b=` | `text/plain` — `a + b` (+ POST body) |
| `GET /pipeline` | `text/plain` — `ok` |
| `GET /json/{count}?m=N` | `application/json` — items with `total = price*quantity*N`, serialized per request |

io_uring needs `seccomp=unconfined` (harness-provided; `engine: "io_uring"`).
`MINIMA_PORT` / `MINIMA_REACTORS` / `MINIMA_DATASET` override for local runs.

