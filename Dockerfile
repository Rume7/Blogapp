# ---- Build Stage ----
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

COPY . .

RUN mvn -pl blog-service clean package -DskipTests

# ---- Run Stage ----
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the built JAR (version-agnostic)
COPY --from=build /app/blog-service/target/blog-service-*.jar blogapp.jar

# Expose the port your Spring Boot app runs on
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "blogapp.jar"]
