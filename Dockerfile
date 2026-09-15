# Build stage
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace/app

COPY pom.xml .
COPY src src
COPY .mvn .mvn
COPY mvnw .

# Build and safely locate the executable fat jar
RUN chmod +x ./mvnw && ./mvnw clean package -DskipTests \
    && find target -maxdepth 1 -name "*.jar" ! -name "*original*" -exec cp {} /workspace/app/app.jar \;

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/app/app.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]