# grpc-go

A gRPC server in Go using the reference `google.golang.org/grpc` implementation, serving both h2c and TLS.

## Stack

- **Language:** Go 1.24
- **Framework:** `google.golang.org/grpc`
- **TLS:** Optional, loaded from `/certs/server.crt` / `/certs/server.key` if present
- **Build:** Multi-stage, `golang:1.24-bookworm` → `debian:bookworm-slim` runtime, `CGO_ENABLED=0`

## Services

| Service | RPC | Description |
|---------|-----|--------------|
| `benchmark.BenchmarkService` | `GetSum` | Unary: `SumRequest{a, b}` → `SumReply{result: a+b}` |

## Notes

- Two independent `grpc.Server` instances registered against the same service implementation: one plaintext (h2c) on `:8080`, one TLS on `:8443` — the TLS listener only starts if cert files exist at startup
- `GOMAXPROCS` explicitly set to `runtime.NumCPU()`
- The generated stubs (`proto/benchmark.pb.go`, `proto/benchmark_grpc.pb.go`) are checked in alongside `benchmark.proto`
