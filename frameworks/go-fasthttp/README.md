# go-fasthttp

High-performance Go HTTP server using fasthttp with zero-allocation design and buffer reuse.

## Stack

- **Language:** Go 1.24
- **Framework:** fasthttp
- **Build:** `golang:1.24-alpine` → `alpine:3.19` runtime

## Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/pipeline` | GET | Returns `ok` (plain text) |
| `/baseline11` | GET | Sums query parameter values |
| `/baseline11` | POST | Sums query parameters + request body |
| `/json` | GET | Processes 50-item dataset, serializes JSON |
| `/compression` | GET | Gzip-compressed large JSON response |
| `/db` | GET | SQLite range query with JSON response |
| `/upload` | POST | Receives 1 MB body, returns byte count |

## Notes

- One goroutine listener per CPU core via `SO_REUSEPORT`
- `modernc.org/sqlite` for CGO-free database access
- Compression via `compress/flate` (level 1)
- Zero-copy query parameter iteration with `VisitAll`
- Baseline11 is the default route handler

