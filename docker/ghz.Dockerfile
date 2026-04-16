# ghz — proto-aware gRPC load generator (https://ghz.sh/)
#
# Self-contained: clones and builds ghz from source in a Go toolchain stage,
# copies the resulting static binary into a scratch-like final image. No
# runtime dependencies on the host, no local gopath, no prebuilt release.
#
# The final image is tiny (~25 MB) because Go statically links and the
# runtime stage ships only the ghz binary plus ca-certificates.

ARG GHZ_VERSION=v0.121.0

FROM golang:1.23-alpine AS build
ARG GHZ_VERSION
RUN apk add --no-cache git ca-certificates
WORKDIR /src
RUN git clone --depth 1 --branch "$GHZ_VERSION" https://github.com/bojand/ghz.git . && \
    CGO_ENABLED=0 GOOS=linux go build -ldflags="-s -w" -o /ghz ./cmd/ghz

FROM alpine:3.21
RUN apk add --no-cache ca-certificates
COPY --from=build /ghz /usr/local/bin/ghz
ENTRYPOINT ["ghz"]
