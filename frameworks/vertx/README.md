# Vert.x — HttpArena

[Eclipse Vert.x](https://github.com/eclipse-vertx/vert.x) is a reactive toolkit for building high-performance applications on the JVM. Unlike traditional frameworks (Spring, Quarkus), Vert.x uses an event-loop threading model directly on Netty — no annotation scanning, no dependency injection, no framework overhead.

## Key Details

- **Vert.x 4.5.14** (latest stable) with vertx-web for routing
- **Event-loop architecture**: one verticle instance per CPU core, each on its own event loop
- **JDK 21** with ParallelGC
- **Jackson** for JSON serialization
- **SQLite** via JDBC (executed on worker threads via `executeBlocking()` to avoid blocking event loops)
- **Pre-computed** JSON and gzip responses at startup
- **Netty native transport** enabled (`preferNativeTransport`)

## Architecture Notes

Vert.x sits in a unique spot in the JVM ecosystem:
- **Spring** = DI + annotations + Tomcat/Netty (high-level framework)
- **Quarkus** = CDI + annotations + Vert.x/Netty under the hood (compile-time optimization)
- **Vert.x** = event loops + handlers + Netty directly (reactive toolkit)

Quarkus actually uses Vert.x internally, so this benchmark shows the raw Vert.x performance vs. the Quarkus abstraction on top.
