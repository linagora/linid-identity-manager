FROM maven:3.9.16-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/*.jar linid-identity-manager-api.jar
EXPOSE 8443
ENTRYPOINT ["java", "-jar", "linid-identity-manager-api.jar"]
