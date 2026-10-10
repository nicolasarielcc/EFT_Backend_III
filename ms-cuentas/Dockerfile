FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -q -DskipTests clean package -pl ms-cuentas -am

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/ms-cuentas/target/banco-bff-cuentas-1.0.0.jar app.jar
EXPOSE 8092
ENTRYPOINT ["java", "-jar", "app.jar"]
