# syntax=docker/dockerfile:1

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests package

FROM tomcat:10.1-jdk21-temurin

RUN rm -rf /usr/local/tomcat/webapps/*

COPY --from=build /app/target/RetailStoreManagement-1.0-SNAPSHOT.war \
    /usr/local/tomcat/webapps/grocery-store.war

ENV CATALINA_OPTS="-Dfile.encoding=UTF-8"

EXPOSE 8080

CMD ["catalina.sh", "run"]
