# The Spring Boot jar of api/ or sync-api/ (the build context), built with Maven in a container: no Java on the host.
# Same as the Dockerfile of the module, plus a BuildKit cache for ~/.m2, so a rebuild does not download the world again.
FROM maven:3.9.16-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp clean package -DskipTests \
    && cp "$(ls target/*.jar | grep -v -- '-plain' | head -1)" /app/app.jar

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/app.jar app.jar
EXPOSE 8443
ENTRYPOINT ["java", "-jar", "app.jar"]
