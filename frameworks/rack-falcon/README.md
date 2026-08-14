# rack-falcon

Minimal Rack application served by [Falcon](https://github.com/socketry/falcon), a fiber-based async HTTP server for Ruby built on `async`/`async-http`. Falcon is restricted to HTTP/1.1 for this entry.

## Stack

- **Language:** Ruby 4.0 (`ruby:4.0-slim`, YJIT enabled)
- **Framework:** Rack 3
- **Server:** Falcon (`Async::HTTP::Endpoint`, `Async::HTTP::Protocol::HTTP11`)
- **Build:** `bundle install --deployment`, jemalloc preloaded

## Endpoints

| Endpoint | Method | Description |
|----------|--------|--------------|
| `/pipeline` | GET | Returns `ok` (plain text) |
| `/baseline11` | GET | Sums query parameters `a` + `b` |
| `/baseline11` | POST | Sums query parameters + request body |
| `/baseline2` | GET | Sums query parameters `a` + `b` |

## Notes

- `falcon.rb` explicitly pins the endpoint to HTTP/1.1 (`Async::HTTP::Protocol::HTTP11`)
- Falcon uses Ruby fibers for concurrency rather than an OS thread pool
- Started via `falcon host` with a `supervisor` process, not `rackup`/Puma
- `RUBY_MN_THREADS=1` and jemalloc (`LD_PRELOAD`) enabled in the Dockerfile
