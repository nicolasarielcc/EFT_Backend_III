FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -q -DskipTests clean package -pl auth-server -am

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/auth-server/target/banco-bff-auth-server-1.0.0.jar app.jar
EXPOSE 9000
ENTRYPOINT ["java", "-jar", "app.jar"]
