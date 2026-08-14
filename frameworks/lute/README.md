# lute

Server built on [Lute](https://github.com/luau-lang/lute), the standalone Luau runtime, using its native `@lute/net/server` HTTP/WebSocket API directly (no application framework layer).

## Stack

- **Language:** Luau
- **Engine:** Lute (standalone runtime, prebuilt binary release)
- **Build:** Prebuilt `lute` binary, `debian:stable-slim` runtime

## Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/pipeline` | GET | Returns `ok` (plain text) |
| `/baseline11` | GET/POST | Sums `a`+`b` query params (+ request body on POST) |
| `/json/{count}` | GET | Returns first `count` dataset items, `total = price * quantity * m` |
| `/upload` | POST | Returns request body byte length |
| `/ws` | GET | Upgrades to a WebSocket echo handler |

## Notes

- `server.luau` spawns one `serve` task per host thread (`system.threadCount()`), each running its own `@lute/vm` VM instance of `serve.luau`
- Each VM instance binds its own listener with `reuseport = true`, so the kernel load-balances connections across threads
- A second, TLS listener on port 8081 is started when `/certs/server.crt`/`server.key` are present
- Dataset is loaded once per VM instance at startup from `/data/dataset.json`
