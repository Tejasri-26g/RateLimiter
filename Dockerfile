FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace/app

COPY pom.xml .
COPY src src
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x ./mvnw && ./mvnw clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Pick the executable jar and avoid any plain/original jars
COPY --from=build /workspace/app/target/*[!original].jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]