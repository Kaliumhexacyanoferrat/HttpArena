# sark-production

sark's production-stack profile: an nginx edge terminating TLS in front of a shared `authsvc` sidecar (stateless JWT verification) and a Redis cache-aside layer, fronting the sark io_uring server. Models a realistic API-gateway deployment — public routes bypass auth, `/api/*` is JWT-checked on every request, and item reads are cached in Redis with invalidation on writes.

## Stack

- **Language:** Rust 1.95 (edition 2024) — sark server and `authsvc`
- **Runtime:** dope (io_uring, thread-per-core) — sark server
- **Edge:** nginx 1.27 (`nginx:1.27-alpine`), TLS termination + `auth_request`
- **Auth:** `authsvc` (axum, HMAC-SHA256 JWT verification)
- **Cache:** Redis 7 (`redis:7-alpine`), cache-aside
- **Build:** three images — nginx edge (`proxy-production/Dockerfile`), `authsvc` (`../_shared/authsvc/Dockerfile`), sark server (shared `../sark/Dockerfile`, H1-only)

## Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/public/baseline` | GET | No auth; sums `a`/`b` query parameters |
| `/api/items/{id}` | GET | JWT-protected via nginx `auth_request`; Redis-cached read (200 ms TTL), `x-cache: HIT`/`MISS` |
| `/api/items/{id}` | POST | JWT-protected; invalidates the Redis cache entry for `id` |
| `/api/me` | GET | JWT-protected; echoes the `X-User-Id` forwarded by nginx from the auth subrequest |
| `/static/*` | GET | Served directly by nginx from disk |

## Notes

- Every `/api/*` request triggers an internal nginx subrequest (`auth_request /_auth`) to `authsvc`, which verifies the Bearer token's HMAC-SHA256 signature and extracts `user_id`; on success nginx forwards it as `X-User-Id`, on failure it returns `401 {"error":"unauthorized"}` — no caching of the auth result, every request pays real verification cost
- `/public/*` bypasses `auth_request` entirely
- sark caches item reads in Redis with a 200 ms TTL (cache-aside); falls back to an in-process `ItemCache` when `REDIS_URL` is unset
- POST to `/api/items/{id}` deletes the Redis key and invalidates the in-process cache
- sark runs with `SARK_HTTPARENA_H1_ONLY=1` behind the edge — no TLS/h2/h3 of its own
- Four services (edge, cache, authsvc, server) each need a disjoint cpuset — the sark server busy-polls io_uring, and any CPU overlap collapses throughput to zero
- The JWT signing secret is a fixed benchmark-only value, not meant for production use

