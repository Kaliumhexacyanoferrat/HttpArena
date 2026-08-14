# actix-h2c

Actix-web 4 listener bound with `bind_auto_h2c` and restricted to HTTP/2 cleartext prior-knowledge: a middleware rejects any request that negotiates HTTP/1.1 so the port only ever serves h2c. Handlers mirror the main `actix` entry's baseline/JSON shape.

## Stack

- **Language:** Rust 1.94
- **Framework:** Actix-web 4 (`bind_auto_h2c`)
- **Build:** Thin LTO, `-C target-cpu=native`, `codegen-units = 1`, `panic = "abort"`, `debian:bookworm-slim` runtime

## Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/baseline2` | GET | Sums `a`/`b` query parameters (h2c) |
| `/json/{count}` | GET | Processes up to `count` dataset items, serializes JSON |

## Notes

- `wrap_fn` middleware returns `400` for any request whose negotiated version is `HTTP_11`, enforcing h2c-only per the anti-cheat validation
- Worker count comes from the cgroup CPU quota (`/sys/fs/cgroup/cpu.max`), falling back to `num_cpus::get()`
- Dataset loaded once at startup from `DATASET_PATH` (default `/data/dataset.json`) and shared via `web::Data`
- Two-stage Docker build compiles a dummy `main.rs` first to cache dependency builds
