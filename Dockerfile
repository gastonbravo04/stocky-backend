FROM maven:3.9-eclipse-temurin-17-noble AS builder
WORKDIR /app

COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests -B

FROM gcr.io/distroless/java17-debian13:nonroot AS runtime
ENV SPRING_OUTPUT_ANSI_ENABLED=NEVER
ENV TZ=America/Argentina/Buenos_Aires
ENV JAVA_OPTS=""

USER nobody
WORKDIR /app

COPY --from=builder /app/target/*.jar /app/app.jar
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
