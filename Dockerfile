# Stage 1: Build
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# Fix Windows CRLF line endings & make executable
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN ./mvnw dependency:resolve
COPY src/ src/
RUN ./mvnw package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
