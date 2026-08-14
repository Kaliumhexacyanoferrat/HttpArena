# cardigan-grpc

Cardigan gRPC over cleartext HTTP/2 (h2c, prior knowledge) — no TLS, no protocol upgrade handshake. Built from the shared `../cardigan` source tree with `CARDIGAN_HTTPARENA_MODE=grpc`; listens in plaintext on port 8080.

## Stack

- **Language:** Java 26 (`--enable-preview`, `jdk.incubator.vector`)
- **Framework:** Cardigan engine (shared with `cardigan`/`cardigan-h2`/etc.); gRPC wire format implemented by hand, no grpc-java/protobuf-java runtime
- **Engine:** io_uring / Panama FFI, virtual threads, `ProtocolMode.HTTP2_ONLY`
- **Build:** no Dockerfile/pom.xml of its own — `build.sh` builds `../cardigan` with `--build-arg CARDIGAN_HTTPARENA_MODE=grpc`

## Services

| Service | RPC | Description |
|---------|-----|--------------|
| `benchmark.BenchmarkService` | `GetSum` | Unary. Hand-decodes a `SumRequest{a,b}` from the raw protobuf wire bytes and returns a single `SumReply{result=a+b}` framed as one gRPC message |
| `benchmark.BenchmarkService` | `StreamSum` | Server-streaming. Decodes `StreamRequest{a,b,count}` and streams `count` `SumReply` messages (`a+b`, `a+b+1`, ...) |

## Notes

- `HttpArenaGrpcController` parses request frames and encodes response frames manually against the 5-byte length-prefixed gRPC wire format — there's no generated protobuf/grpc-java code on the server side.
- Invalid arguments (e.g. negative `count`, or `count` over 1,000,000) return `grpc-status: 3` (`INVALID_ARGUMENT`) trailers rather than throwing.
- The base arithmetic routes (`/baseline11`, `/pipeline`, `/baseline2`) from `HttpArenaController` remain mounted alongside the gRPC service.
- `../cardigan/proto/benchmark.proto` is the canonical service contract (used to generate client stubs for load testing); the server itself does not depend on it at runtime.
