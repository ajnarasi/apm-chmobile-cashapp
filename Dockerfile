# Build stage
FROM --platform=linux/amd64 gradle:8.13-jdk17 AS build
WORKDIR /app

# Copy Gradle wrapper and version catalog
COPY gradle/wrapper/ gradle/wrapper/
COPY gradle/libs.versions.toml gradle/libs.versions.toml
COPY gradlew gradlew
RUN chmod +x gradlew

# Use Docker-specific settings (no Android modules)
COPY docker-settings.gradle.kts settings.gradle.kts

# Minimal root build file (no Android plugin references)
RUN echo 'plugins { }' > build.gradle.kts

# Copy backend module
COPY backend-server/ backend-server/

# Build fat JAR
RUN ./gradlew :backend-server:buildFatJar --no-daemon

# Run stage
FROM --platform=linux/amd64 eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/backend-server/build/libs/backend-server-all.jar app.jar

EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
