# scripts/lib/postgres.sh — Postgres sidecar lifecycle for async-db and
# api-{4,16} tests. Single well-known container name, host networking,
# persistent init from data/pgdb-seed.sql.
#
# Matches the original benchmark.sh setup exactly — `-c max_connections=256`
# passed on the command line rather than mounting a custom config file.

postgres_start() {
    info "starting postgres sidecar"

    docker rm -f "$PG_CONTAINER" 2>/dev/null || true

    docker run -d --name "$PG_CONTAINER" --network host \
        -e POSTGRES_USER=bench \
        -e POSTGRES_PASSWORD=bench \
        -e POSTGRES_DB=benchmark \
        -v "$DATA_DIR/pgdb-seed.sql:/docker-entrypoint-initdb.d/seed.sql:ro" \
        postgres:17-alpine \
        -c max_connections=256 >/dev/null

    # Wait for postgres to accept queries AND for the seed script to finish.
    # Readiness check uses `SELECT 1 FROM items LIMIT 1` — the items table
    # is created + populated by the entrypoint seed, so this covers both
    # "daemon ready" and "seed complete" in one probe.
    local i
    for i in $(seq 1 60); do
        if docker exec "$PG_CONTAINER" pg_isready -U bench -d benchmark >/dev/null 2>&1; then
            if docker exec "$PG_CONTAINER" psql -U bench -d benchmark -tAc \
                'SELECT 1 FROM items LIMIT 1' 2>/dev/null | grep -q 1; then
                info "postgres ready (seeded)"
                return 0
            fi
        fi
        sleep 1
    done

    fail "postgres sidecar did not become ready within 60s"
}

postgres_stop() {
    docker rm -f "$PG_CONTAINER" 2>/dev/null || true
}
