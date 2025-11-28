FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src
COPY .m2/settings.xml /root/.m2/settings.xml

ARG GH_TOKEN
RUN mvn -B -s /root/.m2/settings.xml -U -e -X -DskipTests package