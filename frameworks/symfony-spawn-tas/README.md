# symfony-spawn-tas

Symfony 7.4 running under [symfony-spawn](https://github.com/yangusik/symfony-spawn)'s `TrueAsyncRuntime`: instead of FrankenPHP, the app boots as a native `php public/index.php` process using the TrueAsync PHP core's own built-in HTTP server (TrueAsyncServer), configured entirely via the `runtime` block in `composer.json`.

## Stack

- **Language:** PHP >=8.6 (`trueasync/php-true-async:0.7.0-beta.5-php8.6-alpine` base image)
- **Framework:** Symfony 7.4 (framework-bundle, routing via attributes)
- **Runtime:** TrueAsyncServer (native, coroutine-per-request), via `yangusik/symfony-spawn`'s `TrueAsyncRuntime` (Symfony Runtime component integration)
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
| `/sqlite-db` | GET | SQLite range query via a direct PDO connection, JSON response |
| `/static/{file}` | GET | Fallback PHP handler for static files (br/gzip precompressed) — only reached if the runtime's built-in static handler misses |

## Notes

- Server topology is declared declaratively in `composer.json`'s `extra.runtime.options`: three listeners (`:8080` plain, `:8081` TLS/HTTP1, `:8443` TLS/auto), a built-in static file handler for `/static/` (with ETags and an open-file cache) that falls through to the Symfony route on miss, response compression, and `workers: 0` (auto)
- No Caddy/Go layer — this is PHP's async server directly, unlike the FrankenPHP-based `symfony-spawn-franken` entry
- Doctrine DBAL connection pooling is configured via `true_async.db_pool` (min 4, max 64, 30s healthcheck) for the Postgres-backed `/async-db` route; `/sqlite-db` opens its own unpooled PDO connection per request
- `BenchmarkController` loads the dataset once per instance (not static) on first construction
