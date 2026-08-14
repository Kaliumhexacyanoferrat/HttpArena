# quarkus-jvm-grpc

Quarkus gRPC server using `quarkus-grpc` on Vert.x, with h2c on the main HTTP port plus a separate TLS listener. JDK 21/25, optimized JVM tuning.

## Stack

- **Language:** Java 21 (compiled), running on JDK 25 at runtime
- **Framework:** Quarkus 3.17 (`quarkus-grpc`)
- **Engine:** Vert.x / Netty
- **Build:** `maven:3.9-eclipse-temurin-25` → `eclipse-temurin:25-jre` runtime

## Services

| Service | RPC | Description |
|---------|-----|--------------|
| `benchmark.BenchmarkService` | `GetSum` | Unary. `BenchmarkGrpcService` (a `@GrpcService` bean) returns `SumReply{result = a + b}` via the generated `BenchmarkServiceGrpc.BenchmarkServiceImplBase` |

## Notes

- `quarkus.grpc.server.use-separate-server=false` — gRPC is served on the main Vert.x HTTP server (port 8080) as h2c rather than its own listener.
- A second, TLS-terminated listener is exposed on port 8443 (`quarkus.http.ssl-port`) using a PEM cert/key pair mounted at `/certs/server.crt` / `/certs/server.key`.
- `quarkus.http.accept-backlog=-1` and `quarkus.http.idle-timeout=0` remove backlog/idle-timeout limits; `quarkus.vertx.prefer-native-transport=true` enables the Netty native (epoll) transport.
- JVM tuning: `-XX:+UseParallelGC`, `-XX:+UseNUMA`, `-XX:-StackTraceInThrowable`; Netty bounds/accessibility checks and several Vert.x validations/metrics/websockets/timings are disabled for minimal overhead.
