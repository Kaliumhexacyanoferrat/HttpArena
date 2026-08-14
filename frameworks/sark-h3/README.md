# sark-h3

sark's native HTTP/3 stack: a QUIC endpoint built directly on the dope io_uring runtime (as a dope Manifold), with shin TLS 1.3 (self-signed Ed25519 certificate) and QPACK/H3 framing via `sark-h3`. A couple of cores instead serve HTTP/2-over-TLS with an `alt-svc` header advertising h3, so h2 clients can discover and upgrade.

## Stack

- **Language:** Rust 1.95 (edition 2024)
- **Runtime:** dope (io_uring, thread-per-core), dope-quic (native QUIC endpoint)
- **Protocol:** HTTP/3 over QUIC (most cores); HTTP/2-over-TLS with `alt-svc: h3=":8443"` (2 cores)
- **TLS:** shin (TLS 1.3, self-signed Ed25519)
- **Build:** shared `../sark/Dockerfile`, `--build-arg BIN=httparena-sark-h3`

## Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/baseline2` | GET | Sums `a`/`b` query parameters |
| `/json/{count}` | GET | Processed dataset JSON |
| `/static/{file}` | GET | Serves a file from disk (brotli/gzip precompressed sidecars) |

Unmatched paths return `404` with a JSON error body.

## Notes

- All but two cores run a QUIC/H3 endpoint (`dope_quic::Endpoint`) with a per-core `sark_h3::dope::Session`, driven directly off the io_uring driver — no tokio, no separate async H3 stack
- Two cores instead run HTTP/2-over-TLS (`sark_h2`) with `advertise_h3(true)`, so their responses carry `alt-svc: h3=":8443"`
- QUIC transport params: 30s idle timeout, 64 MiB connection-level flow control, 1 MiB per-stream flow control, 256 max bidirectional streams
- QUIC address validation / retry tokens are opt-in via `SARK_H3_REQUIRE_ADDR_VALIDATION`
- Response bodies and headers are built by hand (no header-map allocation); routing logic (`h2bench::BenchHandler::route`) is shared between the h2 and h3 code paths

