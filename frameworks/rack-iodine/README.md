# rack-iodine

Minimal Rack application served by [Iodine](https://github.com/boazsegev/iodine), a C-extension-backed evented HTTP/WebSocket server for Ruby.

## Stack

- **Language:** Ruby 4.0 (`ruby:4.0-slim`, YJIT enabled)
- **Framework:** Rack 3
- **Server:** Iodine
- **Build:** `bundle install --deployment`, jemalloc preloaded

## Endpoints

| Endpoint | Method | Description |
|----------|--------|--------------|
| `/pipeline` | GET | Returns `ok` (plain text) |
| `/baseline11` | GET | Sums query parameters `a` + `b` |
| `/baseline11` | POST | Sums query parameters + request body |
| `/baseline2` | GET | Sums query parameters `a` + `b` |

## Notes

- Started directly via `iodine -p 8080` (no `config.ru`/Puma wrapper needed beyond the Rack app)
- Negative `WORKERS=-1` env var tells Iodine to size workers as a fraction of CPU cores
- Single thread per worker (`THREADS=1`); concurrency comes from Iodine's evented core, not extra Ruby threads
- `RUBY_MN_THREADS=1` and jemalloc (`LD_PRELOAD`) enabled in the Dockerfile
