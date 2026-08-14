# zix-http3

Zig HTTP/3 server on the `zix.Http3` engine: pure-Zig QUIC implemented on `std.crypto` (RFC 9000/9001/9002/9114), no external QUIC library. One `SO_REUSEPORT` worker per core on UDP 8443, NewReno congestion control with PTO retransmit and rolling `MAX_STREAMS`/`MAX_DATA` credit.

## Stack

- **Language:** Zig 0.16.0
- **Engine:** zix (`zix.Http3`, pure-Zig QUIC, `.URING` dispatch model)
- **TLS:** Baked self-signed Ed25519 certificate, TLS 1.3 (inside the QUIC handshake), ALPN `h3`
- **Build:** Multi-stage, `alpine:3.20` build and runtime, musl target with `x86_64_v3+aes+pclmul+adx`/`baseline` CPU tuning

## Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/baseline2` | GET | Sums query parameter values, served over QUIC |
| `/static/{file}` | GET | Serves `/data/static` with `.br`/`.gz` negotiation, served over QUIC |

## Notes

- QUIC is UDP-only and has no cleartext mode, so a missing/unreadable TLS cert is fatal at startup (unlike the TCP zix entries, there is no degrade path)
- A separate one-worker plain-TCP HTTP/1.1 listener on the same port number (8443) answers the benchmark's readiness probe, since a UDP-only QUIC server can never satisfy a TCP curl check; it idles during the measured run
- Static cache is pre-warmed at startup (every `.br`/`.gz`/identity candidate resolved once) so the request path is a lock-free lookup only
- `+aes+pclmul+adx` CPU features compiled in explicitly for hardware-accelerated AEAD in the QUIC/TLS record layer
- Process priority elevated via `setpriority(-19)`, best-effort
