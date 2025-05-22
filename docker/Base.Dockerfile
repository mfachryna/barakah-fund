FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /app

ARG SERVICE_SRC
ARG JAR_NAME

COPY common-proto common-proto
COPY ${SERVICE_SRC}/pom.xml .
COPY ${SERVICE_SRC}/src src

RUN --mount=type=cache,target=/root/.m2 mvn -f common-proto/pom.xml clean install -DskipTests

RUN --mount=type=cache,target=/root/.m2 mvn -B dependency:go-offline

COPY . .

RUN --mount=type=cache,target=/root/.m2 mvn -B clean package -DskipTests


FROM eclipse-temurin:17-jre
WORKDIR /app

ARG EXPOSE_PORT

COPY --from=builder /app/target/*.jar app.jar
HEALTHCHECK --interval=30s --timeout=5s \
  CMD curl -f http://localhost:${EXPOSE_PORT}/actuator/health || exit 1

EXPOSE ${EXPOSE_PORT}
ENTRYPOINT ["java","-Duser.timezone=Asia/Jakarta","-jar","/app/app.jar"]