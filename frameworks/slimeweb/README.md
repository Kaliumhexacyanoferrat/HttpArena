# slimeweb

[SlimeWeb](https://github.com/ATOMMAX-2001/Slime/), a Python web framework backed by a Rust/hyper runtime, managed and run through its own `slime` CLI (built on `uv`).

## Stack

- **Language:** Python 3.13
- **Framework:** SlimeWeb
- **Engine:** hyper (Rust)
- **Build:** `python:3.13-slim`, scaffolded with `slime new` and dependencies added via `slime add`

## Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/pipeline` | GET | Returns `ok` (plain text) |
| `/baseline11` | GET, POST | Sums query parameters (+ body on POST) |
| `/json/{count}` | GET | Processes dataset, returns JSON (route-level gzip) |
| `/upload` | POST | Returns byte count of the request body (up to 25 MB) |
| `/async-db` | GET | Postgres range query with JSON response |
| `/ws` | WebSocket | Echoes text/binary frames back to the sender |

## Notes

- `/json` uses SlimeWeb's built-in per-route compression (`SlimeCompression.All`, level 1) instead of application-level gzip handling
- `/upload` overrides the default 10 MB body-size limit to 25 MB via the route's `body_size` argument
- WebSocket handling is event-driven: `on_message` registers a callback that mirrors text frames as text and binary frames as binary
- Postgres access via `asyncpg`, with a `NoResetConnection` subclass skipping the per-checkout reset query; pool created in an `@app.start()` hook, sized from `DATABASE_MAX_CONN`
- Static files served from `/data/static` via `app.serve(..., static_path=...)`
- Dependencies (including `asyncpg`) are installed via the `slime`/`uv` toolchain rather than a `requirements.txt`
