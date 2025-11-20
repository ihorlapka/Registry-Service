FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . /workspace
COPY pom.xml .
COPY src /app/src
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY /home/ihor/Documents/nexus-crt/iot-nexus.crt /usr/local/share/ca-certificates/iot-nexus.crt
RUN update-ca-certificates
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]