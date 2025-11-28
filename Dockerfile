FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src
COPY .m2/settings.xml /root/.m2/settings.xml

RUN mvn -B -U -e -X -DskipTests package