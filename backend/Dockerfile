FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /workspace

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY src ./src

RUN chmod +x gradlew \
    && ./gradlew clean bootJar -x test \
    && find build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' -exec cp {} /tmp/app.jar \;

FROM eclipse-temurin:21-jre-jammy AS runtime

RUN apt-get update \
    && apt-get install --no-install-recommends -y curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system anexo24 \
    && useradd --system --gid anexo24 --home-dir /app --no-create-home anexo24

WORKDIR /app
COPY --from=build /tmp/app.jar /app/app.jar
RUN chown anexo24:anexo24 /app/app.jar

USER anexo24
EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=12 \
    CMD curl --fail --silent --show-error http://127.0.0.1:8080/actuator/health || exit 1

# JAVA_TOOL_OPTIONS permite opciones JVM sin introducir un shell intermedio.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
