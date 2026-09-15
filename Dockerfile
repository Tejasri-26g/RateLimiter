FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace/app

COPY pom.xml .
COPY src src
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x ./mvnw && ./mvnw clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp
COPY --from=build /workspace/app/target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
