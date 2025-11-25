FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY ca.crt /usr/local/share/ca-certificates/iot-root-ca.crt
RUN update-ca-certificates
RUN keytool -importcert \
    -alias iot-root-ca \
    -file /usr/local/share/ca-certificates/iot-root-ca.crt \
    -keystore $JAVA_HOME/lib/security/cacerts \
    -storepass changeit -trustcacerts -noprompt
RUN keytool -list -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit | grep -i iot
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY ca.crt /usr/local/share/ca-certificates/iot-root-ca.crt
RUN update-ca-certificates
RUN keytool -importcert \
    -alias iot-root-ca \
    -file /usr/local/share/ca-certificates/iot-root-ca.crt \
    -keystore $JAVA_HOME/lib/security/cacerts \
    -storepass changeit -trustcacerts -noprompt
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]