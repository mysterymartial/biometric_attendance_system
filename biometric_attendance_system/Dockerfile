# Build stage
FROM maven:3-eclipse-temurin-21 AS build
WORKDIR /app
COPY biometric_attendance_system/ .
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre
COPY --from=build /app/target/biometric_attendance_system-1.0-SNAPSHOT.jar biometric-attendance.jar
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "biometric-attendance.jar"]
EXPOSE 8080