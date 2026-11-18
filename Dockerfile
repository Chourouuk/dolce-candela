# ---------- Étape 1 : compilation ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
# Dépendances d'abord : mises en cache tant que pom.xml ne change pas
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
# Les tests sont lancés par la CI (mvn verify) avant la construction de l'image
RUN mvn -B -q package -DskipTests

# ---------- Étape 2 : image d'exécution minimale ----------
FROM eclipse-temurin:21-jre-alpine
LABEL org.opencontainers.image.title="dolce-candela-backend" \
      org.opencontainers.image.description="API e-commerce Dolce Candela"

# Utilisateur non-root
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /build/target/dolce-candela-backend-*.jar app.jar
RUN mkdir -p /app/uploads && chown -R app:app /app
USER app

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError" \
    UPLOAD_DIR=/app/uploads
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
