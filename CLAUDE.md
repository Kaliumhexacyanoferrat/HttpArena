# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

HttpArena is an HTTP framework benchmark platform. Each entry under `frameworks/<name>/` is a self-contained Docker image that implements a set of test endpoints. A CI harness builds the image, starts the container, runs load generators (wrk, h2load, ghz, gcannon), and records results.

## Running locally

```bash
./scripts/validate.sh <framework>           # 18-point correctness check
./scripts/benchmark.sh <framework>          # all subscribed profiles
./scripts/benchmark.sh <framework> baseline # one profile
./scripts/benchmark.sh <framework> --save   # save results to leaderboard data
```

These scripts require Docker. They build the image, spin up any needed sidecars (Postgres, Redis), run the load generator containers from `docker/`, and tear everything down.

## Framework directory layout

Every framework needs exactly:

| File | Purpose |
|------|---------|
| `Dockerfile` | Multi-stage build; `COPY` paths must be relative to the repo root (Docker build context is the repo root: `COPY frameworks/<name>/...`) |
| `meta.json` | Metadata; controls which tests run and how the framework appears on the leaderboard |

### meta.json fields

```json
{
  "display_name": "my-framework",
  "language": "C#",
  "type": "flagship | tuned | engine",
  "mode": "standard | tuned",
  "engine": "kestrel | netty | tokio | ...",
  "description": "One-paragraph description of the implementation.",
  "repo": "https://github.com/...",
  "enabled": true,
  "tests": ["baseline", "pipelined", "limited-conn", ...],
  "maintainers": ["github-username"]
}
```

**`type`** controls leaderboard grouping and rule enforcement:
- `flagship` — standard framework usage, must follow framework defaults
- `tuned` — may adjust thread pools, buffer sizes, TCP options
- `engine` — bare-metal (no HTTP framework); ranked in its own category, no rules

**`tests`** is the list of profiles the framework subscribes to. The harness only runs and validates profiles in this list.

## Test profiles (endpoints each framework must implement)

| Profile | Endpoints |
|---------|-----------|
| `baseline` | `GET /baseline11?a=N&b=M` → `N+M` (text/plain); `POST /baseline11?a=N&b=M` body=K → `N+M+K`; also handles chunked body |
| `pipelined` | `GET /pipeline` → `ok` (text/plain) |
| `limited-conn` | same as baseline |
| `json` | `GET /json/{count}?m=N` → JSON array of processed dataset items |
| `json-comp` | same + gzip/brotli on Accept-Encoding |
| `json-tls` | same on :8081 with TLS |
| `upload` | `POST /upload` (20 MB body) → byte count (text/plain) |
| `static` | `GET /static/{file}` → pre-compressed file from `/data/static/` |
| `async-db` | `GET /async-db?min=&max=&limit=` → JSON from Postgres |
| `crud` | REST API at `/crud/items` with GET list, GET /{id}, POST, PUT /{id} |
| `baseline-h2` | Same baseline endpoints over HTTPS/HTTP2 on :8443 |
| `baseline-h2c` | Same over cleartext HTTP/2 on :8082 (prior-knowledge only) |
| `echo-ws` | WebSocket echo at `/ws` |

Ports: `8080` (HTTP/1.1), `8081` (HTTP/1.1 TLS or h2 TLS), `8082` (h2c), `8443` (HTTP/2 TLS + HTTP/3).

## Architecture of existing C# engine entries

The existing C# engine frameworks (`ioxide`, `minima`, `minima-sync`) hand-roll their own HTTP/1.1 parser directly on raw socket APIs — no Kestrel, no ASP.NET. They use `PipeReader`/`PipeWriter` or direct buffer rings for zero-copy I/O. Key patterns:

- One reactor/thread per core with `SO_REUSEPORT` for kernel-side load distribution
- Pipelining: parse all complete requests in the current buffer before issuing a single `write_all`
- Response batching into a single write per flush cycle
- No allocations in the hot path (pre-allocated buffers, stack spans)

## PR commands (tag @BennyFranciscus for help)

```
/validate -f <framework>
/benchmark -f <framework>
/benchmark -f <framework> -t <test>
/benchmark -f <framework> --save
```
