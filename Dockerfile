# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
COPY src ./src
RUN ./mvnw -q -DskipTests package

# Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/movie-share-*.jar app.jar
EXPOSE 8081
ENV SERVER_PORT=8081
ENTRYPOINT ["java", "-jar", "app.jar"]
