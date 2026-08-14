# spring-jvm-grpc

Spring Boot gRPC server using `grpc-spring-boot-starter` (net.devh) with the Netty transport, JDK 21. The web servlet stack is disabled entirely — the process is gRPC-only, split across a plaintext h2c listener and a manually bootstrapped TLS listener.

## Stack

- **Language:** Java 21
- **Framework:** Spring Boot 3.4 + `grpc-spring-boot-starter` (`net.devh:grpc-spring-boot-starter`)
- **Engine:** gRPC Netty (`grpc-netty-shaded`)
- **Build:** Gradle 8.12 (downloaded in the build stage) → `eclipse-temurin:21-jdk` build / `eclipse-temurin:21-jre` runtime

## Services

| Service | RPC | Description |
|---------|-----|--------------|
| `benchmark.BenchmarkService` | `GetSum` | Unary. `BenchmarkServiceImpl` (a `@GrpcService` bean) returns `SumReply{result = a + b}` via the generated `BenchmarkServiceGrpc.BenchmarkServiceImplBase` |

## Notes

- `spring.main.web-application-type=none` — no embedded Tomcat/servlet container; the only listeners are gRPC.
- The starter's own server (`grpc.server.port=8080`, `grpc.server.security.enabled=false`) serves gRPC in cleartext (h2c) on port 8080.
- On `ContextRefreshedEvent`, `Application` manually builds and starts a second `io.grpc.Server` on port 8443 with TLS (`GrpcSslContexts.forServer`, `/certs/server.crt` + `/certs/server.key`), registering a *second, separate* `BenchmarkServiceImpl` instance directly via `NettyServerBuilder` rather than through the starter — this TLS server only starts if both cert files exist on disk.
- `benchmark.proto` defines only `GetSum` here (no `StreamSum`), unlike the Cardigan gRPC variants.
