---
title: Endpoints
---

Your framework must implement endpoints depending on which test profiles it participates in. All endpoints are served on **port 8080** (HTTP/1.1) unless noted otherwise.

Data files are **mounted automatically** by the benchmark runner — your Dockerfile does not need to include them. The following paths are available inside the container at runtime:

| Path | Description |
|------|-------------|
| `/data/dataset.json` | 50-item dataset for `/json` |
| `/data/dataset-large.json` | 6000-item dataset for `/compression` |
| `/data/benchmark.db` | SQLite database (100K rows) for `/db` |
| `/data/static/` | 20 static files for `/static/*` |
| `/certs/server.crt`, `/certs/server.key` | TLS certificate and key for HTTPS/H2/H3 |

## Core endpoints

These are required for the `baseline`, `limited-conn`, and `noisy` profiles.

### `GET/POST /baseline11?a=N&b=N`

Parse query parameters `a` and `b`, compute their sum, and return it as plain text.

For POST requests, the body contains an additional integer to add. The server must accept both `Content-Length` and `chunked Transfer-Encoding` bodies.

```
GET /baseline11?a=13&b=42 → "55"
POST /baseline11?a=13&b=42 (body: "20") → "75"
```

### `GET /pipeline`

Return a fixed `ok` response (exactly 2 bytes, `text/plain`). Used for the pipelined benchmark — should be as lightweight as possible.

## JSON processing

Required for the `json` profile.

### `GET /json`

Load `/data/dataset.json` at startup (50 items). For each request, compute `total = price * quantity` (rounded to 2 decimals) for every item and return:

```json
{
  "items": [
    {"id": 1, "name": "Alpha Widget", "category": "electronics", "price": 29.99, "quantity": 5, "active": true, "tags": ["fast", "new"], "rating": {"score": 4.2, "count": 127}, "total": 149.95}
  ],
  "count": 50
}
```

Response must have `Content-Type: application/json`.

## Upload

Required for the `upload` profile.

### `POST /upload`

Read the entire request body (up to 20 MB binary) and return its CRC32 checksum as an 8-character lowercase hex string.

```
POST /upload (body: 20MB binary) → "a1b2c3d4"
```

The CRC32 uses the ISO 3309 polynomial (`0xEDB88320`), the same as `zlib.crc32`.

## Database

Required for the `db` and `mixed` profiles.

### `GET /db?min=N&max=N`

Open `/data/benchmark.db` (a SQLite database with 100K rows) at startup in read-only mode. For each request, query items where `price BETWEEN min AND max` (default 10–50), limit 50 rows.

Return JSON with parsed tags and structured rating:

```json
{
  "items": [
    {"id": 1, "name": "Widget", "category": "electronics", "price": 29.99, "quantity": 5, "active": true, "tags": ["fast"], "rating": {"score": 4.2, "count": 127}}
  ],
  "count": 42
}
```

The `tags` column is stored as a JSON string in the database — parse it into an array. The `active` column is an integer (0/1) — return it as a boolean. The `rating_score` and `rating_count` columns should be returned as a nested `rating` object.

Response must have `Content-Type: application/json`.

## Compression

Required for the `compression` profile.

### `GET /compression`

Load `/data/dataset-large.json` at startup (6000 items). Compute `total` for each item (same as `/json`), and return the response with `Content-Type: application/json`.

The server **must** support gzip compression — when the client sends `Accept-Encoding: gzip`, the response should be gzip-compressed. Only frameworks with built-in gzip support are eligible.

## Static files

Required for the `static-h2` and `static-h3` profiles. Served over **HTTPS on port 8443**.

### `GET /static/{filename}`

Serve 20 pre-loaded static files from `/data/static/`. Files should be loaded into memory at startup and served with the correct `Content-Type`:

| Extension | Content-Type |
|-----------|-------------|
| `.css` | `text/css` |
| `.js` | `application/javascript` |
| `.html` | `text/html` |
| `.woff2` | `font/woff2` |
| `.svg` | `image/svg+xml` |
| `.webp` | `image/webp` |
| `.json` | `application/json` |

Return `404` for missing files.

## HTTP/2 baseline

Required for the `baseline-h2` profile. Served over **HTTPS on port 8443**.

### `GET /baseline2?a=N&b=N`

Same logic as `/baseline11` — parse query parameters and return their sum. Served over HTTP/2 with TLS.

Read the TLS certificate and key from `/certs/server.crt` and `/certs/server.key` (mounted read-only by the benchmark runner).

## HTTP/3

For `baseline-h3` and `static-h3` profiles. The same endpoints (`/baseline2` and `/static/*`) are served over **HTTP/3 (QUIC) on port 8443**.

This requires native QUIC support in the framework. Only add H3 tests to your `meta.json` if your framework supports it.
