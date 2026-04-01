# Multi-stage Dockerfile for Quarkus HR+ Backend
# Build mode: fast-jar (optimized for Docker, faster than JVM mode, no native complexity)

# ============================================================================
# Stage 1: Build stage
# ============================================================================
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

# Install build dependencies (none needed for Maven + Java 21)
WORKDIR /build

# Copy Maven configuration files first (for layer caching)
COPY pom.xml ./
COPY .mvn ./.mvn

# Download dependencies (cached layer if pom.xml unchanged)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application in fast-jar mode
# - Skips tests (run separately in CI)
# - Produces optimized JAR structure in target/quarkus-app/
RUN mvn package -DskipTests -Dquarkus.package.jar.type=fast-jar

# ============================================================================
# Stage 2: Runtime stage
# ============================================================================
FROM eclipse-temurin:21-jre-alpine

# Create non-root user for security
RUN addgroup -S quarkus && adduser -S quarkus -G quarkus

# Set working directory
WORKDIR /app

# Copy application artifacts from builder stage
COPY --from=builder --chown=quarkus:quarkus /build/target/quarkus-app/ ./

# Switch to non-root user
USER quarkus

# Expose HTTP port
EXPOSE 8080

# Health check (Quarkus liveness probe)
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/q/health/live || exit 1

# Set JVM options for containerized environment
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

# Run application
# - Uses quarkus-run.jar as entry point
# - Quarkus fast-jar loads dependencies from lib/ directory
ENTRYPOINT ["java", "-jar", "quarkus-run.jar"]

# ============================================================================
# Build and run instructions:
# ============================================================================
# Build:
#   docker build -t hrplus-backend:latest .
#
# Run locally:
#   docker run -p 8080:8080 \
#     -e QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://host.docker.internal:5432/hrplus \
#     -e QUARKUS_DATASOURCE_USERNAME=hrplus \
#     -e QUARKUS_DATASOURCE_PASSWORD=hrplus \
#     -e QUARKUS_OIDC_AUTH_SERVER_URL=https://hrplus.auth0.com \
#     hrplus-backend:latest
#
# Run with docker-compose:
#   docker-compose up --build
#
# ============================================================================
# Image size comparison (approximate):
# ============================================================================
# - JVM mode (quarkus.package.jar.type=uber-jar): ~200-250 MB
# - Fast-jar mode (this Dockerfile): ~180-220 MB
# - Native mode (quarkus.package.type=native): ~100-150 MB (but 5min+ build)
#
# Fast-jar is the sweet spot: faster builds than native, smaller than uber-jar
# ============================================================================
