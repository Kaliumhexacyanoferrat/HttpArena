# zix-grpc

Zig gRPC server (h2c) on the `zix.Grpc` engine, built on top of `zix.Http2`. Shared-nothing by design: each worker owns its own `SO_REUSEPORT` listener, io_uring completion ring, and connections, multiplexing HTTP/2 streams per connection with comptime-cached HPACK reply blocks.

## Stack

- **Language:** Zig 0.16.0
- **Engine:** zix (`zix.Grpc` on `zix.Http2`, `.URING` dispatch model)
- **Build:** Multi-stage, `alpine:3.20` build and runtime, musl target with `x86_64_v3`/`baseline` CPU tuning

## Services

| Service | RPC | Description |
|---------|-----|--------------|
| `benchmark.BenchmarkService` | `GetSum` | Unary: decodes `SumRequest{a, b}`, replies `SumReply{result: a+b}` |
| `benchmark.BenchmarkService` | `StreamSum` | Server-streaming: decodes `SumRequest{a, b, count}`, streams `count` replies `a+b+i` |

## Notes

- Manual protobuf field decoding via `zix.Grpc.MessageReader` — no generated proto bindings, wire format read directly by field number
- `max_streams` set to 128 so a client opening many parallel streams is never refused at startup
- No response caching: sum replies are a few bytes, well under the cache crossover
- Per-worker EPOLL pool sized `max(10, cpu*2)` by default (`pool_size = 0`); the unary path is CPU-bound rather than connection-bound, so an oversized thread-per-connection pool would thrash the scheduler
