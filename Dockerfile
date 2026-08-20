
FROM gradle:9.5.0-jdk25 AS build

WORKDIR /home/gradle/src

COPY --chown=gradle:gradle gradlew build.gradle.kts settings.gradle.kts gradle.properties ./
COPY --chown=gradle:gradle gradle ./gradle

RUN ./gradlew dependencies --no-daemon

COPY --chown=gradle:gradle src ./src

RUN ./gradlew buildFatJar --no-daemon

FROM eclipse-temurin:25-jre-alpine AS runtime

WORKDIR /app

COPY --from=build /home/gradle/src/build/libs/*.jar /app/ktor-app.jar

EXPOSE 9000

ENTRYPOINT ["java", "-jar", "/app/ktor-app.jar"]