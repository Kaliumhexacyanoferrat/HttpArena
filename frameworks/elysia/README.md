# elysia

Ergonomic Bun-native TypeScript framework running a multi-process cluster behind Bun's HTTP server.

## Stack

- **Language:** TypeScript
- **Framework:** [Elysia](https://elysiajs.com) 1.4 + `@elysiajs/static`
- **Engine:** Bun (JavaScriptCore)
- **Build:** `bun build --compile --minify`, distroless runtime

## Endpoints

| Endpoint | Method | Description |
|---|---|---|
| `/pipeline` | GET | Returns `ok` (plain text) |
| `/baseline11` | GET | Sums query parameter values |
| `/baseline11` | POST | Sums query parameters + request body |
| `/baseline2` | GET | Sums query parameter values (HTTP/2 variant) |
| `/json/{count}` | GET | Returns `count` items from the preloaded dataset; honors `Accept-Encoding: gzip/br/deflate` (gzip via `Bun.gzipSync`, brotli via `node:zlib`, deflate via `Bun.deflateSync`) |
| `/async-db` | GET | Postgres range query: `SELECT ... WHERE price BETWEEN $min AND $max LIMIT $limit` |
| `/upload` | POST | Streams `request.body` via `for await` chunks, returns the byte count |
| `/static/*` | GET | Served by `@elysiajs/static` in dynamic mode (`alwaysStatic: false`) from `/data/static` |

## Notes

- HTTP/1.1 on port 8080 (Bun has no native HTTP/2 server; h2/h2c/h3/grpc profiles are skipped)
- Multi-process cluster: one worker per CPU via `node:cluster`, rebalanced with `reusePort: true` so the kernel spreads accepts across workers (override with `ELYSIA_WORKERS`, ~150 MB RSS per worker)
- Dataset (`/data/dataset.json`) and the awaited `staticPlugin` are resolved at module top-level so `bun build --compile` doesn't trip over top-level `await` inside the cluster `else` branch
- Postgres pooled via `pg` (node-postgres), sized `DATABASE_MAX_CONN / workers` per worker so the cluster total matches the server's `max_connections`
- `/async-db` handler catches exceptions and returns an empty payload — `error:` callback style would mask the 500 status code
- `alwaysStatic: false` on `staticPlugin` avoids Bun's pre-buffered static route path which crashes on `Bun.file()` streams under `NODE_ENV=production`
