
FROM maven:3-eclipse-temurin-21 AS build
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jdk-slim
COPY --from=build /target/biometric_attendance_system-1.0-SNAPSHOT.jar biometric-attendance.jar
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "biometric-attendance.jar"]
EXPOSE 8080