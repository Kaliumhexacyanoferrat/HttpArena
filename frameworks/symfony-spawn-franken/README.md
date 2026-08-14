# symfony-spawn-franken

Symfony 7.4 running under [symfony-spawn](https://github.com/yangusik/symfony-spawn)'s `FrankenPhpServer` adapter: a `worker.php` boots the Symfony kernel once and FrankenPHP's Caddy-embedded PHP runtime (built on the TrueAsync PHP core) executes each request as a coroutine via its `async` worker mode.

## Stack

- **Language:** PHP >=8.6 (`trueasync/php-true-async:latest-frankenphp` base image)
- **Framework:** Symfony 7.4 (framework-bundle, routing via attributes)
- **Runtime:** FrankenPHP (Caddy + embedded PHP) with `yangusik/symfony-spawn`'s `FrankenPhpServer` worker adapter
- **Build:** `composer install --no-dev`, `bin/console cache:warmup`, OPcache JIT enabled at image build time

## Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/pipeline` | GET | Returns `ok` (plain text) |
| `/baseline11` | GET/POST | Sums query params (+ request body on POST) |
| `/baseline2` | GET/POST | Same controller action as `/baseline11` |
| `/json/{count}` | GET | Returns first `count` dataset items, `total = price * quantity * m` |
| `/upload` | POST | Returns request body byte length |
| `/async-db` | GET | Postgres range query via Doctrine DBAL, JSON response |
| `/static/{file}` | GET | Serves preloaded static files, with br/gzip precompressed variants |

## Notes

- `BenchmarkController` is a plain (non-autowired-per-request) Symfony controller; dataset and static files are loaded once into `static` properties on first construction and reused across requests within a worker
- Doctrine DBAL connection pooling is configured via `true_async.db_pool` (min 4, max 64, 30s healthcheck), targeting Postgres over `DATABASE_URL`
- Caddyfile runs `worker.php` in `async` mode (`num {$WORKERS:0}`); listeners on `:8080` (plain) and `:8443` (TLS)
- OPcache JIT (`opcache.jit=1255`) enabled; `GOGC=1000` set on the FrankenPHP/Go runtime to reduce GC frequency under cgo-heavy PHP execution
