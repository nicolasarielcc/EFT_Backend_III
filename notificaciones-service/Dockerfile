FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -q -DskipTests clean package -pl notificaciones-service -am

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/notificaciones-service/target/banco-bff-notificaciones-1.0.0.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
