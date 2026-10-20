FROM maven:3.9-eclipse-temurin-17-noble AS builder
WORKDIR /app

COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests -B

FROM gcr.io/distroless/java17-debian13:nonroot AS runtime
ENV SPRING_OUTPUT_ANSI_ENABLED=NEVER
ENV TZ=America/Argentina/Buenos_Aires
ENV JAVA_OPTS=""
ENV APPLICATIONINSIGHTS_ROLE_NAME=stocky-backend

USER nobody
WORKDIR /app

COPY --from=builder /app/target/agent/applicationinsights-agent.jar /app/applicationinsights-agent.jar
COPY --from=builder /app/target/backend-*.jar /app/app.jar
EXPOSE 8080

ENTRYPOINT ["java", "-javaagent:/app/applicationinsights-agent.jar", "-jar", "/app/app.jar"]
