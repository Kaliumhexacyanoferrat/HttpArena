# aspnet-minimal-iouring

ASP.NET Core minimal API on a custom .NET 11 preview runtime build with an experimental io_uring socket engine ([dotnet/runtime#124374](https://github.com/benaadams/runtime/tree/io_uring)), replacing epoll for socket I/O on Linux 6.1+. Otherwise the same handler surface as `aspnet-minimal`.

## Stack

- **Language:** C# / .NET 11 preview
- **Framework:** ASP.NET Core Minimal APIs
- **Engine:** Kestrel, with the runtime's socket layer patched to use io_uring (SQPOLL) instead of epoll
- **Build:** Three-stage — (1) clone `benaadams/runtime` (`io_uring` branch), apply `patch-iouring.py`, and build the CLR/libs from source; (2) publish the app against the stock `sdk:11.0-preview` image; (3) overlay the custom runtime binaries onto `mcr.microsoft.com/dotnet/aspnet:11.0-preview` and run the app against them

## Endpoints

| Endpoint | Method | Description |
|----------|--------|--------------|
| `/pipeline` | GET | Returns `ok` (plain text) |
| `/baseline11` | GET | Sums query parameter values |
| `/baseline11` | POST | Sums query parameters + request body |
| `/baseline2` | GET | Sums query parameter values (HTTP/2 variant) |
| `/json/{count}` | GET | Returns `count` items from the preloaded dataset; honors `Accept-Encoding` for the `json-comp` profile |
| `/async-db` | GET | Postgres range query: `SELECT ... WHERE price BETWEEN $min AND $max LIMIT $limit` |
| `/upload` | POST | Streams the request body via a pooled buffer and returns the byte count |
| `/crud/items` | GET | Paginated list by category |
| `/crud/items/{id}` | GET | Single item read with `IMemoryCache` (1s TTL), returns `X-Cache: HIT/MISS` |
| `/crud/items` | POST | Create item via INSERT with ON CONFLICT upsert, returns 201 |
| `/crud/items/{id}` | PUT | Update item and invalidate cache entry |
| `/static/*` | GET | Serves files from `/data/static` via `MapStaticAssets` |

## Notes

- io_uring is enabled via `AppContext.SetSwitch("System.Net.Sockets.UseIoUringSqPoll", true)` set at the very top of `Main`, before any `Socket` is created, plus `DOTNET_SYSTEM_NET_SOCKETS_IO_URING=1` and `DOTNET_SYSTEM_NET_SOCKETS_IO_URING_SQPOLL=1` env vars in the Dockerfile
- Startup logs whether the io_uring runtime type is actually present in `System.Net.Sockets` (reflection check against the loaded assembly), so `docker logs` confirms the overlay took effect rather than silently falling back to epoll
- Response compression registers only the Brotli and Gzip providers explicitly — the io_uring branch's runtime snapshot predates `ZstandardCompressionOptions`, and the default `AddResponseCompression()` factory throws `TypeLoadException` trying to load all three
- HTTP/1.1 on port 8080, HTTP/1+2+3 on port 8443 (TLS from `$TLS_CERT`/`$TLS_KEY`), h1-only TLS on port 8081 for the `json-tls` profile
- HTTP/2 tuned: 256 max streams per connection, 2 MB initial connection window, 1 MB stream window
- Source layout mirrors `aspnet-minimal` (`Program.cs`, `Handlers.cs`, `AppData.cs`, `Models.cs`)

