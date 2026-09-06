# Build stage
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Gradle wrapper
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./

# Download deps (cached unless build files change)
RUN ./gradlew dependencies --no-daemon || true

# Source & build
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN adduser -D -g "" appuser
USER appuser

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 1567
ENV SERVER_PORT=1567

ENTRYPOINT ["java", "-jar", "app.jar"]
