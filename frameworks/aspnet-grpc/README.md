# aspnet-grpc

ASP.NET Core gRPC server using .NET 10 with Kestrel's native HTTP/2 gRPC support. Covers unary, server-streaming, client-streaming, and bidirectional-streaming RPCs.

## Stack

- **Language:** C# / .NET 10
- **Framework:** ASP.NET Core gRPC (`Grpc.AspNetCore`)
- **Engine:** Kestrel (HTTP/2 only)
- **Build:** Framework-dependent publish, `mcr.microsoft.com/dotnet/aspnet:10.0` runtime

## Services

| Service | RPC | Description |
|---------|-----|--------------|
| `BenchmarkService` | `GetSum` (unary) | Returns `a + b` |
| `BenchmarkService` | `StreamSum` (server streaming) | Emits `count` replies of `a + b + i` for a single request |
| `BenchmarkService` | `CollectSum` (client streaming) | Aggregates `a + b` over all streamed requests into one final total |
| `BenchmarkService` | `EchoSum` (bidirectional streaming) | Emits one `a + b` reply per incoming request on a persistent stream |

## Notes

- `CreateSlimBuilder` (trimmed-down host) rather than the full `WebApplication` builder
- Kestrel listeners are HTTP/2-only: plaintext on 8080, TLS (from `$TLS_CERT`/`$TLS_KEY`, default `/certs/server.crt` + `/certs/server.key`) on 8443
- HTTP/2 tuned: 256 max streams per connection, 2 MB initial connection window, 1 MB stream window
- Logging disabled (`ClearProviders()`) for throughput
- Proto contract defined in `Protos/benchmark.proto`, compiled to C# via the `Grpc.Tools` MSBuild target

