FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -q -DskipTests clean package -pl ms-clientes -am

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/ms-clientes/target/banco-bff-clientes-1.0.0.jar app.jar
EXPOSE 8091
ENTRYPOINT ["java", "-jar", "app.jar"]
