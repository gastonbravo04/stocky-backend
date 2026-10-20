FROM maven:3.9-eclipse-temurin-17-noble AS builder
WORKDIR /app

# Descargar agente oficial de Application Insights para Java (telemetría y Live Metrics)
ADD https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.5.4/applicationinsights-agent-3.5.4.jar /app/applicationinsights-agent.jar

COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests -B

FROM gcr.io/distroless/java17-debian13:nonroot AS runtime
ENV SPRING_OUTPUT_ANSI_ENABLED=ALWAYS
ENV JAVA_OPTS=""

USER nobody
WORKDIR /app

COPY --from=builder /app/applicationinsights-agent.jar /app/applicationinsights-agent.jar
COPY --from=builder /app/target/*.jar /app/app.jar
EXPOSE 8080

ENTRYPOINT ["java", "-javaagent:/app/applicationinsights-agent.jar", "-jar", "/app/app.jar"]
