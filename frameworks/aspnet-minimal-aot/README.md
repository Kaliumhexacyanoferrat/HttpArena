# aspnet-minimal-aot

Minimal ASP.NET Core Native AOT HTTP server using .NET 10 with Kestrel and minimal API routing.

## Stack

- **Language:** C# / .NET 10 (Alpine)
- **Framework:** ASP.NET Core Minimal APIs with Native AOT
- **Engine:** Kestrel
- **Build:** Native AOT publish, `runtime-deps:10.0` runtime

## Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/pipeline` | GET | Returns `ok` (plain text) |
| `/baseline11` | GET | Sums query parameter values |
| `/baseline11` | POST | Sums query parameters + request body |
| `/baseline2` | GET | Sums query parameter values (HTTP/2 variant) |
| `/json` | GET | Processes 50-item dataset, serializes JSON |
| `/compression` | GET | Gzip-compressed large JSON response |
| `/db` | GET | SQLite range query with JSON response |
| `/upload` | POST | Receives 1 MB body, returns byte count |
| `/static/{filename}` | GET | Serves preloaded static files with MIME types |

## Notes

- HTTP/1.1 on port 8080, HTTP/1+2+3 on port 8443
- Logging disabled (`ClearProviders()`) for throughput
- Source-generated JSON metadata for AOT-safe serialization
- Static JSON payload shaping moved to startup for hot-path allocation reduction
- Response compression middleware configured for gzip fastest level
- HTTP/2 tuned: 256 max streams, 2 MB connection window
- Server GC and Native AOT optimization preference set for throughput
