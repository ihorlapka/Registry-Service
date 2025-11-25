FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY nexus-full-chain.pem /usr/local/share/ca-certificates/nexus-full-chain.pem
RUN keytool -importcert -noprompt \
        -alias nexus-ca \
        -file /usr/local/share/ca-certificates/nexus-full-chain.pem \
        -cacerts \
        -storepass changeit

RUN update-ca-certificates
COPY src /app/src
RUN mvn -B -U -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY nexus-full-chain.pem /usr/local/share/ca-certificates/nexus-full-chain.pem
RUN keytool -importcert -noprompt \
        -alias nexus-ca \
        -file /usr/local/share/ca-certificates/nexus-full-chain.pem \
        -cacerts \
        -storepass changeit \


RUN update-ca-certificates
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]