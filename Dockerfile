FROM gradle:8.7-jdk17 AS build

ARG MODULE=api-module
WORKDIR /workspace

COPY settings.gradle build.gradle ./
COPY gradle ./gradle
COPY gradlew ./
COPY core-module ./core-module
COPY api-module ./api-module
COPY admin-module ./admin-module

RUN gradle :${MODULE}:bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre

ARG MODULE=api-module
WORKDIR /app
COPY --from=build /workspace/${MODULE}/build/libs/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
