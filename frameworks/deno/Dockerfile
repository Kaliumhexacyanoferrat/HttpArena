FROM denoland/deno:2.2.2
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY main.ts .
RUN deno cache main.ts
RUN curl -fsSL -o /app/libsqlite3.so https://github.com/denodrivers/sqlite3/releases/download/0.12.0/libsqlite3.so
ENV DENO_SQLITE_PATH=/app/libsqlite3.so
EXPOSE 8080
CMD ["deno", "serve", "--parallel", "--port", "8080", "--host", "0.0.0.0", "-A", "main.ts"]
