FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . /workspace
COPY pom.xml .
COPY src /app/src
COPY ca.crt /usr/local/share/ca-certificates/ca.crt
RUN update-ca-certificates
RUN mvn -B -DskipTests -U package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY ca.crt /usr/local/share/ca-certificates/ca.crt
RUN update-ca-certificates
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]