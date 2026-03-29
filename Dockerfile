# Stage 1 — Build
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY gradle/ gradle/
COPY gradlew .
COPY build.gradle .
COPY settings.gradle .
COPY src/ src/

RUN chmod +x gradlew
RUN ./gradlew clean build -x test

# Stage 2 — Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
