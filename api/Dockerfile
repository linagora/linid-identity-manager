# syntax=docker/dockerfile:1
FROM maven:3.9.16-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# CI supplies credentials as a BuildKit secret; local builds use Maven Central.
RUN --mount=type=secret,id=maven_settings \
    if [ -f /run/secrets/maven_settings ]; then \
      mvn -B -s /run/secrets/maven_settings clean package -DskipTests; \
    else \
      mvn -B clean package -DskipTests; \
    fi

FROM eclipse-temurin:25-jre
RUN apt-get update && apt-get upgrade -y && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/target/*.jar linid-identity-manager-api.jar
EXPOSE 8443
ENTRYPOINT ["java", "-jar", "linid-identity-manager-api.jar"]
