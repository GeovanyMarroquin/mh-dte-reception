FROM eclipse-temurin:25-jdk AS build

WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew

COPY src ./src
RUN ./gradlew --no-daemon bootJar

FROM eclipse-temurin:25-jre

ENV SERVER_PORT=8085
ENV DTE_DB_PATH=/app/data/dte_mock.db
WORKDIR /app

RUN groupadd --system --gid 10001 dteapp \
    && useradd --system --uid 10001 --gid dteapp --home-dir /app --no-create-home dteapp \
    && mkdir -p /app/data \
    && chown -R 10001:10001 /app/data

COPY --from=build --chown=10001:10001 /workspace/build/libs/*.jar /app/app.jar

EXPOSE 8085
USER 10001:10001

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
