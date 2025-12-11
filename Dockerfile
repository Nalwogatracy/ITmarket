# Use Java 21 base image
FROM eclipse-temurin:21-jdk

# Set working directory
WORKDIR /app

# Copy everything into the image
COPY . .

# Build the app
RUN ./mvnw clean package -DskipTests

# Expose port Spring Boot runs on
EXPOSE 8080

# Run the app
CMD ["java", "-jar", "target/sample-0.0.1-SNAPSHOT.jar"]

