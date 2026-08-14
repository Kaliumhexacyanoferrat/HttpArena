# aspnet-websocket

ASP.NET Core WebSocket echo server using .NET 10 with Kestrel and `UseWebSockets()`. Single upgrade endpoint that echoes every frame back to the client.

## Stack

- **Language:** C# / .NET 10
- **Framework:** ASP.NET Core (`Microsoft.AspNetCore.WebSockets`)
- **Engine:** Kestrel (HTTP/1.1 + HTTP/2 listener, plaintext)
- **Build:** Framework-dependent publish, `mcr.microsoft.com/dotnet/aspnet:10.0` runtime

## Endpoints

| Endpoint | Method | Description |
|----------|--------|--------------|
| `/ws` | GET (upgrade) | WebSocket upgrade, then echoes every received frame back verbatim |

## Notes

- Non-WebSocket requests to `/ws` get a `400` with a plain-text body instead of upgrading
- The receive loop uses a fixed 4096-byte buffer; each `ReceiveAsync` result is echoed back with `SendAsync` preserving the original `MessageType` and `EndOfMessage` flag, so fragmented messages pass through as received
- `Close` frames are answered with a normal-closure `CloseAsync` and the loop exits
- Logging disabled (`ClearProviders()`) for throughput
- Listens on 8080 only, no TLS

