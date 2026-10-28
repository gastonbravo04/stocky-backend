# ========================================================
# ETAPA 1: COMPILACIÓN (BUILDER)
# Usa Maven con Java 17 para compilar el código fuente a un JAR
# ========================================================
FROM maven:3.9-eclipse-temurin-17-noble AS builder
WORKDIR /app

COPY pom.xml .
COPY src ./src
# Compila el paquete saltando pruebas unitarias para acelerar el build
RUN mvn clean package -DskipTests -B

# ========================================================
# ETAPA 2: IMAGEN FINAL DE EJECUCIÓN (RUNTIME)
# Usa Google Distroless (ultraliviana, segura, sin shell ni vulnerabilidades del SO)
# ========================================================
FROM gcr.io/distroless/java17-debian13:nonroot AS runtime

# Variables de entorno para formato de logs, zona horaria y telemetría de Azure
ENV SPRING_OUTPUT_ANSI_ENABLED=NEVER
ENV TZ=America/Argentina/Buenos_Aires
ENV JAVA_OPTS=""
ENV APPLICATIONINSIGHTS_ROLE_NAME=stocky-backend

# Ejecución con usuario sin privilegios root por buenas prácticas de seguridad
USER nobody
WORKDIR /app

# Copia solo los artefactos finales desde la etapa builder
COPY --from=builder /app/target/agent/applicationinsights-agent.jar /app/applicationinsights-agent.jar
COPY --from=builder /app/target/backend-*.jar /app/app.jar

# Expone el puerto 8080 de Spring Boot
EXPOSE 8080

# Inicia la aplicación con el agente de Application Insights conectado
ENTRYPOINT ["java", "-javaagent:/app/applicationinsights-agent.jar", "-jar", "/app/app.jar"]
