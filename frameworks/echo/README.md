# Echo

[Echo](https://github.com/labstack/echo) is a high performance, extensible, minimalist Go web framework.

## Key Features

- Optimized HTTP router with zero dynamic memory allocation
- Middleware support (built-in and custom)
- Data binding for HTTP request payload (JSON, XML, form data)
- HTTP/2 support
- Automatic TLS via Let's Encrypt
- Extensive built-in middleware library

## Implementation Notes

- Uses Echo v4 with standard `net/http` server
- Manual compression (deflate preferred, gzip fallback) for the `/compression` endpoint
- SQLite via `modernc.org/sqlite` (pure Go, no CGO)
- Static files pre-loaded into memory at startup
