# frankenphp-trueasync

FrankenPHP (Caddy-embedded PHP app server, written in Go) built on the [TrueAsync](https://github.com/true-async/php-async) PHP core, running its worker in FrankenPHP's `async` mode so each request executes as a cooperative coroutine rather than a classic worker-per-request loop.

## Stack

- **Language:** PHP (`trueasync/php-true-async:latest-frankenphp` base image)
- **Runtime:** FrankenPHP (Caddy + embedded PHP via cgo) with TrueAsync coroutine core
- **Build:** Prebuilt base image, OPcache JIT enabled at image build time

## Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/pipeline` | GET | Returns `ok` (plain text) |
| `/baseline11` | GET/POST | Sums `a`+`b` query params (+ request body on POST) |
| `/baseline2` | GET/POST | Same handler as `/baseline11` |
| `/json/{count}` | GET | Returns first `count` dataset items, `total = price * quantity * m` |
| `/upload` | POST | Returns request body byte length |
| `/db` | GET | SQLite range query via PDO, JSON response |
| `/async-db` | GET | Postgres range query via a pooled PDO connection, JSON response |
| `/static/{file}` | GET | Serves preloaded static files, with br/gzip precompressed variants |

## Notes

- Caddyfile runs the worker script (`worker.php`) in `async` mode with `buffer_size 1`, backed by libuv event loops (one per worker)
- Worker count is controlled by `$WORKERS` (default `0`, i.e. one per `GOMAXPROCS`); listeners on `:8080` (plain) and `:8443` (TLS)
- Datasets and static files are preloaded into memory once at worker startup; SQLite/Postgres connections are opened lazily and cached in globals
- Postgres access uses PDO's connection pooling attributes (`ATTR_POOL_ENABLED`, min 64 / max `DATABASE_MAX_CONN`)
- OPcache JIT (`opcache.jit=1255`) enabled; `GOGC=1000` set to reduce Go GC frequency under the allocation-heavy PHP/cgo workload — see `PERFORMANCE_NOTES.md` for the profiling behind this, which found Go scheduler churn on cgo returns (not PHP execution) as the dominant bottleneck, and recommends over-provisioning `GOMAXPROCS` above core count
