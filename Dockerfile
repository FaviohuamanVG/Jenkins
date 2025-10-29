# Etapa 1: compilación
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN chmod +x mvnw
RUN ./mvnw clean package -Dmaven.test.skip=true

# Etapa 2: runtime limpio
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/vg-ms-user-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8100
CMD ["java", "-jar", "app.jar"]
