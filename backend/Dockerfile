# Stage 1: build the jar using Maven (this image has Java + Maven)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -q package -Dmaven.test.skip=true

# Stage 2: run it on a small Java-only image (no Maven, no source code)
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/mini-hrms-0.1.0.jar app.jar
EXPOSE 8080
# MaxRAMPercentage: let Java use 75% of the container memory (free hosts give ~512 MB)
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
