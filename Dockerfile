FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . /workspace
COPY pom.xml .
COPY src /app/src
COPY iot-nexus.crt /tmp/iot-nexus.crt
RUN keytool -importcert -trustcacerts -noprompt \
    -alias iot-nexus-ca \
    -file /tmp/iot-nexus.crt \
    -keystore $JAVA_HOME/lib/security/cacerts \
    -storepass changeit
RUN mvn -B -DskipTests -U package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY iot-nexus.crt /tmp/iot-nexus.crt
RUN keytool -importcert -trustcacerts -noprompt \
    -alias iot-nexus-ca \
    -file /tmp/iot-nexus.crt \
    -keystore $JAVA_HOME/lib/security/cacerts \
    -storepass changeit
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]