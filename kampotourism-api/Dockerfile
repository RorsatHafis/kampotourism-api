# syntax=docker/dockerfile:1

# ---- Build stage: compile and package with Maven on Java 21 ----------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Resolve dependencies first so they are cached until pom.xml changes.
COPY pom.xml .
RUN mvn -B -q -DskipTests dependency:go-offline

COPY src ./src
# Tests need Docker (Testcontainers), which is not available inside this build;
# run `mvn verify` locally/CI before deploying.
RUN mvn -B -q -DskipTests package \
 && java -Djarmode=tools -jar target/kampotourism-api-*.jar extract --destination /workspace/app

# ---- Runtime stage: JRE only, non-root ------------------------------------------------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S -G app -h /app app \
 && mkdir -p /tmp/kampotourism-media && chown app:app /tmp/kampotourism-media

# Extracted layout (lib/ + app.jar) starts faster than a nested fat jar.
COPY --from=build --chown=app:app /workspace/app/lib ./lib
COPY --from=build --chown=app:app /workspace/app/kampotourism-api-*.jar ./app.jar

USER app

# JVM tuned for a 512 MB / low-CPU instance (Render Free):
#  - heap capped at 60% of the container limit, leaving room for metaspace, threads and code cache
#  - SerialGC has the smallest footprint; C1-only JIT starts much faster on a fraction of a CPU
#  - crash fast on OOM so the platform restarts the instance instead of it limping along
# Override at deploy time by setting JAVA_TOOL_OPTIONS in the platform's environment.
ENV SPRING_PROFILES_ACTIVE=prod \
    PORT=8080 \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k -XX:ReservedCodeCacheSize=64m -XX:MaxDirectMemorySize=32m -XX:+ExitOnOutOfMemoryError -Djava.awt.headless=true"

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
