# ===================================================================
# Stage 1: Builder
# ===================================================================
FROM golang:1.25.1-alpine AS builder

RUN apk add --no-cache git

# Install the xk6 build tool.
WORKDIR /tmp
RUN go install go.k6.io/xk6/cmd/xk6@latest

# Build the k6 binary, including the sse extension.
RUN xk6 build \
    --with github.com/phymbert/xk6-sse@latest

# ===================================================================
# Stage 1: Final image
# ===================================================================
FROM alpine:3.22.1

# Install ca-certificates for TLS support
RUN apk add --no-cache ca-certificates

# Copy our custom-built k6 binary from the builder stage.
COPY --from=builder /tmp/k6 /usr/bin/k6

# Set the entrypoint to our custom k6.
ENTRYPOINT ["k6"]