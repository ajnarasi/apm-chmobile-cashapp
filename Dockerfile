# Build stage
FROM gradle:8.13-jdk17 AS build
WORKDIR /app

# Copy Gradle wrapper and config
COPY gradlew gradlew
COPY gradle/ gradle/
COPY build.gradle.kts build.gradle.kts
COPY gradle/libs.versions.toml gradle/libs.versions.toml

# Create a minimal settings.gradle.kts for Docker (only backend-server)
RUN echo 'pluginManagement { repositories { mavenCentral(); gradlePluginPortal() } }\n\
dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { mavenCentral() } }\n\
rootProject.name = "cashapp-backend"\n\
include(":backend-server")' > settings.gradle.kts

# Copy backend source
COPY backend-server/ backend-server/

# Build fat JAR
RUN gradle :backend-server:buildFatJar --no-daemon

# Run stage — minimal JRE image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/backend-server/build/libs/backend-server-all.jar app.jar

EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
