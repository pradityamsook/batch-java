FROM maven:3.8.4-openjdk-8 AS build
WORKDIR /app
COPY pom.xml .
COPY . /app

## บังคับ Maven ให้ใช้ JDK 8 ในการคอมไพล์
#ENV MAVEN_OPTS="-Dmaven.compiler.source=1.8 -Dmaven.compiler.target=1.8"

FROM openjdk:8-jre-slim
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]