FROM eclipse-temurin:17.0.13_11-jre-alpine
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
EXPOSE 9000
ENTRYPOINT ["java", "-Dspring.profiles.active=pg", "-jar", "app.jar"]