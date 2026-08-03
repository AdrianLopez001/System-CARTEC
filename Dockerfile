# Multi-stage build for Java 21 Spring Boot
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copiar arquivos de dependencias
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

# Copiar codigo fonte e gerar JAR
COPY src src
RUN ./mvnw package -DskipTests

# Stage de execucao
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/target/cartec-sistema.jar app.jar

EXPOSE 8080
ENV PORT=8080

CMD ["java", "-jar", "app.jar"]
