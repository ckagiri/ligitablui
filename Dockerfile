# syntax=docker/dockerfile:1

# Build stage
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /build

# Install Maven
RUN apt-get update && \
    apt-get install -y maven && \
    rm -rf /var/lib/apt/lists/*

# Copy pom.xml first (for better caching)
COPY pom.xml .

# Download dependencies with retry logic
# This handles transient network failures
RUN mvn dependency:go-offline -B || \
    mvn dependency:go-offline -B || \
    mvn dependency:go-offline -B

# Copy source
COPY src src

# Build with retry on failure
RUN mvn clean package -DskipTests -B || \
    (mvn clean && mvn package -DskipTests -B)

# Runtime stage
FROM eclipse-temurin:21-jre

# Install curl for healthchecks
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN groupadd -r appuser && useradd -r -g appuser appuser

WORKDIR /app

# Copy JAR from builder
COPY --from=builder /build/target/*.jar app.jar

RUN chown -R appuser:appuser /app

USER appuser

ENV JAVA_OPTS="" \
    SERVER_PORT=8080

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080 || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]