# cardigan-grpc-tls

Cardigan gRPC over HTTP/2 with TLS. Built from the shared `../cardigan` source tree with `CARDIGAN_HTTPARENA_MODE=grpc-tls`; listens on port 8443 using the certificate/key configured via `CARDIGAN_CERTIFICATE`/`CARDIGAN_PRIVATE_KEY` (defaults `/certs/server.crt`, `/certs/server.key`).

## Stack

- **Language:** Java 26 (`--enable-preview`, `jdk.incubator.vector`)
- **Framework:** Cardigan engine (shared with `cardigan`/`cardigan-grpc`/etc.); gRPC wire format implemented by hand, no grpc-java/protobuf-java runtime
- **TLS:** `dev.cardigan.tls.TlsConfig`, `ProtocolMode.HTTP2_ONLY`
- **Build:** no Dockerfile/pom.xml of its own — `build.sh` builds `../cardigan` with `--build-arg CARDIGAN_HTTPARENA_MODE=grpc-tls`

## Services

| Service | RPC | Description |
|---------|-----|--------------|
| `benchmark.BenchmarkService` | `GetSum` | Unary. Hand-decodes `SumRequest{a,b}` from the protobuf wire bytes, returns `SumReply{result=a+b}` as one gRPC message |
| `benchmark.BenchmarkService` | `StreamSum` | Server-streaming. Decodes `StreamRequest{a,b,count}`, streams `count` `SumReply` messages |

## Notes

- Identical handler code to `cardigan-grpc` (`HttpArenaGrpcController`); only the listener differs — TLS on port 8443 instead of plaintext on port 8080.
- Invalid arguments return `grpc-status: 3` (`INVALID_ARGUMENT`) trailers.
- The base arithmetic routes (`/baseline11`, `/pipeline`, `/baseline2`) remain mounted alongside the gRPC service.
- `../cardigan/proto/benchmark.proto` is the canonical service contract used for client stub generation.
