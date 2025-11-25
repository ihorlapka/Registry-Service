FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . /workspace
COPY pom.xml .
COPY src /app/src
COPY ca.crt /usr/local/share/ca-certificates/ca.crt
RUN update-ca-certificates &&  \
    keytool -import -alias iot-nexus-ca  \
    -file /usr/local/share/ca-certificates/ca.crt  \
    -keystore $JAVA_HOME/lib/security/cacerts  \
    -storepass changeit -noprompt
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests -U package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY ca.crt /usr/local/share/ca-certificates/ca.crt
RUN update-ca-certificates &&  \
    keytool -import -alias iot-nexus-ca  \
    -file /usr/local/share/ca-certificates/ca.crt  \
    -keystore $JAVA_HOME/lib/security/cacerts  \
    -storepass changeit -noprompt
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]