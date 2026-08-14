# phoenix-bandit

Phoenix 1.8 running on the Bandit HTTP server, on the Erlang/OTP BEAM VM. Concurrency comes from OTP's lightweight process model; the benchmark data set is cached via `:persistent_term` and an ETS table, with Postgres access through a lazily-started `Postgrex` connection.

## Stack

- **Language:** Elixir 1.20 (OTP 29)
- **Framework:** Phoenix 1.8
- **Engine:** Bandit 1.5 (HTTP server)
- **Build:** `mix release`, multi-stage `elixir:1.20-otp-29-slim`

## Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/pipeline` | GET | Returns `ok` (plain text) |
| `/baseline11` | GET/POST | Sums query params (+ request body on POST) |
| `/baseline2` | GET | Same handler as `/baseline11` GET |
| `/json/:count` | GET | Returns first `count` dataset items, `total = price * quantity * m`; supports Brotli encoding (gzip handled automatically by Bandit) |
| `/async-db` | GET | Postgres range query via Postgrex, JSON response |
| `/upload` | POST | Streams the request body in chunks, returns byte count |
| `/ws` | GET | Upgrades to a WebSocket echo handler |
| `/crud/items` | GET/POST | List / create items |
| `/crud/items/:id` | GET/PUT | Show / update an item |
| `/static/*` | GET | Served by `Plug.Static`, with gzip/brotli precompressed variants |

## Notes

- Dataset is loaded once at application start and stored via `:persistent_term`; a public ETS table (`:items_cache`) is created for read/write-concurrent caching
- Postgres connection pool size is `min(DATABASE_MAX_CONN, 240) / schedulers_online`, started lazily under a `DynamicSupervisor` on first use
- WebSocket echo handler (`PhoenixBanditWeb.EchoWebSocket`) mirrors text/binary frames back to the client via `WebSockAdapter`
- Released as an OTP release (`mix release`) and run as the `nobody` user in the final image
